package eu.bryants.anthony.toylanguage.parser.rules.member;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Initialiser;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.parser.LanguageParseException;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.Modifier;
import eu.bryants.anthony.toylanguage.parser.parseAST.ModifierType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;

/*
 * Created on 24 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class InitialiserRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> STATIC_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.BLOCK);
  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.BLOCK);

  @SuppressWarnings("unchecked")
  public InitialiserRule()
  {
    super(ParseType.INITIALISER, STATIC_PRODUCTION, PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == STATIC_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      boolean isStatic = false;
      for (Modifier modifier : modifiers)
      {
        if (modifier.getModifierType() != ModifierType.STATIC)
        {
          throw new LanguageParseException("An initialiser cannot have any modifiers but 'static'", modifier.getLexicalPhrase());
        }
        if (isStatic)
        {
          throw new LanguageParseException("Duplicate 'static' modifier for initialiser", modifier.getLexicalPhrase());
        }
        isStatic = true;
      }
      if (!isStatic)
      {
        // shouldn't really get here, but just to be safe:
        throw new LanguageParseException("Cannot have a static initialiser without a 'static' modifier", modifiers.getLexicalPhrase());
      }
      Block block = (Block) args[1];
      return new Initialiser(true, block, LexicalPhrase.combine(modifiers.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    if (production == PRODUCTION)
    {
      Block block = (Block) args[0];
      return new Initialiser(false, block, block.getLexicalPhrase());
    }
    throw badTypeList();
  }

}
