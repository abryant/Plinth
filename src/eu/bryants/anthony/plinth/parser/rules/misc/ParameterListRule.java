package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.misc.AutoAssignParameter;
import eu.bryants.anthony.plinth.ast.misc.DefaultParameter;
import eu.bryants.anthony.plinth.ast.misc.NormalParameter;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 8 Oct 2012
 */

/**
 * @author Anthony Bryant
 */
public class ParameterListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> NORMAL_START_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.TYPE, ParseType.NAME);
  private static Production<ParseType> DEFAULT_START_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.TYPE, ParseType.NAME, ParseType.EQUALS, ParseType.CONDITIONAL_EXPRESSION);
  private static Production<ParseType> DEFAULT_QNAME_START_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.TYPE, ParseType.NAME, ParseType.EQUALS, ParseType.QNAME_OR_LESS_THAN_EXPRESSION);
  private static Production<ParseType> AUTO_ASSIGN_START_PRODUCTION = new Production<ParseType>(ParseType.AT, ParseType.NAME);
  private static Production<ParseType> NORMAL_CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.PARAMETER_LIST, ParseType.COMMA, ParseType.OPTIONAL_MODIFIERS, ParseType.TYPE, ParseType.NAME);
  private static Production<ParseType> DEFAULT_CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.PARAMETER_LIST, ParseType.COMMA, ParseType.OPTIONAL_MODIFIERS, ParseType.TYPE, ParseType.NAME, ParseType.EQUALS, ParseType.CONDITIONAL_EXPRESSION);
  private static Production<ParseType> DEFAULT_QNAME_CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.PARAMETER_LIST, ParseType.COMMA, ParseType.OPTIONAL_MODIFIERS, ParseType.TYPE, ParseType.NAME, ParseType.EQUALS, ParseType.QNAME_OR_LESS_THAN_EXPRESSION);
  private static Production<ParseType> AUTO_ASSIGN_CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.PARAMETER_LIST, ParseType.COMMA, ParseType.AT, ParseType.NAME);

  public ParameterListRule()
  {
    super(ParseType.PARAMETER_LIST, NORMAL_START_PRODUCTION,        DEFAULT_START_PRODUCTION,        DEFAULT_QNAME_START_PRODUCTION,        AUTO_ASSIGN_START_PRODUCTION,
                                    NORMAL_CONTINUATION_PRODUCTION, DEFAULT_CONTINUATION_PRODUCTION, DEFAULT_QNAME_CONTINUATION_PRODUCTION, AUTO_ASSIGN_CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == NORMAL_START_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type type = (Type) args[1];
      Name name = (Name) args[2];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(modifiers.getLexicalPhrase(), type.getLexicalPhrase(), name.getLexicalPhrase());
      Parameter parameter = processModifiers(modifiers, type, name.getName(), null, lexicalPhrase);
      return new ParseList<Parameter>(parameter, lexicalPhrase);
    }
    if (production == DEFAULT_START_PRODUCTION || production == DEFAULT_QNAME_START_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type type = (Type) args[1];
      Name name = (Name) args[2];
      Expression expression = (Expression) args[4];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(modifiers.getLexicalPhrase(), type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[3], expression.getLexicalPhrase());
      Parameter parameter = processModifiers(modifiers, type, name.getName(), expression, lexicalPhrase);
      return new ParseList<Parameter>(parameter, lexicalPhrase);
    }
    if (production == AUTO_ASSIGN_START_PRODUCTION)
    {
      Name name = (Name) args[1];
      Parameter autoAssignParameter = new AutoAssignParameter(name.getName(), LexicalPhrase.combine((LexicalPhrase) args[0], name.getLexicalPhrase()));
      return new ParseList<Parameter>(autoAssignParameter, autoAssignParameter.getLexicalPhrase());
    }
    if (production == NORMAL_CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[2];
      Type type = (Type) args[3];
      Name name = (Name) args[4];
      LexicalPhrase parameterPhrase = LexicalPhrase.combine(modifiers.getLexicalPhrase(), type.getLexicalPhrase(), name.getLexicalPhrase());
      Parameter parameter = processModifiers(modifiers, type, name.getName(), null, parameterPhrase);
      parameters.addLast(parameter, LexicalPhrase.combine(parameters.getLexicalPhrase(), (LexicalPhrase) args[1], parameterPhrase));
      return parameters;
    }
    if (production == DEFAULT_CONTINUATION_PRODUCTION || production == DEFAULT_QNAME_CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[2];
      Type type = (Type) args[3];
      Name name = (Name) args[4];
      Expression expression = (Expression) args[6];
      LexicalPhrase parameterPhrase = LexicalPhrase.combine(modifiers.getLexicalPhrase(), type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[5], expression.getLexicalPhrase());
      Parameter parameter = processModifiers(modifiers, type, name.getName(), expression, parameterPhrase);
      parameters.addLast(parameter, LexicalPhrase.combine(parameters.getLexicalPhrase(), (LexicalPhrase) args[1], parameterPhrase));
      return parameters;
    }
    if (production == AUTO_ASSIGN_CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[0];
      Name name = (Name) args[3];
      Parameter autoAssignParameter = new AutoAssignParameter(name.getName(), LexicalPhrase.combine((LexicalPhrase) args[2], name.getLexicalPhrase()));
      parameters.addLast(autoAssignParameter, LexicalPhrase.combine(parameters.getLexicalPhrase(), (LexicalPhrase) args[1], autoAssignParameter.getLexicalPhrase()));
      return parameters;
    }
    throw badTypeList();
  }

  private Parameter processModifiers(ParseList<Modifier> modifiers, Type type, String name, Expression expression, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isFinal = false;
    for (Modifier modifier : modifiers)
    {
      switch (modifier.getModifierType())
      {
      case ABSTRACT:
        throw new LanguageParseException("Unexpected modifier: Parameters cannot be abstract", modifier.getLexicalPhrase());
      case FINAL:
        if (isFinal)
        {
          throw new LanguageParseException("Duplicate 'final' modifier", modifier.getLexicalPhrase());
        }
        isFinal = true;
        break;
      case IMMUTABLE:
        throw new LanguageParseException("Unexpected modifier: The 'immutable' modifier does not apply to parameters (try using #Type instead)", modifier.getLexicalPhrase());
      case MUTABLE:
        throw new LanguageParseException("Unexpected modifier: Parameters cannot be mutable", modifier.getLexicalPhrase());
      case NATIVE:
        throw new LanguageParseException("Unexpected modifier: Parameters cannot have native specifiers", modifier.getLexicalPhrase());
      case SELFISH:
        throw new LanguageParseException("Unexpected modifier: Parameters cannot be selfish", modifier.getLexicalPhrase());
      case SINCE:
        throw new LanguageParseException("Unexpected modifier: Parameters cannot have since(...) specifiers", modifier.getLexicalPhrase());
      case STATIC:
        throw new LanguageParseException("Unexpected modifier: Parameters cannot be static", modifier.getLexicalPhrase());
      case UNBACKED:
        throw new LanguageParseException("Unexpected modifier: Parameters cannot be unbacked", modifier.getLexicalPhrase());
      default:
        throw new IllegalStateException("Unknown modifier: " + modifier);
      }
    }
    if (expression == null)
    {
      return new NormalParameter(isFinal, type, name, lexicalPhrase);
    }
    return new DefaultParameter(isFinal, type, name, expression, lexicalPhrase);
  }
}
