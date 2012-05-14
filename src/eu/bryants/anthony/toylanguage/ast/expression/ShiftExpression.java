package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 14 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ShiftExpression extends Expression
{
  public enum ShiftOperator
  {
    LEFT_SHIFT("<<"),
    ARITHMETIC_RIGHT_SHIFT(">>"),
    LOGICAL_RIGHT_SHIFT(">>>"),
    ;

    private String stringRepresentation;

    private ShiftOperator(String stringRepresentation)
    {
      this.stringRepresentation = stringRepresentation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
      return stringRepresentation;
    }
  }

  private Expression leftExpression;
  private Expression rightExpression;
  private ShiftOperator operator;

  public ShiftExpression(Expression leftExpression, Expression rightExpression, ShiftOperator operator, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.leftExpression = leftExpression;
    this.rightExpression = rightExpression;
    this.operator = operator;
  }

  /**
   * @return the leftExpression
   */
  public Expression getLeftExpression()
  {
    return leftExpression;
  }

  /**
   * @return the rightExpression
   */
  public Expression getRightExpression()
  {
    return rightExpression;
  }

  /**
   * @return the operator
   */
  public ShiftOperator getOperator()
  {
    return operator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return leftExpression + " " + operator + " " + rightExpression;
  }
}
