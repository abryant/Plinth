package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 13 Nov 2012
 */

/**
 * @author Anthony Bryant
 */
public class OptionalTypeListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.TYPE_LIST_NO_QNAME);
  private static final Production<ParseType> QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_LIST);
  private static final Production<ParseType> BLANK_PRODUCTION = new Production<ParseType>();

  public OptionalTypeListRule()
  {
    super(ParseType.OPTIONAL_TYPE_LIST, PRODUCTION, QNAME_PRODUCTION, BLANK_PRODUCTION);
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
    if (production == QNAME_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<QNameElement> list = (ParseList<QNameElement>) args[0];
      QNameElement[] elements = list.toArray(new QNameElement[list.size()]);
      Type[] types = new Type[elements.length];
      for (int i = 0; i < elements.length; ++i)
      {
        types[i] = elements[i].convertToType();
      }
      return new ParseList<Type>(types, list.getLexicalPhrase());
    }
    if (production == BLANK_PRODUCTION)
    {
      return new ParseList<Type>(null);
    }
    throw badTypeList();
  }

}
