package eu.bryants.anthony.toylanguage.ast.member;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.TypeDefinition;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.statement.Block;

/*
 * Created on 10 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class Constructor extends Member
{
  private String name;
  private Parameter[] parameters;
  private Block block;

  private TypeDefinition containingTypeDefinition;

  public Constructor(String name, Parameter[] parameters, Block block, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.name = name;
    this.parameters = parameters;
    for (int i = 0; i < parameters.length; i++)
    {
      parameters[i].setIndex(i);
    }
    this.block = block;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the parameters
   */
  public Parameter[] getParameters()
  {
    return parameters;
  }

  /**
   * @return the block
   */
  public Block getBlock()
  {
    return block;
  }

  /**
   * @return the containing TypeDefinition
   */
  public TypeDefinition getContainingTypeDefinition()
  {
    return containingTypeDefinition;
  }

  /**
   * @param containingTypeDefinition - the containing TypeDefinition to set
   */
  public void setContainingTypeDefinition(TypeDefinition containingTypeDefinition)
  {
    this.containingTypeDefinition = containingTypeDefinition;
  }

  /**
   * @return the mangled name for this constructor
   */
  public String getMangledName()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append(containingTypeDefinition.getQualifiedName());
    buffer.append("$construct_");
    for (Parameter parameter : parameters)
    {
      buffer.append(parameter.getType().getMangledName());
    }
    return buffer.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer(name);
    buffer.append("(");
    for (int i = 0; i < parameters.length; i++)
    {
      buffer.append(parameters[i]);
      if (i != parameters.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(")\n");
    if (block == null)
    {
      buffer.append("{...}");
    }
    else
    {
      buffer.append(block);
    }
    return buffer.toString();
  }
}
