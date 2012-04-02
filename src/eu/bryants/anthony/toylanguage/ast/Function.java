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
  private String name;
  private Parameter[] parameters;
  private Expression expression;

  private LexicalPhrase lexicalPhrase;

  public Function(String name, Parameter[] parameters, Expression expression, LexicalPhrase lexicalPhrase)
  {
    this.name = name;
    this.parameters = parameters;
    this.expression = expression;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
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

  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer(name);
    buffer.append('(');
    for (int i = 0; i < parameters.length; i++)
    {
      buffer.append(parameters[i]);
      if (i != parameters.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append("): ");
    buffer.append(expression);
    buffer.append(';');
    return buffer.toString();
  }
}
