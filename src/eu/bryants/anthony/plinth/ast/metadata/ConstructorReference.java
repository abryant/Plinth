package eu.bryants.anthony.plinth.ast.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.misc.AutoAssignParameter;
import eu.bryants.anthony.plinth.ast.misc.DefaultParameter;
import eu.bryants.anthony.plinth.ast.misc.NormalParameter;
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
  private DefaultParameter[] defaultParameters;
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
    List<Type> specialisedTypes = new ArrayList<Type>();
    List<DefaultParameter> specialisedDefaultParameters = new ArrayList<DefaultParameter>();
    for (int i = 0; i < genericParameters.length; ++i)
    {
      if (genericParameters[i] instanceof NormalParameter)
      {
        specialisedTypes.add(genericTypeSpecialiser.getSpecialisedType(genericParameters[i].getType()));
      }
      else if (genericParameters[i] instanceof AutoAssignParameter)
      {
        specialisedTypes.add(genericTypeSpecialiser.getSpecialisedType(genericParameters[i].getType()));
      }
      else if (genericParameters[i] instanceof DefaultParameter)
      {
        DefaultParameter existingParameter = (DefaultParameter) genericParameters[i];
        Type specialisedType = genericTypeSpecialiser.getSpecialisedType(existingParameter.getType());
        DefaultParameter specialisedParameter = new DefaultParameter(existingParameter.isFinal(), specialisedType, genericParameters[i].getName(), null, null);
        specialisedParameter.setIndex(existingParameter.getIndex());
        specialisedDefaultParameters.add(specialisedParameter);
      }
      else
      {
        throw new IllegalArgumentException("Unknown Parameter type: " + genericParameters[i]);
      }
    }
    parameterTypes = specialisedTypes.toArray(new Type[specialisedTypes.size()]);
    defaultParameters = specialisedDefaultParameters.toArray(new DefaultParameter[specialisedDefaultParameters.size()]);
    // sort the default parameters by name
    Arrays.sort(defaultParameters, new Comparator<DefaultParameter>()
    {
      @Override
      public int compare(DefaultParameter o1, DefaultParameter o2)
      {
        return o1.getName().compareTo(o2.getName());
      }
    });

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
   * @return the defaultParameters
   */
  public DefaultParameter[] getDefaultParameters()
  {
    return defaultParameters;
  }

  /**
   * @return the checkedThrownTypes
   */
  public NamedType[] getCheckedThrownTypes()
  {
    return checkedThrownTypes;
  }

}
