package eu.bryants.anthony.toylanguage.ast.statement;

import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class WhileStatement extends Statement
{

  private Expression expression;
  private Statement statement;

  public WhileStatement(Expression expression, Statement statement, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.expression = expression;
    this.statement = statement;
  }

  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
  }

  /**
   * @return the statement
   */
  public Statement getStatement()
  {
    return statement;
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
    return "while " + expression + "\n" + statement;
  }
}
