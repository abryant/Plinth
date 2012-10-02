package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleIndexExpression;
import eu.bryants.anthony.toylanguage.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 5 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class TupleIndexExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION        = new Production<ParseType>(ParseType.UNARY_EXPRESSION);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.TUPLE_INDEX_EXPRESSION, ParseType.EXCLAIMATION_MARK, ParseType.INTEGER_LITERAL);
  private static final Production<ParseType> QNAME_PRODUCTION        = new Production<ParseType>(ParseType.QNAME_EXPRESSION,       ParseType.EXCLAIMATION_MARK, ParseType.INTEGER_LITERAL);

  @SuppressWarnings("unchecked")
  public TupleIndexExpressionRule()
  {
    super(ParseType.TUPLE_INDEX_EXPRESSION, START_PRODUCTION, CONTINUATION_PRODUCTION, QNAME_PRODUCTION);
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
    if (production == CONTINUATION_PRODUCTION || production == QNAME_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      IntegerLiteral literal = (IntegerLiteral) args[2];
      return new TupleIndexExpression(expression, literal, LexicalPhrase.combine(expression.getLexicalPhrase(), (LexicalPhrase) args[1], literal.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
