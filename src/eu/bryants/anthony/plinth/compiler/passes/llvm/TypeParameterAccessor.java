package eu.bryants.anthony.plinth.compiler.passes.llvm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMBasicBlockRef;
import nativelib.llvm.LLVM.LLVMBuilderRef;
import nativelib.llvm.LLVM.LLVMTypeRef;
import nativelib.llvm.LLVM.LLVMValueRef;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;

/*
 * Created on 9 May 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeParameterAccessor
{
  private LLVMBuilderRef builder;
  private TypeHelper typeHelper;
  private RTTIHelper rttiHelper;

  // the TypeDefinition that this accessor works in the context of, and can get type parameters from
  private TypeDefinition typeDefinition;

  // the current object, in a temporary type representation
  private LLVMValueRef thisValue;

  // the map of extra known TypeParameter values, usually provided as parameters (e.g. to an interface function)
  private Map<TypeParameter, LLVMValueRef> knownTypeParameters;

  private LLVMValueRef typeArgumentMapper;

  /**
   * Creates a new TypeParameterAccessor to use for accessing the RTTI blocks for TypeParameters.
   * @param builder - the LLVMBuilderRef to build RTTI extractions and type argument mappers with
   * @param typeHelper - the TypeHelper to extract type parameters from 'this' with
   * @param rttiHelper - the RTTIHelper to generate pure RTTI types with
   * @param typeDefinition - the TypeDefinition that we are working inside in a non-static context, or null in a static context
   * @param thisValue - the value of 'this' to extract RTTI values from
   */
  public TypeParameterAccessor(LLVMBuilderRef builder, TypeHelper typeHelper, RTTIHelper rttiHelper, TypeDefinition typeDefinition, LLVMValueRef thisValue)
  {
    this.builder = builder;
    this.typeHelper = typeHelper;
    this.rttiHelper = rttiHelper;
    this.typeDefinition = typeDefinition;
    this.thisValue = thisValue;
  }

  /**
   * Creates a TypeParameterAccessor that simply looks up RTTI values in a map.
   * @param builder - the LLVMBuilderRef to build type argument mappers with
   * @param rttiHelper - the RTTIHelper to generate pure RTTI types with
   * @param typeDefinition - the TypeDefinition that we are working inside in a non-static context, or null in a static context
   * @param knownTypeParameters - the map to look up TypeParameters in
   */
  public TypeParameterAccessor(LLVMBuilderRef builder, RTTIHelper rttiHelper, TypeDefinition typeDefinition, Map<TypeParameter, LLVMValueRef> knownTypeParameters)
  {
    this.builder = builder;
    this.rttiHelper = rttiHelper;
    this.typeDefinition = typeDefinition;
    this.knownTypeParameters = knownTypeParameters;
  }

  /**
   * Creates a TypeParameterAccessor that doesn't contain any type parameter mappings.
   * @param builder - the LLVMBuilderRef to build type argument mappers with
   * @param rttiHelper - the RTTIHelper to generate pure RTTI types with
   */
  public TypeParameterAccessor(LLVMBuilderRef builder, RTTIHelper rttiHelper)
  {
    this.builder = builder;
    this.rttiHelper = rttiHelper;
  }

  /**
   * Creates a TypeParameterAccessor that looks up values in the specified type argument mapper.
   * @param builder - the LLVMBuilderRef to build RTTI extractions with
   * @param rttiHelper - the RTTIHelper to generate pure RTTI types with
   * @param typeDefinition - the TypeDefinition that we are working inside in a non-static context, or null in a static context
   * @param typeArgumentMapper - the type argument mapper to read type parameter values from
   */
  public TypeParameterAccessor(LLVMBuilderRef builder, RTTIHelper rttiHelper, TypeDefinition typeDefinition, LLVMValueRef typeArgumentMapper)
  {
    this.builder = builder;
    this.rttiHelper = rttiHelper;
    this.typeDefinition = typeDefinition;
    this.typeArgumentMapper = typeArgumentMapper;
  }

  /**
   * Tries to find the specified TypeParameter's RTTI block, by first checking any specific known parameters, and then looking inside 'this' for them.
   * @param typeParameter - the TypeParameter to search for
   * @return a pointer to the pure RTTI block for the type argument corresponding to the TypeParameter
   */
  public LLVMValueRef findTypeParameterRTTI(TypeParameter typeParameter)
  {
    // first, check the map
    if (knownTypeParameters != null)
    {
      LLVMValueRef result = knownTypeParameters.get(typeParameter);
      if (result != null)
      {
        return result;
      }
    }

    // the map didn't contain it, so check 'this'
    if (thisValue != null)
    {
      TypeParameter[] typeParameters = typeDefinition.getTypeParameters();
      for (TypeParameter t : typeParameters)
      {
        if (t == typeParameter)
        {
          LLVMValueRef paramPointer = typeHelper.getTypeParameterPointer(builder, thisValue, typeParameter);
          return LLVM.LLVMBuildLoad(builder, paramPointer, "");
        }
      }
    }

    // try the mapper
    if (typeArgumentMapper != null)
    {
      TypeParameter[] typeParameters = typeDefinition.getTypeParameters();
      for (int i = 0; i < typeParameters.length; ++i)
      {
        if (typeParameters[i] == typeParameter)
        {
          LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), i, false)};
          LLVMValueRef pointer = LLVM.LLVMBuildGEP(builder, typeArgumentMapper, C.toNativePointerArray(indices, false, true), indices.length, "");
          return LLVM.LLVMBuildLoad(builder, pointer, "");
        }
      }
    }

    throw new IllegalArgumentException("Could not find the type parameter: " + typeParameter);
  }

  /**
   * @return the list of all type parameters stored in this TypeParameterAccessor
   */
  private List<TypeParameter> getTypeParameterList()
  {
    List<TypeParameter> list = new ArrayList<TypeParameter>();
    if (thisValue != null)
    {
      TypeParameter[] typeParameters = typeDefinition.getTypeParameters();
      for (TypeParameter t : typeParameters)
      {
        list.add(t);
      }
    }
    if (knownTypeParameters != null && !knownTypeParameters.isEmpty())
    {
      for (TypeParameter t : typeDefinition.getTypeParameters())
      {
        if (!knownTypeParameters.containsKey(t))
        {
          throw new IllegalStateException("TypeParameterAccessor does not contain all of " + typeDefinition.getQualifiedName() + "'s type parameters");
        }
        list.add(t);
      }
      if (typeDefinition.getTypeParameters().length < knownTypeParameters.size())
      {
        throw new IllegalStateException("TypeParameterAccessor contains extra type parameters that are not from " + typeDefinition.getQualifiedName());
      }
    }

    return list;
  }

  /**
   * Builds/Gets a type argument mapper for using in runtime functions such as 'plinth_is_type_equivalent'.
   * The mapper is actually built in the entry block and cached, so that it can be used multiple times.
   * @return the pointer to the type argument mapper built
   */
  public LLVMValueRef getTypeArgumentMapper()
  {
    if (typeArgumentMapper != null)
    {
      return LLVM.LLVMBuildBitCast(builder, typeArgumentMapper, rttiHelper.getGenericTypeArgumentMapperType(), "");
    }

    List<TypeParameter> list = getTypeParameterList();

    LLVMTypeRef mapperType = getTypeArgumentMapperType(list.size());
    LLVMValueRef alloca = LLVM.LLVMBuildAllocaInEntryBlock(builder, mapperType, "");

    LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVM.LLVMPositionBuilderAfterEntryAllocas(builder);
    buildTypeArgumentMapper(alloca, list);
    LLVM.LLVMPositionBuilderAtEnd(builder, currentBlock);

    typeArgumentMapper = alloca;
    return LLVM.LLVMBuildBitCast(builder, typeArgumentMapper, rttiHelper.getGenericTypeArgumentMapperType(), "");
  }

  /**
   * @return the full, non-generic, native structure type of a type argument mapper generated from this TypeParameterAccessor
   */
  public LLVMTypeRef getTypeArgumentMapperType()
  {
    return getTypeArgumentMapperType(getTypeParameterList().size());
  }

  /**
   * @return the native structure type of a type argument mapper with the specified number of parameters
   */
  private LLVMTypeRef getTypeArgumentMapperType(int numParameters)
  {
    LLVMTypeRef pureRTTIType = rttiHelper.getGenericPureRTTIType();
    LLVMTypeRef arrayType = LLVM.LLVMArrayType(pureRTTIType, numParameters);
    LLVMTypeRef[] subTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), arrayType};
    LLVMTypeRef mapperType = LLVM.LLVMStructType(C.toNativePointerArray(subTypes, false, true), subTypes.length, false);
    return mapperType;
  }

  /**
   * Builds a type argument mapper inside the specified memory structure.
   * @param memory - the memory to build the type argument mapper in, whose native type should be a pointer to the type returned from getTypeArgumentMapperType()
   */
  public void buildTypeArgumentMapper(LLVMValueRef memory)
  {
    buildTypeArgumentMapper(memory, getTypeParameterList());
  }

  /**
   * Builds a type argument mapper inside the specified memory structure.
   * @param memory - the memory to build the type argument mapper in, whose native type should be a pointer to the type returned from getTypeArgumentMapperType()
   * @param list - the type parameter list to populate the structure with
   */
  private void buildTypeArgumentMapper(LLVMValueRef memory, List<TypeParameter> list)
  {
    LLVMValueRef lengthPtr = LLVM.LLVMBuildStructGEP(builder, memory, 0, "");
    LLVMValueRef length = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), list.size(), false);
    LLVM.LLVMBuildStore(builder, length, lengthPtr);

    for (int i = 0; i < list.size(); ++i)
    {
      LLVMValueRef rttiValue = findTypeParameterRTTI(list.get(i));
      LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                   LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false),
                                                   LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), i, false)};
      LLVMValueRef rttiPointer = LLVM.LLVMBuildGEP(builder, memory, C.toNativePointerArray(indices, false, true), indices.length, "");
      LLVM.LLVMBuildStore(builder, rttiValue, rttiPointer);
    }
  }

}
