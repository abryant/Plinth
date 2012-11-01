package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.Assignee;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 5 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class AssigneeListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> QNAME_PRODUCTION    = new Production<ParseType>(ParseType.QNAME,             ParseType.COMMA, ParseType.ASSIGNEE_LIST);
  private static final Production<ParseType> NO_QNAME_PRODUCTION = new Production<ParseType>(ParseType.ASSIGNEE_NO_QNAME, ParseType.COMMA, ParseType.ASSIGNEE_LIST);
  private static final Production<ParseType> END_PRODUCTION    = new Production<ParseType>(ParseType.ASSIGNEE);

  public AssigneeListRule()
  {
    super(ParseType.ASSIGNEE_LIST, QNAME_PRODUCTION, NO_QNAME_PRODUCTION, END_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == QNAME_PRODUCTION)
    {
      QName qname = (QName) args[0];
      Assignee assignee = AssigneeRule.createQNameAssignee(qname);
      @SuppressWarnings("unchecked")
      ParseList<Assignee> list = (ParseList<Assignee>) args[2];
      list.addFirst(assignee, LexicalPhrase.combine(assignee.getLexicalPhrase(), (LexicalPhrase) args[1], list.getLexicalPhrase()));
      return list;
    }
    if (production == NO_QNAME_PRODUCTION)
    {
      Assignee assignee = (Assignee) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Assignee> list = (ParseList<Assignee>) args[2];
      list.addFirst(assignee, LexicalPhrase.combine(assignee.getLexicalPhrase(), (LexicalPhrase) args[1], list.getLexicalPhrase()));
      return list;
    }
    if (production == END_PRODUCTION)
    {
      Assignee assignee = (Assignee) args[0];
      return new ParseList<Assignee>(assignee, assignee.getLexicalPhrase());
    }
    throw badTypeList();
  }

}
