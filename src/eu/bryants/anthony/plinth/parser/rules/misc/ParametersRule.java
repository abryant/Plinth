package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ParametersRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.LPAREN, ParseType.PARAMETER_LIST, ParseType.RPAREN);
  private static final Production<ParseType> EMPTY_PRODUCTION = new Production<ParseType>(ParseType.LPAREN, ParseType.RPAREN);

  public ParametersRule()
  {
    super(ParseType.PARAMETERS, PRODUCTION, EMPTY_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Parameter> list = (ParseList<Parameter>) args[1];
      list.setLexicalPhrase(LexicalPhrase.combine((LexicalPhrase) args[0], list.getLexicalPhrase(), (LexicalPhrase) args[2]));
      return list;
    }
    if (production == EMPTY_PRODUCTION)
    {
      return new ParseList<Parameter>(LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1]));
    }
    throw badTypeList();
  }
}
