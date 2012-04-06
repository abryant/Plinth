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

  public static native LLVMBasicBlockRef LLVMAppendBasicBlock(LLVMValueRef function, String name);

  public static native LLVMBuilderRef LLVMCreateBuilder();
  public static native LLVMBasicBlockRef LLVMPositionBuilder(LLVMBuilderRef builder, LLVMBasicBlockRef block, LLVMValueRef instruction);

  public static native LLVMValueRef LLVMBuildAdd(LLVMBuilderRef builder, LLVMValueRef lhs, LLVMValueRef rhs, String name);
  public static native LLVMValueRef LLVMBuildAlloca(LLVMBuilderRef builder, LLVMTypeRef type, String name);
  public static native LLVMValueRef LLVMBuildStore(LLVMBuilderRef builder, LLVMValueRef value, LLVMValueRef pointer);
  public static native LLVMValueRef LLVMBuildCall(LLVMBuilderRef builder, LLVMValueRef function, Pointer arguments, int numArgs, String name);
  public static native LLVMValueRef LLVMBuildLoad(LLVMBuilderRef builder, LLVMValueRef pointer, String name);
  public static native LLVMValueRef LLVMBuildRet(LLVMBuilderRef builder, LLVMValueRef value);

  public static native LLVMValueRef LLVMConstInt(LLVMTypeRef type, long n, boolean signExtend);

  public static native LLVMModuleRef LLVMModuleCreateWithName(String name);
  public static native LLVMValueRef LLVMAddFunction(LLVMModuleRef module, String name, LLVMTypeRef type);
  public static native LLVMValueRef LLVMGetNamedFunction(LLVMModuleRef module, String name);

  public static native void LLVMSetValueName(LLVMValueRef value, String name);

  public static native void LLVMSetFunctionCallConv(LLVMValueRef function, int callConv);
  public static native int LLVMGetFunctionCallConv(LLVMValueRef function);
  public static native int LLVMCountParams(LLVMValueRef function);
  public static native LLVMValueRef LLVMGetParam(LLVMValueRef function, int index);

  public static native LLVMTypeRef LLVMFunctionType(LLVMTypeRef returnType, Pointer paramTypes, int paramCount, boolean isVarArg);
  public static native LLVMTypeRef LLVMInt32Type();

  public static native int LLVMWriteBitcodeToFile(LLVMModuleRef module, String path);

  public static native LLVMPassManagerRef LLVMCreatePassManager();
}
