package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class FunctionCallExpression extends Expression
{
  private Expression functionExpression;
  private Expression[] arguments;

  // when this has been resolved (assuming there were no errors) we will have one of the following situations:
  // * just a resolvedBaseExpression, which has a function type
  // * just a resolvedMethodReference, and no resolvedBaseExpression, in which case the method is assumed to be called on 'this' (or on nothing, if the method is static)
  // * a resolvedMethodReference and a resolvedBaseExpression, in which case the base expression has a type has the resolved method as a member
  //   in this last case, a resolvedNullTraversal is specified, which specifies whether or not this expression will just return null if the base expression is null
  private Expression resolvedBaseExpression;
  private MethodReference resolvedMethodReference;
  private boolean resolvedNullTraversal;
  // if there is a resolved method and no resolved base expression, the function call may be resolved as non-virtual, this represents calls of the form 'super.method()'
  private boolean resolvedIsVirtual = true;

  public FunctionCallExpression(Expression functionExpression, Expression[] arguments, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.functionExpression = functionExpression;
    this.arguments = arguments;
  }

  /**
   * @return the functionExpression
   */
  public Expression getFunctionExpression()
  {
    return functionExpression;
  }

  /**
   * @return the arguments
   */
  public Expression[] getArguments()
  {
    return arguments;
  }

  /**
   * @return the resolvedBaseExpression
   */
  public Expression getResolvedBaseExpression()
  {
    return resolvedBaseExpression;
  }

  /**
   * @param resolvedBaseExpression - the resolvedBaseExpression to set
   */
  public void setResolvedBaseExpression(Expression resolvedBaseExpression)
  {
    this.resolvedBaseExpression = resolvedBaseExpression;
  }

  /**
   * @return the resolvedMethodReference
   */
  public MethodReference getResolvedMethodReference()
  {
    return resolvedMethodReference;
  }

  /**
   * @param resolvedMethodReference - the resolvedMethodReference to set
   */
  public void setResolvedMethodReference(MethodReference resolvedMethodReference)
  {
    this.resolvedMethodReference = resolvedMethodReference;
  }

  /**
   * @return the resolvedNullTraversal
   */
  public boolean getResolvedNullTraversal()
  {
    return resolvedNullTraversal;
  }

  /**
   * @param resolvedNullTraversal - the resolvedNullTraversal to set
   */
  public void setResolvedNullTraversal(boolean resolvedNullTraversal)
  {
    this.resolvedNullTraversal = resolvedNullTraversal;
  }

  /**
   * @return the resolvedIsVirtual
   */
  public boolean getResolvedIsVirtual()
  {
    return resolvedIsVirtual;
  }

  /**
   * @param resolvedIsVirtual - the resolvedIsVirtual to set
   */
  public void setResolvedIsVirtual(boolean resolvedIsVirtual)
  {
    this.resolvedIsVirtual = resolvedIsVirtual;
  }

  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer(functionExpression.toString());
    buffer.append('(');
    for (int i = 0; i < arguments.length; i++)
    {
      buffer.append(arguments[i]);
      if (i != arguments.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(')');
    return buffer.toString();
  }
}
