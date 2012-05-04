package eu.bryants.anthony.toylanguage.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.statement.ArrayAssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class AssignStatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.NAME, ParseType.EQUALS, ParseType.TUPLE_EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> ARRAY_PRODUCTION = new Production<ParseType>(ParseType.PRIMARY, ParseType.LSQUARE, ParseType.TUPLE_EXPRESSION, ParseType.RSQUARE, ParseType.EQUALS, ParseType.TUPLE_EXPRESSION, ParseType.SEMICOLON);

  @SuppressWarnings("unchecked")
  public AssignStatementRule()
  {
    super(ParseType.ASSIGN_STATEMENT, PRODUCTION, ARRAY_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      Name name = (Name) args[0];
      Expression expression = (Expression) args[2];
      return new AssignStatement(name, expression,
                                 LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], expression.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == ARRAY_PRODUCTION)
    {
      Expression arrayExpression = (Expression) args[0];
      Expression dimensionExpression = (Expression) args[2];
      Expression valueExpression = (Expression) args[5];
      return new ArrayAssignStatement(arrayExpression, dimensionExpression, valueExpression,
                                      LexicalPhrase.combine(arrayExpression.getLexicalPhrase(), (LexicalPhrase) args[1], dimensionExpression.getLexicalPhrase(), (LexicalPhrase) args[3], (LexicalPhrase) args[4], valueExpression.getLexicalPhrase(), (LexicalPhrase) args[6]));
    }
    throw badTypeList();
  }

}
