package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 9 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class BooleanLiteralExpression extends Expression
{

  private boolean value;

  public BooleanLiteralExpression(boolean value, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.value = value;
  }

  /**
   * @return the value
   */
  public boolean getValue()
  {
    return value;
  }

  @Override
  public String toString()
  {
    return Boolean.toString(value);
  }
}
