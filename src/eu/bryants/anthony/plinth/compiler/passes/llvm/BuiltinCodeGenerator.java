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
import eu.bryants.anthony.plinth.compiler.passes.SpecialTypeHandler;
import eu.bryants.anthony.plinth.compiler.passes.TypeChecker;

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

  /**
   * Creates a new BuiltinCodeGenerator to generate code for special types.
   * @param module - the LLVMModuleRef to add new functions to
   * @param codeGenerator - the CodeGenerator to use to build certain elements of the module
   * @param typeHelper - the TypeHelper to find native types with, and to use to convert between types
   */
  public BuiltinCodeGenerator(LLVMModuleRef module, CodeGenerator codeGenerator, TypeHelper typeHelper)
  {
    this.module = module;
    this.codeGenerator = codeGenerator;
    this.typeHelper = typeHelper;
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
      else if (baseType instanceof ObjectType || baseType instanceof NamedType)
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
    Type baseType = method.getBaseType();
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
    if (!(baseType instanceof NamedType))
    {
      // use linkonce-odr linkage, so that this function does not conflict with anything
      // by doing this, we ensure that linking with modules generated by a newer BuiltinCodeGenerator do not clash with this definition and cause problems
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
    LLVMValueRef valueOfFunction = codeGenerator.getMethodFunction(null, null, SpecialTypeHandler.stringValueOfBoolean);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef[] arguments = new LLVMValueRef[] {LLVM.LLVMConstNull(typeHelper.getOpaquePointer()), parameter};
    LLVMValueRef result = LLVM.LLVMBuildCall(builder, valueOfFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");
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
    LLVMValueRef valueOfFunction = codeGenerator.getMethodFunction(null, null, valueOfMethod);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);

    // get the parameter and convert it to a long
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);
    parameter = typeHelper.convertTemporary(builder, parameter, baseType, valueOfMethod.getParameters()[0].getType());

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
    LLVMValueRef result = LLVM.LLVMBuildCall(builder, valueOfFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");
    LLVM.LLVMBuildRet(builder, result);
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
    LLVMValueRef valueOfFunction = codeGenerator.getMethodFunction(null, null, valueOfMethod);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);

    // get the parameter and convert it to a long
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);
    parameter = typeHelper.convertTemporary(builder, parameter, baseType, valueOfMethod.getParameters()[0].getType());

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
    LLVMValueRef result = LLVM.LLVMBuildCall(builder, valueOfFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");
    LLVM.LLVMBuildRet(builder, result);
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
    LLVMValueRef valueOfFunction = codeGenerator.getMethodFunction(null, null, valueOfMethod);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);
    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef[] arguments = new LLVMValueRef[] {LLVM.LLVMConstNull(typeHelper.getOpaquePointer()), parameter};
    LLVMValueRef result = LLVM.LLVMBuildCall(builder, valueOfFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");
    LLVM.LLVMBuildRet(builder, result);
    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildObjectToString(Type baseType, BuiltinMethod method)
  {
    if (!(baseType instanceof ObjectType || baseType instanceof NamedType))
    {
      throw new IllegalArgumentException("A builtin object toString function must have either an object or a named base type");
    }

    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);

    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef integerValue = LLVM.LLVMBuildPtrToInt(builder, parameter, LLVM.LLVMInt64Type(), "");

    Type ulongType = new PrimitiveType(false, PrimitiveTypeType.ULONG, null);
    Method toStringMethod = ulongType.getMethod(new BuiltinMethod(ulongType, BuiltinMethodType.TO_STRING_RADIX).getDisambiguator());
    LLVMValueRef longToStringFunction = codeGenerator.getMethodFunction(builder, null, toStringMethod);

    LLVMValueRef[] longToStringArguments = new LLVMValueRef[] {integerValue, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 16, false)};
    LLVMValueRef pointerString = LLVM.LLVMBuildCall(builder, longToStringFunction, C.toNativePointerArray(longToStringArguments, false, true), longToStringArguments.length, "");

    // TODO: when we get run-time type information, for class types, put the fully qualified name of the run-time type here instead of "object"
    TypeDefinition resolvedDefinition = baseType instanceof NamedType ? ((NamedType) baseType).getResolvedTypeDefinition() : null;
    String prefixString = "[" + (resolvedDefinition instanceof CompoundDefinition ? ((CompoundDefinition) resolvedDefinition).getQualifiedName() : "object") + "@";
    String suffixString = "]";

    LLVMValueRef startString = codeGenerator.buildStringCreation(builder, prefixString);
    startString = typeHelper.convertTemporaryToStandard(builder, startString, SpecialTypeHandler.STRING_TYPE);
    LLVMValueRef endString = codeGenerator.buildStringCreation(builder, suffixString);
    endString = typeHelper.convertTemporaryToStandard(builder, endString, SpecialTypeHandler.STRING_TYPE);

    LLVMValueRef completeString = codeGenerator.buildStringConcatenation(builder, startString, pointerString, endString);
    completeString = typeHelper.convertTemporaryToStandard(builder, completeString, SpecialTypeHandler.STRING_TYPE);
    LLVM.LLVMBuildRet(builder, completeString);
    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }

  private LLVMValueRef buildArrayToString(ArrayType arrayType, BuiltinMethod method)
  {
    Type baseType = arrayType.getBaseType();

    LLVMValueRef builtinFunction = getBuiltinMethod(method);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(builtinFunction);

    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef array = typeHelper.convertStandardToTemporary(builder, parameter, arrayType);

    LLVMValueRef lengthPointer = typeHelper.getArrayLengthPointer(builder, array);
    LLVMValueRef length = LLVM.LLVMBuildLoad(builder, lengthPointer, "");

    LLVMValueRef llvmStartString = codeGenerator.buildStringCreation(builder, "[");

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

    LLVMValueRef elementPointer = typeHelper.getArrayElementPointer(builder, array, indexPhi);
    LLVMValueRef element = LLVM.LLVMBuildLoad(builder, elementPointer, "");
    element = typeHelper.convertStandardToTemporary(builder, element, baseType);

    // build the string conversion
    Type notNullBaseType = TypeChecker.findTypeWithNullability(baseType, false);
    LLVMValueRef elementString;
    LLVMValueRef notNullElement = element;
    LLVMBasicBlockRef nullBlock = null;
    LLVMBasicBlockRef continuationBlock = null;
    if (baseType.isNullable())
    {
      LLVMValueRef nullCheckResult = codeGenerator.buildNullCheck(builder, element, baseType);
      continuationBlock = LLVM.LLVMAddBasicBlock(builder, "continuation");
      nullBlock = LLVM.LLVMAddBasicBlock(builder, "nullGeneration");
      LLVMBasicBlockRef notNullBlock = LLVM.LLVMAddBasicBlock(builder, "toStringCall");
      LLVM.LLVMBuildCondBr(builder, nullCheckResult, notNullBlock, nullBlock);
      LLVM.LLVMPositionBuilderAtEnd(builder, notNullBlock);
      notNullElement = typeHelper.convertTemporary(builder, element, baseType, notNullBaseType);
    }
    Method toStringMethod = notNullBaseType.getMethod(new BuiltinMethod(notNullBaseType, BuiltinMethodType.TO_STRING).getDisambiguator());
    LLVMValueRef toStringFunction = codeGenerator.getMethodFunction(builder, notNullElement, toStringMethod);
    LLVMValueRef[] arguments = new LLVMValueRef[] {notNullElement};
    LLVMValueRef notNullElementString = LLVM.LLVMBuildCall(builder, toStringFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");

    if (baseType.isNullable())
    {
      LLVMBasicBlockRef endToStringCallBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, nullBlock);
      LLVMValueRef llvmNullString = codeGenerator.buildStringCreation(builder, "null");
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
    LLVMValueRef currentString = codeGenerator.buildStringConcatenation(builder, concatenationValues);

    // check whether this is the last element
    LLVMValueRef incIndex = LLVM.LLVMBuildAdd(builder, indexPhi, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false), "");
    LLVMValueRef loopCheck = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntULT, incIndex, length, "");
    LLVMBasicBlockRef endLoopBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVM.LLVMBuildCondBr(builder, loopCheck, addCommaBlock, exitBlock);

    // build the add comma block
    LLVM.LLVMPositionBuilderAtEnd(builder, addCommaBlock);
    LLVMValueRef llvmCommaString = codeGenerator.buildStringCreation(builder, ", ");
    LLVMValueRef[] commaConcatenationValues = new LLVMValueRef[] {typeHelper.convertTemporaryToStandard(builder, currentString, SpecialTypeHandler.STRING_TYPE),
                                                                  typeHelper.convertTemporaryToStandard(builder, llvmCommaString, SpecialTypeHandler.STRING_TYPE)};
    LLVMValueRef nextString = codeGenerator.buildStringConcatenation(builder, commaConcatenationValues);
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
    LLVMValueRef llvmEndString = codeGenerator.buildStringCreation(builder, "]");
    LLVMValueRef[] finalConcatenationValues = new LLVMValueRef[] {typeHelper.convertTemporaryToStandard(builder, exitStringPhi, SpecialTypeHandler.STRING_TYPE),
                                                                  typeHelper.convertTemporaryToStandard(builder, llvmEndString, SpecialTypeHandler.STRING_TYPE)};
    LLVMValueRef result = codeGenerator.buildStringConcatenation(builder, finalConcatenationValues);
    result = typeHelper.convertTemporaryToStandard(builder, result, SpecialTypeHandler.STRING_TYPE);
    LLVM.LLVMBuildRet(builder, result);
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

    LLVMValueRef parameter = LLVM.LLVMGetParam(builtinFunction, 0);
    LLVMValueRef calleeValue = LLVM.LLVMBuildExtractValue(builder, parameter, 0, "");
    LLVMValueRef calleeIntegerValue = LLVM.LLVMBuildPtrToInt(builder, calleeValue, LLVM.LLVMInt64Type(), "");
    LLVMValueRef functionValue = LLVM.LLVMBuildExtractValue(builder, parameter, 1, "");
    LLVMValueRef functionIntegerValue = LLVM.LLVMBuildPtrToInt(builder, functionValue, LLVM.LLVMInt64Type(), "");

    Type ulongType = new PrimitiveType(false, PrimitiveTypeType.ULONG, null);
    Method toStringMethod = ulongType.getMethod(new BuiltinMethod(ulongType, BuiltinMethodType.TO_STRING_RADIX).getDisambiguator());
    LLVMValueRef longToStringFunction = codeGenerator.getMethodFunction(builder, null, toStringMethod);

    LLVMValueRef[] calleeToStringArguments = new LLVMValueRef[] {calleeIntegerValue, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 16, false)};
    LLVMValueRef calleeString = LLVM.LLVMBuildCall(builder, longToStringFunction, C.toNativePointerArray(calleeToStringArguments, false, true), calleeToStringArguments.length, "");

    LLVMValueRef[] functionToStringArguments = new LLVMValueRef[] {functionIntegerValue, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 16, false)};
    LLVMValueRef functionString = LLVM.LLVMBuildCall(builder, longToStringFunction, C.toNativePointerArray(functionToStringArguments, false, true), functionToStringArguments.length, "");

    String startString = "[" + baseType.toString() + " function@";
    String middleString = " callee@";
    String endString = "]";

    LLVMValueRef llvmStartString = codeGenerator.buildStringCreation(builder, startString);
    llvmStartString = typeHelper.convertTemporaryToStandard(builder, llvmStartString, SpecialTypeHandler.STRING_TYPE);
    LLVMValueRef llvmMiddleString = codeGenerator.buildStringCreation(builder, middleString);
    llvmMiddleString = typeHelper.convertTemporaryToStandard(builder, llvmMiddleString, SpecialTypeHandler.STRING_TYPE);
    LLVMValueRef llvmEndString = codeGenerator.buildStringCreation(builder, endString);
    llvmEndString = typeHelper.convertTemporaryToStandard(builder, llvmEndString, SpecialTypeHandler.STRING_TYPE);

    LLVMValueRef completeString = codeGenerator.buildStringConcatenation(builder, llvmStartString, functionString, llvmMiddleString, calleeString, llvmEndString);
    completeString = typeHelper.convertTemporaryToStandard(builder, completeString, SpecialTypeHandler.STRING_TYPE);
    LLVM.LLVMBuildRet(builder, completeString);
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

    LLVMValueRef tupleValue = LLVM.LLVMGetParam(builtinFunction, 0);

    List<LLVMValueRef> subStrings = new LinkedList<LLVMValueRef>();

    LLVMValueRef llvmStartString = codeGenerator.buildStringCreation(builder, "(");
    llvmStartString = typeHelper.convertTemporaryToStandard(builder, llvmStartString, SpecialTypeHandler.STRING_TYPE);
    subStrings.add(llvmStartString);

    Type[] subTypes = ((TupleType) baseType).getSubTypes();
    for (int i = 0; i < subTypes.length; ++i)
    {
      if (subTypes[i] instanceof NullType)
      {
        LLVMValueRef llvmNullString = codeGenerator.buildStringCreation(builder, "null");
        llvmNullString = typeHelper.convertTemporaryToStandard(builder, llvmNullString, SpecialTypeHandler.STRING_TYPE);
        subStrings.add(llvmNullString);
      }
      else
      {
        LLVMValueRef subValue = LLVM.LLVMBuildExtractValue(builder, tupleValue, i, "");
        LLVMBasicBlockRef nullBlock = null;
        LLVMBasicBlockRef continuationBlock = null;
        if (subTypes[i].isNullable())
        {
          LLVMValueRef nullCheckResult = codeGenerator.buildNullCheck(builder, subValue, subTypes[i]);
          continuationBlock = LLVM.LLVMAddBasicBlock(builder, "continuation");
          nullBlock = LLVM.LLVMAddBasicBlock(builder, "nullGeneration");
          LLVMBasicBlockRef notNullBlock = LLVM.LLVMAddBasicBlock(builder, "toStringCall");
          LLVM.LLVMBuildCondBr(builder, nullCheckResult, notNullBlock, nullBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, notNullBlock);
        }
        Type notNullSubType = TypeChecker.findTypeWithNullability(subTypes[i], false);
        Method toStringMethod = notNullSubType.getMethod(new BuiltinMethod(notNullSubType, BuiltinMethodType.TO_STRING).getDisambiguator());
        LLVMValueRef toStringFunction = codeGenerator.getMethodFunction(builder, subValue, toStringMethod);
        LLVMValueRef[] arguments = new LLVMValueRef[] {subValue};
        LLVMValueRef llvmTypeString = LLVM.LLVMBuildCall(builder, toStringFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");

        if (subTypes[i].isNullable())
        {
          LLVMBasicBlockRef endToStringCallBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildBr(builder, continuationBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, nullBlock);
          LLVMValueRef llvmNullString = codeGenerator.buildStringCreation(builder, "null");
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
        LLVMValueRef llvmCommaString = codeGenerator.buildStringCreation(builder, ", ");
        llvmCommaString = typeHelper.convertTemporaryToStandard(builder, llvmCommaString, SpecialTypeHandler.STRING_TYPE);
        subStrings.add(llvmCommaString);
      }
    }

    LLVMValueRef llvmEndString = codeGenerator.buildStringCreation(builder, ")");
    llvmEndString = typeHelper.convertTemporaryToStandard(builder, llvmEndString, SpecialTypeHandler.STRING_TYPE);
    subStrings.add(llvmEndString);

    LLVMValueRef result = codeGenerator.buildStringConcatenation(builder, subStrings.toArray(new LLVMValueRef[subStrings.size()]));
    result = typeHelper.convertTemporaryToStandard(builder, result, SpecialTypeHandler.STRING_TYPE);
    LLVM.LLVMBuildRet(builder, result);
    LLVM.LLVMDisposeBuilder(builder);

    return builtinFunction;
  }
}
