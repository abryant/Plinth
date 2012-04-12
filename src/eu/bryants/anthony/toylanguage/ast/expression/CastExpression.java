package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 10 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class CastExpression extends Expression
{
  private Expression expression;

  public CastExpression(Type type, Expression expression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    setType(type); // the Type is stored in the superclass
    this.expression = expression;
  }

  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
  }

  @Override
  public String toString()
  {
    return "cast<" + getType() + "> " + expression;
  }
}
