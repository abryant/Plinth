package eu.bryants.anthony.toylanguage.compiler.passes;

import java.math.BigInteger;

import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
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
import eu.bryants.anthony.toylanguage.ast.expression.ComparisonExpression.ComparisonOperator;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.LogicalExpression;
import eu.bryants.anthony.toylanguage.ast.expression.LogicalExpression.LogicalOperator;
import eu.bryants.anthony.toylanguage.ast.expression.MinusExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.member.ArrayLengthMember;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.misc.VariableAssignee;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.statement.BreakStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ContinueStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ExpressionStatement;
import eu.bryants.anthony.toylanguage.ast.statement.IfStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ReturnStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Statement;
import eu.bryants.anthony.toylanguage.ast.statement.WhileStatement;
import eu.bryants.anthony.toylanguage.ast.type.ArrayType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.toylanguage.ast.type.TupleType;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.compiler.ConceptualException;

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
    for (Function f : compilationUnit.getFunctions())
    {
      checkTypes(f.getBlock(), f, compilationUnit);
    }
  }

  private static void checkTypes(Statement statement, Function function, CompilationUnit compilationUnit) throws ConceptualException
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assignStatement = (AssignStatement) statement;
      Type declaredType = assignStatement.getType();
      Assignee[] assignees = assignStatement.getAssignees();
      boolean distributedTupleType = declaredType != null && declaredType instanceof TupleType && ((TupleType) declaredType).getSubTypes().length == assignees.length;
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
            // the type isn't being distributed, so check that
            tupledSubTypes[i] = variableAssignee.getResolvedVariable().getType();
          }
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          Type arrayType = checkTypes(arrayElementAssignee.getArrayExpression(), compilationUnit);
          if (!(arrayType instanceof ArrayType))
          {
            throw new ConceptualException("Array assignments are not defined for the type " + arrayType, arrayElementAssignee.getLexicalPhrase());
          }
          Type dimensionType = checkTypes(arrayElementAssignee.getDimensionExpression(), compilationUnit);
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
        }
        else
        {
          throw new IllegalStateException("Unknown Assignee type: " + assignees[i]);
        }
      }

      if (assignStatement.getExpression() == null)
      {
        assignStatement.setType(new TupleType(tupledSubTypes, null));
      }
      else
      {
        Type exprType = checkTypes(assignStatement.getExpression(), compilationUnit);
        if (tupledSubTypes.length == 1)
        {
          if (!tupledSubTypes[0].canAssign(exprType))
          {
            throw new ConceptualException("Cannot assign an expression of type " + exprType + " to a variable of type " + tupledSubTypes[0], assignStatement.getLexicalPhrase());
          }
          assignStatement.setType(tupledSubTypes[0]);
        }
        else
        {
          TupleType fullTupleType = new TupleType(tupledSubTypes, null);
          if (!fullTupleType.canAssign(exprType))
          {
            throw new ConceptualException("Cannot assign an expression of type " + exprType + " to a tuple of type " + fullTupleType, assignStatement.getLexicalPhrase());
          }
          assignStatement.setType(fullTupleType);
        }
      }
    }
    else if (statement instanceof Block)
    {
      for (Statement s : ((Block) statement).getStatements())
      {
        checkTypes(s, function, compilationUnit);
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
      checkTypes(((ExpressionStatement) statement).getExpression(), compilationUnit);
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      Type exprType = checkTypes(ifStatement.getExpression(), compilationUnit);
      if (!(exprType instanceof PrimitiveType) || ((PrimitiveType) exprType).getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
      {
        throw new ConceptualException("A conditional must be of type '" + PrimitiveTypeType.BOOLEAN.name + "', not '" + exprType + "'", ifStatement.getExpression().getLexicalPhrase());
      }
      checkTypes(ifStatement.getThenClause(), function, compilationUnit);
      if (ifStatement.getElseClause() != null)
      {
        checkTypes(ifStatement.getElseClause(), function, compilationUnit);
      }
    }
    else if (statement instanceof ReturnStatement)
    {
      Type exprType = checkTypes(((ReturnStatement) statement).getExpression(), compilationUnit);
      if (!function.getType().canAssign(exprType))
      {
        throw new ConceptualException("Cannot return an expression of type '" + exprType + "' from a function with return type '" + function.getType() + "'", statement.getLexicalPhrase());
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      Type exprType = checkTypes(whileStatement.getExpression(), compilationUnit);
      if (!(exprType instanceof PrimitiveType) || ((PrimitiveType) exprType).getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
      {
        throw new ConceptualException("A conditional must be of type '" + PrimitiveTypeType.BOOLEAN.name + "', not '" + exprType + "'", whileStatement.getExpression().getLexicalPhrase());
      }
      checkTypes(whileStatement.getStatement(), function, compilationUnit);
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
   * @param compilationUnit - the compilation unit containing the expression
   * @return the Type of the Expression
   * @throws ConceptualException - if a conceptual problem is encountered while checking the types
   */
  public static Type checkTypes(Expression expression, CompilationUnit compilationUnit) throws ConceptualException
  {
    if (expression instanceof ArithmeticExpression)
    {
      ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;
      Type leftType = checkTypes(arithmeticExpression.getLeftSubExpression(), compilationUnit);
      Type rightType = checkTypes(arithmeticExpression.getRightSubExpression(), compilationUnit);
      if ((leftType instanceof PrimitiveType) && (rightType instanceof PrimitiveType))
      {
        PrimitiveTypeType leftPrimitiveType = ((PrimitiveType) leftType).getPrimitiveTypeType();
        PrimitiveTypeType rightPrimitiveType = ((PrimitiveType) rightType).getPrimitiveTypeType();
        if (leftPrimitiveType != PrimitiveTypeType.BOOLEAN && rightPrimitiveType != PrimitiveTypeType.BOOLEAN)
        {
          if (leftType.canAssign(rightType))
          {
            arithmeticExpression.setType(leftType);
            return leftType;
          }
          if (rightType.canAssign(leftType))
          {
            arithmeticExpression.setType(rightType);
            return rightType;
          }
          // the type will now only be null if no conversion can be done, e.g. if leftType is UINT and rightType is INT
        }
      }
      throw new ConceptualException("The operator '" + arithmeticExpression.getOperator() + "' is not defined for types '" + leftType + "' and '" + rightType + "'", arithmeticExpression.getLexicalPhrase());
    }
    else if (expression instanceof ArrayAccessExpression)
    {
      ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
      Type type = checkTypes(arrayAccessExpression.getArrayExpression(), compilationUnit);
      if (!(type instanceof ArrayType))
      {
        throw new ConceptualException("Array accesses are not defined for type " + type, arrayAccessExpression.getLexicalPhrase());
      }
      Type dimensionType = checkTypes(arrayAccessExpression.getDimensionExpression(), compilationUnit);
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
          Type type = checkTypes(e, compilationUnit);
          if (!new PrimitiveType(PrimitiveTypeType.UINT, null).canAssign(type))
          {
            throw new ConceptualException("Cannot use an expression of type " + type + " as an array dimension, or convert it to type " + ArrayLengthMember.ARRAY_LENGTH_TYPE, e.getLexicalPhrase());
          }
        }
      }
      if (creationExpression.getValueExpressions() != null)
      {
        Type baseType = creationExpression.getType().getBaseType();
        for (Expression e : creationExpression.getValueExpressions())
        {
          Type type = checkTypes(e, compilationUnit);
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
      Type type = checkTypes(((BitwiseNotExpression) expression).getExpression(), compilationUnit);
      if (type instanceof PrimitiveType)
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
      Type type = new PrimitiveType(PrimitiveTypeType.BOOLEAN, null);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof BooleanNotExpression)
    {
      Type type = checkTypes(((BooleanNotExpression) expression).getExpression(), compilationUnit);
      if (type instanceof PrimitiveType && ((PrimitiveType) type).getPrimitiveTypeType() == PrimitiveTypeType.BOOLEAN)
      {
        expression.setType(type);
        return type;
      }
      throw new ConceptualException("The operator '!' is not defined for type '" + type + "'", expression.getLexicalPhrase());
    }
    else if (expression instanceof BracketedExpression)
    {
      Type type = checkTypes(((BracketedExpression) expression).getExpression(), compilationUnit);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof CastExpression)
    {
      Type exprType = checkTypes(((CastExpression) expression).getExpression(), compilationUnit);
      Type castedType = expression.getType();
      if (exprType.canAssign(castedType) || castedType.canAssign(exprType))
      {
        // if the assignment works in reverse (i.e. the casted type can be assigned to the expression) then it can be casted back
        // (also allow it if the assignment works forwards, although really that should be a warning about an unnecessary cast)

        // return the type of the cast expression (it has already been set during parsing)
        return expression.getType();
      }
      if (exprType instanceof PrimitiveType && castedType instanceof PrimitiveType)
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
    else if (expression instanceof ComparisonExpression)
    {
      ComparisonExpression comparisonExpression = (ComparisonExpression) expression;
      ComparisonOperator operator = comparisonExpression.getOperator();
      Type leftType = checkTypes(comparisonExpression.getLeftSubExpression(), compilationUnit);
      Type rightType = checkTypes(comparisonExpression.getRightSubExpression(), compilationUnit);
      if ((leftType instanceof PrimitiveType) && (rightType instanceof PrimitiveType))
      {
        PrimitiveTypeType leftPrimitiveType = ((PrimitiveType) leftType).getPrimitiveTypeType();
        PrimitiveTypeType rightPrimitiveType = ((PrimitiveType) rightType).getPrimitiveTypeType();
        if (leftPrimitiveType == PrimitiveTypeType.BOOLEAN && rightPrimitiveType == PrimitiveTypeType.BOOLEAN &&
            (comparisonExpression.getOperator() == ComparisonOperator.EQUAL || comparisonExpression.getOperator() == ComparisonOperator.NOT_EQUAL))
        {
          // comparing booleans is only valid when using '==' or '!='
          comparisonExpression.setComparisonType((PrimitiveType) leftType);
          PrimitiveType type = new PrimitiveType(PrimitiveTypeType.BOOLEAN, null);
          comparisonExpression.setType(type);
          return type;
        }
        if (leftPrimitiveType != PrimitiveTypeType.BOOLEAN && rightPrimitiveType != PrimitiveTypeType.BOOLEAN)
        {
          if (leftType.canAssign(rightType))
          {
            comparisonExpression.setComparisonType((PrimitiveType) leftType);
          }
          else if (rightType.canAssign(leftType))
          {
            comparisonExpression.setComparisonType((PrimitiveType) rightType);
          }
          else
          {
            // comparisonType will be null if no conversion can be done, e.g. if leftType is UINT and rightType is INT
            // but since comparing numeric types should always be valid, we just set the comparisonType to null anyway
            // and let the code generator handle it by converting to larger signed types first
            comparisonExpression.setComparisonType(null);
          }

          // comparing any numeric types is always valid
          Type resultType = new PrimitiveType(PrimitiveTypeType.BOOLEAN, null);
          comparisonExpression.setType(resultType);
          return resultType;
        }
      }
      throw new ConceptualException("The '" + operator + "' operator is not defined for types '" + leftType + "' and '" + rightType + "'", comparisonExpression.getLexicalPhrase());
    }
    else if (expression instanceof FieldAccessExpression)
    {
      FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) expression;
      // no need to do the following type checking here, it has already been done during name resolution, in order to resolve the member
      // Type type = checkTypes(fieldAccessExpression.getExpression(), compilationUnit);
      Member member = fieldAccessExpression.getResolvedMember();
      fieldAccessExpression.setType(member.getType());
      return member.getType();
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      String floatingString = ((FloatingLiteralExpression) expression).getLiteral().toString();
      if (Float.parseFloat(floatingString) == Double.parseDouble(floatingString))
      {
        // the value fits in a float, so that is its initial type (which will automatically be casted to double if necessary)
        Type type = new PrimitiveType(PrimitiveTypeType.FLOAT, null);
        expression.setType(type);
        return type;
      }
      Type type = new PrimitiveType(PrimitiveTypeType.DOUBLE, null);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression functionCallExpression = (FunctionCallExpression) expression;
      Expression[] arguments = functionCallExpression.getArguments();
      Function resolvedFunction = functionCallExpression.getResolvedFunction();
      Parameter[] parameters = resolvedFunction.getParameters();
      if (arguments.length != parameters.length)
      {
        throw new ConceptualException("Function '" + resolvedFunction.getName() + "' is not defined to take " + arguments.length + " arguments", functionCallExpression.getLexicalPhrase());
      }
      for (int i = 0; i < arguments.length; i++)
      {
        Type type = checkTypes(arguments[i], compilationUnit);
        if (!parameters[i].getType().canAssign(type))
        {
          throw new ConceptualException("Cannot pass an argument of type '" + type + "' as a parameter of type '" + parameters[i].getType() + "'", arguments[i].getLexicalPhrase());
        }
      }
      Type type = resolvedFunction.getType();
      functionCallExpression.setType(type);
      return type;
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
        else
        {
          primitiveTypeType = PrimitiveTypeType.LONG;
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
        else
        {
          primitiveTypeType = PrimitiveTypeType.ULONG;
        }
      }
      Type type = new PrimitiveType(primitiveTypeType, null);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof LogicalExpression)
    {
      LogicalExpression logicalExpression = (LogicalExpression) expression;
      Type leftType = checkTypes(logicalExpression.getLeftSubExpression(), compilationUnit);
      Type rightType = checkTypes(logicalExpression.getRightSubExpression(), compilationUnit);
      if ((leftType instanceof PrimitiveType) && (rightType instanceof PrimitiveType))
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
          // allow conversion from signed to unsigned values of the same bit length here
          if (leftPrimitiveType.getBitCount() == rightPrimitiveType.getBitCount())
          {
            if (leftPrimitiveType.isSigned() && !rightPrimitiveType.isSigned())
            {
              logicalExpression.setType(rightType);
              return rightType;
            }
            if (!leftPrimitiveType.isSigned() && rightPrimitiveType.isSigned())
            {
              logicalExpression.setType(leftType);
              return leftType;
            }
          }
        }
      }
      throw new ConceptualException("The operator '" + logicalExpression.getOperator() + "' is not defined for types '" + leftType + "' and '" + rightType + "'", logicalExpression.getLexicalPhrase());
    }
    else if (expression instanceof MinusExpression)
    {
      Type type = checkTypes(((MinusExpression) expression).getExpression(), compilationUnit);
      if (type instanceof PrimitiveType)
      {
        PrimitiveTypeType primitiveTypeType = ((PrimitiveType) type).getPrimitiveTypeType();
        // allow the unary minus operator to automatically convert from unsigned to signed integer values
        if (primitiveTypeType == PrimitiveTypeType.UBYTE)
        {
          PrimitiveType signedType = new PrimitiveType(PrimitiveTypeType.BYTE, null);
          expression.setType(signedType);
          return signedType;
        }
        if (primitiveTypeType == PrimitiveTypeType.USHORT)
        {
          PrimitiveType signedType = new PrimitiveType(PrimitiveTypeType.SHORT, null);
          expression.setType(signedType);
          return signedType;
        }
        if (primitiveTypeType == PrimitiveTypeType.UINT)
        {
          PrimitiveType signedType = new PrimitiveType(PrimitiveTypeType.INT, null);
          expression.setType(signedType);
          return signedType;
        }
        if (primitiveTypeType == PrimitiveTypeType.ULONG)
        {
          PrimitiveType signedType = new PrimitiveType(PrimitiveTypeType.LONG, null);
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
    else if (expression instanceof TupleExpression)
    {
      TupleExpression tupleExpression = (TupleExpression) expression;
      Expression[] subExpressions = tupleExpression.getSubExpressions();
      Type[] subTypes = new Type[subExpressions.length];
      for (int i = 0; i < subTypes.length; i++)
      {
        subTypes[i] = checkTypes(subExpressions[i], compilationUnit);
      }
      TupleType type = new TupleType(subTypes, null);
      tupleExpression.setType(type);
      return type;
    }
    else if (expression instanceof VariableExpression)
    {
      Type type = ((VariableExpression) expression).getResolvedVariable().getType();
      expression.setType(type);
      return type;
    }
    throw new ConceptualException("Internal type checking error: Unknown expression type", expression.getLexicalPhrase());
  }
}
