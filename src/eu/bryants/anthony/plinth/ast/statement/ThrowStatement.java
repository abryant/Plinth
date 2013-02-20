package eu.bryants.anthony.plinth.ast.statement;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;

/*
 * Created on 15 Feb 2013
 */

/**
 * @author Anthony Bryant
 */
public class ThrowStatement extends Statement
{

  private Expression thrownExpression;

  public ThrowStatement(Expression thrownExpression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.thrownExpression = thrownExpression;
  }

  /**
   * @return the thrownExpression
   */
  public Expression getThrownExpression()
  {
    return thrownExpression;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopsExecution()
  {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "throw " + thrownExpression + ";";
  }

}
