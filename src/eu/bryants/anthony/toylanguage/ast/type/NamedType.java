package eu.bryants.anthony.toylanguage.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.ClassDefinition;
import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.TypeDefinition;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.ast.member.Method;
import eu.bryants.anthony.toylanguage.ast.misc.QName;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class NamedType extends Type
{

  private QName qname;

  private TypeDefinition resolvedTypeDefinition;

  public NamedType(boolean nullable, QName qname, LexicalPhrase lexicalPhrase)
  {
    super(nullable, lexicalPhrase);
    this.qname = qname;
  }

  public NamedType(boolean nullable, TypeDefinition typeDefinition)
  {
    super(nullable, null);
    this.qname = typeDefinition.getQualifiedName();
    this.resolvedTypeDefinition = typeDefinition;
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
    // TODO: when we add inheritance, make this more general
    if (resolvedTypeDefinition != null)
    {
      return resolvedTypeDefinition.equals(((NamedType) type).getResolvedTypeDefinition());
    }
    throw new IllegalStateException("Cannot check whether two types are assign-compatible before they are resolved");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEquivalent(Type type)
  {
    if (resolvedTypeDefinition != null)
    {
      return type instanceof NamedType && isNullable() == type.isNullable() && resolvedTypeDefinition.equals(((NamedType) type).getResolvedTypeDefinition());
    }
    throw new IllegalStateException("Cannot check for type equivalence before the named type is resolved");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Member> getMembers(String name)
  {
    if (resolvedTypeDefinition != null)
    {
      HashSet<Member> set = new HashSet<Member>();
      Field field = resolvedTypeDefinition.getField(name);
      if (field != null)
      {
        set.add(field);
      }
      Set<Method> methodSet = resolvedTypeDefinition.getMethodsByName(name);
      if (methodSet != null)
      {
        set.addAll(methodSet);
      }
      return set;
    }
    throw new IllegalStateException("Cannot get the members of a NamedType before it is resolved");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    if (resolvedTypeDefinition != null && resolvedTypeDefinition instanceof ClassDefinition)
    {
      return (isNullable() ? "?" : "") + "{" + resolvedTypeDefinition.getQualifiedName() + "}";
    }
    else if (resolvedTypeDefinition != null && resolvedTypeDefinition instanceof CompoundDefinition)
    {
      return (isNullable() ? "?" : "") + "{$" + resolvedTypeDefinition.getQualifiedName() + "}";
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
    return (isNullable() ? "?" : "") + qname;
  }
}
