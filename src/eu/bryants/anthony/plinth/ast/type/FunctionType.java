package eu.bryants.anthony.plinth.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod.BuiltinMethodType;
import eu.bryants.anthony.plinth.ast.member.Member;

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

  public FunctionType(boolean nullable, boolean isImmutable, Type returnType, Type[] parameterTypes, LexicalPhrase lexicalPhrase)
  {
    super(nullable, lexicalPhrase);
    this.isImmutable = isImmutable;
    this.returnType = returnType;
    this.parameterTypes = parameterTypes;
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
    if (isImmutable() != otherFunction.isImmutable())
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
    Set<Member> memberSet = new HashSet<Member>();
    if (name.equals(BuiltinMethodType.TO_STRING.methodName))
    {
      Type notNullThis = new FunctionType(false, isImmutable, returnType, parameterTypes, null);
      memberSet.add(new BuiltinMethod(notNullThis, BuiltinMethodType.TO_STRING));
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
    if (isImmutable())
    {
      buffer.append('#');
    }
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
