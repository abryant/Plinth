package eu.bryants.anthony.toylanguage.ast.misc;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 4 May 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class Assignee
{
  private LexicalPhrase lexicalPhrase;

  public Assignee(LexicalPhrase lexicalPhrase)
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
