package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.misc.QName;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.QNameElement;

/*
 * Created on 30 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class QNameExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME);
  private static final Production<ParseType> NESTED_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST);

  @SuppressWarnings("unchecked")
  public QNameExpressionRule()
  {
    super(ParseType.QNAME_EXPRESSION, QNAME_PRODUCTION, NESTED_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == QNAME_PRODUCTION)
    {
      QName qname = (QName) args[0];
      return new QNameElement(qname, qname.getLexicalPhrase()).convertToExpression();
    }
    if (production == NESTED_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      return element.convertToExpression();
    }
    throw badTypeList();
  }

}
