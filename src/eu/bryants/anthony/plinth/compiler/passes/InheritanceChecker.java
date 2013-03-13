package eu.bryants.anthony.plinth.compiler.passes;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
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

    Set<Member> inheritedMembers = new HashSet<Member>();
    for (Method method : ObjectType.OBJECT_METHODS)
    {
      if (!method.isStatic())
      {
        inheritedMembers.add(method);
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

      // TODO: should super-interfaces be checked for the same immutability constraint as super-classes?
    }
    else if (typeDefinition instanceof InterfaceDefinition)
    {
      // TODO: should super-interfaces of interfaces be checked for the same immutability constraint as super-classes?
    }

    for (TypeDefinition currentType : linearisation)
    {
      if (currentType != typeDefinition)
      {
        for (Method m : currentType.getAllMethods())
        {
          if (!m.isStatic())
          {
            inheritedMembers.add(m);
          }
        }
        for (Property p : currentType.getProperties())
        {
          if (!p.isStatic())
          {
            inheritedMembers.add(p);
          }
        }
      }
    }

    for (Property property : typeDefinition.getProperties())
    {
      checkProperty(property);
    }

    for (Method method : typeDefinition.getAllMethods())
    {
      checkMethod(method);
    }

    for (Member inheritedMember : inheritedMembers)
    {
      boolean inheritedIsAbstract;
      TypeDefinition inheritedTypeDefinition;
      if (inheritedMember instanceof Method)
      {
        Method inheritedMethod = (Method) inheritedMember;
        inheritedIsAbstract = inheritedMethod.isAbstract();
        inheritedTypeDefinition = inheritedMethod.getContainingTypeDefinition();
      }
      else // inheritedMember instanceof Property
      {
        Property inheritedProperty = (Property) inheritedMember;
        inheritedIsAbstract = inheritedProperty.isAbstract();
        inheritedTypeDefinition = inheritedProperty.getContainingTypeDefinition();
      }

      // find the implementation of this inherited member
      Member implementation = null;
      Set<TypeDefinition> ignoredSuperTypeDefinitions = new HashSet<TypeDefinition>();
      if (inheritedTypeDefinition != null)
      {
        ignoredSuperTypeDefinitions.add(inheritedTypeDefinition);
      }

      typeSearchLoop:
      for (TypeDefinition currentType : linearisation)
      {
        for (TypeDefinition ignored : ignoredSuperTypeDefinitions)
        {
          // if the current type is a super-type of one we have already processed and ignored, then ignore it too
          // (here, we depend on all super-types being processed after their sub-types, which is true due to the nature of the C3 linearisation)
          // we ignore any types which declare this member as abstract
          if (isSuperType(currentType, ignored))
          {
            continue typeSearchLoop;
          }
        }

        if (inheritedMember instanceof Method)
        {
          Method inheritedMethod = (Method) inheritedMember;
          Set<Method> possibleMatches = currentType.getMethodsByName(inheritedMethod.getName());
          for (Method m : possibleMatches)
          {
            if (m.getDisambiguator().matches(inheritedMethod.getDisambiguator()))
            {
              try
              {
                checkMethodOverride(inheritedMethod, m);
              }
              catch (ConceptualException e)
              {
                if (inheritedTypeDefinition == null || isSuperType(inheritedTypeDefinition, currentType))
                {
                  // the existing error message is good enough, because the current type is a sub-type of the inherited type
                  if (currentType != typeDefinition)
                  {
                    // this is an error, but it does not involve the class we are checking, so wait until we check currentType to report it, or we will get duplicate errors
                    continue typeSearchLoop;
                  }
                  throw e;
                }
                // add a new message to point out that the overriding is because this TypeDefinition inherits from two different types
                throw new ConceptualException("Incompatible method: " + buildMethodDisambiguatorString(inheritedMethod) + " is inherited incompatibly from both " + inheritedTypeDefinition.getQualifiedName() + " and " + currentType.getQualifiedName() + " - the problem is:",
                                              typeDefinition.getLexicalPhrase(), e);
              }
              // this method matches the disambiguator, so check that it is an implementation of a method (i.e. it is not abstract)
              if (m.isAbstract())
              {
                // if this method is abstract, ignore this type and all of its super-types
                ignoredSuperTypeDefinitions.add(currentType);
                continue typeSearchLoop;
              }
              // this must be the most-derived implementation of this method
              if (implementation == null)
              {
                implementation = m;
                break;
              }
            }
          }
        }
        else // inheritedMember instanceof Property
        {
          Property inheritedProperty = (Property) inheritedMember;
          Property property = currentType.getProperty(inheritedProperty.getName());
          if (property != null && !property.isStatic())
          {
            try
            {
              checkPropertyOverride(inheritedProperty, property);
            }
            catch (ConceptualException e)
            {
              if (inheritedTypeDefinition == null || isSuperType(inheritedTypeDefinition, currentType))
              {
                // the existing error message is good enough, because the current type is a sub-type of the inherited type
                if (currentType != typeDefinition)
                {
                  // this is an error, but it does not involve the class we are checking, so wait until we check currentType to report it, or we will get duplicate errors
                  continue;
                }
                throw e;
              }
              // add a new message to point out that the overriding is because this TypeDefinition inherits from two different types
              throw new ConceptualException("Incompatible property: '" + inheritedProperty.getName() + "' is inherited incompatibly from both " + inheritedTypeDefinition.getQualifiedName() + " and " + currentType.getQualifiedName() + " - the problem is:",
                                            typeDefinition.getLexicalPhrase(), e);
            }
            if (property.isAbstract())
            {
              // if this property is abstract, ignore this type and all of its super-types
              ignoredSuperTypeDefinitions.add(currentType);
              continue;
            }
            if (implementation == null)
            {
              implementation = property;
            }
          }
        }
      }

      if (!typeDefinition.isAbstract() && inheritedIsAbstract && implementation == null)
      {
        String memberType = (inheritedMember instanceof Method) ? "method" : "property";
        String declarationType = inheritedTypeDefinition == null ? "object" : inheritedTypeDefinition.getQualifiedName().toString();
        String disambiguator;
        if (inheritedMember instanceof Method)
        {
          disambiguator = buildMethodDisambiguatorString((Method) inheritedMember);
        }
        else
        {
          disambiguator = ((Property) inheritedMember).getName();
        }
        throw new ConceptualException(typeDefinition.getName() + " does not implement the abstract " + memberType + ": " + disambiguator + " (from type: " + declarationType + ")", typeDefinition.getLexicalPhrase());
      }
    }
  }

  private static void checkProperty(Property property) throws ConceptualException
  {
    if (property.isAbstract())
    {
      if (property.isStatic())
      {
        throw new ConceptualException("A static property cannot be abstract", property.getLexicalPhrase());
      }
      if (property.getGetterBlock() != null || property.getSetterBlock() != null || property.getConstructorBlock() != null)
      {
        throw new ConceptualException("An abstract property cannot define a getter, a setter, or a constructor", property.getLexicalPhrase());
      }
      if (property.getContainingTypeDefinition() instanceof CompoundDefinition)
      {
        throw new ConceptualException("A compound type cannot contain abstract properties", property.getLexicalPhrase());
      }
      if (!property.getContainingTypeDefinition().isAbstract())
      {
        throw new ConceptualException("Abstract properties can only be declared in abstract classes or interfaces", property.getLexicalPhrase());
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
        throw new ConceptualException("Abstract methods can only be declared in abstract classes or interfaces", method.getLexicalPhrase());
      }
    }
  }

  private static void checkPropertyOverride(Property overriddenProperty, Property property) throws ConceptualException
  {
    if (!overriddenProperty.getType().isEquivalent(property.getType()))
    {
      throw new ConceptualException("A property must always have the same type as a property it overrides, in this case: " + overriddenProperty.getType(), property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
    if (overriddenProperty.isFinal() != property.isFinal())
    {
      throw new ConceptualException("A property must always have the same finality as a property it overrides", property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
    if (overriddenProperty.hasConstructor() && !property.hasConstructor())
    {
      throw new ConceptualException("An overriding property must have a constructor if the property it overrides has a constructor", property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
    if (!overriddenProperty.hasConstructor() && property.hasConstructor())
    {
      throw new ConceptualException("An overriding property must not have a constructor if the property it overrides does not have a constructor", property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
    if (overriddenProperty.isGetterImmutable() && !property.isGetterImmutable())
    {
      throw new ConceptualException("A non-immutable property getter cannot override an immutable property getter", property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
    if (overriddenProperty.isSetterImmutable() && !property.isSetterImmutable())
    {
      throw new ConceptualException("A non-immutable property setter cannot override an immutable property setter", property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
    if (overriddenProperty.isConstructorImmutable() && !property.isConstructorImmutable())
    {
      throw new ConceptualException("A non-immutable property constructor cannot override an immutable property constructor", property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
  }

  private static void checkMethodOverride(Method overriddenMethod, Method method) throws ConceptualException
  {
    if (overriddenMethod.isImmutable() && !method.isImmutable())
    {
      throw new ConceptualException("A non-immutable method cannot override an immutable method", method.getLexicalPhrase(),
                                    overriddenMethod.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenMethod.getLexicalPhrase()) : null);
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
