package eu.bryants.anthony.toylanguage.ast.type;

import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class PrimitiveType extends Type
{
  /**
   * An enum of all possible types of PrimitiveType.
   * @author Anthony Bryant
   */
  public enum PrimitiveTypeType
  {
    BOOLEAN("boolean", false, 1,  false),
    UBYTE  ("ubyte",   false, 8,  false),
    USHORT ("ushort",  false, 16, false),
    UINT   ("uint",    false, 32, false),
    ULONG  ("ulong",   false, 64, false),
    BYTE   ("byte",    false, 8,  true),
    SHORT  ("short",   false, 16, true),
    INT    ("int",     false, 32, true),
    LONG   ("long",    false, 64, true),
    FLOAT  ("float",   true,  32, true),
    DOUBLE ("double",  true,  64, true),
    ;

    public final String name;
    private boolean floating;
    private int bitCount;
    private boolean signed;

    PrimitiveTypeType(String name, boolean floating, int bitCount, boolean signed)
    {
      this.name = name;
      this.floating = floating;
      this.bitCount = bitCount;
      this.signed = signed;
    }

    /**
     * @return the floating
     */
    public boolean isFloating()
    {
      return floating;
    }

    /**
     * @return the bitCount
     */
    public int getBitCount()
    {
      return bitCount;
    }

    /**
     * @return the signed
     */
    public boolean isSigned()
    {
      return signed;
    }
  }

  private PrimitiveTypeType primitiveTypeType;

  public PrimitiveType(PrimitiveTypeType primitiveTypeType, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.primitiveTypeType = primitiveTypeType;
  }

  /**
   * @return the primitiveTypeType
   */
  public PrimitiveTypeType getPrimitiveTypeType()
  {
    return primitiveTypeType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAssign(Type type)
  {
    if (!(type instanceof PrimitiveType))
    {
      return false;
    }
    PrimitiveTypeType otherType = ((PrimitiveType) type).getPrimitiveTypeType();
    // a boolean can only be assigned to a boolean
    // also, only a boolean can be assigned to a boolean
    // (if either of them are booleans, they must both be booleans to be assignment compatible)
    if (primitiveTypeType == PrimitiveTypeType.BOOLEAN || otherType == PrimitiveTypeType.BOOLEAN)
    {
      return primitiveTypeType == PrimitiveTypeType.BOOLEAN && otherType == PrimitiveTypeType.BOOLEAN;
    }
    // floating point types are only assign-compatible if a smaller type is being assigned to a larger type (or they are equal sizes)
    if (primitiveTypeType.isFloating() && otherType.isFloating())
    {
      return primitiveTypeType.getBitCount() >= otherType.getBitCount();
    }
    // integer types can always be assigned to floating point types
    if (primitiveTypeType.isFloating())
    {
      return true;
    }
    // floating point types can never be assigned to integer types
    if (otherType.isFloating())
    {
      return false;
    }
    // both types are now integers
    // smaller integers can always be assigned to larger integers
    if (primitiveTypeType.getBitCount() > otherType.getBitCount())
    {
      return true;
    }
    if (primitiveTypeType.getBitCount() < otherType.getBitCount())
    {
      return false;
    }
    return primitiveTypeType.isSigned() == otherType.isSigned();

    /* or, as a lookup table:
    switch (primitiveTypeType)
    {
    case BOOLEAN:
      return otherType == PrimitiveTypeType.BOOLEAN;
    case DOUBLE:
      return otherType == PrimitiveTypeType.DOUBLE || otherType == PrimitiveTypeType.FLOAT  ||
             otherType == PrimitiveTypeType.LONG   || otherType == PrimitiveTypeType.ULONG  ||
             otherType == PrimitiveTypeType.INT    || otherType == PrimitiveTypeType.UINT   ||
             otherType == PrimitiveTypeType.SHORT  || otherType == PrimitiveTypeType.USHORT ||
             otherType == PrimitiveTypeType.BYTE   || otherType == PrimitiveTypeType.UBYTE;
    case FLOAT:
      return otherType == PrimitiveTypeType.FLOAT ||
             otherType == PrimitiveTypeType.LONG  || otherType == PrimitiveTypeType.ULONG  ||
             otherType == PrimitiveTypeType.INT   || otherType == PrimitiveTypeType.UINT   ||
             otherType == PrimitiveTypeType.SHORT || otherType == PrimitiveTypeType.USHORT ||
             otherType == PrimitiveTypeType.BYTE  || otherType == PrimitiveTypeType.UBYTE;
    case ULONG:
      return otherType == PrimitiveTypeType.ULONG ||
             otherType == PrimitiveTypeType.INT   || otherType == PrimitiveTypeType.UINT   ||
             otherType == PrimitiveTypeType.SHORT || otherType == PrimitiveTypeType.USHORT ||
             otherType == PrimitiveTypeType.BYTE  || otherType == PrimitiveTypeType.UBYTE;
    case LONG:
      return otherType == PrimitiveTypeType.LONG  ||
             otherType == PrimitiveTypeType.INT   || otherType == PrimitiveTypeType.UINT   ||
             otherType == PrimitiveTypeType.SHORT || otherType == PrimitiveTypeType.USHORT ||
             otherType == PrimitiveTypeType.BYTE  || otherType == PrimitiveTypeType.UBYTE;
    case UINT:
      return otherType == PrimitiveTypeType.UINT  ||
             otherType == PrimitiveTypeType.SHORT || otherType == PrimitiveTypeType.USHORT ||
             otherType == PrimitiveTypeType.BYTE  || otherType == PrimitiveTypeType.UBYTE;
    case INT:
      return otherType == PrimitiveTypeType.INT   ||
             otherType == PrimitiveTypeType.SHORT || otherType == PrimitiveTypeType.USHORT ||
             otherType == PrimitiveTypeType.BYTE  || otherType == PrimitiveTypeType.UBYTE;
    case USHORT:
      return otherType == PrimitiveTypeType.USHORT ||
             otherType == PrimitiveTypeType.BYTE   || otherType == PrimitiveTypeType.UBYTE;
    case SHORT:
      return otherType == PrimitiveTypeType.SHORT ||
             otherType == PrimitiveTypeType.BYTE  || otherType == PrimitiveTypeType.UBYTE;
    case UBYTE:
      return otherType == PrimitiveTypeType.UBYTE;
    case BYTE:
      return otherType == PrimitiveTypeType.BYTE;
    }
    throw new IllegalStateException("Unknown primitive type: " + primitiveTypeType);
    */
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEquivalent(Type type)
  {
    return type instanceof PrimitiveType && ((PrimitiveType) type).getPrimitiveTypeType() == primitiveTypeType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Member getMember(String name)
  {
    // primitive types currently have no members
    return null;
  }

  @Override
  public String toString()
  {
    return primitiveTypeType.name;
  }
}
