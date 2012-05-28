package eu.bryants.anthony.toylanguage.ast.misc;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;

/*
 * Created on 4 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ArrayElementAssignee extends Assignee
{
  private Expression arrayExpression;
  private Expression dimensionExpression;

  public ArrayElementAssignee(Expression arrayExpression, Expression dimensionExpression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.arrayExpression = arrayExpression;
    this.dimensionExpression = dimensionExpression;
  }

  /**
   * @return the arrayExpression
   */
  public Expression getArrayExpression()
  {
    return arrayExpression;
  }

  /**
   * @return the dimensionExpression
   */
  public Expression getDimensionExpression()
  {
    return dimensionExpression;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return arrayExpression + "[" + dimensionExpression + "]";
  }
}
