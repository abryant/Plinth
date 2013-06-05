package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.member.Method;
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
    parameterTypes = new Type[genericParameters.length];
    for (int i = 0; i < genericParameters.length; ++i)
    {
      parameterTypes[i] = genericTypeSpecialiser.getSpecialisedType(genericParameters[i].getType());
    }
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
   * A disambiguator for method calls, which allows methods which are semantically equivalent (i.e. have the same name and types) can be easily distinguished.
   * It also allows methods to be sorted into a predictable order, by implementing comparable.
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
      if (isStatic() != other.isStatic() || !returnType.isRuntimeEquivalent(otherReference.returnType) || !getName().equals(other.getName()) || parameterTypes.length != otherReference.parameterTypes.length)
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
