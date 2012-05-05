package eu.bryants.anthony.toylanguage.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.misc.VariableAssignee;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 5 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class AssigneeListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> VARIABLE_PRODUCTION = new Production<ParseType>(ParseType.NAME);
  private static final Production<ParseType> ARRAY_PRODUCTION = new Production<ParseType>(ParseType.PRIMARY, ParseType.LSQUARE, ParseType.TUPLE_EXPRESSION, ParseType.RSQUARE);
  private static final Production<ParseType> CONTINUATION_VARIABLE_PRODUCTION = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.COMMA, ParseType.NAME);
  private static final Production<ParseType> CONTINUATION_ARRAY_PRODUCTION = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.COMMA, ParseType.PRIMARY, ParseType.LSQUARE, ParseType.TUPLE_EXPRESSION, ParseType.RSQUARE);

  @SuppressWarnings("unchecked")
  public AssigneeListRule()
  {
    super(ParseType.ASSIGNEE_LIST, VARIABLE_PRODUCTION, ARRAY_PRODUCTION, CONTINUATION_VARIABLE_PRODUCTION, CONTINUATION_ARRAY_PRODUCTION);
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
      VariableAssignee assignee = new VariableAssignee(name.getName(), name.getLexicalPhrase());
      return new ParseList<Assignee>(assignee, assignee.getLexicalPhrase());
    }
    if (production == ARRAY_PRODUCTION)
    {
      Expression arrayExpression = (Expression) args[0];
      Expression dimensionExpression = (Expression) args[2];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(arrayExpression.getLexicalPhrase(), (LexicalPhrase) args[1], dimensionExpression.getLexicalPhrase(), (LexicalPhrase) args[3]);
      ArrayElementAssignee assignee = new ArrayElementAssignee(arrayExpression, dimensionExpression, lexicalPhrase);
      return new ParseList<Assignee>(assignee, lexicalPhrase);
    }
    if (production == CONTINUATION_VARIABLE_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Assignee> list = (ParseList<Assignee>) args[0];
      Name name = (Name) args[2];
      VariableAssignee assignee = new VariableAssignee(name.getName(), name.getLexicalPhrase());
      list.addLast(assignee, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], assignee.getLexicalPhrase()));
      return list;
    }
    if (production == CONTINUATION_ARRAY_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Assignee> list = (ParseList<Assignee>) args[0];
      Expression arrayExpression = (Expression) args[2];
      Expression dimensionExpression = (Expression) args[4];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(arrayExpression.getLexicalPhrase(), (LexicalPhrase) args[3], dimensionExpression.getLexicalPhrase(), (LexicalPhrase) args[5]);
      ArrayElementAssignee assignee = new ArrayElementAssignee(arrayExpression, dimensionExpression, lexicalPhrase);
      list.addLast(assignee, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], lexicalPhrase));
      return list;
    }
    throw badTypeList();
  }

}
