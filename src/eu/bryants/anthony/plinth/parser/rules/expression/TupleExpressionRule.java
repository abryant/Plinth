package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.TupleExpression;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 4 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class TupleExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> QNAME_PRODUCTION                 = new Production<ParseType>(ParseType.QNAME,               ParseType.COMMA, ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> NESTED_QNAME_LIST_PRODUCTION     = new Production<ParseType>(ParseType.NESTED_QNAME_LIST,   ParseType.COMMA, ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> EXPRESSION_PRODUCTION            = new Production<ParseType>(ParseType.EXPRESSION_NO_TUPLE, ParseType.COMMA, ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> END_QNAME_LIST_PRODUCTION        = new Production<ParseType>(ParseType.EXPRESSION_NO_TUPLE, ParseType.COMMA, ParseType.QNAME_LIST);
  private static final Production<ParseType> END_PRODUCTION                   = new Production<ParseType>(ParseType.EXPRESSION_NO_TUPLE);

  @SuppressWarnings("unchecked")
  public TupleExpressionRule()
  {
    super(ParseType.TUPLE_EXPRESSION, QNAME_PRODUCTION, NESTED_QNAME_LIST_PRODUCTION, EXPRESSION_PRODUCTION, END_QNAME_LIST_PRODUCTION, END_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == END_PRODUCTION)
    {
      return args[0];
    }
    if (production == END_QNAME_LIST_PRODUCTION)
    {
      Expression firstExpression = (Expression) args[0];
      @SuppressWarnings("unchecked")
      ParseList<QNameElement> list = (ParseList<QNameElement>) args[2];
      QNameElement[] elements = list.toArray(new QNameElement[list.size()]);
      Expression[] expressions = new Expression[1 + elements.length];
      expressions[0] = firstExpression;
      for (int i = 0; i < elements.length; ++i)
      {
        expressions[i + 1] = elements[i].convertToExpression();
      }
      return new TupleExpression(expressions, LexicalPhrase.combine(firstExpression.getLexicalPhrase(), (LexicalPhrase) args[1], list.getLexicalPhrase()));
    }

    Expression firstExpression;
    if (production == QNAME_PRODUCTION)
    {
      QName qname = (QName) args[0];
      firstExpression = new QNameElement(qname, qname.getLexicalPhrase()).convertToExpression();
    }
    else if (production == NESTED_QNAME_LIST_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      firstExpression = element.convertToExpression();
    }
    else if (production == EXPRESSION_PRODUCTION)
    {
      firstExpression = (Expression) args[0];
    }
    else
    {
      throw badTypeList();
    }

    Expression secondExpression = (Expression) args[2];
    if (secondExpression instanceof TupleExpression)
    {
      Expression[] oldSubExpressions = ((TupleExpression) secondExpression).getSubExpressions();
      Expression[] newSubExpressions = new Expression[1 + oldSubExpressions.length];
      newSubExpressions[0] = firstExpression;
      System.arraycopy(oldSubExpressions, 0, newSubExpressions, 1, oldSubExpressions.length);
      return new TupleExpression(newSubExpressions, LexicalPhrase.combine(firstExpression.getLexicalPhrase(), (LexicalPhrase) args[1], secondExpression.getLexicalPhrase()));
    }
    return new TupleExpression(new Expression[] {firstExpression, secondExpression}, LexicalPhrase.combine(firstExpression.getLexicalPhrase(), (LexicalPhrase) args[1], secondExpression.getLexicalPhrase()));
  }

}
