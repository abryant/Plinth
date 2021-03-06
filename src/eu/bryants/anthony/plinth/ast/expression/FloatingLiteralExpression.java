package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.terminal.FloatingLiteral;

/*
 * Created on 9 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class FloatingLiteralExpression extends Expression
{
  private FloatingLiteral literal;

  public FloatingLiteralExpression(FloatingLiteral literal, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.literal = literal;
  }

  /**
   * @return the literal
   */
  public FloatingLiteral getLiteral()
  {
    return literal;
  }

  @Override
  public String toString()
  {
    return literal.toString();
  }
}
