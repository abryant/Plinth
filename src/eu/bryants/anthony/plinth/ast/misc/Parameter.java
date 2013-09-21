package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class Parameter
{
  private Type type;
  private String name;
  private LexicalPhrase lexicalPhrase;

  private int index;

  public Parameter(String name, LexicalPhrase lexicalPhrase)
  {
    this.name = name;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @param type - the type to set
   */
  protected void setType(Type type)
  {
    this.type = type;
  }

  /**
   * @return the type
   */
  public Type getType()
  {
    return type;
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
   * @return the mangled name of this parameter
   */
  public abstract String getMangledName();
}
