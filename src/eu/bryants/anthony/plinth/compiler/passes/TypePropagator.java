package eu.bryants.anthony.plinth.compiler.passes;

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
import eu.bryants.anthony.plinth.ast.member.ArrayLengthMember;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Initialiser;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.metadata.FieldInitialiser;
import eu.bryants.anthony.plinth.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.plinth.ast.misc.Assignee;
import eu.bryants.anthony.plinth.ast.misc.BlankAssignee;
import eu.bryants.anthony.plinth.ast.misc.FieldAssignee;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
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
import eu.bryants.anthony.plinth.ast.statement.WhileStatement;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.VoidType;

/*
 * Created on 17 Jul 2012
 */

/**
 * This pass propagates types back up the hierarchy after they have been checked.
 * While the type checker uses the types of the leaf expressions to discover the types of the non-leaf expressions, the
 * type propagator uses the inferred types of these non-leaf expressions to work out the real types of the leaf
 * expressions.
 * For example, the type of 'null' is a NullType, which does not have any direct translation to LLVM code (it could be
 * a simple pointer, or for '?uint' it could be a tuple of a boolean and an integer).
 * The type propagator eliminates all NullTypes by propagating the type information back down to the leaf nodes.
 *
 * @author Anthony Bryant
 */
public class TypePropagator
{
  public static void propagateTypes(CompilationUnit compilationUnit)
  {
    for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
    {
      for (Initialiser initialiser : typeDefinition.getInitialisers())
      {
        if (initialiser instanceof FieldInitialiser)
        {
          Field field = ((FieldInitialiser) initialiser).getField();
          propagateTypes(field.getInitialiserExpression(), field.getType());
        }
        else
        {
          propagateTypes(initialiser.getBlock(), VoidType.VOID_TYPE);
        }
      }
      for (Constructor constructor : typeDefinition.getConstructors())
      {
        propagateTypes(constructor.getBlock(), new VoidType(null));
      }
      for (Method method : typeDefinition.getAllMethods())
      {
        if (method.getBlock() != null)
        {
          propagateTypes(method.getBlock(), method.getReturnType());
        }
      }
    }
  }

