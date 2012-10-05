package eu.bryants.anthony.toylanguage.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Member;

/*
 * Created on 21 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class FunctionType extends Type
{

  private Type returnType;
  private Type[] parameterTypes;

  public FunctionType(boolean nullable, Type returnType, Type[] parameterTypes, LexicalPhrase lexicalPhrase)
  {
    super(nullable, lexicalPhrase);
    this.returnType = returnType;
    this.parameterTypes = parameterTypes;
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
    if (!(type instanceof FunctionType))
    {
      return false;
    }
    if (!isNullable() && type.isNullable())
    {
      // a nullable type cannot be assigned to a non-nullable type
      return false;
    }
    // to be assign-compatible, both of the FunctionTypes must have equivalent parameter and return types
    FunctionType otherFunction = (FunctionType) type;
    if (!returnType.isEquivalent(otherFunction.getReturnType()))
    {
      return false;
    }
    Type[] otherParameters = otherFunction.getParameterTypes();
    if (parameterTypes.length != otherParameters.length)
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
    if (!returnType.isEquivalent(otherFunction.getReturnType()))
    {
      return false;
    }
    Type[] otherParameters = otherFunction.getParameterTypes();
    if (parameterTypes.length != otherParameters.length)
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
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Member> getMembers(String name)
  {
    return new HashSet<Member>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append('&');
    buffer.append(returnType.getMangledName());
    buffer.append('=');
    for (Type type : parameterTypes)
    {
      buffer.append(type.getMangledName());
    }
    buffer.append('@');
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
    buffer.append('{');
    for (int i = 0; i < parameterTypes.length; i++)
    {
      buffer.append(parameterTypes[i]);
      if (i != parameterTypes.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(" -> ");
    buffer.append(returnType);
    buffer.append('}');
    return buffer.toString();
  }

}
