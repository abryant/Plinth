package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.Expression;
import eu.bryants.anthony.toylanguage.ast.Function;
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
public class FunctionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> PRODUCTION               = new Production<ParseType>(ParseType.NAME, ParseType.LPAREN, ParseType.PARAMETERS, ParseType.RPAREN, ParseType.COLON, ParseType.EXPRESSION, ParseType.SEMICOLON);
  private static Production<ParseType> NO_PARAMETERS_PRODUCTION = new Production<ParseType>(ParseType.NAME, ParseType.LPAREN,                       ParseType.RPAREN, ParseType.COLON, ParseType.EXPRESSION, ParseType.SEMICOLON);

  @SuppressWarnings("unchecked")
  public FunctionRule()
  {
    super(ParseType.FUNCTION, PRODUCTION, NO_PARAMETERS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      Name name = (Name) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[2];
      Expression expression = (Expression) args[5];
      return new Function(name.getName(), parameters.toArray(new Parameter[parameters.size()]), expression, LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], parameters.getLexicalPhrase(), (LexicalPhrase) args[3],
                                                                                                                                  (LexicalPhrase) args[4], expression.getLexicalPhrase(), (LexicalPhrase) args[6]));
    }
    if (production == NO_PARAMETERS_PRODUCTION)
    {
      Name name = (Name) args[0];
      Expression expression = (Expression) args[4];
      return new Function(name.getName(), new Parameter[0], expression, LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], (LexicalPhrase) args[2],
                                                                                              (LexicalPhrase) args[3], expression.getLexicalPhrase(), (LexicalPhrase) args[5]));
    }
    throw badTypeList();
  }

}
