package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.Block;
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

  private static Production<ParseType> PRODUCTION               = new Production<ParseType>(ParseType.NAME, ParseType.LPAREN, ParseType.PARAMETERS, ParseType.RPAREN, ParseType.BLOCK);
  private static Production<ParseType> NO_PARAMETERS_PRODUCTION = new Production<ParseType>(ParseType.NAME, ParseType.LPAREN,                       ParseType.RPAREN, ParseType.BLOCK);

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
      Block block = (Block) args[4];
      return new Function(name.getName(),
                          parameters.toArray(new Parameter[parameters.size()]),
                          block,
                          LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], parameters.getLexicalPhrase(), (LexicalPhrase) args[3],
                                                block.getLexicalPhrase()));
    }
    if (production == NO_PARAMETERS_PRODUCTION)
    {
      Name name = (Name) args[0];
      Block block = (Block) args[3];
      return new Function(name.getName(),
                          new Parameter[0],
                          block,
                          LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], (LexicalPhrase) args[2],
                                                block.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