  private static void propagateTypes(Statement statement, Type returnType)
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assignStatement = (AssignStatement) statement;
      Assignee[] assignees = assignStatement.getAssignees();
      for (int i = 0; i < assignees.length; ++i)
      {
        if (assignees[i] instanceof VariableAssignee)
        {
          // nothing to propagate
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          // propagate with the expression's type here, since the TypeChecker has already made sure it is an ArrayType (and we have no better type to use)
          propagateTypes(arrayElementAssignee.getArrayExpression(), arrayElementAssignee.getArrayExpression().getType());
          propagateTypes(arrayElementAssignee.getDimensionExpression(), ArrayLengthMember.ARRAY_LENGTH_TYPE);
        }
        else if (assignees[i] instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];
          propagateTypes(fieldAssignee.getFieldAccessExpression(), fieldAssignee.getResolvedType());
        }
        else if (assignees[i] instanceof BlankAssignee)
        {
          // nothing to propagate
        }
        else
        {
          throw new IllegalStateException("Unknown Assignee type: " + assignees[i]);
        }
      }
      if (assignStatement.getExpression() != null)
      {
        Type type = assignStatement.getResolvedType();
        propagateTypes(assignStatement.getExpression(), type);
      }
    }
    else if (statement instanceof Block)
    {
      for (Statement s : ((Block) statement).getStatements())
      {
        propagateTypes(s, returnType);
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
      Parameter[] parameters = delegateConstructorStatement.getResolvedConstructor().getParameters();
      Expression[] arguments = delegateConstructorStatement.getArguments();
      // propagate the parameter types to the arguments
      for (int i = 0; i < parameters.length; ++i)
      {
        propagateTypes(arguments[i], parameters[i].getType());
      }
    }
    else if (statement instanceof ExpressionStatement)
    {
      Expression expression = ((ExpressionStatement) statement).getExpression();
      // use the type of the expression
      // since the only possible ExpressionStatements are FunctionCallExpressions and ClassCreationExpressions,
      // and these both always have user-specified return types, this should always be correct
      propagateTypes(expression, expression.getType());
    }
    else if (statement instanceof ForStatement)
    {
      ForStatement forStatement = (ForStatement) statement;
      if (forStatement.getInitStatement() != null)
      {
        propagateTypes(forStatement.getInitStatement(), returnType);
      }
      if (forStatement.getConditional() != null)
      {
        propagateTypes(forStatement.getConditional(), new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null));
      }
      if (forStatement.getUpdateStatement() != null)
      {
        propagateTypes(forStatement.getUpdateStatement(), returnType);
      }
      propagateTypes(forStatement.getBlock(), returnType);
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      propagateTypes(ifStatement.getExpression(), new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null));
      propagateTypes(ifStatement.getThenClause(), returnType);
      if (ifStatement.getElseClause() != null)
      {
        propagateTypes(ifStatement.getElseClause(), returnType);
      }
    }
    else if (statement instanceof PrefixIncDecStatement)
    {
      PrefixIncDecStatement prefixIncDecStatement = (PrefixIncDecStatement) statement;
      Assignee assignee = prefixIncDecStatement.getAssignee();
      if (assignee instanceof VariableAssignee)
      {
        // nothing to propagate
      }
      else if (assignee instanceof ArrayElementAssignee)
      {
        ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
        // propagate with the expression's type here, since the TypeChecker has already made sure it is an ArrayType (and we have no better type to use)
        propagateTypes(arrayElementAssignee.getArrayExpression(), arrayElementAssignee.getArrayExpression().getType());
        propagateTypes(arrayElementAssignee.getDimensionExpression(), ArrayLengthMember.ARRAY_LENGTH_TYPE);
      }
      else if (assignee instanceof FieldAssignee)
      {
        FieldAssignee fieldAssignee = (FieldAssignee) assignee;
        propagateTypes(fieldAssignee.getFieldAccessExpression(), fieldAssignee.getResolvedType());
      }
      else
      {
        // ignore blank assignees, they shouldn't be able to get through variable resolution
        throw new IllegalStateException("Unknown Assignee type: " + assignee);
      }
    }
    else if (statement instanceof ReturnStatement)
    {
      ReturnStatement returnStatement = (ReturnStatement) statement;
      if (!(returnType instanceof VoidType))
      {
        propagateTypes(returnStatement.getExpression(), returnType);
      }
    }
    else if (statement instanceof ShorthandAssignStatement)
    {
      ShorthandAssignStatement shorthandAssignStatement = (ShorthandAssignStatement) statement;
      Assignee[] assignees = shorthandAssignStatement.getAssignees();
      Type[] types = new Type[assignees.length];
      for (int i = 0; i < assignees.length; ++i)
      {
        if (assignees[i] instanceof VariableAssignee)
        {
          // nothing to propagate
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          // propagate with the expression's type here, since the TypeChecker has already made sure it is an ArrayType (and we have no better type to use)
          propagateTypes(arrayElementAssignee.getArrayExpression(), arrayElementAssignee.getArrayExpression().getType());
          propagateTypes(arrayElementAssignee.getDimensionExpression(), ArrayLengthMember.ARRAY_LENGTH_TYPE);
        }
        else if (assignees[i] instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];
          propagateTypes(fieldAssignee.getFieldAccessExpression(), fieldAssignee.getResolvedType());
        }
        else
        {
          // ignore blank assignees, they shouldn't be able to get through variable resolution
          throw new IllegalStateException("Unknown Assignee type: " + assignees[i]);
        }
        types[i] = assignees[i].getResolvedType();
      }
      Type expressionType = shorthandAssignStatement.getExpression().getType();
      if (expressionType instanceof TupleType && !expressionType.isNullable() && ((TupleType) expressionType).getSubTypes().length == assignees.length)
      {
        // the expression is distributed over the assignees, so propagate the assignee types back up
        TupleType assigneeTypes = new TupleType(false, types, null);
        propagateTypes(shorthandAssignStatement.getExpression(), assigneeTypes);
      }
      else if (assignees.length == 1)
      {
        // there is only a single assignee, so propagate its type
        propagateTypes(shorthandAssignStatement.getExpression(), types[0]);
      }
      else
      {
        // there are multiple assignees, none of which has an ultimate type which should be propagated, so just propagate the expression type (since we have no better type to use)
        propagateTypes(shorthandAssignStatement.getExpression(), expressionType);
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      propagateTypes(whileStatement.getExpression(), new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null));
      propagateTypes(whileStatement.getStatement(), returnType);
    }
    else
    {
      throw new IllegalStateException("Unknown statement type: " + statement);
    }
  }

  private static void propagateTypes(Expression expression, Type type)
  {
    if (expression instanceof ArithmeticExpression)
    {
      ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;
      Type arithmeticType = arithmeticExpression.getType();
      propagateTypes(arithmeticExpression.getLeftSubExpression(), arithmeticType);
      propagateTypes(arithmeticExpression.getRightSubExpression(), arithmeticType);
    }
    else if (expression instanceof ArrayAccessExpression)
    {
      ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
      // propagate with the expression's type here, since the TypeChecker has already made sure it is an ArrayType (and we have no better type to use)
      propagateTypes(arrayAccessExpression.getArrayExpression(), arrayAccessExpression.getArrayExpression().getType());
      propagateTypes(arrayAccessExpression.getDimensionExpression(), ArrayLengthMember.ARRAY_LENGTH_TYPE);
    }
    else if (expression instanceof ArrayCreationExpression)
    {
      ArrayCreationExpression arrayCreationExpression = (ArrayCreationExpression) expression;
      if (arrayCreationExpression.getDimensionExpressions() != null)
      {
        for (Expression e : arrayCreationExpression.getDimensionExpressions())
        {
          propagateTypes(e, ArrayLengthMember.ARRAY_LENGTH_TYPE);
        }
      }
      if (arrayCreationExpression.getValueExpressions() != null)
      {
        for (Expression e : arrayCreationExpression.getValueExpressions())
        {
          propagateTypes(e, ((ArrayType) arrayCreationExpression.getType()).getBaseType());
        }
      }
    }
    else if (expression instanceof BitwiseNotExpression)
    {
      propagateTypes(((BitwiseNotExpression) expression).getExpression(), expression.getType());
    }
    else if (expression instanceof BooleanLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof BooleanNotExpression)
    {
      propagateTypes(((BooleanNotExpression) expression).getExpression(), expression.getType());
    }
    else if (expression instanceof BracketedExpression)
    {
      BracketedExpression bracketedExpression = (BracketedExpression) expression;
      bracketedExpression.setType(type);
      // propagate the parent's type down to the sub-expression
      propagateTypes(bracketedExpression.getExpression(), type);
    }
    else if (expression instanceof CastExpression)
    {
      CastExpression castExpression = (CastExpression) expression;
      propagateTypes(castExpression.getExpression(), castExpression.getType());
    }
    else if (expression instanceof ClassCreationExpression)
    {
      ClassCreationExpression classCreationExpression = (ClassCreationExpression) expression;
      Constructor resolvedConstructor = classCreationExpression.getResolvedConstructor();
      Parameter[] parameters = resolvedConstructor.getParameters();
      Expression[] arguments = classCreationExpression.getArguments();
      if (parameters.length != arguments.length)
      {
        throw new IllegalStateException("A constructor call must have the same number of arguments as the Constructor has parameters (" + parameters.length + " parameters vs " + arguments.length + " arguments)");
      }
      for (int i = 0; i < parameters.length; ++i)
      {
        propagateTypes(arguments[i], parameters[i].getType());
      }
    }
    else if (expression instanceof EqualityExpression)
    {
      EqualityExpression equalityExpression = (EqualityExpression) expression;
      if (equalityExpression.getComparisonType() == null)
      {
        // we do not have a comparison type. this can only happen if both sub-types are integer PrimitiveTypes and
        // the TypeChecker has left the CodeGenerator to extend both types to a signed type which is 1 bit larger than the maximum of the sub-types' bit widths
        // in this case, we just propagate the sub-expressions' types down to them here
        propagateTypes(equalityExpression.getLeftSubExpression(), equalityExpression.getLeftSubExpression().getType());
        propagateTypes(equalityExpression.getRightSubExpression(), equalityExpression.getRightSubExpression().getType());
      }
      else
      {
        propagateTypes(equalityExpression.getLeftSubExpression(), equalityExpression.getComparisonType());
        propagateTypes(equalityExpression.getRightSubExpression(), equalityExpression.getComparisonType());
      }
    }
    else if (expression instanceof FieldAccessExpression)
    {
      FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) expression;
      if (fieldAccessExpression.getBaseExpression() != null)
      {
        // propagate with the expression's type here, since the TypeChecker has already made sure it has the specified field (and we have no better type to use)
        propagateTypes(fieldAccessExpression.getBaseExpression(), fieldAccessExpression.getBaseExpression().getType());
      }
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression functionCallExpression = (FunctionCallExpression) expression;
      Expression[] arguments = functionCallExpression.getArguments();
      Parameter[] parameters = null;
      Type[] parameterTypes = null;
      if (functionCallExpression.getResolvedMethod() != null)
      {
        if (functionCallExpression.getResolvedBaseExpression() != null)
        {
          // propagate with the expression's type here, since the TypeChecker has already made sure it has the specified field (and we have no better type to use)
          propagateTypes(functionCallExpression.getResolvedBaseExpression(), functionCallExpression.getResolvedBaseExpression().getType());
        }
        parameters = functionCallExpression.getResolvedMethod().getParameters();
      }
      else if (functionCallExpression.getResolvedConstructor() != null)
      {
        parameters = functionCallExpression.getResolvedConstructor().getParameters();
      }
      else if (functionCallExpression.getResolvedBaseExpression() != null)
      {
        Expression baseExpression = functionCallExpression.getResolvedBaseExpression();
        // propagate with the expression's type here, since the TypeChecker has already made sure it has the specified field (and we have no better type to use)
        Type baseType = baseExpression.getType();
        propagateTypes(baseExpression, baseType);

        parameterTypes = ((FunctionType) baseType).getParameterTypes();
      }
      else
      {
        throw new IllegalArgumentException("Unresolved function call: " + functionCallExpression);
      }
      if (parameterTypes == null)
      {
        parameterTypes = new Type[parameters.length];
        for (int i = 0; i < parameters.length; i++)
        {
          parameterTypes[i] = parameters[i].getType();
        }
      }

      // propagate each of the argument types
      for (int i = 0; i < arguments.length; i++)
      {
        propagateTypes(arguments[i], parameterTypes[i]);
      }
    }
    else if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIfExpression = (InlineIfExpression) expression;
      propagateTypes(inlineIfExpression.getCondition(), new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null));
      // propagate the parent's type down to the sub-expressions
      inlineIfExpression.setType(type);
      propagateTypes(inlineIfExpression.getThenExpression(), type);
      propagateTypes(inlineIfExpression.getElseExpression(), type);
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof LogicalExpression)
    {
      LogicalExpression logicalExpression = (LogicalExpression) expression;
      Type logicalType = logicalExpression.getType();
      propagateTypes(logicalExpression.getLeftSubExpression(), logicalType);
      propagateTypes(logicalExpression.getRightSubExpression(), logicalType);
    }
    else if (expression instanceof MinusExpression)
    {
      propagateTypes(((MinusExpression) expression).getExpression(), expression.getType());
    }
    else if (expression instanceof NullCoalescingExpression)
    {
      NullCoalescingExpression nullCoalescingExpression = (NullCoalescingExpression) expression;
      // propagate the parent's type down to the sub-expressions, accounting for nullability (the first sub-expression's type must be nullable)
      nullCoalescingExpression.setType(type);
      propagateTypes(nullCoalescingExpression.getNullableExpression(), TypeChecker.findTypeWithNullability(type, true));
      propagateTypes(nullCoalescingExpression.getAlternativeExpression(), type);
    }
    else if (expression instanceof NullLiteralExpression)
    {
      // we don't care if we get a NullType here, that just means that the value of this null is never used, so we just let the CodeGenerator generate it as such
      expression.setType(type);
    }
    else if (expression instanceof ObjectCreationExpression)
    {
      // do nothing
    }
    else if (expression instanceof RelationalExpression)
    {
      RelationalExpression relationalExpression = (RelationalExpression) expression;
      if (relationalExpression.getComparisonType() == null)
      {
        // we do not have a comparison type. this can only happen if both sub-types are integer PrimitiveTypes and
        // the TypeChecker has left the CodeGenerator to extend both types to a signed type which is 1 bit larger than the maximum of the sub-types' bit widths
        // in this case, we just propagate the sub-expressions' types down to them here
        propagateTypes(relationalExpression.getLeftSubExpression(), relationalExpression.getLeftSubExpression().getType());
        propagateTypes(relationalExpression.getRightSubExpression(), relationalExpression.getRightSubExpression().getType());
      }
      else
      {
        propagateTypes(relationalExpression.getLeftSubExpression(), relationalExpression.getComparisonType());
        propagateTypes(relationalExpression.getRightSubExpression(), relationalExpression.getComparisonType());
      }
    }
    else if (expression instanceof ShiftExpression)
    {
      ShiftExpression shiftExpression = (ShiftExpression) expression;
      Type shiftType = shiftExpression.getType();
      propagateTypes(shiftExpression.getLeftExpression(), shiftType);
      propagateTypes(shiftExpression.getRightExpression(), shiftType);
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
      Expression[] subExpressions = tupleExpression.getSubExpressions();
      // propagate the parent's type down to the sub-expressions, if it is a TupleType
      Type[] subTypes = null;
      if (type instanceof TupleType)
      {
        TupleType propagatedType = (TupleType) type;
        tupleExpression.setType(propagatedType);
        subTypes = propagatedType.getSubTypes();
      }
      for (int i = 0; i < subExpressions.length; ++i)
      {
        // if we don't have a sub-type here, just propagate the sub-expression's type, since we don't have any better choices here
        propagateTypes(subExpressions[i], subTypes == null ? subExpressions[i].getType() : subTypes[i]);
      }
    }
    else if (expression instanceof TupleIndexExpression)
    {
      TupleIndexExpression tupleIndexExpression = (TupleIndexExpression) expression;
      tupleIndexExpression.setType(type);
      TupleType oldTupleType = (TupleType) tupleIndexExpression.getExpression().getType();
      Type[] oldSubTypes = oldTupleType.getSubTypes();
      // propagate the parent's type down into the sub-expression, by replacing the individual type we are indexing in the tuple type before propagating it
      Type[] newSubTypes = new Type[oldSubTypes.length];
      System.arraycopy(oldSubTypes, 0, newSubTypes, 0, oldSubTypes.length);
      newSubTypes[tupleIndexExpression.getIndexLiteral().getValue().intValue() - 1] = type;
      TupleType newTupleType = new TupleType(oldTupleType.isNullable(), newSubTypes, null);
      propagateTypes(tupleIndexExpression.getExpression(), newTupleType);
    }
    else if (expression instanceof VariableExpression)
    {
      // do nothing
    }
    else
    {
      throw new IllegalArgumentException("Unknown expression type: " + expression);
    }
  }
}
