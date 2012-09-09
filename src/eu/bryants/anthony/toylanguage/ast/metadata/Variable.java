package eu.bryants.anthony.toylanguage.ast.metadata;

import eu.bryants.anthony.toylanguage.ast.type.Type;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Variable
{
  private boolean isFinal;
  private Type type;
  private String name;

  public Variable(boolean isFinal, Type type, String name)
  {
    this.isFinal = isFinal;
    this.type = type;
    this.name = name;
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
}
