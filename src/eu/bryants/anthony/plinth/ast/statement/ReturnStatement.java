package eu.bryants.anthony.plinth.ast.statement;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ReturnStatement extends Statement
{
  private Expression expression;

  private boolean canReturnAgainstContextualImmutability;

  public ReturnStatement(Expression expression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.expression = expression;
  }

  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
  }

  /**
   * @return true if this return statement can return contextually immutable types even if the return type is not immutable, false otherwise
   */
  public boolean getCanReturnAgainstContextualImmutability()
  {
    return canReturnAgainstContextualImmutability;
  }

  /**
   * @param canReturnAgainstContextualImmutability - true iff this return statement should be able to return contextually immutable types even if the return type is not immutable
   */
  public void setCanReturnAgainstContextualImmutability(boolean canReturnAgainstContextualImmutability)
  {
    this.canReturnAgainstContextualImmutability = canReturnAgainstContextualImmutability;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopsExecution()
  {
    return true;
  }

  @Override
  public String toString()
  {
    return "return " + expression + ";";
  }
}
