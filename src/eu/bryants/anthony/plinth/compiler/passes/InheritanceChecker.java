package eu.bryants.anthony.plinth.compiler.passes;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompilationUnit;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Method;
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

    for (Method inheritedMethod : inheritedNonStaticMethods)
    {
      for (Method method : typeDefinition.getMethodsByName(inheritedMethod.getName()))
      {
        if (inheritedMethod.getDisambiguator().matches(method.getDisambiguator()))
        {
          checkMethodOverride(inheritedMethod, method);
        }
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
}
