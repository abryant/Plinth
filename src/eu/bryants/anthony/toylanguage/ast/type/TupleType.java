package eu.bryants.anthony.toylanguage.ast.type;

import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 4 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class TupleType extends Type
{

  private Type[] subTypes;

  public TupleType(Type[] subTypes, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.subTypes = subTypes;
  }

  /**
   * @return the subTypes
   */
  public Type[] getSubTypes()
  {
    return subTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAssign(Type type)
  {
    if (!(type instanceof TupleType))
    {
      return subTypes.length == 1 && subTypes[0].canAssign(type);
    }
    TupleType otherTuple = (TupleType) type;
    if (subTypes.length != otherTuple.subTypes.length)
    {
      return false;
    }
    for (int i = 0; i < subTypes.length; i++)
    {
      if (!subTypes[i].canAssign(otherTuple.subTypes[i]))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEquivalent(Type type)
  {
    if (!(type instanceof TupleType))
    {
      return false;
    }
    TupleType otherType = (TupleType) type;
    if (subTypes.length != otherType.subTypes.length)
    {
      return false;
    }
    for (int i = 0; i < subTypes.length; i++)
    {
      if (!subTypes[i].isEquivalent(otherType.getSubTypes()[i]))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Member getMember(String name)
  {
    // tuple types currently have no members
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer("(");
    for (int i = 0; i < subTypes.length; i++)
    {
      buffer.append(subTypes[i]);
      if (i != subTypes.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(")");
    return buffer.toString();
  }
}
