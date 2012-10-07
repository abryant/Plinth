package eu.bryants.anthony.plinth.ast.statement;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;

/*
 * Created on 7 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class IfStatement extends Statement
{

  private Expression expression;
  private Statement thenClause;
  private Statement elseClause;

  public IfStatement(Expression expression, Statement thenClause, Statement elseClause, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.expression = expression;
    this.thenClause = thenClause;
    this.elseClause = elseClause;
  }

  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
  }

  /**
   * @return the thenClause
   */
  public Statement getThenClause()
  {
    return thenClause;
  }

  /**
   * @return the elseClause
   */
  public Statement getElseClause()
  {
    return elseClause;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopsExecution()
  {
    return elseClause != null && thenClause.stopsExecution() && elseClause.stopsExecution();
  }

  @Override
  public String toString()
  {
    return "if " + expression + "\n" +
           thenClause +
           (elseClause == null ? "" : "\nelse" +
             (elseClause instanceof IfStatement ? " " : "\n") +
             elseClause);
  }
}
