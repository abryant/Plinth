package eu.bryants.anthony.plinth.compiler.passes;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
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
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.compiler.ConceptualException;

/*
 * Created on 8 Oct 2012
 */

/**
 * @author Anthony Bryant
 */
public class SpecialTypeHandler
{
  public static final NamedType STRING_TYPE = new NamedType(false, false, new QName("string", null), null, null);
  private static final String STRING_VALUEOF_NAME = "valueOf";
  public static Constructor stringArrayConstructor;
  public static Constructor stringConcatenationConstructor;
  public static Constructor stringArrayConcatenationConstructor;
  public static Method stringValueOfBoolean;
  public static Method stringValueOfLong;
  public static Method stringValueOfUlong;
  public static Method stringValueOfLongRadix;
  public static Method stringValueOfUlongRadix;
  public static Method stringValueOfFloat;
  public static Method stringValueOfDouble;

  public static final NamedType THROWABLE_TYPE = new NamedType(false, false, new QName("Throwable", null), null, null);

  public static final NamedType CAST_ERROR_TYPE = new NamedType(false, false, new QName("CastError", null), null, null);
  public static Constructor castErrorTypesReasonConstructor;

  public static final NamedType INDEX_ERROR_TYPE = new NamedType(false, false, new QName("IndexError", null), null, null);
  public static Constructor indexErrorIndexSizeConstructor;

  // iteratorType has a generic type parameter, so we specify uint here, and then once the TypeDefinition is resolved we will swap the NamedType out for one where the type argument points back to the TypeParameter
  public static NamedType iteratorType = new NamedType(false, false, new QName("Iterator", null), new Type[] {new PrimitiveType(false, PrimitiveTypeType.UINT, null)}, null);
  private static final String ITERATOR_HAS_NEXT_NAME = "hasNext";
  private static final String ITERATOR_NEXT_NAME = "next";
  public static Method iteratorHasNextMethod;
  public static Method iteratorNextMethod;

  // iterableType has a generic type parameter, so we specify uint here, and then once the TypeDefinition is resolved we will swap the NamedType out for one where the type argument points back to the TypeParameter
  public static NamedType iterableType = new NamedType(false, false, new QName("Iterable", null), new Type[] {new PrimitiveType(false, PrimitiveTypeType.UINT, null)}, null);
  private static final String ITERABLE_ITERATOR_NAME = "iterator";
  public static Method iterableIteratorMethod;

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
    TypeChecker.checkType(STRING_TYPE, null, true);
    verifyStringType();

    TypeChecker.checkType(THROWABLE_TYPE, null, true);
    verifyThrowableType();

    TypeChecker.checkType(CAST_ERROR_TYPE, null, true);
    verifyCastErrorType();

    TypeChecker.checkType(INDEX_ERROR_TYPE, null, true);
    verifyIndexErrorType();

    TypeChecker.checkType(iteratorType, iteratorType.getResolvedTypeDefinition(), false);
    verifyIteratorType();

