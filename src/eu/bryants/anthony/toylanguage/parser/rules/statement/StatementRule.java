package eu.bryants.anthony.toylanguage.parser.rules.statement;

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
  private static final Production<ParseType> BLOCK_PRODUCTION  = new Production<ParseType>(ParseType.BLOCK);
  private static final Production<ParseType> IF_PRODUCTION  = new Production<ParseType>(ParseType.IF_STATEMENT);
  private static final Production<ParseType> RETURN_PRODUCTION = new Production<ParseType>(ParseType.RETURN_STATEMENT);
  private static final Production<ParseType> VARIABLE_DEFINITION_PRODUCTION  = new Production<ParseType>(ParseType.VARIABLE_DEFINITION_STATEMENT);
  private static final Production<ParseType> WHILE_PRODUCTION  = new Production<ParseType>(ParseType.WHILE_STATEMENT);

  @SuppressWarnings("unchecked")
  public StatementRule()
  {
    super(ParseType.STATEMENT, ASSIGN_PRODUCTION, BLOCK_PRODUCTION, IF_PRODUCTION, RETURN_PRODUCTION, VARIABLE_DEFINITION_PRODUCTION, WHILE_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == ASSIGN_PRODUCTION || production == BLOCK_PRODUCTION || production == IF_PRODUCTION || production == RETURN_PRODUCTION || production == VARIABLE_DEFINITION_PRODUCTION || production == WHILE_PRODUCTION)
    {
      return args[0];
    }
    throw badTypeList();
  }

}
