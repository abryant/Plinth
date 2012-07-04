package eu.bryants.anthony.toylanguage.ast.metadata;

import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.member.Field;

/*
 * Created on 28 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class GlobalVariable extends Variable
{

  private CompoundDefinition enclosingDefinition;
  private Field field;

  public GlobalVariable(Field field, CompoundDefinition enclosingDefinition)
  {
    super(field.getType(), field.getName());
    this.enclosingDefinition = enclosingDefinition;
    this.field = field;
  }

  /**
   * @return the enclosingDefinition
   */
  public CompoundDefinition getCompoundDefinition()
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

  /**
   * @return the mangled name of this global variable
   */
  public String getMangledName()
  {
    return enclosingDefinition.getName() + "$" + field.getName() + "$" + getType().getMangledName();
  }

}
