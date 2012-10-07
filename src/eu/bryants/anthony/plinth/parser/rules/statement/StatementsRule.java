package eu.bryants.anthony.plinth.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.statement.Statement;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class StatementsRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.STATEMENT);
  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.STATEMENTS, ParseType.STATEMENT);

  @SuppressWarnings("unchecked")
  public StatementsRule()
  {
    super(ParseType.STATEMENTS, START_PRODUCTION, PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      Statement statement = (Statement) args[0];
      return new ParseList<Statement>(statement, statement.getLexicalPhrase());
    }
    if (production == PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Statement> list = (ParseList<Statement>) args[0];
      Statement statement = (Statement) args[1];
      list.addLast(statement, LexicalPhrase.combine(list.getLexicalPhrase(), statement.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
