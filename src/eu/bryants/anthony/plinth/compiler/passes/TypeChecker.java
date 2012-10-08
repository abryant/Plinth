package eu.bryants.anthony.plinth.compiler.passes;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompilationUnit;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.expression.ArithmeticExpression;
import eu.bryants.anthony.plinth.ast.expression.ArithmeticExpression.ArithmeticOperator;
import eu.bryants.anthony.plinth.ast.expression.ArrayAccessExpression;
import eu.bryants.anthony.plinth.ast.expression.ArrayCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.BitwiseNotExpression;
import eu.bryants.anthony.plinth.ast.expression.BooleanLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.BooleanNotExpression;
import eu.bryants.anthony.plinth.ast.expression.BracketedExpression;
import eu.bryants.anthony.plinth.ast.expression.CastExpression;
import eu.bryants.anthony.plinth.ast.expression.ClassCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.EqualityExpression;
import eu.bryants.anthony.plinth.ast.expression.EqualityExpression.EqualityOperator;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.plinth.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.plinth.ast.expression.InlineIfExpression;
import eu.bryants.anthony.plinth.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.LogicalExpression;
import eu.bryants.anthony.plinth.ast.expression.LogicalExpression.LogicalOperator;
import eu.bryants.anthony.plinth.ast.expression.MinusExpression;
import eu.bryants.anthony.plinth.ast.expression.NullCoalescingExpression;
import eu.bryants.anthony.plinth.ast.expression.NullLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression.RelationalOperator;
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
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.metadata.FieldInitialiser;
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
import eu.bryants.anthony.plinth.ast.statement.ContinueStatement;
import eu.bryants.anthony.plinth.ast.statement.ExpressionStatement;
import eu.bryants.anthony.plinth.ast.statement.ForStatement;
import eu.bryants.anthony.plinth.ast.statement.IfStatement;
import eu.bryants.anthony.plinth.ast.statement.PrefixIncDecStatement;
import eu.bryants.anthony.plinth.ast.statement.ReturnStatement;
import eu.bryants.anthony.plinth.ast.statement.ShorthandAssignStatement;
import eu.bryants.anthony.plinth.ast.statement.ShorthandAssignStatement.ShorthandAssignmentOperator;
import eu.bryants.anthony.plinth.ast.statement.Statement;
import eu.bryants.anthony.plinth.ast.statement.WhileStatement;
import eu.bryants.anthony.plinth.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.NullType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.compiler.ConceptualException;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class TypeChecker
{
  public static void checkTypes(CompilationUnit compilationUnit) throws ConceptualException
  {
    for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
    {
      for (Initialiser initialiser : typeDefinition.getInitialisers())
      {
        checkTypes(initialiser);
      }
      for (Constructor constructor : typeDefinition.getConstructors())
      {
        checkTypes(constructor.getBlock(), VoidType.VOID_TYPE);
      }
      for (Field field : typeDefinition.getFields())
      {
        checkTypes(field);
      }
      for (Method method : typeDefinition.getAllMethods())
      {
        checkTypes(method.getBlock(), method.getReturnType());
      }
    }
  }

  private static void checkTypes(Initialiser initialiser) throws ConceptualException
  {
    if (initialiser instanceof FieldInitialiser)
    {
      Field field = ((FieldInitialiser) initialiser).getField();
      Type expressionType = checkTypes(field.getInitialiserExpression());
      if (!field.getType().canAssign(expressionType))
      {
        throw new ConceptualException("Cannot assign an expression of type " + expressionType + " to a field of type " + field.getType(), field.getLexicalPhrase());
      }
    }
    else
    {
      checkTypes(initialiser.getBlock(), VoidType.VOID_TYPE);
    }
  }

  private static void checkTypes(Field field) throws ConceptualException
  {
    if (!field.isStatic())
    {
      // allow any types on a non-static field
      return;
    }
    Type type = field.getType();
    if (!type.hasDefaultValue())
    {
      throw new ConceptualException("Static fields must always have a type which has a language-defined default value (e.g. 0 for uint). Consider making this field nullable.", type.getLexicalPhrase());
    }
  }

  private static void checkTypes(Statement statement, Type returnType) throws ConceptualException
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assignStatement = (AssignStatement) statement;
      Type declaredType = assignStatement.getType();
      Assignee[] assignees = assignStatement.getAssignees();
      boolean distributedTupleType = declaredType != null && declaredType instanceof TupleType && !declaredType.isNullable() && ((TupleType) declaredType).getSubTypes().length == assignees.length;
      Type[] tupledSubTypes;
      if (distributedTupleType)
      {
        // the type is distributed, so in the following statement:
        // (int, long) a, b;
        // a has type int, and b has type long
        // so set the tupledSubTypes array to the declared subTypes array
        tupledSubTypes = ((TupleType) declaredType).getSubTypes();
      }
      else
      {
        tupledSubTypes = new Type[assignees.length];
      }

      for (int i = 0; i < assignees.length; i++)
      {
        if (assignees[i] instanceof VariableAssignee)
        {
          VariableAssignee variableAssignee = (VariableAssignee) assignees[i];
          if (declaredType != null)
          {
            // we have a declared type, so check that the variable matches it
            if (!variableAssignee.getResolvedVariable().getType().isEquivalent(distributedTupleType ? tupledSubTypes[i] : declaredType))
            {
              throw new ConceptualException("The variable type '" + variableAssignee.getResolvedVariable().getType() + "' does not match the declared type '" + (distributedTupleType ? tupledSubTypes[i] : declaredType) + "'", assignees[i].getLexicalPhrase());
            }
          }
          if (!distributedTupleType)
          {
            tupledSubTypes[i] = variableAssignee.getResolvedVariable().getType();
          }
          variableAssignee.setResolvedType(distributedTupleType ? tupledSubTypes[i] : declaredType);
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          Type arrayType = checkTypes(arrayElementAssignee.getArrayExpression());
          if (!(arrayType instanceof ArrayType))
          {
            throw new ConceptualException("Array assignments are not defined for the type " + arrayType, arrayElementAssignee.getLexicalPhrase());
          }
          Type dimensionType = checkTypes(arrayElementAssignee.getDimensionExpression());
          if (!ArrayLengthMember.ARRAY_LENGTH_TYPE.canAssign(dimensionType))
          {
            throw new ConceptualException("Cannot use an expression of type " + dimensionType + " as an array dimension, or convert it to type " + ArrayLengthMember.ARRAY_LENGTH_TYPE, arrayElementAssignee.getDimensionExpression().getLexicalPhrase());
          }
          Type baseType = ((ArrayType) arrayType).getBaseType();
          if (declaredType != null)
          {
            // we have a declared type, so check that the array base type matches it
            if (!baseType.isEquivalent(distributedTupleType ? tupledSubTypes[i] : declaredType))
            {
              throw new ConceptualException("The array element type '" + baseType + "' does not match the declared type '" + (distributedTupleType ? tupledSubTypes[i] : declaredType) + "'", assignees[i].getLexicalPhrase());
            }
          }
          if (!distributedTupleType)
          {
            tupledSubTypes[i] = baseType;
          }
          arrayElementAssignee.setResolvedType(distributedTupleType ? tupledSubTypes[i] : declaredType);
        }
        else if (assignees[i] instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];
          FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
          if (fieldAccessExpression.isNullTraversing())
          {
            throw new IllegalStateException("An assignee cannot be null-traversing: " + fieldAssignee);
          }
          // no need to do the following type checking here, it has already been done during name resolution, in order to resolve the member (as long as this field access has a base expression, and not a base type)
          // Type type = checkTypes(fieldAccessExpression.getBaseExpression(), compilationUnit);
          Member member = fieldAccessExpression.getResolvedMember();
          Type type;
          if (member instanceof ArrayLengthMember)
          {
            throw new ConceptualException("Cannot assign to an array's length", fieldAssignee.getLexicalPhrase());
          }
          else if (member instanceof Field)
          {
            type = ((Field) member).getType();
          }
          else if (member instanceof Method)
          {
            throw new ConceptualException("Cannot assign to a method", fieldAssignee.getLexicalPhrase());
          }
          else
          {
            throw new IllegalStateException("Unknown member type in a FieldAccessExpression: " + member);
          }
          if (declaredType != null)
          {
            if (!type.isEquivalent(distributedTupleType ? tupledSubTypes[i] : declaredType))
            {
              throw new ConceptualException("The field type '" + type + "' does not match the declared type '" + (distributedTupleType ? tupledSubTypes[i] : declaredType) + "'", fieldAssignee.getLexicalPhrase());
            }
          }
          if (!distributedTupleType)
          {
            tupledSubTypes[i] = type;
          }
          fieldAssignee.setResolvedType(distributedTupleType ? tupledSubTypes[i] : declaredType);
        }
        else if (assignees[i] instanceof BlankAssignee)
        {
          // this assignee doesn't actually get assigned to,
          // but we need to make sure tupledSubTypes[i] has its type now, if possible
          if (!distributedTupleType && declaredType != null)
          {
            tupledSubTypes[i] = declaredType;
          }
          // if there is no declared type, then there must be an expression, so we leave tupledSubTypes[i] as null, so that we can fill it in later
          assignees[i].setResolvedType(distributedTupleType ? tupledSubTypes[i] : declaredType);
        }
        else
        {
          throw new IllegalStateException("Unknown Assignee type: " + assignees[i]);
        }
      }

      if (assignStatement.getExpression() == null)
      {
        // we definitely have a declared type here, so the assignees definitely all have their types set
        // so we don't need to do anything
      }
      else
      {
        Type exprType = checkTypes(assignStatement.getExpression());
        if (tupledSubTypes.length == 1)
        {
          if (tupledSubTypes[0] == null)
          {
            tupledSubTypes[0] = exprType;
          }
          if (!tupledSubTypes[0].canAssign(exprType))
          {
            throw new ConceptualException("Cannot assign an expression of type " + exprType + " to a variable of type " + tupledSubTypes[0], assignStatement.getLexicalPhrase());
          }
          assignees[0].setResolvedType(tupledSubTypes[0]);
          assignStatement.setResolvedType(tupledSubTypes[0]);
        }
        else
        {
          boolean assignable = exprType instanceof TupleType && ((TupleType) exprType).getSubTypes().length == tupledSubTypes.length;
          if (assignable)
          {
            TupleType exprTupleType = (TupleType) exprType;
            Type[] exprSubTypes = exprTupleType.getSubTypes();
            for (int i = 0; i < exprSubTypes.length; i++)
            {
              if (tupledSubTypes[i] == null)
              {
                tupledSubTypes[i] = exprSubTypes[i];
              }
              if (!tupledSubTypes[i].canAssign(exprSubTypes[i]))
              {
                assignable = false;
                break;
              }
              assignees[i].setResolvedType(tupledSubTypes[i]);
            }
          }
          if (!assignable)
          {
            StringBuffer buffer = new StringBuffer("(");
            for (int i = 0; i < tupledSubTypes.length; i++)
            {
              buffer.append(tupledSubTypes[i] == null ? "_" : tupledSubTypes[i]);
              if (i != tupledSubTypes.length - 1)
              {
                buffer.append(", ");
              }
            }
            buffer.append(")");
            throw new ConceptualException("Cannot assign an expression of type " + exprType + " to a tuple of type " + buffer, assignStatement.getLexicalPhrase());
          }
          assignStatement.setResolvedType(new TupleType(false, tupledSubTypes, null));
        }
      }
    }
    else if (statement instanceof Block)
    {
      for (Statement s : ((Block) statement).getStatements())
      {
        checkTypes(s, returnType);
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
      checkTypes(((ExpressionStatement) statement).getExpression());
    }
    else if (statement instanceof ForStatement)
    {
      ForStatement forStatement = (ForStatement) statement;
      Statement init = forStatement.getInitStatement();
      if (init != null)
      {
        checkTypes(init, returnType);
      }
      Expression condition = forStatement.getConditional();
      if (condition != null)
      {
        Type conditionType = checkTypes(condition);
        if (conditionType.isNullable() || !(conditionType instanceof PrimitiveType) || ((PrimitiveType) conditionType).getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
        {
          throw new ConceptualException("A conditional must be of type '" + PrimitiveTypeType.BOOLEAN.name + "', not '" + conditionType + "'", condition.getLexicalPhrase());
        }
      }
      Statement update = forStatement.getUpdateStatement();
      if (update != null)
      {
        checkTypes(update, returnType);
      }
      checkTypes(forStatement.getBlock(), returnType);
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      Type exprType = checkTypes(ifStatement.getExpression());
      if (exprType.isNullable() || !(exprType instanceof PrimitiveType) || ((PrimitiveType) exprType).getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
      {
        throw new ConceptualException("A conditional must be of type '" + PrimitiveTypeType.BOOLEAN.name + "', not '" + exprType + "'", ifStatement.getExpression().getLexicalPhrase());
      }
      checkTypes(ifStatement.getThenClause(), returnType);
      if (ifStatement.getElseClause() != null)
      {
        checkTypes(ifStatement.getElseClause(), returnType);
      }
    }
    else if (statement instanceof PrefixIncDecStatement)
    {
      PrefixIncDecStatement prefixIncDecStatement = (PrefixIncDecStatement) statement;
      Assignee assignee = prefixIncDecStatement.getAssignee();
      Type assigneeType;
      if (assignee instanceof VariableAssignee)
      {
        assigneeType = ((VariableAssignee) assignee).getResolvedVariable().getType();
        assignee.setResolvedType(assigneeType);
      }
      else if (assignee instanceof ArrayElementAssignee)
      {
        ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
        Type arrayType = checkTypes(arrayElementAssignee.getArrayExpression());
        if (!(arrayType instanceof ArrayType))
        {
          throw new ConceptualException("Array accesses are not defined for the type " + arrayType, arrayElementAssignee.getLexicalPhrase());
        }
        Type dimensionType = checkTypes(arrayElementAssignee.getDimensionExpression());
        if (!ArrayLengthMember.ARRAY_LENGTH_TYPE.canAssign(dimensionType))
        {
          throw new ConceptualException("Cannot use an expression of type " + dimensionType + " as an array dimension, or convert it to type " + ArrayLengthMember.ARRAY_LENGTH_TYPE, arrayElementAssignee.getDimensionExpression().getLexicalPhrase());
        }
        assigneeType = ((ArrayType) arrayType).getBaseType();
        assignee.setResolvedType(assigneeType);
      }
      else if (assignee instanceof FieldAssignee)
      {
        FieldAssignee fieldAssignee = (FieldAssignee) assignee;
        FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
        // no need to do the following type checking here, it has already been done during name resolution, in order to resolve the member (as long as this field access has a base expression, and not a base type)
        // Type type = checkTypes(fieldAccessExpression.getExpression(), compilationUnit);
        Member member = fieldAccessExpression.getResolvedMember();
        if (member instanceof ArrayLengthMember)
        {
          throw new ConceptualException("Cannot increment or decrement an array's length", fieldAssignee.getLexicalPhrase());
        }
        else if (member instanceof Field)
        {
          assigneeType = ((Field) member).getType();
        }
        else if (member instanceof Method)
        {
          throw new ConceptualException("Cannot increment or decrement a method", fieldAssignee.getLexicalPhrase());
        }
        else
        {
          throw new IllegalStateException("Unknown member type in a FieldAccessExpression: " + member);
        }
        fieldAssignee.setResolvedType(assigneeType);
      }
      else
      {
        // ignore blank assignees, they shouldn't be able to get through variable resolution
        throw new IllegalStateException("Unknown Assignee type: " + assignee);
      }
      if (assigneeType.isNullable() || !(assigneeType instanceof PrimitiveType) || ((PrimitiveType) assigneeType).getPrimitiveTypeType() == PrimitiveTypeType.BOOLEAN)
      {
        throw new ConceptualException("Cannot " + (prefixIncDecStatement.isIncrement() ? "inc" : "dec") + "rement an assignee of type " + assigneeType, assignee.getLexicalPhrase());
      }
    }
    else if (statement instanceof ReturnStatement)
    {
      Expression returnExpression = ((ReturnStatement) statement).getExpression();
      if (returnExpression == null)
      {
        if (!(returnType instanceof VoidType))
        {
          throw new ConceptualException("A non-void function cannot return with no value", statement.getLexicalPhrase());
        }
      }
      else
      {
        if (returnType instanceof VoidType)
        {
          throw new ConceptualException("A void function cannot return a value", statement.getLexicalPhrase());
        }
        Type exprType = checkTypes(returnExpression);
        if (!returnType.canAssign(exprType))
        {
          throw new ConceptualException("Cannot return an expression of type '" + exprType + "' from a function with return type '" + returnType + "'", statement.getLexicalPhrase());
        }
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
          VariableAssignee variableAssignee = (VariableAssignee) assignees[i];
          types[i] = variableAssignee.getResolvedVariable().getType();
          variableAssignee.setResolvedType(types[i]);
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          Type arrayType = checkTypes(arrayElementAssignee.getArrayExpression());
          if (!(arrayType instanceof ArrayType))
          {
            throw new ConceptualException("Array assignments are not defined for the type " + arrayType, arrayElementAssignee.getLexicalPhrase());
          }
          Type dimensionType = checkTypes(arrayElementAssignee.getDimensionExpression());
          if (!ArrayLengthMember.ARRAY_LENGTH_TYPE.canAssign(dimensionType))
          {
            throw new ConceptualException("Cannot use an expression of type " + dimensionType + " as an array dimension, or convert it to type " + ArrayLengthMember.ARRAY_LENGTH_TYPE, arrayElementAssignee.getDimensionExpression().getLexicalPhrase());
          }
          types[i] = ((ArrayType) arrayType).getBaseType();
          arrayElementAssignee.setResolvedType(types[i]);
        }
        else if (assignees[i] instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];
          FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
          // no need to do the following type checking here, it has already been done during name resolution, in order to resolve the member (as long as this field access has a base expression, and not a base type)
          // Type type = checkTypes(fieldAccessExpression.getExpression(), compilationUnit);
          Member member = fieldAccessExpression.getResolvedMember();
          if (member instanceof ArrayLengthMember)
          {
            throw new ConceptualException("Cannot assign to an array's length", fieldAssignee.getLexicalPhrase());
          }
          else if (member instanceof Field)
          {
            types[i] = ((Field) member).getType();
          }
          else if (member instanceof Method)
          {
            throw new ConceptualException("Cannot assign to a method", fieldAssignee.getLexicalPhrase());
          }
          else
          {
            throw new IllegalStateException("Unknown member type in a FieldAccessExpression: " + member);
          }
          fieldAssignee.setResolvedType(types[i]);
        }
        else if (assignees[i] instanceof BlankAssignee)
        {
          // this assignee doesn't actually get assigned to, so leave its type as null
          types[i] = null;
          assignees[i].setResolvedType(null);
        }
        else
        {
          throw new IllegalStateException("Unknown Assignee type: " + assignees[i]);
        }
      }
      Type expressionType = checkTypes(shorthandAssignStatement.getExpression());
      Type[] rightTypes;
      if (expressionType instanceof TupleType && !expressionType.isNullable() && ((TupleType) expressionType).getSubTypes().length == assignees.length)
      {
        TupleType expressionTupleType = (TupleType) expressionType;
        rightTypes = expressionTupleType.getSubTypes();
      }
      else
      {
        rightTypes = new Type[assignees.length];
        for (int i = 0; i < rightTypes.length; ++i)
        {
          rightTypes[i] = expressionType;
        }
      }

      ShorthandAssignmentOperator operator = shorthandAssignStatement.getOperator();
      for (int i = 0; i < assignees.length; ++i)
      {
        Type left = types[i];
        Type right = rightTypes[i];
        if (left == null)
        {
          // the left hand side is a blank assignee, so pretend it is the same type as the right hand side
          left = right;
          types[i] = left;
          assignees[i].setResolvedType(left);
        }
        if (operator == ShorthandAssignmentOperator.ADD && left.isEquivalent(SpecialTypeHandler.STRING_TYPE) && right.isEquivalent(SpecialTypeHandler.STRING_TYPE))
        {
          // do nothing, this is a shorthand string concatenation, which is allowed
        }
        else if ((left instanceof PrimitiveType) && (right instanceof PrimitiveType) && !left.isNullable() && !right.isNullable())
        {
          PrimitiveTypeType leftPrimitiveType = ((PrimitiveType) left).getPrimitiveTypeType();
          PrimitiveTypeType rightPrimitiveType = ((PrimitiveType) right).getPrimitiveTypeType();
          if (operator == ShorthandAssignmentOperator.AND || operator == ShorthandAssignmentOperator.OR || operator == ShorthandAssignmentOperator.XOR)
          {
            if (leftPrimitiveType.isFloating() || rightPrimitiveType.isFloating() || !left.canAssign(right))
            {
              throw new ConceptualException("The operator '" + operator + "' is not defined for types " + left + " and " + right, shorthandAssignStatement.getLexicalPhrase());
            }
          }
          else if (operator == ShorthandAssignmentOperator.ADD || operator == ShorthandAssignmentOperator.SUBTRACT ||
                   operator == ShorthandAssignmentOperator.MULTIPLY || operator == ShorthandAssignmentOperator.DIVIDE ||
                   operator == ShorthandAssignmentOperator.REMAINDER || operator == ShorthandAssignmentOperator.MODULO)
          {
            if (leftPrimitiveType == PrimitiveTypeType.BOOLEAN || rightPrimitiveType == PrimitiveTypeType.BOOLEAN || !left.canAssign(right))
            {
              throw new ConceptualException("The operator '" + operator + "' is not defined for types " + left + " and " + right, shorthandAssignStatement.getLexicalPhrase());
            }
          }
          else if (operator == ShorthandAssignmentOperator.LEFT_SHIFT || operator == ShorthandAssignmentOperator.RIGHT_SHIFT)
          {
            if (leftPrimitiveType.isFloating() || rightPrimitiveType.isFloating() ||
                leftPrimitiveType == PrimitiveTypeType.BOOLEAN || rightPrimitiveType == PrimitiveTypeType.BOOLEAN ||
                rightPrimitiveType.isSigned())
            {
              throw new ConceptualException("The operator '" + operator + "' is not defined for types " + left + " and " + right, shorthandAssignStatement.getLexicalPhrase());
            }
          }
          else
          {
            throw new IllegalStateException("Unknown shorthand assignment operator: " + operator);
          }
        }
        else
        {
          throw new ConceptualException("The operator '" + operator + "' is not defined for types " + left + " and " + right, shorthandAssignStatement.getLexicalPhrase());
        }
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      Type exprType = checkTypes(whileStatement.getExpression());
      if (exprType.isNullable() || !(exprType instanceof PrimitiveType) || ((PrimitiveType) exprType).getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
      {
        throw new ConceptualException("A conditional must be of type '" + PrimitiveTypeType.BOOLEAN.name + "', not '" + exprType + "'", whileStatement.getExpression().getLexicalPhrase());
      }
      checkTypes(whileStatement.getStatement(), returnType);
    }
    else
    {
      throw new ConceptualException("Internal type checking error: Unknown statement type", statement.getLexicalPhrase());
    }
  }

  /**
   * Checks the types on an Expression recursively.
   * This method should only be called on an Expression after the resolver has been run over that Expression
   * @param expression - the Expression to check the types on
   * @return the Type of the Expression
   * @throws ConceptualException - if a conceptual problem is encountered while checking the types
   */
  public static Type checkTypes(Expression expression) throws ConceptualException
  {
    if (expression instanceof ArithmeticExpression)
    {
      ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;
      Type leftType = checkTypes(arithmeticExpression.getLeftSubExpression());
      Type rightType = checkTypes(arithmeticExpression.getRightSubExpression());
      if ((leftType instanceof PrimitiveType) && (rightType instanceof PrimitiveType) && !leftType.isNullable() && !rightType.isNullable())
      {
        PrimitiveTypeType leftPrimitiveType = ((PrimitiveType) leftType).getPrimitiveTypeType();
        PrimitiveTypeType rightPrimitiveType = ((PrimitiveType) rightType).getPrimitiveTypeType();
        if (leftPrimitiveType != PrimitiveTypeType.BOOLEAN && rightPrimitiveType != PrimitiveTypeType.BOOLEAN)
        {
          Type resultType = findCommonSuperType(leftType, rightType);
          if (resultType != null)
          {
            arithmeticExpression.setType(resultType);
            return resultType;
          }
          // the type will now only be null if no conversion can be done, e.g. if leftType is UINT and rightType is INT
        }
      }
      if (arithmeticExpression.getOperator() == ArithmeticOperator.ADD && leftType.isEquivalent(SpecialTypeHandler.STRING_TYPE) && rightType.isEquivalent(SpecialTypeHandler.STRING_TYPE))
      {
        arithmeticExpression.setType(leftType);
        return leftType;
      }
      throw new ConceptualException("The operator '" + arithmeticExpression.getOperator() + "' is not defined for types '" + leftType + "' and '" + rightType + "'", arithmeticExpression.getLexicalPhrase());
    }
    else if (expression instanceof ArrayAccessExpression)
    {
      ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
      Type type = checkTypes(arrayAccessExpression.getArrayExpression());
      if (!(type instanceof ArrayType) || type.isNullable())
      {
        throw new ConceptualException("Array accesses are not defined for type " + type, arrayAccessExpression.getLexicalPhrase());
      }
      Type dimensionType = checkTypes(arrayAccessExpression.getDimensionExpression());
      if (!ArrayLengthMember.ARRAY_LENGTH_TYPE.canAssign(dimensionType))
      {
        throw new ConceptualException("Cannot use an expression of type " + dimensionType + " as an array dimension, or convert it to type " + ArrayLengthMember.ARRAY_LENGTH_TYPE, dimensionType.getLexicalPhrase());
      }
      Type baseType = ((ArrayType) type).getBaseType();
      arrayAccessExpression.setType(baseType);
      return baseType;
    }
    else if (expression instanceof ArrayCreationExpression)
    {
      ArrayCreationExpression creationExpression = (ArrayCreationExpression) expression;
      if (creationExpression.getDimensionExpressions() != null)
      {
        for (Expression e : creationExpression.getDimensionExpressions())
        {
          Type type = checkTypes(e);
          if (!ArrayLengthMember.ARRAY_LENGTH_TYPE.canAssign(type))
          {
            throw new ConceptualException("Cannot use an expression of type " + type + " as an array dimension, or convert it to type " + ArrayLengthMember.ARRAY_LENGTH_TYPE, e.getLexicalPhrase());
          }
        }
      }
      Type baseType = creationExpression.getType().getBaseType();
      if (creationExpression.getValueExpressions() == null)
      {
        if (!baseType.hasDefaultValue())
        {
          throw new ConceptualException("Cannot create an array of '" + baseType + "' without an initialiser.", creationExpression.getLexicalPhrase());
        }
      }
      else
      {
        for (Expression e : creationExpression.getValueExpressions())
        {
          Type type = checkTypes(e);
          if (!baseType.canAssign(type))
          {
            throw new ConceptualException("Cannot add an expression of type " + type + " to an array of type " + baseType, e.getLexicalPhrase());
          }
        }
      }
      return creationExpression.getType();
    }
    else if (expression instanceof BitwiseNotExpression)
    {
      Type type = checkTypes(((BitwiseNotExpression) expression).getExpression());
      if (type instanceof PrimitiveType && !type.isNullable())
      {
        PrimitiveTypeType primitiveTypeType = ((PrimitiveType) type).getPrimitiveTypeType();
        if (!primitiveTypeType.isFloating())
        {
          expression.setType(type);
          return type;
        }
      }
      throw new ConceptualException("The operator '~' is not defined for type '" + type + "'", expression.getLexicalPhrase());
    }
    else if (expression instanceof BooleanLiteralExpression)
    {
      Type type = new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof BooleanNotExpression)
    {
      Type type = checkTypes(((BooleanNotExpression) expression).getExpression());
      if (type instanceof PrimitiveType && !type.isNullable() && ((PrimitiveType) type).getPrimitiveTypeType() == PrimitiveTypeType.BOOLEAN)
      {
        expression.setType(type);
        return type;
      }
      throw new ConceptualException("The operator '!' is not defined for type '" + type + "'", expression.getLexicalPhrase());
    }
    else if (expression instanceof BracketedExpression)
    {
      Type type = checkTypes(((BracketedExpression) expression).getExpression());
      expression.setType(type);
      return type;
    }
    else if (expression instanceof CastExpression)
    {
      Type exprType = checkTypes(((CastExpression) expression).getExpression());
      Type castedType = expression.getType();
      if (exprType.canAssign(castedType) || castedType.canAssign(exprType))
      {
        // if the assignment works in reverse (i.e. the casted type can be assigned to the expression) then it can be casted back
        // (also allow it if the assignment works forwards, although really that should be a warning about an unnecessary cast)

        // return the type of the cast expression (it has already been set during parsing)
        return expression.getType();
      }
      if (exprType instanceof PrimitiveType && castedType instanceof PrimitiveType && !exprType.isNullable() && !castedType.isNullable())
      {
        // allow non-floating primitive types with the same bit count to be casted to each other
        PrimitiveTypeType exprPrimitiveTypeType = ((PrimitiveType) exprType).getPrimitiveTypeType();
        PrimitiveTypeType castedPrimitiveTypeType = ((PrimitiveType) castedType).getPrimitiveTypeType();
        if (!exprPrimitiveTypeType.isFloating() && !castedPrimitiveTypeType.isFloating() &&
            exprPrimitiveTypeType.getBitCount() == castedPrimitiveTypeType.getBitCount())
        {
          // return the type of the cast expression (it has already been set during parsing)
          return expression.getType();
        }
      }
      throw new ConceptualException("Cannot cast from '" + exprType + "' to '" + castedType + "'", expression.getLexicalPhrase());
    }
    else if (expression instanceof ClassCreationExpression)
    {
      ClassCreationExpression classCreationExpression = (ClassCreationExpression) expression;
      Type type = expression.getType();
      if (!(type instanceof NamedType))
      {
        throw new ConceptualException("Cannot use the 'new' operator on '" + type + "', it must be on a class definition", expression.getLexicalPhrase());
      }
      NamedType namedType = (NamedType) type;
      TypeDefinition resolvedTypeDefinition = namedType.getResolvedTypeDefinition();
      if (resolvedTypeDefinition == null || !(resolvedTypeDefinition instanceof ClassDefinition))
      {
        throw new ConceptualException("Cannot use the 'new' operator on '" + type + "', it must be on a class definition", expression.getLexicalPhrase());
      }
      Expression[] arguments = classCreationExpression.getArguments();
      Constructor constructor = classCreationExpression.getResolvedConstructor();
      Parameter[] parameters = constructor.getParameters();
      if (arguments.length != parameters.length)
      {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < parameters.length; i++)
        {
          buffer.append(parameters[i].getType());
          if (i != parameters.length - 1)
          {
            buffer.append(", ");
          }
        }
        throw new ConceptualException("The constructor '" + constructor.getName() + "(" + buffer + ")' is not defined to take " + arguments.length + " arguments", classCreationExpression.getLexicalPhrase());
      }
      for (int i = 0; i < arguments.length; ++i)
      {
        Type argumentType = checkTypes(arguments[i]);
        if (!parameters[i].getType().canAssign(argumentType))
        {
          throw new ConceptualException("Cannot pass an argument of type '" + argumentType + "' as a parameter of type '" + parameters[i].getType() + "'", arguments[i].getLexicalPhrase());
        }
      }
      return type;
    }
    else if (expression instanceof EqualityExpression)
    {
      EqualityExpression equalityExpression = (EqualityExpression) expression;
      EqualityOperator operator = equalityExpression.getOperator();
      Type leftType = checkTypes(equalityExpression.getLeftSubExpression());
      Type rightType = checkTypes(equalityExpression.getRightSubExpression());
      if ((leftType instanceof NullType && !rightType.isNullable()) ||
          (!leftType.isNullable() && rightType instanceof NullType))
      {
        throw new ConceptualException("Cannot perform a null check on a non-nullable type (the '" + operator + "' operator is not defined for types '" + leftType + "' and '" + rightType + "')", equalityExpression.getLexicalPhrase());
      }
      // if we return from checking this EqualityExpression, the result will always be a non-nullable boolean type
      Type resultType = new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null);
      equalityExpression.setType(resultType);

      // if one of the operands is always null (i.e. a NullType), annotate this EqualityExpression as a null check for the other operand
      if (leftType instanceof NullType)
      {
        equalityExpression.setNullCheckExpression(equalityExpression.getRightSubExpression());
        equalityExpression.setComparisonType(rightType);
        return resultType;
      }
      if (rightType instanceof NullType)
      {
        equalityExpression.setNullCheckExpression(equalityExpression.getLeftSubExpression());
        equalityExpression.setComparisonType(leftType);
        return resultType;
      }

      if (leftType instanceof NullType && rightType instanceof NullType)
      {
        // this is a silly edge case where we are just doing something like "null == null" or "null != (b ? null : null)",
        // but allow it anyway - the code generator can turn it into a constant true or false
        equalityExpression.setComparisonType(leftType);
        return resultType;
      }
      if ((leftType instanceof PrimitiveType) && (rightType instanceof PrimitiveType))
      {
        PrimitiveTypeType leftPrimitiveType = ((PrimitiveType) leftType).getPrimitiveTypeType();
        PrimitiveTypeType rightPrimitiveType = ((PrimitiveType) rightType).getPrimitiveTypeType();
        if (leftPrimitiveType != PrimitiveTypeType.BOOLEAN && rightPrimitiveType != PrimitiveTypeType.BOOLEAN &&
            !leftPrimitiveType.isFloating() && !rightPrimitiveType.isFloating())
        {
          // we avoid findCommonSuperType() in this case, because that would make a comparison between a long and a ulong use a float comparison, which is not what we want
          Type leftTestType = leftType;
          Type rightTestType = rightType;
          if (leftTestType.isNullable() || rightTestType.isNullable())
          {
            leftTestType = findTypeWithNullability(leftTestType, true);
            rightTestType = findTypeWithNullability(rightTestType, true);
          }
          if (leftTestType.canAssign(rightTestType))
          {
            equalityExpression.setComparisonType(leftType);
          }
          else if (rightType.canAssign(leftType))
          {
            equalityExpression.setComparisonType(rightType);
          }
          else
          {
            // comparisonType will be null if no conversion can be done, e.g. if leftType is UINT and rightType is INT
            // but since comparing numeric types should always be valid, we just set the comparisonType to null anyway
            // and let the code generator handle it by converting to larger signed types first
            equalityExpression.setComparisonType(null);
          }

          // comparing any integer types is always valid
          return resultType;
        }
      }
      Type commonSuperType = findCommonSuperType(leftType, rightType);
      if (commonSuperType != null)
      {
        equalityExpression.setComparisonType(commonSuperType);
        return resultType;
      }
      throw new ConceptualException("The '" + operator + "' operator is not defined for types '" + leftType + "' and '" + rightType + "'", equalityExpression.getLexicalPhrase());
    }
    else if (expression instanceof FieldAccessExpression)
    {
      FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) expression;
      if (fieldAccessExpression.getBaseExpression() != null)
      {
        // no need to do the following type check here, it has already been done during name resolution, in order to resolve the member (as long as this field access has a base expression, and not a base type)
        // Type type = checkTypes(fieldAccessExpression.getBaseExpression(), compilationUnit);
        Type baseExpressionType = fieldAccessExpression.getBaseExpression().getType();
        if (baseExpressionType.isNullable() && !fieldAccessExpression.isNullTraversing())
        {
          throw new ConceptualException("Cannot access the field '" + fieldAccessExpression.getFieldName() + "' on something which is nullable. Consider using the '?.' operator.", fieldAccessExpression.getLexicalPhrase());
        }
        if (!baseExpressionType.isNullable() && fieldAccessExpression.isNullTraversing())
        {
          throw new ConceptualException("Cannot use the null traversing field access operator '?.' on a non nullable expression", fieldAccessExpression.getLexicalPhrase());
        }
      }
      Member member = fieldAccessExpression.getResolvedMember();
      Type type;
      if (member instanceof Field)
      {
        type = ((Field) member).getType();
      }
      else if (member instanceof ArrayLengthMember)
      {
        type = ArrayLengthMember.ARRAY_LENGTH_TYPE;
      }
      else if (member instanceof Method)
      {
        // create a function type for this method
        Method method = (Method) member;
        if (!method.isStatic() && method.getContainingTypeDefinition() instanceof CompoundDefinition)
        {
          throw new ConceptualException("Cannot convert a non-static method on a compound type to a function type, as there is nowhere to store the compound value of 'this' to call the method on", fieldAccessExpression.getLexicalPhrase());
        }
        Parameter[] parameters = method.getParameters();
        Type[] parameterTypes = new Type[parameters.length];
        for (int i = 0; i < parameters.length; ++i)
        {
          parameterTypes[i] = parameters[i].getType();
        }
        type = new FunctionType(false, method.getReturnType(), parameterTypes, null);
      }
      else
      {
        throw new IllegalStateException("Unknown member type in a FieldAccessExpression: " + member);
      }
      if (fieldAccessExpression.getBaseExpression() != null && fieldAccessExpression.isNullTraversing())
      {
        // we checked earlier that the base expression is nullable in this case
        // so, since this is a null traversing field access, make the result type nullable
        type = findTypeWithNullability(type, true);
      }
      fieldAccessExpression.setType(type);
      return type;
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      String floatingString = ((FloatingLiteralExpression) expression).getLiteral().toString();
      if (Float.parseFloat(floatingString) == Double.parseDouble(floatingString))
      {
        // the value fits in a float, so that is its initial type (which will automatically be casted to double if necessary)
        Type type = new PrimitiveType(false, PrimitiveTypeType.FLOAT, null);
        expression.setType(type);
        return type;
      }
      Type type = new PrimitiveType(false, PrimitiveTypeType.DOUBLE, null);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression functionCallExpression = (FunctionCallExpression) expression;
      Expression[] arguments = functionCallExpression.getArguments();
      Parameter[] parameters = null;
      Type[] parameterTypes = null;
      String name = null;
      Type resultType;
      if (functionCallExpression.getResolvedMethod() != null)
      {
        if (functionCallExpression.getResolvedBaseExpression() != null)
        {
          Type type = checkTypes(functionCallExpression.getResolvedBaseExpression());
          if (type.isNullable() && !functionCallExpression.getResolvedNullTraversal())
          {
            throw new ConceptualException("Cannot access the method '" + functionCallExpression.getResolvedMethod().getName() + "' on something which is nullable. Consider using the '?.' operator.", functionCallExpression.getLexicalPhrase());
          }
          if (!type.isNullable() && functionCallExpression.getResolvedNullTraversal())
          {
            throw new ConceptualException("Cannot use the null traversing method call operator '?.' on a non nullable expression", functionCallExpression.getLexicalPhrase());
          }
          Set<Member> memberSet = type.getMembers(functionCallExpression.getResolvedMethod().getName());
          if (!memberSet.contains(functionCallExpression.getResolvedMethod()))
          {
            throw new ConceptualException("The method '" + functionCallExpression.getResolvedMethod().getName() + "' does not exist for type '" + type + "'", functionCallExpression.getLexicalPhrase());
          }
        }
        parameters = functionCallExpression.getResolvedMethod().getParameters();
        resultType = functionCallExpression.getResolvedMethod().getReturnType();
        if (functionCallExpression.getResolvedNullTraversal() && !(resultType instanceof VoidType))
        {
          // this is a null traversing method call, so make the result type nullable
          resultType = findTypeWithNullability(resultType, true);
        }
        name = functionCallExpression.getResolvedMethod().getName();
      }
      else if (functionCallExpression.getResolvedConstructor() != null)
      {
        parameters = functionCallExpression.getResolvedConstructor().getParameters();
        resultType = new NamedType(false, functionCallExpression.getResolvedConstructor().getContainingTypeDefinition());
        name = functionCallExpression.getResolvedConstructor().getName();
      }
      else if (functionCallExpression.getResolvedBaseExpression() != null)
      {
        Expression baseExpression = functionCallExpression.getResolvedBaseExpression();
        Type baseType = checkTypes(baseExpression);
        if (baseType.isNullable())
        {
          throw new ConceptualException("Cannot call a nullable function.", functionCallExpression.getLexicalPhrase());
        }
        if (!(baseType instanceof FunctionType))
        {
          throw new ConceptualException("Cannot call something which is not a function type, a method or a constructor", functionCallExpression.getLexicalPhrase());
        }
        parameterTypes = ((FunctionType) baseType).getParameterTypes();
        resultType = ((FunctionType) baseType).getReturnType();
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

      if (arguments.length != parameterTypes.length)
      {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < parameterTypes.length; i++)
        {
          buffer.append(parameterTypes[i]);
          if (i != parameterTypes.length - 1)
          {
            buffer.append(", ");
          }
        }
        throw new ConceptualException("The function '" + (name == null ? "" : name) + "(" + buffer + ")' is not defined to take " + arguments.length + " arguments", functionCallExpression.getLexicalPhrase());
      }

      for (int i = 0; i < arguments.length; i++)
      {
        Type type = checkTypes(arguments[i]);
        if (!parameterTypes[i].canAssign(type))
        {
          throw new ConceptualException("Cannot pass an argument of type '" + type + "' as a parameter of type '" + parameterTypes[i] + "'", arguments[i].getLexicalPhrase());
        }
      }
      functionCallExpression.setType(resultType);
      return resultType;
    }
    else if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIf = (InlineIfExpression) expression;
      Type conditionType = checkTypes(inlineIf.getCondition());
      if (!(conditionType instanceof PrimitiveType) || conditionType.isNullable() || ((PrimitiveType) conditionType).getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
      {
        throw new ConceptualException("A conditional must be of type '" + PrimitiveTypeType.BOOLEAN.name + "', not '" + conditionType + "'", inlineIf.getCondition().getLexicalPhrase());
      }
      Type thenType = checkTypes(inlineIf.getThenExpression());
      Type elseType = checkTypes(inlineIf.getElseExpression());
      Type resultType = findCommonSuperType(thenType, elseType);
      if (resultType != null)
      {
        inlineIf.setType(resultType);
        return resultType;
      }
      throw new ConceptualException("The types of the then and else clauses of this inline if expression are incompatible, they are: " + thenType + " and " + elseType, inlineIf.getLexicalPhrase());
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      BigInteger value = ((IntegerLiteralExpression) expression).getLiteral().getValue();
      PrimitiveTypeType primitiveTypeType;
      if (value.signum() < 0)
      {
        // the number must be signed
        // check that bitLength() < SIZE to find out which signed type to use
        // use strictly less than because bitLength() excludes the sign bit
        if (value.bitLength() < Byte.SIZE)
        {
          primitiveTypeType = PrimitiveTypeType.BYTE;
        }
        else if (value.bitLength() < Short.SIZE)
        {
          primitiveTypeType = PrimitiveTypeType.SHORT;
        }
        else if (value.bitLength() < Integer.SIZE)
        {
          primitiveTypeType = PrimitiveTypeType.INT;
        }
        else if (value.bitLength() < Long.SIZE)
        {
          primitiveTypeType = PrimitiveTypeType.LONG;
        }
        else
        {
          throw new ConceptualException("Integer literal will not fit into a long", expression.getLexicalPhrase());
        }
      }
      else
      {
        // the number is assumed to be unsigned
        // use a '<=' check against the size this time, because we don't need to store a sign bit
        if (value.bitLength() <= Byte.SIZE)
        {
          primitiveTypeType = PrimitiveTypeType.UBYTE;
        }
        else if (value.bitLength() <= Short.SIZE)
        {
          primitiveTypeType = PrimitiveTypeType.USHORT;
        }
        else if (value.bitLength() <= Integer.SIZE)
        {
          primitiveTypeType = PrimitiveTypeType.UINT;
        }
        else if (value.bitLength() <= Long.SIZE)
        {
          primitiveTypeType = PrimitiveTypeType.ULONG;
        }
        else
        {
          throw new ConceptualException("Integer literal will not fit into a ulong", expression.getLexicalPhrase());
        }
      }
      Type type = new PrimitiveType(false, primitiveTypeType, null);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof LogicalExpression)
    {
      LogicalExpression logicalExpression = (LogicalExpression) expression;
      Type leftType = checkTypes(logicalExpression.getLeftSubExpression());
      Type rightType = checkTypes(logicalExpression.getRightSubExpression());
      if ((leftType instanceof PrimitiveType) && (rightType instanceof PrimitiveType) && !leftType.isNullable() && !rightType.isNullable())
      {
        PrimitiveTypeType leftPrimitiveType = ((PrimitiveType) leftType).getPrimitiveTypeType();
        PrimitiveTypeType rightPrimitiveType = ((PrimitiveType) rightType).getPrimitiveTypeType();
        // disallow all floating types
        if (!leftPrimitiveType.isFloating() && !rightPrimitiveType.isFloating())
        {
          // disallow short-circuit operators for any types but boolean
          if (logicalExpression.getOperator() == LogicalOperator.SHORT_CIRCUIT_AND || logicalExpression.getOperator() == LogicalOperator.SHORT_CIRCUIT_OR)
          {
            if (leftPrimitiveType == PrimitiveTypeType.BOOLEAN && rightPrimitiveType == PrimitiveTypeType.BOOLEAN)
            {
              logicalExpression.setType(leftType);
              return leftType;
            }
            throw new ConceptualException("The short-circuit operator '" + logicalExpression.getOperator() + "' is not defined for types '" + leftType + "' and '" + rightType + "'", logicalExpression.getLexicalPhrase());
          }
          // allow all (non-short-circuit) boolean/integer operations if the types match
          if (leftPrimitiveType == rightPrimitiveType)
          {
            logicalExpression.setType(leftType);
            return leftType;
          }
          // both types are now integers or booleans
          // if one can be converted to the other (left -> right or right -> left), then do the conversion
          // we cannot use findCommonSuperType() here, because it could choose a floating point type for the result if e.g. the input types were long and ulong
          if (leftType.canAssign(rightType))
          {
            logicalExpression.setType(leftType);
            return leftType;
          }
          if (rightType.canAssign(leftType))
          {
            logicalExpression.setType(rightType);
            return rightType;
          }
          // handle types with the same bit count, which cannot be assigned to each other, but should be compatible for logical operators
          if (leftPrimitiveType.getBitCount() == rightPrimitiveType.getBitCount())
          {
            if (!leftPrimitiveType.isSigned())
            {
              logicalExpression.setType(leftType);
              return leftType;
            }
            if (!rightPrimitiveType.isSigned())
            {
              logicalExpression.setType(rightType);
              return rightType;
            }
          }
        }
      }
      throw new ConceptualException("The operator '" + logicalExpression.getOperator() + "' is not defined for types '" + leftType + "' and '" + rightType + "'", logicalExpression.getLexicalPhrase());
    }
    else if (expression instanceof MinusExpression)
    {
      Type type = checkTypes(((MinusExpression) expression).getExpression());
      if (type instanceof PrimitiveType && !type.isNullable())
      {
        PrimitiveTypeType primitiveTypeType = ((PrimitiveType) type).getPrimitiveTypeType();
        // allow the unary minus operator to automatically convert from unsigned to signed integer values
        if (primitiveTypeType == PrimitiveTypeType.UBYTE)
        {
          PrimitiveType signedType = new PrimitiveType(false, PrimitiveTypeType.BYTE, null);
          expression.setType(signedType);
          return signedType;
        }
        if (primitiveTypeType == PrimitiveTypeType.USHORT)
        {
          PrimitiveType signedType = new PrimitiveType(false, PrimitiveTypeType.SHORT, null);
          expression.setType(signedType);
          return signedType;
        }
        if (primitiveTypeType == PrimitiveTypeType.UINT)
        {
          PrimitiveType signedType = new PrimitiveType(false, PrimitiveTypeType.INT, null);
          expression.setType(signedType);
          return signedType;
        }
        if (primitiveTypeType == PrimitiveTypeType.ULONG)
        {
          PrimitiveType signedType = new PrimitiveType(false, PrimitiveTypeType.LONG, null);
          expression.setType(signedType);
          return signedType;
        }

        if (primitiveTypeType != PrimitiveTypeType.BOOLEAN)
        {
          expression.setType(type);
          return type;
        }
      }
      throw new ConceptualException("The unary operator '-' is not defined for type '" + type + "'", expression.getLexicalPhrase());
    }
    else if (expression instanceof NullCoalescingExpression)
    {
      NullCoalescingExpression nullCoalescingExpression = (NullCoalescingExpression) expression;
      Type nullableType = checkTypes(nullCoalescingExpression.getNullableExpression());
      if (!nullableType.isNullable())
      {
        throw new ConceptualException("The null-coalescing operator '?:' is not defined when the left hand side (here '" + nullableType + "') is not nullable", expression.getLexicalPhrase());
      }
      Type alternativeType = checkTypes(nullCoalescingExpression.getAlternativeExpression());
      if (nullableType instanceof NullType)
      {
        // if the left hand side has the null type, just use the right hand side's type as the result of the expression
        nullCoalescingExpression.setType(alternativeType);
        return alternativeType;
      }
      Type resultType = findCommonSuperType(findTypeWithNullability(nullableType, false), alternativeType);
      if (resultType == null)
      {
        throw new ConceptualException("The null-coalescing operator '?:' is not defined for the types '" + nullableType + "' and '" + alternativeType + "'", expression.getLexicalPhrase());
      }
      nullCoalescingExpression.setType(resultType);
      return resultType;
    }
    else if (expression instanceof NullLiteralExpression)
    {
      Type type = new NullType(null);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof RelationalExpression)
    {
      RelationalExpression relationalExpression = (RelationalExpression) expression;
      RelationalOperator operator = relationalExpression.getOperator();
      Type leftType = checkTypes(relationalExpression.getLeftSubExpression());
      Type rightType = checkTypes(relationalExpression.getRightSubExpression());
      if ((leftType instanceof PrimitiveType) && (rightType instanceof PrimitiveType) && !leftType.isNullable() && !rightType.isNullable())
      {
        PrimitiveTypeType leftPrimitiveType = ((PrimitiveType) leftType).getPrimitiveTypeType();
        PrimitiveTypeType rightPrimitiveType = ((PrimitiveType) rightType).getPrimitiveTypeType();
        if (leftPrimitiveType != PrimitiveTypeType.BOOLEAN && rightPrimitiveType != PrimitiveTypeType.BOOLEAN)
        {
          // we do not use findCommonSuperType() here, because that would make a comparison between a long and a ulong use a float comparison, which is not what we want
          if (leftType.canAssign(rightType))
          {
            relationalExpression.setComparisonType((PrimitiveType) leftType);
          }
          else if (rightType.canAssign(leftType))
          {
            relationalExpression.setComparisonType((PrimitiveType) rightType);
          }
          else
          {
            // comparisonType will be null if no conversion can be done, e.g. if leftType is UINT and rightType is INT
            // but since comparing numeric types should always be valid, we just set the comparisonType to null anyway
            // and let the code generator handle it by converting to larger signed types first
            relationalExpression.setComparisonType(null);
          }
          // comparing any numeric types is always valid
          Type resultType = new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null);
          relationalExpression.setType(resultType);
          return resultType;
        }
      }
      throw new ConceptualException("The '" + operator + "' operator is not defined for types '" + leftType + "' and '" + rightType + "'", relationalExpression.getLexicalPhrase());
    }
    else if (expression instanceof ShiftExpression)
    {
      ShiftExpression shiftExpression = (ShiftExpression) expression;
      Type leftType = checkTypes(shiftExpression.getLeftExpression());
      Type rightType = checkTypes(shiftExpression.getRightExpression());
      if (leftType instanceof PrimitiveType && rightType instanceof PrimitiveType && !leftType.isNullable() && !rightType.isNullable())
      {
        PrimitiveTypeType leftPrimitiveType = ((PrimitiveType) leftType).getPrimitiveTypeType();
        PrimitiveTypeType rightPrimitiveType = ((PrimitiveType) rightType).getPrimitiveTypeType();
        // disallow floating point types and booleans
        if (!leftPrimitiveType.isFloating() && !rightPrimitiveType.isFloating() &&
            leftPrimitiveType != PrimitiveTypeType.BOOLEAN && rightPrimitiveType != PrimitiveTypeType.BOOLEAN &&
            !rightPrimitiveType.isSigned())
        {
          // we know that both types are integers here, and the shift operator should always take the type of the left argument,
          // so we will later convert the right type to the left type, whatever it is
          shiftExpression.setType(leftType);
          return leftType;
        }
      }
      throw new ConceptualException("The operator '" + shiftExpression.getOperator() + "' is not defined for types '" + leftType + "' and '" + rightType + "'", shiftExpression.getLexicalPhrase());
    }
    else if (expression instanceof StringLiteralExpression)
    {
      // the string literal type will have been resolved by the Resolver, so just return it here
      return expression.getType();
    }
    else if (expression instanceof ThisExpression)
    {
      // the type has already been resolved by the Resolver
      return expression.getType();
    }
    else if (expression instanceof TupleExpression)
    {
      TupleExpression tupleExpression = (TupleExpression) expression;
      Expression[] subExpressions = tupleExpression.getSubExpressions();
      Type[] subTypes = new Type[subExpressions.length];
      for (int i = 0; i < subTypes.length; i++)
      {
        subTypes[i] = checkTypes(subExpressions[i]);
      }
      TupleType type = new TupleType(false, subTypes, null);
      tupleExpression.setType(type);
      return type;
    }
    else if (expression instanceof TupleIndexExpression)
    {
      TupleIndexExpression indexExpression = (TupleIndexExpression) expression;
      Type type = checkTypes(indexExpression.getExpression());
      if (!(type instanceof TupleType))
      {
        throw new ConceptualException("Cannot index into the non-tuple type: " + type, indexExpression.getLexicalPhrase());
      }
      if (type.isNullable())
      {
        throw new ConceptualException("Cannot index into a nullable tuple type: " + type, indexExpression.getLexicalPhrase());
      }
      TupleType tupleType = (TupleType) type;
      IntegerLiteral indexLiteral = indexExpression.getIndexLiteral();
      BigInteger value = indexLiteral.getValue();
      Type[] subTypes = tupleType.getSubTypes();
      // using 1 based indexing, do a bounds check and find the result type
      if (value.compareTo(BigInteger.valueOf(1)) < 0 || value.compareTo(BigInteger.valueOf(subTypes.length)) > 0)
      {
        throw new ConceptualException("Index " + value + " does not exist in a tuple of type " + tupleType, indexExpression.getLexicalPhrase());
      }
      Type indexType = subTypes[value.intValue() - 1];
      indexExpression.setType(indexType);
      return indexType;
    }
    else if (expression instanceof VariableExpression)
    {
      VariableExpression variableExpression = (VariableExpression) expression;
      Variable resolvedVariable = variableExpression.getResolvedVariable();
      if (resolvedVariable != null)
      {
        Type type = resolvedVariable.getType();
        expression.setType(type);
        return type;
      }
      Method resolvedMethod = variableExpression.getResolvedMethod();
      if (resolvedMethod != null)
      {
        // create a function type for this method
        if (!resolvedMethod.isStatic() && resolvedMethod.getContainingTypeDefinition() instanceof CompoundDefinition)
        {
          throw new ConceptualException("Cannot convert a non-static method on a compound type to a function type, as there is nowhere to store the compound value of 'this' to call the method on", expression.getLexicalPhrase());
        }
        Parameter[] parameters = resolvedMethod.getParameters();
        Type[] parameterTypes = new Type[parameters.length];
        for (int i = 0; i < parameters.length; ++i)
        {
          parameterTypes[i] = parameters[i].getType();
        }
        FunctionType type = new FunctionType(false, resolvedMethod.getReturnType(), parameterTypes, null);
        expression.setType(type);
        return type;
      }
    }
    throw new ConceptualException("Internal type checking error: Unknown expression type", expression.getLexicalPhrase());
  }

  /**
   * Finds the common super-type of the specified two types.
   * @param a - the first type
   * @param b - the second type
   * @return the common super-type of a and b, that both a and b can be assigned to
   */
  private static Type findCommonSuperType(Type a, Type b)
  {
    // first, account for single-element tuple types
    // these can be nested arbitrarily far, and can also be nullable
    // the common supertype is the type where we have the maximum degree of nesting of the two,
    // and a nested tuple is nullable iff it is nullable in at least one of the two types
    if ((a instanceof TupleType && ((TupleType) a).getSubTypes().length == 1) ||
        (b instanceof TupleType && ((TupleType) b).getSubTypes().length == 1))
    {
      List<TupleType> aTuples = new LinkedList<TupleType>();
      Type baseA = a;
      while (baseA instanceof TupleType && ((TupleType) baseA).getSubTypes().length == 1)
      {
        aTuples.add((TupleType) baseA);
        baseA = ((TupleType) baseA).getSubTypes()[0];
      }
      List<TupleType> bTuples = new LinkedList<TupleType>();
      Type baseB = b;
      while (baseB instanceof TupleType && ((TupleType) baseB).getSubTypes().length == 1)
      {
        bTuples.add((TupleType) baseB);
        baseB = ((TupleType) baseB).getSubTypes()[0];
      }
      TupleType[] aTupleArray = aTuples.toArray(new TupleType[aTuples.size()]);
      TupleType[] bTupleArray = bTuples.toArray(new TupleType[bTuples.size()]);
      Type current = findCommonSuperType(baseA, baseB);
      int tupleNesting = Math.max(aTupleArray.length, bTupleArray.length);
      for (int i = 0; i < tupleNesting; ++i)
      {
        boolean nullable = false;
        if (i < aTupleArray.length)
        {
          nullable |= aTupleArray[aTupleArray.length - 1 - i].isNullable();
        }
        if (i < bTupleArray.length)
        {
          nullable |= bTupleArray[bTupleArray.length - 1 - i].isNullable();
        }
        current = new TupleType(nullable, new Type[] {current}, null);
      }
      return current;
    }

    // try the obvious types first
    if (a.canAssign(b))
    {
      return a;
    }
    if (b.canAssign(a))
    {
      return b;
    }
    // if one of them is NullType, make the other nullable
    if (a instanceof NullType)
    {
      return findTypeWithNullability(b, true);
    }
    if (b instanceof NullType)
    {
      return findTypeWithNullability(a, true);
    }
    // if a nullable version of either can assign the other one, then return that nullable version
    Type nullA = findTypeWithNullability(a, true);
    if (nullA.canAssign(b))
    {
      return nullA;
    }
    Type nullB = findTypeWithNullability(b, true);
    if (nullB.canAssign(a))
    {
      return nullB;
    }
    if (a instanceof PrimitiveType && b instanceof PrimitiveType)
    {
      PrimitiveTypeType aType = ((PrimitiveType) a).getPrimitiveTypeType();
      PrimitiveTypeType bType = ((PrimitiveType) b).getPrimitiveTypeType();
      if (aType == PrimitiveTypeType.BOOLEAN || bType == PrimitiveTypeType.BOOLEAN || aType.isFloating() || bType.isFloating())
      {
        // if either of them was either floating point or boolean, we would have found any compatibilities above
        return null;
      }
      // check through the signed integer types for one which can assign both of them
      // the resulting type must be signed, because if a and b had the same signedness, we would have found a common supertype above

      // exclude the maximum bit width, because if one is signed and the other is unsigned, then they cannot both fit in the size allocated for either one of them
      int minWidth = Math.max(aType.getBitCount(), bType.getBitCount()) + 1;
      PrimitiveTypeType currentBest = null;
      for (PrimitiveTypeType typeType : PrimitiveTypeType.values())
      {
        if (typeType != PrimitiveTypeType.BOOLEAN && !typeType.isFloating() &&
            typeType.isSigned() && typeType.getBitCount() >= minWidth &&
            (currentBest == null || typeType.getBitCount() < currentBest.getBitCount()))
        {
          currentBest = typeType;
        }
      }
      if (currentBest == null)
      {
        currentBest = PrimitiveTypeType.FLOAT;
      }
      boolean nullable = a.isNullable() | b.isNullable();
      return new PrimitiveType(nullable, currentBest, null);
    }
    if (a instanceof ArrayType && b instanceof ArrayType)
    {
      // array types are only compatible if their base types are the same, so we would have found a common supertype above if one existed
      return null;
    }
    if (a instanceof FunctionType && b instanceof FunctionType)
    {
      // function types are only compatible if their parameter and return types are the same, so we would have found a common supertype above if one existed
      return null;
    }
    if (a instanceof NamedType && b instanceof NamedType)
    {
      // named types are only compatible if they are based on the same class (we don't have inheritance yet), so we would have found a common supertype above if one existed
      return null;
    }
    if (a instanceof TupleType && b instanceof TupleType)
    {
      // these TupleTypes must both have at least two elements, since we have handled all single-element tuples above already
      Type[] aSubTypes = ((TupleType) a).getSubTypes();
      Type[] bSubTypes = ((TupleType) b).getSubTypes();
      if (aSubTypes.length != bSubTypes.length)
      {
        return null;
      }
      Type[] commonSubTypes = new Type[aSubTypes.length];
      for (int i = 0; i < aSubTypes.length; ++i)
      {
        commonSubTypes[i] = findCommonSuperType(aSubTypes[i], bSubTypes[i]);
      }
      return new TupleType(a.isNullable() | b.isNullable(), commonSubTypes, null);
    }
    return null;
  }

  /**
   * Finds the equivalent of the specified type with the specified nullability.
   * @param type - the type to find the version of which has the specified nullability
   * @param nullable - true if the returned type should be nullable, false otherwise
   * @return the version of the specified type with the specified nullability, or the original type if it already has the requested nullability
   */
  public static Type findTypeWithNullability(Type type, boolean nullable)
  {
    if (type.isNullable() == nullable)
    {
      return type;
    }
    if (type instanceof ArrayType)
    {
      return new ArrayType(nullable, ((ArrayType) type).getBaseType(), null);
    }
    if (type instanceof FunctionType)
    {
      return new FunctionType(nullable, ((FunctionType) type).getReturnType(), ((FunctionType) type).getParameterTypes(), null);
    }
    if (type instanceof NamedType)
    {
      return new NamedType(nullable, ((NamedType) type).getResolvedTypeDefinition());
    }
    if (type instanceof PrimitiveType)
    {
      return new PrimitiveType(nullable, ((PrimitiveType) type).getPrimitiveTypeType(), null);
    }
    if (type instanceof TupleType)
    {
      return new TupleType(nullable, ((TupleType) type).getSubTypes(), null);
    }
    throw new IllegalArgumentException("Cannot find the " + (nullable ? "nullable" : "non-nullable") + " version of: " + type);
  }
}
