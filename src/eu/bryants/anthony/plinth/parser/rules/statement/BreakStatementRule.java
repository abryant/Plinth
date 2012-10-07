package eu.bryants.anthony.plinth.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.statement.BreakStatement;
import eu.bryants.anthony.plinth.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 13 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class BreakStatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.BREAK_KEYWORD, ParseType.SEMICOLON);
  private static final Production<ParseType> INTEGER_PRODUCTION = new Production<ParseType>(ParseType.BREAK_KEYWORD, ParseType.INTEGER_LITERAL, ParseType.SEMICOLON);

  @SuppressWarnings("unchecked")
  public BreakStatementRule()
  {
    super(ParseType.BREAK_STATEMENT, PRODUCTION, INTEGER_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      return new BreakStatement(null, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1]));
    }
    if (production == INTEGER_PRODUCTION)
    {
      IntegerLiteral literal = (IntegerLiteral) args[1];
      return new BreakStatement(literal, LexicalPhrase.combine((LexicalPhrase) args[0], literal.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    throw badTypeList();
  }

}
