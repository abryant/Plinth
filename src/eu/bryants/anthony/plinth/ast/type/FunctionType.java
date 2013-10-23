package eu.bryants.anthony.plinth.ast.type;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod.BuiltinMethodType;
import eu.bryants.anthony.plinth.ast.metadata.GenericTypeSpecialiser;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;
import eu.bryants.anthony.plinth.ast.misc.DefaultParameter;

/*
 * Created on 21 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class FunctionType extends Type
{

  private boolean isImmutable;
  private Type returnType;
  private Type[] parameterTypes;
  private DefaultParameter[] defaultParameters;
  private NamedType[] thrownTypes;

  public FunctionType(boolean nullable, boolean isImmutable, Type returnType, Type[] parameterTypes, DefaultParameter[] defaultParameters, NamedType[] thrownTypes, LexicalPhrase lexicalPhrase)
  {
    super(nullable, lexicalPhrase);
    this.isImmutable = isImmutable;
    this.returnType = returnType;
    this.parameterTypes = parameterTypes;
    for (DefaultParameter defaultParameter : defaultParameters)
    {
      if (defaultParameter.getExpression() != null)
      {
        throw new IllegalArgumentException("A FunctionType cannot accept DefaultParameters with filled-in expressions");
      }
    }
    Arrays.sort(defaultParameters, new Comparator<DefaultParameter>()
    {
      @Override
      public int compare(DefaultParameter o1, DefaultParameter o2)
      {
        return o1.getName().compareTo(o2.getName());
      }
    });
    this.defaultParameters = defaultParameters;
    this.thrownTypes = thrownTypes;
  }

  /**
   * @return the isImmutable
   */
  public boolean isImmutable()
  {
    return isImmutable;
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
   * @return the thrownTypes
   */
  public NamedType[] getThrownTypes()
  {
    return thrownTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAssign(Type type)
  {
    if (type instanceof NullType && isNullable())
    {
      // all nullable types can have null assigned to them
      return true;
    }
    if (!isNullable() && type.canBeNullable())
    {
      // a nullable type cannot be assigned to a non-nullable type
      return false;
    }

    if (type instanceof FunctionType)
    {
      // a non-immutable function cannot be assigned to an immutable function type
      // NOTE: this is the opposite condition to the one for arrays and named types
      FunctionType otherFunction = (FunctionType) type;
      if (isImmutable() && !otherFunction.isImmutable())
      {
        return false;
      }
      // to be assign-compatible, both of the FunctionTypes must have equivalent parameter and return types
      if (!returnType.isEquivalent(otherFunction.getReturnType()))
      {
        return false;
      }
      Type[] otherParameters = otherFunction.getParameterTypes();
      DefaultParameter[] otherDefaultParameters = otherFunction.getDefaultParameters();
      if (parameterTypes.length != otherParameters.length || defaultParameters.length != otherDefaultParameters.length)
      {
        return false;
      }
      for (int i = 0; i < parameterTypes.length; ++i)
      {
        if (!parameterTypes[i].isEquivalent(otherParameters[i]))
        {
          return false;
        }
      }
      for (int i = 0; i < defaultParameters.length; ++i)
      {
        if (!defaultParameters[i].getName().equals(otherDefaultParameters[i].getName()) ||
            !defaultParameters[i].getType().isEquivalent(otherDefaultParameters[i].getType()))
        {
          return false;
        }
      }
      // the other type can only be assigned to us if we have at least all of the thrown types that it has
      NamedType[] otherThrownTypes = otherFunction.getThrownTypes();
      for (NamedType thrown : otherThrownTypes)
      {
        boolean found = false;
        for (NamedType check : thrownTypes)
        {
          if (check.canAssign(thrown))
          {
            found = true;
            break;
          }
        }
        if (!found)
        {
          return false;
        }
      }
      return true;
    }
    if (type instanceof WildcardType)
    {
      // we have already checked the nullability constraint, so just make sure that the wildcard type is a sub-type of this function type
      return ((WildcardType) type).canBeAssignedTo(this);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canRuntimeAssign(Type type)
  {
    if (!(type instanceof FunctionType))
    {
      return false;
    }
    FunctionType otherFunction = (FunctionType) type;
    if (!returnType.isRuntimeEquivalent(otherFunction.getReturnType()))
    {
      return false;
    }
    Type[] otherParameters = otherFunction.getParameterTypes();
    DefaultParameter[] otherDefaultParameters = otherFunction.getDefaultParameters();
    if (parameterTypes.length != otherParameters.length || defaultParameters.length != otherDefaultParameters.length)
    {
      return false;
    }
    for (int i = 0; i < parameterTypes.length; i++)
    {
      if (!parameterTypes[i].isRuntimeEquivalent(otherParameters[i]))
      {
        return false;
      }
    }
    for (int i = 0; i < defaultParameters.length; ++i)
    {
      if (!defaultParameters[i].getName().equals(otherDefaultParameters[i].getName()) ||
          !defaultParameters[i].getType().isEquivalent(otherDefaultParameters[i].getType()))
      {
        return false;
      }
    }
    // NOTE: we don't check the thrown types in a runtime equivalence check
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEquivalent(Type type)
  {
    if (!(type instanceof FunctionType))
    {
      return false;
    }
    FunctionType otherFunction = (FunctionType) type;
    if (isNullable() != otherFunction.isNullable())
    {
      return false;
    }
    if (isImmutable() != otherFunction.isImmutable())
    {
      return false;
    }
    if (!returnType.isEquivalent(otherFunction.getReturnType()))
    {
      return false;
    }
    Type[] otherParameters = otherFunction.getParameterTypes();
    DefaultParameter[] otherDefaultParameters = otherFunction.getDefaultParameters();
    if (parameterTypes.length != otherParameters.length || defaultParameters.length != otherDefaultParameters.length)
    {
      return false;
    }
    for (int i = 0; i < parameterTypes.length; i++)
    {
      if (!parameterTypes[i].isEquivalent(otherParameters[i]))
      {
        return false;
      }
    }
    for (int i = 0; i < defaultParameters.length; ++i)
    {
      if (!defaultParameters[i].getName().equals(otherDefaultParameters[i].getName()) ||
          !defaultParameters[i].getType().isEquivalent(otherDefaultParameters[i].getType()))
      {
        return false;
      }
    }
    NamedType[] otherThrownTypes = ((FunctionType) type).getThrownTypes();
    // make sure all of our thrown types are also thrown by the other type
    for (Type thrown : thrownTypes)
    {
      boolean found = false;
      for (Type check : otherThrownTypes)
      {
        if (thrown.isEquivalent(check))
        {
          found = true;
          break;
        }
      }
      if (!found)
      {
        return false;
      }
    }
    // also check it the other way around, in case we have something like:
    // this  throws A, A, B
    // other throws A, B, C
    for (Type thrown : otherThrownTypes)
    {
      boolean found = false;
      for (Type check : thrownTypes)
      {
        if (thrown.isEquivalent(check))
        {
          found = true;
          break;
        }
      }
      if (!found)
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
  public boolean isRuntimeEquivalent(Type type)
  {
    if (!(type instanceof FunctionType))
    {
      return false;
    }
    FunctionType otherFunction = (FunctionType) type;
    if (isNullable() != otherFunction.isNullable())
    {
      return false;
    }
    if (isImmutable() != otherFunction.isImmutable())
    {
      return false;
    }
    if (!returnType.isRuntimeEquivalent(otherFunction.getReturnType()))
    {
      return false;
    }
    Type[] otherParameters = otherFunction.getParameterTypes();
    DefaultParameter[] otherDefaultParameters = otherFunction.getDefaultParameters();
    if (parameterTypes.length != otherParameters.length || defaultParameters.length != otherDefaultParameters.length)
    {
      return false;
    }
    for (int i = 0; i < parameterTypes.length; i++)
    {
      if (!parameterTypes[i].isRuntimeEquivalent(otherParameters[i]))
      {
        return false;
      }
    }
    for (int i = 0; i < defaultParameters.length; ++i)
    {
      if (!defaultParameters[i].getName().equals(otherDefaultParameters[i].getName()) ||
          !defaultParameters[i].getType().isEquivalent(otherDefaultParameters[i].getType()))
      {
        return false;
      }
    }
    // NOTE: we don't check the thrown types in a runtime equivalence check
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<MemberReference<?>> getMembers(String name)
  {
    Set<MemberReference<?>> memberSet = new HashSet<MemberReference<?>>();
    if (name.equals(BuiltinMethodType.TO_STRING.methodName))
    {
      Type notNullThis = new FunctionType(false, isImmutable, returnType, parameterTypes, defaultParameters, thrownTypes, null);
      memberSet.add(new MethodReference(new BuiltinMethod(notNullThis, BuiltinMethodType.TO_STRING), GenericTypeSpecialiser.IDENTITY_SPECIALISER));
    }
    if (name.equals(BuiltinMethodType.EQUALS.methodName))
    {
      Type notNullThis = new FunctionType(false, isImmutable, returnType, parameterTypes, defaultParameters, thrownTypes, null);
      memberSet.add(new MethodReference(new BuiltinMethod(notNullThis, BuiltinMethodType.EQUALS), GenericTypeSpecialiser.IDENTITY_SPECIALISER));
    }
    if (name.equals(BuiltinMethodType.HASH_CODE.methodName))
    {
      Type notNullThis = new FunctionType(false, isImmutable, returnType, parameterTypes, defaultParameters, thrownTypes, null);
      memberSet.add(new MethodReference(new BuiltinMethod(notNullThis, BuiltinMethodType.HASH_CODE), GenericTypeSpecialiser.IDENTITY_SPECIALISER));
    }
    return memberSet;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    StringBuffer buffer = new StringBuffer();
    if (isNullable())
    {
      buffer.append('x');
    }
    if (isImmutable())
    {
      buffer.append('c');
    }
    buffer.append('F');
    buffer.append(returnType.getMangledName());
    buffer.append('_');
    for (Type type : parameterTypes)
    {
      buffer.append(type.getMangledName());
    }
    buffer.append('_');
    for (DefaultParameter defaultParameter : defaultParameters)
    {
      buffer.append(defaultParameter.getMangledName());
    }
    buffer.append('E');
    return buffer.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasDefaultValue()
  {
    return isNullable();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    if (isNullable())
    {
      buffer.append('?');
    }
    buffer.append('{');
    for (int i = 0; i < parameterTypes.length; i++)
    {
      buffer.append(parameterTypes[i]);
      if (i != parameterTypes.length - 1 || defaultParameters.length > 0)
      {
        buffer.append(", ");
      }
    }
    for (int i = 0; i < defaultParameters.length; ++i)
    {
      buffer.append(defaultParameters[i]);
      if (i != defaultParameters.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(' ');
    if (isImmutable())
    {
      buffer.append('#');
    }
    buffer.append("-> ");
    buffer.append(returnType);
    if (thrownTypes.length > 0)
    {
      buffer.append(" throws ");
      for (int i = 0; i < thrownTypes.length; ++i)
      {
        buffer.append(thrownTypes[i]);
        if (i != thrownTypes.length - 1)
        {
          buffer.append(", ");
        }
      }
    }
    buffer.append('}');
    return buffer.toString();
  }

}
