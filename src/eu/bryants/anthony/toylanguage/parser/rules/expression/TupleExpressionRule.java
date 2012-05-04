package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleExpression;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 4 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class TupleExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.EXPRESSION);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.TUPLE_EXPRESSION, ParseType.COMMA, ParseType.EXPRESSION);

  @SuppressWarnings("unchecked")
  public TupleExpressionRule()
  {
    super(ParseType.TUPLE_EXPRESSION, START_PRODUCTION, CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      return args[0];
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      Expression firstExpression = (Expression) args[0];
      Expression secondExpression = (Expression) args[2];
      if (firstExpression instanceof TupleExpression)
      {
        Expression[] oldSubExpressions = ((TupleExpression) firstExpression).getSubExpressions();
        Expression[] newSubExpressions = new Expression[oldSubExpressions.length + 1];
        System.arraycopy(oldSubExpressions, 0, newSubExpressions, 0, oldSubExpressions.length);
        newSubExpressions[oldSubExpressions.length] = secondExpression;
        return new TupleExpression(newSubExpressions, LexicalPhrase.combine(firstExpression.getLexicalPhrase(), (LexicalPhrase) args[1], secondExpression.getLexicalPhrase()));
      }
      return new TupleExpression(new Expression[] {firstExpression, secondExpression}, LexicalPhrase.combine(firstExpression.getLexicalPhrase(), (LexicalPhrase) args[1], secondExpression.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
