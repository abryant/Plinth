package eu.bryants.anthony.toylanguage.compiler.passes;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
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
import eu.bryants.anthony.toylanguage.ast.expression.TupleExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleIndexExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
import eu.bryants.anthony.toylanguage.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.misc.BlankAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.misc.VariableAssignee;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.statement.BreakStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ContinueStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ExpressionStatement;
import eu.bryants.anthony.toylanguage.ast.statement.IfStatement;
import eu.bryants.anthony.toylanguage.ast.statement.PrefixIncDecStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ReturnStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Statement;
import eu.bryants.anthony.toylanguage.ast.statement.WhileStatement;
import eu.bryants.anthony.toylanguage.ast.type.ArrayType;
import eu.bryants.anthony.toylanguage.ast.type.NamedType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType;
import eu.bryants.anthony.toylanguage.ast.type.TupleType;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.ast.type.VoidType;
import eu.bryants.anthony.toylanguage.compiler.ConceptualException;
import eu.bryants.anthony.toylanguage.compiler.NameNotResolvedException;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Resolver
{

  /**
   * Finds all of the nested variables of a block.
   * Before calling this, resolve() must have been called on the compilation unit containing the block.
   * @param block - the block to get all the nested variables of
   * @return a set containing all of the variables defined in this block, including in nested blocks
   */
  public static Set<Variable> getAllNestedVariables(Block block)
  {
    Set<Variable> result = new HashSet<Variable>();
    Deque<Statement> stack = new LinkedList<Statement>();
    stack.push(block);
    while (!stack.isEmpty())
    {
      Statement statement = stack.pop();
      if (statement instanceof Block)
      {
        // add all variables from this block to the result set
        result.addAll(((Block) statement).getVariables());
        for (Statement s : ((Block) statement).getStatements())
        {
          stack.push(s);
        }
      }
      else if (statement instanceof IfStatement)
      {
        IfStatement ifStatement = (IfStatement) statement;
        stack.push(ifStatement.getThenClause());
        if (ifStatement.getElseClause() != null)
        {
          stack.push(ifStatement.getElseClause());
        }
      }
      else if (statement instanceof WhileStatement)
      {
        stack.push(((WhileStatement) statement).getStatement());
      }
    }
    return result;
  }

  public static void resolve(CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    for (CompoundDefinition compoundDefinition : compilationUnit.getCompoundDefinitions())
    {
      resolve(compoundDefinition, compilationUnit);
    }
    for (Function function : compilationUnit.getFunctions())
    {
      resolve(function, compilationUnit);
    }
  }

  private static void resolve(CompoundDefinition compound, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    for (Field field : compound.getFields())
    {
      resolve(field.getType(), compilationUnit);
    }
    for (Constructor constructor : compound.getConstructors())
    {
      Block mainBlock = constructor.getBlock();
      for (Parameter p : constructor.getParameters())
      {
        Variable oldVar = mainBlock.addVariable(p.getVariable());
        if (oldVar != null)
        {
          throw new ConceptualException("Duplicate parameter: " + p.getName(), p.getLexicalPhrase());
        }
        resolve(p.getType(), compilationUnit);
      }
      for (Statement s : mainBlock.getStatements())
      {
        resolve(s, mainBlock, compound, compilationUnit);
      }
    }
  }

  private static void resolve(Type type, CompilationUnit compilationUnit) throws NameNotResolvedException
  {
    if (type instanceof ArrayType)
    {
      resolve(((ArrayType) type).getBaseType(), compilationUnit);
    }
    else if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      CompoundDefinition compoundDefinition = compilationUnit.getCompoundDefinition(namedType.getName());
      if (compoundDefinition == null)
      {
        throw new NameNotResolvedException("Unable to resolve: " + namedType.getName(), namedType.getLexicalPhrase());
      }
      namedType.setResolvedDefinition(compoundDefinition);
    }
    else if (type instanceof PrimitiveType)
    {
      // do nothing
    }
    else if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      for (Type subType : tupleType.getSubTypes())
      {
        resolve(subType, compilationUnit);
      }
    }
    else if (type instanceof VoidType)
    {
      // do nothing
    }
    else
    {
      throw new IllegalArgumentException("Unknown Type type: " + type);
    }
  }

  private static void resolve(Function function, CompilationUnit compilationUnit) throws ConceptualException, NameNotResolvedException
  {
    Block mainBlock = function.getBlock();
    resolve(function.getType(), compilationUnit);
    for (Parameter p : function.getParameters())
    {
      Variable oldVar = mainBlock.addVariable(p.getVariable());
      if (oldVar != null)
      {
        throw new ConceptualException("Duplicate parameter: " + p.getName(), p.getLexicalPhrase());
      }
      resolve(p.getType(), compilationUnit);
    }
    for (Statement s : mainBlock.getStatements())
    {
      resolve(s, mainBlock, null, compilationUnit);
    }
  }

  private static void resolve(Statement statement, Block enclosingBlock, CompoundDefinition enclosingDefinition, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assignStatement = (AssignStatement) statement;
      Type type = assignStatement.getType();
      if (type != null)
      {
        resolve(type, compilationUnit);
      }
      Assignee[] assignees = assignStatement.getAssignees();
      boolean distributedTupleType = type != null && type instanceof TupleType && ((TupleType) type).getSubTypes().length == assignees.length;
      for (int i = 0; i < assignees.length; i++)
      {
        if (assignees[i] instanceof VariableAssignee)
        {
          VariableAssignee variableAssignee = (VariableAssignee) assignees[i];
          Variable variable = enclosingBlock.getVariable(variableAssignee.getVariableName());
          if (variable == null && enclosingDefinition != null)
          {
            Field field = enclosingDefinition.getField(variableAssignee.getVariableName());
            if (field != null)
            {
              variable = field.getMemberVariable();
            }
          }
          if (variable == null)
          {
            if (type == null)
            {
              throw new NameNotResolvedException("Unable to resolve: " + variableAssignee.getVariableName(), variableAssignee.getLexicalPhrase());
            }
            // we have a type, so define the variable now
            if (distributedTupleType)
            {
              Type subType = ((TupleType) type).getSubTypes()[i];
              variable = new Variable(subType, variableAssignee.getVariableName());
            }
            else
            {
              variable = new Variable(type, variableAssignee.getVariableName());
            }
            enclosingBlock.addVariable(variable);
          }
          variableAssignee.setResolvedVariable(variable);
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          resolve(arrayElementAssignee.getArrayExpression(), enclosingBlock, enclosingDefinition, compilationUnit);
          resolve(arrayElementAssignee.getDimensionExpression(), enclosingBlock, enclosingDefinition, compilationUnit);
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
        resolve(assignStatement.getExpression(), enclosingBlock, enclosingDefinition, compilationUnit);
      }
    }
    else if (statement instanceof Block)
    {
      Block subBlock = (Block) statement;
      for (Variable v : enclosingBlock.getVariables())
      {
        subBlock.addVariable(v);
      }
      for (Statement s : subBlock.getStatements())
      {
        resolve(s, subBlock, enclosingDefinition, compilationUnit);
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
      resolve(((ExpressionStatement) statement).getExpression(), enclosingBlock, enclosingDefinition, compilationUnit);
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      resolve(ifStatement.getExpression(), enclosingBlock, enclosingDefinition, compilationUnit);
      resolve(ifStatement.getThenClause(), enclosingBlock, enclosingDefinition, compilationUnit);
      if (ifStatement.getElseClause() != null)
      {
        resolve(ifStatement.getElseClause(), enclosingBlock, enclosingDefinition, compilationUnit);
      }
    }
    else if (statement instanceof PrefixIncDecStatement)
    {
      PrefixIncDecStatement prefixIncDecStatement = (PrefixIncDecStatement) statement;
      Assignee assignee = prefixIncDecStatement.getAssignee();
      if (assignee instanceof VariableAssignee)
      {
        VariableAssignee variableAssignee = (VariableAssignee) assignee;
        Variable variable = enclosingBlock.getVariable(variableAssignee.getVariableName());
        if (variable == null)
        {
          throw new NameNotResolvedException("Unable to resolve: " + variableAssignee.getVariableName(), variableAssignee.getLexicalPhrase());
        }
        variableAssignee.setResolvedVariable(variable);
      }
      else if (assignee instanceof ArrayElementAssignee)
      {
        ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
        resolve(arrayElementAssignee.getArrayExpression(), enclosingBlock, enclosingDefinition, compilationUnit);
        resolve(arrayElementAssignee.getDimensionExpression(), enclosingBlock, enclosingDefinition, compilationUnit);
      }
      else if (assignee instanceof BlankAssignee)
      {
        throw new ConceptualException("Cannot " + (prefixIncDecStatement.isIncrement() ? "inc" : "dec") + "rement a blank assignee", assignee.getLexicalPhrase());
      }
      else
      {
        throw new IllegalStateException("Unknown Assignee type: " + assignee);
      }
    }
    else if (statement instanceof ReturnStatement)
    {
      Expression returnedExpression = ((ReturnStatement) statement).getExpression();
      if (returnedExpression != null)
      {
        resolve(returnedExpression, enclosingBlock, enclosingDefinition, compilationUnit);
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      resolve(whileStatement.getExpression(), enclosingBlock, enclosingDefinition, compilationUnit);
      resolve(whileStatement.getStatement(), enclosingBlock, enclosingDefinition, compilationUnit);
    }
    else
    {
      throw new ConceptualException("Internal name resolution error: Unknown statement type: " + statement, statement.getLexicalPhrase());
    }
  }

  private static void resolve(Expression expression, Block block, CompoundDefinition enclosingDefinition, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    if (expression instanceof ArithmeticExpression)
    {
      resolve(((ArithmeticExpression) expression).getLeftSubExpression(), block, enclosingDefinition, compilationUnit);
      resolve(((ArithmeticExpression) expression).getRightSubExpression(), block, enclosingDefinition, compilationUnit);
    }
    else if (expression instanceof ArrayAccessExpression)
    {
      ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
      resolve(arrayAccessExpression.getArrayExpression(), block, enclosingDefinition, compilationUnit);
      resolve(arrayAccessExpression.getDimensionExpression(), block, enclosingDefinition, compilationUnit);
    }
    else if (expression instanceof ArrayCreationExpression)
    {
      ArrayCreationExpression creationExpression = (ArrayCreationExpression) expression;
      resolve(creationExpression.getType(), compilationUnit);
      if (creationExpression.getDimensionExpressions() != null)
      {
        for (Expression e : creationExpression.getDimensionExpressions())
        {
          resolve(e, block, enclosingDefinition, compilationUnit);
        }
      }
      if (creationExpression.getValueExpressions() != null)
      {
        for (Expression e : creationExpression.getValueExpressions())
        {
          resolve(e, block, enclosingDefinition, compilationUnit);
        }
      }
    }
    else if (expression instanceof BitwiseNotExpression)
    {
      resolve(((BitwiseNotExpression) expression).getExpression(), block, enclosingDefinition, compilationUnit);
    }
    else if (expression instanceof BooleanLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof BooleanNotExpression)
    {
      resolve(((BooleanNotExpression) expression).getExpression(), block, enclosingDefinition, compilationUnit);
    }
    else if (expression instanceof BracketedExpression)
    {
      resolve(((BracketedExpression) expression).getExpression(), block, enclosingDefinition, compilationUnit);
    }
    else if (expression instanceof CastExpression)
    {
      resolve(expression.getType(), compilationUnit);
      resolve(((CastExpression) expression).getExpression(), block, enclosingDefinition, compilationUnit);
    }
    else if (expression instanceof ComparisonExpression)
    {
      resolve(((ComparisonExpression) expression).getLeftSubExpression(), block, enclosingDefinition, compilationUnit);
      resolve(((ComparisonExpression) expression).getRightSubExpression(), block, enclosingDefinition, compilationUnit);
    }
    else if (expression instanceof FieldAccessExpression)
    {
      FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) expression;
      String fieldName = fieldAccessExpression.getFieldName();
      resolve(fieldAccessExpression.getExpression(), block, enclosingDefinition, compilationUnit);

      // find the type of the sub-expression, by calling the type checker
      // this is fine as long as we resolve all of the sub-expression first
      Type expressionType = TypeChecker.checkTypes(fieldAccessExpression.getExpression(), compilationUnit);
      Member member = expressionType.getMember(fieldName);
      if (member == null)
      {
        throw new NameNotResolvedException("No such member \"" + fieldName + "\" for type " + expressionType, expression.getLexicalPhrase());
      }
      fieldAccessExpression.setResolvedMember(member);
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression expr = (FunctionCallExpression) expression;
      // resolve all of the sub-expressions
      for (Expression e : expr.getArguments())
      {
        resolve(e, block, enclosingDefinition, compilationUnit);
        TypeChecker.checkTypes(e, compilationUnit);
      }

      // resolve the called function
      Function function = compilationUnit.getFunction(expr.getName());
      CompoundDefinition compoundDefinition = compilationUnit.getCompoundDefinition(expr.getName());
      if (function == null && compoundDefinition == null)
      {
        throw new NameNotResolvedException("Unable to resolve \"" + expr.getName() + "\"", expr.getLexicalPhrase());
      }
      Map<Parameter[], Object> paramLists = new HashMap<Parameter[], Object>();
      if (function != null)
      {
        paramLists.put(function.getParameters(), function);
      }
      if (compoundDefinition != null)
      {
        for (Constructor constructor : compoundDefinition.getConstructors())
        {
          paramLists.put(constructor.getParameters(), constructor);
        }
      }
      boolean resolved = false;
      for (Entry<Parameter[], Object> entry : paramLists.entrySet())
      {
        Parameter[] parameters = entry.getKey();
        // make sure the types match, otherwise we need to find another candidate
        boolean typesMatch = parameters.length == expr.getArguments().length;
        if (typesMatch)
        {
          for (int i = 0; i < parameters.length; i++)
          {
            Type parameterType = parameters[i].getType();
            Type argumentType = expr.getArguments()[i].getType();
            if (!parameterType.canAssign(argumentType))
            {
              typesMatch = false;
              break;
            }
          }
        }
        if (typesMatch)
        {
          if (entry.getValue() instanceof Function)
          {
            expr.setResolvedFunction((Function) entry.getValue());
          }
          else if (entry.getValue() instanceof Constructor)
          {
            expr.setResolvedConstructor((Constructor) entry.getValue());
          }
          else
          {
            throw new IllegalStateException("Unknown function call expression target type: " + entry.getValue());
          }
          resolved = true;
        }
      }
      if (!resolved)
      {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < expr.getArguments().length; i++)
        {
          buffer.append(expr.getArguments()[i].getType());
          if (i != expr.getArguments().length - 1)
          {
            buffer.append(", ");
          }
        }
        throw new NameNotResolvedException("Unable to resolve a call to " + expr.getName() + " with the argument types: " + buffer, expr.getLexicalPhrase());
      }
      expr.setResolvedFunction(function);
    }
    else if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIfExpression = (InlineIfExpression) expression;
      resolve(inlineIfExpression.getCondition(), block, enclosingDefinition, compilationUnit);
      resolve(inlineIfExpression.getThenExpression(), block, enclosingDefinition, compilationUnit);
      resolve(inlineIfExpression.getElseExpression(), block, enclosingDefinition, compilationUnit);
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof LogicalExpression)
    {
      resolve(((LogicalExpression) expression).getLeftSubExpression(), block, enclosingDefinition, compilationUnit);
      resolve(((LogicalExpression) expression).getRightSubExpression(), block, enclosingDefinition, compilationUnit);
    }
    else if (expression instanceof MinusExpression)
    {
      resolve(((MinusExpression) expression).getExpression(), block, enclosingDefinition, compilationUnit);
    }
    else if (expression instanceof TupleExpression)
    {
      TupleExpression tupleExpression = (TupleExpression) expression;
      Expression[] subExpressions = tupleExpression.getSubExpressions();
      for (int i = 0; i < subExpressions.length; i++)
      {
        resolve(subExpressions[i], block, enclosingDefinition, compilationUnit);
      }
    }
    else if (expression instanceof TupleIndexExpression)
    {
      TupleIndexExpression indexExpression = (TupleIndexExpression) expression;
      resolve(indexExpression.getExpression(), block, enclosingDefinition, compilationUnit);
    }
    else if (expression instanceof VariableExpression)
    {
      VariableExpression expr = (VariableExpression) expression;
      Variable var = block.getVariable(expr.getName());
      if (var == null && enclosingDefinition != null)
      {
        Field field = enclosingDefinition.getField(expr.getName());
        if (field != null)
        {
          var = field.getMemberVariable();
        }
      }
      if (var == null)
      {
        throw new NameNotResolvedException("Unable to resolve \"" + expr.getName() + "\"", expr.getLexicalPhrase());
      }
      expr.setResolvedVariable(var);
    }
    else
    {
      throw new ConceptualException("Internal name resolution error: Unknown expression type", expression.getLexicalPhrase());
    }
  }

}
