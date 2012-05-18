package eu.bryants.anthony.toylanguage.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.statement.Statement;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseContainer;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 18 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ForInitRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.ASSIGN_STATEMENT);
  private static final Production<ParseType> BLANK_PRODUCTION = new Production<ParseType>(ParseType.SEMICOLON);

  @SuppressWarnings("unchecked")
  public ForInitRule()
  {
    super(ParseType.FOR_INIT, PRODUCTION, BLANK_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      Statement assign = (Statement) args[0];
      return new ParseContainer<Statement>(assign, assign.getLexicalPhrase());
    }
    if (production == BLANK_PRODUCTION)
    {
      return new ParseContainer<Statement>(null, (LexicalPhrase) args[0]);
    }
    throw badTypeList();
  }

}
