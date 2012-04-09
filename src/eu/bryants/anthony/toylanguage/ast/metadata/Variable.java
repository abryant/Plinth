package eu.bryants.anthony.toylanguage.ast.metadata;

import eu.bryants.anthony.toylanguage.ast.Type;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Variable
{
  private Type type;
  private String name;

  public Variable(Type type, String name)
  {
    this.type = type;
    this.name = name;
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
