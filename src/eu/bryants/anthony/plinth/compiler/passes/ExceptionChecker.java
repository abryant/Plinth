package eu.bryants.anthony.plinth.compiler.passes;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.expression.ArithmeticExpression;
import eu.bryants.anthony.plinth.ast.expression.ArrayAccessExpression;
import eu.bryants.anthony.plinth.ast.expression.ArrayCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.BitwiseNotExpression;
import eu.bryants.anthony.plinth.ast.expression.BooleanLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.BooleanNotExpression;
import eu.bryants.anthony.plinth.ast.expression.BracketedExpression;
import eu.bryants.anthony.plinth.ast.expression.CastExpression;
import eu.bryants.anthony.plinth.ast.expression.ClassCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.EqualityExpression;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.plinth.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.plinth.ast.expression.InlineIfExpression;
import eu.bryants.anthony.plinth.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.LogicalExpression;
import eu.bryants.anthony.plinth.ast.expression.MinusExpression;
import eu.bryants.anthony.plinth.ast.expression.NullCoalescingExpression;
import eu.bryants.anthony.plinth.ast.expression.NullLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.ObjectCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression;
import eu.bryants.anthony.plinth.ast.expression.ShiftExpression;
import eu.bryants.anthony.plinth.ast.expression.StringLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.ThisExpression;
import eu.bryants.anthony.plinth.ast.expression.TupleExpression;
import eu.bryants.anthony.plinth.ast.expression.TupleIndexExpression;
import eu.bryants.anthony.plinth.ast.expression.VariableExpression;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Initialiser;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.metadata.FieldInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.PropertyInitialiser;
import eu.bryants.anthony.plinth.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.plinth.ast.misc.Assignee;
import eu.bryants.anthony.plinth.ast.misc.BlankAssignee;
import eu.bryants.anthony.plinth.ast.misc.CatchClause;
import eu.bryants.anthony.plinth.ast.misc.FieldAssignee;
import eu.bryants.anthony.plinth.ast.misc.VariableAssignee;
import eu.bryants.anthony.plinth.ast.statement.AssignStatement;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.statement.BreakStatement;
import eu.bryants.anthony.plinth.ast.statement.ContinueStatement;
import eu.bryants.anthony.plinth.ast.statement.DelegateConstructorStatement;
import eu.bryants.anthony.plinth.ast.statement.ExpressionStatement;
import eu.bryants.anthony.plinth.ast.statement.ForStatement;
import eu.bryants.anthony.plinth.ast.statement.IfStatement;
import eu.bryants.anthony.plinth.ast.statement.PrefixIncDecStatement;
import eu.bryants.anthony.plinth.ast.statement.ReturnStatement;
import eu.bryants.anthony.plinth.ast.statement.ShorthandAssignStatement;
import eu.bryants.anthony.plinth.ast.statement.Statement;
import eu.bryants.anthony.plinth.ast.statement.ThrowStatement;
import eu.bryants.anthony.plinth.ast.statement.TryStatement;
import eu.bryants.anthony.plinth.ast.statement.WhileStatement;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.compiler.CoalescedConceptualException;
import eu.bryants.anthony.plinth.compiler.ConceptualException;

/*
 * Created on 21 Feb 2013
 */

/**
 * @author Anthony Bryant
 */
public class ExceptionChecker
{

