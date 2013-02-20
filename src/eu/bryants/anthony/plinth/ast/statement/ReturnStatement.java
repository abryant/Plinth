package eu.bryants.anthony.plinth.ast.statement;

import java.util.List;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ReturnStatement extends Statement
{
  private Expression expression;

  /**
   * The list of finally blocks that are broken through before returning.
   */
  private List<TryStatement> resolvedFinallyBlocks;
  /**
   * True if this return gets stopped by a finally block which stops execution, false otherwise.
   */
  private boolean stoppedByFinally;

  private boolean canReturnAgainstContextualImmutability;

  public ReturnStatement(Expression expression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.expression = expression;
  }

  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
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
   * @return the stoppedByFinally
   */
  public boolean isStoppedByFinally()
  {
    return stoppedByFinally;
  }

  /**
   * @param stoppedByFinally - the stoppedByFinally to set
   */
  public void setStoppedByFinally(boolean stoppedByFinally)
  {
    this.stoppedByFinally = stoppedByFinally;
  }

  /**
   * @return true if this return statement can return contextually immutable types even if the return type is not immutable, false otherwise
   */
  public boolean getCanReturnAgainstContextualImmutability()
  {
    return canReturnAgainstContextualImmutability;
  }

  /**
   * @param canReturnAgainstContextualImmutability - true iff this return statement should be able to return contextually immutable types even if the return type is not immutable
   */
  public void setCanReturnAgainstContextualImmutability(boolean canReturnAgainstContextualImmutability)
  {
    this.canReturnAgainstContextualImmutability = canReturnAgainstContextualImmutability;
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
    return "return " + expression + ";";
  }
}
