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
import eu.bryants.anthony.toylanguage.ast.expression.ArithmeticExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BitwiseNotExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BooleanLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BooleanNotExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BracketedExpression;
import eu.bryants.anthony.toylanguage.ast.expression.CastExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ComparisonExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ComparisonExpression.ComparisonOperator;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.LogicalExpression;
import eu.bryants.anthony.toylanguage.ast.expression.LogicalExpression.LogicalOperator;
import eu.bryants.anthony.toylanguage.ast.expression.MinusExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.statement.BreakStatement;
import eu.bryants.anthony.toylanguage.ast.statement.BreakableStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ContinueStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ExpressionStatement;
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
      PrimitiveTypeType primitiveTypeType = ((PrimitiveType) type).getPrimitiveTypeType();
      if (primitiveTypeType == PrimitiveTypeType.DOUBLE)
      {
        return LLVM.LLVMDoubleType();
      }
      if (primitiveTypeType == PrimitiveTypeType.FLOAT)
      {
        return LLVM.LLVMFloatType();
      }
      return LLVM.LLVMIntType(primitiveTypeType.getBitCount());
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

    buildStatement(function.getBlock(), function, llvmFunction, variables, new HashMap<BreakableStatement, LLVM.LLVMBasicBlockRef>(), new HashMap<BreakableStatement, LLVM.LLVMBasicBlockRef>());
  }

  private void buildStatement(Statement statement, Function function, LLVMValueRef llvmFunction, Map<Variable, LLVMValueRef> variables,
                              Map<BreakableStatement, LLVMBasicBlockRef> breakBlocks, Map<BreakableStatement, LLVMBasicBlockRef> continueBlocks)
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assign = (AssignStatement) statement;
      LLVMValueRef value = buildExpression(assign.getExpression(), llvmFunction, variables);
      LLVMValueRef convertedValue = convertType(value, assign.getExpression().getType(), assign.getResolvedVariable().getType());
      LLVMValueRef allocaInst = variables.get(assign.getResolvedVariable());
      if (allocaInst == null)
      {
        throw new IllegalStateException("Missing LLVMValueRef in variable Map: " + assign.getVariableName());
      }
      LLVM.LLVMBuildStore(builder, convertedValue, allocaInst);
    }
    else if (statement instanceof Block)
    {
      for (Statement s : ((Block) statement).getStatements())
      {
        buildStatement(s, function, llvmFunction, variables, breakBlocks, continueBlocks);
      }
    }
    else if (statement instanceof BreakStatement)
    {
      LLVMBasicBlockRef block = breakBlocks.get(((BreakStatement) statement).getResolvedBreakable());
      if (block == null)
      {
        throw new IllegalStateException("Break statement leads to a null block during code generation: " + statement);
      }
      LLVM.LLVMBuildBr(builder, block);
    }
    else if (statement instanceof ContinueStatement)
    {
      LLVMBasicBlockRef block = continueBlocks.get(((ContinueStatement) statement).getResolvedBreakable());
      if (block == null)
      {
        throw new IllegalStateException("Continue statement leads to a null block during code generation: " + statement);
      }
      LLVM.LLVMBuildBr(builder, block);
    }
    else if (statement instanceof ExpressionStatement)
    {
      buildExpression(((ExpressionStatement) statement).getExpression(), llvmFunction, variables);
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      LLVMValueRef conditional = buildExpression(ifStatement.getExpression(), llvmFunction, variables);

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
        buildStatement(ifStatement.getElseClause(), function, llvmFunction, variables, breakBlocks, continueBlocks);
        if (!ifStatement.getElseClause().stopsExecution())
        {
          LLVM.LLVMBuildBr(builder, continuation);
        }
      }

      // build the then clause
      LLVM.LLVMPositionBuilderAtEnd(builder, thenClause);
      buildStatement(ifStatement.getThenClause(), function, llvmFunction, variables, breakBlocks, continueBlocks);
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
      LLVMValueRef value = buildExpression(((ReturnStatement) statement).getExpression(), llvmFunction, variables);
      LLVMValueRef convertedValue = convertType(value, ((ReturnStatement) statement).getExpression().getType(), function.getType());
      LLVM.LLVMBuildRet(builder, convertedValue);
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
        LLVMValueRef value = buildExpression(definition.getExpression(), llvmFunction, variables);
        LLVMValueRef convertedValue = convertType(value, definition.getExpression().getType(), definition.getType());
        LLVM.LLVMBuildStore(builder, convertedValue, allocaInst);
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;

      LLVMBasicBlockRef loopCheck = LLVM.LLVMAppendBasicBlock(llvmFunction, "loopCheck");
      LLVM.LLVMBuildBr(builder, loopCheck);

      LLVM.LLVMPositionBuilderAtEnd(builder, loopCheck);
      LLVMValueRef conditional = buildExpression(whileStatement.getExpression(), llvmFunction, variables);

      LLVMBasicBlockRef loopBodyBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "loopBody");
      LLVMBasicBlockRef afterLoopBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "afterLoop");
      LLVM.LLVMBuildCondBr(builder, conditional, loopBodyBlock, afterLoopBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, loopBodyBlock);
      // add the while statement's afterLoop block to the breakBlocks map before it's statement is built
      breakBlocks.put(whileStatement, afterLoopBlock);
      continueBlocks.put(whileStatement, loopCheck);
      buildStatement(whileStatement.getStatement(), function, llvmFunction, variables, breakBlocks, continueBlocks);

      if (!whileStatement.getStatement().stopsExecution())
      {
        LLVM.LLVMBuildBr(builder, loopCheck);
      }

      LLVM.LLVMPositionBuilderAtEnd(builder, afterLoopBlock);
    }
  }

  private LLVMValueRef convertType(LLVMValueRef value, Type from, Type to)
  {
    if (from instanceof PrimitiveType && to instanceof PrimitiveType)
    {
      return convertPrimitiveType(value, (PrimitiveType) from, (PrimitiveType) to);
    }
    throw new IllegalArgumentException("Unknown type conversion, from '" + from + "' to '" + to + "'");
  }

  private LLVMValueRef convertPrimitiveType(LLVMValueRef value, PrimitiveType from, PrimitiveType to)
  {
    PrimitiveTypeType fromType = from.getPrimitiveTypeType();
    PrimitiveTypeType toType = to.getPrimitiveTypeType();
    if (fromType == toType)
    {
      return value;
    }
    LLVMTypeRef toNativeType = findNativeType(to);
    if (fromType.isFloating() && toType.isFloating())
    {
      return LLVM.LLVMBuildFPCast(builder, value, toNativeType, "");
    }
    if (fromType.isFloating() && !toType.isFloating())
    {
      if (toType.isSigned())
      {
        return LLVM.LLVMBuildFPToSI(builder, value, toNativeType, "");
      }
      return LLVM.LLVMBuildFPToUI(builder, value, toNativeType, "");
    }
    if (!fromType.isFloating() && toType.isFloating())
    {
      if (fromType.isSigned())
      {
        return LLVM.LLVMBuildSIToFP(builder, value, toNativeType, "");
      }
      return LLVM.LLVMBuildUIToFP(builder, value, toNativeType, "");
    }
    // both integer types, so perform a sign-extend, zero-extend, or truncation
    if (fromType.getBitCount() > toType.getBitCount())
    {
      return LLVM.LLVMBuildTrunc(builder, value, toNativeType, "");
    }
    if (fromType.getBitCount() == toType.getBitCount() && fromType.isSigned() != toType.isSigned())
    {
      return LLVM.LLVMBuildBitCast(builder, value, toNativeType, "");
    }
    // the value needs extending, so decide whether to do a sign-extend or a zero-extend based on whether the from type is signed
    if (fromType.isSigned())
    {
      return LLVM.LLVMBuildSExt(builder, value, toNativeType, "");
    }
    return LLVM.LLVMBuildZExt(builder, value, toNativeType, "");
  }

  private int getPredicate(ComparisonOperator operator, boolean floating, boolean signed)
  {
    if (floating)
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
    else
    {
      switch (operator)
      {
      case EQUAL:
        return LLVM.LLVMIntPredicate.LLVMIntEQ;
      case LESS_THAN:
        return signed ? LLVM.LLVMIntPredicate.LLVMIntSLT : LLVM.LLVMIntPredicate.LLVMIntULT;
      case LESS_THAN_EQUAL:
        return signed ? LLVM.LLVMIntPredicate.LLVMIntSLE : LLVM.LLVMIntPredicate.LLVMIntULE;
      case MORE_THAN:
        return signed ? LLVM.LLVMIntPredicate.LLVMIntSGT : LLVM.LLVMIntPredicate.LLVMIntUGT;
      case MORE_THAN_EQUAL:
        return signed ? LLVM.LLVMIntPredicate.LLVMIntSGE : LLVM.LLVMIntPredicate.LLVMIntUGE;
      case NOT_EQUAL:
        return LLVM.LLVMIntPredicate.LLVMIntNE;
      }
    }
    throw new IllegalArgumentException("Unknown predicate '" + operator + "'");
  }

  private LLVMValueRef buildExpression(Expression expression, LLVMValueRef llvmFunction, Map<Variable, LLVMValueRef> variables)
  {
    if (expression instanceof ArithmeticExpression)
    {
      ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;
      LLVMValueRef left = buildExpression(arithmeticExpression.getLeftSubExpression(), llvmFunction, variables);
      LLVMValueRef right = buildExpression(arithmeticExpression.getRightSubExpression(), llvmFunction, variables);
      PrimitiveType leftType = (PrimitiveType) arithmeticExpression.getLeftSubExpression().getType();
      PrimitiveType rightType = (PrimitiveType) arithmeticExpression.getRightSubExpression().getType();
      // cast if necessary
      PrimitiveType resultType = (PrimitiveType) arithmeticExpression.getType();
      left = convertPrimitiveType(left, leftType, resultType);
      right = convertPrimitiveType(right, rightType, resultType);
      boolean floating = resultType.getPrimitiveTypeType().isFloating();
      boolean signed = resultType.getPrimitiveTypeType().isSigned();
      switch (arithmeticExpression.getOperator())
      {
      case ADD:
        return floating ? LLVM.LLVMBuildFAdd(builder, left, right, "") : LLVM.LLVMBuildAdd(builder, left, right, "");
      case SUBTRACT:
        return floating ? LLVM.LLVMBuildFSub(builder, left, right, "") : LLVM.LLVMBuildSub(builder, left, right, "");
      case MULTIPLY:
        return floating ? LLVM.LLVMBuildFMul(builder, left, right, "") : LLVM.LLVMBuildMul(builder, left, right, "");
      case DIVIDE:
        return floating ? LLVM.LLVMBuildFDiv(builder, left, right, "") : signed ? LLVM.LLVMBuildSDiv(builder, left, right, "") : LLVM.LLVMBuildUDiv(builder, left, right, "");
      case REMAINDER:
        return floating ? LLVM.LLVMBuildFRem(builder, left, right, "") : signed ? LLVM.LLVMBuildSRem(builder, left, right, "") : LLVM.LLVMBuildURem(builder, left, right, "");
      case MODULO:
        if (floating)
        {
          LLVMValueRef rem = LLVM.LLVMBuildFRem(builder, left, right, "");
          LLVMValueRef add = LLVM.LLVMBuildFAdd(builder, rem, right, "");
          return LLVM.LLVMBuildFRem(builder, add, right, "");
        }
        if (signed)
        {
          LLVMValueRef rem = LLVM.LLVMBuildSRem(builder, left, right, "");
          LLVMValueRef add = LLVM.LLVMBuildAdd(builder, rem, right, "");
          return LLVM.LLVMBuildSRem(builder, add, right, "");
        }
        // unsigned modulo is the same as unsigned remainder
        return LLVM.LLVMBuildURem(builder, left, right, "");
      }
      throw new IllegalArgumentException("Unknown arithmetic operator: " + arithmeticExpression.getOperator());
    }
    if (expression instanceof BitwiseNotExpression)
    {
      LLVMValueRef value = buildExpression(((BitwiseNotExpression) expression).getExpression(), llvmFunction, variables);
      return LLVM.LLVMBuildNot(builder, value, "");
    }
    if (expression instanceof BooleanLiteralExpression)
    {
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), ((BooleanLiteralExpression) expression).getValue() ? 1 : 0, false);
    }
    if (expression instanceof BooleanNotExpression)
    {
      LLVMValueRef value = buildExpression(((BooleanNotExpression) expression).getExpression(), llvmFunction, variables);
      return LLVM.LLVMBuildNot(builder, value, "");
    }
    if (expression instanceof BracketedExpression)
    {
      return buildExpression(((BracketedExpression) expression).getExpression(), llvmFunction, variables);
    }
    if (expression instanceof CastExpression)
    {
      CastExpression castExpression = (CastExpression) expression;
      LLVMValueRef value = buildExpression(castExpression.getExpression(), llvmFunction, variables);
      return convertType(value, castExpression.getExpression().getType(), castExpression.getType());
    }
    if (expression instanceof ComparisonExpression)
    {
      ComparisonExpression comparisonExpression = (ComparisonExpression) expression;
      LLVMValueRef left = buildExpression(comparisonExpression.getLeftSubExpression(), llvmFunction, variables);
      LLVMValueRef right = buildExpression(comparisonExpression.getRightSubExpression(), llvmFunction, variables);
      PrimitiveType leftType = (PrimitiveType) comparisonExpression.getLeftSubExpression().getType();
      PrimitiveType rightType = (PrimitiveType) comparisonExpression.getRightSubExpression().getType();
      // cast if necessary
      PrimitiveType resultType = comparisonExpression.getComparisonType();
      if (resultType == null)
      {
        PrimitiveTypeType leftTypeType = leftType.getPrimitiveTypeType();
        PrimitiveTypeType rightTypeType = rightType.getPrimitiveTypeType();
        if (!leftTypeType.isFloating() && !rightTypeType.isFloating() &&
            leftTypeType.getBitCount() == rightTypeType.getBitCount() &&
            leftTypeType.isSigned() != rightTypeType.isSigned())
        {
          // compare the signed and non-signed integers as (bitCount + 1) bit numbers, since they will not fit in bitCount bits
          LLVMTypeRef comparisonType = LLVM.LLVMIntType(leftType.getPrimitiveTypeType().getBitCount() + 1);
          if (leftTypeType.isSigned())
          {
            left = LLVM.LLVMBuildSExt(builder, left, comparisonType, "");
            right = LLVM.LLVMBuildZExt(builder, right, comparisonType, "");
          }
          else
          {
            left = LLVM.LLVMBuildZExt(builder, left, comparisonType, "");
            right = LLVM.LLVMBuildSExt(builder, right, comparisonType, "");
          }
          return LLVM.LLVMBuildICmp(builder, getPredicate(comparisonExpression.getOperator(), false, true), left, right, "");
        }
        throw new IllegalArgumentException("Unknown result type, unable to generate comparison expression: " + expression);
      }
      left = convertPrimitiveType(left, leftType, resultType);
      right = convertPrimitiveType(right, rightType, resultType);
      if (resultType.getPrimitiveTypeType().isFloating())
      {
        return LLVM.LLVMBuildFCmp(builder, getPredicate(comparisonExpression.getOperator(), true, true), left, right, "");
      }
      return LLVM.LLVMBuildICmp(builder, getPredicate(comparisonExpression.getOperator(), false, resultType.getPrimitiveTypeType().isSigned()), left, right, "");
    }
    if (expression instanceof FloatingLiteralExpression)
    {
      double value = Double.parseDouble(((FloatingLiteralExpression) expression).getLiteral().toString());
      return LLVM.LLVMConstReal(findNativeType(expression.getType()), value);
    }
    if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression functionExpression = (FunctionCallExpression) expression;
      Parameter[] parameters = functionExpression.getResolvedFunction().getParameters();
      Expression[] arguments = functionExpression.getArguments();
      LLVMValueRef[] values = new LLVMValueRef[arguments.length];
      for (int i = 0; i < arguments.length; i++)
      {
        LLVMValueRef arg = buildExpression(arguments[i], llvmFunction, variables);
        values[i] = convertType(arg, arguments[i].getType(), parameters[i].getType());
      }
      Pointer llvmArguments = C.toNativePointerArray(values, false, true);
      LLVMValueRef llvmResolvedFunction = LLVM.LLVMGetNamedFunction(module, functionExpression.getResolvedFunction().getName());
      return LLVM.LLVMBuildCall(builder, llvmResolvedFunction, llvmArguments, values.length, "");
    }
    if (expression instanceof IntegerLiteralExpression)
    {
      int n = ((IntegerLiteralExpression) expression).getLiteral().getValue().intValue();
      return LLVM.LLVMConstInt(findNativeType(expression.getType()), n, false);
    }
    if (expression instanceof LogicalExpression)
    {
      LogicalExpression logicalExpression = (LogicalExpression) expression;
      LLVMValueRef left = buildExpression(logicalExpression.getLeftSubExpression(), llvmFunction, variables);
      PrimitiveType leftType = (PrimitiveType) logicalExpression.getLeftSubExpression().getType();
      PrimitiveType rightType = (PrimitiveType) logicalExpression.getRightSubExpression().getType();
      // cast if necessary
      PrimitiveType resultType = (PrimitiveType) logicalExpression.getType();
      left = convertPrimitiveType(left, leftType, resultType);
      LogicalOperator operator = logicalExpression.getOperator();
      if (operator != LogicalOperator.SHORT_CIRCUIT_AND && operator != LogicalOperator.SHORT_CIRCUIT_OR)
      {
        LLVMValueRef right = buildExpression(logicalExpression.getRightSubExpression(), llvmFunction, variables);
        right = convertPrimitiveType(right, rightType, resultType);
        switch (operator)
        {
        case AND:
          return LLVM.LLVMBuildAnd(builder, left, right, "");
        case OR:
          return LLVM.LLVMBuildOr(builder, left, right, "");
        case XOR:
          return LLVM.LLVMBuildXor(builder, left, right, "");
        default:
          throw new IllegalStateException("Unexpected non-short-circuit operator: " + logicalExpression.getOperator());
        }
      }
      LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVMBasicBlockRef rightCheckBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "shortCircuitCheck");
      LLVMBasicBlockRef continuationBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "shortCircuitContinue");
      // the only difference between short circuit AND and OR is whether they jump to the check block when the left hand side is true or false
      LLVMBasicBlockRef trueDest = operator == LogicalOperator.SHORT_CIRCUIT_AND ? rightCheckBlock : continuationBlock;
      LLVMBasicBlockRef falseDest = operator == LogicalOperator.SHORT_CIRCUIT_AND ? continuationBlock : rightCheckBlock;
      LLVM.LLVMBuildCondBr(builder, left, trueDest, falseDest);

      LLVM.LLVMPositionBuilderAtEnd(builder, rightCheckBlock);
      LLVMValueRef right = buildExpression(logicalExpression.getRightSubExpression(), llvmFunction, variables);
      right = convertPrimitiveType(right, rightType, resultType);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
      // create a phi node for the result, and return it
      LLVMValueRef phi = LLVM.LLVMBuildPhi(builder, findNativeType(resultType), "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {left, right};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {currentBlock, rightCheckBlock};
      LLVM.LLVMAddIncoming(phi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), 2);
      return phi;
    }
    if (expression instanceof MinusExpression)
    {
      MinusExpression minusExpression = (MinusExpression) expression;
      LLVMValueRef value = buildExpression(minusExpression.getExpression(), llvmFunction, variables);
      value = convertPrimitiveType(value, (PrimitiveType) minusExpression.getExpression().getType(), (PrimitiveType) minusExpression.getType());
      PrimitiveTypeType primitiveTypeType = ((PrimitiveType) minusExpression.getType()).getPrimitiveTypeType();
      if (primitiveTypeType.isFloating())
      {
        return LLVM.LLVMBuildFNeg(builder, value, "");
      }
      return LLVM.LLVMBuildNeg(builder, value, "");
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
