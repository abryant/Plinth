package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 7 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class InlineIfExpression extends Expression
{

  private Expression condition;
  private Expression thenExpression;
  private Expression elseExpression;

  public InlineIfExpression(Expression condition, Expression thenExpression, Expression elseExpression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.condition = condition;
    this.thenExpression = thenExpression;
    this.elseExpression = elseExpression;
  }

  /**
   * @return the condition
   */
  public Expression getCondition()
  {
    return condition;
  }

  /**
   * @return the thenExpression
   */
  public Expression getThenExpression()
  {
    return thenExpression;
  }

  /**
   * @return the elseExpression
   */
  public Expression getElseExpression()
  {
    return elseExpression;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return condition + " ? " + thenExpression + " : " + elseExpression;
  }
}
