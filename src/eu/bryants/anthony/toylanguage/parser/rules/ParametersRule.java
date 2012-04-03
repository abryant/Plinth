package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.Name;
import eu.bryants.anthony.toylanguage.ast.Parameter;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ParametersRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.NAME);
  private static Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.PARAMETERS, ParseType.COMMA, ParseType.NAME);

  @SuppressWarnings("unchecked")
  public ParametersRule()
  {
    super(ParseType.PARAMETERS, START_PRODUCTION, CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      Name name = (Name) args[0];
      return new ParseList<Parameter>(new Parameter(name.getName(), name.getLexicalPhrase()), name.getLexicalPhrase());
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[0];
      Name name = (Name) args[2];
      parameters.addLast(new Parameter(name.getName(), name.getLexicalPhrase()), LexicalPhrase.combine(parameters.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase()));
      return parameters;
    }
    throw badTypeList();
  }

}
