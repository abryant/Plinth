package eu.bryants.anthony.plinth.compiler.passes.llvm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMBasicBlockRef;
import nativelib.llvm.LLVM.LLVMBuilderRef;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMTypeRef;
import nativelib.llvm.LLVM.LLVMValueRef;
import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.ArrayLengthMember;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod.BuiltinMethodType;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.metadata.GenericTypeSpecialiser;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunctionType;
import eu.bryants.anthony.plinth.ast.metadata.MemberVariable;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;
import eu.bryants.anthony.plinth.ast.metadata.PropertyReference;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.NullType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.ast.type.WildcardType;
import eu.bryants.anthony.plinth.compiler.passes.SpecialTypeHandler;

/*
 * Created on 23 Sep 2012
 */

/**
 * A class which helps a CodeGenerator convert the AST into bitcode by providing methods to convert types into their native representations, and methods to convert between these native types.
 * @author Anthony Bryant
 */
public class TypeHelper
{
  private static final String BASE_CHANGE_FUNCTION_PREFIX = "_base_change_o";
  private static final String PROXY_FUNCTION_PREFIX = "_PROXY_";
  private static final String REFERENCE_PROXY_FUNCTION_PREFIX = "_METHOD_PROXY_";
  private static final String PROXY_ARRAY_GETTER_FUNCTION_PREFIX = "_PROXY_ARRAY_GET_";
  private static final String PROXY_ARRAY_SETTER_FUNCTION_PREFIX = "_PROXY_ARRAY_SET_";
  private static final String ARRAY_GETTER_FUNCTION_PREFIX = "_ARRAY_GET_";
  private static final String ARRAY_SETTER_FUNCTION_PREFIX = "_ARRAY_SET_";

  private TypeDefinition typeDefinition;

  private CodeGenerator codeGenerator;
  private VirtualFunctionHandler virtualFunctionHandler;
  private RTTIHelper rttiHelper;

  private LLVMModuleRef module;

  private LLVMTypeRef opaqueType;
  private LLVMTypeRef objectType;
  private Map<String, LLVMTypeRef> nativeArrayTypes = new HashMap<String, LLVMTypeRef>();
  private Map<TypeDefinition, LLVMTypeRef> nativeNamedTypes = new HashMap<TypeDefinition, LLVMTypeRef>();

