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
 * Created on 24 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeBoundListDoubleRAngleRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> SINGLE_PRODUCTION = new Production<ParseType>(ParseType.TYPE_DOUBLE_RANGLE);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.TYPE_BOUND_LIST, ParseType.AMPERSAND, ParseType.TYPE_DOUBLE_RANGLE);

  public TypeBoundListDoubleRAngleRule()
  {
    super(ParseType.TYPE_BOUND_LIST_DOUBLE_RANGLE, SINGLE_PRODUCTION, CONTINUATION_PRODUCTION);
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
      ParseContainer<ParseContainer<Type>> containedContainedType = (ParseContainer<ParseContainer<Type>>) args[0];
      ParseContainer<Type> containedType = containedContainedType.getItem();
      Type type = containedType.getItem();
      ParseList<Type> list = new ParseList<Type>(type, type.getLexicalPhrase());
      ParseContainer<ParseList<Type>> containedList = new ParseContainer<ParseList<Type>>(list, containedType.getLexicalPhrase());
      return new ParseContainer<ParseContainer<ParseList<Type>>>(containedList, containedContainedType.getLexicalPhrase());
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Type> list = (ParseList<Type>) args[0];
      LexicalPhrase listLexicalPhrase = list.getLexicalPhrase();
      @SuppressWarnings("unchecked")
      ParseContainer<ParseContainer<Type>> containedContainedType = (ParseContainer<ParseContainer<Type>>) args[2];
      ParseContainer<Type> containedType = containedContainedType.getItem();
      Type type = containedType.getItem();
      list.addLast(type, LexicalPhrase.combine(listLexicalPhrase, (LexicalPhrase) args[1], type.getLexicalPhrase()));
      ParseContainer<ParseList<Type>> containedList = new ParseContainer<ParseList<Type>>(list, LexicalPhrase.combine(listLexicalPhrase, (LexicalPhrase) args[1], containedType.getLexicalPhrase()));
      return new ParseContainer<ParseContainer<ParseList<Type>>>(containedList, LexicalPhrase.combine(listLexicalPhrase, (LexicalPhrase) args[1], containedContainedType.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
