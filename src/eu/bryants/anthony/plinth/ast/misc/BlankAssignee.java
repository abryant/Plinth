package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;

/*
 * Created on 6 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class BlankAssignee extends Assignee
{

  public BlankAssignee(LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "_";
  }
}
