package eu.bryants.anthony.toylanguage.compiler.passes;

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

import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.Function;
import eu.bryants.anthony.toylanguage.ast.Parameter;
import eu.bryants.anthony.toylanguage.ast.expression.AdditionExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BooleanLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BracketedExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ComparisonExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ComparisonExpression.ComparisonOperator;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.SubtractionExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.statement.IfStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ReturnStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Statement;
import eu.bryants.anthony.toylanguage.ast.statement.VariableDefinition;
import eu.bryants.anthony.toylanguage.ast.statement.WhileStatement;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.toylanguage.ast.type.Type;

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
        types[i] = findNativeType(params[i].getType());
      }
      LLVMTypeRef resultType = findNativeType(function.getType());

      Pointer paramTypes = C.toNativePointerArray(types, false, true);
      LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, paramTypes, types.length, false);
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

  private LLVMTypeRef findNativeType(Type type)
  {
    if (type instanceof PrimitiveType)
    {
      switch (((PrimitiveType) type).getPrimitiveTypeType())
      {
      case BOOLEAN:
        return LLVM.LLVMInt1Type();
      case DOUBLE:
        return LLVM.LLVMDoubleType();
      case INT:
        return LLVM.LLVMInt32Type();
      }
    }
    throw new IllegalStateException("Unexpected Type: " + type);
  }

  private void addFunctionBody(Function function)
  {
    LLVMValueRef llvmFunction = LLVM.LLVMGetNamedFunction(module, function.getName());

    LLVMBasicBlockRef block = LLVM.LLVMAppendBasicBlock(llvmFunction, "entry");
    LLVM.LLVMPositionBuilderAtEnd(builder, block);

    // create LLVMValueRefs for all of the variables, including parameters
    Set<Variable> allVariables = Resolver.getAllNestedVariables(function);
    Map<Variable, LLVMValueRef> variables = new HashMap<Variable, LLVM.LLVMValueRef>();
    for (Variable v : allVariables)
    {
      LLVMValueRef allocaInst = LLVM.LLVMBuildAlloca(builder, findNativeType(v.getType()), v.getName());
      variables.put(v, allocaInst);
    }

    // store the parameter values to the LLVMValueRefs
    for (Parameter p : function.getParameters())
    {
      LLVM.LLVMBuildStore(builder, LLVM.LLVMGetParam(llvmFunction, p.getIndex()), variables.get(p.getVariable()));
    }

    buildStatement(function.getBlock(), llvmFunction, variables);
  }

  private void buildStatement(Statement statement, LLVMValueRef llvmFunction, Map<Variable, LLVMValueRef> variables)
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assign = (AssignStatement) statement;
      LLVMValueRef value = buildExpression(assign.getExpression(), variables);
      LLVMValueRef allocaInst = variables.get(assign.getResolvedVariable());
      if (allocaInst == null)
      {
        throw new IllegalStateException("Missing LLVMValueRef in variable Map: " + assign.getVariableName());
      }
      LLVM.LLVMBuildStore(builder, value, allocaInst);
    }
    else if (statement instanceof Block)
    {
      for (Statement s : ((Block) statement).getStatements())
      {
        buildStatement(s, llvmFunction, variables);
      }
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      LLVMValueRef conditional = buildExpression(ifStatement.getExpression(), variables);

      LLVMBasicBlockRef thenClause = LLVM.LLVMAppendBasicBlock(llvmFunction, "then");
      LLVMBasicBlockRef elseClause = null;
      if (ifStatement.getElseClause() != null)
      {
        elseClause = LLVM.LLVMAppendBasicBlock(llvmFunction, "else");
      }
      LLVMBasicBlockRef continuation = null;
      if (!ifStatement.stopsExecution())
      {
        continuation = LLVM.LLVMAppendBasicBlock(llvmFunction, "continuation");
      }

      // build the branch instruction
      if (elseClause == null)
      {
        // if we have no else clause, then a continuation must have been created, since the if statement cannot stop execution
        LLVM.LLVMBuildCondBr(builder, conditional, thenClause, continuation);
      }
      else
      {
        LLVM.LLVMBuildCondBr(builder, conditional, thenClause, elseClause);

        // build the else clause
        LLVM.LLVMPositionBuilderAtEnd(builder, elseClause);
        buildStatement(ifStatement.getElseClause(), llvmFunction, variables);
        if (!ifStatement.getElseClause().stopsExecution())
        {
          LLVM.LLVMBuildBr(builder, continuation);
        }
      }

      // build the then clause
      LLVM.LLVMPositionBuilderAtEnd(builder, thenClause);
      buildStatement(ifStatement.getThenClause(), llvmFunction, variables);
      if (!ifStatement.getThenClause().stopsExecution())
      {
        LLVM.LLVMBuildBr(builder, continuation);
      }

      if (continuation != null)
      {
        LLVM.LLVMPositionBuilderAtEnd(builder, continuation);
      }
    }
    else if (statement instanceof ReturnStatement)
    {
      LLVMValueRef value = buildExpression(((ReturnStatement) statement).getExpression(), variables);
      LLVM.LLVMBuildRet(builder, value);
    }
    else if (statement instanceof VariableDefinition)
    {
      VariableDefinition definition = (VariableDefinition) statement;
      if (definition.getExpression() != null)
      {
        LLVMValueRef allocaInst = variables.get(definition.getVariable());
        if (allocaInst == null)
        {
          throw new IllegalStateException("Missing LLVMValueRef in variable Map: " + definition.getName().getName());
        }
        LLVMValueRef value = buildExpression(definition.getExpression(), variables);
        LLVM.LLVMBuildStore(builder, value, allocaInst);
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;

      LLVMBasicBlockRef loopCheck = LLVM.LLVMAppendBasicBlock(llvmFunction, "loopCheck");
      LLVM.LLVMBuildBr(builder, loopCheck);

      LLVM.LLVMPositionBuilderAtEnd(builder, loopCheck);
      LLVMValueRef conditional = buildExpression(whileStatement.getExpression(), variables);

      LLVMBasicBlockRef loopBodyBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "loopBody");
      LLVMBasicBlockRef afterLoopBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "afterLoop");
      LLVM.LLVMBuildCondBr(builder, conditional, loopBodyBlock, afterLoopBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, loopBodyBlock);
      buildStatement(whileStatement.getStatement(), llvmFunction, variables);

      if (!whileStatement.stopsExecution())
      {
        LLVM.LLVMBuildBr(builder, loopCheck);
      }

      LLVM.LLVMPositionBuilderAtEnd(builder, afterLoopBlock);
    }
  }

  private PrimitiveType findArithmeticResultType(PrimitiveType left, PrimitiveType right)
  {
    // if either type is DOUBLE, then return DOUBLE
    // otherwise, return INT
    if (right.getPrimitiveTypeType() == PrimitiveTypeType.DOUBLE)
    {
      return right;
    }
    return left;
  }

  private LLVMValueRef convertNumericType(LLVMValueRef value, PrimitiveType from, PrimitiveType to)
  {
    if (from.getPrimitiveTypeType() == PrimitiveTypeType.DOUBLE && to.getPrimitiveTypeType() == PrimitiveTypeType.INT)
    {
      return LLVM.LLVMBuildFPToSI(builder, value, findNativeType(to), "");
    }
    if (from.getPrimitiveTypeType() == PrimitiveTypeType.INT && to.getPrimitiveTypeType() == PrimitiveTypeType.DOUBLE)
    {
      return LLVM.LLVMBuildSIToFP(builder, value, findNativeType(to), "");
    }
    if (from.getPrimitiveTypeType() == to.getPrimitiveTypeType())
    {
      return value;
    }
    throw new IllegalArgumentException("Unknown type conversion, from '" + from + "' to '" + to + "'");
  }

  private int getPredicate(ComparisonOperator operator, PrimitiveTypeType type)
  {
    if (type == PrimitiveTypeType.DOUBLE)
    {
      switch (operator)
      {
      case EQUAL:
        return LLVM.LLVMRealPredicate.LLVMRealOEQ;
      case LESS_THAN:
        return LLVM.LLVMRealPredicate.LLVMRealOLT;
      case LESS_THAN_EQUAL:
        return LLVM.LLVMRealPredicate.LLVMRealOLE;
      case MORE_THAN:
        return LLVM.LLVMRealPredicate.LLVMRealOGT;
      case MORE_THAN_EQUAL:
        return LLVM.LLVMRealPredicate.LLVMRealOGE;
      case NOT_EQUAL:
        return LLVM.LLVMRealPredicate.LLVMRealONE;
      }
    }
    else if (type == PrimitiveTypeType.INT)
    {
      switch (operator)
      {
      case EQUAL:
        return LLVM.LLVMIntPredicate.LLVMIntEQ;
      case LESS_THAN:
        return LLVM.LLVMIntPredicate.LLVMIntSLT;
      case LESS_THAN_EQUAL:
        return LLVM.LLVMIntPredicate.LLVMIntSLE;
      case MORE_THAN:
        return LLVM.LLVMIntPredicate.LLVMIntSGT;
      case MORE_THAN_EQUAL:
        return LLVM.LLVMIntPredicate.LLVMIntSGE;
      case NOT_EQUAL:
        return LLVM.LLVMIntPredicate.LLVMIntNE;
      }
    }
    throw new IllegalArgumentException("Unknown predicate '" + operator + "' for type '" + type + "'");
  }

  private LLVMValueRef buildExpression(Expression expression, Map<Variable, LLVMValueRef> variables)
  {
    if (expression instanceof AdditionExpression)
    {
      AdditionExpression additionExpression = (AdditionExpression) expression;
      LLVMValueRef left = buildExpression(additionExpression.getLeftSubExpression(), variables);
      LLVMValueRef right = buildExpression(additionExpression.getRightSubExpression(), variables);
      PrimitiveType leftType = (PrimitiveType) additionExpression.getLeftSubExpression().getType();
      PrimitiveType rightType = (PrimitiveType) additionExpression.getRightSubExpression().getType();
      // cast if necessary
      PrimitiveType resultType = findArithmeticResultType(leftType, rightType);
      left = convertNumericType(left, leftType, resultType);
      right = convertNumericType(right, rightType, resultType);
      if (resultType.getPrimitiveTypeType() == PrimitiveTypeType.DOUBLE)
      {
        return LLVM.LLVMBuildFAdd(builder, left, right, "");
      }
      return LLVM.LLVMBuildAdd(builder, left, right, "");
    }
    if (expression instanceof BooleanLiteralExpression)
    {
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), ((BooleanLiteralExpression) expression).getValue() ? 1 : 0, false);
    }
    if (expression instanceof BracketedExpression)
    {
      return buildExpression(((BracketedExpression) expression).getExpression(), variables);
    }
    if (expression instanceof ComparisonExpression)
    {
      ComparisonExpression comparisonExpression = (ComparisonExpression) expression;
      LLVMValueRef left = buildExpression(comparisonExpression.getLeftSubExpression(), variables);
      LLVMValueRef right = buildExpression(comparisonExpression.getRightSubExpression(), variables);
      PrimitiveType leftType = (PrimitiveType) comparisonExpression.getLeftSubExpression().getType();
      PrimitiveType rightType = (PrimitiveType) comparisonExpression.getRightSubExpression().getType();
      // cast if necessary
      PrimitiveType resultType = findArithmeticResultType(leftType, rightType);
      left = convertNumericType(left, leftType, resultType);
      right = convertNumericType(right, rightType, resultType);
      if (resultType.getPrimitiveTypeType() == PrimitiveTypeType.DOUBLE)
      {
        return LLVM.LLVMBuildFCmp(builder, getPredicate(comparisonExpression.getOperator(), resultType.getPrimitiveTypeType()), left, right, "");
      }
      return LLVM.LLVMBuildICmp(builder, getPredicate(comparisonExpression.getOperator(), resultType.getPrimitiveTypeType()), left, right, "");
    }
    if (expression instanceof FloatingLiteralExpression)
    {
      double value = Double.parseDouble(((FloatingLiteralExpression) expression).getLiteral().toString());
      return LLVM.LLVMConstReal(LLVM.LLVMDoubleType(), value);
    }
    if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression functionExpression = (FunctionCallExpression) expression;
      Expression[] arguments = functionExpression.getArguments();
      LLVMValueRef[] values = new LLVMValueRef[arguments.length];
      for (int i = 0; i < arguments.length; i++)
      {
        values[i] = buildExpression(arguments[i], variables);
      }
      Pointer llvmArguments = C.toNativePointerArray(values, false, true);
      LLVMValueRef llvmResolvedFunction = LLVM.LLVMGetNamedFunction(module, functionExpression.getResolvedFunction().getName());
      return LLVM.LLVMBuildCall(builder, llvmResolvedFunction, llvmArguments, values.length, "");
    }
    if (expression instanceof IntegerLiteralExpression)
    {
      int n = ((IntegerLiteralExpression) expression).getLiteral().getValue().intValue();
      return LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), n, false);
    }
    if (expression instanceof SubtractionExpression)
    {
      SubtractionExpression subtractionExpression = (SubtractionExpression) expression;
      LLVMValueRef left = buildExpression(subtractionExpression.getLeftSubExpression(), variables);
      LLVMValueRef right = buildExpression(subtractionExpression.getRightSubExpression(), variables);
      PrimitiveType leftType = (PrimitiveType) subtractionExpression.getLeftSubExpression().getType();
      PrimitiveType rightType = (PrimitiveType) subtractionExpression.getRightSubExpression().getType();
      // cast if necessary
      PrimitiveType resultType = findArithmeticResultType(leftType, rightType);
      left = convertNumericType(left, leftType, resultType);
      right = convertNumericType(right, rightType, resultType);
      if (resultType.getPrimitiveTypeType() == PrimitiveTypeType.DOUBLE)
      {
        return LLVM.LLVMBuildFSub(builder, left, right, "");
      }
      return LLVM.LLVMBuildSub(builder, left, right, "");
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
    throw new IllegalArgumentException("Unknown Expression type");
  }
}
