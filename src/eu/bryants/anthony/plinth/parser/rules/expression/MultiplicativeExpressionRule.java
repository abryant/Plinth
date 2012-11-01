package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.ArithmeticExpression;
import eu.bryants.anthony.plinth.ast.expression.ArithmeticExpression.ArithmeticOperator;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 12 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class MultiplicativeExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION                 = new Production<ParseType>(ParseType.SHIFT_EXPRESSION);
  private static final Production<ParseType> MULTIPLY_PRODUCTION              = new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION, ParseType.STAR,           ParseType.SHIFT_EXPRESSION);
  private static final Production<ParseType> MULTIPLY_QNAME_PRODUCTION        = new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION, ParseType.STAR,           ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_MULTIPLY_PRODUCTION        = new Production<ParseType>(ParseType.QNAME_EXPRESSION,          ParseType.STAR,           ParseType.SHIFT_EXPRESSION);
  private static final Production<ParseType> QNAME_MULTIPLY_QNAME_PRODUCTION  = new Production<ParseType>(ParseType.QNAME_EXPRESSION,          ParseType.STAR,           ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> DIVIDE_PRODUCTION                = new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION, ParseType.FORWARD_SLASH,  ParseType.SHIFT_EXPRESSION);
  private static final Production<ParseType> DIVIDE_QNAME_PRODUCTION          = new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION, ParseType.FORWARD_SLASH,  ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_DIVIDE_PRODUCTION          = new Production<ParseType>(ParseType.QNAME_EXPRESSION,          ParseType.FORWARD_SLASH,  ParseType.SHIFT_EXPRESSION);
  private static final Production<ParseType> QNAME_DIVIDE_QNAME_PRODUCTION    = new Production<ParseType>(ParseType.QNAME_EXPRESSION,          ParseType.FORWARD_SLASH,  ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> REMAINDER_PRODUCTION             = new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION, ParseType.PERCENT,        ParseType.SHIFT_EXPRESSION);
  private static final Production<ParseType> REMAINDER_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION, ParseType.PERCENT,        ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_REMAINDER_PRODUCTION       = new Production<ParseType>(ParseType.QNAME_EXPRESSION,          ParseType.PERCENT,        ParseType.SHIFT_EXPRESSION);
  private static final Production<ParseType> QNAME_REMAINDER_QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION,          ParseType.PERCENT,        ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> MODULO_PRODUCTION                = new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION, ParseType.DOUBLE_PERCENT, ParseType.SHIFT_EXPRESSION);
  private static final Production<ParseType> MODULO_QNAME_PRODUCTION          = new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION, ParseType.DOUBLE_PERCENT, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_MODULO_PRODUCTION          = new Production<ParseType>(ParseType.QNAME_EXPRESSION,          ParseType.DOUBLE_PERCENT, ParseType.SHIFT_EXPRESSION);
  private static final Production<ParseType> QNAME_MODULO_QNAME_PRODUCTION    = new Production<ParseType>(ParseType.QNAME_EXPRESSION,          ParseType.DOUBLE_PERCENT, ParseType.QNAME_EXPRESSION);

  public MultiplicativeExpressionRule()
  {
    super(ParseType.MULTIPLICATIVE_EXPRESSION, START_PRODUCTION,
                                               MULTIPLY_PRODUCTION,  MULTIPLY_QNAME_PRODUCTION,  QNAME_MULTIPLY_PRODUCTION,  QNAME_MULTIPLY_QNAME_PRODUCTION,
                                               DIVIDE_PRODUCTION,    DIVIDE_QNAME_PRODUCTION,    QNAME_DIVIDE_PRODUCTION,    QNAME_DIVIDE_QNAME_PRODUCTION,
                                               REMAINDER_PRODUCTION, REMAINDER_QNAME_PRODUCTION, QNAME_REMAINDER_PRODUCTION, QNAME_REMAINDER_QNAME_PRODUCTION,
                                               MODULO_PRODUCTION,    MODULO_QNAME_PRODUCTION,    QNAME_MODULO_PRODUCTION,    QNAME_MODULO_QNAME_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      return args[0];
    }
    ArithmeticOperator operator;
    if (production == MULTIPLY_PRODUCTION       || production == MULTIPLY_QNAME_PRODUCTION ||
        production == QNAME_MULTIPLY_PRODUCTION || production == QNAME_MULTIPLY_QNAME_PRODUCTION)
    {
      operator = ArithmeticOperator.MULTIPLY;
    }
    else if (production == DIVIDE_PRODUCTION       || production == DIVIDE_QNAME_PRODUCTION ||
             production == QNAME_DIVIDE_PRODUCTION || production == QNAME_DIVIDE_QNAME_PRODUCTION)
    {
      operator = ArithmeticOperator.DIVIDE;
    }
    else if (production == REMAINDER_PRODUCTION       || production == REMAINDER_QNAME_PRODUCTION ||
             production == QNAME_REMAINDER_PRODUCTION || production == QNAME_REMAINDER_QNAME_PRODUCTION)
    {
      operator = ArithmeticOperator.REMAINDER;
    }
    else if (production == MODULO_PRODUCTION       || production == MODULO_QNAME_PRODUCTION ||
             production == QNAME_MODULO_PRODUCTION || production == QNAME_MODULO_QNAME_PRODUCTION)
    {
      operator = ArithmeticOperator.MODULO;
    }
    else
    {
      throw badTypeList();
    }
    Expression left = (Expression) args[0];
    Expression right = (Expression) args[2];
    return new ArithmeticExpression(operator, left, right,
                                    LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase()));
  }

}
