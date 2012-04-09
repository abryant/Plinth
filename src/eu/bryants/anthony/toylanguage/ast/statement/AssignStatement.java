package eu.bryants.anthony.toylanguage.ast.statement;

import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class AssignStatement extends Statement
{
  private Name variableName;
  private Expression expression;

  private Variable resolvedVariable;

  public AssignStatement(Name variableName, Expression expression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.variableName = variableName;
    this.expression = expression;
  }

  /**
   * @return the variableName
   */
  public Name getVariableName()
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
    return variableName + " = " + expression + ";";
  }
}
