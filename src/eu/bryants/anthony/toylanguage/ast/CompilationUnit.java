package eu.bryants.anthony.toylanguage.ast;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class CompilationUnit
{
  private Function[] functions;

  private LexicalPhrase lexicalPhrase;

  public CompilationUnit(Function[] functions, LexicalPhrase lexicalPhrase)
  {
    this.functions = functions;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the functions
   */
  public Function[] getFunctions()
  {
    return functions;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }
}
