package eu.bryants.anthony.toylanguage.compiler;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.AdditiveExpression;
import eu.bryants.anthony.toylanguage.ast.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.Block;
import eu.bryants.anthony.toylanguage.ast.BracketedExpression;
import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.Expression;
import eu.bryants.anthony.toylanguage.ast.FloatingLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.Function;
import eu.bryants.anthony.toylanguage.ast.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.ast.IfStatement;
import eu.bryants.anthony.toylanguage.ast.IntegerLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.Parameter;
import eu.bryants.anthony.toylanguage.ast.ReturnStatement;
import eu.bryants.anthony.toylanguage.ast.Statement;
import eu.bryants.anthony.toylanguage.ast.VariableDefinition;
import eu.bryants.anthony.toylanguage.ast.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.WhileStatement;
import eu.bryants.anthony.toylanguage.ast.metadata.Variable;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ControlFlowChecker
{
  /**
   * Checks that the control flow of the specified compilation unit is well defined.
   * @param compilationUnit - the CompilationUnit to check
   * @throws ConceptualException - if any control flow related errors are detected
   */
  public static void checkControlFlow(CompilationUnit compilationUnit) throws ConceptualException
  {
    for (Function f : compilationUnit.getFunctions())
    {
      Set<Variable> initializedVariables = new HashSet<Variable>();
      for (Parameter p : f.getParameters())
      {
        initializedVariables.add(p.getVariable());
      }
      boolean returned = checkControlFlow(f.getBlock(), initializedVariables);
      if (!returned)
      {
        throw new ConceptualException("Function does not always return a value", f.getLexicalPhrase());
      }
    }
  }

  /**
   * Checks that the control flow of the specified statement is well defined.
   * @param statement - the statement to check
   * @return true if the statement returns from its enclosing function, false if control flow continues after it
   * @throws ConceptualException - if any unreachable code is detected
   */
  private static boolean checkControlFlow(Statement statement, Set<Variable> initializedVariables) throws ConceptualException
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assign = (AssignStatement) statement;
      checkUninitializedVariables(assign.getExpression(), initializedVariables);
      initializedVariables.add(assign.getResolvedVariable());
      return false;
    }
    else if (statement instanceof Block)
    {
      boolean returned = false;
      for (Statement s : ((Block) statement).getStatements())
      {
        if (returned)
        {
          throw new ConceptualException("Unreachable code", s.getLexicalPhrase());
        }
        returned = checkControlFlow(s, initializedVariables);
      }
      return returned;
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      checkUninitializedVariables(ifStatement.getExpression(), initializedVariables);
      Statement thenClause = ifStatement.getThenClause();
      Statement elseClause = ifStatement.getElseClause();
      if (elseClause == null)
      {
        Set<Variable> thenClauseVariables = new HashSet<Variable>(initializedVariables);
        checkControlFlow(thenClause, thenClauseVariables);
        return false;
      }
      Set<Variable> thenClauseVariables = new HashSet<Variable>(initializedVariables);
      Set<Variable> elseClauseVariables = new HashSet<Variable>(initializedVariables);
      // don't use a short circuit and ('&&') here, as it could cause elseClauseVariables not to be populated
      boolean returned = checkControlFlow(thenClause, thenClauseVariables) & checkControlFlow(elseClause, elseClauseVariables);
      for (Variable var : thenClauseVariables)
      {
        if (elseClauseVariables.contains(var))
        {
          initializedVariables.add(var);
        }
      }
      return returned;
    }
    else if (statement instanceof ReturnStatement)
    {
      checkUninitializedVariables(((ReturnStatement) statement).getExpression(), initializedVariables);
      return true;
    }
    else if (statement instanceof VariableDefinition)
    {
      VariableDefinition definition = (VariableDefinition) statement;
      if (definition.getExpression() != null)
      {
        checkUninitializedVariables(definition.getExpression(), initializedVariables);
        initializedVariables.add(definition.getVariable());
      }
      return false;
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      checkUninitializedVariables(whileStatement.getExpression(), initializedVariables);

      Set<Variable> loopVariables = new HashSet<Variable>(initializedVariables);
      return checkControlFlow(whileStatement.getStatement(), loopVariables);
    }
    throw new ConceptualException("Internal control flow checking error: Unknown statement type", statement.getLexicalPhrase());
  }

  private static void checkUninitializedVariables(Expression expression, Set<Variable> initializedVariables) throws ConceptualException
  {
    if (expression instanceof AdditiveExpression)
    {
      AdditiveExpression additiveExpression = (AdditiveExpression) expression;
      checkUninitializedVariables(additiveExpression.getLeftSubExpression(), initializedVariables);
      checkUninitializedVariables(additiveExpression.getRightSubExpression(), initializedVariables);
    }
    else if (expression instanceof BracketedExpression)
    {
      checkUninitializedVariables(((BracketedExpression) expression).getExpression(), initializedVariables);
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof FunctionCallExpression)
    {
      for (Expression e : ((FunctionCallExpression) expression).getArguments())
      {
        checkUninitializedVariables(e, initializedVariables);
      }
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof VariableExpression)
    {
      VariableExpression variableExpression = (VariableExpression) expression;
      if (!initializedVariables.contains(variableExpression.getResolvedVariable()))
      {
        throw new ConceptualException("Variable '" + variableExpression.getName() + "' may not be initialized", variableExpression.getLexicalPhrase());
      }
    }
    else
    {
      throw new ConceptualException("Internal control flow checking error: Unknown expression type", expression.getLexicalPhrase());
    }
  }
}
