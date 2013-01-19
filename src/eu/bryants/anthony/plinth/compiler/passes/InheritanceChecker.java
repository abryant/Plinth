package eu.bryants.anthony.plinth.compiler.passes;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompilationUnit;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.compiler.ConceptualException;

/*
 * Created on 9 Jan 2013
 */

/**
 * This compilation pass checks that all inherited and overriding members behave properly, by performing checks such as the following:
 * <ul>
 * <li>Overriding methods have the same guarantees on their behaviour that their superclass methods have, e.g. if a superclass method is immutable, then the overriding method must be too</li>
 * <li>Abstract methods must be overridden by all non-abstract classes</li>
 * <li>Sealed methods must never be overridden</li>
 * </ul>
 *
 * Requires that the following passes have already been run:
 * <ul>
 * <li>Resolver (top level types)</li>
 * <li>Non-static method list building</li>
 * <li>Cycle Checker (inheritance)</li>
 * </ul>
 * @author Anthony Bryant
 */
public class InheritanceChecker
{
  /**
   * Checks the inherited members of TypeDefinitions in the specified CompilationUnit for problems.
   * @param compilationUnit - the CompilationUnit to check all of the TypeDefinitions of
   * @throws ConceptualException - if there is a conceptual problem with one of the TypeDefinitions in the specified CompilationUnit
   */
  public static void checkInheritedMembers(CompilationUnit compilationUnit) throws ConceptualException
  {
    for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
    {
      checkInheritedMembers(typeDefinition);
    }
  }

  /**
   * Finds the linearisation of the inherited classes of the specified type, and returns it after caching it in the TypeDefinition itself
   * @param typeDefinition - the TypeDefinition to get the linearisation of
   * @throws ConceptualException - if there is no linearisation for the specified type
   */
  private static TypeDefinition[] findInheritanceLinearisation(TypeDefinition typeDefinition) throws ConceptualException
  {
    if (typeDefinition.getInheritanceLinearisation() != null)
    {
      return typeDefinition.getInheritanceLinearisation();
    }

    List<TypeDefinition> parents = new LinkedList<TypeDefinition>();
    if (typeDefinition instanceof ClassDefinition)
    {
      ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
      if (classDefinition.getSuperClassDefinition() != null)
      {
        parents.add(classDefinition.getSuperClassDefinition());
      }
      if (classDefinition.getSuperInterfaceDefinitions() != null)
      {
        for (InterfaceDefinition interfaceDefinition : classDefinition.getSuperInterfaceDefinitions())
        {
          parents.add(interfaceDefinition);
        }
      }
    }
    if (typeDefinition instanceof InterfaceDefinition)
    {
      InterfaceDefinition interfaceDefinition = (InterfaceDefinition) typeDefinition;
      if (interfaceDefinition.getSuperInterfaceDefinitions() != null)
      {
        for (InterfaceDefinition superInterface : interfaceDefinition.getSuperInterfaceDefinitions())
        {
          parents.add(superInterface);
        }
      }
    }

    List<List<TypeDefinition>> precedenceLists = new LinkedList<List<TypeDefinition>>();
    for (TypeDefinition parent : parents)
    {
      TypeDefinition[] linearisation = findInheritanceLinearisation(parent);
      List<TypeDefinition> precedenceList = new LinkedList<TypeDefinition>();
      for (TypeDefinition t : linearisation)
      {
        precedenceList.add(t);
      }
      precedenceLists.add(precedenceList);
    }
    // add the local precedence order for this type
    precedenceLists.add(parents);

    List<TypeDefinition> result = new LinkedList<TypeDefinition>();
    result.add(typeDefinition);
    while (true)
    {
      // remove empty lists
      Iterator<List<TypeDefinition>> it = precedenceLists.iterator();
      while (it.hasNext())
      {
        if (it.next().isEmpty())
        {
          it.remove();
        }
      }
      if (precedenceLists.isEmpty())
      {
        break;
      }

      TypeDefinition next = null;
      for (List<TypeDefinition> candidateList : precedenceLists)
      {
        TypeDefinition candidate = candidateList.get(0);
        for (List<TypeDefinition> otherList : precedenceLists)
        {
          // check the tail of this otherList (this depends on the fact that the lists have no duplicate types)
          if (otherList.get(0) != candidate && otherList.contains(candidate))
          {
            candidate = null;
            break;
          }
        }
        if (candidate != null)
        {
          next = candidate;
          break;
        }
      }

      if (next == null)
      {
        StringBuffer buffer = new StringBuffer();
        Set<TypeDefinition> added = new HashSet<TypeDefinition>();
        for (List<TypeDefinition> currentList : precedenceLists)
        {
          TypeDefinition candidate = currentList.get(0);
          if (added.contains(candidate))
          {
            continue;
          }
          added.add(candidate);
          buffer.append("\n");
          buffer.append(candidate.getQualifiedName().toString());
        }
        throw new ConceptualException("Cannot find a good linearisation for the inheritance hierarchy of " + typeDefinition.getQualifiedName() + ": cannot decide which of the following super-types should be preferred:" + buffer, typeDefinition.getLexicalPhrase());
      }
      result.add(next);
      for (List<TypeDefinition> currentList : precedenceLists)
      {
        currentList.remove(next);
      }
    }

    TypeDefinition[] linearisation = result.toArray(new TypeDefinition[result.size()]);
    typeDefinition.setInheritanceLinearisation(linearisation);
    return linearisation;
  }

