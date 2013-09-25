package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;

/*
 * Created on 22 Sep 2013
 */

/**
 * @author Anthony Bryant
 */
public class DefaultArgument extends Argument
{
  private String name;
  private Expression expression;

  public DefaultArgument(String name, Expression expression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.name = name;
    this.expression = expression;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
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
  public String toString()
  {
    return name + " = " + expression;
  }
}
