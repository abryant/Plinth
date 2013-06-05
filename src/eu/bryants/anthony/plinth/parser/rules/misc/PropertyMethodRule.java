package eu.bryants.anthony.plinth.parser.rules.misc;

import java.util.LinkedList;
import java.util.List;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.PropertyMethod;
import eu.bryants.anthony.plinth.parser.parseAST.PropertyMethod.PropertyMethodType;
import eu.bryants.anthony.plinth.parser.parseAST.ThrownExceptionType;

/*
 * Created on 1 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class PropertyMethodRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> GETTER_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.GETTER_KEYWORD);
  private static final Production<ParseType> GETTER_BLOCK_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.GETTER_KEYWORD, ParseType.THROWS_CLAUSE, ParseType.BLOCK);
  private static final Production<ParseType> SETTER_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.SETTER_KEYWORD);
  private static final Production<ParseType> SETTER_BLOCK_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.SETTER_KEYWORD, ParseType.LPAREN, ParseType.OPTIONAL_MODIFIERS, ParseType.NAME, ParseType.RPAREN, ParseType.THROWS_CLAUSE, ParseType.BLOCK);
  private static final Production<ParseType> SETTER_BLOCK_PARAMETERS_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.SETTER_KEYWORD, ParseType.PARAMETER_LIST, ParseType.THROWS_CLAUSE, ParseType.BLOCK);
  private static final Production<ParseType> CONSTRUCTOR_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.CREATE_KEYWORD);
  private static final Production<ParseType> CONSTRUCTOR_BLOCK_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.CREATE_KEYWORD, ParseType.LPAREN, ParseType.OPTIONAL_MODIFIERS, ParseType.NAME, ParseType.RPAREN, ParseType.THROWS_CLAUSE, ParseType.BLOCK);
  private static final Production<ParseType> CONSTRUCTOR_BLOCK_PARAMETERS_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.CREATE_KEYWORD, ParseType.PARAMETER_LIST, ParseType.THROWS_CLAUSE, ParseType.BLOCK);

  public PropertyMethodRule()
  {
    super(ParseType.PROPERTY_METHOD, GETTER_PRODUCTION, GETTER_BLOCK_PRODUCTION,
                                     SETTER_PRODUCTION, SETTER_BLOCK_PRODUCTION, SETTER_BLOCK_PARAMETERS_PRODUCTION,
                                     CONSTRUCTOR_PRODUCTION, CONSTRUCTOR_BLOCK_PRODUCTION, CONSTRUCTOR_BLOCK_PARAMETERS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == GETTER_PRODUCTION || production == SETTER_PRODUCTION || production == CONSTRUCTOR_PRODUCTION)
    {
      PropertyMethodType type = PropertyMethodType.GETTER;
      if (production == SETTER_PRODUCTION)
      {
        type = PropertyMethodType.SETTER;
      }
      else if (production == CONSTRUCTOR_PRODUCTION)
      {
        type = PropertyMethodType.CONSTRUCTOR;
      }
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      return processModifiers(modifiers, type, null, null, null, null, null, LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1]));
    }
    if (production == GETTER_BLOCK_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> thrownTypesList = (ParseList<ThrownExceptionType>) args[2];
      NamedType[] uncheckedThrownTypes = getThrownTypes(thrownTypesList);
      Block block = (Block) args[3];
      return processModifiers(modifiers, PropertyMethodType.GETTER, null, null, null, uncheckedThrownTypes, block, LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], thrownTypesList.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    if (production == SETTER_BLOCK_PRODUCTION || production == CONSTRUCTOR_BLOCK_PRODUCTION)
    {
      PropertyMethodType type = PropertyMethodType.SETTER;
      if (production == CONSTRUCTOR_BLOCK_PRODUCTION)
      {
        type = PropertyMethodType.CONSTRUCTOR;
      }
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Modifier> parameterModifiers = (ParseList<Modifier>) args[3];
      Name parameterName = (Name) args[4];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> thrownTypesList = (ParseList<ThrownExceptionType>) args[6];
      NamedType[] uncheckedThrownTypes = getThrownTypes(thrownTypesList);
      Block block = (Block) args[7];
      return processModifiers(modifiers, type, null, parameterModifiers, parameterName, uncheckedThrownTypes, block,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], (LexicalPhrase) args[2], parameterModifiers.getLexicalPhrase(), parameterName.getLexicalPhrase(), (LexicalPhrase) args[5], thrownTypesList.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    if (production == SETTER_BLOCK_PARAMETERS_PRODUCTION || production == CONSTRUCTOR_BLOCK_PARAMETERS_PRODUCTION)
    {
      PropertyMethodType type = PropertyMethodType.SETTER;
      if (production == CONSTRUCTOR_BLOCK_PARAMETERS_PRODUCTION)
      {
        type = PropertyMethodType.CONSTRUCTOR;
      }
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[2];
      if (parameters.size() != 1)
      {
        throw new LanguageParseException("A " + type + " must take exactly one parameter", parameters.getLexicalPhrase());
      }
      Parameter parameter = parameters.iterator().next();
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> thrownTypesList = (ParseList<ThrownExceptionType>) args[3];
      NamedType[] uncheckedThrownTypes = getThrownTypes(thrownTypesList);
      Block block = (Block) args[4];
      return processModifiers(modifiers, type, parameter, null, null, uncheckedThrownTypes, block,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], parameters.getLexicalPhrase(), thrownTypesList.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    throw badTypeList();
  }

  private static PropertyMethod processModifiers(ParseList<Modifier> modifiers, PropertyMethodType type, Parameter parameter, ParseList<Modifier> parameterModifiers, Name parameterName, NamedType[] uncheckedThrownTypes, Block block, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isMutable = false;
    boolean isImmutable = false;
    if (modifiers != null)
    {
      for (Modifier modifier : modifiers)
      {
        switch (modifier.getModifierType())
        {
        case ABSTRACT:
          throw new LanguageParseException("A " + type + " cannot be abstract", modifier.getLexicalPhrase());
        case FINAL:
          throw new LanguageParseException("A " + type + " cannot be final", modifier.getLexicalPhrase());
        case IMMUTABLE:
          if (isImmutable)
          {
            throw new LanguageParseException("Duplicate 'immutable' modifier", modifier.getLexicalPhrase());
          }
          isImmutable = true;
          break;
        case MUTABLE:
          if (isMutable)
          {
            throw new LanguageParseException("Duplicate 'mutable' modifier", modifier.getLexicalPhrase());
          }
          isMutable = true;
          break;
        case NATIVE:
          throw new LanguageParseException("A " + type + " cannot be native", modifier.getLexicalPhrase());
        case SELFISH:
          throw new LanguageParseException("A " + type + " cannot be selfish", modifier.getLexicalPhrase());
        case SINCE:
          throw new LanguageParseException("A " + type + " cannot have a since specifier", modifier.getLexicalPhrase());
        case STATIC:
          throw new LanguageParseException("A " + type + " cannot be static (maybe you want to make the whole property static?)", modifier.getLexicalPhrase());
        case UNBACKED:
          throw new LanguageParseException("A " + type + " cannot be unbacked", modifier.getLexicalPhrase());
        default:
          throw new IllegalArgumentException("Unknown modifier type: " + modifier.getModifierType());
        }
      }
      if (isMutable & isImmutable)
      {
        throw new LanguageParseException("A " + type + " cannot be both mutable and immutable", modifiers.getLexicalPhrase());
      }
    }
    // getters default to immutable, setters and constructors default to mutable
    boolean isReallyImmutable = (type == PropertyMethodType.GETTER) ? !isMutable : isImmutable;

    if (parameter != null)
    {
      return new PropertyMethod(type, isReallyImmutable, parameter, uncheckedThrownTypes, block, lexicalPhrase);
    }

    boolean isParameterFinal = false;
    if (parameterModifiers != null)
    {
      for (Modifier modifier : parameterModifiers)
      {
        switch (modifier.getModifierType())
        {
        case ABSTRACT:
          throw new LanguageParseException("A " + type + " parameter cannot be abstract", modifier.getLexicalPhrase());
        case FINAL:
          if (isParameterFinal)
          {
            throw new LanguageParseException("Duplicate 'final' modifier", modifier.getLexicalPhrase());
          }
          isParameterFinal = true;
          break;
        case IMMUTABLE:
          throw new LanguageParseException("A " + type + " parameter cannot be immutable", modifier.getLexicalPhrase());
        case MUTABLE:
          throw new LanguageParseException("A " + type + " parameter cannot be mutable", modifier.getLexicalPhrase());
        case NATIVE:
          throw new LanguageParseException("A " + type + " parameter cannot be native", modifier.getLexicalPhrase());
        case SELFISH:
          throw new LanguageParseException("A " + type + " parameter cannot be selfish", modifier.getLexicalPhrase());
        case SINCE:
          throw new LanguageParseException("A " + type + " parameter cannot have a since specifier", modifier.getLexicalPhrase());
        case STATIC:
          throw new LanguageParseException("A " + type + " parameter cannot be static (maybe you want to make the whole property static?)", modifier.getLexicalPhrase());
        case UNBACKED:
          throw new LanguageParseException("A " + type + " parameter cannot be unbacked", modifier.getLexicalPhrase());
        default:
          throw new IllegalArgumentException("Unknown modifier type: " + modifier.getModifierType());
        }
      }
    }
    LexicalPhrase parameterLexicalPhrase = LexicalPhrase.combine(parameterModifiers == null ? null : parameterModifiers.getLexicalPhrase(),
                                                                 parameterName == null ? null : parameterName.getLexicalPhrase());
    return new PropertyMethod(type, isReallyImmutable, isParameterFinal, parameterName == null ? null : parameterName.getName(), parameterLexicalPhrase, uncheckedThrownTypes, block, lexicalPhrase);
  }

  private static NamedType[] getThrownTypes(ParseList<ThrownExceptionType> list) throws LanguageParseException
  {
    if (list.size() == 0)
    {
      return null;
    }
    List<NamedType> types = new LinkedList<NamedType>();
    for (ThrownExceptionType type : list)
    {
      if (!type.isUnchecked())
      {
        throw new LanguageParseException("A property method cannot throw checked exceptions. Consider marking them as unchecked.", type.getLexicalPhrase());
      }
      types.add(type.getType());
    }
    return types.toArray(new NamedType[types.size()]);
  }
}
