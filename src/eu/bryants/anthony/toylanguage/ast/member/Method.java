package eu.bryants.anthony.toylanguage.ast.member;

import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.type.Type;

/*
 * Created on 20 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class Method extends Member
{

  private Type returnType;
  private String name;
  private boolean isStatic;
  private String nativeName;
  private Parameter[] parameters;
  private Block block;

  private CompoundDefinition containingDefinition;

  /**
   * Creates a new Method with the specified parameters
   * @param returnType - the return type of the method
   * @param name - the name of the method
   * @param isStatic - true if the method should be static, false otherwise
   * @param nativeName - the native name of the method, or null if no native name is specified
   * @param parameters - the parameters for the method
   * @param block - the block that the method should run, or null if no block is specified
   * @param lexicalPhrase - the LexicalPhrase of this method
   */
  public Method(Type returnType, String name, boolean isStatic, String nativeName, Parameter[] parameters, Block block, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.returnType = returnType;
    this.name = name;
    this.isStatic = isStatic;
    this.nativeName = nativeName;
    this.parameters = parameters;
    for (int i = 0; i < parameters.length; i++)
    {
      parameters[i].setIndex(i);
    }
    this.block = block;
  }

  /**
   * @return the returnType
   */
  public Type getReturnType()
  {
    return returnType;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the isStatic
   */
  public boolean isStatic()
  {
    return isStatic;
  }

  /**
   * @return the nativeName
   */
  public String getNativeName()
  {
    return nativeName;
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
   * @return the mangled name for this Method
   */
  public String getMangledName()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append(containingDefinition.getName());
    buffer.append('$');
    buffer.append(name);
    if (isStatic)
    {
      buffer.append(":");
    }
    else
    {
      buffer.append('$');
    }
    buffer.append(returnType.getMangledName());
    buffer.append('_');
    for (Parameter p : parameters)
    {
      buffer.append(p.getType().getMangledName());
    }
    return buffer.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    if (isStatic)
    {
      buffer.append("static ");
    }
    if (nativeName != null)
    {
      buffer.append("native \"");
      buffer.append(nativeName);
      buffer.append("\" ");
    }
    buffer.append(returnType);
    buffer.append(' ');
    buffer.append(name);
    buffer.append('(');
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
