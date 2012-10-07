package eu.bryants.anthony.plinth.ast.statement;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;

/*
 * Created on 18 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ForStatement extends BreakableStatement
{

  private Statement initStatement;
  private Expression conditional;
  private Statement updateStatement;
  private Block block;

  public ForStatement(Statement initStatement, Expression conditional, Statement updateStatement, Block block, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.initStatement = initStatement;
    this.conditional = conditional;
    this.updateStatement = updateStatement;
    this.block = block;
  }

  /**
   * @return the initStatement
   */
  public Statement getInitStatement()
  {
    return initStatement;
  }

  /**
   * @return the conditional
   */
  public Expression getConditional()
  {
    return conditional;
  }

  /**
   * @return the updateStatement
   */
  public Statement getUpdateStatement()
  {
    return updateStatement;
  }

  /**
   * @return the block
   */
  public Block getBlock()
  {
    return block;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopsExecution()
  {
    if (initStatement != null && initStatement.stopsExecution())
    {
      return true;
    }
    // this may return false positives before the control flow checker has been run
    // however, this should not be called before the control flow checker has checked the loop body
    return conditional == null && !isBrokenOutOf();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer("for (");
    if (initStatement == null)
    {
      buffer.append(";");
    }
    else
    {
      buffer.append(initStatement);
    }
    if (conditional != null)
    {
      buffer.append(" ");
      buffer.append(conditional);
    }
    buffer.append(";");
    if (updateStatement != null)
    {
      buffer.append(" ");
      buffer.append(updateStatement);
      // remove a semicolon from the end, if one exists
      if (buffer.substring(buffer.length() - 1).equals(";"))
      {
        buffer.replace(buffer.length() - 1, buffer.length(), "");
      }
    }
    buffer.append(")\n");
    buffer.append(block);
    return buffer.toString();
  }

}
