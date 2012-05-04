package eu.bryants.anthony.toylanguage.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.expression.ArrayAccessExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ArrayCreationExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BooleanLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BracketedExpression;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.terminal.FloatingLiteral;
import eu.bryants.anthony.toylanguage.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.ast.type.ArrayType;
import eu.bryants.anthony.toylanguage.ast.type.Type;
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
  private static Production<ParseType> FIELD_ACCESS_PRODUCTION = new Production<ParseType>(ParseType.PRIMARY, ParseType.DOT, ParseType.NAME);
  private static Production<ParseType> ARRAY_ACCESS_PRODUCTION = new Production<ParseType>(ParseType.PRIMARY, ParseType.LSQUARE, ParseType.EXPRESSION, ParseType.RSQUARE);
  private static Production<ParseType> FUNCTION_CALL_PRODUCTION = new Production<ParseType>(ParseType.FUNCTION_CALL_EXPRESSION);
  private static Production<ParseType> BRACKETS_PRODUCTION =  new Production<ParseType>(ParseType.LPAREN, ParseType.EXPRESSION, ParseType.RPAREN);
  private static Production<ParseType> ARRAY_CREATION_PRODUCTION = new Production<ParseType>(ParseType.NEW_KEYWORD, ParseType.DIMENSIONS, ParseType.TYPE);
  private static Production<ParseType> ARRAY_CREATION_EMPTY_LIST_PRODUCTION = new Production<ParseType>(ParseType.NEW_KEYWORD, ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE, ParseType.LBRACE, ParseType.RBRACE);
  private static Production<ParseType> ARRAY_CREATION_LIST_PRODUCTION = new Production<ParseType>(ParseType.NEW_KEYWORD, ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE, ParseType.LBRACE, ParseType.EXPRESSION_LIST, ParseType.RBRACE);

  @SuppressWarnings("unchecked")
  public PrimaryRule()
  {
    super(ParseType.PRIMARY, TRUE_PRODUCTION, FALSE_PRODUCTION, FLOATING_PRODUCTION, INTEGER_PRODUCTION, VARIABLE_PRODUCTION, FIELD_ACCESS_PRODUCTION, ARRAY_ACCESS_PRODUCTION, FUNCTION_CALL_PRODUCTION, BRACKETS_PRODUCTION,
                             ARRAY_CREATION_PRODUCTION, ARRAY_CREATION_EMPTY_LIST_PRODUCTION, ARRAY_CREATION_LIST_PRODUCTION);
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
    if (production == FIELD_ACCESS_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      Name name = (Name) args[2];
      return new FieldAccessExpression(expression, name.getName(), LexicalPhrase.combine(expression.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase()));
    }
    if (production == ARRAY_ACCESS_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      Expression dimensionExpression = (Expression) args[2];
      return new ArrayAccessExpression(expression, dimensionExpression, LexicalPhrase.combine(expression.getLexicalPhrase(), (LexicalPhrase) args[1], dimensionExpression.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == FUNCTION_CALL_PRODUCTION)
    {
      return args[0];
    }
    if (production == BRACKETS_PRODUCTION)
    {
      Expression expression = (Expression) args[1];
      return new BracketedExpression(expression, LexicalPhrase.combine((LexicalPhrase) args[0], expression.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    if (production == ARRAY_CREATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Expression> dimensions = (ParseList<Expression>) args[1];
      Type originalType = (Type) args[2];
      ArrayType arrayType = null;
      for (int i = 0; i < dimensions.size(); i++)
      {
        arrayType = new ArrayType(arrayType == null ? originalType : arrayType, null);
      }
      return new ArrayCreationExpression(arrayType, dimensions.toArray(new Expression[dimensions.size()]), null, LexicalPhrase.combine((LexicalPhrase) args[0], dimensions.getLexicalPhrase(), originalType.getLexicalPhrase()));
    }
    if (production == ARRAY_CREATION_EMPTY_LIST_PRODUCTION)
    {
      Type type = (Type) args[3];
      ArrayType arrayType = new ArrayType(type, null);
      return new ArrayCreationExpression(arrayType, null, new Expression[0], LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], type.getLexicalPhrase(), (LexicalPhrase) args[4], (LexicalPhrase) args[5]));
    }
    if (production == ARRAY_CREATION_LIST_PRODUCTION)
    {
      Type type = (Type) args[3];
      @SuppressWarnings("unchecked")
      ParseList<Expression> valueExpressions = (ParseList<Expression>) args[5];
      ArrayType arrayType = new ArrayType(type, null);
      return new ArrayCreationExpression(arrayType, null, valueExpressions.toArray(new Expression[valueExpressions.size()]),
                                         LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], type.getLexicalPhrase(), (LexicalPhrase) args[4], valueExpressions.getLexicalPhrase(), (LexicalPhrase) args[6]));
    }
    throw badTypeList();
  }

}
