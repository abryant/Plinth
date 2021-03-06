package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.ArrayAccessExpression;
import eu.bryants.anthony.plinth.ast.expression.ArrayCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.BooleanLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.BracketedExpression;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.plinth.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.NullLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.ObjectCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.StringLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.SuperVariableExpression;
import eu.bryants.anthony.plinth.ast.misc.Argument;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.terminal.FloatingLiteral;
import eu.bryants.anthony.plinth.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.terminal.StringLiteral;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class PrimaryNotThisRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> INTEGER_PRODUCTION  = new Production<ParseType>(ParseType.INTEGER_LITERAL);
  private static Production<ParseType> FLOATING_PRODUCTION = new Production<ParseType>(ParseType.FLOATING_LITERAL);
  private static Production<ParseType> TRUE_PRODUCTION     = new Production<ParseType>(ParseType.TRUE_KEYWORD);
  private static Production<ParseType> FALSE_PRODUCTION    = new Production<ParseType>(ParseType.FALSE_KEYWORD);
  private static Production<ParseType> NULL_PRODUCTION     = new Production<ParseType>(ParseType.NULL_KEYWORD);
  private static Production<ParseType> ARRAY_ACCESS_PRODUCTION                   = new Production<ParseType>(ParseType.PRIMARY,           ParseType.LSQUARE, ParseType.EXPRESSION, ParseType.RSQUARE);
  private static Production<ParseType> QNAME_ARRAY_ACCESS_PRODUCTION             = new Production<ParseType>(ParseType.QNAME,             ParseType.LSQUARE, ParseType.EXPRESSION, ParseType.RSQUARE);
  private static Production<ParseType> NESTED_QNAME_LIST_ARRAY_ACCESS_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.LSQUARE, ParseType.EXPRESSION, ParseType.RSQUARE);
  private static Production<ParseType> ARRAY_CREATION_EMPTY_LIST_PRODUCTION  = new Production<ParseType>(ParseType.NEW_KEYWORD, ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE, ParseType.LBRACE, ParseType.RBRACE);
  private static Production<ParseType> ARRAY_CREATION_LIST_PRODUCTION        = new Production<ParseType>(ParseType.NEW_KEYWORD, ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE, ParseType.LBRACE, ParseType.EXPRESSION_LIST, ParseType.RBRACE);
  private static Production<ParseType> ARRAY_CREATION_INITIALISER_PRODUCTION = new Production<ParseType>(ParseType.NEW_KEYWORD, ParseType.DIMENSIONS, ParseType.TYPE, ParseType.LPAREN, ParseType.EXPRESSION, ParseType.RPAREN);
  private static Production<ParseType> SUPER_ACCESS_PRODUCTION                       = new Production<ParseType>(ParseType.SUPER_KEYWORD,            ParseType.DOT,               ParseType.NAME);
  private static Production<ParseType> FIELD_ACCESS_PRODUCTION                       = new Production<ParseType>(ParseType.PRIMARY,                  ParseType.DOT,               ParseType.NAME);
  private static Production<ParseType> NESTED_QNAME_LIST_FIELD_ACCESS_PRODUCTION     = new Production<ParseType>(ParseType.NESTED_QNAME_LIST,        ParseType.DOT,               ParseType.NAME);
  private static Production<ParseType> NULL_TRAVERSING_FIELD_ACCESS_PRODUCTION       = new Production<ParseType>(ParseType.PRIMARY,                  ParseType.QUESTION_MARK_DOT, ParseType.NAME);
  private static Production<ParseType> QNAME_NULL_TRAVERSING_FIELD_ACCESS_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION,         ParseType.QUESTION_MARK_DOT, ParseType.NAME);
  private static Production<ParseType> TYPE_FIELD_ACCESS_PRODUCTION                  = new Production<ParseType>(ParseType.TYPE_NO_SIMPLE_ARGUMENTS, ParseType.DOUBLE_COLON,      ParseType.NAME);
  private static Production<ParseType> FUNCTION_CALL_PRODUCTION = new Production<ParseType>(ParseType.FUNCTION_CALL_EXPRESSION);
  private static Production<ParseType> BRACKETS_PRODUCTION = new Production<ParseType>(ParseType.LPAREN, ParseType.TUPLE_EXPRESSION, ParseType.RPAREN);
  private static Production<ParseType> CREATION_PRODUCTION = new Production<ParseType>(ParseType.CREATION_EXPRESSION);
  private static Production<ParseType> OBJECT_CREATION_PRODUCTION = new Production<ParseType>(ParseType.NEW_KEYWORD, ParseType.OBJECT_KEYWORD, ParseType.ARGUMENTS);
  private static Production<ParseType> STRING_LITERAL_PRODUCTION = new Production<ParseType>(ParseType.STRING_LITERAL);

  public PrimaryNotThisRule()
  {
    super(ParseType.PRIMARY_NOT_THIS, INTEGER_PRODUCTION, FLOATING_PRODUCTION,
                                      TRUE_PRODUCTION, FALSE_PRODUCTION,
                                      NULL_PRODUCTION,
                                      ARRAY_ACCESS_PRODUCTION, QNAME_ARRAY_ACCESS_PRODUCTION, NESTED_QNAME_LIST_ARRAY_ACCESS_PRODUCTION,
                                      ARRAY_CREATION_EMPTY_LIST_PRODUCTION, ARRAY_CREATION_LIST_PRODUCTION, ARRAY_CREATION_INITIALISER_PRODUCTION,
                                      SUPER_ACCESS_PRODUCTION,
                                      FIELD_ACCESS_PRODUCTION, NESTED_QNAME_LIST_FIELD_ACCESS_PRODUCTION, NULL_TRAVERSING_FIELD_ACCESS_PRODUCTION, QNAME_NULL_TRAVERSING_FIELD_ACCESS_PRODUCTION, TYPE_FIELD_ACCESS_PRODUCTION,
                                      FUNCTION_CALL_PRODUCTION,
                                      BRACKETS_PRODUCTION,
                                      CREATION_PRODUCTION,
                                      OBJECT_CREATION_PRODUCTION,
                                      STRING_LITERAL_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == INTEGER_PRODUCTION)
    {
      IntegerLiteral literal = (IntegerLiteral) args[0];
      return new IntegerLiteralExpression(literal, literal.getLexicalPhrase());
    }
    if (production == FLOATING_PRODUCTION)
    {
      FloatingLiteral literal = (FloatingLiteral) args[0];
      return new FloatingLiteralExpression(literal, literal.getLexicalPhrase());
    }
    if (production == TRUE_PRODUCTION)
    {
      return new BooleanLiteralExpression(true, (LexicalPhrase) args[0]);
    }
    if (production == FALSE_PRODUCTION)
    {
      return new BooleanLiteralExpression(false, (LexicalPhrase) args[0]);
    }
    if (production == NULL_PRODUCTION)
    {
      return new NullLiteralExpression((LexicalPhrase) args[0]);
    }
    if (production == ARRAY_ACCESS_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      Expression dimensionExpression = (Expression) args[2];
      return new ArrayAccessExpression(expression, dimensionExpression, LexicalPhrase.combine(expression.getLexicalPhrase(), (LexicalPhrase) args[1], dimensionExpression.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == QNAME_ARRAY_ACCESS_PRODUCTION)
    {
      QName qname = (QName) args[0];
      Expression expression = new QNameElement(qname, qname.getLexicalPhrase()).convertToExpression();
      Expression dimensionExpression = (Expression) args[2];
      return new ArrayAccessExpression(expression, dimensionExpression, LexicalPhrase.combine(expression.getLexicalPhrase(), (LexicalPhrase) args[1], dimensionExpression.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == NESTED_QNAME_LIST_ARRAY_ACCESS_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      Expression expression = element.convertToExpression();
      Expression dimensionExpression = (Expression) args[2];
      return new ArrayAccessExpression(expression, dimensionExpression, LexicalPhrase.combine(expression.getLexicalPhrase(), (LexicalPhrase) args[1], dimensionExpression.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == ARRAY_CREATION_EMPTY_LIST_PRODUCTION)
    {
      Type type = (Type) args[3];
      ArrayType arrayType = new ArrayType(false, false, type, null);
      return new ArrayCreationExpression(arrayType, null, new Expression[0], null, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], type.getLexicalPhrase(), (LexicalPhrase) args[4], (LexicalPhrase) args[5]));
    }
    if (production == ARRAY_CREATION_LIST_PRODUCTION)
    {
      Type type = (Type) args[3];
      @SuppressWarnings("unchecked")
      ParseList<Expression> valueExpressions = (ParseList<Expression>) args[5];
      ArrayType arrayType = new ArrayType(false, false, type, null);
      return new ArrayCreationExpression(arrayType, null, valueExpressions.toArray(new Expression[valueExpressions.size()]), null,
                                         LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], type.getLexicalPhrase(), (LexicalPhrase) args[4], valueExpressions.getLexicalPhrase(), (LexicalPhrase) args[6]));
    }
    if (production == ARRAY_CREATION_INITIALISER_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Expression> dimensionExpressions = (ParseList<Expression>) args[1];
      Type originalType = (Type) args[2];
      Expression initialiserExpression = (Expression) args[4];
      ArrayType arrayType = null;
      for (int i = 0; i < dimensionExpressions.size(); i++)
      {
        arrayType = new ArrayType(false, false, arrayType == null ? originalType : arrayType, null);
      }
      return new ArrayCreationExpression(arrayType, dimensionExpressions.toArray(new Expression[dimensionExpressions.size()]), null, initialiserExpression,
                                         LexicalPhrase.combine((LexicalPhrase) args[0], dimensionExpressions.getLexicalPhrase(), originalType.getLexicalPhrase(), (LexicalPhrase) args[3], initialiserExpression.getLexicalPhrase(), (LexicalPhrase) args[5]));
    }
    if (production == SUPER_ACCESS_PRODUCTION)
    {
      Name name = (Name) args[2];
      return new SuperVariableExpression(name.getName(), LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], name.getLexicalPhrase()));
    }
    if (production == FIELD_ACCESS_PRODUCTION || production == NESTED_QNAME_LIST_FIELD_ACCESS_PRODUCTION || production == NULL_TRAVERSING_FIELD_ACCESS_PRODUCTION || production == QNAME_NULL_TRAVERSING_FIELD_ACCESS_PRODUCTION)
    {
      Expression expression;
      if (production == NESTED_QNAME_LIST_FIELD_ACCESS_PRODUCTION)
      {
        QNameElement element = (QNameElement) args[0];
        expression = element.convertToExpression();
      }
      else
      {
        expression =  (Expression) args[0];
      }
      boolean nullTraversing = production == NULL_TRAVERSING_FIELD_ACCESS_PRODUCTION || production == QNAME_NULL_TRAVERSING_FIELD_ACCESS_PRODUCTION;
      Name name = (Name) args[2];
      return new FieldAccessExpression(expression, nullTraversing, name.getName(), LexicalPhrase.combine(expression.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase()));
    }
    if (production == TYPE_FIELD_ACCESS_PRODUCTION)
    {
      Type type = (Type) args[0];
      Name name = (Name) args[2];
      return new FieldAccessExpression(type, name.getName(), LexicalPhrase.combine(type.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase()));
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
    if (production == CREATION_PRODUCTION)
    {
      return args[0];
    }
    if (production == OBJECT_CREATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Argument> argumentsList = (ParseList<Argument>) args[2];
      return new ObjectCreationExpression(argumentsList.toArray(new Argument[argumentsList.size()]), LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], argumentsList.getLexicalPhrase()));
    }
    if (production == STRING_LITERAL_PRODUCTION)
    {
      StringLiteral literal = (StringLiteral) args[0];
      return new StringLiteralExpression(literal, literal.getLexicalPhrase());
    }
    throw badTypeList();
  }

}
