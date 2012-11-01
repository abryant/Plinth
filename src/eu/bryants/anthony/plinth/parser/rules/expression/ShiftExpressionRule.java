package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.ShiftExpression;
import eu.bryants.anthony.plinth.ast.expression.ShiftExpression.ShiftOperator;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 14 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ShiftExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION                         = new Production<ParseType>(ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> LEFT_SHIFT_PRODUCTION              = new Production<ParseType>(ParseType.SHIFT_EXPRESSION, ParseType.DOUBLE_LANGLE, ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> LEFT_SHIFT_QNAME_PRODUCTION        = new Production<ParseType>(ParseType.SHIFT_EXPRESSION, ParseType.DOUBLE_LANGLE, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_LEFT_SHIFT_PRODUCTION        = new Production<ParseType>(ParseType.QNAME_EXPRESSION, ParseType.DOUBLE_LANGLE, ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> QNAME_LEFT_SHIFT_QNAME_PRODUCTION  = new Production<ParseType>(ParseType.QNAME_EXPRESSION, ParseType.DOUBLE_LANGLE, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> RIGHT_SHIFT_PRODUCTION             = new Production<ParseType>(ParseType.SHIFT_EXPRESSION, ParseType.DOUBLE_RANGLE, ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> RIGHT_SHIFT_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.SHIFT_EXPRESSION, ParseType.DOUBLE_RANGLE, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_RIGHT_SHIFT_PRODUCTION       = new Production<ParseType>(ParseType.QNAME_EXPRESSION, ParseType.DOUBLE_RANGLE, ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> QNAME_RIGHT_SHIFT_QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION, ParseType.DOUBLE_RANGLE, ParseType.QNAME_EXPRESSION);

  public ShiftExpressionRule()
  {
    super(ParseType.SHIFT_EXPRESSION, PRODUCTION,
                                      LEFT_SHIFT_PRODUCTION, LEFT_SHIFT_QNAME_PRODUCTION, QNAME_LEFT_SHIFT_PRODUCTION, QNAME_LEFT_SHIFT_QNAME_PRODUCTION,
                                      RIGHT_SHIFT_PRODUCTION, RIGHT_SHIFT_QNAME_PRODUCTION, QNAME_RIGHT_SHIFT_PRODUCTION, QNAME_RIGHT_SHIFT_QNAME_PRODUCTION);
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
    if (production == LEFT_SHIFT_PRODUCTION       || production == LEFT_SHIFT_QNAME_PRODUCTION ||
        production == QNAME_LEFT_SHIFT_PRODUCTION || production == QNAME_LEFT_SHIFT_QNAME_PRODUCTION)
    {
      operator = ShiftOperator.LEFT_SHIFT;
    }
    else if (production == RIGHT_SHIFT_PRODUCTION       || production == RIGHT_SHIFT_QNAME_PRODUCTION ||
             production == QNAME_RIGHT_SHIFT_PRODUCTION || production == QNAME_RIGHT_SHIFT_QNAME_PRODUCTION)
    {
      operator = ShiftOperator.RIGHT_SHIFT;
    }
    else
    {
      throw badTypeList();
    }

    Expression leftExpression = (Expression) args[0];
    Expression rightExpression = (Expression) args[2];
    return new ShiftExpression(leftExpression, rightExpression, operator, LexicalPhrase.combine(leftExpression.getLexicalPhrase(), (LexicalPhrase) args[1], rightExpression.getLexicalPhrase()));
  }

}
