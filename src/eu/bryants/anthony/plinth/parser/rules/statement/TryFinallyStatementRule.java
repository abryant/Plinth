package eu.bryants.anthony.plinth.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.CatchClause;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.statement.TryStatement;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 16 Feb 2013
 */

/**
 * @author Anthony Bryant
 */
public class TryFinallyStatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.TRY_KEYWORD, ParseType.BLOCK, ParseType.FINALLY_KEYWORD, ParseType.BLOCK);
  private static final Production<ParseType> CATCH_PRODUCTION = new Production<ParseType>(ParseType.TRY_CATCH_STATEMENT, ParseType.FINALLY_KEYWORD, ParseType.BLOCK);

  public TryFinallyStatementRule()
  {
    super(ParseType.TRY_FINALLY_STATEMENT, PRODUCTION, CATCH_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      Block tryBlock = (Block) args[1];
      Block finallyBlock = (Block) args[3];
      return new TryStatement(tryBlock, new CatchClause[0], finallyBlock, LexicalPhrase.combine((LexicalPhrase) args[0], tryBlock.getLexicalPhrase(), (LexicalPhrase) args[2], finallyBlock.getLexicalPhrase()));
    }
    if (production == CATCH_PRODUCTION)
    {
      TryStatement oldStatement = (TryStatement) args[0];
      Block finallyBlock = (Block) args[2];
      return new TryStatement(oldStatement.getTryBlock(), oldStatement.getCatchClauses(), finallyBlock, LexicalPhrase.combine(oldStatement.getLexicalPhrase(), (LexicalPhrase) args[1], finallyBlock.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
