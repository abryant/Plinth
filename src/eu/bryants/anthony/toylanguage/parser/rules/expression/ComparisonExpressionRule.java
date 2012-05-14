package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.expression.ComparisonExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ComparisonExpression.ComparisonOperator;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ComparisonExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> NORMAL_PRODUCTION          = new Production<ParseType>(ParseType.SHIFT_EXPRESSION);
  private static Production<ParseType> EQUAL_PRODUCTION           = new Production<ParseType>(ParseType.SHIFT_EXPRESSION, ParseType.DOUBLE_EQUALS,            ParseType.SHIFT_EXPRESSION);
  private static Production<ParseType> NOT_EQUAL_PRODUCTION       = new Production<ParseType>(ParseType.SHIFT_EXPRESSION, ParseType.EXCLAIMATION_MARK_EQUALS, ParseType.SHIFT_EXPRESSION);
  private static Production<ParseType> LESS_THAN_PRODUCTION       = new Production<ParseType>(ParseType.SHIFT_EXPRESSION, ParseType.LANGLE,                   ParseType.SHIFT_EXPRESSION);
  private static Production<ParseType> LESS_THAN_EQUAL_PRODUCTION = new Production<ParseType>(ParseType.SHIFT_EXPRESSION, ParseType.LANGLE_EQUALS,            ParseType.SHIFT_EXPRESSION);
  private static Production<ParseType> MORE_THAN_PRODUCTION       = new Production<ParseType>(ParseType.SHIFT_EXPRESSION, ParseType.RANGLE,                   ParseType.SHIFT_EXPRESSION);
  private static Production<ParseType> MORE_THAN_EQUAL_PRODUCTION = new Production<ParseType>(ParseType.SHIFT_EXPRESSION, ParseType.RANGLE_EQUALS,            ParseType.SHIFT_EXPRESSION);

  @SuppressWarnings("unchecked")
  public ComparisonExpressionRule()
  {
    super(ParseType.COMPARISON_EXPRESSION, NORMAL_PRODUCTION,
                                           EQUAL_PRODUCTION, NOT_EQUAL_PRODUCTION,
                                           LESS_THAN_PRODUCTION, LESS_THAN_EQUAL_PRODUCTION,
                                           MORE_THAN_PRODUCTION, MORE_THAN_EQUAL_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == NORMAL_PRODUCTION)
    {
      return args[0];
    }
    ComparisonOperator operator;
    if (production == EQUAL_PRODUCTION) { operator = ComparisonOperator.EQUAL; }
    else if (production == NOT_EQUAL_PRODUCTION) { operator = ComparisonOperator.NOT_EQUAL; }
    else if (production == LESS_THAN_PRODUCTION) { operator = ComparisonOperator.LESS_THAN; }
    else if (production == LESS_THAN_EQUAL_PRODUCTION) { operator = ComparisonOperator.LESS_THAN_EQUAL; }
    else if (production == MORE_THAN_PRODUCTION) { operator = ComparisonOperator.MORE_THAN; }
    else if (production == MORE_THAN_EQUAL_PRODUCTION) { operator = ComparisonOperator.MORE_THAN_EQUAL; }
    else { throw badTypeList(); }
    Expression left = (Expression) args[0];
    Expression right = (Expression) args[2];
    LexicalPhrase lexicalPhrase = LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase());
    return new ComparisonExpression(left, right, operator, lexicalPhrase);
  }

}
