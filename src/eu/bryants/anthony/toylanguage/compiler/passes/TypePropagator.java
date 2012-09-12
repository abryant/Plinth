package eu.bryants.anthony.toylanguage.compiler.passes;

import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.TypeDefinition;
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
import eu.bryants.anthony.toylanguage.ast.expression.NullCoalescingExpression;
import eu.bryants.anthony.toylanguage.ast.expression.NullLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ShiftExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ThisExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleIndexExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.member.ArrayLengthMember;
import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.member.Method;
import eu.bryants.anthony.toylanguage.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.misc.BlankAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.FieldAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.misc.VariableAssignee;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.statement.BreakStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ContinueStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ExpressionStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ForStatement;
import eu.bryants.anthony.toylanguage.ast.statement.IfStatement;
import eu.bryants.anthony.toylanguage.ast.statement.PrefixIncDecStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ReturnStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ShorthandAssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Statement;
import eu.bryants.anthony.toylanguage.ast.statement.WhileStatement;
import eu.bryants.anthony.toylanguage.ast.type.FunctionType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.toylanguage.ast.type.TupleType;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.ast.type.VoidType;

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
      for (Constructor constructor : typeDefinition.getConstructors())
      {
        propagateTypes(constructor.getBlock(), new VoidType(null));
      }
      // TODO: propagate types on Field expressions, when fields can have expressions
      for (Method method : typeDefinition.getAllMethods())
      {
        propagateTypes(method.getBlock(), method.getReturnType());
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
    else if (statement instanceof ExpressionStatement)
    {
      Expression expression = ((ExpressionStatement) statement).getExpression();
      // use the type of the expression
      // since the only possible ExpressionStatements are FunctionCallExpressions,
      // and functions always have user-specified return types, this should always be correct
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
      propagateTypes(returnStatement.getExpression(), returnType);
    }
    else if (statement instanceof ShorthandAssignStatement)
    {
      ShorthandAssignStatement shorthandAssignStatement = (ShorthandAssignStatement) statement;
      Assignee[] assignees = shorthandAssignStatement.getAssignees();
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
          propagateTypes(e, arrayCreationExpression.getType().getBaseType());
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
    else if (expression instanceof ComparisonExpression)
    {
      ComparisonExpression comparisonExpression = (ComparisonExpression) expression;
      propagateTypes(comparisonExpression.getLeftSubExpression(), comparisonExpression.getComparisonType());
      propagateTypes(comparisonExpression.getRightSubExpression(), comparisonExpression.getComparisonType());
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
    else if (expression instanceof ShiftExpression)
    {
      ShiftExpression shiftExpression = (ShiftExpression) expression;
      Type shiftType = shiftExpression.getType();
      propagateTypes(shiftExpression.getLeftExpression(), shiftType);
      propagateTypes(shiftExpression.getRightExpression(), shiftType);
    }
    else if (expression instanceof ThisExpression)
    {
      // do nothing
    }
    else if (expression instanceof TupleExpression)
    {
      TupleExpression tupleExpression = (TupleExpression) expression;
      Expression[] subExpressions = tupleExpression.getSubExpressions();
      // propagate the parent's type down to the sub-expressions
      TupleType propagatedType = (TupleType) type;
      tupleExpression.setType(propagatedType);
      Type[] subTypes = propagatedType.getSubTypes();
      for (int i = 0; i < subTypes.length; ++i)
      {
        propagateTypes(subExpressions[i], subTypes[i]);
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
