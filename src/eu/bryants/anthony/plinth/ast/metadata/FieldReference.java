package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 10 Apr 2013
 */

/**
 * @author Anthony Bryant
 */
public class FieldReference extends MemberReference<Field>
{
  private NamedType containingType;

  private Type type;

  /**
   * Creates a new FieldReference to contain a specified Field, possibly with some generic types filled in using the GenericTypeSpecialiser
   * @param referencedField - the referenced Field
   * @param genericTypeSpecialiser - the type specialiser to specialise the type of the referenced Field
   */
  public FieldReference(Field referencedField, GenericTypeSpecialiser genericTypeSpecialiser)
  {
    super(referencedField);
    if (referencedField.getContainingTypeDefinition() != null)
    {
      // this cast is always valid, since we are specialising a reference to a TypeDefinition
      containingType = (NamedType) genericTypeSpecialiser.getSpecialisedType(new NamedType(false, false, false, referencedField.getContainingTypeDefinition()));
    }

    type = genericTypeSpecialiser.getSpecialisedType(referencedField.getType());
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
