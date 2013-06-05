package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseContainer;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 24 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class OptionalTypeParametersRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> EMPTY_PRODUCTION = new Production<ParseType>();
  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.LANGLE, ParseType.TYPE_PARAMETER_LIST_RANGLE);

  public OptionalTypeParametersRule()
  {
    super(ParseType.OPTIONAL_TYPE_PARAMETERS, EMPTY_PRODUCTION, PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == EMPTY_PRODUCTION)
    {
      return new ParseList<TypeParameter>(null);
    }
    if (production == PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseContainer<ParseList<TypeParameter>> containedList = (ParseContainer<ParseList<TypeParameter>>) args[1];
      ParseList<TypeParameter> list = containedList.getItem();
      list.setLexicalPhrase(LexicalPhrase.combine((LexicalPhrase) args[0], containedList.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
