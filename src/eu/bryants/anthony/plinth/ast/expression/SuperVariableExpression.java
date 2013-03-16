package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;

/*
 * Created on 15 Mar 2013
 */

/**
 * Represents an expression of the form <code>this.foo</code>
 * This is actually just a type of VariableExpression which can resolve to variables and methods on a super-class of this. It cannot be used to resolve static members or local variables.
 * @author Anthony Bryant
 */
public class SuperVariableExpression extends VariableExpression
{
  public SuperVariableExpression(String name, LexicalPhrase lexicalPhrase)
  {
    super(name, lexicalPhrase);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "super." + super.toString();
  }
}
