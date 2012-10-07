package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.terminal.IntegerLiteral;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class IntegerLiteralExpression extends Expression
{

  private IntegerLiteral literal;

  public IntegerLiteralExpression(IntegerLiteral literal, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.literal = literal;
  }

  /**
   * @return the literal
   */
  public IntegerLiteral getLiteral()
  {
    return literal;
  }

  @Override
  public String toString()
  {
    return literal.toString();
  }
}