    TypeChecker.checkType(iterableType, iterableType.getResolvedTypeDefinition(), false);
    verifyIterableType();
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
    Type arrayType = new ArrayType(false, true, new PrimitiveType(false, PrimitiveTypeType.UBYTE, null), null);
    Type stringArrayType = new ArrayType(false, false, STRING_TYPE, null);
    for (Constructor constructor : typeDefinition.getUniqueConstructors())
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
      if (parameters.length == 1 && parameters[0].getType().isEquivalent(stringArrayType))
      {
        stringArrayConcatenationConstructor = constructor;
      }
    }
    for (Method method : typeDefinition.getMethodsByName(STRING_VALUEOF_NAME))
    {
      if (!method.getReturnType().isEquivalent(STRING_TYPE) || !method.isStatic())
      {
        continue;
      }
      Parameter[] parameters = method.getParameters();
      if (parameters.length == 1 && parameters[0].getType().isEquivalent(new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null)))
      {
        stringValueOfBoolean = method;
      }
      if (parameters.length == 1 && parameters[0].getType().isEquivalent(new PrimitiveType(false, PrimitiveTypeType.LONG, null)))
      {
        stringValueOfLong = method;
      }
      if (parameters.length == 1 && parameters[0].getType().isEquivalent(new PrimitiveType(false, PrimitiveTypeType.ULONG, null)))
      {
        stringValueOfUlong = method;
      }
      if (parameters.length == 2 && parameters[0].getType().isEquivalent(new PrimitiveType(false, PrimitiveTypeType.LONG, null)) &&
                                    parameters[1].getType().isEquivalent(new PrimitiveType(false, PrimitiveTypeType.UINT, null)))
      {
        stringValueOfLongRadix = method;
      }
      if (parameters.length == 2 && parameters[0].getType().isEquivalent(new PrimitiveType(false, PrimitiveTypeType.ULONG, null)) &&
                                    parameters[1].getType().isEquivalent(new PrimitiveType(false, PrimitiveTypeType.UINT, null)))
      {
        stringValueOfUlongRadix = method;
      }
      if (parameters.length == 1 && parameters[0].getType().isEquivalent(new PrimitiveType(false, PrimitiveTypeType.FLOAT, null)))
      {
        stringValueOfFloat = method;
      }
      if (parameters.length == 1 && parameters[0].getType().isEquivalent(new PrimitiveType(false, PrimitiveTypeType.DOUBLE, null)))
      {
        stringValueOfDouble = method;
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
    if (stringArrayConcatenationConstructor == null)
    {
      throw new ConceptualException("The string type must have a constructor which takes a single " + stringArrayType + " argument", typeDefinition.getLexicalPhrase());
    }
    if (stringValueOfBoolean == null ||
        stringValueOfLong    == null || stringValueOfLongRadix  == null ||
        stringValueOfUlong   == null || stringValueOfUlongRadix == null ||
        stringValueOfFloat   == null || stringValueOfDouble     == null)
    {
      throw new ConceptualException("The string type must have the correct static " + STRING_VALUEOF_NAME + " methods", typeDefinition.getLexicalPhrase());
    }
  }

  private static void verifyThrowableType() throws ConceptualException
  {
    TypeDefinition typeDefinition = THROWABLE_TYPE.getResolvedTypeDefinition();
    if (typeDefinition == null || !(typeDefinition instanceof InterfaceDefinition))
    {
      throw new ConceptualException("The Throwable type must be defined as an interface!", null);
    }
  }

  private static void verifyCastErrorType() throws ConceptualException
  {
    TypeDefinition typeDefinition = CAST_ERROR_TYPE.getResolvedTypeDefinition();
    if (typeDefinition == null || !(typeDefinition instanceof ClassDefinition))
    {
      throw new ConceptualException("The CastError type must be defined as a class!", null);
    }

    boolean isThrowable = false;
    for (NamedType t : typeDefinition.getInheritanceLinearisation())
    {
      if (t.isEquivalent(THROWABLE_TYPE))
      {
        isThrowable = true;
        break;
      }
    }
    if (!isThrowable)
    {
      throw new ConceptualException("The CastError type must inherit from Throwable", typeDefinition.getLexicalPhrase());
    }

    Type nullableStringType = Type.findTypeWithNullability(STRING_TYPE, true);
    for (Constructor constructor : typeDefinition.getUniqueConstructors())
    {
      Parameter[] parameters = constructor.getParameters();
      if (parameters.length == 3 && parameters[0].getType().isEquivalent(STRING_TYPE) &&
                                    parameters[1].getType().isEquivalent(STRING_TYPE) &&
                                    parameters[2].getType().isEquivalent(nullableStringType))
      {
        castErrorTypesReasonConstructor = constructor;
        break;
      }
    }
    if (castErrorTypesReasonConstructor == null)
    {
      throw new ConceptualException("The CastError type must have a constructor which takes two type strings and a nullable reason string as arguments", typeDefinition.getLexicalPhrase());
    }
  }

  private static void verifyIndexErrorType() throws ConceptualException
  {
    TypeDefinition typeDefinition = INDEX_ERROR_TYPE.getResolvedTypeDefinition();
    if (typeDefinition == null || !(typeDefinition instanceof ClassDefinition))
    {
      throw new ConceptualException("The IndexError type must be defined as a class!", null);
    }

    boolean isThrowable = false;
    for (NamedType t : typeDefinition.getInheritanceLinearisation())
    {
      if (t.isEquivalent(THROWABLE_TYPE))
      {
        isThrowable = true;
        break;
      }
    }
    if (!isThrowable)
    {
      throw new ConceptualException("The IndexError type must inherit from Throwable", typeDefinition.getLexicalPhrase());
    }

    Type uintType = new PrimitiveType(false, PrimitiveTypeType.UINT, null);
    for (Constructor constructor : typeDefinition.getUniqueConstructors())
    {
      Parameter[] parameters = constructor.getParameters();
      if (parameters.length == 2 && parameters[0].getType().isEquivalent(uintType) &&
                                    parameters[1].getType().isEquivalent(uintType))
      {
        indexErrorIndexSizeConstructor = constructor;
        break;
      }
    }
    if (indexErrorIndexSizeConstructor == null)
    {
      throw new ConceptualException("The IndexError type must have a constructor which takes an index and a size (both uints) as arguments", typeDefinition.getLexicalPhrase());
    }
  }

  private static void verifyIteratorType() throws ConceptualException
  {
    TypeDefinition typeDefinition = iteratorType.getResolvedTypeDefinition();
    if (typeDefinition == null || !(typeDefinition instanceof InterfaceDefinition))
    {
      throw new ConceptualException("The Iterator type must be defined as an interface!", null);
    }

    TypeParameter[] typeParameters = typeDefinition.getTypeParameters();
    if (typeParameters.length != 1 || typeParameters[0].getSuperTypes().length != 0 || typeParameters[0].getSubTypes().length != 0)
    {
      throw new ConceptualException("The Iterator type must have a single type parameter with no restrictions on acceptable types", null);
    }

    PrimitiveType booleanType = new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null);
    for (Method method : typeDefinition.getMethodsByName(ITERATOR_HAS_NEXT_NAME))
    {
      if (!method.isStatic() && method.isImmutable() &&
          method.getReturnType().isEquivalent(booleanType) &&
          method.getParameters().length == 0 &&
          method.getCheckedThrownTypes().length == 0)
      {
        iteratorHasNextMethod = method;
        break;
      }
    }
    if (iteratorHasNextMethod == null)
    {
      throw new ConceptualException("The Iterator type must have a non-static immutable method with the signature: boolean hasNext()", null);
    }

    NamedType typeParameterType = new NamedType(false, false, false, typeParameters[0]);
    for (Method method : typeDefinition.getMethodsByName(ITERATOR_NEXT_NAME))
    {
      if (!method.isStatic() && !method.isImmutable() &&
          method.getReturnType().isEquivalent(typeParameterType) &&
          method.getParameters().length == 0 &&
          method.getCheckedThrownTypes().length == 0)
      {
        iteratorNextMethod = method;
        break;
      }
    }
    if (iteratorNextMethod == null)
    {
      throw new ConceptualException("The Iterator type must have a non-static non-immutable method with the signature: T next() (where T is the Iterator's type parameter)", null);
    }
  }

  private static void verifyIterableType() throws ConceptualException
  {
    TypeDefinition typeDefinition = iterableType.getResolvedTypeDefinition();
    if (typeDefinition == null || !(typeDefinition instanceof InterfaceDefinition))
    {
      throw new ConceptualException("The Iterable type must be defined as an interface!", null);
    }

    TypeParameter[] typeParameters = typeDefinition.getTypeParameters();
    if (typeParameters.length != 1 || typeParameters[0].getSuperTypes().length != 0 || typeParameters[0].getSubTypes().length != 0)
    {
      throw new ConceptualException("The Iterable type must have a single type parameter with no restrictions on acceptable types", null);
    }

    NamedType typeParameterType = new NamedType(false, false, false, typeParameters[0]);
    NamedType iteratorResultType = new NamedType(false, false, iteratorType.getResolvedTypeDefinition(), new Type[] {typeParameterType});
    for (Method method : typeDefinition.getMethodsByName(ITERABLE_ITERATOR_NAME))
    {
      if (!method.isStatic() && !method.isImmutable() &&
          method.getReturnType().isEquivalent(iteratorResultType) &&
          method.getParameters().length == 0 &&
          method.getCheckedThrownTypes().length == 0)
      {
        iterableIteratorMethod = method;
        break;
      }
    }
    if (iterableIteratorMethod == null)
    {
      throw new ConceptualException("The Iterable type must have a non-static non-immutable method with the signature: Iterator<T> iterator() (where T is the Iterable's type parameter)", null);
    }
  }

  /**
   * Checks that the specified TypeDefinition has a valid main method.
   * @param typeDefinition - the TypeDefinition to check
   * @throws ConceptualException - if a valid main() method could not be found
   */
  public static void checkMainMethod(TypeDefinition typeDefinition) throws ConceptualException
  {
    Type argsType = new ArrayType(false, false, STRING_TYPE, null);
    Type immutableArgsType = new ArrayType(false, true, STRING_TYPE, null);
    Method mainMethod = null;
    for (Method method : typeDefinition.getAllMethods())
    {
      if (method.isStatic() && method.getName().equals(MAIN_METHOD_NAME) && method.getReturnType().isEquivalent(new PrimitiveType(false, PrimitiveTypeType.UINT, null)))
      {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 1 && (parameters[0].getType().isEquivalent(argsType) ||
                                       parameters[0].getType().isEquivalent(immutableArgsType)))
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
