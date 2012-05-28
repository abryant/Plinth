package eu.bryants.anthony.toylanguage.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 4 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class TypeListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.TYPE);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.TYPE_LIST, ParseType.COMMA, ParseType.TYPE);

  @SuppressWarnings("unchecked")
  public TypeListRule()
  {
    super(ParseType.TYPE_LIST, START_PRODUCTION, CONTINUATION_PRODUCTION);
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
