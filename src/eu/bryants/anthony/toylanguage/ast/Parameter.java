package eu.bryants.anthony.toylanguage.ast;

import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Parameter
{
  private Type type;
  private String name;
  private LexicalPhrase lexicalPhrase;

  private Variable variable;
  private int index;

  public Parameter(Type type, String name, LexicalPhrase lexicalPhrase)
  {
    this.type = type;
    this.name = name;
    this.lexicalPhrase = lexicalPhrase;

    variable = new Variable(type, name);
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
    return type + " " + name;
  }
}
