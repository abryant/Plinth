package eu.bryants.anthony.toylanguage.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.misc.BlankAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.VariableAssignee;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;

/*
 * Created on 1 Oct 2012
 */

/**
 * @author Anthony Bryant
 */
public class DeclarationAssigneeListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_NAME_PRODUCTION = new Production<ParseType>(ParseType.NAME);
  private static final Production<ParseType> START_BLANK_PRODUCTION = new Production<ParseType>(ParseType.UNDERSCORE);
  private static final Production<ParseType> CONTINUATION_NAME_PRODUCTION = new Production<ParseType>(ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.COMMA, ParseType.NAME);
  private static final Production<ParseType> CONTINUATION_BLANK_PRODUCTION = new Production<ParseType>(ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.COMMA, ParseType.UNDERSCORE);

  @SuppressWarnings("unchecked")
  public DeclarationAssigneeListRule()
  {
    super(ParseType.DECLARATION_ASSIGNEE_LIST, START_NAME_PRODUCTION, START_BLANK_PRODUCTION, CONTINUATION_NAME_PRODUCTION, CONTINUATION_BLANK_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_NAME_PRODUCTION)
    {
      Name name = (Name) args[0];
      Assignee assignee = new VariableAssignee(name.getName(), name.getLexicalPhrase());
      return new ParseList<Assignee>(assignee, assignee.getLexicalPhrase());
    }
    if (production == START_BLANK_PRODUCTION)
    {
      Assignee assignee = new BlankAssignee((LexicalPhrase) args[0]);
      return new ParseList<Assignee>(assignee, assignee.getLexicalPhrase());
    }
    if (production == CONTINUATION_NAME_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Assignee> list = (ParseList<Assignee>) args[0];
      Name name = (Name) args[2];
      Assignee assignee = new VariableAssignee(name.getName(), name.getLexicalPhrase());
      list.addLast(assignee, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], assignee.getLexicalPhrase()));
      return list;
    }
    if (production == CONTINUATION_BLANK_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Assignee> list = (ParseList<Assignee>) args[0];
      Assignee assignee = new BlankAssignee((LexicalPhrase) args[2]);
      list.addLast(assignee, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], assignee.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
