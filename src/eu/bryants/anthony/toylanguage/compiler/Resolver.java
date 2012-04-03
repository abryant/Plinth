package eu.bryants.anthony.toylanguage.compiler;

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
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Resolver
{

  public static void resolve(CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    for (Function function : compilationUnit.getFunctions())
    {
      resolve(function.getExpression(), function, compilationUnit);
    }
  }

  private static void resolve(Expression expression, Function function, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    if (expression instanceof AdditiveExpression)
    {
      resolve(((AdditiveExpression) expression).getLeftSubExpression(), function, compilationUnit);
      resolve(((AdditiveExpression) expression).getRightSubExpression(), function, compilationUnit);
    }
    else if (expression instanceof BracketedExpression)
    {
      resolve(((BracketedExpression) expression).getExpression(), function, compilationUnit);
    }
    else if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression expr = (FunctionCallExpression) expression;
      // resolve the called function
      Function called = compilationUnit.getFunction(expr.getName());
      if (called == null)
      {
        throw new NameNotResolvedException("Unable to resolve \"" + expr.getName() + "\"", expr.getLexicalPhrase());
      }
      expr.setResolvedFunction(called);

      // resolve all of the sub-expressions
      for (Expression e : expr.getArguments())
      {
        resolve(e, function, compilationUnit);
      }
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof VariableExpression)
    {
      VariableExpression expr = (VariableExpression) expression;
      Parameter parameter = function.getParameter(expr.getName());
      if (parameter == null)
      {
        throw new NameNotResolvedException("Unable to resolve \"" + expr.getName() + "\"", expr.getLexicalPhrase());
      }
      expr.setResolvedParameter(parameter);
    }
  }

}
