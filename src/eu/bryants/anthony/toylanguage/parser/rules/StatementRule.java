package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class StatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> ASSIGN_PRODUCTION = new Production<ParseType>(ParseType.ASSIGN_STATEMENT);
  private static final Production<ParseType> RETURN_PRODUCTION = new Production<ParseType>(ParseType.RETURN_STATEMENT);

  @SuppressWarnings("unchecked")
  public StatementRule()
  {
    super(ParseType.STATEMENT, ASSIGN_PRODUCTION, RETURN_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == ASSIGN_PRODUCTION || production == RETURN_PRODUCTION)
    {
      return args[0];
    }
    throw badTypeList();
  }

}
