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
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.NullType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.compiler.passes.TypeChecker;

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
  public static final byte VOID_SORT_ID = 9;
  public static final byte NULL_SORT_ID = 10;

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
  public LLVMValueRef lookupClassName(LLVMBuilderRef builder, LLVMValueRef rttiPointer)
  {
    // cast the generic RTTI struct to a NamedType RTTI struct
    // (we do not need to give the NamedType a TypeDefinition here, all pure RTTI structs for NamedTypes have the same LLVM types)
    LLVMValueRef castedObjectRTTI = LLVM.LLVMBuildBitCast(builder, rttiPointer, LLVM.LLVMPointerType(getPureRTTIStructType(new NamedType(false, false, null, null)), 0), "");
    LLVMValueRef[] stringIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 4, false)};
    LLVMValueRef classQualifiedNameUbyteArrayPointer = LLVM.LLVMBuildGEP(builder, castedObjectRTTI, C.toNativePointerArray(stringIndices, false, true), stringIndices.length, "");
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
    // every sort of type except interface stores its pure RTTI inside its instance RTTI,
    // but interfaces do not have instanec RTTI, so they must store their pure RTTI on its own
    if ((type instanceof NamedType && ((NamedType) type).getResolvedTypeDefinition() instanceof InterfaceDefinition) ||
        type instanceof NullType ||
        type instanceof VoidType)
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
      LLVMTypeRef nullableType = LLVM.LLVMInt1Type();
      LLVMTypeRef immutableType = LLVM.LLVMInt1Type();
      LLVMTypeRef qualifiedNameType = typeHelper.findRawStringType();
      LLVMTypeRef[] types = new LLVMTypeRef[] {sortIdType, sizeType, nullableType, immutableType, qualifiedNameType};
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
   * Builds an instanceof check, to check whether the specified value is an instance of the specified checkType.
   * This method assumes that the value and the expression type are not nullable. However, null instanceof &lt;anything&gt; should always return false.
   * @param builder - the LLVMBuilderRef to build the check with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the not-null value to check, in a temporary type representation
   * @param expressionType - the type of the value, which must not be nullable
   * @param checkType - the type to check the RTTI against (note: nullability and data-immutability on the top level of this type are ignored)
   * @return an LLVMValueRef representing an i1 (boolean), which will be true iff value is an instance of checkType
   */
  public LLVMValueRef buildInstanceOfCheck(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef value, Type expressionType, Type checkType)
  {
    checkType = TypeChecker.findTypeWithoutModifiers(checkType);
    if (checkType instanceof ObjectType)
    {
      // catch-all: any not-null type instanceof object is always true
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
    }
    if (expressionType instanceof ArrayType)
    {
      boolean baseTypesMatch = checkType instanceof ArrayType &&
                               ((ArrayType) checkType).getBaseType().isEquivalent(((ArrayType) expressionType).getBaseType());
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), baseTypesMatch ? 1 : 0, false);
    }
    if (expressionType instanceof FunctionType)
    {
      boolean signaturesMatch = checkType instanceof FunctionType;
      if (signaturesMatch)
      {
        FunctionType expressionFunctionType = (FunctionType) expressionType;
        FunctionType checkFunctionType = (FunctionType) checkType;
        Type[] expressionParameterTypes = expressionFunctionType.getParameterTypes();
        Type[] checkParameterTypes = checkFunctionType.getParameterTypes();
        signaturesMatch = expressionFunctionType.getReturnType().isEquivalent(checkFunctionType.getReturnType());
        signaturesMatch &= expressionParameterTypes.length == checkParameterTypes.length;
        for (int i = 0; signaturesMatch & i < expressionParameterTypes.length; ++i)
        {
          signaturesMatch = expressionParameterTypes[i].isEquivalent(checkParameterTypes[i]);
        }
      }
      if (signaturesMatch && !((FunctionType) expressionType).isImmutable() & ((FunctionType) checkType).isImmutable())
      {
        // the result depends on the immutability of the actual function in the value, so check its RTTI
        LLVMValueRef rttiPointer = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
        LLVMValueRef[] rttiIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                         LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false)};
        LLVMValueRef pureRTTIPointer = LLVM.LLVMBuildGEP(builder, rttiPointer, C.toNativePointerArray(rttiIndices, false, true), rttiIndices.length, "");
        pureRTTIPointer = LLVM.LLVMBuildBitCast(builder, pureRTTIPointer, LLVM.LLVMPointerType(getPureRTTIStructType(expressionType), 0), "");
        LLVMValueRef[] immutabilityIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                                 LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 3, false)};
        LLVMValueRef immutabilityPointer = LLVM.LLVMBuildGEP(builder, pureRTTIPointer, C.toNativePointerArray(immutabilityIndices, false, true), immutabilityIndices.length, "");
        return LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
      }
      // the result does not depend on the immutability of the run-time type, so just return whether or not the signatures match
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), signaturesMatch ? 1 : 0, false);
    }
    if (expressionType instanceof NamedType && ((NamedType) expressionType).getResolvedTypeDefinition() instanceof CompoundDefinition)
    {
      boolean matches = checkType instanceof NamedType &&
                        ((NamedType) expressionType).getResolvedTypeDefinition() == ((NamedType) checkType).getResolvedTypeDefinition();
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), matches ? 1 : 0, false);
    }
    if (expressionType instanceof PrimitiveType)
    {
      // primitive types are only instances of themselves (and the object type, covered above)
      boolean result = checkType instanceof PrimitiveType &&
                       ((PrimitiveType) expressionType).getPrimitiveTypeType() == ((PrimitiveType) checkType).getPrimitiveTypeType();
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), result ? 1 : 0, false);
    }
    if (expressionType instanceof TupleType)
    {
      // neither the expression nor the check type can be nullable, and instanceof doesn't check sub-types, so we can just do an equivalence check
      boolean result = expressionType.isEquivalent(checkType);
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), result ? 1 : 0, false);
    }
    if (expressionType instanceof NamedType && checkType instanceof NamedType)
    {
      TypeDefinition expressionTypeDefinition = ((NamedType) expressionType).getResolvedTypeDefinition();
      if (expressionTypeDefinition instanceof ClassDefinition || expressionTypeDefinition instanceof InterfaceDefinition)
      {
        TypeDefinition checkTypeDefinition = ((NamedType) checkType).getResolvedTypeDefinition();
        for (TypeDefinition t : expressionTypeDefinition.getInheritanceLinearisation())
        {
          if (t == checkTypeDefinition)
          {
            return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
          }
        }
        // TODO: if expressionTypeDefinition is sealed, return false
      }
    }
    if (expressionType instanceof NamedType && (((NamedType) expressionType).getResolvedTypeDefinition() instanceof ClassDefinition ||
                                                 ((NamedType) expressionType).getResolvedTypeDefinition() instanceof InterfaceDefinition))
    {
      if (checkType instanceof NamedType && (((NamedType) checkType).getResolvedTypeDefinition() instanceof ClassDefinition ||
                                             ((NamedType) checkType).getResolvedTypeDefinition() instanceof InterfaceDefinition))
      {
        LLVMValueRef objectValue = value;
        if (((NamedType) expressionType).getResolvedTypeDefinition() instanceof InterfaceDefinition)
        {
          objectValue = typeHelper.convertTemporary(builder, landingPadContainer, value, expressionType, new ObjectType(false, false, null));
        }
        // instead of building our own search through the super-type VFT list for checkType's TypeDefinition,
        // we can just search for the super-type's VFT pointer, which will come out as null iff the value does not implement checkType
        LLVMValueRef instanceVFTPointer = virtualFunctionHandler.lookupInstanceVFT(builder, objectValue, ((NamedType) checkType).getResolvedTypeDefinition());
        return LLVM.LLVMBuildIsNotNull(builder, instanceVFTPointer, "");
      }
      // we are checking against something which is not a class, interface, or object, so it is definitely false
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
    }
    if (expressionType instanceof ObjectType)
    {
      if (checkType instanceof NamedType && (((NamedType) checkType).getResolvedTypeDefinition() instanceof ClassDefinition ||
                                             ((NamedType) checkType).getResolvedTypeDefinition() instanceof InterfaceDefinition))
      {
        // instead of building our own search through the super-type VFT list for checkType's TypeDefinition,
        // we can just search for the super-type's VFT pointer, which will come out as null iff the value does not implement checkType
        LLVMValueRef instanceVFTPointer = virtualFunctionHandler.lookupInstanceVFT(builder, value, ((NamedType) checkType).getResolvedTypeDefinition());
        return LLVM.LLVMBuildIsNotNull(builder, instanceVFTPointer, "");
      }
      // we are checking against a non-pointer-type, so compare the type info to what we are asking about
      LLVMValueRef pureRTTIPointer = lookupPureRTTI(builder, value);
      return buildTypeInfoCheck(builder, pureRTTIPointer, checkType);
    }
    throw new IllegalArgumentException("Cannot build instanceof check from " + expressionType + " to " + checkType);
  }

  /**
   * Builds a check as to whether the specified RTTI pointer represents the specified checkType
   * @param builder - the LLVMBuilderRef to build the check with
   * @param pureRTTIPointer - the pointer to the RTTI to check
   * @param checkType - the Type to check against
   * @return a LLVMValueRef representing an i1 (boolean), which will be true iff the RTTI pointer represents the checkType
   */
  private LLVMValueRef buildTypeInfoCheck(LLVMBuilderRef builder, LLVMValueRef pureRTTIPointer, Type checkType)
  {
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
      LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
      LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
      LLVMValueRef nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isNullable() ? 1 : 0, false), "");
      LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
      LLVMValueRef immutabilityValue = LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
      LLVMValueRef immutabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, immutabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), arrayType.isContextuallyImmutable() ? 1 : 0, false), "");
      LLVMValueRef baseTypePointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 4, "");
      LLVMValueRef baseTypeValue = LLVM.LLVMBuildLoad(builder, baseTypePointer, "");
      LLVMValueRef baseTypeMatches = buildTypeInfoCheck(builder, baseTypeValue, arrayType.getBaseType());
      resultValue = LLVM.LLVMBuildAnd(builder, nullabilityMatches, immutabilityMatches, "");
      resultValue = LLVM.LLVMBuildAnd(builder, resultValue, baseTypeMatches, "");
      endResultBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);
    }
    else if (checkType instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) checkType;
      Type[] parameterTypes = functionType.getParameterTypes();
      // check the always-present header data
      LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
      LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
      LLVMValueRef nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isNullable() ? 1 : 0, false), "");
      LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
      LLVMValueRef immutabilityValue = LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
      LLVMValueRef immutabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, immutabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), functionType.isImmutable() ? 1 : 0, false), "");
      LLVMValueRef returnTypePointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 4, "");
      LLVMValueRef returnTypeValue = LLVM.LLVMBuildLoad(builder, returnTypePointer, "");
      LLVMValueRef returnTypeMatches = buildTypeInfoCheck(builder, returnTypeValue, functionType.getReturnType());
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
          LLVMValueRef paramTypeMatches = buildTypeInfoCheck(builder, paramType, parameterTypes[i]);
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
      LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
      LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
      LLVMValueRef nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isNullable() ? 1 : 0, false), "");
      LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
      LLVMValueRef immutabilityValue = LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
      LLVMValueRef immutabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, immutabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), namedType.isContextuallyImmutable() ? 1 : 0, false), "");
      LLVMValueRef qualifiedNameByteArrayPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 4, "");
      LLVMValueRef qualifiedNameByteArray = LLVM.LLVMBuildLoad(builder, qualifiedNameByteArrayPointer, "");
      LLVMValueRef qualifiedNameLengthPointer = typeHelper.getArrayLengthPointer(builder, qualifiedNameByteArray);
      LLVMValueRef qualifiedNameLength = LLVM.LLVMBuildLoad(builder, qualifiedNameLengthPointer, "");

      LLVMValueRef checkQualifiedNameString = codeGenerator.addStringConstant(namedType.getResolvedTypeDefinition().getQualifiedName().toString());
      LLVMValueRef checkQualifiedNameLengthPointer = typeHelper.getArrayLengthPointer(builder, checkQualifiedNameString);
      LLVMValueRef checkQualifiedNameLength = LLVM.LLVMBuildLoad(builder, checkQualifiedNameLengthPointer, "");
      LLVMValueRef lengthMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, qualifiedNameLength, checkQualifiedNameLength, "");

      LLVMValueRef headerMatches = LLVM.LLVMBuildAnd(builder, nullabilityMatches, immutabilityMatches, "");
      headerMatches = LLVM.LLVMBuildAnd(builder, headerMatches, lengthMatches, "");

      LLVMBasicBlockRef checkNameContinuationBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoCheckQNameContinue");
      LLVMBasicBlockRef checkNameBlock = LLVM.LLVMAddBasicBlock(builder, "typeInfoCheckQName");
      LLVMBasicBlockRef checkNameStartBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildCondBr(builder, headerMatches, checkNameBlock, checkNameContinuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, checkNameBlock);
      LLVMValueRef firstBytePointer = typeHelper.getArrayElementPointer(builder, qualifiedNameByteArray, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false));
      LLVMValueRef checkFirstBytePointer = typeHelper.getArrayElementPointer(builder, checkQualifiedNameString, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false));
      LLVMValueRef strncmpFunction = getStringComparisonFunction();
      LLVMValueRef[] strncmpArguments = new LLVMValueRef[] {firstBytePointer, checkFirstBytePointer, checkQualifiedNameLength};
      LLVMValueRef strncmpResult = LLVM.LLVMBuildCall(builder, strncmpFunction, C.toNativePointerArray(strncmpArguments, false, true), strncmpArguments.length, "");
      LLVMValueRef nameMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, strncmpResult, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false), "");
      LLVMBasicBlockRef endCheckNameBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, checkNameContinuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, checkNameContinuationBlock);
      LLVMValueRef namePhi = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt1Type(), "");
      LLVMValueRef[] nameIncomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false), nameMatches};
      LLVMBasicBlockRef[] nameIncomingBlocks = new LLVMBasicBlockRef[] {checkNameStartBlock, endCheckNameBlock};
      LLVM.LLVMAddIncoming(namePhi, C.toNativePointerArray(nameIncomingValues, false, true), C.toNativePointerArray(nameIncomingBlocks, false, true), nameIncomingValues.length);

      resultValue = namePhi;
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
      LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
      LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
      LLVMValueRef nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), objectType.isNullable() ? 1 : 0, false), "");
      LLVMValueRef immutabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 3, "");
      LLVMValueRef immutabilityValue = LLVM.LLVMBuildLoad(builder, immutabilityPointer, "");
      LLVMValueRef immutabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, immutabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), objectType.isContextuallyImmutable() ? 1 : 0, false), "");

      resultValue = LLVM.LLVMBuildAnd(builder, nullabilityMatches, immutabilityMatches, "");
      endResultBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);
    }
    else if (checkType instanceof PrimitiveType)
    {
      PrimitiveType primitiveType = (PrimitiveType) checkType;
      LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
      LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
      LLVMValueRef nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), primitiveType.isNullable() ? 1 : 0, false), "");
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
      LLVMValueRef nullabilityPointer = LLVM.LLVMBuildStructGEP(builder, castedRTTIPointer, 2, "");
      LLVMValueRef nullabilityValue = LLVM.LLVMBuildLoad(builder, nullabilityPointer, "");
      LLVMValueRef nullabilityMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, nullabilityValue, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), tupleType.isNullable() ? 1 : 0, false), "");
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
        LLVMValueRef subTypeMatches = buildTypeInfoCheck(builder, subType, subTypes[i]);
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
    throw new IllegalArgumentException("Unknown sort of Type: " + type);
  }
}
