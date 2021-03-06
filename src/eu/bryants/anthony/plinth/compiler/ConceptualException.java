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
  private ConceptualException attachedNote;

  /**
   * Creates a new ConceptualException with the specified message.
   * @param message - the message to embed in this exception
   * @param lexicalPhrase - the LexicalPhrase of the conceptual problem
   */
  public ConceptualException(String message, LexicalPhrase lexicalPhrase)
  {
    super(message);
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * Creates a new ConceptualException with the specified message.
   * @param message - the message to embed in this exception
   * @param lexicalPhrase - the LexicalPhrase of the conceptual problem
   * @param attachedNote - a note to add to the description of the problem
   */
  public ConceptualException(String message, LexicalPhrase lexicalPhrase, ConceptualException attachedNote)
  {
    super(message, attachedNote);
    this.lexicalPhrase = lexicalPhrase;
    this.attachedNote = attachedNote;
  }

  /**
   * @return the LexicalPhrase of the conceptual problem
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

  /**
   * @return any note to add to the description of the problem
   */
  public ConceptualException getAttachedNote()
  {
    return attachedNote;
  }
}
