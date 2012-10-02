package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.InlineIfExpression;
import eu.bryants.anthony.toylanguage.ast.expression.NullCoalescingExpression;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 7 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ExpressionNoTupleRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION);
  private static final Production<ParseType> INLINE_IF_PRODUCTION             = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.QUESTION_MARK, ParseType.EXPRESSION, ParseType.COLON, ParseType.EXPRESSION_NO_TUPLE);
  private static final Production<ParseType> INLINE_IF_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.QUESTION_MARK, ParseType.EXPRESSION, ParseType.COLON, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_INLINE_IF_PRODUCTION       = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.QUESTION_MARK, ParseType.EXPRESSION, ParseType.COLON, ParseType.EXPRESSION_NO_TUPLE);
  private static final Production<ParseType> QNAME_INLINE_IF_QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.QUESTION_MARK, ParseType.EXPRESSION, ParseType.COLON, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> NULL_COALESCING_PRODUCTION             = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.QUESTION_MARK_COLON, ParseType.EXPRESSION_NO_TUPLE);
  private static final Production<ParseType> NULL_COALESCING_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.LOGICAL_EXPRESSION, ParseType.QUESTION_MARK_COLON, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_NULL_COALESCING_PRODUCTION       = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.QUESTION_MARK_COLON, ParseType.EXPRESSION_NO_TUPLE);
  private static final Production<ParseType> QNAME_NULL_COALESCING_QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION,   ParseType.QUESTION_MARK_COLON, ParseType.QNAME_EXPRESSION);

  @SuppressWarnings("unchecked")
  public ExpressionNoTupleRule()
  {
    super(ParseType.EXPRESSION_NO_TUPLE, PRODUCTION,
                                         INLINE_IF_PRODUCTION, INLINE_IF_QNAME_PRODUCTION, QNAME_INLINE_IF_PRODUCTION, QNAME_INLINE_IF_QNAME_PRODUCTION,
                                         NULL_COALESCING_PRODUCTION, NULL_COALESCING_QNAME_PRODUCTION, QNAME_NULL_COALESCING_PRODUCTION, QNAME_NULL_COALESCING_QNAME_PRODUCTION);
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
    if (production == INLINE_IF_PRODUCTION || production == INLINE_IF_QNAME_PRODUCTION || production == QNAME_INLINE_IF_PRODUCTION || production == QNAME_INLINE_IF_QNAME_PRODUCTION)
    {
      Expression conditional = (Expression) args[0];
      Expression thenExpression = (Expression) args[2];
      Expression elseExpression = (Expression) args[4];
      return new InlineIfExpression(conditional, thenExpression, elseExpression, LexicalPhrase.combine(conditional.getLexicalPhrase(), (LexicalPhrase) args[1], thenExpression.getLexicalPhrase(), (LexicalPhrase) args[3], elseExpression.getLexicalPhrase()));
    }
    if (production == NULL_COALESCING_PRODUCTION || production == NULL_COALESCING_QNAME_PRODUCTION || production == QNAME_NULL_COALESCING_PRODUCTION || production == QNAME_NULL_COALESCING_QNAME_PRODUCTION)
    {
      Expression nullableExpression = (Expression) args[0];
      Expression alternativeExpression = (Expression) args[2];
      return new NullCoalescingExpression(nullableExpression, alternativeExpression, LexicalPhrase.combine(nullableExpression.getLexicalPhrase(), (LexicalPhrase) args[1], alternativeExpression.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
