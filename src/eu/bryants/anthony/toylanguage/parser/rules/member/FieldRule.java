package eu.bryants.anthony.toylanguage.parser.rules.member;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.LanguageParseException;
import eu.bryants.anthony.toylanguage.parser.Modifier;
import eu.bryants.anthony.toylanguage.parser.ModifierType;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class FieldRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE, ParseType.NAME, ParseType.SEMICOLON);

  @SuppressWarnings("unchecked")
  public FieldRule()
  {
    super(ParseType.FIELD, PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      boolean isStatic = false;
      for (Modifier modifier : modifiers)
      {
        if (modifier.getModifierType() == ModifierType.STATIC)
        {
          if (isStatic)
          {
            throw new LanguageParseException("Duplicate 'static' modifier", modifier.getLexicalPhrase());
          }
          isStatic = true;
        }
        else
        {
          throw new IllegalStateException("Unknown modifier: " + modifier);
        }
      }
      Type type = (Type) args[1];
      Name name = (Name) args[2];
      return new Field(type, name.getName(), isStatic, LexicalPhrase.combine(modifiers.getLexicalPhrase(), type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    throw badTypeList();
  }

}
