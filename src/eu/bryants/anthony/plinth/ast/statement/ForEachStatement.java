package eu.bryants.anthony.plinth.ast.statement;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.metadata.Variable;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 5 Sep 2013
 */

/**
 * @author Anthony Bryant
 */
public class ForEachStatement extends BreakableStatement
{
  private Type variableType;
  private boolean isVariableFinal;
  private String variableName;
  private Expression iterableExpression;
  private Block block;

  private Variable resolvedVariable;
  private Type resolvedIterableType;

  public ForEachStatement(Type variableType, boolean isVariableFinal, String variableName, Expression iterableExpression, Block block, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.variableType = variableType;
    this.isVariableFinal = isVariableFinal;
    this.variableName = variableName;
    this.iterableExpression = iterableExpression;
    this.block = block;
  }

  /**
   * @return the variableType
   */
  public Type getVariableType()
  {
    return variableType;
  }

  /**
   * @return the isVariableFinal
   */
  public boolean isVariableFinal()
  {
    return isVariableFinal;
  }

  /**
   * @return the variableName
   */
  public String getVariableName()
  {
    return variableName;
  }

  /**
   * @return the iterableExpression
   */
  public Expression getIterableExpression()
  {
    return iterableExpression;
  }

  /**
   * @return the block
   */
  public Block getBlock()
  {
    return block;
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
   * @return the resolvedIterableType
   */
  public Type getResolvedIterableType()
  {
    return resolvedIterableType;
  }

  /**
   * @param resolvedIterableType - the resolvedIterableType to set
   */
  public void setResolvedIterableType(Type resolvedIterableType)
  {
    this.resolvedIterableType = resolvedIterableType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopsExecution()
  {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "for " + (isVariableFinal ? "final " : "") + variableType + " " + variableName + " in " + iterableExpression + "\n" + block;
  }
}
