package eu.bryants.anthony.toylanguage.ast.metadata;

import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Initialiser;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.statement.Statement;

/*
 * Created on 25 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class FieldInitialiser extends Initialiser
{

  private Field field;

  /**
   * Creates a new FieldInitialiser to initialise the specified field
   * @param field - the field which should be initialised
   */
  public FieldInitialiser(Field field)
  {
    super(field.isStatic(), new Block(new Statement[0], null), field.getLexicalPhrase());
    this.field = field;
  }

  /**
   * @return the field
   */
  public Field getField()
  {
    return field;
  }

}