  /**
   * Checks the inherited members of the specified TypeDefinition for problems.
   * @param typeDefinition - the TypeDefinition to check all of the inherited members of
   * @throws ConceptualException - if there is a conceptual problem with the specified TypeDefinition
   */
  public static void checkInheritedMembers(TypeDefinition typeDefinition) throws ConceptualException
  {
    // generate the inheritance linearisation for this TypeDefinition
    // it will be used during this pass and some others after it, all the way to code generation
    TypeDefinition[] linearisation = findInheritanceLinearisation(typeDefinition);

    Set<Method> inheritedNonStaticMethods = new HashSet<Method>();
    for (Method method : ObjectType.OBJECT_METHODS)
    {
      if (!method.isStatic())
      {
        inheritedNonStaticMethods.add(method);
      }
    }
    if (typeDefinition instanceof ClassDefinition)
    {
      ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
      ClassDefinition superClass = classDefinition.getSuperClassDefinition();

      if (superClass != null && superClass.isImmutable() && !classDefinition.isImmutable())
      {
        throw new ConceptualException("Cannot define a non-immutable class to be a subclass of an immutable class", classDefinition.getLexicalPhrase());
      }

      for (TypeDefinition currentType : linearisation)
      {
        if (currentType != typeDefinition)
        {
          for (Method m : currentType.getNonStaticMethods())
          {
            inheritedNonStaticMethods.add(m);
          }
        }
      }
    }

    for (Method method : typeDefinition.getAllMethods())
    {
      checkMethod(method);
    }

    for (Method inheritedMethod : inheritedNonStaticMethods)
    {
      boolean overridden = false;
      for (Method method : typeDefinition.getMethodsByName(inheritedMethod.getName()))
      {
        if (inheritedMethod.getDisambiguator().matches(method.getDisambiguator()))
        {
          checkMethodOverride(inheritedMethod, method);
          overridden = true;
        }
      }
      if (!typeDefinition.isAbstract() && inheritedMethod.isAbstract() && !overridden)
      {
        // we have not implemented this abstract method, but it could have been implemented higher up the inheritance hierarchy, so check for that
        Method implementation = null;
        for (TypeDefinition currentSuperType : linearisation)
        {
          // ignore the current definition, as it has already been checked
          // also ignore any definitions which are parents of the declaring type of the method
          if (currentSuperType == typeDefinition ||
              (inheritedMethod.getContainingTypeDefinition() != null && isSuperType(currentSuperType, inheritedMethod.getContainingTypeDefinition())))
          {
            continue;
          }
          Set<Method> possibleMatches = currentSuperType.getMethodsByName(inheritedMethod.getName());
          for (Method m : possibleMatches)
          {
            if (m.getDisambiguator().matches(inheritedMethod.getDisambiguator()))
            {
              // this method matches the disambiguator, so check that it is an implementation of a method (i.e. it is not abstract)
              if (!m.isAbstract())
              {
                // this must be the most-derived implementation of this method
                implementation = m;
                break;
              }
            }
          }
          if (implementation != null)
          {
            break;
          }
        }

        if (implementation == null)
        {
          String declarationType = inheritedMethod.getContainingTypeDefinition() == null ? "object" : inheritedMethod.getContainingTypeDefinition().getQualifiedName().toString();
          throw new ConceptualException(typeDefinition.getName() + " does not implement the abstract method: " + buildMethodDisambiguatorString(inheritedMethod) + " (from type: " + declarationType + ")", typeDefinition.getLexicalPhrase());
        }
        try
        {
          checkMethodOverride(inheritedMethod, implementation);
        }
        catch (ConceptualException e)
        {
          String declarationType = inheritedMethod.getContainingTypeDefinition() == null ? "object" : inheritedMethod.getContainingTypeDefinition().getQualifiedName().toString();
          String implementationType = implementation.getContainingTypeDefinition() == null ? "object" : implementation.getContainingTypeDefinition().getQualifiedName().toString();
          throw new ConceptualException(typeDefinition.getName() + " does not implement the abstract method: " + buildMethodDisambiguatorString(inheritedMethod) + " (from type: " + declarationType + ")",
                                        typeDefinition.getLexicalPhrase(),
                                        new ConceptualException("Note: It is implemented in " + implementationType + " but that method is incompatible because: " + e.getMessage(), e.getLexicalPhrase()));
        }
      }
    }
  }

