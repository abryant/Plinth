package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.BitwiseNotExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BooleanNotExpression;
import eu.bryants.anthony.toylanguage.ast.expression.CastExpression;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.MinusExpression;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 10 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class UnaryExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRIMARY_PRODUCTION           = new Production<ParseType>(ParseType.PRIMARY);
  private static final Production<ParseType> CAST_PRODUCTION              = new Production<ParseType>(ParseType.CAST_KEYWORD, ParseType.LANGLE, ParseType.TYPE, ParseType.RANGLE, ParseType.UNARY_EXPRESSION);
  private static final Production<ParseType> CAST_QNAME_PRODUCTION        = new Production<ParseType>(ParseType.CAST_KEYWORD, ParseType.LANGLE, ParseType.TYPE, ParseType.RANGLE, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> MINUS_PRODUCTION             = new Production<ParseType>(ParseType.MINUS, ParseType.UNARY_EXPRESSION);
  private static final Production<ParseType> MINUS_QNAME_PRODUCTION       = new Production<ParseType>(ParseType.MINUS, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> BOOLEAN_NOT_PRODUCTION       = new Production<ParseType>(ParseType.EXCLAIMATION_MARK, ParseType.UNARY_EXPRESSION);
  private static final Production<ParseType> BOOLEAN_NOT_QNAME_PRODUCTION = new Production<ParseType>(ParseType.EXCLAIMATION_MARK, ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> BITWISE_NOT_PRODUCTION       = new Production<ParseType>(ParseType.TILDE, ParseType.UNARY_EXPRESSION);
  private static final Production<ParseType> BITWISE_NOT_QNAME_PRODUCTION = new Production<ParseType>(ParseType.TILDE, ParseType.QNAME_EXPRESSION);

  @SuppressWarnings("unchecked")
  public UnaryExpressionRule()
  {
    super(ParseType.UNARY_EXPRESSION, PRIMARY_PRODUCTION,
                                      CAST_PRODUCTION, CAST_QNAME_PRODUCTION,
                                      MINUS_PRODUCTION, MINUS_QNAME_PRODUCTION,
                                      BOOLEAN_NOT_PRODUCTION, BOOLEAN_NOT_QNAME_PRODUCTION,
                                      BITWISE_NOT_PRODUCTION, BITWISE_NOT_QNAME_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRIMARY_PRODUCTION)
    {
      return args[0];
    }
    if (production == CAST_PRODUCTION || production == CAST_QNAME_PRODUCTION)
    {
      Type type = (Type) args[2];
      Expression expression = (Expression) args[4];
      return new CastExpression(type, expression, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], type.getLexicalPhrase(), (LexicalPhrase) args[3], expression.getLexicalPhrase()));
    }
    if (production == MINUS_PRODUCTION || production == MINUS_QNAME_PRODUCTION)
    {
      Expression expression = (Expression) args[1];
      return new MinusExpression(expression, LexicalPhrase.combine((LexicalPhrase) args[0], expression.getLexicalPhrase()));
    }
    if (production == BOOLEAN_NOT_PRODUCTION || production == BOOLEAN_NOT_QNAME_PRODUCTION)
    {
      Expression expression = (Expression) args[1];
      return new BooleanNotExpression(expression, LexicalPhrase.combine((LexicalPhrase) args[0], expression.getLexicalPhrase()));
    }
    if (production == BITWISE_NOT_PRODUCTION || production == BITWISE_NOT_QNAME_PRODUCTION)
    {
      Expression expression = (Expression) args[1];
      return new BitwiseNotExpression(expression, LexicalPhrase.combine((LexicalPhrase) args[0], expression.getLexicalPhrase()));
    }
    throw badTypeList();
  }
}
