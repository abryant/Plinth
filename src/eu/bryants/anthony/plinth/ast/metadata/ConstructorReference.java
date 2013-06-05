package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 10 Apr 2013
 */

/**
 * @author Anthony Bryant
 */
public class ConstructorReference extends MemberReference<Constructor>
{
  private NamedType containingType;

  private Type[] parameterTypes;
  private NamedType[] checkedThrownTypes;

  /**
   * Creates a new ConstructorReference to contain a specified Constructor, possibly with some generic types filled in using the GenericTypeSpecialiser
   * @param referencedConstructor - the referenced Constructor
   * @param genericTypeSpecialiser - the type specialiser to specialise the types of the referenced Constructor
   */
  public ConstructorReference(Constructor referencedConstructor, GenericTypeSpecialiser genericTypeSpecialiser)
  {
    super(referencedConstructor);
    if (referencedConstructor.getContainingTypeDefinition() != null)
    {
      // this cast is always valid, since we are specialising a reference to a TypeDefinition
      containingType = (NamedType) genericTypeSpecialiser.getSpecialisedType(new NamedType(false, false, false, referencedConstructor.getContainingTypeDefinition()));
    }

    Parameter[] genericParameters = referencedConstructor.getParameters();
    parameterTypes = new Type[genericParameters.length];
    for (int i = 0; i < genericParameters.length; ++i)
    {
      parameterTypes[i] = genericTypeSpecialiser.getSpecialisedType(genericParameters[i].getType());
    }
    NamedType[] genericCheckedThrownTypes = referencedConstructor.getCheckedThrownTypes();
    checkedThrownTypes = new NamedType[genericCheckedThrownTypes.length];
    for (int i = 0; i < genericCheckedThrownTypes.length; ++i)
    {
      // here, we might be resolving a generic thrown type into a specialised one
      // to cast this to a NamedType, we must depend on two things:
      // 1. only named types inheriting from Throwable are allowed in a throws clause
      // 2. something has already checked that, if it is a generic type parameter, it is assignable to Throwable
      checkedThrownTypes[i] = (NamedType) genericTypeSpecialiser.getSpecialisedType(genericCheckedThrownTypes[i]);
    }
  }

  /**
   * @return the containingType
   */
  public NamedType getContainingType()
  {
    return containingType;
  }

  /**
   * @return the parameterTypes
   */
  public Type[] getParameterTypes()
  {
    return parameterTypes;
  }

  /**
   * @return the checkedThrownTypes
   */
  public NamedType[] getCheckedThrownTypes()
  {
    return checkedThrownTypes;
  }

}
