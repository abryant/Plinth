package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.metadata.Variable;

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
  // whether or not this expression is in an immutable context
  // this is used by the TypeChecker to make variables implicitly immutable when in an immutable context
  private boolean resolvedContextImmutability;

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

  /**
   * @return the resolvedContextImmutability
   */
  public boolean getResolvedContextImmutability()
  {
    return resolvedContextImmutability;
  }

  /**
   * @param resolvedContextImmutability - the resolvedContextImmutability to set
   */
  public void setResolvedContextImmutability(boolean resolvedContextImmutability)
  {
    this.resolvedContextImmutability = resolvedContextImmutability;
  }

  @Override
  public String toString()
  {
    return name;
  }
}
