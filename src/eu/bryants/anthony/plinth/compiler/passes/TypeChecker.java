package eu.bryants.anthony.plinth.compiler.passes;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
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
import eu.bryants.anthony.plinth.ast.expression.InstanceOfExpression;
import eu.bryants.anthony.plinth.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.LogicalExpression;
import eu.bryants.anthony.plinth.ast.expression.LogicalExpression.LogicalOperator;
import eu.bryants.anthony.plinth.ast.expression.MinusExpression;
import eu.bryants.anthony.plinth.ast.expression.NullCoalescingExpression;
import eu.bryants.anthony.plinth.ast.expression.NullLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.ObjectCreationExpression;
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
import eu.bryants.anthony.plinth.ast.statement.ContinueStatement;
import eu.bryants.anthony.plinth.ast.statement.DelegateConstructorStatement;
import eu.bryants.anthony.plinth.ast.statement.ExpressionStatement;
import eu.bryants.anthony.plinth.ast.statement.ForStatement;
import eu.bryants.anthony.plinth.ast.statement.IfStatement;
import eu.bryants.anthony.plinth.ast.statement.PrefixIncDecStatement;
import eu.bryants.anthony.plinth.ast.statement.ReturnStatement;
import eu.bryants.anthony.plinth.ast.statement.ShorthandAssignStatement;
import eu.bryants.anthony.plinth.ast.statement.ShorthandAssignStatement.ShorthandAssignmentOperator;
import eu.bryants.anthony.plinth.ast.statement.Statement;
import eu.bryants.anthony.plinth.ast.statement.ThrowStatement;
import eu.bryants.anthony.plinth.ast.statement.TryStatement;
import eu.bryants.anthony.plinth.ast.statement.WhileStatement;
import eu.bryants.anthony.plinth.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.NullType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.compiler.CoalescedConceptualException;
import eu.bryants.anthony.plinth.compiler.ConceptualException;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class TypeChecker
{
  public static void checkTypes(TypeDefinition typeDefinition) throws ConceptualException
  {
    CoalescedConceptualException coalescedException = null;
    for (Initialiser initialiser : typeDefinition.getInitialisers())
    {
      try
      {
        checkTypes(initialiser);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
    }
    for (Constructor constructor : typeDefinition.getAllConstructors())
    {
      if (!constructor.getCallsDelegateConstructor() && typeDefinition instanceof ClassDefinition)
      {
        // this constructor does not call a delegate constructor, so we must make sure that if there is a superclass, it has a no-args constructor
        ClassDefinition superClassDefinition = ((ClassDefinition) typeDefinition).getSuperClassDefinition();
        if (superClassDefinition != null)
        {
          boolean hasNoArgsSuper = false;
          for (Constructor test : superClassDefinition.getUniqueConstructors())
          {
            // note: only non-selfish constructors can be called as super-constructors
            if (!test.isSelfish() && test.getParameters().length == 0)
            {
              hasNoArgsSuper = true;
            }
          }
          if (!hasNoArgsSuper)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("This constructor needs to explicitly call a super(...) constructor (as there are no super() constructors which are not selfish and take zero arguments)", constructor.getLexicalPhrase()));
          }
        }
      }
      for (NamedType thrownType : constructor.getCheckedThrownTypes())
      {
        if (!SpecialTypeHandler.THROWABLE_TYPE.canAssign(thrownType))
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The declared thrown type " + thrownType + " does not inherit from Throwable", thrownType.getLexicalPhrase()));
        }
      }
      for (NamedType thrownType : constructor.getUncheckedThrownTypes())
      {
        if (!SpecialTypeHandler.THROWABLE_TYPE.canAssign(thrownType))
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The declared thrown type " + thrownType + " does not inherit from Throwable", thrownType.getLexicalPhrase()));
        }
      }
      try
      {
        checkTypes(constructor.getBlock(), VoidType.VOID_TYPE);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
    }
    for (Field field : typeDefinition.getFields())
    {
      try
      {
        checkTypes(field);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
    }
    for (Method method : typeDefinition.getAllMethods())
    {
      for (NamedType thrownType : method.getCheckedThrownTypes())
      {
        if (!SpecialTypeHandler.THROWABLE_TYPE.canAssign(thrownType))
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The declared thrown type " + thrownType + " does not inherit from Throwable", thrownType.getLexicalPhrase()));
        }
      }
      for (NamedType thrownType : method.getUncheckedThrownTypes())
      {
        if (!SpecialTypeHandler.THROWABLE_TYPE.canAssign(thrownType))
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The declared thrown type " + thrownType + " does not inherit from Throwable", thrownType.getLexicalPhrase()));
        }
      }
      if (method.getBlock() != null)
      {
        try
        {
          checkTypes(method.getBlock(), method.getReturnType());
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

  /**
   * Checks that the thrown types of the specified FunctionType inherit from Throwable.
   * @param functionType - the FunctionType to check
   * @throws CoalescedConceptualException - if there is a problem with the FunctionType
   */
  public static void checkFunctionType(FunctionType functionType) throws CoalescedConceptualException
  {
    CoalescedConceptualException coalescedException = null;
    for (NamedType thrownType : functionType.getThrownTypes())
    {
      if (!SpecialTypeHandler.THROWABLE_TYPE.canAssign(thrownType))
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The declared thrown type " + thrownType + " does not inherit from Throwable", thrownType.getLexicalPhrase()));
      }
    }
    if (coalescedException != null)
    {
      throw coalescedException;
    }
  }

  /**
   * Checks the types on a Statement recursively.
   * This method should only be called on a Statement after the resolver has been run over that Statement
   * @param statement - the Statement to check the types on
   * @param returnType - the return type of the function containing this statement
   * @throws ConceptualException - if a conceptual problem is encountered while checking the types
   */
  public static void checkTypes(Statement statement, Type returnType) throws ConceptualException
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
      CoalescedConceptualException coalescedException = null;
      for (Statement s : ((Block) statement).getStatements())
      {
        try
        {
          checkTypes(s, returnType);
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
      Constructor constructor = delegateConstructorStatement.getResolvedConstructor();

      Parameter[] parameters = constructor.getParameters();
      Type[] parameterTypes = new Type[parameters.length];
      for (int i = 0; i < parameters.length; ++i)
      {
        parameterTypes[i] = parameters[i].getType();
      }
      Expression[] arguments = delegateConstructorStatement.getArguments();

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
        throw new ConceptualException("The constructor '" + constructor.getContainingTypeDefinition().getQualifiedName() + "(" + buffer + ")' is not defined to take " + arguments.length + " arguments", delegateConstructorStatement.getLexicalPhrase());
      }

      CoalescedConceptualException coalescedException = null;
      for (int i = 0; i < arguments.length; i++)
      {
        try
        {
          Type type = checkTypes(arguments[i]);
          if (!parameterTypes[i].canAssign(type))
          {
            throw new ConceptualException("Cannot pass an argument of type '" + type + "' as a parameter of type '" + parameterTypes[i] + "'", arguments[i].getLexicalPhrase());
          }
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
    else if (statement instanceof ExpressionStatement)
    {
      checkTypes(((ExpressionStatement) statement).getExpression());
    }
    else if (statement instanceof ForStatement)
    {
      ForStatement forStatement = (ForStatement) statement;

      CoalescedConceptualException coalescedException = null;
      Statement init = forStatement.getInitStatement();
      if (init != null)
      {
        try
        {
          checkTypes(init, returnType);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      Expression condition = forStatement.getConditional();
      if (condition != null)
      {
        try
        {
          Type conditionType = checkTypes(condition);
          if (conditionType.isNullable() || !(conditionType instanceof PrimitiveType) || ((PrimitiveType) conditionType).getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
          {
            throw new ConceptualException("A conditional must be of type '" + PrimitiveTypeType.BOOLEAN.name + "', not '" + conditionType + "'", condition.getLexicalPhrase());
          }
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      Statement update = forStatement.getUpdateStatement();
      if (update != null)
      {
        try
        {
          checkTypes(update, returnType);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      try
      {
        checkTypes(forStatement.getBlock(), returnType);
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
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      CoalescedConceptualException coalescedException = null;
      try
      {
        Type exprType = checkTypes(ifStatement.getExpression());
        if (exprType.isNullable() || !(exprType instanceof PrimitiveType) || ((PrimitiveType) exprType).getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
        {
          throw new ConceptualException("A conditional must be of type '" + PrimitiveTypeType.BOOLEAN.name + "', not '" + exprType + "'", ifStatement.getExpression().getLexicalPhrase());
        }
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkTypes(ifStatement.getThenClause(), returnType);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (ifStatement.getElseClause() != null)
      {
        try
        {
          checkTypes(ifStatement.getElseClause(), returnType);
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
      ReturnStatement returnStatement = (ReturnStatement) statement;
      Expression returnExpression = returnStatement.getExpression();
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
        Type resultType = exprType;
        if (returnStatement.getCanReturnAgainstContextualImmutability() && !(resultType instanceof VoidType))
        {
          // turn off contextual immutability (unless the type is explicitly immutable), so that only-contextually-immutable things can still be returned from immutable functions
          resultType = findTypeWithDeepContextualImmutability(resultType, false);
        }
        if (!returnType.canAssign(resultType))
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
        if (operator == ShorthandAssignmentOperator.ADD && left.isEquivalent(SpecialTypeHandler.STRING_TYPE) && !(right instanceof VoidType))
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
    else if (statement instanceof ThrowStatement)
    {
      ThrowStatement throwStatement = (ThrowStatement) statement;
      Type thrownType = checkTypes(throwStatement.getThrownExpression());
      if (!SpecialTypeHandler.THROWABLE_TYPE.canAssign(thrownType))
      {
        throw new ConceptualException("Cannot throw a value of type " + thrownType + " (it cannot be converted to " + SpecialTypeHandler.THROWABLE_TYPE + ")", throwStatement.getLexicalPhrase());
      }
    }
    else if (statement instanceof TryStatement)
    {
      TryStatement tryStatement = (TryStatement) statement;
      checkTypes(tryStatement.getTryBlock(), returnType);
      for (CatchClause catchClause : tryStatement.getCatchClauses())
      {
        // the resolver has already called checkCatchClauseTypes(), so the caught variable has already been type-checked
        checkTypes(catchClause.getBlock(), returnType);
      }
      if (tryStatement.getFinallyBlock() != null)
      {
        checkTypes(tryStatement.getFinallyBlock(), returnType);
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      CoalescedConceptualException coalescedException = null;
      try
      {
        Type exprType = checkTypes(whileStatement.getExpression());
        if (exprType.isNullable() || !(exprType instanceof PrimitiveType) || ((PrimitiveType) exprType).getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
        {
          throw new ConceptualException("A conditional must be of type '" + PrimitiveTypeType.BOOLEAN.name + "', not '" + exprType + "'", whileStatement.getExpression().getLexicalPhrase());
        }
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        checkTypes(whileStatement.getStatement(), returnType);
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
      Type leftType = null;
      Type rightType = null;
      CoalescedConceptualException coalescedException = null;
      try
      {
        leftType = checkTypes(arithmeticExpression.getLeftSubExpression());
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        rightType = checkTypes(arithmeticExpression.getRightSubExpression());
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
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
      if (arithmeticExpression.getOperator() == ArithmeticOperator.ADD && (leftType.isEquivalent(SpecialTypeHandler.STRING_TYPE) || rightType.isEquivalent(SpecialTypeHandler.STRING_TYPE)) &&
          !(leftType instanceof VoidType) && !(rightType instanceof VoidType))
      {
        // if either side of this addition expression is a string, make the result type string, so that both of them are converted to strings
        arithmeticExpression.setType(SpecialTypeHandler.STRING_TYPE);
        return SpecialTypeHandler.STRING_TYPE;
      }
      throw new ConceptualException("The operator '" + arithmeticExpression.getOperator() + "' is not defined for types '" + leftType + "' and '" + rightType + "'", arithmeticExpression.getLexicalPhrase());
    }
    else if (expression instanceof ArrayAccessExpression)
    {
      ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
      CoalescedConceptualException coalescedException = null;
      Type type = null;
      try
      {
        type = checkTypes(arrayAccessExpression.getArrayExpression());
        if (!(type instanceof ArrayType) || type.isNullable())
        {
          throw new ConceptualException("Array accesses are not defined for type " + type, arrayAccessExpression.getLexicalPhrase());
        }
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        Type dimensionType = checkTypes(arrayAccessExpression.getDimensionExpression());
        if (!ArrayLengthMember.ARRAY_LENGTH_TYPE.canAssign(dimensionType))
        {
          throw new ConceptualException("Cannot use an expression of type " + dimensionType + " as an array dimension, or convert it to type " + ArrayLengthMember.ARRAY_LENGTH_TYPE, arrayAccessExpression.getDimensionExpression().getLexicalPhrase());
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
      Type baseType = ((ArrayType) type).getBaseType();
      arrayAccessExpression.setType(baseType);
      return baseType;
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
            Type type = checkTypes(expr);
            if (!ArrayLengthMember.ARRAY_LENGTH_TYPE.canAssign(type))
            {
              throw new ConceptualException("Cannot use an expression of type " + type + " as an array dimension, or convert it to type " + ArrayLengthMember.ARRAY_LENGTH_TYPE, expr.getLexicalPhrase());
            }
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
      }
      ArrayType declaredType = creationExpression.getDeclaredType();
      Type baseType = declaredType.getBaseType();
      if (creationExpression.getValueExpressions() == null)
      {
        if (!baseType.hasDefaultValue())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot create an array of '" + baseType + "' without an initialiser.", creationExpression.getLexicalPhrase()));
        }
      }
      else
      {
        for (Expression expr : creationExpression.getValueExpressions())
        {
          try
          {
            Type type = checkTypes(expr);
            if (!baseType.canAssign(type))
            {
              throw new ConceptualException("Cannot add an expression of type " + type + " to an array of type " + baseType, expr.getLexicalPhrase());
            }
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
      creationExpression.setType(declaredType);
      return declaredType;
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
      if (type instanceof VoidType)
      {
        throw new ConceptualException("Cannot enclose a void value in brackets", expression.getLexicalPhrase());
      }
      expression.setType(type);
      return type;
    }
    else if (expression instanceof CastExpression)
    {
      Type exprType = checkTypes(((CastExpression) expression).getExpression());
      if (exprType instanceof VoidType)
      {
        throw new ConceptualException("Cannot perform a cast on void", expression.getLexicalPhrase());
      }
      Type castedType = expression.getType();
      // forbid casting away immutability (both explicit and contextual)
      boolean fromExplicitlyImmutable = (exprType instanceof ArrayType  && ((ArrayType)  exprType).isExplicitlyImmutable()) ||
                                        (exprType instanceof NamedType  && ((NamedType)  exprType).isExplicitlyImmutable()) ||
                                        (exprType instanceof ObjectType && ((ObjectType) exprType).isExplicitlyImmutable());
      boolean toExplicitlyImmutable = (castedType instanceof ArrayType  && ((ArrayType)  castedType).isExplicitlyImmutable()) ||
                                      (castedType instanceof NamedType  && ((NamedType)  castedType).isExplicitlyImmutable()) ||
                                      (castedType instanceof ObjectType && ((ObjectType) castedType).isExplicitlyImmutable());
      if (fromExplicitlyImmutable & !toExplicitlyImmutable)
      {
        throw new ConceptualException("Cannot cast away immutability, from '" + exprType + "' to '" + castedType + "'", expression.getLexicalPhrase());
      }
      boolean fromContextuallyImmutable = (exprType instanceof ArrayType  && ((ArrayType)  exprType).isContextuallyImmutable()) ||
                                          (exprType instanceof NamedType  && ((NamedType)  exprType).isContextuallyImmutable()) ||
                                          (exprType instanceof ObjectType && ((ObjectType) exprType).isContextuallyImmutable());
      boolean toContextuallyImmutable = (castedType instanceof ArrayType  && ((ArrayType)  castedType).isContextuallyImmutable()) ||
                                        (castedType instanceof NamedType  && ((NamedType)  castedType).isContextuallyImmutable()) ||
                                        (castedType instanceof ObjectType && ((ObjectType) castedType).isContextuallyImmutable());
      if (fromContextuallyImmutable & !toContextuallyImmutable)
      {
        throw new ConceptualException("Cannot cast away contextual immutability, from '" + exprType + "' to '" + castedType + "'", expression.getLexicalPhrase());
      }
      // NOTE: we allow casting function values to and from immutable, since the immutability constraints will be checked at run-time

      // we have checked the immutability constraints properly, so we can ignore them in this next check
      // we need to do this so that e.g. casts from A to #B work, if A is a supertype of B
      Type checkExprType = findTypeWithDataImmutability(exprType, toExplicitlyImmutable, toContextuallyImmutable);
      if (exprType instanceof FunctionType && castedType instanceof FunctionType)
      {
        FunctionType functionExprType = (FunctionType) checkExprType;
        // for function types, ignore the immutability constraint, allowing it to be checked at runtime (since function immutability is a property of the value, not the pointer)
        // also remove any constraints on the thrown types. while they are not checked at run-time, casting away the checked exceptions is equivalent to rethrowing them as unchecked
        if (functionExprType.isImmutable() != ((FunctionType) castedType).isImmutable() ||
            functionExprType.getThrownTypes().length > 0)
        {
          checkExprType = new FunctionType(functionExprType.isNullable(), ((FunctionType) castedType).isImmutable(), functionExprType.getReturnType(), functionExprType.getParameterTypes(), new NamedType[0], null);
        }
      }

      if (findTypeWithNullability(checkExprType, true).canAssign(castedType) || findTypeWithNullability(castedType, true).canAssign(checkExprType))
      {
        // if the assignment works in reverse (i.e. the casted type can be assigned to the expression) then it can be casted back
        // (also allow it if the assignment works forwards, although usually that should be a warning about an unnecessary cast, unless the cast allows access to a hidden field)

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
      if (exprType instanceof NamedType && castedType instanceof NamedType)
      {
        TypeDefinition exprDefinition = ((NamedType) exprType).getResolvedTypeDefinition();
        TypeDefinition castedDefinition = ((NamedType) castedType).getResolvedTypeDefinition();
        // allow sideways casts between named types where:
        // * at least one of the types is an interface
        // * neither of the types is a compound type
        // * TODO: neither of the types is a sealed class type
        if ((exprDefinition instanceof InterfaceDefinition || castedDefinition instanceof InterfaceDefinition) &&
            !(exprDefinition instanceof CompoundDefinition) && !(castedDefinition instanceof CompoundDefinition))
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
      // the type has already been resolved by the Resolver
      NamedType type = classCreationExpression.getResolvedType();
      TypeDefinition resolvedTypeDefinition = type.getResolvedTypeDefinition();
      if (resolvedTypeDefinition == null || !(resolvedTypeDefinition instanceof ClassDefinition))
      {
        throw new ConceptualException("Cannot use the 'new' operator on '" + type + "', it must be on a class definition", expression.getLexicalPhrase());
      }
      if (((ClassDefinition) resolvedTypeDefinition).isAbstract())
      {
        throw new ConceptualException("Cannot create a new " + resolvedTypeDefinition.getQualifiedName() + ", because it is an abstract class", expression.getLexicalPhrase());
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
        throw new ConceptualException("The constructor '" + constructor.getContainingTypeDefinition().getQualifiedName() + "(" + buffer + ")' is not defined to take " + arguments.length + " arguments", classCreationExpression.getLexicalPhrase());
      }
      CoalescedConceptualException coalescedException = null;
      for (int i = 0; i < arguments.length; ++i)
      {
        try
        {
          Type argumentType = checkTypes(arguments[i]);
          if (!parameters[i].getType().canAssign(argumentType))
          {
            throw new ConceptualException("Cannot pass an argument of type '" + argumentType + "' as a parameter of type '" + parameters[i].getType() + "'", arguments[i].getLexicalPhrase());
          }
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
      classCreationExpression.setType(type);
      return type;
    }
    else if (expression instanceof EqualityExpression)
    {
      EqualityExpression equalityExpression = (EqualityExpression) expression;
      Type leftType = null;
      Type rightType = null;
      CoalescedConceptualException coalescedException = null;
      try
      {
        leftType = checkTypes(equalityExpression.getLeftSubExpression());
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        rightType = checkTypes(equalityExpression.getRightSubExpression());
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }

      if (leftType instanceof VoidType || rightType instanceof VoidType)
      {
        throw new ConceptualException("Cannot check whether a void value is/is not equal to something", expression.getLexicalPhrase());
      }

      EqualityOperator operator = equalityExpression.getOperator();
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
      boolean receiverIsImmutable = fieldAccessExpression.getResolvedContextImmutability();
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
        receiverIsImmutable = isDataImmutable(baseExpressionType);
      }
      Member member = fieldAccessExpression.getResolvedMember();
      Type type;
      if (member instanceof Field)
      {
        Field field = (Field) member;
        type = field.getType();
        if (receiverIsImmutable && !field.isMutable())
        {
          type = findTypeWithDeepContextualImmutability(type, true);
        }
      }
      else if (member instanceof ArrayLengthMember)
      {
        type = ArrayLengthMember.ARRAY_LENGTH_TYPE;
        // context immutability does not apply here, since ARRAY_LENGTH_TYPE cannot be immutable (and has no subtypes which could be immutable)
      }
      else if (member instanceof Method)
      {
        // create a function type for this method
        Method method = (Method) member;
        Parameter[] parameters = method.getParameters();
        Type[] parameterTypes = new Type[parameters.length];
        for (int i = 0; i < parameters.length; ++i)
        {
          parameterTypes[i] = parameters[i].getType();
        }
        type = new FunctionType(false, method.isImmutable(), method.getReturnType(), parameterTypes, method.getCheckedThrownTypes(), null);
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
      CoalescedConceptualException coalescedException = null;
      Expression[] arguments = functionCallExpression.getArguments();
      Parameter[] parameters = null;
      Type[] parameterTypes = null;
      String name = null;
      Type resultType;
      if (functionCallExpression.getResolvedMethod() != null)
      {
        if (functionCallExpression.getResolvedBaseExpression() != null)
        {
          try
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
          }
          catch (ConceptualException e)
          {
            // we can continue despite this for now, because the base expression doesn't affect the function parameter or result types in this case
            // however, we cannot ignore the case where all we have is a resolvedBaseExpression, because there it does determine the parameters and result types
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
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
        resultType = new NamedType(false, false, false, functionCallExpression.getResolvedConstructor().getContainingTypeDefinition());
        name = functionCallExpression.getResolvedConstructor().getContainingTypeDefinition().getQualifiedName().toString();
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
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The function '" + (name == null ? "" : name) + "(" + buffer + ")' is not defined to take " + arguments.length + " arguments", functionCallExpression.getLexicalPhrase()));
        throw coalescedException;
      }

      for (int i = 0; i < arguments.length; i++)
      {
        try
        {
          Type type = checkTypes(arguments[i]);
          if (!parameterTypes[i].canAssign(type))
          {
            throw new ConceptualException("Cannot pass an argument of type '" + type + "' as a parameter of type '" + parameterTypes[i] + "'", arguments[i].getLexicalPhrase());
          }
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
      functionCallExpression.setType(resultType);
      return resultType;
    }
    else if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIf = (InlineIfExpression) expression;
      CoalescedConceptualException coalescedException = null;
      try
      {
        Type conditionType = checkTypes(inlineIf.getCondition());
        if (!(conditionType instanceof PrimitiveType) || conditionType.isNullable() || ((PrimitiveType) conditionType).getPrimitiveTypeType() != PrimitiveTypeType.BOOLEAN)
        {
          throw new ConceptualException("A conditional must be of type '" + PrimitiveTypeType.BOOLEAN.name + "', not '" + conditionType + "'", inlineIf.getCondition().getLexicalPhrase());
        }
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      Type thenType = null;
      Type elseType = null;
      try
      {
        thenType = checkTypes(inlineIf.getThenExpression());
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        elseType = checkTypes(inlineIf.getElseExpression());
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
      if (!(thenType instanceof VoidType) && !(elseType instanceof VoidType))
      {
        Type resultType = findCommonSuperType(thenType, elseType);
        if (resultType != null)
        {
          inlineIf.setType(resultType);
          return resultType;
        }
      }
      throw new ConceptualException("The types of the then and else clauses of this inline if expression are incompatible, they are: " + thenType + " and " + elseType, inlineIf.getLexicalPhrase());
    }
    else if (expression instanceof InstanceOfExpression)
    {
      InstanceOfExpression instanceOfExpression = (InstanceOfExpression) expression;
      CoalescedConceptualException coalescedException = null;
      Type checkType = instanceOfExpression.getInstanceOfType();
      if (checkType.isNullable())
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot use 'instanceof' to check whether something is nullable", checkType.getLexicalPhrase()));
      }
      if ((checkType instanceof ArrayType && ((ArrayType) checkType).isExplicitlyImmutable()) ||
          (checkType instanceof NamedType && ((NamedType) checkType).isExplicitlyImmutable() && !((NamedType) checkType).getResolvedTypeDefinition().isImmutable()) ||
          (checkType instanceof ObjectType && ((ObjectType) checkType).isExplicitlyImmutable()))
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot use 'instanceof' to check whether something is an immutable value", checkType.getLexicalPhrase()));
      }
      Type expressionType;
      try
      {
        expressionType = checkTypes(instanceOfExpression.getExpression());
        if (expressionType instanceof VoidType)
        {
          throw new ConceptualException("Cannot check whether void is an instance of " + checkType, instanceOfExpression.getLexicalPhrase());
        }
      }
      catch (ConceptualException e)
      {
        throw CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (!isInstanceOfCompatible(expressionType, checkType))
      {
        throw CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot check whether a " + expressionType + " is an instance of " + checkType + " (the result would always be false)", instanceOfExpression.getLexicalPhrase()));
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
      Type resultType = new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null);
      instanceOfExpression.setType(resultType);
      return resultType;
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
      CoalescedConceptualException coalescedException = null;
      Type leftType = null;
      Type rightType = null;
      try
      {
        leftType = checkTypes(logicalExpression.getLeftSubExpression());
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        rightType = checkTypes(logicalExpression.getRightSubExpression());
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
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
      CoalescedConceptualException coalescedException = null;
      Type nullableType = null;
      Type alternativeType = null;
      try
      {
        nullableType = checkTypes(nullCoalescingExpression.getNullableExpression());
        if (!nullableType.isNullable())
        {
          throw new ConceptualException("The null-coalescing operator '?:' is not defined when the left hand side (here '" + nullableType + "') is not nullable", expression.getLexicalPhrase());
        }
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        alternativeType = checkTypes(nullCoalescingExpression.getAlternativeExpression());
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
      if (!(nullableType instanceof VoidType) && !(alternativeType instanceof VoidType))
      {
        if (nullableType instanceof NullType)
        {
          // if the left hand side has the null type, just use the right hand side's type as the result of the expression
          nullCoalescingExpression.setType(alternativeType);
          return alternativeType;
        }
        Type resultType = findCommonSuperType(findTypeWithNullability(nullableType, false), alternativeType);
        if (resultType != null)
        {
          nullCoalescingExpression.setType(resultType);
          return resultType;
        }
      }
      throw new ConceptualException("The null-coalescing operator '?:' is not defined for the types '" + nullableType + "' and '" + alternativeType + "'", expression.getLexicalPhrase());
    }
    else if (expression instanceof NullLiteralExpression)
    {
      Type type = new NullType(null);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof ObjectCreationExpression)
    {
      Type type = new ObjectType(false, false, null);
      expression.setType(type);
      return type;
    }
    else if (expression instanceof RelationalExpression)
    {
      RelationalExpression relationalExpression = (RelationalExpression) expression;
      CoalescedConceptualException coalescedException = null;
      Type leftType = null;
      Type rightType = null;
      try
      {
        leftType = checkTypes(relationalExpression.getLeftSubExpression());
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        rightType = checkTypes(relationalExpression.getRightSubExpression());
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }

      RelationalOperator operator = relationalExpression.getOperator();
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
      CoalescedConceptualException coalescedException = null;
      Type leftType = null;
      Type rightType = null;
      try
      {
        leftType = checkTypes(shiftExpression.getLeftExpression());
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        rightType = checkTypes(shiftExpression.getRightExpression());
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
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
      CoalescedConceptualException coalescedException = null;
      for (int i = 0; i < subTypes.length; i++)
      {
        try
        {
          subTypes[i] = checkTypes(subExpressions[i]);
          if (subTypes[i] instanceof VoidType)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot create a tuple containing a void value", subExpressions[i].getLexicalPhrase()));
          }
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

        if (variableExpression.getResolvedContextImmutability())
        {
          if ((resolvedVariable instanceof GlobalVariable && !((GlobalVariable) resolvedVariable).getField().isMutable()) ||
              (resolvedVariable instanceof MemberVariable && !((MemberVariable) resolvedVariable).getField().isMutable()))
          {
            type = findTypeWithDeepContextualImmutability(type, true);
          }
        }
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
        FunctionType type = new FunctionType(false, resolvedMethod.isImmutable(), resolvedMethod.getReturnType(), parameterTypes, resolvedMethod.getCheckedThrownTypes(), null);
        expression.setType(type);
        return type;
      }
    }
    throw new ConceptualException("Internal type checking error: Unknown expression type", expression.getLexicalPhrase());
  }

  /**
   * Checks whether the specified expression type is instanceof-compatible with the specified check type.
   * @param expressionType - the type of the Expression which instanceof is being applied to
   * @param checkType - the type that the expression is being checked against
   * @return true if the types are compatible for an instanceof check, false otherwise
   */
  private static boolean isInstanceOfCompatible(Type expressionType, Type checkType)
  {
    if (expressionType instanceof NullType)
    {
      // null can be checked against anything
      return true;
    }
    if (expressionType instanceof ObjectType)
    {
      // object can be checked against anything
      return true;
    }

    if (checkType instanceof ArrayType)
    {
      if (expressionType instanceof ArrayType)
      {
        return ((ArrayType) checkType).getBaseType().isRuntimeEquivalent(((ArrayType) expressionType).getBaseType());
      }
    }
    if (checkType instanceof FunctionType)
    {
      if (expressionType instanceof FunctionType)
      {
        // don't check the immutability, since the value could have any immutability, and we wouldn't know about it from the static type
        // check that the parameter and return types match
        if (!((FunctionType) checkType).getReturnType().isRuntimeEquivalent(((FunctionType) expressionType).getReturnType()))
        {
          return false;
        }
        Type[] checkParams = ((FunctionType) checkType).getParameterTypes();
        Type[] expressionParams = ((FunctionType) expressionType).getParameterTypes();
        if (checkParams.length != expressionParams.length)
        {
          return false;
        }
        for (int i = 0; i < checkParams.length; ++i)
        {
          if (!checkParams[i].isRuntimeEquivalent(expressionParams[i]))
          {
            return false;
          }
        }
        return true;
      }
    }
    if (checkType instanceof NamedType)
    {
      if (expressionType instanceof NamedType)
      {
        NamedType checkNamedType = (NamedType) checkType;
        NamedType expressionNamedType = (NamedType) expressionType;
        if (checkNamedType.getResolvedTypeDefinition() instanceof ClassDefinition)
        {
          if (expressionNamedType.getResolvedTypeDefinition() instanceof ClassDefinition)
          {
            for (TypeDefinition t : checkNamedType.getResolvedTypeDefinition().getInheritanceLinearisation())
            {
              if (t == expressionNamedType.getResolvedTypeDefinition())
              {
                return true;
              }
            }
            for (TypeDefinition t : expressionNamedType.getResolvedTypeDefinition().getInheritanceLinearisation())
            {
              if (t == checkNamedType.getResolvedTypeDefinition())
              {
                return true;
              }
            }
          }
          else if (expressionNamedType.getResolvedTypeDefinition() instanceof InterfaceDefinition)
          {
            // TODO: when we have sealed classes, disallow checking whether an interface is an instance of a sealed class which does not implement it
            return true;
          }
        }
        else if (checkNamedType.getResolvedTypeDefinition() instanceof InterfaceDefinition)
        {
          if (expressionNamedType.getResolvedTypeDefinition() instanceof ClassDefinition)
          {
            // TODO: when we have sealed classes, disallow checking whether a sealed class is an instance of an interface which it does not implement
            return true;
          }
          else if (expressionNamedType.getResolvedTypeDefinition() instanceof InterfaceDefinition)
          {
            return true;
          }
        }
        else if (checkNamedType.getResolvedTypeDefinition() instanceof CompoundDefinition)
        {
          return checkNamedType.getResolvedTypeDefinition() == expressionNamedType.getResolvedTypeDefinition();
        }
      }
    }
    if (checkType instanceof ObjectType)
    {
      return true;
    }
    if (checkType instanceof PrimitiveType)
    {
      if (expressionType instanceof PrimitiveType)
      {
        return ((PrimitiveType) checkType).getPrimitiveTypeType() == ((PrimitiveType) expressionType).getPrimitiveTypeType();
      }
    }
    if (checkType instanceof TupleType)
    {
      if (expressionType instanceof TupleType)
      {
        Type[] checkSubTypes = ((TupleType) checkType).getSubTypes();
        Type[] expressionSubTypes = ((TupleType) expressionType).getSubTypes();
        if (checkSubTypes.length != expressionSubTypes.length)
        {
          return false;
        }
        for (int i = 0; i < checkSubTypes.length; ++i)
        {
          if (!expressionSubTypes[i].isRuntimeEquivalent(checkSubTypes[i]))
          {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  /**
   * Checks that the specified types are valid for a catch clause, and finds the common super-type that the caught variable should be.
   * This method depends on all of the types having been resolved already.
   * @param caughtTypes - the list of caught types
   * @return the common super-type of the caught types
   * @throws ConceptualException - if there is a problem with any of the caught types
   */
  public static Type checkCatchClauseTypes(Type[] caughtTypes) throws ConceptualException
  {
    CoalescedConceptualException coalescedException = null;
    for (int i = 0; i < caughtTypes.length; ++i)
    {
      boolean isThrowable = false;
      if (!(caughtTypes[i] instanceof NamedType))
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot catch a type which is not throwable", caughtTypes[i].getLexicalPhrase()));
        continue;
      }
      NamedType namedType = (NamedType) caughtTypes[i];
      for (TypeDefinition t : namedType.getResolvedTypeDefinition().getInheritanceLinearisation())
      {
        if (t == SpecialTypeHandler.THROWABLE_TYPE.getResolvedTypeDefinition())
        {
          isThrowable = true;
          break;
        }
      }
      if (!isThrowable)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot catch a type which is not throwable", caughtTypes[i].getLexicalPhrase()));
        continue;
      }
    }
    if (coalescedException != null)
    {
      throw coalescedException;
    }
    // find the common super-type of all of the caught types
    Type currentType = caughtTypes[0];
    for (int i = 1; i < caughtTypes.length; ++i)
    {
      currentType = findCommonSuperType(currentType, caughtTypes[i]);
      if (currentType instanceof ObjectType)
      {
        // if the common super-type found two equidistant super-types, it could have defaulted to the object type
        // in that case, we already know that all of our types inherit from Throwable, so we can change it back to Throwable
        currentType = new NamedType(currentType.isNullable(), ((ObjectType) currentType).isExplicitlyImmutable(), ((ObjectType) currentType).isContextuallyImmutable(), SpecialTypeHandler.THROWABLE_TYPE.getResolvedTypeDefinition());
      }
    }
    if (!(currentType instanceof NamedType))
    {
      throw new IllegalStateException("Found an caught exception super-type which is not a NamedType");
    }
    return currentType;
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
      // if either of them was either floating point or boolean, we would have found any compatibilities above
      if (aType != PrimitiveTypeType.BOOLEAN && bType != PrimitiveTypeType.BOOLEAN && !aType.isFloating() && !bType.isFloating())
      {
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
    }
    if (a instanceof ArrayType && b instanceof ArrayType)
    {
      ArrayType arrayA = (ArrayType) a;
      ArrayType arrayB = (ArrayType) b;
      boolean nullability = a.isNullable() || b.isNullable();
      boolean explicitImmutability   = arrayA.isExplicitlyImmutable()   || arrayB.isExplicitlyImmutable();
      boolean contextualImmutability = arrayA.isContextuallyImmutable() || arrayB.isContextuallyImmutable();
      // alter one of the types to have the minimum nullability, explicit immutability, and contextual immutability that we need
      // if the altered type cannot assign the other one, then altering the other type would not help,
      // since the only other variable in array.canAssign() is the base type, and the checking for it is symmetric
      Type alteredA = findTypeWithNullability(arrayA, nullability);
      alteredA = findTypeWithDataImmutability(alteredA, explicitImmutability, contextualImmutability);
      if (alteredA.canAssign(b))
      {
        return alteredA;
      }
    }
    if (a instanceof FunctionType && b instanceof FunctionType)
    {
      FunctionType functionA = (FunctionType) a;
      FunctionType functionB = (FunctionType) b;
      // find the combination's nullability and immutability
      boolean nullability = a.isNullable() | b.isNullable();
      boolean immutability = functionA.isImmutable() & functionB.isImmutable();
      // find the union of the two functions' thrown types
      List<NamedType> combinedThrown = new LinkedList<NamedType>();
      for (NamedType aThrown : functionA.getThrownTypes())
      {
        boolean found = false;
        for (NamedType check : combinedThrown)
        {
          if (check.canAssign(aThrown))
          {
            found = true;
            break;
          }
        }
        if (!found)
        {
          combinedThrown.add(aThrown);
        }
      }
      for (NamedType bThrown : functionB.getThrownTypes())
      {
        boolean found = false;
        for (NamedType check : combinedThrown)
        {
          if (check.canAssign(bThrown))
          {
            found = true;
            break;
          }
        }
        if (!found)
        {
          combinedThrown.add(bThrown);
        }
      }
      // alter one of the types to have the minimum nullability and immutability that we need, and give it the union of their thrown types
      // if the altered type cannot assign the other one, then altering the other type would not help,
      // since the only other variables in function.canAssign() are the parameter and return types, and the checking for those is symmetric
      FunctionType alteredA = new FunctionType(nullability, immutability, functionA.getReturnType(), functionA.getParameterTypes(), combinedThrown.toArray(new NamedType[combinedThrown.size()]), null);

      if (alteredA.canAssign(b))
      {
        return alteredA;
      }
    }
    if (a instanceof NamedType && b instanceof NamedType)
    {
      NamedType namedA = (NamedType) a;
      NamedType namedB = (NamedType) b;
      boolean nullability = a.isNullable() || b.isNullable();
      boolean explicitImmutability   = namedA.isExplicitlyImmutable()   || namedB.isExplicitlyImmutable();
      boolean contextualImmutability = namedA.isContextuallyImmutable() || namedB.isContextuallyImmutable();
      // alter the types to have the minimum nullability, explicit immutability, and contextual immutability that we need
      // if neither of the altered types can assign the unaltered other type, then we cannot do anything else,
      // since either the types are the same, or one of them is a supertype of the other, because we do not yet have a concept of multiple inheritance (e.g. interfaces)
      Type alteredA = findTypeWithNullability(namedA, nullability);
      alteredA = findTypeWithDataImmutability(alteredA, explicitImmutability, contextualImmutability);
      if (alteredA.canAssign(b))
      {
        return alteredA;
      }
      Type alteredB = findTypeWithNullability(namedB, nullability);
      alteredB = findTypeWithDataImmutability(alteredB, explicitImmutability, contextualImmutability);
      if (alteredB.canAssign(a))
      {
        return alteredB;
      }

      // the two types are not in a parent-child relationship, i.e. neither of the type definitions inherits from the other
      // so see if they have any super-types in common
      // if we find a class definition in common, we choose it immediately
      // if we find any interface definitions in common, we choose the one which is closest to the start of the two types' linearisations (or if two are equally close, we choose object)
      int bestCombinedIndex = Integer.MAX_VALUE;
      TypeDefinition bestSuperType = null;
      TypeDefinition[] linearisationA = namedA.getResolvedTypeDefinition().getInheritanceLinearisation();
      TypeDefinition[] linearisationB = namedB.getResolvedTypeDefinition().getInheritanceLinearisation();
      superTypeLoop:
      for (int indexA = 0; indexA < linearisationA.length; ++indexA)
      {
        for (int indexB = 0; indexB < linearisationB.length; ++indexB)
        {
          if (linearisationA[indexA] == linearisationB[indexB])
          {
            if (linearisationA[indexA] instanceof ClassDefinition)
            {
              bestSuperType = linearisationA[indexA];
              break superTypeLoop;
            }
            int combinedIndex = indexA + indexB;
            if (combinedIndex < bestCombinedIndex)
            {
              bestCombinedIndex = combinedIndex;
              bestSuperType = linearisationA[indexA];
            }
            else if (combinedIndex == bestCombinedIndex)
            {
              // default to object, unless we can find a better combined index
              bestSuperType = null;
            }
            // skip the rest of the inner loop, since the lists do not contain duplicates
            continue superTypeLoop;
          }
        }
      }
      if (bestSuperType != null)
      {
        return new NamedType(nullability, explicitImmutability, contextualImmutability, bestSuperType);
      }
    }
    if (a instanceof TupleType && b instanceof TupleType)
    {
      // these TupleTypes must both have at least two elements, since we have handled all single-element tuples above already
      Type[] aSubTypes = ((TupleType) a).getSubTypes();
      Type[] bSubTypes = ((TupleType) b).getSubTypes();
      if (aSubTypes.length == bSubTypes.length)
      {
        Type[] commonSubTypes = new Type[aSubTypes.length];
        for (int i = 0; i < aSubTypes.length; ++i)
        {
          commonSubTypes[i] = findCommonSuperType(aSubTypes[i], bSubTypes[i]);
        }
        return new TupleType(a.isNullable() | b.isNullable(), commonSubTypes, null);
      }
    }
    // otherwise, object is the common supertype, so find its nullability and immutability
    boolean nullable = a.isNullable() || b.isNullable();
    boolean explicitlyImmutable = (a instanceof ArrayType  && ((ArrayType)  a).isExplicitlyImmutable()) ||
                                  (a instanceof NamedType  && ((NamedType)  a).isExplicitlyImmutable()) ||
                                  (a instanceof ObjectType && ((ObjectType) a).isExplicitlyImmutable()) ||
                                  (b instanceof ArrayType  && ((ArrayType)  b).isExplicitlyImmutable()) ||
                                  (b instanceof NamedType  && ((NamedType)  b).isExplicitlyImmutable()) ||
                                  (b instanceof ObjectType && ((ObjectType) b).isExplicitlyImmutable());
    boolean contextuallyImmutable = (a instanceof ArrayType  && ((ArrayType)  a).isContextuallyImmutable()) ||
                                    (a instanceof NamedType  && ((NamedType)  a).isContextuallyImmutable()) ||
                                    (a instanceof ObjectType && ((ObjectType) a).isContextuallyImmutable()) ||
                                    (b instanceof ArrayType  && ((ArrayType)  b).isContextuallyImmutable()) ||
                                    (b instanceof NamedType  && ((NamedType)  b).isContextuallyImmutable()) ||
                                    (b instanceof ObjectType && ((ObjectType) b).isContextuallyImmutable());
    return new ObjectType(nullable, explicitlyImmutable, contextuallyImmutable, null);
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
      ArrayType arrayType = (ArrayType) type;
      return new ArrayType(nullable, arrayType.isExplicitlyImmutable(), arrayType.isContextuallyImmutable(), arrayType.getBaseType(), null);
    }
    if (type instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) type;
      return new FunctionType(nullable, functionType.isImmutable(), functionType.getReturnType(), functionType.getParameterTypes(), functionType.getThrownTypes(), null);
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      return new NamedType(nullable, namedType.isExplicitlyImmutable(), namedType.isContextuallyImmutable(), namedType.getResolvedTypeDefinition());
    }
    if (type instanceof ObjectType)
    {
      ObjectType objectType = (ObjectType) type;
      return new ObjectType(nullable, objectType.isExplicitlyImmutable(), objectType.isContextuallyImmutable(), null);
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

  /**
   * Finds whether the specified type is immutable in terms of data. i.e. the data inside it cannot be modified.
   * @param type - the type to check
   * @return true if the specified type is immutable in terms of data, false if the type is not or cannot be immutable
   */
  private static boolean isDataImmutable(Type type)
  {
    if (type instanceof ArrayType)
    {
      return ((ArrayType) type).isExplicitlyImmutable() || ((ArrayType) type).isContextuallyImmutable();
    }
    if (type instanceof NamedType)
    {
      return ((NamedType) type).isExplicitlyImmutable() || ((NamedType) type).isContextuallyImmutable();
    }
    if (type instanceof ObjectType)
    {
      return ((ObjectType) type).isExplicitlyImmutable() || ((ObjectType) type).isContextuallyImmutable();
    }
    if (type instanceof FunctionType || type instanceof NullType || type instanceof PrimitiveType || type instanceof TupleType)
    {
      return false;
    }
    throw new IllegalArgumentException("Cannot find the data immutability of an unknown type: " + type);
  }

  /**
   * Finds the equivalent of the specified type with the specified explicit and contextual data-immutability.
   * If the specified type does not have a concept of either explicit or contextual data-immutability, then that type of immutability is not checked in the calculation.
   * @param type - the type to find the version of which has the specified explicit immutability
   * @param explicitlyImmutable - true if the returned type should be explicitly data-immutable, false otherwise
   * @param contextuallyImmutable - true if the returned type should be contextually data-immutable, false otherwise
   * @return the version of the specified type with the specified explicit and contextual data-immutability, or the original type if it already has the requested data-immutability
   */
  public static Type findTypeWithDataImmutability(Type type, boolean explicitlyImmutable, boolean contextuallyImmutable)
  {
    if (type instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) type;
      if (arrayType.isExplicitlyImmutable() == explicitlyImmutable && arrayType.isContextuallyImmutable() == contextuallyImmutable)
      {
        return arrayType;
      }
      return new ArrayType(arrayType.isNullable(), explicitlyImmutable, contextuallyImmutable, arrayType.getBaseType(), null);
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      if (namedType.isExplicitlyImmutable() == explicitlyImmutable && namedType.isContextuallyImmutable() == contextuallyImmutable)
      {
        return namedType;
      }
      return new NamedType(namedType.isNullable(), explicitlyImmutable, contextuallyImmutable, namedType.getResolvedTypeDefinition());
    }
    if (type instanceof ObjectType)
    {
      ObjectType objectType = (ObjectType) type;
      if (objectType.isExplicitlyImmutable() == explicitlyImmutable && objectType.isContextuallyImmutable() == contextuallyImmutable)
      {
        return objectType;
      }
      return new ObjectType(objectType.isNullable(), explicitlyImmutable, contextuallyImmutable, null);
    }
    if (type instanceof PrimitiveType || type instanceof TupleType || type instanceof FunctionType || type instanceof NullType)
    {
      return type;
    }
    throw new IllegalArgumentException("Cannot change the immutability of an unknown type: " + type);
  }

  /**
   * Finds the equivalent of the specified type with the specified contextual immutability throughout.
   * If the provided type is explicitly immutable, then the resulting type may be equivalent to the original type,
   * even if the purpose of the call was to find a type without contextual immutability; this is because an explicitly
   * immutable type must always be contextually immutable, and this function does not change explicit immutability.
   * @param type - the type to find the version of which has the specified contextual immutability
   * @param contextuallyImmutable - true if the returned type should be contextually immutable, false otherwise
   * @return the version of the specified type with the specified contextual immutability, or the original type if it already has the requested contextual immutability
   */
  public static Type findTypeWithDeepContextualImmutability(Type type, boolean contextuallyImmutable)
  {
    if (type instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) type;
      if (arrayType.isContextuallyImmutable() == contextuallyImmutable)
      {
        return arrayType;
      }
      Type baseType = findTypeWithDeepContextualImmutability(arrayType.getBaseType(), contextuallyImmutable);
      return new ArrayType(arrayType.isNullable(), arrayType.isExplicitlyImmutable(), contextuallyImmutable, baseType, null);
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      if (namedType.isContextuallyImmutable() == contextuallyImmutable)
      {
        return namedType;
      }
      return new NamedType(namedType.isNullable(), namedType.isExplicitlyImmutable(), contextuallyImmutable, namedType.getResolvedTypeDefinition());
    }
    if (type instanceof ObjectType)
    {
      ObjectType objectType = (ObjectType) type;
      if (objectType.isContextuallyImmutable() == contextuallyImmutable)
      {
        return objectType;
      }
      return new ObjectType(objectType.isNullable(), objectType.isExplicitlyImmutable(), contextuallyImmutable, null);
    }
    if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      Type[] subTypes = tupleType.getSubTypes();
      Type[] alteredSubTypes = new Type[subTypes.length];
      for (int i = 0; i < subTypes.length; ++i)
      {
        alteredSubTypes[i] = findTypeWithDeepContextualImmutability(subTypes[i], contextuallyImmutable);
      }
      return new TupleType(tupleType.isNullable(), alteredSubTypes, null);
    }
    if (type instanceof FunctionType || type instanceof PrimitiveType || type instanceof NullType)
    {
      // return the original type, since none of these sorts of type have a concept of contextual immutability
      return type;
    }
    throw new IllegalArgumentException("Cannot find the " + (contextuallyImmutable ? "" : "non-") + "contextually-immutable version of: " + type);
  }

  /**
   * Finds a version of the specified type without any type modifiers (i.e. without nullability and immutability).
   * @param type - the Type to find without any type modifiers
   * @return a version of the specified type without any type modifiers
   */
  public static Type findTypeWithoutModifiers(Type type)
  {
    Type result = findTypeWithNullability(type, false);
    return findTypeWithDataImmutability(result, false, false);
  }
}
