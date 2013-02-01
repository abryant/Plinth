package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.InstanceOfExpression;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 29 Jan 2013
 */

/**
 * @author Anthony Bryant
 */
public class InstanceOfExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> INSTANCEOF_PRODUCTION = new Production<ParseType>(ParseType.INSTANCEOF_EXPRESSION, ParseType.INSTANCEOF_KEYWORD, ParseType.TYPE);
  private static final Production<ParseType> QNAME_INSTANCEOF_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION, ParseType.INSTANCEOF_KEYWORD, ParseType.TYPE);

  public InstanceOfExpressionRule()
  {
    super(ParseType.INSTANCEOF_EXPRESSION, PRODUCTION, INSTANCEOF_PRODUCTION, QNAME_INSTANCEOF_PRODUCTION);
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
    if (production == INSTANCEOF_PRODUCTION || production == QNAME_INSTANCEOF_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      Type type = (Type) args[2];
      return new InstanceOfExpression(expression, type, LexicalPhrase.combine(expression.getLexicalPhrase(), (LexicalPhrase) args[1], type.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
