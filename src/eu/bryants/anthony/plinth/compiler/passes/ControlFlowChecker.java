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
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
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
import eu.bryants.anthony.plinth.ast.expression.CreationExpression;
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
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.metadata.ConstructorReference;
import eu.bryants.anthony.plinth.ast.metadata.FieldInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.FieldReference;
import eu.bryants.anthony.plinth.ast.metadata.GlobalVariable;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;
import eu.bryants.anthony.plinth.ast.metadata.MemberVariable;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;
import eu.bryants.anthony.plinth.ast.metadata.PropertyInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.PropertyPseudoVariable;
import eu.bryants.anthony.plinth.ast.metadata.PropertyReference;
import eu.bryants.anthony.plinth.ast.metadata.Variable;
import eu.bryants.anthony.plinth.ast.misc.Argument;
import eu.bryants.anthony.plinth.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.plinth.ast.misc.Assignee;
import eu.bryants.anthony.plinth.ast.misc.AutoAssignParameter;
import eu.bryants.anthony.plinth.ast.misc.BlankAssignee;
import eu.bryants.anthony.plinth.ast.misc.CatchClause;
import eu.bryants.anthony.plinth.ast.misc.DefaultArgument;
import eu.bryants.anthony.plinth.ast.misc.DefaultParameter;
import eu.bryants.anthony.plinth.ast.misc.FieldAssignee;
import eu.bryants.anthony.plinth.ast.misc.NormalArgument;
import eu.bryants.anthony.plinth.ast.misc.NormalParameter;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.misc.VariableAssignee;
import eu.bryants.anthony.plinth.ast.statement.AssignStatement;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.statement.BreakStatement;
import eu.bryants.anthony.plinth.ast.statement.BreakableStatement;
import eu.bryants.anthony.plinth.ast.statement.ContinueStatement;
import eu.bryants.anthony.plinth.ast.statement.DelegateConstructorStatement;
import eu.bryants.anthony.plinth.ast.statement.ExpressionStatement;
import eu.bryants.anthony.plinth.ast.statement.ForEachStatement;
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
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.ast.type.WildcardType;
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
    // find all of the properties in this type, so that we can use its pseudo-variables instead of the ones from super-classes in superClassVariables
    Map<String, PropertyPseudoVariable> nonStaticPropertyPseudoVariables = new HashMap<String, PropertyPseudoVariable>();
    for (Property property : typeDefinition.getProperties())
    {
      if (!property.isStatic())
      {
        nonStaticPropertyPseudoVariables.put(property.getName(), property.getPseudoVariable());
      }
    }

    // build the set of variables from superclasses
    Set<Variable> superClassVariables = new HashSet<Variable>();
    for (NamedType superType : typeDefinition.getInheritanceLinearisation())
    {
      TypeDefinition superTypeDefinition = superType.getResolvedTypeDefinition();
      if (superTypeDefinition == typeDefinition)
      {
        continue;
      }
      if (superTypeDefinition instanceof ClassDefinition)
      {
        for (Field field : superTypeDefinition.getFields())
        {
          if (!field.isStatic())
          {
            superClassVariables.add(field.getMemberVariable());
          }
        }
        for (Property property : superTypeDefinition.getProperties())
        {
          // all class properties must be initialised by the super-class
          if (!property.isStatic())
          {
            // always use the variable from lowest-down the type hierarchy
            PropertyPseudoVariable pseudoVariable = nonStaticPropertyPseudoVariables.get(property.getName());
            if (pseudoVariable == null)
            {
              pseudoVariable = property.getPseudoVariable();
              nonStaticPropertyPseudoVariables.put(property.getName(), pseudoVariable);
            }
            superClassVariables.add(pseudoVariable);
          }
        }
      }
      else if (superTypeDefinition instanceof InterfaceDefinition)
      {
        for (Property property : superTypeDefinition.getProperties())
        {
          // interface properties are only already-initialised if they do not have a constructor (and therefore do not need to be initialised)
          if (!property.isStatic())
          {
            // always use the variable from lowest-down the type hierarchy
            PropertyPseudoVariable pseudoVariable = nonStaticPropertyPseudoVariables.get(property.getName());
            if (pseudoVariable == null)
            {
              pseudoVariable = property.getPseudoVariable();
              nonStaticPropertyPseudoVariables.put(property.getName(), pseudoVariable);
            }
            if (!property.hasConstructor())
            {
              superClassVariables.add(pseudoVariable);
            }
          }
        }
      }
      else
      {
        throw new IllegalArgumentException("Unknown superTypeDefinition: " + superTypeDefinition);
      }
    }

    // check the initialisers

    // note: while we check the initialiser, we tell the checker that the initialiser has been run
    // this is because this assumption allows the user to use 'this' in the initialiser once all other variables are initialised,
    // which is perfectly legal, since the initialiser must be run after any super-constructors
    // also, since initialisers cannot call delegate constructors, this cannot do any harm
    ControlFlowState instanceState = new ControlFlowState(InitialiserState.DEFINITELY_RUN);
    ControlFlowState staticState   = new ControlFlowState(InitialiserState.DEFINITELY_RUN);

    // the super-class's constructor has always been run by the time we get to the initialiser, so add the superClassVariables to the initialiser's variable set now
    for (Variable var : superClassVariables)
    {
      instanceState.variables.initialised.add(var);
      instanceState.variables.possiblyInitialised.add(var);
    }

    // non-final fields which have default values are treated as already-initialised
    for (Field field : typeDefinition.getFields())
    {
      if (!field.isStatic() && field.getType().hasDefaultValue() && !field.isFinal())
      {
        instanceState.variables.initialised.add(field.getMemberVariable());
        instanceState.variables.possiblyInitialised.add(field.getMemberVariable());
      }
    }
    for (Property property : typeDefinition.getProperties())
    {
      if (!property.hasConstructor())
      {
        if (property.isStatic())
        {
          staticState.variables.initialised.add(property.getPseudoVariable());
          staticState.variables.possiblyInitialised.add(property.getPseudoVariable());
        }
        else
        {
          instanceState.variables.initialised.add(property.getPseudoVariable());
          instanceState.variables.possiblyInitialised.add(property.getPseudoVariable());
        }
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
            GlobalVariable var = field.getGlobalVariable();
            checkControlFlow(field.getInitialiserExpression(), typeDefinition, staticState.variables.initialised, staticState.variables.initialiserState, false, false, true, false);
            if (field.isFinal() && staticState.variables.possiblyInitialised.contains(var))
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + field.getName() + "' may already have been initialised", initialiser.getLexicalPhrase()));
            }
            staticState.variables.initialised.add(var);
            staticState.variables.possiblyInitialised.add(var);
          }
          else
          {
            MemberVariable var = field.getMemberVariable();
            checkControlFlow(field.getInitialiserExpression(), typeDefinition, instanceState.variables.initialised, instanceState.variables.initialiserState, true, onlyHasSelfishConstructors, false, hasImmutableConstructors);
            if (field.isFinal() && instanceState.variables.possiblyInitialised.contains(var))
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final field '" + field.getName() + "' may already have been initialised", initialiser.getLexicalPhrase()));
            }
            instanceState.variables.initialised.add(var);
            instanceState.variables.possiblyInitialised.add(var);
          }
        }
        else if (initialiser instanceof PropertyInitialiser)
        {
          PropertyInitialiser propertyInitialiser = (PropertyInitialiser) initialiser;
          Property property = propertyInitialiser.getProperty();
          ControlFlowState initialiserState;
          if (property.isStatic())
          {
            checkControlFlow(property.getInitialiserExpression(), typeDefinition, staticState.variables.initialised, staticState.variables.initialiserState, false, false, true, false);
            initialiserState = staticState;
          }
          else
          {
            checkControlFlow(property.getInitialiserExpression(), typeDefinition, instanceState.variables.initialised, instanceState.variables.initialiserState, true, onlyHasSelfishConstructors, false, hasImmutableConstructors);
            initialiserState = instanceState;
          }
          PropertyPseudoVariable var = property.getPseudoVariable();
          if (initialiserState.variables.initialised.contains(var))
          {
            // this property has already been initialised, so this is a setter call
            if (var.isFinal())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final property '" + var.getName() + "' cannot be modified", property.getInitialiserExpression().getLexicalPhrase()));
            }
            else if (!property.isStatic() && hasImmutableConstructors && !property.isSetterImmutable())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the property '" + var.getName() + "' in an immutable context: its setter is not immutable", property.getInitialiserExpression().getLexicalPhrase()));
            }
            if (!property.isStatic())
            {
              // this is a setter call during a constructor, so make sure it is allowed (i.e. everything else is already initialised)
              try
              {
                checkThisAccess(initialiserState.variables.initialised, initialiserState.variables.initialiserState, typeDefinition, onlyHasSelfishConstructors, "call a property setter on", property.getInitialiserExpression().getLexicalPhrase());
              }
              catch (ConceptualException e)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
              }
            }
            // this property has already been initialised, so this is a setter call
            propertyInitialiser.setPropertyConstructorCall(false);
          }
          else if (!initialiserState.variables.possiblyInitialised.contains(var))
          {
            // this property has definitely not been initialised yet, so this is a constructor call
            propertyInitialiser.setPropertyConstructorCall(true);
            if (!property.isStatic() && hasImmutableConstructors && !property.isConstructorImmutable())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot initialise the property '" + var.getName() + "' in an immutable context: its constructor is not immutable", property.getInitialiserExpression().getLexicalPhrase()));
            }
            initialiserState.variables.initialised.add(var);
            initialiserState.variables.possiblyInitialised.add(var);
          }
          else
          {
            // this property may or may not have been initialised - this is an error, since we cannot decide between the constructor and the setter
            throw new ConceptualException("This property may or may not have already been initialised. Cannot call either its constructor or its setter.", initialiser.getLexicalPhrase());
          }
        }
        else
        {
          boolean stopsExecution;
          if (initialiser.isStatic())
          {
            stopsExecution = checkControlFlow(initialiser.getBlock(), typeDefinition, staticState, null, new LinkedList<Statement>(), false, false, true, false, true);
          }
          else
          {
            stopsExecution = checkControlFlow(initialiser.getBlock(), typeDefinition, instanceState, null, new LinkedList<Statement>(), true, onlyHasSelfishConstructors, false, hasImmutableConstructors, true);
          }
          if (stopsExecution)
          {
            Statement[] statements = initialiser.getBlock().getStatements();
            throw new ConceptualException("An initialiser can not terminate execution", statements.length == 0 ? initialiser.getLexicalPhrase() : statements[statements.length - 1].getLexicalPhrase());
          }
        }
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
    }

    // final fields must be initialised exactly once, so make sure any static fields have been initialised
    for (Field field : typeDefinition.getFields())
    {
      if (field.isStatic() && field.isFinal() && !staticState.variables.initialised.contains(field.getGlobalVariable()))
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + field.getName() + "' is not always initialised", field.getLexicalPhrase()));
      }
    }
    // all properties with constructors must be initialised at least once, so make sure any static properties have been initialised
    for (Property property : typeDefinition.getProperties())
    {
      if (property.isStatic() && property.hasConstructor() && !staticState.variables.initialised.contains(property.getPseudoVariable()))
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static property '" + property.getName() + "' is not always initialised", property.getLexicalPhrase()));
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
        checkControlFlow(constructor, delegateConstructorVariables, nonStaticPropertyPseudoVariables);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
    }
    for (Property property : typeDefinition.getProperties())
    {
      try
      {
        checkControlFlow(property, superClassVariables);
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
  private static void checkControlFlow(Constructor constructor, DelegateConstructorVariables delegateConstructorVariables, Map<String, PropertyPseudoVariable> nonStaticPropertyPseudoVariables) throws ConceptualException
  {
    boolean initialiserAlreadyRun = !constructor.getCallsDelegateConstructor();
    ControlFlowState state = new ControlFlowState(initialiserAlreadyRun ? InitialiserState.DEFINITELY_RUN : InitialiserState.NOT_RUN);
    if (initialiserAlreadyRun)
    {
      // this should behave exactly as if we are running the no-args super() constructor
      // since the initialiser variable sets already contain all superclass member variables, just copy them to this Constructor's ControlFlowState
      state.variables.initialised = new HashSet<Variable>(delegateConstructorVariables.initialiserDefinitelyInitialised);
      state.variables.possiblyInitialised = new HashSet<Variable>(delegateConstructorVariables.initialiserPossiblyInitialised);
    }
    CoalescedConceptualException coalescedException = null;
    for (Parameter parameter : constructor.getParameters())
    {
      if (parameter instanceof NormalParameter)
      {
        NormalParameter normalParameter = (NormalParameter) parameter;
        state.variables.initialised.add(normalParameter.getVariable());
        state.variables.possiblyInitialised.add(normalParameter.getVariable());
      }
      else if (parameter instanceof AutoAssignParameter)
      {
        // this parameter is actually an assignment to a field or a property, so make sure we get all of the assignment constraints right
        AutoAssignParameter autoAssignParameter = (AutoAssignParameter) parameter;
        Variable var = autoAssignParameter.getResolvedVariable();
        if (var instanceof MemberVariable)
        {
          if (var.isFinal() && state.variables.possiblyInitialised.contains(var))
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final field '" + var.getName() + "' may already have been initialised", autoAssignParameter.getLexicalPhrase()));
          }
          state.variables.initialised.add(var);
          state.variables.possiblyInitialised.add(var);
        }
        else if (var instanceof GlobalVariable)
        {
          if (var.isFinal())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' cannot be modified", autoAssignParameter.getLexicalPhrase()));
          }
          boolean isMutable = (((GlobalVariable) var).getField() != null    && ((GlobalVariable) var).getField().isMutable()) ||
                              (((GlobalVariable) var).getProperty() != null && ((GlobalVariable) var).getProperty().isMutable());
          if (constructor.isImmutable() && !isMutable)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the static variable '" + var.getName() + "' in an immutable context", autoAssignParameter.getLexicalPhrase()));
          }
        }
        else if (var instanceof PropertyPseudoVariable)
        {
          Property property = ((PropertyPseudoVariable) var).getProperty();
          if (state.variables.initialised.contains(var))
          {
            // the variable has definitely been initialised, so this is a setter call
            if (var.isFinal())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final property '" + var.getName() + "' cannot be modified", autoAssignParameter.getLexicalPhrase()));
            }
            else if (constructor.isImmutable() && !property.isSetterImmutable())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the property '" + var.getName() + "' in an immutable context: its setter is not immutable", autoAssignParameter.getLexicalPhrase()));
            }
            autoAssignParameter.setPropertyConstructorCall(false);

            if (!property.isStatic())
            {
              // this is a setter call during a constructor, so make sure it is allowed (i.e. everything else is already initialised)
              try
              {
                checkThisAccess(state.variables.initialised, state.variables.initialiserState, constructor.getContainingTypeDefinition(), constructor.isSelfish(), "call a property setter on", autoAssignParameter.getLexicalPhrase());
              }
              catch (ConceptualException e)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
              }
            }
          }
          else if (!state.variables.possiblyInitialised.contains(var))
          {
            // this property has definitely not already been initialised, so this is a constructor call
            autoAssignParameter.setPropertyConstructorCall(true);
            if (constructor.isImmutable() && !property.isConstructorImmutable())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot initialise the property '" + var.getName() + "' in an immutable context: its constructor is not immutable", autoAssignParameter.getLexicalPhrase()));
            }
            state.variables.initialised.add(var);
            state.variables.possiblyInitialised.add(var);
          }
          else
          {
            // this property may or may not have been initialised - this is an error, since we cannot decide between the constructor and the setter
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("This property may or may not have already been initialised. Cannot call either its constructor or its setter.", autoAssignParameter.getLexicalPhrase()));
          }
        }
        else
        {
          throw new IllegalArgumentException("An auto-assign parameter should only be allowed to assign to a field or a property! the variable was: " + var);
        }
      }
      else if (parameter instanceof DefaultParameter)
      {
        DefaultParameter defaultParameter = (DefaultParameter) parameter;
        try
        {
          checkControlFlow(defaultParameter.getExpression(), constructor.getContainingTypeDefinition(), state.variables.initialised, state.variables.initialiserState, true, constructor.isSelfish(), false, constructor.isImmutable());
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
        state.variables.initialised.add(defaultParameter.getVariable());
        state.variables.possiblyInitialised.add(defaultParameter.getVariable());
      }
      else
      {
        throw new IllegalArgumentException("Unknown type of Parameter: " + parameter);
      }
    }
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
    for (Field field : constructor.getContainingTypeDefinition().getFields())
    {
      if (!field.isStatic() && !state.variables.initialised.contains(field.getMemberVariable()))
      {
        if (!field.getType().hasDefaultValue())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Constructor does not always initialise the non-static field '" + field.getName() + "', which does not have a default value", constructor.getLexicalPhrase()));
        }
        else if (field.isFinal())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Constructor does not always initialise the non-static final field '" + field.getName() + "'", constructor.getLexicalPhrase()));
        }
      }
    }
    for (Property property : constructor.getContainingTypeDefinition().getProperties())
    {
      if (!property.isStatic() && property.hasConstructor() && !state.variables.initialised.contains(nonStaticPropertyPseudoVariables.get(property.getName())))
      {
        // non-static properties must always be initialised by a constructor
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Constructor does not always initialise the non-static property '" + property.getName() + "'", constructor.getLexicalPhrase()));
      }
    }
    for (NamedType superType : constructor.getContainingTypeDefinition().getInheritanceLinearisation())
    {
      TypeDefinition superTypeDefinition = superType.getResolvedTypeDefinition();
      if (superTypeDefinition instanceof InterfaceDefinition)
      {
        for (Property property : superTypeDefinition.getProperties())
        {
          if (!property.isStatic() && property.hasConstructor() && !state.variables.initialised.contains(nonStaticPropertyPseudoVariables.get(property.getName())))
          {
            // inherited interface properties which have constructors must always be initialised by a constructor
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Constructor does not always initialise the non-static property '" + property.getName() + "' (inherited from the interface: " + superTypeDefinition.getQualifiedName() + ")", constructor.getLexicalPhrase()));
          }
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
    CoalescedConceptualException coalescedException = null;
    ControlFlowState state = new ControlFlowState(InitialiserState.DEFINITELY_RUN);
    for (Parameter parameter : method.getParameters())
    {
      if (parameter instanceof NormalParameter)
      {
        NormalParameter normalParameter = (NormalParameter) parameter;
        state.variables.initialised.add(normalParameter.getVariable());
        state.variables.possiblyInitialised.add(normalParameter.getVariable());
      }
      else if (parameter instanceof AutoAssignParameter)
      {
        AutoAssignParameter autoAssignParameter = (AutoAssignParameter) parameter;
        Variable var = autoAssignParameter.getResolvedVariable();
        if (var instanceof MemberVariable)
        {
          if (method.isStatic())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the instance variable '" + var.getName() + "' in a static context", autoAssignParameter.getLexicalPhrase()));
          }
          else
          {
            if (var.isFinal())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final field '" + var.getName() + "' cannot be modified", autoAssignParameter.getLexicalPhrase()));
            }
            // if it is a mutable field or a mutable property backing variable, allow assignments even in an immutable context
            boolean isMutable = (((MemberVariable) var).getField() != null    && ((MemberVariable) var).getField().isMutable()) ||
                                (((MemberVariable) var).getProperty() != null && ((MemberVariable) var).getProperty().isMutable());
            if (method.isImmutable() && !isMutable)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the instance variable '" + var.getName() + "' in an immutable context", autoAssignParameter.getLexicalPhrase()));
            }
          }
        }
        else if (var instanceof GlobalVariable)
        {
          if (var.isFinal())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' cannot be modified", autoAssignParameter.getLexicalPhrase()));
          }
          // if it is a mutable field or a mutable property backing variable, allow assignments even in an immutable context
          boolean isMutable = (((GlobalVariable) var).getField() != null    && ((GlobalVariable) var).getField().isMutable()) ||
                              (((GlobalVariable) var).getProperty() != null && ((GlobalVariable) var).getProperty().isMutable());
          if (method.isImmutable() && !isMutable)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the static variable '" + var.getName() + "' in an immutable context", autoAssignParameter.getLexicalPhrase()));
          }
        }
        else if (var instanceof PropertyPseudoVariable)
        {
          Property property = ((PropertyPseudoVariable) var).getProperty();
          if (method.isStatic() && !property.isStatic())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the instance property '" + var.getName() + "' in a static context", autoAssignParameter.getLexicalPhrase()));
          }
          else
          {
            // this cannot be a property constructor call, as it is inside a method, so it must be a setter
            if (var.isFinal())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final property '" + var.getName() + "' cannot be modified", autoAssignParameter.getLexicalPhrase()));
            }
            else if (method.isImmutable() && !property.isSetterImmutable())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the property '" + var.getName() + "' in an immutable context: its setter is not immutable", autoAssignParameter.getLexicalPhrase()));
            }
            autoAssignParameter.setPropertyConstructorCall(false);
          }
        }
        else
        {
          throw new IllegalArgumentException("An auto-assign parameter should only be allowed to assign to a field or a property! the variable was: " + var);
        }
      }
      else if (parameter instanceof DefaultParameter)
      {
        DefaultParameter defaultParameter = (DefaultParameter) parameter;
        try
        {
          checkControlFlow(defaultParameter.getExpression(), method.getContainingTypeDefinition(), state.variables.initialised, state.variables.initialiserState, false, false, method.isStatic(), method.isImmutable());
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
        state.variables.initialised.add(defaultParameter.getVariable());
        state.variables.possiblyInitialised.add(defaultParameter.getVariable());
      }
      else
      {
        throw new IllegalArgumentException("Unknown type of Parameter: " + parameter);
      }
    }
    try
    {
      boolean returned = checkControlFlow(method.getBlock(), method.getContainingTypeDefinition(), state, null, new LinkedList<Statement>(), false, false, method.isStatic(), method.isImmutable(), false);
      if (!returned && !(method.getReturnType() instanceof VoidType))
      {
        throw new ConceptualException("Method does not always return a value", method.getLexicalPhrase());
      }
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

  private static void checkControlFlow(Property property, Set<Variable> superClassVariables) throws ConceptualException
  {
    CoalescedConceptualException coalescedException = null;
    if (property.getGetterBlock() == null)
    {
      // check that the default getter is possible
      if (property.isUnbacked() && !property.isAbstract())
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("An unbacked property must define a custom getter", property.getLexicalPhrase()));
      }
    }
    else
    {
      try
      {
        ControlFlowState state = new ControlFlowState(InitialiserState.DEFINITELY_RUN);
        boolean returned = checkControlFlow(property.getGetterBlock(), property.getContainingTypeDefinition(), state, null, new LinkedList<Statement>(), false, false, property.isStatic(), property.isGetterImmutable(), false);
        if (!returned)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("A getter must always return a value", property.getLexicalPhrase()));
        }
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
    }

    if (property.getDeclaresSetter() && property.isFinal())
    {
      coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("A final property cannot have a setter, only a constructor", property.getLexicalPhrase()));
    }
    boolean shouldHaveSetter = !property.isFinal();
    if (shouldHaveSetter)
    {
      if (property.getSetterBlock() == null)
      {
        // check that the default setter is possible here
        if (property.isUnbacked() && !property.isAbstract())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("An unbacked property must define a custom setter", property.getLexicalPhrase()));
        }
        if (property.isSetterImmutable() && !property.isUnbacked() && !property.isMutable())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Making the default setter immutable can only work if the backing variable is mutable", property.getLexicalPhrase()));
        }
      }
      else
      {
        try
        {
          ControlFlowState state = new ControlFlowState(InitialiserState.DEFINITELY_RUN);
          Parameter setterParameter = property.getSetterParameter();
          if (setterParameter instanceof NormalParameter)
          {
            NormalParameter normalParameter = (NormalParameter) setterParameter;
            state.variables.initialised.add(normalParameter.getVariable());
            state.variables.possiblyInitialised.add(normalParameter.getVariable());
          }
          else if (setterParameter instanceof AutoAssignParameter)
          {
            AutoAssignParameter autoAssignParameter = (AutoAssignParameter) setterParameter;
            Variable var = autoAssignParameter.getResolvedVariable();
            if (var instanceof MemberVariable)
            {
              if (property.isStatic())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the instance variable '" + var.getName() + "' in a static context", autoAssignParameter.getLexicalPhrase()));
              }
              else
              {
                if (var.isFinal())
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final field '" + var.getName() + "' cannot be modified", autoAssignParameter.getLexicalPhrase()));
                }
                // if it is a mutable field or a mutable property backing variable, allow assignments even in an immutable context
                boolean isMutable = (((MemberVariable) var).getField() != null    && ((MemberVariable) var).getField().isMutable()) ||
                                    (((MemberVariable) var).getProperty() != null && ((MemberVariable) var).getProperty().isMutable());
                if (property.isSetterImmutable() && !isMutable)
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the instance variable '" + var.getName() + "' in an immutable context", autoAssignParameter.getLexicalPhrase()));
                }
              }
            }
            else if (var instanceof GlobalVariable)
            {
              if (var.isFinal())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' cannot be modified", autoAssignParameter.getLexicalPhrase()));
              }
              // if it is a mutable field or a mutable property backing variable, allow assignments even in an immutable context
              boolean isMutable = (((GlobalVariable) var).getField() != null    && ((GlobalVariable) var).getField().isMutable()) ||
                                  (((GlobalVariable) var).getProperty() != null && ((GlobalVariable) var).getProperty().isMutable());
              if (property.isSetterImmutable() && !isMutable)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the static variable '" + var.getName() + "' in an immutable context", autoAssignParameter.getLexicalPhrase()));
              }
            }
            else if (var instanceof PropertyPseudoVariable)
            {
              Property varProperty = ((PropertyPseudoVariable) var).getProperty();
              if (property.isStatic() && !varProperty.isStatic())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the instance property '" + var.getName() + "' in a static context", autoAssignParameter.getLexicalPhrase()));
              }
              else
              {
                if (var.isFinal())
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final property '" + var.getName() + "' cannot be modified", autoAssignParameter.getLexicalPhrase()));
                }
                else if (property.isSetterImmutable() && !varProperty.isSetterImmutable())
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the property '" + var.getName() + "' in an immutable context: its setter is not immutable", autoAssignParameter.getLexicalPhrase()));
                }
                autoAssignParameter.setPropertyConstructorCall(false);
              }
            }
            else
            {
              throw new IllegalArgumentException("An auto-assign parameter should only be allowed to assign to a field or a property! the variable was: " + var);
            }
          }
          else if (setterParameter instanceof DefaultParameter)
          {
            throw new IllegalArgumentException("A property setter cannot take a default parameter");
          }
          else
          {
            throw new IllegalArgumentException("Unknown type of Parameter: " + setterParameter);
          }
          checkControlFlow(property.getSetterBlock(), property.getContainingTypeDefinition(), state, null, new LinkedList<Statement>(), false, false, property.isStatic(), property.isSetterImmutable(), false);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
    }

    if (property.getDeclaresConstructor())
    {
      if (property.isStatic() && !property.isFinal())
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("A static, non-final property cannot have a constructor, only a setter", property.getLexicalPhrase()));
      }
    }
    if (property.hasConstructor())
    {
      boolean setterIsConstructor = false;
      Parameter constructorParameter = property.getConstructorParameter();
      Block constructorBlock = property.getConstructorBlock();
      if (constructorBlock == null)
      {
        // check that the default constructor is possible here
        if (property.isFinal())
        {
          // there should be no setter, so we cannot default to the setter being the constructor, so check the default constructor is possible
          if (property.isUnbacked() && !property.isAbstract())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("An unbacked final property must define a custom constructor", property.getLexicalPhrase()));
          }
        }
        else
        {
          // this is a non-static, non-final property which declares a constructor without giving it an implementation,
          // so the constructor defaults to being the setter, so check the setter as a constructor
          setterIsConstructor = true;
          constructorParameter = property.getSetterParameter();
          constructorBlock = property.getSetterBlock();

          // we've already checked the setter once, so before we check it again as a constructor, check whether the first pass produced any errors
          // only if there were no errors the first time should we check it again as a constructor
          if (coalescedException != null)
          {
            throw coalescedException;
          }
        }
      }

      if (constructorBlock != null)
      {
        try
        {
          ControlFlowState state;
          if (property.isStatic())
          {
            // pretend we are just in a static method
            state = new ControlFlowState(InitialiserState.DEFINITELY_RUN);
            for (Field field : property.getContainingTypeDefinition().getFields())
            {
              if (field.isStatic())
              {
                state.variables.possiblyInitialised.add(field.getGlobalVariable());
              }
            }
            for (Property p : property.getContainingTypeDefinition().getProperties())
            {
              if (p.isStatic())
              {
                state.variables.possiblyInitialised.add(p.getPseudoVariable());
              }
            }
          }
          else
          {
            // we are inside a constructor, but we do not know whether or not the initialiser has been run or whether any of the other variables (including super-class variables) have been initialised
            state = new ControlFlowState(InitialiserState.POSSIBLY_RUN);
            for (Field field : property.getContainingTypeDefinition().getFields())
            {
              if (!field.isStatic())
              {
                state.variables.possiblyInitialised.add(field.getMemberVariable());
              }
            }
            for (Property p : property.getContainingTypeDefinition().getProperties())
            {
              if (!p.isStatic())
              {
                state.variables.possiblyInitialised.add(p.getPseudoVariable());
              }
            }
            for (Variable var : superClassVariables)
            {
              state.variables.possiblyInitialised.add(var);
            }
          }

          if (constructorParameter instanceof NormalParameter)
          {
            NormalParameter normalParameter = (NormalParameter) constructorParameter;
            state.variables.initialised.add(normalParameter.getVariable());
            state.variables.possiblyInitialised.add(normalParameter.getVariable());
          }
          else if (constructorParameter instanceof AutoAssignParameter)
          {
            AutoAssignParameter autoAssignParameter = (AutoAssignParameter) constructorParameter;
            Variable var = autoAssignParameter.getResolvedVariable();
            if (var instanceof MemberVariable)
            {
              if (property.isStatic())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the instance variable '" + var.getName() + "' in a static context", autoAssignParameter.getLexicalPhrase()));
              }
              else
              {
                if (var.isFinal())
                {
                  if (state.variables.possiblyInitialised.contains(var))
                  {
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final field '" + var.getName() + "' may already have been initialised", autoAssignParameter.getLexicalPhrase()));
                  }
                }
                state.variables.initialised.add(var);
                state.variables.possiblyInitialised.add(var);
              }
            }
            else if (var instanceof GlobalVariable)
            {
              // assigning to a global variable is only ever an initialisation if the variable is final, otherwise something else could have assigned to it first
              boolean isInitialisation = var.isFinal() && property.isStatic() && ((GlobalVariable) var).getEnclosingTypeDefinition() == property.getContainingTypeDefinition();
              if (isInitialisation)
              {
                if (state.variables.possiblyInitialised.contains(var))
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' may already have been initialised", autoAssignParameter.getLexicalPhrase()));
                }
                state.variables.initialised.add(var);
                state.variables.possiblyInitialised.add(var);
              }
              else if (var.isFinal())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' cannot be modified", autoAssignParameter.getLexicalPhrase()));
              }
              if (!isInitialisation && property.isStatic() && ((GlobalVariable) var).getProperty() != null)
              {
                // this is a property backing variable, and we are inside a property constructor, so initialise this backing variable
                state.variables.initialised.add(var);
                state.variables.possiblyInitialised.add(var);
              }
              // if it is a mutable field or a mutable property backing variable, allow assignments even in an immutable context
              boolean isMutable = (((GlobalVariable) var).getField() != null    && ((GlobalVariable) var).getField().isMutable()) ||
                                  (((GlobalVariable) var).getProperty() != null && ((GlobalVariable) var).getProperty().isMutable());
              if (property.isConstructorImmutable() && !isMutable)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the static variable '" + var.getName() + "' in an immutable context", autoAssignParameter.getLexicalPhrase()));
              }
            }
            else if (var instanceof PropertyPseudoVariable)
            {
              Property varProperty = ((PropertyPseudoVariable) var).getProperty();
              if (property.isStatic() && !varProperty.isStatic())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the instance property '" + var.getName() + "' in a static context", autoAssignParameter.getLexicalPhrase()));
              }
              else
              {
                boolean isInitialisation = varProperty.isStatic() ?
                                           (varProperty.isFinal() && property.isStatic() && varProperty.getContainingTypeDefinition() == property.getContainingTypeDefinition()) :
                                           true;
                if (!isInitialisation || state.variables.initialised.contains(var))
                {
                  // either we are not in any sort of initialiser, or this property has already been initialised
                  // so this is a setter call
                  if (var.isFinal())
                  {
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final property '" + var.getName() + "' cannot be modified", autoAssignParameter.getLexicalPhrase()));
                  }
                  else if (property.isConstructorImmutable() && !varProperty.isSetterImmutable())
                  {
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the property '" + var.getName() + "' in an immutable context: its setter is not immutable", autoAssignParameter.getLexicalPhrase()));
                  }
                  autoAssignParameter.setPropertyConstructorCall(false);
                }
                if (isInitialisation)
                {
                  if (state.variables.initialised.contains(var))
                  {
                    if (!varProperty.isStatic())
                    {
                      // this is a setter call during a constructor, so make sure it is allowed (i.e. everything else is already initialised)
                      try
                      {
                        checkThisAccess(state.variables.initialised, state.variables.initialiserState, property.getContainingTypeDefinition(), false, "call a property setter on", autoAssignParameter.getLexicalPhrase());
                      }
                      catch (ConceptualException e)
                      {
                        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
                      }
                    }
                  }
                  else if (!state.variables.possiblyInitialised.contains(var))
                  {
                    // this property has definitely not already been initialised, so this is a constructor call
                    autoAssignParameter.setPropertyConstructorCall(true);
                    if (property.isConstructorImmutable() && !varProperty.isConstructorImmutable())
                    {
                      coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot initialise the property '" + var.getName() + "' in an immutable context: its constructor is not immutable", autoAssignParameter.getLexicalPhrase()));
                    }
                    state.variables.initialised.add(var);
                    state.variables.possiblyInitialised.add(var);
                  }
                  else
                  {
                    // this property may or may not have been initialised - this is an error, since we cannot decide between the constructor and the setter
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("This property may or may not have already been initialised. Cannot call either its constructor or its setter.", autoAssignParameter.getLexicalPhrase()));
                  }
                }
              }
            }
            else
            {
              throw new IllegalArgumentException("An auto-assign parameter should only be allowed to assign to a field or a property! the variable was: " + var);
            }
          }
          else if (constructorParameter instanceof DefaultParameter)
          {
            throw new IllegalArgumentException("A property constructor cannot take a default parameter");
          }
          else
          {
            throw new IllegalArgumentException("Unknown type of Parameter: " + constructorParameter);
          }


          checkControlFlow(constructorBlock, property.getContainingTypeDefinition(), state, null, new LinkedList<Statement>(), true, false, property.isStatic(), property.isConstructorImmutable(), false);

          if (!property.isUnbacked() && !property.getType().hasDefaultValue())
          {
            // make sure the backing variable was initialised
            Variable backingVariable = property.isStatic() ? property.getBackingGlobalVariable() : property.getBackingMemberVariable();
            if (!state.variables.initialised.contains(backingVariable))
            {
              throw new ConceptualException("A property's constructor" + (setterIsConstructor ? " (in this case its setter)" : "") + " must always initialise the backing variable", property.getLexicalPhrase());
            }
          }
        }
        catch (ConceptualException e)
        {
          if (setterIsConstructor)
          {
            // modify the exception before coalescing it, to point out why we encountered this error in the constructor but not the setter
            e = new ConceptualException("Encountered an error while checking a property's constructor (which in this case defaults to its setter)...", property.getLexicalPhrase(), e);
          }
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
    }

    if (coalescedException != null)
    {
      throw coalescedException;
    }
  }

  /**
   * Checks that the control flow of the specified statement is well defined.
   * @param statement - the statement to check
   * @param enclosingTypeDefinition - the TypeDefinition that the specified Statement is enclosed inside
   * @param state - the state of the variables before this statement, to be updated to the after-statement state
   * @param delegateConstructorVariables - the sets of variables which indicate which variables the initialiser initialises
   * @param enclosingBreakableStack - the stack of statements that can be broken out of that enclose this statement (includes the TryStatements with finally blocks along the way)
   * @param inConstructor - true if the statement is part of a constructor - this should be true for constructors, non-static initialisers, and property constructors
   * @param inSelfishContext - true if the statement is part of a selfish constructor - this is possible for constructors and non-static initialisers
   * @param inStaticContext - true if the statement is in a static context - this should be true in static initialisers, static property methods, and static methods
   * @param inImmutableContext - true if the statement is in an immutable context
   * @param inInitialiser - true if the statement is in an initialiser - this should only be true in initialisers, both static and non-static
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
              if (inStaticContext)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the instance variable '" + var.getName() + "' in a static context", assignees[i].getLexicalPhrase()));
              }
              else
              {
                if (var.isFinal())
                {
                  if (inConstructor)
                  {
                    if (state.variables.possiblyInitialised.contains(var))
                    {
                      coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final field '" + var.getName() + "' may already have been initialised", assignees[i].getLexicalPhrase()));
                    }
                  }
                  else
                  {
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final field '" + var.getName() + "' cannot be modified", assignees[i].getLexicalPhrase()));
                  }
                }
                // if it is a mutable field or a mutable property backing variable, allow assignments even in an immutable context
                boolean isMutable = (((MemberVariable) var).getField() != null    && ((MemberVariable) var).getField().isMutable()) ||
                                    (((MemberVariable) var).getProperty() != null && ((MemberVariable) var).getProperty().isMutable());
                if (inImmutableContext && !inConstructor && !isMutable)
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the instance variable '" + var.getName() + "' in an immutable context", assignees[i].getLexicalPhrase()));
                }
                if (inConstructor)
                {
                  nowInitialisedVariables.add(var);
                }
              }
            }
            else if (var instanceof GlobalVariable)
            {
              // assigning to a global variable is only ever an initialisation if the variable is final, otherwise something else could have assigned to it first
              // static variables can be initialised in either static initialisers or static property-constructors
              boolean isInitialisation = var.isFinal() && inStaticContext && (inInitialiser || inConstructor) && ((GlobalVariable) var).getEnclosingTypeDefinition() == enclosingTypeDefinition;
              if (isInitialisation)
              {
                if (state.variables.possiblyInitialised.contains(var))
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' may already have been initialised", assignees[i].getLexicalPhrase()));
                }
                nowInitialisedVariables.add(var);
              }
              else if (var.isFinal())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' cannot be modified", assignees[i].getLexicalPhrase()));
              }
              if (!isInitialisation && inStaticContext && inConstructor && ((GlobalVariable) var).getProperty() != null)
              {
                // this is a property backing variable, and we are inside a property constructor, so initialise this backing variable
                nowInitialisedVariables.add(var);
              }
              // if it is a mutable field or a mutable property backing variable, allow assignments even in an immutable context
              boolean isMutable = (((GlobalVariable) var).getField() != null    && ((GlobalVariable) var).getField().isMutable()) ||
                                  (((GlobalVariable) var).getProperty() != null && ((GlobalVariable) var).getProperty().isMutable());
              if (inImmutableContext && !isMutable)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the static variable '" + var.getName() + "' in an immutable context", assignees[i].getLexicalPhrase()));
              }
            }
            else if (var instanceof PropertyPseudoVariable)
            {
              Property property = ((PropertyPseudoVariable) var).getProperty();
              if (inStaticContext && !property.isStatic())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the instance property '" + var.getName() + "' in a static context", assignees[i].getLexicalPhrase()));
              }
              else
              {
                // static properties can be initialised in either static initialisers or static property-constructors
                boolean isInitialisation = property.isStatic() ?
                                           (property.isFinal() && inStaticContext && (inInitialiser || inConstructor) && property.getContainingTypeDefinition() == enclosingTypeDefinition) :
                                           inConstructor;
                if (!isInitialisation || state.variables.initialised.contains(var))
                {
                  // either we are not in any sort of initialiser, or this property has already been initialised
                  // so this is a setter call
                  if (var.isFinal())
                  {
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final property '" + var.getName() + "' cannot be modified", assignees[i].getLexicalPhrase()));
                  }
                  else if (inImmutableContext && !property.isSetterImmutable())
                  {
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the property '" + var.getName() + "' in an immutable context: its setter is not immutable", assignees[i].getLexicalPhrase()));
                  }
                  ((VariableAssignee) assignees[i]).setPropertyConstructorCall(false);
                }
                if (isInitialisation)
                {
                  if (state.variables.initialised.contains(var))
                  {
                    if (!property.isStatic())
                    {
                      // this is a setter call during a constructor, so make sure it is allowed (i.e. everything else is already initialised)
                      try
                      {
                        checkThisAccess(state.variables.initialised, state.variables.initialiserState, enclosingTypeDefinition, inSelfishContext, "call a property setter on", assignees[i].getLexicalPhrase());
                      }
                      catch (ConceptualException e)
                      {
                        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
                      }
                    }
                  }
                  else if (!state.variables.possiblyInitialised.contains(var))
                  {
                    // this property has definitely not already been initialised, so this is a constructor call
                    ((VariableAssignee) assignees[i]).setPropertyConstructorCall(true);
                    if (inImmutableContext && !property.isConstructorImmutable())
                    {
                      coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot initialise the property '" + var.getName() + "' in an immutable context: its constructor is not immutable", assignees[i].getLexicalPhrase()));
                    }
                    nowInitialisedVariables.add(var);
                  }
                  else
                  {
                    // this property may or may not have been initialised - this is an error, since we cannot decide between the constructor and the setter
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("This property may or may not have already been initialised. Cannot call either its constructor or its setter.", assignees[i].getLexicalPhrase()));
                  }
                }
              }
            }
            else // parameters and local variables
            {
              if (var.isFinal() && state.variables.possiblyInitialised.contains(var))
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Variable '" + var.getName() + "' may already have been initialised.", assignees[i].getLexicalPhrase()));
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
            checkControlFlow(arrayElementAssignee.getArrayExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
          try
          {
            checkControlFlow(arrayElementAssignee.getDimensionExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
          MemberReference<?> resolvedMemberReference = fieldAccessExpression.getResolvedMemberReference();

          // check whether this assignee is for an instance field/property on 'this'
          boolean isOnThis = false;
          if (inConstructor && !inStaticContext && fieldAccessExpression.getBaseExpression() != null)
          {
            Expression expression = fieldAccessExpression.getBaseExpression();
            while (expression instanceof BracketedExpression)
            {
              expression = ((BracketedExpression) expression).getExpression();
            }
            isOnThis = expression instanceof ThisExpression;
          }

          // if we're in a constructor, only check the sub-expression for uninitialised variables if it doesn't just access 'this'
          // this allows the programmer to access fields before 'this' is fully initialised
          if (!isOnThis && fieldAccessExpression.getBaseExpression() != null)
          {
            try
            {
              checkControlFlow(fieldAccessExpression.getBaseExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
            }
            catch (ConceptualException e)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
            }
          }

          if (resolvedMemberReference instanceof FieldReference)
          {
            FieldReference fieldReference = (FieldReference) resolvedMemberReference;
            Field field = fieldReference.getReferencedMember();
            Variable var = field.isStatic() ? field.getGlobalVariable() : field.getMemberVariable();
            boolean isInitialisation = field.isStatic() ?
                                       (inStaticContext && (inInitialiser || inConstructor) && field.getGlobalVariable().getEnclosingTypeDefinition() == enclosingTypeDefinition) :
                                       isOnThis;
            if (var.isFinal())
            {
              if (isInitialisation)
              {
                if (state.variables.possiblyInitialised.contains(var))
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The final field '" + var.getName() + "' may already have been initialised", assignees[i].getLexicalPhrase()));
                }
              }
              else // if not in this variable's initialiser
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The final field '" + var.getName() + "' cannot be modified", assignees[i].getLexicalPhrase()));
              }
            }
            if (isInitialisation)
            {
              nowInitialisedVariables.add(var);
            }
            if (field.isStatic())
            {
              if (inImmutableContext && !isInitialisation && !field.isMutable())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the field '" + var.getName() + "' in an immutable context", assignees[i].getLexicalPhrase()));
              }
            }
            else
            {
              boolean isBaseImmutable = Type.isContextuallyDataImmutable(fieldAccessExpression.getBaseExpression().getType());
              if (isBaseImmutable && !isInitialisation && !field.isMutable())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the field '" + var.getName() + "' through an immutable object", assignees[i].getLexicalPhrase()));
              }
            }
          }
          else if (resolvedMemberReference instanceof PropertyReference)
          {
            PropertyReference propertyReference = (PropertyReference) resolvedMemberReference;
            Property property = propertyReference.getReferencedMember();
            PropertyPseudoVariable var = property.getPseudoVariable();
            boolean isInitialisation = property.isStatic() ?
                                       (property.isFinal() && inStaticContext && (inInitialiser || inConstructor) && property.getContainingTypeDefinition() == enclosingTypeDefinition) :
                                       isOnThis;
            if (!isInitialisation || state.variables.initialised.contains(var))
            {
              // either we are not an initialiser for this property, or it has already been initialised
              // so this is a setter call
              if (var.isFinal())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final property '" + var.getName() + "' cannot be modified", assignees[i].getLexicalPhrase()));
              }
              else if (inImmutableContext && !property.isSetterImmutable())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the property '" + var.getName() + "' in an immutable context: its setter is not immutable", assignees[i].getLexicalPhrase()));
              }
              else if (!property.isStatic())
              {
                boolean isBaseImmutable = Type.isContextuallyDataImmutable(fieldAccessExpression.getBaseExpression().getType());
                if (isBaseImmutable && !isInitialisation && !property.isSetterImmutable())
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the property '" + var.getName() + "' through an immutable object: its setter is not immutable", assignees[i].getLexicalPhrase()));
                }
              }
              fieldAssignee.setPropertyConstructorCall(false);
            }
            if (isInitialisation)
            {
              if (state.variables.initialised.contains(var))
              {
                if (!property.isStatic())
                {
                  // this is a setter call during a constructor, so make sure it is allowed (i.e. everything else is already initialised)
                  try
                  {
                    checkThisAccess(state.variables.initialised, state.variables.initialiserState, enclosingTypeDefinition, inSelfishContext, "call a property setter on", assignees[i].getLexicalPhrase());
                  }
                  catch (ConceptualException e)
                  {
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
                  }
                }
              }
              else if (!state.variables.possiblyInitialised.contains(var))
              {
                // this property has definitely not already been initialised, so this is a constructor call
                fieldAssignee.setPropertyConstructorCall(true);
                if (inImmutableContext && !property.isConstructorImmutable())
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot initialise the property '" + var.getName() + "' in an immutable context: its constructor is not immutable", assignees[i].getLexicalPhrase()));
                }
                if (!property.isStatic())
                {
                  boolean isBaseImmutable = Type.isContextuallyDataImmutable(fieldAccessExpression.getBaseExpression().getType());
                  if (isBaseImmutable && !isInitialisation && !property.isConstructorImmutable())
                  {
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot assign to the property '" + var.getName() + "' through an immutable object: its constructor is not immutable", assignees[i].getLexicalPhrase()));
                  }
                }
                nowInitialisedVariables.add(var);
              }
              else
              {
                // this property may or may not have been initialised - this is an error, since we cannot decide between the constructor and the setter
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("This property may or may not have already been initialised. Cannot call either its constructor or its setter.", assignees[i].getLexicalPhrase()));
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
          checkControlFlow(assignStatement.getExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
      ConstructorReference constructorReference = delegateConstructorStatement.getResolvedConstructorReference();
      boolean isConstructorImmutable = constructorReference == null ? true : constructorReference.getReferencedMember().isImmutable();
      boolean isConstructorSelfish = constructorReference == null ? false : constructorReference.getReferencedMember().isSelfish();
      if (!inConstructor | inStaticContext | inInitialiser)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Delegate constructors may only be called from other constructors", delegateConstructorStatement.getLexicalPhrase()));
      }
      if (state.variables.initialiserState != InitialiserState.NOT_RUN)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("A delegate constructor may already have been run", delegateConstructorStatement.getLexicalPhrase()));
      }
      if (inImmutableContext && !isConstructorImmutable)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a non-immutable delegate constructor from an immutable constructor", delegateConstructorStatement.getLexicalPhrase()));
      }

      if (delegateConstructorStatement.isSuperConstructor() && isConstructorSelfish)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a selfish constructor as a super() constructor", delegateConstructorStatement.getLexicalPhrase()));
      }
      if (!inSelfishContext && isConstructorSelfish)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a selfish delegate constructor from a not-selfish constructor", delegateConstructorStatement.getLexicalPhrase()));
      }

      for (Argument argument : delegateConstructorStatement.getArguments())
      {
        if (argument instanceof NormalArgument)
        {
          try
          {
            checkControlFlow(((NormalArgument) argument).getExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        else if (argument instanceof DefaultArgument)
        {
          try
          {
            checkControlFlow(((DefaultArgument) argument).getExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        else
        {
          throw new IllegalArgumentException("Unknown type of Argument: " + argument);
        }
      }

      state.variables.initialiserState = InitialiserState.DEFINITELY_RUN;

      // after a delegate constructor call, all superclass member variables have now been initialised
      Set<Variable> nowInitialisedVariables = new HashSet<Variable>();
      Set<Variable> nowPossiblyInitialisedVariables = new HashSet<Variable>();
      for (Variable var : delegateConstructorVariables.superClassVariables)
      {
        if (var instanceof MemberVariable && var.isFinal() && state.variables.possiblyInitialised.contains(var))
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a delegate constructor here, since it would overwrite the value of '" + var.getName() + "' (in: " + ((MemberVariable) var).getEnclosingTypeDefinition().getQualifiedName() + "), which may already have been initialised", delegateConstructorStatement.getLexicalPhrase()));
        }
        if (var instanceof PropertyPseudoVariable && state.variables.possiblyInitialised.contains(var))
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a delegate constructor here, since it would initialise the property '" + var.getName() + "' (in: " + ((PropertyPseudoVariable) var).getProperty().getContainingTypeDefinition().getQualifiedName() + "), which may already have been initialised", delegateConstructorStatement.getLexicalPhrase()));
        }
        nowInitialisedVariables.add(var);
        nowPossiblyInitialisedVariables.add(var);
      }

      if (delegateConstructorStatement.isSuperConstructor())
      {
        // a super() constructor has been run, and the initialiser runs immediately after it, so all variables that are set by the initialiser must now be initialised
        for (Variable var : delegateConstructorVariables.initialiserDefinitelyInitialised)
        {
          nowInitialisedVariables.add(var);
        }
        for (Variable var : delegateConstructorVariables.initialiserPossiblyInitialised)
        {
          if (var instanceof MemberVariable && var.isFinal() && state.variables.possiblyInitialised.contains(var))
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a delegate constructor here, since the initialiser (which is run immediately after the super() constructor) could overwrite the value of '" + var.getName() + "' (in: " + ((MemberVariable) var).getEnclosingTypeDefinition().getQualifiedName() + "), which may already have been initialised", delegateConstructorStatement.getLexicalPhrase()));
          }
          if (var instanceof PropertyPseudoVariable && state.variables.possiblyInitialised.contains(var))
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a delegate constructor here, since the initialiser (which is run immediately after the super() constructor) could initialise the property '" + var.getName() + "' (in: " + ((PropertyPseudoVariable) var).getProperty().getContainingTypeDefinition().getQualifiedName() + "), which may already have been initialised", delegateConstructorStatement.getLexicalPhrase()));
          }
          nowPossiblyInitialisedVariables.add(var);
        }
      }
      else
      {
        // a this() constructor has been run, so all of the fields and properties have now been initialised, including the ones from the super-type
        for (Field field : enclosingTypeDefinition.getFields())
        {
          if (!field.isStatic())
          {
            MemberVariable var = field.getMemberVariable();
            if (field.isFinal() && state.variables.possiblyInitialised.contains(var))
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a this() constructor here, since it would overwrite the value of '" + field.getName() + "', which may already have been initialised", delegateConstructorStatement.getLexicalPhrase()));
            }
            nowInitialisedVariables.add(var);
            nowPossiblyInitialisedVariables.add(var);
          }
        }
        for (Property property : enclosingTypeDefinition.getProperties())
        {
          if (!property.isStatic())
          {
            PropertyPseudoVariable var = property.getPseudoVariable();
            if (state.variables.possiblyInitialised.contains(var))
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a this() constructor here, since it would initialise the value of '" + property.getName() + "', which may already have been initialised", delegateConstructorStatement.getLexicalPhrase()));
            }
            nowInitialisedVariables.add(var);
            nowPossiblyInitialisedVariables.add(var);
          }
        }
      }
      for (Variable var : nowInitialisedVariables)
      {
        state.variables.initialised.add(var);
      }
      for (Variable var : nowPossiblyInitialisedVariables)
      {
        state.variables.possiblyInitialised.add(var);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
      return false;
    }
    else if (statement instanceof ExpressionStatement)
    {
      checkControlFlow(((ExpressionStatement) statement).getExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
          checkControlFlow(condition, enclosingTypeDefinition, loopState.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
            checkControlFlow(condition, enclosingTypeDefinition, loopState.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
    else if (statement instanceof ForEachStatement)
    {
      ForEachStatement forEachStatement = (ForEachStatement) statement;

      // check the iterable expression
      CoalescedConceptualException coalescedException = null;
      try
      {
        checkControlFlow(forEachStatement.getIterableExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }

      // check the immutability constraints on the iterable expression
      if (forEachStatement.getResolvedIterableType() instanceof NamedType)
      {
        NamedType iterableType = (NamedType) forEachStatement.getResolvedIterableType();
        if (iterableType.getResolvedTypeDefinition() == SpecialTypeHandler.iteratorType.getResolvedTypeDefinition() ||
            iterableType.getResolvedTypeDefinition() == SpecialTypeHandler.iterableType.getResolvedTypeDefinition())
        {
          if (inImmutableContext)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot iterate through an Iterator in an immutable context, as Iterator::next() is not immutable", forEachStatement.getIterableExpression().getLexicalPhrase()));
          }
          else if (iterableType.canBeExplicitlyImmutable() || iterableType.isContextuallyImmutable())
          {
            if (iterableType.getResolvedTypeDefinition() == SpecialTypeHandler.iteratorType.getResolvedTypeDefinition())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot iterate through an Iterator on an immutable object, as Iterator::next() is not immutable", forEachStatement.getIterableExpression().getLexicalPhrase()));
            }
            else
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot iterate through an Iterable on an immutable object, as Iterator::next() is not immutable", forEachStatement.getIterableExpression().getLexicalPhrase()));
            }
          }
        }
      }

      ControlFlowState loopState = state.copy();

      Variable resolvedVariable = forEachStatement.getResolvedVariable();
      loopState.variables.initialised.add(resolvedVariable);
      loopState.variables.possiblyInitialised.add(resolvedVariable);

      enclosingBreakableStack.push(forEachStatement);
      boolean returned = forEachStatement.getBlock().stopsExecution();
      try
      {
        returned = checkControlFlow(forEachStatement.getBlock(), enclosingTypeDefinition, loopState, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (returned)
      {
        loopState.overwriteWithContinueVariables(forEachStatement);
      }
      else
      {
        loopState.reintegrateContinueVariables(forEachStatement);
      }

      if (coalescedException != null)
      {
        // make sure we clean up the breakable stack if something fails
        enclosingBreakableStack.pop();
        throw coalescedException;
      }

      // run through the loop block again, so that we catch any final variables that are initialised in the loop
      // (only if it is possible to run the loop more than once)
      if (!returned || forEachStatement.isContinuedThrough())
      {
        boolean secondReturned = forEachStatement.getBlock().stopsExecution();
        try
        {
          secondReturned = checkControlFlow(forEachStatement.getBlock(), enclosingTypeDefinition, loopState, delegateConstructorVariables, enclosingBreakableStack, inConstructor, inSelfishContext, inStaticContext, inImmutableContext, inInitialiser);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
        if (secondReturned)
        {
          loopState.overwriteWithContinueVariables(forEachStatement);
        }
        else
        {
          loopState.reintegrateContinueVariables(forEachStatement);
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
      state.reintegrateBreakVariables(forEachStatement);

      enclosingBreakableStack.pop();

      if (coalescedException != null)
      {
        throw coalescedException;
      }
      return false;
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      CoalescedConceptualException coalescedException = null;
      try
      {
        checkControlFlow(ifStatement.getExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
      Variable nowInitialisedVariable = null;
      if (assignee instanceof VariableAssignee)
      {
        Variable var = ((VariableAssignee) assignee).getResolvedVariable();
        if (var instanceof MemberVariable)
        {
          if (inStaticContext)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The instance variable '" + var.getName() + "' does not exist in a static context", assignee.getLexicalPhrase()));
          }
          else
          {
            if (inConstructor && !state.variables.initialised.contains(var))
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Variable '" + ((VariableAssignee) assignee).getVariableName() + "' may not have been initialised", assignee.getLexicalPhrase()));
            }
            if (var.isFinal())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final variable '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
            }
            // if it is a mutable field or a mutable property backing variable, allow assignments even in an immutable context
            boolean isMutable = (((MemberVariable) var).getField() != null    && ((MemberVariable) var).getField().isMutable()) ||
                                (((MemberVariable) var).getProperty() != null && ((MemberVariable) var).getProperty().isMutable());
            if (inImmutableContext && !inConstructor && !isMutable)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the instance variable '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase()));
            }
          }
        }
        else if (var instanceof GlobalVariable)
        {
          // an increment/decrement can be an initialisation for a static final variable, because they can be read before they are initialised
          // assigning to a global variable is only ever an initialisation if the variable is final, otherwise something else could have assigned to it first
          boolean isInitialisation = var.isFinal() && inStaticContext && (inInitialiser || inConstructor) && ((GlobalVariable) var).getEnclosingTypeDefinition() == enclosingTypeDefinition;
          if (isInitialisation)
          {
            if (state.variables.possiblyInitialised.contains(var))
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' may already have been initialised", assignee.getLexicalPhrase()));
            }
            nowInitialisedVariable = var;
          }
          else if (var.isFinal())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
          }
          if (!isInitialisation && inStaticContext && inConstructor && ((GlobalVariable) var).getProperty() != null)
          {
            // this is a property backing variable, and we are inside a property constructor, so initialise this backing variable
            nowInitialisedVariable = var;
          }
          // if it is a mutable field or a mutable property backing variable, allow assignments even in an immutable context
          boolean isMutable = (((GlobalVariable) var).getField() != null    && ((GlobalVariable) var).getField().isMutable()) ||
                              (((GlobalVariable) var).getProperty() != null && ((GlobalVariable) var).getProperty().isMutable());
          if (inImmutableContext && !isMutable)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the static variable '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase()));
          }
        }
        else if (var instanceof PropertyPseudoVariable)
        {
          Property property = ((PropertyPseudoVariable) var).getProperty();
          if (inStaticContext && !property.isStatic())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The instance property '" + var.getName() + "' does not exist in a static context", assignee.getLexicalPhrase()));
          }
          else
          {
            if (inImmutableContext && !property.isGetterImmutable())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot read from the property '" + property.getName() + "' in an immutable context: its getter is not immutable", assignee.getLexicalPhrase()));
            }
            if (!property.isStatic() && !inStaticContext && inConstructor && !state.variables.initialised.contains(property.getPseudoVariable()))
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The property '" + property.getName() + "' may not have been initialised", assignee.getLexicalPhrase()));
            }

            // we can only initialise static final properties here, as they are the only ones that can be read before their constructor is called
            // (static non-final properties never have a constructor, and cannot be initialised)
            boolean isInitialisation = property.isStatic() && property.isFinal() && inStaticContext && (inInitialiser || inConstructor) && property.getContainingTypeDefinition() == enclosingTypeDefinition;
            if (!isInitialisation || state.variables.initialised.contains(var))
            {
              // either we are not in any sort of initialiser, or this property has already been initialised
              // so this is a setter call
              if (var.isFinal())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final property '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
              }
              else if (inImmutableContext && !property.isSetterImmutable())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the property '" + var.getName() + "' in an immutable context: its setter is not immutable", assignee.getLexicalPhrase()));
              }
              ((VariableAssignee) assignee).setPropertyConstructorCall(false);
            }
            if (isInitialisation)
            {
              // we are initialising a static final property
              if (state.variables.possiblyInitialised.contains(var))
              {
                // this property may or may not have been initialised - this is an error, since we cannot decide between the constructor and the setter
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The final property '" + property.getName() + "' may have already been initialised.", assignee.getLexicalPhrase()));
              }
              else
              {
                // this property has definitely not already been initialised, so this is a constructor call
                ((VariableAssignee) assignee).setPropertyConstructorCall(true);
                if (inImmutableContext && !property.isConstructorImmutable())
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot initialise the property '" + var.getName() + "' in an immutable context: its constructor is not immutable", assignee.getLexicalPhrase()));
                }
                nowInitialisedVariable = var;
              }
            }
            if (!property.isStatic() && !inStaticContext && inConstructor)
            {
              // this is a getter call during a constructor, so make sure it is allowed (i.e. everything else is already initialised)
              try
              {
                checkThisAccess(state.variables.initialised, state.variables.initialiserState, enclosingTypeDefinition, inSelfishContext, "call a property getter on", assignee.getLexicalPhrase());
              }
              catch (ConceptualException e)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
              }
            }
          }
        }
        else
        {
          if (!state.variables.initialised.contains(var))
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Variable '" + ((VariableAssignee) assignee).getVariableName() + "' may not have been initialised", assignee.getLexicalPhrase()));
          }
        }
      }
      else if (assignee instanceof ArrayElementAssignee)
      {
        ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
        try
        {
          checkControlFlow(arrayElementAssignee.getArrayExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
        try
        {
          checkControlFlow(arrayElementAssignee.getDimensionExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
        MemberReference<?> resolvedMemberReference = fieldAccessExpression.getResolvedMemberReference();

        // check whether this assignee is for an instance field/property on 'this'
        boolean isOnThis = false;
        if (inConstructor && !inStaticContext && fieldAccessExpression.getBaseExpression() != null)
        {
          Expression expression = fieldAccessExpression.getBaseExpression();
          while (expression instanceof BracketedExpression)
          {
            expression = ((BracketedExpression) expression).getExpression();
          }
          isOnThis = expression instanceof ThisExpression;
        }

        // treat this as a field access, and check for uninitialised variables as normal
        // note: we are not checking the base expression here, and if the Expression control flow checker finds that the access is on this, it will not check it either
        try
        {
          checkControlFlow(fieldAccessExpression, enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }

        if (resolvedMemberReference instanceof FieldReference)
        {
          FieldReference fieldReference = (FieldReference) resolvedMemberReference;
          Field field = fieldReference.getReferencedMember();
          Variable var = field.isStatic() ? field.getGlobalVariable() : field.getMemberVariable();
          boolean isInitialisation = field.isStatic() && field.isFinal() && inStaticContext && inInitialiser && field.getGlobalVariable().getEnclosingTypeDefinition() == enclosingTypeDefinition;
          if (isInitialisation)
          {
            if (state.variables.possiblyInitialised.contains(var))
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' may already have been initialised", assignee.getLexicalPhrase()));
            }
            nowInitialisedVariable = var;
          }
          else if (var.isFinal())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The final field '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
          }
          if (field.isStatic())
          {
            if (inImmutableContext && !isInitialisation && !field.isMutable())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the field '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase()));
            }
          }
          else
          {
            boolean isBaseImmutable = Type.isContextuallyDataImmutable(fieldAccessExpression.getBaseExpression().getType());
            if (isBaseImmutable && !isInitialisation && !field.isMutable())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the field '" + var.getName() + "' through an immutable object", assignee.getLexicalPhrase()));
            }
          }
        }
        else if (resolvedMemberReference instanceof PropertyReference)
        {
          PropertyReference propertyReference = (PropertyReference) resolvedMemberReference;
          Property property = propertyReference.getReferencedMember();
          PropertyPseudoVariable var = property.getPseudoVariable();
          boolean isInitialisation = property.isStatic() && property.isFinal() && inStaticContext && (inInitialiser || inConstructor) && property.getContainingTypeDefinition() == enclosingTypeDefinition;
          if (!isInitialisation || state.variables.initialised.contains(var))
          {
            // either we are not an initialiser for this property, or it has already been initialised
            // so this is a setter call
            if (var.isFinal())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final property '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
            }
            else if (inImmutableContext && !property.isSetterImmutable())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the property '" + var.getName() + "' in an immutable context: its setter is not immutable", assignee.getLexicalPhrase()));
            }
            else if (!property.isStatic())
            {
              boolean isBaseImmutable = Type.isContextuallyDataImmutable(fieldAccessExpression.getBaseExpression().getType());
              if (isBaseImmutable && !isInitialisation && !property.isSetterImmutable())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the property '" + var.getName() + "' through an immutable object: its setter is not immutable", assignee.getLexicalPhrase()));
              }
            }
            fieldAssignee.setPropertyConstructorCall(false);
          }
          if (isInitialisation)
          {
            // we are initialising a static final property
            if (state.variables.possiblyInitialised.contains(var))
            {
              // this property may or may not have been initialised - this is an error, since we cannot decide between the constructor and the setter
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The final property '" + property.getName() + "' may have already been initialised.", assignee.getLexicalPhrase()));
            }
            else
            {
              // this property has definitely not already been initialised, so this is a constructor call
              fieldAssignee.setPropertyConstructorCall(true);
              if (inImmutableContext && !property.isConstructorImmutable())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot initialise the property '" + var.getName() + "' in an immutable context: its constructor is not immutable", assignee.getLexicalPhrase()));
              }
              nowInitialisedVariable = var;
            }
          }
          if (!property.isStatic() && isOnThis)
          {
            // this is a getter call during a constructor, so make sure it is allowed (i.e. everything else is already initialised)
            try
            {
              checkThisAccess(state.variables.initialised, state.variables.initialiserState, enclosingTypeDefinition, inSelfishContext, "call a property getter on", assignee.getLexicalPhrase());
            }
            catch (ConceptualException e)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
            }
          }
        }
      }
      else
      {
        // ignore blank assignees, they shouldn't be able to get through variable resolution
        throw new IllegalStateException("Unknown Assignee type: " + assignee);
      }
      // static final things can be initialised by being modified, since they can be read before they are initialised
      if (nowInitialisedVariable != null)
      {
        state.variables.initialised.add(nowInitialisedVariable);
        state.variables.possiblyInitialised.add(nowInitialisedVariable);
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
        checkControlFlow(returnedExpression, enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
      Set<Variable> nowInitialisedVariables = new HashSet<Variable>();
      for (Assignee assignee : shorthandAssignStatement.getAssignees())
      {
        if (assignee instanceof VariableAssignee)
        {
          VariableAssignee variableAssignee = (VariableAssignee) assignee;
          Variable var = variableAssignee.getResolvedVariable();
          if (var instanceof MemberVariable)
          {
            if (inStaticContext)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The instance variable '" + var.getName() + "' does not exist in a static context", assignee.getLexicalPhrase()));
            }
            else
            {
              if (inConstructor && !state.variables.initialised.contains(var))
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Variable '" + ((VariableAssignee) assignee).getVariableName() + "' may not have been initialised", assignee.getLexicalPhrase()));
              }
              if (var.isFinal())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final variable '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
              }
              // if it is a mutable field or a mutable property backing variable, allow assignments even in an immutable context
              boolean isMutable = (((MemberVariable) var).getField() != null    && ((MemberVariable) var).getField().isMutable()) ||
                                  (((MemberVariable) var).getProperty() != null && ((MemberVariable) var).getProperty().isMutable());
              if (inImmutableContext && !inConstructor && !isMutable)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the instance variable '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase()));
              }
            }
          }
          else if (var instanceof GlobalVariable)
          {
            // an increment/decrement can be an initialisation for a static final variable, because they can be read before they are initialised
            // assigning to a global variable is only ever an initialisation if the variable is final, otherwise something else could have assigned to it first
            boolean isInitialisation = var.isFinal() && inStaticContext && (inInitialiser || inConstructor) && ((GlobalVariable) var).getEnclosingTypeDefinition() == enclosingTypeDefinition;
            if (isInitialisation)
            {
              if (state.variables.possiblyInitialised.contains(var))
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' may already have been initialised", assignee.getLexicalPhrase()));
              }
              nowInitialisedVariables.add(var);
            }
            else if (var.isFinal())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
            }
            if (!isInitialisation && inStaticContext && inConstructor && ((GlobalVariable) var).getProperty() != null)
            {
              // this is a property backing variable, and we are inside a property constructor, so initialise this backing variable
              nowInitialisedVariables.add(var);
            }
            // if it is a mutable field or a mutable property backing variable, allow assignments even in an immutable context
            boolean isMutable = (((GlobalVariable) var).getField() != null    && ((GlobalVariable) var).getField().isMutable()) ||
                                (((GlobalVariable) var).getProperty() != null && ((GlobalVariable) var).getProperty().isMutable());
            if (inImmutableContext && !isMutable)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the static variable '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase()));
            }
          }
          else if (var instanceof PropertyPseudoVariable)
          {
            Property property = ((PropertyPseudoVariable) var).getProperty();
            if (inStaticContext && !property.isStatic())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The instance property '" + var.getName() + "' does not exist in a static context", assignee.getLexicalPhrase()));
            }
            else
            {
              if (inImmutableContext && !property.isGetterImmutable())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot read from the property '" + property.getName() + "' in an immutable context: its getter is not immutable", assignee.getLexicalPhrase()));
              }
              if (!property.isStatic() && !inStaticContext && inConstructor && !state.variables.initialised.contains(property.getPseudoVariable()))
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The property '" + property.getName() + "' may not have been initialised", assignee.getLexicalPhrase()));
              }

              // we can only initialise static final properties here, as they are the only ones that can be read before their constructor is called
              // (static non-final properties never have a constructor, and cannot be initialised)
              boolean isInitialisation = property.isStatic() && property.isFinal() && inStaticContext && inInitialiser && property.getContainingTypeDefinition() == enclosingTypeDefinition;
              if (!isInitialisation || state.variables.initialised.contains(var))
              {
                // either we are not in any sort of initialiser, or this property has already been initialised
                // so this is a setter call
                if (var.isFinal())
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final property '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
                }
                else if (inImmutableContext && !property.isSetterImmutable())
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the property '" + var.getName() + "' in an immutable context: its setter is not immutable", assignee.getLexicalPhrase()));
                }
                ((VariableAssignee) assignee).setPropertyConstructorCall(false);
              }
              if (isInitialisation)
              {
                // we are initialising a static final property
                if (state.variables.possiblyInitialised.contains(var))
                {
                  // this property may or may not have been initialised - this is an error, since we cannot decide between the constructor and the setter
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The final property '" + property.getName() + "' may have already been initialised.", assignee.getLexicalPhrase()));
                }
                else
                {
                  // this property has definitely not already been initialised, so this is a constructor call
                  ((VariableAssignee) assignee).setPropertyConstructorCall(true);
                  if (inImmutableContext && !property.isConstructorImmutable())
                  {
                    coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot initialise the property '" + var.getName() + "' in an immutable context: its constructor is not immutable", assignee.getLexicalPhrase()));
                  }
                  nowInitialisedVariables.add(var);
                }
              }
              if (!property.isStatic() && !inStaticContext && inConstructor)
              {
                // this is a getter call during a constructor, so make sure it is allowed (i.e. everything else is already initialised)
                try
                {
                  checkThisAccess(state.variables.initialised, state.variables.initialiserState, enclosingTypeDefinition, inSelfishContext, "call a property getter on", assignee.getLexicalPhrase());
                }
                catch (ConceptualException e)
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
                }
              }
            }
          }
          else
          {
            if (!state.variables.initialised.contains(var))
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Variable '" + ((VariableAssignee) assignee).getVariableName() + "' may not have been initialised", assignee.getLexicalPhrase()));
            }
          }
        }
        else if (assignee instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
          try
          {
            checkControlFlow(arrayElementAssignee.getArrayExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
          try
          {
            checkControlFlow(arrayElementAssignee.getDimensionExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
          MemberReference<?> resolvedMemberReference = fieldAccessExpression.getResolvedMemberReference();

          // check whether this assignee is for an instance field/property on 'this'
          boolean isOnThis = false;
          if (inConstructor && !inStaticContext && fieldAccessExpression.getBaseExpression() != null)
          {
            Expression expression = fieldAccessExpression.getBaseExpression();
            while (expression instanceof BracketedExpression)
            {
              expression = ((BracketedExpression) expression).getExpression();
            }
            isOnThis = expression instanceof ThisExpression;
          }

          // treat this as a field access, and check for uninitialised variables as normal
          // note: we are not checking the base expression here, and if the Expression control flow checker finds that the access is on this, it will not check it either
          try
          {
            checkControlFlow(fieldAccessExpression, enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }

          if (resolvedMemberReference instanceof FieldReference)
          {
            FieldReference fieldReference = (FieldReference) resolvedMemberReference;
            Field field = fieldReference.getReferencedMember();
            Variable var = field.isStatic() ? field.getGlobalVariable() : field.getMemberVariable();
            boolean isInitialisation = field.isStatic() && field.isFinal() && inStaticContext && (inInitialiser || inConstructor) && field.getGlobalVariable().getEnclosingTypeDefinition() == enclosingTypeDefinition;
            if (isInitialisation)
            {
              if (state.variables.possiblyInitialised.contains(var))
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The static final field '" + var.getName() + "' may already have been initialised", assignee.getLexicalPhrase()));
              }
              nowInitialisedVariables.add(var);
            }
            else if (var.isFinal())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The final field '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
            }
            if (field.isStatic())
            {
              if (inImmutableContext && !isInitialisation && !field.isMutable())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the field '" + var.getName() + "' in an immutable context", assignee.getLexicalPhrase()));
              }
            }
            else
            {
              boolean isBaseImmutable = Type.isContextuallyDataImmutable(fieldAccessExpression.getBaseExpression().getType());
              if (isBaseImmutable && !isInitialisation && !field.isMutable())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the field '" + var.getName() + "' through an immutable object", assignee.getLexicalPhrase()));
              }
            }
          }
          else if (resolvedMemberReference instanceof PropertyReference)
          {
            PropertyReference propertyReference = (PropertyReference) resolvedMemberReference;
            Property property = propertyReference.getReferencedMember();
            PropertyPseudoVariable var = property.getPseudoVariable();
            boolean isInitialisation = property.isStatic() && property.isFinal() && inStaticContext && inInitialiser && property.getContainingTypeDefinition() == enclosingTypeDefinition;
            if (!isInitialisation || state.variables.initialised.contains(var))
            {
              // either we are not an initialiser for this property, or it has already been initialised
              // so this is a setter call
              if (var.isFinal())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Final property '" + var.getName() + "' cannot be modified", assignee.getLexicalPhrase()));
              }
              else if (inImmutableContext && !property.isSetterImmutable())
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the property '" + var.getName() + "' in an immutable context: its setter is not immutable", assignee.getLexicalPhrase()));
              }
              else if (!property.isStatic())
              {
                boolean isBaseImmutable = Type.isContextuallyDataImmutable(fieldAccessExpression.getBaseExpression().getType());
                if (isBaseImmutable && !isInitialisation && !property.isSetterImmutable())
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot modify the property '" + var.getName() + "' through an immutable object: its setter is not immutable", assignee.getLexicalPhrase()));
                }
              }
              fieldAssignee.setPropertyConstructorCall(false);
            }
            if (isInitialisation)
            {
              // we are initialising a static final property
              if (state.variables.possiblyInitialised.contains(var))
              {
                // this property may or may not have been initialised - this is an error, since we cannot decide between the constructor and the setter
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The final property '" + property.getName() + "' may have already been initialised.", assignee.getLexicalPhrase()));
              }
              else
              {
                // this property has definitely not already been initialised, so this is a constructor call
                fieldAssignee.setPropertyConstructorCall(true);
                if (inImmutableContext && !property.isConstructorImmutable())
                {
                  coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot initialise the property '" + var.getName() + "' in an immutable context: its constructor is not immutable", assignee.getLexicalPhrase()));
                }
                nowInitialisedVariables.add(var);
              }
            }
            if (!property.isStatic() && isOnThis)
            {
              // this is a getter call during a constructor, so make sure it is allowed (i.e. everything else is already initialised)
              try
              {
                checkThisAccess(state.variables.initialised, state.variables.initialiserState, enclosingTypeDefinition, inSelfishContext, "call a property getter on", assignee.getLexicalPhrase());
              }
              catch (ConceptualException e)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
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
        checkControlFlow(shorthandAssignStatement.getExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      for (Variable var : nowInitialisedVariables)
      {
        state.variables.initialised.add(var);
        state.variables.possiblyInitialised.add(var);
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
      checkControlFlow(throwStatement.getThrownExpression(), enclosingTypeDefinition, state.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
        checkControlFlow(whileStatement.getExpression(), enclosingTypeDefinition, loopState.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
          checkControlFlow(whileStatement.getExpression(), enclosingTypeDefinition, loopState.variables.initialised, state.variables.initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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

  private static void checkControlFlow(Expression expression, TypeDefinition enclosingTypeDefinition, Set<Variable> initialisedVariables, InitialiserState initialiserState, boolean inConstructor, boolean inSelfishContext, boolean inStaticContext, boolean inImmutableContext) throws ConceptualException
  {
    if (expression instanceof ArithmeticExpression)
    {
      ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;
      CoalescedConceptualException coalescedException = null;
      try
      {
        checkControlFlow(arithmeticExpression.getLeftSubExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(arithmeticExpression.getRightSubExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
        checkControlFlow(arrayAccessExpression.getArrayExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(arrayAccessExpression.getDimensionExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
      if (creationExpression.getDimensionExpressions() != null)
      {
        for (Expression expr : creationExpression.getDimensionExpressions())
        {
          try
          {
            checkControlFlow(expr, enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
            checkControlFlow(expr, enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
      }
      if (creationExpression.getInitialisationExpression() != null)
      {
        try
        {
          checkControlFlow(creationExpression.getInitialisationExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
        if (creationExpression.getResolvedIsInitialiserFunction())
        {
          FunctionType initialiserFunctionType = (FunctionType) creationExpression.getInitialisationExpression().getType();
          if (inImmutableContext && initialiserFunctionType.isImmutable())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a non-immutable function in an immutable context", creationExpression.getInitialisationExpression().getLexicalPhrase()));
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
      checkControlFlow(((BitwiseNotExpression) expression).getExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof BooleanLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof BooleanNotExpression)
    {
      checkControlFlow(((BooleanNotExpression) expression).getExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof BracketedExpression)
    {
      checkControlFlow(((BracketedExpression) expression).getExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof CastExpression)
    {
      checkControlFlow(((CastExpression) expression).getExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof CreationExpression)
    {
      CreationExpression creationExpression = (CreationExpression) expression;
      CoalescedConceptualException coalescedException = null;
      for (Argument argument : creationExpression.getArguments())
      {
        if (argument instanceof NormalArgument)
        {
          try
          {
            checkControlFlow(((NormalArgument) argument).getExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        else if (argument instanceof DefaultArgument)
        {
          try
          {
            checkControlFlow(((DefaultArgument) argument).getExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        else
        {
          throw new IllegalArgumentException("Unknown type of Argument: " + argument);
        }
      }
      if (inImmutableContext && !creationExpression.getResolvedConstructorReference().getReferencedMember().isImmutable())
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a non-immutable constructor from an immutable context (it may alter global variables)", creationExpression.getLexicalPhrase()));
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
        checkControlFlow(equalityExpression.getLeftSubExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(equalityExpression.getRightSubExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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

      // check whether this assignee is for an instance field/property on 'this'
      boolean isOnThis = false;
      if (inConstructor && !inStaticContext && fieldAccessExpression.getBaseExpression() != null)
      {
        Expression baseExpression = fieldAccessExpression.getBaseExpression();
        while (baseExpression instanceof BracketedExpression)
        {
          baseExpression = ((BracketedExpression) baseExpression).getExpression();
        }
        isOnThis = baseExpression instanceof ThisExpression;
      }

      // if we're in a constructor, only check the sub-expression for uninitialised variables if it doesn't just access 'this'
      // this allows the programmer to access fields before 'this' is fully initialised
      if (!isOnThis && fieldAccessExpression.getBaseExpression() != null)
      {
        try
        {
          checkControlFlow(fieldAccessExpression.getBaseExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }

      MemberReference<?> resolvedMemberReference = fieldAccessExpression.getResolvedMemberReference();
      if (resolvedMemberReference instanceof FieldReference)
      {
        FieldReference fieldReference = (FieldReference) resolvedMemberReference;
        Field field = fieldReference.getReferencedMember();
        if (!field.isStatic() && isOnThis && !initialisedVariables.contains(field.getMemberVariable()))
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The field '" + field.getName() + "' may not have been initialised", fieldAccessExpression.getLexicalPhrase()));
        }
      }
      else if (resolvedMemberReference instanceof PropertyReference)
      {
        PropertyReference propertyReference = (PropertyReference) resolvedMemberReference;
        Property property = propertyReference.getReferencedMember();
        if (inImmutableContext && !property.isGetterImmutable())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot read from the property '" + property.getName() + "' in an immutable context: its getter is not immutable", fieldAccessExpression.getLexicalPhrase()));
        }
        if (!property.isStatic() && isOnThis)
        {
          try
          {
            checkThisAccess(initialisedVariables, initialiserState, enclosingTypeDefinition, inSelfishContext, "call a getter on", expression.getLexicalPhrase());
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
          if (!initialisedVariables.contains(property.getPseudoVariable()))
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The property '" + property.getName() + "' may not have been initialised", fieldAccessExpression.getLexicalPhrase()));
          }
        }
      }
      else if (resolvedMemberReference instanceof MethodReference)
      {
        // non-static methods cannot be accessed as fields before 'this' has been initialised
        if (isOnThis)
        {
          checkThisAccess(initialisedVariables, initialiserState, enclosingTypeDefinition, inSelfishContext, "access a method on", expression.getLexicalPhrase());
        }
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
      if (functionCallExpression.getResolvedMethodReference() != null)
      {
        if (inImmutableContext && !functionCallExpression.getResolvedMethodReference().getReferencedMember().isImmutable())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a non-immutable method from an immutable context", functionCallExpression.getLexicalPhrase()));
        }
        Expression resolvedBaseExpression = functionCallExpression.getResolvedBaseExpression();
        if (resolvedBaseExpression == null)
        {
          Method resolvedMethod = functionCallExpression.getResolvedMethodReference().getReferencedMember();
          if (inStaticContext && !resolvedMethod.isStatic())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call the instance method '" + resolvedMethod.getName() + "' from a static context", functionCallExpression.getLexicalPhrase()));
          }
          else if (inConstructor && !inStaticContext && !resolvedMethod.isStatic())
          {
            // we are in a constructor, and we are calling a non-static method without a base expression (i.e. on 'this')
            // this should only be allowed if 'this' is fully initialised
            checkThisAccess(initialisedVariables, initialiserState, enclosingTypeDefinition, inSelfishContext, "call a method on", expression.getLexicalPhrase());
          }
        }
        else // resolvedBaseExpression != null
        {
          try
          {
            checkControlFlow(resolvedBaseExpression, enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
          Type baseType = resolvedBaseExpression.getType();
          if ((baseType instanceof ArrayType    &&  ((ArrayType)    baseType).isContextuallyImmutable()) ||
              (baseType instanceof NamedType    && (((NamedType)    baseType).isContextuallyImmutable()  || ((NamedType) baseType).canBeExplicitlyImmutable())) ||
              (baseType instanceof ObjectType   &&  ((ObjectType)   baseType).isContextuallyImmutable()) ||
              (baseType instanceof WildcardType && (((WildcardType) baseType).isContextuallyImmutable()  || ((WildcardType) baseType).canBeExplicitlyImmutable())))
          {
            if (!functionCallExpression.getResolvedMethodReference().getReferencedMember().isImmutable())
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a non-immutable method on an immutable object", functionCallExpression.getLexicalPhrase()));
            }
          }
        }
      }
      else if (functionCallExpression.getResolvedBaseExpression() != null)
      {
        checkControlFlow(functionCallExpression.getResolvedBaseExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
      for (Argument argument : functionCallExpression.getArguments())
      {
        if (argument instanceof NormalArgument)
        {
          try
          {
            checkControlFlow(((NormalArgument) argument).getExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        else if (argument instanceof DefaultArgument)
        {
          try
          {
            checkControlFlow(((DefaultArgument) argument).getExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        else
        {
          throw new IllegalArgumentException("Unknown type of Argument: " + argument);
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
        checkControlFlow(inlineIfExpression.getCondition(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(inlineIfExpression.getThenExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(inlineIfExpression.getElseExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
      checkControlFlow(((InstanceOfExpression) expression).getExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
        checkControlFlow(logicalExpression.getLeftSubExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(logicalExpression.getRightSubExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
      checkControlFlow(((MinusExpression) expression).getExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof NullCoalescingExpression)
    {
      CoalescedConceptualException coalescedException = null;
      try
      {
        checkControlFlow(((NullCoalescingExpression) expression).getNullableExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(((NullCoalescingExpression) expression).getAlternativeExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
        checkControlFlow(relationalExpression.getLeftSubExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(relationalExpression.getRightSubExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
        checkControlFlow(shiftExpression.getLeftExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkControlFlow(shiftExpression.getRightExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
        checkThisAccess(initialisedVariables, initialiserState, enclosingTypeDefinition, inSelfishContext, "use", expression.getLexicalPhrase());
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
          checkControlFlow(subExpressions[i], enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
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
      checkControlFlow(indexExpression.getExpression(), enclosingTypeDefinition, initialisedVariables, initialiserState, inConstructor, inSelfishContext, inStaticContext, inImmutableContext);
    }
    else if (expression instanceof VariableExpression)
    {
      CoalescedConceptualException coalescedException = null;
      VariableExpression variableExpression = (VariableExpression) expression;
      Variable var = variableExpression.getResolvedVariable();
      MemberReference<?> memberReference = variableExpression.getResolvedMemberReference();
      if (var != null)
      {
        if (var instanceof MemberVariable)
        {
          if (inStaticContext)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The non-static instance variable '" + variableExpression.getName() + "' does not exist in a static context", expression.getLexicalPhrase()));
          }
          else if (inConstructor && !initialisedVariables.contains(var))
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Variable '" + variableExpression.getName() + "' may not have been initialised", variableExpression.getLexicalPhrase()));
          }
        }
        else if (var instanceof GlobalVariable)
        {
          // accessing a global variable is always allowed
        }
        else if (var instanceof PropertyPseudoVariable)
        {
          Property property = ((PropertyPseudoVariable) var).getProperty();
          if (inImmutableContext && !property.isGetterImmutable())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot read from the property '" + var.getName() + "' in an immutable context: its getter is not immutable", variableExpression.getLexicalPhrase()));
          }
          if (property.isStatic())
          {
            // accessing a static property is always allowed (assuming immutability is not a concern)
          }
          else
          {
            if (inStaticContext)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The instance property '" + variableExpression.getName() + "' does not exist in a static context", expression.getLexicalPhrase()));
            }
            else if (inConstructor && !initialisedVariables.contains(var))
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Property '" + variableExpression.getName() + "' may not have been initialised", variableExpression.getLexicalPhrase()));
            }
            else if (inConstructor && !inStaticContext)
            {
              try
              {
                checkThisAccess(initialisedVariables, initialiserState, enclosingTypeDefinition, inSelfishContext, "call a getter on", expression.getLexicalPhrase());
              }
              catch (ConceptualException e)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
              }
            }
          }
        }
        else // local variable or parameter
        {
          if (!initialisedVariables.contains(var))
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Variable '" + variableExpression.getName() + "' may not have been initialised", variableExpression.getLexicalPhrase()));
          }
        }
      }
      else if (memberReference != null && memberReference instanceof MethodReference)
      {
        MethodReference methodReference = (MethodReference) memberReference;
        Method method = methodReference.getReferencedMember();
        if (inStaticContext && !method.isStatic())
        {
          throw new ConceptualException("Cannot access the non-static method '" + method.getName() + "' from a static context", expression.getLexicalPhrase());
        }
        if (inConstructor && !method.isStatic())
        {
          // non-static methods cannot be accessed as fields before 'this' has been initialised
          checkThisAccess(initialisedVariables, initialiserState, enclosingTypeDefinition, inSelfishContext, "access a method on", expression.getLexicalPhrase());
        }
      }
      else
      {
        throw new IllegalArgumentException("A VariableExpression must have been resolved to either a variable or a method");
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else
    {
      throw new IllegalArgumentException("Internal control flow checking error: Unknown expression type: " + expression);
    }
  }

  /**
   * Checks whether it is safe to use 'this' at this point in the current constructor or non-static initialiser.
   * This can depend on whether we are in a selfish context, whether or not the initialiser has been run, and whether or not all of the variables have been initialised.
   * @param initialisedVariables - the currently initialised variables
   * @param initialiserState - whether the initialiser has definitely, possibly, or definitely not been run
   * @param containingTypeDefinition - the containing type definition, to make sure all of its fields and properties have been initialised
   * @param inSelfishContext - whether we are in a selfish context
   * @param failedAccessDescription - a very brief description of what we are trying to do with 'this', such as "use" or "access methods on" - for use in a phrase like "Cannot {description} 'this' here"
   * @param lexicalPhrase - the LexicalPhrase of the thing which needs access to 'this'
   * @throws ConceptualException - if a problem is detected
   */
  private static void checkThisAccess(Set<Variable> initialisedVariables, InitialiserState initialiserState, TypeDefinition containingTypeDefinition, boolean inSelfishContext, String failedAccessDescription, LexicalPhrase lexicalPhrase) throws ConceptualException
  {
    if (!inSelfishContext)
    {
      throw new ConceptualException("Cannot " + failedAccessDescription + " 'this' unless we are within a selfish constructor", lexicalPhrase);
    }
    if (initialiserState != InitialiserState.DEFINITELY_RUN)
    {
      throw new ConceptualException("Cannot " + failedAccessDescription + " 'this' here. The initialiser of this " + containingTypeDefinition.getQualifiedName() + " may not have been run yet", lexicalPhrase);
    }
    for (Field field : containingTypeDefinition.getFields())
    {
      if (!field.isStatic() && !initialisedVariables.contains(field.getMemberVariable()))
      {
        throw new ConceptualException("Cannot " + failedAccessDescription + " 'this' here.\n" +
                                      "Not all of the non-static fields of this " + containingTypeDefinition.getQualifiedName() + " have been initialised " +
                                      "(specifically: '" + field.getName() + "'), and I cannot work out whether or not you are going to initialise them before they are used", lexicalPhrase);
      }
    }
    for (Property property : containingTypeDefinition.getProperties())
    {
      if (!property.isStatic() && !initialisedVariables.contains(property.getPseudoVariable()))
      {
        throw new ConceptualException("Cannot " + failedAccessDescription + " 'this' here.\n" +
                                      "Not all of the non-static properties of this " + containingTypeDefinition.getQualifiedName() + " have been initialised " +
                                      "(specifically: '" + property.getName() + "'), and I cannot work out whether or not you are going to initialise them before they are used", lexicalPhrase);
      }
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
     * @param initialiserState - the state of the initialiser
     */
    public ControlFlowVariables(InitialiserState initialiserState)
    {
      initialised = new HashSet<Variable>();
      possiblyInitialised = new HashSet<Variable>();
      this.initialiserState = initialiserState;
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
    ControlFlowState(InitialiserState initialiserState)
    {
      variables = new ControlFlowVariables(initialiserState);
      breakVariables    = new HashMap<BreakableStatement, ControlFlowVariables>();
      continueVariables = new HashMap<BreakableStatement, ControlFlowVariables>();
      terminatedVariables = new ControlFlowVariables(initialiserState);
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
