package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;

/*
 * Created on 3 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class DimensionsRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.LSQUARE, ParseType.EXPRESSION, ParseType.RSQUARE);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.DIMENSIONS, ParseType.LSQUARE, ParseType.EXPRESSION, ParseType.RSQUARE);

  @SuppressWarnings("unchecked")
  public DimensionsRule()
  {
    super(ParseType.DIMENSIONS, START_PRODUCTION, CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      Expression expression = (Expression) args[1];
      return new ParseList<Expression>(expression, LexicalPhrase.combine((LexicalPhrase) args[0], expression.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Expression> expressions = (ParseList<Expression>) args[0];
      Expression expression = (Expression) args[2];
      expressions.addLast(expression, LexicalPhrase.combine(expressions.getLexicalPhrase(), (LexicalPhrase) args[1], expression.getLexicalPhrase(), (LexicalPhrase) args[3]));
      return expressions;
    }
    throw badTypeList();
  }

}
