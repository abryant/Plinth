package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;

/*
 * Created on 4 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ArrayAccessExpression extends Expression
{

  private Expression arrayExpression;
  private Expression dimensionExpression;

  public ArrayAccessExpression(Expression arrayExpression, Expression dimensionExpression, LexicalPhrase lexicalPhrase)
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
