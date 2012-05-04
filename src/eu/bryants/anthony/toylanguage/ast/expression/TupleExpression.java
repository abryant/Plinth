package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 4 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class TupleExpression extends Expression
{

  private Expression[] subExpressions;

  public TupleExpression(Expression[] subExpressions, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.subExpressions = subExpressions;
  }

  /**
   * @return the subExpressions
   */
  public Expression[] getSubExpressions()
  {
    return subExpressions;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer("(");
    for (int i = 0; i < subExpressions.length; i++)
    {
      buffer.append(subExpressions[i]);
      if (i != subExpressions.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(")");
    return buffer.toString();
  }
}
