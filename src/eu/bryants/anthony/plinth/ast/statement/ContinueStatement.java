package eu.bryants.anthony.plinth.ast.statement;

import java.util.List;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.terminal.IntegerLiteral;

/*
 * Created on 13 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ContinueStatement extends Statement
{
  private IntegerLiteral continueSteps;

  /**
   * The list of finally blocks that are continued through before reaching the BreakableStatement.
   */
  private List<TryStatement> resolvedFinallyBlocks;
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
   * @return the resolvedFinallyBlocks
   */
  public List<TryStatement> getResolvedFinallyBlocks()
  {
    return resolvedFinallyBlocks;
  }

  /**
   * @param resolvedFinallyBlocks - the resolvedFinallyBlocks to set
   */
  public void setResolvedFinallyBlocks(List<TryStatement> resolvedFinallyBlocks)
  {
    this.resolvedFinallyBlocks = resolvedFinallyBlocks;
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
