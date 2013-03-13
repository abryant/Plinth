package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.PropertyMethod;

/*
 * Created on 4 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class PropertyMethodListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> EMPTY_PRODUCTION = new Production<ParseType>();
  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.PROPERTY_METHOD_LIST, ParseType.PROPERTY_METHOD);

  public PropertyMethodListRule()
  {
    super(ParseType.PROPERTY_METHOD_LIST, EMPTY_PRODUCTION, PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == EMPTY_PRODUCTION)
    {
      return new ParseList<PropertyMethod>(null);
    }
    if (production == PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<PropertyMethod> list = (ParseList<PropertyMethod>) args[0];
      PropertyMethod newMethod = (PropertyMethod) args[1];
      list.addLast(newMethod, LexicalPhrase.combine(list.getLexicalPhrase(), newMethod.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
