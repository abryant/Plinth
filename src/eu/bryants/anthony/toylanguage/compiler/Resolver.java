package eu.bryants.anthony.toylanguage.compiler;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

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
      Variable oldVar = mainBlock.addVariable(p);
      if (oldVar != null)
      {
        throw new ConceptualException("Duplicate parameter: " + p.getName(), p.getLexicalPhrase());
      }
    }
    resolve(function.getBlock(), compilationUnit);
  }

  private static void resolve(Block block, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    for (Statement s : block.getStatements())
    {
      if (s instanceof AssignStatement)
      {
        AssignStatement assign = (AssignStatement) s;
        resolve(assign.getExpression(), block, compilationUnit);
        Variable variable = block.getVariable(assign.getVariableName());
        if (variable == null)
        {
          variable = new Variable(assign.getVariableName());
          block.addVariable(variable);
        }
        assign.setResolvedVariable(variable);
      }
      else if (s instanceof ReturnStatement)
      {
        resolve(((ReturnStatement) s).getExpression(), block, compilationUnit);
      }
      else if (s instanceof Block)
      {
        Block subBlock = (Block) s;
        for (Variable v : block.getVariables())
        {
          subBlock.addVariable(v);
        }
        resolve(subBlock, compilationUnit);
      }
    }
  }

  private static void resolve(Expression expression, Block block, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    if (expression instanceof AdditiveExpression)
    {
      resolve(((AdditiveExpression) expression).getLeftSubExpression(), block, compilationUnit);
      resolve(((AdditiveExpression) expression).getRightSubExpression(), block, compilationUnit);
    }
    else if (expression instanceof BracketedExpression)
    {
      resolve(((BracketedExpression) expression).getExpression(), block, compilationUnit);
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
  }

}
