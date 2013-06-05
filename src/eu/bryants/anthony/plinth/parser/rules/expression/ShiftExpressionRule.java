package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.ShiftExpression;
import eu.bryants.anthony.plinth.ast.expression.ShiftExpression.ShiftOperator;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 14 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ShiftExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION                                = new Production<ParseType>(ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> LEFT_SHIFT_PRODUCTION                     = new Production<ParseType>(ParseType.SHIFT_EXPRESSION,  ParseType.DOUBLE_LANGLE, ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> LEFT_SHIFT_QNAME_PRODUCTION               = new Production<ParseType>(ParseType.SHIFT_EXPRESSION,  ParseType.DOUBLE_LANGLE, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_LEFT_SHIFT_PRODUCTION               = new Production<ParseType>(ParseType.QNAME_EXPRESSION,  ParseType.DOUBLE_LANGLE, ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> QNAME_LEFT_SHIFT_QNAME_PRODUCTION         = new Production<ParseType>(ParseType.QNAME_EXPRESSION,  ParseType.DOUBLE_LANGLE, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> RIGHT_SHIFT_PRODUCTION                    = new Production<ParseType>(ParseType.SHIFT_EXPRESSION,  ParseType.DOUBLE_RANGLE, ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> RIGHT_SHIFT_QNAME_PRODUCTION              = new Production<ParseType>(ParseType.SHIFT_EXPRESSION,  ParseType.DOUBLE_RANGLE, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_RIGHT_SHIFT_PRODUCTION              = new Production<ParseType>(ParseType.QNAME,             ParseType.DOUBLE_RANGLE, ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> NESTED_QNAME_RIGHT_SHIFT_PRODUCTION       = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.DOUBLE_RANGLE, ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> QNAME_RIGHT_SHIFT_QNAME_PRODUCTION        = new Production<ParseType>(ParseType.QNAME,             ParseType.DOUBLE_RANGLE, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> NESTED_QNAME_RIGHT_SHIFT_QNAME_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.DOUBLE_RANGLE, ParseType.QNAME_EXPRESSION);

  public ShiftExpressionRule()
  {
    super(ParseType.SHIFT_EXPRESSION, PRODUCTION,
                                      LEFT_SHIFT_PRODUCTION, LEFT_SHIFT_QNAME_PRODUCTION, QNAME_LEFT_SHIFT_PRODUCTION, QNAME_LEFT_SHIFT_QNAME_PRODUCTION,
                                      RIGHT_SHIFT_PRODUCTION, RIGHT_SHIFT_QNAME_PRODUCTION,
                                      QNAME_RIGHT_SHIFT_PRODUCTION, NESTED_QNAME_RIGHT_SHIFT_PRODUCTION,
                                      QNAME_RIGHT_SHIFT_QNAME_PRODUCTION, NESTED_QNAME_RIGHT_SHIFT_QNAME_PRODUCTION);
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
    Expression leftExpression;
    ShiftOperator operator;
    if (production == LEFT_SHIFT_PRODUCTION       || production == LEFT_SHIFT_QNAME_PRODUCTION ||
        production == QNAME_LEFT_SHIFT_PRODUCTION || production == QNAME_LEFT_SHIFT_QNAME_PRODUCTION)
    {
      leftExpression = (Expression) args[0];
      operator = ShiftOperator.LEFT_SHIFT;
    }
    else if (production == RIGHT_SHIFT_PRODUCTION || production == RIGHT_SHIFT_QNAME_PRODUCTION)
    {
      leftExpression = (Expression) args[0];
      operator = ShiftOperator.RIGHT_SHIFT;
    }
    else if (production == QNAME_RIGHT_SHIFT_PRODUCTION || production == QNAME_RIGHT_SHIFT_QNAME_PRODUCTION)
    {
      QName qname = (QName) args[0];
      leftExpression = new QNameElement(qname, qname.getLexicalPhrase()).convertToExpression();
      operator = ShiftOperator.RIGHT_SHIFT;
    }
    else if (production == NESTED_QNAME_RIGHT_SHIFT_PRODUCTION || production == NESTED_QNAME_RIGHT_SHIFT_QNAME_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      leftExpression = element.convertToExpression();
      operator = ShiftOperator.RIGHT_SHIFT;
    }
    else
    {
      throw badTypeList();
    }
    Expression rightExpression = (Expression) args[2];
    return new ShiftExpression(leftExpression, rightExpression, operator, LexicalPhrase.combine(leftExpression.getLexicalPhrase(), (LexicalPhrase) args[1], rightExpression.getLexicalPhrase()));
  }

}
