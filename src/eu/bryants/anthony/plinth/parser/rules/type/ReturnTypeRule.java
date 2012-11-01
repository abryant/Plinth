package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 4 Oct 2012
 */

/**
 * @author Anthony Bryant
 */
public class ReturnTypeRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.TYPE);
  private static final Production<ParseType> VOID_PRODUCTION = new Production<ParseType>(ParseType.VOID_KEYWORD);

  public ReturnTypeRule()
  {
    super(ParseType.RETURN_TYPE, PRODUCTION, VOID_PRODUCTION);
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
    if (production == VOID_PRODUCTION)
    {
      return new VoidType((LexicalPhrase) args[0]);
    }
    throw badTypeList();
  }

}
