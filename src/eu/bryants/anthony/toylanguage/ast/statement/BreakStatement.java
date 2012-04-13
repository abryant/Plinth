package eu.bryants.anthony.toylanguage.ast.statement;

import eu.bryants.anthony.toylanguage.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 13 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class BreakStatement extends Statement
{

  private IntegerLiteral breakSteps;

  private BreakableStatement resolvedBreakable;

  public BreakStatement(IntegerLiteral breakSteps, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.breakSteps = breakSteps;
  }

  /**
   * @return the breakSteps
   */
  public IntegerLiteral getBreakSteps()
  {
    return breakSteps;
  }

  /**
   * @return the resolvedBreakable
   */
  public BreakableStatement getResolvedBreakable()
  {
    return resolvedBreakable;
  }

  /**
   * @param resolvedBreakable - the resolvedBreakable to set
   */
  public void setResolvedBreakable(BreakableStatement resolvedBreakable)
  {
    this.resolvedBreakable = resolvedBreakable;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopsExecution()
  {
    return true;
  }

  @Override
  public String toString()
  {
    if (breakSteps == null)
    {
      return "break;";
    }
    return "break " + breakSteps + ";";
  }
}