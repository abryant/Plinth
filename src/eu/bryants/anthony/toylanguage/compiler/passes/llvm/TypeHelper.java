package eu.bryants.anthony.toylanguage.compiler.passes.llvm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMBasicBlockRef;
import nativelib.llvm.LLVM.LLVMBuilderRef;
import nativelib.llvm.LLVM.LLVMTypeRef;
import nativelib.llvm.LLVM.LLVMValueRef;
import eu.bryants.anthony.toylanguage.ast.ClassDefinition;
import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.TypeDefinition;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.type.ArrayType;
import eu.bryants.anthony.toylanguage.ast.type.FunctionType;
import eu.bryants.anthony.toylanguage.ast.type.NamedType;
import eu.bryants.anthony.toylanguage.ast.type.NullType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.toylanguage.ast.type.TupleType;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.ast.type.VoidType;
import eu.bryants.anthony.toylanguage.compiler.passes.TypeChecker;

/*
 * Created on 23 Sep 2012
 */

/**
 * A class which helps a CodeGenerator convert the AST into bitcode by providing methods to convert types into their native representations, and methods to convert between these native types.
 * @author Anthony Bryant
 */
public class TypeHelper
{
  private LLVMBuilderRef builder;

  private LLVMTypeRef opaqueType;
  private Map<TypeDefinition, LLVMTypeRef> nativeNamedTypes = new HashMap<TypeDefinition, LLVMTypeRef>();

  /**
   * Creates a new TypeHelper to build type conversions with the specified builder.
   * @param builder - the LLVMBuilderRef to build type conversions with
   */
  public TypeHelper(LLVMBuilderRef builder)
  {
    this.builder = builder;
    opaqueType = LLVM.LLVMStructCreateNamed(LLVM.LLVMGetGlobalContext(), "opaque");
  }

