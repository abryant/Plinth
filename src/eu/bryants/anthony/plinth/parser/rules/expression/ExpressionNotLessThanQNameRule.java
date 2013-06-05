package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.ArrayCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.InstanceOfExpression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression.RelationalOperator;
import eu.bryants.anthony.plinth.ast.misc.QName;
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
public class ExpressionNotLessThanQNameRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> NORMAL_PRODUCTION                       = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> LESS_THAN_PRODUCTION                    = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.LANGLE,                   ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> LESS_THAN_QNAME_PRODUCTION              = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.LANGLE,                   ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_LESS_THAN_PRODUCTION              = new Production<ParseType>(ParseType.QNAME,               ParseType.LANGLE,                   ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> NESTED_QNAME_LESS_THAN_PRODUCTION       = new Production<ParseType>(ParseType.NESTED_QNAME_LIST,   ParseType.LANGLE,                   ParseType.ADDITIVE_EXPRESSION);
  // QNameExpression LAngle QNameExpression is deliberately missing, as it causes conflicts with generics
  private static final Production<ParseType> LESS_THAN_EQUAL_PRODUCTION              = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.LANGLE_EQUALS,            ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> LESS_THAN_EQUAL_QNAME_PRODUCTION        = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.LANGLE_EQUALS,            ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_LESS_THAN_EQUAL_PRODUCTION        = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.LANGLE_EQUALS,            ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> QNAME_LESS_THAN_EQUAL_QNAME_PRODUCTION  = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.LANGLE_EQUALS,            ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> MORE_THAN_PRODUCTION                    = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.RANGLE,                   ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> MORE_THAN_QNAME_PRODUCTION              = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.RANGLE,                   ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_MORE_THAN_PRODUCTION              = new Production<ParseType>(ParseType.QNAME,               ParseType.RANGLE,                   ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> NESTED_QNAME_MORE_THAN_PRODUCTION       = new Production<ParseType>(ParseType.NESTED_QNAME_LIST,   ParseType.RANGLE,                   ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> QNAME_MORE_THAN_QNAME_PRODUCTION        = new Production<ParseType>(ParseType.QNAME,               ParseType.RANGLE,                   ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> NESTED_QNAME_MORE_THAN_QNAME_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST,   ParseType.RANGLE,                   ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> MORE_THAN_EQUAL_PRODUCTION              = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.RANGLE_EQUALS,            ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> MORE_THAN_EQUAL_QNAME_PRODUCTION        = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.RANGLE_EQUALS,            ParseType.QNAME_EXPRESSION);
  private static final Production<ParseType> QNAME_MORE_THAN_EQUAL_PRODUCTION        = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.RANGLE_EQUALS,            ParseType.ADDITIVE_EXPRESSION);
  private static final Production<ParseType> QNAME_MORE_THAN_EQUAL_QNAME_PRODUCTION  = new Production<ParseType>(ParseType.QNAME_EXPRESSION,    ParseType.RANGLE_EQUALS,            ParseType.QNAME_EXPRESSION);

  private static final Production<ParseType> INSTANCEOF_PRODUCTION = new Production<ParseType>(ParseType.ADDITIVE_EXPRESSION, ParseType.INSTANCEOF_KEYWORD, ParseType.TYPE);
  private static final Production<ParseType> QNAME_INSTANCEOF_PRODUCTION = new Production<ParseType>(ParseType.QNAME_EXPRESSION, ParseType.INSTANCEOF_KEYWORD, ParseType.TYPE);

  private static final Production<ParseType> ARRAY_CREATION_PRODUCTION = new Production<ParseType>(ParseType.NEW_KEYWORD, ParseType.DIMENSIONS, ParseType.TYPE);

  public ExpressionNotLessThanQNameRule()
  {
    super(ParseType.EXPRESSION_NOT_LESS_THAN_QNAME, NORMAL_PRODUCTION,
                                                    LESS_THAN_PRODUCTION,       LESS_THAN_QNAME_PRODUCTION,
                                                    QNAME_LESS_THAN_PRODUCTION, NESTED_QNAME_LESS_THAN_PRODUCTION,
                                                    LESS_THAN_EQUAL_PRODUCTION, LESS_THAN_EQUAL_QNAME_PRODUCTION, QNAME_LESS_THAN_EQUAL_PRODUCTION, QNAME_LESS_THAN_EQUAL_QNAME_PRODUCTION,
                                                    MORE_THAN_PRODUCTION,       MORE_THAN_QNAME_PRODUCTION,
                                                    QNAME_MORE_THAN_PRODUCTION, NESTED_QNAME_MORE_THAN_PRODUCTION, QNAME_MORE_THAN_QNAME_PRODUCTION, NESTED_QNAME_MORE_THAN_QNAME_PRODUCTION,
                                                    MORE_THAN_EQUAL_PRODUCTION, MORE_THAN_EQUAL_QNAME_PRODUCTION, QNAME_MORE_THAN_EQUAL_PRODUCTION, QNAME_MORE_THAN_EQUAL_QNAME_PRODUCTION,
                                                    INSTANCEOF_PRODUCTION, QNAME_INSTANCEOF_PRODUCTION,
                                                    ARRAY_CREATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == NORMAL_PRODUCTION)
    {
      return args[0];
    }
    if (production == INSTANCEOF_PRODUCTION || production == QNAME_INSTANCEOF_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      Type type = (Type) args[2];
      return new InstanceOfExpression(expression, type, LexicalPhrase.combine(expression.getLexicalPhrase(), (LexicalPhrase) args[1], type.getLexicalPhrase()));
    }
    if (production == ARRAY_CREATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Expression> dimensions = (ParseList<Expression>) args[1];
      Type originalType = (Type) args[2];
      ArrayType arrayType = null;
      for (int i = 0; i < dimensions.size(); i++)
      {
        arrayType = new ArrayType(false, false, arrayType == null ? originalType : arrayType, null);
      }
      return new ArrayCreationExpression(arrayType, dimensions.toArray(new Expression[dimensions.size()]), null, LexicalPhrase.combine((LexicalPhrase) args[0], dimensions.getLexicalPhrase(), originalType.getLexicalPhrase()));
    }
    RelationalOperator operator;
    Expression left;
    if (production == LESS_THAN_PRODUCTION || production == LESS_THAN_QNAME_PRODUCTION)
    {
      operator = RelationalOperator.LESS_THAN;
      left = (Expression) args[0];
    }
    else if (production == QNAME_LESS_THAN_PRODUCTION)
    {
      operator = RelationalOperator.LESS_THAN;
      QName qname = (QName) args[0];
      left = new QNameElement(qname, qname.getLexicalPhrase()).convertToExpression();
    }
    else if (production == NESTED_QNAME_LESS_THAN_PRODUCTION)
    {
      operator = RelationalOperator.LESS_THAN;
      QNameElement element = (QNameElement) args[0];
      left = element.convertToExpression();
    }
    else if (production == LESS_THAN_EQUAL_PRODUCTION       || production == LESS_THAN_EQUAL_QNAME_PRODUCTION ||
             production == QNAME_LESS_THAN_EQUAL_PRODUCTION || production == QNAME_LESS_THAN_EQUAL_QNAME_PRODUCTION)
    {
      operator = RelationalOperator.LESS_THAN_EQUAL;
      left = (Expression) args[0];
    }
    else if (production == MORE_THAN_PRODUCTION       || production == MORE_THAN_QNAME_PRODUCTION)
    {
      operator = RelationalOperator.MORE_THAN;
      left = (Expression) args[0];
    }
    else if (production == QNAME_MORE_THAN_PRODUCTION || production == QNAME_MORE_THAN_QNAME_PRODUCTION)
    {
      operator = RelationalOperator.MORE_THAN;
      QName qname = (QName) args[0];
      left = new QNameElement(qname, qname.getLexicalPhrase()).convertToExpression();
    }
    else if (production == NESTED_QNAME_MORE_THAN_PRODUCTION || production == NESTED_QNAME_MORE_THAN_QNAME_PRODUCTION)
    {
      operator = RelationalOperator.MORE_THAN;
      QNameElement element = (QNameElement) args[0];
      left = element.convertToExpression();
    }
    else if (production == MORE_THAN_EQUAL_PRODUCTION       || production == MORE_THAN_EQUAL_QNAME_PRODUCTION ||
             production == QNAME_MORE_THAN_EQUAL_PRODUCTION || production == QNAME_MORE_THAN_EQUAL_QNAME_PRODUCTION)
    {
      operator = RelationalOperator.MORE_THAN_EQUAL;
      left = (Expression) args[0];
    }
    else
    {
      throw badTypeList();
    }
    Expression right = (Expression) args[2];
    LexicalPhrase lexicalPhrase = LexicalPhrase.combine(left.getLexicalPhrase(), (LexicalPhrase) args[1], right.getLexicalPhrase());
    return new RelationalExpression(left, right, operator, lexicalPhrase);
  }

}