  private static void checkMethod(Method method) throws ConceptualException
  {
    if (method.isAbstract())
    {
      if (method.isStatic())
      {
        throw new ConceptualException("A static method cannot be abstract", method.getLexicalPhrase());
      }
      if (method.getNativeName() != null)
      {
        throw new ConceptualException("An abstract method cannot be native", method.getLexicalPhrase());
      }
      if (method.getBlock() != null)
      {
        throw new ConceptualException("An abstract method cannot have a body", method.getLexicalPhrase());
      }
      if (method.getContainingTypeDefinition() instanceof CompoundDefinition)
      {
        throw new ConceptualException("A compound type cannot contain abstract methods", method.getLexicalPhrase());
      }
      if (!method.getContainingTypeDefinition().isAbstract())
      {
        throw new ConceptualException("Abstract methods can only be declared in abstract classes", method.getLexicalPhrase());
      }
    }
  }

  private static void checkMethodOverride(Method overriddenMethod, Method method) throws ConceptualException
  {
    if (overriddenMethod.isImmutable() && !method.isImmutable())
    {
      throw new ConceptualException("A non-immutable method cannot override an immutable method", method.getLexicalPhrase());
    }
  }

  /**
   * Checks whether parent is a super-type of child.
   * @param parent - the parent TypeDefinition
   * @param child - the child TypeDefinition
   * @return true if parent is a super-type of child, false otherwise
   * @throws ConceptualException - if the inheritance linearisation for child is not well defined
   */
  private static boolean isSuperType(TypeDefinition parent, TypeDefinition child) throws ConceptualException
  {
    for (TypeDefinition test : findInheritanceLinearisation(child))
    {
      if (test == parent)
      {
        return true;
      }
    }
    return false;
  }

  private static String buildMethodDisambiguatorString(Method method)
  {
    Parameter[] parameters = method.getParameters();
    StringBuffer buffer = new StringBuffer();
    buffer.append(method.getReturnType());
    buffer.append(' ');
    buffer.append(method.getName());
    buffer.append('(');
    for (int i = 0; i < parameters.length; ++i)
    {
      buffer.append(parameters[i].getType());
      if (i != parameters.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(')');
    return buffer.toString();
  }
}
