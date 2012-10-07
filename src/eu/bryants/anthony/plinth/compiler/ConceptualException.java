package eu.bryants.anthony.plinth.compiler;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;

/*
 * Created on 3 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ConceptualException extends Exception
{
  private static final long serialVersionUID = 1L;

  private LexicalPhrase lexicalPhrase;

  /**
   * Creates a new ConceptualException with the specified message.
   * @param message - the message to embed in this exception
   * @param lexicalPhrase - the LexicalPhrase of the name that could not be resolved
   */
  public ConceptualException(String message, LexicalPhrase lexicalPhrase)
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
