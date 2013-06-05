package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.member.Member;

/*
 * Created on 10 Apr 2013
 */

/**
 * @author Anthony Bryant
 * @param <M> - the type of member being referenced
 */
public abstract class MemberReference<M extends Member>
{

  private M referencedMember;

  /**
   * Creates a new MemberReference with the specified referenced member
   * @param referencedMember - the referenced member
   */
  public MemberReference(M referencedMember)
  {
    this.referencedMember = referencedMember;
  }

  /**
   * @return the referencedMember
   */
  public M getReferencedMember()
  {
    return referencedMember;
  }
}
