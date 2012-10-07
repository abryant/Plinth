package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 4 May 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class Assignee
{
  private Type resolvedType;

  private LexicalPhrase lexicalPhrase;

  public Assignee(LexicalPhrase lexicalPhrase)
  {
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the resolvedType
   */
  public Type getResolvedType()
  {
    return resolvedType;
  }

  /**
   * @param resolvedType - the resolvedType to set
   */
  public void setResolvedType(Type resolvedType)
  {
    this.resolvedType = resolvedType;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }
}
