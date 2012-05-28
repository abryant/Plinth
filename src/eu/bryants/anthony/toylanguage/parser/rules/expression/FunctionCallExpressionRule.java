package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;

/*
 * Created on 16 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class FunctionCallExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> FUNCTION_CALL_PRODUCTION              = new Production<ParseType>(ParseType.PRIMARY, ParseType.LPAREN, ParseType.EXPRESSION_LIST, ParseType.RPAREN);
  private static Production<ParseType> FUNCTION_CALL_NO_ARGUMENTS_PRODUCTION = new Production<ParseType>(ParseType.PRIMARY, ParseType.LPAREN,                            ParseType.RPAREN);

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
      Expression functionExpression = (Expression) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Expression> arguments = (ParseList<Expression>) args[2];
      return new FunctionCallExpression(functionExpression, arguments.toArray(new Expression[arguments.size()]),
                                        LexicalPhrase.combine(functionExpression.getLexicalPhrase(), (LexicalPhrase) args[1], arguments.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == FUNCTION_CALL_NO_ARGUMENTS_PRODUCTION)
    {
      Expression functionExpression = (Expression) args[0];
      return new FunctionCallExpression(functionExpression, new Expression[0],
                                        LexicalPhrase.combine(functionExpression.getLexicalPhrase(), (LexicalPhrase) args[1], (LexicalPhrase) args[2]));
    }
    throw badTypeList();
  }

}
