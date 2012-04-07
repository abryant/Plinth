package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.Expression;
import eu.bryants.anthony.toylanguage.ast.IfStatement;
import eu.bryants.anthony.toylanguage.ast.Statement;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 7 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class IfStatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> IF_PRODUCTION         = new Production<ParseType>(ParseType.IF_KEYWORD, ParseType.EXPRESSION, ParseType.BLOCK);
  private static final Production<ParseType> IF_ELSE_PRODUCTION    = new Production<ParseType>(ParseType.IF_KEYWORD, ParseType.EXPRESSION, ParseType.BLOCK, ParseType.ELSE_KEYWORD, ParseType.BLOCK);
  private static final Production<ParseType> IF_ELSE_IF_PRODUCTION = new Production<ParseType>(ParseType.IF_KEYWORD, ParseType.EXPRESSION, ParseType.BLOCK, ParseType.ELSE_KEYWORD, ParseType.IF_STATEMENT);

  @SuppressWarnings("unchecked")
  public IfStatementRule()
  {
    super(ParseType.IF_STATEMENT, IF_PRODUCTION, IF_ELSE_PRODUCTION, IF_ELSE_IF_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == IF_PRODUCTION)
    {
      Expression expression = (Expression) args[1];
      Statement thenClause = (Statement) args[2];
      return new IfStatement(expression, thenClause, null, LexicalPhrase.combine((LexicalPhrase) args[0], expression.getLexicalPhrase(), thenClause.getLexicalPhrase()));
    }
    if (production == IF_ELSE_PRODUCTION || production == IF_ELSE_IF_PRODUCTION)
    {
      Expression expression = (Expression) args[1];
      Statement thenClause = (Statement) args[2];
      Statement elseClause = (Statement) args[4];
      return new IfStatement(expression, thenClause, elseClause, LexicalPhrase.combine((LexicalPhrase) args[0], expression.getLexicalPhrase(), thenClause.getLexicalPhrase(),
                                                                                       (LexicalPhrase) args[3], elseClause.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
