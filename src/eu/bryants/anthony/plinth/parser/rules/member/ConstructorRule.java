package eu.bryants.anthony.plinth.parser.rules.member;

import java.util.LinkedList;
import java.util.List;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.SinceModifier;
import eu.bryants.anthony.plinth.parser.parseAST.ThrownExceptionType;

/*
 * Created on 11 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ConstructorRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> MODIFIERS_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.THIS_KEYWORD, ParseType.PARAMETER_LIST, ParseType.THROWS_CLAUSE, ParseType.BLOCK);
  private static final Production<ParseType> PRODUCTION           = new Production<ParseType>(                     ParseType.THIS_KEYWORD, ParseType.PARAMETER_LIST, ParseType.THROWS_CLAUSE, ParseType.BLOCK);

  public ConstructorRule()
  {
    super(ParseType.CONSTRUCTOR, MODIFIERS_PRODUCTION, PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == MODIFIERS_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[2];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[3];
      List<NamedType> checkedThrownTypes = new LinkedList<NamedType>();
      List<NamedType> uncheckedThrownTypes = new LinkedList<NamedType>();
      for (ThrownExceptionType thrownExceptionType : throwsList)
      {
        if (thrownExceptionType.isUnchecked())
        {
          uncheckedThrownTypes.add(thrownExceptionType.getType());
        }
        else
        {
          checkedThrownTypes.add(thrownExceptionType.getType());
        }
      }
      Block block = (Block) args[4];
      return processModifiers(modifiers, parameters.toArray(new Parameter[parameters.size()]),
                              checkedThrownTypes.toArray(new NamedType[checkedThrownTypes.size()]), uncheckedThrownTypes.toArray(new NamedType[uncheckedThrownTypes.size()]),
                              block,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], parameters.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    if (production == PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[1];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[2];
      List<NamedType> checkedThrownTypes = new LinkedList<NamedType>();
      List<NamedType> uncheckedThrownTypes = new LinkedList<NamedType>();
      for (ThrownExceptionType thrownExceptionType : throwsList)
      {
        if (thrownExceptionType.isUnchecked())
        {
          uncheckedThrownTypes.add(thrownExceptionType.getType());
        }
        else
        {
          checkedThrownTypes.add(thrownExceptionType.getType());
        }
      }
      Block block = (Block) args[3];
      return new Constructor(false, false, null, parameters.toArray(new Parameter[parameters.size()]),
                             checkedThrownTypes.toArray(new NamedType[checkedThrownTypes.size()]), uncheckedThrownTypes.toArray(new NamedType[uncheckedThrownTypes.size()]),
                             block,
                             LexicalPhrase.combine((LexicalPhrase) args[0], parameters.getLexicalPhrase(), throwsList.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    throw badTypeList();
  }

  private Constructor processModifiers(ParseList<Modifier> modifiers, Parameter[] parameters, NamedType[] checkedThrownTypes, NamedType[] uncheckedThrownTypes, Block block, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isImmutable = false;
    boolean isSelfish = false;
    SinceSpecifier sinceSpecifier = null;
    for (Modifier modifier : modifiers)
    {
      switch (modifier.getModifierType())
      {
      case ABSTRACT:
        throw new LanguageParseException("Unexpected modifier: Constructors cannot be abstract", modifier.getLexicalPhrase());
      case FINAL:
        throw new LanguageParseException("Unexpected modifier: Constructors cannot be final", modifier.getLexicalPhrase());
      case IMMUTABLE:
        if (isImmutable)
        {
          throw new LanguageParseException("Duplicate 'immutable' modifier", modifier.getLexicalPhrase());
        }
        isImmutable = true;
        break;
      case MUTABLE:
        throw new LanguageParseException("Unexpected modifier: Constructors cannot be mutable", modifier.getLexicalPhrase());
      case NATIVE:
        throw new LanguageParseException("Unexpected modifier: Constructors cannot be native functions", modifier.getLexicalPhrase());
      case SELFISH:
        if (isSelfish)
        {
          throw new LanguageParseException("Duplicate 'selfish' modifier", modifier.getLexicalPhrase());
        }
        isSelfish = true;
        break;
      case SINCE:
        if (sinceSpecifier != null)
        {
          throw new LanguageParseException("Duplicate since(...) specifier", modifier.getLexicalPhrase());
        }
        sinceSpecifier = ((SinceModifier) modifier).getSinceSpecifier();
        break;
      case STATIC:
        throw new LanguageParseException("Unexpected modifier: Constructors cannot be static", modifier.getLexicalPhrase());
      default:
        throw new IllegalStateException("Unknown modifier: " + modifier);
      }
    }
    return new Constructor(isImmutable, isSelfish, sinceSpecifier, parameters, checkedThrownTypes, uncheckedThrownTypes, block, lexicalPhrase);
  }
}
