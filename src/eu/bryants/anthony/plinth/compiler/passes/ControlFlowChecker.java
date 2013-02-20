package eu.bryants.anthony.plinth.compiler.passes;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
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
import eu.bryants.anthony.plinth.ast.expression.InstanceOfExpression;
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
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Initialiser;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.metadata.FieldInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.GlobalVariable;
import eu.bryants.anthony.plinth.ast.metadata.MemberVariable;
import eu.bryants.anthony.plinth.ast.metadata.Variable;
import eu.bryants.anthony.plinth.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.plinth.ast.misc.Assignee;
import eu.bryants.anthony.plinth.ast.misc.BlankAssignee;
import eu.bryants.anthony.plinth.ast.misc.CatchClause;
import eu.bryants.anthony.plinth.ast.misc.FieldAssignee;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.misc.VariableAssignee;
import eu.bryants.anthony.plinth.ast.statement.AssignStatement;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.statement.BreakStatement;
import eu.bryants.anthony.plinth.ast.statement.BreakableStatement;
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
import eu.bryants.anthony.plinth.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.compiler.CoalescedConceptualException;
import eu.bryants.anthony.plinth.compiler.ConceptualException;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ControlFlowChecker
{
  /**
   * Checks that the control flow of the specified TypeDefinition is well defined.
   * @param typeDefinition - the TypeDefinition to check
   * @throws ConceptualException - if any control flow related errors are detected
   */
  public static void checkControlFlow(TypeDefinition typeDefinition) throws ConceptualException
  {
    // build the set of variables from superclasses
    Set<Variable> superClassVariables = new HashSet<Variable>();
    if (typeDefinition instanceof ClassDefinition)
    {
      ClassDefinition currentClassDefinition = ((ClassDefinition) typeDefinition).getSuperClassDefinition();
      while (currentClassDefinition != null)
      {
        for (Field field : currentClassDefinition.getNonStaticFields())
        {
          superClassVariables.add(field.getMemberVariable());
        }
        currentClassDefinition = currentClassDefinition.getSuperClassDefinition();
      }
    }

    // check the initialisers

    // note: while we check the initialiser, we tell the checker that the initialiser has been run
    // this is because this assumption allows the user to use 'this' in the initialiser once all other variables are initialised,
    // which is perfectly legal, since the initialiser must be run after any super-constructors
    // also, since initialisers cannot call delegate constructors, this cannot do any harm
    ControlFlowState instanceState = new ControlFlowState(true);
    ControlFlowState staticState   = new ControlFlowState(true);

    // the super-class's constructor has always been run by the time we get to the initialiser, so add the superClassVariables to the initialiser's variable set now
    for (Variable var : superClassVariables)
    {
      instanceState.variables.initialised.add(var);
      instanceState.variables.possiblyInitialised.add(var);
    }

    for (Field field : typeDefinition.getNonStaticFields())
    {
      if (field.getType().hasDefaultValue() && !field.isFinal())
      {
        instanceState.variables.initialised.add(field.getMemberVariable());
        instanceState.variables.possiblyInitialised.add(field.getMemberVariable());
      }
    }

    // the non-static initialisers are immutable iff there is at least one immutable constructor, so find out whether one exists
    boolean hasImmutableConstructors = false;
    // the non-static initialisers are selfish iff all constructors are selfish
    boolean onlyHasSelfishConstructors = true;
    for (Constructor constructor : typeDefinition.getAllConstructors())
    {
      if (constructor.isImmutable())
      {
        hasImmutableConstructors = true;
      }
      if (!constructor.isSelfish())
      {
        onlyHasSelfishConstructors = false;
      }
    }

    CoalescedConceptualException coalescedException = null;
    for (Initialiser initialiser : typeDefinition.getInitialisers())
    {
      try
      {
        if (initialiser instanceof FieldInitialiser)
        {
          Field field = ((FieldInitialiser) initialiser).getField();
          if (field.isStatic())
          {
            checkControlFlow(field.getInitialiserExpression(), staticState.variables.initialised, staticState.variables.initialiserState, false, false, true, false);
            staticState.variables.initialised.add(field.getGlobalVariable());
            staticState.variables.possiblyInitialised.add(field.getGlobalVariable());
          }
          else
          {
            checkControlFlow(field.getInitialiserExpression(), instanceState.variables.initialised, instanceState.variables.initialiserState, true, onlyHasSelfishConstructors, false, hasImmutableConstructors);
            instanceState.variables.initialised.add(field.getMemberVariable());
            instanceState.variables.possiblyInitialised.add(field.getMemberVariable());
          }
        }
        else
        {
          if (initialiser.isStatic())
          {
            checkControlFlow(initialiser.getBlock(), typeDefinition, staticState, null, new LinkedList<Statement>(), false, false, true, false, true);
          }
          else
          {
            checkControlFlow(initialiser.getBlock(), typeDefinition, instanceState, null, new LinkedList<Statement>(), true, onlyHasSelfishConstructors, false, hasImmutableConstructors, true);
          }
        }
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
    }

    for (Field field : typeDefinition.getFields())
    {
      if (field.isStatic() && field.isFinal() && !staticState.variables.initialised.contains(field.getGlobalVariable()))
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + field.getName() + "' is not always initialised", field.getLexicalPhrase()));
      }
    }

    DelegateConstructorVariables delegateConstructorVariables = new DelegateConstructorVariables();
    delegateConstructorVariables.initialiserDefinitelyInitialised = instanceState.variables.initialised;
    delegateConstructorVariables.initialiserPossiblyInitialised = instanceState.variables.possiblyInitialised;
    delegateConstructorVariables.superClassVariables = superClassVariables;
    for (Constructor constructor : typeDefinition.getAllConstructors())
    {
      try
      {
        checkControlFlow(constructor, delegateConstructorVariables);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
    }
    for (Method method : typeDefinition.getAllMethods())
    {
      try
      {
        checkControlFlow(method);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
    }

    if (coalescedException != null)
    {
      throw coalescedException;
    }
  }

  /**
   * Checks that the control flow of the specified constructor is well defined.
   * @param constructor - the constructor to check
   * @param delegateConstructorVariables - the sets of variables which are needed to calculate which variables have been initialised after a delegate constructor call
   * @throws ConceptualException - if any control flow related errors are detected
   */
  private static void checkControlFlow(Constructor constructor, DelegateConstructorVariables delegateConstructorVariables) throws ConceptualException
  {
    boolean initialiserAlreadyRun = !constructor.getCallsDelegateConstructor();
    ControlFlowState state = new ControlFlowState(initialiserAlreadyRun);
    if (initialiserAlreadyRun)
    {
      // this should behave exactly as if we are running the no-args super() constructor
      // since the initialiser variable sets already contain all superclass member variables, just copy them to this Constructor's ControlFlowState
      state.variables.initialised = new HashSet<Variable>(delegateConstructorVariables.initialiserDefinitelyInitialised);
      state.variables.possiblyInitialised = new HashSet<Variable>(delegateConstructorVariables.initialiserPossiblyInitialised);
    }
    for (Parameter p : constructor.getParameters())
    {
      state.variables.initialised.add(p.getVariable());
      state.variables.possiblyInitialised.add(p.getVariable());
    }
    CoalescedConceptualException coalescedException = null;
    try
    {
      checkControlFlow(constructor.getBlock(), constructor.getContainingTypeDefinition(), state, delegateConstructorVariables, new LinkedList<Statement>(), true, constructor.isSelfish(), false, constructor.isImmutable(), false);
    }
    catch (ConceptualException e)
    {
      coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
    }
    if (state.variables.initialiserState != InitialiserState.DEFINITELY_RUN)
    {
      coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Constructor does not always call a delegate constructor, i.e. this(...) or super(...), or otherwise implicitly run the initialiser", constructor.getLexicalPhrase()));
    }
    for (Field field : constructor.getContainingTypeDefinition().getNonStaticFields())
    {
      if (!state.variables.initialised.contains(field.getMemberVariable()))
      {
        if (!field.getType().hasDefaultValue())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Constructor does not always initialise the non-static field '" + field.getName() + "', which does not have a default value", constructor.getLexicalPhrase()));
        }
        if (field.isFinal())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Constructor does not always initialise the non-static final field '" + field.getName() + "'", constructor.getLexicalPhrase()));
        }
      }
    }
    if (coalescedException != null)
    {
      throw coalescedException;
    }
  }

  /**
   * Checks that the control flow of the specified method is well defined.
   * @param method - the method to check
   * @throws ConceptualException - if any control flow related errors are detected
   */
  private static void checkControlFlow(Method method) throws ConceptualException
  {
    Block mainBlock = method.getBlock();
    if (mainBlock == null)
    {
      if (method.getNativeName() == null && !method.isAbstract() && !(method instanceof BuiltinMethod))
      {
        throw new ConceptualException("A method must have a body, unless it is abstract or native", method.getLexicalPhrase());
      }
      // this method has no body, so there is nothing to check
      return;
    }
    ControlFlowState state = new ControlFlowState(true);
    for (Parameter p : method.getParameters())
    {
      state.variables.initialised.add(p.getVariable());
      state.variables.possiblyInitialised.add(p.getVariable());
    }
    boolean returned = checkControlFlow(method.getBlock(), method.getContainingTypeDefinition(), state, null, new LinkedList<Statement>(), false, false, method.isStatic(), method.isImmutable(), false);
    if (!returned && !(method.getReturnType() instanceof VoidType))
    {
      throw new ConceptualException("Method does not always return a value", method.getLexicalPhrase());
    }
  }

  /**
   * Checks that the control flow of the specified statement is well defined.
   * @param statement - the statement to check
   * @param enclosingTypeDefinition - the TypeDefinition that the specified Statement is enclosed inside
   * @param state - the state of the variables before this statement, to be updated to the after-statement state
   * @param delegateConstructorVariables - the sets of variables which indicate which variables the initialiser initialises
   * @param enclosingBreakableStack - the stack of statements that can be broken out of that enclose this statement (includes the TryStatements with finally blocks along the way)
   * @param inConstructor - true if the statement is part of a constructor call
   * @param inSelfishContext - true if the statement is part of a selfish constructor
   * @param inStaticContext - true if the statement is in a static context
   * @param inImmutableContext - true if the statement is in an immutable context
   * @param inInitialiser - true if the statement is in an initialiser
   * @return true if the statement returns from its enclosing function or control cannot reach statements after it, false if control flow continues after it
   * @throws ConceptualException - if any unreachable code is detected
   */
  private static boolean checkControlFlow(Statement statement, TypeDefinition enclosingTypeDefinition, ControlFlowState state, DelegateConstructorVariables delegateConstructorVariables, LinkedList<Statement> enclosingBreakableStack,
                                          boolean inConstructor, boolean inSelfishContext, boolean inStaticContext, boolean inImmutableContext, boolean inInitialiser) throws ConceptualException
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assignStatement = (AssignStatement) statement;
      CoalescedConceptualException coalescedException = null;
      Assignee[] assignees = assignStatement.getAssignees();
      Set<Variable> nowInitialisedVariables = new HashSet<Variable>();
      for (int i = 0; i < assignees.length; i++)
      {
        if (assignees[i] instanceof VariableAssignee)
        {
          // it hasn't been initialised unless there's an expression
          if (assignStatement.getExpression() != null)
          {
            Variable var = ((VariableAssignee) assignees[i]).getResolvedVariable();
            if (var instanceof MemberVariable)
            {
              if (var.isFinal())
              {
                if (inConstructor)
                {
                  if (state.variables.possiblyInitialised.contains(var))
                  {
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final field '" + var.getName() + "' may already have been initialised", assignStatement.getLexicalPhrase()));
                  }
                }
                else
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final field '" + var.getName() + "' cannot be modified", assignStatement.getLexicalPhrase()));
                }
              }
              if (inImmutableContext && !inConstructor)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the member variable '" + var.getName() + "' in an immutable context", assignStatement.getLexicalPhrase()));
              }
              nowInitialisedVariables.add(var);
            }
            else if (var instanceof GlobalVariable)
            {
              if (var.isFinal())
              {
                if (inStaticContext && inInitialiser && ((GlobalVariable) var).getEnclosingTypeDefinition().equals(enclosingTypeDefinition))
                {
                  if (state.variables.possiblyInitialised.contains(var))
                  {
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' may already have been initialised", assignStatement.getLexicalPhrase()));
                  }
                }
                else // if not in a static initialiser
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' cannot be modified", assignStatement.getLexicalPhrase()));
                }
              }
              if (inImmutableContext)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the static variable '" + var.getName() + "' in an immutable context", assignStatement.getLexicalPhrase()));
              }
              nowInitialisedVariables.add(var);
            }
            else // parameters and local variables
            {
              if (var.isFinal() && state.variables.possiblyInitialised.contains(var))
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Variable '" + var.getName() + "' may already have been initialised.", assignStatement.getLexicalPhrase()));
              }
              nowInitialisedVariables.add(var);
            }
          }
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          try
          {
            checkControlFlow(arrayElementAssignee.getArrayExpression(), state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
          try
          {
            checkControlFlow(arrayElementAssignee.getDimensionExpression(), state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
          ArrayType baseType = (ArrayType) arrayElementAssignee.getArrayExpression().getType();
          if (baseType.isContextuallyImmutable())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to an element of an immutable array", arrayElementAssignee.getLexicalPhrase()));
          }
        }
        else if (assignees[i] instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];
          FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
          Member resolvedMember = fieldAccessExpression.getResolvedMember();

          // if the field is being accessed on 'this' or '(this)' (to any number of brackets), then accept it as initialising that member variable
          Expression expression = fieldAccessExpression.getBaseExpression();
          while (expression != null && expression instanceof BracketedExpression && inConstructor)
          {
            expression = ((BracketedExpression) expression).getExpression();
          }
          // if we're in a constructor, only check the sub-expression for uninitialised variables if it doesn't just access 'this'
          // this allows the programmer to access fields before 'this' is fully initialised
          if (expression != null && expression instanceof ThisExpression && inConstructor)
          {
            if (resolvedMember instanceof Field)
            {
              if (((Field) resolvedMember).isStatic())
              {
                throw new IllegalStateException("Field Assignee on 'this' resolves to a static member: " + fieldAssignee);
              }
              MemberVariable var = ((Field) resolvedMember).getMemberVariable();
              if (var.isFinal() && state.variables.possiblyInitialised.contains(var))
              {
                throw new ConceptualException("Final field '" + var.getName() + "' may already have been initialised.", assignStatement.getLexicalPhrase());
              }
              nowInitialisedVariables.add(var);
            }
          }
          else
          {
            boolean isMutableField = resolvedMember instanceof Field && ((Field) resolvedMember).isMutable();
            if (fieldAccessExpression.getBaseExpression() != null)
            {
              // if we aren't in a constructor, or the base expression isn't 'this', but we do have a base expression, then check the uninitialised variables for the base expression normally
              try
              {
                checkControlFlow(fieldAccessExpression.getBaseExpression(), state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
              }
              catch (ConceptualException e)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
              }

              Type baseType = fieldAccessExpression.getBaseExpression().getType();
              if (baseType instanceof NamedType && ((NamedType) baseType).isContextuallyImmutable() && !isMutableField)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to a non-mutable field of an immutable type", fieldAccessExpression.getLexicalPhrase()));
              }
            }
            else
            {
              // we do not have a base expression, so we must have a base type, so the field must be static
              // in this case, since static variables must always be initialised to a default value, the control flow checker does not need to check that it is initialised

              if (inImmutableContext & !isMutableField)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to a static non-mutable field in an immutable context", fieldAccessExpression.getLexicalPhrase()));
              }
            }
            if (resolvedMember instanceof Field)
            {
              Field field = (Field) resolvedMember;
              if (field.isStatic())
              {
                GlobalVariable globalVar = field.getGlobalVariable();
                if (globalVar != null && globalVar.isFinal())
                {
                  if (inStaticContext && inInitialiser && globalVar.getEnclosingTypeDefinition().equals(enclosingTypeDefinition))
                  {
                    if (state.variables.possiblyInitialised.contains(globalVar))
                    {
                      coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + globalVar.getName() + "' may already have been initialised", assignStatement.getLexicalPhrase()));
                    }
                    nowInitialisedVariables.add(globalVar);
                  }
                  else // if not in a static initialiser
                  {
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + globalVar.getName() + "' cannot be modified", assignStatement.getLexicalPhrase()));
                  }
                }
              }
              else
              {
                MemberVariable var = field.getMemberVariable();
                if (var != null && var.isFinal())
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final field '" + var.getName() + "' cannot be modified", assignStatement.getLexicalPhrase()));
                }
              }
            }
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
        try
        {
          checkControlFlow(assignStatement.getExpression(), state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      state.variables.initialised.addAll(nowInitialisedVariables);
      state.variables.possiblyInitialised.addAll(nowInitialisedVariables);
      if (coalescedException != null)
      {
        throw coalescedException;
      }
      return false;
    }
    else if (statement instanceof Block)
    {
      CoalescedConceptualException coalescedException = null;
      boolean returned = false;
      for (Statement s : ((Block) statement).getStatements())
      {
        if (returned)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Unreachable code", s.getLexicalPhrase()));
          throw coalescedException;
        }
        try
        {
          returned = checkControlFlow(s, enclosingTypeDefinition, state, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
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
      int breakIndex = 1;
      if (stepsLiteral != null)
      {
        BigInteger value = stepsLiteral.getValue();
        if (value.signum() < 1)
        {
          throw new ConceptualException("Cannot break out of less than one statement", breakStatement.getLexicalPhrase());
        }
        if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0)
        {
          throw new ConceptualException("Cannot break out of more than " + Integer.MAX_VALUE + " statements", breakStatement.getLexicalPhrase());
        }
        breakIndex = value.intValue();
      }
      int brokenCount = 0;
      List<TryStatement> finallyBlocks = new LinkedList<TryStatement>();
      BreakableStatement breakable = null;
      for (Statement brokenThrough : enclosingBreakableStack)
      {
        if (brokenThrough instanceof BreakableStatement)
        {
          ++brokenCount;
          if (brokenCount == breakIndex)
          {
            breakable = (BreakableStatement) brokenThrough;
            break;
          }
        }
        else if (brokenThrough instanceof TryStatement)
        {
          finallyBlocks.add((TryStatement) brokenThrough);
        }
        else
        {
          throw new IllegalStateException("Found an invalid statement type on the breakable stack");
        }
      }
      if (brokenCount != breakIndex)
      {
        throw new ConceptualException("Cannot break out of more than " + brokenCount + " statement" + (brokenCount == 1 ? "" : "s") + " at this point", breakStatement.getLexicalPhrase());
      }

      ControlFlowState finallyState = state.copy();

      boolean finallyReturned = false;
      Iterator<TryStatement> it = finallyBlocks.iterator();
      while (it.hasNext())
      {
        TryStatement tryStatement = it.next();
        if (finallyReturned)
        {
          // once a finally has returned, remove all of the other finally blocks from the list of broken-through finallyBlocks
          it.remove();
          continue;
        }
        LinkedList<Statement> finallyBreakableStack = new LinkedList<Statement>(enclosingBreakableStack);
        while (!finallyBreakableStack.isEmpty())
        {
          if (finallyBreakableStack.getFirst() != tryStatement)
          {
            finallyBreakableStack.removeFirst();
          }
          else
          {
            finallyBreakableStack.removeFirst();
            break;
          }
        }
        finallyReturned = checkControlFlow(tryStatement.getFinallyBlock(), enclosingTypeDefinition, finallyState, delegateConstructorVariables, finallyBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      }
      if (finallyReturned)
      {
        // it turns out we just break to a finally which returns somehow, we never actually reach the thing we're trying to break out of
        breakStatement.setResolvedFinallyBlocks(finallyBlocks);
        breakStatement.setResolvedBreakable(null);
      }
      else
      {
        breakStatement.setResolvedFinallyBlocks(finallyBlocks);
        breakStatement.setResolvedBreakable(breakable);
        breakable.setBrokenOutOf(true);
        finallyState.addToBreakVariables(breakable);
        // we must reset the state we have just generated processing the finally blocks, or it will be recombined into the start of the finally block's checking later on,
        // which would give the finally block an initial state which assumes it might already have been run
        finallyState.variables = state.variables.copy();
        finallyState.resetTerminatedState();
        state.combineReturned(finallyState);
      }
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
      int continueIndex = 1;
      if (stepsLiteral != null)
      {
        BigInteger value = stepsLiteral.getValue();
        if (value.signum() < 1)
        {
          throw new ConceptualException("Cannot continue through less than one statement", continueStatement.getLexicalPhrase());
        }
        if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0)
        {
          throw new ConceptualException("Cannot continue through more than " + Integer.MAX_VALUE + " statements", continueStatement.getLexicalPhrase());
        }
        continueIndex = value.intValue();
      }
      int continuedCount = 0;
      List<TryStatement> finallyBlocks = new LinkedList<TryStatement>();
      BreakableStatement breakable = null;
      for (Statement brokenThrough : enclosingBreakableStack)
      {
        // TODO: when we get switch statements, make sure continue is forbidden for them
        if (brokenThrough instanceof BreakableStatement)
        {
          ++continuedCount;
          if (continuedCount == continueIndex)
          {
            breakable = (BreakableStatement) brokenThrough;
            break;
          }
        }
        else if (brokenThrough instanceof TryStatement)
        {
          finallyBlocks.add((TryStatement) brokenThrough);
        }
        else
        {
          throw new IllegalStateException("Found an invalid statement type on the breakable stack");
        }
      }
      if (continuedCount != continueIndex)
      {
        throw new ConceptualException("Cannot continue through more than " + continuedCount + " statement" + (continuedCount == 1 ? "" : "s") + " at this point", continueStatement.getLexicalPhrase());
      }

      ControlFlowState finallyState = state.copy();

      boolean finallyReturned = false;
      Iterator<TryStatement> it = finallyBlocks.iterator();
      while (it.hasNext())
      {
        TryStatement tryStatement = it.next();
        if (finallyReturned)
        {
          // once a finally has returned, remove all of the other finally blocks from the list of continued-through finallyBlocks
          it.remove();
          continue;
        }
        LinkedList<Statement> finallyBreakableStack = new LinkedList<Statement>(enclosingBreakableStack);
        while (!finallyBreakableStack.isEmpty())
        {
          if (finallyBreakableStack.getFirst() != tryStatement)
          {
            finallyBreakableStack.removeFirst();
          }
          else
          {
            finallyBreakableStack.removeFirst();
            break;
          }
        }
        finallyReturned = checkControlFlow(tryStatement.getFinallyBlock(), enclosingTypeDefinition, finallyState, delegateConstructorVariables, finallyBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      }
      if (finallyReturned)
      {
        // it turns out we just break to a finally which returns somehow, we never actually reach the thing we're trying to break out of
        continueStatement.setResolvedFinallyBlocks(finallyBlocks);
        continueStatement.setResolvedBreakable(null);
      }
      else
      {
        continueStatement.setResolvedFinallyBlocks(finallyBlocks);
        continueStatement.setResolvedBreakable(breakable);
        breakable.setContinuedThrough(true);
        finallyState.addToContinueVariables(breakable);
        // we must reset the state we have just generated processing the finally blocks, or it will be recombined into the start of the finally block's checking later on,
        // which would give the finally block an initial state which assumes it might already have been run
        finallyState.variables = state.variables.copy();
        finallyState.resetTerminatedState();
        state.combineReturned(finallyState);
      }
      return true;
    }
    else if (statement instanceof DelegateConstructorStatement)
    {
      DelegateConstructorStatement delegateConstructorStatement = (DelegateConstructorStatement) statement;
      CoalescedConceptualException coalescedException = null;
      if (!inConstructor | inStaticContext | inInitialiser)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Delegate constructors may only be called from other constructors", delegateConstructorStatement.getLexicalPhrase()));
      }
      if (state.variables.initialiserState != InitialiserState.NOT_RUN)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("A delegate constructor may already have been run", delegateConstructorStatement.getLexicalPhrase()));
      }
      if (inImmutableContext && !delegateConstructorStatement.getResolvedConstructor().isImmutable())
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a non-immutable delegate constructor from an immutable constructor", delegateConstructorStatement.getLexicalPhrase()));
      }

      if (delegateConstructorStatement.isSuperConstructor() && delegateConstructorStatement.getResolvedConstructor().isSelfish())
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a selfish constructor as a super() constructor", delegateConstructorStatement.getLexicalPhrase()));
      }
      if (!inSelfishContext && delegateConstructorStatement.getResolvedConstructor().isSelfish())
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a selfish delegate constructor from a not-selfish constructor", delegateConstructorStatement.getLexicalPhrase()));
      }

      for (Expression argument : delegateConstructorStatement.getArguments())
      {
        try
        {
          checkControlFlow(argument, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }

      state.variables.initialiserState = InitialiserState.DEFINITELY_RUN;

      // after a delegate constructor call, all superclass member variables have now been initialised
      for (Variable var : delegateConstructorVariables.superClassVariables)
      {
        if (var instanceof MemberVariable && var.isFinal() && state.variables.possiblyInitialised.contains(var))
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a delegate constructor here, since it would overwrite the value of '" + var.getName() + "' (in: " + ((MemberVariable) var).getEnclosingTypeDefinition().getQualifiedName() + "), which may already have been initialised", delegateConstructorStatement.getLexicalPhrase()));
        }
        state.variables.initialised.add(var);
        state.variables.possiblyInitialised.add(var);
      }

      if (delegateConstructorStatement.isSuperConstructor())
      {
        // a super() constructor has been run, so all variables that are set by the initialiser have now been initialised
        for (Variable var : delegateConstructorVariables.initialiserDefinitelyInitialised)
        {
          state.variables.initialised.add(var);
        }
      }
      else
      {
        // a this() constructor has been run, so all of the member variables have now been initialised
        for (Field field : enclosingTypeDefinition.getNonStaticFields())
        {
          MemberVariable var = field.getMemberVariable();
          if (field.isFinal() && state.variables.possiblyInitialised.contains(var))
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a this() constructor here, since it would overwrite the value of '" + field.getName() + "', which may already have been initialised", delegateConstructorStatement.getLexicalPhrase()));
          }
          state.variables.initialised.add(var);
          state.variables.possiblyInitialised.add(var);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
      return false;
    }
    else if (statement instanceof ExpressionStatement)
    {
      checkControlFlow(((ExpressionStatement) statement).getExpression(), state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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

      CoalescedConceptualException coalescedException = null;
      if (init != null)
      {
        try
        {
          // check the loop initialisation variable in the block outside the loop, because it may add new variables which have now been initialised
          boolean returned = checkControlFlow(init, enclosingTypeDefinition, state, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
          if (returned)
          {
            throw new IllegalStateException("Reached a state where a for loop initialisation statement returned");
          }
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      ControlFlowState loopState = state.copy();
      if (condition != null)
      {
        try
        {
          checkControlFlow(condition, loopState.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      boolean returned = block.stopsExecution();
      try
      {
        returned = checkControlFlow(block, enclosingTypeDefinition, loopState, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (returned)
      {
        loopState.overwriteWithContinueVariables(forStatement);
      }
      else
      {
        loopState.reintegrateContinueVariables(forStatement);
      }

      if (update != null)
      {
        if (returned && !forStatement.isContinuedThrough())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Unreachable code", update.getLexicalPhrase()));
        }
        try
        {
          boolean updateReturned = checkControlFlow(update, enclosingTypeDefinition, loopState, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
          if (updateReturned)
          {
            throw new IllegalStateException("Reached a state where a for loop update statement returned");
          }
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }

      if (coalescedException != null)
      {
        // make sure we clean up the breakable stack if something fails
        enclosingBreakableStack.pop();
        throw coalescedException;
      }

      // run through the conditional, loop block, and update again, so that we catch any final variables that are initialised in the loop
      // (only if the loop can actually run more than once)
      if (!returned || forStatement.isContinuedThrough())
      {
        loopState.combine(state);
        if (condition != null)
        {
          try
          {
            checkControlFlow(condition, loopState.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        boolean secondReturned = returned;
        try
        {
          secondReturned = checkControlFlow(block, enclosingTypeDefinition, loopState, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
        if (secondReturned)
        {
          loopState.overwriteWithContinueVariables(forStatement);
        }
        else
        {
          loopState.reintegrateContinueVariables(forStatement);
        }
        if (update != null)
        {
          try
          {
            checkControlFlow(update, enclosingTypeDefinition, loopState, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
      }

      if (returned)
      {
        state.combineReturned(loopState);
      }
      else
      {
        state.combine(loopState);
      }

      // if there is no condition, then the only way to get to the code after the loop is to break out of it
      // so in that case, we overwrite the variables with the break variables
      if (condition == null)
      {
        state.overwriteWithBreakVariables(forStatement);
      }
      else
      {
        state.reintegrateBreakVariables(forStatement);
      }

      enclosingBreakableStack.pop();

      if (coalescedException != null)
      {
        throw coalescedException;
      }
      // if there is no conditional and the for statement is never broken out of, then control cannot continue after the end of the loop
      return condition == null && !forStatement.isBrokenOutOf();
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      CoalescedConceptualException coalescedException = null;
      try
      {
        checkControlFlow(ifStatement.getExpression(), state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      Statement thenClause = ifStatement.getThenClause();
      Statement elseClause = ifStatement.getElseClause();
      if (elseClause == null)
      {
        ControlFlowState thenClauseState = state.copy();
        boolean thenReturned = thenClause.stopsExecution();
        try
        {
          thenReturned = checkControlFlow(thenClause, enclosingTypeDefinition, thenClauseState, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
        if (thenReturned)
        {
          state.combineReturned(thenClauseState);
        }
        else
        {
          state.combine(thenClauseState);
        }
        if (coalescedException != null)
        {
          throw coalescedException;
        }
        return false;
      }
      ControlFlowState thenClauseState = state.copy();
      ControlFlowState elseClauseState = state.copy();
      boolean thenReturned = thenClause.stopsExecution();
      boolean elseReturned = elseClause.stopsExecution();
      try
      {
        thenReturned = checkControlFlow(thenClause, enclosingTypeDefinition, thenClauseState, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        elseReturned = checkControlFlow(elseClause, enclosingTypeDefinition, elseClauseState, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (!thenReturned & !elseReturned)
      {
        state.overwrite(thenClauseState);
        state.combine(elseClauseState);
      }
      else if (!thenReturned & elseReturned)
      {
        state.overwrite(thenClauseState);
        state.combineReturned(elseClauseState);
      }
      else if (thenReturned & !elseReturned)
      {
        state.overwrite(elseClauseState);
        state.combineReturned(thenClauseState);
      }
      else // thenReturned & elseReturned
      {
        state.combineReturned(thenClauseState);
        state.combineReturned(elseClauseState);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
      return thenReturned & elseReturned;
    }
    else if (statement instanceof PrefixIncDecStatement)
    {
      PrefixIncDecStatement prefixIncDecStatement = (PrefixIncDecStatement) statement;
      CoalescedConceptualException coalescedException = null;
      Assignee assignee = prefixIncDecStatement.getAssignee();
      if (assignee instanceof VariableAssignee)
      {
        Variable var = ((VariableAssignee) assignee).getResolvedVariable();
        if (!(var instanceof GlobalVariable) && !state.variables.initialised.contains(var) && (inConstructor || !(var instanceof MemberVariable)))
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Variable '" + ((VariableAssignee) assignee).getVariableName() + "' may not have been initialised", assignee.getLexicalPhrase()));
        }
        if (inStaticContext && var instanceof MemberVariable)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The non-static member variable '" + ((VariableAssignee) assignee).getVariableName() + "' does not exist in static methods", assignee.getLexicalPhrase()));
        }
        if (var.isFinal())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final variable '" + ((VariableAssignee) assignee).getVariableName() + "' cannot be modified", assignee.getLexicalPhrase()));
        }
        if (inImmutableContext)
        {
          if (!inConstructor && var instanceof MemberVariable)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the member variable '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase()));
          }
          else if (var instanceof GlobalVariable)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the static variable '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase()));
          }
        }
      }
      else if (assignee instanceof ArrayElementAssignee)
      {
        ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
        try
        {
          checkControlFlow(arrayElementAssignee.getArrayExpression(), state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
        try
        {
          checkControlFlow(arrayElementAssignee.getDimensionExpression(), state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
        if (((ArrayType) arrayElementAssignee.getArrayExpression().getType()).isContextuallyImmutable())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify an element of an immutable array", assignee.getLexicalPhrase()));
        }
      }
      else if (assignee instanceof FieldAssignee)
      {
        FieldAssignee fieldAssignee = (FieldAssignee) assignee;
        FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
        Member resolvedMember = fieldAccessExpression.getResolvedMember();
        // treat this as a field access, and check for uninitialised variables as normal
        try
        {
          checkControlFlow(fieldAccessExpression, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }

        boolean isMutableField = resolvedMember instanceof Field && ((Field) resolvedMember).isMutable();
        if (fieldAccessExpression.getBaseExpression() != null)
        {
          Type baseType = fieldAccessExpression.getBaseExpression().getType();
          if (baseType instanceof NamedType && ((NamedType) baseType).isContextuallyImmutable() && !isMutableField)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify a non-mutable field of an immutable type", assignee.getLexicalPhrase()));
          }
        }
        else
        {
          if (inImmutableContext & !isMutableField)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify a static non-mutable field in an immutable context", assignee.getLexicalPhrase()));
          }
        }
        // make sure we don't modify any final variables
        if (resolvedMember instanceof Field)
        {
          if (((Field) resolvedMember).isStatic())
          {
            GlobalVariable globalVar = ((Field) resolvedMember).getGlobalVariable();
            if (globalVar != null && globalVar.isFinal())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Static final field '" + globalVar.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
            }
          }
          else
          {
            MemberVariable var = ((Field) resolvedMember).getMemberVariable();
            if (var != null && var.isFinal())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final field '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
            }
          }
        }
      }
      else
      {
        // ignore blank assignees, they shouldn't be able to get through variable resolution
        throw new IllegalStateException("Unknown Assignee type: " + assignee);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
      return false;
    }
    else if (statement instanceof ReturnStatement)
    {
      ReturnStatement returnStatement = (ReturnStatement) statement;
      if (inInitialiser)
      {
        throw new ConceptualException("Cannot return from an initialiser", returnStatement.getLexicalPhrase());
      }
      Expression returnedExpression = returnStatement.getExpression();
      if (returnedExpression != null)
      {
        checkControlFlow(returnedExpression, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }

      List<TryStatement> finallyBlocks = new LinkedList<TryStatement>();
      for (Statement s : enclosingBreakableStack)
      {
        if (s instanceof TryStatement)
        {
          finallyBlocks.add((TryStatement) s);
          if (((TryStatement) s).getFinallyBlock().stopsExecution())
          {
            // this finally block never returns, so stop resolving them here
            returnStatement.setStoppedByFinally(true);
            break;
          }
        }
        else if (s instanceof BreakableStatement)
        {
          continue;
        }
        else
        {
          throw new IllegalStateException("Found an invalid statement type on the breakable stack");
        }
      }
      returnStatement.setResolvedFinallyBlocks(finallyBlocks);
      return true;
    }
    else if (statement instanceof ShorthandAssignStatement)
    {
      ShorthandAssignStatement shorthandAssignStatement = (ShorthandAssignStatement) statement;
      CoalescedConceptualException coalescedException = null;
      for (Assignee assignee : shorthandAssignStatement.getAssignees())
      {
        if (assignee instanceof VariableAssignee)
        {
          VariableAssignee variableAssignee = (VariableAssignee) assignee;
          Variable var = variableAssignee.getResolvedVariable();
          if (!(var instanceof GlobalVariable) && !state.variables.initialised.contains(var) && (inConstructor || !(var instanceof MemberVariable)))
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Variable '" + variableAssignee.getVariableName() + "' may not have been initialised", variableAssignee.getLexicalPhrase()));
          }
          if (inStaticContext && var instanceof MemberVariable)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The non-static member variable '" + variableAssignee.getVariableName() + "' does not exist in static methods", variableAssignee.getLexicalPhrase()));
          }
          if (var.isFinal())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final variable '" + variableAssignee.getVariableName() + "' cannot be modified", assignee.getLexicalPhrase()));
          }
          if (inImmutableContext)
          {
            if (!inConstructor && var instanceof MemberVariable)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the member variable '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase()));
            }
            else if (var instanceof GlobalVariable)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the static variable '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase()));
            }
          }
        }
        else if (assignee instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
          try
          {
            checkControlFlow(arrayElementAssignee.getArrayExpression(), state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
          try
          {
            checkControlFlow(arrayElementAssignee.getDimensionExpression(), state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
          if (((ArrayType) arrayElementAssignee.getArrayExpression().getType()).isContextuallyImmutable())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify an element of an immutable array", assignee.getLexicalPhrase()));
          }
        }
        else if (assignee instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignee;
          FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
          Member resolvedMember = fieldAccessExpression.getResolvedMember();
          // treat this as a field access, and check for uninitialised variables as normal
          try
          {
            checkControlFlow(fieldAccessExpression, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }

          boolean isMutableField = resolvedMember instanceof Field && ((Field) resolvedMember).isMutable();
          if (fieldAccessExpression.getBaseExpression() != null)
          {
            Type baseType = fieldAccessExpression.getBaseExpression().getType();
            if (baseType instanceof NamedType && ((NamedType) baseType).isContextuallyImmutable() && !isMutableField)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify a non-mutable field of an immutable type", assignee.getLexicalPhrase()));
            }
          }
          else
          {
            if (inImmutableContext & !isMutableField)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify a static non-mutable field in an immutable context", assignee.getLexicalPhrase()));
            }
          }
          // make sure we don't modify any final variables
          if (resolvedMember instanceof Field)
          {
            if (((Field) resolvedMember).isStatic())
            {
              GlobalVariable globalVar = ((Field) resolvedMember).getGlobalVariable();
              if (globalVar != null && globalVar.isFinal())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Static final field '" + globalVar.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
              }
            }
            else
            {
              MemberVariable var = ((Field) resolvedMember).getMemberVariable();
              if (var != null && var.isFinal())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final field '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
              }
            }
          }
        }
        else if (assignee instanceof BlankAssignee)
        {
          // do nothing, this assignee doesn't actually get assigned to
        }
        else
        {
          throw new IllegalStateException("Unknown Assignee type: " + assignee);
        }
      }
      try
      {
        checkControlFlow(shorthandAssignStatement.getExpression(), state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
      return false;
    }
    else if (statement instanceof ThrowStatement)
    {
      ThrowStatement throwStatement = (ThrowStatement) statement;
      checkControlFlow(throwStatement.getThrownExpression(), state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      return true;
    }
    else if (statement instanceof TryStatement)
    {
      TryStatement tryStatement = (TryStatement) statement;

      if (tryStatement.getFinallyBlock() != null)
      {
        // if we have a finally block, break and continue statements have to come through us
        enclosingBreakableStack.push(tryStatement);
      }

      ControlFlowState tryState = state.copy();
      // reset the try block's terminated state, so that we have a clean state for the catch/finally blocks
      tryState.resetTerminatedState();
      boolean tryReturned;
      try
      {
        tryReturned = checkControlFlow(tryStatement.getTryBlock(), enclosingTypeDefinition, tryState, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      }
      catch (ConceptualException e)
      {
        // make sure we clean up the breakable stack if something fails
        enclosingBreakableStack.pop();
        throw e;
      }

      ControlFlowState beforeCatchState = tryState.getTerminatedState();
      CatchClause[] catchClauses = tryStatement.getCatchClauses();
      ControlFlowState[] catchStates = new ControlFlowState[catchClauses.length];
      boolean[] catchReturned = new boolean[catchClauses.length];
      CoalescedConceptualException coalescedException = null;
      for (int i = 0; i < catchClauses.length; ++i)
      {
        catchStates[i] = beforeCatchState.copy();
        // reset the catch block's terminated state, so that we have a clean state for the finally block (if there is one)
        catchStates[i].resetTerminatedState();
        catchStates[i].variables.initialised.add(catchClauses[i].getResolvedExceptionVariable());
        catchStates[i].variables.possiblyInitialised.add(catchClauses[i].getResolvedExceptionVariable());
        try
        {
          catchReturned[i] = checkControlFlow(catchClauses[i].getBlock(), enclosingTypeDefinition, catchStates[i], delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (tryStatement.getFinallyBlock() != null)
      {
        enclosingBreakableStack.pop();
      }

      if (coalescedException != null)
      {
        throw coalescedException;
      }

      // check the case where a finally is run due to a propagating exception
      // in this case, it could be run with or without the try block executing, and with or without any of the catch blocks executing
      // (if a catch block throws an exception, the finally block gets executed next)
      if (tryStatement.getFinallyBlock() != null)
      {
        ControlFlowState finallyState = beforeCatchState; // no need to copy it, it won't be used again
        for (int i = 0; i < catchClauses.length; ++i)
        {
          finallyState.combine(catchStates[i].getTerminatedState());
        }
        checkControlFlow(tryStatement.getFinallyBlock(), enclosingTypeDefinition, finallyState, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);

        // we need to combine the result of the finally block into the main state, so that any surrounding statements know what
        // happens in general if something breaks out of this finally block, or throws an exception in the middle of it
        state.combineReturned(finallyState);
      }

      // handle the case where a try or catch block terminated successfully
      // to do this, we need to combine all of the finishing sets from each of the try and catch blocks which did not return (i.e. the ones that control reached the end of)
      boolean variablesOverwritten = false;
      if (tryReturned)
      {
        state.combineReturned(tryState);
      }
      else
      {
        if (!variablesOverwritten)
        {
          state.overwrite(tryState);
          variablesOverwritten = true;
        }
        else
        {
          state.combine(tryState);
        }
      }
      for (int i = 0; i < catchClauses.length; ++i)
      {
        if (catchReturned[i])
        {
          state.combineReturned(catchStates[i]);
        }
        else
        {
          if (!variablesOverwritten)
          {
            state.overwrite(catchStates[i]);
            variablesOverwritten = true;
          }
          else
          {
            state.combine(catchStates[i]);
          }
        }
      }

      boolean finallyReturned = false;
      if (variablesOverwritten && tryStatement.getFinallyBlock() != null)
      {
        finallyReturned = checkControlFlow(tryStatement.getFinallyBlock(), enclosingTypeDefinition, state, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      }
      // this statement has returned iff either all of the try and catch blocks returned, or the finally block returned
      return !variablesOverwritten | finallyReturned;
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      CoalescedConceptualException coalescedException = null;

      ControlFlowState loopState = state.copy();

      try
      {
        checkControlFlow(whileStatement.getExpression(), loopState.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }

      // we don't care about the result of this, as the loop could execute zero times
      enclosingBreakableStack.push(whileStatement);
      boolean whileReturned = whileStatement.getStatement().stopsExecution();
      try
      {
        whileReturned = checkControlFlow(whileStatement.getStatement(), enclosingTypeDefinition, loopState, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (whileReturned)
      {
        loopState.overwriteWithContinueVariables(whileStatement);
      }
      else
      {
        loopState.reintegrateContinueVariables(whileStatement);
      }

      if (coalescedException != null)
      {
        // make sure we clean up the breakable stack if something fails
        enclosingBreakableStack.pop();
        throw coalescedException;
      }

      // run through the conditional and loop block again, so that we catch any final variables that are initialised in the loop
      // (only if it is possible to run the loop more than once)
      if (!whileReturned || whileStatement.isContinuedThrough())
      {
        try
        {
          checkControlFlow(whileStatement.getExpression(), loopState.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
        boolean secondReturned = whileStatement.getStatement().stopsExecution();
        try
        {
          secondReturned = checkControlFlow(whileStatement.getStatement(), enclosingTypeDefinition, loopState, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
        if (secondReturned)
        {
          loopState.overwriteWithContinueVariables(whileStatement);
        }
        else
        {
          loopState.reintegrateContinueVariables(whileStatement);
        }
      }

      if (whileReturned)
      {
        state.combineReturned(loopState);
      }
      else
      {
        state.combine(loopState);
      }
      state.reintegrateBreakVariables(whileStatement);

      enclosingBreakableStack.pop();

      if (coalescedException != null)
      {
        throw coalescedException;
      }
      return false;
    }
    throw new ConceptualException("Internal control flow checking error: Unknown statement type", statement.getLexicalPhrase());
  }

  private static void checkControlFlow(Expression expression, Set<Variable> initialisedVariables, InitialiserState initialiserState, boolean inConstructor, boolean inSelfishContext, boolean inStaticContext, boolean inImmutableContext) throws ConceptualException
  {
    if (expression instanceof ArithmeticExpression)
    {
      ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;
      CoalescedConceptualException coalescedException = null;
      try
      {
        checkControlFlow(arithmeticExpression.getLeftSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(arithmeticExpression.getRightSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof ArrayAccessExpression)
    {
      ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
      CoalescedConceptualException coalescedException = null;
      try
      {
        checkControlFlow(arrayAccessExpression.getArrayExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(arrayAccessExpression.getDimensionExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof ArrayCreationExpression)
    {
      ArrayCreationExpression creationExpression = (ArrayCreationExpression) expression;
      CoalescedConceptualException coalescedException = null;
      // TODO: when we add the "new [7]Foo(creationFunction)" syntax, make sure creationFunction is immutable if we are in an immutable context
      if (creationExpression.getDimensionExpressions() != null)
      {
        for (Expression expr : creationExpression.getDimensionExpressions())
        {
          try
          {
            checkControlFlow(expr, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
      }
      if (creationExpression.getValueExpressions() != null)
      {
        for (Expression expr : creationExpression.getValueExpressions())
        {
          try
          {
            checkControlFlow(expr, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof BitwiseNotExpression)
    {
      checkControlFlow(((BitwiseNotExpression) expression).getExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof BooleanLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof BooleanNotExpression)
    {
      checkControlFlow(((BooleanNotExpression) expression).getExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof BracketedExpression)
    {
      checkControlFlow(((BracketedExpression) expression).getExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof CastExpression)
    {
      checkControlFlow(((CastExpression) expression).getExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof ClassCreationExpression)
    {
      ClassCreationExpression classCreationExpression = (ClassCreationExpression) expression;
      CoalescedConceptualException coalescedException = null;
      for (Expression argument : classCreationExpression.getArguments())
      {
        try
        {
          checkControlFlow(argument, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (inImmutableContext && !classCreationExpression.getResolvedConstructor().isImmutable())
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a non-immutable constructor from an immutable context (it may alter global variables)", classCreationExpression.getLexicalPhrase()));
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof EqualityExpression)
    {
      EqualityExpression equalityExpression = (EqualityExpression) expression;
      CoalescedConceptualException coalescedException = null;
      try
      {
        checkControlFlow(equalityExpression.getLeftSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(equalityExpression.getRightSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof FieldAccessExpression)
    {
      FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) expression;
      CoalescedConceptualException coalescedException = null;
      Expression subExpression = fieldAccessExpression.getBaseExpression();

      // if we're in a constructor, we only want to check the sub-expression for uninitialised variables if it doesn't just access 'this'
      // this allows the programmer to access certain fields before 'this' is fully initialised
      // if it isn't accessed on 'this', we don't care about the field access itself, as we assume that all fields on other objects are initialised when they are defined
      while (subExpression != null && subExpression instanceof BracketedExpression && inConstructor)
      {
        subExpression = ((BracketedExpression) subExpression).getExpression();
      }
      if (subExpression != null && subExpression instanceof ThisExpression && inConstructor)
      {
        Member resolvedMember = fieldAccessExpression.getResolvedMember();
        if (resolvedMember instanceof Field && !((Field) resolvedMember).isStatic() && !initialisedVariables.contains(((Field) resolvedMember).getMemberVariable()))
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Field '" + ((Field) resolvedMember).getName() + "' may not have been initialised", fieldAccessExpression.getLexicalPhrase()));
        }
        if (resolvedMember instanceof Method && !((Method) resolvedMember).isStatic())
        {
          // non-static methods cannot be accessed as fields before 'this' has been initialised
          Method resolvedMethod = (Method) resolvedMember;
          if (!inSelfishContext)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot access methods on 'this' unless we are within a selfish constructor", expression.getLexicalPhrase()));
          }
          else if (initialiserState != InitialiserState.DEFINITELY_RUN)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot access methods on 'this' here. The initialiser of this '" + new NamedType(false, false, resolvedMethod.getContainingTypeDefinition()) + "' may not have been run yet", expression.getLexicalPhrase()));
          }
          else
          {
            for (Field field : resolvedMethod.getContainingTypeDefinition().getNonStaticFields())
            {
              if (!initialisedVariables.contains(field.getMemberVariable()))
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot access methods on 'this' here. Not all of the non-static fields of this '" + new NamedType(false, false, resolvedMethod.getContainingTypeDefinition()) + "' have been initialised (specifically: '" + field.getName() + "'), and I can't work out whether or not you're going to initialise them before they're used", expression.getLexicalPhrase()));
                break;
              }
            }
          }
        }
      }
      else if (subExpression != null)
      {
        // otherwise (if we aren't in a constructor, or the base expression isn't 'this', but we do have a base expression) check the uninitialised variables normally
        try
        {
          checkControlFlow(fieldAccessExpression.getBaseExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      else
      {
        // otherwise, we do not have a base expression, so we must have a base type, so the field must be static
        // in this case, since static variables must always be initialised to a default value, the control flow checker does not need to check anything
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression functionCallExpression = (FunctionCallExpression) expression;
      CoalescedConceptualException coalescedException = null;
      if (functionCallExpression.getResolvedMethod() != null)
      {
        if (inImmutableContext && !functionCallExpression.getResolvedMethod().isImmutable())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a non-immutable method from an immutable context", functionCallExpression.getLexicalPhrase()));
        }
        Expression resolvedBaseExpression = functionCallExpression.getResolvedBaseExpression();
        if (resolvedBaseExpression == null)
        {
          Method resolvedMethod = functionCallExpression.getResolvedMethod();
          if (inConstructor && !resolvedMethod.isStatic())
          {
            // we are in a constructor, and we are calling a non-static method without a base expression (i.e. on 'this')
            // this should only be allowed if 'this' is fully initialised
            if (!inSelfishContext)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call methods on 'this' unless we are within a selfish constructor", expression.getLexicalPhrase()));
            }
            else if (initialiserState != InitialiserState.DEFINITELY_RUN)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call methods on 'this' here. The initialiser of this '" + new NamedType(false, false, resolvedMethod.getContainingTypeDefinition()) + "' may not have been run yet", expression.getLexicalPhrase()));
            }
            else
            {
              for (Field field : resolvedMethod.getContainingTypeDefinition().getNonStaticFields())
              {
                if (!initialisedVariables.contains(field.getMemberVariable()))
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call methods on 'this' here. Not all of the non-static fields of this '" + new NamedType(false, false, resolvedMethod.getContainingTypeDefinition()) + "' have been initialised (specifically: '" + field.getName() + "'), and I can't work out whether or not you're going to initialise them before they're used", expression.getLexicalPhrase()));
                  break;
                }
              }
            }
          }
          if (inStaticContext && !resolvedMethod.isStatic())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call the non-static method '" + resolvedMethod.getName() + "' from a static context", functionCallExpression.getLexicalPhrase()));
          }
        }
        else // resolvedBaseExpression != null
        {
          try
          {
            checkControlFlow(resolvedBaseExpression, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
          Type baseType = resolvedBaseExpression.getType();
          if ((baseType instanceof ArrayType && ((ArrayType) baseType).isContextuallyImmutable()) ||
              (baseType instanceof NamedType && ((NamedType) baseType).isContextuallyImmutable()))
          {
            if (!functionCallExpression.getResolvedMethod().isImmutable())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a non-immutable method on an immutable object", functionCallExpression.getLexicalPhrase()));
            }
          }
        }
      }
      else if (functionCallExpression.getResolvedConstructor() != null)
      {
        if (inImmutableContext && !functionCallExpression.getResolvedConstructor().isImmutable())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a non-immutable constructor from an immutable context (it may alter global variables)", functionCallExpression.getLexicalPhrase()));
        }
      }
      else if (functionCallExpression.getResolvedBaseExpression() != null)
      {
        checkControlFlow(functionCallExpression.getResolvedBaseExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        FunctionType type = (FunctionType) functionCallExpression.getResolvedBaseExpression().getType();
        if (inImmutableContext && !type.isImmutable())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a non-immutable function from an immutable context", functionCallExpression.getLexicalPhrase()));
        }
      }
      else
      {
        throw new IllegalStateException("Unresolved function call: " + functionCallExpression);
      }
      // check that the arguments are all initialised
      for (Expression expr : functionCallExpression.getArguments())
      {
        try
        {
          checkControlFlow(expr, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIfExpression = (InlineIfExpression) expression;
      CoalescedConceptualException coalescedException = null;
      try
      {
        checkControlFlow(inlineIfExpression.getCondition(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(inlineIfExpression.getThenExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(inlineIfExpression.getElseExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof InstanceOfExpression)
    {
      checkControlFlow(((InstanceOfExpression) expression).getExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof LogicalExpression)
    {
      LogicalExpression logicalExpression = (LogicalExpression) expression;
      CoalescedConceptualException coalescedException = null;
      try
      {
        checkControlFlow(logicalExpression.getLeftSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(logicalExpression.getRightSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof MinusExpression)
    {
      checkControlFlow(((MinusExpression) expression).getExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof NullCoalescingExpression)
    {
      CoalescedConceptualException coalescedException = null;
      try
      {
        checkControlFlow(((NullCoalescingExpression) expression).getNullableExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(((NullCoalescingExpression) expression).getAlternativeExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
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
      RelationalExpression relationalExpression = (RelationalExpression) expression;
      CoalescedConceptualException coalescedException = null;
      try
      {
        checkControlFlow(relationalExpression.getLeftSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(relationalExpression.getRightSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof ShiftExpression)
    {
      ShiftExpression shiftExpression = (ShiftExpression) expression;
      CoalescedConceptualException coalescedException = null;
      try
      {
        checkControlFlow(shiftExpression.getLeftExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(shiftExpression.getRightExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof StringLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof ThisExpression)
    {
      if (inStaticContext)
      {
        throw new ConceptualException("'this' does not refer to anything in this static context", expression.getLexicalPhrase());
      }
      if (inConstructor)
      {
        // the type has already been resolved by the resolver, so we can access it here
        NamedType type = (NamedType) expression.getType();
        if (!inSelfishContext)
        {
          throw new ConceptualException("Cannot use 'this' unless we are within a selfish constructor", expression.getLexicalPhrase());
        }
        else if (initialiserState != InitialiserState.DEFINITELY_RUN)
        {
          throw new ConceptualException("Cannot use 'this' here. The initialiser of this '" + type + "' may not have been run yet.", expression.getLexicalPhrase());
        }
        else
        {
          for (Field field : type.getResolvedTypeDefinition().getNonStaticFields())
          {
            if (!initialisedVariables.contains(field.getMemberVariable()))
            {
              throw new ConceptualException("Cannot use 'this' here. Not all of the non-static fields of this '" + type + "' have been initialised (specifically: '" + field.getName() + "'), and I can't work out whether or not you're going to initialise them before they're used", expression.getLexicalPhrase());
            }
          }
        }
      }
    }
    else if (expression instanceof TupleExpression)
    {
      TupleExpression tupleExpression = (TupleExpression) expression;
      CoalescedConceptualException coalescedException = null;
      Expression[] subExpressions = tupleExpression.getSubExpressions();
      for (int i = 0; i < subExpressions.length; i++)
      {
        try
        {
          checkControlFlow(subExpressions[i], initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof TupleIndexExpression)
    {
      TupleIndexExpression indexExpression = (TupleIndexExpression) expression;
      checkControlFlow(indexExpression.getExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof VariableExpression)
    {
      VariableExpression variableExpression = (VariableExpression) expression;
      Variable var = variableExpression.getResolvedVariable();
      Method method = variableExpression.getResolvedMethod();
      if (var != null)
      {
        if (!(var instanceof GlobalVariable) && !initialisedVariables.contains(var) && (inConstructor || !(var instanceof MemberVariable)))
        {
          throw new ConceptualException("Variable '" + variableExpression.getName() + "' may not have been initialised", variableExpression.getLexicalPhrase());
        }
        if (inStaticContext && var instanceof MemberVariable)
        {
          throw new ConceptualException("The non-static member variable '" + variableExpression.getName() + "' does not exist in static methods", expression.getLexicalPhrase());
        }
      }
      else if (method != null)
      {
        if (inStaticContext && !method.isStatic())
        {
          throw new ConceptualException("Cannot access the non-static method '" + method.getName() + "' from a static context", expression.getLexicalPhrase());
        }
        if (inConstructor && !method.isStatic())
        {
          // non-static methods cannot be accessed as fields before 'this' has been initialised
          if (!inSelfishContext)
          {
            throw new ConceptualException("Cannot access methods on 'this' unless we are within a selfish constructor", expression.getLexicalPhrase());
          }
          else if (initialiserState != InitialiserState.DEFINITELY_RUN)
          {
            throw new ConceptualException("Cannot access methods on 'this' here. The initialiser of this '" + new NamedType(false, false, method.getContainingTypeDefinition()) + "' may not have been run yet", expression.getLexicalPhrase());
          }
          else
          {
            for (Field field : method.getContainingTypeDefinition().getNonStaticFields())
            {
              if (!initialisedVariables.contains(field.getMemberVariable()))
              {
                throw new ConceptualException("Cannot access methods on 'this' here. Not all of the non-static fields of this '" + new NamedType(false, false, method.getContainingTypeDefinition()) + "' have been initialised (specifically: '" + field.getName() + "'), and I can't work out whether or not you're going to initialise them before they're used", expression.getLexicalPhrase());
              }
            }
          }
        }
      }
      else
      {
        throw new IllegalArgumentException("A VariableExpression must have been resolved to either a variable or a method");
      }
    }
    else
    {
      throw new IllegalArgumentException("Internal control flow checking error: Unknown expression type: " + expression);
    }
  }

  /**
   * Describes whether or not the initialiser has been run at a given point in the execution of a constructor.
   * @author Anthony Bryant
   */
  private static enum InitialiserState
  {
    DEFINITELY_RUN,
    POSSIBLY_RUN,
    NOT_RUN;

    /**
     * Combines two InitialiserStates into a single InitialiserState
     * @param firstState - the first state
     * @param secondState - the second state
     * @return the combined InitialiserState
     */
    private static InitialiserState combine(InitialiserState firstState, InitialiserState secondState)
    {
      if (firstState == NOT_RUN & secondState == NOT_RUN)
      {
        return NOT_RUN;
      }
      if (firstState == DEFINITELY_RUN & secondState == DEFINITELY_RUN)
      {
        return DEFINITELY_RUN;
      }
      return POSSIBLY_RUN;
    }
  }

  /**
   * Keeps track of sets of variables which will be initialised when a delegate constructor is run.
   * The initialiserDefinitelyInitialised and initialiserPossiblyInitialised sets contain variables which will/may be initialised by the non-static initialiser.
   * The superClassVariables set keeps track of variables which will definitely be initialised by a super(...) constructor.
   * @author Anthony Bryant
   */
  private static final class DelegateConstructorVariables
  {
    private Set<Variable> initialiserDefinitelyInitialised;
    private Set<Variable> initialiserPossiblyInitialised;
    private Set<Variable> superClassVariables;
  }

  /**
   * Represents the state of a function's variables at a given point in the control flow graph.
   * @author Anthony Bryant
   */
  private static final class ControlFlowVariables
  {
    private Set<Variable> initialised;
    private Set<Variable> possiblyInitialised;
    private InitialiserState initialiserState;

    /**
     * Creates a new ControlFlowVariables which has no initialised variables, and the specified initialiser state
     * @param initialiserRun - true if the initialiser has already been run, false if it has not been run
     */
    public ControlFlowVariables(boolean initialiserRun)
    {
      initialised = new HashSet<Variable>();
      possiblyInitialised = new HashSet<Variable>();
      initialiserState = initialiserRun ? InitialiserState.DEFINITELY_RUN : InitialiserState.NOT_RUN;
    }

    /**
     * Creates a new ControlFlowVariables with the specified sets of initialised and possiblyInitialised variables and the specified InitialiserState.
     * @param initialisedVariables - the set of initialised variables
     * @param possiblyInitialisedVariables - the set of possibly initialised variables
     * @param initialiserState - the initialiser state
     */
    public ControlFlowVariables(Set<Variable> initialisedVariables, Set<Variable> possiblyInitialisedVariables, InitialiserState initialiserState)
    {
      this.initialised = initialisedVariables;
      this.possiblyInitialised = possiblyInitialisedVariables;
      this.initialiserState = initialiserState;
    }

    /**
     * @return a copy of this ControlFlowVariables
     */
    public ControlFlowVariables copy()
    {
      return new ControlFlowVariables(new HashSet<Variable>(initialised), new HashSet<Variable>(possiblyInitialised), initialiserState);
    }

    /**
     * Combines the specified other ControlFlowVariables into this one
     * @param other - the ControlFlowVariables to combine into this one
     */
    private void combine(ControlFlowVariables other)
    {
      intersect(initialised, other.initialised);
      possiblyInitialised.addAll(other.possiblyInitialised);
      initialiserState = InitialiserState.combine(initialiserState, other.initialiserState);
    }

    /**
     * Takes the intersection of two variable sets, and stores the result in the first.
     * @param destination - the destination set, to filter any elements that do not exist in source out of
     * @param source - the source set
     */
    private static void intersect(Set<Variable> destination, Set<Variable> source)
    {
      Iterator<Variable> it = destination.iterator();
      while (it.hasNext())
      {
        Variable current = it.next();
        if (!source.contains(current))
        {
          it.remove();
        }
      }
    }
  }

  /**
   * Keeps track of metadata required for control flow checking, and provides methods for combining two intersecting control flow regions into one.
   * @author Anthony Bryant
   */
  private static final class ControlFlowState
  {
    // the variables at this point in the control flow checking process
    private ControlFlowVariables variables;
    // the variables at the points where a given BreakableStatement is broken out of
    private Map<BreakableStatement, ControlFlowVariables> breakVariables;
    // the variables at the points where a given BreakableStatement is continued through
    private Map<BreakableStatement, ControlFlowVariables> continueVariables;
    // the combined variable state from all of the termination points in all of the executed code so far (up to the try or catch block we are currently in)
    // since we combine the variables at each termination point, the result is a variable state which models all situations since the start of the current try or catch block
    // this is used to handle catch and finally blocks, which can be entered from any point in the corresponding try or catch block
    private ControlFlowVariables terminatedVariables;

    /**
     * Creates a new, empty, ControlFlowState, with the specified initialiser status.
     */
    ControlFlowState(boolean initialiserRun)
    {
      variables = new ControlFlowVariables(initialiserRun);
      breakVariables    = new HashMap<BreakableStatement, ControlFlowVariables>();
      continueVariables = new HashMap<BreakableStatement, ControlFlowVariables>();
      terminatedVariables = new ControlFlowVariables(initialiserRun);
    }

    /**
     * Creates a new ControlFlowState with the specified state.
     * @param variables - the state of the variables at this point
     * @param breakVariables - the sets of variable states at the point of breaking out of a given BreakableStatement
     * @param continueVariables - the sets of variable states at the point of continuing through a given BreakableStatement
     * @param terminatedVariables - the combined variable state from all of the termination points in all of the code executed so far
     */
    ControlFlowState(ControlFlowVariables variables,
                     Map<BreakableStatement, ControlFlowVariables> breakVariables,
                     Map<BreakableStatement, ControlFlowVariables> continueVariables,
                     ControlFlowVariables terminatedVariables)
    {
      this.variables = variables;
      this.breakVariables = breakVariables;
      this.continueVariables = continueVariables;
      this.terminatedVariables = terminatedVariables;
    }

    /**
     * @return a copy of this ControlFlowState
     */
    ControlFlowState copy()
    {
      Map<BreakableStatement, ControlFlowVariables> copiedBreakVariables = new HashMap<BreakableStatement, ControlFlowVariables>();
      for (Entry<BreakableStatement, ControlFlowVariables> entry : breakVariables.entrySet())
      {
        copiedBreakVariables.put(entry.getKey(), entry.getValue().copy());
      }
      Map<BreakableStatement, ControlFlowVariables> copiedContinueVariables = new HashMap<BreakableStatement, ControlFlowVariables>();
      for (Entry<BreakableStatement, ControlFlowVariables> entry : continueVariables.entrySet())
      {
        copiedContinueVariables.put(entry.getKey(), entry.getValue().copy());
      }
      return new ControlFlowState(variables.copy(),
                                  copiedBreakVariables,
                                  copiedContinueVariables,
                                  terminatedVariables.copy());
    }

    /**
     * Combines the current variable state with the variable state at the point of breaking out of the specified BreakableStatement.
     * @param breakableStatement - the BreakableStatement to combine this variable state with
     */
    void addToBreakVariables(BreakableStatement breakableStatement)
    {
      Map<BreakableStatement, ControlFlowVariables> newBreakVariables = new HashMap<BreakableStatement, ControlFlowVariables>();
      newBreakVariables.put(breakableStatement, variables);
      combineBreakableVariables(breakVariables, newBreakVariables);
    }

    /**
     * Combines the current variable state with the variable state at the point of continuing through the specified BreakableStatement.
     * @param breakableStatement - the BreakableStatement to combine this variable state with
     */
    void addToContinueVariables(BreakableStatement breakableStatement)
    {
      Map<BreakableStatement, ControlFlowVariables> newContinueVariables = new HashMap<BreakableStatement, ControlFlowVariables>();
      newContinueVariables.put(breakableStatement, variables);
      combineBreakableVariables(continueVariables, newContinueVariables);
    }

    /**
     * Overwrites the current variable state with the combined ones from each of the break statements for the specified BreakableStatement.
     * If there are no sets of break variables for this BreakableStatement, then the current set of variables is replaced with null.
     * @param breakableStatement - the BreakableStatement to overwrite the variables with the break variables of
     */
    void overwriteWithBreakVariables(BreakableStatement breakableStatement)
    {
      terminatedVariables.combine(variables);
      variables = breakVariables.get(breakableStatement);
    }

    /**
     * Overwrites the current variable state with the combined ones from each of the continue statements for the specified BreakableStatement.
     * If there are no sets of continue variables for this BreakableStatement, then the current set of variables is replaced with null.
     * @param breakableStatement - the BreakableStatement to overwrite the variables with the continue variables of
     */
    void overwriteWithContinueVariables(BreakableStatement breakableStatement)
    {
      terminatedVariables.combine(variables);
      variables = continueVariables.get(breakableStatement);
    }

    /**
     * Reintegrates the variable states from each of the break statements for the specified BreakableStatement into the current variable state.
     * @param breakableStatement - the BreakableStatement to reintegrate the break variables of
     */
    void reintegrateBreakVariables(BreakableStatement breakableStatement)
    {
      ControlFlowVariables breakStatementVariables = breakVariables.get(breakableStatement);
      if (breakStatementVariables != null)
      {
        variables.combine(breakStatementVariables);
      }
    }

    /**
     * Reintegrates the variable states from each of the continue statements for the specified BreakableStatement into the current variable state.
     * @param breakableStatement - the BreakableStatement to reintegrate the continue variables of
     */
    void reintegrateContinueVariables(BreakableStatement breakableStatement)
    {
      ControlFlowVariables continueStatementVariables = continueVariables.get(breakableStatement);
      if (continueStatementVariables != null)
      {
        variables.combine(continueStatementVariables);
      }
    }

    /**
     * Resets the terminated variables on this ControlFlowState to the current state of the variables.
     */
    void resetTerminatedState()
    {
      terminatedVariables = variables.copy();
    }

    /**
     * @return the terminated state of this ControlFlowState, which contains the generic state which takes into account every state since it was last reset
     */
    ControlFlowState getTerminatedState()
    {
      ControlFlowState copied = copy();
      copied.variables = terminatedVariables.copy();
      return copied;
    }

    /**
     * Overwrites this ControlFlowState object's initialised variable state with the specified object's state.
     * This method still combines the rest of the state as if this had returned.
     * @param state - the ControlFlowState to overwrite this one with
     */
    void overwrite(ControlFlowState state)
    {
      variables = state.variables;
      combineReturned(state);
    }

    /**
     * Combines all data from the specified ControlFlowState into this one.
     * @param state - the state to combine into this one
     */
    void combine(ControlFlowState state)
    {
      variables.combine(state.variables);
      combineReturned(state);
    }

    /**
     * Combines the specified variable state with this one, assuming that the specified state has returned, and therefore discounting all of its current variable information.
     * @param state - the state to combine with
     */
    void combineReturned(ControlFlowState state)
    {
      combineBreakableVariables(breakVariables, state.breakVariables);
      combineBreakableVariables(continueVariables, state.continueVariables);

      if (state.variables != null)
      {
        // this can happen if something has overwritten state's variables with break/continue variables which don't exist
        terminatedVariables.combine(state.variables);
      }
      terminatedVariables.combine(state.terminatedVariables);
    }

    /**
     * Combines the specified breakable variable states.
     * @param destination - the destination map to store the resulting variable states in
     * @param source - the source map to get new data for the destination from
     */
    private static void combineBreakableVariables(Map<BreakableStatement, ControlFlowVariables> destination, Map<BreakableStatement, ControlFlowVariables> source)
    {
      for (Entry<BreakableStatement, ControlFlowVariables> entry : source.entrySet())
      {
        if (destination.containsKey(entry.getKey()))
        {
          ControlFlowVariables destinationVariables = destination.get(entry.getKey());
          destinationVariables.combine(entry.getValue());
        }
        else
        {
          destination.put(entry.getKey(), entry.getValue().copy());
        }
      }
    }
  }

}
