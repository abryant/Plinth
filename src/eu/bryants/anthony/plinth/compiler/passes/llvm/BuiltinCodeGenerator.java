package eu.bryants.anthony.plinth.compiler.passes.llvm;

import java.util.LinkedList;
import java.util.List;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMBasicBlockRef;
import nativelib.llvm.LLVM.LLVMBuilderRef;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMTypeRef;
import nativelib.llvm.LLVM.LLVMValueRef;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod.BuiltinMethodType;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.metadata.GenericTypeSpecialiser;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference.Disambiguator;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NullType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
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
    for (int i = 0; i < parameters.length; ++i)
    {
      LLVMValueRef parameter = LLVM.LLVMGetParam(llvmFunc, i + 1);
      LLVM.LLVMSetValueName(parameter, parameters[i].getName());
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
    parameter = typeHelper.convertTemporary(builder, landingPadContainer, parameter, baseType, valueOfMethod.getParameters()[0].getType(), false, typeParameterAccessor);

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
    parameter = typeHelper.convertTemporary(builder, landingPadContainer, parameter, baseType, valueOfMethod.getParameters()[0].getType(), false, typeParameterAccessor);

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
      LLVMValueRef rttiPointer = rttiHelper.lookupPureRTTI(builder, parameter);
      LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                   LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false)};
      LLVMValueRef sortIdPointer = LLVM.LLVMBuildGEP(builder, rttiPointer, C.toNativePointerArray(indices, false, true), indices.length, "");
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
      LLVMValueRef classQualifiedNameUbyteArray = rttiHelper.lookupNamedTypeName(builder, rttiPointer);

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
      notNullElement = typeHelper.convertTemporary(builder, landingPadContainer, element, baseType, notNullBaseType, false, typeParameterAccessor);
    }
    // we shouldn't usually create BuiltinMethods like this, as they might need to have a method index set depending on the base type
    // but it's fine in this case, because we're just using it to get a Disambiguator
    Disambiguator toStringMethodDisambiguator = new MethodReference(new BuiltinMethod(notNullBaseType, BuiltinMethodType.TO_STRING), GenericTypeSpecialiser.IDENTITY_SPECIALISER).getDisambiguator();
    MethodReference toStringMethod = notNullBaseType.getMethod(toStringMethodDisambiguator);
    LLVMValueRef notNullElementString = typeHelper.buildMethodCall(builder, landingPadContainer, notNullElement, notNullBaseType, toStringMethod, new LLVMValueRef[0], typeParameterAccessor);
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
        LLVMValueRef notNullSubValue = typeHelper.convertTemporary(builder, landingPadContainer, subValue, subTypes[i], notNullSubType, false, typeParameterAccessor);
        // we shouldn't usually create BuiltinMethods like this, as they might need to have a method index set depending on the base type
        // but it's fine in this case, because we're just using it to get a Disambiguator
        Disambiguator toStringMethodDisambiguator = new MethodReference(new BuiltinMethod(notNullSubType, BuiltinMethodType.TO_STRING), GenericTypeSpecialiser.IDENTITY_SPECIALISER).getDisambiguator();
        MethodReference toStringMethodReference = notNullSubType.getMethod(toStringMethodDisambiguator);
        LLVMValueRef llvmTypeString = typeHelper.buildMethodCall(builder, landingPadContainer, notNullSubValue, notNullSubType, toStringMethodReference, new LLVMValueRef[0], typeParameterAccessor);
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
}
