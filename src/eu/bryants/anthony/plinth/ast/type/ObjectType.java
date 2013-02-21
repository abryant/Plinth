package eu.bryants.anthony.plinth.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod.BuiltinMethodType;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.member.Method;

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
      OBJECT_METHODS[i].setMethodIndex(i);
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
    if (!isNullable() && type.isNullable())
    {
      return false;
    }

    // check the data-immutability of the other type
    // note that this is only data immutability, and a function's immutability does not affect things here
    // in fact, all non-data-immutable types are assumed to be not-immutable, so that they can be cast to directly 'object' rather than requiring '#object'
    boolean otherExplicitlyImmutable = false;
    boolean otherContextuallyImmutable = false;
    if (type instanceof ArrayType)
    {
      otherExplicitlyImmutable = ((ArrayType) type).isExplicitlyImmutable();
      otherContextuallyImmutable = ((ArrayType) type).isContextuallyImmutable();
    }
    else if (type instanceof NamedType)
    {
      otherExplicitlyImmutable = ((NamedType) type).isExplicitlyImmutable();
      otherContextuallyImmutable = ((NamedType) type).isContextuallyImmutable();
    }
    else if (type instanceof ObjectType)
    {
      otherExplicitlyImmutable = ((ObjectType) type).isExplicitlyImmutable();
      otherContextuallyImmutable = ((ObjectType) type).isContextuallyImmutable();
    }

    // an explicitly-immutable named type cannot be assigned to a non-explicitly-immutable named type
    if (!isExplicitlyImmutable() && otherExplicitlyImmutable)
    {
      return false;
    }
    // a contextually-immutable named type cannot be assigned to a non-immutable named type
    if (!isContextuallyImmutable() && otherContextuallyImmutable)
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
  public Set<Member> getMembers(String name)
  {
    Set<Member> members = new HashSet<Member>();
    for (Method method : OBJECT_METHODS)
    {
      if (method.getName().equals(name))
      {
        members.add(method);
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
    return MANGLED_NAME;
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
