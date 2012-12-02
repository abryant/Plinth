package eu.bryants.anthony.plinth.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.misc.QName;

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
    if (resolvedTypeDefinition instanceof ClassDefinition)
    {
      if (!(otherNamedType.getResolvedTypeDefinition() instanceof ClassDefinition))
      {
        // cannot convert a non-ClassDefinition NamedType into a ClassDefinition one, since it can never be a subtype
        return false;
      }
      ClassDefinition current = (ClassDefinition) otherNamedType.getResolvedTypeDefinition();
      while (current != null)
      {
        if (current.equals(resolvedTypeDefinition))
        {
          break;
        }
        current = current.getSuperClassDefinition();
      }
      if (current == null)
      {
        // we couldn't find this type's ClassDefinition in the parent hierarchy of the other type's ClassDefinition
        // so this is not a superclass of the other type, so it is not assign-compatible
        return false;
      }
    }
    else if (resolvedTypeDefinition instanceof CompoundDefinition)
    {
      if (!resolvedTypeDefinition.equals(otherNamedType.getResolvedTypeDefinition()))
      {
        return false;
      }
    }
    else
    {
      throw new IllegalStateException("Unknown type of TypeDefinition: " + resolvedTypeDefinition);
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
    if (resolvedTypeDefinition == null)
    {
      throw new IllegalStateException("Cannot get the members of a NamedType before it is resolved");
    }
    HashSet<Member> set = new HashSet<Member>();
    // maintain a set of disambiguators as well as the members, so that we can keep track of
    // whether we have added e.g. a function with type {uint -> void}
    Set<Object> disambiguators = new HashSet<Object>();
    Field field = resolvedTypeDefinition.getField(name);
    if (field != null)
    {
      set.add(field);
      disambiguators.add(field.getName());
    }
    Set<Method> methodSet = resolvedTypeDefinition.getMethodsByName(name);
    if (methodSet != null)
    {
      for (Method method : methodSet)
      {
        set.add(method);
        disambiguators.add(method.getDisambiguator());
      }
    }
    if (resolvedTypeDefinition instanceof ClassDefinition)
    {
      // try to add members from the parent classes, but be careful not to add
      // members which cannot be disambiguated from those we have already added
      ClassDefinition currentDefinition = ((ClassDefinition) resolvedTypeDefinition).getSuperClassDefinition();
      while (currentDefinition != null)
      {
        Field currentField = currentDefinition.getField(name);
        // exclude static fields from being inherited
        if (currentField != null && !currentField.isStatic() && !disambiguators.contains(currentField.getName()))
        {
          set.add(currentField);
          disambiguators.add(currentField.getName());
        }
        Set<Method> currentMethodSet = currentDefinition.getMethodsByName(name);
        if (currentMethodSet != null)
        {
          for (Method method : currentMethodSet)
          {
            if (!disambiguators.contains(method.getDisambiguator()))
            {
              set.add(method);
              disambiguators.add(method.getDisambiguator());
            }
          }
        }
        currentDefinition = currentDefinition.getSuperClassDefinition();
      }
    }
    return set;
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
