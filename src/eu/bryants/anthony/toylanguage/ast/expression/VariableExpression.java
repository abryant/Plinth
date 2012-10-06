package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Method;
import eu.bryants.anthony.toylanguage.ast.metadata.Variable;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class VariableExpression extends Expression
{
  private String name;

  private Variable resolvedVariable;
  private Method resolvedMethod;

  public VariableExpression(String name, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.name = name;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the resolved variable
   */
  public Variable getResolvedVariable()
  {
    return resolvedVariable;
  }

  /**
   * @param resolvedVariable - the resolvedVariable to set
   */
  public void setResolvedVariable(Variable resolvedVariable)
  {
    this.resolvedVariable = resolvedVariable;
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
    return name;
  }
}
