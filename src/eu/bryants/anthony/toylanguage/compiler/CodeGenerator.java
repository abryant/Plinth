package eu.bryants.anthony.toylanguage.compiler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMBasicBlockRef;
import nativelib.llvm.LLVM.LLVMBuilderRef;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMTypeRef;
import nativelib.llvm.LLVM.LLVMValueRef;

import com.sun.jna.Pointer;

import eu.bryants.anthony.toylanguage.ast.AdditiveExpression;
import eu.bryants.anthony.toylanguage.ast.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.Block;
import eu.bryants.anthony.toylanguage.ast.BracketedExpression;
import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.Expression;
import eu.bryants.anthony.toylanguage.ast.Function;
import eu.bryants.anthony.toylanguage.ast.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.ast.IntegerLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.Parameter;
import eu.bryants.anthony.toylanguage.ast.ReturnStatement;
import eu.bryants.anthony.toylanguage.ast.Statement;
import eu.bryants.anthony.toylanguage.ast.Variable;
import eu.bryants.anthony.toylanguage.ast.VariableExpression;

/*
 * Created on 5 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class CodeGenerator
{
  private CompilationUnit compilationUnit;

  private LLVMModuleRef module;
  private LLVMBuilderRef builder;

  public CodeGenerator(CompilationUnit compilationUnit)
  {
    this.compilationUnit = compilationUnit;
    module = LLVM.LLVMModuleCreateWithName("MainModule");
    builder = LLVM.LLVMCreateBuilder();
  }

  public void generate(String outputPath)
  {
    addFunctions();

    for (Function f : compilationUnit.getFunctions())
    {
      addFunctionBody(f);
    }
    LLVM.LLVMWriteBitcodeToFile(module, outputPath);
  }

  private void addFunctions()
  {
    for (Function function : compilationUnit.getFunctions())
    {
      Parameter[] params = function.getParameters();

      LLVMTypeRef[] types = new LLVMTypeRef[params.length];
      for (int i = 0; i < types.length; i++)
      {
        types[i] = LLVM.LLVMInt32Type();
      }

      Pointer paramTypes = C.toNativePointerArray(types, false, true);
      LLVMTypeRef functionType = LLVM.LLVMFunctionType(LLVM.LLVMInt32Type(), paramTypes, types.length, false);
      LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, function.getName(), functionType);
      LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);

      int paramCount = LLVM.LLVMCountParams(llvmFunc);
      if (paramCount != params.length)
      {
        throw new IllegalStateException("LLVM returned wrong number of parameters");
      }
      for (int i = 0; i < paramCount; i++)
      {
        LLVMValueRef parameter = LLVM.LLVMGetParam(llvmFunc, i);
        LLVM.LLVMSetValueName(parameter, params[i].getName());
      }
    }
  }

  private void addFunctionBody(Function function)
  {
    LLVMValueRef llvmFunction = LLVM.LLVMGetNamedFunction(module, function.getName());

    LLVMBasicBlockRef block = LLVM.LLVMAppendBasicBlock(llvmFunction, "entry");
    LLVM.LLVMPositionBuilder(builder, block, null);

    // create LLVMValueRefs for all of the variables, including parameters
    Set<Variable> allVariables = Resolver.getAllNestedVariables(function);
    Map<Variable, LLVMValueRef> variables = new HashMap<Variable, LLVM.LLVMValueRef>();
    for (Variable v : allVariables)
    {
      LLVMValueRef allocaInst = LLVM.LLVMBuildAlloca(builder, LLVM.LLVMInt32Type(), v.getName());
      variables.put(v, allocaInst);
    }

    // store the parameter values to the LLVMValueRefs
    for (Parameter p : function.getParameters())
    {
      LLVM.LLVMBuildStore(builder, LLVM.LLVMGetParam(llvmFunction, p.getIndex()), variables.get(p));
    }

    buildBlock(function.getBlock(), variables);
  }

  private void buildBlock(Block block, Map<Variable, LLVMValueRef> variables)
  {
    for (Statement s : block.getStatements())
    {
      if (s instanceof AssignStatement)
      {
        AssignStatement assign = (AssignStatement) s;
        LLVMValueRef value = buildExpression(block, variables, assign.getExpression());
        LLVMValueRef allocaInst = variables.get(assign.getResolvedVariable());
        if (allocaInst == null)
        {
          throw new IllegalStateException("Missing LLVMValueRef in variable Map: " + assign.getVariableName());
        }
        LLVM.LLVMBuildStore(builder, value, allocaInst);
      }
      else if (s instanceof ReturnStatement)
      {
        LLVMValueRef value = buildExpression(block, variables, ((ReturnStatement) s).getExpression());
        LLVM.LLVMBuildRet(builder, value);
      }
      else if (s instanceof Block)
      {
        buildBlock((Block) s, variables);
      }
    }
  }

  private LLVMValueRef buildExpression(Block block, Map<Variable, LLVMValueRef> variables, Expression expression)
  {
    if (expression instanceof IntegerLiteralExpression)
    {
      int n = ((IntegerLiteralExpression) expression).getLiteral().getValue().intValue();
      return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), n, false);
    }
    if (expression instanceof VariableExpression)
    {
      Variable variable = ((VariableExpression) expression).getResolvedVariable();
      LLVMValueRef value = variables.get(variable);
      if (value == null)
      {
        throw new IllegalStateException("Missing LLVMValueRef in variable Map: " + ((VariableExpression) expression).getName());
      }
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    if (expression instanceof BracketedExpression)
    {
      return buildExpression(block, variables, ((BracketedExpression) expression).getExpression());
    }
    if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression functionExpression = (FunctionCallExpression) expression;
      Expression[] arguments = functionExpression.getArguments();
      LLVMValueRef[] values = new LLVMValueRef[arguments.length];
      for (int i = 0; i < arguments.length; i++)
      {
        values[i] = buildExpression(block, variables, arguments[i]);
      }
      Pointer llvmArguments = C.toNativePointerArray(values, false, true);
      LLVMValueRef llvmResolvedFunction = LLVM.LLVMGetNamedFunction(module, functionExpression.getResolvedFunction().getName());
      return LLVM.LLVMBuildCall(builder, llvmResolvedFunction, llvmArguments, values.length, "");
    }
    if (expression instanceof AdditiveExpression)
    {
      AdditiveExpression additiveExpression = (AdditiveExpression) expression;
      LLVMValueRef left = buildExpression(block, variables, additiveExpression.getLeftSubExpression());
      LLVMValueRef right = buildExpression(block, variables, additiveExpression.getRightSubExpression());
      return LLVM.LLVMBuildAdd(builder, left, right, "");
    }
    throw new IllegalArgumentException("Unknown Expression type");
  }
}
