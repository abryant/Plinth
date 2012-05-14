package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.ShiftExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ShiftExpression.ShiftOperator;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 14 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ShiftExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> LEFT_SHIFT_PRODUCTION = new Production<ParseType>(ParseType.SHIFT_EXPRESSION, ParseType.DOUBLE_LANGLE, ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> ARITHMETIC_RIGHT_SHIFT_PRODUCTION = new Production<ParseType>(ParseType.SHIFT_EXPRESSION, ParseType.DOUBLE_RANGLE, ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> LOGICAL_RIGHT_SHIFT_PRODUCTION = new Production<ParseType>(ParseType.SHIFT_EXPRESSION, ParseType.TRIPLE_RANGLE, ParseType.ADDITIVE_EXPRESSION);

  @SuppressWarnings("unchecked")
  public ShiftExpressionRule()
  {
    super(ParseType.SHIFT_EXPRESSION, PRODUCTION, LEFT_SHIFT_PRODUCTION, ARITHMETIC_RIGHT_SHIFT_PRODUCTION, LOGICAL_RIGHT_SHIFT_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      return args[0];
    }
    ShiftOperator operator;
    if (production == LEFT_SHIFT_PRODUCTION) { operator = ShiftOperator.LEFT_SHIFT; }
    else if (production == ARITHMETIC_RIGHT_SHIFT_PRODUCTION) { operator = ShiftOperator.ARITHMETIC_RIGHT_SHIFT; }
    else if (production == LOGICAL_RIGHT_SHIFT_PRODUCTION) { operator = ShiftOperator.LOGICAL_RIGHT_SHIFT; }
    else { throw badTypeList(); }

    Expression leftExpression = (Expression) args[0];
    Expression rightExpression = (Expression) args[2];
    return new ShiftExpression(leftExpression, rightExpression, operator, LexicalPhrase.combine(leftExpression.getLexicalPhrase(), (LexicalPhrase) args[1], rightExpression.getLexicalPhrase()));
  }

}
