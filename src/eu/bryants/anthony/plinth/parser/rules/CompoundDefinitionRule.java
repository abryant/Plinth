package eu.bryants.anthony.plinth.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class CompoundDefinitionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.COMPOUND_KEYWORD, ParseType.NAME, ParseType.OPTIONAL_TYPE_PARAMETERS, ParseType.LBRACE, ParseType.MEMBER_LIST, ParseType.RBRACE);

  public CompoundDefinitionRule()
  {
    super(ParseType.COMPOUND_DEFINITION, PRODUCTION);
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
      ParseList<TypeParameter> typeParameterList = (ParseList<TypeParameter>) args[3];
      TypeParameter[] typeParameters = typeParameterList.toArray(new TypeParameter[typeParameterList.size()]);
      @SuppressWarnings("unchecked")
      ParseList<Member> members = (ParseList<Member>) args[5];
      return processModifiers(modifiers, name.getName(), typeParameters, members.toArray(new Member[members.size()]),
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase(), typeParameterList.getLexicalPhrase(), (LexicalPhrase) args[4], members.getLexicalPhrase(), (LexicalPhrase) args[6]));
    }
    throw badTypeList();
  }

  private CompoundDefinition processModifiers(ParseList<Modifier> modifiers, String name, TypeParameter[] typeParameters, Member[] members, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isImmutable = false;
    boolean hasSince = false;
    for (Modifier modifier : modifiers)
    {
      switch (modifier.getModifierType())
      {
      case ABSTRACT:
        throw new LanguageParseException("Unexpected modifier: Compound type definitions cannot be abstract", modifier.getLexicalPhrase());
      case FINAL:
        throw new LanguageParseException("Unexpected modifier: Compound type definitions cannot be final", modifier.getLexicalPhrase());
      case IMMUTABLE:
        if (isImmutable)
        {
          throw new LanguageParseException("Duplicate 'immutable' modifier", modifier.getLexicalPhrase());
        }
        isImmutable = true;
        break;
      case MUTABLE:
        throw new LanguageParseException("Unexpected modifier: Compound type definitions cannot be mutable", modifier.getLexicalPhrase());
      case NATIVE:
        throw new LanguageParseException("Unexpected modifier: Compound type definitions cannot be native", modifier.getLexicalPhrase());
      case SELFISH:
        throw new LanguageParseException("Unexpected modifier: Compound type definitions cannot be selfish", modifier.getLexicalPhrase());
      case SINCE:
        // these are ignored by all stages of compilation after parsing, but are allowed for documentation purposes
        if (hasSince)
        {
          throw new LanguageParseException("Duplicate since(...) specifier", modifier.getLexicalPhrase());
        }
        hasSince = true;
        break;
      case STATIC:
        throw new LanguageParseException("Unexpected modifier: Compound type definitions cannot be static", modifier.getLexicalPhrase());
      case UNBACKED:
        throw new LanguageParseException("Unexpected modifier: Compound type definitions cannot be unbacked", modifier.getLexicalPhrase());
      default:
        throw new IllegalStateException("Unknown modifier: " + modifier);
      }
    }
    return new CompoundDefinition(isImmutable, name, typeParameters, members, lexicalPhrase);
  }
}
