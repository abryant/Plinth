package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.LogicalExpression;
import eu.bryants.anthony.toylanguage.ast.expression.LogicalExpression.LogicalOperator;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 12 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class LogicalExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION                         = new Production<ParseType>(ParseType.COMPARISON_EXPRESSION);
  private static final Production<ParseType> AND_PRODUCTION                           = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.AMPERSAND,        ParseType.COMPARISON_EXPRESSION);
  private static final Production<ParseType> AND_QNAME_PRODUCTION                     = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.AMPERSAND,        ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_AND_PRODUCTION                     = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.AMPERSAND,        ParseType.COMPARISON_EXPRESSION);
  private static final Production<ParseType> QNAME_AND_QNAME_PRODUCTION               = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.AMPERSAND,        ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> OR_PRODUCTION                            = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.PIPE,             ParseType.COMPARISON_EXPRESSION);
  private static final Production<ParseType> OR_QNAME_PRODUCTION                      = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.PIPE,             ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_OR_PRODUCTION                      = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.PIPE,             ParseType.COMPARISON_EXPRESSION);
  private static final Production<ParseType> QNAME_OR_QNAME_PRODUCTION                = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.PIPE,             ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> XOR_PRODUCTION                           = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.CARET,            ParseType.COMPARISON_EXPRESSION);
  private static final Production<ParseType> XOR_QNAME_PRODUCTION                     = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.CARET,            ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_XOR_PRODUCTION                     = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.CARET,            ParseType.COMPARISON_EXPRESSION);
  private static final Production<ParseType> QNAME_XOR_QNAME_PRODUCTION               = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.CARET,            ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> SHORT_CIRCUIT_AND_PRODUCTION             = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.DOUBLE_AMPERSAND, ParseType.COMPARISON_EXPRESSION);
  private static final Production<ParseType> SHORT_CIRCUIT_AND_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.DOUBLE_AMPERSAND, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_SHORT_CIRCUIT_AND_PRODUCTION       = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.DOUBLE_AMPERSAND, ParseType.COMPARISON_EXPRESSION);
  private static final Production<ParseType> QNAME_SHORT_CIRCUIT_AND_QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.DOUBLE_AMPERSAND, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> SHORT_CIRCUIT_OR_PRODUCTION              = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.DOUBLE_PIPE,      ParseType.COMPARISON_EXPRESSION);
  private static final Production<ParseType> SHORT_CIRCUIT_OR_QNAME_PRODUCTION        = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.DOUBLE_PIPE,      ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_SHORT_CIRCUIT_OR_PRODUCTION        = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.DOUBLE_PIPE,      ParseType.COMPARISON_EXPRESSION);
  private static final Production<ParseType> QNAME_SHORT_CIRCUIT_OR_QNAME_PRODUCTION  = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.DOUBLE_PIPE,      ParseType.QNAME_EXPRESSION);

  @SuppressWarnings("unchecked")
  public LogicalExpressionRule()
  {
    super(ParseType.LOGICAL_EXPRESSION, START_PRODUCTION,
                                        AND_PRODUCTION,               AND_QNAME_PRODUCTION,               QNAME_AND_PRODUCTION,               QNAME_AND_QNAME_PRODUCTION,
                                        OR_PRODUCTION,                OR_QNAME_PRODUCTION,                QNAME_OR_PRODUCTION,                QNAME_OR_QNAME_PRODUCTION,
                                        XOR_PRODUCTION,               XOR_QNAME_PRODUCTION,               QNAME_XOR_PRODUCTION,               QNAME_XOR_QNAME_PRODUCTION,
                                        SHORT_CIRCUIT_AND_PRODUCTION, SHORT_CIRCUIT_AND_QNAME_PRODUCTION, QNAME_SHORT_CIRCUIT_AND_PRODUCTION, QNAME_SHORT_CIRCUIT_AND_QNAME_PRODUCTION,
                                        SHORT_CIRCUIT_OR_PRODUCTION,  SHORT_CIRCUIT_OR_QNAME_PRODUCTION,  QNAME_SHORT_CIRCUIT_OR_PRODUCTION,  QNAME_SHORT_CIRCUIT_OR_QNAME_PRODUCTION);
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
    LogicalOperator operator;
    if (production == AND_PRODUCTION       || production == AND_QNAME_PRODUCTION ||
        production == QNAME_AND_PRODUCTION || production == QNAME_AND_QNAME_PRODUCTION)
    {
      operator = LogicalOperator.AND;
    }
    else if (production == OR_PRODUCTION       || production == OR_QNAME_PRODUCTION ||
             production == QNAME_OR_PRODUCTION || production == QNAME_OR_QNAME_PRODUCTION)
    {
      operator = LogicalOperator.OR;
    }
    else if (production == XOR_PRODUCTION       || production == XOR_QNAME_PRODUCTION ||
             production == QNAME_XOR_PRODUCTION || production == QNAME_XOR_QNAME_PRODUCTION)
    {
      operator = LogicalOperator.XOR;
    }
    else if (production == SHORT_CIRCUIT_AND_PRODUCTION       || production == SHORT_CIRCUIT_AND_QNAME_PRODUCTION ||
             production == QNAME_SHORT_CIRCUIT_AND_PRODUCTION || production == QNAME_SHORT_CIRCUIT_AND_QNAME_PRODUCTION)
    {
      operator = LogicalOperator.SHORT_CIRCUIT_AND;
    }
    else if (production == SHORT_CIRCUIT_OR_PRODUCTION       || production == SHORT_CIRCUIT_OR_QNAME_PRODUCTION ||
             production == QNAME_SHORT_CIRCUIT_OR_PRODUCTION || production == QNAME_SHORT_CIRCUIT_OR_QNAME_PRODUCTION)
    {
      operator = LogicalOperator.SHORT_CIRCUIT_OR;
    }
    else
    {
      throw badTypeList();
    }
    Expression left = (Expression) args[0];
    Expression right = (Expression) args[2];
    return new LogicalExpression(operator, left, right,
                                 LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase()));
  }

}
