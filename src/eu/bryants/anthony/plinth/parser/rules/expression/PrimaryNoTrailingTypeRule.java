package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.ThisExpression;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 2 Nov 2012
 */

/**
 * @author Anthony Bryant
 */
public class PrimaryNoTrailingTypeRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION      = new Production<ParseType>(ParseType.BASIC_PRIMARY);
  private static final Production<ParseType> THIS_PRODUCTION = new Production<ParseType>(ParseType.THIS_KEYWORD);

  public PrimaryNoTrailingTypeRule()
  {
    super(ParseType.PRIMARY_NO_TRAILING_TYPE, PRODUCTION, THIS_PRODUCTION);
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
