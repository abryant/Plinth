package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 12 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class LogicalExpression extends Expression
{

  public enum LogicalOperator
  {
    AND("&"),
    OR("|"),
    XOR("^"),
    SHORT_CIRCUIT_AND("&&"),
    SHORT_CIRCUIT_OR("||"),
    ;
    private String stringRepresentation;

    LogicalOperator(String stringRepresentation)
    {
      this.stringRepresentation = stringRepresentation;
    }
    @Override
    public String toString()
    {
      return stringRepresentation;
    }
  }

  private LogicalOperator operator;
  private Expression leftSubExpression;
  private Expression rightSubExpression;

  public LogicalExpression(LogicalOperator operator, Expression leftSubExpression, Expression rightSubExpression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.operator = operator;
    this.leftSubExpression = leftSubExpression;
    this.rightSubExpression = rightSubExpression;
  }

  /**
   * @return the operator
   */
  public LogicalOperator getOperator()
  {
    return operator;
  }

  /**
   * @return the leftSubExpression
   */
  public Expression getLeftSubExpression()
  {
    return leftSubExpression;
  }

  /**
   * @return the rightSubExpression
   */
  public Expression getRightSubExpression()
  {
    return rightSubExpression;
  }

  @Override
  public String toString()
  {
    return leftSubExpression + " " + operator + " " + rightSubExpression;
  }
}
