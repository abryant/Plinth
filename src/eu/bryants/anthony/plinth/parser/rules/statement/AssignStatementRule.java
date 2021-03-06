package eu.bryants.anthony.plinth.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.misc.Assignee;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.misc.VariableAssignee;
import eu.bryants.anthony.plinth.ast.statement.AssignStatement;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class AssignStatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> ASSIGN_PRODUCTION                                  = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> DECLARATION_PRODUCTION                             = new Production<ParseType>(                     ParseType.TYPE_NOT_QNAME,    ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> MODIFIERS_DECLARATION_PRODUCTION                   = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE_NOT_QNAME,    ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> DEFINTION_PRODUCTION                               = new Production<ParseType>(                     ParseType.TYPE_NOT_QNAME,    ParseType.DECLARATION_ASSIGNEE_LIST_NOT_SINGLE_NAME, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> SINGLE_DEFINTION_PRODUCTION                        = new Production<ParseType>(                     ParseType.TYPE_NOT_QNAME,    ParseType.NAME,                      ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> MODIFIERS_DEFINTION_PRODUCTION                     = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE_NOT_QNAME,    ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> QNAME_DECLARATION_PRODUCTION                       = new Production<ParseType>(                     ParseType.QNAME,             ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> QNAME_MODIFIERS_DECLARATION_PRODUCTION             = new Production<ParseType>(ParseType.MODIFIERS, ParseType.QNAME,             ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> QNAME_DEFINTION_PRODUCTION                         = new Production<ParseType>(                     ParseType.QNAME,             ParseType.DECLARATION_ASSIGNEE_LIST_NOT_SINGLE_NAME, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> QNAME_SINGLE_DEFINTION_PRODUCTION                  = new Production<ParseType>(                     ParseType.QNAME,             ParseType.NAME,                      ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> QNAME_MODIFIERS_DEFINTION_PRODUCTION               = new Production<ParseType>(ParseType.MODIFIERS, ParseType.QNAME,             ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> NESTED_QNAME_LIST_DECLARATION_PRODUCTION           = new Production<ParseType>(                     ParseType.NESTED_QNAME_LIST, ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> NESTED_QNAME_LIST_MODIFIERS_DECLARATION_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.NESTED_QNAME_LIST, ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> NESTED_QNAME_LIST_DEFINTION_PRODUCTION             = new Production<ParseType>(                     ParseType.NESTED_QNAME_LIST, ParseType.DECLARATION_ASSIGNEE_LIST_NOT_SINGLE_NAME, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> NESTED_QNAME_LIST_SINGLE_DEFINTION_PRODUCTION      = new Production<ParseType>(                     ParseType.NESTED_QNAME_LIST, ParseType.NAME,                      ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> NESTED_QNAME_LIST_MODIFIERS_DEFINTION_PRODUCTION   = new Production<ParseType>(ParseType.MODIFIERS, ParseType.NESTED_QNAME_LIST, ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);

  public AssignStatementRule()
  {
    super(ParseType.ASSIGN_STATEMENT, ASSIGN_PRODUCTION,
                                      DECLARATION_PRODUCTION, MODIFIERS_DECLARATION_PRODUCTION, DEFINTION_PRODUCTION, SINGLE_DEFINTION_PRODUCTION, MODIFIERS_DEFINTION_PRODUCTION,
                                      QNAME_DECLARATION_PRODUCTION, QNAME_MODIFIERS_DECLARATION_PRODUCTION, QNAME_DEFINTION_PRODUCTION, QNAME_SINGLE_DEFINTION_PRODUCTION, QNAME_MODIFIERS_DEFINTION_PRODUCTION,
                                      NESTED_QNAME_LIST_DECLARATION_PRODUCTION, NESTED_QNAME_LIST_MODIFIERS_DECLARATION_PRODUCTION, NESTED_QNAME_LIST_DEFINTION_PRODUCTION, NESTED_QNAME_LIST_SINGLE_DEFINTION_PRODUCTION, NESTED_QNAME_LIST_MODIFIERS_DEFINTION_PRODUCTION);
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
      return new AssignStatement(false, null, assignees.toArray(new Assignee[assignees.size()]), expression,
                                 LexicalPhrase.combine(assignees.getLexicalPhrase(), (LexicalPhrase) args[1], expression.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == SINGLE_DEFINTION_PRODUCTION || production == QNAME_SINGLE_DEFINTION_PRODUCTION || production == NESTED_QNAME_LIST_SINGLE_DEFINTION_PRODUCTION)
    {
      Type type;
      if (production == SINGLE_DEFINTION_PRODUCTION)
      {
        type = (Type) args[0];
      }
      else if (production == QNAME_SINGLE_DEFINTION_PRODUCTION)
      {
        QName qname = (QName) args[0];
        type = new QNameElement(qname, qname.getLexicalPhrase()).convertToType();
      }
      else // if (production == NESTED_QNAME_LIST_SINGLE_DEFINTION_PRODUCTION)
      {
        QNameElement element = (QNameElement) args[0];
        type = element.convertToType();
      }
      Name name = (Name) args[1];
      Expression expression = (Expression) args[3];
      Assignee assignee = new VariableAssignee(name.getName(), name.getLexicalPhrase());
      return new AssignStatement(false, type, new Assignee[] {assignee}, expression, LexicalPhrase.combine(type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2], expression.getLexicalPhrase(), (LexicalPhrase) args[4]));
    }

    Type type;
    if (production == DECLARATION_PRODUCTION || production == DEFINTION_PRODUCTION)
    {
      type = (Type) args[0];
    }
    else if (production == MODIFIERS_DECLARATION_PRODUCTION || production == MODIFIERS_DEFINTION_PRODUCTION)
    {
      type = (Type) args[1];
    }
    else if (production == QNAME_DECLARATION_PRODUCTION || production == QNAME_DEFINTION_PRODUCTION)
    {
      QName qname = (QName) args[0];
      type = new QNameElement(qname, qname.getLexicalPhrase()).convertToType();
    }
    else if (production == QNAME_MODIFIERS_DECLARATION_PRODUCTION || production == QNAME_MODIFIERS_DEFINTION_PRODUCTION)
    {
      QName qname = (QName) args[1];
      type = new QNameElement(qname, qname.getLexicalPhrase()).convertToType();
    }
    else if (production == NESTED_QNAME_LIST_DECLARATION_PRODUCTION || production == NESTED_QNAME_LIST_DEFINTION_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      type = element.convertToType();
    }
    else if (production == NESTED_QNAME_LIST_MODIFIERS_DECLARATION_PRODUCTION || production == NESTED_QNAME_LIST_MODIFIERS_DEFINTION_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[1];
      type = element.convertToType();
    }
    else
    {
      throw badTypeList();
    }



    if (production == DECLARATION_PRODUCTION || production == QNAME_DECLARATION_PRODUCTION || production == NESTED_QNAME_LIST_DECLARATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Assignee> assignees = (ParseList<Assignee>) args[1];
      return new AssignStatement(false, type, assignees.toArray(new Assignee[assignees.size()]), null,
                                 LexicalPhrase.combine(type.getLexicalPhrase(), assignees.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    if (production == MODIFIERS_DECLARATION_PRODUCTION || production == QNAME_MODIFIERS_DECLARATION_PRODUCTION || production == NESTED_QNAME_LIST_MODIFIERS_DECLARATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Assignee> assignees = (ParseList<Assignee>) args[2];
      return processModifiers(modifiers, type, assignees.toArray(new Assignee[assignees.size()]), null,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), type.getLexicalPhrase(), assignees.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == DEFINTION_PRODUCTION || production == QNAME_DEFINTION_PRODUCTION || production == NESTED_QNAME_LIST_DEFINTION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Assignee> assignees = (ParseList<Assignee>) args[1];
      Expression expression = (Expression) args[3];
      return new AssignStatement(false, type, assignees.toArray(new Assignee[assignees.size()]), expression,
                                 LexicalPhrase.combine(type.getLexicalPhrase(), assignees.getLexicalPhrase(), (LexicalPhrase) args[2], expression.getLexicalPhrase(), (LexicalPhrase) args[4]));
    }
    if (production == MODIFIERS_DEFINTION_PRODUCTION || production == QNAME_MODIFIERS_DEFINTION_PRODUCTION || production == NESTED_QNAME_LIST_MODIFIERS_DEFINTION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Assignee> assignees = (ParseList<Assignee>) args[2];
      Expression expression = (Expression) args[4];
      return processModifiers(modifiers, type, assignees.toArray(new Assignee[assignees.size()]), expression,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), type.getLexicalPhrase(), assignees.getLexicalPhrase(), (LexicalPhrase) args[3], expression.getLexicalPhrase(), (LexicalPhrase) args[5]));
    }
    throw badTypeList();
  }

  private AssignStatement processModifiers(ParseList<Modifier> modifiers, Type type, Assignee[] assignees, Expression expression, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isFinal = false;
    for (Modifier modifier : modifiers)
    {
      switch (modifier.getModifierType())
      {
      case ABSTRACT:
        throw new LanguageParseException("Unexpected modifier: Local variables cannot be abstract", modifier.getLexicalPhrase());
      case FINAL:
        if (isFinal)
        {
          throw new LanguageParseException("Duplicate 'final' modifier", modifier.getLexicalPhrase());
        }
        isFinal = true;
        break;
      case IMMUTABLE:
        throw new LanguageParseException("Unexpected modifier: The 'immutable' modifier does not apply to local variables (try using #Type instead)", modifier.getLexicalPhrase());
      case MUTABLE:
        throw new LanguageParseException("Unexpected modifier: Local variables cannot be mutable", modifier.getLexicalPhrase());
      case NATIVE:
        throw new LanguageParseException("Unexpected modifier: Local variables cannot have native specifiers", modifier.getLexicalPhrase());
      case SELFISH:
        throw new LanguageParseException("Unexpected modifier: Local variables cannot be selfish", modifier.getLexicalPhrase());
      case SINCE:
        throw new LanguageParseException("Unexpected modifier: Local variables cannot have since(...) specifiers", modifier.getLexicalPhrase());
      case STATIC:
        throw new LanguageParseException("Unexpected modifier: Local variables cannot be static", modifier.getLexicalPhrase());
      case UNBACKED:
        throw new LanguageParseException("Unexpected modifier: Local variables cannot be unbacked", modifier.getLexicalPhrase());
      default:
        throw new IllegalStateException("Unknown modifier: " + modifier);
      }
    }
    return new AssignStatement(isFinal, type, assignees, expression, lexicalPhrase);
  }
}
