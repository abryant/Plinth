package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ExpressionListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.EXPRESSION);
  private static Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.EXPRESSION_LIST, ParseType.COMMA, ParseType.EXPRESSION);

  @SuppressWarnings("unchecked")
  public ExpressionListRule()
  {
    super(ParseType.EXPRESSION_LIST, START_PRODUCTION, CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      return new ParseList<Expression>(expression, expression.getLexicalPhrase());
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Expression> expressions = (ParseList<Expression>) args[0];
      Expression expression = (Expression) args[2];
      expressions.addLast(expression, LexicalPhrase.combine(expressions.getLexicalPhrase(), (LexicalPhrase) args[1], expression.getLexicalPhrase()));
      return expressions;
    }
    throw badTypeList();
  }

}
