package eu.bryants.anthony.toylanguage.ast;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class AssignStatement extends Statement
{
  private String variableName;
  private Expression expression;

  private Variable resolvedVariable;

  public AssignStatement(String variableName, Expression expression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.variableName = variableName;
    this.expression = expression;
  }

  /**
   * @return the variableName
   */
  public String getVariableName()
  {
    return variableName;
  }

  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
  }

  /**
   * @return the resolvedVariable
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

  @Override
  public String toString()
  {
    return variableName + " = " + expression + ";";
  }
}
