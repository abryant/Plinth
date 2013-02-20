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
public class BreakStatement extends Statement
{
  private IntegerLiteral breakSteps;

  /**
   * The list of finally blocks that are broken through before reaching the BreakableStatement.
   */
  private List<TryStatement> resolvedFinallyBlocks;
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
    if (breakSteps == null)
    {
      return "break;";
    }
    return "break " + breakSteps + ";";
  }
}
