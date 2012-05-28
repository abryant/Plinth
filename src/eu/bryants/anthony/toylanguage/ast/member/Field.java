package eu.bryants.anthony.toylanguage.ast.member;

import eu.bryants.anthony.toylanguage.ast.metadata.MemberVariable;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class Field extends Member
{

  private Type type;
  private String name;
  private boolean isStatic;

  private MemberVariable memberVariable;
  private int index;

  public Field(Type type, String name, boolean isStatic, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.type = type;
    this.name = name;
    this.isStatic = isStatic;
  }

  /**
   * @return the type
   */
  public Type getType()
  {
    return type;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the isStatic
   */
  public boolean isStatic()
  {
    return isStatic;
  }

  /**
   * @return the index
   */
  public int getIndex()
  {
    return index;
  }

  /**
   * @param index - the index to set
   */
  public void setIndex(int index)
  {
    this.index = index;
  }

  /**
   * @return the memberVariable
   */
  public MemberVariable getMemberVariable()
  {
    return memberVariable;
  }

  /**
   * @param memberVariable - the memberVariable to set
   */
  public void setMemberVariable(MemberVariable memberVariable)
  {
    this.memberVariable = memberVariable;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return (isStatic ? "static " : "") + type + " " + name + ";";
  }
}
