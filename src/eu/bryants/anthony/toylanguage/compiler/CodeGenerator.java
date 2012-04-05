package eu.bryants.anthony.toylanguage.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMBasicBlockRef;
import nativelib.llvm.LLVM.LLVMBuilderRef;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMTypeRef;
import nativelib.llvm.LLVM.LLVMValueRef;

import com.sun.jna.Pointer;

import eu.bryants.anthony.toylanguage.ast.AdditiveExpression;
import eu.bryants.anthony.toylanguage.ast.BracketedExpression;
import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.Expression;
import eu.bryants.anthony.toylanguage.ast.Function;
import eu.bryants.anthony.toylanguage.ast.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.ast.IntegerLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.Parameter;
import eu.bryants.anthony.toylanguage.ast.VariableExpression;

/*
 * Created on 5 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class CodeGenerator
{
  private LLVMModuleRef module;
  private LLVMBuilderRef builder;

  public CodeGenerator()
  {
    module = LLVM.LLVMModuleCreateWithName("MainModule");
    builder = LLVM.LLVMCreateBuilder();
  }

  public void generate(CompilationUnit compilationUnit, String outputPath)
  {
    addFunctions(compilationUnit);

    for (Function f : compilationUnit.getFunctions())
    {
      addFunctionBody(f);
    }
    LLVM.LLVMWriteBitcodeToFile(module, outputPath);
  }

  private void addFunctions(CompilationUnit compilationUnit)
  {
    for (Function function : compilationUnit.getFunctions())
    {
      List<Parameter> params = new ArrayList<Parameter>(function.getParameters());
      Collections.sort(params, new Comparator<Parameter>()
      {
        @Override
        public int compare(Parameter o1, Parameter o2)
        {
          return o1.getIndex() - o2.getIndex();
        }
      });

      LLVMTypeRef[] types = new LLVMTypeRef[params.size()];
      for (int i = 0; i < types.length; i++)
      {
        types[i] = LLVM.LLVMInt32Type();
      }

      Pointer paramTypes = C.toNativePointerArray(types, false);
      LLVMTypeRef functionType = LLVM.LLVMFunctionType(LLVM.LLVMInt32Type(), paramTypes, types.length, false);
      LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, function.getName(), functionType);
      LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);

      int paramCount = LLVM.LLVMCountParams(llvmFunc);
      if (paramCount != params.size())
      {
        throw new IllegalStateException("LLVM returned wrong number of parameters");
      }
      for (int i = 0; i < paramCount; i++)
      {
        LLVMValueRef parameter = LLVM.LLVMGetParam(llvmFunc, i);
        LLVM.LLVMSetValueName(parameter, params.get(i).getName());
      }
    }
  }

  private void addFunctionBody(Function function)
  {
    LLVMValueRef llvmFunction = LLVM.LLVMGetNamedFunction(module, function.getName());

    LLVMBasicBlockRef block = LLVM.LLVMAppendBasicBlock(llvmFunction, "entry");
    LLVM.LLVMPositionBuilder(builder, block, null);

    LLVMValueRef value = buildExpression(llvmFunction, function.getExpression());
    LLVM.LLVMBuildRet(builder, value);
  }

  private LLVMValueRef buildExpression(LLVMValueRef llvmFunction, Expression expression)
  {
    if (expression instanceof IntegerLiteralExpression)
    {
      int n = ((IntegerLiteralExpression) expression).getLiteral().getValue().intValue();
      return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), n, false);
    }
    if (expression instanceof VariableExpression)
    {
      int index = ((VariableExpression) expression).getResolvedParameter().getIndex();
      return LLVM.LLVMGetParam(llvmFunction, index);
    }
    if (expression instanceof BracketedExpression)
    {
      return buildExpression(llvmFunction, ((BracketedExpression) expression).getExpression());
    }
    if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression functionExpression = (FunctionCallExpression) expression;
      Expression[] arguments = functionExpression.getArguments();
      LLVMValueRef[] values = new LLVMValueRef[arguments.length];
      for (int i = 0; i < arguments.length; i++)
      {
        values[i] = buildExpression(llvmFunction, arguments[i]);
      }
      Pointer llvmArguments = C.toNativePointerArray(values, false);
      LLVMValueRef llvmResolvedFunction = LLVM.LLVMGetNamedFunction(module, functionExpression.getResolvedFunction().getName());
      return LLVM.LLVMBuildCall(builder, llvmResolvedFunction, llvmArguments, values.length, "");
    }
    if (expression instanceof AdditiveExpression)
    {
      AdditiveExpression additiveExpression = (AdditiveExpression) expression;
      LLVMValueRef left = buildExpression(llvmFunction, additiveExpression.getLeftSubExpression());
      LLVMValueRef right = buildExpression(llvmFunction, additiveExpression.getRightSubExpression());
      return LLVM.LLVMBuildAdd(builder, left, right, "");
    }
    throw new IllegalArgumentException("Unknown Expression type");
  }
}
