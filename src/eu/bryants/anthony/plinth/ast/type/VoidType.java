package eu.bryants.anthony.plinth.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;

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
  public boolean canRuntimeAssign(Type type)
  {
    throw new UnsupportedOperationException("canRuntimeAssign() is undefined for VoidType");
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
