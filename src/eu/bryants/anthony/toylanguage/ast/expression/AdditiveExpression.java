package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class AdditiveExpression extends Expression
{
  private Expression leftSubExpression;
  private Expression rightSubExpression;

  public AdditiveExpression(Expression leftSubExpression, Expression rightSubExpression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.leftSubExpression = leftSubExpression;
    this.rightSubExpression = rightSubExpression;
  }

  /**
   * @return the leftSubExpression
   */
  public Expression getLeftSubExpression()
  {
    return leftSubExpression;
  }
  /**
   * @return the rightSubExpression
   */
  public Expression getRightSubExpression()
  {
    return rightSubExpression;
  }

  @Override
  public String toString()
  {
    return leftSubExpression + " + " + rightSubExpression;
  }
}
