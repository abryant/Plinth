package eu.bryants.anthony.toylanguage.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ParametersRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.TYPE, ParseType.NAME);
  private static Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.PARAMETERS, ParseType.COMMA, ParseType.TYPE, ParseType.NAME);

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
      Type type = (Type) args[0];
      Name name = (Name) args[1];
      LexicalPhrase lexicalPhrase = LexicalPhrase.combine(type.getLexicalPhrase(), name.getLexicalPhrase());
      return new ParseList<Parameter>(new Parameter(type, name.getName(), lexicalPhrase), lexicalPhrase);
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[0];
      Type type = (Type) args[2];
      Name name = (Name) args[3];
      LexicalPhrase parameterPhrase = LexicalPhrase.combine(type.getLexicalPhrase(), name.getLexicalPhrase());
      parameters.addLast(new Parameter(type, name.getName(), parameterPhrase), LexicalPhrase.combine(parameters.getLexicalPhrase(), (LexicalPhrase) args[1], parameterPhrase));
      return parameters;
    }
    throw badTypeList();
  }

}
