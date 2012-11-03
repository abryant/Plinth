package eu.bryants.anthony.plinth.ast.statement;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;

/*
 * Created on 2 Nov 2012
 */

/**
 * @author Anthony Bryant
 */
public class DelegateConstructorStatement extends Statement
{
  private Expression[] arguments;

  public DelegateConstructorStatement(Expression[] arguments, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.arguments = arguments;
  }

  /**
   * @return the arguments
   */
  public Expression[] getArguments()
  {
    return arguments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopsExecution()
  {
    return false;
  }

  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append("this(");
    for (int i = 0; i < arguments.length; ++i)
    {
      buffer.append(arguments[i]);
      if (i != arguments.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(");");
    return buffer.toString();
  }

}
