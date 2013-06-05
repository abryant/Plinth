package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 23 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeBoundListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.TYPE);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.TYPE_BOUND_LIST, ParseType.AMPERSAND, ParseType.TYPE);

  public TypeBoundListRule()
  {
    super(ParseType.TYPE_BOUND_LIST, START_PRODUCTION, CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      Type type = (Type) args[0];
      return new ParseList<Type>(type, type.getLexicalPhrase());
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Type> list = (ParseList<Type>) args[0];
      Type type = (Type) args[2];
      list.addLast(type, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], type.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
