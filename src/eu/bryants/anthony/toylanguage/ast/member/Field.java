package eu.bryants.anthony.toylanguage.ast.member;

import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class Field extends Member
{

  private Type type;
  private String name;

  private int index;

  public Field(Type type, String name, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.type = type;
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
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
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return type + " " + name + ";";
  }
}
