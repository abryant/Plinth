package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.EqualityExpression;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression;
import eu.bryants.anthony.plinth.ast.expression.EqualityExpression.EqualityOperator;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression.RelationalOperator;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ComparisonExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> NORMAL_PRODUCTION                      = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION);
  private static Production<ParseType> EQUAL_PRODUCTION                       = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.DOUBLE_EQUALS,            ParseType.ADDITIVE_EXPRESSION);
  private static Production<ParseType> EQUAL_QNAME_PRODUCTION                 = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.DOUBLE_EQUALS,            ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> QNAME_EQUAL_PRODUCTION                 = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.DOUBLE_EQUALS,            ParseType.ADDITIVE_EXPRESSION);
  private static Production<ParseType> QNAME_EQUAL_QNAME_PRODUCTION           = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.DOUBLE_EQUALS,            ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> NOT_EQUAL_PRODUCTION                   = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.EXCLAIMATION_MARK_EQUALS, ParseType.ADDITIVE_EXPRESSION);
  private static Production<ParseType> NOT_EQUAL_QNAME_PRODUCTION             = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.EXCLAIMATION_MARK_EQUALS, ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> QNAME_NOT_EQUAL_PRODUCTION             = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.EXCLAIMATION_MARK_EQUALS, ParseType.ADDITIVE_EXPRESSION);
  private static Production<ParseType> QNAME_NOT_EQUAL_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.EXCLAIMATION_MARK_EQUALS, ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> LESS_THAN_PRODUCTION                   = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.LANGLE,                   ParseType.ADDITIVE_EXPRESSION);
  private static Production<ParseType> LESS_THAN_QNAME_PRODUCTION             = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.LANGLE,                   ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> QNAME_LESS_THAN_PRODUCTION             = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.LANGLE,                   ParseType.ADDITIVE_EXPRESSION);
  private static Production<ParseType> QNAME_LESS_THAN_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.LANGLE,                   ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> LESS_THAN_EQUAL_PRODUCTION             = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.LANGLE_EQUALS,            ParseType.ADDITIVE_EXPRESSION);
  private static Production<ParseType> LESS_THAN_EQUAL_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.LANGLE_EQUALS,            ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> QNAME_LESS_THAN_EQUAL_PRODUCTION       = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.LANGLE_EQUALS,            ParseType.ADDITIVE_EXPRESSION);
  private static Production<ParseType> QNAME_LESS_THAN_EQUAL_QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.LANGLE_EQUALS,            ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> MORE_THAN_PRODUCTION                   = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.RANGLE,                   ParseType.ADDITIVE_EXPRESSION);
  private static Production<ParseType> MORE_THAN_QNAME_PRODUCTION             = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.RANGLE,                   ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> QNAME_MORE_THAN_PRODUCTION             = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.RANGLE,                   ParseType.ADDITIVE_EXPRESSION);
  private static Production<ParseType> QNAME_MORE_THAN_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.RANGLE,                   ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> MORE_THAN_EQUAL_PRODUCTION             = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.RANGLE_EQUALS,            ParseType.ADDITIVE_EXPRESSION);
  private static Production<ParseType> MORE_THAN_EQUAL_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.RANGLE_EQUALS,            ParseType.QNAME_EXPRESSION);
  private static Production<ParseType> QNAME_MORE_THAN_EQUAL_PRODUCTION       = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.RANGLE_EQUALS,            ParseType.ADDITIVE_EXPRESSION);
  private static Production<ParseType> QNAME_MORE_THAN_EQUAL_QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.RANGLE_EQUALS,            ParseType.QNAME_EXPRESSION);

  @SuppressWarnings("unchecked")
  public ComparisonExpressionRule()
  {
    super(ParseType.COMPARISON_EXPRESSION, NORMAL_PRODUCTION,
                                           EQUAL_PRODUCTION,           EQUAL_QNAME_PRODUCTION,           QNAME_EQUAL_PRODUCTION,           QNAME_EQUAL_QNAME_PRODUCTION,
                                           NOT_EQUAL_PRODUCTION,       NOT_EQUAL_QNAME_PRODUCTION,       QNAME_NOT_EQUAL_PRODUCTION,       QNAME_NOT_EQUAL_QNAME_PRODUCTION,
                                           LESS_THAN_PRODUCTION,       LESS_THAN_QNAME_PRODUCTION,       QNAME_LESS_THAN_PRODUCTION,       QNAME_LESS_THAN_QNAME_PRODUCTION,
                                           LESS_THAN_EQUAL_PRODUCTION, LESS_THAN_EQUAL_QNAME_PRODUCTION, QNAME_LESS_THAN_EQUAL_PRODUCTION, QNAME_LESS_THAN_EQUAL_QNAME_PRODUCTION,
                                           MORE_THAN_PRODUCTION,       MORE_THAN_QNAME_PRODUCTION,       QNAME_MORE_THAN_PRODUCTION,       QNAME_MORE_THAN_QNAME_PRODUCTION,
                                           MORE_THAN_EQUAL_PRODUCTION, MORE_THAN_EQUAL_QNAME_PRODUCTION, QNAME_MORE_THAN_EQUAL_PRODUCTION, QNAME_MORE_THAN_EQUAL_QNAME_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == NORMAL_PRODUCTION)
    {
      return args[0];
    }
    if (production == EQUAL_PRODUCTION || production == EQUAL_QNAME_PRODUCTION ||
        production == QNAME_EQUAL_PRODUCTION || production == QNAME_EQUAL_QNAME_PRODUCTION)
    {
      Expression left = (Expression) args[0];
      Expression right = (Expression) args[2];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase());
      return new EqualityExpression(left, right, EqualityOperator.EQUAL, lexicalPhrase);
    }
    if (production == NOT_EQUAL_PRODUCTION || production == NOT_EQUAL_QNAME_PRODUCTION ||
        production == QNAME_NOT_EQUAL_PRODUCTION || production == QNAME_NOT_EQUAL_QNAME_PRODUCTION)
    {
      Expression left = (Expression) args[0];
      Expression right = (Expression) args[2];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase());
      return new EqualityExpression(left, right, EqualityOperator.NOT_EQUAL, lexicalPhrase);
    }
    RelationalOperator operator;
    if (production == LESS_THAN_PRODUCTION       || production == LESS_THAN_QNAME_PRODUCTION ||
        production == QNAME_LESS_THAN_PRODUCTION || production == QNAME_LESS_THAN_QNAME_PRODUCTION)
    {
      operator = RelationalOperator.LESS_THAN;
    }
    else if (production == LESS_THAN_EQUAL_PRODUCTION       || production == LESS_THAN_EQUAL_QNAME_PRODUCTION ||
             production == QNAME_LESS_THAN_EQUAL_PRODUCTION || production == QNAME_LESS_THAN_EQUAL_QNAME_PRODUCTION)
    {
      operator = RelationalOperator.LESS_THAN_EQUAL;
    }
    else if (production == MORE_THAN_PRODUCTION       || production == MORE_THAN_QNAME_PRODUCTION ||
             production == QNAME_MORE_THAN_PRODUCTION || production == QNAME_MORE_THAN_QNAME_PRODUCTION)
    {
      operator = RelationalOperator.MORE_THAN;
    }
    else if (production == MORE_THAN_EQUAL_PRODUCTION       || production == MORE_THAN_EQUAL_QNAME_PRODUCTION ||
             production == QNAME_MORE_THAN_EQUAL_PRODUCTION || production == QNAME_MORE_THAN_EQUAL_QNAME_PRODUCTION)
    {
      operator = RelationalOperator.MORE_THAN_EQUAL;
    }
    else
    {
      throw badTypeList();
    }
    Expression left = (Expression) args[0];
    Expression right = (Expression) args[2];
    LexicalPhrase lexicalPhrase = LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase());
    return new RelationalExpression(left, right, operator, lexicalPhrase);
  }

}
