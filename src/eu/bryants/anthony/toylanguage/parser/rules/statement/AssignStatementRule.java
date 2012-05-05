package eu.bryants.anthony.toylanguage.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class AssignStatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> ASSIGN_PRODUCTION = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.EQUALS, ParseType.TUPLE_EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> DECLARATION_PRODUCTION = new Production<ParseType>(ParseType.TYPE, ParseType.ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> DEFINTION_PRODUCTION = new Production<ParseType>(ParseType.TYPE, ParseType.ASSIGNEE_LIST, ParseType.EQUALS, ParseType.TUPLE_EXPRESSION, ParseType.SEMICOLON);

  @SuppressWarnings("unchecked")
  public AssignStatementRule()
  {
    super(ParseType.ASSIGN_STATEMENT, ASSIGN_PRODUCTION, DECLARATION_PRODUCTION, DEFINTION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == ASSIGN_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Assignee> assignees = (ParseList<Assignee>) args[0];
      Expression expression = (Expression) args[2];
      return new AssignStatement(null, assignees.toArray(new Assignee[assignees.size()]), expression,
                                 LexicalPhrase.combine(assignees.getLexicalPhrase(), (LexicalPhrase) args[1], expression.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == DECLARATION_PRODUCTION)
    {
      Type type = (Type) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Assignee> assignees = (ParseList<Assignee>) args[1];
      return new AssignStatement(type, assignees.toArray(new Assignee[assignees.size()]), null,
                                 LexicalPhrase.combine(type.getLexicalPhrase(), assignees.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    if (production == DEFINTION_PRODUCTION)
    {
      Type type = (Type) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Assignee> assignees = (ParseList<Assignee>) args[1];
      Expression expression = (Expression) args[3];
      return new AssignStatement(type, assignees.toArray(new Assignee[assignees.size()]), expression,
                                 LexicalPhrase.combine(type.getLexicalPhrase(), assignees.getLexicalPhrase(), (LexicalPhrase) args[2], expression.getLexicalPhrase(), (LexicalPhrase) args[4]));
    }
    throw badTypeList();
  }

}
