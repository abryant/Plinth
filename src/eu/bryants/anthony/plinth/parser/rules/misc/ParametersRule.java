package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ModifierType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ParametersRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.TYPE, ParseType.NAME);
  private static Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.PARAMETERS, ParseType.COMMA, ParseType.OPTIONAL_MODIFIERS, ParseType.TYPE, ParseType.NAME);

  public ParametersRule()
  {
    super(ParseType.PARAMETERS, START_PRODUCTION, CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type type = (Type) args[1];
      Name name = (Name) args[2];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(modifiers.getLexicalPhrase(), type.getLexicalPhrase(), name.getLexicalPhrase());
      Parameter parameter = processModifiers(modifiers, type, name.getName(), lexicalPhrase);
      return new ParseList<Parameter>(parameter, lexicalPhrase);
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[2];
      Type type = (Type) args[3];
      Name name = (Name) args[4];
      LexicalPhrase parameterPhrase = LexicalPhrase.combine(modifiers.getLexicalPhrase(), type.getLexicalPhrase(), name.getLexicalPhrase());
      Parameter parameter = processModifiers(modifiers, type, name.getName(), parameterPhrase);
      parameters.addLast(parameter, LexicalPhrase.combine(parameters.getLexicalPhrase(), (LexicalPhrase) args[1], parameterPhrase));
      return parameters;
    }
    throw badTypeList();
  }

  private Parameter processModifiers(ParseList<Modifier> modifiers, Type type, String name, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isFinal = false;
    for (Modifier modifier : modifiers)
    {
      if (modifier.getModifierType() == ModifierType.FINAL)
      {
        if (isFinal)
        {
          throw new LanguageParseException("Duplicate 'final' modifier", modifier.getLexicalPhrase());
        }
        isFinal = true;
      }
      else if (modifier.getModifierType() == ModifierType.NATIVE)
      {
        throw new LanguageParseException("Unexpected modifier: Local variables cannot have native specifiers", modifier.getLexicalPhrase());
      }
      else if (modifier.getModifierType() == ModifierType.STATIC)
      {
        throw new LanguageParseException("Unexpected modifier: Local variables cannot be static", modifier.getLexicalPhrase());
      }
      else
      {
        throw new IllegalStateException("Unknown modifier: " + modifier);
      }
    }
    return new Parameter(isFinal, type, name, lexicalPhrase);
  }
}
