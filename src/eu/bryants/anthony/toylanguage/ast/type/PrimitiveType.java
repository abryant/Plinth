package eu.bryants.anthony.toylanguage.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Member;

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
    BOOLEAN("boolean", "z", false, 1,  false),
    UBYTE  ("ubyte",   "B", false, 8,  false),
    USHORT ("ushort",  "S", false, 16, false),
    UINT   ("uint",    "I", false, 32, false),
    ULONG  ("ulong",   "L", false, 64, false),
    BYTE   ("byte",    "b", false, 8,  true),
    SHORT  ("short",   "s", false, 16, true),
    INT    ("int",     "i", false, 32, true),
    LONG   ("long",    "l", false, 64, true),
    FLOAT  ("float",   "f", true,  32, true),
    DOUBLE ("double",  "d", true,  64, true),
    ;

    public final String name;
    public final String mangledName;
    private boolean floating;
    private int bitCount;
    private boolean signed;

    PrimitiveTypeType(String name, String mangledName, boolean floating, int bitCount, boolean signed)
    {
      this.name = name;
      this.mangledName = mangledName;
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

    /**
     * Finds the PrimitiveTypeType with the specified name.
     * @param name - the name of the PrimitiveTypeType to find
     * @return the PrimitiveTypeType with the specified name, or null if none exists with that name.
     */
    public static PrimitiveTypeType getByName(String name)
    {
      for (PrimitiveTypeType type : values())
      {
        if (type.name.equals(name))
        {
          return type;
        }
      }
      return null;
    }
  }

  private PrimitiveTypeType primitiveTypeType;

  public PrimitiveType(boolean nullable, PrimitiveTypeType primitiveTypeType, LexicalPhrase lexicalPhrase)
  {
    super(nullable, lexicalPhrase);
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
    if (type instanceof NullType && isNullable())
    {
      // all nullable types can have null assigned to them
      return true;
    }
    if (!(type instanceof PrimitiveType))
    {
      return false;
    }
    PrimitiveTypeType otherType = ((PrimitiveType) type).getPrimitiveTypeType();
    // a nullable type cannot be assigned to a non-nullable type
    if (!isNullable() && type.isNullable())
    {
      return false;
    }

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
    // smaller unsigned integers can always be assigned to larger integers
    if (primitiveTypeType.getBitCount() > otherType.getBitCount() && !otherType.isSigned())
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
      return otherType == PrimitiveTypeType.ULONG  ||
             otherType == PrimitiveTypeType.UINT   ||
             otherType == PrimitiveTypeType.USHORT ||
             otherType == PrimitiveTypeType.UBYTE;
    case LONG:
      return otherType == PrimitiveTypeType.LONG  ||
             otherType == PrimitiveTypeType.INT   || otherType == PrimitiveTypeType.UINT   ||
             otherType == PrimitiveTypeType.SHORT || otherType == PrimitiveTypeType.USHORT ||
             otherType == PrimitiveTypeType.BYTE  || otherType == PrimitiveTypeType.UBYTE;
    case UINT:
      return otherType == PrimitiveTypeType.UINT   ||
             otherType == PrimitiveTypeType.USHORT ||
             otherType == PrimitiveTypeType.UBYTE;
    case INT:
      return otherType == PrimitiveTypeType.INT   ||
             otherType == PrimitiveTypeType.SHORT || otherType == PrimitiveTypeType.USHORT ||
             otherType == PrimitiveTypeType.BYTE  || otherType == PrimitiveTypeType.UBYTE;
    case USHORT:
      return otherType == PrimitiveTypeType.USHORT ||
             otherType == PrimitiveTypeType.UBYTE;
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
    return type instanceof PrimitiveType && isNullable() == type.isNullable() && ((PrimitiveType) type).getPrimitiveTypeType() == primitiveTypeType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Member> getMembers(String name)
  {
    // primitive types currently have no members
    return new HashSet<Member>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    return (isNullable() ? "x" : "") + primitiveTypeType.mangledName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasDefaultValue()
  {
    return true;
  }

  @Override
  public String toString()
  {
    return (isNullable() ? "?" : "") + primitiveTypeType.name;
  }
}
