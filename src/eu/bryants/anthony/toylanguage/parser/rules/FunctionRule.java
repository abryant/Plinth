package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.Function;
import eu.bryants.anthony.toylanguage.ast.Parameter;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.ast.type.Type;
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

  private static Production<ParseType> PRODUCTION               = new Production<ParseType>(ParseType.TYPE, ParseType.NAME, ParseType.LPAREN, ParseType.PARAMETERS, ParseType.RPAREN, ParseType.BLOCK);
  private static Production<ParseType> NO_PARAMETERS_PRODUCTION = new Production<ParseType>(ParseType.TYPE, ParseType.NAME, ParseType.LPAREN,                       ParseType.RPAREN, ParseType.BLOCK);

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
      Type type = (Type) args[0];
      Name name = (Name) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[3];
      Block block = (Block) args[5];
      return new Function(type,
                          name.getName(),
                          parameters.toArray(new Parameter[parameters.size()]),
                          block,
                          LexicalPhrase.combine(type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2], parameters.getLexicalPhrase(), (LexicalPhrase) args[4],
                                                block.getLexicalPhrase()));
    }
    if (production == NO_PARAMETERS_PRODUCTION)
    {
      Type type = (Type) args[0];
      Name name = (Name) args[1];
      Block block = (Block) args[4];
      return new Function(type,
                          name.getName(),
                          new Parameter[0],
                          block,
                          LexicalPhrase.combine(type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2], (LexicalPhrase) args[3],
                                                block.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
