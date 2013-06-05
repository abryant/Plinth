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
  private static final String IS_TYPE_EQUIVALENT_FUNCTION_NAME = "plinth_is_type_equivalent";

  private static final String PURE_RTTI_MANGLED_NAME_PREFIX = "_PURE_RTTI_";
  private static final String INSTANCE_RTTI_MANGLED_NAME_PREFIX = "_INSTANCE_RTTI_";
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

  private LLVMModuleRef module;

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
   * @return the pointer to the field inside the specified base value which should contain a pointer to the instance RTTI
   */
  public LLVMValueRef getRTTIPointer(LLVMBuilderRef builder, LLVMValueRef baseValue)
  {
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false)};
    return LLVM.LLVMBuildGEP(builder, baseValue, C.toNativePointerArray(indices, false, true), indices.length, "");
  }

  /**
   * Looks up the VFT search list on the specified object value. This is done by first looking up the instance RTTI on the object, and then loading its VFT search list pointer.
   * @param builder - the builder to build the lookup code with
   * @param baseValue - the object-typed value to look up the VFT search list on
   * @return the VFT search list from the RTTI of the specified value
   */
  public LLVMValueRef lookupVFTSearchList(LLVMBuilderRef builder, LLVMValueRef baseValue)
  {
    LLVMValueRef rttiPointer = getRTTIPointer(builder, baseValue);
    LLVMValueRef rtti = LLVM.LLVMBuildLoad(builder, rttiPointer, "");
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false)};
    LLVMValueRef vftSearchListPointer = LLVM.LLVMBuildGEP(builder, rtti, C.toNativePointerArray(indices, false, true), indices.length, "");
    LLVMValueRef vftSearchList = LLVM.LLVMBuildLoad(builder, vftSearchListPointer, "");
    return vftSearchList;
  }

  /**
   * Looks up the pure RTTI on the specified object value. This is done by first looking up the instance RTTI on the object, and then doing a GEP for its pure RTTI subsection.
   * @param builder - the builder to build the lookup code with
   * @param baseValue - the object-typed value to look up the pure RTTI on
   * @return the pure RTTI from the specified value
   */
  public LLVMValueRef lookupPureRTTI(LLVMBuilderRef builder, LLVMValueRef baseValue)
  {
    LLVMValueRef rttiPointer = getRTTIPointer(builder, baseValue);
    LLVMValueRef rtti = LLVM.LLVMBuildLoad(builder, rttiPointer, "");
    LLVMValueRef[] rttiIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                     LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false)};
    LLVMValueRef pureRTTIPointer = LLVM.LLVMBuildGEP(builder, rtti, C.toNativePointerArray(rttiIndices, false, true), rttiIndices.length, "");
    return pureRTTIPointer;
  }

  /**
   * Looks up the name of the class in the specified RTTI.
   * Assumes that baseValue is a class's RTTI value.
   * @param builder - the builder to build the lookup code with
   * @param rttiPointer - the pointer to the RTTI to look up the class name inside
   * @return a []ubyte holding the class's fully qualified name
   */
  public LLVMValueRef lookupNamedTypeName(LLVMBuilderRef builder, LLVMValueRef rttiPointer)
  {
    // cast the generic RTTI struct to something that looks like the start of a NamedType RTTI struct
    LLVMTypeRef sortIdType = LLVM.LLVMInt8Type();
    LLVMTypeRef sizeType = LLVM.LLVMInt32Type();
    LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
    LLVMTypeRef immutableType = LLVM.LLVMInt1Type();
    LLVMTypeRef qualifiedNameType = typeHelper.findRawStringType();

    LLVMTypeRef[] namedSubTypes = new LLVMTypeRef[] {sortIdType, sizeType, nullableType, immutableType, qualifiedNameType};
    LLVMTypeRef namedRTTIType = LLVM.LLVMStructType(C.toNativePointerArray(namedSubTypes, false, true), namedSubTypes.length, false);

    LLVMValueRef castedObjectRTTI = LLVM.LLVMBuildBitCast(builder, rttiPointer, LLVM.LLVMPointerType(namedRTTIType, 0), "");
    LLVMValueRef classQualifiedNameUbyteArrayPointer = LLVM.LLVMBuildStructGEP(builder, castedObjectRTTI, 4, "");
    LLVMValueRef classQualifiedNameUbyteArray = LLVM.LLVMBuildLoad(builder, classQualifiedNameUbyteArrayPointer, "");
    return classQualifiedNameUbyteArray;
  }

  /**
   * Finds the pure RTTI for the specified type, without a VFT search list
   * @param type - the type to find the RTTI for
   * @return the pure RTTI for the specified type
   */
  public LLVMValueRef getPureRTTI(Type type)
  {
    String mangledName = PURE_RTTI_MANGLED_NAME_PREFIX + type.getMangledName();
    LLVMValueRef existingGlobal = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingGlobal != null)
    {
      return LLVM.LLVMConstBitCast(existingGlobal, getGenericPureRTTIType());
    }

    LLVMTypeRef pureRTTIType = getPureRTTIStructType(type);

    LLVMValueRef global = LLVM.LLVMAddGlobal(module, pureRTTIType, mangledName);
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMLinkOnceODRLinkage);
    LLVM.LLVMSetVisibility(global, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    LLVM.LLVMSetGlobalConstant(global, true);

    LLVMValueRef pureRTTI = getPureRTTIStruct(type);
    LLVM.LLVMSetInitializer(global, pureRTTI);

    return LLVM.LLVMConstBitCast(global, getGenericPureRTTIType());
  }

  /**
   * Finds the instance RTTI for the specified type, including a VFT search list
   * @param type - the type to find the RTTI for
   * @return the instance RTTI for the specified type
   */
  public LLVMValueRef getInstanceRTTI(Type type)
  {
    String mangledName = INSTANCE_RTTI_MANGLED_NAME_PREFIX + type.getMangledName();
    LLVMValueRef existingGlobal = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingGlobal != null)
    {
      return LLVM.LLVMConstBitCast(existingGlobal, getGenericInstanceRTTIType());
    }

    LLVMTypeRef vftSearchListType = LLVM.LLVMPointerType(virtualFunctionHandler.getVFTSearchListType(), 0);
    LLVMTypeRef pureRTTIType = getPureRTTIStructType(type);
    LLVMTypeRef[] types = new LLVMTypeRef[] {vftSearchListType, pureRTTIType};
    LLVMTypeRef instanceRTTIType = LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);

    LLVMValueRef global = LLVM.LLVMAddGlobal(module, instanceRTTIType, mangledName);
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMLinkOnceODRLinkage);
    LLVM.LLVMSetVisibility(global, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    LLVM.LLVMSetGlobalConstant(global, true);

    LLVMValueRef vftSearchList;
    if (type instanceof NamedType)
    {
      TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        vftSearchList = virtualFunctionHandler.getVFTSearchList(typeDefinition);
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        vftSearchList = virtualFunctionHandler.getObjectVFTSearchList(type, virtualFunctionHandler.getBaseChangeObjectVFT(type));
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        throw new IllegalArgumentException("Interfaces do not have instance RTTI, as they cannot be instantiated");
      }
      else
      {
        throw new IllegalArgumentException("Cannot find RTTI for unknown NamedType: " + type);
      }
    }
    else if (type instanceof ObjectType)
    {
      vftSearchList = virtualFunctionHandler.getObjectVFTSearchList(type, virtualFunctionHandler.getObjectVFTGlobal());
    }
    else
    {
      vftSearchList = virtualFunctionHandler.getObjectVFTSearchList(type, virtualFunctionHandler.getBaseChangeObjectVFT(type));
    }
    vftSearchList = LLVM.LLVMConstBitCast(vftSearchList, LLVM.LLVMPointerType(virtualFunctionHandler.getVFTSearchListType(), 0));
    LLVMValueRef pureRTTIStruct = getPureRTTIStruct(type);
    LLVMValueRef[] subValues = new LLVMValueRef[] {vftSearchList, pureRTTIStruct};
    LLVMValueRef instanceRTTI = LLVM.LLVMConstStruct(C.toNativePointerArray(subValues, false, true), subValues.length, false);

    LLVM.LLVMSetInitializer(global, instanceRTTI);

    return LLVM.LLVMConstBitCast(global, getGenericInstanceRTTIType());
  }

  /**
   * Finds the pure RTTI struct for the specified Type
   * @param type - the Type to find the RTTI for
   * @return the RTTI for the specified type
   */
  private LLVMValueRef getPureRTTIStruct(Type type)
  {
    LLVMValueRef size = findTypeSize(type);

    if (type instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) type;
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), ARRAY_SORT_ID, false);
      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isNullable() ? 1 : 0, false);
      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isContextuallyImmutable() ? 1 : 0, false);
      LLVMValueRef baseType = getPureRTTI(arrayType.getBaseType());
      LLVMValueRef[] values = new LLVMValueRef[] {sortId, size, nullable, immutable, baseType};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) type;
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), FUNCTION_SORT_ID, false);
      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isNullable() ? 1 : 0, false);
      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isImmutable() ? 1 : 0, false);
      LLVMValueRef returnType = getPureRTTI(functionType.getReturnType());
      Type[] parameterTypes = functionType.getParameterTypes();
      LLVMValueRef numParameters = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), parameterTypes.length, false);
      LLVMValueRef[] parameterRTTIs = new LLVMValueRef[parameterTypes.length];
      for (int i = 0; i < parameterTypes.length; ++i)
      {
        parameterRTTIs[i] = getPureRTTI(parameterTypes[i]);
      }
      LLVMValueRef parameterArray = LLVM.LLVMConstArray(getGenericPureRTTIType(), C.toNativePointerArray(parameterRTTIs, false, true), parameterRTTIs.length);
      LLVMValueRef[] values = new LLVMValueRef[] {sortId, size, nullable, immutable, returnType, numParameters, parameterArray};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;

      TypeParameter typeParameter = namedType.getResolvedTypeParameter();
      if (typeParameter != null)
      {
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
        LLVMValueRef[] values = new LLVMValueRef[] {sortId, size, nullable, immutable, indexValue};
        return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
      }

      TypeDefinition typeDefinition = namedType.getResolvedTypeDefinition();
      LLVMValueRef sortId;
      if (typeDefinition instanceof ClassDefinition)
      {
        sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), CLASS_SORT_ID, false);
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), COMPOUND_SORT_ID, false);
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), INTERFACE_SORT_ID, false);
      }
      else
      {
        throw new IllegalArgumentException("Cannot find run-time type information for the unknown named type: " + type);
      }
      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isNullable() ? 1 : 0, false);
      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isContextuallyImmutable() ? 1 : 0, false);
      LLVMValueRef qualifiedName = codeGenerator.addStringConstant(typeDefinition.getQualifiedName().toString());
      qualifiedName = LLVM.LLVMConstBitCast(qualifiedName, typeHelper.findRawStringType());

      Type[] typeArguments = namedType.getTypeArguments();
      LLVMValueRef[] typeArgumentValues = new LLVMValueRef[typeArguments == null ? 0 : typeArguments.length];
      LLVMValueRef numTypeArguments = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), typeArgumentValues.length, false);
      for (int i = 0; i < typeArgumentValues.length; ++i)
      {
        typeArgumentValues[i] = getPureRTTI(typeArguments[i]);
      }
      LLVMValueRef typeArgumentsArray = LLVM.LLVMConstArray(getGenericPureRTTIType(), C.toNativePointerArray(typeArgumentValues, false, true), typeArgumentValues.length);

      LLVMValueRef[] typeArgumentMapperValues = new LLVMValueRef[] {numTypeArguments, typeArgumentsArray};
      LLVMValueRef typeArgumentMapper = LLVM.LLVMConstStruct(C.toNativePointerArray(typeArgumentMapperValues, false, true), typeArgumentMapperValues.length, false);

      LLVMValueRef[] values = new LLVMValueRef[] {sortId, size, nullable, immutable, qualifiedName, typeArgumentMapper};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof NullType)
    {
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), NULL_SORT_ID, false);
      LLVMValueRef[] values = new LLVMValueRef[] {sortId, size};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof ObjectType)
    {
      ObjectType objectType = (ObjectType) type;
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), OBJECT_SORT_ID, false);
      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), objectType.isNullable() ? 1 : 0, false);
      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), objectType.isContextuallyImmutable() ? 1 : 0, false);
      LLVMValueRef[] values = new LLVMValueRef[] {sortId, size, nullable, immutable};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof PrimitiveType)
    {
      PrimitiveType primitiveType = (PrimitiveType) type;
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), PRIMITIVE_SORT_ID, false);
      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), primitiveType.isNullable() ? 1 : 0, false);
      LLVMValueRef primitiveId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), primitiveType.getPrimitiveTypeType().getRunTimeId(), false);
      LLVMValueRef[] values = new LLVMValueRef[] {sortId, size, nullable, primitiveId};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), TUPLE_SORT_ID, false);
      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), tupleType.isNullable() ? 1 : 0, false);
      Type[] subTypes = tupleType.getSubTypes();
      LLVMValueRef numSubTypes = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), subTypes.length, false);
      LLVMValueRef[] subTypeRTTIs = new LLVMValueRef[subTypes.length];
      for (int i = 0; i < subTypes.length; ++i)
      {
        subTypeRTTIs[i] = getPureRTTI(subTypes[i]);
      }
      LLVMValueRef subTypeArray = LLVM.LLVMConstArray(getGenericPureRTTIType(), C.toNativePointerArray(subTypeRTTIs, false, true), subTypeRTTIs.length);
      LLVMValueRef[] values = new LLVMValueRef[] {sortId, size, nullable, numSubTypes, subTypeArray};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    if (type instanceof VoidType)
    {
      LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), VOID_SORT_ID, false);
      LLVMValueRef[] values = new LLVMValueRef[] {sortId, size};
      return LLVM.LLVMConstStruct(C.toNativePointerArray(values, false, true), values.length, false);
    }
    throw new IllegalArgumentException("Cannot create a run-time type information struct for the unknown type: " + type);
  }

  /**
   * Finds the type of a pure RTTI struct for the specified type.
   * @param type - the type to find the RTTI struct type for
   * @return the RTTI struct type for the specified type
   */
  public LLVMTypeRef getPureRTTIStructType(Type type)
  {
    LLVMTypeRef sortIdType = LLVM.LLVMInt8Type();
    LLVMTypeRef sizeType = LLVM.LLVMInt32Type();
    if (type instanceof ArrayType)
    {
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef immutableType = LLVM.LLVMInt1Type();
      LLVMTypeRef baseTypeType = getGenericPureRTTIType();
      LLVMTypeRef[] types = new LLVMTypeRef[] {sortIdType, sizeType, nullableType, immutableType, baseTypeType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof FunctionType)
    {
      Type[] parameterTypes = ((FunctionType) type).getParameterTypes();
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef immutableType = LLVM.LLVMInt1Type();
      LLVMTypeRef returnTypeType = getGenericPureRTTIType();
      LLVMTypeRef numParametersType = LLVM.LLVMInt32Type();
      LLVMTypeRef parameterArrayType = LLVM.LLVMArrayType(getGenericPureRTTIType(), parameterTypes.length);
      LLVMTypeRef[] types = new LLVMTypeRef[] {sortIdType, sizeType, nullableType, immutableType, returnTypeType, numParametersType, parameterArrayType};
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
        LLVMTypeRef[] types = new LLVMTypeRef[] {sortIdType, sizeType, nullableType, immutableType, indexType};
        return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
      }
      LLVMTypeRef qualifiedNameType = typeHelper.findRawStringType();

      LLVMTypeRef numTypeArgumentsType = LLVM.LLVMInt32Type();
      LLVMTypeRef typeArgumentType = getGenericPureRTTIType();
      int numTypeArguments = namedType.getTypeArguments() == null ? 0 : namedType.getTypeArguments().length;
      LLVMTypeRef typeArgumentArrayType = LLVM.LLVMArrayType(typeArgumentType, numTypeArguments);

      LLVMTypeRef[] typeArgumentMapperTypes = new LLVMTypeRef[] {numTypeArgumentsType, typeArgumentArrayType};
      LLVMTypeRef typeArgumentMapperType = LLVM.LLVMStructType(C.toNativePointerArray(typeArgumentMapperTypes, false, true), typeArgumentMapperTypes.length, false);

      LLVMTypeRef[] types = new LLVMTypeRef[] {sortIdType, sizeType, nullableType, immutableType, qualifiedNameType, typeArgumentMapperType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof NullType)
    {
      LLVMTypeRef[] types = new LLVMTypeRef[] {sortIdType, sizeType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof ObjectType)
    {
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef immutableType = LLVM.LLVMInt1Type();
      LLVMTypeRef[] types = new LLVMTypeRef[] {sortIdType, sizeType, nullableType, immutableType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof PrimitiveType)
    {
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef primitiveIdType = LLVM.LLVMInt8Type();
      LLVMTypeRef[] types = new LLVMTypeRef[] {sortIdType, sizeType, nullableType, primitiveIdType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef numParametersType = LLVM.LLVMInt32Type();
      LLVMTypeRef parameterArrayType = LLVM.LLVMArrayType(getGenericPureRTTIType(), tupleType.getSubTypes().length);
      LLVMTypeRef[] types = new LLVMTypeRef[] {sortIdType, sizeType, nullableType, numParametersType, parameterArrayType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    if (type instanceof VoidType)
    {
      LLVMTypeRef[] types = new LLVMTypeRef[] {sortIdType, sizeType};
      return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    }
    throw new IllegalArgumentException("Cannot find a run-time type information struct type for the unknown type: " + type);
  }

  /**
   * @return the type of a pointer to a generic instance-RTTI struct
   */
  public LLVMTypeRef getGenericInstanceRTTIType()
  {
    LLVMTypeRef vftSearchListType = LLVM.LLVMPointerType(virtualFunctionHandler.getVFTSearchListType(), 0);

    LLVMTypeRef sortIdType = LLVM.LLVMInt8Type();
    LLVMTypeRef sizeType = LLVM.LLVMInt32Type();
    LLVMTypeRef[] pureTypes = new LLVMTypeRef[] {sortIdType, sizeType};
    LLVMTypeRef pureStructType = LLVM.LLVMStructType(C.toNativePointerArray(pureTypes, false, true), pureTypes.length, false);
    LLVMTypeRef[] types = new LLVMTypeRef[] {vftSearchListType, pureStructType};
    LLVMTypeRef structType = LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    return LLVM.LLVMPointerType(structType, 0);
  }

  /**
   * @return the type of a pointer to a generic pure-RTTI struct
   */
  public LLVMTypeRef getGenericPureRTTIType()
  {
    LLVMTypeRef sortIdType = LLVM.LLVMInt8Type();
    LLVMTypeRef sizeType = LLVM.LLVMInt32Type();
    LLVMTypeRef[] types = new LLVMTypeRef[] {sortIdType, sizeType};
    LLVMTypeRef structType = LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
    return LLVM.LLVMPointerType(structType, 0);
  }

  /**
   * @return the type of a generic type argument mapper struct (with an unspecified number of type arguments)
   */
  public LLVMTypeRef getGenericTypeArgumentMapperStructureType()
  {
    LLVMTypeRef numArgumentsType = LLVM.LLVMInt32Type();
    LLVMTypeRef argumentsType = LLVM.LLVMArrayType(getGenericPureRTTIType(), 0);
    LLVMTypeRef[] types = new LLVMTypeRef[] {numArgumentsType, argumentsType};
    return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
  }

  /**
   * @return the type of a pointer to a generic type argument mapper struct (with an unspecified number of type arguments)
   */
  public LLVMTypeRef getGenericTypeArgumentMapperType()
  {
    return LLVM.LLVMPointerType(getGenericTypeArgumentMapperStructureType(), 0);
  }

  /**
   * Finds the size of the specified type's standard native representation.
   * @param type - the type to find the size of
   * @return the size of the specified type's standard representation, as a 32 bit integer value
   */
  private LLVMValueRef findTypeSize(Type type)
  {
    if (type instanceof VoidType)
    {
      // void types have zero size, but sometimes need RTTI blocks (e.g. for function return types)
      return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false);
    }
    LLVMTypeRef llvmType = typeHelper.findStandardType(type);
    LLVMTypeRef arrayType = LLVM.LLVMPointerType(LLVM.LLVMArrayType(llvmType, 0), 0);
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false)};
    LLVMValueRef pointer = LLVM.LLVMConstGEP(LLVM.LLVMConstNull(arrayType), C.toNativePointerArray(indices, false, true), indices.length);
    return LLVM.LLVMConstPtrToInt(pointer, LLVM.LLVMInt32Type());
  }

  /**
   * Builds a check that determines whether the specified RTTI block represents a nullable type.
   * @param builder - the builder to build code with
   * @param pureRTTI - the pure RTTI to check the nullability of
   * @return an i1 which is true if the specified pure RTTI is nullable, and false otherwise
   */
  public LLVMValueRef buildTypeIsNullableCheck(LLVMBuilderRef builder, LLVMValueRef pureRTTI)
  {
    LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "typeIsNullableContinuation");
    LLVMBasicBlockRef checkBlock = LLVM.LLVMAddBasicBlock(builder, "typeIsNullableCheck");

    LLVMValueRef sortIdPtr = LLVM.LLVMBuildStructGEP(builder, pureRTTI, 0, "");
    LLVMValueRef sortId = LLVM.LLVMBuildLoad(builder, sortIdPtr, "");

    LLVMValueRef isVoidType = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, sortId, LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), VOID_SORT_ID, false), "");
    LLVMValueRef isNullType = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, sortId, LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), NULL_SORT_ID, false), "");
    LLVMValueRef condition = LLVM.LLVMBuildOr(builder, isVoidType, isNullType, "");
    LLVM.LLVMBuildCondBr(builder, condition, continuationBlock, checkBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, checkBlock);
    LLVMTypeRef[] subTypes = new LLVMTypeRef[] {LLVM.LLVMInt8Type(), LLVM.LLVMInt32Type(), LLVM.LLVMInt1Type()};
    LLVMTypeRef rttiWithNullabilityType = LLVM.LLVMStructType(C.toNativePointerArray(subTypes, false, true), subTypes.length, false);
    LLVMValueRef rttiWithNullability = LLVM.LLVMBuildBitCast(builder, pureRTTI, LLVM.LLVMPointerType(rttiWithNullabilityType, 0), "");

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
   * Builds an instanceof check, to check whether the specified value is an instance of the specified checkType.
   * This method assumes that the value and the expression type are not nullable. However, null instanceof &lt;anything&gt; should always return false.
   * @param builder - the LLVMBuilderRef to build the check with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the not-null value to check, in a temporary type representation
   * @param expressionType - the type of the value, which must not be nullable
   * @param checkType - the type to check the RTTI against (note: nullability and data-immutability on the top level of this type are ignored)
   * @param typeParameterAccessor - the TypeParameterAccessor to use to access the RTTI blocks of any TypeParameters that might be used in checkType
   * @return an LLVMValueRef representing an i1 (boolean), which will be true iff value is an instance of checkType
   */
  public LLVMValueRef buildInstanceOfCheck(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef value, Type expressionType, Type checkType, TypeParameterAccessor typeParameterAccessor)
  {
    checkType = Type.findTypeWithoutModifiers(checkType);
    if (checkType instanceof ObjectType)
    {
      // catch-all: any not-null type instanceof object is always true
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
    }

    if (checkType instanceof NamedType && ((NamedType) checkType).getResolvedTypeParameter() != null)
    {
      TypeParameter typeParameter = ((NamedType) checkType).getResolvedTypeParameter();
      LLVMValueRef checkRTTI = typeParameterAccessor.findTypeParameterRTTI(typeParameter);

      // we are checking whether something is a type parameter
      if (expressionType instanceof NamedType && (((NamedType) expressionType).getResolvedTypeDefinition() instanceof ClassDefinition ||
                                                  ((NamedType) expressionType).getResolvedTypeDefinition() instanceof InterfaceDefinition))
      {
        // we have a class/interface type, and we need to check whether its run-time value is of the type parameter's type
        // so we need to do a search through the object's VFT search list for the type parameter's RTTI

        // search through the object's VFT search list
        LLVMValueRef objectValue = value;
        if (expressionType instanceof NamedType && ((NamedType) expressionType).getResolvedTypeDefinition() instanceof InterfaceDefinition)
        {
          objectValue = typeHelper.convertTemporary(builder, landingPadContainer, value, expressionType, new ObjectType(false, false, null), false, typeParameterAccessor);
        }
        LLVMValueRef vftPointer = virtualFunctionHandler.lookupInstanceVFT(builder, objectValue, checkRTTI);
        return LLVM.LLVMBuildIsNotNull(builder, vftPointer, "");
      }
      if (expressionType instanceof ObjectType ||
          expressionType instanceof NamedType && ((NamedType) expressionType).getResolvedTypeParameter() != null)
      {
        // we are checking whether an arbitrary object is an instance of a type parameter
        // the problem here is that we do not know whether it represents a class/interface or some other type
        // i.e. whether we should just check the RTTI, or check through the whole of the VFT search list
        // so we must compare the check RTTI to both the object's RTTI and the object's VFT search list

        // check against the object's pure RTTI
        // (we ignore the type modifiers by passing ignoreTypeModifiers=true as the last argument)
        LLVMValueRef objectRTTI = lookupPureRTTI(builder, value);
        LLVMValueRef nullTypeMapper = LLVM.LLVMConstNull(getGenericTypeArgumentMapperType());
        LLVMValueRef[] checkArguments = new LLVMValueRef[] {objectRTTI, checkRTTI, nullTypeMapper, nullTypeMapper, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false)};
        LLVMValueRef comparisonResult = LLVM.LLVMBuildCall(builder, getIsTypeEquivalentFunction(), C.toNativePointerArray(checkArguments, false, true), checkArguments.length, "");

        // if comparisonResult turns out to be true, we can skip the VFT search
        LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "typeParameterInstanceOfContinue");
        LLVMBasicBlockRef checkBlock = LLVM.LLVMAddBasicBlock(builder, "typeParameterInstanceOfSearchVFTList");
        LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildCondBr(builder, comparisonResult, continuationBlock, checkBlock);

        // build the VFT search
        LLVM.LLVMPositionBuilderAtEnd(builder, checkBlock);
        LLVMValueRef objectValue = value;
        if (expressionType instanceof NamedType && ((NamedType) expressionType).getResolvedTypeDefinition() instanceof InterfaceDefinition)
        {
          objectValue = typeHelper.convertTemporary(builder, landingPadContainer, value, expressionType, new ObjectType(false, false, null), false, typeParameterAccessor);
        }
        LLVMValueRef vftPointer = virtualFunctionHandler.lookupInstanceVFT(builder, objectValue, checkRTTI);
        LLVMValueRef vftResult = LLVM.LLVMBuildIsNotNull(builder, vftPointer, "");
        LLVMBasicBlockRef endVFTSearchBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildBr(builder, continuationBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
        LLVMValueRef phi = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt1Type(), "");
        LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), vftResult};
        LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endVFTSearchBlock};
        LLVM.LLVMAddIncoming(phi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
        return phi;
      }

      // otherwise, the expression type is known at compile time, so we can just build a normal type check for it
      return buildTypeInfoCheck(builder, checkRTTI, expressionType, false, typeParameterAccessor);
    }


    if (expressionType instanceof ArrayType)
    {
      if (checkType instanceof ArrayType)
      {
        return buildTypeEquivalenceCheck(builder, ((ArrayType) expressionType).getBaseType(), ((ArrayType) checkType).getBaseType(), false, typeParameterAccessor);
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
          LLVMValueRef equivalentSubTypes = buildTypeEquivalenceCheck(builder, expressionFunctionType.getReturnType(), checkFunctionType.getReturnType(), false, typeParameterAccessor);

          for (int i = 0; i < expressionParameterTypes.length; ++i)
          {
            LLVMValueRef paramsEquivalent = buildTypeEquivalenceCheck(builder, expressionParameterTypes[i], checkParameterTypes[i], false, typeParameterAccessor);
            equivalentSubTypes = LLVM.LLVMBuildAnd(builder, equivalentSubTypes, paramsEquivalent, "");
          }

          LLVMValueRef result = equivalentSubTypes;
          if (!expressionFunctionType.isImmutable() && checkFunctionType.isImmutable())
          {
            // the result depends on the immutability of the actual function in the value, so check its RTTI
            LLVMValueRef rttiPointer = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
            LLVMValueRef pureRTTIPointer = LLVM.LLVMBuildStructGEP(builder, rttiPointer, 1, "");
            pureRTTIPointer = LLVM.LLVMBuildBitCast(builder, pureRTTIPointer, LLVM.LLVMPointerType(getPureRTTIStructType(expressionFunctionType), 0), "");
            LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 3, "");
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
        Type[] checkArguments = checkNamedType.getTypeArguments();
        if ((expressionArguments == null) != (checkArguments == null) ||
            (expressionArguments != null && (expressionArguments.length != checkArguments.length)))
        {
          throw new IllegalArgumentException("Two references to the same compound type do not have the same number of type arguments");
        }

        LLVMValueRef result = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
        if (expressionArguments != null)
        {
          for (int i = 0; i < expressionArguments.length; ++i)
          {
            LLVMValueRef argsEquivalent = buildTypeEquivalenceCheck(builder, expressionArguments[i], checkArguments[i], false, typeParameterAccessor);
            result = LLVM.LLVMBuildAnd(builder, result, argsEquivalent, "");
          }
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
            LLVMValueRef subEquivalent = buildTypeEquivalenceCheck(builder, expressionSubTypes[i], checkSubTypes[i], false, typeParameterAccessor);
            result = LLVM.LLVMBuildAnd(builder, result, subEquivalent, "");
          }
          return result;
        }
      }
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
    }
    if (expressionType instanceof NamedType && ((NamedType) expressionType).getResolvedTypeDefinition() != null)
    {
      // note: expressionType cannot be a compound type, as they have already been handled
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
          objectValue = typeHelper.convertTemporary(builder, landingPadContainer, value, expressionType, new ObjectType(false, false, null), false, typeParameterAccessor);
        }
        // search through the object's VFT search list for a VFT for checkType
        // the object does not implement checkType iff the VFT pointer comes back as null
        LLVMValueRef vftPointer = virtualFunctionHandler.lookupInstanceVFT(builder, objectValue, checkNamedType, typeParameterAccessor);
        return LLVM.LLVMBuildIsNotNull(builder, vftPointer, "");
      }
      // return false
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
    }
    if (expressionType instanceof ObjectType ||
        (expressionType instanceof NamedType && ((NamedType) expressionType).getResolvedTypeParameter() != null))
    {
      if (checkType instanceof NamedType && (((NamedType) checkType).getResolvedTypeDefinition() instanceof ClassDefinition ||
                                             ((NamedType) checkType).getResolvedTypeDefinition() instanceof InterfaceDefinition))
      {
        // checkType is the sort of type which turns up in VFT search lists
        // so search through the object's VFT search list for a VFT for checkType
        // the object does not implement checkType iff the VFT pointer comes back as null
        LLVMValueRef vftPointer = virtualFunctionHandler.lookupInstanceVFT(builder, value, (NamedType) checkType, typeParameterAccessor);
        return LLVM.LLVMBuildIsNotNull(builder, vftPointer, "");
      }
      // we are checking against a non-pointer-type, so compare the type info to what we are asking about
      LLVMValueRef pureRTTIPointer = lookupPureRTTI(builder, value);
      return buildTypeInfoCheck(builder, pureRTTIPointer, checkType, false, typeParameterAccessor);
    }
    throw new IllegalArgumentException("Cannot build instanceof check from " + expressionType + " to " + checkType);
  }

  /**
   * Checks whether two types are equivalent in the current context, including accounting for type parameters.
   * @param builder - the LLVMBuilderRef to build code with
   * @param typeA - the first type
   * @param typeB - the second type
   * @param typeParameterAccessor - the TypeParameterAccessor to look up any type parameters in
   * @return an LLVMValueRef representing an i1, which is true iff the types are runtime-equivalent in this context
   */
  private LLVMValueRef buildTypeEquivalenceCheck(LLVMBuilderRef builder, Type typeA, Type typeB, boolean ignoreTypeModifiers, TypeParameterAccessor typeParameterAccessor)
  {
    if (typeA.isRuntimeEquivalent(typeB))
    {
      // return true
      return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false);
    }
    if ((typeA instanceof NamedType && ((NamedType) typeA).getResolvedTypeParameter() != null) ||
        (typeB instanceof NamedType && ((NamedType) typeB).getResolvedTypeParameter() != null))
    {
      // call plinth_is_type_equivalent on the two types, with the same mapper to fill in the type arguments
      LLVMValueRef rttiPointerA = getPureRTTI(typeA);
      LLVMValueRef rttiPointerB = getPureRTTI(typeB);
      LLVMValueRef isTypeEquivalentFunction = getIsTypeEquivalentFunction();
      LLVMValueRef typeArgumentMapper = typeParameterAccessor.getTypeArgumentMapper();

      LLVMValueRef[] arguments = new LLVMValueRef[] {rttiPointerA, rttiPointerB, typeArgumentMapper, typeArgumentMapper};
      LLVMValueRef result = LLVM.LLVMBuildCall(builder, isTypeEquivalentFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");
      return result;
    }

    if (typeA instanceof ArrayType && typeB instanceof ArrayType)
    {
      ArrayType arrayA = (ArrayType) typeA;
      ArrayType arrayB = (ArrayType) typeB;
      boolean typeModifiersMatch = ignoreTypeModifiers ||
                                   (arrayA.isNullable() == arrayB.isNullable() &&
                                    arrayA.isExplicitlyImmutable() == arrayB.isExplicitlyImmutable() &&
                                    arrayA.isContextuallyImmutable() == arrayB.isContextuallyImmutable());
      if (typeModifiersMatch)
      {
        return buildTypeEquivalenceCheck(builder, arrayA.getBaseType(), arrayB.getBaseType(), false, typeParameterAccessor);
      }
    }
    if (typeA instanceof FunctionType && typeB instanceof FunctionType)
    {
      FunctionType functionA = (FunctionType) typeA;
      FunctionType functionB = (FunctionType) typeB;
      Type[] paramTypesA = functionA.getParameterTypes();
      Type[] paramTypesB = functionB.getParameterTypes();
      boolean typeModifiersMatch = ignoreTypeModifiers || functionA.isNullable() == functionB.isNullable();
      if (typeModifiersMatch &&
          functionA.isImmutable() == functionB.isImmutable() &&
          paramTypesA.length == paramTypesB.length)
      {
        LLVMValueRef currentResult = buildTypeEquivalenceCheck(builder, functionA.getReturnType(), functionB.getReturnType(), false, typeParameterAccessor);
        for (int i = 0; i < paramTypesA.length; ++i)
        {
          LLVMValueRef newResult = buildTypeEquivalenceCheck(builder, paramTypesA[i], paramTypesB[i], false, typeParameterAccessor);
          currentResult = LLVM.LLVMBuildAnd(builder, currentResult, newResult, "");
        }
        return currentResult;
      }
    }
    if (typeA instanceof NamedType && typeB instanceof NamedType)
    {
      NamedType namedA = (NamedType) typeA;
      NamedType namedB = (NamedType) typeB;
      boolean typeModifiersMatch = ignoreTypeModifiers ||
                                   (namedA.isNullable() == namedB.isNullable() &&
                                    (namedA.getResolvedTypeDefinition().isImmutable() ||
                                      (namedA.isExplicitlyImmutable() == namedB.isExplicitlyImmutable() &&
                                       namedA.isContextuallyImmutable() == namedB.isContextuallyImmutable())));
      if (typeModifiersMatch &&
          namedA.getResolvedTypeDefinition() == namedB.getResolvedTypeDefinition() &&
          (namedA.getTypeArguments() == null) == (namedB.getTypeArguments() == null))
      {
        if (namedA.getTypeArguments() == null || namedA.getTypeArguments().length < 1)
        {
          throw new IllegalStateException("Two named types which seem to be runtime equivalent are not runtime equivalent!");
        }
        Type[] argumentsA = namedA.getTypeArguments();
        Type[] argumentsB = namedB.getTypeArguments();
        if (argumentsA.length == argumentsB.length)
        {
          LLVMValueRef currentResult = null;
          for (int i = 0; i < argumentsA.length; ++i)
          {
            LLVMValueRef newResult = buildTypeEquivalenceCheck(builder, argumentsA[i], argumentsB[i], false, typeParameterAccessor);
            if (currentResult == null)
            {
              currentResult = newResult;
            }
            else
            {
              currentResult = LLVM.LLVMBuildAnd(builder, currentResult, newResult, "");
            }
          }
          return currentResult;
        }
      }
    }
    if (typeA instanceof TupleType && typeB instanceof TupleType)
    {
      TupleType tupleA = (TupleType) typeA;
      TupleType tupleB = (TupleType) typeB;
      Type[] subTypesA = tupleA.getSubTypes();
      Type[] subTypesB = tupleB.getSubTypes();
      boolean typeModifiersMatch = ignoreTypeModifiers || tupleA.isNullable() == tupleB.isNullable();
      if (typeModifiersMatch &&
          subTypesA.length == subTypesB.length)
      {
        LLVMValueRef currentResult = null;
        for (int i = 0; i < subTypesA.length; ++i)
        {
          LLVMValueRef newResult = buildTypeEquivalenceCheck(builder, subTypesA[i], subTypesB[i], false, typeParameterAccessor);
          if (currentResult == null)
          {
            currentResult = newResult;
          }
          else
          {
            currentResult = LLVM.LLVMBuildAnd(builder, currentResult, newResult, "");
          }
        }
        return currentResult;
      }
    }
    // they are not equivalent
    // return false
    return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false);
  }

  /**
   * Builds a check as to whether the specified RTTI pointer represents the specified checkType.
   * @param builder - the LLVMBuilderRef to build the check with
   * @param pureRTTIPointer - the pointer to the RTTI to check
   * @param checkType - the Type to check against
   * @param checkTypeModifiers - true for the top-level type modifiers (nullability and immutability) should be checked, false if they should only be checked on nested types
   *                             NOTE: if checkType is a type parameter, this is always taken to be true, even if false is passed in
   * @param typeParameterAccessor - the TypeParameterAccessor to look up the type parameters of checkType in
   * @return a LLVMValueRef representing an i1 (boolean), which will be true iff the RTTI pointer represents the checkType
   */
  private LLVMValueRef buildTypeInfoCheck(LLVMBuilderRef builder, LLVMValueRef pureRTTIPointer, Type checkType, boolean checkTypeModifiers, TypeParameterAccessor typeParameterAccessor)
  {
    if (checkType instanceof NamedType && ((NamedType) checkType).getResolvedTypeParameter() != null)
    {
      // call plinth_is_type_equivalent with the type parameter's actual value
      LLVMValueRef isTypeEquivalentFunction = getIsTypeEquivalentFunction();
      LLVMValueRef checkRTTI = getPureRTTI(checkType);
      LLVMValueRef checkTypeArgumentMapper = typeParameterAccessor.getTypeArgumentMapper();
      LLVMValueRef nullMapper = LLVM.LLVMConstNull(getGenericTypeArgumentMapperType());

      LLVMValueRef[] arguments = new LLVMValueRef[] {pureRTTIPointer, checkRTTI, nullMapper, checkTypeArgumentMapper};
      LLVMValueRef result = LLVM.LLVMBuildCall(builder, isTypeEquivalentFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");
      return result;
    }

    byte sortId = getSortId(checkType);
    LLVMValueRef sortIdPointer = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 0, "");
    LLVMValueRef sortIdValue = LLVM.LLVMBuildLoad(builder, sortIdPointer, "");
    LLVMValueRef sortEqual = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, sortIdValue, LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), sortId, false), "");

    LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoContinue");
    LLVMBasicBlockRef checkBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoCheck");
    LLVM.LLVMBuildCondBr(builder, sortEqual, checkBlock, continuationBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, checkBlock);
    LLVMValueRef castedRTTIPointer = LLVM.LLVMBuildBitCast(builder, pureRTTIPointer, LLVM.LLVMPointerType(getPureRTTIStructType(checkType), 0), "");
    LLVMValueRef resultValue;
    LLVMBasicBlockRef endResultBlock;
    if (checkType instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) checkType;
      if (checkTypeModifiers)
      {
        LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
        LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
        LLVMValueRef nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isNullable() ? 1 : 0, false), "");
        LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
        LLVMValueRef immutabilityValue = LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
        LLVMValueRef immutabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, immutabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isContextuallyImmutable() ? 1 : 0, false), "");
        resultValue = LLVM.LLVMBuildAnd(builder, nullabilityMatches, immutabilityMatches, "");
      }
      else
      {
        resultValue = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
      }
      LLVMValueRef baseTypePointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 4, "");
      LLVMValueRef baseTypeValue = LLVM.LLVMBuildLoad(builder, baseTypePointer, "");
      LLVMValueRef baseTypeMatches = buildTypeInfoCheck(builder, baseTypeValue, arrayType.getBaseType(), true, typeParameterAccessor);
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
      if (checkTypeModifiers)
      {
        LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
        LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
        nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isNullable() ? 1 : 0, false), "");
      }
      else
      {
        nullabilityMatches = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
      }
      LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
      LLVMValueRef immutabilityValue = LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
      LLVMValueRef immutabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, immutabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isImmutable() ? 1 : 0, false), "");
      LLVMValueRef returnTypePointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 4, "");
      LLVMValueRef returnTypeValue = LLVM.LLVMBuildLoad(builder, returnTypePointer, "");
      LLVMValueRef returnTypeMatches = buildTypeInfoCheck(builder, returnTypeValue, functionType.getReturnType(), true, typeParameterAccessor);
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
          LLVMValueRef paramTypeMatches = buildTypeInfoCheck(builder, paramType, parameterTypes[i], true, typeParameterAccessor);
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
      if (checkTypeModifiers)
      {
        LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
        LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
        LLVMValueRef nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isNullable() ? 1 : 0, false), "");
        LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
        LLVMValueRef immutabilityValue = LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
        LLVMValueRef immutabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, immutabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isContextuallyImmutable() ? 1 : 0, false), "");
        headerMatches = LLVM.LLVMBuildAnd(builder, nullabilityMatches, immutabilityMatches, "");
      }
      else
      {
        headerMatches = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
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
          LLVMValueRef matches = buildTypeInfoCheck(builder, typeInfo, typeArguments[i], true, typeParameterAccessor);
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
      if (checkTypeModifiers)
      {
        LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
        LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
        LLVMValueRef nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), objectType.isNullable() ? 1 : 0, false), "");
        LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
        LLVMValueRef immutabilityValue = LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
        LLVMValueRef immutabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, immutabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), objectType.isContextuallyImmutable() ? 1 : 0, false), "");
        resultValue = LLVM.LLVMBuildAnd(builder, nullabilityMatches, immutabilityMatches, "");
      }
      else
      {
        resultValue = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
      }

      endResultBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);
    }
    else if (checkType instanceof PrimitiveType)
    {
      PrimitiveType primitiveType = (PrimitiveType) checkType;
      LLVMValueRef nullabilityMatches;
      if (checkTypeModifiers)
      {
        LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
        LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
        nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), primitiveType.isNullable() ? 1 : 0, false), "");
      }
      else
      {
        nullabilityMatches = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
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
      if (checkTypeModifiers)
      {
        LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
        LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
        nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), tupleType.isNullable() ? 1 : 0, false), "");
      }
      else
      {
        nullabilityMatches = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
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
        LLVMValueRef subTypeMatches = buildTypeInfoCheck(builder, subType, subTypes[i], true, typeParameterAccessor);
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
   * @param instanceRTTI - true to generate instance RTTI, false to generate pure RTTI
   * @param type - the type to generate the RTTI block for
   * @param typeParameterAccessor - an accessor for the values of any type parameters referenced inside type
   * @return the RTTI block created, in a standard pointer representation
   */
  public LLVMValueRef buildRTTICreation(LLVMBuilderRef builder, boolean instanceRTTI, Type type, TypeParameterAccessor typeParameterAccessor)
  {
    if (!containsTypeParameters(type) || typeParameterAccessor == null)
    {
      if (instanceRTTI)
      {
        return getInstanceRTTI(type);
      }
      return getPureRTTI(type);
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
      if (instanceRTTI)
      {
        throw new IllegalArgumentException("Cannot create instance RTTI for a type parameter");
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

    LLVMTypeRef nativeType = getPureRTTIStructType(type);
    if (instanceRTTI)
    {
      // instance RTTI has a VFT search list tupled onto the beginning
      LLVMTypeRef vftSearchListType = LLVM.LLVMPointerType(virtualFunctionHandler.getVFTSearchListType(), 0);
      LLVMTypeRef[] subTypes = new LLVMTypeRef[] {vftSearchListType, nativeType};
      nativeType = LLVM.LLVMStructType(C.toNativePointerArray(subTypes, false, true), subTypes.length, false);
    }
    nativeType = LLVM.LLVMPointerType(nativeType, 0);

    // find a constant reference to the VFT search list
    LLVMValueRef vftSearchList = null;
    if (instanceRTTI)
    {
      if (type instanceof NamedType)
      {
        TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
        if (typeDefinition instanceof ClassDefinition)
        {
          vftSearchList = virtualFunctionHandler.getVFTSearchList(typeDefinition);
        }
        else if (typeDefinition instanceof CompoundDefinition)
        {
          vftSearchList = virtualFunctionHandler.getObjectVFTSearchList(type, virtualFunctionHandler.getBaseChangeObjectVFT(type));
        }
        else if (typeDefinition instanceof InterfaceDefinition)
        {
          throw new IllegalArgumentException("Interfaces do not have instance RTTI, as they cannot be instantiated");
        }
        else
        {
          throw new IllegalArgumentException("Cannot find RTTI for unknown NamedType: " + type);
        }
      }
      else if (type instanceof ObjectType)
      {
        vftSearchList = virtualFunctionHandler.getObjectVFTSearchList(type, virtualFunctionHandler.getObjectVFTGlobal());
      }
      else
      {
        vftSearchList = virtualFunctionHandler.getObjectVFTSearchList(type, virtualFunctionHandler.getBaseChangeObjectVFT(type));
      }
      vftSearchList = LLVM.LLVMConstBitCast(vftSearchList, LLVM.LLVMPointerType(virtualFunctionHandler.getVFTSearchListType(), 0));
    }

    // allocate the RTTI block
    LLVMValueRef pointer = codeGenerator.buildHeapAllocation(builder, nativeType);

    LLVMValueRef pureRTTIPointer = pointer;
    if (instanceRTTI)
    {
      LLVMValueRef vftSearchListPointer = LLVM.LLVMBuildStructGEP(builder, pointer, 0, "");
      LLVM.LLVMBuildStore(builder, vftSearchList, vftSearchListPointer);

      pureRTTIPointer = LLVM.LLVMBuildStructGEP(builder, pointer, 1, "");
    }

    // now just fill in the pure RTTI
    LLVMValueRef sortId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), getSortId(type), false);
    LLVMValueRef sortIdPtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 0, "");
    LLVM.LLVMBuildStore(builder, sortId, sortIdPtr);

    LLVMValueRef size = findTypeSize(type);
    LLVMValueRef sizePtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 1, "");
    LLVM.LLVMBuildStore(builder, size, sizePtr);

    if (type instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) type;

      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isNullable() ? 1 : 0, false);
      LLVMValueRef nullablePtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 2, "");
      LLVM.LLVMBuildStore(builder, nullable, nullablePtr);

      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isContextuallyImmutable() ? 1 : 0, false);
      LLVMValueRef immutablePtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 3, "");
      LLVM.LLVMBuildStore(builder, immutable, immutablePtr);

      LLVMValueRef baseTypeRTTI = buildRTTICreation(builder, false, arrayType.getBaseType(), typeParameterAccessor);
      LLVMValueRef baseTypeRTTIPtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 4, "");
      LLVM.LLVMBuildStore(builder, baseTypeRTTI, baseTypeRTTIPtr);
    }
    else if (type instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) type;

      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isNullable() ? 1 : 0, false);
      LLVMValueRef nullablePtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 2, "");
      LLVM.LLVMBuildStore(builder, nullable, nullablePtr);

      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isImmutable() ? 1 : 0, false);
      LLVMValueRef immutablePtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 3, "");
      LLVM.LLVMBuildStore(builder, immutable, immutablePtr);

      LLVMValueRef returnTypeRTTI = buildRTTICreation(builder, false, functionType.getReturnType(), typeParameterAccessor);
      LLVMValueRef returnTypeRTTIPtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 4, "");
      LLVM.LLVMBuildStore(builder, returnTypeRTTI, returnTypeRTTIPtr);

      Type[] parameterTypes = functionType.getParameterTypes();
      LLVMValueRef numParams = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), parameterTypes.length, false);
      LLVMValueRef numParamsPtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 5, "");
      LLVM.LLVMBuildStore(builder, numParams, numParamsPtr);

      for (int i = 0; i < parameterTypes.length; ++i)
      {
        LLVMValueRef parameterRTTI = buildRTTICreation(builder, false, parameterTypes[i], typeParameterAccessor);
        LLVMValueRef[] parameterIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                              LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 6, false),
                                                              LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), i, false)};
        LLVMValueRef parameterRTTIPtr = LLVM.LLVMBuildGEP(builder, pureRTTIPointer, C.toNativePointerArray(parameterIndices, false, true), parameterIndices.length, "");
        LLVM.LLVMBuildStore(builder, parameterRTTI, parameterRTTIPtr);
      }
    }
    else if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;

      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isNullable() ? 1 : 0, false);
      LLVMValueRef nullablePtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 2, "");
      LLVM.LLVMBuildStore(builder, nullable, nullablePtr);

      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isContextuallyImmutable() ? 1 : 0, false);
      LLVMValueRef immutablePtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 3, "");
      LLVM.LLVMBuildStore(builder, immutable, immutablePtr);

      LLVMValueRef qualifiedName = codeGenerator.addStringConstant(namedType.getResolvedTypeDefinition().getQualifiedName().toString());
      qualifiedName = LLVM.LLVMConstBitCast(qualifiedName, typeHelper.findRawStringType());
      LLVMValueRef qualifiedNamePtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 4, "");
      LLVM.LLVMBuildStore(builder, qualifiedName, qualifiedNamePtr);

      Type[] typeArguments = namedType.getTypeArguments();
      LLVMValueRef[] numTypeArgumentsIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                                   LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false),
                                                                   LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false)};
      LLVMValueRef numTypeArguments = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), typeArguments.length, false);
      LLVMValueRef numTypeArgumentsPtr = LLVM.LLVMBuildGEP(builder, pureRTTIPointer, C.toNativePointerArray(numTypeArgumentsIndices, false, true), numTypeArgumentsIndices.length, "");
      LLVM.LLVMBuildStore(builder, numTypeArguments, numTypeArgumentsPtr);

      for (int i = 0; i < typeArguments.length; ++i)
      {
        LLVMValueRef argumentRTTI = buildRTTICreation(builder, false, typeArguments[i], typeParameterAccessor);
        LLVMValueRef[] argumentIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                             LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false),
                                                             LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false),
                                                             LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), i, false)};
        LLVMValueRef argumentRTTIPtr = LLVM.LLVMBuildGEP(builder, pureRTTIPointer, C.toNativePointerArray(argumentIndices, false, true), argumentIndices.length, "");
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
      LLVMValueRef nullablePtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 2, "");
      LLVM.LLVMBuildStore(builder, nullable, nullablePtr);

      LLVMValueRef immutable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), objectType.isContextuallyImmutable() ? 1 : 0, false);
      LLVMValueRef immutablePtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 3, "");
      LLVM.LLVMBuildStore(builder, immutable, immutablePtr);
    }
    else if (type instanceof PrimitiveType)
    {
      PrimitiveType primitiveType = (PrimitiveType) type;

      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), primitiveType.isNullable() ? 1 : 0, false);
      LLVMValueRef nullablePtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 2, "");
      LLVM.LLVMBuildStore(builder, nullable, nullablePtr);

      LLVMValueRef primitiveId = LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), primitiveType.getPrimitiveTypeType().getRunTimeId(), false);
      LLVMValueRef primitiveIdPtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 3, "");
      LLVM.LLVMBuildStore(builder, primitiveId, primitiveIdPtr);
    }
    else if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;

      LLVMValueRef nullable = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), tupleType.isNullable() ? 1 : 0, false);
      LLVMValueRef nullablePtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 2, "");
      LLVM.LLVMBuildStore(builder, nullable, nullablePtr);

      Type[] subTypes = tupleType.getSubTypes();
      LLVMValueRef numSubTypes = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), subTypes.length, false);
      LLVMValueRef numSubTypesPtr = LLVM.LLVMBuildStructGEP(builder, pureRTTIPointer, 3, "");
      LLVM.LLVMBuildStore(builder, numSubTypes, numSubTypesPtr);

      for (int i = 0; i < subTypes.length; ++i)
      {
        LLVMValueRef subTypeRTTI = buildRTTICreation(builder, false, subTypes[i], typeParameterAccessor);
        LLVMValueRef[] subTypeIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                            LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 4, false),
                                                            LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), i, false)};
        LLVMValueRef subTypeRTTIPtr = LLVM.LLVMBuildGEP(builder, pureRTTIPointer, C.toNativePointerArray(subTypeIndices, false, true), subTypeIndices.length, "");
        LLVM.LLVMBuildStore(builder, subTypeRTTI, subTypeRTTIPtr);
      }
    }
    else if (type instanceof VoidType)
    {
      // nothing to fill in
    }
    else
    {
      throw new IllegalArgumentException("Unknown type: " + type);
    }

    if (instanceRTTI)
    {
      return LLVM.LLVMBuildBitCast(builder, pointer, getGenericInstanceRTTIType(), "");
    }
    return LLVM.LLVMBuildBitCast(builder, pointer, getGenericPureRTTIType(), "");
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

    LLVMTypeRef[] types = new LLVMTypeRef[] {getGenericPureRTTIType(), LLVM.LLVMInt1Type(), LLVM.LLVMInt1Type(), LLVM.LLVMInt1Type()};
    LLVMTypeRef resultType = getGenericPureRTTIType();
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);
    return LLVM.LLVMAddFunction(module, FORCE_TYPE_MODIFIERS_FUNCTION_NAME, functionType);
  }

  /**
   * @return an LLVM function representing the 'plinth_is_type_equivalent' function, which checks whether two RTTI blocks are equivalent
   */
  private LLVMValueRef getIsTypeEquivalentFunction()
  {
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, IS_TYPE_EQUIVALENT_FUNCTION_NAME);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    LLVMTypeRef pureRTTIType = getGenericPureRTTIType();
    LLVMTypeRef mapperType = getGenericTypeArgumentMapperType();
    LLVMTypeRef ignoreTypeModifersType = LLVM.LLVMInt1Type();
    LLVMTypeRef[] types = new LLVMTypeRef[] {pureRTTIType, pureRTTIType, mapperType, mapperType, ignoreTypeModifersType};
    LLVMTypeRef resultType = LLVM.LLVMInt1Type();
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);
    return LLVM.LLVMAddFunction(module, IS_TYPE_EQUIVALENT_FUNCTION_NAME, functionType);
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
    if (type instanceof NullType || type instanceof ObjectType || type instanceof PrimitiveType || type instanceof VoidType)
    {
      return false;
    }
    throw new IllegalArgumentException("Unknown type: " + type);
  }
}
