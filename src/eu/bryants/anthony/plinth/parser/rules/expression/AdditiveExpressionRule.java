package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.ArithmeticExpression;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.ArithmeticExpression.ArithmeticOperator;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 9 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class AdditiveExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> START_PRODUCTION                   = new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION);
  private static Production<ParseType> ADDITION_PRODUCTION                = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.PLUS, ParseType.MULTIPLICATIVE_EXPRESSION);
  private static Production<ParseType> ADDITION_QNAME_PRODUCTION          = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.PLUS, ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> QNAME_ADDITION_PRODUCTION          = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.PLUS, ParseType.MULTIPLICATIVE_EXPRESSION);
  private static Production<ParseType> QNAME_ADDITION_QNAME_PRODUCTION    = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.PLUS, ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> SUBTRACTION_PRODUCTION             = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.MINUS, ParseType.MULTIPLICATIVE_EXPRESSION);
  private static Production<ParseType> SUBTRACTION_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.MINUS, ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> QNAME_SUBTRACTION_PRODUCTION       = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.MINUS, ParseType.MULTIPLICATIVE_EXPRESSION);
  private static Production<ParseType> QNAME_SUBTRACTION_QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.MINUS, ParseType.QNAME_EXPRESSION);

  @SuppressWarnings("unchecked")
  public AdditiveExpressionRule()
  {
    super(ParseType.ADDITIVE_EXPRESSION, START_PRODUCTION,
                                         ADDITION_PRODUCTION,    ADDITION_QNAME_PRODUCTION,    QNAME_ADDITION_PRODUCTION,    QNAME_ADDITION_QNAME_PRODUCTION,
                                         SUBTRACTION_PRODUCTION, SUBTRACTION_QNAME_PRODUCTION, QNAME_SUBTRACTION_PRODUCTION, QNAME_SUBTRACTION_QNAME_PRODUCTION);
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
    if (production == ADDITION_PRODUCTION       || production == ADDITION_QNAME_PRODUCTION ||
        production == QNAME_ADDITION_PRODUCTION || production == QNAME_ADDITION_QNAME_PRODUCTION)
    {
      Expression left = (Expression) args[0];
      Expression right = (Expression) args[2];
      return new ArithmeticExpression(ArithmeticOperator.ADD, left, right, LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase()));
    }
    if (production == SUBTRACTION_PRODUCTION       || production == SUBTRACTION_QNAME_PRODUCTION ||
        production == QNAME_SUBTRACTION_PRODUCTION || production == QNAME_SUBTRACTION_QNAME_PRODUCTION)
    {
      Expression left = (Expression) args[0];
      Expression right = (Expression) args[2];
      return new ArithmeticExpression(ArithmeticOperator.SUBTRACT, left, right, LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
