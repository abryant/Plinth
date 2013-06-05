package eu.bryants.anthony.plinth.ast.statement;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.metadata.ConstructorReference;

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

  private ConstructorReference resolvedConstructorReference;

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
   * @return the resolvedConstructorReference
   */
  public ConstructorReference getResolvedConstructorReference()
  {
    return resolvedConstructorReference;
  }

  /**
   * @param resolvedConstructorReference - the resolvedConstructorReference to set
   */
  public void setResolvedConstructorReference(ConstructorReference resolvedConstructorReference)
  {
    this.resolvedConstructorReference = resolvedConstructorReference;
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
