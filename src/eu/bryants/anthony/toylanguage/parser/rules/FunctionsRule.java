package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.Function;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class FunctionsRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> FUNCTION_PRODUCTION  = new Production<ParseType>(ParseType.FUNCTION);
  private static Production<ParseType> FUNCTIONS_PRODUCTION = new Production<ParseType>(ParseType.FUNCTIONS, ParseType.FUNCTION);

  @SuppressWarnings("unchecked")
  public FunctionsRule()
  {
    super(ParseType.FUNCTIONS, FUNCTION_PRODUCTION, FUNCTIONS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == FUNCTION_PRODUCTION)
    {
      return new Function[] {(Function) args[0]};
    }
    if (production == FUNCTIONS_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Function> functions = (ParseList<Function>) args[0];
      Function newFunction = (Function) args[1];
      functions.addLast(newFunction, LexicalPhrase.combine(functions.getLexicalPhrase(), newFunction.getLexicalPhrase()));
      return functions;
    }
    throw badTypeList();
  }

}
