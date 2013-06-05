package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseContainer;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 23 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeBoundListRAngleRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> SINGLE_PRODUCTION = new Production<ParseType>(ParseType.TYPE_RANGLE);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.TYPE_BOUND_LIST, ParseType.AMPERSAND, ParseType.TYPE_RANGLE);

  public TypeBoundListRAngleRule()
  {
    super(ParseType.TYPE_BOUND_LIST_RANGLE, SINGLE_PRODUCTION, CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == SINGLE_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseContainer<Type> typeContainer = (ParseContainer<Type>) args[0];
      Type type = typeContainer.getItem();
      ParseList<Type> list = new ParseList<Type>(type, type.getLexicalPhrase());
      return new ParseContainer<ParseList<Type>>(list, typeContainer.getLexicalPhrase());
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Type> list = (ParseList<Type>) args[0];
      LexicalPhrase listLexicalPhrase = list.getLexicalPhrase();
      @SuppressWarnings("unchecked")
      ParseContainer<Type> containedType = (ParseContainer<Type>) args[2];
      Type type = containedType.getItem();
      list.addLast(type, LexicalPhrase.combine(listLexicalPhrase, (LexicalPhrase) args[1], type.getLexicalPhrase()));
      return new ParseContainer<ParseList<Type>>(list, LexicalPhrase.combine(listLexicalPhrase, (LexicalPhrase) args[1], containedType.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
