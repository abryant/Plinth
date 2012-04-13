package eu.bryants.anthony.toylanguage.compiler.passes;

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
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.statement.BreakStatement;
import eu.bryants.anthony.toylanguage.ast.statement.IfStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ReturnStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Statement;
import eu.bryants.anthony.toylanguage.ast.statement.VariableDefinition;
import eu.bryants.anthony.toylanguage.ast.statement.WhileStatement;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.compiler.ConceptualException;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class TypeChecker
{
  public static void checkTypes(CompilationUnit compilationUnit) throws ConceptualException
  {
    for (Function f : compilationUnit.getFunctions())
    {
      checkTypes(f.getBlock(), f, compilationUnit);
    }
  }

  private static void checkTypes(Statement statement, Function function, CompilationUnit compilationUnit) throws ConceptualException
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assign = (AssignStatement) statement;
      Type exprType = checkTypes(assign.getExpression(), compilationUnit);
      if (!assign.getResolvedVariable().getType().canAssign(exprType))
      {
        throw new ConceptualException("Cannot assign an expression of type '" + exprType + "' to a variable of type '" + assign.getResolvedVariable().getType() + "'", assign.getLexicalPhrase());
      }
    }
    else if (statement instanceof Block)
    {
      for (Statement s : ((Block) statement).getStatements())
      {
        checkTypes(s, function, compilationUnit);
      }
    }
    else if (statement instanceof BreakStatement)
    {
      // do nothing
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      Type exprType = checkTypes(ifStatement.getExpression(), compilationUnit);
      if (!(exprType instanceof PrimitiveType) || ((PrimitiveType) exprType).getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
      {
        throw new ConceptualException("A conditional must be of type '" + PrimitiveTypeType.BOOLEAN.name + "', not '" + exprType + "'", ifStatement.getExpression().getLexicalPhrase());
      }
      checkTypes(ifStatement.getThenClause(), function, compilationUnit);
      if (ifStatement.getElseClause() != null)
      {
        checkTypes(ifStatement.getElseClause(), function, compilationUnit);
      }
    }
    else if (statement instanceof ReturnStatement)
    {
      Type exprType = checkTypes(((ReturnStatement) statement).getExpression(), compilationUnit);
      if (!function.getType().canAssign(exprType))
      {
        throw new ConceptualException("Cannot return an expression of type '" + exprType + "' from a function with return type '" + function.getType() + "'", statement.getLexicalPhrase());
      }
    }
    else if (statement instanceof VariableDefinition)
    {
      VariableDefinition definition = (VariableDefinition) statement;
      Expression expression = definition.getExpression();
      if (expression != null)
      {
        Type exprType = checkTypes(expression, compilationUnit);
        if (!definition.getType().canAssign(exprType))
        {
          throw new ConceptualException("Cannot assign an expression of type '" + exprType + "' to a variable of type '" + definition.getType() + "'", definition.getLexicalPhrase());
        }
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      Type exprType = checkTypes(whileStatement.getExpression(), compilationUnit);
      if (!(exprType instanceof PrimitiveType) || ((PrimitiveType) exprType).getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
      {
        throw new ConceptualException("A conditional must be of type '" + PrimitiveTypeType.BOOLEAN.name + "', not '" + exprType + "'", whileStatement.getExpression().getLexicalPhrase());
      }
      checkTypes(whileStatement.getStatement(), function, compilationUnit);
    }
    else
    {
      throw new ConceptualException("Internal type checking error: Unknown statement type", statement.getLexicalPhrase());
    }
  }

  private static Type checkTypes(Expression expression, CompilationUnit compilationUnit) throws ConceptualException
  {
    if (expression instanceof ArithmeticExpression)
    {
      ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;
      Type leftType = checkTypes(arithmeticExpression.getLeftSubExpression(), compilationUnit);
      Type rightType = checkTypes(arithmeticExpression.getRightSubExpression(), compilationUnit);
      if ((leftType instanceof PrimitiveType) && (rightType instanceof PrimitiveType))
      {
        PrimitiveTypeType leftPrimitiveType = ((PrimitiveType) leftType).getPrimitiveTypeType();
        PrimitiveTypeType rightPrimitiveType = ((PrimitiveType) rightType).getPrimitiveTypeType();
        if (leftPrimitiveType != PrimitiveTypeType.BOOLEAN && rightPrimitiveType != PrimitiveTypeType.BOOLEAN)
        {
          if (rightPrimitiveType == PrimitiveTypeType.DOUBLE)
          {
            arithmeticExpression.setType(rightType);
            return rightType;
          }
          arithmeticExpression.setType(leftType);
          return leftType;
        }
      }
      throw new ConceptualException("The operator '" + arithmeticExpression.getOperator() + "' is not defined for types '" + leftType + "' and '" + rightType + "'", arithmeticExpression.getLexicalPhrase());
    }
    else if (expression instanceof BitwiseNotExpression)
    {
      Type type = checkTypes(((BitwiseNotExpression) expression).getExpression(), compilationUnit);
      if (type instanceof PrimitiveType)
      {
        PrimitiveTypeType primitiveTypeType = ((PrimitiveType) type).getPrimitiveTypeType();
        if (primitiveTypeType != PrimitiveTypeType.DOUBLE)
        {
          expression.setType(type);
          return type;
        }
      }
      throw new ConceptualException("The operator '~' is not defined for type '" + type + "'", expression.getLexicalPhrase());
    }
    else if (expression instanceof BooleanLiteralExpression)
    {
      Type type = new PrimitiveType(PrimitiveTypeType.BOOLEAN, null);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof BooleanNotExpression)
    {
      Type type = checkTypes(((BooleanNotExpression) expression).getExpression(), compilationUnit);
      if (type instanceof PrimitiveType && ((PrimitiveType) type).getPrimitiveTypeType() == PrimitiveTypeType.BOOLEAN)
      {
        expression.setType(type);
        return type;
      }
      throw new ConceptualException("The operator '!' is not defined for type '" + type + "'", expression.getLexicalPhrase());
    }
    else if (expression instanceof BracketedExpression)
    {
      Type type = checkTypes(((BracketedExpression) expression).getExpression(), compilationUnit);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof CastExpression)
    {
      Type exprType = checkTypes(((CastExpression) expression).getExpression(), compilationUnit);
      Type castedType = expression.getType();
      if (exprType.canAssign(castedType) || castedType.canAssign(exprType))
      {
        // if the assignment works in reverse (i.e. the casted type can be assigned to the expression) then it can be casted back
        // (also allow it if the assignment works forwards, although really that should be a warning about an unnecessary cast)
        return expression.getType();
      }
      throw new ConceptualException("Cannot cast from '" + exprType + "' to '" + castedType + "'", expression.getLexicalPhrase());
    }
    else if (expression instanceof ComparisonExpression)
    {
      ComparisonExpression comparisonExpression = (ComparisonExpression) expression;
      ComparisonOperator operator = comparisonExpression.getOperator();
      Type leftType = checkTypes(comparisonExpression.getLeftSubExpression(), compilationUnit);
      Type rightType = checkTypes(comparisonExpression.getRightSubExpression(), compilationUnit);
      if ((leftType instanceof PrimitiveType) && (rightType instanceof PrimitiveType))
      {
        PrimitiveTypeType leftPrimitiveType = ((PrimitiveType) leftType).getPrimitiveTypeType();
        PrimitiveTypeType rightPrimitiveType = ((PrimitiveType) rightType).getPrimitiveTypeType();
        if (leftPrimitiveType == PrimitiveTypeType.BOOLEAN && rightPrimitiveType == PrimitiveTypeType.BOOLEAN &&
            (comparisonExpression.getOperator() == ComparisonOperator.EQUAL || comparisonExpression.getOperator() == ComparisonOperator.NOT_EQUAL))
        {
          // comparing booleans is only valid when using '==' or '!='
          PrimitiveType type = new PrimitiveType(PrimitiveTypeType.BOOLEAN, null);
          comparisonExpression.setComparisonType(type);
          comparisonExpression.setType(type);
          return type;
        }
        if (leftPrimitiveType != PrimitiveTypeType.BOOLEAN && rightPrimitiveType != PrimitiveTypeType.BOOLEAN)
        {
          if (rightPrimitiveType == PrimitiveTypeType.DOUBLE)
          {
            comparisonExpression.setComparisonType((PrimitiveType) rightType);
          }
          else
          {
            comparisonExpression.setComparisonType((PrimitiveType) leftType);
          }
          // comparing any numeric types is always valid
          Type resultType = new PrimitiveType(PrimitiveTypeType.BOOLEAN, null);
          comparisonExpression.setType(resultType);
          return resultType;
        }
      }
      throw new ConceptualException("The '" + operator + "' operator is not defined for types '" + leftType + "' and '" + rightType + "'", comparisonExpression.getLexicalPhrase());
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      Type type = new PrimitiveType(PrimitiveTypeType.DOUBLE, null);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression functionCallExpression = (FunctionCallExpression) expression;
      Expression[] arguments = functionCallExpression.getArguments();
      Function resolvedFunction = functionCallExpression.getResolvedFunction();
      Parameter[] parameters = resolvedFunction.getParameters();
      if (arguments.length != parameters.length)
      {
        throw new ConceptualException("Function '" + resolvedFunction.getName() + "' is not defined to take " + arguments.length + " arguments", functionCallExpression.getLexicalPhrase());
      }
      for (int i = 0; i < arguments.length; i++)
      {
        Type type = checkTypes(arguments[i], compilationUnit);
        if (!parameters[i].getType().canAssign(type))
        {
          throw new ConceptualException("Cannot pass an argument of type '" + type + "' as a parameter of type '" + parameters[i].getType() + "'", arguments[i].getLexicalPhrase());
        }
      }
      Type type = resolvedFunction.getType();
      functionCallExpression.setType(type);
      return type;
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      Type type = new PrimitiveType(PrimitiveTypeType.INT, null);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof LogicalExpression)
    {
      LogicalExpression logicalExpression = (LogicalExpression) expression;
      Type leftType = checkTypes(logicalExpression.getLeftSubExpression(), compilationUnit);
      Type rightType = checkTypes(logicalExpression.getRightSubExpression(), compilationUnit);
      if ((leftType instanceof PrimitiveType) && (rightType instanceof PrimitiveType))
      {
        PrimitiveTypeType leftPrimitiveType = ((PrimitiveType) leftType).getPrimitiveTypeType();
        PrimitiveTypeType rightPrimitiveType = ((PrimitiveType) rightType).getPrimitiveTypeType();
        // disallow all floating types
        if (leftPrimitiveType != PrimitiveTypeType.DOUBLE && rightPrimitiveType != PrimitiveTypeType.DOUBLE)
        {
          // disallow short-circuit operators for any types but boolean
          if (logicalExpression.getOperator() == LogicalOperator.SHORT_CIRCUIT_AND || logicalExpression.getOperator() == LogicalOperator.SHORT_CIRCUIT_OR)
          {
            if (leftPrimitiveType == PrimitiveTypeType.BOOLEAN && rightPrimitiveType == PrimitiveTypeType.BOOLEAN)
            {
              logicalExpression.setType(leftType);
              return leftType;
            }
            throw new ConceptualException("The short-circuit operator '" + logicalExpression.getOperator() + "' is not defined for types '" + leftType + "' and '" + rightType + "'", logicalExpression.getLexicalPhrase());
          }
          // allow all (non-short-circuit) boolean/integer operations if the types match
          if (leftPrimitiveType == rightPrimitiveType)
          {
            logicalExpression.setType(leftType);
            return leftType;
          }
          // TODO: when multiple integer types exist, allow converting between integer operations by taking the longer integer type
        }
      }
      throw new ConceptualException("The operator '" + logicalExpression.getOperator() + "' is not defined for types '" + leftType + "' and '" + rightType + "'", logicalExpression.getLexicalPhrase());
    }
    else if (expression instanceof MinusExpression)
    {
      Type type = checkTypes(((MinusExpression) expression).getExpression(), compilationUnit);
      if (type instanceof PrimitiveType && ((PrimitiveType) type).getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
      {
        expression.setType(type);
        return type;
      }
      throw new ConceptualException("The unary operator '-' is not defined for type '" + type + "'", expression.getLexicalPhrase());
    }
    else if (expression instanceof VariableExpression)
    {
      Type type = ((VariableExpression) expression).getResolvedVariable().getType();
      expression.setType(type);
      return type;
    }
    throw new ConceptualException("Internal type checking error: Unknown expression type", expression.getLexicalPhrase());
  }
}
