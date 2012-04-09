package eu.bryants.anthony.toylanguage.ast;

import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
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

  private Variable resolvedVariable;

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
   * @return the resolved variable
   */
  public Variable getResolvedVariable()
  {
    return resolvedVariable;
  }

  /**
   * @param resolvedVariable - the resolvedVariable to set
   */
  public void setResolvedVariable(Variable resolvedVariable)
  {
    this.resolvedVariable = resolvedVariable;
  }

  @Override
  public String toString()
  {
    return name;
  }
}
