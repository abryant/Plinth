package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 10 Apr 2013
 */

/**
 * @author Anthony Bryant
 */
public class PropertyReference extends MemberReference<Property>
{
  private NamedType containingType;

  private Type type;

  /**
   * Creates a new PropertyReference to contain a specified Property, possibly with some generic types filled in using the GenericTypeSpecialiser
   * @param referencedProperty - the referenced Property
   * @param genericTypeSpecialiser - the type specialiser to specialise the type of the referenced Property
   */
  public PropertyReference(Property referencedProperty, GenericTypeSpecialiser genericTypeSpecialiser)
  {
    super(referencedProperty);
    if (referencedProperty.getContainingTypeDefinition() != null)
    {
      // this cast is always valid, since we are specialising a reference to a TypeDefinition
      containingType = (NamedType) genericTypeSpecialiser.getSpecialisedType(new NamedType(false, false, false, referencedProperty.getContainingTypeDefinition()));
    }

    type = genericTypeSpecialiser.getSpecialisedType(referencedProperty.getType());
  }

  /**
   * @return the containingType
   */
  public NamedType getContainingType()
  {
    return containingType;
  }

  /**
   * @return the type
   */
  public Type getType()
  {
    return type;
  }

}
