package eu.bryants.anthony.toylanguage.ast;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class Statement
{
  private LexicalPhrase lexicalPhrase;

  public Statement(LexicalPhrase lexicalPhrase)
  {
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }
}
