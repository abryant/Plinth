package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.expression.BooleanLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BracketedExpression;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.terminal.FloatingLiteral;
import eu.bryants.anthony.toylanguage.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class PrimaryRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> TRUE_PRODUCTION = new Production<ParseType>(ParseType.TRUE_KEYWORD);
  private static Production<ParseType> FALSE_PRODUCTION = new Production<ParseType>(ParseType.FALSE_KEYWORD);
  private static Production<ParseType> FLOATING_PRODUCTION = new Production<ParseType>(ParseType.FLOATING_LITERAL);
  private static Production<ParseType> INTEGER_PRODUCTION = new Production<ParseType>(ParseType.INTEGER_LITERAL);
  private static Production<ParseType> VARIABLE_PRODUCTION = new Production<ParseType>(ParseType.NAME);
  private static Production<ParseType> FUNCTION_CALL_PRODUCTION = new Production<ParseType>(ParseType.NAME, ParseType.LPAREN, ParseType.ARGUMENTS, ParseType.RPAREN);
  private static Production<ParseType> FUNCTION_CALL_NO_ARGUMENTS_PRODUCTION = new Production<ParseType>(ParseType.NAME, ParseType.LPAREN, ParseType.RPAREN);
  private static Production<ParseType> BRACKETS_PRODUCTION =  new Production<ParseType>(ParseType.LPAREN, ParseType.EXPRESSION, ParseType.RPAREN);

  @SuppressWarnings("unchecked")
  public PrimaryRule()
  {
    super(ParseType.PRIMARY, TRUE_PRODUCTION, FALSE_PRODUCTION, FLOATING_PRODUCTION, INTEGER_PRODUCTION, VARIABLE_PRODUCTION, FUNCTION_CALL_PRODUCTION, FUNCTION_CALL_NO_ARGUMENTS_PRODUCTION, BRACKETS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == TRUE_PRODUCTION)
    {
      return new BooleanLiteralExpression(true, (LexicalPhrase) args[0]);
    }
    if (production == FALSE_PRODUCTION)
    {
      return new BooleanLiteralExpression(false, (LexicalPhrase) args[0]);
    }
    if (production == FLOATING_PRODUCTION)
    {
      FloatingLiteral literal = (FloatingLiteral) args[0];
      return new FloatingLiteralExpression(literal, literal.getLexicalPhrase());
    }
    if (production == INTEGER_PRODUCTION)
    {
      IntegerLiteral literal = (IntegerLiteral) args[0];
      return new IntegerLiteralExpression(literal, literal.getLexicalPhrase());
    }
    if (production == VARIABLE_PRODUCTION)
    {
      Name name = (Name) args[0];
      return new VariableExpression(name.getName(), name.getLexicalPhrase());
    }
    if (production == FUNCTION_CALL_PRODUCTION)
    {
      Name name = (Name) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Expression> arguments = (ParseList<Expression>) args[2];
      return new FunctionCallExpression(name.getName(), arguments.toArray(new Expression[arguments.size()]), LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1],
                                                                                                                                   arguments.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == FUNCTION_CALL_NO_ARGUMENTS_PRODUCTION)
    {
      Name name = (Name) args[0];
      return new FunctionCallExpression(name.getName(), new Expression[0], LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], (LexicalPhrase) args[2]));
    }
    if (production == BRACKETS_PRODUCTION)
    {
      Expression expression = (Expression) args[1];
      return new BracketedExpression(expression, LexicalPhrase.combine((LexicalPhrase) args[0], expression.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    throw badTypeList();
  }

}
