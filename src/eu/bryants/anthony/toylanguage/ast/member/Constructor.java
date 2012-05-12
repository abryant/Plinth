package eu.bryants.anthony.toylanguage.ast.member;

import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

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

  private CompoundDefinition containingDefinition;

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
   * @return the containingDefinition
   */
  public CompoundDefinition getContainingDefinition()
  {
    return containingDefinition;
  }

  /**
   * @param containingDefinition - the containingDefinition to set
   */
  public void setContainingDefinition(CompoundDefinition containingDefinition)
  {
    this.containingDefinition = containingDefinition;
  }

  /**
   * @return the mangled name for this constructor. This should be appended to the type name to get the full mangled name for the low level function, and should never be used on its own.
   */
  public String getMangledName()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append(containingDefinition.getName());
    buffer.append("_construct_");
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
    buffer.append(block);
    return buffer.toString();
  }
}
