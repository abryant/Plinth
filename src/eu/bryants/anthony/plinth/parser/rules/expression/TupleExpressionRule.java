package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression.RelationalOperator;
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

  private static final Production<ParseType> QNAME_PRODUCTION                   = new Production<ParseType>(ParseType.QNAME,                                                            ParseType.COMMA, ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> NESTED_QNAME_LIST_PRODUCTION       = new Production<ParseType>(ParseType.NESTED_QNAME_LIST,                                                ParseType.COMMA, ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> QNAME_LESS_THAN_QNAME_PRODUCTION   = new Production<ParseType>(ParseType.QNAME,             ParseType.LANGLE, ParseType.QNAME,             ParseType.COMMA, ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> QNAME_LESS_THAN_NESTED_PRODUCTION  = new Production<ParseType>(ParseType.QNAME,             ParseType.LANGLE, ParseType.NESTED_QNAME_LIST, ParseType.COMMA, ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> NESTED_LESS_THAN_QNAME_PRODUCTION  = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.LANGLE, ParseType.QNAME,             ParseType.COMMA, ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> NESTED_LESS_THAN_NESTED_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.LANGLE, ParseType.NESTED_QNAME_LIST, ParseType.COMMA, ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> EXPRESSION_PRODUCTION              = new Production<ParseType>(ParseType.CONDITIONAL_EXPRESSION,                                           ParseType.COMMA, ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> END_QNAME_LIST_PRODUCTION          = new Production<ParseType>(ParseType.CONDITIONAL_EXPRESSION,                                           ParseType.COMMA, ParseType.QNAME_LIST);
  private static final Production<ParseType> END_PRODUCTION                     = new Production<ParseType>(ParseType.CONDITIONAL_EXPRESSION);
  private static final Production<ParseType> END_LESS_THAN_PRODUCTION           = new Production<ParseType>(ParseType.COMPARISON_EXPRESSION_LESS_THAN_QNAME);

  public TupleExpressionRule()
  {
    super(ParseType.TUPLE_EXPRESSION, QNAME_PRODUCTION, NESTED_QNAME_LIST_PRODUCTION,
                                      QNAME_LESS_THAN_QNAME_PRODUCTION,  QNAME_LESS_THAN_NESTED_PRODUCTION,
                                      NESTED_LESS_THAN_QNAME_PRODUCTION, NESTED_LESS_THAN_NESTED_PRODUCTION,
                                      EXPRESSION_PRODUCTION,
                                      END_QNAME_LIST_PRODUCTION, END_PRODUCTION, END_LESS_THAN_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == END_PRODUCTION || production == END_LESS_THAN_PRODUCTION)
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
    else if (production == QNAME_LESS_THAN_QNAME_PRODUCTION  || production == QNAME_LESS_THAN_NESTED_PRODUCTION ||
             production == NESTED_LESS_THAN_QNAME_PRODUCTION || production == NESTED_LESS_THAN_NESTED_PRODUCTION)
    {
      QNameElement firstElement;
      if (production == QNAME_LESS_THAN_QNAME_PRODUCTION || production == QNAME_LESS_THAN_NESTED_PRODUCTION)
      {
        QName first = (QName) args[0];
        firstElement = new QNameElement(first, first.getLexicalPhrase());
      }
      else if (production == NESTED_LESS_THAN_QNAME_PRODUCTION || production == NESTED_LESS_THAN_NESTED_PRODUCTION)
      {
        firstElement = (QNameElement) args[0];
      }
      else
      {
        throw badTypeList();
      }
      QNameElement secondElement;
      if (production == QNAME_LESS_THAN_QNAME_PRODUCTION || production == NESTED_LESS_THAN_QNAME_PRODUCTION)
      {
        QName second = (QName) args[2];
        secondElement = new QNameElement(second, second.getLexicalPhrase());
      }
      else if (production == QNAME_LESS_THAN_NESTED_PRODUCTION || production == NESTED_LESS_THAN_NESTED_PRODUCTION)
      {
        secondElement = (QNameElement) args[2];
      }
      else
      {
        throw badTypeList();
      }
      Expression left = firstElement.convertToExpression();
      Expression right = secondElement.convertToExpression();
      firstExpression = new RelationalExpression(left, right, RelationalOperator.LESS_THAN, LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase()));
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
