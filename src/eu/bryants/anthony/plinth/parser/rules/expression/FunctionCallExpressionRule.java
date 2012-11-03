package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 16 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class FunctionCallExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> FUNCTION_CALL_PRODUCTION       = new Production<ParseType>(ParseType.PRIMARY_NO_THIS,  ParseType.ARGUMENTS);
  private static Production<ParseType> QNAME_FUNCTION_CALL_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION, ParseType.ARGUMENTS);

  public FunctionCallExpressionRule()
  {
    super(ParseType.FUNCTION_CALL_EXPRESSION, FUNCTION_CALL_PRODUCTION, QNAME_FUNCTION_CALL_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == FUNCTION_CALL_PRODUCTION || production == QNAME_FUNCTION_CALL_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Expression> arguments = (ParseList<Expression>) args[1];
      return new FunctionCallExpression(expression, arguments.toArray(new Expression[arguments.size()]),
                                        LexicalPhrase.combine(expression.getLexicalPhrase(), arguments.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
