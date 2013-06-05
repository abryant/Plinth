package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 25 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class QNameOrLessThanExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> LESS_THAN_PRODUCTION = new Production<ParseType>(ParseType.COMPARISON_EXPRESSION_LESS_THAN_QNAME);

  public QNameOrLessThanExpressionRule()
  {
    super(ParseType.QNAME_OR_LESS_THAN_EXPRESSION, QNAME_PRODUCTION, LESS_THAN_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == QNAME_PRODUCTION || production == LESS_THAN_PRODUCTION)
    {
      return args[0];
    }
    throw badTypeList();
  }

}
