package eu.bryants.anthony.plinth.ast.member;

import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;

/*
 * Created on 3 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ArrayLengthMember extends Member
{
  public ArrayLengthMember()
  {
    super(null);
  }

  public static final PrimitiveType ARRAY_LENGTH_TYPE = new PrimitiveType(false, PrimitiveTypeType.UINT, null);
}
