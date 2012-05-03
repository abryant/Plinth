package eu.bryants.anthony.toylanguage.ast.type;

import eu.bryants.anthony.toylanguage.ast.member.ArrayLengthMember;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 3 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ArrayType extends Type
{

  private static final String LENGTH_FIELD_NAME = "length";
  private static final ArrayLengthMember LENGTH_MEMBER = new ArrayLengthMember();

  private Type baseType;

  public ArrayType(Type baseType, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.baseType = baseType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAssign(Type type)
  {
    // only allow assignment if it would work both ways,
    // so we cannot do either of the following (for some Bar extends Foo)

    // 1:
    // []Bar bs = new [1]Bar;
    // []Foo fs = bs;
    // fs[0] = new Foo(); // bs now contains a Foo, despite it being a []Bar

    // 2:
    // []Foo fs = new [1]Foo;
    // fs[0] = new Foo();
    // []Bar bs = fs; // bs now contains a Foo, despite it being a []Bar

    if (!(type instanceof ArrayType))
    {
      return false;
    }
    Type otherBaseType = ((ArrayType) type).getBaseType();
    return baseType.canAssign(otherBaseType) && otherBaseType.canAssign(baseType);
  }

  /**
   * @return the baseType
   */
  public Type getBaseType()
  {
    return baseType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Member getMember(String name)
  {
    if (name.equals(LENGTH_FIELD_NAME))
    {
      return LENGTH_MEMBER;
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "[]" + baseType;
  }
}
