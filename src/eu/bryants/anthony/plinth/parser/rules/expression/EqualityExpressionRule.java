package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.EqualityExpression;
import eu.bryants.anthony.plinth.ast.expression.EqualityExpression.EqualityOperator;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 25 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class EqualityExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.EXPRESSION_NOT_LESS_THAN_QNAME);
  private static final Production<ParseType> EQUAL_PRODUCTION                 = new Production<ParseType>(ParseType.EXPRESSION_NOT_LESS_THAN_QNAME, ParseType.DOUBLE_EQUALS,            ParseType.EXPRESSION_NOT_LESS_THAN_QNAME);
  private static final Production<ParseType> EQUAL_QNAME_PRODUCTION           = new Production<ParseType>(ParseType.EXPRESSION_NOT_LESS_THAN_QNAME, ParseType.DOUBLE_EQUALS,            ParseType.QNAME_OR_LESS_THAN_EXPRESSION);
  private static final Production<ParseType> QNAME_EQUAL_PRODUCTION           = new Production<ParseType>(ParseType.QNAME_OR_LESS_THAN_EXPRESSION,  ParseType.DOUBLE_EQUALS,            ParseType.EXPRESSION_NOT_LESS_THAN_QNAME);
  private static final Production<ParseType> QNAME_EQUAL_QNAME_PRODUCTION     = new Production<ParseType>(ParseType.QNAME_OR_LESS_THAN_EXPRESSION,  ParseType.DOUBLE_EQUALS,            ParseType.QNAME_OR_LESS_THAN_EXPRESSION);
  private static final Production<ParseType> NOT_EQUAL_PRODUCTION             = new Production<ParseType>(ParseType.EXPRESSION_NOT_LESS_THAN_QNAME, ParseType.EXCLAIMATION_MARK_EQUALS, ParseType.EXPRESSION_NOT_LESS_THAN_QNAME);
  private static final Production<ParseType> NOT_EQUAL_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.EXPRESSION_NOT_LESS_THAN_QNAME, ParseType.EXCLAIMATION_MARK_EQUALS, ParseType.QNAME_OR_LESS_THAN_EXPRESSION);
  private static final Production<ParseType> QNAME_NOT_EQUAL_PRODUCTION       = new Production<ParseType>(ParseType.QNAME_OR_LESS_THAN_EXPRESSION,  ParseType.EXCLAIMATION_MARK_EQUALS, ParseType.EXPRESSION_NOT_LESS_THAN_QNAME);
  private static final Production<ParseType> QNAME_NOT_EQUAL_QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_OR_LESS_THAN_EXPRESSION,  ParseType.EXCLAIMATION_MARK_EQUALS, ParseType.QNAME_OR_LESS_THAN_EXPRESSION);
  private static final Production<ParseType> IDENTICALLY_EQUAL_PRODUCTION                 = new Production<ParseType>(ParseType.EXPRESSION_NOT_LESS_THAN_QNAME, ParseType.TRIPLE_EQUALS,                   ParseType.EXPRESSION_NOT_LESS_THAN_QNAME);
  private static final Production<ParseType> IDENTICALLY_EQUAL_QNAME_PRODUCTION           = new Production<ParseType>(ParseType.EXPRESSION_NOT_LESS_THAN_QNAME, ParseType.TRIPLE_EQUALS,                   ParseType.QNAME_OR_LESS_THAN_EXPRESSION);
  private static final Production<ParseType> QNAME_IDENTICALLY_EQUAL_PRODUCTION           = new Production<ParseType>(ParseType.QNAME_OR_LESS_THAN_EXPRESSION,  ParseType.TRIPLE_EQUALS,                   ParseType.EXPRESSION_NOT_LESS_THAN_QNAME);
  private static final Production<ParseType> QNAME_IDENTICALLY_EQUAL_QNAME_PRODUCTION     = new Production<ParseType>(ParseType.QNAME_OR_LESS_THAN_EXPRESSION,  ParseType.TRIPLE_EQUALS,                   ParseType.QNAME_OR_LESS_THAN_EXPRESSION);
  private static final Production<ParseType> NOT_IDENTICALLY_EQUAL_PRODUCTION             = new Production<ParseType>(ParseType.EXPRESSION_NOT_LESS_THAN_QNAME, ParseType.EXCLAIMATION_MARK_DOUBLE_EQUALS, ParseType.EXPRESSION_NOT_LESS_THAN_QNAME);
  private static final Production<ParseType> NOT_IDENTICALLY_EQUAL_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.EXPRESSION_NOT_LESS_THAN_QNAME, ParseType.EXCLAIMATION_MARK_DOUBLE_EQUALS, ParseType.QNAME_OR_LESS_THAN_EXPRESSION);
  private static final Production<ParseType> QNAME_NOT_IDENTICALLY_EQUAL_PRODUCTION       = new Production<ParseType>(ParseType.QNAME_OR_LESS_THAN_EXPRESSION,  ParseType.EXCLAIMATION_MARK_DOUBLE_EQUALS, ParseType.EXPRESSION_NOT_LESS_THAN_QNAME);
  private static final Production<ParseType> QNAME_NOT_IDENTICALLY_EQUAL_QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_OR_LESS_THAN_EXPRESSION,  ParseType.EXCLAIMATION_MARK_DOUBLE_EQUALS, ParseType.QNAME_OR_LESS_THAN_EXPRESSION);

  public EqualityExpressionRule()
  {
    super(ParseType.EQUALITY_EXPRESSION, PRODUCTION,
                                         EQUAL_PRODUCTION,     EQUAL_QNAME_PRODUCTION,     QNAME_EQUAL_PRODUCTION,     QNAME_EQUAL_QNAME_PRODUCTION,
                                         NOT_EQUAL_PRODUCTION, NOT_EQUAL_QNAME_PRODUCTION, QNAME_NOT_EQUAL_PRODUCTION, QNAME_NOT_EQUAL_QNAME_PRODUCTION,
                                         IDENTICALLY_EQUAL_PRODUCTION,     IDENTICALLY_EQUAL_QNAME_PRODUCTION,     QNAME_IDENTICALLY_EQUAL_PRODUCTION,     QNAME_IDENTICALLY_EQUAL_QNAME_PRODUCTION,
                                         NOT_IDENTICALLY_EQUAL_PRODUCTION, NOT_IDENTICALLY_EQUAL_QNAME_PRODUCTION, QNAME_NOT_IDENTICALLY_EQUAL_PRODUCTION, QNAME_NOT_IDENTICALLY_EQUAL_QNAME_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      return args[0];
    }
    if (production == EQUAL_PRODUCTION       || production == EQUAL_QNAME_PRODUCTION ||
        production == QNAME_EQUAL_PRODUCTION || production == QNAME_EQUAL_QNAME_PRODUCTION)
    {
      Expression left = (Expression) args[0];
      Expression right = (Expression) args[2];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase());
      return new EqualityExpression(left, right, EqualityOperator.EQUAL, lexicalPhrase);
    }
    if (production == NOT_EQUAL_PRODUCTION       || production == NOT_EQUAL_QNAME_PRODUCTION ||
        production == QNAME_NOT_EQUAL_PRODUCTION || production == QNAME_NOT_EQUAL_QNAME_PRODUCTION)
    {
      Expression left = (Expression) args[0];
      Expression right = (Expression) args[2];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase());
      return new EqualityExpression(left, right, EqualityOperator.NOT_EQUAL, lexicalPhrase);
    }
    if (production == IDENTICALLY_EQUAL_PRODUCTION       || production == IDENTICALLY_EQUAL_QNAME_PRODUCTION ||
        production == QNAME_IDENTICALLY_EQUAL_PRODUCTION || production == QNAME_IDENTICALLY_EQUAL_QNAME_PRODUCTION)
    {
      Expression left = (Expression) args[0];
      Expression right = (Expression) args[2];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase());
      return new EqualityExpression(left, right, EqualityOperator.IDENTICALLY_EQUAL, lexicalPhrase);
    }
    if (production == NOT_IDENTICALLY_EQUAL_PRODUCTION       || production == NOT_IDENTICALLY_EQUAL_QNAME_PRODUCTION ||
        production == QNAME_NOT_IDENTICALLY_EQUAL_PRODUCTION || production == QNAME_NOT_IDENTICALLY_EQUAL_QNAME_PRODUCTION)
    {
      Expression left = (Expression) args[0];
      Expression right = (Expression) args[2];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase());
      return new EqualityExpression(left, right, EqualityOperator.NOT_IDENTICALLY_EQUAL, lexicalPhrase);
    }
    throw badTypeList();
  }

}
