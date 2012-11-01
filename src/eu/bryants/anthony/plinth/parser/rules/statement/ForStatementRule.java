package eu.bryants.anthony.plinth.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.statement.ForStatement;
import eu.bryants.anthony.plinth.ast.statement.Statement;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseContainer;

/*
 * Created on 18 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ForStatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION              = new Production<ParseType>(ParseType.FOR_KEYWORD, ParseType.LPAREN, ParseType.FOR_INIT, ParseType.EXPRESSION, ParseType.SEMICOLON, ParseType.FOR_UPDATE, ParseType.RPAREN, ParseType.BLOCK);
  private static final Production<ParseType> NO_CONDITION_PRODUCTION = new Production<ParseType>(ParseType.FOR_KEYWORD, ParseType.LPAREN, ParseType.FOR_INIT,                       ParseType.SEMICOLON, ParseType.FOR_UPDATE, ParseType.RPAREN, ParseType.BLOCK);

  public ForStatementRule()
  {
    super(ParseType.FOR_STATEMENT, PRODUCTION, NO_CONDITION_PRODUCTION);
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
      ParseContainer<Statement> init = (ParseContainer<Statement>) args[2];
      Expression condition = (Expression) args[3];
      Statement update = (Statement) args[5];
      Block block = (Block) args[7];
      return new ForStatement(init.getItem(), condition, update, block,
                              LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], init.getLexicalPhrase(), condition.getLexicalPhrase(), (LexicalPhrase) args[4],
                                                    update == null ? null : update.getLexicalPhrase(), (LexicalPhrase) args[6], block.getLexicalPhrase()));
    }
    if (production == NO_CONDITION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseContainer<Statement> init = (ParseContainer<Statement>) args[2];
      Statement update = (Statement) args[4];
      Block block = (Block) args[6];
      return new ForStatement(init.getItem(), null, update, block,
                              LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], init.getLexicalPhrase(), (LexicalPhrase) args[3],
                                                    update == null ? null : update.getLexicalPhrase(), (LexicalPhrase) args[5], block.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
