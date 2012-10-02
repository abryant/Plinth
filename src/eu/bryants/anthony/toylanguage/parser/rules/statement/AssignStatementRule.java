package eu.bryants.anthony.toylanguage.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.misc.QName;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.LanguageParseException;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.Modifier;
import eu.bryants.anthony.toylanguage.parser.parseAST.ModifierType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;
import eu.bryants.anthony.toylanguage.parser.parseAST.QNameElement;

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
  private static final Production<ParseType> DECLARATION_PRODUCTION                             = new Production<ParseType>(                     ParseType.TYPE_NO_QNAME,     ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> MODIFIERS_DECLARATION_PRODUCTION                   = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE_NO_QNAME,     ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> DEFINTION_PRODUCTION                               = new Production<ParseType>(                     ParseType.TYPE_NO_QNAME,     ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> MODIFIERS_DEFINTION_PRODUCTION                     = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE_NO_QNAME,     ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> QNAME_DECLARATION_PRODUCTION                       = new Production<ParseType>(                     ParseType.QNAME,             ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> QNAME_MODIFIERS_DECLARATION_PRODUCTION             = new Production<ParseType>(ParseType.MODIFIERS, ParseType.QNAME,             ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> QNAME_DEFINTION_PRODUCTION                         = new Production<ParseType>(                     ParseType.QNAME,             ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> QNAME_MODIFIERS_DEFINTION_PRODUCTION               = new Production<ParseType>(ParseType.MODIFIERS, ParseType.QNAME,             ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> NESTED_QNAME_LIST_DECLARATION_PRODUCTION           = new Production<ParseType>(                     ParseType.NESTED_QNAME_LIST, ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> NESTED_QNAME_LIST_MODIFIERS_DECLARATION_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.NESTED_QNAME_LIST, ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> NESTED_QNAME_LIST_DEFINTION_PRODUCTION             = new Production<ParseType>(                     ParseType.NESTED_QNAME_LIST, ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> NESTED_QNAME_LIST_MODIFIERS_DEFINTION_PRODUCTION   = new Production<ParseType>(ParseType.MODIFIERS, ParseType.NESTED_QNAME_LIST, ParseType.DECLARATION_ASSIGNEE_LIST, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);

  @SuppressWarnings("unchecked")
  public AssignStatementRule()
  {
    super(ParseType.ASSIGN_STATEMENT, ASSIGN_PRODUCTION,
                                      DECLARATION_PRODUCTION, MODIFIERS_DECLARATION_PRODUCTION, DEFINTION_PRODUCTION, MODIFIERS_DEFINTION_PRODUCTION,
                                      QNAME_DECLARATION_PRODUCTION, QNAME_DEFINTION_PRODUCTION, QNAME_MODIFIERS_DECLARATION_PRODUCTION, QNAME_MODIFIERS_DEFINTION_PRODUCTION,
                                      NESTED_QNAME_LIST_DECLARATION_PRODUCTION, NESTED_QNAME_LIST_DEFINTION_PRODUCTION, NESTED_QNAME_LIST_MODIFIERS_DECLARATION_PRODUCTION, NESTED_QNAME_LIST_MODIFIERS_DEFINTION_PRODUCTION);
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
      if (modifier.getModifierType() == ModifierType.FINAL)
      {
        if (isFinal)
        {
          throw new LanguageParseException("Duplicate 'final' modifier", modifier.getLexicalPhrase());
        }
        isFinal = true;
      }
      else if (modifier.getModifierType() == ModifierType.NATIVE)
      {
        throw new LanguageParseException("Unexpected modifier: Local variables cannot have native specifiers", modifier.getLexicalPhrase());
      }
      else if (modifier.getModifierType() == ModifierType.STATIC)
      {
        throw new LanguageParseException("Unexpected modifier: Local variables cannot be static", modifier.getLexicalPhrase());
      }
      else
      {
        throw new IllegalStateException("Unknown modifier: " + modifier);
      }
    }
    return new AssignStatement(isFinal, type, assignees, expression, lexicalPhrase);
  }
}
