package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 12 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class BitwiseNotExpression extends Expression
{

  private Expression expression;

  public BitwiseNotExpression(Expression expression, LexicalPhrase lexicalPhrase)
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
    return "~" + expression;
  }
}
