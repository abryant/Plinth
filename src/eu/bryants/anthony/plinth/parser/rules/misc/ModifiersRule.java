package eu.bryants.anthony.plinth.parser.rules.misc;

import java.util.HashMap;
import java.util.Map;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;
import eu.bryants.anthony.plinth.ast.terminal.StringLiteral;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ModifierType;
import eu.bryants.anthony.plinth.parser.parseAST.NativeSpecifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.SinceModifier;

/*
 * Created on 28 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ModifiersRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_ABSTRACT_PRODUCTION    = new Production<ParseType>(ParseType.ABSTRACT_KEYWORD);
  private static final Production<ParseType> START_FINAL_PRODUCTION       = new Production<ParseType>(ParseType.FINAL_KEYWORD);
  private static final Production<ParseType> START_IMMUTABLE_PRODUCTION   = new Production<ParseType>(ParseType.IMMUTABLE_KEYWORD);
  private static final Production<ParseType> START_MUTABLE_PRODUCTION     = new Production<ParseType>(ParseType.MUTABLE_KEYWORD);
  private static final Production<ParseType> START_SELFISH_PRODUCTION     = new Production<ParseType>(ParseType.SELFISH_KEYWORD);
  private static final Production<ParseType> START_STATIC_PRODUCTION      = new Production<ParseType>(ParseType.STATIC_KEYWORD);
  private static final Production<ParseType> START_UNBACKED_PRODUCTION    = new Production<ParseType>(ParseType.UNBACKED_KEYWORD);
  private static final Production<ParseType> ABSTRACT_PRODUCTION    = new Production<ParseType>(ParseType.MODIFIERS, ParseType.ABSTRACT_KEYWORD);
  private static final Production<ParseType> FINAL_PRODUCTION       = new Production<ParseType>(ParseType.MODIFIERS, ParseType.FINAL_KEYWORD);
  private static final Production<ParseType> IMMUTABLE_PRODUCTION   = new Production<ParseType>(ParseType.MODIFIERS, ParseType.IMMUTABLE_KEYWORD);
  private static final Production<ParseType> MUTABLE_PRODUCTION     = new Production<ParseType>(ParseType.MODIFIERS, ParseType.MUTABLE_KEYWORD);
  private static final Production<ParseType> SELFISH_PRODUCTION     = new Production<ParseType>(ParseType.MODIFIERS, ParseType.SELFISH_KEYWORD);
  private static final Production<ParseType> STATIC_PRODUCTION      = new Production<ParseType>(ParseType.MODIFIERS, ParseType.STATIC_KEYWORD);
  private static final Production<ParseType> UNBACKED_PRODUCTION    = new Production<ParseType>(ParseType.MODIFIERS, ParseType.UNBACKED_KEYWORD);

  private static final Production<ParseType> START_NATIVE_PRODUCTION      = new Production<ParseType>(ParseType.NATIVE_KEYWORD);
  private static final Production<ParseType> START_NATIVE_NAME_PRODUCTION = new Production<ParseType>(ParseType.NATIVE_KEYWORD, ParseType.STRING_LITERAL);
  private static final Production<ParseType> START_SINCE_PRODUCTION       = new Production<ParseType>(ParseType.SINCE_SPECIFIER);
  private static final Production<ParseType> NATIVE_PRODUCTION      = new Production<ParseType>(ParseType.MODIFIERS, ParseType.NATIVE_KEYWORD);
  private static final Production<ParseType> NATIVE_NAME_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.NATIVE_KEYWORD, ParseType.STRING_LITERAL);
  private static final Production<ParseType> SINCE_PRODUCTION       = new Production<ParseType>(ParseType.MODIFIERS, ParseType.SINCE_SPECIFIER);

  private static final Map<Production<ParseType>, ModifierType> START_KEYWORDS_MAP = new HashMap<Production<ParseType>, ModifierType>();
  private static final Map<Production<ParseType>, ModifierType>       KEYWORDS_MAP = new HashMap<Production<ParseType>, ModifierType>();
  static
  {
    START_KEYWORDS_MAP.put(START_ABSTRACT_PRODUCTION,  ModifierType.ABSTRACT);
    START_KEYWORDS_MAP.put(START_FINAL_PRODUCTION,     ModifierType.FINAL);
    START_KEYWORDS_MAP.put(START_IMMUTABLE_PRODUCTION, ModifierType.IMMUTABLE);
    START_KEYWORDS_MAP.put(START_MUTABLE_PRODUCTION,   ModifierType.MUTABLE);
    START_KEYWORDS_MAP.put(START_SELFISH_PRODUCTION,   ModifierType.SELFISH);
    START_KEYWORDS_MAP.put(START_STATIC_PRODUCTION,    ModifierType.STATIC);
    START_KEYWORDS_MAP.put(START_UNBACKED_PRODUCTION,  ModifierType.UNBACKED);
    KEYWORDS_MAP.put(ABSTRACT_PRODUCTION,  ModifierType.ABSTRACT);
    KEYWORDS_MAP.put(FINAL_PRODUCTION,     ModifierType.FINAL);
    KEYWORDS_MAP.put(IMMUTABLE_PRODUCTION, ModifierType.IMMUTABLE);
    KEYWORDS_MAP.put(MUTABLE_PRODUCTION,   ModifierType.MUTABLE);
    KEYWORDS_MAP.put(SELFISH_PRODUCTION,   ModifierType.SELFISH);
    KEYWORDS_MAP.put(STATIC_PRODUCTION,    ModifierType.STATIC);
    KEYWORDS_MAP.put(UNBACKED_PRODUCTION,  ModifierType.UNBACKED);
  }

  public ModifiersRule()
  {
    super(ParseType.MODIFIERS, START_ABSTRACT_PRODUCTION,    ABSTRACT_PRODUCTION,
                                  START_FINAL_PRODUCTION,       FINAL_PRODUCTION,
                              START_IMMUTABLE_PRODUCTION,   IMMUTABLE_PRODUCTION,
                                START_MUTABLE_PRODUCTION,     MUTABLE_PRODUCTION,
                                START_SELFISH_PRODUCTION,     SELFISH_PRODUCTION,
                                 START_STATIC_PRODUCTION,      STATIC_PRODUCTION,
                               START_UNBACKED_PRODUCTION,    UNBACKED_PRODUCTION,
                                 START_NATIVE_PRODUCTION,      NATIVE_PRODUCTION,
                            START_NATIVE_NAME_PRODUCTION, NATIVE_NAME_PRODUCTION,
                                  START_SINCE_PRODUCTION,       SINCE_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (START_KEYWORDS_MAP.containsKey(production))
    {
      return new ParseList<Modifier>(new Modifier(START_KEYWORDS_MAP.get(production), (LexicalPhrase) args[0]), (LexicalPhrase) args[0]);
    }
    if (KEYWORDS_MAP.containsKey(production))
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> list = (ParseList<Modifier>) args[0];
      Modifier modifier = new Modifier(KEYWORDS_MAP.get(production), (LexicalPhrase) args[1]);
      list.addLast(modifier, LexicalPhrase.combine(list.getLexicalPhrase(), modifier.getLexicalPhrase()));
      return list;
    }

    if (production == START_NATIVE_PRODUCTION)
    {
      return new ParseList<Modifier>(new NativeSpecifier(null, (LexicalPhrase) args[0]), (LexicalPhrase) args[0]);
    }
    if (production == START_NATIVE_NAME_PRODUCTION)
    {
      StringLiteral literal = (StringLiteral) args[1];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], literal.getLexicalPhrase());
      return new ParseList<Modifier>(new NativeSpecifier(literal.getLiteralValue(), lexicalPhrase), lexicalPhrase);
    }
    if (production == START_SINCE_PRODUCTION)
    {
      SinceSpecifier sinceSpecifier = (SinceSpecifier) args[0];
      return new ParseList<Modifier>(new SinceModifier(sinceSpecifier), sinceSpecifier.getLexicalPhrase());
    }
    if (production == NATIVE_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> list = (ParseList<Modifier>) args[0];
      Modifier modifier = new NativeSpecifier(null, (LexicalPhrase) args[1]);
      list.addLast(modifier, LexicalPhrase.combine(list.getLexicalPhrase(), modifier.getLexicalPhrase()));
      return list;
    }
    if (production == NATIVE_NAME_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> list = (ParseList<Modifier>) args[0];
      StringLiteral literal = (StringLiteral) args[2];
      Modifier modifier = new NativeSpecifier(literal.getLiteralValue(), LexicalPhrase.combine((LexicalPhrase) args[1], literal.getLexicalPhrase()));
      list.addLast(modifier, LexicalPhrase.combine(list.getLexicalPhrase(), modifier.getLexicalPhrase()));
      return list;
    }
    if (production == SINCE_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> list = (ParseList<Modifier>) args[0];
      SinceSpecifier sinceSpecifier = (SinceSpecifier) args[1];
      Modifier modifier = new SinceModifier(sinceSpecifier);
      list.addLast(modifier, LexicalPhrase.combine(list.getLexicalPhrase(), modifier.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
