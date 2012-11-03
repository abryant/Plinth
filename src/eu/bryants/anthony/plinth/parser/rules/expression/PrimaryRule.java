package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.ThisExpression;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 8 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class PrimaryRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION      = new Production<ParseType>(ParseType.PRIMARY_NO_THIS);
  private static final Production<ParseType> THIS_PRODUCTION = new Production<ParseType>(ParseType.THIS_KEYWORD);

  public PrimaryRule()
  {
    super(ParseType.PRIMARY, PRODUCTION, THIS_PRODUCTION);
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
    if (production == THIS_PRODUCTION)
    {
      return new ThisExpression((LexicalPhrase) args[0]);
    }
    throw badTypeList();
  }

}
