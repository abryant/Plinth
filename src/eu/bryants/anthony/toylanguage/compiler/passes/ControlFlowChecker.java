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
import eu.bryants.anthony.toylanguage.ast.expression.ShiftExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ThisExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleIndexExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.ast.member.Method;
import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
import eu.bryants.anthony.toylanguage.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.misc.BlankAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.FieldAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.misc.VariableAssignee;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.statement.BreakStatement;
import eu.bryants.anthony.toylanguage.ast.statement.BreakableStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ContinueStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ExpressionStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ForStatement;
import eu.bryants.anthony.toylanguage.ast.statement.IfStatement;
import eu.bryants.anthony.toylanguage.ast.statement.PrefixIncDecStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ReturnStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Statement;
import eu.bryants.anthony.toylanguage.ast.statement.WhileStatement;
import eu.bryants.anthony.toylanguage.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.toylanguage.ast.type.NamedType;
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
        checkControlFlow(constructor.getBlock(), initializedVariables, new LinkedList<BreakableStatement>(), true);
        for (Field field : compoundDefinition.getFields())
        {
          if (!initializedVariables.contains(field.getMemberVariable()))
          {
            throw new ConceptualException("Constructor does not always initialize the field: " + field.getName(), constructor.getLexicalPhrase());
          }
        }
      }

      for (Method method : compoundDefinition.getAllMethods())
      {
        Set<Variable> initializedVariables = new HashSet<Variable>();
        for (Parameter p : method.getParameters())
        {
          initializedVariables.add(p.getVariable());
        }
        checkControlFlow(method.getBlock(), initializedVariables, new LinkedList<BreakableStatement>(), false);
      }
    }
    for (Function f : compilationUnit.getFunctions())
    {
      Set<Variable> initializedVariables = new HashSet<Variable>();
      for (Parameter p : f.getParameters())
      {
        initializedVariables.add(p.getVariable());
      }
      boolean returned = checkControlFlow(f.getBlock(), initializedVariables, new LinkedList<BreakableStatement>(), false);
      if (!returned && !(f.getType() instanceof VoidType))
      {
        throw new ConceptualException("Function does not always return a value", f.getLexicalPhrase());
      }
    }
  }

  /**
   * Checks that the control flow of the specified statement is well defined.
   * @param statement - the statement to check
   * @param initializedVariables - the set of variables which have definitely been initialized before the specified statement is executed - this will be added to with variables that are initialized in this statement
   * @param enclosingBreakableStack - the stack of statements that can be broken out of that enclose this statement
   * @param inConstructor - true if the statement is part of a constructor call
   * @return true if the statement returns from its enclosing function or control cannot reach statements after it, false if control flow continues after it
   * @throws ConceptualException - if any unreachable code is detected
   */
  private static boolean checkControlFlow(Statement statement, Set<Variable> initializedVariables, LinkedList<BreakableStatement> enclosingBreakableStack, boolean inConstructor) throws ConceptualException
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
          checkUninitializedVariables(arrayElementAssignee.getArrayExpression(), initializedVariables, inConstructor);
          checkUninitializedVariables(arrayElementAssignee.getDimensionExpression(), initializedVariables, inConstructor);
        }
        else if (assignees[i] instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];

          // if the field is being accessed on 'this' or '(this)' (to any number of brackets), then accept it as initialising that member variable
          Expression expression = fieldAssignee.getExpression();
          while (expression instanceof BracketedExpression && inConstructor)
          {
            expression = ((BracketedExpression) expression).getExpression();
          }
          if (expression instanceof ThisExpression && inConstructor)
          {
            Member resolvedMember = fieldAssignee.getResolvedMember();
            if (resolvedMember instanceof Field)
            {
              nowInitializedVariables.add(((Field) resolvedMember).getMemberVariable());
            }
          }
          else
          {
            // if we're in a constructor, only check the sub-expression for uninitialized variables if it doesn't just access 'this'
            // this allows the programmer to access fields before 'this' is fully initialized
            checkUninitializedVariables(fieldAssignee.getExpression(), initializedVariables, inConstructor);
          }
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
        checkUninitializedVariables(assignStatement.getExpression(), initializedVariables, inConstructor);
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
        returned = checkControlFlow(s, initializedVariables, enclosingBreakableStack, inConstructor);
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
      breakable.setContinuedThrough(true);
      // TODO: when we get switch statements, make sure continue is forbidden for them
      return true;
    }
    else if (statement instanceof ExpressionStatement)
    {
      checkUninitializedVariables(((ExpressionStatement) statement).getExpression(), initializedVariables, inConstructor);
      return false;
    }
    else if (statement instanceof ForStatement)
    {
      ForStatement forStatement = (ForStatement) statement;
      Statement init = forStatement.getInitStatement();
      Expression condition = forStatement.getConditional();
      Statement update = forStatement.getUpdateStatement();
      Block block = forStatement.getBlock();

      enclosingBreakableStack.push(forStatement);

      if (init != null)
      {
        // check the loop initialisation variable in the block outside the loop, because it may add new variables which have now been initialised
        boolean returned = checkControlFlow(init, initializedVariables, enclosingBreakableStack, inConstructor);
        if (returned)
        {
          throw new IllegalStateException("Reached a state where a for loop initialisation statement returned");
        }
      }
      HashSet<Variable> loopVariables = new HashSet<Variable>(initializedVariables);
      if (condition != null)
      {
        checkUninitializedVariables(condition, loopVariables, inConstructor);
      }
      boolean returned = false;
      for (Statement s : block.getStatements())
      {
        if (returned)
        {
          throw new ConceptualException("Unreachable code", s.getLexicalPhrase());
        }
        returned = checkControlFlow(s, loopVariables, enclosingBreakableStack, inConstructor);
      }

      if (update != null)
      {
        if (returned && !forStatement.isContinuedThrough())
        {
          throw new ConceptualException("Unreachable code", update.getLexicalPhrase());
        }
        boolean updateReturned = checkControlFlow(update, loopVariables, enclosingBreakableStack, inConstructor);
        if (updateReturned)
        {
          throw new IllegalStateException("Reached a state where a for loop update statement returned");
        }
      }

      enclosingBreakableStack.pop();

      // if there is no conditional and the for statement is never broken out of, then control cannot continue after the end of the loop
      return condition == null && !forStatement.isBrokenOutOf();
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      checkUninitializedVariables(ifStatement.getExpression(), initializedVariables, inConstructor);
      Statement thenClause = ifStatement.getThenClause();
      Statement elseClause = ifStatement.getElseClause();
      if (elseClause == null)
      {
        Set<Variable> thenClauseVariables = new HashSet<Variable>(initializedVariables);
        checkControlFlow(thenClause, thenClauseVariables, enclosingBreakableStack, inConstructor);
        return false;
      }
      Set<Variable> thenClauseVariables = new HashSet<Variable>(initializedVariables);
      Set<Variable> elseClauseVariables = new HashSet<Variable>(initializedVariables);
      boolean thenReturned = checkControlFlow(thenClause, thenClauseVariables, enclosingBreakableStack, inConstructor);
      boolean elseReturned = checkControlFlow(elseClause, elseClauseVariables, enclosingBreakableStack, inConstructor);
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
        checkUninitializedVariables(arrayElementAssignee.getArrayExpression(), initializedVariables, inConstructor);
        checkUninitializedVariables(arrayElementAssignee.getDimensionExpression(), initializedVariables, inConstructor);
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
        checkUninitializedVariables(returnedExpression, initializedVariables, inConstructor);
      }
      return true;
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      checkUninitializedVariables(whileStatement.getExpression(), initializedVariables, inConstructor);

      Set<Variable> loopVariables = new HashSet<Variable>(initializedVariables);
      // we don't care about the result of this, as the loop could execute zero times
      enclosingBreakableStack.push(whileStatement);
      checkControlFlow(whileStatement.getStatement(), loopVariables, enclosingBreakableStack, inConstructor);
      enclosingBreakableStack.pop();
      return false;
    }
    throw new ConceptualException("Internal control flow checking error: Unknown statement type", statement.getLexicalPhrase());
  }

  private static void checkUninitializedVariables(Expression expression, Set<Variable> initializedVariables, boolean inConstructor) throws ConceptualException
  {
    if (expression instanceof ArithmeticExpression)
    {
      ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;
      checkUninitializedVariables(arithmeticExpression.getLeftSubExpression(), initializedVariables, inConstructor);
      checkUninitializedVariables(arithmeticExpression.getRightSubExpression(), initializedVariables, inConstructor);
    }
    else if (expression instanceof ArrayAccessExpression)
    {
      ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
      checkUninitializedVariables(arrayAccessExpression.getArrayExpression(), initializedVariables, inConstructor);
      checkUninitializedVariables(arrayAccessExpression.getDimensionExpression(), initializedVariables, inConstructor);
    }
    else if (expression instanceof ArrayCreationExpression)
    {
      ArrayCreationExpression creationExpression = (ArrayCreationExpression) expression;
      if (creationExpression.getDimensionExpressions() != null)
      {
        for (Expression e : creationExpression.getDimensionExpressions())
        {
          checkUninitializedVariables(e, initializedVariables, inConstructor);
        }
      }
      if (creationExpression.getValueExpressions() != null)
      {
        for (Expression e : creationExpression.getValueExpressions())
        {
          checkUninitializedVariables(e, initializedVariables, inConstructor);
        }
      }
    }
    else if (expression instanceof BitwiseNotExpression)
    {
      checkUninitializedVariables(((BitwiseNotExpression) expression).getExpression(), initializedVariables, inConstructor);
    }
    else if (expression instanceof BooleanLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof BooleanNotExpression)
    {
      checkUninitializedVariables(((BooleanNotExpression) expression).getExpression(), initializedVariables, inConstructor);
    }
    else if (expression instanceof BracketedExpression)
    {
      checkUninitializedVariables(((BracketedExpression) expression).getExpression(), initializedVariables, inConstructor);
    }
    else if (expression instanceof CastExpression)
    {
      checkUninitializedVariables(((CastExpression) expression).getExpression(), initializedVariables, inConstructor);
    }
    else if (expression instanceof ComparisonExpression)
    {
      ComparisonExpression comparisonExpression = (ComparisonExpression) expression;
      checkUninitializedVariables(comparisonExpression.getLeftSubExpression(), initializedVariables, inConstructor);
      checkUninitializedVariables(comparisonExpression.getRightSubExpression(), initializedVariables, inConstructor);
    }
    else if (expression instanceof FieldAccessExpression)
    {
      FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) expression;
      Expression subExpression = ((FieldAccessExpression) expression).getExpression();
      while (subExpression instanceof BracketedExpression && inConstructor)
      {
        subExpression = ((BracketedExpression) subExpression).getExpression();
      }
      if (subExpression instanceof ThisExpression && inConstructor)
      {
        Member resolvedMember = fieldAccessExpression.getResolvedMember();
        if (resolvedMember instanceof Field && !initializedVariables.contains(((Field) resolvedMember).getMemberVariable()))
        {
          throw new ConceptualException("Field '" + ((Field) resolvedMember).getName() + "' may not have been initialized", fieldAccessExpression.getLexicalPhrase());
        }
      }
      else
      {
        // if we're in a constructor, only check the sub-expression for uninitialized variables if it doesn't just access 'this'
        // this allows the programmer to access fields before 'this' is fully initialized
        checkUninitializedVariables(((FieldAccessExpression) expression).getExpression(), initializedVariables, inConstructor);
        // if it isn't accessed on 'this', we don't care about the field itself, we assume that all fields on other objects are initialised when they are defined
      }
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof FunctionCallExpression)
    {
      // TODO: when we add method calls, make sure they don't work in the constructor until 'this' is fully initialized (i.e. all fields initialized)
      for (Expression e : ((FunctionCallExpression) expression).getArguments())
      {
        checkUninitializedVariables(e, initializedVariables, inConstructor);
      }
    }
    else if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIfExpression = (InlineIfExpression) expression;
      checkUninitializedVariables(inlineIfExpression.getCondition(), initializedVariables, inConstructor);
      checkUninitializedVariables(inlineIfExpression.getThenExpression(), initializedVariables, inConstructor);
      checkUninitializedVariables(inlineIfExpression.getElseExpression(), initializedVariables, inConstructor);
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof LogicalExpression)
    {
      LogicalExpression logicalExpression = (LogicalExpression) expression;
      checkUninitializedVariables(logicalExpression.getLeftSubExpression(), initializedVariables, inConstructor);
      checkUninitializedVariables(logicalExpression.getRightSubExpression(), initializedVariables, inConstructor);
    }
    else if (expression instanceof MinusExpression)
    {
      checkUninitializedVariables(((MinusExpression) expression).getExpression(), initializedVariables, inConstructor);
    }
    else if (expression instanceof ShiftExpression)
    {
      ShiftExpression shiftExpression = (ShiftExpression) expression;
      checkUninitializedVariables(shiftExpression.getLeftExpression(), initializedVariables, inConstructor);
      checkUninitializedVariables(shiftExpression.getRightExpression(), initializedVariables, inConstructor);
    }
    else if (expression instanceof ThisExpression)
    {
      if (inConstructor)
      {
        // the type has already been resolved by the resolver, so we can access it here
        NamedType type = (NamedType) expression.getType();
        CompoundDefinition compoundDefinition = type.getResolvedDefinition();
        for (Field field : compoundDefinition.getFields())
        {
          if (!initializedVariables.contains(field.getMemberVariable()))
          {
            throw new ConceptualException("Cannot use 'this' here. Not all of the fields of this '" + type + "' have been initialized (specifically: '" + field.getName() + "'), and I can't work out whether or not you're going to assign to them before they're used", expression.getLexicalPhrase());
          }
        }
      }
    }
    else if (expression instanceof TupleExpression)
    {
      TupleExpression tupleExpression = (TupleExpression) expression;
      Expression[] subExpressions = tupleExpression.getSubExpressions();
      for (int i = 0; i < subExpressions.length; i++)
      {
        checkUninitializedVariables(subExpressions[i], initializedVariables, inConstructor);
      }
    }
    else if (expression instanceof TupleIndexExpression)
    {
      TupleIndexExpression indexExpression = (TupleIndexExpression) expression;
      checkUninitializedVariables(indexExpression.getExpression(), initializedVariables, inConstructor);
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
