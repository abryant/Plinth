package eu.bryants.anthony.toylanguage.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.BlankAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.VariableAssignee;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 8 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class AssigneeRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> VARIABLE_PRODUCTION = new Production<ParseType>(ParseType.NAME);
  private static final Production<ParseType> ARRAY_PRODUCTION = new Production<ParseType>(ParseType.PRIMARY, ParseType.LSQUARE, ParseType.TUPLE_EXPRESSION, ParseType.RSQUARE);
  private static final Production<ParseType> UNDERSCORE_PRODUCTION = new Production<ParseType>(ParseType.UNDERSCORE);

  @SuppressWarnings("unchecked")
  public AssigneeRule()
  {
    super(ParseType.ASSIGNEE, VARIABLE_PRODUCTION, ARRAY_PRODUCTION, UNDERSCORE_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == VARIABLE_PRODUCTION)
    {
      Name name = (Name) args[0];
      return new VariableAssignee(name.getName(), name.getLexicalPhrase());
    }
    if (production == ARRAY_PRODUCTION)
    {
      Expression arrayExpression = (Expression) args[0];
      Expression dimensionExpression = (Expression) args[2];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(arrayExpression.getLexicalPhrase(), (LexicalPhrase) args[1], dimensionExpression.getLexicalPhrase(), (LexicalPhrase) args[3]);
      return new ArrayElementAssignee(arrayExpression, dimensionExpression, lexicalPhrase);
    }
    if (production == UNDERSCORE_PRODUCTION)
    {
      return new BlankAssignee((LexicalPhrase) args[0]);
    }
    throw badTypeList();
  }

}
