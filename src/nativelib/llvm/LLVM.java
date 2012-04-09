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
    Native.register("LLVM-3.0");
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

  public static native LLVMBasicBlockRef LLVMAppendBasicBlock(LLVMValueRef function, String name);
  public static native LLVMBasicBlockRef LLVMInsertBasicBlock(LLVMBasicBlockRef insertBeforeBlock, String name);

  public static native LLVMBuilderRef LLVMCreateBuilder();
  public static native void LLVMPositionBuilder(LLVMBuilderRef builder, LLVMBasicBlockRef block, LLVMValueRef instruction);
  public static native void LLVMPositionBuilderBefore(LLVMBuilderRef builder, LLVMValueRef instruction);
  public static native void LLVMPositionBuilderAtEnd(LLVMBuilderRef builder, LLVMBasicBlockRef block);
  public static native LLVMBasicBlockRef LLVMGetInsertBlock(LLVMBuilderRef builder);

  public static native LLVMValueRef LLVMBuildAdd(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildAlloca(LLVMBuilderRef builder, LLVMTypeRef type, String name);
  public static native LLVMValueRef LLVMBuildBr(LLVMBuilderRef builder, LLVMBasicBlockRef dest);
  public static native LLVMValueRef LLVMBuildCondBr(LLVMBuilderRef builder, LLVMValueRef conditional, LLVMBasicBlockRef trueDest, LLVMBasicBlockRef falseDest);
  public static native LLVMValueRef LLVMBuildStore(LLVMBuilderRef builder, LLVMValueRef value, LLVMValueRef pointer);
  public static native LLVMValueRef LLVMBuildCall(LLVMBuilderRef builder, LLVMValueRef function, Pointer arguments, int numArgs, String name);
  public static native LLVMValueRef LLVMBuildICmp(LLVMBuilderRef builder, int intPredicate, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildLoad(LLVMBuilderRef builder, LLVMValueRef pointer, String name);
  public static native LLVMValueRef LLVMBuildRet(LLVMBuilderRef builder, LLVMValueRef value);

  public static native LLVMValueRef LLVMConstInt(LLVMTypeRef type, long n, boolean signExtend);
  public static native LLVMValueRef LLVMConstReal(LLVMTypeRef type, double n);

  public static native LLVMModuleRef LLVMModuleCreateWithName(String name);
  public static native LLVMValueRef LLVMAddFunction(LLVMModuleRef module, String name, LLVMTypeRef type);
  public static native LLVMValueRef LLVMGetNamedFunction(LLVMModuleRef module, String name);
  public static native void LLVMDumpModule(LLVMModuleRef module);

  public static native void LLVMSetValueName(LLVMValueRef value, String name);

  public static native void LLVMSetFunctionCallConv(LLVMValueRef function, int callConv);
  public static native int LLVMGetFunctionCallConv(LLVMValueRef function);
  public static native int LLVMCountParams(LLVMValueRef function);
  public static native LLVMValueRef LLVMGetParam(LLVMValueRef function, int index);

  public static native LLVMTypeRef LLVMFunctionType(LLVMTypeRef returnType, Pointer paramTypes, int paramCount, boolean isVarArg);
  public static native LLVMTypeRef LLVMDoubleType();
  public static native LLVMTypeRef LLVMInt1Type();
  public static native LLVMTypeRef LLVMInt32Type();
  public static native LLVMTypeRef LLVMIntType(int bits);

  public static native int LLVMWriteBitcodeToFile(LLVMModuleRef module, String path);

  public static native LLVMPassManagerRef LLVMCreatePassManager();
}
