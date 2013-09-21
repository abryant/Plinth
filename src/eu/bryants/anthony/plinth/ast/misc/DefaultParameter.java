package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.metadata.Variable;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 8 Sep 2013
 */

/**
 * @author Anthony Bryant
 */
public class DefaultParameter extends Parameter
{
  private boolean isFinal;

  private Expression expression;

  private Variable variable;

  public DefaultParameter(boolean isFinal, Type type, String name, Expression expression, LexicalPhrase lexicalPhrase)
  {
    super(name, lexicalPhrase);
    setType(type);
    this.isFinal = isFinal;
    this.expression = expression;

    if (expression != null)
    {
      variable = new Variable(isFinal, type, name);
    }
  }

  /**
   * @return the isFinal
   */
  public boolean isFinal()
  {
    return isFinal;
  }

  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
  }

  /**
   * @return the variable
   */
  public Variable getVariable()
  {
    return variable;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    return getType().getMangledName() + getName().getBytes().length + getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return (isFinal ? "final " : "") + getType() + " " + getName() + "=" + (expression == null ? "..." : expression.toString());
  }
}
