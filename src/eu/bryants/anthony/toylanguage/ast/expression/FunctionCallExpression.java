package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.member.Method;

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
  // * just a resolvedConstructor, and no resolvedBaseExpression
  // * just a resolvedMethod, and no resolvedBaseExpression, in which case the method is assumed to be called on 'this' (or on nothing, if the method is static)
  // * a resolvedMethod and a resolvedBaseExpression, in which case the base expression has a type has the resolved method as a member
  private Expression resolvedBaseExpression;
  private Constructor resolvedConstructor;
  private Method resolvedMethod;

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
   * @return the resolvedMethod
   */
  public Method getResolvedMethod()
  {
    return resolvedMethod;
  }

  /**
   * @param resolvedMethod - the resolvedMethod to set
   */
  public void setResolvedMethod(Method resolvedMethod)
  {
    this.resolvedMethod = resolvedMethod;
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
