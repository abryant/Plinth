package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.expression.FieldAccessExpression;

/*
 * Created on 19 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class FieldAssignee extends Assignee
{

  private FieldAccessExpression fieldAccessExpression;
  private boolean isPropertyConstructorCall;

  public FieldAssignee(FieldAccessExpression fieldAccessExpression)
  {
    super(fieldAccessExpression.getLexicalPhrase());
    this.fieldAccessExpression = fieldAccessExpression;
  }

  /**
   * @return the FieldAccessExpression that this assignee is based on
   */
  public FieldAccessExpression getFieldAccessExpression()
  {
    return fieldAccessExpression;
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
    return fieldAccessExpression.toString();
  }
}
