package eu.bryants.anthony.toylanguage.ast.terminal;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;

/*
 * Created on 6 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class StringLiteral
{
  private LexicalPhrase lexicalPhrase;

  private String literalValue;
  private String stringRepresentation;

  /**
   * Creates a new String literal with the specified value
   * @param literalValue - the value of the string literal
   * @param stringRepresentation - the string representation of this literal, including escape sequences and surrounding quotes
   * @param lexicalPhrase - the lexical phrase associated with this string literal
   */
  public StringLiteral(String literalValue, String stringRepresentation, LexicalPhrase lexicalPhrase)
  {
    this.lexicalPhrase = lexicalPhrase;
    this.literalValue = literalValue;
    this.stringRepresentation = stringRepresentation;
  }

  /**
   * @return the literal string
   */
  public String getLiteralValue()
  {
    return literalValue;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

  /**
   * {@inheritDoc}
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return stringRepresentation;
  }
}
