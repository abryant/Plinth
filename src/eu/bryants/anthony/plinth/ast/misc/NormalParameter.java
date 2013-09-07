package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.metadata.Variable;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 6 Sep 2013
 */

/**
 * @author Anthony Bryant
 */
public class NormalParameter extends Parameter
{

  private boolean isFinal;

  private Variable variable;

  public NormalParameter(boolean isFinal, Type type, String name, LexicalPhrase lexicalPhrase)
  {
    super(name, lexicalPhrase);
    setType(type);
    this.isFinal = isFinal;

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
   * @return the variable
   */
  public Variable getVariable()
  {
    return variable;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return (isFinal ? "final " : "") + getType() + " " + getName();
  }
}
