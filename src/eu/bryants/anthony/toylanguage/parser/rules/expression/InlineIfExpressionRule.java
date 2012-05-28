package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.InlineIfExpression;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 7 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class InlineIfExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION);
  private static final Production<ParseType> IF_PRODUCTION = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.QUESTION_MARK, ParseType.TUPLE_EXPRESSION, ParseType.COLON, ParseType.EXPRESSION);

  @SuppressWarnings("unchecked")
  public InlineIfExpressionRule()
  {
    super(ParseType.EXPRESSION, PRODUCTION, IF_PRODUCTION);
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
    if (production == IF_PRODUCTION)
    {
      Expression conditional = (Expression) args[0];
      Expression thenExpression = (Expression) args[2];
      Expression elseExpression = (Expression) args[4];
      return new InlineIfExpression(conditional, thenExpression, elseExpression, LexicalPhrase.combine(conditional.getLexicalPhrase(), (LexicalPhrase) args[1], thenExpression.getLexicalPhrase(), (LexicalPhrase) args[3], elseExpression.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
