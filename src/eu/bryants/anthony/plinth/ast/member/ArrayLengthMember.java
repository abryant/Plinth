package eu.bryants.anthony.plinth.ast.member;

import eu.bryants.anthony.plinth.ast.metadata.ArrayLengthMemberReference;
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

  public static final String LENGTH_FIELD_NAME = "length";
  public static final ArrayLengthMemberReference LENGTH_MEMBER_REFERENCE = new ArrayLengthMemberReference(new ArrayLengthMember());
}
