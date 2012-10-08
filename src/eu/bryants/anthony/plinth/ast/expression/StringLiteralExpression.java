package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.terminal.StringLiteral;

/*
 * Created on 8 Oct 2012
 */

/**
 * @author Anthony Bryant
 */
public class StringLiteralExpression extends Expression
{
  private StringLiteral literal;

  /**
   * Creates a new StringLiteralExpression with the specified literal and LexicalPhrase
   * @param literal - the StringLiteral for this StringLiteralExpression to hold
   * @param lexicalPhrase - the LexicalPhrase of this StringLiteralExpression
   */
  public StringLiteralExpression(StringLiteral literal, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.literal = literal;
  }

  /**
   * @return the literal
   */
  public StringLiteral getLiteral()
  {
    return literal;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return literal.toString();
  }
}
