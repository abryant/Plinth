package eu.bryants.anthony.plinth.compiler.passes.llvm;

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
import eu.bryants.anthony.plinth.ast.metadata.GenericTypeSpecialiser;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.NullType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.ast.type.WildcardType;

/*
 * Created on 23 Jan 2013
 */

/**
 * This Run-Time Type Information Helper provides methods for generating and retrieving run-time type information.
 * @author Anthony Bryant
 */
public class RTTIHelper
{
  private static final String FORCE_TYPE_MODIFIERS_FUNCTION_NAME = "plinth_force_type_modifiers";
  private static final String CHECK_TYPE_MATCHES_FUNCTION_NAME = "plinth_check_type_matches";
  private static final String SUPER_TYPE_LOOKUP_FUNCTION_NAME = "plinth_core_find_super_type";

  private static final String RTTI_MANGLED_NAME_PREFIX = "_RTTI_";
  public static final byte OBJECT_SORT_ID = 1;
  public static final byte PRIMITIVE_SORT_ID = 2;
  public static final byte ARRAY_SORT_ID = 3;
  public static final byte TUPLE_SORT_ID = 4;
  public static final byte FUNCTION_SORT_ID = 5;
  public static final byte CLASS_SORT_ID = 6;
  public static final byte COMPOUND_SORT_ID = 7;
  public static final byte INTERFACE_SORT_ID = 8;
  public static final byte VOID_SORT_ID = 9;
  public static final byte NULL_SORT_ID = 10;
  public static final byte TYPE_PARAMETER_SORT_ID = 11;
  public static final byte WILDCARD_SORT_ID = 12;

  private LLVMModuleRef module;

  private LLVMTypeRef typeSearchListType;

  private CodeGenerator codeGenerator;
  private TypeHelper typeHelper;
  private VirtualFunctionHandler virtualFunctionHandler;

  public RTTIHelper(LLVMModuleRef module, CodeGenerator codeGenerator, TypeHelper typeHelper, VirtualFunctionHandler virtualFunctionHandler)
  {
    this.module = module;
    this.codeGenerator = codeGenerator;
    this.typeHelper = typeHelper;
    this.virtualFunctionHandler = virtualFunctionHandler;
  }

  /**
   * Gets the RTTI pointer on the specified object value.
   * @param builder - the builder to build the GEP instruction with
   * @param baseValue - the object-typed value to get the RTTI pointer for
   * @return the pointer to the field inside the specified base value which should contain a pointer to the object's RTTI
   */
  public LLVMValueRef getRTTIPointer(LLVMBuilderRef builder, LLVMValueRef baseValue)
  {
    // the object's RTTI is always stored at index 0 in an object structure
    return LLVM.LLVMBuildStructGEP(builder, baseValue, 0, "");
  }

  /**
   * Finds a pointer to the type argument mapper inside the specified NamedType RTTI block.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param namedRTTI - the RTTI block (which represents a NamedType) to look up the TypeArgumentMapper inside
   * @return a pointer to the TypeArgumentMapper inside the specified NamedType RTTI block
   */
  public LLVMValueRef getNamedTypeArgumentMapper(LLVMBuilderRef builder, LLVMValueRef namedRTTI)
  {
    return LLVM.LLVMBuildStructGEP(builder, namedRTTI, 5, "");
  }

