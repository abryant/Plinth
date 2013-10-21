package eu.bryants.anthony.plinth.ast.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import eu.bryants.anthony.plinth.ast.member.Method;
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
public class MethodReference extends MemberReference<Method>
{
  private NamedType containingType;

  private Type returnType;
  private Type[] parameterTypes;
  private DefaultParameter[] defaultParameters;
  private NamedType[] checkedThrownTypes;

  private Disambiguator disambiguator = new Disambiguator();

  /**
   * Creates a new MethodReference to contain a specified Method, possibly with some generic types filled in using the GenericTypeSpecialiser
   * @param referencedMethod - the referenced Method
   * @param genericTypeSpecialiser - the type specialiser to specialise the types of the referenced Method
   */
  public MethodReference(Method referencedMethod, GenericTypeSpecialiser genericTypeSpecialiser)
  {
    super(referencedMethod);
    if (referencedMethod.getContainingTypeDefinition() != null)
    {
      // this cast is always valid, since we are specialising a reference to a TypeDefinition
      containingType = (NamedType) genericTypeSpecialiser.getSpecialisedType(new NamedType(false, false, false, referencedMethod.getContainingTypeDefinition()));
    }

    returnType = genericTypeSpecialiser.getSpecialisedType(referencedMethod.getReturnType());
    Parameter[] genericParameters = referencedMethod.getParameters();
    List<Type> specialisedTypes = new ArrayList<Type>();
    List<DefaultParameter> specialisedDefaultParameters = new ArrayList<DefaultParameter>();
    for (int i = 0; i < genericParameters.length; ++i)
    {
      if (genericParameters[i] instanceof NormalParameter || genericParameters[i] instanceof AutoAssignParameter)
      {
        specialisedTypes.add(genericTypeSpecialiser.getSpecialisedType(genericParameters[i].getType()));
      }
      else if (genericParameters[i] instanceof DefaultParameter)
      {
        DefaultParameter existingParameter = (DefaultParameter) genericParameters[i];
        Type specialisedType = genericTypeSpecialiser.getSpecialisedType(existingParameter.getType());
        // create a new DefaultParameter which doesn't have an Expression
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

    NamedType[] genericCheckedThrownTypes = referencedMethod.getCheckedThrownTypes();
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
   * @return the returnType
   */
  public Type getReturnType()
  {
    return returnType;
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

  /**
   * @return the disambiguator
   */
  public Disambiguator getDisambiguator()
  {
    return disambiguator;
  }

  /**
   * A disambiguator for method calls, which allows methods which are semantically equivalent (i.e. have the same name and types) to be easily distinguished.
   * @author Anthony Bryant
   */
  public class Disambiguator
  {

    /**
     * @return the name associated with this Disambiguator
     */
    public String getName()
    {
      return getReferencedMember().getName();
    }

    /**
     * @return true if the Method referenced by this Disambiguator is static, false otherwise
     */
    public boolean isStatic()
    {
      return getReferencedMember().isStatic();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
      StringBuffer buffer = new StringBuffer();
      buffer.append(isStatic() ? "SM_" : "M_");
      buffer.append(getName());
      buffer.append('_');
      buffer.append(returnType.getMangledName());
      buffer.append('_');
      for (int i = 0; i < parameterTypes.length; ++i)
      {
        buffer.append(parameterTypes[i].getMangledName());
      }
      for (int i = 0; i < defaultParameters.length; ++i)
      {
        buffer.append(defaultParameters[i].getMangledName());
      }
      return buffer.toString();
    }

    /**
     * @return the enclosing MethodReference, for use only in matches()
     */
    private MethodReference getMethodReference()
    {
      return MethodReference.this;
    }

    /**
     * Checks whether this disambiguator matches the signature of the specified other disambiguator.
     * This checks the signature of the method, but not the since specifier.
     * It also only tests runtime equivalence for types, not absolute equivalence.
     * @param other - the Disambiguator to check
     * @return true if this Disambiguator matches the specified Disambiguator, false otherwise
     */
    public boolean matches(Disambiguator other)
    {
      MethodReference otherReference = other.getMethodReference();
      if (isStatic() != other.isStatic() ||
          !returnType.isRuntimeEquivalent(otherReference.returnType) ||
          !getName().equals(other.getName()) ||
          parameterTypes.length != otherReference.parameterTypes.length ||
          defaultParameters.length != otherReference.defaultParameters.length)
      {
        return false;
      }
      for (int i = 0; i < parameterTypes.length; ++i)
      {
        if (!parameterTypes[i].isRuntimeEquivalent(otherReference.parameterTypes[i]))
        {
          return false;
        }
      }
      for (int i = 0; i < defaultParameters.length; ++i)
      {
        if (!defaultParameters[i].getType().isRuntimeEquivalent(otherReference.defaultParameters[i].getType()) ||
            defaultParameters[i].getIndex() != otherReference.defaultParameters[i].getIndex() ||
            !defaultParameters[i].getName().equals(otherReference.defaultParameters[i].getName()))
        {
          return false;
        }
      }
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o)
    {
      if (!(o instanceof Disambiguator))
      {
        return false;
      }
      return matches((Disambiguator) o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
      return getName().hashCode();
    }
  }

}
