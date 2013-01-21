package eu.bryants.anthony.plinth.compiler;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;

/*
 * Created on 29 Mar 2011
 */

/**
 * An exception that is thrown when resolve() cannot find a matching function or variable for a name.
 * @author Anthony Bryant
 */
public class NameNotResolvedException extends ConceptualException
{
  private static final long serialVersionUID = 1L;

  /**
   * Creates a new NameNotResolvedException with the specified message.
   * @param message - the message to embed in this exception
   * @param lexicalPhrase - the LexicalPhrase of the name that could not be resolved
   */
  public NameNotResolvedException(String message, LexicalPhrase lexicalPhrase)
  {
    super(message, lexicalPhrase);
  }

}
