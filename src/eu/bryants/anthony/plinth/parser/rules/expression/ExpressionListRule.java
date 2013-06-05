package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression.RelationalOperator;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ExpressionListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> LIST_PRODUCTION                         = new Production<ParseType>(ParseType.CONDITIONAL_EXPRESSION,                                           ParseType.COMMA, ParseType.EXPRESSION_LIST);
  private static final Production<ParseType> LIST_QNAME_PRODUCTION                   = new Production<ParseType>(ParseType.QNAME,                                                            ParseType.COMMA, ParseType.EXPRESSION_LIST);
  private static final Production<ParseType> LIST_NESTED_PRODUCTION                  = new Production<ParseType>(ParseType.NESTED_QNAME_LIST,                                                ParseType.COMMA, ParseType.EXPRESSION_LIST);
  private static final Production<ParseType> LIST_QNAME_LESS_THAN_QNAME_PRODUCTION   = new Production<ParseType>(ParseType.QNAME,             ParseType.LANGLE, ParseType.QNAME,             ParseType.COMMA, ParseType.EXPRESSION_LIST);
  private static final Production<ParseType> LIST_QNAME_LESS_THAN_NESTED_PRODUCTION  = new Production<ParseType>(ParseType.QNAME,             ParseType.LANGLE, ParseType.NESTED_QNAME_LIST, ParseType.COMMA, ParseType.EXPRESSION_LIST);
  private static final Production<ParseType> LIST_NESTED_LESS_THAN_QNAME_PRODUCTION  = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.LANGLE, ParseType.QNAME,             ParseType.COMMA, ParseType.EXPRESSION_LIST);
  private static final Production<ParseType> LIST_NESTED_LESS_THAN_NESTED_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.LANGLE, ParseType.NESTED_QNAME_LIST, ParseType.COMMA, ParseType.EXPRESSION_LIST);
  private static final Production<ParseType> END_PRODUCTION                          = new Production<ParseType>(ParseType.CONDITIONAL_EXPRESSION);
  private static final Production<ParseType> END_QNAME_PRODUCTION                    = new Production<ParseType>(ParseType.QNAME_OR_LESS_THAN_EXPRESSION);

  public ExpressionListRule()
  {
    super(ParseType.EXPRESSION_LIST, LIST_PRODUCTION,
                                     LIST_QNAME_PRODUCTION, LIST_NESTED_PRODUCTION,
                                     LIST_QNAME_LESS_THAN_QNAME_PRODUCTION, LIST_QNAME_LESS_THAN_NESTED_PRODUCTION,
                                     LIST_NESTED_LESS_THAN_QNAME_PRODUCTION, LIST_NESTED_LESS_THAN_NESTED_PRODUCTION,
                                     END_PRODUCTION, END_QNAME_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == END_PRODUCTION || production == END_QNAME_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      return new ParseList<Expression>(expression, expression.getLexicalPhrase());
    }
    Expression firstExpression;
    LexicalPhrase commaLexicalPhrase;
    ParseList<Expression> list;
    if (production == LIST_PRODUCTION)
    {
      firstExpression = (Expression) args[0];
      commaLexicalPhrase = (LexicalPhrase) args[1];
      list = (ParseList<Expression>) args[2];
    }
    else if (production == LIST_QNAME_PRODUCTION)
    {
      QName qname = (QName) args[0];
      firstExpression = new QNameElement(qname, qname.getLexicalPhrase()).convertToExpression();
      commaLexicalPhrase = (LexicalPhrase) args[1];
      list = (ParseList<Expression>) args[2];
    }
    else if (production == LIST_NESTED_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      firstExpression = element.convertToExpression();
      commaLexicalPhrase = (LexicalPhrase) args[1];
      list = (ParseList<Expression>) args[2];
    }
    else if (production == LIST_QNAME_LESS_THAN_QNAME_PRODUCTION  || production == LIST_QNAME_LESS_THAN_NESTED_PRODUCTION ||
             production == LIST_NESTED_LESS_THAN_QNAME_PRODUCTION || production == LIST_NESTED_LESS_THAN_NESTED_PRODUCTION)
    {
      QNameElement firstElement;
      if (production == LIST_QNAME_LESS_THAN_QNAME_PRODUCTION || production == LIST_QNAME_LESS_THAN_NESTED_PRODUCTION)
      {
        QName first = (QName) args[0];
        firstElement = new QNameElement(first, first.getLexicalPhrase());
      }
      else if (production == LIST_NESTED_LESS_THAN_QNAME_PRODUCTION || production == LIST_NESTED_LESS_THAN_NESTED_PRODUCTION)
      {
        firstElement = (QNameElement) args[0];
      }
      else
      {
        throw badTypeList();
      }
      QNameElement secondElement;
      if (production == LIST_QNAME_LESS_THAN_QNAME_PRODUCTION || production == LIST_NESTED_LESS_THAN_QNAME_PRODUCTION)
      {
        QName second = (QName) args[2];
        secondElement = new QNameElement(second, second.getLexicalPhrase());
      }
      else if (production == LIST_QNAME_LESS_THAN_NESTED_PRODUCTION || production == LIST_NESTED_LESS_THAN_NESTED_PRODUCTION)
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
      commaLexicalPhrase = (LexicalPhrase) args[3];
      list = (ParseList<Expression>) args[4];
    }
    else
    {
      throw badTypeList();
    }
    list.addFirst(firstExpression, LexicalPhrase.combine(firstExpression.getLexicalPhrase(), commaLexicalPhrase, list.getLexicalPhrase()));
    return list;
  }

}
