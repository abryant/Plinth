package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.expression.AdditiveExpression;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> START_PRODUCTION =  new Production<ParseType>(ParseType.PRIMARY);
  private static Production<ParseType> CONTINUATION_PRODUCTION =  new Production<ParseType>(ParseType.EXPRESSION, ParseType.PLUS, ParseType.PRIMARY);

  @SuppressWarnings("unchecked")
  public ExpressionRule()
  {
    super(ParseType.EXPRESSION, START_PRODUCTION, CONTINUATION_PRODUCTION);
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
    if (production == CONTINUATION_PRODUCTION)
    {
      Expression left = (Expression) args[0];
      Expression right = (Expression) args[2];
      return new AdditiveExpression(left, right, LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
