package eu.bryants.anthony.toylanguage.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.statement.Statement;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class StatementsRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> EMPTY_PRODUCTION = new Production<ParseType>();
  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.STATEMENTS, ParseType.STATEMENT);

  @SuppressWarnings("unchecked")
  public StatementsRule()
  {
    super(ParseType.STATEMENTS, EMPTY_PRODUCTION, PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == EMPTY_PRODUCTION)
    {
      return new ParseList<Statement>(null);
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
