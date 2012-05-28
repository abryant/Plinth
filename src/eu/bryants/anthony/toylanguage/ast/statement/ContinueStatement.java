package eu.bryants.anthony.toylanguage.ast.statement;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.terminal.IntegerLiteral;

/*
 * Created on 13 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ContinueStatement extends Statement
{
  private IntegerLiteral continueSteps;

  private BreakableStatement resolvedBreakable;

  public ContinueStatement(IntegerLiteral breakSteps, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.continueSteps = breakSteps;
  }

  /**
   * @return the continueSteps
   */
  public IntegerLiteral getContinueSteps()
  {
    return continueSteps;
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
    if (continueSteps == null)
    {
      return "continue;";
    }
    return "continue " + continueSteps + ";";
  }
}
