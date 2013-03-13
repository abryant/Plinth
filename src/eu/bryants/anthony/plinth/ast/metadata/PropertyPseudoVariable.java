package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.member.Property;

/*
 * Created on 5 Mar 2013
 */

/**
 * A Variable which represents access to a property.
 * Uses of these pseudo-variables will actually generate calls to getters, setters, and constructors.
 * @author Anthony Bryant
 */
public class PropertyPseudoVariable extends Variable
{
  private Property property;

  public PropertyPseudoVariable(Property property)
  {
    super(property.isFinal(), property.getType(), property.getName());
    this.property = property;
  }

  /**
   * @return the property
   */
  public Property getProperty()
  {
    return property;
  }

}
