package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.DefaultParameter;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 8 Sep 2013
 */

/**
 * @author Anthony Bryant
 */
public class FunctionTypeDefaultParameterListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.FUNCTION_TYPE_DEFAULT_PARAMETER);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.FUNCTION_TYPE_DEFAULT_PARAMETER_LIST, ParseType.COMMA, ParseType.FUNCTION_TYPE_DEFAULT_PARAMETER);

  public FunctionTypeDefaultParameterListRule()
  {
    super(ParseType.FUNCTION_TYPE_DEFAULT_PARAMETER_LIST, START_PRODUCTION, CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      DefaultParameter defaultParameter = (DefaultParameter) args[0];
      return new ParseList<DefaultParameter>(defaultParameter, defaultParameter.getLexicalPhrase());
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<DefaultParameter> list = (ParseList<DefaultParameter>) args[0];
      DefaultParameter defaultParameter = (DefaultParameter) args[2];
      list.addLast(defaultParameter, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], defaultParameter.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
