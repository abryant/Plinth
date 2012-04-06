package eu.bryants.anthony.toylanguage.ast;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Parameter extends Variable
{
  private int index;
  private LexicalPhrase lexicalPhrase;

  public Parameter(String name, LexicalPhrase lexicalPhrase)
  {
    super(name);
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the index
   */
  public int getIndex()
  {
    return index;
  }

  /**
   * @param index - the index to set
   */
  public void setIndex(int index)
  {
    this.index = index;
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
    return getName();
  }
}
