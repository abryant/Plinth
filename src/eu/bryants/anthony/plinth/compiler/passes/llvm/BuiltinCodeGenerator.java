package eu.bryants.anthony.plinth.compiler.passes.llvm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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
import eu.bryants.anthony.plinth.ast.expression.EqualityExpression.EqualityOperator;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod.BuiltinMethodType;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.metadata.GenericTypeSpecialiser;
import eu.bryants.anthony.plinth.ast.metadata.MemberVariable;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference.Disambiguator;
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
import eu.bryants.anthony.plinth.ast.type.WildcardType;
import eu.bryants.anthony.plinth.compiler.passes.SpecialTypeHandler;

/*
 * Created on 11 Oct 2012
 */

/**
 * @author Anthony Bryant
 */
public class BuiltinCodeGenerator
{
  private LLVMModuleRef module;

  private CodeGenerator codeGenerator;
  private TypeHelper typeHelper;
  private RTTIHelper rttiHelper;

  /**
   * Creates a new BuiltinCodeGenerator to generate code for special types.
   * @param module - the LLVMModuleRef to add new functions to
   * @param codeGenerator - the CodeGenerator to use to build certain elements of the module
   * @param typeHelper - the TypeHelper to find native types with, and to use to convert between types
   * @param rttiHelper - the RTTIHelper to use to extract run-time type information in builtin methods
   */
  public BuiltinCodeGenerator(LLVMModuleRef module, CodeGenerator codeGenerator, TypeHelper typeHelper, RTTIHelper rttiHelper)
  {
    this.module = module;
    this.codeGenerator = codeGenerator;
    this.typeHelper = typeHelper;
    this.rttiHelper = rttiHelper;
  }

  public LLVMValueRef generateMethod(BuiltinMethod method)
  {
    // only define the function now if it has not been defined already
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, method.getMangledName());
    if (existingFunction != null && !LLVM.LLVMIsDeclaration(existingFunction))
    {
      return existingFunction;
    }

