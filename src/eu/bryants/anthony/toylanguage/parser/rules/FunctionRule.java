package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.Function;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.ast.type.VoidType;
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

  private static final Production<ParseType> PARAMS_PRODUCTION      = new Production<ParseType>(ParseType.TYPE,         ParseType.NAME, ParseType.LPAREN, ParseType.PARAMETERS, ParseType.RPAREN, ParseType.BLOCK);
  private static final Production<ParseType> PRODUCTION             = new Production<ParseType>(ParseType.TYPE,         ParseType.NAME, ParseType.LPAREN,                       ParseType.RPAREN, ParseType.BLOCK);
  private static final Production<ParseType> VOID_PARAMS_PRODUCTION = new Production<ParseType>(ParseType.VOID_KEYWORD, ParseType.NAME, ParseType.LPAREN, ParseType.PARAMETERS, ParseType.RPAREN, ParseType.BLOCK);
  private static final Production<ParseType> VOID_PRODUCTION        = new Production<ParseType>(ParseType.VOID_KEYWORD, ParseType.NAME, ParseType.LPAREN,                       ParseType.RPAREN, ParseType.BLOCK);

  @SuppressWarnings("unchecked")
  public FunctionRule()
  {
    super(ParseType.FUNCTION, PARAMS_PRODUCTION, PRODUCTION, VOID_PARAMS_PRODUCTION, VOID_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PARAMS_PRODUCTION)
    {
      Type returnType = (Type) args[0];
      Name name = (Name) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[3];
      Block block = (Block) args[5];
      return new Function(returnType, name.getName(), parameters.toArray(new Parameter[parameters.size()]), block,
                          LexicalPhrase.combine(returnType.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2], parameters.getLexicalPhrase(), (LexicalPhrase) args[4], block.getLexicalPhrase()));
    }
    if (production == PRODUCTION)
    {
      Type returnType = (Type) args[0];
      Name name = (Name) args[1];
      Block block = (Block) args[4];
      return new Function(returnType, name.getName(), new Parameter[0], block,
                          LexicalPhrase.combine(returnType.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2], (LexicalPhrase) args[3], block.getLexicalPhrase()));
    }
    if (production == VOID_PARAMS_PRODUCTION)
    {
      Name name = (Name) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[3];
      Block block = (Block) args[5];
      return new Function(new VoidType((LexicalPhrase) args[0]), name.getName(), parameters.toArray(new Parameter[parameters.size()]), block,
                          LexicalPhrase.combine((LexicalPhrase) args[0], name.getLexicalPhrase(), (LexicalPhrase) args[2], parameters.getLexicalPhrase(), (LexicalPhrase) args[4], block.getLexicalPhrase()));
    }
    if (production == VOID_PRODUCTION)
    {
      Name name = (Name) args[1];
      Block block = (Block) args[4];
      return new Function(new VoidType((LexicalPhrase) args[0]), name.getName(), new Parameter[0], block,
                          LexicalPhrase.combine((LexicalPhrase) args[0], name.getLexicalPhrase(), (LexicalPhrase) args[2], (LexicalPhrase) args[3], block.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