  /**
   * Creates a new TypeHelper to build type conversions with the specified builder.
   * @param typeDefinition - the TypeDefinition to generate proxy functions in the context of
   * @param codeGenerator - the CodeGenerator to use to generate any miscellaneous sections of code, such as null checks
   * @param virtualFunctionHandler - the VirtualFunctionHandler to handle building the types of virtual function tables
   * @param module - the LLVMModuleRef that this TypeHelper will build inside
   */
  public TypeHelper(TypeDefinition typeDefinition, CodeGenerator codeGenerator, VirtualFunctionHandler virtualFunctionHandler, LLVMModuleRef module)
  {
    this.typeDefinition = typeDefinition;
    this.codeGenerator = codeGenerator;
    this.virtualFunctionHandler = virtualFunctionHandler;
    this.module = module;
    opaqueType = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), "opaque");
  }

  /**
   * Initialises this TypeHelper, so that it has all of the references required to operate..
   * @param rttiHelper - the RTTIHelper to set
   */
  public void initialise(RTTIHelper rttiHelper)
  {
    this.rttiHelper = rttiHelper;
  }

  /**
   * @return an opaque pointer type
   */
  public LLVMTypeRef getOpaquePointer()
  {
    return LLVM.LLVMPointerType(opaqueType, 0);
  }

  /**
   * @return the result type of a landingpad instruction
   */
  public LLVMTypeRef getLandingPadType()
  {
    LLVMTypeRef[] subTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0), LLVM.LLVMInt32Type()};
    return LLVM.LLVMStructType(C.toNativePointerArray(subTypes, false, true), subTypes.length, false);
  }

  /**
   * Finds the standard representation for the specified type, to be used when passing parameters, or storing fields, etc.
   * @param type - the type to find the native type of
   * @return the standard native representation of the specified Type
   */
  public LLVMTypeRef findStandardType(Type type)
  {
    return findNativeType(type, false);
  }

  /**
   * Finds the temporary representation for the specified type, to be used when manipulating values inside a function.
   * @param type - the type to find the native type of
   * @return the temporary native representation of the specified Type
   */
  public LLVMTypeRef findTemporaryType(Type type)
  {
    return findNativeType(type, true);
  }

  /**
   * Finds the native representation of the specified type. The native representation can be of two forms: standard, and temporary.
   * These forms are used in different places, and can be converted between using other utility functions.
   * This method is not public, so to find a standard representation of a type, use findStandardType(Type); or to find a temporary representation, use findTemporaryType(Type).
   * @param type - the type to find the native representation of
   * @param temporary - true if the representation should be of the temporary form, or false if it should be in the standard form
   * @return the native type of the specified type
   */
  private LLVMTypeRef findNativeType(Type type, boolean temporary)
  {
    if (type instanceof PrimitiveType)
    {
      LLVMTypeRef nonNullableType;
      PrimitiveTypeType primitiveTypeType = ((PrimitiveType) type).getPrimitiveTypeType();
      if (primitiveTypeType == PrimitiveTypeType.DOUBLE)
      {
        nonNullableType = LLVM.LLVMDoubleType();
      }
      else if (primitiveTypeType == PrimitiveTypeType.FLOAT)
      {
        nonNullableType = LLVM.LLVMFloatType();
      }
      else
      {
        nonNullableType = LLVM.LLVMIntType(primitiveTypeType.getBitCount());
      }
      if (type.canBeNullable())
      {
        // tuple the non-nullable type with a boolean, so that we can tell whether or not the value is null
        LLVMTypeRef[] types = new LLVMTypeRef[] {LLVM.LLVMInt1Type(), nonNullableType};
        return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
      }
      return nonNullableType;
    }
    if (type instanceof ArrayType)
    {
      // strip any type parameters out of the base type, as we don't want them to change the mangled name of the array
      // the reason for this is that there should never be any difference between the native representation of a type, and its native representation after having its type parameters stripped
      Type baseType = stripTypeParameters(((ArrayType) type).getBaseType());
      ArrayType arrayType = new ArrayType(false, false, baseType, null);
      String mangledTypeName = arrayType.getMangledName();
      LLVMTypeRef existingType = nativeArrayTypes.get(mangledTypeName);
      if (existingType != null)
      {
        return LLVM.LLVMPointerType(existingType, 0);
      }
      LLVMTypeRef llvmArrayType = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), mangledTypeName);
      nativeArrayTypes.put(mangledTypeName, llvmArrayType);

      LLVMTypeRef rttiType = rttiHelper.getGenericRTTIType();
      LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(virtualFunctionHandler.getObjectVFTType(), 0);
      LLVMTypeRef lengthType = LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount());

      LLVMTypeRef[] getterFunctionParamTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(llvmArrayType, 0), lengthType};
      LLVMTypeRef getterFunctionType = LLVM.LLVMFunctionType(findTemporaryType(arrayType.getBaseType()), C.toNativePointerArray(getterFunctionParamTypes, false, true), getterFunctionParamTypes.length, false);
      LLVMTypeRef[] setterFunctionParamTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(llvmArrayType, 0), lengthType, findStandardType(arrayType.getBaseType())};
      LLVMTypeRef setterFunctionType = LLVM.LLVMFunctionType(LLVM.LLVMVoidType(), C.toNativePointerArray(setterFunctionParamTypes, false, true), setterFunctionParamTypes.length, false);

      LLVMTypeRef[] structureTypes = new LLVMTypeRef[] {rttiType, vftPointerType, lengthType, LLVM.LLVMPointerType(getterFunctionType, 0), LLVM.LLVMPointerType(setterFunctionType, 0)};
      LLVM.LLVMStructSetBody(llvmArrayType, C.toNativePointerArray(structureTypes, false, true), structureTypes.length, false);
      return LLVM.LLVMPointerType(llvmArrayType, 0);
    }
    if (type instanceof FunctionType)
    {
      // create a tuple of an opaque pointer and a function pointer which has an opaque pointer as its first argument
      FunctionType functionType = (FunctionType) type;
      LLVMTypeRef rttiPointerType = rttiHelper.getGenericRTTIType();
      LLVMTypeRef llvmOpaquePointerType = LLVM.LLVMPointerType(opaqueType, 0);
      LLVMTypeRef llvmFunctionPointer = findRawFunctionPointerType(functionType);
      LLVMTypeRef[] subTypes = new LLVMTypeRef[] {rttiPointerType, llvmOpaquePointerType, llvmFunctionPointer};
      return LLVM.LLVMStructType(C.toNativePointerArray(subTypes, false, true), subTypes.length, false);
    }
    if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      Type[] subTypes = tupleType.getSubTypes();
      LLVMTypeRef[] llvmSubTypes = new LLVMTypeRef[subTypes.length];
      for (int i = 0; i < subTypes.length; i++)
      {
        llvmSubTypes[i] = findNativeType(subTypes[i], temporary);
      }
      LLVMTypeRef nonNullableType = LLVM.LLVMStructType(C.toNativePointerArray(llvmSubTypes, false, true), llvmSubTypes.length, false);
      if (tupleType.canBeNullable())
      {
        // tuple the non-nullable type with a boolean, so that we can tell whether or not the value is null
        LLVMTypeRef[] types = new LLVMTypeRef[] {LLVM.LLVMInt1Type(), nonNullableType};
        return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
      }
      return nonNullableType;
    }
    if (type instanceof NamedType && ((NamedType) type).getResolvedTypeDefinition() != null)
    {
      NamedType namedType = (NamedType) type;
      TypeDefinition typeDefinition = namedType.getResolvedTypeDefinition();
      // check whether the type has been cached
      LLVMTypeRef existingType = nativeNamedTypes.get(typeDefinition);
      if (existingType != null)
      {
        if (typeDefinition instanceof CompoundDefinition)
        {
          if (temporary)
          {
            // for temporary CompoundDefinition values, we use a pointer to the non-nullable type, whether or not the type is nullable
            return LLVM.LLVMPointerType(existingType, 0);
          }
          if (namedType.canBeNullable())
          {
            // tuple the non-nullable type with a boolean, so that we can tell whether or not the value is null
            // this is not necessary for ClassDefinitions, since they are pointers which can actually be null
            LLVMTypeRef[] types = new LLVMTypeRef[] {LLVM.LLVMInt1Type(), existingType};
            return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
          }
        }
        return existingType;
      }
      // the type isn't cached, so create it
      if (typeDefinition instanceof ClassDefinition)
      {
        // cache the LLVM type before we recurse, so that once we recurse, everything will be able to use this type instead of recreating it and possibly recursing infinitely
        // later on, we add the fields using LLVMStructSetBody
        LLVMTypeRef structType = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), typeDefinition.getQualifiedName().toString());
        LLVMTypeRef pointerToStruct = LLVM.LLVMPointerType(structType, 0);
        nativeNamedTypes.put(typeDefinition, pointerToStruct);

        // add the fields to the struct type (findClassSubTypes() will call findNativeType() recursively)
        LLVMTypeRef[] llvmSubTypes = findClassSubTypes((ClassDefinition) typeDefinition);
        LLVM.LLVMStructSetBody(structType, C.toNativePointerArray(llvmSubTypes, false, true), llvmSubTypes.length, false);
        return pointerToStruct;
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        // cache the LLVM type before we recurse, so that once we recurse, everything will be able to use this type instead of recreating it
        // later on, we add the fields using LLVMStructSetBody
        LLVMTypeRef nonNullableStructType = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), typeDefinition.getQualifiedName().toString());
        nativeNamedTypes.put(typeDefinition, nonNullableStructType);

        TypeParameter[] typeParameters = typeDefinition.getTypeParameters();
        MemberVariable[] instanceVariables = ((CompoundDefinition) typeDefinition).getMemberVariables();
        LLVMTypeRef[] llvmSubTypes = new LLVMTypeRef[typeParameters.length + instanceVariables.length];

        // add the type argument RTTI pointers to the struct
        for (int i = 0; i < typeParameters.length; ++i)
        {
          llvmSubTypes[i] = rttiHelper.getGenericRTTIType();
        }

        // add the fields to the struct recursively
        for (int i = 0; i < instanceVariables.length; i++)
        {
          llvmSubTypes[typeParameters.length + i] = findNativeType(instanceVariables[i].getType(), false);
        }
        LLVM.LLVMStructSetBody(nonNullableStructType, C.toNativePointerArray(llvmSubTypes, false, true), llvmSubTypes.length, false);
        if (temporary)
        {
          // for temporary values, we use a pointer to the non-nullable type, whether or not the type is nullable
          return LLVM.LLVMPointerType(nonNullableStructType, 0);
        }
        if (namedType.canBeNullable())
        {
          // tuple the non-nullable type with a boolean, so that we can tell whether or not the value is null
          LLVMTypeRef[] types = new LLVMTypeRef[] {LLVM.LLVMInt1Type(), nonNullableStructType};
          return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
        }
        return nonNullableStructType;
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        // an interface's type is a tuple of:
        // 1. the interface's VFT
        // 2. the object being pointed to
        // 3. any wildcard type arguments' RTTI pointers

        int numWildcards = 0;
        Type[] typeArguments = namedType.getTypeArguments();
        for (int i = 0; typeArguments != null && i < typeArguments.length; ++i)
        {
          if (typeArguments[i] instanceof WildcardType)
          {
            numWildcards++;
          }
        }

        LLVMTypeRef[] types = new LLVMTypeRef[2 + numWildcards];
        types[0] = LLVM.LLVMPointerType(virtualFunctionHandler.getVFTType(typeDefinition), 0);
        types[1] = findNativeType(new ObjectType(false, false, null), false);
        for (int i = 0; i < numWildcards; ++i)
        {
          types[2 + i] = rttiHelper.getGenericRTTIType();
        }
        return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
      }
    }
    if (type instanceof ObjectType ||
        (type instanceof NamedType && ((NamedType) type).getResolvedTypeParameter() != null) ||
        type instanceof WildcardType)
    {
      if (objectType != null)
      {
        return LLVM.LLVMPointerType(objectType, 0);
      }
      objectType = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), "object");

      LLVMTypeRef rttiType = rttiHelper.getGenericRTTIType();
      LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(virtualFunctionHandler.getObjectVFTType(), 0);
      LLVMTypeRef[] structSubTypes = new LLVMTypeRef[] {rttiType, vftPointerType};
      LLVM.LLVMStructSetBody(objectType, C.toNativePointerArray(structSubTypes, false, true), structSubTypes.length, false);
      return LLVM.LLVMPointerType(objectType, 0);
    }
    if (type instanceof NullType)
    {
      return LLVM.LLVMStructType(C.toNativePointerArray(new LLVMTypeRef[0], false, true), 0, false);
    }
    if (type instanceof VoidType)
    {
      return LLVM.LLVMVoidType();
    }
    throw new IllegalStateException("Unexpected Type: " + type);
  }

  /**
   * Finds a function pointer type in its raw form, before being tupled with its RTTI and first argument (always an opaque pointer).
   * This <b>IS NOT</b> a full function type, and should not be used as such.
   * @param functionType - the function type to find the raw LLVM form of
   * @return the LLVMTypeRef corresponding to the raw form of the specified function type
   */
  // package protected and not private, because it needs to be accessible to CodeGenerator for buildNullCheck()
  LLVMTypeRef findRawFunctionPointerType(FunctionType functionType)
  {
    LLVMTypeRef llvmFunctionReturnType = findNativeType(functionType.getReturnType(), false);
    Type[] parameterTypes = functionType.getParameterTypes();
    LLVMTypeRef[] llvmParameterTypes = new LLVMTypeRef[parameterTypes.length + 1];
    llvmParameterTypes[0] = LLVM.LLVMPointerType(opaqueType, 0);
    for (int i = 0; i < parameterTypes.length; ++i)
    {
      llvmParameterTypes[i + 1] = findNativeType(parameterTypes[i], false);
    }
    LLVMTypeRef llvmFunctionType = LLVM.LLVMFunctionType(llvmFunctionReturnType, C.toNativePointerArray(llvmParameterTypes, false, true), llvmParameterTypes.length, false);
    return LLVM.LLVMPointerType(llvmFunctionType, 0);
  }

  /**
   * Finds the raw string type.
   * This is similar to the type of []ubyte, as an LLVM struct, but also contains the actual array of values in addition to the getter and setter functions.
   * This makes it incompatible with any arrays of ubyte which might be proxied generic arrays.
   * @return the raw LLVM type of a string constant
   */
  public LLVMTypeRef findRawStringType()
  {
    ArrayType arrayType = new ArrayType(false, false, new PrimitiveType(false, PrimitiveTypeType.UBYTE, null), null);
    return LLVM.LLVMPointerType(findNonProxiedArrayStructureType(arrayType, 0), 0);
  }

  /**
   * Finds an object type specialised to include some specialised data of the specified type.
   * @param specialisationType - the type that this object should hold (to be appended to the normal object type representation)
   * @return an object type, with an extra field to contain the standard type representation of the specified specialisation type
   */
  public LLVMTypeRef findSpecialisedObjectType(Type specialisationType)
  {
    LLVMTypeRef rttiType = rttiHelper.getGenericRTTIType();
    LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(virtualFunctionHandler.getObjectVFTType(), 0);
    LLVMTypeRef llvmSpecialisedType = findStandardType(specialisationType);
    LLVMTypeRef[] structSubTypes = new LLVMTypeRef[] {rttiType, vftPointerType, llvmSpecialisedType};
    LLVMTypeRef structType = LLVM.LLVMStructType(C.toNativePointerArray(structSubTypes, false, true), structSubTypes.length, false);
    return structType;
  }

  /**
   * Finds the type of the structure inside (i.e. not a pointer to) a non-proxied array of the specified length.
   * @param arrayType - the type of array to find the non-proxied type of
   * @param length - the length of the native array
   * @return the LLVM type of the structure inside a non-proxied array of the specified ArrayType
   */
  public LLVMTypeRef findNonProxiedArrayStructureType(ArrayType arrayType, int length)
  {
    LLVMTypeRef llvmArrayType = findStandardType(arrayType);

    LLVMTypeRef rttiType = rttiHelper.getGenericRTTIType();
    LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(virtualFunctionHandler.getObjectVFTType(), 0);
    LLVMTypeRef lengthType = findStandardType(ArrayLengthMember.ARRAY_LENGTH_TYPE);

    LLVMTypeRef[] getFunctionParamTypes = new LLVMTypeRef[] {llvmArrayType, lengthType};
    LLVMTypeRef getFunctionType = LLVM.LLVMFunctionType(findTemporaryType(arrayType.getBaseType()), C.toNativePointerArray(getFunctionParamTypes, false, true), getFunctionParamTypes.length, false);
    LLVMTypeRef[] setFunctionParamTypes = new LLVMTypeRef[] {llvmArrayType, lengthType, findStandardType(arrayType.getBaseType())};
    LLVMTypeRef setFunctionType = LLVM.LLVMFunctionType(LLVM.LLVMVoidType(), C.toNativePointerArray(setFunctionParamTypes, false, true), setFunctionParamTypes.length, false);

    LLVMTypeRef standardElementType = findStandardType(arrayType.getBaseType());
    LLVMTypeRef elementsArrayType = LLVM.LLVMArrayType(standardElementType, length);

    LLVMTypeRef[] structureTypes = new LLVMTypeRef[] {rttiType, vftPointerType, lengthType, LLVM.LLVMPointerType(getFunctionType, 0), LLVM.LLVMPointerType(setFunctionType, 0), elementsArrayType};
    LLVMTypeRef resultType = LLVM.LLVMStructType(C.toNativePointerArray(structureTypes, false, true), structureTypes.length, false);
    return resultType;
  }

  /**
   * Finds the native (LLVM) type of the specified Method
   * @param method - the Method to find the LLVM type of
   * @return the LLVMTypeRef representing the type of the specified Method
   */
  public LLVMTypeRef findMethodType(Method method)
  {
    Parameter[] parameters = method.getParameters();
    LLVMTypeRef[] types;
    int offset = 1;
    // add the 'this' type to the function - 'this' always has a temporary type representation
    if (method.isStatic())
    {
      // for static methods, we add an unused opaque*, so that the static method can be easily converted to a function type
      types = new LLVMTypeRef[1 + parameters.length];
      types[0] = getOpaquePointer();
    }
    else if (method.getContainingTypeDefinition() instanceof ClassDefinition)
    {
      types = new LLVMTypeRef[1 + parameters.length];
      types[0] = findTemporaryType(new NamedType(false, method.isImmutable(), method.isImmutable(), method.getContainingTypeDefinition()));
    }
    else if (method.getContainingTypeDefinition() instanceof CompoundDefinition)
    {
      types = new LLVMTypeRef[1 + parameters.length];
      types[0] = findTemporaryType(new NamedType(false, method.isImmutable(), method.isImmutable(), method.getContainingTypeDefinition()));
    }
    else if (method.getContainingTypeDefinition() instanceof InterfaceDefinition)
    {
      TypeParameter[] typeParameters = method.getContainingTypeDefinition().getTypeParameters();
      types = new LLVMTypeRef[1 + typeParameters.length + parameters.length];
      types[0] = findTemporaryType(new NamedType(false, method.isImmutable(), method.isImmutable(), method.getContainingTypeDefinition()));
      for (int i = 0; i < typeParameters.length; ++i)
      {
        types[offset + i] = rttiHelper.getGenericRTTIType();
      }
      offset += typeParameters.length;
    }
    else if (method instanceof BuiltinMethod)
    {
      types = new LLVMTypeRef[1 + parameters.length];
      types[0] = findTemporaryType(((BuiltinMethod) method).getBaseType());
    }
    else
    {
      throw new IllegalArgumentException("Unknown type of method: " + method);
    }
    for (int i = 0; i < parameters.length; ++i)
    {
      types[offset + i] = findStandardType(parameters[i].getType());
    }
    LLVMTypeRef resultType = findStandardType(method.getReturnType());

    return LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);
  }

  /**
   * Finds the native (LLVM) type of the getter of the specified Property
   * @param property - the Property to find the LLVM type of the getter of
   * @return the LLVMTypeRef representing the type of the specified Property's getter method
   */
  public LLVMTypeRef findPropertyGetterType(Property property)
  {
    TypeDefinition typeDefinition = property.getContainingTypeDefinition();
    LLVMTypeRef[] types;
    if (property.isStatic())
    {
      types = new LLVMTypeRef[1];
      types[0] = getOpaquePointer();
    }
    else
    {
      if (typeDefinition instanceof InterfaceDefinition)
      {
        TypeParameter[] typeParameters = typeDefinition.getTypeParameters();
        types = new LLVMTypeRef[1 + typeParameters.length];
        types[0] = findTemporaryType(new NamedType(false, false, false, typeDefinition));
        for (int i = 0; i < typeParameters.length; ++i)
        {
          types[1 + i] = rttiHelper.getGenericRTTIType();
        }
      }
      else
      {
        types = new LLVMTypeRef[1];
        types[0] = findTemporaryType(new NamedType(false, false, false, typeDefinition));
      }
    }
    LLVMTypeRef resultType = findStandardType(property.getType());
    return LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);
  }

  /**
   * Finds the native (LLVM) type of the setter or constructor of the specified Property
   * @param property - the Property to find the LLVM type of the setter/constructor of
   * @return the LLVMTypeRef representing the type of the specified Property's setter/constructor method
   */
  public LLVMTypeRef findPropertySetterConstructorType(Property property)
  {
    TypeDefinition typeDefinition = property.getContainingTypeDefinition();
    LLVMTypeRef[] types;
    int offset = 1;
    if (property.isStatic())
    {
      types = new LLVMTypeRef[2];
      types[0] = getOpaquePointer();
    }
    else
    {
      if (typeDefinition instanceof InterfaceDefinition)
      {
        TypeParameter[] typeParameters = typeDefinition.getTypeParameters();
        types = new LLVMTypeRef[2 + typeParameters.length];
        types[0] = findTemporaryType(new NamedType(false, false, false, typeDefinition));
        for (int i = 0; i < typeParameters.length; ++i)
        {
          types[offset + i] = rttiHelper.getGenericRTTIType();
        }
        offset += typeParameters.length;
      }
      else
      {
        types = new LLVMTypeRef[2];
        types[0] = findTemporaryType(new NamedType(false, false, false, typeDefinition));
      }
    }
    types[offset] = findStandardType(property.getType());
    LLVMTypeRef resultType = LLVM.LLVMVoidType();
    return LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);
  }

  /**
   * Finds the sub-types of the native representation of the specified ClassDefinition, including fields and virtual function table pointers.
   * @param classDefinition - the class definition to find the sub-types of
   * @return the sub-types of the specified ClassDefinition
   */
  private LLVMTypeRef[] findClassSubTypes(ClassDefinition classDefinition)
  {
    NamedType[] implementedInterfaces = findSubClassInterfaces(classDefinition);
    NamedType superType = classDefinition.getSuperType();
    TypeParameter[] typeParameters = classDefinition.getTypeParameters();
    MemberVariable[] nonStaticVariables = classDefinition.getMemberVariables();
    int offset; // offset to the class VFT
    LLVMTypeRef[] subTypes;
    if (superType == null)
    {
      // 1 RTTI pointer, 1 object-VFT (for builtin methods), 1 class VFT, some interface VFTs, some type parameter RTTI pointers, and some fields
      subTypes = new LLVMTypeRef[3 + implementedInterfaces.length + typeParameters.length + nonStaticVariables.length];
      subTypes[0] = rttiHelper.getGenericRTTIType();
      subTypes[1] = LLVM.LLVMPointerType(virtualFunctionHandler.getObjectVFTType(), 0);
      offset = 2;
    }
    else
    {
      ClassDefinition superClassDefinition = (ClassDefinition) superType.getResolvedTypeDefinition();
      LLVMTypeRef[] superClassSubTypes = findClassSubTypes(superClassDefinition);
      // everything from the super-class, 1 class VFT, some interface VFTs, some type parameter RTTI pointers, and some fields
      // we only include interfaces which were not included in any super-classes
      subTypes = new LLVMTypeRef[superClassSubTypes.length + 1 + implementedInterfaces.length + typeParameters.length + nonStaticVariables.length];
      System.arraycopy(superClassSubTypes, 0, subTypes, 0, superClassSubTypes.length);
      offset = superClassSubTypes.length;
    }
    subTypes[offset] = LLVM.LLVMPointerType(virtualFunctionHandler.getVFTType(classDefinition), 0);
    for (int i = 0; i < implementedInterfaces.length; ++i)
    {
      subTypes[offset + 1 + i] = LLVM.LLVMPointerType(virtualFunctionHandler.getVFTType(implementedInterfaces[i].getResolvedTypeDefinition()), 0);
    }
    offset += 1 + implementedInterfaces.length;
    for (int i = 0; i < typeParameters.length; ++i)
    {
      subTypes[offset + i] = rttiHelper.getGenericRTTIType();
    }
    offset += typeParameters.length;
    for (int i = 0; i < nonStaticVariables.length; ++i)
    {
      subTypes[offset + i] = findNativeType(nonStaticVariables[i].getType(), false);
    }
    return subTypes;
  }

  /**
   * Finds the list of interface types which are implemented in the specified ClassDefinition, but in none of its super-classes, in linearisation order.
   * @param classDefinition - the ClassDefinition to find the list of interfaces for
   * @return the list of interfaces for the specified sub-class in linearisation order
   */
  NamedType[] findSubClassInterfaces(ClassDefinition classDefinition)
  {
    List<NamedType> result = new LinkedList<NamedType>();
    for (NamedType superType : classDefinition.getInheritanceLinearisation())
    {
      if (superType.getResolvedTypeDefinition() instanceof InterfaceDefinition)
      {
        result.add(superType);
      }
    }
    // since the direct super-class's linearisation contains everything inherited above this class, we don't need to go through the whole hierarchy ourselves
    NamedType directSuperType = classDefinition.getSuperType();
    if (directSuperType != null)
    {
      GenericTypeSpecialiser genericTypeSpecialiser = new GenericTypeSpecialiser(directSuperType);
      for (NamedType superSuperType : directSuperType.getResolvedTypeDefinition().getInheritanceLinearisation())
      {
        Iterator<NamedType> it = result.iterator();
        while (it.hasNext())
        {
          if (it.next().isRuntimeEquivalent(genericTypeSpecialiser.getSpecialisedType(superSuperType)))
          {
            it.remove();
          }
        }
      }
    }
    return result.toArray(new NamedType[result.size()]);
  }

  /**
   * Finds the pointer to the specified TypeParameter inside the specified value.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param baseValue - the base value to get the TypeParameter of, in a temporary type representation - this must be of a type which contains the specified TypeParameter (or a sub-type thereof)
   * @param typeParameter - the TypeParameter to get the pointer to - this must not be an interface's TypeParameter
   * @return the pointer to the specified TypeParameter's location inside the specified base value
   */
  public LLVMValueRef getTypeParameterPointer(LLVMBuilderRef builder, LLVMValueRef baseValue, TypeParameter typeParameter)
  {
    TypeDefinition typeDefinition = typeParameter.getContainingTypeDefinition();
    if (typeDefinition instanceof InterfaceDefinition)
    {
      throw new IllegalArgumentException("Cannot get a pointer to an interface's TypeParameter RTTI block: " + typeParameter);
    }

    TypeParameter[] typeParameters = typeDefinition.getTypeParameters();
    int index = -1;
    for (int i = 0; i < typeParameters.length; ++i)
    {
      if (typeParameters[i] == typeParameter)
      {
        index = i;
        break;
      }
    }
    if (index == -1)
    {
      throw new IllegalStateException("A TypeParameter is not contained by its containingTypeDefinition: " + typeParameter);
    }

    if (typeDefinition instanceof ClassDefinition)
    {
      // skip the RTTI pointer and the object VFT from the top-level class
      index += 2;

      // skip the super-class representations
      NamedType superType = ((ClassDefinition) typeDefinition).getSuperType();
      while (superType != null)
      {
        ClassDefinition superClassDefinition = (ClassDefinition) superType.getResolvedTypeDefinition();
        // 1 class VFT, some interface VFTs, some type parameter RTTI pointers, and some fields
        index += 1 + findSubClassInterfaces(superClassDefinition).length + superClassDefinition.getTypeParameters().length + superClassDefinition.getMemberVariables().length;
        superType = superClassDefinition.getSuperType();
      }
      // skip the virtual function tables from this class
      index += 1 + findSubClassInterfaces((ClassDefinition) typeDefinition).length;
    }
    // note: compound definitions work here too, because their type parameters are stored right at the start of their type representation
    return LLVM.LLVMBuildStructGEP(builder, baseValue, index, "");
  }

  /**
   * Finds the pointer to the specified field inside the specified value.
   * The value should be a NamedType in a temporary type representation, and should be for the type which contains the specified field, or a subtype thereof.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param baseValue - the base value to get the field of
   * @param memberVariable - the MemberVariable to extract
   * @return a pointer to the specified field inside baseValue
   */
  public LLVMValueRef getMemberPointer(LLVMBuilderRef builder, LLVMValueRef baseValue, MemberVariable memberVariable)
  {
    TypeDefinition typeDefinition = memberVariable.getEnclosingTypeDefinition();
    int index = memberVariable.getMemberIndex();
    if (typeDefinition instanceof ClassDefinition)
    {
      // skip the RTTI pointer and the object VFT from the top-level class
      index += 2;
      // skip the super-class representations
      NamedType superType = ((ClassDefinition) typeDefinition).getSuperType();
      while (superType != null)
      {
        ClassDefinition superClassDefinition = (ClassDefinition) superType.getResolvedTypeDefinition();
        // 1 class VFT, some interface VFTs, some type parameter RTTI pointers, and some fields
        index += 1 + findSubClassInterfaces(superClassDefinition).length + superClassDefinition.getTypeParameters().length + superClassDefinition.getMemberVariables().length;
        superType = superClassDefinition.getSuperType();
      }
      // skip the virtual function tables and type parameters from this class
      index += 1 + findSubClassInterfaces((ClassDefinition) typeDefinition).length + typeDefinition.getTypeParameters().length;
    }
    else if (typeDefinition instanceof CompoundDefinition)
    {
      // skip this compound type's type parameters
      index += typeDefinition.getTypeParameters().length;
    }
    return LLVM.LLVMBuildStructGEP(builder, baseValue, index, "");
  }

  /**
   * Gets the poiner to the length field of the specified array.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param array - the array to get the length field of, in a temporary type representation
   * @return a pointer to the length field of the specified array
   */
  public LLVMValueRef getArrayLengthPointer(LLVMBuilderRef builder, LLVMValueRef array)
  {
    return LLVM.LLVMBuildStructGEP(builder, array, 2, "");
  }

  /**
   * Finds the pointer to the getter function inside the specified array
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param array - the array to get the getter function of
   * @return the pointer to the getter function of the specified array
   */
  public LLVMValueRef getArrayGetterFunctionPointer(LLVMBuilderRef builder, LLVMValueRef array)
  {
    return LLVM.LLVMBuildStructGEP(builder, array, 3, "");
  }

  /**
   * Finds the pointer to the setter function inside the specified array
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param array - the array to get the setter function of
   * @return the pointer to the setter function of the specified array
   */
  public LLVMValueRef getArraySetterFunctionPointer(LLVMBuilderRef builder, LLVMValueRef array)
  {
    return LLVM.LLVMBuildStructGEP(builder, array, 4, "");
  }

  /**
   * Finds the pointer to the specified element inside the specified non-proxied array.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param array - the non-proxied array to get the element of
   * @param index - the index to get inside the array
   * @return the pointer to the element at the specified index inside the non-proxied array
   */
  public LLVMValueRef getNonProxiedArrayElementPointer(LLVMBuilderRef builder, LLVMValueRef array, LLVMValueRef index)
  {
    LLVMValueRef[] elementIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                        LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false),
                                                        index};
    return LLVM.LLVMBuildGEP(builder, array, C.toNativePointerArray(elementIndices, false, true), elementIndices.length, "");
  }

  /**
   * Retrieves the value at the specified index in the specified array.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param array - the array to get the element out of, in a temporary type representation
   * @param index - the index in the array to retrieve the element from, as an i32
   * @return the array element at the specified index, in a temporary type representation
   */
  public LLVMValueRef buildRetrieveArrayElement(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef array, LLVMValueRef index)
  {
    LLVMValueRef getterFunctionPtr = LLVM.LLVMBuildStructGEP(builder, array, 3, "");
    LLVMValueRef getterFunction = LLVM.LLVMBuildLoad(builder, getterFunctionPtr, "");
    LLVMValueRef[] arguments = new LLVMValueRef[] {array, index};
    LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "arrayRetrieveInvokeContinue");
    LLVMValueRef result = LLVM.LLVMBuildInvoke(builder, getterFunction, C.toNativePointerArray(arguments, false, true), arguments.length, continuationBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
    return result;
  }

  /**
   * Stores the specified value in the specified array.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param array - the array to store the element inside
   * @param index - the index in the array to store the element in, as an i32
   * @param value - the value to store in the array, in a standard type representation
   */
  public void buildStoreArrayElement(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef array, LLVMValueRef index, LLVMValueRef value)
  {
    LLVMValueRef setterFunctionPtr = LLVM.LLVMBuildStructGEP(builder, array, 4, "");
    LLVMValueRef setterFunction = LLVM.LLVMBuildLoad(builder, setterFunctionPtr, "");
    LLVMValueRef[] arguments = new LLVMValueRef[] {array, index, value};
    LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "arrayStoreInvokeContinue");
    LLVM.LLVMBuildInvoke(builder, setterFunction, C.toNativePointerArray(arguments, false, true), arguments.length, continuationBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
  }

  /**
   * Builds a function which proxies a given Method and converts the argument and return types to the types of the specified MethodReference.
   * The generated function accepts arguments and returns results of the MethodReference's type, and expects a callee containing some proxy-function-specific data, which includes a type argument mapper.
   * The generated function is private to the current TypeDefinition, as it extracts data from a type argument mapper which is expected to be in the format of this TypeDefinition.
   * @param methodReference - the MethodReference that the proxy function is for
   * @return the LLVM function created
   */
  private LLVMValueRef buildMethodReferenceProxyFunction(MethodReference methodReference)
  {
    String mangledName = REFERENCE_PROXY_FUNCTION_PREFIX + methodReference.getDisambiguator().toString() + "_" + methodReference.getReferencedMember().getMangledName();
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    Method method = methodReference.getReferencedMember();
    Parameter[] proxiedParameters = method.getParameters();

    Type[] parameterTypes = methodReference.getParameterTypes();
    LLVMTypeRef[] llvmParameterTypes = new LLVMTypeRef[1 + parameterTypes.length];
    llvmParameterTypes[0] = getOpaquePointer();
    for (int i = 0; i < parameterTypes.length; ++i)
    {
      llvmParameterTypes[1 + i] = findStandardType(parameterTypes[i]);
    }
    LLVMTypeRef returnType = findStandardType(methodReference.getReturnType());
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(returnType, C.toNativePointerArray(llvmParameterTypes, false, true), llvmParameterTypes.length, false);

    LLVMValueRef function = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetLinkage(function, LLVM.LLVMLinkage.LLVMPrivateLinkage);
    LLVM.LLVMSetVisibility(function, LLVM.LLVMVisibility.LLVMHiddenVisibility);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(function);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    // find the type of the callee opaque pointer, and the things we need to extract from it
    LLVMTypeRef nativeCalleeType;
    int numRTTIBlocks = 0;
    if (method.isStatic())
    {
      nativeCalleeType = getOpaquePointer();
    }
    else
    {
      if (methodReference.getContainingType() != null)
      {
        if (method.getContainingTypeDefinition() instanceof InterfaceDefinition)
        {
          // make sure we don't accidentally use a type with wildcard type parameters embedded in it
          nativeCalleeType = findStandardType(new NamedType(false, false, false, method.getContainingTypeDefinition()));

          // interfaces need to have their type arguments' RTTI blocks passed into their functions, so we need to pass them in through the opaque pointer
          // here, we find out how many of them we will need to pass through
          Type[] typeArguments = methodReference.getContainingType().getTypeArguments();
          numRTTIBlocks = typeArguments == null ? 0 : typeArguments.length;
        }
        else
        {
          nativeCalleeType = findStandardType(methodReference.getContainingType());
        }
      }
      else
      {
        // the method reference does not have a containing type, so we can assume that this is a builtin method
        nativeCalleeType = findStandardType(((BuiltinMethod) method).getBaseType());
      }
    }
    LLVMTypeRef proxiedFunctionType = LLVM.LLVMPointerType(findMethodType(method), 0);
    LLVMTypeRef typeArgumentMapperType = rttiHelper.getGenericTypeArgumentMapperStructureType();
    LLVMTypeRef[] opaquePointerSubTypes = new LLVMTypeRef[3 + numRTTIBlocks];
    opaquePointerSubTypes[0] = nativeCalleeType;
    opaquePointerSubTypes[1] = proxiedFunctionType;
    for (int i = 0; i < numRTTIBlocks; ++i)
    {
      opaquePointerSubTypes[2 + i] = rttiHelper.getGenericRTTIType();
    }
    opaquePointerSubTypes[2 + numRTTIBlocks] = typeArgumentMapperType;
    LLVMTypeRef opaquePointerType = LLVM.LLVMStructType(C.toNativePointerArray(opaquePointerSubTypes, false, true), opaquePointerSubTypes.length, false);

    LLVMValueRef opaquePointer = LLVM.LLVMGetParam(function, 0);
    opaquePointer = LLVM.LLVMBuildBitCast(builder, opaquePointer, LLVM.LLVMPointerType(opaquePointerType, 0), "");

    LLVMValueRef calleePointer = LLVM.LLVMBuildStructGEP(builder, opaquePointer, 0, "");
    LLVMValueRef callee = LLVM.LLVMBuildLoad(builder, calleePointer, "");

    LLVMValueRef proxiedFunctionPtr = LLVM.LLVMBuildStructGEP(builder, opaquePointer, 1, "");
    LLVMValueRef proxiedFunction = LLVM.LLVMBuildLoad(builder, proxiedFunctionPtr, "");

    LLVMValueRef typeMapper = LLVM.LLVMBuildStructGEP(builder, opaquePointer, 2 + numRTTIBlocks, "");
    TypeParameterAccessor typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper, typeDefinition, typeMapper);

    // create the argument list to the proxy function, and put the callee and any required type parameter RTTI pointers into it
    LLVMValueRef[] realArguments;
    int offset;
    TypeParameterAccessor methodTypeAccessor;
    if (method.isStatic())
    {
      methodTypeAccessor = new TypeParameterAccessor(builder, rttiHelper);
      realArguments = new LLVMValueRef[1 + proxiedParameters.length];
      offset = 1;
    }
    else if (method.getContainingTypeDefinition() instanceof InterfaceDefinition)
    {
      // interfaces need type argument RTTI blocks to be passed in as parameters, so extract them from the opaque pointer where they are stored
      // also, we need to build methodTypeAccessor from these RTTI blocks
      Map<TypeParameter, LLVMValueRef> knownTypeParameters = new HashMap<TypeParameter, LLVMValueRef>();
      TypeParameter[] typeParameters = method.getContainingTypeDefinition().getTypeParameters();

      realArguments = new LLVMValueRef[1 + numRTTIBlocks + proxiedParameters.length];
      for (int i = 0; i < numRTTIBlocks; ++i)
      {
        LLVMValueRef rttiBlockPtr = LLVM.LLVMBuildStructGEP(builder, opaquePointer, 2 + i, "");
        realArguments[1 + i] = LLVM.LLVMBuildLoad(builder, rttiBlockPtr, "");
        knownTypeParameters.put(typeParameters[i], realArguments[1 + i]);
      }
      methodTypeAccessor = new TypeParameterAccessor(builder, rttiHelper, method.getContainingTypeDefinition(), knownTypeParameters);
      offset = 1 + numRTTIBlocks;
    }
    else
    {
      if (method.getContainingTypeDefinition() != null)
      {
        methodTypeAccessor = new TypeParameterAccessor(builder, this, rttiHelper, method.getContainingTypeDefinition(), callee);
      }
      else
      {
        // assume this method is a builtin method, in which case it can reference type parameters from inside typeParameterAccessor, but not define its own
        methodTypeAccessor = typeParameterAccessor;
      }
      realArguments = new LLVMValueRef[1 + proxiedParameters.length];
      offset = 1;
    }
    realArguments[0] = callee;

    // convert the arguments
    for (int i = 0; i < parameterTypes.length; ++i)
    {
      LLVMValueRef argument = LLVM.LLVMGetParam(function, 1 + i);
      Type realParameterType = proxiedParameters[i].getType();
      argument = convertStandardToTemporary(builder, argument, parameterTypes[i]);
      // we skip run-time checks for this conversion, because the only change is type parameters being filled in by the MethodReference,
      // and we know at compile-time that they are more specific in the MethodReference than in the Method's actual type
      argument = convertTemporary(builder, landingPadContainer, argument, parameterTypes[i], realParameterType, true, typeParameterAccessor, methodTypeAccessor);
      argument = convertTemporaryToStandard(builder, argument, realParameterType);
      realArguments[offset + i] = argument;
    }

    // call the function
    LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "methodInvokeContinue");
    LLVMValueRef result = LLVM.LLVMBuildInvoke(builder, proxiedFunction, C.toNativePointerArray(realArguments, false, true), realArguments.length, invokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);

    if (methodReference.getReturnType() instanceof VoidType)
    {
      LLVM.LLVMBuildRetVoid(builder);
    }
    else
    {
      // convert the result
      Type realResultType = method.getReturnType();
      result = convertStandardToTemporary(builder, result, realResultType);
      // we skip run-time checks for this conversion, because the only change is type parameters being filled in by the MethodReference,
      // and we know at compile-time that they are more specific in the MethodReference than in the Method's actual type
      result = convertTemporary(builder, landingPadContainer, result, realResultType, methodReference.getReturnType(), true, methodTypeAccessor, typeParameterAccessor);
      result = convertTemporaryToStandard(builder, result, methodReference.getReturnType());
      LLVM.LLVMBuildRet(builder, result);
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return function;
  }

  /**
   * Extracts the specified MethodReference on the specified callee into a function-typed value.
   * If the callee is an interface, or some conversion needs to be done between the MethodReference's type
   * and the underlying Method's type, then the function is proxied, and the proxy function is returned.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param callee - the callee for the method, in a temporary type representation
   * @param calleeType - the current type of the callee
   * @param methodReference - the reference to the Method to extract
   * @param skipVirtualLookup - true to skip a virtual function table lookup and use the function directly, false to do a VFT lookup as normal
   * @param typeParameterAccessor - the TypeParameterAccessor to get the values of any TypeParameters from
   * @return the function extracted, in the temporary type representation of a FunctionType constructed from the MethodReference (not the underlying Method)
   */
  public LLVMValueRef extractMethodFunction(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef callee, Type calleeType, MethodReference methodReference, boolean skipVirtualLookup, TypeParameterAccessor typeParameterAccessor)
  {
    Method method = methodReference.getReferencedMember();

    boolean needsProxying = false;
    if (!method.isStatic() && method.getContainingTypeDefinition() instanceof InterfaceDefinition)
    {
      // for interface methods, the callee is the standard representation of the interface (i.e. a tuple of the object and the interface VFT)
      // so in order to extract it to a function pointer (a tuple of RTTI, callee pointer, and function pointer), we need to store the interface
      // callee as a pointer, which can only be done by proxying the method
      needsProxying = true;
    }
    else
    {
      // if the return type or any of the parameter types need converting, we need to proxy the method
      Parameter[] parameters = method.getParameters();
      Type[] referenceParameterTypes = methodReference.getParameterTypes();
      for (int i = 0; i < parameters.length; ++i)
      {
        // we skip run-time checks for this conversion, because the only change is type parameters being filled in by the MethodReference,
        // and we know at compile-time that they are more specific in the MethodReference than in the Method's actual type
        if (checkRequiresConversion(referenceParameterTypes[i], parameters[i].getType(), false))
        {
          needsProxying = true;
          break;
        }
      }
      // we skip run-time checks for this conversion, because the only change is type parameters being filled in by the MethodReference,
      // and we know at compile-time that they are more specific in the MethodReference than in the Method's actual type
      if (checkRequiresConversion(method.getReturnType(), methodReference.getReturnType(), false))
      {
        needsProxying = true;
      }
    }

    if (!method.isStatic())
    {
      if (methodReference.getContainingType() != null)
      {
        Type newCalleeType = methodReference.getContainingType();
        callee = convertTemporary(builder, landingPadContainer, callee, calleeType, newCalleeType, false, typeParameterAccessor, typeParameterAccessor);
        calleeType = newCalleeType;
      }
      else
      {
        // there is no containing type definition, so assume this is a builtin method
        Type newCalleeType = ((BuiltinMethod) method).getBaseType();
        // since this is a builtin method, it cannot contain type parameters from any context but the MethodReference's context
        // so we can convert it with the same TypeParameterAccessor as in the other case
        callee = convertTemporary(builder, landingPadContainer, callee, calleeType, newCalleeType, false, typeParameterAccessor, typeParameterAccessor);
        calleeType = newCalleeType;
      }
    }

    LLVMValueRef function;
    if (needsProxying ||
        method.isStatic() ||
        calleeType instanceof ObjectType ||
        calleeType instanceof WildcardType ||
        (calleeType instanceof NamedType && ((NamedType) calleeType).getResolvedTypeDefinition() instanceof ClassDefinition) ||
        (calleeType instanceof NamedType && ((NamedType) calleeType).getResolvedTypeDefinition() instanceof InterfaceDefinition) ||
        (calleeType instanceof NamedType && ((NamedType) calleeType).getResolvedTypeParameter() != null) ||
        calleeType instanceof ArrayType)
    {
      function = codeGenerator.lookupMethodFunction(builder, landingPadContainer, callee, calleeType, methodReference, skipVirtualLookup, typeParameterAccessor);
    }
    else
    {
      function = getBaseChangeFunction(method);
    }

    LLVMValueRef realCallee;
    LLVMValueRef realFunction;
    if (needsProxying)
    {
      // find the type of the callee opaque pointer, and the things we need to extract from it
      LLVMTypeRef nativeCalleeType;
      int numRTTIBlocks = 0;
      if (method.isStatic())
      {
        nativeCalleeType = getOpaquePointer();
      }
      else
      {
        if (methodReference.getContainingType() != null)
        {
          if (method.getContainingTypeDefinition() instanceof InterfaceDefinition)
          {
            // make sure we don't accidentally use a type with wildcard type parameters embedded in it
            nativeCalleeType = findStandardType(new NamedType(false, false, false, method.getContainingTypeDefinition()));

            // interfaces need to have their type arguments' RTTI blocks passed into their functions, so we need to pass them in through the opaque pointer
            // here, we find out how many of them we will need to pass through
            Type[] typeArguments = methodReference.getContainingType().getTypeArguments();
            numRTTIBlocks = typeArguments == null ? 0 : typeArguments.length;
          }
          else
          {
            nativeCalleeType = findStandardType(methodReference.getContainingType());
          }
        }
        else
        {
          // the method reference does not have a containing type, so we can assume that this is a builtin method
          nativeCalleeType = findStandardType(((BuiltinMethod) method).getBaseType());
        }
      }
      LLVMTypeRef proxiedFunctionType = LLVM.LLVMPointerType(findMethodType(method), 0);
      LLVMTypeRef typeArgumentMapperType = typeParameterAccessor.getTypeArgumentMapperType();
      LLVMTypeRef[] opaquePointerSubTypes = new LLVMTypeRef[3 + numRTTIBlocks];
      opaquePointerSubTypes[0] = nativeCalleeType;
      opaquePointerSubTypes[1] = proxiedFunctionType;
      for (int i = 0; i < numRTTIBlocks; ++i)
      {
        opaquePointerSubTypes[2 + i] = rttiHelper.getGenericRTTIType();
      }
      opaquePointerSubTypes[2 + numRTTIBlocks] = typeArgumentMapperType;

      LLVMTypeRef opaquePointerType = LLVM.LLVMPointerType(LLVM.LLVMStructType(C.toNativePointerArray(opaquePointerSubTypes, false, true), opaquePointerSubTypes.length, false), 0);

      // allocate the opaque pointer as the real callee of this object
      LLVMValueRef pointer = codeGenerator.buildHeapAllocation(builder, opaquePointerType);

      if (!method.isStatic() && method.getContainingTypeDefinition() instanceof InterfaceDefinition)
      {
        // this is a non-static interface method, so store each of the type argument RTTI blocks inside the new piece of memory
        int wildcardIndex = 0;
        Type[] typeArguments = methodReference.getContainingType().getTypeArguments();
        for (int i = 0; i < numRTTIBlocks; ++i)
        {
          LLVMValueRef rttiBlock;
          if (typeArguments[i] instanceof WildcardType)
          {
            // the first wildcard RTTI is at index 2 inside the callee interface (before it are the interface's VFT pointer and the object pointer)
            rttiBlock = LLVM.LLVMBuildExtractValue(builder, callee, 2 + wildcardIndex, "");
            ++wildcardIndex;
          }
          else
          {
            rttiBlock = rttiHelper.buildRTTICreation(builder, typeArguments[i], typeParameterAccessor);
          }
          LLVMValueRef rttiBlockPointer = LLVM.LLVMBuildStructGEP(builder, pointer, 2 + i, "");
          LLVM.LLVMBuildStore(builder, rttiBlock, rttiBlockPointer);
        }

        if (wildcardIndex > 0)
        {
          // there were wildcards in the interface's type, so we need to remove them from the native type of the callee before they can be stored in the opaque pointer block
          // to do this, we find the native type of the interface without any wildcard type arguments
          // (it doesn't matter what they actually are, so we just use the NamedType constructor which sets them to type parameter references)
          // then, we copy the VFT pointer and object pointer from our current interface value and put them into the one without wildcard type arguments
          LLVMValueRef newCallee = LLVM.LLVMGetUndef(findStandardType(new NamedType(false, false, false, method.getContainingTypeDefinition())));
          LLVMValueRef vftPointer = LLVM.LLVMBuildExtractValue(builder, callee, 0, "");
          newCallee = LLVM.LLVMBuildInsertValue(builder, newCallee, vftPointer, 0, "");
          LLVMValueRef objectPointer = LLVM.LLVMBuildExtractValue(builder, callee, 1, "");
          newCallee = LLVM.LLVMBuildInsertValue(builder, newCallee, objectPointer, 1, "");
          callee = newCallee;
        }
      }

      // store the callee inside the new piece of memory
      LLVMValueRef calleePointer = LLVM.LLVMBuildStructGEP(builder, pointer, 0, "");
      LLVM.LLVMBuildStore(builder, callee, calleePointer);

      // store the function pointer inside the new piece of memory
      LLVMValueRef functionPointer = LLVM.LLVMBuildStructGEP(builder, pointer, 1, "");
      LLVM.LLVMBuildStore(builder, function, functionPointer);

      // store the type argument mapper inside the new piece of memory
      LLVMValueRef typeMapper = LLVM.LLVMBuildStructGEP(builder, pointer, 2 + numRTTIBlocks, "");
      typeParameterAccessor.buildTypeArgumentMapper(typeMapper);

      // set the callee and the function to the proxied callee and function
      realCallee = LLVM.LLVMBuildBitCast(builder, pointer, getOpaquePointer(), "");
      realFunction = buildMethodReferenceProxyFunction(methodReference);
    }
    else
    {
      if (method.isStatic())
      {
        realCallee = LLVM.LLVMConstNull(getOpaquePointer());
      }
      else
      {
        realCallee = convertTemporary(builder, landingPadContainer, callee, calleeType, new ObjectType(false, false, null), false, typeParameterAccessor, new TypeParameterAccessor(builder, rttiHelper));
        realCallee = LLVM.LLVMBuildBitCast(builder, realCallee, getOpaquePointer(), "");
      }
      realFunction = function;
    }

    FunctionType functionType = new FunctionType(false, method.isImmutable(), methodReference.getReturnType(), methodReference.getParameterTypes(), methodReference.getCheckedThrownTypes(), null);
    // make sure this function type doesn't reference any type parameters (or if it does, replace them with 'object')
    // otherwise, the RTTI would turn out wrong (due to typeParameterAccessor) and people would be able to cast
    // from object to a function of whatever the values of those type parameters are,
    // even though the function's native representation might not take whatever type those type parameters happen to be
    functionType = (FunctionType) stripTypeParameters(functionType);

    realFunction = LLVM.LLVMBuildBitCast(builder, realFunction, findRawFunctionPointerType(functionType), "");

    LLVMValueRef result = LLVM.LLVMGetUndef(findStandardType(functionType));
    result = LLVM.LLVMBuildInsertValue(builder, result, rttiHelper.buildRTTICreation(builder, functionType, typeParameterAccessor), 0, "");
    result = LLVM.LLVMBuildInsertValue(builder, result, realCallee, 1, "");
    result = LLVM.LLVMBuildInsertValue(builder, result, realFunction, 2, "");
    return convertStandardToTemporary(builder, result, functionType);
  }


  /**
   * Builds the preamble to the LLVM argument list of a non-static interface method, which contains the callee and all of the interface's type argument RTTI blocks.
   * The resulting array is an argument list which begins with the preamble, and leaves enough space for the normal arguments to the function.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param typeParameterAccessor - the TypeParameterAccessor with which to look up any type parameters referenced by calleeType
   * @param callee - the callee of the interface, which should be of the type specified by calleeType
   * @param calleeType - the type of the callee of this interface
   * @param calleeAccessor - the TypeParameterAccessor to look up the values of any type parameters inside callee with
   * @param normalArgumentCount - the number of trailing spaces to leave in the argument list for normal arguments
   * @return the argument list created
   */
  private LLVMValueRef[] buildInterfaceArgListPreamble(LLVMBuilderRef builder, LLVMValueRef callee, NamedType calleeType, TypeParameterAccessor calleeAccessor, int normalArgumentCount)
  {
    Type[] typeArguments = calleeType.getTypeArguments();
    LLVMValueRef[] realArguments = new LLVMValueRef[1 + (typeArguments == null ? 0 : typeArguments.length) + normalArgumentCount];
    // if we have any wildcard type arguments, their RTTI blocks need to be taken from the callee
    int wildcardIndex = 0;
    for (int i = 0; typeArguments != null && i < typeArguments.length; ++i)
    {
      if (typeArguments[i] instanceof WildcardType)
      {
        // the first wildcard RTTI is at index 2 (before it are the interface's VFT pointer and the object pointer)
        realArguments[1 + i] = LLVM.LLVMBuildExtractValue(builder, callee, 2 + wildcardIndex, "");
        ++wildcardIndex;
      }
      else
      {
        realArguments[1 + i] = rttiHelper.buildRTTICreation(builder, typeArguments[i], calleeAccessor);
      }
    }
    if (wildcardIndex > 0)
    {
      // there were wildcards in the interface's type, so we need to remove them from the native type of the callee before performing the call
      // to do this, we find the native type of the interface without any wildcard type arguments
      // (it doesn't matter what they actually are, so we just use the NamedType constructor which sets them to type parameter references)
      // then, we copy the VFT pointer and object pointer from our current interface value and put them into the one without type arguments
      LLVMValueRef newCallee = LLVM.LLVMGetUndef(findStandardType(new NamedType(false, false, false, calleeType.getResolvedTypeDefinition())));
      LLVMValueRef vftPointer = LLVM.LLVMBuildExtractValue(builder, callee, 0, "");
      newCallee = LLVM.LLVMBuildInsertValue(builder, newCallee, vftPointer, 0, "");
      LLVMValueRef objectPointer = LLVM.LLVMBuildExtractValue(builder, callee, 1, "");
      newCallee = LLVM.LLVMBuildInsertValue(builder, newCallee, objectPointer, 1, "");
      callee = newCallee;
    }
    realArguments[0] = callee;
    return realArguments;
  }


  /**
   * Builds code to call the specified MethodReference.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param callee - the callee for the method, in a temporary type representation
   * @param calleeType - the current type of the callee
   * @param methodReference - the MethodReference to build the call to
   * @param arguments - the arguments, in temporary type representations of the types that the MethodReference (not the Method) expects
   * @param typeParameterAccessor - the TypeParameterAccessor to get the values of any TypeParameters in the MethodReference or the callee from
   * @return the result of the method call, in a temporary type representation of the type that the MethodReference (not the Method) returns
   */
  public LLVMValueRef buildMethodCall(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef callee, Type calleeType, MethodReference methodReference, LLVMValueRef[] arguments, TypeParameterAccessor typeParameterAccessor)
  {
    Method method = methodReference.getReferencedMember();

    if (!method.isStatic())
    {
      if (methodReference.getContainingType() != null)
      {
        Type newCalleeType = methodReference.getContainingType();
        callee = convertTemporary(builder, landingPadContainer, callee, calleeType, newCalleeType, false, typeParameterAccessor, typeParameterAccessor);
        calleeType = newCalleeType;
      }
      else
      {
        // there is no containing type definition, so assume this is a builtin method
        Type newCalleeType = ((BuiltinMethod) method).getBaseType();
        // since this is a builtin method, it cannot contain type parameters from any context but the MethodReference's context
        // so we can convert it with the same TypeParameterAccessor as in the other case
        callee = convertTemporary(builder, landingPadContainer, callee, calleeType, newCalleeType, false, typeParameterAccessor, typeParameterAccessor);
        calleeType = newCalleeType;
      }
    }

    LLVMValueRef function = codeGenerator.lookupMethodFunction(builder, landingPadContainer, callee, calleeType, methodReference, false, typeParameterAccessor);
    LLVMValueRef[] realArguments;
    int offset;
    if (!method.isStatic() && method.getContainingTypeDefinition() instanceof InterfaceDefinition)
    {
      // interfaces need type argument RTTI blocks to be passed in as parameters
      NamedType containingType = methodReference.getContainingType();
      realArguments = buildInterfaceArgListPreamble(builder, callee, containingType, typeParameterAccessor, arguments.length);

      Type[] typeArguments = containingType.getTypeArguments();
      offset = 1 + (typeArguments == null ? 0 : typeArguments.length);
    }
    else
    {
      realArguments = new LLVMValueRef[1 + arguments.length];
      realArguments[0] = callee;
      offset = 1;
    }
    Type[] referenceParameterTypes = methodReference.getParameterTypes();
    TypeParameterAccessor methodTypeAccessor = typeParameterAccessor;
    if (methodReference.getContainingType() != null)
    {
      if (method.isStatic())
      {
        methodTypeAccessor = new TypeParameterAccessor(builder, rttiHelper);
      }
      else if (method.getContainingTypeDefinition() instanceof InterfaceDefinition)
      {
        methodTypeAccessor = new TypeParameterAccessor(builder, rttiHelper, callee, methodReference.getContainingType(), typeParameterAccessor);
      }
      else
      {
        methodTypeAccessor = new TypeParameterAccessor(builder, this, rttiHelper, method.getContainingTypeDefinition(), callee);
      }
    }
    Parameter[] parameters = method.getParameters();
    for (int i = 0; i < arguments.length; ++i)
    {
      Type realParameterType = parameters[i].getType();
      realArguments[offset + i] = convertTemporaryToStandard(builder, landingPadContainer, arguments[i], referenceParameterTypes[i], realParameterType, typeParameterAccessor, methodTypeAccessor);
    }
    LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "methodInvokeContinue");
    LLVMValueRef result = LLVM.LLVMBuildInvoke(builder, function, C.toNativePointerArray(realArguments, false, true), realArguments.length, invokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);

    if (methodReference.getReturnType() instanceof VoidType)
    {
      return null;
    }
    Type resultType = method.getReturnType();
    result = convertStandardToTemporary(builder, landingPadContainer, result, resultType, methodReference.getReturnType(), methodTypeAccessor, typeParameterAccessor);
    return result;
  }

  /**
   * Builds code to call the specified PropertyReference's getter.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param callee - the callee for the property function, in a temporary type representation
   * @param calleeType - the current type of the callee
   * @param propertyReference - the PropertyReference to build the call to the getter of
   * @param typeParameterAccessor - the TypeParameterAccessor to get the values of any TypeParameters from
   * @return the result of the property getter call, in a temporary type representation of the type that the PropertyReference (not the Property) returns
   */
  public LLVMValueRef buildPropertyGetterFunctionCall(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef callee, Type calleeType, PropertyReference propertyReference, TypeParameterAccessor typeParameterAccessor)
  {
    Property property = propertyReference.getReferencedMember();

    if (!property.isStatic())
    {
      Type newCalleeType = propertyReference.getContainingType();
      // newCalleeType is always non-null, because there is no such thing as a built-in property
      callee = convertTemporary(builder, landingPadContainer, callee, calleeType, newCalleeType, false, typeParameterAccessor, typeParameterAccessor);
      calleeType = newCalleeType;
    }

    LLVMValueRef function = codeGenerator.lookupPropertyFunction(builder, landingPadContainer, callee, calleeType, propertyReference, MemberFunctionType.PROPERTY_GETTER, typeParameterAccessor);
    LLVMValueRef[] realArguments;
    if (!property.isStatic() && property.getContainingTypeDefinition() instanceof InterfaceDefinition)
    {
      // interfaces need type argument RTTI blocks to be passed in as parameters
      NamedType containingType = propertyReference.getContainingType();
      realArguments = buildInterfaceArgListPreamble(builder, callee, containingType, typeParameterAccessor, 0);
    }
    else
    {
      realArguments = new LLVMValueRef[1];
      realArguments[0] = callee;
    }
    TypeParameterAccessor propertyTypeAccessor;
    if (property.isStatic())
    {
      propertyTypeAccessor = new TypeParameterAccessor(builder, rttiHelper);
    }
    else if (property.getContainingTypeDefinition() instanceof InterfaceDefinition)
    {
      propertyTypeAccessor = new TypeParameterAccessor(builder, rttiHelper, callee, propertyReference.getContainingType(), typeParameterAccessor);
    }
    else
    {
      propertyTypeAccessor = new TypeParameterAccessor(builder, this, rttiHelper, property.getContainingTypeDefinition(), callee);
    }
    LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "propertyGetterInvokeContinue");
    LLVMValueRef result = LLVM.LLVMBuildInvoke(builder, function, C.toNativePointerArray(realArguments, false, true), realArguments.length, invokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);

    Type resultType = property.getType();
    result = convertStandardToTemporary(builder, landingPadContainer, result, resultType, propertyReference.getType(), propertyTypeAccessor, typeParameterAccessor);
    return result;
  }

  /**
   * Builds code to call the specified PropertyReference's setter or constructor.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param callee - the callee for the property function, in a temporary type representation
   * @param calleeType - the current type of the callee
   * @param argument - the value to pass to the setter/constructor, in a temporary type representation
   * @param propertyReference - the PropertyReference to build the call to the setter/constructor of
   * @param functionType - the type of function to call, should be either PROPERTY_SETTER or PROPERTY_CONSTRUCTOR
   * @param typeParameterAccessor - the TypeParameterAccessor to get the values of any TypeParameters from
   */
  public void buildPropertySetterConstructorFunctionCall(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef callee, Type calleeType, LLVMValueRef argument, PropertyReference propertyReference, MemberFunctionType functionType, TypeParameterAccessor typeParameterAccessor)
  {
    Property property = propertyReference.getReferencedMember();

    if (!property.isStatic())
    {
      Type newCalleeType = propertyReference.getContainingType();
      // newCalleeType is always non-null, because there is no such thing as a built-in property
      callee = convertTemporary(builder, landingPadContainer, callee, calleeType, newCalleeType, false, typeParameterAccessor, typeParameterAccessor);
      calleeType = newCalleeType;
    }

    LLVMValueRef function = codeGenerator.lookupPropertyFunction(builder, landingPadContainer, callee, calleeType, propertyReference, functionType, typeParameterAccessor);
    LLVMValueRef[] realArguments;
    int offset;
    if (!property.isStatic() && property.getContainingTypeDefinition() instanceof InterfaceDefinition)
    {
      // interfaces need type argument RTTI blocks to be passed in as parameters
      NamedType containingType = propertyReference.getContainingType();
      realArguments = buildInterfaceArgListPreamble(builder, callee, containingType, typeParameterAccessor, 1);

      Type[] typeArguments = containingType.getTypeArguments();
      offset = 1 + (typeArguments == null ? 0 : typeArguments.length);
    }
    else
    {
      realArguments = new LLVMValueRef[2];
      realArguments[0] = callee;
      offset = 1;
    }
    TypeParameterAccessor propertyTypeAccessor;
    if (property.isStatic())
    {
      propertyTypeAccessor = new TypeParameterAccessor(builder, rttiHelper);
    }
    else if (property.getContainingTypeDefinition() instanceof InterfaceDefinition)
    {
      propertyTypeAccessor = new TypeParameterAccessor(builder, rttiHelper, callee, propertyReference.getContainingType(), typeParameterAccessor);
    }
    else
    {
      propertyTypeAccessor = new TypeParameterAccessor(builder, this, rttiHelper, property.getContainingTypeDefinition(), callee);
    }

    // convert the argument from the PropertyReference's expected type to the Property's expected type
    Type argumentType = property.getType();
    realArguments[offset] = convertTemporaryToStandard(builder, landingPadContainer, argument, propertyReference.getType(), argumentType, typeParameterAccessor, propertyTypeAccessor);

    LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "property" + (functionType == MemberFunctionType.PROPERTY_CONSTRUCTOR ? "Constructor" : "Setter") + "InvokeContinue");
    LLVM.LLVMBuildInvoke(builder, function, C.toNativePointerArray(realArguments, false, true), realArguments.length, invokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);
  }

  /**
   * Gets a function that takes a callee of type 'object' and converts it to the base type of the specified method before calling that method.
   * @param method - the method to find the base change method for, cannot be an interface method
   * @return the base change method for the specified method
   */
  public LLVMValueRef getBaseChangeFunction(Method method)
  {
    if (method.isStatic())
    {
      throw new IllegalArgumentException("Cannot change the base of a static method");
    }
    if (method.getContainingTypeDefinition() instanceof InterfaceDefinition)
    {
      throw new IllegalArgumentException("Cannot change the base of an interface method");
    }
    String mangledName = BASE_CHANGE_FUNCTION_PREFIX + method.getMangledName();

    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    Parameter[] parameters = method.getParameters();
    LLVMTypeRef[] types = new LLVMTypeRef[1 + parameters.length];
    ObjectType objectType = new ObjectType(false, false, null);
    types[0] = findTemporaryType(objectType);
    for (int i = 0; i < parameters.length; ++i)
    {
      types[i + 1] = findStandardType(parameters[i].getType());
    }
    LLVMTypeRef resultType = findStandardType(method.getReturnType());
    LLVMTypeRef objectFunctionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);

    LLVMValueRef objectFunction = LLVM.LLVMAddFunction(module, mangledName, objectFunctionType);
    LLVM.LLVMSetLinkage(objectFunction, LLVM.LLVMLinkage.LLVMLinkOnceODRLinkage);
    LLVM.LLVMSetVisibility(objectFunction, LLVM.LLVMVisibility.LLVMHiddenVisibility);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(objectFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    Type baseType;
    if (method.getContainingTypeDefinition() != null)
    {
      baseType = new NamedType(false, false, false, method.getContainingTypeDefinition());
    }
    else if (method instanceof BuiltinMethod)
    {
      baseType = ((BuiltinMethod) method).getBaseType();
    }
    else
    {
      throw new IllegalArgumentException("Method has no base type: " + method);
    }
    TypeParameterAccessor nullAccessor = new TypeParameterAccessor(builder, rttiHelper);
    LLVMValueRef callee = LLVM.LLVMGetParam(objectFunction, 0);
    // disable checks for this conversion, since (a) we know the the object contains exactly the type we are expecting, and (b) we don't have the TypeParameterAccessor to do the instanceof check with
    LLVMValueRef convertedBaseValue = convertTemporary(builder, landingPadContainer, callee, objectType, baseType, true, nullAccessor, nullAccessor);

    MethodReference methodReference = new MethodReference(method, GenericTypeSpecialiser.IDENTITY_SPECIALISER);
    LLVMValueRef methodFunction = codeGenerator.lookupMethodFunction(builder, landingPadContainer, convertedBaseValue, baseType, methodReference, false, null);
    LLVMValueRef[] arguments = new LLVMValueRef[1 + parameters.length];
    arguments[0] = convertedBaseValue;
    for (int i = 0; i < parameters.length; ++i)
    {
      arguments[i + 1] = LLVM.LLVMGetParam(objectFunction, i + 1);
    }
    LLVMBasicBlockRef methodInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "methodInvokeContinue");
    LLVMValueRef result = LLVM.LLVMBuildInvoke(builder, methodFunction, C.toNativePointerArray(arguments, false, true), arguments.length, methodInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, methodInvokeContinueBlock);
    if (method.getReturnType() instanceof VoidType)
    {
      LLVM.LLVMBuildRetVoid(builder);
    }
    else
    {
      LLVM.LLVMBuildRet(builder, result);
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return objectFunction;
  }

  /**
   * Initialises the specified value as a compound definition of the specified type.
   * This method performs any initialisation which must happen before the constructor is called, such as zeroing fields which have default values, and setting the type parameters.
   * @param compoundType - the compound type to initialise the value as
   * @param compoundValue - the value to initialise, which is a temporary type representation of the specified CompoundDefinition
   */
  void initialiseCompoundType(LLVMBuilderRef builder, NamedType compoundType, LLVMValueRef compoundValue, TypeParameterAccessor typeParameterAccessor)
  {
    CompoundDefinition compoundDefinition = (CompoundDefinition) compoundType.getResolvedTypeDefinition();
    TypeParameter[] typeParameters = compoundDefinition.getTypeParameters();
    Type[] typeArguments = compoundType.getTypeArguments();
    if (typeArguments == null ? typeParameters.length > 0 : (typeArguments.length != typeParameters.length))
    {
      throw new IllegalArgumentException("Cannot initialise a compound type without the required type arguments");
    }
    // initialise the type parameters (this must be done during allocation, not in the constructor)
    for (int i = 0; typeArguments != null && i < typeArguments.length; ++i)
    {
      LLVMValueRef pointer = getTypeParameterPointer(builder, compoundValue, typeParameters[i]);
      LLVMValueRef rtti = rttiHelper.buildRTTICreation(builder, typeArguments[i], typeParameterAccessor);
      LLVM.LLVMBuildStore(builder, rtti, pointer);
    }
    // initialise all of the fields which have default values to zero/null
    for (MemberVariable variable : compoundDefinition.getMemberVariables())
    {
      if (variable.getType().hasDefaultValue())
      {
        LLVMValueRef pointer = getMemberPointer(builder, compoundValue, variable);
        LLVM.LLVMBuildStore(builder, LLVM.LLVMConstNull(findStandardType(variable.getType())), pointer);
      }
    }
  }

  /**
   * Builds a conversion from one function type to another, through a proxy function.
   * @param builder - the LLVMBuilderRef to build the conversion with
   * @param value - the function to convert, in a temporary type representation
   * @param fromType - the function type to convert from
   * @param toType - the function type to convert to
   * @param fromAccessor - the TypeParameterAccessor containing the values of all TypeParameters that may exist in fromType
   * @param toAccessor - the TypeParameterAccessor containing the values of all TypeParameters that may exist in toType
   * @return the proxied function, in a temporary type representation
   */
  private LLVMValueRef buildFunctionProxyConversion(LLVMBuilderRef builder, LLVMValueRef value, FunctionType fromType, FunctionType toType, TypeParameterAccessor fromAccessor, TypeParameterAccessor toAccessor)
  {
    // make sure this function type's RTTI doesn't reference any type parameters
    // otherwise, the RTTI would turn out wrong (due to typeParameterAccessor) and people would be able to cast
    // from object to a function of whatever the values of those type parameters are,
    // even though the function's native representation might not take whatever type those type parameters happen to be
    LLVMValueRef rtti = rttiHelper.buildRTTICreation(builder, stripTypeParameters(toType), toAccessor);

    TypeDefinition fromMapperTypeDefinition = fromAccessor.getTypeDefinition();
    TypeDefinition toMapperTypeDefinition = toAccessor.getTypeDefinition();

    LLVMValueRef proxyFunction = getProxyFunction(fromType, toType, fromMapperTypeDefinition, toMapperTypeDefinition);

    LLVMTypeRef fromNativeType = findStandardType(fromType);
    LLVMTypeRef fromTypeMapperType = rttiHelper.getTypeArgumentMapperType(fromMapperTypeDefinition == null ? 0 : fromMapperTypeDefinition.getTypeParameters().length);
    LLVMTypeRef toTypeMapperType = rttiHelper.getTypeArgumentMapperType(toMapperTypeDefinition == null ? 0 : toMapperTypeDefinition.getTypeParameters().length);
    LLVMTypeRef[] opaquePointerSubTypes = new LLVMTypeRef[] {fromNativeType, fromTypeMapperType, toTypeMapperType};
    LLVMTypeRef allocationType = LLVM.LLVMStructType(C.toNativePointerArray(opaquePointerSubTypes, false, true), opaquePointerSubTypes.length, false);
    LLVMTypeRef allocationPtrType = LLVM.LLVMPointerType(allocationType, 0);

    LLVMValueRef pointer = codeGenerator.buildHeapAllocation(builder, allocationPtrType);

    LLVMValueRef functionValuePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 0, "");
    LLVMValueRef functionValue = convertTemporaryToStandard(builder, value, fromType);
    LLVM.LLVMBuildStore(builder, functionValue, functionValuePtr);
    LLVMValueRef fromTypeMapperPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 1, "");
    fromAccessor.buildTypeArgumentMapper(fromTypeMapperPtr);
    LLVMValueRef toTypeMapperPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 2, "");
    toAccessor.buildTypeArgumentMapper(toTypeMapperPtr);

    LLVMValueRef callee = LLVM.LLVMBuildBitCast(builder, pointer, getOpaquePointer(), "");

    LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(toType));
    result = LLVM.LLVMBuildInsertValue(builder, result, rtti, 0, "");
    result = LLVM.LLVMBuildInsertValue(builder, result, callee, 1, "");
    result = LLVM.LLVMBuildInsertValue(builder, result, proxyFunction, 2, "");
    return result;
  }

  /**
   * Builds a proxy function that allows one function type to be converted into another.
   * @param fromType - the type of function that the proxy function will call down into
   * @param toType - the type of function that the proxy function will be
   * @param fromMapperTypeDefinition - the TypeDefinition that the type mapper for the from type is based on
   * @param toMapperTypeDefinition - the TypeDefinition that the type mapper for the to type is based on
   * @return the proxy function created
   */
  private LLVMValueRef getProxyFunction(FunctionType fromType, FunctionType toType, TypeDefinition fromMapperTypeDefinition, TypeDefinition toMapperTypeDefinition)
  {
    // mangled name and type mappers:
    // because the ABI of this function depends on which type mappers are used for it, regardless of whether or not they are used, we must include the size of each of the type mappers in the mangled name
    // otherwise we could try to generate a similar proxy function later on, and find one with the same mangled name that has a slightly different ABI, which would cause things to break
    int fromMapperNumTypeParams = fromMapperTypeDefinition == null ? 0 : fromMapperTypeDefinition.getTypeParameters().length;
    int toMapperNumTypeParams = toMapperTypeDefinition == null ? 0 : toMapperTypeDefinition.getTypeParameters().length;
    String mangledName = PROXY_FUNCTION_PREFIX + typeDefinition.getQualifiedName().getMangledName() + "_" + fromType.getMangledName() + "_" + toType.getMangledName() + "_" + fromMapperNumTypeParams + "_" + toMapperNumTypeParams;
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    Type[] fromParamTypes = fromType.getParameterTypes();
    Type[] toParamTypes = toType.getParameterTypes();

    if (fromParamTypes.length > toParamTypes.length)
    {
      throw new IllegalArgumentException("Cannot convert from " + fromType + " to " + toType + " - not enough parameter types");
    }
    if ((fromType.getReturnType() instanceof VoidType) && !(toType.getReturnType() instanceof VoidType))
    {
      throw new IllegalArgumentException("Cannot convert from " + fromType + " to " + toType + " - return types conflict");
    }

    LLVMTypeRef[] parameterTypes = new LLVMTypeRef[1 + toParamTypes.length];
    parameterTypes[0] = getOpaquePointer();
    for (int i = 0; i < toParamTypes.length; ++i)
    {
      parameterTypes[1 + i] = findStandardType(toParamTypes[i]);
    }
    LLVMTypeRef returnType = findStandardType(toType.getReturnType());

    LLVMTypeRef proxyFunctionType = LLVM.LLVMFunctionType(returnType, C.toNativePointerArray(parameterTypes, false, true), parameterTypes.length, false);
    LLVMValueRef proxyFunction = LLVM.LLVMAddFunction(module, mangledName, proxyFunctionType);
    LLVM.LLVMSetLinkage(proxyFunction, LLVM.LLVMLinkage.LLVMPrivateLinkage);
    LLVM.LLVMSetVisibility(proxyFunction, LLVM.LLVMVisibility.LLVMHiddenVisibility);


    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(proxyFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    LLVMTypeRef fromNativeType = findStandardType(fromType);
    LLVMTypeRef fromTypeMapperType = rttiHelper.getTypeArgumentMapperType(fromMapperNumTypeParams);
    LLVMTypeRef toTypeMapperType = rttiHelper.getTypeArgumentMapperType(toMapperNumTypeParams);
    LLVMTypeRef[] subTypes = new LLVMTypeRef[] {fromNativeType, fromTypeMapperType, toTypeMapperType};
    LLVMTypeRef opaqueValueType = LLVM.LLVMStructType(C.toNativePointerArray(subTypes, false, true), subTypes.length, false);
    LLVMTypeRef opaquePointerType = LLVM.LLVMPointerType(opaqueValueType, 0);

    LLVMValueRef opaquePointer = LLVM.LLVMGetParam(proxyFunction, 0);
    LLVMValueRef calleeValue = LLVM.LLVMBuildBitCast(builder, opaquePointer, opaquePointerType, "");
    LLVMValueRef baseFunctionStructurePtr = LLVM.LLVMBuildStructGEP(builder, calleeValue, 0, "");
    LLVMValueRef fromTypeMapper = LLVM.LLVMBuildStructGEP(builder, calleeValue, 1, "");
    LLVMValueRef toTypeMapper = LLVM.LLVMBuildStructGEP(builder, calleeValue, 2, "");

    TypeParameterAccessor fromAccessor = new TypeParameterAccessor(builder, rttiHelper, fromMapperTypeDefinition, fromTypeMapper);
    TypeParameterAccessor toAccessor = new TypeParameterAccessor(builder, rttiHelper, toMapperTypeDefinition, toTypeMapper);

    LLVMValueRef[] baseFunctionParameters = new LLVMValueRef[1 + fromParamTypes.length];
    for (int i = 0; i < fromParamTypes.length; ++i)
    {
      LLVMValueRef parameter = LLVM.LLVMGetParam(proxyFunction, 1 + i);
      if (toParamTypes[i].isRuntimeEquivalent(fromParamTypes[i]))
      {
        baseFunctionParameters[i + 1] = parameter;
      }
      else
      {
        LLVMValueRef converted = convertStandardToTemporary(builder, landingPadContainer, parameter, toParamTypes[i], fromParamTypes[i], toAccessor, fromAccessor);
        converted = convertTemporaryToStandard(builder, converted, fromParamTypes[i]);
        baseFunctionParameters[i + 1] = converted;
      }
    }

    LLVMValueRef baseFunctionCalleePtr = LLVM.LLVMBuildStructGEP(builder, baseFunctionStructurePtr, 1, "");
    LLVMValueRef baseFunctionCallee = LLVM.LLVMBuildLoad(builder, baseFunctionCalleePtr, "");
    baseFunctionParameters[0] = baseFunctionCallee;

    LLVMValueRef baseFunctionPtr = LLVM.LLVMBuildStructGEP(builder, baseFunctionStructurePtr, 2, "");
    LLVMValueRef baseFunction = LLVM.LLVMBuildLoad(builder, baseFunctionPtr, "");

    LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "invokeContinue");
    LLVMValueRef result = LLVM.LLVMBuildInvoke(builder, baseFunction, C.toNativePointerArray(baseFunctionParameters, false, true), baseFunctionParameters.length, invokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");

    LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);

    if (toType.getReturnType() instanceof VoidType)
    {
      LLVM.LLVMBuildRetVoid(builder);
    }
    else
    {
      LLVMValueRef convertedResult = convertStandardToTemporary(builder, landingPadContainer, result, fromType.getReturnType(), toType.getReturnType(), fromAccessor, toAccessor);
      convertedResult = convertTemporaryToStandard(builder, convertedResult, toType.getReturnType());
      LLVM.LLVMBuildRet(builder, convertedResult);
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return proxyFunction;
  }

  /**
   * Builds a conversion from one array type to another, through a proxy array.
   * @param builder - the LLVMBuilderRef to build the conversion with
   * @param value - the array to convert, in a temporary type representation
   * @param fromType - the array type to convert from
   * @param toType - the array type to convert to - any type parameters inside this type are considered to just be objects (i.e. they are stripped out before use)
   * @param fromAccessor - the TypeParameterAccessor containing the values of all TypeParameters that may exist in fromType
   * @param toAccessor - the TypeParameterAccessor containing the values of all TypeParameters that may exist in toType
   * @return the proxied array, in a temporary type representation
   */
  private LLVMValueRef buildProxyArrayConversion(LLVMBuilderRef builder, LLVMValueRef value, ArrayType fromType, ArrayType toType, TypeParameterAccessor fromAccessor, TypeParameterAccessor toAccessor)
  {
    // we need to create the array as if any type parameters inside it are actually objects, so strip them
    toType = (ArrayType) stripTypeParameters(toType);

    // find the type of the proxy array
    LLVMTypeRef llvmArrayType = findStandardType(toType);
    LLVMTypeRef rttiType = rttiHelper.getGenericRTTIType();
    LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(virtualFunctionHandler.getObjectVFTType(), 0);
    LLVMTypeRef lengthType = LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount());

    LLVMTypeRef[] getFunctionParamTypes = new LLVMTypeRef[] {llvmArrayType, lengthType};
    LLVMTypeRef getFunctionType = LLVM.LLVMFunctionType(findTemporaryType(toType.getBaseType()), C.toNativePointerArray(getFunctionParamTypes, false, true), getFunctionParamTypes.length, false);
    LLVMTypeRef[] setFunctionParamTypes = new LLVMTypeRef[] {llvmArrayType, lengthType, findStandardType(toType.getBaseType())};
    LLVMTypeRef setFunctionType = LLVM.LLVMFunctionType(LLVM.LLVMVoidType(), C.toNativePointerArray(setFunctionParamTypes, false, true), setFunctionParamTypes.length, false);

    LLVMTypeRef baseArrayType = findStandardType(fromType);

    TypeDefinition fromMapperTypeDefinition = fromAccessor.getTypeDefinition();
    TypeDefinition toMapperTypeDefinition = toAccessor.getTypeDefinition();

    LLVMTypeRef fromTypeMapperType = rttiHelper.getTypeArgumentMapperType(fromMapperTypeDefinition == null ? 0 : fromMapperTypeDefinition.getTypeParameters().length);
    LLVMTypeRef toTypeMapperType = rttiHelper.getTypeArgumentMapperType(toMapperTypeDefinition == null ? 0 : toMapperTypeDefinition.getTypeParameters().length);

    LLVMTypeRef[] subTypes = new LLVMTypeRef[] {rttiType, vftPointerType, lengthType, LLVM.LLVMPointerType(getFunctionType, 0), LLVM.LLVMPointerType(setFunctionType, 0), baseArrayType, fromTypeMapperType, toTypeMapperType};
    LLVMTypeRef proxyArrayType = LLVM.LLVMStructType(C.toNativePointerArray(subTypes, false, true), subTypes.length, false);
    LLVMTypeRef proxyArrayPointerType = LLVM.LLVMPointerType(proxyArrayType, 0);

    // allocate memory for the proxy array
    LLVMValueRef pointer = codeGenerator.buildHeapAllocation(builder, proxyArrayPointerType);

    // store the RTTI
    LLVMValueRef rtti = rttiHelper.buildRTTICreation(builder, toType, toAccessor);
    LLVMValueRef rttiPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 0, "");
    LLVM.LLVMBuildStore(builder, rtti, rttiPtr);

    // store the VFT
    LLVMValueRef vft = virtualFunctionHandler.getBaseChangeObjectVFT(toType);
    LLVMValueRef vftPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 1, "");
    LLVM.LLVMBuildStore(builder, vft, vftPtr);

    // extract the length from the underlying array, and store it here
    LLVMValueRef baseLengthPtr = getArrayLengthPointer(builder, value);
    LLVMValueRef length = LLVM.LLVMBuildLoad(builder, baseLengthPtr, "");
    LLVMValueRef lengthPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 2, "");
    LLVM.LLVMBuildStore(builder, length, lengthPtr);

    // store the getter and setter functions
    LLVMValueRef getterFunction = getProxyArrayGetterFunction(fromType, toType, fromMapperTypeDefinition, toMapperTypeDefinition);
    LLVMValueRef getterFunctionPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 3, "");
    LLVM.LLVMBuildStore(builder, getterFunction, getterFunctionPtr);
    LLVMValueRef setterFunction = getProxyArraySetterFunction(fromType, toType, fromMapperTypeDefinition, toMapperTypeDefinition);
    LLVMValueRef setterFunctionPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 4, "");
    LLVM.LLVMBuildStore(builder, setterFunction, setterFunctionPtr);

    // store the base array
    LLVMValueRef baseArrayPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 5, "");
    LLVM.LLVMBuildStore(builder, value, baseArrayPtr);

    // store the type mappers
    LLVMValueRef fromTypeMapper = LLVM.LLVMBuildStructGEP(builder, pointer, 6, "");
    fromAccessor.buildTypeArgumentMapper(fromTypeMapper);
    LLVMValueRef toTypeMapper = LLVM.LLVMBuildStructGEP(builder, pointer, 7, "");
    toAccessor.buildTypeArgumentMapper(toTypeMapper);

    // bitcast the result to the normal array type and return it
    return LLVM.LLVMBuildBitCast(builder, pointer, findTemporaryType(toType), "");
  }

  /**
   * Builds a getter function for a proxy array, which along with a setter function allows one array type to be converted into another.
   * @param fromType - the type of array that this function will call the getter of
   * @param toType - the type of array that this function will be for
   * @param fromMapperTypeDefinition - the TypeDefinition that the type mapper for the from type is based on
   * @param toMapperTypeDefinition - the TypeDefinition that the type mapper for the to type is based on
   * @return the proxy getter function created
   */
  private LLVMValueRef getProxyArrayGetterFunction(ArrayType fromType, ArrayType toType, TypeDefinition fromMapperTypeDefinition, TypeDefinition toMapperTypeDefinition)
  {
    // mangled name and type mappers:
    // because the ABI of this function depends on which type mappers are used for it, regardless of whether or not they are used, we must include the size of each of the type mappers in the mangled name
    // otherwise we could try to generate a similar proxy function later on, and find one with the same mangled name that has a slightly different ABI, which would cause things to break
    int fromMapperNumTypeParams = fromMapperTypeDefinition == null ? 0 : fromMapperTypeDefinition.getTypeParameters().length;
    int toMapperNumTypeParams = toMapperTypeDefinition == null ? 0 : toMapperTypeDefinition.getTypeParameters().length;
    String mangledName = PROXY_ARRAY_GETTER_FUNCTION_PREFIX + typeDefinition.getQualifiedName().getMangledName() + "_" + fromType.getMangledName() + "_" + toType.getMangledName() + "_" + fromMapperNumTypeParams + "_" + toMapperNumTypeParams;
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    LLVMTypeRef toArrayNativeType = findTemporaryType(toType);
    LLVMTypeRef indexType = LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount());

    LLVMTypeRef[] getterFunctionParamTypes = new LLVMTypeRef[] {toArrayNativeType, indexType};
    LLVMTypeRef getterFunctionType = LLVM.LLVMFunctionType(findTemporaryType(toType.getBaseType()), C.toNativePointerArray(getterFunctionParamTypes, false, true), getterFunctionParamTypes.length, false);

    LLVMValueRef proxyFunction = LLVM.LLVMAddFunction(module, mangledName, getterFunctionType);
    LLVM.LLVMSetLinkage(proxyFunction, LLVM.LLVMLinkage.LLVMPrivateLinkage);
    LLVM.LLVMSetVisibility(proxyFunction, LLVM.LLVMVisibility.LLVMHiddenVisibility);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(proxyFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    LLVMValueRef array = LLVM.LLVMGetParam(proxyFunction, 0);
    LLVMValueRef index = LLVM.LLVMGetParam(proxyFunction, 1);

    // find the real type of the proxy array
    LLVMTypeRef llvmArrayType = findStandardType(toType);
    LLVMTypeRef rttiType = rttiHelper.getGenericRTTIType();
    LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(virtualFunctionHandler.getObjectVFTType(), 0);
    LLVMTypeRef lengthType = LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount());

    LLVMTypeRef[] getFunctionParamTypes = new LLVMTypeRef[] {llvmArrayType, lengthType};
    LLVMTypeRef getFunctionType = LLVM.LLVMFunctionType(findTemporaryType(toType.getBaseType()), C.toNativePointerArray(getFunctionParamTypes, false, true), getFunctionParamTypes.length, false);
    LLVMTypeRef[] setFunctionParamTypes = new LLVMTypeRef[] {llvmArrayType, lengthType, findStandardType(toType.getBaseType())};
    LLVMTypeRef setFunctionType = LLVM.LLVMFunctionType(LLVM.LLVMVoidType(), C.toNativePointerArray(setFunctionParamTypes, false, true), setFunctionParamTypes.length, false);

    LLVMTypeRef baseArrayType = findStandardType(fromType);
    LLVMTypeRef fromTypeMapperType = rttiHelper.getTypeArgumentMapperType(fromMapperNumTypeParams);
    LLVMTypeRef toTypeMapperType = rttiHelper.getTypeArgumentMapperType(toMapperNumTypeParams);

    LLVMTypeRef[] realArraySubTypes = new LLVMTypeRef[] {rttiType, vftPointerType, lengthType, LLVM.LLVMPointerType(getFunctionType, 0), LLVM.LLVMPointerType(setFunctionType, 0), baseArrayType, fromTypeMapperType, toTypeMapperType};
    LLVMTypeRef realArrayType = LLVM.LLVMPointerType(LLVM.LLVMStructType(C.toNativePointerArray(realArraySubTypes, false, true), realArraySubTypes.length, false), 0);
    array = LLVM.LLVMBuildBitCast(builder, array, realArrayType, "");

    // extract the required values from the specialised proxy array
    LLVMValueRef baseArrayPtr = LLVM.LLVMBuildStructGEP(builder, array, 5, "");
    LLVMValueRef baseArray = LLVM.LLVMBuildLoad(builder, baseArrayPtr, "");

    LLVMValueRef fromTypeMapper = LLVM.LLVMBuildStructGEP(builder, array, 6, "");
    LLVMValueRef toTypeMapper = LLVM.LLVMBuildStructGEP(builder, array, 7, "");

    TypeParameterAccessor fromAccessor = new TypeParameterAccessor(builder, rttiHelper, fromMapperTypeDefinition, fromTypeMapper);
    TypeParameterAccessor toAccessor = new TypeParameterAccessor(builder, rttiHelper, toMapperTypeDefinition, toTypeMapper);

    // call the base array's getter
    LLVMValueRef retrievedElement = buildRetrieveArrayElement(builder, landingPadContainer, baseArray, index);

    // convert the element to the base type of toType
    LLVMValueRef convertedElement = convertTemporary(builder, landingPadContainer, retrievedElement, fromType.getBaseType(), toType.getBaseType(), false, fromAccessor, toAccessor);

    // return the converted value
    LLVM.LLVMBuildRet(builder, convertedElement);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return proxyFunction;
  }

  /**
   * Builds a setter function for a proxy array, which along with a getter function allows one array type to be converted into another.
   * @param fromType - the type of array that this function will call the setter of
   * @param toType - the type of array that this function will be for
   * @param fromMapperTypeDefinition - the TypeDefinition that the type mapper for the from type is based on
   * @param toMapperTypeDefinition - the TypeDefinition that the type mapper for the to type is based on
   * @return the proxy setter function created
   */
  private LLVMValueRef getProxyArraySetterFunction(ArrayType fromType, ArrayType toType, TypeDefinition fromMapperTypeDefinition, TypeDefinition toMapperTypeDefinition)
  {
    // mangled name and type mappers:
    // because the ABI of this function depends on which type mappers are used for it, regardless of whether or not they are used, we must include the size of each of the type mappers in the mangled name
    // otherwise we could try to generate a similar proxy function later on, and find one with the same mangled name that has a slightly different ABI, which would cause things to break
    int fromMapperNumTypeParams = fromMapperTypeDefinition == null ? 0 : fromMapperTypeDefinition.getTypeParameters().length;
    int toMapperNumTypeParams = toMapperTypeDefinition == null ? 0 : toMapperTypeDefinition.getTypeParameters().length;
    String mangledName = PROXY_ARRAY_SETTER_FUNCTION_PREFIX + typeDefinition.getQualifiedName().getMangledName() + "_" + fromType.getMangledName() + "_" + toType.getMangledName() + "_" + fromMapperNumTypeParams + "_" + toMapperNumTypeParams;
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    LLVMTypeRef toArrayNativeType = findTemporaryType(toType);
    LLVMTypeRef indexType = LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount());

    LLVMTypeRef[] setterFunctionParamTypes = new LLVMTypeRef[] {toArrayNativeType, indexType, findStandardType(toType.getBaseType())};
    LLVMTypeRef setterFunctionType = LLVM.LLVMFunctionType(LLVM.LLVMVoidType(), C.toNativePointerArray(setterFunctionParamTypes, false, true), setterFunctionParamTypes.length, false);

    LLVMValueRef proxyFunction = LLVM.LLVMAddFunction(module, mangledName, setterFunctionType);
    LLVM.LLVMSetLinkage(proxyFunction, LLVM.LLVMLinkage.LLVMPrivateLinkage);
    LLVM.LLVMSetVisibility(proxyFunction, LLVM.LLVMVisibility.LLVMHiddenVisibility);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(proxyFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    LLVMValueRef array = LLVM.LLVMGetParam(proxyFunction, 0);
    LLVMValueRef index = LLVM.LLVMGetParam(proxyFunction, 1);
    LLVMValueRef value = LLVM.LLVMGetParam(proxyFunction, 2);

    // find the real type of the proxy array
    LLVMTypeRef llvmArrayType = findStandardType(toType);
    LLVMTypeRef rttiType = rttiHelper.getGenericRTTIType();
    LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(virtualFunctionHandler.getObjectVFTType(), 0);
    LLVMTypeRef lengthType = LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount());

    LLVMTypeRef[] getFunctionParamTypes = new LLVMTypeRef[] {llvmArrayType, lengthType};
    LLVMTypeRef getFunctionType = LLVM.LLVMFunctionType(findTemporaryType(toType.getBaseType()), C.toNativePointerArray(getFunctionParamTypes, false, true), getFunctionParamTypes.length, false);
    LLVMTypeRef[] setFunctionParamTypes = new LLVMTypeRef[] {llvmArrayType, lengthType, findStandardType(toType.getBaseType())};
    LLVMTypeRef setFunctionType = LLVM.LLVMFunctionType(LLVM.LLVMVoidType(), C.toNativePointerArray(setFunctionParamTypes, false, true), setFunctionParamTypes.length, false);

    LLVMTypeRef baseArrayType = findStandardType(fromType);
    LLVMTypeRef fromTypeMapperType = rttiHelper.getTypeArgumentMapperType(fromMapperNumTypeParams);
    LLVMTypeRef toTypeMapperType = rttiHelper.getTypeArgumentMapperType(toMapperNumTypeParams);

    LLVMTypeRef[] realArraySubTypes = new LLVMTypeRef[] {rttiType, vftPointerType, lengthType, LLVM.LLVMPointerType(getFunctionType, 0), LLVM.LLVMPointerType(setFunctionType, 0), baseArrayType, fromTypeMapperType, toTypeMapperType};
    LLVMTypeRef realArrayType = LLVM.LLVMPointerType(LLVM.LLVMStructType(C.toNativePointerArray(realArraySubTypes, false, true), realArraySubTypes.length, false), 0);
    array = LLVM.LLVMBuildBitCast(builder, array, realArrayType, "");

    // extract the required values from the specialised proxy array
    LLVMValueRef baseArrayPtr = LLVM.LLVMBuildStructGEP(builder, array, 5, "");
    LLVMValueRef baseArray = LLVM.LLVMBuildLoad(builder, baseArrayPtr, "");

    LLVMValueRef fromTypeMapper = LLVM.LLVMBuildStructGEP(builder, array, 6, "");
    LLVMValueRef toTypeMapper = LLVM.LLVMBuildStructGEP(builder, array, 7, "");

    TypeParameterAccessor fromAccessor = new TypeParameterAccessor(builder, rttiHelper, fromMapperTypeDefinition, fromTypeMapper);
    TypeParameterAccessor toAccessor = new TypeParameterAccessor(builder, rttiHelper, toMapperTypeDefinition, toTypeMapper);

    // convert the value to the base array's type
    LLVMValueRef convertedValue = convertStandardToTemporary(builder, landingPadContainer, value, toType.getBaseType(), fromType.getBaseType(), toAccessor, fromAccessor);
    convertedValue = convertTemporaryToStandard(builder, convertedValue, fromType.getBaseType());

    // call the base array's setter
    buildStoreArrayElement(builder, landingPadContainer, baseArray, index, convertedValue);

    LLVM.LLVMBuildRetVoid(builder);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return proxyFunction;
  }

  /**
   * Gets the non-proxied array getter function for the specified ArrayType.
   * @param arrayType - the type of array to get the getter function for
   * @return the getter function for the specified array type
   */
  public LLVMValueRef getArrayGetterFunction(ArrayType arrayType)
  {
    String mangledName = ARRAY_GETTER_FUNCTION_PREFIX + arrayType.getMangledName();
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    LLVMTypeRef llvmArrayType = findTemporaryType(arrayType);
    LLVMTypeRef indexType = LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount());

    LLVMTypeRef[] getterFunctionParamTypes = new LLVMTypeRef[] {llvmArrayType, indexType};
    LLVMTypeRef getterFunctionType = LLVM.LLVMFunctionType(findTemporaryType(arrayType.getBaseType()), C.toNativePointerArray(getterFunctionParamTypes, false, true), getterFunctionParamTypes.length, false);

    LLVMValueRef function = LLVM.LLVMAddFunction(module, mangledName, getterFunctionType);
    LLVM.LLVMSetLinkage(function, LLVM.LLVMLinkage.LLVMLinkOnceODRLinkage);
    LLVM.LLVMSetVisibility(function, LLVM.LLVMVisibility.LLVMHiddenVisibility);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(function);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    LLVMValueRef array = LLVM.LLVMGetParam(function, 0);
    LLVMValueRef index = LLVM.LLVMGetParam(function, 1);

    LLVMTypeRef realArrayType = LLVM.LLVMPointerType(findNonProxiedArrayStructureType(arrayType, 0), 0);
    array = LLVM.LLVMBuildBitCast(builder, array, realArrayType, "");

    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false),
                                                 index};
    LLVMValueRef elementPointer = LLVM.LLVMBuildGEP(builder, array, C.toNativePointerArray(indices, false, true), indices.length, "");
    LLVMValueRef element = convertStandardPointerToTemporary(builder, elementPointer, arrayType.getBaseType());

    LLVM.LLVMBuildRet(builder, element);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return function;
  }

  /**
   * Gets the non-proxied array setter function for the specified ArrayType.
   * @param arrayType - the type of array to get the setter function for
   * @return the setter function for the specified array type
   */
  public LLVMValueRef getArraySetterFunction(ArrayType arrayType)
  {
    String mangledName = ARRAY_SETTER_FUNCTION_PREFIX + arrayType.getMangledName();
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    LLVMTypeRef llvmArrayType = findTemporaryType(arrayType);
    LLVMTypeRef indexType = LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount());

    LLVMTypeRef[] setterFunctionParamTypes = new LLVMTypeRef[] {llvmArrayType, indexType, findStandardType(arrayType.getBaseType())};
    LLVMTypeRef setterFunctionType = LLVM.LLVMFunctionType(LLVM.LLVMVoidType(), C.toNativePointerArray(setterFunctionParamTypes, false, true), setterFunctionParamTypes.length, false);

    LLVMValueRef function = LLVM.LLVMAddFunction(module, mangledName, setterFunctionType);
    LLVM.LLVMSetLinkage(function, LLVM.LLVMLinkage.LLVMLinkOnceODRLinkage);
    LLVM.LLVMSetVisibility(function, LLVM.LLVMVisibility.LLVMHiddenVisibility);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(function);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    LLVMValueRef array = LLVM.LLVMGetParam(function, 0);
    LLVMValueRef index = LLVM.LLVMGetParam(function, 1);
    LLVMValueRef value = LLVM.LLVMGetParam(function, 2);

    LLVMTypeRef realArrayType = LLVM.LLVMPointerType(findNonProxiedArrayStructureType(arrayType, 0), 0);
    array = LLVM.LLVMBuildBitCast(builder, array, realArrayType, "");

    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false),
                                                 index};
    LLVMValueRef elementPointer = LLVM.LLVMBuildGEP(builder, array, C.toNativePointerArray(indices, false, true), indices.length, "");
    LLVM.LLVMBuildStore(builder, value, elementPointer);

    LLVM.LLVMBuildRetVoid(builder);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return function;
  }

  /**
   * Builds code to throw a cast error, with the specified from type, to type, and reason.
   * Note: this method finishes the current LLVM basic block.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param fromType - the fromType string to call the CastError constructor with
   * @param toType - the toType string to call the CastError constructor with
   * @param reason - the reason string to call the CastError constructor with (can be null)
   */
  public void buildThrowCastError(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, String fromType, String toType, String reason)
  {
    buildThrowCastError(builder, landingPadContainer, codeGenerator.buildStringCreation(builder, landingPadContainer, fromType), toType, reason);
  }

  /**
   * Builds code to throw a cast error, with the specified from type, to type, and reason.
   * Note: this method finishes the current LLVM basic block.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param llvmFromType - the fromType string to call the CastError constructor with, in a temporary type representation
   * @param toType - the toType string to call the CastError constructor with
   * @param reason - the reason string to call the CastError constructor with (can be null)
   */
  public void buildThrowCastError(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef llvmFromType, String toType, String reason)
  {
    ClassDefinition castErrorTypeDefinition = (ClassDefinition) SpecialTypeHandler.CAST_ERROR_TYPE.getResolvedTypeDefinition();
    LLVMValueRef[] allocatorArgs = new LLVMValueRef[0];
    LLVMValueRef allocator = codeGenerator.getAllocatorFunction(castErrorTypeDefinition);
    LLVMBasicBlockRef allocatorInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "castErrorAllocatorContinue");
    LLVMValueRef allocatedCastError = LLVM.LLVMBuildInvoke(builder, allocator, C.toNativePointerArray(allocatorArgs, false, true), allocatorArgs.length, allocatorInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, allocatorInvokeContinueBlock);

    llvmFromType = convertTemporaryToStandard(builder, llvmFromType, SpecialTypeHandler.STRING_TYPE);
    LLVMValueRef llvmToType = codeGenerator.buildStringCreation(builder, landingPadContainer, toType);
    llvmToType = convertTemporaryToStandard(builder, llvmToType, SpecialTypeHandler.STRING_TYPE);
    LLVMValueRef llvmReason;
    Type nullableStringType = Type.findTypeWithNullability(SpecialTypeHandler.STRING_TYPE, true);
    if (reason == null)
    {
      llvmReason = LLVM.LLVMConstNull(findStandardType(nullableStringType));
    }
    else
    {
      llvmReason = codeGenerator.buildStringCreation(builder, landingPadContainer, reason);
      llvmReason = convertTemporaryToStandard(builder, landingPadContainer, llvmReason, SpecialTypeHandler.STRING_TYPE, nullableStringType, null, null);
    }

    LLVMValueRef constructorFunction = codeGenerator.getConstructorFunction(SpecialTypeHandler.castErrorTypesReasonConstructor);
    LLVMValueRef[] constructorArguments = new LLVMValueRef[] {allocatedCastError, llvmFromType, llvmToType, llvmReason};
    LLVMBasicBlockRef constructorInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "castErrorConstructorContinue");
    LLVM.LLVMBuildInvoke(builder, constructorFunction, C.toNativePointerArray(constructorArguments, false, true), constructorArguments.length, constructorInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, constructorInvokeContinueBlock);
    LLVMValueRef castError = convertTemporaryToStandard(builder, allocatedCastError, SpecialTypeHandler.CAST_ERROR_TYPE);

    LLVMValueRef unwindException = codeGenerator.buildCreateException(builder, castError);
    codeGenerator.buildThrow(builder, landingPadContainer, unwindException);
  }

  /**
   * Builds a null check which will throw a CastError on failure.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the value to perform the null check on, in a temporary type representation
   * @param from - the Type of value
   * @param to - the Type that the value is being converted to
   * @param toAccessor - the TypeParameterAccessor to look up 'to' in, if it turns out to be a type parameter
   */
  private void buildCastNullCheck(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef value, Type from, Type to, TypeParameterAccessor toAccessor)
  {
    LLVMBasicBlockRef continueBlock = LLVM.LLVMAddBasicBlock(builder, "castNullCheckContinue");
    LLVMBasicBlockRef nullBlock = LLVM.LLVMAddBasicBlock(builder, "castNullFailure");

    LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
    String reasonString = null;
    boolean isTupleUnboxing = from instanceof TupleType && ((TupleType) from).getSubTypes().length == 1 && ((TupleType) from).getSubTypes()[0].isRuntimeEquivalent(to);
    if (to instanceof NamedType && ((NamedType) to).getResolvedTypeParameter() != null && !isTupleUnboxing)
    {
      TypeParameter typeParameter = ((NamedType) to).getResolvedTypeParameter();
      LLVMValueRef rtti = toAccessor.findTypeParameterRTTI(typeParameter);
      LLVMValueRef typeIsNullable = rttiHelper.buildTypeIsNullableCheck(builder, rtti);
      LLVMValueRef success = LLVM.LLVMBuildOr(builder, isNotNull, typeIsNullable, "");
      LLVM.LLVMBuildCondBr(builder, success, continueBlock, nullBlock);
      reasonString = "the type parameter " + to + " turned out not to be nullable";
    }
    else
    {
      LLVM.LLVMBuildCondBr(builder, isNotNull, continueBlock, nullBlock);
    }

    LLVM.LLVMPositionBuilderAtEnd(builder, nullBlock);
    buildThrowCastError(builder, landingPadContainer, "null", to.toString(), reasonString);

    LLVM.LLVMPositionBuilderAtEnd(builder, continueBlock);
  }

  /**
   * Checks whether a conversion is required to convert from 'from' to 'to'.
   * This is designed to be used when deciding e.g. whether or not to proxy a function or an array during a conversion.
   * In some circumstances it will return that no conversion is required when in fact some bitcasting of contained pointers must be done for the LLVM types to line up correctly.
   * Crucially, it will never return false if a value needs to be re-packed into a different LLVM type, or if we would need to perform some run-time checks on the value.
   * In case of an unexpected conversion which should be invalid, this method will always return true, so that the caller will try to perform a conversion and convertTemporary() will discover the error.
   * @param from - the type that we are converting from
   * @param to - the type that we are converting to
   * @param performChecks - true if this check should assume that run-time checks need to be performed, false if it should assume that they aren't necessary
   * @return false if we can be sure that no conversion (over and above a bitcast) is required from 'from' to 'to', true otherwise
   */
  private boolean checkRequiresConversion(Type from, Type to, boolean performChecks)
  {
    if (from.isRuntimeEquivalent(to))
    {
      return false;
    }
    if (from instanceof PrimitiveType && to instanceof PrimitiveType)
    {
      // they are not equivalent, so there needs to be a conversion
      return true;
    }
    if (from instanceof ArrayType && to instanceof ArrayType)
    {
      ArrayType fromArray = (ArrayType) from;
      ArrayType toArray = (ArrayType) to;
      if (performChecks && fromArray.canBeNullable() && !toArray.isNullable())
      {
        return true;
      }
      return checkRequiresConversion(fromArray.getBaseType(), toArray.getBaseType(), performChecks) ||
             checkRequiresConversion(toArray.getBaseType(), fromArray.getBaseType(), performChecks);
    }
    if (from instanceof FunctionType && to instanceof FunctionType)
    {
      FunctionType fromFunction = (FunctionType) from;
      FunctionType toFunction = (FunctionType) to;
      if (performChecks &&
          ((fromFunction.canBeNullable() && !toFunction.isNullable()) ||
           (!fromFunction.isImmutable() && toFunction.isImmutable())))
      {
        return true;
      }
      if (checkRequiresConversion(fromFunction.getReturnType(), toFunction.getReturnType(), performChecks))
      {
        return true;
      }
      Type[] fromParams = fromFunction.getParameterTypes();
      Type[] toParams = toFunction.getParameterTypes();
      if (fromParams.length != toParams.length)
      {
        return true;
      }
      for (int i = 0; i < fromParams.length; ++i)
      {
        if (checkRequiresConversion(toParams[i], fromParams[i], performChecks))
        {
          return true;
        }
      }
      return false;
    }
    // note: we let single-element-tuple extraction and insertion fall through to the catch-all, as they always require conversion
    if (from instanceof TupleType && to instanceof TupleType)
    {
      TupleType fromTuple = (TupleType) from;
      TupleType toTuple = (TupleType) to;
      Type[] fromSubTypes = fromTuple.getSubTypes();
      Type[] toSubTypes = toTuple.getSubTypes();
      if (from.canBeNullable() != to.isNullable() ||
          fromSubTypes.length != toSubTypes.length)
      {
        return true;
      }
      for (int i = 0; i < fromSubTypes.length; ++i)
      {
        if (checkRequiresConversion(fromSubTypes[i], toSubTypes[i], performChecks))
        {
          return true;
        }
      }
      // the elements inside the tuples do not require conversion
      return false;
    }
    if (to instanceof WildcardType)
    {
      if (!performChecks && from.canBeNullable() && !to.canBeNullable())
      {
        return true;
      }
      if (from instanceof ObjectType ||
          from instanceof ArrayType ||
          from instanceof WildcardType ||
          (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition) ||
          (from instanceof NamedType && ((NamedType) from).getResolvedTypeParameter() != null))
      {
        // a run-time check only needs to be generated if the 'to' type's super-types can't assign 'from'
        if (performChecks)
        {
          Type fromNoModifiers = Type.findTypeWithoutModifiers(from);
          for (Type t : Type.findAllSuperTypes(to))
          {
            if (!t.canAssign(fromNoModifiers))
            {
              return true;
            }
          }
        }
        return false;
      }
      return true;
    }
    if (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition)
    {
      if (!performChecks && from.canBeNullable() && !to.isNullable() && !(to instanceof WildcardType && to.canBeNullable()))
      {
        return true;
      }
      // in some circumstances, we might be able to get away without casting a class, since it has the same native representation as various other types
      if (to instanceof ObjectType ||
          from.isRuntimeEquivalent(to) ||
          (!performChecks && to instanceof NamedType && ((NamedType) to).getResolvedTypeParameter() != null) ||
          (!performChecks && to instanceof NamedType && ((NamedType) to).getResolvedTypeDefinition() instanceof ClassDefinition))
      {
        return false;
      }
      if (to instanceof WildcardType)
      {
        return performChecks && !((WildcardType) to).encompasses(from);
      }
      if (!(to instanceof NamedType))
      {
        return true;
      }
      NamedType fromNamed = (NamedType) from;
      GenericTypeSpecialiser specialiser = new GenericTypeSpecialiser(fromNamed);
      NamedType toNamed = (NamedType) to;

      if (toNamed.getResolvedTypeDefinition() != null)
      {
        Type[] toTypeArguments = toNamed.getTypeArguments();
        for (NamedType t : fromNamed.getResolvedTypeDefinition().getInheritanceLinearisation())
        {
          NamedType superType = (NamedType) specialiser.getSpecialisedType(t);
          if (!(superType.getResolvedTypeDefinition() instanceof ClassDefinition) ||
              toNamed.getResolvedTypeDefinition() != superType.getResolvedTypeDefinition())
          {
            continue;
          }

          Type[] superTypeArguments = superType.getTypeArguments();
          if ((toTypeArguments == null) != (superTypeArguments == null) ||
              (toTypeArguments != null && toTypeArguments.length != superTypeArguments.length))
          {
            continue;
          }

          boolean argumentsMatch = true;
          for (int i = 0; toTypeArguments != null && i < toTypeArguments.length; ++i)
          {
            if (toTypeArguments[i] instanceof WildcardType)
            {
              // the type argument of 'to' is a wildcard, so allow the conversion as long as it encompasses the current type argument
              if (!((WildcardType) toTypeArguments[i]).encompasses(superTypeArguments[i]))
              {
                argumentsMatch = false;
                break;
              }
            }
            else if (toTypeArguments[i].isRuntimeEquivalent(superTypeArguments[i]))
            {
              // all non-wildcard type arguments must be equivalent
              argumentsMatch = false;
              break;
            }
          }
          if (argumentsMatch)
          {
            // 'to' is above 'from' in the type hierarchy, so the only conversion we need is a bitcast
            return false;
          }
        }
        return true;
      }
    }
    if (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof InterfaceDefinition)
    {
      if (to instanceof NamedType && ((NamedType) to).getResolvedTypeDefinition() instanceof InterfaceDefinition)
      {
        if (performChecks && from.canBeNullable() && !to.isNullable())
        {
          return true;
        }
        // if these two interfaces are completely identical, then we don't need to do any conversion
        // however, if either the interface or any of the type arguments differ, we will need to lookup the VFT again
        NamedType fromNamed = (NamedType) from;
        NamedType toNamed = (NamedType) to;
        if (fromNamed.getResolvedTypeDefinition() != toNamed.getResolvedTypeDefinition() ||
            fromNamed.getTypeArguments().length != toNamed.getTypeArguments().length)
        {
          return true;
        }
        Type[] fromArguments = fromNamed.getTypeArguments();
        Type[] toArguments = toNamed.getTypeArguments();
        for (int i = 0; i < fromArguments.length; ++i)
        {
          if (fromArguments[i] instanceof WildcardType && toArguments[i] instanceof WildcardType)
          {
            // both of the arguments are wildcards, so if the 'to' argument encompasses the 'from' argument, then this argument doesn't make us require a conversion
            // NOTE: we require that both of them are wildcards in order to relax the equivalence check, because if one is a wildcard but one isn't then the interface's type representation will change
            if (!((WildcardType) toArguments[i]).encompasses(fromArguments[i]))
            {
              return true;
            }
          }
          else if (!fromArguments[i].isRuntimeEquivalent(toArguments[i]))
          {
            return true;
          }
        }
        // the 'from' and 'to' types are identical, except for perhaps their nullability, which we have already checked for
        return false;
      }
      return true;
    }
    if (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof CompoundDefinition)
    {
      if (to instanceof NamedType && ((NamedType) to).getResolvedTypeDefinition() instanceof CompoundDefinition)
      {
        if (performChecks && from.canBeNullable() && !to.isNullable())
        {
          return true;
        }
        // if these two compound types are completely identical, then we don't need to do any checks
        // however, if either the compound type or any of the type arguments differ, we will need to do some checks
        if (performChecks)
        {
          NamedType fromNamed = (NamedType) from;
          NamedType toNamed = (NamedType) to;
          if (fromNamed.getResolvedTypeDefinition() != toNamed.getResolvedTypeDefinition() ||
              fromNamed.getTypeArguments().length != toNamed.getTypeArguments().length)
          {
            return true;
          }
          Type[] fromArguments = fromNamed.getTypeArguments();
          Type[] toArguments = toNamed.getTypeArguments();
          for (int i = 0; i < fromArguments.length; ++i)
          {
            if (toArguments[i] instanceof WildcardType)
            {
              // the type argument of 'to' is a wildcard, so allow the conversion as long as it encompasses the current type argument
              if (!((WildcardType) toArguments[i]).encompasses(fromArguments[i]))
              {
                return true;
              }
            }
            else if (!fromArguments[i].isRuntimeEquivalent(toArguments[i]))
            {
              return true;
            }
          }
        }
        // the 'from' and 'to' types are identical, except for perhaps their nullability, which we have already checked for
        return false;
      }
      return true;
    }
    if (from instanceof NamedType && ((NamedType) from).getResolvedTypeParameter() != null)
    {
      if (performChecks && from.canBeNullable() && !to.isNullable() && !(to instanceof WildcardType && to.canBeNullable()))
      {
        return true;
      }
      TypeParameter typeParameter = ((NamedType) from).getResolvedTypeParameter();
      if (to instanceof ObjectType ||
          (!performChecks && to instanceof NamedType && ((NamedType) to).getResolvedTypeParameter() != null) ||
          (to instanceof NamedType && ((NamedType) to).getResolvedTypeParameter() == typeParameter))
      {
        return false;
      }
      if ((to instanceof NamedType && ((NamedType) to).getResolvedTypeDefinition() instanceof ClassDefinition) ||
          to instanceof ArrayType)
      {
        for (Type superType : typeParameter.getSuperTypes())
        {
          if (((superType instanceof NamedType && ((NamedType) superType).getResolvedTypeDefinition() instanceof ClassDefinition) ||
                superType instanceof ArrayType) &&
              !checkRequiresConversion(superType, to, performChecks))
          {
            return false;
          }
        }
      }
      return true;
    }
    if (!performChecks && (from instanceof ObjectType || from instanceof WildcardType))
    {
      // if we are coming from the object type or a wildcard type and not performing checks, we can go to any type which is represented like an object, by bitcasting
      return !(to instanceof ArrayType ||
               (to instanceof NamedType && ((NamedType) to).getResolvedTypeDefinition() instanceof ClassDefinition) ||
               (to instanceof NamedType && ((NamedType) to).getResolvedTypeParameter() != null) ||
               to instanceof ObjectType ||
               to instanceof WildcardType);
    }
    // if we aren't doing any run-time checks, treat type parameters as objects
    if (to instanceof ObjectType ||
        (!performChecks && to instanceof NamedType && ((NamedType) to).getResolvedTypeParameter() != null))
    {
      if (performChecks && from.canBeNullable() && !to.isNullable())
      {
        return true;
      }
      // if we are converting from something which is not represented like a pointer to an object, then we require a conversion
      return !(from instanceof ArrayType ||
               (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition) ||
               (from instanceof NamedType && ((NamedType) from).getResolvedTypeParameter() != null) ||
               from instanceof ObjectType ||
               from instanceof WildcardType);
    }
    if (performChecks && to instanceof NamedType && ((NamedType) to).getResolvedTypeParameter() != null)
    {
      if (from instanceof NamedType && ((NamedType) from).getResolvedTypeParameter() == ((NamedType) to).getResolvedTypeParameter())
      {
        // these types represent the same type parameter, so we only need to do any run-time checks if their nullabilities can't always be implicitly converted
        return from.isNullable() && !to.isNullable();
      }
      // if we are converting from something other than exactly the same type parameter, we will have to do RTTI checks
      return true;
    }
    // note: we let all other conversions from objects fall through to the catch-all, as they always require RTTI checks
    return true;
  }

  /**
   * Strips any type parameters and wildcard types which are not part of a type argument to a NamedType from the specified type.
   * The stripped type parameters are replaced by the object type.
   * Types are meant to be treated as immutable, so a new stripped type is returned rather than modifying the existing one.
   * @param type - the Type to find the stripped version of
   * @return the type, with any type parameters and wildcard types which could affect the native type replaced by objects
   */
  public Type stripTypeParameters(Type type)
  {
    if (type instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) type;
      Type baseType = stripTypeParameters(arrayType.getBaseType());
      if (baseType == arrayType.getBaseType())
      {
        // it wasn't modified, so return the existing array
        return arrayType;
      }
      return new ArrayType(arrayType.canBeNullable(), arrayType.isExplicitlyImmutable(), arrayType.isContextuallyImmutable(), baseType, null);
    }
    if (type instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) type;
      Type returnType = stripTypeParameters(functionType.getReturnType());
      boolean changed = returnType != functionType.getReturnType();
      Type[] oldParamTypes = functionType.getParameterTypes();
      Type[] paramTypes = new Type[oldParamTypes.length];
      for (int i = 0; i < paramTypes.length; ++i)
      {
        paramTypes[i] = stripTypeParameters(oldParamTypes[i]);
        changed |= paramTypes[i] != oldParamTypes[i];
      }
      if (changed)
      {
        return new FunctionType(functionType.canBeNullable(), functionType.isImmutable(), returnType, paramTypes, functionType.getThrownTypes(), null);
      }
      return functionType;
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      if (namedType.getResolvedTypeParameter() != null)
      {
        return new ObjectType(namedType.canBeNullable(), namedType.canBeExplicitlyImmutable(), namedType.isContextuallyImmutable(), null);
      }
      // all non-type-parameter NamedTypes don't need anything to be replaced, as their native types do not depend on their type arguments
      return namedType;
    }
    if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      boolean changed = false;
      Type[] oldSubTypes = tupleType.getSubTypes();
      Type[] subTypes = new Type[oldSubTypes.length];
      for (int i = 0; i < oldSubTypes.length; ++i)
      {
        subTypes[i] = stripTypeParameters(oldSubTypes[i]);
        changed |= subTypes[i] != oldSubTypes[i];
      }
      if (changed)
      {
        return new TupleType(tupleType.canBeNullable(), subTypes, null);
      }
      return tupleType;
    }
    if (type instanceof WildcardType)
    {
      WildcardType wildcardType = (WildcardType) type;
      return new ObjectType(wildcardType.canBeNullable(), wildcardType.canBeExplicitlyImmutable(), null);
    }
    if (type instanceof NullType || type instanceof ObjectType || type instanceof PrimitiveType || type instanceof VoidType)
    {
      return type;
    }
    throw new IllegalArgumentException("Unknown sort of type: " + type);
  }

  /**
   * Converts the specified value from the specified 'from' type to the specified 'to' type, as a temporary.
   * This method assumes that the incoming value has a temporary native type, and produces a result with a temporary native type.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the value to convert
   * @param from - the Type to convert from
   * @param to - the Type to convert to
   * @param skipRuntimeChecks - true if run-time checks should be checked during this conversion - only use this if you can be SURE that the value is definitely of the correct type already
   * @param fromAccessor - the TypeParameterAccessor to get the values of any type parameters inside 'from' from
   * @param toAccessor - the TypeParameterAccessor to get the values of any type parameters inside 'to' from
   * @return the converted value
   */
  public LLVMValueRef convertTemporary(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef value, Type from, Type to, boolean skipRuntimeChecks, TypeParameterAccessor fromAccessor, TypeParameterAccessor toAccessor)
  {
    // Note concerning Type Parameters:
    // Sometimes, we will be at a boundary between two objects, such as a method call, converting from a type in one object's context to a type in another object's context.
    // For type parameters, you might expect this to cause a problem when the two type parameters are equivalent at compile time but are looked up in different TypeParameterAccessors at run time.
    // However, whenever we convert something at a type boundary, we always do a direct conversion from the real type (e.g. of a parameter) to the specialised type, where the only
    // difference is that the parameter has been specialised to the outer context. Because of this, we can know that if we have two equivalent TypeParameters, they must have
    // equivalent values in their respective mappers.
    if (from.isRuntimeEquivalent(to))
    {
      return value;
    }

    if (from instanceof PrimitiveType && to instanceof PrimitiveType)
    {
      if (!skipRuntimeChecks && from.canBeNullable() && !to.isNullable())
      {
        buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
      }
      return convertPrimitiveType(builder, value, (PrimitiveType) from, (PrimitiveType) to);
    }
    if (from instanceof ArrayType && to instanceof ArrayType)
    {
      // array casts are illegal unless the base types are the same, so they must have the same basic type
      if (!skipRuntimeChecks && from.canBeNullable() && !to.isNullable())
      {
        buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
      }
      // immutability will be checked by the type checker, but it doesn't have any effect on the native type, so we do not need to do anything special here

      ArrayType fromArray = (ArrayType) from;
      // the run-time type of an array never includes any references to type parameters, so they need to be stripped here
      ArrayType toArray = (ArrayType) stripTypeParameters(to);

      boolean needsProxying = checkRequiresConversion(fromArray.getBaseType(), toArray.getBaseType(), true) ||
                              checkRequiresConversion(toArray.getBaseType(), fromArray.getBaseType(), true);
      if (needsProxying)
      {
        return buildProxyArrayConversion(builder, value, fromArray, toArray, fromAccessor, toAccessor);
      }

      return LLVM.LLVMBuildBitCast(builder, value, findTemporaryType(toArray), "");
    }
    if (from instanceof FunctionType && to instanceof FunctionType)
    {
      // function casts are illegal unless the parameter and return types are the same, so they must have the same basic type
      if (!skipRuntimeChecks && from.canBeNullable() && !to.isNullable())
      {
        buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
      }

      FunctionType fromFunction = (FunctionType) from;
      FunctionType toFunction = (FunctionType) to;

      // a cast from a non-immutable function to an immutable function type is impossible
      // so perform a run-time check that this constraint is not violated
      if (!skipRuntimeChecks && !fromFunction.isImmutable() && toFunction.isImmutable())
      {
        // this is only allowed if the run-time type of value shows that it is immutable
        LLVMBasicBlockRef functionImmutabilityCheckContinueBlock = LLVM.LLVMAddBasicBlock(builder, "functionImmutabilityCheckContinue");
        // if the value is nullable, allow null values to pass through (we've already checked the nullability)
        if (from.canBeNullable())
        {
          LLVMBasicBlockRef functionImmutabilityCheckBlock = LLVM.LLVMAddBasicBlock(builder, "functionImmutabilityCheck");
          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
          LLVM.LLVMBuildCondBr(builder, isNotNull, functionImmutabilityCheckBlock, functionImmutabilityCheckContinueBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, functionImmutabilityCheckBlock);
        }
        LLVMValueRef runtimeTypeMatches = rttiHelper.buildInstanceOfCheck(builder, landingPadContainer, value, Type.findTypeWithNullability(from, false), to, fromAccessor, toAccessor);
        LLVMBasicBlockRef immutabilityCastFailure = LLVM.LLVMAddBasicBlock(builder, "functionImmutabilityCheckFailure");
        LLVM.LLVMBuildCondBr(builder, runtimeTypeMatches, functionImmutabilityCheckContinueBlock, immutabilityCastFailure);

        LLVM.LLVMPositionBuilderAtEnd(builder, immutabilityCastFailure);
        buildThrowCastError(builder, landingPadContainer, from.toString(), to.toString(), "non-immutable functions cannot be cast to immutable");

        LLVM.LLVMPositionBuilderAtEnd(builder, functionImmutabilityCheckContinueBlock);
      }

      // strip any type parameters from the 'to' type, so that the RTTI doesn't contain anything which could confuse things
      // if we tried to convert to object and back to another type, RTTI containing type parameters would cause breakages in the cast instanceof checks
      toFunction = (FunctionType) stripTypeParameters(toFunction);

      boolean needsProxying = checkRequiresConversion(toFunction.getReturnType(), fromFunction.getReturnType(), true);
      Type[] fromParamTypes = fromFunction.getParameterTypes();
      Type[] toParamTypes = toFunction.getParameterTypes();
      needsProxying |= fromParamTypes.length != toParamTypes.length;
      for (int i = 0; !needsProxying & i < fromParamTypes.length; ++i)
      {
        needsProxying |= checkRequiresConversion(fromParamTypes[i], toParamTypes[i], true);
      }
      if (needsProxying)
      {
        return buildFunctionProxyConversion(builder, value, fromFunction, toFunction, fromAccessor, toAccessor);
      }

      if (!fromFunction.isRuntimeEquivalent(toFunction))
      {
        // repack the function, after bitcasting the function pointer
        LLVMValueRef rtti = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
        LLVMValueRef callee = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
        LLVMValueRef function = LLVM.LLVMBuildExtractValue(builder, value, 2, "");
        function = LLVM.LLVMBuildBitCast(builder, function, findRawFunctionPointerType(toFunction), "");
        LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(toFunction));
        result = LLVM.LLVMBuildInsertValue(builder, result, rtti, 0, "");
        result = LLVM.LLVMBuildInsertValue(builder, result, callee, 1, "");
        result = LLVM.LLVMBuildInsertValue(builder, result, function, 2, "");
        return result;
      }

      return value;
    }
    if (from instanceof NamedType && to instanceof NamedType &&
        ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition &&
        ((NamedType) to).getResolvedTypeDefinition() instanceof ClassDefinition)
    {
      if (!skipRuntimeChecks && from.canBeNullable() && !to.isNullable())
      {
        buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
      }

      // check whether 'to' is a super-class of 'from'
      NamedType fromNamed = (NamedType) from;
      NamedType toNamed = (NamedType) to;
      GenericTypeSpecialiser genericTypeSpecialiser = new GenericTypeSpecialiser(fromNamed);
      boolean isInherited = false;
      for (NamedType t : fromNamed.getResolvedTypeDefinition().getInheritanceLinearisation())
      {
        NamedType superType = (NamedType) genericTypeSpecialiser.getSpecialisedType(t);
        if (!(superType.getResolvedTypeDefinition() instanceof ClassDefinition) ||
            superType.getResolvedTypeDefinition() != toNamed.getResolvedTypeDefinition())
        {
          continue;
        }
        Type[] fromArguments = superType.getTypeArguments();
        Type[] toArguments = toNamed.getTypeArguments();
        if ((fromArguments == null) != (toArguments == null) ||
            (fromArguments != null && fromArguments.length != toArguments.length))
        {
          continue;
        }
        boolean argumentsMatch = true;
        for (int i = 0; fromArguments != null && i < fromArguments.length; ++i)
        {
          if (toArguments[i] instanceof WildcardType)
          {
            // the type argument of 'to' is a wildcard, so the super-type matches as long as it encompasses the current type argument
            if (!((WildcardType) toArguments[i]).encompasses(fromArguments[i]))
            {
              argumentsMatch = false;
              break;
            }
          }
          else if (!toArguments[i].isRuntimeEquivalent(fromArguments[i]))
          {
            argumentsMatch = false;
            break;
          }
        }
        if (argumentsMatch)
        {
          isInherited = true;
          break;
        }
      }

      if (!isInherited && !skipRuntimeChecks)
      {
        LLVMBasicBlockRef instanceOfContinueBlock = LLVM.LLVMAddBasicBlock(builder, "castInstanceOfCheckContinue");
        if (from.canBeNullable())
        {
          // if the value is null, then skip the check
          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
          LLVMBasicBlockRef instanceOfCheckBlock = LLVM.LLVMAddBasicBlock(builder, "castInstanceOfCheck");
          LLVM.LLVMBuildCondBr(builder, isNotNull, instanceOfCheckBlock, instanceOfContinueBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfCheckBlock);
        }
        LLVMValueRef runtimeTypeMatches = rttiHelper.buildInstanceOfCheck(builder, landingPadContainer, value, Type.findTypeWithNullability(from, false), to, fromAccessor, toAccessor);
        LLVMBasicBlockRef castFailureBlock = LLVM.LLVMAddBasicBlock(builder, "castFailure");
        LLVM.LLVMBuildCondBr(builder, runtimeTypeMatches, instanceOfContinueBlock, castFailureBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, castFailureBlock);
        LLVMValueRef rttiPointer = rttiHelper.getRTTIPointer(builder, value);
        LLVMValueRef rtti = LLVM.LLVMBuildLoad(builder, rttiPointer, "");
        LLVMValueRef classNameUbyteArray = rttiHelper.lookupNamedTypeName(builder, rtti);
        LLVMValueRef classNameString = codeGenerator.buildStringCreation(builder, landingPadContainer, classNameUbyteArray);
        buildThrowCastError(builder, landingPadContainer, classNameString, to.toString(), null);

        LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfContinueBlock);
      }
      // both from and to are class types, and we have made sure that we can convert between them
      // so bitcast value to the new type
      return LLVM.LLVMBuildBitCast(builder, value, findTemporaryType(to), "");
    }
    if (from instanceof NamedType && to instanceof NamedType &&
        ((NamedType) from).getResolvedTypeDefinition() instanceof CompoundDefinition &&
        ((NamedType) to).getResolvedTypeDefinition() instanceof CompoundDefinition)
    {
      // compound type casts are illegal unless the type definitions are the same, so they must have the same type definition
      if (!skipRuntimeChecks && from.canBeNullable() && !to.isNullable())
      {
        buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
      }

      if (!skipRuntimeChecks && !from.isRuntimeEquivalent(to))
      {
        LLVMBasicBlockRef instanceOfContinueBlock = LLVM.LLVMAddBasicBlock(builder, "castInstanceOfCheckContinue");
        if (from.canBeNullable())
        {
          // if the value is null, then skip the check
          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
          LLVMBasicBlockRef instanceOfCheckBlock = LLVM.LLVMAddBasicBlock(builder, "castInstanceOfCheck");
          LLVM.LLVMBuildCondBr(builder, isNotNull, instanceOfCheckBlock, instanceOfContinueBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfCheckBlock);
        }
        LLVMValueRef runtimeTypeMatches = rttiHelper.buildInstanceOfCheck(builder, landingPadContainer, value, Type.findTypeWithNullability(from, false), to, fromAccessor, toAccessor);
        LLVMBasicBlockRef castFailureBlock = LLVM.LLVMAddBasicBlock(builder, "castFailure");
        LLVM.LLVMBuildCondBr(builder, runtimeTypeMatches, instanceOfContinueBlock, castFailureBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, castFailureBlock);
        buildThrowCastError(builder, landingPadContainer, from.toString(), to.toString(), null);

        LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfContinueBlock);
        // both from and to are compound types, and we have made sure that we can convert between them
        // so bitcast value to the new type
        value = LLVM.LLVMBuildBitCast(builder, value, findTemporaryType(to), "");
      }

      return value;
    }
    if (from instanceof NamedType && to instanceof NamedType &&
        ((NamedType) from).getResolvedTypeDefinition() instanceof InterfaceDefinition &&
        ((NamedType) to).getResolvedTypeDefinition() instanceof ClassDefinition)
    {
      ObjectType objectType = new ObjectType(from.canBeNullable(), false, null);
      // there are no type parameters inside objectType, so use a null TypeParameterAccessor
      TypeParameterAccessor nullAccessor = new TypeParameterAccessor(builder, rttiHelper);
      LLVMValueRef objectValue = convertTemporary(builder, landingPadContainer, value, from, objectType, skipRuntimeChecks, fromAccessor, nullAccessor);
      return convertTemporary(builder, landingPadContainer, objectValue, objectType, to, skipRuntimeChecks, nullAccessor, toAccessor);
    }
    if (from instanceof NamedType && to instanceof NamedType &&
        ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition &&
        ((NamedType) to).getResolvedTypeDefinition() instanceof InterfaceDefinition)
    {
      ClassDefinition classDefinition = (ClassDefinition) ((NamedType) from).getResolvedTypeDefinition();
      GenericTypeSpecialiser genericTypeSpecialiser = new GenericTypeSpecialiser((NamedType) from);
      NamedType toNamed = (NamedType) to;
      NamedType foundType = null;
      for (NamedType t : classDefinition.getInheritanceLinearisation())
      {
        NamedType superType = (NamedType) genericTypeSpecialiser.getSpecialisedType(t);
        // check whether superType matches toNamed
        // we cannot just remove modifiers and check for equivalence, because toNamed might have wildcard type arguments which match superType's type arguments
        if (toNamed.getResolvedTypeDefinition() != superType.getResolvedTypeDefinition())
        {
          continue;
        }
        Type[] toTypeArguments = toNamed.getTypeArguments();
        Type[] superTypeArguments = superType.getTypeArguments();
        if (((toTypeArguments == null) != (superTypeArguments == null)) ||
            (toTypeArguments != null && toTypeArguments.length != superTypeArguments.length))
        {
          continue;
        }
        boolean argumentsMatch = true;
        for (int i = 0; argumentsMatch && toTypeArguments != null && i < toTypeArguments.length; ++i)
        {
          if (toTypeArguments[i] instanceof WildcardType)
          {
            argumentsMatch = ((WildcardType) toTypeArguments[i]).encompasses(superTypeArguments[i]);
          }
          else
          {
            argumentsMatch = toTypeArguments[i].isRuntimeEquivalent(superTypeArguments[i]);
          }
        }
        if (argumentsMatch)
        {
          // Note: we want the unspecialised version of the super-type here, so that it can be specialised at run-time to have the same type arguments as the run-time type of the class we are converting from
          // one point to consider is, if 'from' has wildcard type arguments, we want to set the interface's type argument RTTI pointers to their real values, not a '?'
          foundType = t;
          break;
        }
      }
      if (foundType != null)
      {
        if (!skipRuntimeChecks && from.canBeNullable() && !to.isNullable())
        {
          buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
        }

        LLVMBasicBlockRef startBlock = null;
        LLVMBasicBlockRef continueBlock = null;
        // make sure we don't do a second null check if this is a cast from nullable to not-nullable
        if (from.canBeNullable() && to.isNullable())
        {
          startBlock = LLVM.LLVMGetInsertBlock(builder);
          continueBlock = LLVM.LLVMAddBasicBlock(builder, "toInterfaceContinuation");
          LLVMBasicBlockRef convertBlock = LLVM.LLVMAddBasicBlock(builder, "toInterfaceConversion");

          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
          LLVM.LLVMBuildCondBr(builder, isNotNull, convertBlock, continueBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, convertBlock);
        }
        LLVMTypeRef resultNativeType = findStandardType(to);

        // we know exactly where the VFT is at compile time, so we don't need to search for it at run time, just look it up
        TypeParameterAccessor foundAccessor = new TypeParameterAccessor(builder, this, rttiHelper, classDefinition, value);
        LLVMValueRef vft = virtualFunctionHandler.getVirtualFunctionTable(builder, landingPadContainer, value, (NamedType) from, foundType, fromAccessor, foundAccessor);
        LLVMValueRef objectPointer = convertTemporary(builder, landingPadContainer, value, from, new ObjectType(from.canBeNullable(), false, null), skipRuntimeChecks, fromAccessor, new TypeParameterAccessor(builder, rttiHelper));
        LLVMValueRef interfaceValue = LLVM.LLVMGetUndef(resultNativeType);
        interfaceValue = LLVM.LLVMBuildInsertValue(builder, interfaceValue, vft, 0, "");
        interfaceValue = LLVM.LLVMBuildInsertValue(builder, interfaceValue, objectPointer, 1, "");

        // fill in the interface's wildcard type argument RTTI values
        Type[] toTypeArguments = toNamed.getTypeArguments();
        Type[] foundTypeArguments = foundType.getTypeArguments();
        int wildcardIndex = 0;
        TypeParameterAccessor wildcardTypeParameterAccessor = new TypeParameterAccessor(builder, this, rttiHelper, classDefinition, value);
        for (int i = 0; toTypeArguments != null && i < toTypeArguments.length; ++i)
        {
          if (toTypeArguments[i] instanceof WildcardType)
          {
            LLVMValueRef rtti = rttiHelper.buildRTTICreation(builder, foundTypeArguments[i], wildcardTypeParameterAccessor);
            interfaceValue = LLVM.LLVMBuildInsertValue(builder, interfaceValue, rtti, 2 + wildcardIndex, "");
            ++wildcardIndex;
          }
        }

        if (from.canBeNullable() && to.isNullable())
        {
          LLVMBasicBlockRef endConvertBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildBr(builder, continueBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, continueBlock);

          LLVMValueRef phiNode = LLVM.LLVMBuildPhi(builder, resultNativeType, "");
          LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstNull(resultNativeType), interfaceValue};
          LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endConvertBlock};
          LLVM.LLVMAddIncoming(phiNode, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);

          return phiNode;
        }
        return interfaceValue;
      }
    }
    if (to instanceof NamedType && ((NamedType) to).getResolvedTypeDefinition() instanceof InterfaceDefinition)
    {
      LLVMValueRef objectValue = null;
      Type objectType = null;
      if (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition)
      {
        objectType = new ObjectType(from.canBeNullable(), ((NamedType) from).isContextuallyImmutable(), null);
        objectValue = convertTemporary(builder, landingPadContainer, value, from, objectType, skipRuntimeChecks, fromAccessor, new TypeParameterAccessor(builder, rttiHelper));
      }
      else if (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof InterfaceDefinition)
      {
        objectType = new ObjectType(from.canBeNullable(), ((NamedType) from).isContextuallyImmutable(), null);
        objectValue = convertTemporary(builder, landingPadContainer, value, from, objectType, skipRuntimeChecks, fromAccessor, new TypeParameterAccessor(builder, rttiHelper));
      }
      else if (from instanceof ObjectType || from instanceof WildcardType)
      {
        objectType = from;
        objectValue = value;
      }
      if (objectValue != null)
      {
        if (!skipRuntimeChecks && from.canBeNullable() && !to.isNullable())
        {
          buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
        }

        LLVMBasicBlockRef startBlock = null;
        LLVMBasicBlockRef continueBlock = null;
        // make sure we don't do a second null check if this is a cast from nullable to not-nullable
        if (objectType.canBeNullable() && to.isNullable())
        {
          startBlock = LLVM.LLVMGetInsertBlock(builder);
          continueBlock = LLVM.LLVMAddBasicBlock(builder, "toInterfaceContinuation");
          LLVMBasicBlockRef convertBlock = LLVM.LLVMAddBasicBlock(builder, "toInterfaceConversion");

          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, objectValue, objectType);
          LLVM.LLVMBuildCondBr(builder, isNotNull, convertBlock, continueBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, convertBlock);
        }
        LLVMTypeRef resultNativeType = findStandardType(to);

        NamedType toNamed = (NamedType) to;
        LLVMValueRef objectRTTIPtr = rttiHelper.getRTTIPointer(builder, objectValue);
        LLVMValueRef objectRTTI = LLVM.LLVMBuildLoad(builder, objectRTTIPtr, "");
        LLVMValueRef typeLookupResult = rttiHelper.lookupInstanceSuperType(builder, objectRTTI, toNamed, toAccessor);

        LLVMValueRef vftPointer = LLVM.LLVMBuildExtractValue(builder, typeLookupResult, 1, "");
        LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(virtualFunctionHandler.getVFTType(toNamed.getResolvedTypeDefinition()), 0);
        vftPointer = LLVM.LLVMBuildBitCast(builder, vftPointer, vftPointerType, "");
        LLVMValueRef vftPointerIsNotNull = LLVM.LLVMBuildIsNotNull(builder, vftPointer, "");
        LLVMBasicBlockRef castSuccessBlock = LLVM.LLVMAddBasicBlock(builder, "toInterfaceCastSuccess");
        LLVMBasicBlockRef castFailureBlock = LLVM.LLVMAddBasicBlock(builder, "toInterfaceCastFailure");
        LLVM.LLVMBuildCondBr(builder, vftPointerIsNotNull, castSuccessBlock, castFailureBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, castFailureBlock);
        if ((from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition) ||
            (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof InterfaceDefinition))
        {
          // if we're coming from a class or an interface, then we know that we can look up the real class name
          LLVMValueRef classNameUbyteArray = rttiHelper.lookupNamedTypeName(builder, objectRTTI);
          LLVMValueRef classNameString = codeGenerator.buildStringCreation(builder, landingPadContainer, classNameUbyteArray);
          buildThrowCastError(builder, landingPadContainer, classNameString, to.toString(), null);
        }
        else
        {
          buildThrowCastError(builder, landingPadContainer, from.toString(), to.toString(), "this object does not implement " + to);
        }

        LLVM.LLVMPositionBuilderAtEnd(builder, castSuccessBlock);

        LLVMValueRef interfaceValue = LLVM.LLVMGetUndef(resultNativeType);
        interfaceValue = LLVM.LLVMBuildInsertValue(builder, interfaceValue, vftPointer, 0, "");
        interfaceValue = LLVM.LLVMBuildInsertValue(builder, interfaceValue, objectValue, 1, "");

        Type[] toTypeArguments = toNamed.getTypeArguments();
        TypeParameterAccessor interfaceTypeParameterAccessor = null;
        int wildcardIndex = 0;
        for (int i = 0; toTypeArguments != null && i < toTypeArguments.length; ++i)
        {
          if (toTypeArguments[i] instanceof WildcardType)
          {
            if (interfaceTypeParameterAccessor == null)
            {
              LLVMValueRef interfaceRTTIValue = LLVM.LLVMBuildExtractValue(builder, typeLookupResult, 0, "");
              interfaceRTTIValue = LLVM.LLVMBuildBitCast(builder, interfaceRTTIValue, LLVM.LLVMPointerType(rttiHelper.getRTTIStructType(toNamed), 0), "");
              interfaceTypeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper, toNamed.getResolvedTypeDefinition(), rttiHelper.getNamedTypeArgumentMapper(builder, interfaceRTTIValue));
            }
            // extract the type argument's RTTI from the interface RTTI value, by looking up the value of the type parameter at index i
            LLVMValueRef unspecialisedRTTI = interfaceTypeParameterAccessor.findTypeParameterRTTI(toNamed.getResolvedTypeDefinition().getTypeParameters()[i]);
            // specialise the extracted type argument to the same level as its concrete type
            // here we assume that if the interface's unspecialised RTTI in the type search list references a type parameter, then the concrete type of that object is a named type which defines that parameter
            LLVMValueRef objectNamedRTTI = LLVM.LLVMBuildBitCast(builder, objectRTTI, rttiHelper.getGenericNamedRTTIType(), "");
            LLVMValueRef specialisedRTTI = rttiHelper.buildRTTISpecialisation(builder, unspecialisedRTTI, rttiHelper.getNamedTypeArgumentMapper(builder, objectNamedRTTI));
            // store the new RTTI block inside the interface's value
            interfaceValue = LLVM.LLVMBuildInsertValue(builder, interfaceValue, specialisedRTTI, 2 + wildcardIndex, "");
            ++wildcardIndex;
          }
        }

        if (objectType.canBeNullable() && to.isNullable())
        {
          LLVMBasicBlockRef endConvertBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildBr(builder, continueBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, continueBlock);

          LLVMValueRef phiNode = LLVM.LLVMBuildPhi(builder, resultNativeType, "");
          LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstNull(resultNativeType), interfaceValue};
          LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endConvertBlock};
          LLVM.LLVMAddIncoming(phiNode, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);

          return phiNode;
        }
        return interfaceValue;
      }
    }
    if (from instanceof TupleType)
    {
      // check for a single-element-tuple extraction
      TupleType fromTuple = (TupleType) from;
      if (fromTuple.getSubTypes().length == 1 && fromTuple.getSubTypes()[0].isRuntimeEquivalent(to))
      {
        if (from.canBeNullable())
        {
          if (!skipRuntimeChecks)
          {
            // if from is nullable and value is null, then we need to throw a CastError here
            buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
          }

          // extract the value of the tuple from the nullable structure
          value = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
        }
        return LLVM.LLVMBuildExtractValue(builder, value, 0, "");
      }
    }
    if (to instanceof TupleType)
    {
      // check for a single-element-tuple insertion
      TupleType toTuple = (TupleType) to;
      if (toTuple.getSubTypes().length == 1 && toTuple.getSubTypes()[0].isRuntimeEquivalent(from))
      {
        LLVMValueRef tupledValue = LLVM.LLVMGetUndef(findTemporaryType(new TupleType(false, toTuple.getSubTypes(), null)));
        tupledValue = LLVM.LLVMBuildInsertValue(builder, tupledValue, value, 0, "");
        if (to.isNullable())
        {
          LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(to));
          result = LLVM.LLVMBuildInsertValue(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), 0, "");
          return LLVM.LLVMBuildInsertValue(builder, result, tupledValue, 1, "");
        }
        return tupledValue;
      }
    }
    if (from instanceof TupleType && to instanceof TupleType)
    {
      if (!skipRuntimeChecks && from.canBeNullable() && !to.isNullable())
      {
        buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
      }

      TupleType fromTuple = (TupleType) from;
      TupleType toTuple = (TupleType) to;
      Type[] fromSubTypes = fromTuple.getSubTypes();
      Type[] toSubTypes = toTuple.getSubTypes();
      if (fromSubTypes.length != toSubTypes.length)
      {
        throw new IllegalArgumentException("Cannot convert from a " + from + " to a " + to);
      }
      boolean subTypesEquivalent = true;
      for (int i = 0; i < fromSubTypes.length; ++i)
      {
        if (!fromSubTypes[i].isRuntimeEquivalent(toSubTypes[i]))
        {
          subTypesEquivalent = false;
          break;
        }
      }
      if (subTypesEquivalent)
      {
        // just convert the nullability
        if (from.canBeNullable() && !to.isNullable())
        {
          // extract the value of the tuple from the nullable structure
          // (we have already done the associated null check above)
          return LLVM.LLVMBuildExtractValue(builder, value, 1, "");
        }
        if (!from.canBeNullable() && to.isNullable())
        {
          LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(to));
          // set the flag to one to indicate that this value is not null
          result = LLVM.LLVMBuildInsertValue(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), 0, "");
          return LLVM.LLVMBuildInsertValue(builder, result, value, 1, "");
        }
        throw new IllegalArgumentException("Unable to convert from a " + from + " to a " + to + " - their sub types and nullability are equivalent, but the types themselves are not");
      }

      LLVMValueRef isNotNullValue = null;
      LLVMValueRef tupleValue = value;
      if (from.canBeNullable())
      {
        isNotNullValue = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
        tupleValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
      }

      LLVMValueRef currentValue = LLVM.LLVMGetUndef(findTemporaryType(toTuple));
      for (int i = 0; i < fromSubTypes.length; i++)
      {
        LLVMValueRef current = LLVM.LLVMBuildExtractValue(builder, tupleValue, i, "");
        LLVMValueRef converted = convertTemporary(builder, landingPadContainer, current, fromSubTypes[i], toSubTypes[i], skipRuntimeChecks, fromAccessor, toAccessor);
        currentValue = LLVM.LLVMBuildInsertValue(builder, currentValue, converted, i, "");
      }

      if (to.isNullable())
      {
        LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(to));
        if (from.canBeNullable())
        {
          result = LLVM.LLVMBuildInsertValue(builder, result, isNotNullValue, 0, "");
        }
        else
        {
          // set the flag to one to indicate that this value is not null
          result = LLVM.LLVMBuildInsertValue(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), 0, "");
        }
        return LLVM.LLVMBuildInsertValue(builder, result, currentValue, 1, "");
      }
      // return the value directly, since the to type is not nullable
      return currentValue;
    }
    if ((from instanceof ObjectType || (from instanceof NamedType && ((NamedType) from).getResolvedTypeParameter() != null) || from instanceof WildcardType) &&
        to instanceof ObjectType)
    {
      // object casts are always legal
      if (!skipRuntimeChecks && from.canBeNullable() && !to.isNullable())
      {
        buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
      }
      // immutability will be checked by the type checker, and doesn't have any effect on the native type, so we do not need to do anything special here
      return value;
    }
    if (to instanceof ObjectType ||
        (to instanceof NamedType && ((NamedType) to).getResolvedTypeParameter() != null) ||
        to instanceof WildcardType)
    {
      // anything can convert to object
      if (from instanceof NullType)
      {
        if (!to.canBeNullable())
        {
          throw new IllegalArgumentException("Cannot convert from NullType to a not-null object/type parameter/wildcard type");
        }
        if (!skipRuntimeChecks && !to.isNullable() && !(to instanceof WildcardType && to.canBeNullable()))
        {
          // make sure to is actually nullable (if it is a TypeParameter, we can't check this at compile time)
          buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
        }
        return LLVM.LLVMConstNull(findTemporaryType(to));
      }
      if (from instanceof ArrayType ||
          (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition) ||
          (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof InterfaceDefinition) ||
          (from instanceof NamedType && ((NamedType) from).getResolvedTypeParameter() != null) ||
          from instanceof ObjectType ||
          from instanceof WildcardType)
      {
        if (!skipRuntimeChecks && from.canBeNullable() && !to.isNullable() && !(to instanceof WildcardType && to.canBeNullable()))
        {
          buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
        }
        if (!skipRuntimeChecks && ((to instanceof NamedType && ((NamedType) to).getResolvedTypeParameter() != null) || to instanceof WildcardType))
        {
          // build an instanceof check for the 'to' type
          LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "toTypeParamContinuation");
          LLVMValueRef notNullValue = value;
          Type notNullFromType = Type.findTypeWithNullability(from, false);
          if (from.canBeNullable())
          {
            // only do the instanceof check it if it is not null - if it is null then the conversion should succeed (if 'to' was nullable then we would already have failed)

            // if 'to' is not nullable, then we have already done the null check, so don't repeat it
            if (to.isNullable() || (to instanceof WildcardType && to.canBeNullable()))
            {
              LLVMBasicBlockRef notNullBlock = LLVM.LLVMAddBasicBlock(builder, "toTypeParamConversionNotNull");

              LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
              LLVM.LLVMBuildCondBr(builder, isNotNull, notNullBlock, continuationBlock);

              LLVM.LLVMPositionBuilderAtEnd(builder, notNullBlock);
            }
            // skip run-time checking on this nullable -> not-null conversion, as we have already done them
            notNullValue = convertTemporary(builder, landingPadContainer, value, from, notNullFromType, true, fromAccessor, fromAccessor);
          }
          LLVMValueRef isInstance = rttiHelper.buildInstanceOfCheck(builder, landingPadContainer, notNullValue, notNullFromType, to, fromAccessor, toAccessor);
          LLVMBasicBlockRef instanceOfFailureBlock = LLVM.LLVMAddBasicBlock(builder, "castToTypeParamInstanceOfFailure");
          LLVM.LLVMBuildCondBr(builder, isInstance, continuationBlock, instanceOfFailureBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfFailureBlock);
          buildThrowCastError(builder, landingPadContainer, from.toString(), to.toString(), null);

          LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
        }

        if (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof InterfaceDefinition)
        {
          // extract the object part of the interface's type
          return LLVM.LLVMBuildExtractValue(builder, value, 1, "");
        }

        // object, class, type parameter, wildcard, and array types can be safely bitcast to object types
        return LLVM.LLVMBuildBitCast(builder, value, findTemporaryType(to), "");
      }
      Type notNullFromType = Type.findTypeWithNullability(from, false);
      LLVMValueRef notNullValue = value;
      LLVMBasicBlockRef startBlock = null;
      LLVMBasicBlockRef notNullBlock;
      LLVMBasicBlockRef continuationBlock = null;
      if (from.canBeNullable())
      {
        if (to.isNullable() || (to instanceof WildcardType && to.canBeNullable()))
        {
          continuationBlock = LLVM.LLVMAddBasicBlock(builder, "toObjectConversionContinuation");
          notNullBlock = LLVM.LLVMAddBasicBlock(builder, "toObjectConversionNotNull");

          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
          startBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildCondBr(builder, isNotNull, notNullBlock, continuationBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, notNullBlock);
        }
        else if (!skipRuntimeChecks)
        {
          buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
        }
        // skip run-time checking on this nullable -> not-null conversion, as we have already done them
        notNullValue = convertTemporary(builder, landingPadContainer, value, from, notNullFromType, true, fromAccessor, fromAccessor);
      }

      if (!skipRuntimeChecks && ((to instanceof NamedType && ((NamedType) to).getResolvedTypeParameter() != null) || to instanceof WildcardType))
      {
        // do an instanceof check to make sure we are allowed to convert to this type parameter/wildcard
        LLVMValueRef isInstanceOfToType = rttiHelper.buildInstanceOfCheck(builder, landingPadContainer, notNullValue, notNullFromType, to, fromAccessor, toAccessor);
        LLVMBasicBlockRef instanceOfSuccessBlock = LLVM.LLVMAddBasicBlock(builder, "castObjectInstanceOfSuccess");
        LLVMBasicBlockRef instanceOfFailureBlock = LLVM.LLVMAddBasicBlock(builder, "castObjectInstanceOfFailure");
        LLVM.LLVMBuildCondBr(builder, isInstanceOfToType, instanceOfSuccessBlock, instanceOfFailureBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfFailureBlock);
        buildThrowCastError(builder, landingPadContainer, from.toString(), to.toString(), null);

        LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfSuccessBlock);
      }

      LLVMTypeRef nativeType = LLVM.LLVMPointerType(findSpecialisedObjectType(notNullFromType), 0);
      // allocate memory for the object
      LLVMValueRef pointer = codeGenerator.buildHeapAllocation(builder, nativeType);

      // store the object's run-time type information
      LLVMValueRef rtti;
      if (notNullFromType instanceof FunctionType)
      {
        // for function types, take the RTTI out of the value, don't generate it from the static type
        rtti = LLVM.LLVMBuildExtractValue(builder, notNullValue, 0, "");
      }
      else
      {
        // make sure this type's RTTI doesn't reference any type parameters
        // otherwise, the RTTI would turn out wrong (due to the TypeParameterAccessor) and people would be able to cast
        // things in ways that would crash at runtime: e.g. (uint, T) to object to (uint, string) if T was string
        // to fix this, we set the RTTI to (uint, object), which is the only type that this should actually be castable to once it is an object
        rtti = rttiHelper.buildRTTICreation(builder, stripTypeParameters(notNullFromType), fromAccessor);
      }
      LLVMValueRef rttiPointer = rttiHelper.getRTTIPointer(builder, pointer);
      LLVM.LLVMBuildStore(builder, rtti, rttiPointer);

      // build the base change VFT, and store it as the object's VFT
      LLVMValueRef baseChangeVFT = virtualFunctionHandler.getBaseChangeObjectVFT(notNullFromType);
      LLVMValueRef vftElementPointer = virtualFunctionHandler.getFirstVirtualFunctionTablePointer(builder, pointer);
      LLVM.LLVMBuildStore(builder, baseChangeVFT, vftElementPointer);

      // store the value inside the object
      LLVMValueRef elementPointer = LLVM.LLVMBuildStructGEP(builder, pointer, 2, "");
      notNullValue = convertTemporaryToStandard(builder, notNullValue, notNullFromType);
      LLVM.LLVMBuildStore(builder, notNullValue, elementPointer);

      // cast away the part of the type that contains the value
      LLVMValueRef notNullResult = LLVM.LLVMBuildBitCast(builder, pointer, findTemporaryType(to), "");

      if (from.canBeNullable() && (to.isNullable() || (to instanceof WildcardType && to.canBeNullable())))
      {
        LLVMBasicBlockRef endNotNullBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildBr(builder, continuationBlock);
        LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);

        LLVMValueRef resultPhi = LLVM.LLVMBuildPhi(builder, findTemporaryType(to), "");
        LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstNull(findTemporaryType(to)), notNullResult};
        LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endNotNullBlock};
        LLVM.LLVMAddIncoming(resultPhi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
        return resultPhi;
      }
      return notNullResult;
    }
    if (from instanceof ObjectType ||
        (from instanceof NamedType && ((NamedType) from).getResolvedTypeParameter() != null) ||
        from instanceof WildcardType)
    {
      LLVMValueRef notNullValue = value;
      LLVMBasicBlockRef startBlock = null;
      LLVMBasicBlockRef notNullBlock = null;
      LLVMBasicBlockRef continuationBlock = null;
      if (from.canBeNullable())
      {
        if (to.isNullable() || (to instanceof WildcardType && to.canBeNullable()))
        {
          continuationBlock = LLVM.LLVMAddBasicBlock(builder, "fromObjectConversionContinuation");
          notNullBlock = LLVM.LLVMAddBasicBlock(builder, "fromObjectConversionNotNull");

          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
          startBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildCondBr(builder, isNotNull, notNullBlock, continuationBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, notNullBlock);
        }
        else if (!skipRuntimeChecks)
        {
          buildCastNullCheck(builder, landingPadContainer, value, from, to, toAccessor);
        }
        // skip run-time checking on this nullable -> not-null conversion, as we have already done them
        notNullValue = convertTemporary(builder, landingPadContainer, value, from, Type.findTypeWithNullability(from, false), true, fromAccessor, fromAccessor);
      }

      if (!skipRuntimeChecks)
      {
        LLVMValueRef isInstanceOfToType = rttiHelper.buildInstanceOfCheck(builder, landingPadContainer, notNullValue, Type.findTypeWithNullability(from, false), to, fromAccessor, toAccessor);
        LLVMBasicBlockRef instanceOfSuccessBlock = LLVM.LLVMAddBasicBlock(builder, "castObjectInstanceOfSuccess");
        LLVMBasicBlockRef instanceOfFailureBlock = LLVM.LLVMAddBasicBlock(builder, "castObjectInstanceOfFailure");
        LLVM.LLVMBuildCondBr(builder, isInstanceOfToType, instanceOfSuccessBlock, instanceOfFailureBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfFailureBlock);
        buildThrowCastError(builder, landingPadContainer, from.toString(), to.toString(), null);

        LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfSuccessBlock);
      }

      LLVMValueRef notNullResult;
      if ((to instanceof NamedType && ((NamedType) to).getResolvedTypeDefinition() instanceof ClassDefinition) ||
          to instanceof ArrayType)
      {
        notNullResult = LLVM.LLVMBuildBitCast(builder, notNullValue, findTemporaryType(to), "");
      }
      else
      {
        Type notNullToType = Type.findTypeWithNullability(to, false);
        LLVMTypeRef nativeType = LLVM.LLVMPointerType(findSpecialisedObjectType(notNullToType), 0);
        LLVMValueRef castedValue = LLVM.LLVMBuildBitCast(builder, notNullValue, nativeType, "");

        LLVMValueRef elementPointer = LLVM.LLVMBuildStructGEP(builder, castedValue, 2, "");
        notNullResult = convertStandardPointerToTemporary(builder, elementPointer, notNullToType);
        notNullResult = convertTemporary(builder, landingPadContainer, notNullResult, notNullToType, to, skipRuntimeChecks, toAccessor, toAccessor);
      }

      if (from.canBeNullable() && (to.isNullable() || (to instanceof WildcardType && to.canBeNullable())))
      {
        LLVMBasicBlockRef endNotNullBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildBr(builder, continuationBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);

        LLVMValueRef resultPhi = LLVM.LLVMBuildPhi(builder, findTemporaryType(to), "");
        LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstNull(findTemporaryType(to)), notNullResult};
        LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endNotNullBlock};
        LLVM.LLVMAddIncoming(resultPhi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
        return resultPhi;
      }
      return notNullResult;
    }
    throw new IllegalArgumentException("Unknown type conversion, from '" + from + "' to '" + to + "'");
  }

  /**
   * Converts the specified value from the specified 'from' PrimitiveType to the specified 'to' PrimitiveType.
   * @param value - the value to convert
   * @param from - the PrimitiveType to convert from
   * @param to - the PrimitiveType to convert to
   * @return the converted value
   */
  private LLVMValueRef convertPrimitiveType(LLVMBuilderRef builder, LLVMValueRef value, PrimitiveType from, PrimitiveType to)
  {
    PrimitiveTypeType fromType = from.getPrimitiveTypeType();
    PrimitiveTypeType toType = to.getPrimitiveTypeType();
    if (fromType == toType && from.canBeNullable() == to.canBeNullable())
    {
      return value;
    }
    LLVMValueRef primitiveValue = value;
    if (from.canBeNullable())
    {
      primitiveValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
    }
    // perform the conversion
    LLVMTypeRef toNativeType = findTemporaryType(new PrimitiveType(false, toType, null));
    if (fromType == toType)
    {
      // do not alter primitiveValue, we only need to change the nullability
    }
    else if (fromType.isFloating() && toType.isFloating())
    {
      primitiveValue = LLVM.LLVMBuildFPCast(builder, primitiveValue, toNativeType, "");
    }
    else if (fromType.isFloating() && !toType.isFloating())
    {
      if (toType.isSigned())
      {
        primitiveValue = LLVM.LLVMBuildFPToSI(builder, primitiveValue, toNativeType, "");
      }
      else
      {
        primitiveValue = LLVM.LLVMBuildFPToUI(builder, primitiveValue, toNativeType, "");
      }
    }
    else if (!fromType.isFloating() && toType.isFloating())
    {
      if (fromType.isSigned())
      {
        primitiveValue = LLVM.LLVMBuildSIToFP(builder, primitiveValue, toNativeType, "");
      }
      else
      {
        primitiveValue = LLVM.LLVMBuildUIToFP(builder, primitiveValue, toNativeType, "");
      }
    }
    // both integer types, so perform a sign-extend, zero-extend, or truncation
    else if (fromType.getBitCount() > toType.getBitCount())
    {
      primitiveValue = LLVM.LLVMBuildTrunc(builder, primitiveValue, toNativeType, "");
    }
    else if (fromType.getBitCount() == toType.getBitCount() && fromType.isSigned() != toType.isSigned())
    {
      primitiveValue = LLVM.LLVMBuildBitCast(builder, primitiveValue, toNativeType, "");
    }
    // the value needs extending, so decide whether to do a sign-extend or a zero-extend based on whether the from type is signed
    else if (fromType.isSigned())
    {
      primitiveValue = LLVM.LLVMBuildSExt(builder, primitiveValue, toNativeType, "");
    }
    else
    {
      primitiveValue = LLVM.LLVMBuildZExt(builder, primitiveValue, toNativeType, "");
    }
    // pack up the result before returning it
    if (to.canBeNullable())
    {
      LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(to));
      if (from.canBeNullable())
      {
        LLVMValueRef isNotNullValue = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
        result = LLVM.LLVMBuildInsertValue(builder, result, isNotNullValue, 0, "");
      }
      else
      {
        // set the flag to one to indicate that this value is not null
        result = LLVM.LLVMBuildInsertValue(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), 0, "");
      }
      return LLVM.LLVMBuildInsertValue(builder, result, primitiveValue, 1, "");
    }
    // return the primitive value directly, since the to type is not nullable
    return primitiveValue;
  }

  /**
   * Builds a conversion of the specified value from the specified type into the string type, by calling toString() or using the constant "null" string as necessary.
   * @param builder - the builder to build the conversion with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the value to convert to a string, in a temporary type representation
   * @param type - the type of the value to convert, which can be any type except VoidType (including NullType and any nullable types)
   * @param typeAccessor - the TypeParameterAccessor to get the values of any TypeParameters inside 'type' from
   * @return an LLVMValueRef containing the string representation of value, in a standard type representation
   */
  public LLVMValueRef convertToString(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef value, Type type, TypeParameterAccessor typeAccessor)
  {
    if (type.isRuntimeEquivalent(SpecialTypeHandler.STRING_TYPE))
    {
      return convertTemporaryToStandard(builder, value, SpecialTypeHandler.STRING_TYPE);
    }
    if (type instanceof NullType)
    {
      LLVMValueRef stringValue = codeGenerator.buildStringCreation(builder, landingPadContainer, "null");
      return convertTemporaryToStandard(builder, stringValue, SpecialTypeHandler.STRING_TYPE);
    }
    LLVMValueRef notNullValue = value;
    Type notNullType = type;
    LLVMBasicBlockRef alternativeBlock = null;
    LLVMBasicBlockRef continuationBlock = null;
    if (type.canBeNullable())
    {
      continuationBlock = LLVM.LLVMAddBasicBlock(builder, "stringConversionContinuation");
      alternativeBlock = LLVM.LLVMAddBasicBlock(builder, "stringConversionNull");
      LLVMBasicBlockRef conversionBlock = LLVM.LLVMAddBasicBlock(builder, "stringConversion");

      LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, type);
      LLVM.LLVMBuildCondBr(builder, isNotNull, conversionBlock, alternativeBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, conversionBlock);
      notNullType = Type.findTypeWithNullability(type, false);
      notNullValue = convertTemporary(builder, landingPadContainer, value, type, notNullType, false, typeAccessor, typeAccessor);
    }
    MethodReference methodReference = notNullType.getMethod(new MethodReference(new BuiltinMethod(notNullType, BuiltinMethodType.TO_STRING), GenericTypeSpecialiser.IDENTITY_SPECIALISER).getDisambiguator());
    if (methodReference == null)
    {
      throw new IllegalStateException("Type " + type + " does not have a 'toString()' method!");
    }

    LLVMValueRef[] arguments = new LLVMValueRef[] {};
    LLVMValueRef stringValue = buildMethodCall(builder, landingPadContainer, notNullValue, notNullType, methodReference, arguments, typeAccessor);

    if (type.canBeNullable())
    {
      LLVMBasicBlockRef endConversionBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, alternativeBlock);
      LLVMValueRef alternativeStringValue = codeGenerator.buildStringCreation(builder, landingPadContainer, "null");
      LLVMBasicBlockRef endAlternativeBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
      LLVMValueRef phi = LLVM.LLVMBuildPhi(builder, findTemporaryType(SpecialTypeHandler.STRING_TYPE), "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {stringValue, alternativeStringValue};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {endConversionBlock, endAlternativeBlock};
      LLVM.LLVMAddIncoming(phi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
      return convertTemporaryToStandard(builder, phi, SpecialTypeHandler.STRING_TYPE);
    }
    return convertTemporaryToStandard(builder, stringValue, SpecialTypeHandler.STRING_TYPE);
  }

  /**
   * Converts the specified value of the specified type from a temporary type representation to a standard type representation, after converting it from 'fromType' to 'toType'.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the value to convert
   * @param fromType - the type to convert from
   * @param toType - the type to convert to
   * @param fromAccessor - the TypeParameterAccessor to get the values of any TypeParameters inside 'from' from
   * @param toAccessor - the TypeParameterAccessor to get the values of any TypeParameters inside 'to' from
   * @return the converted value
   */
  public LLVMValueRef convertTemporaryToStandard(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef value, Type fromType, Type toType, TypeParameterAccessor fromAccessor, TypeParameterAccessor toAccessor)
  {
    LLVMValueRef temporary = convertTemporary(builder, landingPadContainer, value, fromType, toType, false, fromAccessor, toAccessor);
    return convertTemporaryToStandard(builder, temporary, toType);
  }

  /**
   * Converts the specified value of the specified type from a temporary type representation to a standard type representation.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param value - the value to convert
   * @param type - the type to convert
   * @return the converted value
   */
  public LLVMValueRef convertTemporaryToStandard(LLVMBuilderRef builder, LLVMValueRef value, Type type)
  {
    if (type instanceof ArrayType)
    {
      // the temporary and standard types are the same for ArrayTypes
      return value;
    }
    if (type instanceof FunctionType)
    {
      // the temporary and standard types are the same for FunctionTypes
      return value;
    }
    if (type instanceof NamedType)
    {
      if (((NamedType) type).getResolvedTypeParameter() != null)
      {
        // the temporary and standard types are the same for type parameters
        return value;
      }
      TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        // the temporary and standard types are the same for class types
        return value;
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        // the temporary and standard types are the same for interface types
        return value;
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        if (type.canBeNullable())
        {
          LLVMTypeRef standardType = findStandardType(type);
          // we are converting from a pointer to a non-nullable compound into a possibly-null compound
          LLVMValueRef isNotNullValue = LLVM.LLVMBuildIsNotNull(builder, value, "");
          // we need to branch on isNotNullValue, to decide whether to load from the pointer
          LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVMBasicBlockRef convertedBlock = LLVM.LLVMAddBasicBlock(builder, "compoundConverted");
          LLVMBasicBlockRef loadBlock = LLVM.LLVMAddBasicBlock(builder, "compoundConversion");

          LLVM.LLVMBuildCondBr(builder, isNotNullValue, loadBlock, convertedBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, loadBlock);
          LLVMValueRef loaded = LLVM.LLVMBuildLoad(builder, value, "");
          LLVMValueRef notNullResult = LLVM.LLVMBuildInsertValue(builder, LLVM.LLVMGetUndef(standardType), LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), 0, "");
          notNullResult = LLVM.LLVMBuildInsertValue(builder, notNullResult, loaded, 1, "");
          LLVM.LLVMBuildBr(builder, convertedBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, convertedBlock);
          LLVMValueRef phi = LLVM.LLVMBuildPhi(builder, standardType, "");
          LLVMValueRef nullResult = LLVM.LLVMConstNull(standardType);
          LLVMValueRef[] values = new LLVMValueRef[] {nullResult, notNullResult};
          LLVMBasicBlockRef[] blocks = new LLVMBasicBlockRef[] {currentBlock, loadBlock};
          LLVM.LLVMAddIncoming(phi, C.toNativePointerArray(values, false, true), C.toNativePointerArray(blocks, false, true), values.length);
          return phi;
        }
        // type is not nullable, so we can just load it directly
        return LLVM.LLVMBuildLoad(builder, value, "");
      }
    }
    if (type instanceof NullType)
    {
      // the temporary and standard types are the same for NullTypes
      return value;
    }
    if (type instanceof ObjectType)
    {
      // the temporary and standard types are the same for ObjectTypes
      return value;
    }
    if (type instanceof PrimitiveType)
    {
      // the temporary and standard types are the same for PrimitiveTypes
      return value;
    }
    if (type instanceof TupleType)
    {
      boolean containsCompound = false;
      Queue<TupleType> typeQueue = new LinkedList<TupleType>();
      typeQueue.add((TupleType) type);
      while (!typeQueue.isEmpty())
      {
        TupleType currentType = typeQueue.poll();
        for (Type subType : currentType.getSubTypes())
        {
          if (subType instanceof TupleType)
          {
            typeQueue.add((TupleType) subType);
          }
          if (subType instanceof NamedType && ((NamedType) subType).getResolvedTypeDefinition() instanceof CompoundDefinition)
          {
            containsCompound = true;
            break;
          }
        }
      }
      if (!containsCompound)
      {
        // if this tuple does not contain any compound types (after an arbitrary degree of nesting),
        // then it does not need converting, as the standard and temporary representations are the same
        return value;
      }

      LLVMValueRef notNullValue = value;
      if (type.canBeNullable())
      {
        notNullValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
      }
      LLVMValueRef resultNotNull = LLVM.LLVMGetUndef(findStandardType(Type.findTypeWithNullability(type, false)));
      Type[] subTypes = ((TupleType) type).getSubTypes();
      for (int i = 0; i < subTypes.length; ++i)
      {
        LLVMValueRef extractedValue = LLVM.LLVMBuildExtractValue(builder, notNullValue, i, "");
        LLVMValueRef convertedValue = convertTemporaryToStandard(builder, extractedValue, subTypes[i]);
        resultNotNull = LLVM.LLVMBuildInsertValue(builder, resultNotNull, convertedValue, i, "");
      }
      if (type.canBeNullable())
      {
        LLVMValueRef isNotNullValue = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
        LLVMValueRef result = LLVM.LLVMGetUndef(findStandardType(type));
        result = LLVM.LLVMBuildInsertValue(builder, result, isNotNullValue, 0, "");
        result = LLVM.LLVMBuildInsertValue(builder, result, resultNotNull, 1, "");
        return result;
      }
      return resultNotNull;
    }
    if (type instanceof VoidType)
    {
      throw new IllegalArgumentException("VoidType has no standard representation");
    }
    if (type instanceof WildcardType)
    {
      // the temporary and standard types are the same for WildcardTypes
      return value;
    }
    throw new IllegalArgumentException("Unknown type: " + type);
  }

  /**
   * Converts the specified value of the specified type from a standard type representation to a temporary type representation, before converting it from 'fromType' to 'toType'.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the value to convert
   * @param fromType - the type to convert from
   * @param toType - the type to convert to
   * @param fromAccessor - the TypeParameterAccessor to get the values of any TypeParameters inside 'from' from
   * @param toAccessor - the TypeParameterAccessor to get the values of any TypeParameters inside 'to' from
   * @return the converted value
   */
  public LLVMValueRef convertStandardToTemporary(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef value, Type fromType, Type toType, TypeParameterAccessor fromAccessor, TypeParameterAccessor toAccessor)
  {
    LLVMValueRef temporary = convertStandardToTemporary(builder, value, fromType);
    return convertTemporary(builder, landingPadContainer, temporary, fromType, toType, false, fromAccessor, toAccessor);
  }

  /**
   * Converts the specified value of the specified type from a standard type representation to a temporary type representation.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param value - the value to convert
   * @param type - the type to convert
   * @return the converted value
   */
  public LLVMValueRef convertStandardToTemporary(LLVMBuilderRef builder, LLVMValueRef value, Type type)
  {
    if (type instanceof ArrayType)
    {
      // the temporary and standard types are the same for ArrayTypes
      return value;
    }
    if (type instanceof FunctionType)
    {
      // the temporary and standard types are the same for FunctionTypes
      return value;
    }
    if (type instanceof NamedType)
    {
      if (((NamedType) type).getResolvedTypeParameter() != null)
      {
        // the temporary and standard types are the same for type parameters
        return value;
      }
      TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        // the temporary and standard types are the same for class types
        return value;
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        // the temporary and standard types are the same for interface types
        return value;
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        LLVMValueRef notNullValue = value;
        if (type.canBeNullable())
        {
          notNullValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
        }

        // find the type to alloca, which is the standard representation of a non-nullable version of this type
        // when we alloca this type, it becomes equivalent to the temporary type representation of this compound type (with any nullability)
        LLVMTypeRef allocaBaseType = findStandardType(Type.findTypeWithNullability(type, false));
        LLVMValueRef alloca = LLVM.LLVMBuildAllocaInEntryBlock(builder, allocaBaseType, "");
        LLVM.LLVMBuildStore(builder, notNullValue, alloca);
        if (type.canBeNullable())
        {
          LLVMValueRef isNotNullValue = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
          return LLVM.LLVMBuildSelect(builder, isNotNullValue, alloca, LLVM.LLVMConstNull(findTemporaryType(type)), "");
        }
        return alloca;
      }
    }
    if (type instanceof NullType)
    {
      // the temporary and standard types are the same for NullTypes
      return value;
    }
    if (type instanceof ObjectType)
    {
      // the temporary and standard types are the same for ObjectTypes
      return value;
    }
    if (type instanceof PrimitiveType)
    {
      // the temporary and standard types are the same for PrimitiveTypes
      return value;
    }
    if (type instanceof TupleType)
    {
      boolean containsCompound = false;
      Queue<TupleType> typeQueue = new LinkedList<TupleType>();
      typeQueue.add((TupleType) type);
      while (!typeQueue.isEmpty())
      {
        TupleType currentType = typeQueue.poll();
        for (Type subType : currentType.getSubTypes())
        {
          if (subType instanceof TupleType)
          {
            typeQueue.add((TupleType) subType);
          }
          if (subType instanceof NamedType && ((NamedType) subType).getResolvedTypeDefinition() instanceof CompoundDefinition)
          {
            containsCompound = true;
            break;
          }
        }
      }
      if (!containsCompound)
      {
        // if this tuple does not contain any compound types (after an arbitrary degree of nesting),
        // then it does not need converting, as the standard and temporary representations are the same
        return value;
      }

      LLVMValueRef notNullValue = value;
      if (type.canBeNullable())
      {
        notNullValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
      }
      LLVMValueRef resultNotNull = LLVM.LLVMGetUndef(findTemporaryType(Type.findTypeWithNullability(type, false)));
      Type[] subTypes = ((TupleType) type).getSubTypes();
      for (int i = 0; i < subTypes.length; ++i)
      {
        LLVMValueRef extractedValue = LLVM.LLVMBuildExtractValue(builder, notNullValue, i, "");
        LLVMValueRef convertedValue = convertStandardToTemporary(builder, extractedValue, subTypes[i]);
        resultNotNull = LLVM.LLVMBuildInsertValue(builder, resultNotNull, convertedValue, i, "");
      }
      if (type.canBeNullable())
      {
        LLVMValueRef isNotNullValue = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
        LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(type));
        result = LLVM.LLVMBuildInsertValue(builder, result, isNotNullValue, 0, "");
        result = LLVM.LLVMBuildInsertValue(builder, result, resultNotNull, 1, "");
        return result;
      }
      return resultNotNull;
    }
    if (type instanceof VoidType)
    {
      throw new IllegalArgumentException("VoidType has no temporary representation");
    }
    if (type instanceof WildcardType)
    {
      // the temporary and standard types are the same for WildcardTypes
      return value;
    }
    throw new IllegalArgumentException("Unknown type: " + type);
  }

  /**
   * Converts the specified pointer to a value of the specified type from a pointer to a standard type representation to a temporary type representation, before converting it from 'fromType' to 'toType'.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param pointer - the pointer to the value to convert
   * @param fromType - the type to convert from
   * @param toType - the type to convert to
   * @param fromAccessor - the TypeParameterAccessor to get the values of any TypeParameters inside 'from' from
   * @param toAccessor - the TypeParameterAccessor to get the values of any TypeParameters inside 'to' from
   * @return the converted value
   */
  public LLVMValueRef convertStandardPointerToTemporary(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef pointer, Type fromType, Type toType, TypeParameterAccessor fromAccessor, TypeParameterAccessor toAccessor)
  {
    LLVMValueRef temporary = convertStandardPointerToTemporary(builder, pointer, fromType);
    return convertTemporary(builder, landingPadContainer, temporary, fromType, toType, false, fromAccessor, toAccessor);
  }

  /**
   * Converts the specified pointer to a value of the specified type from a pointer to a standard type representation to a temporary type representation.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param value - the pointer to the value to convert
   * @param type - the type to convert
   * @return the converted value
   */
  public LLVMValueRef convertStandardPointerToTemporary(LLVMBuilderRef builder, LLVMValueRef value, Type type)
  {
    if (type instanceof ArrayType)
    {
      // the temporary and standard types are the same for ArrayTypes
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    if (type instanceof FunctionType)
    {
      // the temporary and standard types are the same for FunctionTypes
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    if (type instanceof NamedType)
    {
      if (((NamedType) type).getResolvedTypeParameter() != null)
      {
        // the temporary and standard types are the same for type parameters
        return LLVM.LLVMBuildLoad(builder, value, "");
      }
      TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        // the temporary and standard types are the same for class types
        return LLVM.LLVMBuildLoad(builder, value, "");
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        // the temporary and standard types are the same for interface types
        return LLVM.LLVMBuildLoad(builder, value, "");
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        if (type.canBeNullable())
        {
          LLVMValueRef[] nullabilityIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                                  LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false)};
          LLVMValueRef isNotNullPointer = LLVM.LLVMBuildGEP(builder, value, C.toNativePointerArray(nullabilityIndices, false, true), nullabilityIndices.length, "");
          LLVMValueRef isNotNullValue = LLVM.LLVMBuildLoad(builder, isNotNullPointer, "");

          LLVMValueRef[] valueIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                            LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false)};
          LLVMValueRef notNullValue = LLVM.LLVMBuildGEP(builder, value, C.toNativePointerArray(valueIndices, false, true), valueIndices.length, "");
          return LLVM.LLVMBuildSelect(builder, isNotNullValue, notNullValue, LLVM.LLVMConstNull(findTemporaryType(type)), "");
        }
        // the pointer to the standard non-nullable representation is the same as the temporary representation
        return value;
      }
    }
    if (type instanceof NullType)
    {
      // the temporary and standard types are the same for NullTypes
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    if (type instanceof ObjectType)
    {
      // the temporary and standard types are the same for ObjectTypes
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    if (type instanceof PrimitiveType)
    {
      // the temporary and standard types are the same for PrimitiveTypes
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    if (type instanceof TupleType)
    {
      boolean containsCompound = false;
      Queue<TupleType> typeQueue = new LinkedList<TupleType>();
      typeQueue.add((TupleType) type);
      while (!typeQueue.isEmpty())
      {
        TupleType currentType = typeQueue.poll();
        for (Type subType : currentType.getSubTypes())
        {
          if (subType instanceof TupleType)
          {
            typeQueue.add((TupleType) subType);
          }
          if (subType instanceof NamedType && ((NamedType) subType).getResolvedTypeDefinition() instanceof CompoundDefinition)
          {
            containsCompound = true;
            break;
          }
        }
      }
      if (!containsCompound)
      {
        // if this tuple does not contain any compound types (after an arbitrary degree of nesting),
        // then it does not need converting, as the standard and temporary representations are the same
        return LLVM.LLVMBuildLoad(builder, value, "");
      }

      LLVMValueRef isNotNullValue = null;
      LLVMValueRef notNullPointer = value;
      if (type.canBeNullable())
      {
        LLVMValueRef[] nullabilityIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                                LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false)};
        LLVMValueRef isNotNullPointer = LLVM.LLVMBuildGEP(builder, value, C.toNativePointerArray(nullabilityIndices, false, true), nullabilityIndices.length, "");
        isNotNullValue = LLVM.LLVMBuildLoad(builder, isNotNullPointer, "");

        LLVMValueRef[] valueIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                          LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false)};
        notNullPointer = LLVM.LLVMBuildGEP(builder, value, C.toNativePointerArray(valueIndices, false, true), valueIndices.length, "");
      }
      LLVMValueRef resultNotNull = LLVM.LLVMGetUndef(findTemporaryType(Type.findTypeWithNullability(type, false)));
      Type[] subTypes = ((TupleType) type).getSubTypes();
      for (int i = 0; i < subTypes.length; ++i)
      {
        LLVMValueRef[] valueIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                          LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), i, false)};
        LLVMValueRef valuePointer = LLVM.LLVMBuildGEP(builder, notNullPointer, C.toNativePointerArray(valueIndices, false, true), valueIndices.length, "");
        LLVMValueRef convertedValue = convertStandardPointerToTemporary(builder, valuePointer, subTypes[i]);
        resultNotNull = LLVM.LLVMBuildInsertValue(builder, resultNotNull, convertedValue, i, "");
      }
      if (type.canBeNullable())
      {
        LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(type));
        result = LLVM.LLVMBuildInsertValue(builder, result, isNotNullValue, 0, "");
        result = LLVM.LLVMBuildInsertValue(builder, result, resultNotNull, 1, "");
        return result;
      }
      return resultNotNull;
    }
    if (type instanceof VoidType)
    {
      throw new IllegalArgumentException("VoidType has no standard representation");
    }
    if (type instanceof WildcardType)
    {
      // the temporary and standard types are the same for WildcardTypes
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    throw new IllegalArgumentException("Unknown type: " + type);
  }

}
