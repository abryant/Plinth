package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseContainer;

/*
 * Created on 23 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeArgumentRAngleRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.TYPE_RANGLE);
  private static final Production<ParseType> WILDCARD_PRODUCTION = new Production<ParseType>(ParseType.WILDCARD_TYPE_ARGUMENT_RANGLE);

  public TypeArgumentRAngleRule()
  {
    super(ParseType.TYPE_ARGUMENT_RANGLE, PRODUCTION, WILDCARD_PRODUCTION);
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
      ParseContainer<Type> containedType = (ParseContainer<Type>) args[0];
      Type typeArgument = containedType.getItem();
      return new ParseContainer<Type>(typeArgument, containedType.getLexicalPhrase());
    }
    if (production == WILDCARD_PRODUCTION)
    {
      return args[0];
    }
    throw badTypeList();
  }

}
