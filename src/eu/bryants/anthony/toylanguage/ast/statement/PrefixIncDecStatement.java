package eu.bryants.anthony.toylanguage.ast.statement;

import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 8 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class PrefixIncDecStatement extends Statement
{

  private Assignee assignee;
  private boolean increment;

  public PrefixIncDecStatement(Assignee assignee, boolean increment, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.assignee = assignee;
    this.increment = increment;
  }

  /**
   * @return the assignee
   */
  public Assignee getAssignee()
  {
    return assignee;
  }

  /**
   * @return the increment
   */
  public boolean isIncrement()
  {
    return increment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopsExecution()
  {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return (increment ? "++" : "--") + assignee + ";";
  }

}
