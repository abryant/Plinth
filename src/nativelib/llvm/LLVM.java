package nativelib.llvm;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/*
 * Created on 4 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class LLVM
{
  static
  {
    Native.register("LLVM-3.1");
  }
  public static class LLVMBasicBlockRef extends PointerType { /* custom type name */ }
  public static class LLVMBuilderRef extends PointerType { /* custom type name */ }
  public static class LLVMModuleRef extends PointerType { /* custom type name */ }
  public static class LLVMPassManagerRef extends PointerType { /* custom type name */ }
  public static class LLVMTypeRef extends PointerType { /* custom type name */ }
  public static class LLVMValueRef extends PointerType { /* custom type name */ }

  public static class LLVMCallConv
  {
    public static final int LLVMCCallConv = 0;
    public static final int LLVMFastCallConv = 8;
    public static final int LLVMColdCallConv = 9;
    public static final int LLVMX86StdcallCallConv = 64;
    public static final int LLVMX86FastcallCallConv = 65;
  }

  public static class LLVMIntPredicate
  {
    public static final int LLVMIntEQ  = 32;
    public static final int LLVMIntNE  = 33;
    public static final int LLVMIntUGT = 34;
    public static final int LLVMIntUGE = 35;
    public static final int LLVMIntULT = 36;
    public static final int LLVMIntULE = 37;
    public static final int LLVMIntSGT = 38;
    public static final int LLVMIntSGE = 39;
    public static final int LLVMIntSLT = 40;
    public static final int LLVMIntSLE = 41;
  }

  public static class LLVMRealPredicate
  {
    public static final int LLVMRealPredicateFalse = 0;
    public static final int LLVMRealOEQ = 1;
    public static final int LLVMRealOGT = 2;
    public static final int LLVMRealOGE = 3;
    public static final int LLVMRealOLT = 4;
    public static final int LLVMRealOLE = 5;
    public static final int LLVMRealONE = 6;
    public static final int LLVMRealORD = 7;
    public static final int LLVMRealUNO = 8;
    public static final int LLVMRealUEQ = 9;
    public static final int LLVMRealUGT = 10;
    public static final int LLVMRealUGE = 11;
    public static final int LLVMRealULT = 12;
    public static final int LLVMRealULE = 13;
    public static final int LLVMRealUNE = 14;
    public static final int LLVMRealPredicateTrue = 15;
  }

  public static native LLVMBasicBlockRef LLVMAppendBasicBlock(LLVMValueRef function, String name);
  public static native LLVMBasicBlockRef LLVMInsertBasicBlock(LLVMBasicBlockRef insertBeforeBlock, String name);
  public static native LLVMBasicBlockRef LLVMGetEntryBasicBlock(LLVMValueRef function);
  public static native LLVMValueRef LLVMGetFirstInstruction(LLVMBasicBlockRef block);
  public static native LLVMValueRef LLVMGetLastInstruction(LLVMBasicBlockRef block);

  public static native LLVMBuilderRef LLVMCreateBuilder();
  public static native void LLVMPositionBuilder(LLVMBuilderRef builder, LLVMBasicBlockRef block, LLVMValueRef instruction);
  public static native void LLVMPositionBuilderBefore(LLVMBuilderRef builder, LLVMValueRef instruction);
  public static native void LLVMPositionBuilderAtEnd(LLVMBuilderRef builder, LLVMBasicBlockRef block);
  public static native LLVMBasicBlockRef LLVMGetInsertBlock(LLVMBuilderRef builder);

  public static native LLVMValueRef LLVMBuildAdd(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildAlloca(LLVMBuilderRef builder, LLVMTypeRef type, String name);
  public static native LLVMValueRef LLVMBuildAnd(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildAShr(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildBitCast(LLVMBuilderRef builder, LLVMValueRef value, LLVMTypeRef destType, String name);
  public static native LLVMValueRef LLVMBuildBr(LLVMBuilderRef builder, LLVMBasicBlockRef dest);
  public static native LLVMValueRef LLVMBuildCondBr(LLVMBuilderRef builder, LLVMValueRef conditional, LLVMBasicBlockRef trueDest, LLVMBasicBlockRef falseDest);
  public static native LLVMValueRef LLVMBuildCall(LLVMBuilderRef builder, LLVMValueRef function, Pointer arguments, int numArgs, String name);
  public static native LLVMValueRef LLVMBuildExtractValue(LLVMBuilderRef builder, LLVMValueRef aggregateVal, int index, String name);
  public static native LLVMValueRef LLVMBuildFAdd(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildFCmp(LLVMBuilderRef builder, int realPredicate, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildFDiv(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildFMul(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildFNeg(LLVMBuilderRef builder, LLVMValueRef value, String name);
  public static native LLVMValueRef LLVMBuildFRem(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildFSub(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildFPCast(LLVMBuilderRef builder, LLVMValueRef value, LLVMTypeRef destType, String name);
  public static native LLVMValueRef LLVMBuildFPToSI(LLVMBuilderRef builder, LLVMValueRef value, LLVMTypeRef destType, String name);
  public static native LLVMValueRef LLVMBuildFPToUI(LLVMBuilderRef builder, LLVMValueRef value, LLVMTypeRef destType, String name);
  public static native LLVMValueRef LLVMBuildGEP(LLVMBuilderRef builder, LLVMValueRef pointer, Pointer indices, int numIndices, String name);
  public static native LLVMValueRef LLVMBuildICmp(LLVMBuilderRef builder, int intPredicate, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildInsertValue(LLVMBuilderRef builder, LLVMValueRef aggregateVal, LLVMValueRef elementVal, int index, String name);
  public static native LLVMValueRef LLVMBuildLoad(LLVMBuilderRef builder, LLVMValueRef pointer, String name);
  public static native LLVMValueRef LLVMBuildLShr(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildMul(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildNot(LLVMBuilderRef builder, LLVMValueRef value, String name);
  public static native LLVMValueRef LLVMBuildNeg(LLVMBuilderRef builder, LLVMValueRef value, String name);
  public static native LLVMValueRef LLVMBuildOr(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildPhi(LLVMBuilderRef builder, LLVMTypeRef type, String name);
  public static native LLVMValueRef LLVMBuildPtrToInt(LLVMBuilderRef builder, LLVMValueRef value, LLVMTypeRef destType, String name);
  public static native LLVMValueRef LLVMBuildRet(LLVMBuilderRef builder, LLVMValueRef value);
  public static native LLVMValueRef LLVMBuildRetVoid(LLVMBuilderRef builder);
  public static native LLVMValueRef LLVMBuildSDiv(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildSExt(LLVMBuilderRef builder, LLVMValueRef value, LLVMTypeRef destType, String name);
  public static native LLVMValueRef LLVMBuildShl(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildSIToFP(LLVMBuilderRef builder, LLVMValueRef value, LLVMTypeRef destType, String name);
  public static native LLVMValueRef LLVMBuildUIToFP(LLVMBuilderRef builder, LLVMValueRef value, LLVMTypeRef destType, String name);
  public static native LLVMValueRef LLVMBuildSRem(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildStore(LLVMBuilderRef builder, LLVMValueRef value, LLVMValueRef pointer);
  public static native LLVMValueRef LLVMBuildSub(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildTrunc(LLVMBuilderRef builder, LLVMValueRef value, LLVMTypeRef destType, String name);
  public static native LLVMValueRef LLVMBuildUDiv(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildURem(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildXor(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildZExt(LLVMBuilderRef builder, LLVMValueRef value, LLVMTypeRef destType, String name);

  public static native void LLVMAddIncoming(LLVMValueRef phiNode, Pointer incomingValues, Pointer incomingBlocks, int count);

  public static native LLVMValueRef LLVMConstInt(LLVMTypeRef type, long n, boolean signExtend);
  public static native LLVMValueRef LLVMConstReal(LLVMTypeRef type, double n);
  public static native LLVMValueRef LLVMConstNull(LLVMTypeRef type);
  public static native LLVMValueRef LLVMConstStruct(Pointer constantValues, int constantValueCount, boolean packed);
  public static native LLVMValueRef LLVMGetUndef(LLVMTypeRef type);

  public static native LLVMValueRef LLVMMDString(String str, int length);
  public static native LLVMValueRef LLVMMDNode(Pointer values, int count);
  public static native void LLVMAddNamedMetadataOperand(LLVMModuleRef module, String name, LLVMValueRef value);

  public static native LLVMModuleRef LLVMModuleCreateWithName(String name);
  public static native LLVMValueRef LLVMAddFunction(LLVMModuleRef module, String name, LLVMTypeRef type);
  public static native LLVMValueRef LLVMGetNamedFunction(LLVMModuleRef module, String name);
  public static native void LLVMDumpModule(LLVMModuleRef module);

  public static native void LLVMSetValueName(LLVMValueRef value, String name);

  public static native void LLVMSetFunctionCallConv(LLVMValueRef function, int callConv);
  public static native int LLVMGetFunctionCallConv(LLVMValueRef function);
  public static native int LLVMCountParams(LLVMValueRef function);
  public static native LLVMValueRef LLVMGetParam(LLVMValueRef function, int index);

  public static native LLVMValueRef LLVMAddGlobal(LLVMModuleRef module, LLVMTypeRef type, String name);
  public static native void LLVMSetInitializer(LLVMValueRef globalVariable, LLVMValueRef constantValue);

  public static native LLVMTypeRef LLVMArrayType(LLVMTypeRef elementType, int elementCount);
  public static native LLVMTypeRef LLVMFunctionType(LLVMTypeRef returnType, Pointer paramTypes, int paramCount, boolean isVarArg);
  public static native LLVMTypeRef LLVMDoubleType();
  public static native LLVMTypeRef LLVMFloatType();
  public static native LLVMTypeRef LLVMInt1Type();
  public static native LLVMTypeRef LLVMInt8Type();
  public static native LLVMTypeRef LLVMInt16Type();
  public static native LLVMTypeRef LLVMInt32Type();
  public static native LLVMTypeRef LLVMInt64Type();
  public static native LLVMTypeRef LLVMIntType(int bits);
  public static native LLVMTypeRef LLVMPointerType(LLVMTypeRef elementType, int addressSpace);
  public static native LLVMTypeRef LLVMStructType(Pointer elementTypes, int elementCount, boolean packed);
  public static native LLVMTypeRef LLVMVoidType();

  public static native int LLVMWriteBitcodeToFile(LLVMModuleRef module, String path);

  public static native LLVMPassManagerRef LLVMCreatePassManager();
}
