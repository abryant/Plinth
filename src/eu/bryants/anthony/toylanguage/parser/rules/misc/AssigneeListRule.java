package eu.bryants.anthony.toylanguage.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
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

  private static final Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.ASSIGNEE);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.COMMA, ParseType.ASSIGNEE);

  @SuppressWarnings("unchecked")
  public AssigneeListRule()
  {
    super(ParseType.ASSIGNEE_LIST, START_PRODUCTION, CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      Assignee assignee = (Assignee) args[0];
      return new ParseList<Assignee>(assignee, assignee.getLexicalPhrase());
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Assignee> list = (ParseList<Assignee>) args[0];
      Assignee assignee = (Assignee) args[2];
      list.addLast(assignee, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], assignee.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
