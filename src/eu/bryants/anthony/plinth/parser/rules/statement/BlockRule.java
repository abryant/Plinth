package eu.bryants.anthony.plinth.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.statement.Statement;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class BlockRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.LBRACE, ParseType.STATEMENTS, ParseType.RBRACE);
  private static final Production<ParseType> EMPTY_PRODUCTION = new Production<ParseType>(ParseType.LBRACE, ParseType.RBRACE);

  @SuppressWarnings("unchecked")
  public BlockRule()
  {
    super(ParseType.BLOCK, PRODUCTION, EMPTY_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Statement> list = (ParseList<Statement>) args[1];
      return new Block(list.toArray(new Statement[list.size()]), LexicalPhrase.combine((LexicalPhrase) args[0], list.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    if (production == EMPTY_PRODUCTION)
    {
      return new Block(new Statement[0], LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1]));
    }
    throw badTypeList();
  }

}
