package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;

/*
 * Created on 26 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class NullCoalescingExpression extends Expression
{

  private Expression nullableExpression;
  private Expression alternativeExpression;

  public NullCoalescingExpression(Expression nullableExpression, Expression alternativeExpression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.nullableExpression = nullableExpression;
    this.alternativeExpression = alternativeExpression;
  }

  /**
   * @return the nullableExpression
   */
  public Expression getNullableExpression()
  {
    return nullableExpression;
  }

  /**
   * @return the alternativeExpression
   */
  public Expression getAlternativeExpression()
  {
    return alternativeExpression;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return nullableExpression + " ?: " + alternativeExpression;
  }
}
