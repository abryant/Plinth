package eu.bryants.anthony.plinth.ast.type;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod.BuiltinMethodType;
import eu.bryants.anthony.plinth.ast.metadata.GenericTypeSpecialiser;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;

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
    BOOLEAN("boolean", "z", false, 1,  false, (byte)  1),
    UBYTE  ("ubyte",   "B", false, 8,  false, (byte)  2),
    USHORT ("ushort",  "S", false, 16, false, (byte)  3),
    UINT   ("uint",    "I", false, 32, false, (byte)  4),
    ULONG  ("ulong",   "L", false, 64, false, (byte)  5),
    BYTE   ("byte",    "b", false, 8,  true,  (byte)  6),
    SHORT  ("short",   "s", false, 16, true,  (byte)  7),
    INT    ("int",     "i", false, 32, true,  (byte)  8),
    LONG   ("long",    "l", false, 64, true,  (byte)  9),
    FLOAT  ("float",   "f", true,  32, true,  (byte) 10),
    DOUBLE ("double",  "d", true,  64, true,  (byte) 11),
    ;

    public final String name;
    public final String mangledName;
    private boolean floating;
    private int bitCount;
    private boolean signed;
    private byte runTimeId;

    PrimitiveTypeType(String name, String mangledName, boolean floating, int bitCount, boolean signed, byte runTimeId)
    {
      this.name = name;
      this.mangledName = mangledName;
      this.floating = floating;
      this.bitCount = bitCount;
      this.signed = signed;
      this.runTimeId = runTimeId;
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
     * @return the runTimeId
     */
    public byte getRunTimeId()
    {
      return runTimeId;
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
    // a nullable type cannot be assigned to a non-nullable type
    if (!isNullable() && type.canBeNullable())
    {
      return false;
    }

    if (type instanceof WildcardType)
    {
      // we have already checked the nullability constraint, so just make sure that the wildcard type is a sub-type of this primitive type
      return ((WildcardType) type).canBeAssignedTo(this);
    }

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
    return isRuntimeEquivalent(type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRuntimeEquivalent(Type type)
  {
    return type instanceof PrimitiveType && isNullable() == type.isNullable() && ((PrimitiveType) type).getPrimitiveTypeType() == primitiveTypeType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<MemberReference<?>> getMembers(String name)
  {
    PrimitiveType notNullThis = new PrimitiveType(false, primitiveTypeType, null);

    HashSet<MemberReference<?>> members = new HashSet<MemberReference<?>>();
    if (name.equals(BuiltinMethodType.TO_STRING.methodName))
    {
      members.add(new MethodReference(new BuiltinMethod(notNullThis, BuiltinMethodType.TO_STRING), GenericTypeSpecialiser.IDENTITY_SPECIALISER));
    }
    if (name.equals(BuiltinMethodType.TO_STRING_RADIX.methodName) &&
        primitiveTypeType != PrimitiveTypeType.BOOLEAN && !primitiveTypeType.isFloating())
    {
      members.add(new MethodReference(new BuiltinMethod(notNullThis, BuiltinMethodType.TO_STRING_RADIX), GenericTypeSpecialiser.IDENTITY_SPECIALISER));
    }
    return members;
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