  /**
   * @return an opaque pointer type
   */
  public LLVMTypeRef getOpaquePointer()
  {
    return LLVM.LLVMPointerType(opaqueType, 0);
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
      if (type.isNullable())
      {
        // tuple the non-nullable type with a boolean, so that we can tell whether or not the value is null
        LLVMTypeRef[] types = new LLVMTypeRef[] {LLVM.LLVMInt1Type(), nonNullableType};
        return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
      }
      return nonNullableType;
    }
    if (type instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) type;
      LLVMTypeRef baseType = findNativeType(arrayType.getBaseType(), false);
      LLVMTypeRef llvmArray = LLVM.LLVMArrayType(baseType, 0);
      LLVMTypeRef[] structureTypes = new LLVMTypeRef[] {LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), llvmArray};
      LLVMTypeRef llvmStructure = LLVM.LLVMStructType(C.toNativePointerArray(structureTypes, false, true), 2, false);
      return LLVM.LLVMPointerType(llvmStructure, 0);
    }
    if (type instanceof FunctionType)
    {
      // create a tuple of an opaque pointer and a function pointer which has an opaque pointer as its first argument
      FunctionType functionType = (FunctionType) type;
      LLVMTypeRef llvmOpaquePointerType = LLVM.LLVMPointerType(opaqueType, 0);
      LLVMTypeRef llvmFunctionPointer = findRawFunctionPointerType(functionType);
      LLVMTypeRef[] subTypes = new LLVMTypeRef[] {llvmOpaquePointerType, llvmFunctionPointer};
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
      if (tupleType.isNullable())
      {
        // tuple the non-nullable type with a boolean, so that we can tell whether or not the value is null
        LLVMTypeRef[] types = new LLVMTypeRef[] {LLVM.LLVMInt1Type(), nonNullableType};
        return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
      }
      return nonNullableType;
    }
    if (type instanceof NamedType)
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
          if (namedType.isNullable())
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
        LLVMTypeRef structType = LLVM.LLVMStructCreateNamed(LLVM.LLVMGetGlobalContext(), typeDefinition.getQualifiedName().toString());
        LLVMTypeRef pointerToStruct = LLVM.LLVMPointerType(structType, 0);
        nativeNamedTypes.put(typeDefinition, pointerToStruct);

        // add the fields to the struct type recursively
        Field[] fields = typeDefinition.getNonStaticFields();
        LLVMTypeRef[] llvmSubTypes = new LLVMTypeRef[fields.length];
        for (int i = 0; i < fields.length; ++i)
        {
          llvmSubTypes[i] = findNativeType(fields[i].getType(), false);
        }
        LLVM.LLVMStructSetBody(structType, C.toNativePointerArray(llvmSubTypes, false, true), llvmSubTypes.length, false);
        return pointerToStruct;
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        // cache the LLVM type before we recurse, so that once we recurse, everything will be able to use this type instead of recreating it
        // later on, we add the fields using LLVMStructSetBody
        LLVMTypeRef nonNullableStructType = LLVM.LLVMStructCreateNamed(LLVM.LLVMGetGlobalContext(), typeDefinition.getQualifiedName().toString());
        nativeNamedTypes.put(typeDefinition, nonNullableStructType);

        // add the fields to the struct recursively
        Field[] fields = typeDefinition.getNonStaticFields();
        LLVMTypeRef[] llvmSubTypes = new LLVMTypeRef[fields.length];
        for (int i = 0; i < fields.length; i++)
        {
          llvmSubTypes[i] = findNativeType(fields[i].getType(), false);
        }
        LLVM.LLVMStructSetBody(nonNullableStructType, C.toNativePointerArray(llvmSubTypes, false, true), llvmSubTypes.length, false);
        if (temporary)
        {
          // for temporary values, we use a pointer to the non-nullable type, whether or not the type is nullable
          return LLVM.LLVMPointerType(nonNullableStructType, 0);
        }
        if (namedType.isNullable())
        {
          // tuple the non-nullable type with a boolean, so that we can tell whether or not the value is null
          LLVMTypeRef[] types = new LLVMTypeRef[] {LLVM.LLVMInt1Type(), nonNullableStructType};
          return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
        }
        return nonNullableStructType;
      }
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
   * Finds a function pointer type in its raw form, before being tupled with its first argument (always an opaque pointer).
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
   * Converts the specified value from the specified 'from' type to the specified 'to' type, as a temporary.
   * This method assumes that the incoming value has a temporary native type, and produces a result with a temporary native type.
   * @param value - the value to convert
   * @param from - the Type to convert from
   * @param to - the Type to convert to
   * @return the converted value
   */
  public LLVMValueRef convertTemporary(LLVMValueRef value, Type from, Type to)
  {
    if (from.isEquivalent(to))
    {
      return value;
    }
    if (from instanceof PrimitiveType && to instanceof PrimitiveType)
    {
      return convertPrimitiveType(value, (PrimitiveType) from, (PrimitiveType) to);
    }
    if (from instanceof ArrayType && to instanceof ArrayType)
    {
      // array casts are illegal unless from and to types are the same, so they must have the same type
      // nullability will be checked by the type checker, but has no effect on the native type, so we do not need to do anything special here

      // if from is nullable, to is not nullable, and value is null, then the value we are returning here is undefined
      // TODO: if from is nullable, to is not nullable, and value is null, throw an exception here instead of having undefined behaviour
      return value;
    }
    if (from instanceof NamedType && to instanceof NamedType &&
        ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition &&
        ((NamedType) to).getResolvedTypeDefinition() instanceof ClassDefinition)
    {
      // TODO: this will need changing when we add inheritance
      // class type casts are illegal unless from and to types are the same, so they must have the same type
      // nullability will be checked by the type checker, but has no effect on the temporary type, so we do not need to do anything special here

      // if from is nullable, to is not nullable, and value is null, then the value we are returning here is undefined
      // TODO: if from is nullable, to is not nullable, and value is null, throw an exception here instead of having undefined behaviour
      return value;
    }
    if (from instanceof NamedType && to instanceof NamedType &&
        ((NamedType) from).getResolvedTypeDefinition() instanceof CompoundDefinition &&
        ((NamedType) to).getResolvedTypeDefinition() instanceof CompoundDefinition)
    {
      // compound type casts are illegal unless from and to types are the same, so they must have the same type
      // nullability will be checked by the type checker, but has no effect on the temporary type, so we do not need to do anything special here

      // if from is nullable, to is not nullable, and value is null, then the value we are returning here is undefined
      // TODO: if from is nullable, to is not nullable, and value is null, throw an exception here instead of having undefined behaviour
      return value;
    }
    if (from instanceof TupleType && !(to instanceof TupleType))
    {
      TupleType fromTuple = (TupleType) from;
      if (fromTuple.getSubTypes().length != 1)
      {
        throw new IllegalArgumentException("Cannot convert from a " + from + " to a " + to);
      }
      if (from.isNullable())
      {
        // extract the value of the tuple from the nullable structure
        // if from is nullable and value is null, then the value we are using here is undefined
        // TODO: if from is nullable and value is null, throw an exception here instead of having undefined behaviour
        value = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
      }
      return LLVM.LLVMBuildExtractValue(builder, value, 0, "");
    }
    if (!(from instanceof TupleType) && to instanceof TupleType)
    {
      TupleType toTuple = (TupleType) to;
      if (toTuple.getSubTypes().length != 1)
      {
        throw new IllegalArgumentException("Cannot convert from a " + from + " to a " + to);
      }
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
    if (from instanceof TupleType && to instanceof TupleType)
    {
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
        if (!fromSubTypes[i].isEquivalent(toSubTypes[i]))
        {
          subTypesEquivalent = false;
          break;
        }
      }
      if (subTypesEquivalent)
      {
        // just convert the nullability
        if (from.isNullable() && !to.isNullable())
        {
          // extract the value of the tuple from the nullable structure
          // if from is nullable and value is null, then the value we are using here is undefined
          // TODO: if from is nullable and value is null, throw an exception here instead of having undefined behaviour
          return LLVM.LLVMBuildExtractValue(builder, value, 1, "");
        }
        if (!from.isNullable() && to.isNullable())
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
      if (from.isNullable())
      {
        isNotNullValue = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
        tupleValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
      }

      LLVMValueRef currentValue = LLVM.LLVMGetUndef(findTemporaryType(toTuple));
      for (int i = 0; i < fromSubTypes.length; i++)
      {
        LLVMValueRef current = LLVM.LLVMBuildExtractValue(builder, tupleValue, i, "");
        LLVMValueRef converted = convertTemporary(current, fromSubTypes[i], toSubTypes[i]);
        currentValue = LLVM.LLVMBuildInsertValue(builder, currentValue, converted, i, "");
      }

      if (to.isNullable())
      {
        LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(to));
        if (from.isNullable())
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
      // if from is nullable and value is null, then the value we are using here is undefined
      // TODO: if from is nullable and value is null, throw an exception here instead of having undefined behaviour
      return currentValue;
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
  private LLVMValueRef convertPrimitiveType(LLVMValueRef value, PrimitiveType from, PrimitiveType to)
  {
    PrimitiveTypeType fromType = from.getPrimitiveTypeType();
    PrimitiveTypeType toType = to.getPrimitiveTypeType();
    if (fromType == toType && from.isNullable() == to.isNullable())
    {
      return value;
    }
    LLVMValueRef primitiveValue = value;
    if (from.isNullable())
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
    if (to.isNullable())
    {
      LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(to));
      if (from.isNullable())
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
    // if from was null, then the value we are returning here is undefined
    // TODO: if from was null, throw an exception here instead of having undefined behaviour
    return primitiveValue;
  }

  /**
   * Converts the specified value of the specified type from a temporary type representation to a standard type representation, after converting it from 'fromType' to 'toType'.
   * @param value - the value to convert
   * @param fromType - the type to convert from
   * @param toType - the type to convert to
   * @param llvmFunction - the function to add any LLVMBasicBlockRefs to if required
   * @return the converted value
   */
  public LLVMValueRef convertTemporaryToStandard(LLVMValueRef value, Type fromType, Type toType, LLVMValueRef llvmFunction)
  {
    LLVMValueRef temporary = convertTemporary(value, fromType, toType);
    return convertTemporaryToStandard(temporary, toType, llvmFunction);
  }

  /**
   * Converts the specified value of the specified type from a temporary type representation to a standard type representation.
   * @param value - the value to convert
   * @param type - the type to convert
   * @param llvmFunction - the function to add any LLVMBasicBlockRefs to if required
   * @return the converted value
   */
  public LLVMValueRef convertTemporaryToStandard(LLVMValueRef value, Type type, LLVMValueRef llvmFunction)
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
      TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        // the temporary and standard types are the same for class types
        return value;
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        if (type.isNullable())
        {
          LLVMTypeRef standardType = findStandardType(type);
          // we are converting from a pointer to a non-nullable compound into a possibly-null compound
          LLVMValueRef isNotNullValue = LLVM.LLVMBuildIsNotNull(builder, value, "");
          // we need to branch on isNotNullValue, to decide whether to load from the pointer
          LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVMBasicBlockRef loadBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "compoundConversion");
          LLVMBasicBlockRef convertedBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "compoundConverted");

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
      throw new IllegalArgumentException("NullType has no standard representation");
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
      if (type.isNullable())
      {
        notNullValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
      }
      LLVMValueRef resultNotNull = LLVM.LLVMGetUndef(findStandardType(TypeChecker.findTypeWithNullability(type, false)));
      Type[] subTypes = ((TupleType) type).getSubTypes();
      for (int i = 0; i < subTypes.length; ++i)
      {
        LLVMValueRef extractedValue = LLVM.LLVMBuildExtractValue(builder, notNullValue, i, "");
        LLVMValueRef convertedValue = convertTemporaryToStandard(extractedValue, subTypes[i], llvmFunction);
        resultNotNull = LLVM.LLVMBuildInsertValue(builder, resultNotNull, convertedValue, i, "");
      }
      if (type.isNullable())
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
    throw new IllegalArgumentException("Unknown type: " + type);
  }

  /**
   * Converts the specified value of the specified type from a standard type representation to a temporary type representation, before converting it from 'fromType' to 'toType'.
   * @param value - the value to convert
   * @param fromType - the type to convert from
   * @param toType - the type to convert to
   * @param llvmFunction - the function to add any LLVMBasicBlockRefs to if required
   * @return the converted value
   */
  public LLVMValueRef convertStandardToTemporary(LLVMValueRef value, Type fromType, Type toType, LLVMValueRef llvmFunction)
  {
    LLVMValueRef temporary = convertStandardToTemporary(value, fromType, llvmFunction);
    return convertTemporary(temporary, fromType, toType);
  }

  /**
   * Converts the specified value of the specified type from a standard type representation to a temporary type representation.
   * @param value - the value to convert
   * @param type - the type to convert
   * @param llvmFunction - the function to add any allocas to the start of if required
   * @return the converted value
   */
  public LLVMValueRef convertStandardToTemporary(LLVMValueRef value, Type type, LLVMValueRef llvmFunction)
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
      TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        // the temporary and standard types are the same for class types
        return value;
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        LLVMValueRef notNullValue = value;
        if (type.isNullable())
        {
          notNullValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
        }

        // build an alloca at the top of the entry block, to store this new value
        LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMPositionBuilderAtStart(builder, LLVM.LLVMGetEntryBasicBlock(llvmFunction));
        // find the type to alloca, which is the standard representation of a non-nullable version of this type
        // when we alloca this type, it becomes equivalent to the temporary type representation of this compound type (with any nullability)
        LLVMTypeRef allocaBaseType = findStandardType(TypeChecker.findTypeWithNullability(type, false));
        LLVMValueRef alloca = LLVM.LLVMBuildAlloca(builder, allocaBaseType, "");
        LLVM.LLVMPositionBuilderAtEnd(builder, currentBlock);
        LLVM.LLVMBuildStore(builder, notNullValue, alloca);
        if (type.isNullable())
        {
          LLVMValueRef isNotNullValue = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
          return LLVM.LLVMBuildSelect(builder, isNotNullValue, alloca, LLVM.LLVMConstNull(findTemporaryType(type)), "");
        }
        return alloca;
      }
    }
    if (type instanceof NullType)
    {
      throw new IllegalArgumentException("NullType has no standard representation");
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
      if (type.isNullable())
      {
        notNullValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
      }
      LLVMValueRef resultNotNull = LLVM.LLVMGetUndef(findStandardType(TypeChecker.findTypeWithNullability(type, false)));
      Type[] subTypes = ((TupleType) type).getSubTypes();
      for (int i = 0; i < subTypes.length; ++i)
      {
        LLVMValueRef extractedValue = LLVM.LLVMBuildExtractValue(builder, notNullValue, i, "");
        LLVMValueRef convertedValue = convertStandardToTemporary(extractedValue, subTypes[i], llvmFunction);
        resultNotNull = LLVM.LLVMBuildInsertValue(builder, resultNotNull, convertedValue, i, "");
      }
      if (type.isNullable())
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
      throw new IllegalArgumentException("VoidType has no temporary representation");
    }
    throw new IllegalArgumentException("Unknown type: " + type);
  }

  /**
   * Converts the specified pointer to a value of the specified type from a pointer to a standard type representation to a temporary type representation, before converting it from 'fromType' to 'toType'.
   * @param pointer - the pointer to the value to convert
   * @param fromType - the type to convert from
   * @param toType - the type to convert to
   * @param llvmFunction - the function to add any LLVMBasicBlockRefs to if required
   * @return the converted value
   */
  public LLVMValueRef convertStandardPointerToTemporary(LLVMValueRef pointer, Type fromType, Type toType, LLVMValueRef llvmFunction)
  {
    LLVMValueRef temporary = convertStandardPointerToTemporary(pointer, fromType, llvmFunction);
    return convertTemporary(temporary, fromType, toType);
  }

  /**
   * Converts the specified pointer to a value of the specified type from a pointer to a standard type representation to a temporary type representation.
   * @param value - the pointer to the value to convert
   * @param type - the type to convert
   * @param llvmFunction - the function to add any allocas to the start of if required
   * @return the converted value
   */
  public LLVMValueRef convertStandardPointerToTemporary(LLVMValueRef value, Type type, LLVMValueRef llvmFunction)
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
      TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        // the temporary and standard types are the same for class types
        return LLVM.LLVMBuildLoad(builder, value, "");
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        if (type.isNullable())
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
      throw new IllegalArgumentException("NullType has no standard representation");
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
      if (type.isNullable())
      {
        LLVMValueRef[] nullabilityIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                                LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false)};
        LLVMValueRef isNotNullPointer = LLVM.LLVMBuildGEP(builder, value, C.toNativePointerArray(nullabilityIndices, false, true), nullabilityIndices.length, "");
        isNotNullValue = LLVM.LLVMBuildLoad(builder, isNotNullPointer, "");

        LLVMValueRef[] valueIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                          LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false)};
        notNullPointer = LLVM.LLVMBuildGEP(builder, value, C.toNativePointerArray(valueIndices, false, true), valueIndices.length, "");
      }
      LLVMValueRef resultNotNull = LLVM.LLVMGetUndef(findStandardType(TypeChecker.findTypeWithNullability(type, false)));
      Type[] subTypes = ((TupleType) type).getSubTypes();
      for (int i = 0; i < subTypes.length; ++i)
      {
        LLVMValueRef[] valueIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                          LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), i, false)};
        LLVMValueRef valuePointer = LLVM.LLVMBuildGEP(builder, notNullPointer, C.toNativePointerArray(valueIndices, false, true), valueIndices.length, "");
        LLVMValueRef convertedValue = convertStandardPointerToTemporary(valuePointer, subTypes[i], llvmFunction);
        resultNotNull = LLVM.LLVMBuildInsertValue(builder, resultNotNull, convertedValue, i, "");
      }
      if (type.isNullable())
      {
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
    throw new IllegalArgumentException("Unknown type: " + type);
  }

}
