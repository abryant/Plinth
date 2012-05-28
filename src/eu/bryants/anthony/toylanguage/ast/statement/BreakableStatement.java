package eu.bryants.anthony.toylanguage.ast.statement;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;

/*
 * Created on 13 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class BreakableStatement extends Statement
{

  private boolean brokenOutOf = false;
  private boolean continuedThrough = false;

  public BreakableStatement(LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
  }

  /**
   * @return true if this loop statement is broken out of via a break statement
   */
  public boolean isBrokenOutOf()
  {
    return brokenOutOf;
  }

  /**
   * @param brokenOutOf - true to signify that this loop statement is broken out of via a break statement, false if it is definitely not broken out of
   */
  public void setBrokenOutOf(boolean brokenOutOf)
  {
    this.brokenOutOf = brokenOutOf;
  }

  /**
   * @return true if this loop statement is continued through via a continue statement
   */
  public boolean isContinuedThrough()
  {
    return continuedThrough;
  }

  /**
   * @param continuedThrough - true to signify that this loop statement is continued through via a continue statement, false if it is definitely never continued through
   */
  public void setContinuedThrough(boolean continuedThrough)
  {
    this.continuedThrough = continuedThrough;
  }


}
