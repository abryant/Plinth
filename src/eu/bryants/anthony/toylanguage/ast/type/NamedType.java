package eu.bryants.anthony.toylanguage.ast.type;

import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

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
  public Member getMember(String name)
  {
    return resolvedDefinition.getField(name);
    // TODO: when functions are added to named types, add them here
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
