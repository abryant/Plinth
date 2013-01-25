package eu.bryants.anthony.plinth.compiler.passes.llvm;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMBuilderRef;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMTypeRef;
import nativelib.llvm.LLVM.LLVMValueRef;
import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 23 Jan 2013
 */

/**
 * This Run-Time Type Information Helper provides methods for generating and retrieving run-time type information.
 * @author Anthony Bryant
 */
public class RTTIHelper
{
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
   * Finds the pure RTTI for the specified type, without a VFT search list
   * @param type - the type to find the RTTI for
   * @return the pure RTTI for the specified type
   */
  public LLVMValueRef getPureRTTI(Type type)
  {
    // every sort of type except interface stores its pure RTTI inside its instance RTTI,
    // but interfaces do not have instanec RTTI, so they must store their pure RTTI on its own
    if (type instanceof NamedType && ((NamedType) type).getResolvedTypeDefinition() instanceof InterfaceDefinition)
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

    // lookup the pure RTTI inside this type's instance RTTI
    LLVMValueRef instanceRTTI = getInstanceRTTI(type);
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false)};
    return LLVM.LLVMConstGEP(instanceRTTI, C.toNativePointerArray(indices, false, true), indices.length);
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

      LLVMValueRef[] values = new LLVMValueRef[] {sortId, size, nullable, immutable, qualifiedName};
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
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef immutableType = LLVM.LLVMInt1Type();
      LLVMTypeRef qualifiedNameType = typeHelper.findRawStringType();
      LLVMTypeRef[] types = new LLVMTypeRef[] {sortIdType, sizeType, nullableType, immutableType, qualifiedNameType};
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
    throw new IllegalArgumentException("Cannot find a run-time type information struct type for the unknown type: " + type);
  }

  /**
   * @return the type of a generic instance-RTTI struct
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
   * @return the type of a generic pure-RTTI struct
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
   * Finds the size of the specified type's standard native representation.
   * @param type - the type to find the size of
   * @return the size of the specified type's standard representation, as a 32 bit integer value
   */
  private LLVMValueRef findTypeSize(Type type)
  {
    LLVMTypeRef llvmType = typeHelper.findStandardType(type);
    LLVMTypeRef arrayType = LLVM.LLVMPointerType(LLVM.LLVMArrayType(llvmType, 0), 0);
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false)};
    LLVMValueRef pointer = LLVM.LLVMConstGEP(LLVM.LLVMConstNull(arrayType), C.toNativePointerArray(indices, false, true), indices.length);
    return LLVM.LLVMConstPtrToInt(pointer, LLVM.LLVMInt32Type());
  }
}
