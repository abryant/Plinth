package eu.bryants.anthony.plinth.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.statement.ForEachStatement;
import eu.bryants.anthony.plinth.ast.statement.ForStatement;
import eu.bryants.anthony.plinth.ast.statement.Statement;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseContainer;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 18 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ForStatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION              = new Production<ParseType>(ParseType.FOR_KEYWORD, ParseType.LPAREN, ParseType.FOR_INIT, ParseType.EXPRESSION, ParseType.SEMICOLON, ParseType.FOR_UPDATE, ParseType.RPAREN, ParseType.BLOCK);
  private static final Production<ParseType> NO_CONDITION_PRODUCTION = new Production<ParseType>(ParseType.FOR_KEYWORD, ParseType.LPAREN, ParseType.FOR_INIT,                       ParseType.SEMICOLON, ParseType.FOR_UPDATE, ParseType.RPAREN, ParseType.BLOCK);
  private static final Production<ParseType> FOR_EACH_PRODUCTION           = new Production<ParseType>(ParseType.FOR_KEYWORD,                      ParseType.TYPE, ParseType.NAME, ParseType.IN_KEYWORD, ParseType.EXPRESSION, ParseType.BLOCK);
  private static final Production<ParseType> FOR_EACH_MODIFIERS_PRODUCTION = new Production<ParseType>(ParseType.FOR_KEYWORD, ParseType.MODIFIERS, ParseType.TYPE, ParseType.NAME, ParseType.IN_KEYWORD, ParseType.EXPRESSION, ParseType.BLOCK);

  public ForStatementRule()
  {
    super(ParseType.FOR_STATEMENT, PRODUCTION, NO_CONDITION_PRODUCTION, FOR_EACH_PRODUCTION, FOR_EACH_MODIFIERS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseContainer<Statement> init = (ParseContainer<Statement>) args[2];
      Expression condition = (Expression) args[3];
      Statement update = (Statement) args[5];
      Block block = (Block) args[7];
      return new ForStatement(init.getItem(), condition, update, block,
                              LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], init.getLexicalPhrase(), condition.getLexicalPhrase(), (LexicalPhrase) args[4],
                                                    update == null ? null : update.getLexicalPhrase(), (LexicalPhrase) args[6], block.getLexicalPhrase()));
    }
    if (production == NO_CONDITION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseContainer<Statement> init = (ParseContainer<Statement>) args[2];
      Statement update = (Statement) args[4];
      Block block = (Block) args[6];
      return new ForStatement(init.getItem(), null, update, block,
                              LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], init.getLexicalPhrase(), (LexicalPhrase) args[3],
                                                    update == null ? null : update.getLexicalPhrase(), (LexicalPhrase) args[5], block.getLexicalPhrase()));
    }
    if (production == FOR_EACH_PRODUCTION)
    {
      Type type = (Type) args[1];
      Name name = (Name) args[2];
      Expression iterableExpression = (Expression) args[4];
      Block block = (Block) args[5];
      return new ForEachStatement(type, false, name.getName(), iterableExpression, block,
                                  LexicalPhrase.combine((LexicalPhrase) args[0], type.getLexicalPhrase(), name.getLexicalPhrase(),
                                                        (LexicalPhrase) args[3], iterableExpression.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    if (production == FOR_EACH_MODIFIERS_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[1];
      Type type = (Type) args[2];
      Name name = (Name) args[3];
      Expression iterableExpression = (Expression) args[5];
      Block block = (Block) args[6];
      return processModifiers(modifiers, type, name.getName(), iterableExpression, block,
                              LexicalPhrase.combine((LexicalPhrase) args[0], modifiers.getLexicalPhrase(), type.getLexicalPhrase(), name.getLexicalPhrase(),
                                                    (LexicalPhrase) args[4], iterableExpression.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    throw badTypeList();
  }

  private ForEachStatement processModifiers(ParseList<Modifier> modifiers, Type type, String name, Expression iterableExpression, Block block, LexicalPhrase lexicalPhrase) throws LanguageParseException
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
    return new ForEachStatement(type, isFinal, name, iterableExpression, block, lexicalPhrase);
  }
}
