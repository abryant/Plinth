package eu.bryants.anthony.plinth.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ModifierType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 12 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class ClassDefinitionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.CLASS_KEYWORD, ParseType.NAME, ParseType.LBRACE, ParseType.MEMBER_LIST, ParseType.RBRACE);

  public ClassDefinitionRule()
  {
    super(ParseType.CLASS_DEFINITION, PRODUCTION);
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
      Name name = (Name) args[2];
      @SuppressWarnings("unchecked")
      ParseList<Member> members = (ParseList<Member>) args[4];
      return processModifiers(modifiers, name.getName(), members.toArray(new Member[members.size()]),
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase(), (LexicalPhrase) args[3], members.getLexicalPhrase(), (LexicalPhrase) args[5]));
    }
    throw badTypeList();
  }

  private ClassDefinition processModifiers(ParseList<Modifier> modifiers, String name, Member[] members, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isImmutable = false;
    for (Modifier modifier : modifiers)
    {
      if (modifier.getModifierType() == ModifierType.FINAL)
      {
        throw new LanguageParseException("Unexpected modifier: Class definitions cannot be final", modifier.getLexicalPhrase());
      }
      else if (modifier.getModifierType() == ModifierType.IMMUTABLE)
      {
        if (isImmutable)
        {
          throw new LanguageParseException("Duplicate 'immutable' modifier", modifier.getLexicalPhrase());
        }
        isImmutable = true;
      }
      else if (modifier.getModifierType() == ModifierType.MUTABLE)
      {
        throw new LanguageParseException("Unexpected modifier: Class definitions cannot be mutable", modifier.getLexicalPhrase());
      }
      else if (modifier.getModifierType() == ModifierType.NATIVE)
      {
        throw new LanguageParseException("Unexpected modifier: Class definitions cannot be native", modifier.getLexicalPhrase());
      }
      else if (modifier.getModifierType() == ModifierType.STATIC)
      {
        throw new LanguageParseException("Unexpected modifier: Class definitions cannot be native", modifier.getLexicalPhrase());
      }
      else
      {
        throw new IllegalStateException("Unknown modifier: " + modifier);
      }
    }
    return new ClassDefinition(isImmutable, name, members, lexicalPhrase);
  }
}
