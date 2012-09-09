package eu.bryants.anthony.toylanguage.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.LanguageParseException;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.Modifier;
import eu.bryants.anthony.toylanguage.parser.parseAST.ModifierType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class AssignStatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> ASSIGN_PRODUCTION                = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.EQUALS, ParseType.TUPLE_EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> DECLARATION_PRODUCTION           = new Production<ParseType>(                     ParseType.TYPE, ParseType.ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> MODIFIERS_DECLARATION_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE, ParseType.ASSIGNEE_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> DEFINTION_PRODUCTION             = new Production<ParseType>(                     ParseType.TYPE, ParseType.ASSIGNEE_LIST, ParseType.EQUALS, ParseType.TUPLE_EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> MODIFIERS_DEFINTION_PRODUCTION   = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE, ParseType.ASSIGNEE_LIST, ParseType.EQUALS, ParseType.TUPLE_EXPRESSION, ParseType.SEMICOLON);

  @SuppressWarnings("unchecked")
  public AssignStatementRule()
  {
    super(ParseType.ASSIGN_STATEMENT, ASSIGN_PRODUCTION, DECLARATION_PRODUCTION, MODIFIERS_DECLARATION_PRODUCTION, DEFINTION_PRODUCTION, MODIFIERS_DEFINTION_PRODUCTION);
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
    if (production == DECLARATION_PRODUCTION)
    {
      Type type = (Type) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Assignee> assignees = (ParseList<Assignee>) args[1];
      return new AssignStatement(false, type, assignees.toArray(new Assignee[assignees.size()]), null,
                                 LexicalPhrase.combine(type.getLexicalPhrase(), assignees.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    if (production == MODIFIERS_DECLARATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type type = (Type) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Assignee> assignees = (ParseList<Assignee>) args[2];
      return processModifiers(modifiers, type, assignees.toArray(new Assignee[assignees.size()]), null,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), type.getLexicalPhrase(), assignees.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == DEFINTION_PRODUCTION)
    {
      Type type = (Type) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Assignee> assignees = (ParseList<Assignee>) args[1];
      Expression expression = (Expression) args[3];
      return new AssignStatement(false, type, assignees.toArray(new Assignee[assignees.size()]), expression,
                                 LexicalPhrase.combine(type.getLexicalPhrase(), assignees.getLexicalPhrase(), (LexicalPhrase) args[2], expression.getLexicalPhrase(), (LexicalPhrase) args[4]));
    }
    if (production == MODIFIERS_DEFINTION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type type = (Type) args[1];
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
