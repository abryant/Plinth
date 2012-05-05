package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 5 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class TupleIndexExpression extends Expression
{

  private Expression expression;
  private IntegerLiteral indexLiteral;

  public TupleIndexExpression(Expression expression, IntegerLiteral indexLiteral, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.expression = expression;
    this.indexLiteral = indexLiteral;
  }

  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
  }

  /**
   * @return the indexLiteral
   */
  public IntegerLiteral getIndexLiteral()
  {
    return indexLiteral;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return expression + " ! " + indexLiteral;
  }
}