  /**
   * Checks that any checked exceptions that are thrown in the specified TypeDefinition are either caught or declared as thrown.
   * @param typeDefinition - the TypeDefinition to check
   * @throws ConceptualException - if a conceptual problem is found with the checked exceptions
   */
  public static void checkExceptions(TypeDefinition typeDefinition) throws ConceptualException
  {
    CoalescedConceptualException coalescedException = null;

    List<ThrownTypeEntry> instanceInitialiserUncaught = new LinkedList<ThrownTypeEntry>();
    List<ThrownTypeEntry> staticInitialiserUncaught = new LinkedList<ThrownTypeEntry>();

    for (Initialiser initialiser : typeDefinition.getInitialisers())
    {
      List<ThrownTypeEntry> uncaught = initialiser.isStatic() ? staticInitialiserUncaught : instanceInitialiserUncaught;
      if (initialiser instanceof FieldInitialiser)
      {
        findUncaughtExceptions(((FieldInitialiser) initialiser).getField().getInitialiserExpression(), uncaught);
      }
      else if (initialiser instanceof PropertyInitialiser)
      {
        findUncaughtExceptions(((PropertyInitialiser) initialiser).getProperty().getInitialiserExpression(), uncaught);
      }
      else
      {
        findUncaughtExceptions(initialiser.getBlock(), uncaught);
      }
    }

    for (ThrownTypeEntry thrownTypeEntry : staticInitialiserUncaught)
    {
      ConceptualException noteException = new ConceptualException("Note: thrown from here", thrownTypeEntry.throwLocation);
      ConceptualException exception = new ConceptualException(thrownTypeEntry.thrownType + " must be caught by this type's static initialiser", typeDefinition.getLexicalPhrase(), noteException);
      coalescedException = CoalescedConceptualException.coalesce(coalescedException, exception);
    }

    for (Property property : typeDefinition.getProperties())
    {
      if (property.getGetterBlock() != null)
      {
        List<ThrownTypeEntry> getterUncaught = new LinkedList<ThrownTypeEntry>();
        findUncaughtExceptions(property.getGetterBlock(), getterUncaught);
        for (ThrownTypeEntry thrownTypeEntry : getterUncaught)
        {
          if (!matchesDeclaredType(thrownTypeEntry.thrownType, property.getGetterUncheckedThrownTypes()))
          {
            ConceptualException noteException = new ConceptualException("Note: thrown from here", thrownTypeEntry.throwLocation);
            ConceptualException exception = new ConceptualException(thrownTypeEntry.thrownType + " must either be caught or declared to be thrown by this property's getter", property.getLexicalPhrase(), noteException);
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, exception);
          }
        }
      }
      if (property.getSetterBlock() != null)
      {
        List<ThrownTypeEntry> setterUncaught = new LinkedList<ThrownTypeEntry>();
        findUncaughtExceptions(property.getSetterBlock(), setterUncaught);
        for (ThrownTypeEntry thrownTypeEntry : setterUncaught)
        {
          if (!matchesDeclaredType(thrownTypeEntry.thrownType, property.getSetterUncheckedThrownTypes()))
          {
            ConceptualException noteException = new ConceptualException("Note: thrown from here", thrownTypeEntry.throwLocation);
            ConceptualException exception = new ConceptualException(thrownTypeEntry.thrownType + " must either be caught or declared to be thrown by this property's setter", property.getLexicalPhrase(), noteException);
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, exception);
          }
        }
      }
      if (property.getConstructorBlock() != null)
      {
        List<ThrownTypeEntry> constructorUncaught = new LinkedList<ThrownTypeEntry>();
        findUncaughtExceptions(property.getConstructorBlock(), constructorUncaught);
        for (ThrownTypeEntry thrownTypeEntry : constructorUncaught)
        {
          if (!matchesDeclaredType(thrownTypeEntry.thrownType, property.getConstructorUncheckedThrownTypes()))
          {
            ConceptualException noteException = new ConceptualException("Note: thrown from here", thrownTypeEntry.throwLocation);
            ConceptualException exception = new ConceptualException(thrownTypeEntry.thrownType + " must either be caught or declared to be thrown by this property's constructor", property.getLexicalPhrase(), noteException);
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, exception);
          }
        }
      }
    }
    for (Constructor constructor : typeDefinition.getAllConstructors())
    {
      List<ThrownTypeEntry> uncaught = new LinkedList<ThrownTypeEntry>(instanceInitialiserUncaught);
      findUncaughtExceptions(constructor.getBlock(), uncaught);
      for (ThrownTypeEntry thrownTypeEntry : uncaught)
      {
        if (!matchesDeclaredType(thrownTypeEntry.thrownType, constructor.getCheckedThrownTypes()) &&
            !matchesDeclaredType(thrownTypeEntry.thrownType, constructor.getUncheckedThrownTypes()))
        {
          ConceptualException noteException = new ConceptualException("Note: thrown from here", thrownTypeEntry.throwLocation);
          ConceptualException exception = new ConceptualException(thrownTypeEntry.thrownType + " must either be caught or declared to be thrown by this constructor", constructor.getLexicalPhrase(), noteException);
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, exception);
        }
      }
    }
    for (Method method : typeDefinition.getAllMethods())
    {
      if (method.getBlock() == null)
      {
        continue;
      }
      List<ThrownTypeEntry> uncaught = new LinkedList<ThrownTypeEntry>();
      findUncaughtExceptions(method.getBlock(), uncaught);
      for (ThrownTypeEntry thrownTypeEntry : uncaught)
      {
        if (!matchesDeclaredType(thrownTypeEntry.thrownType, method.getCheckedThrownTypes()) &&
            !matchesDeclaredType(thrownTypeEntry.thrownType, method.getUncheckedThrownTypes()))
        {
          ConceptualException noteException = new ConceptualException("Note: thrown from here", thrownTypeEntry.throwLocation);
          ConceptualException exception = new ConceptualException(thrownTypeEntry.thrownType + " must either be caught or declared to be thrown by this method", method.getLexicalPhrase(), noteException);
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, exception);
        }
      }
    }
    if (coalescedException != null)
    {
      throw coalescedException;
    }
  }

  /**
   * Checks whether the specified thrown type matches one of the specified declared types.
   * @param thrownType - the type to search for a matching declaration for
   * @param declaredTypes - the array of declared thrown types
   * @return true if there is a declared type which matches the specified thrown type, false otherwise
   */
  private static boolean matchesDeclaredType(NamedType thrownType, NamedType[] declaredTypes)
  {
    if (declaredTypes == null)
    {
      return false;
    }
    for (NamedType declaredThrown : declaredTypes)
    {
      if (declaredThrown.canAssign(thrownType))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Finds all of the uncaught exceptions inside the specified Statement
   * @param statement - the Statement to search
   * @param uncaughtExceptions - the list of uncaught exceptions to add to
   */
  private static void findUncaughtExceptions(Statement statement, List<ThrownTypeEntry> uncaughtExceptions)
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assignStatement = (AssignStatement) statement;
      Assignee[] assignees = assignStatement.getAssignees();
      for (Assignee assignee : assignees)
      {
        findUncaughtExceptions(assignee, uncaughtExceptions);
      }
      if (assignStatement.getExpression() != null)
      {
        findUncaughtExceptions(assignStatement.getExpression(), uncaughtExceptions);
      }
    }
    else if (statement instanceof Block)
    {
      for (Statement subStatement : ((Block) statement).getStatements())
      {
        findUncaughtExceptions(subStatement, uncaughtExceptions);
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
    else if (statement instanceof DelegateConstructorStatement)
    {
      DelegateConstructorStatement delegateConstructorStatement = (DelegateConstructorStatement) statement;
      for (Expression argument : delegateConstructorStatement.getArguments())
      {
        findUncaughtExceptions(argument, uncaughtExceptions);
      }
      if (delegateConstructorStatement.getResolvedConstructor() != null)
      {
        for (NamedType thrownType : delegateConstructorStatement.getResolvedConstructor().getCheckedThrownTypes())
        {
          addUncaughtException(thrownType, delegateConstructorStatement.getLexicalPhrase(), uncaughtExceptions);
        }
      }
    }
    else if (statement instanceof ExpressionStatement)
    {
      findUncaughtExceptions(((ExpressionStatement) statement).getExpression(), uncaughtExceptions);
    }
    else if (statement instanceof ForStatement)
    {
      ForStatement forStatement = (ForStatement) statement;
      if (forStatement.getInitStatement() != null)
      {
        findUncaughtExceptions(forStatement.getInitStatement(), uncaughtExceptions);
      }
      if (forStatement.getConditional() != null)
      {
        findUncaughtExceptions(forStatement.getConditional(), uncaughtExceptions);
      }
      if (forStatement.getUpdateStatement() != null)
      {
        findUncaughtExceptions(forStatement.getUpdateStatement(), uncaughtExceptions);
      }
      findUncaughtExceptions(forStatement.getBlock(), uncaughtExceptions);
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      findUncaughtExceptions(ifStatement.getExpression(), uncaughtExceptions);
      findUncaughtExceptions(ifStatement.getThenClause(), uncaughtExceptions);
      if (ifStatement.getElseClause() != null)
      {
        findUncaughtExceptions(ifStatement.getElseClause(), uncaughtExceptions);
      }
    }
    else if (statement instanceof PrefixIncDecStatement)
    {
      findUncaughtExceptions(((PrefixIncDecStatement) statement).getAssignee(), uncaughtExceptions);
    }
    else if (statement instanceof ReturnStatement)
    {
      ReturnStatement returnStatement = (ReturnStatement) statement;
      if (returnStatement.getExpression() != null)
      {
        findUncaughtExceptions(returnStatement.getExpression(), uncaughtExceptions);
      }
    }
    else if (statement instanceof ShorthandAssignStatement)
    {
      ShorthandAssignStatement shorthandAssignStatement = (ShorthandAssignStatement) statement;
      for (Assignee assignee : shorthandAssignStatement.getAssignees())
      {
        findUncaughtExceptions(assignee, uncaughtExceptions);
      }
      findUncaughtExceptions(shorthandAssignStatement.getExpression(), uncaughtExceptions);
    }
    else if (statement instanceof ThrowStatement)
    {
      ThrowStatement throwStatement = (ThrowStatement) statement;
      findUncaughtExceptions(throwStatement.getThrownExpression(), uncaughtExceptions);
      addUncaughtException((NamedType) throwStatement.getThrownExpression().getType(), throwStatement.getLexicalPhrase(), uncaughtExceptions);
    }
    else if (statement instanceof TryStatement)
    {
      TryStatement tryStatement = (TryStatement) statement;
      List<ThrownTypeEntry> tryUncaught = new LinkedList<ThrownTypeEntry>();
      // add exceptions from the try block
      findUncaughtExceptions(tryStatement.getTryBlock(), tryUncaught);
      // filter out the caught exceptions
      Iterator<ThrownTypeEntry> it = tryUncaught.iterator();
      while (it.hasNext())
      {
        ThrownTypeEntry thrownTypeEntry = it.next();
        catchLoop:
        for (CatchClause catchClause : tryStatement.getCatchClauses())
        {
          for (Type caughtType : catchClause.getCaughtTypes())
          {
            if (caughtType.canAssign(thrownTypeEntry.thrownType))
            {
              it.remove();
              break catchLoop;
            }
          }
        }
      }
      // add exceptions from catch blocks
      for (CatchClause catchClause : tryStatement.getCatchClauses())
      {
        findUncaughtExceptions(catchClause.getBlock(), tryUncaught);
      }
      if (tryStatement.getFinallyBlock() != null && tryStatement.getFinallyBlock().stopsExecution())
      {
        // the finally block stops execution, so exceptions thrown from the try and catch blocks will never propagate out of it
        // so ignore them
        tryUncaught.clear();
      }
      // merge the try/catch exceptions list into the main exceptions list
      for (ThrownTypeEntry thrownTypeEntry : tryUncaught)
      {
        boolean found = false;
        for (ThrownTypeEntry existingEntry : uncaughtExceptions)
        {
          if (existingEntry.thrownType.canAssign(thrownTypeEntry.thrownType))
          {
            found = true;
            break;
          }
        }
        if (!found)
        {
          uncaughtExceptions.add(thrownTypeEntry);
        }
      }
      if (tryStatement.getFinallyBlock() != null)
      {
        // add exceptions from the finally block
        findUncaughtExceptions(tryStatement.getFinallyBlock(), uncaughtExceptions);
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      findUncaughtExceptions(whileStatement.getExpression(), uncaughtExceptions);
      findUncaughtExceptions(whileStatement.getStatement(), uncaughtExceptions);
    }
    else
    {
      throw new IllegalArgumentException("Unknown Statement during exception checking: " + statement);
    }
  }

  private static void findUncaughtExceptions(Assignee assignee, List<ThrownTypeEntry> uncaughtExceptions)
  {
    if (assignee instanceof VariableAssignee)
    {
      // do nothing
    }
    else if (assignee instanceof ArrayElementAssignee)
    {
      ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
      findUncaughtExceptions(arrayElementAssignee.getArrayExpression(), uncaughtExceptions);
      findUncaughtExceptions(arrayElementAssignee.getDimensionExpression(), uncaughtExceptions);
    }
    else if (assignee instanceof FieldAssignee)
    {
      findUncaughtExceptions(((FieldAssignee) assignee).getFieldAccessExpression(), uncaughtExceptions);
    }
    else if (assignee instanceof BlankAssignee)
    {
      // do nothing
    }
    else
    {
      throw new IllegalArgumentException("Unknown Assignee during exception checking: " + assignee);
    }
  }

  /**
   * Finds all of the uncaught exceptions inside the specified Expression
   * @param expression - the Expression to search
   * @param uncaughtExceptions - the list of uncaught exceptions to add to
   */
  private static void findUncaughtExceptions(Expression expression, List<ThrownTypeEntry> uncaughtExceptions)
  {
    if (expression instanceof ArithmeticExpression)
    {
      findUncaughtExceptions(((ArithmeticExpression) expression).getLeftSubExpression(), uncaughtExceptions);
      findUncaughtExceptions(((ArithmeticExpression) expression).getRightSubExpression(), uncaughtExceptions);
    }
    else if (expression instanceof ArrayAccessExpression)
    {
      findUncaughtExceptions(((ArrayAccessExpression) expression).getArrayExpression(), uncaughtExceptions);
      findUncaughtExceptions(((ArrayAccessExpression) expression).getDimensionExpression(), uncaughtExceptions);
    }
    else if (expression instanceof ArrayCreationExpression)
    {
      ArrayCreationExpression arrayCreationExpression = (ArrayCreationExpression) expression;
      if (arrayCreationExpression.getDimensionExpressions() != null)
      {
        for (Expression dimensionExpression : arrayCreationExpression.getDimensionExpressions())
        {
          findUncaughtExceptions(dimensionExpression, uncaughtExceptions);
        }
      }
      if (arrayCreationExpression.getValueExpressions() != null)
      {
        for (Expression valueExpression : arrayCreationExpression.getValueExpressions())
        {
          findUncaughtExceptions(valueExpression, uncaughtExceptions);
        }
      }
    }
    else if (expression instanceof BitwiseNotExpression)
    {
      findUncaughtExceptions(((BitwiseNotExpression) expression).getExpression(), uncaughtExceptions);
    }
    else if (expression instanceof BooleanLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof BooleanNotExpression)
    {
      findUncaughtExceptions(((BooleanNotExpression) expression).getExpression(), uncaughtExceptions);
    }
    else if (expression instanceof BracketedExpression)
    {
      findUncaughtExceptions(((BracketedExpression) expression).getExpression(), uncaughtExceptions);
    }
    else if (expression instanceof CastExpression)
    {
      findUncaughtExceptions(((CastExpression) expression).getExpression(), uncaughtExceptions);
    }
    else if (expression instanceof ClassCreationExpression)
    {
      ClassCreationExpression classCreationExpression = (ClassCreationExpression) expression;
      for (Expression argument : classCreationExpression.getArguments())
      {
        findUncaughtExceptions(argument, uncaughtExceptions);
      }
      for (NamedType thrownType : classCreationExpression.getResolvedConstructor().getCheckedThrownTypes())
      {
        addUncaughtException(thrownType, classCreationExpression.getLexicalPhrase(), uncaughtExceptions);
      }
    }
    else if (expression instanceof EqualityExpression)
    {
      findUncaughtExceptions(((EqualityExpression) expression).getLeftSubExpression(), uncaughtExceptions);
      findUncaughtExceptions(((EqualityExpression) expression).getRightSubExpression(), uncaughtExceptions);
    }
    else if (expression instanceof FieldAccessExpression)
    {
      FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) expression;
      if (fieldAccessExpression.getBaseExpression() != null)
      {
        findUncaughtExceptions(fieldAccessExpression.getBaseExpression(), uncaughtExceptions);
      }
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression functionCallExpression = (FunctionCallExpression) expression;
      for (Expression argument : functionCallExpression.getArguments())
      {
        findUncaughtExceptions(argument, uncaughtExceptions);
      }
      if (functionCallExpression.getResolvedConstructor() != null)
      {
        for (NamedType thrownType : functionCallExpression.getResolvedConstructor().getCheckedThrownTypes())
        {
          addUncaughtException(thrownType, functionCallExpression.getLexicalPhrase(), uncaughtExceptions);
        }
      }
      else if (functionCallExpression.getResolvedMethod() != null)
      {
        if (functionCallExpression.getResolvedBaseExpression() != null)
        {
          findUncaughtExceptions(functionCallExpression.getResolvedBaseExpression(), uncaughtExceptions);
        }
        for (NamedType thrownType : functionCallExpression.getResolvedMethod().getCheckedThrownTypes())
        {
          addUncaughtException(thrownType, functionCallExpression.getLexicalPhrase(), uncaughtExceptions);
        }
      }
      else if (functionCallExpression.getResolvedBaseExpression() != null)
      {
        findUncaughtExceptions(functionCallExpression.getResolvedBaseExpression(), uncaughtExceptions);
        FunctionType functionType = (FunctionType) functionCallExpression.getResolvedBaseExpression().getType();
        for (NamedType thrownType : functionType.getThrownTypes())
        {
          addUncaughtException(thrownType, functionCallExpression.getLexicalPhrase(), uncaughtExceptions);
        }
      }
    }
    else if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIfExpression = (InlineIfExpression) expression;
      findUncaughtExceptions(inlineIfExpression.getCondition(), uncaughtExceptions);
      findUncaughtExceptions(inlineIfExpression.getThenExpression(), uncaughtExceptions);
      findUncaughtExceptions(inlineIfExpression.getElseExpression(), uncaughtExceptions);
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof LogicalExpression)
    {
      findUncaughtExceptions(((LogicalExpression) expression).getLeftSubExpression(), uncaughtExceptions);
      findUncaughtExceptions(((LogicalExpression) expression).getRightSubExpression(), uncaughtExceptions);
    }
    else if (expression instanceof MinusExpression)
    {
      findUncaughtExceptions(((MinusExpression) expression).getExpression(), uncaughtExceptions);
    }
    else if (expression instanceof NullCoalescingExpression)
    {
      findUncaughtExceptions(((NullCoalescingExpression) expression).getNullableExpression(), uncaughtExceptions);
      findUncaughtExceptions(((NullCoalescingExpression) expression).getAlternativeExpression(), uncaughtExceptions);
    }
    else if (expression instanceof NullLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof ObjectCreationExpression)
    {
      // do nothing
    }
    else if (expression instanceof RelationalExpression)
    {
      findUncaughtExceptions(((RelationalExpression) expression).getLeftSubExpression(), uncaughtExceptions);
      findUncaughtExceptions(((RelationalExpression) expression).getRightSubExpression(), uncaughtExceptions);
    }
    else if (expression instanceof ShiftExpression)
    {
      findUncaughtExceptions(((ShiftExpression) expression).getLeftExpression(), uncaughtExceptions);
      findUncaughtExceptions(((ShiftExpression) expression).getRightExpression(), uncaughtExceptions);
    }
    else if (expression instanceof StringLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof ThisExpression)
    {
      // do nothing
    }
    else if (expression instanceof TupleExpression)
    {
      TupleExpression tupleExpression = (TupleExpression) expression;
      for (Expression subExpression : tupleExpression.getSubExpressions())
      {
        findUncaughtExceptions(subExpression, uncaughtExceptions);
      }
    }
    else if (expression instanceof TupleIndexExpression)
    {
      findUncaughtExceptions(((TupleIndexExpression) expression).getExpression(), uncaughtExceptions);
    }
    else if (expression instanceof VariableExpression)
    {
      // do nothing
    }
    else
    {
      throw new IllegalArgumentException("Unknown Expression during exception checking: " + expression);
    }
  }

  /**
   * Adds the specified uncaught exception to the specified list of uncaught exceptions, checking that it does not already exist.
   * @param thrownType - the thrown type to add
   * @param thrownLocation - the LexicalPhrase of the thing which throws the specified thrownType
   * @param uncaughtExceptions - the list of uncaught exceptions to add to
   */
  private static void addUncaughtException(NamedType thrownType, LexicalPhrase thrownLocation, List<ThrownTypeEntry> uncaughtExceptions)
  {
    for (ThrownTypeEntry thrownTypeEntry : uncaughtExceptions)
    {
      if (thrownTypeEntry.thrownType.isEquivalent(thrownType))
      {
        // it's already in the list, from somewhere else
        return;
      }
    }
    uncaughtExceptions.add(new ThrownTypeEntry(thrownType, thrownLocation));
  }

  /**
   * Represents a location which has thrown an exception.
   * @author Anthony Bryant
   */
  private static class ThrownTypeEntry
  {
    NamedType thrownType;
    LexicalPhrase throwLocation;
    public ThrownTypeEntry(NamedType thrownType, LexicalPhrase throwLocation)
    {
      this.thrownType = thrownType;
      this.throwLocation = throwLocation;
    }
  }
}
