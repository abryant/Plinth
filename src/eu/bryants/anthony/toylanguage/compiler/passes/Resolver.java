package eu.bryants.anthony.toylanguage.compiler.passes;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

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
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.LogicalExpression;
import eu.bryants.anthony.toylanguage.ast.expression.MinusExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.statement.BreakStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ContinueStatement;
import eu.bryants.anthony.toylanguage.ast.statement.IfStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ReturnStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Statement;
import eu.bryants.anthony.toylanguage.ast.statement.VariableDefinition;
import eu.bryants.anthony.toylanguage.ast.statement.WhileStatement;
import eu.bryants.anthony.toylanguage.compiler.ConceptualException;
import eu.bryants.anthony.toylanguage.compiler.NameNotResolvedException;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Resolver
{

  /**
   * Finds all of the nested variables of a function.
   * Before calling this, resolve() must have been called on the compilation unit containing the function.
   * @param function - the function to get all the nested variables of
   * @return a set containing all of the variables defined in this function, including in nested blocks
   */
  public static Set<Variable> getAllNestedVariables(Function function)
  {
    Set<Variable> result = new HashSet<Variable>();
    Deque<Statement> stack = new LinkedList<Statement>();
    stack.push(function.getBlock());
    while (!stack.isEmpty())
    {
      Statement statement = stack.pop();
      if (statement instanceof Block)
      {
        // add all variables from this block to the result set
        result.addAll(((Block) statement).getVariables());
        for (Statement s : ((Block) statement).getStatements())
        {
          stack.push(s);
        }
      }
      else if (statement instanceof IfStatement)
      {
        IfStatement ifStatement = (IfStatement) statement;
        stack.push(ifStatement.getThenClause());
        if (ifStatement.getElseClause() != null)
        {
          stack.push(ifStatement.getElseClause());
        }
      }
      else if (statement instanceof WhileStatement)
      {
        stack.push(((WhileStatement) statement).getStatement());
      }
    }
    return result;
  }

  public static void resolve(CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    for (Function function : compilationUnit.getFunctions())
    {
      resolve(function, compilationUnit);
    }
  }

  private static void resolve(Function function, CompilationUnit compilationUnit) throws ConceptualException, NameNotResolvedException
  {
    Block mainBlock = function.getBlock();
    for (Parameter p : function.getParameters())
    {
      Variable oldVar = mainBlock.addVariable(p.getVariable());
      if (oldVar != null)
      {
        throw new ConceptualException("Duplicate parameter: " + p.getName(), p.getLexicalPhrase());
      }
    }
    for (Statement s : mainBlock.getStatements())
    {
      resolve(s, mainBlock, compilationUnit);
    }
  }

  private static void resolve(Statement statement, Block enclosingBlock, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assign = (AssignStatement) statement;
      resolve(assign.getExpression(), enclosingBlock, compilationUnit);
      Variable variable = enclosingBlock.getVariable(assign.getVariableName().getName());
      if (variable == null)
      {
        throw new NameNotResolvedException("Unable to resolve: " + assign.getVariableName(), assign.getVariableName().getLexicalPhrase());
      }
      assign.setResolvedVariable(variable);
    }
    else if (statement instanceof Block)
    {
      Block subBlock = (Block) statement;
      for (Variable v : enclosingBlock.getVariables())
      {
        subBlock.addVariable(v);
      }
      for (Statement s : subBlock.getStatements())
      {
        resolve(s, subBlock, compilationUnit);
      }
    }
    else if (statement instanceof BreakStatement)
    {
      // do nothing
    }
    else if (statement instanceof ContinueStatement)
    {
      // do nothing
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      resolve(ifStatement.getExpression(), enclosingBlock, compilationUnit);
      resolve(ifStatement.getThenClause(), enclosingBlock, compilationUnit);
      if (ifStatement.getElseClause() != null)
      {
        resolve(ifStatement.getElseClause(), enclosingBlock, compilationUnit);
      }
    }
    else if (statement instanceof ReturnStatement)
    {
      resolve(((ReturnStatement) statement).getExpression(), enclosingBlock, compilationUnit);
    }
    else if (statement instanceof VariableDefinition)
    {
      VariableDefinition definition = (VariableDefinition) statement;
      Variable variable = enclosingBlock.getVariable(definition.getName().getName());
      if (variable != null)
      {
        throw new NameNotResolvedException("Variable already defined: " + definition.getName().getName(), definition.getName().getLexicalPhrase());
      }
      enclosingBlock.addVariable(definition.getVariable());
      if (definition.getExpression() != null)
      {
        resolve(definition.getExpression(), enclosingBlock, compilationUnit);
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      resolve(whileStatement.getExpression(), enclosingBlock, compilationUnit);
      resolve(whileStatement.getStatement(), enclosingBlock, compilationUnit);
    }
    else
    {
      throw new ConceptualException("Internal name resolution error: Unknown statement type", statement.getLexicalPhrase());
    }
  }

  private static void resolve(Expression expression, Block block, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    if (expression instanceof ArithmeticExpression)
    {
      resolve(((ArithmeticExpression) expression).getLeftSubExpression(), block, compilationUnit);
      resolve(((ArithmeticExpression) expression).getRightSubExpression(), block, compilationUnit);
    }
    else if (expression instanceof BitwiseNotExpression)
    {
      resolve(((BitwiseNotExpression) expression).getExpression(), block, compilationUnit);
    }
    else if (expression instanceof BooleanLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof BooleanNotExpression)
    {
      resolve(((BooleanNotExpression) expression).getExpression(), block, compilationUnit);
    }
    else if (expression instanceof BracketedExpression)
    {
      resolve(((BracketedExpression) expression).getExpression(), block, compilationUnit);
    }
    else if (expression instanceof CastExpression)
    {
      resolve(((CastExpression) expression).getExpression(), block, compilationUnit);
    }
    else if (expression instanceof ComparisonExpression)
    {
      resolve(((ComparisonExpression) expression).getLeftSubExpression(), block, compilationUnit);
      resolve(((ComparisonExpression) expression).getRightSubExpression(), block, compilationUnit);
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      // do nothing
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
        resolve(e, block, compilationUnit);
      }
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof LogicalExpression)
    {
      resolve(((LogicalExpression) expression).getLeftSubExpression(), block, compilationUnit);
      resolve(((LogicalExpression) expression).getRightSubExpression(), block, compilationUnit);
    }
    else if (expression instanceof MinusExpression)
    {
      resolve(((MinusExpression) expression).getExpression(), block, compilationUnit);
    }
    else if (expression instanceof VariableExpression)
    {
      VariableExpression expr = (VariableExpression) expression;
      Variable var = block.getVariable(expr.getName());
      if (var == null)
      {
        throw new NameNotResolvedException("Unable to resolve \"" + expr.getName() + "\"", expr.getLexicalPhrase());
      }
      expr.setResolvedVariable(var);
    }
    else
    {
      throw new ConceptualException("Internal name resolution error: Unknown expression type", expression.getLexicalPhrase());
    }
  }

}
