package eu.bryants.anthony.toylanguage.parser.rules.member;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 11 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ConstructorRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.NAME, ParseType.LPAREN, ParseType.RPAREN, ParseType.BLOCK);
  private static final Production<ParseType> PARAMETERS_PRODUCTION = new Production<ParseType>(ParseType.NAME, ParseType.LPAREN, ParseType.PARAMETERS, ParseType.RPAREN, ParseType.BLOCK);

  @SuppressWarnings("unchecked")
  public ConstructorRule()
  {
    super(ParseType.CONSTRUCTOR, PRODUCTION, PARAMETERS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      Name name = (Name) args[0];
      Block block = (Block) args[3];
      return new Constructor(name.getName(), new Parameter[0], block, LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], (LexicalPhrase) args[2], block.getLexicalPhrase()));
    }
    if (production == PARAMETERS_PRODUCTION)
    {
      Name name = (Name) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[2];
      Block block = (Block) args[4];
      return new Constructor(name.getName(), parameters.toArray(new Parameter[parameters.size()]), block,
                             LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], parameters.getLexicalPhrase(), (LexicalPhrase) args[3], block.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
