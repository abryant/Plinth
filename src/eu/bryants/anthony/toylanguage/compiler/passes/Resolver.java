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
import eu.bryants.anthony.toylanguage.ast.expression.ShiftExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ThisExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleIndexExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.member.ArrayLengthMember;
import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.ast.member.Method;
import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
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
import eu.bryants.anthony.toylanguage.ast.type.ArrayType;
import eu.bryants.anthony.toylanguage.ast.type.FunctionType;
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
      else if (statement instanceof ForStatement)
      {
        ForStatement forStatement = (ForStatement) statement;
        if (forStatement.getInitStatement() != null)
        {
          stack.push(forStatement.getInitStatement());
        }
        if (forStatement.getUpdateStatement() != null)
        {
          stack.push(forStatement.getUpdateStatement());
        }
        stack.push(forStatement.getBlock());
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
    // resolve the top level types first, e.g. function parameters and return types, field types
    // so that they can be used anywhere in statements and expressions
    for (CompoundDefinition compoundDefinition : compilationUnit.getCompoundDefinitions())
    {
      resolveTypes(compoundDefinition, compilationUnit);
    }
    // resolve the bodies of methods, field assignments, etc.
    for (CompoundDefinition compoundDefinition : compilationUnit.getCompoundDefinitions())
    {
      resolve(compoundDefinition, compilationUnit);
    }
  }

  private static void resolveTypes(CompoundDefinition compound, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
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
    }
    // resolve all method return and parameter types, and check for duplicate methods
    class MethodDisambiguator
    {
      Type returnType;
      Type[] parameterTypes;
      String name;
      public MethodDisambiguator(Type returnType, Type[] parameterTypes, String name)
      {
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.name = name;
      }
      @Override
      public boolean equals(Object o)
      {
        if (!(o instanceof MethodDisambiguator))
        {
          return false;
        }
        MethodDisambiguator other = (MethodDisambiguator) o;
        if (!returnType.isEquivalent(other.returnType) || !name.equals(other.name) || parameterTypes.length != other.parameterTypes.length)
        {
          return false;
        }
        for (int i = 0; i < parameterTypes.length; ++i)
        {
          if (!parameterTypes[i].isEquivalent(other.parameterTypes[i]))
          {
            return false;
          }
        }
        return true;
      }
      @Override
      public int hashCode()
      {
        return name.hashCode(); // don't bother to work out a way of finding hashCodes for the types, this is sufficient
      }
    }
    Map<MethodDisambiguator, Method> allMethods = new HashMap<MethodDisambiguator, Method>();
    for (Method method : compound.getAllMethods())
    {
      resolve(method.getReturnType(), compilationUnit);
      Block mainBlock = method.getBlock();
      Parameter[] parameters = method.getParameters();
      Type[] parameterTypes = new Type[parameters.length];
      for (int i = 0; i < parameters.length; ++i)
      {
        Variable oldVar = mainBlock.addVariable(parameters[i].getVariable());
        if (oldVar != null)
        {
          throw new ConceptualException("Duplicate parameter: " + parameters[i].getName(), parameters[i].getLexicalPhrase());
        }
        resolve(parameters[i].getType(), compilationUnit);
        parameterTypes[i] = parameters[i].getType();
      }
      Method oldMethod = allMethods.put(new MethodDisambiguator(method.getReturnType(), parameterTypes, method.getName()), method);
      if (oldMethod != null)
      {
        throw new ConceptualException("Duplicate method: " + method.getName(), method.getLexicalPhrase());
      }
    }
  }

  private static void resolve(CompoundDefinition compound, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    // TODO: resolve field expressions, when they exist
    for (Constructor constructor : compound.getConstructors())
    {
      Block mainBlock = constructor.getBlock();
      for (Statement s : mainBlock.getStatements())
      {
        resolve(s, mainBlock, compound, compilationUnit);
      }
    }
    for (Method method : compound.getAllMethods())
    {
      Block mainBlock = method.getBlock();
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
      if (namedType.getResolvedDefinition() != null)
      {
        return;
      }
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
              if (field.isStatic())
              {
                variable = field.getGlobalVariable();
              }
              else
              {
                variable = field.getMemberVariable();
              }
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
        else if (assignees[i] instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];
          // use the expression resolver to resolve the contained field access expression
          resolve(fieldAssignee.getFieldAccessExpression(), enclosingBlock, enclosingDefinition, compilationUnit);
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
    else if (statement instanceof ForStatement)
    {
      ForStatement forStatement = (ForStatement) statement;
      Statement init = forStatement.getInitStatement();
      Expression condition = forStatement.getConditional();
      Statement update = forStatement.getUpdateStatement();
      Block block = forStatement.getBlock();
      // process this block right here instead of recursing, since we need to process the init, condition, and update parts of the statement inside it after adding the variables, but before the rest of the resolution
      for (Variable v : enclosingBlock.getVariables())
      {
        block.addVariable(v);
      }
      if (init != null)
      {
        resolve(init, block, enclosingDefinition, compilationUnit);
      }
      if (condition != null)
      {
        resolve(condition, block, enclosingDefinition, compilationUnit);
      }
      if (update != null)
      {
        resolve(update, block, enclosingDefinition, compilationUnit);
      }
      for (Statement s : block.getStatements())
      {
        resolve(s, block, enclosingDefinition, compilationUnit);
      }
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
    else if (statement instanceof ShorthandAssignStatement)
    {
      ShorthandAssignStatement shorthandAssignStatement = (ShorthandAssignStatement) statement;
      for (Assignee assignee : shorthandAssignStatement.getAssignees())
      {
        if (assignee instanceof VariableAssignee)
        {
          VariableAssignee variableAssignee = (VariableAssignee) assignee;
          Variable variable = enclosingBlock.getVariable(variableAssignee.getVariableName());
          if (variable == null && enclosingDefinition != null)
          {
            Field field = enclosingDefinition.getField(variableAssignee.getVariableName());
            if (field != null)
            {
              if (field.isStatic())
              {
                variable = field.getGlobalVariable();
              }
              else
              {
                variable = field.getMemberVariable();
              }
            }
          }
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
        else if (assignee instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignee;
          // use the expression resolver to resolve the contained field access expression
          resolve(fieldAssignee.getFieldAccessExpression(), enclosingBlock, enclosingDefinition, compilationUnit);
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
      resolve(shorthandAssignStatement.getExpression(), enclosingBlock, enclosingDefinition, compilationUnit);
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

      Type baseType;
      boolean baseIsStatic;
      if (fieldAccessExpression.getBaseExpression() != null)
      {
        resolve(fieldAccessExpression.getBaseExpression(), block, enclosingDefinition, compilationUnit);

        // find the type of the sub-expression, by calling the type checker
        // this is fine as long as we resolve all of the sub-expression first
        baseType = TypeChecker.checkTypes(fieldAccessExpression.getBaseExpression(), compilationUnit);
        baseIsStatic = false;
      }
      else if (fieldAccessExpression.getBaseType() != null)
      {
        baseType = fieldAccessExpression.getBaseType();
        resolve(baseType, compilationUnit);
        baseIsStatic = true;
      }
      else
      {
        throw new IllegalStateException("Unknown base type for a field access: " + fieldAccessExpression);
      }

      Set<Member> memberSet = baseType.getMembers(fieldName);
      Set<Member> filtered = new HashSet<Member>();
      for (Member member : memberSet)
      {
        if (member instanceof ArrayLengthMember)
        {
          if (baseIsStatic)
          {
            throw new ConceptualException("Cannot access the array length member statically", fieldAccessExpression.getLexicalPhrase());
          }
          filtered.add(member);
        }
        else if (member instanceof Field)
        {
          if (((Field) member).isStatic() == baseIsStatic)
          {
            filtered.add(member);
          }
        }
        else if (member instanceof Method)
        {
          if (((Method) member).isStatic() == baseIsStatic)
          {
            filtered.add(member);
          }
        }
        else
        {
          throw new IllegalStateException("Unknown member type: " + member);
        }
      }

      if (filtered.isEmpty())
      {
        throw new NameNotResolvedException("No such " + (baseIsStatic ? "static" : "non-static") + " member \"" + fieldName + "\" for type " + baseType, fieldAccessExpression.getLexicalPhrase());
      }
      if (filtered.size() > 1)
      {
        throw new ConceptualException("Multiple " + (baseIsStatic ? "static" : "non-static") + " members have the name '" + fieldName + "'", fieldAccessExpression.getLexicalPhrase());
      }
      fieldAccessExpression.setResolvedMember(filtered.iterator().next());
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

      Expression functionExpression = expr.getFunctionExpression();
      Type expressionType = null;
      Exception cachedException = null;
      // first, try to resolve the function call as a normal expression
      // this MUST be done first, so that local variables with function types are considered before outside methods
      try
      {
        resolve(functionExpression, block, enclosingDefinition, compilationUnit);
        expressionType = TypeChecker.checkTypes(functionExpression, compilationUnit);
      }
      catch (NameNotResolvedException e)
      {
        cachedException = e;
      }
      catch (ConceptualException e)
      {
        cachedException = e;
      }
      if (cachedException == null)
      {
        if (expressionType instanceof FunctionType)
        {
          // the sub-expressions all resolved properly, and we can leave it to the type checker to make sure the parameters match the arguments
          expr.setResolvedBaseExpression(functionExpression);
          return;
        }
        throw new ConceptualException("Cannot call a function on a non-function type", functionExpression.getLexicalPhrase());
      }

      // we failed to resolve the sub-expression into something with a function type
      // but the recursive resolver doesn't know which parameter types we're looking for here, so we may be able to consider some different options
      // we can do this by checking if the function expression is actually a variable access or a field access expression, and checking them for other sources of method calls,
      // such as constructor calls and method calls, each of which can be narrowed down by their parameter types

      // first, go through any bracketed expressions, as we can ignore them
      while (functionExpression instanceof BracketedExpression)
      {
        functionExpression = ((BracketedExpression) functionExpression).getExpression();
      }

      Map<Parameter[], Object> paramLists = new HashMap<Parameter[], Object>();
      Map<Method, Expression> methodBaseExpressions = new HashMap<Method, Expression>();
      if (functionExpression instanceof VariableExpression)
      {
        String name = ((VariableExpression) functionExpression).getName();
        // the sub-expression didn't resolve to a variable or a field, or we would have got a valid type back in expressionType
        if (enclosingDefinition != null)
        {
          Set<Method> methodSet = enclosingDefinition.getMethodsByName(name);
          if (methodSet != null)
          {
            for (Method m : methodSet)
            {
              paramLists.put(m.getParameters(), m);
              // leave methodBaseExpressions with a null value for this method, as we have no base expression
            }
          }
        }
        CompoundDefinition compoundDefinition = compilationUnit.getCompoundDefinition(name);
        if (compoundDefinition != null)
        {
          for (Constructor c : compoundDefinition.getConstructors())
          {
            paramLists.put(c.getParameters(), c);
          }
        }
      }
      else if (functionExpression instanceof FieldAccessExpression)
      {
        FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) functionExpression;
        String name = fieldAccessExpression.getFieldName();

        Expression baseExpression = fieldAccessExpression.getBaseExpression();
        Type baseType;
        boolean baseIsStatic;
        if (baseExpression != null)
        {
          resolve(baseExpression, block, enclosingDefinition, compilationUnit);

          // find the type of the sub-expression, by calling the type checker
          // this is fine as long as we resolve all of the sub-expression first
          baseType = TypeChecker.checkTypes(baseExpression, compilationUnit);
          baseIsStatic = false;
        }
        else if (fieldAccessExpression.getBaseType() != null)
        {
          baseType = fieldAccessExpression.getBaseType();
          resolve(baseType, compilationUnit);
          baseIsStatic = true;
        }
        else
        {
          throw new IllegalStateException("Unknown base type for a field access: " + fieldAccessExpression);
        }

        if (baseType instanceof NamedType)
        {
          CompoundDefinition compoundDefinition = ((NamedType) baseType).getResolvedDefinition();
          Set<Method> methodSet = compoundDefinition.getMethodsByName(name);
          if (methodSet != null)
          {
            for (Method m : methodSet)
            {
              // only allow access to this method if it is called in the right way, depending on whether or not it is static
              if (m.isStatic() == baseIsStatic)
              {
                paramLists.put(m.getParameters(), m);
                methodBaseExpressions.put(m, baseExpression);
              }
            }
          }
        }
      }

      // resolve the called function
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
          if (resolved)
          {
            throw new ConceptualException("Ambiguous function call, there are at least two applicable functions which take these arguments", expr.getLexicalPhrase());
          }
          else if (entry.getValue() instanceof Constructor)
          {
            expr.setResolvedConstructor((Constructor) entry.getValue());
          }
          else if (entry.getValue() instanceof Method)
          {
            expr.setResolvedMethod((Method) entry.getValue());
            expr.setResolvedBaseExpression(methodBaseExpressions.get(entry.getValue()));
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
        // we didn't find anything, so rethrow the exception from earlier
        if (cachedException instanceof NameNotResolvedException)
        {
          throw (NameNotResolvedException) cachedException;
        }
        throw (ConceptualException) cachedException;
      }
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
    else if (expression instanceof ShiftExpression)
    {
      resolve(((ShiftExpression) expression).getLeftExpression(), block, enclosingDefinition, compilationUnit);
      resolve(((ShiftExpression) expression).getRightExpression(), block, enclosingDefinition, compilationUnit);
    }
    else if (expression instanceof ThisExpression)
    {
      ThisExpression thisExpression = (ThisExpression) expression;
      if (enclosingDefinition == null)
      {
        throw new ConceptualException("'this' does not refer to anything in this context", thisExpression.getLexicalPhrase());
      }
      thisExpression.setType(new NamedType(false, enclosingDefinition));
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
          if (field.isStatic())
          {
            var = field.getGlobalVariable();
          }
          else
          {
            var = field.getMemberVariable();
          }
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
