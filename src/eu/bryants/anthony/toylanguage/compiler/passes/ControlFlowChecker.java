package eu.bryants.anthony.toylanguage.compiler.passes;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.Function;
import eu.bryants.anthony.toylanguage.ast.expression.ArithmeticExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ArrayAccessExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ArrayCreationExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BitwiseNotExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BooleanLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BooleanNotExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BracketedExpression;
import eu.bryants.anthony.toylanguage.ast.expression.CastExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ComparisonExpression;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.ast.expression.InlineIfExpression;
import eu.bryants.anthony.toylanguage.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.LogicalExpression;
import eu.bryants.anthony.toylanguage.ast.expression.MinusExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleIndexExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
import eu.bryants.anthony.toylanguage.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.misc.BlankAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.misc.VariableAssignee;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.statement.BreakStatement;
import eu.bryants.anthony.toylanguage.ast.statement.BreakableStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ContinueStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ExpressionStatement;
import eu.bryants.anthony.toylanguage.ast.statement.IfStatement;
import eu.bryants.anthony.toylanguage.ast.statement.PrefixIncDecStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ReturnStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Statement;
import eu.bryants.anthony.toylanguage.ast.statement.WhileStatement;
import eu.bryants.anthony.toylanguage.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.toylanguage.ast.type.VoidType;
import eu.bryants.anthony.toylanguage.compiler.ConceptualException;

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
    for (CompoundDefinition compoundDefinition : compilationUnit.getCompoundDefinitions())
    {
      for (Constructor constructor : compoundDefinition.getConstructors())
      {
        Set<Variable> initializedVariables = new HashSet<Variable>();
        for (Parameter p : constructor.getParameters())
        {
          initializedVariables.add(p.getVariable());
        }
        checkControlFlow(constructor.getBlock(), initializedVariables, new LinkedList<BreakableStatement>());
        for (Field field : compoundDefinition.getFields())
        {
          if (!initializedVariables.contains(field.getMemberVariable()))
          {
            throw new ConceptualException("Constructor does not always initialize the field: " + field.getName(), constructor.getLexicalPhrase());
          }
        }
      }
    }
    for (Function f : compilationUnit.getFunctions())
    {
      Set<Variable> initializedVariables = new HashSet<Variable>();
      for (Parameter p : f.getParameters())
      {
        initializedVariables.add(p.getVariable());
      }
      boolean returned = checkControlFlow(f.getBlock(), initializedVariables, new LinkedList<BreakableStatement>());
      if (!returned && !(f.getType() instanceof VoidType))
      {
        throw new ConceptualException("Function does not always return a value", f.getLexicalPhrase());
      }
    }
  }

  /**
   * Checks that the control flow of the specified statement is well defined.
   * @param statement - the statement to check
   * @param initializedVariables - the set of variables which have definitely been initialized before the specified statement is executed
   * @param enclosingBreakableStack - the stack of statements that can be broken out of that enclose this statement
   * @return true if the statement returns from its enclosing function, false if control flow continues after it
   * @throws ConceptualException - if any unreachable code is detected
   */
  private static boolean checkControlFlow(Statement statement, Set<Variable> initializedVariables, LinkedList<BreakableStatement> enclosingBreakableStack) throws ConceptualException
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assignStatement = (AssignStatement) statement;
      Assignee[] assignees = assignStatement.getAssignees();
      Set<Variable> nowInitializedVariables = new HashSet<Variable>();
      for (int i = 0; i < assignees.length; i++)
      {
        if (assignees[i] instanceof VariableAssignee)
        {
          // it hasn't been initialised unless there's an expression
          if (assignStatement.getExpression() != null)
          {
            nowInitializedVariables.add(((VariableAssignee) assignees[i]).getResolvedVariable());
          }
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          checkUninitializedVariables(arrayElementAssignee.getArrayExpression(), initializedVariables);
          checkUninitializedVariables(arrayElementAssignee.getDimensionExpression(), initializedVariables);
        }
        else if (assignees[i] instanceof BlankAssignee)
        {
          // do nothing, this assignee doesn't actually get assigned to
        }
        else
        {
          throw new IllegalStateException("Unknown Assignee type: " + assignees[i]);
        }
      }
      if (assignStatement.getExpression() != null)
      {
        checkUninitializedVariables(assignStatement.getExpression(), initializedVariables);
      }
      for (Variable var : nowInitializedVariables)
      {
        initializedVariables.add(var);
      }
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
        returned = checkControlFlow(s, initializedVariables, enclosingBreakableStack);
      }
      return returned;
    }
    else if (statement instanceof BreakStatement)
    {
      if (enclosingBreakableStack.isEmpty())
      {
        throw new ConceptualException("Nothing to break out of", statement.getLexicalPhrase());
      }
      BreakStatement breakStatement = (BreakStatement) statement;
      IntegerLiteral stepsLiteral = breakStatement.getBreakSteps();
      int breakCount = 1;
      if (stepsLiteral != null)
      {
        BigInteger value = stepsLiteral.getValue();
        if (value.signum() < 1)
        {
          throw new ConceptualException("Cannot break out of less than one statement", breakStatement.getLexicalPhrase());
        }
        if (value.bitLength() > Integer.SIZE || value.intValue() > enclosingBreakableStack.size())
        {
          throw new ConceptualException("Cannot break out of more than " + enclosingBreakableStack.size() + " statement" + (enclosingBreakableStack.size() == 1 ? "" : "s") + " at this point", breakStatement.getLexicalPhrase());
        }
        breakCount = value.intValue();
      }
      BreakableStatement breakable = enclosingBreakableStack.get(breakCount - 1);
      breakStatement.setResolvedBreakable(breakable);
      breakable.setBrokenOutOf(true);
      return true;
    }
    else if (statement instanceof ContinueStatement)
    {
      if (enclosingBreakableStack.isEmpty())
      {
        throw new ConceptualException("Nothing to continue through", statement.getLexicalPhrase());
      }
      ContinueStatement continueStatement = (ContinueStatement) statement;
      IntegerLiteral stepsLiteral = continueStatement.getContinueSteps();
      int continueCount = 1;
      if (stepsLiteral != null)
      {
        BigInteger value = stepsLiteral.getValue();
        if (value.signum() < 1)
        {
          throw new ConceptualException("Cannot continue through less than one statement", continueStatement.getLexicalPhrase());
        }
        if (value.bitLength() > Integer.SIZE || value.intValue() > enclosingBreakableStack.size())
        {
          throw new ConceptualException("Cannot continue through more than " + enclosingBreakableStack.size() + " statement" + (enclosingBreakableStack.size() == 1 ? "" : "s") + " at this point", continueStatement.getLexicalPhrase());
        }
        continueCount = value.intValue();
      }
      BreakableStatement breakable = enclosingBreakableStack.get(continueCount - 1);
      continueStatement.setResolvedBreakable(breakable);
      // TODO: when we get switch statements, make sure continue is forbidden for them
      return true;
    }
    else if (statement instanceof ExpressionStatement)
    {
      checkUninitializedVariables(((ExpressionStatement) statement).getExpression(), initializedVariables);
      return false;
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
        checkControlFlow(thenClause, thenClauseVariables, enclosingBreakableStack);
        return false;
      }
      Set<Variable> thenClauseVariables = new HashSet<Variable>(initializedVariables);
      Set<Variable> elseClauseVariables = new HashSet<Variable>(initializedVariables);
      boolean thenReturned = checkControlFlow(thenClause, thenClauseVariables, enclosingBreakableStack);
      boolean elseReturned = checkControlFlow(elseClause, elseClauseVariables, enclosingBreakableStack);
      if (!thenReturned && !elseReturned)
      {
        for (Variable var : thenClauseVariables)
        {
          if (elseClauseVariables.contains(var))
          {
            initializedVariables.add(var);
          }
        }
      }
      else if (!thenReturned && elseReturned)
      {
        initializedVariables.addAll(thenClauseVariables);
      }
      else if (thenReturned && !elseReturned)
      {
        initializedVariables.addAll(elseClauseVariables);
      }
      return thenReturned && elseReturned;
    }
    else if (statement instanceof PrefixIncDecStatement)
    {
      PrefixIncDecStatement prefixIncDecStatement = (PrefixIncDecStatement) statement;
      Assignee assignee = prefixIncDecStatement.getAssignee();
      if (assignee instanceof VariableAssignee)
      {
        Variable resolvedVariable = ((VariableAssignee) assignee).getResolvedVariable();
        if (!initializedVariables.contains(resolvedVariable))
        {
          throw new ConceptualException("Variable '" + ((VariableAssignee) assignee).getVariableName() + "' may not have been initialized", assignee.getLexicalPhrase());
        }
      }
      else if (assignee instanceof ArrayElementAssignee)
      {
        ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
        checkUninitializedVariables(arrayElementAssignee.getArrayExpression(), initializedVariables);
        checkUninitializedVariables(arrayElementAssignee.getDimensionExpression(), initializedVariables);
      }
      else
      {
        // ignore blank assignees, they shouldn't be able to get through variable resolution
        throw new IllegalStateException("Unknown Assignee type: " + assignee);
      }
      return false;
    }
    else if (statement instanceof ReturnStatement)
    {
      Expression returnedExpression = ((ReturnStatement) statement).getExpression();
      if (returnedExpression != null)
      {
        checkUninitializedVariables(returnedExpression, initializedVariables);
      }
      return true;
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      checkUninitializedVariables(whileStatement.getExpression(), initializedVariables);

      Set<Variable> loopVariables = new HashSet<Variable>(initializedVariables);
      // we don't care about the result of this, as the loop could execute zero times
      enclosingBreakableStack.push(whileStatement);
      checkControlFlow(whileStatement.getStatement(), loopVariables, enclosingBreakableStack);
      enclosingBreakableStack.pop();
      return whileStatement.stopsExecution();
    }
    throw new ConceptualException("Internal control flow checking error: Unknown statement type", statement.getLexicalPhrase());
  }

  private static void checkUninitializedVariables(Expression expression, Set<Variable> initializedVariables) throws ConceptualException
  {
    if (expression instanceof ArithmeticExpression)
    {
      ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;
      checkUninitializedVariables(arithmeticExpression.getLeftSubExpression(), initializedVariables);
      checkUninitializedVariables(arithmeticExpression.getRightSubExpression(), initializedVariables);
    }
    else if (expression instanceof ArrayAccessExpression)
    {
      ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
      checkUninitializedVariables(arrayAccessExpression.getArrayExpression(), initializedVariables);
      checkUninitializedVariables(arrayAccessExpression.getDimensionExpression(), initializedVariables);
    }
    else if (expression instanceof ArrayCreationExpression)
    {
      ArrayCreationExpression creationExpression = (ArrayCreationExpression) expression;
      if (creationExpression.getDimensionExpressions() != null)
      {
        for (Expression e : creationExpression.getDimensionExpressions())
        {
          checkUninitializedVariables(e, initializedVariables);
        }
      }
      if (creationExpression.getValueExpressions() != null)
      {
        for (Expression e : creationExpression.getValueExpressions())
        {
          checkUninitializedVariables(e, initializedVariables);
        }
      }
    }
    else if (expression instanceof BitwiseNotExpression)
    {
      checkUninitializedVariables(((BitwiseNotExpression) expression).getExpression(), initializedVariables);
    }
    else if (expression instanceof BooleanLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof BooleanNotExpression)
    {
      checkUninitializedVariables(((BooleanNotExpression) expression).getExpression(), initializedVariables);
    }
    else if (expression instanceof BracketedExpression)
    {
      checkUninitializedVariables(((BracketedExpression) expression).getExpression(), initializedVariables);
    }
    else if (expression instanceof CastExpression)
    {
      checkUninitializedVariables(((CastExpression) expression).getExpression(), initializedVariables);
    }
    else if (expression instanceof ComparisonExpression)
    {
      ComparisonExpression comparisonExpression = (ComparisonExpression) expression;
      checkUninitializedVariables(comparisonExpression.getLeftSubExpression(), initializedVariables);
      checkUninitializedVariables(comparisonExpression.getRightSubExpression(), initializedVariables);
    }
    else if (expression instanceof FieldAccessExpression)
    {
      checkUninitializedVariables(((FieldAccessExpression) expression).getExpression(), initializedVariables);
      // we don't care about the field of this expression, we assume that all fields are initialised when they are defined
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
    else if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIfExpression = (InlineIfExpression) expression;
      checkUninitializedVariables(inlineIfExpression.getCondition(), initializedVariables);
      checkUninitializedVariables(inlineIfExpression.getThenExpression(), initializedVariables);
      checkUninitializedVariables(inlineIfExpression.getElseExpression(), initializedVariables);
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof LogicalExpression)
    {
      LogicalExpression logicalExpression = (LogicalExpression) expression;
      checkUninitializedVariables(logicalExpression.getLeftSubExpression(), initializedVariables);
      checkUninitializedVariables(logicalExpression.getRightSubExpression(), initializedVariables);
    }
    else if (expression instanceof MinusExpression)
    {
      checkUninitializedVariables(((MinusExpression) expression).getExpression(), initializedVariables);
    }
    else if (expression instanceof TupleExpression)
    {
      TupleExpression tupleExpression = (TupleExpression) expression;
      Expression[] subExpressions = tupleExpression.getSubExpressions();
      for (int i = 0; i < subExpressions.length; i++)
      {
        checkUninitializedVariables(subExpressions[i], initializedVariables);
      }
    }
    else if (expression instanceof TupleIndexExpression)
    {
      TupleIndexExpression indexExpression = (TupleIndexExpression) expression;
      checkUninitializedVariables(indexExpression.getExpression(), initializedVariables);
    }
    else if (expression instanceof VariableExpression)
    {
      VariableExpression variableExpression = (VariableExpression) expression;
      if (!initializedVariables.contains(variableExpression.getResolvedVariable()))
      {
        throw new ConceptualException("Variable '" + variableExpression.getName() + "' may not have been initialized", variableExpression.getLexicalPhrase());
      }
    }
    else
    {
      throw new ConceptualException("Internal control flow checking error: Unknown expression type", expression.getLexicalPhrase());
    }
  }
}
