package eu.bryants.anthony.plinth.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.CatchClause;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.statement.TryStatement;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 16 Feb 2013
 */

/**
 * @author Anthony Bryant
 */
public class TryCatchStatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION                        = new Production<ParseType>(ParseType.TRY_KEYWORD, ParseType.BLOCK, ParseType.CATCH_KEYWORD,                      ParseType.CATCH_TYPE_LIST, ParseType.NAME, ParseType.BLOCK);
  private static final Production<ParseType> MODIFIERS_PRODUCTION              = new Production<ParseType>(ParseType.TRY_KEYWORD, ParseType.BLOCK, ParseType.CATCH_KEYWORD, ParseType.MODIFIERS, ParseType.CATCH_TYPE_LIST, ParseType.NAME, ParseType.BLOCK);
  private static final Production<ParseType> CONTINUATION_PRODUCTION           = new Production<ParseType>(ParseType.TRY_CATCH_STATEMENT,          ParseType.CATCH_KEYWORD,                      ParseType.CATCH_TYPE_LIST, ParseType.NAME, ParseType.BLOCK);
  private static final Production<ParseType> CONTINUATION_MODIFIERS_PRODUCTION = new Production<ParseType>(ParseType.TRY_CATCH_STATEMENT,          ParseType.CATCH_KEYWORD, ParseType.MODIFIERS, ParseType.CATCH_TYPE_LIST, ParseType.NAME, ParseType.BLOCK);

  public TryCatchStatementRule()
  {
    super(ParseType.TRY_CATCH_STATEMENT, PRODUCTION, MODIFIERS_PRODUCTION, CONTINUATION_PRODUCTION, CONTINUATION_MODIFIERS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      Block tryBlock = (Block) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Type> catchTypeList = (ParseList<Type>) args[3];
      Name catchVariableName = (Name) args[4];
      Block catchBlock = (Block) args[5];
      CatchClause catchClause = processCatchModifiers(null, catchTypeList.toArray(new Type[catchTypeList.size()]), catchVariableName.getName(), catchBlock,
                                                      LexicalPhrase.combine((LexicalPhrase) args[2], catchTypeList.getLexicalPhrase(), catchVariableName.getLexicalPhrase(), catchBlock.getLexicalPhrase()));
      return new TryStatement(tryBlock, new CatchClause[] {catchClause}, null, LexicalPhrase.combine((LexicalPhrase) args[0], tryBlock.getLexicalPhrase(), catchClause.getLexicalPhrase()));
    }
    if (production == MODIFIERS_PRODUCTION)
    {
      Block tryBlock = (Block) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[3];
      @SuppressWarnings("unchecked")
      ParseList<Type> catchTypeList = (ParseList<Type>) args[4];
      Name catchVariableName = (Name) args[5];
      Block catchBlock = (Block) args[6];
      CatchClause catchClause = processCatchModifiers(modifiers.toArray(new Modifier[modifiers.size()]), catchTypeList.toArray(new Type[catchTypeList.size()]), catchVariableName.getName(), catchBlock,
                                                      LexicalPhrase.combine((LexicalPhrase) args[2], modifiers.getLexicalPhrase(), catchTypeList.getLexicalPhrase(), catchVariableName.getLexicalPhrase(), catchBlock.getLexicalPhrase()));
      return new TryStatement(tryBlock, new CatchClause[] {catchClause}, null, LexicalPhrase.combine((LexicalPhrase) args[0], tryBlock.getLexicalPhrase(), catchClause.getLexicalPhrase()));
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      TryStatement oldStatement = (TryStatement) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Type> catchTypeList = (ParseList<Type>) args[2];
      Name catchVariableName = (Name) args[3];
      Block catchBlock = (Block) args[4];
      CatchClause catchClause = processCatchModifiers(null, catchTypeList.toArray(new Type[catchTypeList.size()]), catchVariableName.getName(), catchBlock,
                                                      LexicalPhrase.combine((LexicalPhrase) args[1], catchTypeList.getLexicalPhrase(), catchVariableName.getLexicalPhrase(), catchBlock.getLexicalPhrase()));
      CatchClause[] oldClauses = oldStatement.getCatchClauses();
      CatchClause[] newClauses = new CatchClause[oldClauses.length + 1];
      System.arraycopy(oldClauses, 0, newClauses, 0, oldClauses.length);
      newClauses[oldClauses.length] = catchClause;
      return new TryStatement(oldStatement.getTryBlock(), newClauses, oldStatement.getFinallyBlock(), LexicalPhrase.combine(oldStatement.getLexicalPhrase(), catchClause.getLexicalPhrase()));
    }
    if (production == CONTINUATION_MODIFIERS_PRODUCTION)
    {
      TryStatement oldStatement = (TryStatement) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[2];
      @SuppressWarnings("unchecked")
      ParseList<Type> catchTypeList = (ParseList<Type>) args[3];
      Name catchVariableName = (Name) args[4];
      Block catchBlock = (Block) args[5];
      CatchClause catchClause = processCatchModifiers(modifiers.toArray(new Modifier[modifiers.size()]), catchTypeList.toArray(new Type[catchTypeList.size()]), catchVariableName.getName(), catchBlock,
                                                      LexicalPhrase.combine((LexicalPhrase) args[1], modifiers.getLexicalPhrase(), catchTypeList.getLexicalPhrase(), catchVariableName.getLexicalPhrase(), catchBlock.getLexicalPhrase()));
      CatchClause[] oldClauses = oldStatement.getCatchClauses();
      CatchClause[] newClauses = new CatchClause[oldClauses.length + 1];
      System.arraycopy(oldClauses, 0, newClauses, 0, oldClauses.length);
      newClauses[oldClauses.length] = catchClause;
      return new TryStatement(oldStatement.getTryBlock(), newClauses, oldStatement.getFinallyBlock(), LexicalPhrase.combine(oldStatement.getLexicalPhrase(), catchClause.getLexicalPhrase()));
    }
    throw badTypeList();
  }

  private CatchClause processCatchModifiers(Modifier[] modifiers, Type[] typeList, String name, Block block, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isFinal = false;
    if (modifiers != null)
    {
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
        default:
          throw new IllegalStateException("Unknown modifier: " + modifier);
        }
      }
    }
    return new CatchClause(typeList, isFinal, name, block, lexicalPhrase);
  }
}
