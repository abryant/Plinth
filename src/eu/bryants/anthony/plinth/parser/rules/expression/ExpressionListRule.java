package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ExpressionListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.EXPRESSION_NO_TUPLE);
  private static final Production<ParseType> START_QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.EXPRESSION_LIST, ParseType.COMMA, ParseType.EXPRESSION_NO_TUPLE);
  private static final Production<ParseType> CONTINUATION_QNAME_PRODUCTION = new Production<ParseType>(ParseType.EXPRESSION_LIST, ParseType.COMMA, ParseType.QNAME_EXPRESSION);

  @SuppressWarnings("unchecked")
  public ExpressionListRule()
  {
    super(ParseType.EXPRESSION_LIST, START_PRODUCTION, START_QNAME_PRODUCTION, CONTINUATION_PRODUCTION, CONTINUATION_QNAME_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION || production == START_QNAME_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      return new ParseList<Expression>(expression, expression.getLexicalPhrase());
    }
    if (production == CONTINUATION_PRODUCTION || production == CONTINUATION_QNAME_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Expression> list = (ParseList<Expression>) args[0];
      Expression expression = (Expression) args[2];
      list.addLast(expression, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], expression.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
