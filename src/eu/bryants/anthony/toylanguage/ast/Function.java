package eu.bryants.anthony.toylanguage.ast;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Function
{
  private Parameter[] parameters;
  private Expression expression;

  private LexicalPhrase lexicalPhrase;

  public Function(Parameter[] parameters, Expression expression, LexicalPhrase lexicalPhrase)
  {
    this.parameters = parameters;
    this.expression = expression;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the parameters
   */
  public Parameter[] getParameters()
  {
    return parameters;
  }
  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }
}
