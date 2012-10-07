package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.TupleExpression;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 30 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class ExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> TUPLE_PRODUCTION             = new Production<ParseType>(ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> QNAME_LIST_PRODUCTION        = new Production<ParseType>(ParseType.QNAME_LIST);

  @SuppressWarnings("unchecked")
  public ExpressionRule()
  {
    super(ParseType.EXPRESSION, TUPLE_PRODUCTION, QNAME_LIST_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == TUPLE_PRODUCTION)
    {
      return args[0];
    }
    if (production == QNAME_LIST_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<QNameElement> list = (ParseList<QNameElement>) args[0];
      QNameElement[] elements = list.toArray(new QNameElement[list.size()]);
      Expression[] expressions = new Expression[elements.length];
      for (int i = 0; i < elements.length; ++i)
      {
        expressions[i] = elements[i].convertToExpression();
      }
      if (expressions.length == 1)
      {
        return expressions[0];
      }
      return new TupleExpression(expressions, list.getLexicalPhrase());
    }
    throw badTypeList();
  }

}
