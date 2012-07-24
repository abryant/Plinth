package eu.bryants.anthony.toylanguage.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Member;

/*
 * Created on 14 Jul 2012
 */

/**
 * A special type that represents the value <code>null</code>. This should never be used anywhere except for the type of this value.
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
  public boolean isEquivalent(Type type)
  {
    return type instanceof NullType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Member> getMembers(String name)
  {
    return new HashSet<Member>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    throw new UnsupportedOperationException("The null type does not have a mangled name");
  }

}