  /**
   * @return the super-type lookup function (see the core runtime's 'typeinfo.ll' file)
   */
  private LLVMValueRef getSuperTypeLookupFunction()
  {
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, SUPER_TYPE_LOOKUP_FUNCTION_NAME);
    if (existingFunction != null)
    {
      return existingFunction;
    }
    LLVMTypeRef rttiType = getGenericRTTIType();
    LLVMTypeRef typeArgumentMapperType = getGenericTypeArgumentMapperType();
    LLVMTypeRef[] parameterTypes = new LLVMTypeRef[] {rttiType, rttiType, typeArgumentMapperType};
    LLVMTypeRef[] returnTypes = new LLVMTypeRef[] {getGenericRTTIType(), LLVM.LLVMPointerType(virtualFunctionHandler.getGenericVFTType(), 0)};
    LLVMTypeRef resultType = LLVM.LLVMStructType(C.toNativePointerArray(returnTypes, false, true), returnTypes.length, false);
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(parameterTypes, false, true), parameterTypes.length, false);
    LLVMValueRef function = LLVM.LLVMAddFunction(module, SUPER_TYPE_LOOKUP_FUNCTION_NAME, functionType);
    return function;
  }

  /**
   * Builds code to lookup the specified super type inside the specified object's type search list.
   * @param builder - the builder to build code with
   * @param objectRTTI - the RTTI of the object to do the look-up on
   * @param searchType - the type to search for
   * @param searchAccessor - the TypeParameterAccessor that provides a way of getting at all possible TypeParameters inside searchType
   * @return an LLVMValueRef representing a tuple of the looked-up-type's RTTI and a pointer to the corresponding VFT, or a tuple of two null pointers if this object does not implement the specified search type
   */
  public LLVMValueRef lookupInstanceSuperType(LLVMBuilderRef builder, LLVMValueRef objectRTTI, Type searchType, TypeParameterAccessor searchAccessor)
  {
    LLVMValueRef searchTypeRTTI = getRTTI(searchType);
    LLVMValueRef searchTypeMapper = searchAccessor.getTypeArgumentMapper();

    LLVMValueRef[] arguments = new LLVMValueRef[] {objectRTTI, searchTypeRTTI, searchTypeMapper};
    LLVMValueRef superTypeLookupFunction = getSuperTypeLookupFunction();
    LLVMValueRef result = LLVM.LLVMBuildCall(builder, superTypeLookupFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");
    return result;
  }

  /**
   * Looks up the name of the class in the specified RTTI.
   * Assumes that baseValue is a class's RTTI value.
   * @param builder - the builder to build the lookup code with
   * @param rttiPointer - the pointer to the RTTI block to look up the class name inside
   * @return a []ubyte holding the class's fully qualified name
   */
  public LLVMValueRef lookupNamedTypeName(LLVMBuilderRef builder, LLVMValueRef rttiPointer)
  {
    // cast the generic RTTI struct to something that looks like the start of a NamedType RTTI struct
    LLVMTypeRef typeSearchListType = LLVM.LLVMPointerType(getTypeSearchListType(), 0);
    LLVMTypeRef sortIdType = LLVM.LLVMInt8Type();
    LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
    LLVMTypeRef immutableType = LLVM.LLVMInt1Type();
    LLVMTypeRef qualifiedNameType = typeHelper.findRawStringType();

    LLVMTypeRef[] namedSubTypes = new LLVMTypeRef[] {typeSearchListType, sortIdType, nullableType, immutableType, qualifiedNameType};
    LLVMTypeRef namedRTTIType = LLVM.LLVMStructType(C.toNativePointerArray(namedSubTypes, false, true), namedSubTypes.length, false);

    LLVMValueRef castedObjectRTTI = LLVM.LLVMBuildBitCast(builder, rttiPointer, LLVM.LLVMPointerType(namedRTTIType, 0), "");
    LLVMValueRef classQualifiedNameUbyteArrayPointer = LLVM.LLVMBuildStructGEP(builder, castedObjectRTTI, 4, "");
    LLVMValueRef classQualifiedNameUbyteArray = LLVM.LLVMBuildLoad(builder, classQualifiedNameUbyteArrayPointer, "");
    return classQualifiedNameUbyteArray;
  }

  /**
   * Finds the RTTI for the specified type, as an LLVM global constant.
   * @param type - the type to find the RTTI for
   * @return the RTTI for the specified type
   */
  public LLVMValueRef getRTTI(Type type)
  {
    String mangledName = RTTI_MANGLED_NAME_PREFIX + type.getMangledName();
    LLVMValueRef existingGlobal = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingGlobal != null)
    {
      return LLVM.LLVMConstBitCast(existingGlobal, getGenericRTTIType());
    }

    LLVMTypeRef rttiType = getRTTIStructType(type);

    LLVMValueRef global = LLVM.LLVMAddGlobal(module, rttiType, mangledName);
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMLinkOnceODRLinkage);
    LLVM.LLVMSetVisibility(global, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    LLVM.LLVMSetGlobalConstant(global, true);

    LLVMValueRef rtti = getRTTIStruct(type);
    LLVM.LLVMSetInitializer(global, rtti);

    return LLVM.LLVMConstBitCast(global, getGenericRTTIType());
  }

  /**
   * Finds the RTTI struct for the specified Type
   * @param type - the Type to find the RTTI for
   * @return the constant RTTI structure for the specified type (as a constant value, not an LLVM global)
   */
  private LLVMValueRef getRTTIStruct(Type type)
  {
    if (type instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) type;
      LLVMValueRef typeSearchList = virtualFunctionHandler.getObjectTypeSearchList(type, virtualFunctionHandler.getBaseChangeObjectVFT(type));
      typeSearchList = LLVM.LLVMConstBitCast(typeSearchList, LLVM.LLVMPointerType(getTypeSearchListType(), 0));
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), ARRAY_SORT_ID, false);
      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isNullable() ? 1 : 0, false);
      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isContextuallyImmutable() ? 1 : 0, false);
      LLVMValueRef baseType = getRTTI(arrayType.getBaseType());
      LLVMValueRef[] values = new LLVMValueRef[] {typeSearchList, sortId, nullable, immutable, baseType};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) type;
      LLVMValueRef typeSearchList = virtualFunctionHandler.getObjectTypeSearchList(type, virtualFunctionHandler.getBaseChangeObjectVFT(type));
      typeSearchList = LLVM.LLVMConstBitCast(typeSearchList, LLVM.LLVMPointerType(getTypeSearchListType(), 0));
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), FUNCTION_SORT_ID, false);
      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isNullable() ? 1 : 0, false);
      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isImmutable() ? 1 : 0, false);
      LLVMValueRef returnType = getRTTI(functionType.getReturnType());
      Type[] parameterTypes = functionType.getParameterTypes();
      LLVMValueRef numParameters = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), parameterTypes.length, false);
      LLVMValueRef[] parameterRTTIs = new LLVMValueRef[parameterTypes.length];
      for (int i = 0; i < parameterTypes.length; ++i)
      {
        parameterRTTIs[i] = getRTTI(parameterTypes[i]);
      }
      LLVMValueRef parameterArray = LLVM.LLVMConstArray(getGenericRTTIType(), C.toNativePointerArray(parameterRTTIs, false, true), parameterRTTIs.length);
      LLVMValueRef[] values = new LLVMValueRef[] {typeSearchList, sortId, nullable, immutable, returnType, numParameters, parameterArray};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;

      TypeParameter typeParameter = namedType.getResolvedTypeParameter();
      if (typeParameter != null)
      {
        LLVMValueRef typeSearchList = virtualFunctionHandler.getObjectTypeSearchList(type, LLVM.LLVMConstNull(LLVM.LLVMPointerType(virtualFunctionHandler.getGenericVFTType(), 0)));
        typeSearchList = LLVM.LLVMConstBitCast(typeSearchList, LLVM.LLVMPointerType(getTypeSearchListType(), 0));
        LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), TYPE_PARAMETER_SORT_ID, false);
        LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isNullable() ? 1 : 0, false);
        LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isContextuallyImmutable() ? 1 : 0, false);
        TypeDefinition containingTypeDefinition = typeParameter.getContainingTypeDefinition();
        int index = -1;
        TypeParameter[] typeParameters = containingTypeDefinition.getTypeParameters();
        for (int i = 0; i < typeParameters.length; ++i)
        {
          if (typeParameter == typeParameters[i])
          {
            index = i;
            break;
          }
        }
        if (index == -1)
        {
          throw new IllegalStateException("TypeParameter " + typeParameter + " is not part of its containing type definition");
        }
        LLVMValueRef indexValue = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), index, false);
        LLVMValueRef[] values = new LLVMValueRef[] {typeSearchList, sortId, nullable, immutable, indexValue};
        return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
      }

      TypeDefinition typeDefinition = namedType.getResolvedTypeDefinition();
      LLVMValueRef typeSearchList;
      LLVMValueRef sortId;
      if (typeDefinition instanceof ClassDefinition)
      {
        sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), CLASS_SORT_ID, false);
        typeSearchList = virtualFunctionHandler.getTypeSearchList(typeDefinition);
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), COMPOUND_SORT_ID, false);
        typeSearchList = virtualFunctionHandler.getObjectTypeSearchList(type, virtualFunctionHandler.getBaseChangeObjectVFT(type));
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), INTERFACE_SORT_ID, false);
        typeSearchList = virtualFunctionHandler.getTypeSearchList(typeDefinition);
      }
      else
      {
        throw new IllegalArgumentException("Cannot find run-time type information for the unknown named type: " + type);
      }
      typeSearchList = LLVM.LLVMConstBitCast(typeSearchList, LLVM.LLVMPointerType(getTypeSearchListType(), 0));

      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isNullable() ? 1 : 0, false);
      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isContextuallyImmutable() ? 1 : 0, false);
      LLVMValueRef qualifiedName = codeGenerator.addStringConstant(typeDefinition.getQualifiedName().toString());
      qualifiedName = LLVM.LLVMConstBitCast(qualifiedName, typeHelper.findRawStringType());

      Type[] typeArguments = namedType.getTypeArguments();
      LLVMValueRef[] typeArgumentValues = new LLVMValueRef[typeArguments == null ? 0 : typeArguments.length];
      LLVMValueRef numTypeArguments = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), typeArgumentValues.length, false);
      for (int i = 0; i < typeArgumentValues.length; ++i)
      {
        typeArgumentValues[i] = getRTTI(typeArguments[i]);
      }
      LLVMValueRef typeArgumentsArray = LLVM.LLVMConstArray(getGenericRTTIType(), C.toNativePointerArray(typeArgumentValues, false, true), typeArgumentValues.length);

      LLVMValueRef[] typeArgumentMapperValues = new LLVMValueRef[] {numTypeArguments, typeArgumentsArray};
      LLVMValueRef typeArgumentMapper = LLVM.LLVMConstStruct(C.toNativePointerArray(typeArgumentMapperValues, false, true), typeArgumentMapperValues.length, false);

      LLVMValueRef[] values = new LLVMValueRef[] {typeSearchList, sortId, nullable, immutable, qualifiedName, typeArgumentMapper};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof NullType)
    {
      LLVMValueRef typeSearchList = virtualFunctionHandler.getObjectTypeSearchList(type, LLVM.LLVMConstNull(LLVM.LLVMPointerType(virtualFunctionHandler.getGenericVFTType(), 0)));
      typeSearchList = LLVM.LLVMConstBitCast(typeSearchList, LLVM.LLVMPointerType(getTypeSearchListType(), 0));
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), NULL_SORT_ID, false);
      LLVMValueRef[] values = new LLVMValueRef[] {typeSearchList, sortId};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof ObjectType)
    {
      ObjectType objectType = (ObjectType) type;
      LLVMValueRef typeSearchList = virtualFunctionHandler.getObjectTypeSearchList(type, virtualFunctionHandler.getObjectVFTGlobal());
      typeSearchList = LLVM.LLVMConstBitCast(typeSearchList, LLVM.LLVMPointerType(getTypeSearchListType(), 0));
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), OBJECT_SORT_ID, false);
      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), objectType.isNullable() ? 1 : 0, false);
      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), objectType.isContextuallyImmutable() ? 1 : 0, false);
      LLVMValueRef[] values = new LLVMValueRef[] {typeSearchList, sortId, nullable, immutable};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof PrimitiveType)
    {
      PrimitiveType primitiveType = (PrimitiveType) type;
      LLVMValueRef typeSearchList = virtualFunctionHandler.getObjectTypeSearchList(type, virtualFunctionHandler.getBaseChangeObjectVFT(type));
      typeSearchList = LLVM.LLVMConstBitCast(typeSearchList, LLVM.LLVMPointerType(getTypeSearchListType(), 0));
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), PRIMITIVE_SORT_ID, false);
      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), primitiveType.isNullable() ? 1 : 0, false);
      LLVMValueRef primitiveId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), primitiveType.getPrimitiveTypeType().getRunTimeId(), false);
      LLVMValueRef[] values = new LLVMValueRef[] {typeSearchList, sortId, nullable, primitiveId};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      LLVMValueRef typeSearchList = virtualFunctionHandler.getObjectTypeSearchList(type, virtualFunctionHandler.getBaseChangeObjectVFT(type));
      typeSearchList = LLVM.LLVMConstBitCast(typeSearchList, LLVM.LLVMPointerType(getTypeSearchListType(), 0));
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), TUPLE_SORT_ID, false);
      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), tupleType.isNullable() ? 1 : 0, false);
      Type[] subTypes = tupleType.getSubTypes();
      LLVMValueRef numSubTypes = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), subTypes.length, false);
      LLVMValueRef[] subTypeRTTIs = new LLVMValueRef[subTypes.length];
      for (int i = 0; i < subTypes.length; ++i)
      {
        subTypeRTTIs[i] = getRTTI(subTypes[i]);
      }
      LLVMValueRef subTypeArray = LLVM.LLVMConstArray(getGenericRTTIType(), C.toNativePointerArray(subTypeRTTIs, false, true), subTypeRTTIs.length);
      LLVMValueRef[] values = new LLVMValueRef[] {typeSearchList, sortId, nullable, numSubTypes, subTypeArray};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof VoidType)
    {
      LLVMValueRef typeSearchList = virtualFunctionHandler.getObjectTypeSearchList(type, LLVM.LLVMConstNull(LLVM.LLVMPointerType(virtualFunctionHandler.getGenericVFTType(), 0)));
      typeSearchList = LLVM.LLVMConstBitCast(typeSearchList, LLVM.LLVMPointerType(getTypeSearchListType(), 0));
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), VOID_SORT_ID, false);
      LLVMValueRef[] values = new LLVMValueRef[] {typeSearchList, sortId};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof WildcardType)
    {
      WildcardType wildcardType = (WildcardType) type;
      LLVMValueRef typeSearchList = virtualFunctionHandler.getObjectTypeSearchList(type, LLVM.LLVMConstNull(LLVM.LLVMPointerType(virtualFunctionHandler.getGenericVFTType(), 0)));
      typeSearchList = LLVM.LLVMConstBitCast(typeSearchList, LLVM.LLVMPointerType(getTypeSearchListType(), 0));
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), WILDCARD_SORT_ID, false);
      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), wildcardType.canBeNullable() ? 1 : 0, false);
      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), wildcardType.canBeExplicitlyImmutable() ? 1 : 0, false);
      Type[] superTypes = wildcardType.getSuperTypes();
      Type[] subTypes = wildcardType.getSubTypes();
      LLVMValueRef numSuperTypes = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), superTypes.length, false);
      LLVMValueRef numSubTypes = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), subTypes.length, false);
      LLVMValueRef[] referenedRTTIListValues = new LLVMValueRef[superTypes.length + subTypes.length];
      for (int i = 0; i < superTypes.length; ++i)
      {
        referenedRTTIListValues[i] = getRTTI(superTypes[i]);
      }
      for (int i = 0; i < subTypes.length; ++i)
      {
        referenedRTTIListValues[superTypes.length + i] = getRTTI(subTypes[i]);
      }
      LLVMValueRef referencedRTTIList = LLVM.LLVMConstArray(getGenericRTTIType(), C.toNativePointerArray(referenedRTTIListValues, false, true), referenedRTTIListValues.length);
      LLVMValueRef[] values = new LLVMValueRef[] {typeSearchList, sortId, nullable, immutable, numSuperTypes, numSubTypes, referencedRTTIList};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    throw new IllegalArgumentException("Cannot create a run-time type information struct for the unknown type: " + type);
  }

  /**
   * Finds the type of an RTTI struct for the specified type.
   * @param type - the type to find the RTTI struct type for
   * @return the RTTI struct type for the specified type
   */
  public LLVMTypeRef getRTTIStructType(Type type)
  {
    LLVMTypeRef typeSearchListType = LLVM.LLVMPointerType(getTypeSearchListType(), 0);
    LLVMTypeRef sortIdType = LLVM.LLVMInt8Type();
    if (type instanceof ArrayType)
    {
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef immutableType = LLVM.LLVMInt1Type();
      LLVMTypeRef baseTypeType = getGenericRTTIType();
      LLVMTypeRef[] types = new LLVMTypeRef[] {typeSearchListType, sortIdType, nullableType, immutableType, baseTypeType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof FunctionType)
    {
      Type[] parameterTypes = ((FunctionType) type).getParameterTypes();
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef immutableType = LLVM.LLVMInt1Type();
      LLVMTypeRef returnTypeType = getGenericRTTIType();
      LLVMTypeRef numParametersType = LLVM.LLVMInt32Type();
      LLVMTypeRef parameterArrayType = LLVM.LLVMArrayType(getGenericRTTIType(), parameterTypes.length);
      LLVMTypeRef[] types = new LLVMTypeRef[] {typeSearchListType, sortIdType, nullableType, immutableType, returnTypeType, numParametersType, parameterArrayType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef immutableType = LLVM.LLVMInt1Type();
      if (namedType.getResolvedTypeParameter() != null)
      {
        LLVMTypeRef indexType = LLVM.LLVMInt32Type();
        LLVMTypeRef[] types = new LLVMTypeRef[] {typeSearchListType, sortIdType, nullableType, immutableType, indexType};
        return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
      }
      LLVMTypeRef qualifiedNameType = typeHelper.findRawStringType();

      LLVMTypeRef numTypeArgumentsType = LLVM.LLVMInt32Type();
      LLVMTypeRef typeArgumentType = getGenericRTTIType();
      int numTypeArguments = namedType.getTypeArguments() == null ? 0 : namedType.getTypeArguments().length;
      LLVMTypeRef typeArgumentArrayType = LLVM.LLVMArrayType(typeArgumentType, numTypeArguments);

      LLVMTypeRef[] typeArgumentMapperTypes = new LLVMTypeRef[] {numTypeArgumentsType, typeArgumentArrayType};
      LLVMTypeRef typeArgumentMapperType = LLVM.LLVMStructType(C.toNativePointerArray(typeArgumentMapperTypes, false, true), typeArgumentMapperTypes.length, false);

      LLVMTypeRef[] types = new LLVMTypeRef[] {typeSearchListType, sortIdType, nullableType, immutableType, qualifiedNameType, typeArgumentMapperType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof NullType)
    {
      LLVMTypeRef[] types = new LLVMTypeRef[] {typeSearchListType, sortIdType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof ObjectType)
    {
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef immutableType = LLVM.LLVMInt1Type();
      LLVMTypeRef[] types = new LLVMTypeRef[] {typeSearchListType, sortIdType, nullableType, immutableType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof PrimitiveType)
    {
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef primitiveIdType = LLVM.LLVMInt8Type();
      LLVMTypeRef[] types = new LLVMTypeRef[] {typeSearchListType, sortIdType, nullableType, primitiveIdType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef numParametersType = LLVM.LLVMInt32Type();
      LLVMTypeRef parameterArrayType = LLVM.LLVMArrayType(getGenericRTTIType(), tupleType.getSubTypes().length);
      LLVMTypeRef[] types = new LLVMTypeRef[] {typeSearchListType, sortIdType, nullableType, numParametersType, parameterArrayType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof VoidType)
    {
      LLVMTypeRef[] types = new LLVMTypeRef[] {typeSearchListType, sortIdType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof WildcardType)
    {
      WildcardType wildcardType = (WildcardType) type;
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef immutableType = LLVM.LLVMInt1Type();
      LLVMTypeRef numSuperTypesType = LLVM.LLVMInt32Type();
      LLVMTypeRef numSubTypesType = LLVM.LLVMInt32Type();
      LLVMTypeRef referencedRTTIListType = LLVM.LLVMArrayType(getGenericRTTIType(), wildcardType.getSuperTypes().length + wildcardType.getSubTypes().length);
      LLVMTypeRef[] types = new LLVMTypeRef[] {typeSearchListType, sortIdType, nullableType, immutableType, numSuperTypesType, numSubTypesType, referencedRTTIListType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    throw new IllegalArgumentException("Cannot find a run-time type information struct type for the unknown type: " + type);
  }

  /**
   * Finds the type of a type search list, a named struct type representing: {i32, [0 x {%RTTI*, %VFT*}]}
   * @return the LLVM type of a type search list
   */
  private LLVMTypeRef getTypeSearchListType()
  {
    if (typeSearchListType != null)
    {
      return typeSearchListType;
    }
    // store the named struct in typeSearchListType first, so that when we get the raw string type we don't infinitely recurse
    typeSearchListType = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), "TypeSearchList");
    LLVMTypeRef rttiType = getGenericRTTIType();
    LLVMTypeRef vftType = LLVM.LLVMPointerType(virtualFunctionHandler.getGenericVFTType(), 0);
    LLVMTypeRef[] elementSubTypes = new LLVMTypeRef[] {rttiType, vftType};
    LLVMTypeRef elementType = LLVM.LLVMStructType(C.toNativePointerArray(elementSubTypes, false, true), elementSubTypes.length, false);
    LLVMTypeRef arrayType = LLVM.LLVMArrayType(elementType, 0);
    LLVMTypeRef[] searchListSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), arrayType};
    LLVM.LLVMStructSetBody(typeSearchListType, C.toNativePointerArray(searchListSubTypes, false, true), searchListSubTypes.length, false);
    return typeSearchListType;
  }

  /**
   * @return the type of a pointer to a generic RTTI struct
   */
  public LLVMTypeRef getGenericRTTIType()
  {
    LLVMTypeRef typeSearchListType = LLVM.LLVMPointerType(getTypeSearchListType(), 0);
    LLVMTypeRef sortIdType = LLVM.LLVMInt8Type();
    LLVMTypeRef[] types = new LLVMTypeRef[] {typeSearchListType, sortIdType};
    LLVMTypeRef structType = LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    return LLVM.LLVMPointerType(structType, 0);
  }

  /**
   * @return the type of a pointer to a generic NamedType's RTTI struct
   */
  public LLVMTypeRef getGenericNamedRTTIType()
  {
    LLVMTypeRef typeSearchListType = LLVM.LLVMPointerType(getTypeSearchListType(), 0);
    LLVMTypeRef sortIdType = LLVM.LLVMInt8Type();
    LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
    LLVMTypeRef immutableType = LLVM.LLVMInt1Type();
    LLVMTypeRef qualifiedNameType = typeHelper.findRawStringType();

    LLVMTypeRef numTypeArgumentsType = LLVM.LLVMInt32Type();
    LLVMTypeRef typeArgumentType = getGenericRTTIType();
    LLVMTypeRef typeArgumentArrayType = LLVM.LLVMArrayType(typeArgumentType, 0);

    LLVMTypeRef[] typeArgumentMapperTypes = new LLVMTypeRef[] {numTypeArgumentsType, typeArgumentArrayType};
    LLVMTypeRef typeArgumentMapperType = LLVM.LLVMStructType(C.toNativePointerArray(typeArgumentMapperTypes, false, true), typeArgumentMapperTypes.length, false);

    LLVMTypeRef[] types = new LLVMTypeRef[] {typeSearchListType, sortIdType, nullableType, immutableType, qualifiedNameType, typeArgumentMapperType};
    LLVMTypeRef structType = LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    return LLVM.LLVMPointerType(structType, 0);
  }

  /**
   * @return the type of a generic type argument mapper struct (with an unspecified number of type arguments)
   */
  public LLVMTypeRef getGenericTypeArgumentMapperStructureType()
  {
    return getTypeArgumentMapperType(0);
  }

  /**
   * Finds the type of a type argument mapper with the specified number of type parameters.
   * @param numParameters - the number of type parameters that the mapper should be able to store
   * @return the native structure type of a type argument mapper with the specified number of parameters
   */
  public LLVMTypeRef getTypeArgumentMapperType(int numParameters)
  {
    LLVMTypeRef rttiType = getGenericRTTIType();
    LLVMTypeRef arrayType = LLVM.LLVMArrayType(rttiType, numParameters);
    LLVMTypeRef[] subTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), arrayType};
    LLVMTypeRef mapperType = LLVM.LLVMStructType(C.toNativePointerArray(subTypes, false, true), subTypes.length, false);
    return mapperType;
  }

  /**
   * @return the type of a pointer to a generic type argument mapper struct (with an unspecified number of type arguments)
   */
  public LLVMTypeRef getGenericTypeArgumentMapperType()
  {
    return LLVM.LLVMPointerType(getGenericTypeArgumentMapperStructureType(), 0);
  }

  /**
   * Builds a check that determines whether the specified RTTI block represents a nullable type.
   * @param builder - the builder to build code with
   * @param rtti - the RTTI to check the nullability of
   * @return an i1 which is true if the specified RTTI is nullable, and false otherwise
   */
  public LLVMValueRef buildTypeIsNullableCheck(LLVMBuilderRef builder, LLVMValueRef rtti)
  {
    LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "typeIsNullableContinuation");
    LLVMBasicBlockRef checkBlock = LLVM.LLVMAddBasicBlock(builder, "typeIsNullableCheck");

    LLVMValueRef sortIdPtr = LLVM.LLVMBuildStructGEP(builder, rtti, 1, "");
    LLVMValueRef sortId = LLVM.LLVMBuildLoad(builder, sortIdPtr, "");

    LLVMValueRef isVoidType = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, sortId, LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), VOID_SORT_ID, false), "");
    LLVMValueRef isNullType = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, sortId, LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), NULL_SORT_ID, false), "");
    LLVMValueRef condition = LLVM.LLVMBuildOr(builder, isVoidType, isNullType, "");
    LLVM.LLVMBuildCondBr(builder, condition, continuationBlock, checkBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, checkBlock);
    // sub types: type search list, sort ID, nullability
    LLVMTypeRef[] subTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(getTypeSearchListType(), 0), LLVM.LLVMInt8Type(), LLVM.LLVMInt1Type()};
    LLVMTypeRef rttiWithNullabilityType = LLVM.LLVMStructType(C.toNativePointerArray(subTypes, false, true), subTypes.length, false);
    LLVMValueRef rttiWithNullability = LLVM.LLVMBuildBitCast(builder, rtti, LLVM.LLVMPointerType(rttiWithNullabilityType, 0), "");

    LLVMValueRef isNullablePtr = LLVM.LLVMBuildStructGEP(builder, rttiWithNullability, 2, "");
    LLVMValueRef isNullable = LLVM.LLVMBuildLoad(builder, isNullablePtr, "");
    LLVM.LLVMBuildBr(builder, continuationBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
    LLVMValueRef phi = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt1Type(), "");
    LLVMValueRef[] incomingValues = new LLVMValueRef[] {isNullType, isNullable};
    LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, checkBlock};
    LLVM.LLVMAddIncoming(phi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
    return phi;
  }

  /**
   * Builds a run-time RTTI specialisation, that will specialise any type parameters in the specified unspecialisedRTTI using the specified TypeParameterAccessor.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param unspecialisedRTTI - a pointer to the RTTI block to specialise
   * @param typeArgumentMapper - the type argument mapper to specialise the RTTI with
   * @return the new specialised RTTI, or a pointer to the same RTTI block if nothing needed changing
   */
  public LLVMValueRef buildRTTISpecialisation(LLVMBuilderRef builder, LLVMValueRef unspecialisedRTTI, LLVMValueRef typeArgumentMapper)
  {
    final String SPECIALISE_TYPE_INFO_FUNCTION_NAME = "plinth_get_specialised_type_info";
    LLVMValueRef function = LLVM.LLVMGetNamedFunction(module, SPECIALISE_TYPE_INFO_FUNCTION_NAME);
    if (function == null)
    {
      LLVMTypeRef rttiType = getGenericRTTIType();
      LLVMTypeRef typeArgumentMapperType = getGenericTypeArgumentMapperType();
      LLVMTypeRef[] parameterTypes = new LLVMTypeRef[] {rttiType, typeArgumentMapperType};
      LLVMTypeRef functionType = LLVM.LLVMFunctionType(rttiType, C.toNativePointerArray(parameterTypes, false, true), parameterTypes.length, false);

      function = LLVM.LLVMAddFunction(module, SPECIALISE_TYPE_INFO_FUNCTION_NAME, functionType);
    }

    LLVMValueRef[] arguments = new LLVMValueRef[] {unspecialisedRTTI, typeArgumentMapper};
    return LLVM.LLVMBuildCall(builder, function, C.toNativePointerArray(arguments, false, true), arguments.length, "");
  }

  /**
   * Builds an instanceof check, to check whether the specified value is an instance of the specified checkType.
   * This method assumes that the value and the expression type are not nullable. However, null instanceof &lt;anything&gt; should always return false.
   * @param builder - the LLVMBuilderRef to build the check with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the not-null value to check, in a temporary type representation
   * @param expressionType - the type of the value, which must not be nullable
   * @param checkType - the type to check the RTTI against (note: nullability and data-immutability on the top level of this type are ignored)
   * @param expressionAccessor - the TypeParameterAccessor to use to access the RTTI blocks of any TypeParameters that might be used in expressionType
   * @param checkAccessor - the TypeParameterAccessor to use to access the RTTI blocks of any TypeParameters that might be used in checkType
   * @return an LLVMValueRef representing an i1 (boolean), which will be true iff value is an instance of checkType
   */
  public LLVMValueRef buildInstanceOfCheck(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef value, Type expressionType, Type checkType, TypeParameterAccessor expressionAccessor, TypeParameterAccessor checkAccessor)
  {
    checkType = Type.findTypeWithoutModifiers(checkType);
    if (checkType instanceof ObjectType)
    {
      // catch-all: any not-null type instanceof object is always true
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
    }

    if (checkType instanceof WildcardType)
    {
      // to check whether something is an instance of a wildcard type, we only need to check that it is an instance of all of the super-types of that wildcard
      Type[] checkSuperTypes = ((WildcardType) checkType).getSuperTypes();
      if (checkSuperTypes.length == 0)
      {
        return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
      }

      // add all of the blocks in reverse order
      LLVMBasicBlockRef endBlock = LLVM.LLVMAddBasicBlock(builder, "instanceOfWildcardEnd");
      LLVMBasicBlockRef[] checkBlocks = new LLVMBasicBlockRef[checkSuperTypes.length];
      for (int i = checkSuperTypes.length - 1; i > 0; --i)
      {
        checkBlocks[i] = LLVM.LLVMAddBasicBlock(builder, "instanceOfWildcardCheck");
      }
      LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);
      checkBlocks[0] = startBlock;

      // add the phi node
      LLVM.LLVMPositionBuilderAtEnd(builder, endBlock);
      LLVMValueRef wildcardPhi = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt1Type(), "");
      LLVM.LLVMPositionBuilderAtEnd(builder, startBlock);

      // build an instanceof check in each of the check blocks, and if any of them fail, branch straight to the end block
      for (int i = 0; i < checkSuperTypes.length; ++i)
      {
        LLVM.LLVMPositionBuilderAtEnd(builder, checkBlocks[i]);
        LLVMValueRef isInstanceOfSuperType = buildInstanceOfCheck(builder, landingPadContainer, value, expressionType, checkSuperTypes[i], expressionAccessor, checkAccessor);
        LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
        if (i == checkSuperTypes.length - 1)
        {
          LLVM.LLVMBuildBr(builder, endBlock);
          LLVMValueRef[] incomingValues = new LLVMValueRef[] {isInstanceOfSuperType};
          LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {currentBlock};
          LLVM.LLVMAddIncoming(wildcardPhi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
        }
        else
        {
          LLVM.LLVMBuildCondBr(builder, isInstanceOfSuperType, checkBlocks[i + 1], endBlock);
          LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false)};
          LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {currentBlock};
          LLVM.LLVMAddIncoming(wildcardPhi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
        }
      }

      LLVM.LLVMPositionBuilderAtEnd(builder, endBlock);
      return wildcardPhi;
    }

    if (checkType instanceof NamedType && ((NamedType) checkType).getResolvedTypeParameter() != null)
    {
      // we are checking whether something is a type parameter
      if (expressionType instanceof NamedType && (((NamedType) expressionType).getResolvedTypeDefinition() instanceof ClassDefinition ||
                                                  ((NamedType) expressionType).getResolvedTypeDefinition() instanceof InterfaceDefinition))
      {
        // we have a class/interface type, and we need to check whether its run-time value is of the type parameter's type
        // so we need to do a search through the object's type search list for the type parameter's RTTI

        // search through the object's type search list
        LLVMValueRef objectValue = value;
        if (expressionType instanceof NamedType && ((NamedType) expressionType).getResolvedTypeDefinition() instanceof InterfaceDefinition)
        {
          TypeParameterAccessor nullAccessor = new TypeParameterAccessor(builder, this);
          objectValue = typeHelper.convertTemporary(builder, landingPadContainer, value, expressionType, new ObjectType(false, false, null), false, expressionAccessor, nullAccessor);
        }
        LLVMValueRef objectRTTIPtr = getRTTIPointer(builder, objectValue);
        LLVMValueRef objectRTTI = LLVM.LLVMBuildLoad(builder, objectRTTIPtr, "");
        LLVMValueRef superTypeResult = lookupInstanceSuperType(builder, objectRTTI, checkType, checkAccessor);
        LLVMValueRef resultRTTI = LLVM.LLVMBuildExtractValue(builder, superTypeResult, 0, "");
        return LLVM.LLVMBuildIsNotNull(builder, resultRTTI, "");
      }
      if (expressionType instanceof ObjectType ||
          (expressionType instanceof NamedType && ((NamedType) expressionType).getResolvedTypeParameter() != null) ||
          expressionType instanceof WildcardType)
      {
        // we are checking whether an arbitrary object is an instance of a type parameter
        // the problem here is that we do not know whether it represents a class/interface or some other type
        // i.e. whether we should just check the RTTI, or check through the whole of the type search list
        // so we must compare the check RTTI to both the object's RTTI and the object's type search list

        // check against the object's RTTI
        // (we ignore the type modifiers by passing ignoreTypeModifiers=true, and loosely match wildcards by passing looselyMatchWildcards=true)
        LLVMValueRef objectRTTIPtr = getRTTIPointer(builder, value);
        LLVMValueRef objectRTTI = LLVM.LLVMBuildLoad(builder, objectRTTIPtr, "");
        LLVMValueRef checkRTTI = getRTTI(checkType);
        LLVMValueRef nullTypeMapper = LLVM.LLVMConstNull(getGenericTypeArgumentMapperType());
        LLVMValueRef[] checkArguments = new LLVMValueRef[] {objectRTTI, checkRTTI, nullTypeMapper, checkAccessor.getTypeArgumentMapper(),
                                                            LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false),
                                                            LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false)};
        LLVMValueRef comparisonResult = LLVM.LLVMBuildCall(builder, getCheckTypeMatchesFunction(), C.toNativePointerArray(checkArguments, false, true), checkArguments.length, "");

        // if comparisonResult turns out to be true, we can skip the search through the type search list
        LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "typeParameterInstanceOfContinue");
        LLVMBasicBlockRef checkBlock = LLVM.LLVMAddBasicBlock(builder, "typeParameterInstanceOfSearchTypeList");
        LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildCondBr(builder, comparisonResult, continuationBlock, checkBlock);

        // build the search through the type search list
        LLVM.LLVMPositionBuilderAtEnd(builder, checkBlock);
        LLVMValueRef superTypeResult = lookupInstanceSuperType(builder, objectRTTI, checkType, checkAccessor);
        LLVMValueRef resultRTTI = LLVM.LLVMBuildExtractValue(builder, superTypeResult, 0, "");
        LLVMValueRef rttiResult = LLVM.LLVMBuildIsNotNull(builder, resultRTTI, "");
        LLVMBasicBlockRef endTypeSearchBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildBr(builder, continuationBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
        LLVMValueRef phi = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt1Type(), "");
        LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), rttiResult};
        LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endTypeSearchBlock};
        LLVM.LLVMAddIncoming(phi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
        return phi;
      }

      // otherwise, the expression cannot be a class type, so we can just build a normal type check for it
      if (!(expressionType instanceof ArrayType) && !(expressionType instanceof FunctionType))
      {
        // we don't need to build an RTTI creation, as the type match check can use the expressionAccessor and checkAccessor itself, so just use a global
        LLVMValueRef expressionRTTI = getRTTI(expressionType);
        LLVMValueRef checkRTTI = getRTTI(checkType);

        LLVMValueRef[] checkArguments = new LLVMValueRef[] {expressionRTTI, checkRTTI, expressionAccessor.getTypeArgumentMapper(), checkAccessor.getTypeArgumentMapper(),
                                                            LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false),
                                                            LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false)};
        return LLVM.LLVMBuildCall(builder, getCheckTypeMatchesFunction(), C.toNativePointerArray(checkArguments, false, true), checkArguments.length, "");
      }

      LLVMValueRef expressionRTTI;
      if (expressionType instanceof ArrayType)
      {
        LLVMValueRef objectRTTIPtr = getRTTIPointer(builder, value);
        expressionRTTI = LLVM.LLVMBuildLoad(builder, objectRTTIPtr, "");
      }
      else // if (expressionType instanceof FunctionType)
      {
        // extract the RTTI from the function
        expressionRTTI = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
      }
      return buildTypeInfoCheck(builder, expressionRTTI, checkType, checkAccessor, true, true);
    }


    if (expressionType instanceof ArrayType)
    {
      if (checkType instanceof ArrayType)
      {
        return buildTypeMatchingCheck(builder, ((ArrayType) expressionType).getBaseType(), ((ArrayType) checkType).getBaseType(), expressionAccessor, checkAccessor, false, false);
      }
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
    }
    if (expressionType instanceof FunctionType)
    {
      if (checkType instanceof FunctionType)
      {
        FunctionType expressionFunctionType = (FunctionType) expressionType;
        FunctionType checkFunctionType = (FunctionType) checkType;
        Type[] expressionParameterTypes = expressionFunctionType.getParameterTypes();
        Type[] checkParameterTypes = checkFunctionType.getParameterTypes();
        if (expressionParameterTypes.length == checkParameterTypes.length)
        {
          LLVMValueRef equivalentSubTypes = buildTypeMatchingCheck(builder, expressionFunctionType.getReturnType(), checkFunctionType.getReturnType(), expressionAccessor, checkAccessor, false, false);

          for (int i = 0; i < expressionParameterTypes.length; ++i)
          {
            LLVMValueRef paramsEquivalent = buildTypeMatchingCheck(builder, expressionParameterTypes[i], checkParameterTypes[i], expressionAccessor, checkAccessor, false, false);
            equivalentSubTypes = LLVM.LLVMBuildAnd(builder, equivalentSubTypes, paramsEquivalent, "");
          }

          LLVMValueRef result = equivalentSubTypes;
          if (!expressionFunctionType.isImmutable() && checkFunctionType.isImmutable())
          {
            // the result depends on the immutability of the actual function in the value, so check its RTTI
            LLVMValueRef rttiPointer = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
            rttiPointer = LLVM.LLVMBuildBitCast(builder, rttiPointer, LLVM.LLVMPointerType(getRTTIStructType(expressionFunctionType), 0), "");
            LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, rttiPointer, 3, "");
            LLVMValueRef immutabilityMatches = LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
            result = LLVM.LLVMBuildAnd(builder, result, immutabilityMatches, "");
          }
          return result;
        }
      }
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
    }
    if (expressionType instanceof NamedType && ((NamedType) expressionType).getResolvedTypeDefinition() instanceof CompoundDefinition)
    {
      if (checkType instanceof NamedType && ((NamedType) checkType).getResolvedTypeDefinition() instanceof CompoundDefinition)
      {
        NamedType expressionNamedType = (NamedType) expressionType;
        NamedType checkNamedType = (NamedType) checkType;
        Type[] expressionArguments = expressionNamedType.getTypeArguments();
        TypeParameter[] expressionTypeParameters = expressionNamedType.getResolvedTypeDefinition().getTypeParameters();
        Type[] checkArguments = checkNamedType.getTypeArguments();
        if ((expressionArguments == null) != (checkArguments == null) ||
            (expressionArguments != null && (expressionArguments.length != checkArguments.length)))
        {
          throw new IllegalArgumentException("Two references to the same compound type do not have the same number of type arguments");
        }

        LLVMValueRef result = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
        for (int i = 0; expressionArguments != null && i < expressionArguments.length; ++i)
        {
          LLVMValueRef actualTypeRTTIPtr = typeHelper.getTypeParameterPointer(builder, value, expressionTypeParameters[i]);
          LLVMValueRef actualTypeRTTI = LLVM.LLVMBuildLoad(builder, actualTypeRTTIPtr, "");
          // don't ignore type modifiers, but do loosely match wildcards
          LLVMValueRef argsEquivalent = buildTypeInfoCheck(builder, actualTypeRTTI, checkArguments[i], checkAccessor, false, true);
          result = LLVM.LLVMBuildAnd(builder, result, argsEquivalent, "");
        }
        return result;
      }
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
    }
    if (expressionType instanceof PrimitiveType)
    {
      if (checkType instanceof PrimitiveType)
      {
        boolean result = ((PrimitiveType) expressionType).getPrimitiveTypeType() == ((PrimitiveType) checkType).getPrimitiveTypeType();
        return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), result ? 1 : 0, false);
      }
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
    }
    if (expressionType instanceof TupleType)
    {
      if (checkType instanceof TupleType)
      {
        TupleType expressionTupleType = (TupleType) expressionType;
        TupleType checkTupleType = (TupleType) checkType;
        Type[] expressionSubTypes = expressionTupleType.getSubTypes();
        Type[] checkSubTypes = checkTupleType.getSubTypes();
        if (expressionSubTypes.length == checkSubTypes.length)
        {
          LLVMValueRef result = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
          for (int i = 0; i < expressionSubTypes.length; ++i)
          {
            LLVMValueRef subEquivalent = buildTypeMatchingCheck(builder, expressionSubTypes[i], checkSubTypes[i], expressionAccessor, checkAccessor, false, false);
            result = LLVM.LLVMBuildAnd(builder, result, subEquivalent, "");
          }
          return result;
        }
      }
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
    }
    if (expressionType instanceof NamedType && ((NamedType) expressionType).getResolvedTypeDefinition() != null)
    {
      // note: expressionType cannot be a compound type, as compound types have already been handled
      if (checkType instanceof NamedType && ((NamedType) checkType).getResolvedTypeDefinition() != null)
      {
        NamedType expressionNamedType = (NamedType) expressionType;
        NamedType checkNamedType = (NamedType) checkType;

        if (checkNamedType.getResolvedTypeDefinition() instanceof CompoundDefinition)
        {
          // class/interface NamedTypes can never be compound types
          // return false
          return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
        }

        TypeDefinition expressionDefinition = expressionNamedType.getResolvedTypeDefinition();
        GenericTypeSpecialiser expressionSpecialiser = new GenericTypeSpecialiser(expressionNamedType);
        for (NamedType t : expressionDefinition.getInheritanceLinearisation())
        {
          if (expressionSpecialiser.getSpecialisedType(t).isRuntimeEquivalent(checkType))
          {
            // checkType is in expressionType's linearisation
            // return true
            return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
          }
        }

        // either checkType is not in expressionType's linearisation, or it is, but it is disguised by type parameters
        // either way, we should do a run-time check through the object's RTTI
        // TODO: we could avoid this if expressionType is sealed and neither it nor checkType contain any type parameters
        LLVMValueRef objectValue = value;
        if (expressionDefinition instanceof InterfaceDefinition)
        {
          TypeParameterAccessor nullAccessor = new TypeParameterAccessor(builder, this);
          objectValue = typeHelper.convertTemporary(builder, landingPadContainer, value, expressionType, new ObjectType(false, false, null), false, expressionAccessor, nullAccessor);
        }
        // search through the object's type search list for checkType
        // the object does not implement checkType iff the RTTI pointer comes back as null
        LLVMValueRef objectRTTIPtr = getRTTIPointer(builder, objectValue);
        LLVMValueRef objectRTTI = LLVM.LLVMBuildLoad(builder, objectRTTIPtr, "");
        LLVMValueRef superTypeValue = lookupInstanceSuperType(builder, objectRTTI, checkNamedType, checkAccessor);
        LLVMValueRef superTypeRTTI = LLVM.LLVMBuildExtractValue(builder, superTypeValue, 0, "");
        return LLVM.LLVMBuildIsNotNull(builder, superTypeRTTI, "");
      }
      // return false
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
    }
    if (expressionType instanceof ObjectType ||
        (expressionType instanceof NamedType && ((NamedType) expressionType).getResolvedTypeParameter() != null) ||
        expressionType instanceof WildcardType)
    {
      if (checkType instanceof NamedType && (((NamedType) checkType).getResolvedTypeDefinition() instanceof ClassDefinition ||
                                             ((NamedType) checkType).getResolvedTypeDefinition() instanceof InterfaceDefinition))
      {
        // checkType is the sort of type which turns up in type search lists
        // so search through the object's type search list for checkType
        // the object implements checkType iff the RTTI pointer comes back as something other than null
        LLVMValueRef objectRTTIPtr = getRTTIPointer(builder, value);
        LLVMValueRef objectRTTI = LLVM.LLVMBuildLoad(builder, objectRTTIPtr, "");
        LLVMValueRef superTypeValue = lookupInstanceSuperType(builder, objectRTTI, checkType, checkAccessor);
        LLVMValueRef superTypeRTTI = LLVM.LLVMBuildExtractValue(builder, superTypeValue, 0, "");
        return LLVM.LLVMBuildIsNotNull(builder, superTypeRTTI, "");
      }
      // we are checking against something which is not a class type, so compare the type info to what we are asking about
      LLVMValueRef objectRTTIPtr = getRTTIPointer(builder, value);
      LLVMValueRef objectRTTI = LLVM.LLVMBuildLoad(builder, objectRTTIPtr, "");
      return buildTypeInfoCheck(builder, objectRTTI, checkType, checkAccessor, true, true);
    }
    throw new IllegalArgumentException("Cannot build instanceof check from " + expressionType + " to " + checkType);
  }

  /**
   * Checks whether two types match, including accounting for type parameters.
   * @param builder - the LLVMBuilderRef to build code with
   * @param queryType - the type to compare to specType
   * @param specType - the specification type that queryType must match
   * @param queryTypeAccessor - the TypeParameterAccessor to look up any type parameters inside queryType with
   * @param specTypeAccessor - the TypeParameterAccessor to look up any type parameters inside specType with
   * @param ignoreTypeModifiers - true to ignore nullability and immutability during the check, false to take them into account
   * @param looselyMatchWildcards - false if this should be an equivalence check, true if a range of different queryTypes should match a wildcard in specType
   * @return an LLVMValueRef representing an i1, which is true iff the types are runtime-equivalent in this context
   */
  private LLVMValueRef buildTypeMatchingCheck(LLVMBuilderRef builder, Type queryType, Type specType, TypeParameterAccessor queryTypeAccessor, TypeParameterAccessor specTypeAccessor, boolean ignoreTypeModifiers, boolean looselyMatchWildcards)
  {
    if (containsTypeParameters(queryType) || containsTypeParameters(specType) || looselyMatchWildcards)
    {
      if (queryTypeAccessor == specTypeAccessor &&
          (queryType.isRuntimeEquivalent(specType) ||
           (ignoreTypeModifiers && Type.findTypeWithoutModifiers(queryType).isRuntimeEquivalent(Type.findTypeWithoutModifiers(specType)))))
      {
        // the types are equivalent, even after the type parameters have been replaced, so return true
        return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false);
      }

      // call plinth_check_type_matches() on the two types
      LLVMValueRef checkTypeMatchesFunction = getCheckTypeMatchesFunction();
      LLVMValueRef queryRTTI = getRTTI(queryType);
      LLVMValueRef specRTTI = getRTTI(specType);
      LLVMValueRef queryTypeArgumentMapper = queryTypeAccessor.getTypeArgumentMapper();
      LLVMValueRef specTypeArgumentMapper = specTypeAccessor.getTypeArgumentMapper();
      LLVMValueRef llvmIgnoreTypeModifiers = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), ignoreTypeModifiers ? 1 : 0, false);
      LLVMValueRef llvmLooselyMatchWildcards = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), looselyMatchWildcards ? 1 : 0, false);

      LLVMValueRef[] arguments = new LLVMValueRef[] {queryRTTI, specRTTI, queryTypeArgumentMapper, specTypeArgumentMapper, llvmIgnoreTypeModifiers, llvmIgnoreTypeModifiers, llvmLooselyMatchWildcards};
      LLVMValueRef result = LLVM.LLVMBuildCall(builder, checkTypeMatchesFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");
      return result;
    }

    boolean result = queryType.isRuntimeEquivalent(specType) ||
                     (ignoreTypeModifiers && Type.findTypeWithoutModifiers(queryType).isRuntimeEquivalent(Type.findTypeWithoutModifiers(specType)));
    return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), result ? 1 : 0, false);
  }

  /**
   * Builds a check as to whether the specified RTTI pointer represents the specified checkType.
   * @param builder - the LLVMBuilderRef to build the check with
   * @param rttiPointer - the pointer to the RTTI to check
   * @param checkType - the Type to check against
   * @param checkAccessor - the TypeParameterAccessor to look up the type parameters of checkType in
   * @param ignoreTypeModifiers - false for the top-level type modifiers (nullability and immutability) should be checked, true if they should only be checked on nested types
   * @param looselyMatchWildcards - true if wildcard types should be matched loosely, false otherwise
   * @return a LLVMValueRef representing an i1 (boolean), which will be true iff the RTTI pointer represents the checkType
   */
  private LLVMValueRef buildTypeInfoCheck(LLVMBuilderRef builder, LLVMValueRef rttiPointer, Type checkType, TypeParameterAccessor checkAccessor, boolean ignoreTypeModifiers, boolean looselyMatchWildcards)
  {
    if ((checkType instanceof NamedType && ((NamedType) checkType).getResolvedTypeParameter() != null) ||
        checkType instanceof WildcardType)
    {
      // call plinth_check_type_matches
      LLVMValueRef checkTypeMatchesFunction = getCheckTypeMatchesFunction();
      LLVMValueRef checkRTTI = getRTTI(checkType);
      LLVMValueRef checkTypeArgumentMapper = checkAccessor.getTypeArgumentMapper();
      LLVMValueRef nullMapper = LLVM.LLVMConstNull(getGenericTypeArgumentMapperType());
      LLVMValueRef ignoreTypeModifiersValue = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), ignoreTypeModifiers ? 1 : 0, false);
      LLVMValueRef looselyMatchWildcardsValue = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), looselyMatchWildcards ? 1 : 0, false);

      LLVMValueRef[] arguments = new LLVMValueRef[] {rttiPointer, checkRTTI, nullMapper, checkTypeArgumentMapper, ignoreTypeModifiersValue, ignoreTypeModifiersValue, looselyMatchWildcardsValue};
      LLVMValueRef result = LLVM.LLVMBuildCall(builder, checkTypeMatchesFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");
      return result;
    }

    byte sortId = getSortId(checkType);
    LLVMValueRef sortIdPointer = LLVM.LLVMBuildStructGEP(builder, rttiPointer, 1, "");
    LLVMValueRef sortIdValue = LLVM.LLVMBuildLoad(builder, sortIdPointer, "");
    LLVMValueRef sortEqual = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, sortIdValue, LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), sortId, false), "");

    LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoContinue");
    LLVMBasicBlockRef checkBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoCheck");
    LLVM.LLVMBuildCondBr(builder, sortEqual, checkBlock, continuationBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, checkBlock);
    LLVMValueRef castedRTTIPointer = LLVM.LLVMBuildBitCast(builder, rttiPointer, LLVM.LLVMPointerType(getRTTIStructType(checkType), 0), "");
    LLVMValueRef resultValue;
    LLVMBasicBlockRef endResultBlock;
    if (checkType instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) checkType;
      if (ignoreTypeModifiers)
      {
        resultValue = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
      }
      else
      {
        LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
        LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
        LLVMValueRef nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isNullable() ? 1 : 0, false), "");
        LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
        LLVMValueRef immutabilityValue = LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
        LLVMValueRef immutabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, immutabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isContextuallyImmutable() ? 1 : 0, false), "");
        resultValue = LLVM.LLVMBuildAnd(builder, nullabilityMatches, immutabilityMatches, "");
      }
      LLVMValueRef baseTypePointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 4, "");
      LLVMValueRef baseTypeValue = LLVM.LLVMBuildLoad(builder, baseTypePointer, "");
      LLVMValueRef baseTypeMatches = buildTypeInfoCheck(builder, baseTypeValue, arrayType.getBaseType(), checkAccessor, false, false);
      resultValue = LLVM.LLVMBuildAnd(builder, resultValue, baseTypeMatches, "");
      endResultBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);
    }
    else if (checkType instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) checkType;
      Type[] parameterTypes = functionType.getParameterTypes();
      // check the always-present header data
      LLVMValueRef nullabilityMatches;
      if (ignoreTypeModifiers)
      {
        nullabilityMatches = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
      }
      else
      {
        LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
        LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
        nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isNullable() ? 1 : 0, false), "");
      }
      LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
      LLVMValueRef immutabilityValue = LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
      LLVMValueRef immutabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, immutabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isImmutable() ? 1 : 0, false), "");
      LLVMValueRef returnTypePointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 4, "");
      LLVMValueRef returnTypeValue = LLVM.LLVMBuildLoad(builder, returnTypePointer, "");
      LLVMValueRef returnTypeMatches = buildTypeInfoCheck(builder, returnTypeValue, functionType.getReturnType(), checkAccessor, false, false);
      LLVMValueRef numParametersPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 5, "");
      LLVMValueRef numParametersValue = LLVM.LLVMBuildLoad(builder, numParametersPointer, "");
      LLVMValueRef numParametersMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, numParametersValue, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), parameterTypes.length, false), "");
      LLVMValueRef nullabilityImmutabilityMatches = LLVM.LLVMBuildAnd(builder, nullabilityMatches, immutabilityMatches, "");
      LLVMValueRef matches = LLVM.LLVMBuildAnd(builder, returnTypeMatches, numParametersMatches, "");
      matches = LLVM.LLVMBuildAnd(builder, nullabilityImmutabilityMatches, matches, "");

      if (parameterTypes.length == 0)
      {
        // there are no parameters to check, so return whether or not the header data matched
        resultValue = matches;
        endResultBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildBr(builder, continuationBlock);
      }
      else
      {
        // we have parameters to check, so check them if the header data matched
        // (if it didn't match we might have the number of parameters wrong)
        LLVMBasicBlockRef checkParametersContinuationBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoCheckParametersContinue");
        LLVMBasicBlockRef checkParametersBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoCheckParameters");
        LLVMBasicBlockRef checkParametersStartBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildCondBr(builder, matches, checkParametersBlock, checkParametersContinuationBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, checkParametersBlock);
        // the RTTI block has already been casted to the right struct for this number of parameters, so just index through it
        LLVMValueRef currentMatches = null;
        for (int i = 0; i < parameterTypes.length; ++i)
        {
          LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 6, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), i, false)};
          LLVMValueRef paramTypePointer = LLVM.LLVMBuildGEP(builder, castedRTTIPointer, C.toNativePointerArray(indices, false, true), indices.length, "");
          LLVMValueRef paramType = LLVM.LLVMBuildLoad(builder, paramTypePointer, "");
          LLVMValueRef paramTypeMatches = buildTypeInfoCheck(builder, paramType, parameterTypes[i], checkAccessor, false, false);
          if (currentMatches == null)
          {
            currentMatches = paramTypeMatches;
          }
          else
          {
            currentMatches = LLVM.LLVMBuildAnd(builder, currentMatches, paramTypeMatches, "");
          }
        }
        LLVMBasicBlockRef endCheckParametersBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildBr(builder, checkParametersContinuationBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, checkParametersContinuationBlock);
        LLVMValueRef parametersPhi = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt1Type(), "");
        LLVMValueRef[] parametersIncomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false), currentMatches};
        LLVMBasicBlockRef[] parametersIncomingBlocks = new LLVMBasicBlockRef[] {checkParametersStartBlock, endCheckParametersBlock};
        LLVM.LLVMAddIncoming(parametersPhi, C.toNativePointerArray(parametersIncomingValues, false, true), C.toNativePointerArray(parametersIncomingBlocks, false, true), parametersIncomingValues.length);

        resultValue = parametersPhi;
        endResultBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildBr(builder, continuationBlock);
      }
    }
    else if (checkType instanceof NamedType)
    {
      NamedType namedType = (NamedType) checkType;
      LLVMValueRef headerMatches;
      if (ignoreTypeModifiers)
      {
        headerMatches = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
      }
      else
      {
        LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
        LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
        LLVMValueRef nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isNullable() ? 1 : 0, false), "");
        LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
        LLVMValueRef immutabilityValue = LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
        LLVMValueRef immutabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, immutabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isContextuallyImmutable() ? 1 : 0, false), "");
        headerMatches = LLVM.LLVMBuildAnd(builder, nullabilityMatches, immutabilityMatches, "");
      }
      LLVMValueRef qualifiedNameByteArrayPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 4, "");
      LLVMValueRef qualifiedNameByteArray = LLVM.LLVMBuildLoad(builder, qualifiedNameByteArrayPointer, "");
      LLVMValueRef qualifiedNameLengthPointer = typeHelper.getArrayLengthPointer(builder, qualifiedNameByteArray);
      LLVMValueRef qualifiedNameLength = LLVM.LLVMBuildLoad(builder, qualifiedNameLengthPointer, "");

      LLVMValueRef checkQualifiedNameString = codeGenerator.addStringConstant(namedType.getResolvedTypeDefinition().getQualifiedName().toString());
      LLVMValueRef checkQualifiedNameLengthPointer = typeHelper.getArrayLengthPointer(builder, checkQualifiedNameString);
      LLVMValueRef checkQualifiedNameLength = LLVM.LLVMBuildLoad(builder, checkQualifiedNameLengthPointer, "");
      LLVMValueRef lengthMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, qualifiedNameLength, checkQualifiedNameLength, "");

      LLVMValueRef[] numTypeArgumentsIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                                   LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false),
                                                                   LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false)};
      LLVMValueRef numTypeArgumentsPointer = LLVM.LLVMBuildGEP(builder, castedRTTIPointer, C.toNativePointerArray(numTypeArgumentsIndices, false, true), numTypeArgumentsIndices.length, "");
      LLVMValueRef numTypeArguments = LLVM.LLVMBuildLoad(builder, numTypeArgumentsPointer, "");
      Type[] typeArguments = namedType.getTypeArguments();
      LLVMValueRef numTypeArgumentsMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, numTypeArguments, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), typeArguments == null ? 0 : typeArguments.length, false), "");

      headerMatches = LLVM.LLVMBuildAnd(builder, headerMatches, lengthMatches, "");
      headerMatches = LLVM.LLVMBuildAnd(builder, headerMatches, numTypeArgumentsMatches, "");

      LLVMBasicBlockRef checkNamedTypeContinuationBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoCheckNamedTypeContinue");
      LLVMBasicBlockRef checkTypeArgsBlock = null;
      if (typeArguments != null)
      {
        checkTypeArgsBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoCheckTypeArguments");
      }
      LLVMBasicBlockRef checkNameBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoCheckQName");
      LLVMBasicBlockRef endCheckHeaderBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildCondBr(builder, headerMatches, checkNameBlock, checkNamedTypeContinuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, checkNameBlock);
      LLVMValueRef firstBytePointer = typeHelper.getNonProxiedArrayElementPointer(builder, qualifiedNameByteArray, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false));
      LLVMValueRef checkFirstBytePointer = typeHelper.getNonProxiedArrayElementPointer(builder, checkQualifiedNameString, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false));
      LLVMValueRef strncmpFunction = getStringComparisonFunction();
      LLVMValueRef[] strncmpArguments = new LLVMValueRef[] {firstBytePointer, checkFirstBytePointer, checkQualifiedNameLength};
      LLVMValueRef strncmpResult = LLVM.LLVMBuildCall(builder, strncmpFunction, C.toNativePointerArray(strncmpArguments, false, true), strncmpArguments.length, "");
      LLVMValueRef nameMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, strncmpResult, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false), "");

      LLVMBasicBlockRef endCheckNameBlock = LLVM.LLVMGetInsertBlock(builder);
      if (typeArguments == null)
      {
        LLVM.LLVMBuildBr(builder, checkNamedTypeContinuationBlock);
      }
      else
      {
        LLVM.LLVMBuildCondBr(builder, nameMatches, checkTypeArgsBlock, checkNamedTypeContinuationBlock);
      }

      // start building the result phi early
      LLVM.LLVMPositionBuilderAtEnd(builder, checkNamedTypeContinuationBlock);
      LLVMValueRef resultPhi = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt1Type(), "");
      LLVMValueRef nameResultPhiValue;
      if (typeArguments == null)
      {
        nameResultPhiValue = nameMatches;
      }
      else
      {
        // if there are type arguments, then a branch to the continuation from the name check means that it failed
        nameResultPhiValue = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
      }
      LLVMValueRef[] initialIncomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false), nameResultPhiValue};
      LLVMBasicBlockRef[] initialIncomingBlocks = new LLVMBasicBlockRef[] {endCheckHeaderBlock, endCheckNameBlock};
      LLVM.LLVMAddIncoming(resultPhi, C.toNativePointerArray(initialIncomingValues, false, true), C.toNativePointerArray(initialIncomingBlocks, false, true), initialIncomingValues.length);

      if (typeArguments != null)
      {
        LLVM.LLVMPositionBuilderAtEnd(builder, checkTypeArgsBlock);
        for (int i = 0; i < typeArguments.length; ++i)
        {
          LLVMValueRef[] typeInfoIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                               LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false),
                                                               LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false),
                                                               LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), i, false)};
          LLVMValueRef typeInfoPointer = LLVM.LLVMBuildGEP(builder, castedRTTIPointer, C.toNativePointerArray(typeInfoIndices, false, true), typeInfoIndices.length, "");
          LLVMValueRef typeInfo = LLVM.LLVMBuildLoad(builder, typeInfoPointer, "");
          LLVMValueRef matches = buildTypeInfoCheck(builder, typeInfo, typeArguments[i], checkAccessor, false, looselyMatchWildcards);
          LLVMBasicBlockRef nextBlock;
          LLVMValueRef[] newIncomingValues;
          LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
          if (i == typeArguments.length - 1)
          {
            nextBlock = checkNamedTypeContinuationBlock;
            newIncomingValues = new LLVMValueRef[] {matches};
            LLVM.LLVMBuildBr(builder, nextBlock);
          }
          else
          {
            nextBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoCheckNextTypeArgument");
            newIncomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false)};
            LLVM.LLVMBuildCondBr(builder, matches, nextBlock, checkNamedTypeContinuationBlock);
          }

          // add to the result phi
          LLVMBasicBlockRef[] newIncomingBlocks = new LLVMBasicBlockRef[] {currentBlock};
          LLVM.LLVMAddIncoming(resultPhi, C.toNativePointerArray(newIncomingValues, false, true), C.toNativePointerArray(newIncomingBlocks, false, true), newIncomingValues.length);

          LLVM.LLVMPositionBuilderAtEnd(builder, nextBlock);
        }
      }

      resultValue = resultPhi;
      endResultBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);
    }
    else if (checkType instanceof NullType)
    {
      resultValue = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
      endResultBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);
    }
    else if (checkType instanceof ObjectType)
    {
      ObjectType objectType = (ObjectType) checkType;
      if (ignoreTypeModifiers)
      {
        resultValue = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
      }
      else
      {
        LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
        LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
        LLVMValueRef nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), objectType.isNullable() ? 1 : 0, false), "");
        LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
        LLVMValueRef immutabilityValue = LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
        LLVMValueRef immutabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, immutabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), objectType.isContextuallyImmutable() ? 1 : 0, false), "");
        resultValue = LLVM.LLVMBuildAnd(builder, nullabilityMatches, immutabilityMatches, "");
      }

      endResultBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);
    }
    else if (checkType instanceof PrimitiveType)
    {
      PrimitiveType primitiveType = (PrimitiveType) checkType;
      LLVMValueRef nullabilityMatches;
      if (ignoreTypeModifiers)
      {
        nullabilityMatches = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
      }
      else
      {
        LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
        LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
        nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), primitiveType.isNullable() ? 1 : 0, false), "");
      }
      LLVMValueRef primitiveIdPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
      LLVMValueRef primitiveIdValue = LLVM.LLVMBuildLoad(builder, primitiveIdPointer, "");
      LLVMValueRef primitiveIdMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, primitiveIdValue, LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), primitiveType.getPrimitiveTypeType().getRunTimeId(), false), "");

      resultValue = LLVM.LLVMBuildAnd(builder, nullabilityMatches, primitiveIdMatches, "");
      endResultBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);
    }
    else if (checkType instanceof TupleType)
    {
      TupleType tupleType = (TupleType) checkType;
      Type[] subTypes = tupleType.getSubTypes();
      // check the always-present header data
      LLVMValueRef nullabilityMatches;
      if (ignoreTypeModifiers)
      {
        nullabilityMatches = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
      }
      else
      {
        LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
        LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
        nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), tupleType.isNullable() ? 1 : 0, false), "");
      }
      LLVMValueRef numSubTypesPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
      LLVMValueRef numSubTypesValue = LLVM.LLVMBuildLoad(builder, numSubTypesPointer, "");
      LLVMValueRef numSubTypesMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, numSubTypesValue, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), subTypes.length, false), "");
      LLVMValueRef headerMatches = LLVM.LLVMBuildAnd(builder, nullabilityMatches, numSubTypesMatches, "");

      // check the sub-types if the header data matched
      // (if it didn't match we might have the number of parameters wrong)
      LLVMBasicBlockRef checkSubTypesContinuationBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoCheckSubTypesContinue");
      LLVMBasicBlockRef checkSubTypesBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoCheckSubTypes");
      LLVMBasicBlockRef checkSubTypesStartBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildCondBr(builder, headerMatches, checkSubTypesBlock, checkSubTypesContinuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, checkSubTypesBlock);
      // the RTTI block has already been casted to the right struct for this number of sub-types, so just index through it
      LLVMValueRef currentMatches = null;
      for (int i = 0; i < subTypes.length; ++i)
      {
        LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                     LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 4, false),
                                                     LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), i, false)};
        LLVMValueRef subTypePointer = LLVM.LLVMBuildGEP(builder, castedRTTIPointer, C.toNativePointerArray(indices, false, true), indices.length, "");
        LLVMValueRef subType = LLVM.LLVMBuildLoad(builder, subTypePointer, "");
        LLVMValueRef subTypeMatches = buildTypeInfoCheck(builder, subType, subTypes[i], checkAccessor, false, false);
        if (currentMatches == null)
        {
          currentMatches = subTypeMatches;
        }
        else
        {
          currentMatches = LLVM.LLVMBuildAnd(builder, currentMatches, subTypeMatches, "");
        }
      }
      LLVMBasicBlockRef endCheckSubTypesBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, checkSubTypesContinuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, checkSubTypesContinuationBlock);
      LLVMValueRef subTypesPhi = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt1Type(), "");
      LLVMValueRef[] subTypesIncomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false), currentMatches};
      LLVMBasicBlockRef[] subTypesIncomingBlocks = new LLVMBasicBlockRef[] {checkSubTypesStartBlock, endCheckSubTypesBlock};
      LLVM.LLVMAddIncoming(subTypesPhi, C.toNativePointerArray(subTypesIncomingValues, false, true), C.toNativePointerArray(subTypesIncomingBlocks, false, true), subTypesIncomingValues.length);

      resultValue = subTypesPhi;
      endResultBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);
    }
    else if (checkType instanceof VoidType)
    {
      resultValue = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
      endResultBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);
    }
    else
    {
      throw new IllegalArgumentException("Cannot generate RTTI check - unknown sort of Type: " + checkType);
    }

    LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
    LLVMValueRef phi = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt1Type(), "");
    LLVMValueRef[] incomingValues = new LLVMValueRef[] {resultValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false)};
    LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {endResultBlock, startBlock};
    LLVM.LLVMAddIncoming(phi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
    return phi;
  }

  /**
   * Builds code to create an RTTI block for the specified type.
   * @param builder - the builder to build code with
   * @param type - the type to generate the RTTI block for
   * @param typeParameterAccessor - an accessor for the values of any type parameters referenced inside type
   * @return the RTTI block created, in a standard pointer representation
   */
  public LLVMValueRef buildRTTICreation(LLVMBuilderRef builder, Type type, TypeParameterAccessor typeParameterAccessor)
  {
    if (!containsTypeParameters(type) || typeParameterAccessor == null)
    {
      return getRTTI(type);
    }

    if (type instanceof NamedType && ((NamedType) type).getResolvedTypeParameter() != null)
    {
      NamedType namedType = (NamedType) type;
      // this is a TypeParameter, so just return the RTTI for it
      // but make sure to add forced nullability and immutability to it if necessary
      LLVMValueRef typeArgument = typeParameterAccessor.findTypeParameterRTTI(namedType.getResolvedTypeParameter());
      if (typeArgument == null)
      {
        throw new IllegalArgumentException("Cannot build RTTI for a type parameter which is not in the mapping: " + namedType);
      }
      LLVMValueRef result = typeArgument;
      if (namedType.isNullable() || namedType.isContextuallyImmutable())
      {
        // add forced nullability/immutability
        LLVMValueRef[] arguments = new LLVMValueRef[] {typeArgument,
                                                       LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), // only add modifiers, don't remove them
                                                       LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isNullable() ? 1 : 0, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isContextuallyImmutable() ? 1 : 0, false)};
        result = LLVM.LLVMBuildCall(builder, getTypeModifierForcingFunction(), C.toNativePointerArray(arguments, false, true), arguments.length, "");
      }
      return result;
    }

    LLVMTypeRef nativeType = LLVM.LLVMPointerType(getRTTIStructType(type), 0);

    // find a constant reference to the type search list
    LLVMValueRef typeSearchList;
    if (type instanceof NamedType)
    {
      TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        typeSearchList = virtualFunctionHandler.getTypeSearchList(typeDefinition);
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        typeSearchList = virtualFunctionHandler.getObjectTypeSearchList(type, virtualFunctionHandler.getBaseChangeObjectVFT(type));
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        typeSearchList = virtualFunctionHandler.getTypeSearchList(typeDefinition);
      }
      else
      {
        throw new IllegalArgumentException("Cannot find RTTI for unknown NamedType: " + type);
      }
    }
    else if (type instanceof ObjectType)
    {
      typeSearchList = virtualFunctionHandler.getObjectTypeSearchList(type, virtualFunctionHandler.getObjectVFTGlobal());
    }
    else
    {
      typeSearchList = virtualFunctionHandler.getObjectTypeSearchList(type, virtualFunctionHandler.getBaseChangeObjectVFT(type));
    }
    typeSearchList = LLVM.LLVMConstBitCast(typeSearchList, LLVM.LLVMPointerType(getTypeSearchListType(), 0));

    // allocate the RTTI block
    LLVMValueRef pointer = codeGenerator.buildHeapAllocation(builder, nativeType);

    // now fill in the data inside the RTTI block
    LLVMValueRef typeSearchListPointer = LLVM.LLVMBuildStructGEP(builder, pointer, 0, "");
    LLVM.LLVMBuildStore(builder, typeSearchList, typeSearchListPointer);

    LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), getSortId(type), false);
    LLVMValueRef sortIdPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 1, "");
    LLVM.LLVMBuildStore(builder, sortId, sortIdPtr);

    if (type instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) type;

      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isNullable() ? 1 : 0, false);
      LLVMValueRef nullablePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 2, "");
      LLVM.LLVMBuildStore(builder, nullable, nullablePtr);

      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isContextuallyImmutable() ? 1 : 0, false);
      LLVMValueRef immutablePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 3, "");
      LLVM.LLVMBuildStore(builder, immutable, immutablePtr);

      LLVMValueRef baseTypeRTTI = buildRTTICreation(builder, arrayType.getBaseType(), typeParameterAccessor);
      LLVMValueRef baseTypeRTTIPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 4, "");
      LLVM.LLVMBuildStore(builder, baseTypeRTTI, baseTypeRTTIPtr);
    }
    else if (type instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) type;

      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isNullable() ? 1 : 0, false);
      LLVMValueRef nullablePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 2, "");
      LLVM.LLVMBuildStore(builder, nullable, nullablePtr);

      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isImmutable() ? 1 : 0, false);
      LLVMValueRef immutablePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 3, "");
      LLVM.LLVMBuildStore(builder, immutable, immutablePtr);

      LLVMValueRef returnTypeRTTI = buildRTTICreation(builder, functionType.getReturnType(), typeParameterAccessor);
      LLVMValueRef returnTypeRTTIPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 4, "");
      LLVM.LLVMBuildStore(builder, returnTypeRTTI, returnTypeRTTIPtr);

      Type[] parameterTypes = functionType.getParameterTypes();
      LLVMValueRef numParams = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), parameterTypes.length, false);
      LLVMValueRef numParamsPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 5, "");
      LLVM.LLVMBuildStore(builder, numParams, numParamsPtr);

      for (int i = 0; i < parameterTypes.length; ++i)
      {
        LLVMValueRef parameterRTTI = buildRTTICreation(builder, parameterTypes[i], typeParameterAccessor);
        LLVMValueRef[] parameterIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                              LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 6, false),
                                                              LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), i, false)};
        LLVMValueRef parameterRTTIPtr = LLVM.LLVMBuildGEP(builder, pointer, C.toNativePointerArray(parameterIndices, false, true), parameterIndices.length, "");
        LLVM.LLVMBuildStore(builder, parameterRTTI, parameterRTTIPtr);
      }
    }
    else if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;

      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isNullable() ? 1 : 0, false);
      LLVMValueRef nullablePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 2, "");
      LLVM.LLVMBuildStore(builder, nullable, nullablePtr);

      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isContextuallyImmutable() ? 1 : 0, false);
      LLVMValueRef immutablePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 3, "");
      LLVM.LLVMBuildStore(builder, immutable, immutablePtr);

      LLVMValueRef qualifiedName = codeGenerator.addStringConstant(namedType.getResolvedTypeDefinition().getQualifiedName().toString());
      qualifiedName = LLVM.LLVMConstBitCast(qualifiedName, typeHelper.findRawStringType());
      LLVMValueRef qualifiedNamePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 4, "");
      LLVM.LLVMBuildStore(builder, qualifiedName, qualifiedNamePtr);

      Type[] typeArguments = namedType.getTypeArguments();
      LLVMValueRef[] numTypeArgumentsIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                                   LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false),
                                                                   LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false)};
      LLVMValueRef numTypeArguments = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), typeArguments.length, false);
      LLVMValueRef numTypeArgumentsPtr = LLVM.LLVMBuildGEP(builder, pointer, C.toNativePointerArray(numTypeArgumentsIndices, false, true), numTypeArgumentsIndices.length, "");
      LLVM.LLVMBuildStore(builder, numTypeArguments, numTypeArgumentsPtr);

      for (int i = 0; i < typeArguments.length; ++i)
      {
        LLVMValueRef argumentRTTI = buildRTTICreation(builder, typeArguments[i], typeParameterAccessor);
        LLVMValueRef[] argumentIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                             LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false),
                                                             LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false),
                                                             LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), i, false)};
        LLVMValueRef argumentRTTIPtr = LLVM.LLVMBuildGEP(builder, pointer, C.toNativePointerArray(argumentIndices, false, true), argumentIndices.length, "");
        LLVM.LLVMBuildStore(builder, argumentRTTI, argumentRTTIPtr);
      }
    }
    else if (type instanceof NullType)
    {
      // nothing to fill in
    }
    else if (type instanceof ObjectType)
    {
      ObjectType objectType = (ObjectType) type;

      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), objectType.isNullable() ? 1 : 0, false);
      LLVMValueRef nullablePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 2, "");
      LLVM.LLVMBuildStore(builder, nullable, nullablePtr);

      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), objectType.isContextuallyImmutable() ? 1 : 0, false);
      LLVMValueRef immutablePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 3, "");
      LLVM.LLVMBuildStore(builder, immutable, immutablePtr);
    }
    else if (type instanceof PrimitiveType)
    {
      PrimitiveType primitiveType = (PrimitiveType) type;

      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), primitiveType.isNullable() ? 1 : 0, false);
      LLVMValueRef nullablePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 2, "");
      LLVM.LLVMBuildStore(builder, nullable, nullablePtr);

      LLVMValueRef primitiveId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), primitiveType.getPrimitiveTypeType().getRunTimeId(), false);
      LLVMValueRef primitiveIdPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 3, "");
      LLVM.LLVMBuildStore(builder, primitiveId, primitiveIdPtr);
    }
    else if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;

      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), tupleType.isNullable() ? 1 : 0, false);
      LLVMValueRef nullablePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 2, "");
      LLVM.LLVMBuildStore(builder, nullable, nullablePtr);

      Type[] subTypes = tupleType.getSubTypes();
      LLVMValueRef numSubTypes = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), subTypes.length, false);
      LLVMValueRef numSubTypesPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 3, "");
      LLVM.LLVMBuildStore(builder, numSubTypes, numSubTypesPtr);

      for (int i = 0; i < subTypes.length; ++i)
      {
        LLVMValueRef subTypeRTTI = buildRTTICreation(builder, subTypes[i], typeParameterAccessor);
        LLVMValueRef[] subTypeIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                            LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 4, false),
                                                            LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), i, false)};
        LLVMValueRef subTypeRTTIPtr = LLVM.LLVMBuildGEP(builder, pointer, C.toNativePointerArray(subTypeIndices, false, true), subTypeIndices.length, "");
        LLVM.LLVMBuildStore(builder, subTypeRTTI, subTypeRTTIPtr);
      }
    }
    else if (type instanceof VoidType)
    {
      // nothing to fill in
    }
    else if (type instanceof WildcardType)
    {
      WildcardType wildcardType = (WildcardType) type;
      Type[] superTypes = wildcardType.getSuperTypes();
      Type[] subTypes = wildcardType.getSubTypes();

      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), wildcardType.canBeNullable() ? 1 : 0, false);
      LLVMValueRef nullablePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 2, "");
      LLVM.LLVMBuildStore(builder, nullable, nullablePtr);

      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), wildcardType.canBeExplicitlyImmutable() ? 1 : 0, false);
      LLVMValueRef immutablePtr = LLVM.LLVMBuildStructGEP(builder, pointer, 3, "");
      LLVM.LLVMBuildStore(builder, immutable, immutablePtr);

      LLVMValueRef numSuperTypes = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), superTypes.length, false);
      LLVMValueRef numSuperTypesPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 4, "");
      LLVM.LLVMBuildStore(builder, numSuperTypes, numSuperTypesPtr);

      LLVMValueRef numSubTypes = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), subTypes.length, false);
      LLVMValueRef numSubTypesPtr = LLVM.LLVMBuildStructGEP(builder, pointer, 5, "");
      LLVM.LLVMBuildStore(builder, numSubTypes, numSubTypesPtr);

      for (int i = 0; i < superTypes.length; ++i)
      {
        LLVMValueRef superTypeRTTI = buildRTTICreation(builder, superTypes[i], typeParameterAccessor);
        LLVMValueRef[] superTypeIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                              LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 6, false),
                                                              LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), i, false)};
        LLVMValueRef superTypeRTTIPtr = LLVM.LLVMBuildGEP(builder, pointer, C.toNativePointerArray(superTypeIndices, false, true), superTypeIndices.length, "");
        LLVM.LLVMBuildStore(builder, superTypeRTTI, superTypeRTTIPtr);
      }
      for (int i = 0; i < subTypes.length; ++i)
      {
        LLVMValueRef subTypeRTTI = buildRTTICreation(builder, subTypes[i], typeParameterAccessor);
        LLVMValueRef[] subTypeIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                            LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 6, false),
                                                            LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), superTypes.length + i, false)};
        LLVMValueRef subTypeRTTIPtr = LLVM.LLVMBuildGEP(builder, pointer, C.toNativePointerArray(subTypeIndices, false, true), subTypeIndices.length, "");
        LLVM.LLVMBuildStore(builder, subTypeRTTI, subTypeRTTIPtr);
      }
    }
    else
    {
      throw new IllegalArgumentException("Unknown type: " + type);
    }

    return LLVM.LLVMBuildBitCast(builder, pointer, getGenericRTTIType(), "");
  }


  /**
   * @return an LLVM function representing: i32 strncmp(i8*, i8*, i32)
   */
  private LLVMValueRef getStringComparisonFunction()
  {
    final String STRNCMP_NAME = "strncmp";
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, STRNCMP_NAME);
    if (existingFunction != null)
    {
      return existingFunction;
    }
    LLVMTypeRef bytePoinerType = LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0);
    LLVMTypeRef[] parameterTypes = new LLVMTypeRef[] {bytePoinerType, bytePoinerType, LLVM.LLVMInt32Type()};
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(LLVM.LLVMInt32Type(), C.toNativePointerArray(parameterTypes, false, true), parameterTypes.length, false);
    LLVMValueRef strncmpFunction = LLVM.LLVMAddFunction(module, STRNCMP_NAME, functionType);
    return strncmpFunction;
  }

  /**
   * @return an LLVM function representing the 'plinth_force_type_modifiers' function, which forces an RTTI block to have the specified type modifiers, by cloning it and altering a cloned version if necessary
   */
  private LLVMValueRef getTypeModifierForcingFunction()
  {
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, FORCE_TYPE_MODIFIERS_FUNCTION_NAME);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    LLVMTypeRef[] types = new LLVMTypeRef[] {getGenericRTTIType(), LLVM.LLVMInt1Type(), LLVM.LLVMInt1Type(), LLVM.LLVMInt1Type()};
    LLVMTypeRef resultType = getGenericRTTIType();
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);
    return LLVM.LLVMAddFunction(module, FORCE_TYPE_MODIFIERS_FUNCTION_NAME, functionType);
  }

  /**
   * @return an LLVM function representing the 'plinth_check_type_matches' function, which checks whether two RTTI blocks are equivalent
   */
  private LLVMValueRef getCheckTypeMatchesFunction()
  {
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, CHECK_TYPE_MATCHES_FUNCTION_NAME);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    LLVMTypeRef rttiType = getGenericRTTIType();
    LLVMTypeRef mapperType = getGenericTypeArgumentMapperType();
    LLVMTypeRef ignoreNullabilityType = LLVM.LLVMInt1Type();
    LLVMTypeRef ignoreImmutabilityType = LLVM.LLVMInt1Type();
    LLVMTypeRef looselyMatchWildcardsType = LLVM.LLVMInt1Type();
    LLVMTypeRef[] types = new LLVMTypeRef[] {rttiType, rttiType, mapperType, mapperType, ignoreNullabilityType, ignoreImmutabilityType, looselyMatchWildcardsType};
    LLVMTypeRef resultType = LLVM.LLVMInt1Type();
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);
    return LLVM.LLVMAddFunction(module, CHECK_TYPE_MATCHES_FUNCTION_NAME, functionType);
  }

  /**
   * Finds the sort id of the specified Type.
   * @param type - the Type to get the sort id of
   * @return the sort id of the specified Type
   */
  private static byte getSortId(Type type)
  {
    if (type instanceof ArrayType)
    {
      return ARRAY_SORT_ID;
    }
    if (type instanceof FunctionType)
    {
      return FUNCTION_SORT_ID;
    }
    if (type instanceof NamedType && ((NamedType) type).getResolvedTypeDefinition() instanceof ClassDefinition)
    {
      return CLASS_SORT_ID;
    }
    if (type instanceof NamedType && ((NamedType) type).getResolvedTypeDefinition() instanceof InterfaceDefinition)
    {
      return INTERFACE_SORT_ID;
    }
    if (type instanceof NamedType && ((NamedType) type).getResolvedTypeDefinition() instanceof CompoundDefinition)
    {
      return COMPOUND_SORT_ID;
    }
    if (type instanceof NullType)
    {
      return NULL_SORT_ID;
    }
    if (type instanceof ObjectType)
    {
      return OBJECT_SORT_ID;
    }
    if (type instanceof PrimitiveType)
    {
      return PRIMITIVE_SORT_ID;
    }
    if (type instanceof TupleType)
    {
      return TUPLE_SORT_ID;
    }
    if (type instanceof VoidType)
    {
      return VOID_SORT_ID;
    }
    if (type instanceof NamedType && ((NamedType) type).getResolvedTypeParameter() != null)
    {
      return TYPE_PARAMETER_SORT_ID;
    }
    if (type instanceof WildcardType)
    {
      return WILDCARD_SORT_ID;
    }
    throw new IllegalArgumentException("Unknown sort of Type: " + type);
  }

  /**
   * Checks whether the RTTI for the specified type will contain any references to type parameters.
   * @param type - the type to check
   * @return true if the type references any type parameters, false otherwise
   */
  private static boolean containsTypeParameters(Type type)
  {
    if (type instanceof ArrayType)
    {
      return containsTypeParameters(((ArrayType) type).getBaseType());
    }
    if (type instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) type;
      if (containsTypeParameters(functionType.getReturnType()))
      {
        return true;
      }
      for (Type t : functionType.getParameterTypes())
      {
        if (containsTypeParameters(t))
        {
          return true;
        }
      }
      // ignore exception types, as they are erased at runtime
      return false;
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      if (namedType.getResolvedTypeParameter() != null)
      {
        return true;
      }
      Type[] typeArguments = namedType.getTypeArguments();
      if (typeArguments != null)
      {
        for (Type t : typeArguments)
        {
          if (containsTypeParameters(t))
          {
            return true;
          }
        }
      }
      return false;
    }
    if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      for (Type t : tupleType.getSubTypes())
      {
        if (containsTypeParameters(t))
        {
          return true;
        }
      }
      return false;
    }
    if (type instanceof WildcardType)
    {
      WildcardType wildcardType = (WildcardType) type;
      for (Type t : wildcardType.getSuperTypes())
      {
        if (containsTypeParameters(t))
        {
          return true;
        }
      }
      for (Type t : wildcardType.getSubTypes())
      {
        if (containsTypeParameters(t))
        {
          return true;
        }
      }
      return false;
    }
    if (type instanceof NullType || type instanceof ObjectType || type instanceof PrimitiveType || type instanceof VoidType)
    {
      return false;
    }
    throw new IllegalArgumentException("Unknown type: " + type);
  }
}
