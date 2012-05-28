package eu.bryants.anthony.toylanguage.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.Modifier;
import eu.bryants.anthony.toylanguage.parser.ModifierType;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 28 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ModifiersRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> EMPTY_PRODUCTION = new Production<ParseType>();
  private static final Production<ParseType> STATIC_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.STATIC_KEYWORD);

  @SuppressWarnings("unchecked")
  public ModifiersRule()
  {
    super(ParseType.MODIFIERS, EMPTY_PRODUCTION, STATIC_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == EMPTY_PRODUCTION)
    {
      return new ParseList<Modifier>(null);
    }
    if (production == STATIC_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> list = (ParseList<Modifier>) args[0];
      Modifier modifier = new Modifier(ModifierType.STATIC, (LexicalPhrase) args[1]);
      list.addLast(modifier, LexicalPhrase.combine(list.getLexicalPhrase(), modifier.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
