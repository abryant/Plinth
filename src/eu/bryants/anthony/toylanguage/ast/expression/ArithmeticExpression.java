package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ArithmeticExpression extends Expression
{
  public enum ArithmeticOperator
  {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    REMAINDER("%"),
    MODULO("%%"),
    ;
    private String stringRepresentation;

    ArithmeticOperator(String stringRepresentation)
    {
      this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String toString()
    {
      return stringRepresentation;
    }
  }

  private ArithmeticOperator operator;

  private Expression leftSubExpression;
  private Expression rightSubExpression;

  public ArithmeticExpression(ArithmeticOperator operator, Expression leftSubExpression, Expression rightSubExpression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.operator = operator;
    this.leftSubExpression = leftSubExpression;
    this.rightSubExpression = rightSubExpression;
  }

  /**
   * @return the operator
   */
  public ArithmeticOperator getOperator()
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
