package eu.bryants.anthony.plinth.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod.BuiltinMethodType;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.metadata.GenericTypeSpecialiser;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunction;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;

/*
 * Created on 7 Dec 2012
 */

/**
 * @author Anthony Bryant
 */
public class ObjectType extends Type
{

  public static final String MANGLED_NAME = "o";

  public static final BuiltinMethod[] OBJECT_METHODS = new BuiltinMethod[]
  {
    new BuiltinMethod(new ObjectType(false, true, null), BuiltinMethodType.TO_STRING),
  };

  static
  {
    for (int i = 0; i < OBJECT_METHODS.length; ++i)
    {
      MemberFunction memberFunction = new MemberFunction(OBJECT_METHODS[i]);
      memberFunction.setIndex(i);
      OBJECT_METHODS[i].setMemberFunction(memberFunction);
    }
  }

  // a type is explicitly immutable if it has been declared as immutable explicitly,
  // whereas a type is contextually immutable if it is just accessed in an immutable context
  // if a type is explicitly immutable, then it is always also contextually immutable
  private boolean explicitlyImmutable;
  private boolean contextuallyImmutable;

  /**
   * Creates a new ObjectType with the specified nullability and immutability.
   * @param nullable - true if the type should be nullable, false otherwise
   * @param explicitlyImmutable - true if the type should be explicitly immutable, false otherwise
   * @param contextuallyImmutable - true if the type should be contextually immutable, false otherwise
   * @param lexicalPhrase - the LexicalPhrase of the parsed Type, or null if this ObjectType was not parsed
   */
  public ObjectType(boolean nullable, boolean explicitlyImmutable, boolean contextuallyImmutable, LexicalPhrase lexicalPhrase)
  {
    super(nullable, lexicalPhrase);
    this.explicitlyImmutable = explicitlyImmutable;
    this.contextuallyImmutable = explicitlyImmutable | contextuallyImmutable;
  }

  /**
   * Creates a new ObjectType with the specified nullability and explicit immutability.
   * @param nullable - true if the type should be nullable, false otherwise
   * @param explicitlyImmutable - true if the type should be explicitly immutable, false otherwise
   * @param lexicalPhrase - the LexicalPhrase of the parsed Type, or null if this ObjectType was not parsed
   */
  public ObjectType(boolean nullable, boolean explicitlyImmutable, LexicalPhrase lexicalPhrase)
  {
    this(nullable, explicitlyImmutable, false, lexicalPhrase);
  }

  /**
   * @return the explicitlyImmutable
   */
  public boolean isExplicitlyImmutable()
  {
    return explicitlyImmutable;
  }

  /**
   * @return the contextuallyImmutable
   */
  public boolean isContextuallyImmutable()
  {
    return contextuallyImmutable;
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

    // a nullable type cannot be assigned to a non-nullable type
    if (!isNullable() && type.canBeNullable())
    {
      return false;
    }

    // a (possibly-)explicitly-immutable type cannot be assigned to a non-explicitly-immutable object type
    if (!isExplicitlyImmutable() && Type.canBeExplicitlyDataImmutable(type))
    {
      return false;
    }
    // a contextually-immutable type cannot be assigned to a non-immutable object type
    if (!isContextuallyImmutable() && Type.isContextuallyDataImmutable(type))
    {
      return false;
    }

    // providing the nullability and immutability is compatible, any type can be assigned to an object type
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canRuntimeAssign(Type type)
  {
    // object is at the top of the type system
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEquivalent(Type type)
  {
    return isRuntimeEquivalent(type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRuntimeEquivalent(Type type)
  {
    return type instanceof ObjectType &&
           isNullable() == type.isNullable() &&
           explicitlyImmutable == ((ObjectType) type).isExplicitlyImmutable() &&
           contextuallyImmutable == ((ObjectType) type).isContextuallyImmutable();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<MemberReference<?>> getMembers(String name)
  {
    Set<MemberReference<?>> members = new HashSet<MemberReference<?>>();
    for (Method method : OBJECT_METHODS)
    {
      if (method.getName().equals(name))
      {
        members.add(new MethodReference(method, GenericTypeSpecialiser.IDENTITY_SPECIALISER));
      }
    }
    return members;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    return (isNullable() ? "x" : "") + (isContextuallyImmutable() ? "c" : "") + MANGLED_NAME;
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
    return (isNullable() ? "?" : "") + (contextuallyImmutable ? "#" : "") + "object";
  }

}
