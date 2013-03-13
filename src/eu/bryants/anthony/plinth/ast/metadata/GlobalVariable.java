package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Property;

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
  private Property property;

  public GlobalVariable(Field field)
  {
    super(field.isFinal(), field.getType(), field.getName());
    this.field = field;
  }

  public GlobalVariable(Property property)
  {
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
   * @return the mangled name of this global variable
   */
  public String getMangledName()
  {
    return "_G" + enclosingTypeDefinition.getQualifiedName().getMangledName() + '_' + getName() + '_' + getType().getMangledName();
  }

}
