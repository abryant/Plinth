package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;

/*
 * Created on 19 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ThisExpression extends Expression
{

  public ThisExpression(LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "this";
  }
}