    Type baseType = method.getBaseType();
    BuiltinMethodType builtinType = method.getBuiltinType();
    switch (builtinType)
    {
    case TO_STRING:
      if (baseType instanceof PrimitiveType)
      {
        switch (((PrimitiveType) baseType).getPrimitiveTypeType())
        {
        case BOOLEAN:
          return buildBooleanToString((PrimitiveType) baseType, method);
        case BYTE: case SHORT: case INT: case LONG:
          return buildSignedToString((PrimitiveType) baseType, method, false);
        case UBYTE: case USHORT: case UINT: case ULONG:
          return buildUnsignedToString((PrimitiveType) baseType, method, false);
        case FLOAT: case DOUBLE:
          return buildFloatingToString((PrimitiveType) baseType, method);
        }
      }
      else if (baseType instanceof ObjectType || method.getContainingTypeDefinition() instanceof CompoundDefinition)
      {
        return buildObjectToString(baseType, method);
      }
      else if (baseType instanceof ArrayType)
      {
        return buildArrayToString((ArrayType) baseType, method);
      }
      else if (baseType instanceof FunctionType)
      {
        return buildFunctionToString(baseType, method);
      }
      else if (baseType instanceof TupleType)
      {
        return buildTupleToString(baseType, method);
      }
      throw new IllegalArgumentException("Unknown base type for a toString() method: " + baseType);
    case TO_STRING_RADIX:
      if (baseType instanceof PrimitiveType)
      {
        if (((PrimitiveType) baseType).getPrimitiveTypeType().isSigned())
        {
          return buildSignedToString((PrimitiveType) baseType, method, true);
        }
        return buildUnsignedToString((PrimitiveType) baseType, method, true);
      }
      throw new IllegalArgumentException("Unknown base type for a toString(uint radix) method: " + baseType);
    case EQUALS:
      if (baseType instanceof PrimitiveType)
      {
        switch (((PrimitiveType) baseType).getPrimitiveTypeType())
        {
        case BOOLEAN:
          return buildBooleanEquals((PrimitiveType) baseType, method);
        case BYTE: case SHORT: case INT: case LONG:
        case UBYTE: case USHORT: case UINT: case ULONG:
          return buildIntegerEquals((PrimitiveType) baseType, method);
        case FLOAT: case DOUBLE:
          return buildFloatingEquals((PrimitiveType) baseType, method);
        }
      }
      else if (baseType instanceof ObjectType || method.getContainingTypeDefinition() instanceof CompoundDefinition)
      {
        return buildObjectEquals(baseType, method);
      }
      else if (baseType instanceof ArrayType)
      {
        return buildArrayEquals((ArrayType) baseType, method);
      }
      else if (baseType instanceof FunctionType)
      {
        return buildFunctionEquals((FunctionType) baseType, method);
      }
      else if (baseType instanceof TupleType)
      {
        return buildTupleEquals((TupleType) baseType, method);
      }
      throw new IllegalArgumentException("Unknown base type for an equals(?#object other) method: " + baseType);
    case HASH_CODE:
      if (baseType instanceof PrimitiveType)
      {
        switch (((PrimitiveType) baseType).getPrimitiveTypeType())
        {
        case BOOLEAN:
          return buildBooleanHashCode((PrimitiveType) baseType, method);
        case BYTE: case SHORT: case INT: case LONG:
        case UBYTE: case USHORT: case UINT: case ULONG:
          return buildIntegerHashCode((PrimitiveType) baseType, method);
        case FLOAT: case DOUBLE:
          return buildFloatingHashCode((PrimitiveType) baseType, method);
        }
      }
      else if (baseType instanceof ObjectType || method.getContainingTypeDefinition() instanceof CompoundDefinition)
      {
        return buildObjectHashCode(baseType, method);
      }
      else if (baseType instanceof ArrayType)
      {
        return buildArrayHashCode((ArrayType) baseType, method);
      }
      else if (baseType instanceof FunctionType)
      {
        return buildFunctionHashCode(method);
      }
      else if (baseType instanceof TupleType)
      {
        return buildTupleHashCode((TupleType) baseType, method);
      }
      throw new IllegalArgumentException("Unknown base type for a hashCode() method: " + baseType);
    default:
      throw new IllegalArgumentException("Unknown built-in method: " + method);
    }
  }

  private LLVMValueRef getBuiltinMethod(BuiltinMethod method)
  {
    String mangledName = method.getMangledName();

    // if the function has already been declared, then don't re-declare it
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    LLVMTypeRef functionType = typeHelper.findMethodType(method);
    LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);
    if (method.getContainingTypeDefinition() == null)
    {
      // use linkonce-odr linkage, so that this function does not conflict with anything
      LLVM.LLVMSetLinkage(llvmFunc, LLVM.LLVMLinkage.LLVMLinkOnceODRLinkage);
      LLVM.LLVMSetVisibility(llvmFunc, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    }

    Parameter[] parameters = method.getParameters();
    LLVM.LLVMSetValueName(LLVM.LLVMGetParam(llvmFunc, 0), method.isStatic() ? "unused" : "this");
    for (Parameter parameter : parameters)
    {
      LLVMValueRef llvmParameter = LLVM.LLVMGetParam(llvmFunc, 1 + parameter.getIndex());
      LLVM.LLVMSetValueName(llvmParameter, parameter.getName());
    }

    return llvmFunc;
  }

  private LLVMValueRef buildBooleanToString(PrimitiveType baseType, BuiltinMethod method)
  {
    if (baseType.getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
    {
      throw new IllegalArgumentException("A builtin boolean toString function must have the correct base type");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);
    LLVMValueRef valueOfFunction = codeGenerator.getMethodFunction(SpecialTypeHandler.stringValueOfBoolean);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef[] arguments = new LLVMValueRef[] {LLVM.LLVMConstNull(typeHelper.getOpaquePointer()), parameter};
    LLVMBasicBlockRef landingPadBlock = LLVM.LLVMAppendBasicBlock(LLVM.LLVMGetBasicBlockParent(LLVM.LLVMGetInsertBlock(builder)), "landingPad");
    LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "invokeContinue");
    LLVMValueRef result = LLVM.LLVMBuildInvoke(builder, valueOfFunction, C.toNativePointerArray(arguments, false, true), arguments.length, invokeContinueBlock, landingPadBlock, "");
    LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);
    LLVM.LLVMBuildRet(builder, result);

    LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
    LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
    LLVM.LLVMSetCleanup(landingPad, true);
    LLVM.LLVMBuildResume(builder, landingPad);

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildBooleanEquals(PrimitiveType baseType, BuiltinMethod method)
  {
    if (baseType.getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
    {
      throw new IllegalArgumentException("A builtin boolean equals function must have the correct base type");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 1);

    ObjectType objectType = new ObjectType(true, true, null);
    parameter = typeHelper.convertStandardToTemporary(builder, parameter, objectType);

    LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, parameter, objectType);
    LLVMBasicBlockRef failureBlock = LLVM.LLVMAddBasicBlock(builder, "failure");
    LLVMBasicBlockRef notNullParameterBlock = LLVM.LLVMAddBasicBlock(builder, "notNullParameter");
    LLVM.LLVMBuildCondBr(builder, isNotNull, notNullParameterBlock, failureBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, failureBlock);
    LLVM.LLVMBuildRet(builder, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false));

    LLVM.LLVMPositionBuilderAtEnd(builder, notNullParameterBlock);
    LLVMValueRef rttiPointer = LLVM.LLVMBuildLoad(builder, rttiHelper.getRTTIPointer(builder, parameter), "");
    TypeParameterAccessor nullAccessor = new TypeParameterAccessor(builder, rttiHelper);
    LLVMValueRef isBoolean = rttiHelper.buildTypeInfoCheck(builder, rttiPointer, baseType, nullAccessor, true, false);

    LLVMBasicBlockRef comparisonBlock = LLVM.LLVMAddBasicBlock(builder, "comparison");
    LLVM.LLVMBuildCondBr(builder, isBoolean, comparisonBlock, failureBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, comparisonBlock);
    LLVMValueRef booleanValue = typeHelper.convertTemporary(builder, landingPadContainer, parameter, objectType, baseType, false, nullAccessor, nullAccessor);
    LLVMValueRef equal = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, callee, booleanValue, "");
    LLVM.LLVMBuildRet(builder, equal);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildBooleanHashCode(PrimitiveType baseType, BuiltinMethod method)
  {
    if (baseType.getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
    {
      throw new IllegalArgumentException("A builtin boolean equals function must have the correct base type");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);
    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);

    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef result = LLVM.LLVMBuildSelect(builder, callee, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false), LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false), "");
    LLVM.LLVMBuildRet(builder, result);

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildSignedToString(PrimitiveType baseType, BuiltinMethod method, boolean radix)
  {
    if (baseType.getPrimitiveTypeType() != PrimitiveTypeType.BYTE &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.SHORT &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.INT &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.LONG)
    {
      throw new IllegalArgumentException("A builtin signed toString function must have the correct base type");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);
    Method valueOfMethod = radix ? SpecialTypeHandler.stringValueOfLongRadix : SpecialTypeHandler.stringValueOfLong;
    LLVMValueRef valueOfFunction = codeGenerator.getMethodFunction(valueOfMethod);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    TypeParameterAccessor typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper);

    // get the parameter and convert it to a long
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);
    parameter = typeHelper.convertTemporary(builder, landingPadContainer, parameter, baseType, valueOfMethod.getParameters()[0].getType(), false, typeParameterAccessor, typeParameterAccessor);

    LLVMValueRef[] arguments;
    if (radix)
    {
      LLVMValueRef radixParam = LLVM.LLVMGetParam(builtinFunction, 1);
      arguments = new LLVMValueRef[] {LLVM.LLVMConstNull(typeHelper.getOpaquePointer()), parameter, radixParam};
    }
    else
    {
      arguments = new LLVMValueRef[] {LLVM.LLVMConstNull(typeHelper.getOpaquePointer()), parameter};
    }
    LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "invokeContinue");
    LLVMValueRef result = LLVM.LLVMBuildInvoke(builder, valueOfFunction, C.toNativePointerArray(arguments, false, true), arguments.length, invokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);
    LLVM.LLVMBuildRet(builder, result);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildUnsignedToString(PrimitiveType baseType, BuiltinMethod method, boolean radix)
  {
    if (baseType.getPrimitiveTypeType() != PrimitiveTypeType.UBYTE &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.USHORT &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.UINT &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.ULONG)
    {
      throw new IllegalArgumentException("A builtin unsigned toString function must have the correct base type");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);
    Method valueOfMethod = radix ? SpecialTypeHandler.stringValueOfUlongRadix : SpecialTypeHandler.stringValueOfUlong;
    LLVMValueRef valueOfFunction = codeGenerator.getMethodFunction(valueOfMethod);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    TypeParameterAccessor typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper);

    // get the parameter and convert it to a long
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);
    parameter = typeHelper.convertTemporary(builder, landingPadContainer, parameter, baseType, valueOfMethod.getParameters()[0].getType(), false, typeParameterAccessor, typeParameterAccessor);

    LLVMValueRef[] arguments;
    if (radix)
    {
      LLVMValueRef radixParam = LLVM.LLVMGetParam(builtinFunction, 1);
      arguments = new LLVMValueRef[] {LLVM.LLVMConstNull(typeHelper.getOpaquePointer()), parameter, radixParam};
    }
    else
    {
      arguments = new LLVMValueRef[] {LLVM.LLVMConstNull(typeHelper.getOpaquePointer()), parameter};
    }
    LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "invokeContinue");
    LLVMValueRef result = LLVM.LLVMBuildInvoke(builder, valueOfFunction, C.toNativePointerArray(arguments, false, true), arguments.length, invokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);
    LLVM.LLVMBuildRet(builder, result);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildIntegerEquals(PrimitiveType baseType, BuiltinMethod method)
  {
    if (baseType.getPrimitiveTypeType() != PrimitiveTypeType.BYTE &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.SHORT &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.INT &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.LONG &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.UBYTE &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.USHORT &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.UINT &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.ULONG)
    {
      throw new IllegalArgumentException("A builtin integer equals() function must have the correct base type");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 1);

    ObjectType objectType = new ObjectType(true, true, null);
    parameter = typeHelper.convertStandardToTemporary(builder, parameter, objectType);

    LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, parameter, objectType);
    LLVMBasicBlockRef failureBlock = LLVM.LLVMAddBasicBlock(builder, "failure");
    LLVMBasicBlockRef notNullParameterBlock = LLVM.LLVMAddBasicBlock(builder, "notNullParameter");
    LLVM.LLVMBuildCondBr(builder, isNotNull, notNullParameterBlock, failureBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, failureBlock);
    LLVM.LLVMBuildRet(builder, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false));

    LLVM.LLVMPositionBuilderAtEnd(builder, notNullParameterBlock);
    LLVMValueRef rttiPointer = LLVM.LLVMBuildLoad(builder, rttiHelper.getRTTIPointer(builder, parameter), "");

    LLVMValueRef sortIdPtr = LLVM.LLVMBuildStructGEP(builder, rttiPointer, 1, "");
    LLVMValueRef sortIdValue = LLVM.LLVMBuildLoad(builder, sortIdPtr, "");
    LLVMValueRef sortMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, sortIdValue, LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), RTTIHelper.PRIMITIVE_SORT_ID, false), "");

    LLVMBasicBlockRef checkPrimitiveIdBlock = LLVM.LLVMAddBasicBlock(builder, "checkPrimitiveId");
    LLVM.LLVMBuildCondBr(builder, sortMatches, checkPrimitiveIdBlock, failureBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, checkPrimitiveIdBlock);
    // this assumes that all primitive types' RTTI blocks are represented the same way
    rttiPointer = LLVM.LLVMBuildBitCast(builder, rttiPointer, LLVM.LLVMPointerType(rttiHelper.getRTTIStructType(baseType), 0), "");
    LLVMValueRef primitiveIdPtr = LLVM.LLVMBuildStructGEP(builder, rttiPointer, 3, "");
    LLVMValueRef primitiveId = LLVM.LLVMBuildLoad(builder, primitiveIdPtr, "");
    TypeParameterAccessor nullAccessor = new TypeParameterAccessor(builder, rttiHelper);
    LLVMValueRef switchInstruction = LLVM.LLVMBuildSwitch(builder, primitiveId, failureBlock, 8);
    // the constant 8 is a hint as to how many cases there are, and is only used for efficient allocation (it doesn't have to be correct)

    for (PrimitiveTypeType primitive : PrimitiveTypeType.values())
    {
      if (primitive == PrimitiveTypeType.BOOLEAN || primitive.isFloating())
      {
        continue;
      }
      LLVMBasicBlockRef checkBlock = LLVM.LLVMAddBasicBlock(builder, primitive.name + "Check");
      LLVM.LLVMAddCase(switchInstruction, LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), primitive.getRunTimeId(), false), checkBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, checkBlock);
      PrimitiveType primitiveType = new PrimitiveType(false, primitive, null);
      LLVMValueRef convertedParameter = typeHelper.convertTemporary(builder, landingPadContainer, parameter, objectType, primitiveType, false, nullAccessor, nullAccessor);

      LLVMValueRef equal;
      if (baseType.canAssign(primitiveType))
      {
        LLVMValueRef left = callee;
        LLVMValueRef right = typeHelper.convertTemporary(builder, landingPadContainer, convertedParameter, primitiveType, baseType, false, nullAccessor, nullAccessor);
        equal = codeGenerator.buildEqualityCheck(builder, landingPadContainer, left, right, baseType, EqualityOperator.IDENTICALLY_EQUAL);
      }
      else if (primitiveType.canAssign(baseType))
      {
        LLVMValueRef left = typeHelper.convertTemporary(builder, landingPadContainer, callee, baseType, primitiveType, false, nullAccessor, nullAccessor);
        LLVMValueRef right = convertedParameter;
        equal = codeGenerator.buildEqualityCheck(builder, landingPadContainer, left, right, primitiveType, EqualityOperator.IDENTICALLY_EQUAL);
      }
      else
      {
        // neither can assign the other, so one of them must be signed and the other must be unsigned
        // convert them both to a (max bitLength + 1) bit integer, and do the comparison on that
        LLVMTypeRef comparisonType = LLVM.LLVMIntType(Math.max(baseType.getPrimitiveTypeType().getBitCount(), primitive.getBitCount()) + 1);
        LLVMValueRef left;
        LLVMValueRef right;
        if (primitive.isSigned())
        {
          left = LLVM.LLVMBuildZExt(builder, callee, comparisonType, "");
          right = LLVM.LLVMBuildSExt(builder, convertedParameter, comparisonType, "");
        }
        else
        {
          left = LLVM.LLVMBuildSExt(builder, callee, comparisonType, "");
          right = LLVM.LLVMBuildZExt(builder, convertedParameter, comparisonType, "");
        }
        equal = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, left, right, "");
      }
      LLVM.LLVMBuildRet(builder, equal);
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildIntegerHashCode(PrimitiveType baseType, BuiltinMethod method)
  {
    PrimitiveTypeType baseTypeType = baseType.getPrimitiveTypeType();
    if (baseTypeType != PrimitiveTypeType.BYTE &&
        baseTypeType != PrimitiveTypeType.SHORT &&
        baseTypeType != PrimitiveTypeType.INT &&
        baseTypeType != PrimitiveTypeType.LONG &&
        baseTypeType != PrimitiveTypeType.UBYTE &&
        baseTypeType != PrimitiveTypeType.USHORT &&
        baseTypeType != PrimitiveTypeType.UINT &&
        baseTypeType != PrimitiveTypeType.ULONG)
    {
      throw new IllegalArgumentException("A builtin integer hashCode() function must have the correct base type");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);
    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);

    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);
    if (baseTypeType.getBitCount() == PrimitiveTypeType.UINT.getBitCount())
    {
      LLVM.LLVMBuildRet(builder, callee);
    }
    else if (baseTypeType.getBitCount() < PrimitiveTypeType.UINT.getBitCount())
    {
      LLVMValueRef result = LLVM.LLVMBuildZExt(builder, callee, LLVM.LLVMInt32Type(), "");
      LLVM.LLVMBuildRet(builder, result);
    }
    else
    {
      // this is either a long or a ulong, so it has 64 bits
      LLVMValueRef bottomHalf = LLVM.LLVMBuildTrunc(builder, callee, LLVM.LLVMInt32Type(), "");
      LLVMValueRef topHalf = LLVM.LLVMBuildLShr(builder, callee, LLVM.LLVMConstInt(LLVM.LLVMInt64Type(), 32, false), "");
      topHalf = LLVM.LLVMBuildTrunc(builder, topHalf, LLVM.LLVMInt32Type(), "");
      // result = 33 * topHalf + bottomHalf = topHalf << 5 + topHalf + bottomHalf
      LLVMValueRef shiftedTopHalf = LLVM.LLVMBuildShl(builder, topHalf, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false), "");
      LLVMValueRef result = LLVM.LLVMBuildAdd(builder, shiftedTopHalf, topHalf, "");
      result = LLVM.LLVMBuildAdd(builder, result, bottomHalf, "");
      LLVM.LLVMBuildRet(builder, result);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildFloatingToString(PrimitiveType baseType, BuiltinMethod method)
  {
    if (baseType.getPrimitiveTypeType() != PrimitiveTypeType.FLOAT &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.DOUBLE)
    {
      throw new IllegalArgumentException("A builtin floating toString function must have the correct base type");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);
    Method valueOfMethod = baseType.getPrimitiveTypeType() == PrimitiveTypeType.FLOAT ? SpecialTypeHandler.stringValueOfFloat : SpecialTypeHandler.stringValueOfDouble;
    LLVMValueRef valueOfFunction = codeGenerator.getMethodFunction(valueOfMethod);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef[] arguments = new LLVMValueRef[] {LLVM.LLVMConstNull(typeHelper.getOpaquePointer()), parameter};
    LLVMBasicBlockRef landingPadBlock = LLVM.LLVMAppendBasicBlock(LLVM.LLVMGetBasicBlockParent(LLVM.LLVMGetInsertBlock(builder)), "landingPad");
    LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "invokeContinue");
    LLVMValueRef result = LLVM.LLVMBuildInvoke(builder, valueOfFunction, C.toNativePointerArray(arguments, false, true), arguments.length, invokeContinueBlock, landingPadBlock, "");
    LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);
    LLVM.LLVMBuildRet(builder, result);

    LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
    LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
    LLVM.LLVMSetCleanup(landingPad, true);
    LLVM.LLVMBuildResume(builder, landingPad);

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildFloatingEquals(PrimitiveType baseType, BuiltinMethod method)
  {
    if (baseType.getPrimitiveTypeType() != PrimitiveTypeType.FLOAT &&
        baseType.getPrimitiveTypeType() != PrimitiveTypeType.DOUBLE)
    {
      throw new IllegalArgumentException("A builtin floating equals function must have the correct base type");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 1);

    ObjectType objectType = new ObjectType(true, true, null);
    parameter = typeHelper.convertStandardToTemporary(builder, parameter, objectType);

    LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, parameter, objectType);
    LLVMBasicBlockRef failureBlock = LLVM.LLVMAddBasicBlock(builder, "failure");
    LLVMBasicBlockRef notNullParameterBlock = LLVM.LLVMAddBasicBlock(builder, "notNullParameter");
    LLVM.LLVMBuildCondBr(builder, isNotNull, notNullParameterBlock, failureBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, failureBlock);
    LLVM.LLVMBuildRet(builder, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false));

    LLVM.LLVMPositionBuilderAtEnd(builder, notNullParameterBlock);
    LLVMValueRef rttiPointer = LLVM.LLVMBuildLoad(builder, rttiHelper.getRTTIPointer(builder, parameter), "");

    LLVMValueRef sortIdPtr = LLVM.LLVMBuildStructGEP(builder, rttiPointer, 1, "");
    LLVMValueRef sortIdValue = LLVM.LLVMBuildLoad(builder, sortIdPtr, "");
    LLVMValueRef sortMatches = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, sortIdValue, LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), RTTIHelper.PRIMITIVE_SORT_ID, false), "");

    LLVMBasicBlockRef checkPrimitiveIdBlock = LLVM.LLVMAddBasicBlock(builder, "checkPrimitiveId");
    LLVM.LLVMBuildCondBr(builder, sortMatches, checkPrimitiveIdBlock, failureBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, checkPrimitiveIdBlock);
    // this assumes that all primitive types' RTTI blocks are represented the same way
    rttiPointer = LLVM.LLVMBuildBitCast(builder, rttiPointer, LLVM.LLVMPointerType(rttiHelper.getRTTIStructType(baseType), 0), "");
    LLVMValueRef primitiveIdPtr = LLVM.LLVMBuildStructGEP(builder, rttiPointer, 3, "");
    LLVMValueRef primitiveId = LLVM.LLVMBuildLoad(builder, primitiveIdPtr, "");

    TypeParameterAccessor nullAccessor = new TypeParameterAccessor(builder, rttiHelper);
    LLVMBasicBlockRef doubleCheckBlock = LLVM.LLVMAddBasicBlock(builder, "doubleCheck");
    LLVMBasicBlockRef floatCheckBlock = LLVM.LLVMAddBasicBlock(builder, "floatCheck");
    LLVMValueRef switchInstruction = LLVM.LLVMBuildSwitch(builder, primitiveId, failureBlock, 2);
    LLVM.LLVMAddCase(switchInstruction, LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), PrimitiveTypeType.FLOAT.getRunTimeId(), false), floatCheckBlock);
    LLVM.LLVMAddCase(switchInstruction, LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), PrimitiveTypeType.DOUBLE.getRunTimeId(), false), doubleCheckBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, floatCheckBlock);
    PrimitiveType floatType = new PrimitiveType(false, PrimitiveTypeType.FLOAT, null);
    LLVMValueRef convertedFloatValue = typeHelper.convertTemporary(builder, landingPadContainer, parameter, objectType, floatType, false, nullAccessor, nullAccessor);
    if (baseType.getPrimitiveTypeType() == PrimitiveTypeType.FLOAT)
    {
      LLVMValueRef left = callee;
      LLVMValueRef right = convertedFloatValue;
      LLVMValueRef equal = LLVM.LLVMBuildFCmp(builder, LLVM.LLVMRealPredicate.LLVMRealOEQ, left, right, "");
      LLVM.LLVMBuildRet(builder, equal);
    }
    else
    {
      LLVMValueRef left = callee;
      LLVMValueRef right = typeHelper.convertTemporary(builder, landingPadContainer, convertedFloatValue, floatType, baseType, false, nullAccessor, nullAccessor);
      LLVMValueRef equal = LLVM.LLVMBuildFCmp(builder, LLVM.LLVMRealPredicate.LLVMRealOEQ, left, right, "");
      LLVM.LLVMBuildRet(builder, equal);
    }

    LLVM.LLVMPositionBuilderAtEnd(builder, doubleCheckBlock);
    PrimitiveType doubleType = new PrimitiveType(false, PrimitiveTypeType.DOUBLE, null);
    LLVMValueRef convertedDoubleValue = typeHelper.convertTemporary(builder, landingPadContainer, parameter, objectType, doubleType, false, nullAccessor, nullAccessor);
    if (baseType.getPrimitiveTypeType() == PrimitiveTypeType.FLOAT)
    {
      LLVMValueRef left = typeHelper.convertTemporary(builder, landingPadContainer, callee, baseType, doubleType, false, nullAccessor, nullAccessor);
      LLVMValueRef right = convertedDoubleValue;
      LLVMValueRef equal = LLVM.LLVMBuildFCmp(builder, LLVM.LLVMRealPredicate.LLVMRealOEQ, left, right, "");
      LLVM.LLVMBuildRet(builder, equal);
    }
    else
    {
      LLVMValueRef left = callee;
      LLVMValueRef right = convertedDoubleValue;
      LLVMValueRef equal = LLVM.LLVMBuildFCmp(builder, LLVM.LLVMRealPredicate.LLVMRealOEQ, left, right, "");
      LLVM.LLVMBuildRet(builder, equal);
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildFloatingHashCode(PrimitiveType baseType, BuiltinMethod method)
  {
    PrimitiveTypeType baseTypeType = baseType.getPrimitiveTypeType();
    if (baseTypeType != PrimitiveTypeType.FLOAT &&
        baseTypeType != PrimitiveTypeType.DOUBLE)
    {
      throw new IllegalArgumentException("A builtin floating hashCode() function must have the correct base type");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);
    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);

    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);

    // always work with doubles, so that float hashCode()s equal double hashCode()s given the same floating point values
    LLVMValueRef doubleValue = callee;
    if (baseTypeType == PrimitiveTypeType.FLOAT)
    {
      doubleValue = LLVM.LLVMBuildFPCast(builder, callee, LLVM.LLVMDoubleType(), "");
    }
    LLVMValueRef longValue = LLVM.LLVMBuildBitCast(builder, doubleValue, LLVM.LLVMInt64Type(), "");
    LLVMValueRef bottomHalf = LLVM.LLVMBuildTrunc(builder, longValue, LLVM.LLVMInt32Type(), "");
    LLVMValueRef topHalf = LLVM.LLVMBuildLShr(builder, longValue, LLVM.LLVMConstInt(LLVM.LLVMInt64Type(), 32, false), "");
    topHalf = LLVM.LLVMBuildTrunc(builder, topHalf, LLVM.LLVMInt32Type(), "");
    // result = 33 * topHalf + bottomHalf = topHalf << 5 + topHalf + bottomHalf
    LLVMValueRef shiftedTopHalf = LLVM.LLVMBuildShl(builder, topHalf, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false), "");
    LLVMValueRef result = LLVM.LLVMBuildAdd(builder, shiftedTopHalf, topHalf, "");
    result = LLVM.LLVMBuildAdd(builder, result, bottomHalf, "");
    LLVM.LLVMBuildRet(builder, result);

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildObjectToString(Type baseType, BuiltinMethod method)
  {
    if (!(baseType instanceof ObjectType || method.getContainingTypeDefinition() instanceof CompoundDefinition))
    {
      throw new IllegalArgumentException("A builtin object toString function must have either an object base type or be part of a compound definition");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);


    LLVMValueRef integerValue = LLVM.LLVMBuildPtrToInt(builder, parameter, LLVM.LLVMInt64Type(), "");
    Type ulongType = new PrimitiveType(false, PrimitiveTypeType.ULONG, null);
    MethodReference ulongToStringMethodReference = new MethodReference(new BuiltinMethod(ulongType, BuiltinMethodType.TO_STRING_RADIX), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
    Method toStringMethod = ulongType.getMethod(ulongToStringMethodReference.getDisambiguator()).getReferencedMember();
    LLVMValueRef ulongToStringFunction = codeGenerator.getMethodFunction(toStringMethod);

    LLVMValueRef[] ulongToStringArguments = new LLVMValueRef[] {integerValue, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 16, false)};
    LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "invokeContinue");
    LLVMValueRef pointerString = LLVM.LLVMBuildInvoke(builder, ulongToStringFunction, C.toNativePointerArray(ulongToStringArguments, false, true), ulongToStringArguments.length, invokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);

    LLVMValueRef startString;
    // for compound types, we can hard-code the start string, but for object and class types, we must extract it from the run-time type information
    TypeDefinition containingDefinition = method.getContainingTypeDefinition();
    if (containingDefinition instanceof CompoundDefinition)
    {
      // TODO: add the run-time types of the generic type arguments to the string somehow
      String prefixString = "[" + (containingDefinition instanceof CompoundDefinition ? ((CompoundDefinition) containingDefinition).getQualifiedName() : "object") + "@";
      startString = codeGenerator.buildStringCreation(builder, landingPadContainer, prefixString);
      startString = typeHelper.convertTemporaryToStandard(builder, startString, SpecialTypeHandler.STRING_TYPE);
    }
    else
    {
      // use the run-time type information to find the real type
      LLVMValueRef rttiPointer = rttiHelper.getRTTIPointer(builder, parameter);
      LLVMValueRef rtti = LLVM.LLVMBuildLoad(builder, rttiPointer, "");
      LLVMValueRef sortIdPointer = LLVM.LLVMBuildStructGEP(builder, rtti, 1, "");
      LLVMValueRef sortId = LLVM.LLVMBuildLoad(builder, sortIdPointer, "");
      LLVMValueRef isObject = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, sortId, LLVM.LLVMConstInt(LLVM.LLVMInt8Type(), RTTIHelper.OBJECT_SORT_ID, false), "");

      LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "continuation");
      LLVMBasicBlockRef isNotObjectBlock = LLVM.LLVMAddBasicBlock(builder, "extractClassName");
      LLVMBasicBlockRef isObjectBlock = LLVM.LLVMAddBasicBlock(builder, "generateObjectString");

      LLVM.LLVMBuildCondBr(builder, isObject, isObjectBlock, isNotObjectBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, isObjectBlock);
      LLVMValueRef objectStartString = codeGenerator.buildStringCreation(builder, landingPadContainer, "[object@");
      objectStartString = typeHelper.convertTemporaryToStandard(builder, objectStartString, SpecialTypeHandler.STRING_TYPE);
      LLVMBasicBlockRef endIsObjectBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, isNotObjectBlock);

      // look up the name of this type inside the RTTI block
      LLVMValueRef classQualifiedNameUbyteArray = rttiHelper.lookupNamedTypeName(builder, rtti);

      // TODO: add the run-time types of the generic type arguments to the string somehow
      LLVMValueRef classStringPrefix = codeGenerator.buildStringCreation(builder, landingPadContainer, "[");
      LLVMValueRef classQualifiedNameString = codeGenerator.buildStringCreation(builder, landingPadContainer, classQualifiedNameUbyteArray);
      LLVMValueRef classStringSuffix = codeGenerator.buildStringCreation(builder, landingPadContainer, "@");
      classStringPrefix = typeHelper.convertTemporaryToStandard(builder, classStringPrefix, SpecialTypeHandler.STRING_TYPE);
      classQualifiedNameString = typeHelper.convertTemporaryToStandard(builder, classQualifiedNameString, SpecialTypeHandler.STRING_TYPE);
      classStringSuffix = typeHelper.convertTemporaryToStandard(builder, classStringSuffix, SpecialTypeHandler.STRING_TYPE);

      LLVMValueRef classStartString = codeGenerator.buildStringConcatenation(builder, landingPadContainer, classStringPrefix, classQualifiedNameString, classStringSuffix);
      classStartString = typeHelper.convertTemporaryToStandard(builder, classStartString, SpecialTypeHandler.STRING_TYPE);
      LLVMBasicBlockRef endIsNotObjectBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
      startString = LLVM.LLVMBuildPhi(builder, typeHelper.findStandardType(SpecialTypeHandler.STRING_TYPE), "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {objectStartString, classStartString};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {endIsObjectBlock, endIsNotObjectBlock};
      LLVM.LLVMAddIncoming(startString, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
    }

    String suffixString = "]";
    LLVMValueRef endString = codeGenerator.buildStringCreation(builder, landingPadContainer, suffixString);
    endString = typeHelper.convertTemporaryToStandard(builder, endString, SpecialTypeHandler.STRING_TYPE);

    LLVMValueRef completeString = codeGenerator.buildStringConcatenation(builder, landingPadContainer, startString, pointerString, endString);
    completeString = typeHelper.convertTemporaryToStandard(builder, completeString, SpecialTypeHandler.STRING_TYPE);
    LLVM.LLVMBuildRet(builder, completeString);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildObjectEquals(Type baseType, BuiltinMethod method)
  {
    if (!(baseType instanceof ObjectType || method.getContainingTypeDefinition() instanceof CompoundDefinition))
    {
      throw new IllegalArgumentException("A builtin object equals() function must have either an object base type or be part of a compound definition");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 1);

    ObjectType objectType = new ObjectType(true, true, null);
    parameter = typeHelper.convertStandardToTemporary(builder, parameter, objectType);

    LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, parameter, objectType);
    LLVMBasicBlockRef failureBlock = LLVM.LLVMAddBasicBlock(builder, "failure");
    LLVMBasicBlockRef notNullParameterBlock = LLVM.LLVMAddBasicBlock(builder, "notNullParameter");
    LLVM.LLVMBuildCondBr(builder, isNotNull, notNullParameterBlock, failureBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, failureBlock);
    LLVM.LLVMBuildRet(builder, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false));

    LLVM.LLVMPositionBuilderAtEnd(builder, notNullParameterBlock);

    TypeDefinition containingDefinition = method.getContainingTypeDefinition();
    if (containingDefinition instanceof CompoundDefinition)
    {
      Type checkType = new NamedType(false, true, true, containingDefinition);
      TypeParameterAccessor calleeAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, containingDefinition, callee);
      TypeParameterAccessor nullAccessor = new TypeParameterAccessor(builder, rttiHelper);
      LLVMValueRef isInstanceOfCompound = rttiHelper.buildInstanceOfCheck(builder, landingPadContainer, parameter, new ObjectType(false, true, null), checkType, nullAccessor, calleeAccessor);
      LLVMBasicBlockRef checkBlock = LLVM.LLVMAddBasicBlock(builder, "checkCompound");
      LLVM.LLVMBuildCondBr(builder, isInstanceOfCompound, checkBlock, failureBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, checkBlock);
      LLVMValueRef convertedValue = typeHelper.convertTemporary(builder, landingPadContainer, parameter, objectType, checkType, false, nullAccessor, calleeAccessor);

      // compare each of the member variables from the left and right values
      // this cannot be done using buildEqualityCheck(), as using EqualityOperator.EQUAL on a compound
      // type just calls the equals() method on that compound type, which would just recurse
      MemberVariable[] memberVariables = ((CompoundDefinition) containingDefinition).getMemberVariables();
      LLVMValueRef[] compareResults = new LLVMValueRef[memberVariables.length];
      for (int i = 0; i < memberVariables.length; ++i)
      {
        Type variableType = memberVariables[i].getType();
        LLVMValueRef leftField = typeHelper.getMemberPointer(builder, callee, memberVariables[i]);
        LLVMValueRef rightField = typeHelper.getMemberPointer(builder, convertedValue, memberVariables[i]);
        LLVMValueRef leftValue = typeHelper.convertStandardPointerToTemporary(builder, leftField, variableType);
        LLVMValueRef rightValue = typeHelper.convertStandardPointerToTemporary(builder, rightField, variableType);
        compareResults[i] = codeGenerator.buildEqualityCheck(builder, landingPadContainer, leftValue, rightValue, variableType, EqualityOperator.EQUAL);
      }

      // AND the list together, using a binary tree
      int multiple = 1;
      while (multiple < memberVariables.length)
      {
        for (int i = 0; i < memberVariables.length; i += 2 * multiple)
        {
          LLVMValueRef first = compareResults[i];
          if (i + multiple >= memberVariables.length)
          {
            continue;
          }
          LLVMValueRef second = compareResults[i + multiple];
          compareResults[i] = LLVM.LLVMBuildAnd(builder, first, second, "");
        }
        multiple *= 2;
      }
      LLVMValueRef equal;
      if (memberVariables.length == 0)
      {
        equal = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
      }
      else
      {
        equal = compareResults[0];
      }

      LLVM.LLVMBuildRet(builder, equal);
    }
    else
    {
      // note: objectType is ?#object, so we implicitly cast callee to nullable first here
      LLVMValueRef equal = codeGenerator.buildEqualityCheck(builder, landingPadContainer, callee, parameter, objectType, EqualityOperator.IDENTICALLY_EQUAL);
      LLVM.LLVMBuildRet(builder, equal);
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildObjectHashCode(Type baseType, BuiltinMethod method)
  {
    if (!(baseType instanceof ObjectType || method.getContainingTypeDefinition() instanceof CompoundDefinition))
    {
      throw new IllegalArgumentException("A builtin object hashCode() function must have either an object base type or be part of a compound definition");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);

    TypeDefinition containingDefinition = method.getContainingTypeDefinition();
    if (containingDefinition instanceof CompoundDefinition)
    {
      TypeParameterAccessor typeParameterAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, containingDefinition, callee);
      LLVMValueRef result = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false);
      for (MemberVariable memberVariable : ((CompoundDefinition) containingDefinition).getMemberVariables())
      {
        // call hashCode() on the member variable
        Type memberType = memberVariable.getType();
        LLVMValueRef memberPtr = typeHelper.getMemberPointer(builder, callee, memberVariable);
        LLVMValueRef memberValue = typeHelper.convertStandardPointerToTemporary(builder, memberPtr, memberType);

        Type notNullMemberType = memberType;
        LLVMBasicBlockRef startBlock = null;
        LLVMBasicBlockRef continuationBlock = null;
        if (memberType.canBeNullable())
        {
          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, memberValue, memberType);
          LLVMBasicBlockRef hashCodeBlock = LLVM.LLVMAddBasicBlock(builder, "notNullMember");
          continuationBlock = LLVM.LLVMAddBasicBlock(builder, "nullCheckContinuation");
          startBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildCondBr(builder, isNotNull, hashCodeBlock, continuationBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, hashCodeBlock);
          notNullMemberType = Type.findTypeWithNullability(memberType, false);
          memberValue = typeHelper.convertTemporary(builder, landingPadContainer, memberValue, memberType, notNullMemberType, true, typeParameterAccessor, typeParameterAccessor);
        }

        Disambiguator hashCodeDisambiguator = new MethodReference(new BuiltinMethod(new ObjectType(false, false, null), BuiltinMethodType.HASH_CODE), GenericTypeSpecialiser.IDENTITY_SPECIALISER).getDisambiguator();
        MethodReference hashCodeMethodReference = notNullMemberType.getMethod(hashCodeDisambiguator);
        if (hashCodeMethodReference == null)
        {
          throw new IllegalArgumentException("Could not find the hashCode() method on " + notNullMemberType);
        }
        LLVMValueRef memberHashCode = typeHelper.buildMethodCall(builder, landingPadContainer, memberValue, notNullMemberType, hashCodeMethodReference, new HashMap<Parameter, LLVMValueRef>(), typeParameterAccessor);

        if (memberType.canBeNullable())
        {
          LLVMBasicBlockRef endHashCodeBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildBr(builder, continuationBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
          LLVMValueRef phiNode = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt32Type(), "");
          LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false), memberHashCode};
          LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endHashCodeBlock};
          LLVM.LLVMAddIncoming(phiNode, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);

          memberHashCode = phiNode;
        }

        // result = result << 5 + result + memberHashCode
        LLVMValueRef shiftedResult = LLVM.LLVMBuildShl(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false), "");
        result = LLVM.LLVMBuildAdd(builder, shiftedResult, result, "");
        result = LLVM.LLVMBuildAdd(builder, result, memberHashCode, "");
      }
      LLVM.LLVMBuildRet(builder, result);
    }
    else
    {
      // this is an object, so cast its pointer to an integer to derive the hash code
      LLVMValueRef longValue = LLVM.LLVMBuildPtrToInt(builder, callee, LLVM.LLVMInt64Type(), "");
      LLVMValueRef bottomHalf = LLVM.LLVMBuildTrunc(builder, longValue, LLVM.LLVMInt32Type(), "");
      LLVMValueRef topHalf = LLVM.LLVMBuildLShr(builder, longValue, LLVM.LLVMConstInt(LLVM.LLVMInt64Type(), 32, false), "");
      topHalf = LLVM.LLVMBuildTrunc(builder, topHalf, LLVM.LLVMInt32Type(), "");
      // result = 33 * topHalf + bottomHalf = topHalf << 5 + topHalf + bottomHalf
      LLVMValueRef shiftedTopHalf = LLVM.LLVMBuildShl(builder, topHalf, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false), "");
      LLVMValueRef result = LLVM.LLVMBuildAdd(builder, shiftedTopHalf, topHalf, "");
      result = LLVM.LLVMBuildAdd(builder, result, bottomHalf, "");
      LLVM.LLVMBuildRet(builder, result);
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildArrayToString(ArrayType arrayType, BuiltinMethod method)
  {
    Type baseType = arrayType.getBaseType();

    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    TypeParameterAccessor typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper);

    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef array = typeHelper.convertStandardToTemporary(builder, parameter, arrayType);

    LLVMValueRef lengthPointer = typeHelper.getArrayLengthPointer(builder, array);
    LLVMValueRef length = LLVM.LLVMBuildLoad(builder, lengthPointer, "");

    LLVMValueRef llvmStartString = codeGenerator.buildStringCreation(builder, landingPadContainer, "[");

    LLVMBasicBlockRef exitBlock = LLVM.LLVMAddBasicBlock(builder, "exit");
    LLVMBasicBlockRef addCommaBlock = LLVM.LLVMAddBasicBlock(builder, "appendComma");
    LLVMBasicBlockRef loopBlock = LLVM.LLVMAddBasicBlock(builder, "elementLoop");

    // go straight to the exit block if the array has a zero length
    LLVMValueRef zeroLengthCheck = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntEQ, length, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false), "");
    LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVM.LLVMBuildCondBr(builder, zeroLengthCheck, exitBlock, loopBlock);

    // build the main loop block
    LLVM.LLVMPositionBuilderAtEnd(builder, loopBlock);
    LLVMValueRef stringPhi = LLVM.LLVMBuildPhi(builder, typeHelper.findTemporaryType(SpecialTypeHandler.STRING_TYPE), "");
    LLVMValueRef indexPhi = LLVM.LLVMBuildPhi(builder, LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), "");

    LLVMValueRef element = typeHelper.buildRetrieveArrayElement(builder, landingPadContainer, array, indexPhi);

    // build the string conversion
    Type notNullBaseType = Type.findTypeWithNullability(baseType, false);
    LLVMValueRef elementString;
    LLVMValueRef notNullElement = element;
    LLVMBasicBlockRef nullBlock = null;
    LLVMBasicBlockRef continuationBlock = null;
    if (baseType.canBeNullable())
    {
      LLVMValueRef nullCheckResult = codeGenerator.buildNullCheck(builder, element, baseType);
      continuationBlock = LLVM.LLVMAddBasicBlock(builder, "continuation");
      nullBlock = LLVM.LLVMAddBasicBlock(builder, "nullGeneration");
      LLVMBasicBlockRef notNullBlock = LLVM.LLVMAddBasicBlock(builder, "toStringCall");
      LLVM.LLVMBuildCondBr(builder, nullCheckResult, notNullBlock, nullBlock);
      LLVM.LLVMPositionBuilderAtEnd(builder, notNullBlock);
      // skip runtime checks on this nullable -> not-nullable conversion, as we have just done them (see: nullCheckResult)
      notNullElement = typeHelper.convertTemporary(builder, landingPadContainer, element, baseType, notNullBaseType, true, typeParameterAccessor, typeParameterAccessor);
    }
    // we shouldn't usually create BuiltinMethods like this, as they might need to have a method index set depending on the base type
    // but it's fine in this case, because we're just using it to get a Disambiguator
    Disambiguator toStringMethodDisambiguator = new MethodReference(new BuiltinMethod(notNullBaseType, BuiltinMethodType.TO_STRING), GenericTypeSpecialiser.IDENTITY_SPECIALISER).getDisambiguator();
    MethodReference toStringMethod = notNullBaseType.getMethod(toStringMethodDisambiguator);
    LLVMValueRef notNullElementString = typeHelper.buildMethodCall(builder, landingPadContainer, notNullElement, notNullBaseType, toStringMethod, new HashMap<Parameter, LLVM.LLVMValueRef>(), typeParameterAccessor);
    notNullElementString = typeHelper.convertTemporaryToStandard(builder, notNullElementString, SpecialTypeHandler.STRING_TYPE);

    if (baseType.canBeNullable())
    {
      LLVMBasicBlockRef endToStringCallBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, nullBlock);
      LLVMValueRef llvmNullString = codeGenerator.buildStringCreation(builder, landingPadContainer, "null");
      llvmNullString = typeHelper.convertTemporaryToStandard(builder, llvmNullString, SpecialTypeHandler.STRING_TYPE);
      LLVMBasicBlockRef endNullBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
      elementString = LLVM.LLVMBuildPhi(builder, typeHelper.findStandardType(SpecialTypeHandler.STRING_TYPE), "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {notNullElementString, llvmNullString};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {endToStringCallBlock, endNullBlock};
      LLVM.LLVMAddIncoming(elementString, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
    }
    else
    {
      elementString = notNullElementString;
    }

    // concatenate the string so far (from stringPhi) with the new string
    LLVMValueRef[] concatenationValues = new LLVMValueRef[] {typeHelper.convertTemporaryToStandard(builder, stringPhi, SpecialTypeHandler.STRING_TYPE),
                                                             elementString};
    LLVMValueRef currentString = codeGenerator.buildStringConcatenation(builder, landingPadContainer, concatenationValues);

    // check whether this is the last element
    LLVMValueRef incIndex = LLVM.LLVMBuildAdd(builder, indexPhi, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false), "");
    LLVMValueRef loopCheck = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntULT, incIndex, length, "");
    LLVMBasicBlockRef endLoopBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVM.LLVMBuildCondBr(builder, loopCheck, addCommaBlock, exitBlock);

    // build the add comma block
    LLVM.LLVMPositionBuilderAtEnd(builder, addCommaBlock);
    LLVMValueRef llvmCommaString = codeGenerator.buildStringCreation(builder, landingPadContainer, ", ");
    LLVMValueRef[] commaConcatenationValues = new LLVMValueRef[] {typeHelper.convertTemporaryToStandard(builder, currentString, SpecialTypeHandler.STRING_TYPE),
                                                                  typeHelper.convertTemporaryToStandard(builder, llvmCommaString, SpecialTypeHandler.STRING_TYPE)};
    LLVMValueRef nextString = codeGenerator.buildStringConcatenation(builder, landingPadContainer, commaConcatenationValues);
    LLVMBasicBlockRef endCommaBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVM.LLVMBuildBr(builder, loopBlock);

    // add incoming values to the loop phi nodes
    LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endCommaBlock};
    LLVMValueRef[] indexIncomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false), incIndex};
    LLVM.LLVMAddIncoming(indexPhi, C.toNativePointerArray(indexIncomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), indexIncomingValues.length);
    LLVMValueRef[] stringIncomingValues = new LLVMValueRef[] {llvmStartString, nextString};
    LLVM.LLVMAddIncoming(stringPhi, C.toNativePointerArray(stringIncomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), stringIncomingValues.length);

    // build the exit block
    LLVM.LLVMPositionBuilderAtEnd(builder, exitBlock);
    LLVMValueRef exitStringPhi = LLVM.LLVMBuildPhi(builder, typeHelper.findTemporaryType(SpecialTypeHandler.STRING_TYPE), "");
    LLVMBasicBlockRef[] exitIncomingBlocks = new LLVMBasicBlockRef[] {startBlock, endLoopBlock};
    LLVMValueRef[] exitIncomingValues = new LLVMValueRef[] {llvmStartString, currentString};
    LLVM.LLVMAddIncoming(exitStringPhi, C.toNativePointerArray(exitIncomingValues, false, true), C.toNativePointerArray(exitIncomingBlocks, false, true), exitIncomingValues.length);

    // do the final concatenation, and return the result
    LLVMValueRef llvmEndString = codeGenerator.buildStringCreation(builder, landingPadContainer, "]");
    LLVMValueRef[] finalConcatenationValues = new LLVMValueRef[] {typeHelper.convertTemporaryToStandard(builder, exitStringPhi, SpecialTypeHandler.STRING_TYPE),
                                                                  typeHelper.convertTemporaryToStandard(builder, llvmEndString, SpecialTypeHandler.STRING_TYPE)};
    LLVMValueRef result = codeGenerator.buildStringConcatenation(builder, landingPadContainer, finalConcatenationValues);
    result = typeHelper.convertTemporaryToStandard(builder, result, SpecialTypeHandler.STRING_TYPE);
    LLVM.LLVMBuildRet(builder, result);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildArrayEquals(ArrayType arrayType, BuiltinMethod method)
  {
    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 1);

    ObjectType objectType = new ObjectType(true, true, null);
    parameter = typeHelper.convertStandardToTemporary(builder, parameter, objectType);

    LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, parameter, objectType);
    LLVMBasicBlockRef failureBlock = LLVM.LLVMAddBasicBlock(builder, "failure");
    LLVMBasicBlockRef notNullParameterBlock = LLVM.LLVMAddBasicBlock(builder, "notNullParameter");
    LLVM.LLVMBuildCondBr(builder, isNotNull, notNullParameterBlock, failureBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, failureBlock);
    LLVM.LLVMBuildRet(builder, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false));

    LLVM.LLVMPositionBuilderAtEnd(builder, notNullParameterBlock);

    LLVMValueRef parameterRTTI = LLVM.LLVMBuildLoad(builder, rttiHelper.getRTTIPointer(builder, parameter), "");
    LLVMValueRef isInstanceOfArray = rttiHelper.buildTypeInfoCheck(builder, parameterRTTI, arrayType, null, true, false);

    LLVMBasicBlockRef arrayCheckBlock = LLVM.LLVMAddBasicBlock(builder, "arrayCheck");
    LLVM.LLVMBuildCondBr(builder, isInstanceOfArray, arrayCheckBlock, failureBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, arrayCheckBlock);
    // convert the other array, but don't use typeHelper.convertTemporary(), as it might look for the values of type parameters, which we don't have here
    // we already know that the other array has the same representation as this one does (since the RTTI matches exactly), so the bitcast must be safe
    LLVMValueRef convertedParameter = LLVM.LLVMBuildBitCast(builder, parameter, typeHelper.findTemporaryType(arrayType), "");
    LLVMValueRef equal = codeGenerator.buildEqualityCheck(builder, landingPadContainer, callee, convertedParameter, arrayType, EqualityOperator.EQUAL);
    LLVM.LLVMBuildRet(builder, equal);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildArrayHashCode(ArrayType arrayType, BuiltinMethod method)
  {
    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);

    LLVMValueRef lengthPtr = typeHelper.getArrayLengthPointer(builder, callee);
    LLVMValueRef length = LLVM.LLVMBuildLoad(builder, lengthPtr, "");

    LLVMValueRef hasElements = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntNE, length, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false), "");
    LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "elementHashCodeContinuation");
    LLVMBasicBlockRef hasElementsBlock = LLVM.LLVMAddBasicBlock(builder, "findFirstElementHashCode");
    LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVM.LLVMBuildCondBr(builder, hasElements, hasElementsBlock, continuationBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, hasElementsBlock);
    LLVMValueRef firstElement = typeHelper.buildRetrieveArrayElement(builder, landingPadContainer, callee, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false));

    Type elementType = arrayType.getBaseType();
    Type notNullType = elementType;
    LLVMValueRef notNullValue = firstElement;
    LLVMBasicBlockRef nullCheckBlock = null;
    if (elementType.canBeNullable())
    {
      LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, firstElement, elementType);
      LLVMBasicBlockRef notNullBlock = LLVM.LLVMAddBasicBlock(builder, "elementNotNull");
      nullCheckBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildCondBr(builder, isNotNull, notNullBlock, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, notNullBlock);
      notNullType = Type.findTypeWithNullability(elementType, false);
      // skip checks on this conversion, since we have already done them
      // also, use null TypeParameterAccessors, since we don't have our own and we're only converting from nullable to not-null
      notNullValue = typeHelper.convertTemporary(builder, landingPadContainer, firstElement, elementType, notNullType, true, null, null);
    }

    // we don't have a TypeParameterAccessor for any type parameters in this base type, so we have
    // to be careful about how we call the hashCode() method on this element
    TypeParameterAccessor elementTypeParameterAccessor = null;
    if ((notNullType instanceof ObjectType) ||
        (notNullType instanceof NamedType && ((NamedType) notNullType).getResolvedTypeParameter() != null) ||
        (notNullType instanceof NamedType && ((NamedType) notNullType).getResolvedTypeDefinition() instanceof ClassDefinition) ||
        (notNullType instanceof NamedType && ((NamedType) notNullType).getResolvedTypeDefinition() instanceof InterfaceDefinition) ||
        (notNullType instanceof WildcardType))
    {
      // this can be converted to object without a heap allocation, so do that and then just call its object::hashCode() method
      // this way of calling the hashCode() method avoids trying to convert from notNullType to an interface which we don't know the type parameters for, which would be impossible
      ObjectType objectType = new ObjectType(false, Type.canBeExplicitlyDataImmutable(notNullType), null);
      notNullValue = typeHelper.convertTemporary(builder, landingPadContainer, notNullValue, notNullType, objectType, true, null, null);
      notNullType = objectType;
    }
    else if ((notNullType instanceof NamedType && ((NamedType) notNullType).getResolvedTypeDefinition() instanceof CompoundDefinition))
    {
      elementTypeParameterAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, ((NamedType) notNullType).getResolvedTypeDefinition(), notNullValue);
    }

    Disambiguator hashCodeDisambiguator = new MethodReference(new BuiltinMethod(new ObjectType(false, false, null), BuiltinMethodType.HASH_CODE), GenericTypeSpecialiser.IDENTITY_SPECIALISER).getDisambiguator();
    MethodReference hashCodeMethodReference = notNullType.getMethod(hashCodeDisambiguator);
    if (hashCodeMethodReference == null)
    {
      throw new IllegalArgumentException("Could not find the hashCode() method on " + notNullType);
    }
    // note: elementTypeParameterAccessor may be null here, but in the cases where it is null, buildMethodCall() shouldn't need it
    // (this is because of the conversion to object we did earlier, which ensures that the method's base type will never be an interface)
    LLVMValueRef firstElementHashCode = typeHelper.buildMethodCall(builder, landingPadContainer, notNullValue, notNullType, hashCodeMethodReference, new HashMap<Parameter, LLVMValueRef>(), elementTypeParameterAccessor);

    LLVMBasicBlockRef hashCodeBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVM.LLVMBuildBr(builder, continuationBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
    LLVMValueRef hashCodePhi = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt32Type(), "");
    LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false), firstElementHashCode};
    LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, hashCodeBlock};
    LLVM.LLVMAddIncoming(hashCodePhi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);

    if (elementType.canBeNullable())
    {
      LLVMValueRef[] nullCheckIncomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false)};
      LLVMBasicBlockRef[] nullCheckIncomingBlocks = new LLVMBasicBlockRef[] {nullCheckBlock};
      LLVM.LLVMAddIncoming(hashCodePhi, C.toNativePointerArray(nullCheckIncomingValues, false, true), C.toNativePointerArray(nullCheckIncomingBlocks, false, true), nullCheckIncomingValues.length);
    }

    // result = length << 5 + length + elementHashCode
    LLVMValueRef shiftedLength = LLVM.LLVMBuildShl(builder, length, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false), "");
    LLVMValueRef result = LLVM.LLVMBuildAdd(builder, shiftedLength, length, "");
    result = LLVM.LLVMBuildAdd(builder, result, hashCodePhi, "");
    LLVM.LLVMBuildRet(builder, result);


    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildFunctionToString(Type baseType, BuiltinMethod method)
  {
    if (!(baseType instanceof FunctionType))
    {
      throw new IllegalArgumentException("A builtin function toString function must have a function base type");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef calleeValue = LLVM.LLVMBuildExtractValue(builder, parameter, 1, "");
    LLVMValueRef calleeIntegerValue = LLVM.LLVMBuildPtrToInt(builder, calleeValue, LLVM.LLVMInt64Type(), "");
    LLVMValueRef functionValue = LLVM.LLVMBuildExtractValue(builder, parameter, 2, "");
    LLVMValueRef functionIntegerValue = LLVM.LLVMBuildPtrToInt(builder, functionValue, LLVM.LLVMInt64Type(), "");

    Type ulongType = new PrimitiveType(false, PrimitiveTypeType.ULONG, null);
    Disambiguator toStringMethodDisambiguator = new MethodReference(new BuiltinMethod(ulongType, BuiltinMethodType.TO_STRING_RADIX), GenericTypeSpecialiser.IDENTITY_SPECIALISER).getDisambiguator();
    Method toStringMethod = ulongType.getMethod(toStringMethodDisambiguator).getReferencedMember();
    LLVMValueRef ulongToStringFunction = codeGenerator.getMethodFunction(toStringMethod);

    LLVMValueRef[] calleeToStringArguments = new LLVMValueRef[] {calleeIntegerValue, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 16, false)};
    LLVMBasicBlockRef calleeStringContinueBlock = LLVM.LLVMAddBasicBlock(builder, "calleeStringInvokeContinue");
    LLVMValueRef calleeString = LLVM.LLVMBuildInvoke(builder, ulongToStringFunction, C.toNativePointerArray(calleeToStringArguments, false, true), calleeToStringArguments.length, calleeStringContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, calleeStringContinueBlock);

    LLVMValueRef[] functionToStringArguments = new LLVMValueRef[] {functionIntegerValue, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 16, false)};
    LLVMBasicBlockRef functionStringContinueBlock = LLVM.LLVMAddBasicBlock(builder, "functionStringInvokeContinue");
    LLVMValueRef functionString = LLVM.LLVMBuildInvoke(builder, ulongToStringFunction, C.toNativePointerArray(functionToStringArguments, false, true), functionToStringArguments.length, functionStringContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, functionStringContinueBlock);

    String startString = "[" + baseType.toString() + " function@";
    String middleString = " callee@";
    String endString = "]";

    LLVMValueRef llvmStartString = codeGenerator.buildStringCreation(builder, landingPadContainer, startString);
    llvmStartString = typeHelper.convertTemporaryToStandard(builder, llvmStartString, SpecialTypeHandler.STRING_TYPE);
    LLVMValueRef llvmMiddleString = codeGenerator.buildStringCreation(builder, landingPadContainer, middleString);
    llvmMiddleString = typeHelper.convertTemporaryToStandard(builder, llvmMiddleString, SpecialTypeHandler.STRING_TYPE);
    LLVMValueRef llvmEndString = codeGenerator.buildStringCreation(builder, landingPadContainer, endString);
    llvmEndString = typeHelper.convertTemporaryToStandard(builder, llvmEndString, SpecialTypeHandler.STRING_TYPE);

    LLVMValueRef completeString = codeGenerator.buildStringConcatenation(builder, landingPadContainer, llvmStartString, functionString, llvmMiddleString, calleeString, llvmEndString);
    completeString = typeHelper.convertTemporaryToStandard(builder, completeString, SpecialTypeHandler.STRING_TYPE);
    LLVM.LLVMBuildRet(builder, completeString);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildFunctionEquals(FunctionType functionType, BuiltinMethod method)
  {
    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 1);

    ObjectType objectType = new ObjectType(true, true, null);
    parameter = typeHelper.convertStandardToTemporary(builder, parameter, objectType);

    LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, parameter, objectType);
    LLVMBasicBlockRef failureBlock = LLVM.LLVMAddBasicBlock(builder, "failure");
    LLVMBasicBlockRef notNullParameterBlock = LLVM.LLVMAddBasicBlock(builder, "notNullParameter");
    LLVM.LLVMBuildCondBr(builder, isNotNull, notNullParameterBlock, failureBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, failureBlock);
    LLVM.LLVMBuildRet(builder, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false));

    LLVM.LLVMPositionBuilderAtEnd(builder, notNullParameterBlock);

    LLVMValueRef parameterRTTI = LLVM.LLVMBuildLoad(builder, rttiHelper.getRTTIPointer(builder, parameter), "");
    LLVMValueRef isInstanceOfFunction = rttiHelper.buildTypeInfoCheck(builder, parameterRTTI, functionType, null, true, false);

    LLVMBasicBlockRef functionCheckBlock = LLVM.LLVMAddBasicBlock(builder, "functionCheck");
    LLVM.LLVMBuildCondBr(builder, isInstanceOfFunction, functionCheckBlock, failureBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, functionCheckBlock);
    // skip runtime type checks for this conversion, since we already know that it is the correct type, and we do not have the TypeParameterAccessors which may be required to perform convertTemporary()'s run-time checks
    ObjectType notNullObjectType = new ObjectType(false, true, null);
    LLVMValueRef convertedParameter = typeHelper.convertTemporary(builder, landingPadContainer, parameter, notNullObjectType, functionType, true, null, null);
    LLVMValueRef equal = codeGenerator.buildEqualityCheck(builder, landingPadContainer, callee, convertedParameter, functionType, EqualityOperator.EQUAL);
    LLVM.LLVMBuildRet(builder, equal);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildFunctionHashCode(BuiltinMethod method)
  {
    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);

    LLVMValueRef opaquePtr = LLVM.LLVMBuildExtractValue(builder, callee, 1, "");
    LLVMValueRef functionPtr = LLVM.LLVMBuildExtractValue(builder, callee, 2, "");

    LLVMValueRef opaqueLong = LLVM.LLVMBuildPtrToInt(builder, opaquePtr, LLVM.LLVMInt64Type(), "");
    LLVMValueRef functionLong = LLVM.LLVMBuildPtrToInt(builder, functionPtr, LLVM.LLVMInt64Type(), "");

    LLVMValueRef opaqueBottomHalf = LLVM.LLVMBuildTrunc(builder, opaqueLong, LLVM.LLVMInt32Type(), "");
    LLVMValueRef opaqueTopHalf = LLVM.LLVMBuildLShr(builder, opaqueLong, LLVM.LLVMConstInt(LLVM.LLVMInt64Type(), 32, false), "");
    opaqueTopHalf = LLVM.LLVMBuildTrunc(builder, opaqueTopHalf, LLVM.LLVMInt32Type(), "");
    // result = opaqueTopHalf * 33 = opaqueTopHalf << 5 + opaqueTopHalf
    LLVMValueRef shiftedOpaqueTopHalf = LLVM.LLVMBuildShl(builder, opaqueTopHalf, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false), "");
    LLVMValueRef result = LLVM.LLVMBuildAdd(builder, shiftedOpaqueTopHalf, opaqueTopHalf, "");
    // result = result + opaqueBottomHalf
    result = LLVM.LLVMBuildAdd(builder, result, opaqueBottomHalf, "");
    // result = result * 33
    LLVMValueRef shiftedResult = LLVM.LLVMBuildShl(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false), "");
    result = LLVM.LLVMBuildAdd(builder, shiftedResult, result, "");

    LLVMValueRef functionBottomHalf = LLVM.LLVMBuildTrunc(builder, functionLong, LLVM.LLVMInt32Type(), "");
    LLVMValueRef functionTopHalf = LLVM.LLVMBuildLShr(builder, functionLong, LLVM.LLVMConstInt(LLVM.LLVMInt64Type(), 32, false), "");
    functionTopHalf = LLVM.LLVMBuildTrunc(builder, functionTopHalf, LLVM.LLVMInt32Type(), "");
    // result = result + functionTopHalf
    result = LLVM.LLVMBuildAdd(builder, result, functionTopHalf, "");
    // result = result * 33 = result << 5 + result
    LLVMValueRef shiftedSecondResult = LLVM.LLVMBuildShl(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false), "");
    result = LLVM.LLVMBuildAdd(builder, shiftedSecondResult, result, "");
    // result = result + functionBottomHalf
    result = LLVM.LLVMBuildAdd(builder, result, functionBottomHalf, "");

    LLVM.LLVMBuildRet(builder, result);

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildTupleToString(Type baseType, BuiltinMethod method)
  {
    if (!(baseType instanceof TupleType))
    {
      throw new IllegalArgumentException("A builtin tuple toString function must have a tuple base type");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);
    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    TypeParameterAccessor typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper);

    LLVMValueRef tupleValue = LLVM.LLVMGetParam(builtinFunction, 0);
    List<LLVMValueRef> subStrings = new LinkedList<LLVMValueRef>();

    LLVMValueRef llvmStartString = codeGenerator.buildStringCreation(builder, landingPadContainer, "(");
    llvmStartString = typeHelper.convertTemporaryToStandard(builder, llvmStartString, SpecialTypeHandler.STRING_TYPE);
    subStrings.add(llvmStartString);

    Type[] subTypes = ((TupleType) baseType).getSubTypes();
    for (int i = 0; i < subTypes.length; ++i)
    {
      if (subTypes[i] instanceof NullType)
      {
        LLVMValueRef llvmNullString = codeGenerator.buildStringCreation(builder, landingPadContainer, "null");
        llvmNullString = typeHelper.convertTemporaryToStandard(builder, llvmNullString, SpecialTypeHandler.STRING_TYPE);
        subStrings.add(llvmNullString);
      }
      else
      {
        LLVMValueRef subValue = LLVM.LLVMBuildExtractValue(builder, tupleValue, i, "");
        LLVMBasicBlockRef nullBlock = null;
        LLVMBasicBlockRef continuationBlock = null;
        if (subTypes[i].canBeNullable())
        {
          LLVMValueRef nullCheckResult = codeGenerator.buildNullCheck(builder, subValue, subTypes[i]);
          continuationBlock = LLVM.LLVMAddBasicBlock(builder, "continuation");
          nullBlock = LLVM.LLVMAddBasicBlock(builder, "nullGeneration");
          LLVMBasicBlockRef notNullBlock = LLVM.LLVMAddBasicBlock(builder, "toStringCall");
          LLVM.LLVMBuildCondBr(builder, nullCheckResult, notNullBlock, nullBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, notNullBlock);
        }
        Type notNullSubType = Type.findTypeWithNullability(subTypes[i], false);
        // skip runtime checks on this nullable -> not-nullable conversion, as we have just done them ourselves (see: nullCheckResult)
        LLVMValueRef notNullSubValue = typeHelper.convertTemporary(builder, landingPadContainer, subValue, subTypes[i], notNullSubType, true, typeParameterAccessor, typeParameterAccessor);
        // we shouldn't usually create BuiltinMethods like this, as they might need to have a method index set depending on the base type
        // but it's fine in this case, because we're just using it to get a Disambiguator
        Disambiguator toStringMethodDisambiguator = new MethodReference(new BuiltinMethod(notNullSubType, BuiltinMethodType.TO_STRING), GenericTypeSpecialiser.IDENTITY_SPECIALISER).getDisambiguator();
        MethodReference toStringMethodReference = notNullSubType.getMethod(toStringMethodDisambiguator);
        LLVMValueRef llvmTypeString = typeHelper.buildMethodCall(builder, landingPadContainer, notNullSubValue, notNullSubType, toStringMethodReference, new HashMap<Parameter, LLVMValueRef>(), typeParameterAccessor);
        llvmTypeString = typeHelper.convertTemporaryToStandard(builder, llvmTypeString, SpecialTypeHandler.STRING_TYPE);

        if (subTypes[i].canBeNullable())
        {
          LLVMBasicBlockRef endToStringCallBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildBr(builder, continuationBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, nullBlock);
          LLVMValueRef llvmNullString = codeGenerator.buildStringCreation(builder, landingPadContainer, "null");
          llvmNullString = typeHelper.convertTemporaryToStandard(builder, llvmNullString, SpecialTypeHandler.STRING_TYPE);
          LLVMBasicBlockRef endNullBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildBr(builder, continuationBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
          LLVMValueRef result = LLVM.LLVMBuildPhi(builder, typeHelper.findStandardType(SpecialTypeHandler.STRING_TYPE), "");
          LLVMValueRef[] incomingValues = new LLVMValueRef[] {llvmTypeString, llvmNullString};
          LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {endToStringCallBlock, endNullBlock};
          LLVM.LLVMAddIncoming(result, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);

          subStrings.add(result);
        }
        else
        {
          subStrings.add(llvmTypeString);
        }
      }

      if (i != subTypes.length - 1)
      {
        LLVMValueRef llvmCommaString = codeGenerator.buildStringCreation(builder, landingPadContainer, ", ");
        llvmCommaString = typeHelper.convertTemporaryToStandard(builder, llvmCommaString, SpecialTypeHandler.STRING_TYPE);
        subStrings.add(llvmCommaString);
      }
    }

    LLVMValueRef llvmEndString = codeGenerator.buildStringCreation(builder, landingPadContainer, ")");
    llvmEndString = typeHelper.convertTemporaryToStandard(builder, llvmEndString, SpecialTypeHandler.STRING_TYPE);
    subStrings.add(llvmEndString);

    LLVMValueRef result = codeGenerator.buildStringConcatenation(builder, landingPadContainer, subStrings.toArray(new LLVMValueRef[subStrings.size()]));
    result = typeHelper.convertTemporaryToStandard(builder, result, SpecialTypeHandler.STRING_TYPE);
    LLVM.LLVMBuildRet(builder, result);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildTupleEquals(TupleType tupleType, BuiltinMethod method)
  {
    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 1);

    ObjectType objectType = new ObjectType(true, true, null);
    parameter = typeHelper.convertStandardToTemporary(builder, parameter, objectType);

    LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, parameter, objectType);
    LLVMBasicBlockRef failureBlock = LLVM.LLVMAddBasicBlock(builder, "failure");
    LLVMBasicBlockRef notNullParameterBlock = LLVM.LLVMAddBasicBlock(builder, "notNullParameter");
    LLVM.LLVMBuildCondBr(builder, isNotNull, notNullParameterBlock, failureBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, failureBlock);
    LLVM.LLVMBuildRet(builder, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false));

    LLVM.LLVMPositionBuilderAtEnd(builder, notNullParameterBlock);

    LLVMValueRef parameterRTTI = LLVM.LLVMBuildLoad(builder, rttiHelper.getRTTIPointer(builder, parameter), "");
    LLVMValueRef isInstanceOfTuple = rttiHelper.buildTypeInfoCheck(builder, parameterRTTI, tupleType, null, true, false);

    LLVMBasicBlockRef tupleCheckBlock = LLVM.LLVMAddBasicBlock(builder, "tupleCheck");
    LLVM.LLVMBuildCondBr(builder, isInstanceOfTuple, tupleCheckBlock, failureBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, tupleCheckBlock);
    // skip runtime type checks for this conversion, since we already know that it is the correct type, and we do not have the TypeParameterAccessors which may be required to perform convertTemporary()'s run-time checks
    ObjectType notNullObjectType = new ObjectType(false, true, null);
    LLVMValueRef convertedParameter = typeHelper.convertTemporary(builder, landingPadContainer, parameter, notNullObjectType, tupleType, true, null, null);
    LLVMValueRef equal = codeGenerator.buildEqualityCheck(builder, landingPadContainer, callee, convertedParameter, tupleType, EqualityOperator.EQUAL);
    LLVM.LLVMBuildRet(builder, equal);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildTupleHashCode(TupleType tupleType, BuiltinMethod method)
  {
    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    LLVMValueRef callee = LLVM.LLVMGetParam(builtinFunction, 0);

    Type[] subTypes = tupleType.getSubTypes();
    LLVMValueRef result = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), subTypes.length, false);
    for (int i = 0; i < subTypes.length; ++i)
    {
      LLVMValueRef elementHashCode;
      Type subType = subTypes[i];
      if (subType instanceof NullType)
      {
        elementHashCode = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false);
      }
      else
      {
        LLVMValueRef elementValue = LLVM.LLVMBuildExtractValue(builder, callee, i, "");

        Type notNullSubType = subType;
        LLVMValueRef notNullElement = elementValue;
        LLVMBasicBlockRef nullCheckBlock = null;
        LLVMBasicBlockRef continuationBlock = null;
        if (subType.canBeNullable())
        {
          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, elementValue, subType);
          continuationBlock = LLVM.LLVMAddBasicBlock(builder, "nullCheckContinuation");
          LLVMBasicBlockRef notNullBlock = LLVM.LLVMAddBasicBlock(builder, "notNullElement");
          nullCheckBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildCondBr(builder, isNotNull, notNullBlock, continuationBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, notNullBlock);
          notNullSubType = Type.findTypeWithNullability(subType, false);
          // skip checks on this conversion, since we have already done them
          // also, use null TypeParameterAccessors, since we don't have our own and we're only converting from nullable to not-null
          notNullElement = typeHelper.convertTemporary(builder, landingPadContainer, elementValue, subType, notNullSubType, true, null, null);
        }

        // we don't have a TypeParameterAccessor for any type parameters in this sub-type, so we have
        // to be careful about how we call the hashCode() method on this element
        TypeParameterAccessor elementTypeParameterAccessor = null;
        if ((notNullSubType instanceof ObjectType) ||
            (notNullSubType instanceof NamedType && ((NamedType) notNullSubType).getResolvedTypeParameter() != null) ||
            (notNullSubType instanceof NamedType && ((NamedType) notNullSubType).getResolvedTypeDefinition() instanceof ClassDefinition) ||
            (notNullSubType instanceof NamedType && ((NamedType) notNullSubType).getResolvedTypeDefinition() instanceof InterfaceDefinition) ||
            (notNullSubType instanceof WildcardType))
        {
          // this can be converted to object without a heap allocation, so do that and then just call its object::hashCode() method
          // this way of calling the hashCode() method avoids trying to convert from notNullSubType to an interface which we don't know the type parameters for, which would be impossible
          ObjectType objectType = new ObjectType(false, Type.canBeExplicitlyDataImmutable(notNullSubType), null);
          notNullElement = typeHelper.convertTemporary(builder, landingPadContainer, notNullElement, notNullSubType, objectType, true, null, null);
          notNullSubType = objectType;
        }
        else if ((notNullSubType instanceof NamedType && ((NamedType) notNullSubType).getResolvedTypeDefinition() instanceof CompoundDefinition))
        {
          elementTypeParameterAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, ((NamedType) notNullSubType).getResolvedTypeDefinition(), notNullElement);
        }

        Disambiguator hashCodeDisambiguator = new MethodReference(new BuiltinMethod(new ObjectType(false, false, null), BuiltinMethodType.HASH_CODE), GenericTypeSpecialiser.IDENTITY_SPECIALISER).getDisambiguator();
        MethodReference hashCodeMethodReference = notNullSubType.getMethod(hashCodeDisambiguator);
        if (hashCodeMethodReference == null)
        {
          throw new IllegalArgumentException("Could not find the hashCode() method on " + notNullSubType);
        }
        // note: elementTypeParameterAccessor may be null here, but in the cases where it is null, buildMethodCall() shouldn't need it
        // (this is because of the conversion to object we did earlier, which ensures that the method's base type will never be an interface)
        elementHashCode = typeHelper.buildMethodCall(builder, landingPadContainer, notNullElement, notNullSubType, hashCodeMethodReference, new HashMap<Parameter, LLVMValueRef>(), elementTypeParameterAccessor);

        if (subType.canBeNullable())
        {
          LLVMBasicBlockRef hashCodeBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildBr(builder, continuationBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
          LLVMValueRef phiNode = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt32Type(), "");
          LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false), elementHashCode};
          LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {nullCheckBlock, hashCodeBlock};
          LLVM.LLVMAddIncoming(phiNode, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);

          elementHashCode = phiNode;
        }
      }

      // result = result * 33 = result << 5 + result
      LLVMValueRef shiftedResult = LLVM.LLVMBuildShl(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 5, false), "");
      result = LLVM.LLVMBuildAdd(builder, shiftedResult, result, "");
      // result = result + elementHashCode
      result = LLVM.LLVMBuildAdd(builder, result, elementHashCode, "");
    }

    LLVM.LLVMBuildRet(builder, result);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }
}
