package eu.bryants.anthony.plinth.parser.rules.member;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.SinceModifier;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class FieldRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION             = new Production<ParseType>(ParseType.TYPE, ParseType.NAME, ParseType.SEMICOLON);
  private static final Production<ParseType> INITIALISER_PRODUCTION = new Production<ParseType>(ParseType.TYPE, ParseType.NAME, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> MODIFIERS_PRODUCTION             = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE, ParseType.NAME, ParseType.SEMICOLON);
  private static final Production<ParseType> MODIFIERS_INITIALISER_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE, ParseType.NAME, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);

  public FieldRule()
  {
    super(ParseType.FIELD, PRODUCTION, INITIALISER_PRODUCTION, MODIFIERS_PRODUCTION, MODIFIERS_INITIALISER_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      Type type = (Type) args[0];
      Name name = (Name) args[1];
      return new Field(type, name.getName(), false, false, false, null, null, LexicalPhrase.combine(type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    if (production == INITIALISER_PRODUCTION)
    {
      Type type = (Type) args[0];
      Name name = (Name) args[1];
      Expression initialiserExpression = (Expression) args[3];
      return new Field(type, name.getName(), false, false, false, null, initialiserExpression, LexicalPhrase.combine(type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2], initialiserExpression.getLexicalPhrase(), (LexicalPhrase) args[4]));
    }
    if (production == MODIFIERS_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type type = (Type) args[1];
      Name name = (Name) args[2];
      return processModifiers(modifiers, type, name.getName(), null,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == MODIFIERS_INITIALISER_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type type = (Type) args[1];
      Name name = (Name) args[2];
      Expression initialiserExpression = (Expression) args[4];
      return processModifiers(modifiers, type, name.getName(), initialiserExpression,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[3], initialiserExpression.getLexicalPhrase(), (LexicalPhrase) args[5]));
    }
    throw badTypeList();
  }

  private Field processModifiers(ParseList<Modifier> modifiers, Type type, String name, Expression initialiserExpression, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isStatic = false;
    boolean isFinal = false;
    boolean isMutable = false;
    SinceSpecifier sinceSpecifier = null;
    for (Modifier modifier : modifiers)
    {
      switch (modifier.getModifierType())
      {
      case ABSTRACT:
        throw new LanguageParseException("Unexpected modifier: Fields cannot be abstract", modifier.getLexicalPhrase());
      case FINAL:
        if (isFinal)
        {
          throw new LanguageParseException("Duplicate 'final' modifier", modifier.getLexicalPhrase());
        }
        isFinal = true;
        break;
      case IMMUTABLE:
        throw new LanguageParseException("Unexpected modifier: The 'immutable' modifier does not apply to fields (try using #Type instead)", modifier.getLexicalPhrase());
      case MUTABLE:
        if (isMutable)
        {
          throw new LanguageParseException("Duplicate 'mutable' modifier", modifier.getLexicalPhrase());
        }
        isMutable = true;
        break;
      case NATIVE:
        throw new LanguageParseException("Unexpected modifier: Fields cannot be native", modifier.getLexicalPhrase());
      case SELFISH:
        throw new LanguageParseException("Unexpected modifier: Fields cannot be selfish", modifier.getLexicalPhrase());
      case SINCE:
        if (sinceSpecifier != null)
        {
          throw new LanguageParseException("Duplicate since(...) specifier", modifier.getLexicalPhrase());
        }
        sinceSpecifier = ((SinceModifier) modifier).getSinceSpecifier();
        break;
      case STATIC:
        if (isStatic)
        {
          throw new LanguageParseException("Duplicate 'static' modifier", modifier.getLexicalPhrase());
        }
        isStatic = true;
        break;
      default:
        throw new IllegalStateException("Unknown modifier: " + modifier);
      }
    }
    return new Field(type, name, isStatic, isFinal, isMutable, sinceSpecifier, initialiserExpression, lexicalPhrase);
  }
}
