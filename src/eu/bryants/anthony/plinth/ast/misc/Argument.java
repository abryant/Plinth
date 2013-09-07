package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;

/*
 * Created on 7 Sep 2013
 */

/**
 * @author Anthony Bryant
 */
public abstract class Argument
{
  private LexicalPhrase lexicalPhrase;

  public Argument(LexicalPhrase lexicalPhrase)
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
