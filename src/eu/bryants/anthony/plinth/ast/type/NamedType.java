package eu.bryants.anthony.plinth.ast.type;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class NamedType extends Type
{

  private QName qname;

  // a type is explicitly immutable if it has been declared as immutable explicitly,
  // whereas a type is contextually immutable if it is just accessed in an immutable context
  // if a type is explicitly immutable, then it is always also contextually immutable
  private boolean explicitlyImmutable;
  private boolean contextuallyImmutable;

  private TypeDefinition resolvedTypeDefinition;

  public NamedType(boolean nullable, boolean explicitlyImmutable, boolean contextuallyImmutable, QName qname, LexicalPhrase lexicalPhrase)
  {
    super(nullable, lexicalPhrase);
    this.explicitlyImmutable = explicitlyImmutable;
    this.contextuallyImmutable = explicitlyImmutable | contextuallyImmutable;
    this.qname = qname;
  }

  public NamedType(boolean nullable, boolean explicitlyImmutable, boolean contextuallyImmutable, TypeDefinition typeDefinition)
  {
    this(nullable, explicitlyImmutable, contextuallyImmutable, typeDefinition.getQualifiedName(), null);
    this.resolvedTypeDefinition = typeDefinition;
  }

  public NamedType(boolean nullable, boolean explicitlyImmutable, QName qname, LexicalPhrase lexicalPhrase)
  {
    this(nullable, explicitlyImmutable, false, qname, lexicalPhrase);
  }

  public NamedType(boolean nullable, boolean explicitlyImmutable, TypeDefinition typeDefinition)
  {
    this(nullable, explicitlyImmutable, false, typeDefinition);
  }

  /**
   * Note: this method should not be called until this type has been resolved
   * @return true if this type is explicitly immutable, false otherwise
   */
  public boolean isExplicitlyImmutable()
  {
    return explicitlyImmutable || resolvedTypeDefinition.isImmutable();
  }

  /**
   * Note: this method should not be called until this type has been resolved
   * @return true if this type is contextually immutable, false otherwise
   */
  public boolean isContextuallyImmutable()
  {
    return contextuallyImmutable || resolvedTypeDefinition.isImmutable();
  }

  /**
   * @return the qualified name
   */
  public QName getQualifiedName()
  {
    return qname;
  }

  /**
   * @return the resolved TypeDefinition
   */
  public TypeDefinition getResolvedTypeDefinition()
  {
    return resolvedTypeDefinition;
  }

  /**
   * @param resolvedTypeDefinition - the resolved TypeDefinition to set
   */
  public void setResolvedTypeDefinition(TypeDefinition resolvedTypeDefinition)
  {
    this.resolvedTypeDefinition = resolvedTypeDefinition;
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
    if (!(type instanceof NamedType))
    {
      return false;
    }
    // a nullable type cannot be assigned to a non-nullable type
    if (!isNullable() && type.isNullable())
    {
      return false;
    }
    if (resolvedTypeDefinition == null)
    {
      throw new IllegalStateException("Cannot check whether two types are assign-compatible before they are resolved");
    }
    NamedType otherNamedType = (NamedType) type;
    TypeDefinition otherResolvedTypeDefinition = otherNamedType.getResolvedTypeDefinition();
    boolean found = false;
    for (TypeDefinition otherSuperType : otherResolvedTypeDefinition.getInheritanceLinearisation())
    {
      if (otherSuperType == resolvedTypeDefinition)
      {
        found = true;
        break;
      }
    }
    if (!found)
    {
      return false;
    }
    // an immutable type definition means the type is always immutable, so it must be equivalent regardless of immutability
    // this works with inheritance, since an immutable parent class can only have immutable subclasses
    if (!resolvedTypeDefinition.isImmutable())
    {
      // an explicitly-immutable named type cannot be assigned to a non-explicitly-immutable named type
      if (!isExplicitlyImmutable() && otherNamedType.isExplicitlyImmutable())
      {
        return false;
      }
      // a contextually-immutable named type cannot be assigned to a non-immutable named type
      if (!isContextuallyImmutable() && otherNamedType.isContextuallyImmutable())
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
    if (resolvedTypeDefinition != null)
    {
      return type instanceof NamedType &&
             isNullable() == type.isNullable() &&
             resolvedTypeDefinition.equals(((NamedType) type).getResolvedTypeDefinition()) &&
             // an immutable type definition means the type is always immutable, so it must be equivalent regardless of immutability
             (resolvedTypeDefinition.isImmutable() ||
              (isExplicitlyImmutable() == ((NamedType) type).isExplicitlyImmutable() &&
               isContextuallyImmutable() == ((NamedType) type).isContextuallyImmutable()));
    }
    throw new IllegalStateException("Cannot check for type equivalence before the named type is resolved");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Member> getMembers(String name)
  {
    return getMembers(name, false);
  }

  /**
   * Finds all members of this type with the specified name, optionally including inherited static fields.
   * @param name - the name to search for
   * @param inheritStaticMembers - true if inherited static members should be included, false otherwise
   * @return the set of members that are part of this type
   */
  public Set<Member> getMembers(String name, boolean inheritStaticMembers)
  {
    if (resolvedTypeDefinition == null)
    {
      throw new IllegalStateException("Cannot get the members of a NamedType before it is resolved");
    }

    // store the disambiguators as well as the members, so that we can keep track of
    // whether we have added e.g. a function with type {uint -> void}
    Map<String, Member> matches = new HashMap<String, Member>();

    // try to add members from the inherited classes in order, but be careful not to add
    // members which cannot be disambiguated from those we have already added
    TypeDefinition[] searchList = resolvedTypeDefinition.getInheritanceLinearisation();
    for (TypeDefinition currentDefinition : searchList)
    {
      Field currentField = currentDefinition.getField(name);
      // exclude static fields from being inherited
      if (currentField != null && (!currentField.isStatic() || inheritStaticMembers || currentDefinition == resolvedTypeDefinition) && !matches.containsKey("F" + currentField.getName()))
      {
        matches.put("F" + currentField.getName(), currentField);
      }
      Set<Method> currentMethodSet = currentDefinition.getMethodsByName(name);
      if (currentMethodSet != null)
      {
        for (Method method : currentMethodSet)
        {
          if (!method.isStatic() || inheritStaticMembers || currentDefinition == resolvedTypeDefinition)
          {
            String disambiguator = "M" + method.getDisambiguator().toString();
            Member old = matches.get(disambiguator);
            if (old == null)
            {
              matches.put(disambiguator, method);
            }
            else
            {
              Method oldMethod = (Method) old;
              // there is already another method with this disambiguator
              // if it is from a different class, then it was earlier in the resolution list, so don't even try to overwrite it
              if (oldMethod.getContainingTypeDefinition() != currentDefinition)
              {
                continue;
              }
              // since the Resolver prohibits duplicate non-static methods in a single type, this one must be static
              if (!method.isStatic() || !oldMethod.isStatic())
              {
                throw new IllegalStateException("Duplicate non-static methods can not exist in a class");
              }
              // find which of these static methods has the greater since specifier, and use it
              SinceSpecifier oldSince = oldMethod.getSinceSpecifier();
              SinceSpecifier newSince = method.getSinceSpecifier();
              Method newer = oldSince == null ? method :
                             oldSince.compareTo(newSince) < 0 ? method : oldMethod;
              matches.put(disambiguator, newer);
            }
          }
        }
      }
    }
    for (BuiltinMethod builtin : ObjectType.OBJECT_METHODS)
    {
      if (!builtin.getName().equals(name))
      {
        continue;
      }
      if (!builtin.isStatic() || inheritStaticMembers)
      {
        String disambiguator = "M" + builtin.getDisambiguator().toString();
        Member old = matches.get(disambiguator);
        if (old == null)
        {
          matches.put(disambiguator, builtin);
        }
      }
    }
    return new HashSet<Member>(matches.values());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    if (resolvedTypeDefinition != null && resolvedTypeDefinition instanceof ClassDefinition)
    {
      return (isNullable() ? "x" : "") + (isContextuallyImmutable() ? "c" : "") + "C" + resolvedTypeDefinition.getQualifiedName().getMangledName() + "E";
    }
    else if (resolvedTypeDefinition != null && resolvedTypeDefinition instanceof CompoundDefinition)
    {
      return (isNullable() ? "x" : "") + (isContextuallyImmutable() ? "c" : "") + "V" + resolvedTypeDefinition.getQualifiedName().getMangledName() + "E";
    }
    else if (resolvedTypeDefinition != null && resolvedTypeDefinition instanceof InterfaceDefinition)
    {
      return (isNullable() ? "x" : "") + (isContextuallyImmutable() ? "c" : "") + "N" + resolvedTypeDefinition.getQualifiedName().getMangledName() + "E";
    }
    throw new IllegalStateException("Cannot get a mangled name before the NamedType is resolved");
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
    return (isNullable() ? "?" : "") + (contextuallyImmutable ? "#" : "") + qname;
  }
}
