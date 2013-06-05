package eu.bryants.anthony.plinth.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
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

  private static final Production<ParseType> PRODUCTION         = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.CLASS_KEYWORD, ParseType.NAME, ParseType.OPTIONAL_TYPE_PARAMETERS,
                                                                                            ParseType.IMPLEMENTS_CLAUSE,
                                                                                            ParseType.LBRACE, ParseType.MEMBER_LIST, ParseType.RBRACE);
  private static final Production<ParseType> EXTENDS_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.CLASS_KEYWORD, ParseType.NAME, ParseType.OPTIONAL_TYPE_PARAMETERS,
                                                                                            ParseType.EXTENDS_KEYWORD, ParseType.NAMED_TYPE_NO_MODIFIERS,
                                                                                            ParseType.IMPLEMENTS_CLAUSE,
                                                                                            ParseType.LBRACE, ParseType.MEMBER_LIST, ParseType.RBRACE);

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
      ParseList<TypeParameter> typeParameterList = (ParseList<TypeParameter>) args[3];
      TypeParameter[] typeParameters = typeParameterList.toArray(new TypeParameter[typeParameterList.size()]);
      @SuppressWarnings("unchecked")
      ParseList<NamedType> implementsList = (ParseList<NamedType>) args[4];
      LexicalPhrase implementsLexicalPhrase = implementsList == null ? null : implementsList.getLexicalPhrase();
      NamedType[] interfaceTypes = null;
      if (implementsList != null)
      {
        interfaceTypes = implementsList.toArray(new NamedType[implementsList.size()]);
      }
      @SuppressWarnings("unchecked")
      ParseList<Member> members = (ParseList<Member>) args[6];
      return processModifiers(modifiers, name.getName(), typeParameters, null, interfaceTypes, members.toArray(new Member[members.size()]),
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase(), typeParameterList.getLexicalPhrase(),
                                                    implementsLexicalPhrase,
                                                    (LexicalPhrase) args[5], members.getLexicalPhrase(), (LexicalPhrase) args[7]));
    }
    if (production == EXTENDS_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Name name = (Name) args[2];
      @SuppressWarnings("unchecked")
      ParseList<TypeParameter> typeParameterList = (ParseList<TypeParameter>) args[3];
      TypeParameter[] typeParameters = typeParameterList.toArray(new TypeParameter[typeParameterList.size()]);
      NamedType superType = (NamedType) args[5];
      @SuppressWarnings("unchecked")
      ParseList<NamedType> implementsList = (ParseList<NamedType>) args[6];
      LexicalPhrase implementsLexicalPhrase = implementsList == null ? null : implementsList.getLexicalPhrase();
      NamedType[] interfaceTypes = null;
      if (implementsList != null)
      {
        interfaceTypes = implementsList.toArray(new NamedType[implementsList.size()]);
      }
      @SuppressWarnings("unchecked")
      ParseList<Member> members = (ParseList<Member>) args[8];
      return processModifiers(modifiers, name.getName(), typeParameters, superType, interfaceTypes, members.toArray(new Member[members.size()]),
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase(), typeParameterList.getLexicalPhrase(),
                                                    (LexicalPhrase) args[4], superType.getLexicalPhrase(),
                                                    implementsLexicalPhrase,
                                                    (LexicalPhrase) args[7], members.getLexicalPhrase(), (LexicalPhrase) args[9]));
    }
    throw badTypeList();
  }

  private ClassDefinition processModifiers(ParseList<Modifier> modifiers, String name, TypeParameter[] typeParameters, NamedType superType, NamedType[] interfaceTypes, Member[] members, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isAbstract = false;
    boolean isImmutable = false;
    boolean hasSince = false;
    for (Modifier modifier : modifiers)
    {
      switch (modifier.getModifierType())
      {
      case ABSTRACT:
        if (isAbstract)
        {
          throw new LanguageParseException("Duplicate 'abstract' modifier", modifier.getLexicalPhrase());
        }
        isAbstract = true;
        break;
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
      case SINCE:
        // these are ignored by all stages of compilation after parsing, but are allowed for documentation purposes
        if (hasSince)
        {
          throw new LanguageParseException("Duplicate since(...) specifier", modifier.getLexicalPhrase());
        }
        hasSince = true;
        break;
      case STATIC:
        throw new LanguageParseException("Unexpected modifier: Class definitions cannot be static", modifier.getLexicalPhrase());
      case UNBACKED:
        throw new LanguageParseException("Unexpected modifier: Class definitions cannot be unbacked", modifier.getLexicalPhrase());
      default:
        throw new IllegalStateException("Unknown modifier: " + modifier);
      }
    }
    return new ClassDefinition(isAbstract, isImmutable, name, typeParameters, superType, interfaceTypes, members, lexicalPhrase);
  }
}
