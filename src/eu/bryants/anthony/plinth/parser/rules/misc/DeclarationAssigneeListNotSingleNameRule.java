package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.Assignee;
import eu.bryants.anthony.plinth.ast.misc.BlankAssignee;
import eu.bryants.anthony.plinth.ast.misc.VariableAssignee;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 10 Sep 2013
 */

/**
 * @author Anthony Bryant
 */
public class DeclarationAssigneeListNotSingleNameRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_NAME_NAME_PRODUCTION = new Production<ParseType>(ParseType.NAME, ParseType.COMMA, ParseType.NAME);
  private static final Production<ParseType> START_BLANK_NAME_PRODUCTION = new Production<ParseType>(ParseType.UNDERSCORE, ParseType.COMMA, ParseType.NAME);
  private static final Production<ParseType> START_BLANK_PRODUCTION = new Production<ParseType>(ParseType.UNDERSCORE);
  private static final Production<ParseType> CONTINUATION_NAME_PRODUCTION = new Production<ParseType>(ParseType.NAME, ParseType.COMMA, ParseType.DECLARATION_ASSIGNEE_LIST_NOT_SINGLE_NAME);
  private static final Production<ParseType> CONTINUATION_BLANK_PRODUCTION = new Production<ParseType>(ParseType.UNDERSCORE, ParseType.COMMA, ParseType.DECLARATION_ASSIGNEE_LIST_NOT_SINGLE_NAME);

  public DeclarationAssigneeListNotSingleNameRule()
  {
    super(ParseType.DECLARATION_ASSIGNEE_LIST_NOT_SINGLE_NAME, START_NAME_NAME_PRODUCTION, START_BLANK_NAME_PRODUCTION, START_BLANK_PRODUCTION, CONTINUATION_NAME_PRODUCTION, CONTINUATION_BLANK_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_NAME_NAME_PRODUCTION)
    {
      Name name1 = (Name) args[0];
      Name name2 = (Name) args[2];
      Assignee assignee1 = new VariableAssignee(name1.getName(), name1.getLexicalPhrase());
      Assignee assignee2 = new VariableAssignee(name2.getName(), name2.getLexicalPhrase());
      return new ParseList<Assignee>(new Assignee[] {assignee1, assignee2}, LexicalPhrase.combine(assignee1.getLexicalPhrase(), (LexicalPhrase) args[1], assignee2.getLexicalPhrase()));
    }
    if (production == START_BLANK_NAME_PRODUCTION)
    {
      Assignee assignee1 = new BlankAssignee((LexicalPhrase) args[0]);
      Name name = (Name) args[2];
      Assignee assignee2 = new VariableAssignee(name.getName(), name.getLexicalPhrase());
      return new ParseList<Assignee>(new Assignee[] {assignee1, assignee2}, LexicalPhrase.combine(assignee1.getLexicalPhrase(), (LexicalPhrase) args[1], assignee2.getLexicalPhrase()));
    }
    if (production == START_BLANK_PRODUCTION)
    {
      Assignee assignee = new BlankAssignee((LexicalPhrase) args[0]);
      return new ParseList<Assignee>(assignee, assignee.getLexicalPhrase());
    }
    if (production == CONTINUATION_NAME_PRODUCTION)
    {
      Name name = (Name) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Assignee> list = (ParseList<Assignee>) args[2];
      Assignee assignee = new VariableAssignee(name.getName(), name.getLexicalPhrase());
      list.addFirst(assignee, LexicalPhrase.combine(assignee.getLexicalPhrase(), (LexicalPhrase) args[1], list.getLexicalPhrase()));
      return list;
    }
    if (production == CONTINUATION_BLANK_PRODUCTION)
    {
      Assignee assignee = new BlankAssignee((LexicalPhrase) args[0]);
      @SuppressWarnings("unchecked")
      ParseList<Assignee> list = (ParseList<Assignee>) args[2];
      list.addFirst(assignee, LexicalPhrase.combine(assignee.getLexicalPhrase(), (LexicalPhrase) args[1], list.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
