package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Field;

/*
 * Created on 28 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class GlobalVariable extends Variable
{

  private TypeDefinition enclosingTypeDefinition;
  private Field field;

  public GlobalVariable(Field field, TypeDefinition enclosingTypeDefinition)
  {
    super(field.isFinal(), field.getType(), field.getName());
    this.enclosingTypeDefinition = enclosingTypeDefinition;
    this.field = field;
  }

  /**
   * @return the enclosing TypeDefinition
   */
  public TypeDefinition getEnclosingTypeDefinition()
  {
    return enclosingTypeDefinition;
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
    return "_G" + enclosingTypeDefinition.getQualifiedName().getMangledName() + '_' + field.getName() + '_' + getType().getMangledName();
  }

}
