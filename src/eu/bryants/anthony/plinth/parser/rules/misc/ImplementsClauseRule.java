package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 8 Jan 2013
 */

/**
 * @author Anthony Bryant
 */
public class ImplementsClauseRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> EMPTY_PRODUCTION = new Production<ParseType>();
  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.IMPLEMENTS_KEYWORD, ParseType.INTERFACE_LIST);

  public ImplementsClauseRule()
  {
    super(ParseType.IMPLEMENTS_CLAUSE, EMPTY_PRODUCTION, PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == EMPTY_PRODUCTION)
    {
      return null;
    }
    if (production == PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<QName> list = (ParseList<QName>) args[1];
      list.setLexicalPhrase(LexicalPhrase.combine((LexicalPhrase) args[0], list.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
