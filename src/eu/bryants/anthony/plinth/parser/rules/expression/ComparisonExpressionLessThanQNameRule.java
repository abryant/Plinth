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
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 25 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class ComparisonExpressionLessThanQNameRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> QNAME_QNAME_PRODUCTION   = new Production<ParseType>(ParseType.QNAME,             ParseType.LANGLE, ParseType.QNAME);
  private static final Production<ParseType> QNAME_NESTED_PRODUCTION  = new Production<ParseType>(ParseType.QNAME,             ParseType.LANGLE, ParseType.NESTED_QNAME_LIST);
  private static final Production<ParseType> NESTED_QNAME_PRODUCTION  = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.LANGLE, ParseType.QNAME);
  private static final Production<ParseType> NESTED_NESTED_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.LANGLE, ParseType.NESTED_QNAME_LIST);

  public ComparisonExpressionLessThanQNameRule()
  {
    super(ParseType.COMPARISON_EXPRESSION_LESS_THAN_QNAME, QNAME_QNAME_PRODUCTION, QNAME_NESTED_PRODUCTION, NESTED_QNAME_PRODUCTION, NESTED_NESTED_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    QNameElement firstElement;
    if (production == QNAME_QNAME_PRODUCTION || production == QNAME_NESTED_PRODUCTION)
    {
      QName first = (QName) args[0];
      firstElement = new QNameElement(first, first.getLexicalPhrase());
    }
    else if (production == NESTED_QNAME_PRODUCTION || production == NESTED_NESTED_PRODUCTION)
    {
      firstElement = (QNameElement) args[0];
    }
    else
    {
      throw badTypeList();
    }
    QNameElement secondElement;
    if (production == QNAME_QNAME_PRODUCTION || production == NESTED_QNAME_PRODUCTION)
    {
      QName second = (QName) args[2];
      secondElement = new QNameElement(second, second.getLexicalPhrase());
    }
    else if (production == QNAME_NESTED_PRODUCTION || production == NESTED_NESTED_PRODUCTION)
    {
      secondElement = (QNameElement) args[2];
    }
    else
    {
      throw badTypeList();
    }
    Expression left = firstElement.convertToExpression();
    Expression right = secondElement.convertToExpression();
    return new RelationalExpression(left, right, RelationalOperator.LESS_THAN, LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase()));
  }

}
