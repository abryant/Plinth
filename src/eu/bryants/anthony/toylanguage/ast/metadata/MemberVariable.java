package eu.bryants.anthony.toylanguage.ast.metadata;

import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.member.Field;

/*
 * Created on 11 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class MemberVariable extends Variable
{

  private CompoundDefinition enclosingDefinition;
  private Field field;

  public MemberVariable(Field field, CompoundDefinition enclosingDefinition)
  {
    super(field.getType(), field.getName());
    this.enclosingDefinition = enclosingDefinition;
    this.field = field;
  }

  /**
   * @return the enclosingDefinition
   */
  public CompoundDefinition getEnclosingDefinition()
  {
    return enclosingDefinition;
  }

  /**
   * @return the field
   */
  public Field getField()
  {
    return field;
  }

}
