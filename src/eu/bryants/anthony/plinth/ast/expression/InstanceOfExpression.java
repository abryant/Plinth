package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 29 Jan 2013
 */

/**
 * @author Anthony Bryant
 */
public class InstanceOfExpression extends Expression
{

  private Expression expression;
  private Type instanceOfType;

  /**
   * Creates a new InstanceOfExpression to represent a statement of the form '&lt;expression&gt; instanceof &lt;type&gt;'
   * @param expression - the Expression to be checked against the type
   * @param instanceOfType - the type to check the Expression for
   * @param lexicalPhrase - the lexical information about where this expression was parsed from
   */
  public InstanceOfExpression(Expression expression, Type instanceOfType, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.expression = expression;
    this.instanceOfType = instanceOfType;
  }

  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
  }

  /**
   * @return the instanceOfType
   */
  public Type getInstanceOfType()
  {
    return instanceOfType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return expression + " instanceof " + instanceOfType;
  }
}
