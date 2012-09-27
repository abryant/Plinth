package eu.bryants.anthony.toylanguage.parser.rules.member;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Initialiser;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 24 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class InitialiserRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> STATIC_PRODUCTION = new Production<ParseType>(ParseType.STATIC_KEYWORD, ParseType.BLOCK);
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
      Block block = (Block) args[1];
      return new Initialiser(true, block, LexicalPhrase.combine((LexicalPhrase) args[0], block.getLexicalPhrase()));
    }
    if (production == PRODUCTION)
    {
      Block block = (Block) args[0];
      return new Initialiser(false, block, block.getLexicalPhrase());
    }
    throw badTypeList();
  }

}
