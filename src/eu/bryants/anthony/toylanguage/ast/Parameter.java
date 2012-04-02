package eu.bryants.anthony.toylanguage.ast;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Parameter
{
  private String name;
  private LexicalPhrase lexicalPhrase;

  public Parameter(String name, LexicalPhrase lexicalPhrase)
  {
    this.name = name;
    this.lexicalPhrase = lexicalPhrase;
  }

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
}
