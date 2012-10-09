package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.terminal.StringLiteral;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ModifierType;
import eu.bryants.anthony.plinth.parser.parseAST.NativeSpecifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 28 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ModifiersRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_FINAL_PRODUCTION       = new Production<ParseType>(ParseType.FINAL_KEYWORD);
  private static final Production<ParseType> START_STATIC_PRODUCTION      = new Production<ParseType>(ParseType.STATIC_KEYWORD);
  private static final Production<ParseType> START_NATIVE_PRODUCTION      = new Production<ParseType>(ParseType.NATIVE_KEYWORD);
  private static final Production<ParseType> START_NATIVE_NAME_PRODUCTION = new Production<ParseType>(ParseType.NATIVE_KEYWORD, ParseType.STRING_LITERAL);
  private static final Production<ParseType> FINAL_PRODUCTION       = new Production<ParseType>(ParseType.MODIFIERS, ParseType.FINAL_KEYWORD);
  private static final Production<ParseType> STATIC_PRODUCTION      = new Production<ParseType>(ParseType.MODIFIERS, ParseType.STATIC_KEYWORD);
  private static final Production<ParseType> NATIVE_PRODUCTION      = new Production<ParseType>(ParseType.MODIFIERS, ParseType.NATIVE_KEYWORD);
  private static final Production<ParseType> NATIVE_NAME_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.NATIVE_KEYWORD, ParseType.STRING_LITERAL);

  @SuppressWarnings("unchecked")
  public ModifiersRule()
  {
    super(ParseType.MODIFIERS, START_FINAL_PRODUCTION, START_STATIC_PRODUCTION, START_NATIVE_PRODUCTION, START_NATIVE_NAME_PRODUCTION, FINAL_PRODUCTION, STATIC_PRODUCTION, NATIVE_PRODUCTION, NATIVE_NAME_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_FINAL_PRODUCTION)
    {
      return new ParseList<Modifier>(new Modifier(ModifierType.FINAL, (LexicalPhrase) args[0]), (LexicalPhrase) args[0]);
    }
    if (production == START_STATIC_PRODUCTION)
    {
      return new ParseList<Modifier>(new Modifier(ModifierType.STATIC, (LexicalPhrase) args[0]), (LexicalPhrase) args[0]);
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
    if (production == FINAL_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> list = (ParseList<Modifier>) args[0];
      Modifier modifier = new Modifier(ModifierType.FINAL, (LexicalPhrase) args[1]);
      list.addLast(modifier, LexicalPhrase.combine(list.getLexicalPhrase(), modifier.getLexicalPhrase()));
      return list;
    }
    if (production == STATIC_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> list = (ParseList<Modifier>) args[0];
      Modifier modifier = new Modifier(ModifierType.STATIC, (LexicalPhrase) args[1]);
      list.addLast(modifier, LexicalPhrase.combine(list.getLexicalPhrase(), modifier.getLexicalPhrase()));
      return list;
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
    throw badTypeList();
  }

}
