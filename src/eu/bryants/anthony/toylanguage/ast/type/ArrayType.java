package eu.bryants.anthony.toylanguage.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.ArrayLengthMember;
import eu.bryants.anthony.toylanguage.ast.member.Member;

/*
 * Created on 3 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ArrayType extends Type
{

  private static final String LENGTH_FIELD_NAME = "length";
  private static final ArrayLengthMember LENGTH_MEMBER = new ArrayLengthMember();

  private Type baseType;

  public ArrayType(boolean nullable, Type baseType, LexicalPhrase lexicalPhrase)
  {
    super(nullable, lexicalPhrase);
    this.baseType = baseType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAssign(Type type)
  {
    // only allow assignment if base type assignment would work both ways,
    // so we cannot do either of the following (for some Bar extends Foo)

    // 1:
    // []Bar bs = new [1]Bar;
    // []Foo fs = bs;
    // fs[0] = new Foo(); // bs now contains a Foo, despite it being a []Bar

    // 2:
    // []Foo fs = new [1]Foo;
    // fs[0] = new Foo();
    // []Bar bs = fs; // bs now contains a Foo, despite it being a []Bar

    if (type instanceof NullType && isNullable())
    {
      // all nullable types can have null assigned to them
      return true;
    }

    if (!(type instanceof ArrayType))
    {
      return false;
    }
    // a nullable type cannot be assigned to a non-nullable type
    if (!isNullable() && type.isNullable())
    {
      return false;
    }
    Type otherBaseType = ((ArrayType) type).getBaseType();
    return baseType.canAssign(otherBaseType) && otherBaseType.canAssign(baseType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEquivalent(Type type)
  {
    return type instanceof ArrayType && isNullable() == type.isNullable() && baseType.isEquivalent(((ArrayType) type).getBaseType());
  }

  /**
   * @return the baseType
   */
  public Type getBaseType()
  {
    return baseType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Member> getMembers(String name)
  {
    HashSet<Member> set = new HashSet<Member>();
    if (name.equals(LENGTH_FIELD_NAME))
    {
      set.add(LENGTH_MEMBER);
    }
    return set;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    return (isNullable() ? "?" : "") + "[" + baseType.getMangledName() + "]";
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
    return (isNullable() ? "?" : "") + "[]" + baseType;
  }
}
