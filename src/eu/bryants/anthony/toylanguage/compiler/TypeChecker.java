package eu.bryants.anthony.toylanguage.compiler;

import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.Function;
import eu.bryants.anthony.toylanguage.ast.Parameter;
import eu.bryants.anthony.toylanguage.ast.expression.AdditionExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BooleanLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BracketedExpression;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.SubtractionExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
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
    }
    else
    {
      throw new ConceptualException("Internal type checking error: Unknown statement type", statement.getLexicalPhrase());
    }
  }

  private static Type checkTypes(Expression expression, CompilationUnit compilationUnit) throws ConceptualException
  {
    if (expression instanceof AdditionExpression)
    {
      AdditionExpression additionExpression = (AdditionExpression) expression;
      Type leftType = checkTypes(additionExpression.getLeftSubExpression(), compilationUnit);
      Type rightType = checkTypes(additionExpression.getRightSubExpression(), compilationUnit);
      if ((leftType instanceof PrimitiveType) && (rightType instanceof PrimitiveType))
      {
        PrimitiveTypeType leftPrimitiveType = ((PrimitiveType) leftType).getPrimitiveTypeType();
        PrimitiveTypeType rightPrimitiveType = ((PrimitiveType) rightType).getPrimitiveTypeType();
        if (leftPrimitiveType == PrimitiveTypeType.INT    && rightPrimitiveType == PrimitiveTypeType.INT ||
            leftPrimitiveType == PrimitiveTypeType.DOUBLE && rightPrimitiveType == PrimitiveTypeType.DOUBLE)
        {
          return leftType;
        }
      }
      throw new ConceptualException("Addition is not defined for types '" + leftType + "' and '" + rightType + "'", additionExpression.getLexicalPhrase());
    }
    else if (expression instanceof BooleanLiteralExpression)
    {
      return new PrimitiveType(PrimitiveTypeType.BOOLEAN, null);
    }
    else if (expression instanceof BracketedExpression)
    {
      return checkTypes(((BracketedExpression) expression).getExpression(), compilationUnit);
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      return new PrimitiveType(PrimitiveTypeType.DOUBLE, null);
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
      return resolvedFunction.getType();
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      return new PrimitiveType(PrimitiveTypeType.INT, null);
    }
    else if (expression instanceof SubtractionExpression)
    {
      SubtractionExpression subtractionExpression = (SubtractionExpression) expression;
      Type leftType = checkTypes(subtractionExpression.getLeftSubExpression(), compilationUnit);
      Type rightType = checkTypes(subtractionExpression.getRightSubExpression(), compilationUnit);
      if ((leftType instanceof PrimitiveType) && (rightType instanceof PrimitiveType))
      {
        PrimitiveTypeType leftPrimitiveType = ((PrimitiveType) leftType).getPrimitiveTypeType();
        PrimitiveTypeType rightPrimitiveType = ((PrimitiveType) rightType).getPrimitiveTypeType();
        if (leftPrimitiveType == PrimitiveTypeType.INT    && rightPrimitiveType == PrimitiveTypeType.INT ||
            leftPrimitiveType == PrimitiveTypeType.DOUBLE && rightPrimitiveType == PrimitiveTypeType.DOUBLE)
        {
          return leftType;
        }
      }
      throw new ConceptualException("Subtraction is not defined for types '" + leftType + "' and '" + rightType + "'", subtractionExpression.getLexicalPhrase());
    }
    else if (expression instanceof VariableExpression)
    {
      return ((VariableExpression) expression).getResolvedVariable().getType();
    }
    throw new ConceptualException("Internal type checking error: Unknown expression type", expression.getLexicalPhrase());
  }
}
