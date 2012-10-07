package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Field;

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

  public MemberVariable(Field field, TypeDefinition enclosingTypeDefinition)
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

}
