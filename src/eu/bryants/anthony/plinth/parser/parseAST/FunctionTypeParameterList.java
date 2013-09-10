package eu.bryants.anthony.plinth.parser.parseAST;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.DefaultParameter;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 8 Sep 2013
 */

/**
 * @author Anthony Bryant
 */
public class FunctionTypeParameterList
{
  private ParseList<Type> nonDefaultTypes;
  private DefaultParameter[] defaultParameters;
  private LexicalPhrase lexicalPhrase;

  public FunctionTypeParameterList(ParseList<Type> nonDefaultTypes, DefaultParameter[] defaultParameters, LexicalPhrase lexicalPhrase)
  {
    this.nonDefaultTypes = nonDefaultTypes;
    this.defaultParameters = defaultParameters;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the nonDefaultTypes
   */
  public ParseList<Type> getNonDefaultTypes()
  {
    return nonDefaultTypes;
  }

  /**
   * @return the defaultParameters
   */
  public DefaultParameter[] getDefaultParameters()
  {
    return defaultParameters;
  }

  /**
   * @param lexicalPhrase - the lexicalPhrase to set
   */
  public void setLexicalPhrase(LexicalPhrase lexicalPhrase)
  {
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

}
