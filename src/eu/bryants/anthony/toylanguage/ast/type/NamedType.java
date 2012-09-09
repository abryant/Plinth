package eu.bryants.anthony.toylanguage.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
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

  private CompoundDefinition resolvedDefinition;

  public NamedType(boolean nullable, QName qname, LexicalPhrase lexicalPhrase)
  {
    super(nullable, lexicalPhrase);
    this.qname = qname;
  }

  public NamedType(boolean nullable, CompoundDefinition compoundDefinition)
  {
    super(nullable, null);
    this.qname = compoundDefinition.getQualifiedName();
    this.resolvedDefinition = compoundDefinition;
  }

  /**
   * @return the qualified name
   */
  public QName getQualifiedName()
  {
    return qname;
  }

  /**
   * @return the resolvedDefinition
   */
  public CompoundDefinition getResolvedDefinition()
  {
    return resolvedDefinition;
  }

  /**
   * @param resolvedDefinition - the resolvedDefinition to set
   */
  public void setResolvedDefinition(CompoundDefinition resolvedDefinition)
  {
    this.resolvedDefinition = resolvedDefinition;
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
    // TODO: when we add classes, make this more general
    return resolvedDefinition.equals(((NamedType) type).getResolvedDefinition());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEquivalent(Type type)
  {
    return type instanceof NamedType && isNullable() == type.isNullable() && resolvedDefinition.equals(((NamedType) type).getResolvedDefinition());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Member> getMembers(String name)
  {
    HashSet<Member> set = new HashSet<Member>();
    Field field = resolvedDefinition.getField(name);
    if (field != null)
    {
      set.add(field);
    }
    Set<Method> methodSet = resolvedDefinition.getMethodsByName(name);
    if (methodSet != null)
    {
      set.addAll(methodSet);
    }
    return set;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    // TODO: this should eventually use the fully-qualified name, and may need to differ between compound and class types
    return (isNullable() ? "?" : "") + "{" + resolvedDefinition.getQualifiedName() + "}";
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
