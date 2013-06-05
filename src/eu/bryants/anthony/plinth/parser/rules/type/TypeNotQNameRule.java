package eu.bryants.anthony.plinth.parser.rules.type;

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
public class TypeNotQNameRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.BASIC_TYPE);
  private static final Production<ParseType> ARGS_PRODUCTION = new Production<ParseType>(ParseType.TYPE_TRAILING_ARGS);

  public TypeNotQNameRule()
  {
    super(ParseType.TYPE_NOT_QNAME, PRODUCTION, ARGS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION || production == ARGS_PRODUCTION)
    {
      return args[0];
    }
    throw badTypeList();
  }

}
