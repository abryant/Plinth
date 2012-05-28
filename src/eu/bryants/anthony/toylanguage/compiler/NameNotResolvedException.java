package eu.bryants.anthony.toylanguage.compiler;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;

/*
 * Created on 29 Mar 2011
 */

/**
 * An exception that is thrown when resolve() cannot find a matching function or variable for a name.
 * @author Anthony Bryant
 */
public class NameNotResolvedException extends Exception
{
  private static final long serialVersionUID = 1L;

  private LexicalPhrase lexicalPhrase;

  /**
   * Creates a new NameNotResolvedException with the specified message.
   * @param message - the message to embed in this exception
   * @param lexicalPhrase - the LexicalPhrase of the name that could not be resolved
   */
  public NameNotResolvedException(String message, LexicalPhrase lexicalPhrase)
  {
    super(message);
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the lexicalPhrase of the unresolved name
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

}
