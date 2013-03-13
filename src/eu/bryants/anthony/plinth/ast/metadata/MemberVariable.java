package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Property;

/*
 * Created on 11 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class MemberVariable extends Variable
{

  private TypeDefinition enclosingTypeDefinition;
  private Field field;
  private Property property;
  private int memberIndex;

  public MemberVariable(Field field)
  {
    super(field.isFinal(), field.getType(), field.getName());
    this.field = field;
  }

  public MemberVariable(Property property)
  {
    // this MemberVariable is not final, since it is impossible to mark a Property's backing variable as final
    super(false, property.getType(), property.getName());
    this.property = property;
  }

  /**
   * @return the enclosing TypeDefinition
   */
  public TypeDefinition getEnclosingTypeDefinition()
  {
    return enclosingTypeDefinition;
  }

  /**
   * @param enclosingTypeDefinition - the enclosingTypeDefinition to set
   */
  public void setEnclosingTypeDefinition(TypeDefinition enclosingTypeDefinition)
  {
    this.enclosingTypeDefinition = enclosingTypeDefinition;
  }

  /**
   * @return the field
   */
  public Field getField()
  {
    return field;
  }

  /**
   * @return the property
   */
  public Property getProperty()
  {
    return property;
  }

  /**
   * @return the memberIndex
   */
  public int getMemberIndex()
  {
    return memberIndex;
  }

  /**
   * @param memberIndex - the memberIndex to set
   */
  public void setMemberIndex(int memberIndex)
  {
    this.memberIndex = memberIndex;
  }

}
