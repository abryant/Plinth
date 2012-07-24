package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;

/*
 * Created on 14 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class NullLiteralExpression extends Expression
{

  public NullLiteralExpression(LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "null";
  }
}
