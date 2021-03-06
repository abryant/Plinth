package eu.bryants.anthony.plinth.ast.terminal;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Name
{
  private String name;

  private LexicalPhrase lexicalPhrase;

  public Name(String name, LexicalPhrase lexicalPhrase)
  {
    this.name = name;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
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
    return name;
  }
}
