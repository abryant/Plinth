package eu.bryants.anthony.toylanguage.ast;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Variable
{
  private String name;

  public Variable(String name)
  {
    this.name = name;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }
}
