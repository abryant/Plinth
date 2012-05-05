package eu.bryants.anthony.toylanguage.ast.misc;

import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 4 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class VariableAssignee extends Assignee
{
  private String variableName;

  private Variable resolvedVariable;

  public VariableAssignee(String variableName, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.variableName = variableName;
  }

  /**
   * @return the variableName
   */
  public String getVariableName()
  {
    return variableName;
  }

  /**
   * @return the resolvedVariable
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

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return variableName;
  }
}
