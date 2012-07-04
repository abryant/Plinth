package eu.bryants.anthony.toylanguage.ast.misc;

import eu.bryants.anthony.toylanguage.ast.expression.FieldAccessExpression;

/*
 * Created on 19 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class FieldAssignee extends Assignee
{

  private FieldAccessExpression fieldAccessExpression;

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
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return fieldAccessExpression.toString();
  }
}
