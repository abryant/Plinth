package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.metadata.Variable;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Parameter
{
  private boolean isFinal;
  private Type type;
  private String name;
  private LexicalPhrase lexicalPhrase;

  private Variable variable;
  private int index;

  public Parameter(boolean isFinal, Type type, String name, LexicalPhrase lexicalPhrase)
  {
    this.isFinal = isFinal;
    this.type = type;
    this.name = name;
    this.lexicalPhrase = lexicalPhrase;

    variable = new Variable(isFinal, type, name);
  }

  /**
   * @return the isFinal
   */
  public boolean isFinal()
  {
    return isFinal;
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
   * @return the variable
   */
  public Variable getVariable()
  {
    return variable;
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

  @Override
  public String toString()
  {
    return (isFinal ? "final " : "") + type + " " + name;
  }
}
