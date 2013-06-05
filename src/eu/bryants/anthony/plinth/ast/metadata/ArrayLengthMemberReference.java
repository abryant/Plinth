package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.member.ArrayLengthMember;

/*
 * Created on 10 Apr 2013
 */

/**
 * @author Anthony Bryant
 */
public class ArrayLengthMemberReference extends MemberReference<ArrayLengthMember>
{

  /**
   * Creates a new ArrayLengthMemberReference to hold a reference to an ArrayLengthMember.
   * @param referencedArrayLengthMember - the referenced ArrayLengthMember
   */
  public ArrayLengthMemberReference(ArrayLengthMember referencedArrayLengthMember)
  {
    super(referencedArrayLengthMember);
  }

}
