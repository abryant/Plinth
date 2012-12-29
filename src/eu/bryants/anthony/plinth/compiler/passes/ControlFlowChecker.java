package eu.bryants.anthony.plinth.compiler.passes;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompilationUnit;
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
import eu.bryants.anthony.plinth.ast.statement.WhileStatement;
import eu.bryants.anthony.plinth.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.VoidType;
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
   * Checks that the control flow of the specified compilation unit is well defined.
   * @param compilationUnit - the CompilationUnit to check
   * @throws ConceptualException - if any control flow related errors are detected
   */
  public static void checkControlFlow(CompilationUnit compilationUnit) throws ConceptualException
  {
    for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
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
      ControlFlowVariables instanceVariables = new ControlFlowVariables(true);
      ControlFlowVariables staticVariables   = new ControlFlowVariables(true);

      // the super-class's constructor has always been run by the time we get to the initialiser, so add the superClassVariables to the initialiser's variable set now
      for (Variable var : superClassVariables)
      {
        instanceVariables.initialised.add(var);
        instanceVariables.possiblyInitialised.add(var);
      }

      for (Field field : typeDefinition.getNonStaticFields())
      {
        if (field.getType().hasDefaultValue() && !field.isFinal())
        {
          instanceVariables.initialised.add(field.getMemberVariable());
          instanceVariables.possiblyInitialised.add(field.getMemberVariable());
        }
      }

      // the non-static initialisers are immutable iff there is at least one immutable constructor, so find out whether one exists
      boolean hasImmutableConstructors = false;
      // the non-static initialisers are selfish iff all constructors are selfish
      boolean onlyHasSelfishConstructors = true;
      for (Constructor constructor : typeDefinition.getConstructors())
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

      for (Initialiser initialiser : typeDefinition.getInitialisers())
      {
        if (initialiser instanceof FieldInitialiser)
        {
          Field field = ((FieldInitialiser) initialiser).getField();
          if (field.isStatic())
          {
            checkControlFlow(field.getInitialiserExpression(), staticVariables.initialised, staticVariables.initialiserState, false, false, true, false);
            staticVariables.initialised.add(field.getGlobalVariable());
            staticVariables.possiblyInitialised.add(field.getGlobalVariable());
          }
          else
          {
            checkControlFlow(field.getInitialiserExpression(), instanceVariables.initialised, instanceVariables.initialiserState, true, onlyHasSelfishConstructors, false, hasImmutableConstructors);
            instanceVariables.initialised.add(field.getMemberVariable());
            instanceVariables.possiblyInitialised.add(field.getMemberVariable());
          }
        }
        else
        {
          if (initialiser.isStatic())
          {
            checkControlFlow(initialiser.getBlock(), typeDefinition, staticVariables, null, new LinkedList<BreakableStatement>(), false, false, true, false, true);
          }
          else
          {
            checkControlFlow(initialiser.getBlock(), typeDefinition, instanceVariables, null, new LinkedList<BreakableStatement>(), true, onlyHasSelfishConstructors, false, hasImmutableConstructors, true);
          }
        }
      }

      for (Field field : typeDefinition.getFields())
      {
        if (field.isStatic() && field.isFinal() && !staticVariables.initialised.contains(field.getGlobalVariable()))
        {
          throw new ConceptualException("The static final field '" + field.getName() + "' is not always initialised", field.getLexicalPhrase());
        }
      }

      DelegateConstructorVariables delegateConstructorVariables = new DelegateConstructorVariables();
      delegateConstructorVariables.initialiserDefinitelyInitialised = instanceVariables.initialised;
      delegateConstructorVariables.initialiserPossiblyInitialised = instanceVariables.possiblyInitialised;
      delegateConstructorVariables.superClassVariables = superClassVariables;
      for (Constructor constructor : typeDefinition.getConstructors())
      {
        checkControlFlow(constructor, delegateConstructorVariables);
      }
      for (Method method : typeDefinition.getAllMethods())
      {
        checkControlFlow(method);
      }
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
    ControlFlowVariables variables = new ControlFlowVariables(initialiserAlreadyRun);
    if (initialiserAlreadyRun)
    {
      // this should behave exactly as if we are running the no-args super() constructor
      // since initialiserVariables already contains all superclass member variables, just copy it to this Constructor's ControlFlowVariables
      variables.initialised = new HashSet<Variable>(delegateConstructorVariables.initialiserDefinitelyInitialised);
      variables.possiblyInitialised = new HashSet<Variable>(delegateConstructorVariables.initialiserPossiblyInitialised);
    }
    for (Parameter p : constructor.getParameters())
    {
      variables.initialised.add(p.getVariable());
      variables.possiblyInitialised.add(p.getVariable());
    }
    checkControlFlow(constructor.getBlock(), constructor.getContainingTypeDefinition(), variables, delegateConstructorVariables, new LinkedList<BreakableStatement>(), true, constructor.isSelfish(), false, constructor.isImmutable(), false);
    if (variables.initialiserState != InitialiserState.DEFINITELY_RUN)
    {
      throw new ConceptualException("Constructor does not always call a delegate constructor, i.e. this(...) or super(...), or otherwise implicitly run the initialiser", constructor.getLexicalPhrase());
    }
    for (Field field : constructor.getContainingTypeDefinition().getNonStaticFields())
    {
      if (!variables.initialised.contains(field.getMemberVariable()))
      {
        if (!field.getType().hasDefaultValue())
        {
          throw new ConceptualException("Constructor does not always initialise the non-static field '" + field.getName() + "', which does not have a default value", constructor.getLexicalPhrase());
        }
        if (field.isFinal())
        {
          throw new ConceptualException("Constructor does not always initialise the non-static final field '" + field.getName() + "'", constructor.getLexicalPhrase());
        }
      }
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
      if (method.getNativeName() == null && !(method instanceof BuiltinMethod))
      {
        throw new ConceptualException("A non-native method must always have a body", method.getLexicalPhrase());
      }
      // this method has no body, so there is nothing to check
      return;
    }
    ControlFlowVariables variables = new ControlFlowVariables(true);
    for (Parameter p : method.getParameters())
    {
      variables.initialised.add(p.getVariable());
      variables.possiblyInitialised.add(p.getVariable());
    }
    boolean returned = checkControlFlow(method.getBlock(), method.getContainingTypeDefinition(), variables, null, new LinkedList<BreakableStatement>(), false, false, method.isStatic(), method.isImmutable(), false);
    if (!returned && !(method.getReturnType() instanceof VoidType))
    {
      throw new ConceptualException("Method does not always return a value", method.getLexicalPhrase());
    }
  }

  /**
   * Checks that the control flow of the specified statement is well defined.
   * @param statement - the statement to check
   * @param enclosingTypeDefinition - the TypeDefinition that the specified Statement is enclosed inside
   * @param variables - the state of the variables before this statement, to be updated to the after-statement state
   * @param delegateConstructorVariables - the sets of variables which indicate which variables the initialiser initialises
   * @param enclosingBreakableStack - the stack of statements that can be broken out of that enclose this statement
   * @param inConstructor - true if the statement is part of a constructor call
   * @param inSelfishContext - true if the statement is part of a selfish constructor
   * @param inStaticContext - true if the statement is in a static context
   * @param inImmutableContext - true if the statement is in an immutable context
   * @param inInitialiser - true if the statement is in an initialiser
   * @return true if the statement returns from its enclosing function or control cannot reach statements after it, false if control flow continues after it
   * @throws ConceptualException - if any unreachable code is detected
   */
  private static boolean checkControlFlow(Statement statement, TypeDefinition enclosingTypeDefinition, ControlFlowVariables variables, DelegateConstructorVariables delegateConstructorVariables, LinkedList<BreakableStatement> enclosingBreakableStack,
                                          boolean inConstructor, boolean inSelfishContext, boolean inStaticContext, boolean inImmutableContext, boolean inInitialiser) throws ConceptualException
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assignStatement = (AssignStatement) statement;
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
                  if (variables.possiblyInitialised.contains(var))
                  {
                    throw new ConceptualException("Final field '" + var.getName() + "' may already have been initialised", assignStatement.getLexicalPhrase());
                  }
                }
                else
                {
                  throw new ConceptualException("Final field '" + var.getName() + "' cannot be modified", assignStatement.getLexicalPhrase());
                }
              }
              if (inImmutableContext && !inConstructor)
              {
                throw new ConceptualException("Cannot assign to the member variable '" + var.getName() + "' in an immutable context", assignStatement.getLexicalPhrase());
              }
              nowInitialisedVariables.add(var);
            }
            else if (var instanceof GlobalVariable)
            {
              if (var.isFinal())
              {
                if (inStaticContext && inInitialiser && ((GlobalVariable) var).getEnclosingTypeDefinition().equals(enclosingTypeDefinition))
                {
                  if (variables.possiblyInitialised.contains(var))
                  {
                    throw new ConceptualException("The static final field '" + var.getName() + "' may already have been initialised", assignStatement.getLexicalPhrase());
                  }
                }
                else // if not in a static initialiser
                {
                  throw new ConceptualException("The static final field '" + var.getName() + "' cannot be modified", assignStatement.getLexicalPhrase());
                }
              }
              if (inImmutableContext)
              {
                throw new ConceptualException("Cannot assign to the static variable '" + var.getName() + "' in an immutable context", assignStatement.getLexicalPhrase());
              }
              nowInitialisedVariables.add(var);
            }
            else // parameters and local variables
            {
              if (var.isFinal() && variables.possiblyInitialised.contains(var))
              {
                throw new ConceptualException("Variable '" + var.getName() + "' may already have been initialised.", assignStatement.getLexicalPhrase());
              }
              nowInitialisedVariables.add(var);
            }
          }
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          checkControlFlow(arrayElementAssignee.getArrayExpression(), variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          checkControlFlow(arrayElementAssignee.getDimensionExpression(), variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          ArrayType baseType = (ArrayType) arrayElementAssignee.getArrayExpression().getType();
          if (baseType.isContextuallyImmutable())
          {
            throw new ConceptualException("Cannot assign to an element of an immutable array", arrayElementAssignee.getLexicalPhrase());
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
              if (var.isFinal() && variables.possiblyInitialised.contains(var))
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
              checkControlFlow(fieldAccessExpression.getBaseExpression(), variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);

              Type baseType = fieldAccessExpression.getBaseExpression().getType();
              if (baseType instanceof NamedType && ((NamedType) baseType).isContextuallyImmutable() && !isMutableField)
              {
                throw new ConceptualException("Cannot assign to a non-mutable field of an immutable type", fieldAccessExpression.getLexicalPhrase());
              }
            }
            else
            {
              // we do not have a base expression, so we must have a base type, so the field must be static
              // in this case, since static variables must always be initialised to a default value, the control flow checker does not need to check that it is initialised

              if (inImmutableContext & !isMutableField)
              {
                throw new ConceptualException("Cannot assign to a static non-mutable field in an immutable context", fieldAccessExpression.getLexicalPhrase());
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
                    if (variables.possiblyInitialised.contains(globalVar))
                    {
                      throw new ConceptualException("The static final field '" + globalVar.getName() + "' may already have been initialised", assignStatement.getLexicalPhrase());
                    }
                    nowInitialisedVariables.add(globalVar);
                  }
                  else // if not in a static initialiser
                  {
                    throw new ConceptualException("The static final field '" + globalVar.getName() + "' cannot be modified", assignStatement.getLexicalPhrase());
                  }
                }
              }
              else
              {
                MemberVariable var = field.getMemberVariable();
                if (var != null && var.isFinal())
                {
                  throw new ConceptualException("Final field '" + var.getName() + "' cannot be modified", assignStatement.getLexicalPhrase());
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
        checkControlFlow(assignStatement.getExpression(), variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      variables.initialised.addAll(nowInitialisedVariables);
      variables.possiblyInitialised.addAll(nowInitialisedVariables);
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
        returned = checkControlFlow(s, enclosingTypeDefinition, variables, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
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
      variables.addToBreakVariables(breakable);
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
      variables.addToContinueVariables(breakable);
      // TODO: when we get switch statements, make sure continue is forbidden for them
      return true;
    }
    else if (statement instanceof DelegateConstructorStatement)
    {
      DelegateConstructorStatement delegateConstructorStatement = (DelegateConstructorStatement) statement;
      if (!inConstructor | inStaticContext | inInitialiser)
      {
        throw new ConceptualException("Delegate constructors may only be called from other constructors", delegateConstructorStatement.getLexicalPhrase());
      }
      if (variables.initialiserState != InitialiserState.NOT_RUN)
      {
        throw new ConceptualException("A delegate constructor may already have been run", delegateConstructorStatement.getLexicalPhrase());
      }
      if (inImmutableContext && !delegateConstructorStatement.getResolvedConstructor().isImmutable())
      {
        throw new ConceptualException("Cannot call a non-immutable delegate constructor from an immutable constructor", delegateConstructorStatement.getLexicalPhrase());
      }

      if (delegateConstructorStatement.isSuperConstructor() && delegateConstructorStatement.getResolvedConstructor().isSelfish())
      {
        throw new ConceptualException("Cannot call a selfish constructor as a super() constructor", delegateConstructorStatement.getLexicalPhrase());
      }
      if (!inSelfishContext && delegateConstructorStatement.getResolvedConstructor().isSelfish())
      {
        throw new ConceptualException("Cannot call a selfish delegate constructor from a not-selfish constructor", delegateConstructorStatement.getLexicalPhrase());
      }

      for (Expression argument : delegateConstructorStatement.getArguments())
      {
        checkControlFlow(argument, variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }

      variables.initialiserState = InitialiserState.DEFINITELY_RUN;

      // after a delegate constructor call, all superclass member variables have now been initialised
      for (Variable var : delegateConstructorVariables.superClassVariables)
      {
        if (var instanceof MemberVariable && var.isFinal() && variables.possiblyInitialised.contains(var))
        {
          throw new ConceptualException("Cannot call a delegate constructor here, since it would overwrite the value of '" + var.getName() + "' (in: " + ((MemberVariable) var).getEnclosingTypeDefinition().getQualifiedName() + "), which may already have been initialised", delegateConstructorStatement.getLexicalPhrase());
        }
        variables.initialised.add(var);
        variables.possiblyInitialised.add(var);
      }

      if (delegateConstructorStatement.isSuperConstructor())
      {
        // a super() constructor has been run, so all variables that are set by the initialiser have now been initialised
        for (Variable var : delegateConstructorVariables.initialiserDefinitelyInitialised)
        {
          variables.initialised.add(var);
        }
      }
      else
      {
        // a this() constructor has been run, so all of the member variables have now been initialised
        for (Field field : enclosingTypeDefinition.getNonStaticFields())
        {
          MemberVariable var = field.getMemberVariable();
          if (field.isFinal() && variables.possiblyInitialised.contains(var))
          {
            throw new ConceptualException("Cannot call a this() constructor here, since it would overwrite the value of '" + field.getName() + "', which may already have been initialised", delegateConstructorStatement.getLexicalPhrase());
          }
          variables.initialised.add(var);
          variables.possiblyInitialised.add(var);
        }
      }
      return false;
    }
    else if (statement instanceof ExpressionStatement)
    {
      checkControlFlow(((ExpressionStatement) statement).getExpression(), variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
        boolean returned = checkControlFlow(init, enclosingTypeDefinition, variables, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
        if (returned)
        {
          throw new IllegalStateException("Reached a state where a for loop initialisation statement returned");
        }
      }
      ControlFlowVariables loopVariables = variables.copy();
      if (condition != null)
      {
        checkControlFlow(condition, loopVariables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      boolean returned = checkControlFlow(block, enclosingTypeDefinition, loopVariables, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      if (returned)
      {
        loopVariables.overwriteWithContinueVariables(forStatement);
      }
      else
      {
        loopVariables.reintegrateContinueVariables(forStatement);
      }

      if (update != null)
      {
        if (returned && !forStatement.isContinuedThrough())
        {
          throw new ConceptualException("Unreachable code", update.getLexicalPhrase());
        }
        boolean updateReturned = checkControlFlow(update, enclosingTypeDefinition, loopVariables, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
        if (updateReturned)
        {
          throw new IllegalStateException("Reached a state where a for loop update statement returned");
        }
      }

      // run through the conditional, loop block, and update again, so that we catch any final variables that are initialised in the loop
      loopVariables.combine(variables);
      if (condition != null)
      {
        checkControlFlow(condition, loopVariables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      boolean secondReturned = checkControlFlow(block, enclosingTypeDefinition, loopVariables, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      if (secondReturned)
      {
        loopVariables.overwriteWithContinueVariables(forStatement);
      }
      else
      {
        loopVariables.reintegrateContinueVariables(forStatement);
      }
      if (update != null)
      {
        checkControlFlow(update, enclosingTypeDefinition, loopVariables, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      }

      if (returned)
      {
        variables.combineReturned(loopVariables);
      }
      else
      {
        variables.combine(loopVariables);
      }

      // if there is no condition, then the only way to get to the code after the loop is to break out of it
      // so in that case, we overwrite the variables with the break variables
      if (condition == null)
      {
        variables.overwriteWithBreakVariables(forStatement);
      }
      else
      {
        variables.reintegrateBreakVariables(forStatement);
      }

      enclosingBreakableStack.pop();

      // if there is no conditional and the for statement is never broken out of, then control cannot continue after the end of the loop
      return condition == null && !forStatement.isBrokenOutOf();
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      checkControlFlow(ifStatement.getExpression(), variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      Statement thenClause = ifStatement.getThenClause();
      Statement elseClause = ifStatement.getElseClause();
      if (elseClause == null)
      {
        ControlFlowVariables thenClauseVariables = variables.copy();
        boolean thenReturned = checkControlFlow(thenClause, enclosingTypeDefinition, thenClauseVariables, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
        if (thenReturned)
        {
          variables.combineReturned(thenClauseVariables);
        }
        else
        {
          variables.combine(thenClauseVariables);
        }
        return false;
      }
      ControlFlowVariables thenClauseVariables = variables.copy();
      ControlFlowVariables elseClauseVariables = variables.copy();
      boolean thenReturned = checkControlFlow(thenClause, enclosingTypeDefinition, thenClauseVariables, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      boolean elseReturned = checkControlFlow(elseClause, enclosingTypeDefinition, elseClauseVariables, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      if (!thenReturned & !elseReturned)
      {
        variables.overwrite(thenClauseVariables);
        variables.combine(elseClauseVariables);
      }
      else if (!thenReturned & elseReturned)
      {
        variables.overwrite(thenClauseVariables);
        variables.combineReturned(elseClauseVariables);
      }
      else if (thenReturned & !elseReturned)
      {
        variables.overwrite(elseClauseVariables);
        variables.combineReturned(thenClauseVariables);
      }
      else // thenReturned & elseReturned
      {
        variables.combineReturned(thenClauseVariables);
        variables.combineReturned(elseClauseVariables);
      }
      return thenReturned & elseReturned;
    }
    else if (statement instanceof PrefixIncDecStatement)
    {
      PrefixIncDecStatement prefixIncDecStatement = (PrefixIncDecStatement) statement;
      Assignee assignee = prefixIncDecStatement.getAssignee();
      if (assignee instanceof VariableAssignee)
      {
        Variable var = ((VariableAssignee) assignee).getResolvedVariable();
        if (!(var instanceof GlobalVariable) && !variables.initialised.contains(var) && (inConstructor || !(var instanceof MemberVariable)))
        {
          throw new ConceptualException("Variable '" + ((VariableAssignee) assignee).getVariableName() + "' may not have been initialised", assignee.getLexicalPhrase());
        }
        if (inStaticContext && var instanceof MemberVariable)
        {
          throw new ConceptualException("The non-static member variable '" + ((VariableAssignee) assignee).getVariableName() + "' does not exist in static methods", assignee.getLexicalPhrase());
        }
        if (var.isFinal())
        {
          throw new ConceptualException("Final variable '" + ((VariableAssignee) assignee).getVariableName() + "' cannot be modified", assignee.getLexicalPhrase());
        }
        if (inImmutableContext)
        {
          if (!inConstructor && var instanceof MemberVariable)
          {
            throw new ConceptualException("Cannot modify the member variable '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase());
          }
          else if (var instanceof GlobalVariable)
          {
            throw new ConceptualException("Cannot modify the static variable '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase());
          }
        }
      }
      else if (assignee instanceof ArrayElementAssignee)
      {
        ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
        checkControlFlow(arrayElementAssignee.getArrayExpression(), variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        checkControlFlow(arrayElementAssignee.getDimensionExpression(), variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        if (((ArrayType) arrayElementAssignee.getArrayExpression().getType()).isContextuallyImmutable())
        {
          throw new ConceptualException("Cannot modify an element of an immutable array", assignee.getLexicalPhrase());
        }
      }
      else if (assignee instanceof FieldAssignee)
      {
        FieldAssignee fieldAssignee = (FieldAssignee) assignee;
        FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
        Member resolvedMember = fieldAccessExpression.getResolvedMember();
        // treat this as a field access, and check for uninitialised variables as normal
        checkControlFlow(fieldAccessExpression, variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);

        boolean isMutableField = resolvedMember instanceof Field && ((Field) resolvedMember).isMutable();
        if (fieldAccessExpression.getBaseExpression() != null)
        {
          Type baseType = fieldAccessExpression.getBaseExpression().getType();
          if (baseType instanceof NamedType && ((NamedType) baseType).isContextuallyImmutable() && !isMutableField)
          {
            throw new ConceptualException("Cannot modify a non-mutable field of an immutable type", assignee.getLexicalPhrase());
          }
        }
        else
        {
          if (inImmutableContext & !isMutableField)
          {
            throw new ConceptualException("Cannot modify a static non-mutable field in an immutable context", assignee.getLexicalPhrase());
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
              throw new ConceptualException("Static final field '" + globalVar.getName() + "' cannot be modified", assignee.getLexicalPhrase());
            }
          }
          else
          {
            MemberVariable var = ((Field) resolvedMember).getMemberVariable();
            if (var != null && var.isFinal())
            {
              throw new ConceptualException("Final field '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase());
            }
          }
        }
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
      if (inInitialiser)
      {
        throw new ConceptualException("Cannot return from an initialiser", statement.getLexicalPhrase());
      }
      Expression returnedExpression = ((ReturnStatement) statement).getExpression();
      if (returnedExpression != null)
      {
        checkControlFlow(returnedExpression, variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      return true;
    }
    else if (statement instanceof ShorthandAssignStatement)
    {
      ShorthandAssignStatement shorthandAssignStatement = (ShorthandAssignStatement) statement;
      for (Assignee assignee : shorthandAssignStatement.getAssignees())
      {
        if (assignee instanceof VariableAssignee)
        {
          VariableAssignee variableAssignee = (VariableAssignee) assignee;
          Variable var = variableAssignee.getResolvedVariable();
          if (!(var instanceof GlobalVariable) && !variables.initialised.contains(var) && (inConstructor || !(var instanceof MemberVariable)))
          {
            throw new ConceptualException("Variable '" + variableAssignee.getVariableName() + "' may not have been initialised", variableAssignee.getLexicalPhrase());
          }
          if (inStaticContext && var instanceof MemberVariable)
          {
            throw new ConceptualException("The non-static member variable '" + variableAssignee.getVariableName() + "' does not exist in static methods", variableAssignee.getLexicalPhrase());
          }
          if (var.isFinal())
          {
            throw new ConceptualException("Final variable '" + variableAssignee.getVariableName() + "' cannot be modified", assignee.getLexicalPhrase());
          }
          if (inImmutableContext)
          {
            if (!inConstructor && var instanceof MemberVariable)
            {
              throw new ConceptualException("Cannot modify the member variable '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase());
            }
            else if (var instanceof GlobalVariable)
            {
              throw new ConceptualException("Cannot modify the static variable '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase());
            }
          }
        }
        else if (assignee instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
          checkControlFlow(arrayElementAssignee.getArrayExpression(), variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          checkControlFlow(arrayElementAssignee.getDimensionExpression(), variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          if (((ArrayType) arrayElementAssignee.getArrayExpression().getType()).isContextuallyImmutable())
          {
            throw new ConceptualException("Cannot modify an element of an immutable array", assignee.getLexicalPhrase());
          }
        }
        else if (assignee instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignee;
          FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
          Member resolvedMember = fieldAccessExpression.getResolvedMember();
          // treat this as a field access, and check for uninitialised variables as normal
          checkControlFlow(fieldAccessExpression, variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);

          boolean isMutableField = resolvedMember instanceof Field && ((Field) resolvedMember).isMutable();
          if (fieldAccessExpression.getBaseExpression() != null)
          {
            Type baseType = fieldAccessExpression.getBaseExpression().getType();
            if (baseType instanceof NamedType && ((NamedType) baseType).isContextuallyImmutable() && !isMutableField)
            {
              throw new ConceptualException("Cannot modify a non-mutable field of an immutable type", assignee.getLexicalPhrase());
            }
          }
          else
          {
            if (inImmutableContext & !isMutableField)
            {
              throw new ConceptualException("Cannot modify a static non-mutable field in an immutable context", assignee.getLexicalPhrase());
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
                throw new ConceptualException("Static final field '" + globalVar.getName() + "' cannot be modified", assignee.getLexicalPhrase());
              }
            }
            else
            {
              MemberVariable var = ((Field) resolvedMember).getMemberVariable();
              if (var != null && var.isFinal())
              {
                throw new ConceptualException("Final field '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase());
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
      checkControlFlow(shorthandAssignStatement.getExpression(), variables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      return false;
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;

      ControlFlowVariables loopVariables = variables.copy();

      checkControlFlow(whileStatement.getExpression(), loopVariables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);

      // we don't care about the result of this, as the loop could execute zero times
      enclosingBreakableStack.push(whileStatement);
      boolean whileReturned = checkControlFlow(whileStatement.getStatement(), enclosingTypeDefinition, loopVariables, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      if (whileReturned)
      {
        loopVariables.overwriteWithContinueVariables(whileStatement);
      }
      else
      {
        loopVariables.reintegrateContinueVariables(whileStatement);
      }

      // run through the conditional and loop block again, so that we catch any final variables that are initialised in the loop
      checkControlFlow(whileStatement.getExpression(), loopVariables.initialised, variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      boolean secondReturned = checkControlFlow(whileStatement.getStatement(), enclosingTypeDefinition, loopVariables, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      if (secondReturned)
      {
        loopVariables.overwriteWithContinueVariables(whileStatement);
      }
      else
      {
        loopVariables.reintegrateContinueVariables(whileStatement);
      }

      if (whileReturned)
      {
        variables.combineReturned(loopVariables);
      }
      else
      {
        variables.combine(loopVariables);
      }
      variables.reintegrateBreakVariables(whileStatement);

      enclosingBreakableStack.pop();
      return false;
    }
    throw new ConceptualException("Internal control flow checking error: Unknown statement type", statement.getLexicalPhrase());
  }

  private static void checkControlFlow(Expression expression, Set<Variable> initialisedVariables, InitialiserState initialiserState, boolean inConstructor, boolean inSelfishContext, boolean inStaticContext, boolean inImmutableContext) throws ConceptualException
  {
    if (expression instanceof ArithmeticExpression)
    {
      ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;
      checkControlFlow(arithmeticExpression.getLeftSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      checkControlFlow(arithmeticExpression.getRightSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof ArrayAccessExpression)
    {
      ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
      checkControlFlow(arrayAccessExpression.getArrayExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      checkControlFlow(arrayAccessExpression.getDimensionExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof ArrayCreationExpression)
    {
      ArrayCreationExpression creationExpression = (ArrayCreationExpression) expression;
      // TODO: when we add the "new [7]Foo(creationFunction)" syntax, make sure creationFunction is immutable if we are in an immutable context
      if (creationExpression.getDimensionExpressions() != null)
      {
        for (Expression e : creationExpression.getDimensionExpressions())
        {
          checkControlFlow(e, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
      }
      if (creationExpression.getValueExpressions() != null)
      {
        for (Expression e : creationExpression.getValueExpressions())
        {
          checkControlFlow(e, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
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
      for (Expression argument : classCreationExpression.getArguments())
      {
        checkControlFlow(argument, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      if (inImmutableContext && !classCreationExpression.getResolvedConstructor().isImmutable())
      {
        throw new ConceptualException("Cannot call a non-immutable constructor from an immutable context (it may alter global variables)", classCreationExpression.getLexicalPhrase());
      }
    }
    else if (expression instanceof EqualityExpression)
    {
      EqualityExpression equalityExpression = (EqualityExpression) expression;
      checkControlFlow(equalityExpression.getLeftSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      checkControlFlow(equalityExpression.getRightSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof FieldAccessExpression)
    {
      FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) expression;
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
          throw new ConceptualException("Field '" + ((Field) resolvedMember).getName() + "' may not have been initialised", fieldAccessExpression.getLexicalPhrase());
        }
        if (resolvedMember instanceof Method && !((Method) resolvedMember).isStatic())
        {
          // non-static methods cannot be accessed as fields before 'this' has been initialised
          Method resolvedMethod = (Method) resolvedMember;
          if (!inSelfishContext)
          {
            throw new ConceptualException("Cannot access methods on 'this' unless we are within a selfish constructor", expression.getLexicalPhrase());
          }
          if (initialiserState != InitialiserState.DEFINITELY_RUN)
          {
            throw new ConceptualException("Cannot access methods on 'this' here. The initialiser of this '" + new NamedType(false, false, resolvedMethod.getContainingTypeDefinition()) + "' may not have been run yet", expression.getLexicalPhrase());
          }
          for (Field field : resolvedMethod.getContainingTypeDefinition().getNonStaticFields())
          {
            if (!initialisedVariables.contains(field.getMemberVariable()))
            {
              throw new ConceptualException("Cannot access methods on 'this' here. Not all of the non-static fields of this '" + new NamedType(false, false, resolvedMethod.getContainingTypeDefinition()) + "' have been initialised (specifically: '" + field.getName() + "'), and I can't work out whether or not you're going to initialise them before they're used", expression.getLexicalPhrase());
            }
          }
        }
      }
      else if (subExpression != null)
      {
        // otherwise (if we aren't in a constructor, or the base expression isn't 'this', but we do have a base expression) check the uninitialised variables normally
        checkControlFlow(fieldAccessExpression.getBaseExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      else
      {
        // otherwise, we do not have a base expression, so we must have a base type, so the field must be static
        // in this case, since static variables must always be initialised to a default value, the control flow checker does not need to check anything
      }
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression functionCallExpression = (FunctionCallExpression) expression;
      if (functionCallExpression.getResolvedMethod() != null)
      {
        if (inImmutableContext && !functionCallExpression.getResolvedMethod().isImmutable())
        {
          throw new ConceptualException("Cannot call a non-immutable method from an immutable context", functionCallExpression.getLexicalPhrase());
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
              throw new ConceptualException("Cannot call methods on 'this' unless we are within a selfish constructor", expression.getLexicalPhrase());
            }
            if (initialiserState != InitialiserState.DEFINITELY_RUN)
            {
              throw new ConceptualException("Cannot call methods on 'this' here. The initialiser of this '" + new NamedType(false, false, resolvedMethod.getContainingTypeDefinition()) + "' may not have been run yet", expression.getLexicalPhrase());
            }
            for (Field field : resolvedMethod.getContainingTypeDefinition().getNonStaticFields())
            {
              if (!initialisedVariables.contains(field.getMemberVariable()))
              {
                throw new ConceptualException("Cannot call methods on 'this' here. Not all of the non-static fields of this '" + new NamedType(false, false, resolvedMethod.getContainingTypeDefinition()) + "' have been initialised (specifically: '" + field.getName() + "'), and I can't work out whether or not you're going to initialise them before they're used", expression.getLexicalPhrase());
              }
            }
          }
          if (inStaticContext && !resolvedMethod.isStatic())
          {
            throw new ConceptualException("Cannot call the non-static method '" + resolvedMethod.getName() + "' from a static context", functionCallExpression.getLexicalPhrase());
          }
        }
        else // resolvedBaseExpression != null
        {
          checkControlFlow(resolvedBaseExpression, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          Type baseType = resolvedBaseExpression.getType();
          if ((baseType instanceof ArrayType && ((ArrayType) baseType).isContextuallyImmutable()) ||
              (baseType instanceof NamedType && ((NamedType) baseType).isContextuallyImmutable()))
          {
            if (!functionCallExpression.getResolvedMethod().isImmutable())
            {
              throw new ConceptualException("Cannot call a non-immutable method on an immutable object", functionCallExpression.getLexicalPhrase());
            }
          }
        }
      }
      else if (functionCallExpression.getResolvedConstructor() != null)
      {
        if (inImmutableContext && !functionCallExpression.getResolvedConstructor().isImmutable())
        {
          throw new ConceptualException("Cannot call a non-immutable constructor from an immutable context (it may alter global variables)", functionCallExpression.getLexicalPhrase());
        }
      }
      else if (functionCallExpression.getResolvedBaseExpression() != null)
      {
        checkControlFlow(functionCallExpression.getResolvedBaseExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        FunctionType type = (FunctionType) functionCallExpression.getResolvedBaseExpression().getType();
        if (inImmutableContext && !type.isImmutable())
        {
          throw new ConceptualException("Cannot call a non-immutable function from an immutable context", functionCallExpression.getLexicalPhrase());
        }
      }
      else
      {
        throw new IllegalStateException("Unresolved function call: " + functionCallExpression);
      }
      // check that the arguments are all initialised
      for (Expression e : functionCallExpression.getArguments())
      {
        checkControlFlow(e, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
    }
    else if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIfExpression = (InlineIfExpression) expression;
      checkControlFlow(inlineIfExpression.getCondition(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      checkControlFlow(inlineIfExpression.getThenExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      checkControlFlow(inlineIfExpression.getElseExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof LogicalExpression)
    {
      LogicalExpression logicalExpression = (LogicalExpression) expression;
      checkControlFlow(logicalExpression.getLeftSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      checkControlFlow(logicalExpression.getRightSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof MinusExpression)
    {
      checkControlFlow(((MinusExpression) expression).getExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof NullCoalescingExpression)
    {
      checkControlFlow(((NullCoalescingExpression) expression).getNullableExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      checkControlFlow(((NullCoalescingExpression) expression).getAlternativeExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
      checkControlFlow(relationalExpression.getLeftSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      checkControlFlow(relationalExpression.getRightSubExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof ShiftExpression)
    {
      ShiftExpression shiftExpression = (ShiftExpression) expression;
      checkControlFlow(shiftExpression.getLeftExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      checkControlFlow(shiftExpression.getRightExpression(), initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
        if (initialiserState != InitialiserState.DEFINITELY_RUN)
        {
          throw new ConceptualException("Cannot use 'this' here. The initialiser of this '" + type + "' may not have been run yet.", expression.getLexicalPhrase());
        }
        for (Field field : type.getResolvedTypeDefinition().getNonStaticFields())
        {
          if (!initialisedVariables.contains(field.getMemberVariable()))
          {
            throw new ConceptualException("Cannot use 'this' here. Not all of the non-static fields of this '" + type + "' have been initialised (specifically: '" + field.getName() + "'), and I can't work out whether or not you're going to initialise them before they're used", expression.getLexicalPhrase());
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
        checkControlFlow(subExpressions[i], initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
          if (initialiserState != InitialiserState.DEFINITELY_RUN)
          {
            throw new ConceptualException("Cannot access methods on 'this' here. The initialiser of this '" + new NamedType(false, false, method.getContainingTypeDefinition()) + "' may not have been run yet", expression.getLexicalPhrase());
          }
          for (Field field : method.getContainingTypeDefinition().getNonStaticFields())
          {
            if (!initialisedVariables.contains(field.getMemberVariable()))
            {
              throw new ConceptualException("Cannot access methods on 'this' here. Not all of the non-static fields of this '" + new NamedType(false, false, method.getContainingTypeDefinition()) + "' have been initialised (specifically: '" + field.getName() + "'), and I can't work out whether or not you're going to initialise them before they're used", expression.getLexicalPhrase());
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
      throw new ConceptualException("Internal control flow checking error: Unknown expression type", expression.getLexicalPhrase());
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
   * Keeps track of metadata required for control flow checking, and provides methods for combining two intersection control flow regions into one.
   * @author Anthony Bryant
   */
  private static final class ControlFlowVariables
  {
    // the initialised and possibly initialised variables at this point in the control flow checking process
    private Set<Variable> initialised;
    private Set<Variable> possiblyInitialised;
    private InitialiserState initialiserState;
    // the initialised and possibly initialised variables at the points where a given BreakableStatement is broken out of
    private Map<BreakableStatement, Set<Variable>> breakInitialised;
    private Map<BreakableStatement, Set<Variable>> breakPossiblyInitialised;
    private Map<BreakableStatement, InitialiserState> breakInitialiserState;
    // the initialised and possibly initialised variables at the points where a given BreakableStatement is continued through
    private Map<BreakableStatement, Set<Variable>> continueInitialised;
    private Map<BreakableStatement, Set<Variable>> continuePossiblyInitialised;
    private Map<BreakableStatement, InitialiserState> continueInitialiserState;

    /**
     * Creates a new, empty, ControlFlowVariables.
     */
    ControlFlowVariables(boolean initialiserRun)
    {
      initialised         = new HashSet<Variable>();
      possiblyInitialised = new HashSet<Variable>();
      initialiserState    = initialiserRun ? InitialiserState.DEFINITELY_RUN : InitialiserState.NOT_RUN;
      breakInitialised            = new HashMap<BreakableStatement, Set<Variable>>();
      breakPossiblyInitialised    = new HashMap<BreakableStatement, Set<Variable>>();
      breakInitialiserState       = new HashMap<BreakableStatement, InitialiserState>();
      continueInitialised         = new HashMap<BreakableStatement, Set<Variable>>();
      continuePossiblyInitialised = new HashMap<BreakableStatement, Set<Variable>>();
      continueInitialiserState    = new HashMap<BreakableStatement, InitialiserState>();
    }

    /**
     * Creates a new ControlFlowVariables with the specified state.
     * @param initialised         - the set of variables which will  be initialised at this point
     * @param possiblyInitialised - the set of variables which might be initialised at this point
     * @param initialiserState    - true if the initialiser of the constructor has been run, false otherwise
     * @param breakInitialised            - the sets of variables which will  be initialised when breaking out of    a given BreakableStatement
     * @param breakPossiblyInitialised    - the sets of variables which might be initialised when breaking out of    a given BreakableStatement
     * @param breakInitialiserState       - whether or not the initialiser has been run at the point of breaking out of a given BreakableStatement
     * @param continueInitialised         - the sets of variables which will  be initialised when continuing through a given BreakableStatement
     * @param continuePossiblyInitialised - the sets of variables which might be initialised when continuing through a given BreakableStatement
     * @param continueInitialiserState    - whether or not the initialiser has been run at the point of continuing through a given BreakableStatement
     */
    ControlFlowVariables(Set<Variable> initialised, Set<Variable> possiblyInitialised, InitialiserState initialiserState,
                         Map<BreakableStatement, Set<Variable>> breakInitialised,
                         Map<BreakableStatement, Set<Variable>> breakPossiblyInitialised,
                         Map<BreakableStatement, InitialiserState> breakInitialiserState,
                         Map<BreakableStatement, Set<Variable>> continueInitialised,
                         Map<BreakableStatement, Set<Variable>> continuePossiblyInitialised,
                         Map<BreakableStatement, InitialiserState> continueInitialiserState)
    {
      this.initialised         = initialised;
      this.possiblyInitialised = possiblyInitialised;
      this.initialiserState    = initialiserState;
      this.breakInitialised            = breakInitialised;
      this.breakPossiblyInitialised    = breakPossiblyInitialised;
      this.breakInitialiserState       = breakInitialiserState;
      this.continueInitialised         = continueInitialised;
      this.continuePossiblyInitialised = continuePossiblyInitialised;
      this.continueInitialiserState      = continueInitialiserState;
    }

    /**
     * @return a copy of this ControlFlowVariables state
     */
    ControlFlowVariables copy()
    {
      return new ControlFlowVariables(new HashSet<Variable>(initialised), new HashSet<Variable>(possiblyInitialised), initialiserState,
                                      new HashMap<BreakableStatement, Set<Variable>>(breakInitialised),
                                      new HashMap<BreakableStatement, Set<Variable>>(breakPossiblyInitialised),
                                      new HashMap<BreakableStatement, InitialiserState>(breakInitialiserState),
                                      new HashMap<BreakableStatement, Set<Variable>>(continueInitialised),
                                      new HashMap<BreakableStatement, Set<Variable>>(continuePossiblyInitialised),
                                      new HashMap<BreakableStatement, InitialiserState>(continueInitialiserState));
    }

    /**
     * Combines the current variable state with the variable state at the point of breaking out of the specified BreakableStatement.
     * @param breakableStatement - the BreakableStatement to combine this variable state with
     */
    void addToBreakVariables(BreakableStatement breakableStatement)
    {
      Map<BreakableStatement, Set<Variable>> newBreakInitialised = new HashMap<BreakableStatement, Set<Variable>>();
      newBreakInitialised.put(breakableStatement, initialised);
      combineBreakableSet(breakInitialised, newBreakInitialised, true);

      Map<BreakableStatement, Set<Variable>> newBreakPossiblyInitialised = new HashMap<BreakableStatement, Set<Variable>>();
      newBreakPossiblyInitialised.put(breakableStatement, possiblyInitialised);
      combineBreakableSet(breakPossiblyInitialised, newBreakPossiblyInitialised, false);

      Map<BreakableStatement, InitialiserState> newBreakInitialiserState = new HashMap<BreakableStatement, InitialiserState>();
      newBreakInitialiserState.put(breakableStatement, initialiserState);
      combineInitialiserStates(breakInitialiserState, newBreakInitialiserState);
    }

    /**
     * Combines the current variable state with the variable state at the point of continuing through the specified BreakableStatement.
     * @param breakableStatement - the BreakableStatement to combine this variable state with
     */
    void addToContinueVariables(BreakableStatement breakableStatement)
    {
      Map<BreakableStatement, Set<Variable>> newContinueInitialised = new HashMap<BreakableStatement, Set<Variable>>();
      newContinueInitialised.put(breakableStatement, initialised);
      combineBreakableSet(continueInitialised, newContinueInitialised, true);

      Map<BreakableStatement, Set<Variable>> newContinuePossiblyInitialised = new HashMap<BreakableStatement, Set<Variable>>();
      newContinuePossiblyInitialised.put(breakableStatement, possiblyInitialised);
      combineBreakableSet(continuePossiblyInitialised, newContinuePossiblyInitialised, false);

      Map<BreakableStatement, InitialiserState> newContinueInitialiserState = new HashMap<BreakableStatement, InitialiserState>();
      newContinueInitialiserState.put(breakableStatement, initialiserState);
      combineInitialiserStates(continueInitialiserState, newContinueInitialiserState);
    }

    /**
     * Overwrites the sets of initialised and possiblyInitialised variables in this ControlFlowVariables with the combined ones from each of the break statements for the specified BreakableStatement.
     * If there are no sets of break variables for this BreakableStatement, then the current set of variables is replaced with an empty set.
     * @param breakableStatement - the BreakableStatement to overwrite the variables with the break variables of
     */
    void overwriteWithBreakVariables(BreakableStatement breakableStatement)
    {
      Set<Variable> breakVariables = breakInitialised.get(breakableStatement);
      if (breakVariables == null)
      {
        breakVariables = new HashSet<Variable>();
      }
      initialised = breakVariables;

      Set<Variable> breakPossibleVariables = breakPossiblyInitialised.get(breakableStatement);
      if (breakPossibleVariables == null)
      {
        breakPossibleVariables = new HashSet<Variable>();
      }
      possiblyInitialised = breakPossibleVariables;

      initialiserState = breakInitialiserState.get(breakableStatement);
    }

    /**
     * Overwrites the sets of initialised and possiblyInitialised variables in this ControlFlowVariables with the combined ones from each of the continue statements for the specified BreakableStatement.
     * If there are no sets of continue variables for this BreakableStatement, then the current set of variables is replaced with an empty set.
     * @param breakableStatement - the BreakableStatement to overwrite the variables with the continue variables of
     */
    void overwriteWithContinueVariables(BreakableStatement breakableStatement)
    {
      Set<Variable> continueVariables = continueInitialised.get(breakableStatement);
      if (continueVariables == null)
      {
        continueVariables = new HashSet<Variable>();
      }
      initialised = continueVariables;

      Set<Variable> continuePossibleVariables = continuePossiblyInitialised.get(breakableStatement);
      if (continuePossibleVariables == null)
      {
        continuePossibleVariables = new HashSet<Variable>();
      }
      possiblyInitialised = continuePossibleVariables;

      initialiserState = continueInitialiserState.get(breakableStatement);
    }

    /**
     * Reintegrates the variable sets from each of the break statements for the specified BreakableStatement into the current initialised and possiblyInitialised sets.
     * @param breakableStatement - the BreakableStatement to reintegrate the break variables of
     */
    void reintegrateBreakVariables(BreakableStatement breakableStatement)
    {
      Set<Variable> breakVariables = breakInitialised.get(breakableStatement);
      if (breakVariables != null)
      {
        intersect(initialised, breakVariables);
      }
      Set<Variable> breakPossibleVariables = breakPossiblyInitialised.get(breakableStatement);
      if (breakPossibleVariables != null)
      {
        possiblyInitialised.addAll(breakPossibleVariables);
      }
      InitialiserState breakInitialiserRun = breakInitialiserState.get(breakableStatement);
      if (breakInitialiserRun != null)
      {
        initialiserState = combineInitialiserState(initialiserState, breakInitialiserRun);
      }
    }

    /**
     * Reintegrates the variable sets from each of the continue statements for the specified BreakableStatement into the current initialised and possiblyInitialised sets.
     * @param breakableStatement - the BreakableStatement to reintegrate the continue variables of
     */
    void reintegrateContinueVariables(BreakableStatement breakableStatement)
    {
      Set<Variable> continueVariables = continueInitialised.get(breakableStatement);
      if (continueVariables != null)
      {
        intersect(initialised, continueVariables);
      }
      Set<Variable> continuePossibleVariables = continuePossiblyInitialised.get(breakableStatement);
      if (continuePossibleVariables != null)
      {
        possiblyInitialised.addAll(continuePossibleVariables);
      }
      InitialiserState continueInitialiserRun = continueInitialiserState.get(breakableStatement);
      if (continueInitialiserRun != null)
      {
        initialiserState = combineInitialiserState(initialiserState, continueInitialiserRun);
      }
    }

    /**
     * Overwrites this ControlFlowVariables object's initialised variable state with the specified object's state.
     * This method still combines the rest of the state as if this had returned.
     * @param variables - the ControlFlowVariables to overwrite this one with
     */
    void overwrite(ControlFlowVariables variables)
    {
      initialised = new HashSet<Variable>(variables.initialised);
      possiblyInitialised = new HashSet<Variable>(variables.possiblyInitialised);
      initialiserState = variables.initialiserState;
      combineReturned(variables);
    }

    /**
     * Combines all data from the specified ControlFlowVariables into this one.
     * @param variables - the state to combine into this one
     */
    void combine(ControlFlowVariables variables)
    {
      intersect(initialised, variables.initialised);
      possiblyInitialised.addAll(variables.possiblyInitialised);
      initialiserState = combineInitialiserState(initialiserState, variables.initialiserState);
      combineReturned(variables);
    }

    /**
     * Combines the specified variable state with this one, assuming that the specified state has returned, and therefore discounting all of its current variable information.
     * @param variables - the state to combine with
     */
    void combineReturned(ControlFlowVariables variables)
    {
      combineBreakableSet(breakInitialised,              variables.breakInitialised,            true);
      combineBreakableSet(breakPossiblyInitialised,      variables.breakPossiblyInitialised,    false);
      combineInitialiserStates(breakInitialiserState,    variables.breakInitialiserState);
      combineBreakableSet(continueInitialised,           variables.continueInitialised,         true);
      combineBreakableSet(continuePossiblyInitialised,   variables.continuePossiblyInitialised, false);
      combineInitialiserStates(continueInitialiserState, variables.continueInitialiserState);
    }

    /**
     * Combines the specified sets of variables.
     * @param destination - the destination map to store the resulting sets of variables in
     * @param source - the source map to get new data for the destination from
     * @param intersection - true to intersect sets which exist in both the source and destination, false to take the union of any such sets
     */
    private static void combineBreakableSet(Map<BreakableStatement, Set<Variable>> destination, Map<BreakableStatement, Set<Variable>> source, boolean intersection)
    {
      for (Entry<BreakableStatement, Set<Variable>> entry : source.entrySet())
      {
        if (destination.containsKey(entry.getKey()))
        {
          Set<Variable> destinationSet = destination.get(entry.getKey());
          if (intersection)
          {
            intersect(destinationSet, entry.getValue());
          }
          else // union
          {
            destinationSet.addAll(entry.getValue());
          }
        }
        else
        {
          destination.put(entry.getKey(), new HashSet<Variable>(entry.getValue()));
        }
      }
    }

    /**
     * Combines the specified maps from BreakableStatement to InitialiserState. If only one map contains an entry, that entry is kept. If both maps contain an entry, they are combined using combineInitialiserState().
     * The results are stored in the destination map.
     * @param destination - the destination map
     * @param source - the source map, to combine into the destination map
     */
    private static void combineInitialiserStates(Map<BreakableStatement, InitialiserState> destination, Map<BreakableStatement, InitialiserState> source)
    {
      for (Entry<BreakableStatement, InitialiserState> entry : source.entrySet())
      {
        if (destination.containsKey(entry.getKey()))
        {
          destination.put(entry.getKey(), combineInitialiserState(entry.getValue(), destination.get(entry.getKey())));
        }
        else
        {
          destination.put(entry.getKey(), entry.getValue());
        }
      }
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

    /**
     * Combines two InitialiserStates into a single InitialiserState
     * @param firstState - the first state
     * @param secondState - the second state
     * @return the combined InitialiserState
     */
    private static InitialiserState combineInitialiserState(InitialiserState firstState, InitialiserState secondState)
    {
      if (firstState == InitialiserState.NOT_RUN & secondState == InitialiserState.NOT_RUN)
      {
        return InitialiserState.NOT_RUN;
      }
      if (firstState == InitialiserState.DEFINITELY_RUN & secondState == InitialiserState.DEFINITELY_RUN)
      {
        return InitialiserState.DEFINITELY_RUN;
      }
      return InitialiserState.POSSIBLY_RUN;
    }
  }

}
