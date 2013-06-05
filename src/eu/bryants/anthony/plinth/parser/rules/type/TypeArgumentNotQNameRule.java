package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 23 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeArgumentNotQNameRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.TYPE_NOT_QNAME);
  private static final Production<ParseType> WILDCARD_PRODUCTION = new Production<ParseType>(ParseType.WILDCARD_TYPE_ARGUMENT);

  public TypeArgumentNotQNameRule()
  {
    super(ParseType.TYPE_ARGUMENT_NOT_QNAME, PRODUCTION, WILDCARD_PRODUCTION);
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
    if (production == WILDCARD_PRODUCTION)
    {
      return args[0];
    }
    throw badTypeList();
  }

}
