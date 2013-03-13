package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.member.Initialiser;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.statement.Statement;

/*
 * Created on 27 Feb 2013
 */

/**
 * @author Anthony Bryant
 */
public class PropertyInitialiser extends Initialiser
{
  private Property property;

  private boolean isPropertyConstructorCall;

  /**
   * Creates a new PropertyInitialiser to initialise the specified Property
   * @param property - the property which should be initialised
   */
  public PropertyInitialiser(Property property)
  {
    super(property.isStatic(), new Block(new Statement[0], null), property.getLexicalPhrase());
    this.property = property;
  }

  /**
   * @return the property
   */
  public Property getProperty()
  {
    return property;
  }

  /**
   * @return the isPropertyConstructorCall
   */
  public boolean isPropertyConstructorCall()
  {
    return isPropertyConstructorCall;
  }

  /**
   * @param isPropertyConstructorCall - the isPropertyConstructorCall to set
   */
  public void setPropertyConstructorCall(boolean isPropertyConstructorCall)
  {
    this.isPropertyConstructorCall = isPropertyConstructorCall;
  }
}
