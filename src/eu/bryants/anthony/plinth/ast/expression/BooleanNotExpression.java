package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;

/*
 * Created on 12 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class BooleanNotExpression extends Expression
{

  private Expression expression;

  public BooleanNotExpression(Expression expression, LexicalPhrase lexicalPhrase)
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

  @Override
  public String toString()
  {
    return "!" + expression;
  }
}
