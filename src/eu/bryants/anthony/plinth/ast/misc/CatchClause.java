package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.metadata.Variable;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 16 Feb 2013
 */

/**
 * @author Anthony Bryant
 */
public class CatchClause
{

  private Type[] caughtTypes;
  private boolean isVariableFinal;
  private String variableName;
  private Block block;
  private LexicalPhrase lexicalPhrase;

  private Variable resolvedExceptionVariable;

  public CatchClause(Type[] caughtTypes, boolean isVariableFinal, String variableName, Block block, LexicalPhrase lexicalPhrase)
  {
    this.caughtTypes = caughtTypes;
    this.isVariableFinal = isVariableFinal;
    this.variableName = variableName;
    this.block = block;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the caughtTypes
   */
  public Type[] getCaughtTypes()
  {
    return caughtTypes;
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
   * @return the block
   */
  public Block getBlock()
  {
    return block;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

  /**
   * @return the resolvedExceptionVariable
   */
  public Variable getResolvedExceptionVariable()
  {
    return resolvedExceptionVariable;
  }

  /**
   * @param resolvedExceptionVariable - the resolvedExceptionVariable to set
   */
  public void setResolvedExceptionVariable(Variable resolvedExceptionVariable)
  {
    this.resolvedExceptionVariable = resolvedExceptionVariable;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append("catch ");
    if (isVariableFinal)
    {
      buffer.append("final ");
    }
    for (int i = 0; i < caughtTypes.length; ++i)
    {
      buffer.append(caughtTypes[i]);
      if (i != caughtTypes.length - 1)
      {
        buffer.append(" | ");
      }
    }
    buffer.append(" ");
    buffer.append(variableName);
    buffer.append("\n");
    buffer.append(block);
    return buffer.toString();
  }
}
