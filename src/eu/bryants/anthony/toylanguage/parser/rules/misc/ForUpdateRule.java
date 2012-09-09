package eu.bryants.anthony.toylanguage.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ExpressionStatement;
import eu.bryants.anthony.toylanguage.ast.statement.PrefixIncDecStatement;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;

/*
 * Created on 18 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ForUpdateRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> INCREMENT_PRODUCTION = new Production<ParseType>(ParseType.DOUBLE_PLUS, ParseType.ASSIGNEE);
  private static final Production<ParseType> DECREMENT_PRODUCTION = new Production<ParseType>(ParseType.DOUBLE_MINUS, ParseType.ASSIGNEE);
  private static final Production<ParseType> ASSIGNMENT_PRODUCTION = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.EQUALS, ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> SHORTHAND_ASSIGN_PRODUCTION = new Production<ParseType>(ParseType.SHORTHAND_ASSIGNMENT);
  private static final Production<ParseType> FUNCTION_CALL_PRODUCTION = new Production<ParseType>(ParseType.FUNCTION_CALL_EXPRESSION);
  private static final Production<ParseType> EMPTY_PRODUCTION = new Production<ParseType>();

  @SuppressWarnings("unchecked")
  public ForUpdateRule()
  {
    super(ParseType.FOR_UPDATE, INCREMENT_PRODUCTION, DECREMENT_PRODUCTION, ASSIGNMENT_PRODUCTION, SHORTHAND_ASSIGN_PRODUCTION, FUNCTION_CALL_PRODUCTION, EMPTY_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == INCREMENT_PRODUCTION)
    {
      Assignee assignee = (Assignee) args[1];
      return new PrefixIncDecStatement(assignee, true, LexicalPhrase.combine((LexicalPhrase) args[0], assignee.getLexicalPhrase()));
    }
    if (production == DECREMENT_PRODUCTION)
    {
      Assignee assignee = (Assignee) args[1];
      return new PrefixIncDecStatement(assignee, false, LexicalPhrase.combine((LexicalPhrase) args[0], assignee.getLexicalPhrase()));
    }
    if (production == ASSIGNMENT_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Assignee> assignees = (ParseList<Assignee>) args[0];
      Expression expression = (Expression) args[2];
      return new AssignStatement(false, null, assignees.toArray(new Assignee[assignees.size()]), expression, LexicalPhrase.combine(assignees.getLexicalPhrase(), (LexicalPhrase) args[1], expression.getLexicalPhrase()));
    }
    if (production == SHORTHAND_ASSIGN_PRODUCTION)
    {
      return args[0];
    }
    if (production == FUNCTION_CALL_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      return new ExpressionStatement(expression, expression.getLexicalPhrase());
    }
    if (production == EMPTY_PRODUCTION)
    {
      return null;
    }
    throw badTypeList();
  }

}
