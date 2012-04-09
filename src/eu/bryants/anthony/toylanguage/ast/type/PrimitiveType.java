package eu.bryants.anthony.toylanguage.ast.type;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class PrimitiveType extends Type
{
  /**
   * An enum of all possible types of PrimitiveType.
   * @author Anthony Bryant
   */
  public enum PrimitiveTypeType
  {
    BOOLEAN("boolean"),
    DOUBLE("double"),
    INT("int"),
    ;

    public final String name;

    PrimitiveTypeType(String name)
    {
      this.name = name;
    }
  }

  private PrimitiveTypeType primitiveTypeType;

  public PrimitiveType(PrimitiveTypeType primitiveTypeType, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.primitiveTypeType = primitiveTypeType;
  }

  /**
   * @return the primitiveTypeType
   */
  public PrimitiveTypeType getPrimitiveTypeType()
  {
    return primitiveTypeType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAssign(Type type)
  {
    if (!(type instanceof PrimitiveType))
    {
      return false;
    }
    PrimitiveType primitive = (PrimitiveType) type;
    return primitive.getPrimitiveTypeType() == primitiveTypeType;
  }

  @Override
  public String toString()
  {
    return primitiveTypeType.name;
  }
}
