package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 9 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ComparisonExpression extends Expression
{

  public enum ComparisonOperator
  {
    EQUAL("=="),
    NOT_EQUAL("!="),
    LESS_THAN("<"),
    LESS_THAN_EQUAL("<="),
    MORE_THAN(">"),
    MORE_THAN_EQUAL(">="),
    ;

    private String stringRepresentation;
    private ComparisonOperator(String stringRepresentation)
    {
      this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String toString()
    {
      return stringRepresentation;
    }
  }

  private Expression leftSubExpression;
  private Expression rightSubExpression;
  private ComparisonOperator operator;

  public ComparisonExpression(Expression leftSubExpression, Expression rightSubExpression, ComparisonOperator operator, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.operator = operator;
    this.leftSubExpression = leftSubExpression;
    this.rightSubExpression = rightSubExpression;
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

  /**
   * @return the operator
   */
  public ComparisonOperator getOperator()
  {
    return operator;
  }

  @Override
  public String toString()
  {
    return leftSubExpression + " " + operator + " " + rightSubExpression;
  }
}
