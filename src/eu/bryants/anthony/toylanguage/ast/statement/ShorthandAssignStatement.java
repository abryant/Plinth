package eu.bryants.anthony.toylanguage.ast.statement;

import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 26 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ShorthandAssignStatement extends Statement
{

  public enum ShorthandAssignmentOperator
  {
    ADD("+="),
    SUBTRACT("-="),
    MULTIPLY("*="),
    DIVIDE("/="),
    REMAINDER("%="),
    MODULO("%%="),
    LEFT_SHIFT("<<="),
    RIGHT_SHIFT(">>="),
    AND("&="),
    OR("|="),
    XOR("^="),
    ;
    private String stringRepresentation;
    private ShorthandAssignmentOperator(String stringRepresentation)
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

  private Assignee[] assignees;
  private ShorthandAssignmentOperator operator;
  private Expression expression;

  public ShorthandAssignStatement(Assignee[] assignees, ShorthandAssignmentOperator operator, Expression expression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.assignees = assignees;
    this.operator = operator;
    this.expression = expression;
  }

  /**
   * @return the assignees
   */
  public Assignee[] getAssignees()
  {
    return assignees;
  }

  /**
   * @return the operator
   */
  public ShorthandAssignmentOperator getOperator()
  {
    return operator;
  }

  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopsExecution()
  {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < assignees.length; ++i)
    {
      buffer.append(assignees[i]);
      if (i != assignees.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(' ');
    buffer.append(operator);
    buffer.append(' ');
    buffer.append(expression);
    buffer.append(';');
    return buffer.toString();
  }
}
