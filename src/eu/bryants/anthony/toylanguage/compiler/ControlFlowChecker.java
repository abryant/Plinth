package eu.bryants.anthony.toylanguage.compiler;

import eu.bryants.anthony.toylanguage.ast.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.Block;
import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.Function;
import eu.bryants.anthony.toylanguage.ast.IfStatement;
import eu.bryants.anthony.toylanguage.ast.ReturnStatement;
import eu.bryants.anthony.toylanguage.ast.Statement;
import eu.bryants.anthony.toylanguage.ast.VariableDefinition;
import eu.bryants.anthony.toylanguage.ast.WhileStatement;

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
    for (Function f : compilationUnit.getFunctions())
    {
      boolean returned = checkControlFlow(f.getBlock());
      if (!returned)
      {
        throw new ConceptualException("Function does not always return a value", f.getLexicalPhrase());
      }
    }
  }

  /**
   * Checks that the control flow of the specified statement is well defined.
   * @param statement - the statement to check
   * @return true if the statement returns from its enclosing function, false if control flow continues after it
   * @throws ConceptualException - if any unreachable code is detected
   */
  private static boolean checkControlFlow(Statement statement) throws ConceptualException
  {
    if (statement instanceof AssignStatement)
    {
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
        returned = checkControlFlow(s);
      }
      return returned;
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      Statement thenClause = ifStatement.getThenClause();
      Statement elseClause = ifStatement.getElseClause();
      if (elseClause == null)
      {
        checkControlFlow(thenClause);
        return false;
      }
      return checkControlFlow(thenClause) && checkControlFlow(elseClause);
    }
    else if (statement instanceof ReturnStatement)
    {
      return true;
    }
    else if (statement instanceof VariableDefinition)
    {
      return false;
    }
    else if (statement instanceof WhileStatement)
    {
      return checkControlFlow(((WhileStatement) statement).getStatement());
    }
    throw new ConceptualException("Internal control flow checking error: Unknown statement type", statement.getLexicalPhrase());
  }
}
