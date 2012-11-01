package eu.bryants.anthony.plinth.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.statement.ReturnStatement;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ReturnStatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.RETURN_KEYWORD, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> VOID_PRODUCTION = new Production<ParseType>(ParseType.RETURN_KEYWORD, ParseType.SEMICOLON);

  public ReturnStatementRule()
  {
    super(ParseType.RETURN_STATEMENT, PRODUCTION, VOID_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      Expression expression = (Expression) args[1];
      return new ReturnStatement(expression, LexicalPhrase.combine((LexicalPhrase) args[0], expression.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    if (production == VOID_PRODUCTION)
    {
      return new ReturnStatement(null, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1]));
    }
    throw badTypeList();
  }

}
