package eu.bryants.anthony.toylanguage.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.ast.member.Method;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class NamedType extends Type
{

  private String name;

  private CompoundDefinition resolvedDefinition;

  public NamedType(String name, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.name = name;
  }

  public NamedType(CompoundDefinition compoundDefinition)
  {
    super(null);
    this.name = compoundDefinition.getName();
    this.resolvedDefinition = compoundDefinition;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
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
    if (!(type instanceof NamedType))
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
    return type instanceof NamedType && resolvedDefinition.equals(((NamedType) type).getResolvedDefinition());
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
    return "{" + resolvedDefinition.getName() + "}";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return name;
  }
}
