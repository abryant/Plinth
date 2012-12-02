package eu.bryants.anthony.plinth.ast.statement;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.member.Constructor;

/*
 * Created on 2 Nov 2012
 */

/**
 * @author Anthony Bryant
 */
public class DelegateConstructorStatement extends Statement
{
  private boolean isSuperConstructor;
  private Expression[] arguments;

  private Constructor resolvedConstructor;

  public DelegateConstructorStatement(boolean isSuperConstructor, Expression[] arguments, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.isSuperConstructor = isSuperConstructor;
    this.arguments = arguments;
  }

  /**
   * @return the isSuperConstructor
   */
  public boolean isSuperConstructor()
  {
    return isSuperConstructor;
  }

  /**
   * @return the arguments
   */
  public Expression[] getArguments()
  {
    return arguments;
  }

  /**
   * @return the resolvedConstructor
   */
  public Constructor getResolvedConstructor()
  {
    return resolvedConstructor;
  }

  /**
   * @param resolvedConstructor - the resolvedConstructor to set
   */
  public void setResolvedConstructor(Constructor resolvedConstructor)
  {
    this.resolvedConstructor = resolvedConstructor;
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
    buffer.append(isSuperConstructor ? "super" : "this");
    buffer.append("(");
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
