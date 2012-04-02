package eu.bryants.anthony.toylanguage.ast;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class VariableExpression extends Expression
{
  private String name;

  private Parameter resolvedParameter;

  public VariableExpression(String name, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.name = name;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the resolvedParameter
   */
  public Parameter getResolvedParameter()
  {
    return resolvedParameter;
  }

  /**
   * @param resolvedParameter - the resolvedParameter to set
   */
  public void setResolvedParameter(Parameter resolvedParameter)
  {
    this.resolvedParameter = resolvedParameter;
  }

  @Override
  public String toString()
  {
    return name;
  }
}
