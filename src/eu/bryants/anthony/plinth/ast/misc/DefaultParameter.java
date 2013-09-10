package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 8 Sep 2013
 */

/**
 * @author Anthony Bryant
 */
public class DefaultParameter extends Parameter
{

  private Expression expression;

  public DefaultParameter(Type type, String name, Expression expression, LexicalPhrase lexicalPhrase)
  {
    super(name, lexicalPhrase);
    setType(type);
    this.expression = expression;
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
    return getType() + " " + getName() + "=" + (expression == null ? "..." : expression.toString());
  }
}
