package eu.bryants.anthony.toylanguage.ast.statement;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.type.Type;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class AssignStatement extends Statement
{
  private boolean isFinal;
  private Type type;
  private Assignee[] assignees;
  private Expression expression;

  private Type resolvedType;

  public AssignStatement(boolean isFinal, Type type, Assignee[] assignees, Expression expression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.isFinal = isFinal;
    this.type = type;
    this.assignees = assignees;
    this.expression = expression;
    resolvedType = type;
  }

  /**
   * @return the isFinal
   */
  public boolean isFinal()
  {
    return isFinal;
  }

  /**
   * @return the type
   */
  public Type getType()
  {
    return type;
  }

  /**
   * @return the assignees
   */
  public Assignee[] getAssignees()
  {
    return assignees;
  }

  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
  }

  /**
   * @return the resolvedType
   */
  public Type getResolvedType()
  {
    return resolvedType;
  }

  /**
   * @param resolvedType - the resolvedType to set
   */
  public void setResolvedType(Type resolvedType)
  {
    this.resolvedType = resolvedType;
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
    if (type != null)
    {
      if (isFinal)
      {
        buffer.append("final ");
      }
      buffer.append(type);
      buffer.append(" ");
    }
    for (int i = 0; i < assignees.length; i++)
    {
      buffer.append(assignees[i]);
      if (i != assignees.length - 1)
      {
        buffer.append(", ");
      }
    }
    if (expression != null)
    {
      buffer.append(" = ");
      buffer.append(expression);
    }
    buffer.append(";");
    return buffer.toString();
  }
}
