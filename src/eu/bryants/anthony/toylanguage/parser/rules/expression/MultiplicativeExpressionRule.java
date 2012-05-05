package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.expression.ArithmeticExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ArithmeticExpression.ArithmeticOperator;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 12 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class MultiplicativeExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION     = new Production<ParseType>(ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> MULTIPLY_PRODUCTION  = new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION, ParseType.STAR,           ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> DIVIDE_PRODUCTION    = new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION, ParseType.FORWARD_SLASH,  ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> REMAINDER_PRODUCTION = new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION, ParseType.PERCENT,        ParseType.TUPLE_INDEX_EXPRESSION);
  private static final Production<ParseType> MODULO_PRODUCTION    = new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION, ParseType.DOUBLE_PERCENT, ParseType.TUPLE_INDEX_EXPRESSION);

  @SuppressWarnings("unchecked")
  public MultiplicativeExpressionRule()
  {
    super(ParseType.MULTIPLICATIVE_EXPRESSION, START_PRODUCTION, MULTIPLY_PRODUCTION, DIVIDE_PRODUCTION, REMAINDER_PRODUCTION, MODULO_PRODUCTION);
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
    if (production == MULTIPLY_PRODUCTION)       { operator = ArithmeticOperator.MULTIPLY; }
    else if (production == DIVIDE_PRODUCTION)    { operator = ArithmeticOperator.DIVIDE; }
    else if (production == REMAINDER_PRODUCTION) { operator = ArithmeticOperator.REMAINDER; }
    else if (production == MODULO_PRODUCTION)    { operator = ArithmeticOperator.MODULO; }
    else { throw badTypeList(); }
    Expression left = (Expression) args[0];
    Expression right = (Expression) args[2];
    return new ArithmeticExpression(operator, left, right,
                                    LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase()));
  }

}
