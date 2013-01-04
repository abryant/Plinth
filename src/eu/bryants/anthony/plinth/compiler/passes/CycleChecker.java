package eu.bryants.anthony.plinth.compiler.passes;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompilationUnit;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Field;
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
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.compiler.ConceptualException;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class CycleChecker
{

  /**
   * Checks for cycles in the class hierarchies each of the class definitions in the specified CompilationUnit.
   * @param compilationUnit - the CompilationUnit to check
   * @throws ConceptualException - if a cyclic class hierarchy is detected
   */
  public static void checkInheritanceCycles(CompilationUnit compilationUnit) throws ConceptualException
  {
    for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
    {
      if (!(typeDefinition instanceof ClassDefinition))
      {
        continue;
      }
      ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
      checkInheritanceCycles(classDefinition);
    }
  }

  /**
   * Checks for cycles in the specified ClassDefinition's class hierarchy.
   * @param classDefinition - the ClassDefinition to check for inheritance cycles
   * @throws ConceptualException - if a cyclic class hierarchy is detected
   */
  public static void checkInheritanceCycles(ClassDefinition classDefinition) throws ConceptualException
  {
    Set<ClassDefinition> visited = new HashSet<ClassDefinition>();
    ClassDefinition current = classDefinition;
    while (current != null)
    {
      if (visited.contains(current))
      {
        throw new ConceptualException("Cyclic inheritance hierarchy detected: " + current.getQualifiedName() + " extends itself (perhaps indirectly).", current.getLexicalPhrase());
      }
      visited.add(current);
      current = current.getSuperClassDefinition();
    }
  }

  /**
   * Checks for any cycles in the non-static fields of compound types, where one of its fields recursively contains a value of the initial type.
   * This would result in an impossible infinitely-deep object representation, so this method checks that it does not happen.
   * @param compilationUnit - the CompilationUnit to check
   * @throws ConceptualException - if a cycle is found
   */
  public static void checkCompoundTypeCycles(CompilationUnit compilationUnit) throws ConceptualException
  {
    for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
    {
      if (!(typeDefinition instanceof CompoundDefinition))
      {
        continue;
      }
      CompoundDefinition compoundDefinition = (CompoundDefinition) typeDefinition;
      Set<CompoundDefinition> visited = new HashSet<CompoundDefinition>();
      visited.add(compoundDefinition);
      checkCompoundTypeCycles(compoundDefinition, new LinkedList<Field>(), visited);
    }
  }

  private static void checkCompoundTypeCycles(CompoundDefinition startDefinition, List<Field> fieldStack, Set<CompoundDefinition> visited) throws ConceptualException
  {
    CompoundDefinition current = startDefinition;
    if (!fieldStack.isEmpty())
    {
      NamedType type = (NamedType) fieldStack.get(fieldStack.size() - 1).getType();
      current = (CompoundDefinition) type.getResolvedTypeDefinition();
    }
    for (Field field : current.getNonStaticFields())
    {
      Type type = field.getType();
      if (type instanceof NamedType)
      {
        TypeDefinition resolvedDefinition = ((NamedType) type).getResolvedTypeDefinition();
        if (!(resolvedDefinition instanceof CompoundDefinition))
        {
          // skip any NamedType which isn't for a CompoundDefinition
          continue;
        }
        CompoundDefinition compoundDefinition = (CompoundDefinition) resolvedDefinition;
        if (visited.contains(compoundDefinition))
        {
          StringBuffer buffer = new StringBuffer();
          for (Field f : fieldStack)
          {
            buffer.append(f.getName());
            buffer.append(".");
          }
          buffer.append(field.getName());
          throw new ConceptualException("Cycle detected in compound type '" + startDefinition.getQualifiedName() + "' at: " + buffer, field.getLexicalPhrase());
        }
        fieldStack.add(field);
        visited.add(compoundDefinition);
        checkCompoundTypeCycles(startDefinition, fieldStack, visited);
        visited.remove(compoundDefinition);
        fieldStack.remove(fieldStack.size() - 1);
      }
    }
  }

  /**
   * Checks that none of the constructors in any of the TypeDefinitions in the specified CompilationUnit can cyclically call each other.
   * This method will also populate the Constructors' data on whether they call delegate constructors.
   * @param compilationUnit - the CompilationUnit to check
   * @throws ConceptualException - if a cycle is detected in one of the Constructors in the specified CompilationUnit
   */
  public static void checkConstructorDelegateCycles(CompilationUnit compilationUnit) throws ConceptualException
  {
    for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
    {
      checkConstructorDelegateCycles(typeDefinition);
    }
  }

  /**
   * Checks that none of the constructors in the specified TypeDefinition can cyclically call each other.
   * This method will also populate the Constructors' data on whether they call delegate constructors.
   * @param typeDefinition - the TypeDefinition to check the Constructors of for cycles
   * @throws ConceptualException - if a cycle is detected in one of the Constructors of the specified TypeDefinition
   */
  private static void checkConstructorDelegateCycles(TypeDefinition typeDefinition) throws ConceptualException
  {
    Map<Constructor, List<Constructor>> constructorDelegates = new HashMap<Constructor, List<Constructor>>();
    for (Constructor constructor : typeDefinition.getAllConstructors())
    {
      List<Constructor> delegateConstructors = new LinkedList<Constructor>();
      findDelegateConstructors(constructor.getBlock(), delegateConstructors);
      // some of delegateConstructors may be super(...) constructors, which is allowed, and will not affect the cycle detection
      // in fact, they must be in the list so that setCallsDelegateConstructor() gets the correct value
      if (!delegateConstructors.isEmpty())
      {
        constructorDelegates.put(constructor, delegateConstructors);
      }
      constructor.setCallsDelegateConstructor(!delegateConstructors.isEmpty());
    }
    for (Constructor constructor : constructorDelegates.keySet())
    {
      Set<Constructor> path = new HashSet<Constructor>();
      Deque<Constructor> stack = new LinkedList<Constructor>();
      stack.push(constructor);
      while (!stack.isEmpty())
      {
        Constructor current = stack.peek();
        if (path.contains(current))
        {
          path.remove(current);
          stack.pop();
          continue;
        }
        path.add(current);
        List<Constructor> delegates = constructorDelegates.get(current);
        if (delegates != null)
        {
          for (Constructor delegate : delegates)
          {
            if (!path.contains(delegate))
            {
              stack.add(delegate);
            }
            else if (delegate == constructor)
            {
              throw new ConceptualException("Constructor may recursively call itself", constructor.getLexicalPhrase());
            }
          }
        }
      }
    }
  }

  /**
   * Finds all delegated constructors nested within the specified statement, and adds them to the specified list.
   * @param statement - the statement to search
   * @param delegateConstructors - the list of Constructors to add to
   */
  private static void findDelegateConstructors(Statement statement, List<Constructor> delegateConstructors)
  {
    if (statement instanceof AssignStatement ||
        statement instanceof BreakStatement ||
        statement instanceof ContinueStatement ||
        statement instanceof ExpressionStatement ||
        statement instanceof PrefixIncDecStatement ||
        statement instanceof ReturnStatement ||
        statement instanceof ShorthandAssignStatement)
    {
      // do nothing
    }
    else if (statement instanceof Block)
    {
      for (Statement s : ((Block) statement).getStatements())
      {
        findDelegateConstructors(s, delegateConstructors);
      }
    }
    else if (statement instanceof DelegateConstructorStatement)
    {
      delegateConstructors.add(((DelegateConstructorStatement) statement).getResolvedConstructor());
    }
    else if (statement instanceof ForStatement)
    {
      ForStatement forStatement = (ForStatement) statement;
      if (forStatement.getInitStatement() != null)
      {
        findDelegateConstructors(forStatement.getInitStatement(), delegateConstructors);
      }
      if (forStatement.getUpdateStatement() != null)
      {
        findDelegateConstructors(forStatement.getUpdateStatement(), delegateConstructors);
      }
      findDelegateConstructors(forStatement.getBlock(), delegateConstructors);
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      findDelegateConstructors(ifStatement.getThenClause(), delegateConstructors);
      if (ifStatement.getElseClause() != null)
      {
        findDelegateConstructors(ifStatement.getElseClause(), delegateConstructors);
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      findDelegateConstructors(whileStatement.getStatement(), delegateConstructors);
    }
    else
    {
      throw new IllegalArgumentException("Unknown statement type: " + statement);
    }
  }
}
