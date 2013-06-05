package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;
import eu.bryants.anthony.plinth.ast.metadata.Variable;

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
  private MemberReference<?> resolvedMemberReference;
  private boolean isPropertyConstructorCall;

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
   * @return the resolvedMemberReference
   */
  public MemberReference<?> getResolvedMemberReference()
  {
    return resolvedMemberReference;
  }

  /**
   * @param resolvedMemberReference - the resolvedMemberReference to set
   */
  public void setResolvedMemberReference(MemberReference<?> resolvedMemberReference)
  {
    this.resolvedMemberReference = resolvedMemberReference;
  }

  /**
   * @return the isPropertyConstructorCall
   */
  public boolean isPropertyConstructorCall()
  {
    return isPropertyConstructorCall;
  }

  /**
   * @param isPropertyConstructorCall - the isPropertyConstructorCall to set
   */
  public void setPropertyConstructorCall(boolean isPropertyConstructorCall)
  {
    this.isPropertyConstructorCall = isPropertyConstructorCall;
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
