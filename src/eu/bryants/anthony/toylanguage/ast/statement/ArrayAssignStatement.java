package eu.bryants.anthony.toylanguage.ast.statement;

import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 4 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ArrayAssignStatement extends Statement
{

  private Expression arrayExpression;
  private Expression dimensionExpression;
  private Expression valueExpression;

  public ArrayAssignStatement(Expression arrayExpression, Expression dimensionExpression, Expression valueExpression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.arrayExpression = arrayExpression;
    this.dimensionExpression = dimensionExpression;
    this.valueExpression = valueExpression;
  }

  /**
   * @return the arrayExpression
   */
  public Expression getArrayExpression()
  {
    return arrayExpression;
  }

  /**
   * @return the dimensionExpression
   */
  public Expression getDimensionExpression()
  {
    return dimensionExpression;
  }

  /**
   * @return the valueExpression
   */
  public Expression getValueExpression()
  {
    return valueExpression;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopsExecution()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return arrayExpression + "[" + dimensionExpression + "] = " + valueExpression + ";";
  }

}
