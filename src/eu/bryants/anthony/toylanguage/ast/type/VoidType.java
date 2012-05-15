package eu.bryants.anthony.toylanguage.ast.type;

import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

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
    super(lexicalPhrase);
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
    return type instanceof VoidType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Member getMember(String name)
  {
    return null;
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
  public String toString()
  {
    return "void";
  }
}