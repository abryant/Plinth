package eu.bryants.anthony.plinth.ast.statement;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.CatchClause;

/*
 * Created on 16 Feb 2013
 */

/**
 * @author Anthony Bryant
 */
public class TryStatement extends Statement
{

  private Block tryBlock;
  private CatchClause[] catchClauses;
  private Block finallyBlock;

  public TryStatement(Block tryBlock, CatchClause[] catchClauses, Block finallyBlock, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.tryBlock = tryBlock;
    this.catchClauses = catchClauses;
    this.finallyBlock = finallyBlock;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopsExecution()
  {
    // a try statement stops execution if:
    // a) a finally block stops execution
    // b) the try block and all of the catch blocks stop execution
    if (finallyBlock != null && finallyBlock.stopsExecution())
    {
      return true;
    }
    if (!tryBlock.stopsExecution())
    {
      return false;
    }
    for (CatchClause catchClause : catchClauses)
    {
      if (!catchClause.getBlock().stopsExecution())
      {
        return false;
      }
    }
    return true;
  }

  /**
   * @return the tryBlock
   */
  public Block getTryBlock()
  {
    return tryBlock;
  }

  /**
   * @return the catchClauses
   */
  public CatchClause[] getCatchClauses()
  {
    return catchClauses;
  }

  /**
   * @return the finallyBlock
   */
  public Block getFinallyBlock()
  {
    return finallyBlock;
  }

  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append("try\n");
    buffer.append(tryBlock);
    for (CatchClause catchClause : catchClauses)
    {
      buffer.append("\n");
      buffer.append(catchClause);
    }
    if (finallyBlock != null)
    {
      buffer.append("\nfinally\n");
      buffer.append(finallyBlock);
    }
    return buffer.toString();
  }
}
