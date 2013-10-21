package eu.bryants.anthony.plinth.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod.BuiltinMethodType;
import eu.bryants.anthony.plinth.ast.metadata.GenericTypeSpecialiser;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;

/*
 * Created on 4 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class TupleType extends Type
{

  private Type[] subTypes;

  public TupleType(boolean nullable, Type[] subTypes, LexicalPhrase lexicalPhrase)
  {
    super(nullable, lexicalPhrase);
    this.subTypes = subTypes;
  }

  /**
   * @return the subTypes
   */
  public Type[] getSubTypes()
  {
    return subTypes;
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
    if (subTypes.length == 1 && subTypes[0].canAssign(type))
    {
      // if we are a single element tuple, and that element can assign the type, then we can also assign the type
      return true;
    }

    // a nullable type cannot be assigned to a non-nullable type
    if (!isNullable() && type.canBeNullable())
    {
      return false;
    }

    if (type instanceof TupleType)
    {
      TupleType otherTuple = (TupleType) type;
      if (subTypes.length != otherTuple.subTypes.length)
      {
        return false;
      }
      for (int i = 0; i < subTypes.length; i++)
      {
        if (!subTypes[i].canAssign(otherTuple.subTypes[i]))
        {
          return false;
        }
      }
      return true;
    }
    if (type instanceof WildcardType)
    {
      // we have already checked the nullability constraint, so just make sure that the wildcard type is a sub-type of this tuple type
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
    if (!(type instanceof TupleType))
    {
      return false;
    }
    TupleType otherType = (TupleType) type;
    if (subTypes.length != otherType.subTypes.length)
    {
      return false;
    }
    for (int i = 0; i < subTypes.length; i++)
    {
      if (!subTypes[i].isRuntimeEquivalent(otherType.getSubTypes()[i]))
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
    if (!(type instanceof TupleType))
    {
      return false;
    }
    TupleType otherType = (TupleType) type;
    if (isNullable() != otherType.isNullable())
    {
      return false;
    }
    if (subTypes.length != otherType.subTypes.length)
    {
      return false;
    }
    for (int i = 0; i < subTypes.length; i++)
    {
      if (!subTypes[i].isEquivalent(otherType.getSubTypes()[i]))
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
    if (!(type instanceof TupleType))
    {
      return false;
    }
    TupleType otherType = (TupleType) type;
    if (isNullable() != otherType.isNullable())
    {
      return false;
    }
    if (subTypes.length != otherType.subTypes.length)
    {
      return false;
    }
    for (int i = 0; i < subTypes.length; i++)
    {
      if (!subTypes[i].isRuntimeEquivalent(otherType.getSubTypes()[i]))
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
  public Set<MemberReference<?>> getMembers(String name)
  {
    Set<MemberReference<?>> memberSet = new HashSet<MemberReference<?>>();
    if (name.equals(BuiltinMethodType.TO_STRING.methodName))
    {
      Type notNullThis = new TupleType(false, subTypes, null);
      memberSet.add(new MethodReference(new BuiltinMethod(notNullThis, BuiltinMethodType.TO_STRING), GenericTypeSpecialiser.IDENTITY_SPECIALISER));
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
    buffer.append("T");
    for (int i = 0; i < subTypes.length; i++)
    {
      buffer.append(subTypes[i].getMangledName());
    }
    buffer.append("t");
    return buffer.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasDefaultValue()
  {
    if (isNullable())
    {
      return true;
    }
    boolean hasDefault = true;
    for (Type subType : subTypes)
    {
      hasDefault &= subType.hasDefaultValue();
      if (!hasDefault)
      {
        break;
      }
    }
    return hasDefault;
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
    buffer.append('(');
    for (int i = 0; i < subTypes.length; i++)
    {
      buffer.append(subTypes[i]);
      if (i != subTypes.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(")");
    return buffer.toString();
  }
}
