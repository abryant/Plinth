package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.Expression;
import eu.bryants.anthony.toylanguage.ast.Statement;
import eu.bryants.anthony.toylanguage.ast.WhileStatement;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class WhileStatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.WHILE_KEYWORD, ParseType.EXPRESSION, ParseType.BLOCK);

  @SuppressWarnings("unchecked")
  public WhileStatementRule()
  {
    super(ParseType.WHILE_STATEMENT, PRODUCTION);
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
      Statement statement = (Statement) args[2];
      return new WhileStatement(expression, statement, LexicalPhrase.combine((LexicalPhrase) args[0], expression.getLexicalPhrase(), statement.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
