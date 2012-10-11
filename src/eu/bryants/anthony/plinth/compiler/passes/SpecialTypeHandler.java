package eu.bryants.anthony.plinth.compiler.passes;

import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.compiler.ConceptualException;

/*
 * Created on 8 Oct 2012
 */

/**
 * @author Anthony Bryant
 */
public class SpecialTypeHandler
{
  public static final NamedType STRING_TYPE = new NamedType(false, new QName("string", null), null);
  public static Constructor stringArrayConstructor;
  public static Constructor stringConcatenationConstructor;

  public static final String MAIN_METHOD_NAME = "main";

  /**
   * Verifies that all of the special types (types that the compiler has special cases for)
   * conform to the assumptions that the compiler makes about them.
   * This method must be run after the resolver, so that all special types have been resolved
   * (if they are used anywhere) before this is run.
   * @throws ConceptualException - if there is a problem with one of the special types
   */
  public static void verifySpecialTypes() throws ConceptualException
  {
    verifyStringType();
  }

  private static void verifyStringType() throws ConceptualException
  {
    TypeDefinition typeDefinition = STRING_TYPE.getResolvedTypeDefinition();
    if (typeDefinition == null)
    {
      // nothing used the string type, so we don't need to check it
      return;
    }
    if (!(typeDefinition instanceof CompoundDefinition))
    {
      throw new ConceptualException("The string type must be a compound definition!", typeDefinition.getLexicalPhrase());
    }
    Type arrayType = new ArrayType(false, new PrimitiveType(false, PrimitiveTypeType.UBYTE, null), null);
    for (Constructor constructor : typeDefinition.getConstructors())
    {
      Parameter[] parameters = constructor.getParameters();
      if (parameters.length == 1 && parameters[0].getType().isEquivalent(arrayType))
      {
        stringArrayConstructor = constructor;
      }
      if (parameters.length == 2 && parameters[0].getType().isEquivalent(STRING_TYPE) && parameters[1].getType().isEquivalent(STRING_TYPE))
      {
        stringConcatenationConstructor = constructor;
      }
    }
    if (stringArrayConstructor == null)
    {
      throw new ConceptualException("The string type must have a constructor which takes a single " + arrayType + " argument", typeDefinition.getLexicalPhrase());
    }
    if (stringConcatenationConstructor == null)
    {
      throw new ConceptualException("The string type must have a constructor which takes two " + STRING_TYPE + " arguments", typeDefinition.getLexicalPhrase());
    }
  }

  /**
   * Checks that the specified TypeDefinition has a valid main method.
   * @param typeDefinition - the TypeDefinition to check
   * @throws ConceptualException - if a valid main() method could not be found
   */
  public static void checkMainMethod(TypeDefinition typeDefinition) throws ConceptualException
  {
    Type argsType = new ArrayType(false, STRING_TYPE, null);
    Method mainMethod = null;
    for (Method method : typeDefinition.getAllMethods())
    {
      if (method.isStatic() && method.getName().equals(MAIN_METHOD_NAME) && method.getReturnType().isEquivalent(new PrimitiveType(false, PrimitiveTypeType.UINT, null)))
      {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 1 && parameters[0].getType().isEquivalent(argsType))
        {
          mainMethod = method;
          break;
        }
      }
    }
    if (mainMethod == null)
    {
      throw new ConceptualException("Could not find main method in " + typeDefinition.getQualifiedName(), typeDefinition.getLexicalPhrase());
    }
  }

}
