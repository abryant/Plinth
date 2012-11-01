package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 9 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class OptionalModifiersRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> EMPTY_PRODUCTION = new Production<ParseType>();
  private static final Production<ParseType> MODIFIERS_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS);

  public OptionalModifiersRule()
  {
    super(ParseType.OPTIONAL_MODIFIERS, EMPTY_PRODUCTION, MODIFIERS_PRODUCTION);
  }

  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == EMPTY_PRODUCTION)
    {
      return new ParseList<Modifier>(null);
    }
    if (production == MODIFIERS_PRODUCTION)
    {
      return args[0];
    }
    throw badTypeList();
  }

}
