package eu.bryants.anthony.plinth.ast.member;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;

/*
 * Created on 3 May 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class Member
{
  private LexicalPhrase lexicalPhrase;

  public Member(LexicalPhrase lexicalPhrase)
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
