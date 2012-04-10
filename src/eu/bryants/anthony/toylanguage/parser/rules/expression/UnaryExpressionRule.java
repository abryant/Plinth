package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.expression.CastExpression;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
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

  private static final Production<ParseType> PRIMARY_PRODUCTION = new Production<ParseType>(ParseType.PRIMARY);
  private static final Production<ParseType> CAST_PRODUCTION = new Production<ParseType>(ParseType.CAST_KEYWORD, ParseType.LANGLE, ParseType.TYPE, ParseType.RANGLE, ParseType.UNARY_EXPRESSION);

  @SuppressWarnings("unchecked")
  public UnaryExpressionRule()
  {
    super(ParseType.UNARY_EXPRESSION, PRIMARY_PRODUCTION, CAST_PRODUCTION);
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
    if (production == CAST_PRODUCTION)
    {
      Type type = (Type) args[2];
      Expression expression = (Expression) args[4];
      return new CastExpression(type, expression, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], type.getLexicalPhrase(), (LexicalPhrase) args[3], expression.getLexicalPhrase()));
    }
    throw badTypeList();
  }
}
