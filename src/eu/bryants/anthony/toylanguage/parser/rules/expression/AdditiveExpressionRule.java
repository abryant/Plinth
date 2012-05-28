package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.ArithmeticExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ArithmeticExpression.ArithmeticOperator;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 9 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class AdditiveExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> START_PRODUCTION =  new Production<ParseType>(ParseType.MULTIPLICATIVE_EXPRESSION);
  private static Production<ParseType> ADDITION_PRODUCTION =  new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.PLUS, ParseType.MULTIPLICATIVE_EXPRESSION);
  private static Production<ParseType> SUBTRACTION_PRODUCTION =  new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.MINUS, ParseType.MULTIPLICATIVE_EXPRESSION);

  @SuppressWarnings("unchecked")
  public AdditiveExpressionRule()
  {
    super(ParseType.ADDITIVE_EXPRESSION, START_PRODUCTION, ADDITION_PRODUCTION, SUBTRACTION_PRODUCTION);
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
    if (production == ADDITION_PRODUCTION)
    {
      Expression left = (Expression) args[0];
      Expression right = (Expression) args[2];
      return new ArithmeticExpression(ArithmeticOperator.ADD, left, right, LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase()));
    }
    if (production == SUBTRACTION_PRODUCTION)
    {
      Expression left = (Expression) args[0];
      Expression right = (Expression) args[2];
      return new ArithmeticExpression(ArithmeticOperator.SUBTRACT, left, right, LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
