package eu.bryants.anthony.plinth.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.Member;

/*
 * Created on 7 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class VoidType extends Type
{

  public static final VoidType VOID_TYPE = new VoidType(null);

  public VoidType(LexicalPhrase lexicalPhrase)
  {
    super(false, lexicalPhrase);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAssign(Type type)
  {
    return false;
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
    return type instanceof VoidType;
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
    return "v";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasDefaultValue()
  {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "void";
  }
}
