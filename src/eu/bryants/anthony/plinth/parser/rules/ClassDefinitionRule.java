package eu.bryants.anthony.plinth.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
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

  private static final Production<ParseType> PRODUCTION         = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.CLASS_KEYWORD, ParseType.NAME,                                             ParseType.LBRACE, ParseType.MEMBER_LIST, ParseType.RBRACE);
  private static final Production<ParseType> EXTENDS_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.CLASS_KEYWORD, ParseType.NAME, ParseType.EXTENDS_KEYWORD, ParseType.QNAME, ParseType.LBRACE, ParseType.MEMBER_LIST, ParseType.RBRACE);

  public ClassDefinitionRule()
  {
    super(ParseType.CLASS_DEFINITION, PRODUCTION, EXTENDS_PRODUCTION);
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
      return processModifiers(modifiers, name.getName(), null, members.toArray(new Member[members.size()]),
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase(), (LexicalPhrase) args[3], members.getLexicalPhrase(), (LexicalPhrase) args[5]));
    }
    if (production == EXTENDS_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Name name = (Name) args[2];
      QName superQName = (QName) args[4];
      @SuppressWarnings("unchecked")
      ParseList<Member> members = (ParseList<Member>) args[6];
      return processModifiers(modifiers, name.getName(), superQName, members.toArray(new Member[members.size()]),
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase(), (LexicalPhrase) args[3], superQName.getLexicalPhrase(), (LexicalPhrase) args[5], members.getLexicalPhrase(), (LexicalPhrase) args[7]));
    }
    throw badTypeList();
  }

  private ClassDefinition processModifiers(ParseList<Modifier> modifiers, String name, QName superQName, Member[] members, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isImmutable = false;
    for (Modifier modifier : modifiers)
    {
      switch (modifier.getModifierType())
      {
      case FINAL:
        throw new LanguageParseException("Unexpected modifier: Class definitions cannot be final", modifier.getLexicalPhrase());
      case IMMUTABLE:
        if (isImmutable)
        {
          throw new LanguageParseException("Duplicate 'immutable' modifier", modifier.getLexicalPhrase());
        }
        isImmutable = true;
        break;
      case MUTABLE:
        throw new LanguageParseException("Unexpected modifier: Class definitions cannot be mutable", modifier.getLexicalPhrase());
      case NATIVE:
        throw new LanguageParseException("Unexpected modifier: Class definitions cannot be native", modifier.getLexicalPhrase());
      case SELFISH:
        throw new LanguageParseException("Unexpected modifier: Class definitions cannot be selfish", modifier.getLexicalPhrase());
      case STATIC:
        throw new LanguageParseException("Unexpected modifier: Class definitions cannot be static", modifier.getLexicalPhrase());
      default:
        throw new IllegalStateException("Unknown modifier: " + modifier);
      }
    }
    return new ClassDefinition(isImmutable, name, superQName, members, lexicalPhrase);
  }
}
