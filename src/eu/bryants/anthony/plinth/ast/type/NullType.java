package eu.bryants.anthony.plinth.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;

/*
 * Created on 14 Jul 2012
 */

/**
 * A special type that represents the value <code>null</code>. It should never be possible to declare something (e.g. variable, returned value) to be of this type.
 * @author Anthony Bryant
 */
public class NullType extends Type
{

  public NullType(LexicalPhrase lexicalPhrase)
  {
    super(true, lexicalPhrase);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAssign(Type type)
  {
    return type instanceof NullType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canRuntimeAssign(Type type)
  {
    throw new UnsupportedOperationException("canRuntimeAssign() is undefined for NullType");
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
    return type instanceof NullType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<MemberReference<?>> getMembers(String name)
  {
    return new HashSet<MemberReference<?>>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    return "n";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasDefaultValue()
  {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "null";
  }
}
