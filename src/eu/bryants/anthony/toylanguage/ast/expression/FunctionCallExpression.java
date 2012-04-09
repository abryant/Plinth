package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.Function;
import eu.bryants.anthony.toylanguage.compiler.ConceptualException;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class FunctionCallExpression extends Expression
{
  private String name;
  private Expression[] arguments;

  private Function resolvedFunction;

  public FunctionCallExpression(String name, Expression[] arguments, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.name = name;
    this.arguments = arguments;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the arguments
   */
  public Expression[] getArguments()
  {
    return arguments;
  }

  /**
   * @return the resolvedFunction
   */
  public Function getResolvedFunction()
  {
    return resolvedFunction;
  }

  /**
   * @param resolvedFunction - the resolvedFunction to set
   * @throws ConceptualException - if the specified function takes a different number of arguments than this call provides
   */
  public void setResolvedFunction(Function resolvedFunction) throws ConceptualException
  {
    this.resolvedFunction = resolvedFunction;
  }

  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer(name);
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
