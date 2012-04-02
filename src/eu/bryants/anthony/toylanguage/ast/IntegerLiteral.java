package eu.bryants.anthony.toylanguage.ast;

import java.math.BigInteger;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class IntegerLiteral
{

  private BigInteger value;
  private String stringRepresentation;

  private LexicalPhrase lexicalPhrase;

  public IntegerLiteral(BigInteger value, String stringRepresentation, LexicalPhrase lexicalPhrase)
  {
    this.value = value;
    this.stringRepresentation = stringRepresentation;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the value
   */
  public BigInteger getValue()
  {
    return value;
  }

  /**
   * @return the stringRepresentation
   */
  public String getStringRepresentation()
  {
    return stringRepresentation;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

  @Override
  public String toString()
  {
    return stringRepresentation;
  }
}
