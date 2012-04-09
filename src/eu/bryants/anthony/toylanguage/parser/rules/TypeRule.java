package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.PrimitiveType;
import eu.bryants.anthony.toylanguage.ast.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class TypeRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> BOOLEAN_PRODUCTION = new Production<ParseType>(ParseType.BOOLEAN_KEYWORD);
  private static final Production<ParseType> DOUBLE_PRODUCTION = new Production<ParseType>(ParseType.DOUBLE_KEYWORD);
  private static final Production<ParseType> INT_PRODUCTION = new Production<ParseType>(ParseType.INT_KEYWORD);

  @SuppressWarnings("unchecked")
  public TypeRule()
  {
    super(ParseType.TYPE, BOOLEAN_PRODUCTION, DOUBLE_PRODUCTION, INT_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == BOOLEAN_PRODUCTION)
    {
      return new PrimitiveType(PrimitiveTypeType.BOOLEAN, (LexicalPhrase) args[0]);
    }
    if (production == DOUBLE_PRODUCTION)
    {
      return new PrimitiveType(PrimitiveTypeType.DOUBLE, (LexicalPhrase) args[0]);
    }
    if (production == INT_PRODUCTION)
    {
      return new PrimitiveType(PrimitiveTypeType.INT, (LexicalPhrase) args[0]);
    }
    throw badTypeList();
  }

}
