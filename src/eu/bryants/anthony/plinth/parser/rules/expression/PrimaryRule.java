package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.ArrayCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 8 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class PrimaryRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> NO_TRAILING_TYPE_PRODUCTION = new Production<ParseType>(ParseType.PRIMARY_NO_TRAILING_TYPE);
  private static Production<ParseType> ARRAY_CREATION_PRODUCTION = new Production<ParseType>(ParseType.NEW_KEYWORD, ParseType.DIMENSIONS, ParseType.TYPE);

  public PrimaryRule()
  {
    super(ParseType.PRIMARY, NO_TRAILING_TYPE_PRODUCTION, ARRAY_CREATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == NO_TRAILING_TYPE_PRODUCTION)
    {
      return args[0];
    }
    if (production == ARRAY_CREATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Expression> dimensions = (ParseList<Expression>) args[1];
      Type originalType = (Type) args[2];
      ArrayType arrayType = null;
      for (int i = 0; i < dimensions.size(); i++)
      {
        arrayType = new ArrayType(false, arrayType == null ? originalType : arrayType, null);
      }
      return new ArrayCreationExpression(arrayType, dimensions.toArray(new Expression[dimensions.size()]), null, LexicalPhrase.combine((LexicalPhrase) args[0], dimensions.getLexicalPhrase(), originalType.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
