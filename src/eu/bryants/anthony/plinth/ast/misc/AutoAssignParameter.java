package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.metadata.Variable;

/*
 * Created on 6 Sep 2013
 */

/**
 * @author Anthony Bryant
 */
public class AutoAssignParameter extends Parameter
{

  private Variable resolvedVariable;
  private boolean propertyConstructorCall;

  public AutoAssignParameter(String name, LexicalPhrase lexicalPhrase)
  {
    super(name, lexicalPhrase);
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
    setType(resolvedVariable.getType());
  }

  /**
   * @return the propertyConstructorCall
   */
  public boolean isPropertyConstructorCall()
  {
    return propertyConstructorCall;
  }

  /**
   * @param propertyConstructorCall - the propertyConstructorCall to set
   */
  public void setPropertyConstructorCall(boolean propertyConstructorCall)
  {
    this.propertyConstructorCall = propertyConstructorCall;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    return getType().getMangledName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "@" + getName();
  }
}
