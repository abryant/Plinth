package eu.bryants.anthony.toylanguage.ast;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ReturnStatement extends Statement
{
  private Expression expression;

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

  @Override
  public String toString()
  {
    return "return " + expression + ";";
  }
}