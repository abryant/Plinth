package eu.bryants.anthony.plinth.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.ArrayLengthMember;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod.BuiltinMethodType;
import eu.bryants.anthony.plinth.ast.metadata.GenericTypeSpecialiser;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;

/*
 * Created on 3 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ArrayType extends Type
{

  // a type is explicitly immutable if it has been declared as immutable explicitly,
  // whereas a type is contextually immutable if it is just accessed in an immutable context
  // if a type is explicitly immutable, then it is always also contextually immutable
  private boolean explicitlyImmutable;
  private boolean contextuallyImmutable;

  private Type baseType;

  public ArrayType(boolean nullable, boolean explicitlyImmutable, boolean contextuallyImmutable, Type baseType, LexicalPhrase lexicalPhrase)
  {
    super(nullable, lexicalPhrase);
    this.explicitlyImmutable = explicitlyImmutable;
    this.contextuallyImmutable = explicitlyImmutable | contextuallyImmutable;
    this.baseType = baseType;
  }

  public ArrayType(boolean nullable, boolean isImmutable, Type baseType, LexicalPhrase lexicalPhrase)
  {
    this(nullable, isImmutable, false, baseType, lexicalPhrase);
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

    // a nullable type cannot be assigned to a non-nullable type
    if (!isNullable() && type.canBeNullable())
    {
      return false;
    }

    // an explicitly-immutable type cannot be assigned to a non-explicitly-immutable array type
    if (!isExplicitlyImmutable() && ((type instanceof ArrayType && ((ArrayType) type).isExplicitlyImmutable()) ||
                                     (type instanceof WildcardType && ((WildcardType) type).isExplicitlyImmutable())))
    {
      return false;
    }
    // a contextually-immutable type cannot be assigned to a non-immutable array type
    if (!isContextuallyImmutable() && ((type instanceof ArrayType && ((ArrayType) type).isContextuallyImmutable()) ||
                                       (type instanceof WildcardType && ((WildcardType) type).isContextuallyImmutable())))
    {
      return false;
    }

    if (type instanceof ArrayType)
    {
      Type otherBaseType = ((ArrayType) type).getBaseType();
      return baseType.canAssign(otherBaseType) && otherBaseType.canAssign(baseType);
    }
    if (type instanceof WildcardType)
    {
      // we have already checked the nullability and immutability constraints, so make sure that the wildcard type is a sub-type of this array type
      return ((WildcardType) type).canBeAssignedTo(this);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEquivalent(Type type)
  {
    return type instanceof ArrayType &&
           isNullable() == type.isNullable() &&
           isExplicitlyImmutable() == ((ArrayType) type).isExplicitlyImmutable() &&
           isContextuallyImmutable() == ((ArrayType) type).isContextuallyImmutable() &&
           baseType.isEquivalent(((ArrayType) type).getBaseType());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRuntimeEquivalent(Type type)
  {
    return type instanceof ArrayType &&
           isNullable() == type.isNullable() &&
           isExplicitlyImmutable() == ((ArrayType) type).isExplicitlyImmutable() &&
           isContextuallyImmutable() == ((ArrayType) type).isContextuallyImmutable() &&
           baseType.isRuntimeEquivalent(((ArrayType) type).getBaseType());
  }

  /**
   * @return true iff this ArrayType is explicitly immutable
   */
  public boolean isExplicitlyImmutable()
  {
    return explicitlyImmutable;
  }

  /**
   * @return true iff this ArrayType is contextually immutable or explicitly immutable
   */
  public boolean isContextuallyImmutable()
  {
    return contextuallyImmutable;
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
  public Set<MemberReference<?>> getMembers(String name)
  {
    HashSet<MemberReference<?>> set = new HashSet<MemberReference<?>>();
    if (name.equals(ArrayLengthMember.LENGTH_FIELD_NAME))
    {
      set.add(ArrayLengthMember.LENGTH_MEMBER_REFERENCE);
    }
    if (name.equals(BuiltinMethodType.TO_STRING.methodName))
    {
      ArrayType notNullThis = new ArrayType(false, explicitlyImmutable, contextuallyImmutable, baseType, null);
      set.add(new MethodReference(new BuiltinMethod(notNullThis, BuiltinMethodType.TO_STRING), GenericTypeSpecialiser.IDENTITY_SPECIALISER));
    }
    return set;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    return (isNullable() ? "x" : "") + (isContextuallyImmutable() ? "c" : "") + "A" + baseType.getMangledName();
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
    return (isNullable() ? "?" : "") + (isContextuallyImmutable() ? "#" : "") + "[]" + baseType;
  }
}
