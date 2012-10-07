package eu.bryants.anthony.toylanguage.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.toylanguage.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.BlankAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.FieldAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.QName;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.QNameElement;

/*
 * Created on 30 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class AssigneeNoQNameRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> ARRAY_PRODUCTION = new Production<ParseType>(ParseType.PRIMARY, ParseType.LSQUARE, ParseType.EXPRESSION, ParseType.RSQUARE);
  private static final Production<ParseType> QNAME_ARRAY_PRODUCTION = new Production<ParseType>(ParseType.QNAME, ParseType.LSQUARE, ParseType.EXPRESSION, ParseType.RSQUARE);
  private static final Production<ParseType> NESTED_QNAME_LIST_ARRAY_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.LSQUARE, ParseType.EXPRESSION, ParseType.RSQUARE);
  private static final Production<ParseType> FIELD_PRODUCTION = new Production<ParseType>(ParseType.PRIMARY_NO_TRAILING_TYPE, ParseType.DOT, ParseType.NAME);
  private static final Production<ParseType> TYPE_FIELD_PRODUCTION = new Production<ParseType>(ParseType.TYPE, ParseType.DOUBLE_COLON, ParseType.NAME);
  private static final Production<ParseType> UNDERSCORE_PRODUCTION = new Production<ParseType>(ParseType.UNDERSCORE);

  @SuppressWarnings("unchecked")
  public AssigneeNoQNameRule()
  {
    super(ParseType.ASSIGNEE_NO_QNAME, ARRAY_PRODUCTION, QNAME_ARRAY_PRODUCTION, NESTED_QNAME_LIST_ARRAY_PRODUCTION, FIELD_PRODUCTION, TYPE_FIELD_PRODUCTION, UNDERSCORE_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == ARRAY_PRODUCTION)
    {
      Expression arrayExpression = (Expression) args[0];
      Expression dimensionExpression = (Expression) args[2];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(arrayExpression.getLexicalPhrase(), (LexicalPhrase) args[1], dimensionExpression.getLexicalPhrase(), (LexicalPhrase) args[3]);
      return new ArrayElementAssignee(arrayExpression, dimensionExpression, lexicalPhrase);
    }
    if (production == QNAME_ARRAY_PRODUCTION)
    {
      QName qname = (QName) args[0];
      Expression arrayExpression = new QNameElement(qname, qname.getLexicalPhrase()).convertToExpression();
      Expression dimensionExpression = (Expression) args[2];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(arrayExpression.getLexicalPhrase(), (LexicalPhrase) args[1], dimensionExpression.getLexicalPhrase(), (LexicalPhrase) args[3]);
      return new ArrayElementAssignee(arrayExpression, dimensionExpression, lexicalPhrase);
    }
    if (production == NESTED_QNAME_LIST_ARRAY_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      Expression arrayExpression = element.convertToExpression();
      Expression dimensionExpression = (Expression) args[2];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(arrayExpression.getLexicalPhrase(), (LexicalPhrase) args[1], dimensionExpression.getLexicalPhrase(), (LexicalPhrase) args[3]);
      return new ArrayElementAssignee(arrayExpression, dimensionExpression, lexicalPhrase);
    }
    if (production == FIELD_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      Name name = (Name) args[2];
      FieldAccessExpression fieldAccess = new FieldAccessExpression(expression, false, name.getName(), LexicalPhrase.combine(expression.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase()));
      return new FieldAssignee(fieldAccess);
    }
    if (production == TYPE_FIELD_PRODUCTION)
    {
      Type type = (Type) args[0];
      Name name = (Name) args[2];
      FieldAccessExpression fieldAccess = new FieldAccessExpression(type, name.getName(), LexicalPhrase.combine(type.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase()));
      return new FieldAssignee(fieldAccess);
    }
    if (production == UNDERSCORE_PRODUCTION)
    {
      return new BlankAssignee((LexicalPhrase) args[0]);
    }
    throw badTypeList();
  }

}
