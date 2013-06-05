package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.ThrownExceptionType;

/*
 * Created on 20 Feb 2013
 */

/**
 * @author Anthony Bryant
 */
public class ThrowsListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION           = new Production<ParseType>(ParseType.NAMED_TYPE_NO_MODIFIERS);
  private static final Production<ParseType> START_UNCHECKED_PRODUCTION = new Production<ParseType>(ParseType.UNCHECKED_KEYWORD, ParseType.NAMED_TYPE_NO_MODIFIERS);
  private static final Production<ParseType> CONTINUE_PRODUCTION           = new Production<ParseType>(ParseType.THROWS_LIST, ParseType.COMMA, ParseType.NAMED_TYPE_NO_MODIFIERS);
  private static final Production<ParseType> CONTINUE_UNCHECKED_PRODUCTION = new Production<ParseType>(ParseType.THROWS_LIST, ParseType.COMMA, ParseType.UNCHECKED_KEYWORD, ParseType.NAMED_TYPE_NO_MODIFIERS);

  public ThrowsListRule()
  {
    super(ParseType.THROWS_LIST, START_PRODUCTION, START_UNCHECKED_PRODUCTION, CONTINUE_PRODUCTION, CONTINUE_UNCHECKED_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      NamedType type = (NamedType) args[0];
      ThrownExceptionType thrownExceptionType = new ThrownExceptionType(type, false, type.getLexicalPhrase());
      return new ParseList<ThrownExceptionType>(thrownExceptionType, thrownExceptionType.getLexicalPhrase());
    }
    if (production == START_UNCHECKED_PRODUCTION)
    {
      NamedType type = (NamedType) args[1];
      ThrownExceptionType thrownExceptionType = new ThrownExceptionType(type, true, LexicalPhrase.combine((LexicalPhrase) args[0], type.getLexicalPhrase()));
      return new ParseList<ThrownExceptionType>(thrownExceptionType, thrownExceptionType.getLexicalPhrase());
    }
    if (production == CONTINUE_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> list = (ParseList<ThrownExceptionType>) args[0];
      NamedType type = (NamedType) args[2];
      ThrownExceptionType thrownExceptionType = new ThrownExceptionType(type, false, type.getLexicalPhrase());
      list.addLast(thrownExceptionType, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], thrownExceptionType.getLexicalPhrase()));
      return list;
    }
    if (production == CONTINUE_UNCHECKED_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> list = (ParseList<ThrownExceptionType>) args[0];
      NamedType type = (NamedType) args[3];
      ThrownExceptionType thrownExceptionType = new ThrownExceptionType(type, true, LexicalPhrase.combine((LexicalPhrase) args[2], type.getLexicalPhrase()));
      list.addLast(thrownExceptionType, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], thrownExceptionType.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
