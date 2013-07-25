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
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.ast.type.WildcardType;

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

  // the TypeDefinition or the interface NamedType that this accessor works in the context of, and can get type parameters from
  private TypeDefinition typeDefinition;
  private NamedType interfaceNamedType;

  // the current object, in a temporary type representation
  private LLVMValueRef thisValue;

  // the proxy accessor, only for use when we have an interfaceNamedType
  private TypeParameterAccessor proxyAccessor;

  // the map of extra known TypeParameter values, usually provided as parameters (e.g. to an interface function)
  private Map<TypeParameter, LLVMValueRef> knownTypeParameters;

  // the instruction and block that mark when this TypeParameterAccessor was created, so that a type argument mapper can be created at exactly that point if necessary
  private LLVMBasicBlockRef createdDuringBlock;
  private LLVMValueRef createdAfterInstruction;

  private LLVMValueRef typeArgumentMapper;

  /**
   * Creates a new TypeParameterAccessor to use for accessing the RTTI blocks for TypeParameters.
   * @param builder - the LLVMBuilderRef to build RTTI extractions and type argument mappers with
   * @param typeHelper - the TypeHelper to extract type parameters from 'this' with
   * @param rttiHelper - the RTTIHelper to generate pure RTTI types with
   * @param typeDefinition - the TypeDefinition that we are working inside in a non-static context, or null in a static context
   * @param thisValue - the value of 'this' to extract RTTI values from, in a temporary type representation
   */
  public TypeParameterAccessor(LLVMBuilderRef builder, TypeHelper typeHelper, RTTIHelper rttiHelper, TypeDefinition typeDefinition, LLVMValueRef thisValue)
  {
    this.builder = builder;
    this.typeHelper = typeHelper;
    this.rttiHelper = rttiHelper;
    this.typeDefinition = typeDefinition;
    this.thisValue = thisValue;
    createdDuringBlock = LLVM.LLVMGetInsertBlock(builder);
    createdAfterInstruction = LLVM.LLVMGetLastInstruction(createdDuringBlock);
  }

  /**
   * Creates a new TypeParameterAccessor which looks up an interface's RTTI blocks.
   * It does this by either creating it from the known value inside thisInterfaceType, or if it is a wildcard, by looking it up inside thisValue.
   * When creating a new RTTI block from thisInterfaceType, any encountered type parameters will be looked up in the specified proxy accessor.
   * @param builder - the LLVMBuilderRef to build RTTI creations and extractions and type argument mappers with
   * @param rttiHelper - the RTTIHelper to generate RTTI types with
   * @param thisValue - the value of this, which must be in a temporary representation of thisInterfaceType
   * @param thisInterfaceType - the type of the interface that this accessor will access the type parameters from
   * @param proxyAccessor - the TypeParameterAccessor to consult about any type parameters inside thisInterfaceType
   */
  public TypeParameterAccessor(LLVMBuilderRef builder, RTTIHelper rttiHelper, LLVMValueRef thisValue, NamedType thisInterfaceType, TypeParameterAccessor proxyAccessor)
  {
    if (!(thisInterfaceType.getResolvedTypeDefinition() instanceof InterfaceDefinition))
    {
      throw new IllegalArgumentException("An interface TypeParameterAccessor cannot be created for anything but an interface");
    }
    this.builder = builder;
    this.rttiHelper = rttiHelper;
    this.thisValue = thisValue;
    this.interfaceNamedType = thisInterfaceType;
    this.proxyAccessor = proxyAccessor;
    createdDuringBlock = LLVM.LLVMGetInsertBlock(builder);
    createdAfterInstruction = LLVM.LLVMGetLastInstruction(createdDuringBlock);
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
    createdDuringBlock = LLVM.LLVMGetInsertBlock(builder);
    createdAfterInstruction = LLVM.LLVMGetLastInstruction(createdDuringBlock);
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
    createdDuringBlock = LLVM.LLVMGetInsertBlock(builder);
    createdAfterInstruction = LLVM.LLVMGetLastInstruction(createdDuringBlock);
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
    // no need to set createdAfterInstruction here, as the typeArgumentMapper already exists
  }

  /**
   * @return the TypeDefinition associated with this TypeParameterAccessor, or null if this TypeParameterAccessor doesn't have any sort of associated TypeDefinition
   */
  public TypeDefinition getTypeDefinition()
  {
    if (interfaceNamedType != null)
    {
      return interfaceNamedType.getResolvedTypeDefinition();
    }
    return typeDefinition;
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
      if (typeDefinition != null)
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
      else if (interfaceNamedType != null)
      {
        TypeDefinition interfaceTypeDefinition = interfaceNamedType.getResolvedTypeDefinition();
        TypeParameter[] typeParameters = interfaceTypeDefinition.getTypeParameters();
        Type[] typeArguments = interfaceNamedType.getTypeArguments();
        int wildcardIndex = 0;
        for (int i = 0; i < typeParameters.length; ++i)
        {
          if (typeArguments[i] instanceof WildcardType)
          {
            if (typeParameters[i] == typeParameter)
            {
              // extract the wildcard type's RTTI block from the interface's 'this' value
              return LLVM.LLVMBuildExtractValue(builder, thisValue, 2 + wildcardIndex, "");
            }
            ++wildcardIndex;
          }
          if (typeParameters[i] == typeParameter)
          {
            return rttiHelper.buildRTTICreation(builder, false, typeArguments[i], proxyAccessor);
          }
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
      if (typeDefinition != null)
      {
        TypeParameter[] typeParameters = typeDefinition.getTypeParameters();
        for (TypeParameter t : typeParameters)
        {
          list.add(t);
        }
      }
      else if (interfaceNamedType != null)
      {
        TypeParameter[] typeParameters = interfaceNamedType.getResolvedTypeDefinition().getTypeParameters();
        for (TypeParameter t : typeParameters)
        {
          list.add(t);
        }
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
   * @return the pointer to the type argument mapper built, in the generic type argument mapper LLVM type
   */
  public LLVMValueRef getTypeArgumentMapper()
  {
    if (typeArgumentMapper != null)
    {
      return LLVM.LLVMBuildBitCast(builder, typeArgumentMapper, rttiHelper.getGenericTypeArgumentMapperType(), "");
    }

    List<TypeParameter> list = getTypeParameterList();

    LLVMTypeRef mapperType = rttiHelper.getTypeArgumentMapperType(list.size());
    LLVMValueRef alloca = LLVM.LLVMBuildAllocaInEntryBlock(builder, mapperType, "");

    LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);

    if (createdDuringBlock.equals(LLVM.LLVMGetEntryBasicBlock(LLVM.LLVMGetBasicBlockParent(currentBlock))) &&
        (createdAfterInstruction == null || LLVM.LLVMIsAAllocaInst(createdAfterInstruction) != null))
    {
      // this mapper was created during the allocas in the entry block, so make sure the mapper building code is built after the allocas
      LLVM.LLVMPositionBuilderAfterEntryAllocas(builder);
    }
    else
    {
      LLVM.LLVMPositionBuilderAfter(builder, createdDuringBlock, createdAfterInstruction);
    }
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
    return rttiHelper.getTypeArgumentMapperType(getTypeParameterList().size());
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
