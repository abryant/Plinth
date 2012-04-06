package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.Expression;
import eu.bryants.anthony.toylanguage.ast.ReturnStatement;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

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

  @SuppressWarnings("unchecked")
  public ReturnStatementRule()
  {
    super(ParseType.RETURN_STATEMENT, PRODUCTION);
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
    throw badTypeList();
  }

}
