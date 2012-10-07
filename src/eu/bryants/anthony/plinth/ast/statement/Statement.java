package eu.bryants.anthony.plinth.ast.statement;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class Statement
{
  private LexicalPhrase lexicalPhrase;

  public Statement(LexicalPhrase lexicalPhrase)
  {
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * This method checks whether the statement stops execution.
   * It should not be called until after the control flow checker has been run over this statement.
   * @return true if this Statement stops execution so that any statements after it are not executed, false if execution continues after it
   */
  public abstract boolean stopsExecution();

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }
}
