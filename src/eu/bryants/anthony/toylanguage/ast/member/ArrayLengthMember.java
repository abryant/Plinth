package eu.bryants.anthony.toylanguage.ast.member;

import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.toylanguage.ast.type.Type;

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

  public static final PrimitiveType ARRAY_LENGTH_TYPE = new PrimitiveType(PrimitiveTypeType.UINT, null);

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType()
  {
    return ARRAY_LENGTH_TYPE;
  }
}
