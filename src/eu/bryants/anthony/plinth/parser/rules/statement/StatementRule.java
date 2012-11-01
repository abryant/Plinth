package eu.bryants.anthony.plinth.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.statement.ExpressionStatement;
import eu.bryants.anthony.plinth.ast.statement.ShorthandAssignStatement;
import eu.bryants.anthony.plinth.parser.ParseType;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class StatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> ASSIGN_PRODUCTION = new Production<ParseType>(ParseType.ASSIGN_STATEMENT);
  private static final Production<ParseType> BLOCK_PRODUCTION  = new Production<ParseType>(ParseType.BLOCK);
  private static final Production<ParseType> BREAK_PRODUCTION  = new Production<ParseType>(ParseType.BREAK_STATEMENT);
  private static final Production<ParseType> CONTINUE_PRODUCTION  = new Production<ParseType>(ParseType.CONTINUE_STATEMENT);
  private static final Production<ParseType> FOR_PRODUCTION  = new Production<ParseType>(ParseType.FOR_STATEMENT);
  private static final Production<ParseType> IF_PRODUCTION  = new Production<ParseType>(ParseType.IF_STATEMENT);
  private static final Production<ParseType> INC_DEC_PRODUCTION = new Production<ParseType>(ParseType.PREFIX_INC_DEC_STATEMENT);
  private static final Production<ParseType> RETURN_PRODUCTION = new Production<ParseType>(ParseType.RETURN_STATEMENT);
  private static final Production<ParseType> WHILE_PRODUCTION  = new Production<ParseType>(ParseType.WHILE_STATEMENT);

  private static final Production<ParseType> SHORTHAND_ASSIGN_PRODUCTION = new Production<ParseType>(ParseType.SHORTHAND_ASSIGNMENT, ParseType.SEMICOLON);

  private static final Production<ParseType> FUNCTION_CALL_PRODUCTION = new Production<ParseType>(ParseType.FUNCTION_CALL_EXPRESSION, ParseType.SEMICOLON);
  private static final Production<ParseType> CLASS_CREATION_PRODUCTION = new Production<ParseType>(ParseType.CLASS_CREATION_EXPRESSION, ParseType.SEMICOLON);

  public StatementRule()
  {
    super(ParseType.STATEMENT, ASSIGN_PRODUCTION, BLOCK_PRODUCTION, BREAK_PRODUCTION, CONTINUE_PRODUCTION, IF_PRODUCTION, FOR_PRODUCTION, INC_DEC_PRODUCTION,
                               RETURN_PRODUCTION, WHILE_PRODUCTION, SHORTHAND_ASSIGN_PRODUCTION, FUNCTION_CALL_PRODUCTION, CLASS_CREATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == ASSIGN_PRODUCTION   || production == BLOCK_PRODUCTION  || production == BREAK_PRODUCTION   ||
        production == CONTINUE_PRODUCTION || production == IF_PRODUCTION     || production == FOR_PRODUCTION     ||
        production == INC_DEC_PRODUCTION  || production == RETURN_PRODUCTION   || production == WHILE_PRODUCTION)
    {
      return args[0];
    }
    if (production == SHORTHAND_ASSIGN_PRODUCTION)
    {
      // re-create the ShorthandAssignStatement to add the LexicalPhrase from the semicolon
      ShorthandAssignStatement oldStatement = (ShorthandAssignStatement) args[0];
      return new ShorthandAssignStatement(oldStatement.getAssignees(), oldStatement.getOperator(), oldStatement.getExpression(),
                                          LexicalPhrase.combine(oldStatement.getLexicalPhrase(), (LexicalPhrase) args[1]));
    }
    if (production == FUNCTION_CALL_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      return new ExpressionStatement(expression, LexicalPhrase.combine(expression.getLexicalPhrase(), (LexicalPhrase) args[1]));
    }
    if (production == CLASS_CREATION_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      return new ExpressionStatement(expression, LexicalPhrase.combine(expression.getLexicalPhrase(), (LexicalPhrase) args[1]));
    }
    throw badTypeList();
  }

}
