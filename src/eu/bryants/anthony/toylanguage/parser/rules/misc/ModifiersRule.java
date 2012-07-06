package eu.bryants.anthony.toylanguage.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.terminal.StringLiteral;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.Modifier;
import eu.bryants.anthony.toylanguage.parser.parseAST.ModifierType;
import eu.bryants.anthony.toylanguage.parser.parseAST.NativeSpecifier;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;

/*
 * Created on 28 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ModifiersRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> EMPTY_PRODUCTION = new Production<ParseType>();
  private static final Production<ParseType> STATIC_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.STATIC_KEYWORD);
  private static final Production<ParseType> NATIVE_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.NATIVE_KEYWORD);
  private static final Production<ParseType> NATIVE_NAME_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.NATIVE_KEYWORD, ParseType.STRING_LITERAL);

  @SuppressWarnings("unchecked")
  public ModifiersRule()
  {
    super(ParseType.MODIFIERS, EMPTY_PRODUCTION, STATIC_PRODUCTION, NATIVE_PRODUCTION, NATIVE_NAME_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == EMPTY_PRODUCTION)
    {
      return new ParseList<Modifier>(null);
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
