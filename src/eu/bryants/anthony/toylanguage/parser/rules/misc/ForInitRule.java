package eu.bryants.anthony.toylanguage.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.statement.Statement;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseContainer;

/*
 * Created on 18 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ForInitRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> ASSIGN_PRODUCTION = new Production<ParseType>(ParseType.ASSIGN_STATEMENT);
  private static final Production<ParseType> SHORTHAND_ASSIGN_PRODUCTION = new Production<ParseType>(ParseType.SHORTHAND_ASSIGNMENT, ParseType.SEMICOLON);
  private static final Production<ParseType> BLANK_PRODUCTION = new Production<ParseType>(ParseType.SEMICOLON);

  @SuppressWarnings("unchecked")
  public ForInitRule()
  {
    super(ParseType.FOR_INIT, ASSIGN_PRODUCTION, SHORTHAND_ASSIGN_PRODUCTION, BLANK_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == ASSIGN_PRODUCTION)
    {
      Statement statement = (Statement) args[0];
      return new ParseContainer<Statement>(statement, statement.getLexicalPhrase());
    }
    if (production == SHORTHAND_ASSIGN_PRODUCTION)
    {
      Statement statement = (Statement) args[0];
      return new ParseContainer<Statement>(statement, LexicalPhrase.combine(statement.getLexicalPhrase(), (LexicalPhrase) args[1]));
    }
    if (production == BLANK_PRODUCTION)
    {
      return new ParseContainer<Statement>(null, (LexicalPhrase) args[0]);
    }
    throw badTypeList();
  }

}
