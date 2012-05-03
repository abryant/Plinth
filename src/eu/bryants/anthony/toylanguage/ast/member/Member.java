package eu.bryants.anthony.toylanguage.ast.member;

import eu.bryants.anthony.toylanguage.ast.type.Type;

/*
 * Created on 3 May 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class Member
{
  /**
   * @return the type of the member
   */
  public abstract Type getType();
}
