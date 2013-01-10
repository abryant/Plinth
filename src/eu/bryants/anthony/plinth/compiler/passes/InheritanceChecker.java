package eu.bryants.anthony.plinth.compiler.passes;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompilationUnit;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
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
   * Checks the inherited members of the specified TypeDefinition for problems.
   * @param typeDefinition - the TypeDefinition to check all of the inherited members of
   * @throws ConceptualException - if there is a conceptual problem with the specified TypeDefinition
   */
  public static void checkInheritedMembers(TypeDefinition typeDefinition) throws ConceptualException
  {
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
      ClassDefinition superClass = ((ClassDefinition) typeDefinition).getSuperClassDefinition();

      if (superClass != null && superClass.isImmutable() && !typeDefinition.isImmutable())
      {
        throw new ConceptualException("Cannot define a non-immutable class to be a subclass of an immutable class", typeDefinition.getLexicalPhrase());
      }

      while (superClass != null)
      {
        for (Method m : superClass.getNonStaticMethods())
        {
          inheritedNonStaticMethods.add(m);
        }
        superClass = superClass.getSuperClassDefinition();
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
        // we have not implemented this abstract method, but it could have been implemented higher up the class hierarchy, so check for that
        Method implementation = null;
        if (typeDefinition instanceof ClassDefinition)
        {
          ClassDefinition superClass = ((ClassDefinition) typeDefinition).getSuperClassDefinition();
          while (implementation == null && superClass != null && superClass != inheritedMethod.getContainingTypeDefinition())
          {
            Set<Method> possibleMatches = superClass.getMethodsByName(inheritedMethod.getName());
            for (Method m : possibleMatches)
            {
              if (m.getDisambiguator().matches(inheritedMethod.getDisambiguator()))
              {
                // this method matches the disambiguator, so check that it is an implementation of a method (i.e. has a body or is a native down-call)
                if (m.getBlock() != null || m.getNativeName() != null)
                {
                  // this must be the most-derived implementation of this method
                  implementation = m;
                  break;
                }
              }
            }
          }
          if (superClass == null && implementation == null)
          {
            // we reached the top of the inheritance hierarchy (i.e. we got all the way up to object without reaching the inheritedMethod's TypeDefinition, so it must be defined in an interface)
            // and we still haven't found anything, so check the object methods
            for (Method m : ObjectType.OBJECT_METHODS)
            {
              if (m.getDisambiguator().matches(inheritedMethod.getDisambiguator()))
              {
                implementation = m;
                break;
              }
            }
          }
        }
        if (implementation != null)
        {
          try
          {
            checkMethodOverride(inheritedMethod, implementation);
          }
          catch (ConceptualException e)
          {
            String implementationType = implementation.getContainingTypeDefinition() == null ? "object" : implementation.getContainingTypeDefinition().getQualifiedName().toString();
            throw new ConceptualException(typeDefinition.getName() + " does not implement the abstract method: " + buildMethodDisambiguatorString(inheritedMethod),
                                          typeDefinition.getLexicalPhrase(),
                                          new ConceptualException("Note: It is implemented in " + implementationType + " but that method is incompatible because: " + e.getMessage(), e.getLexicalPhrase()));
          }
        }
        if (implementation == null)
        {
          throw new ConceptualException(typeDefinition.getName() + " does not implement the abstract method: " + buildMethodDisambiguatorString(inheritedMethod), typeDefinition.getLexicalPhrase());
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
