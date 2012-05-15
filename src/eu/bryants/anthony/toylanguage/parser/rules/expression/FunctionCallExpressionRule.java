package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 16 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class FunctionCallExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> FUNCTION_CALL_PRODUCTION = new Production<ParseType>(ParseType.NAME, ParseType.LPAREN, ParseType.EXPRESSION_LIST, ParseType.RPAREN);
  private static Production<ParseType> FUNCTION_CALL_NO_ARGUMENTS_PRODUCTION = new Production<ParseType>(ParseType.NAME, ParseType.LPAREN, ParseType.RPAREN);

  @SuppressWarnings("unchecked")
  public FunctionCallExpressionRule()
  {
    super(ParseType.FUNCTION_CALL_EXPRESSION, FUNCTION_CALL_PRODUCTION, FUNCTION_CALL_NO_ARGUMENTS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == FUNCTION_CALL_PRODUCTION)
    {
      Name name = (Name) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Expression> arguments = (ParseList<Expression>) args[2];
      return new FunctionCallExpression(name.getName(), arguments.toArray(new Expression[arguments.size()]), LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1],
                                                                                                                                   arguments.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == FUNCTION_CALL_NO_ARGUMENTS_PRODUCTION)
    {
      Name name = (Name) args[0];
      return new FunctionCallExpression(name.getName(), new Expression[0], LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], (LexicalPhrase) args[2]));
    }
    throw badTypeList();
  }

}