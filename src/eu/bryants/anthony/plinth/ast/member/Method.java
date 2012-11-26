package eu.bryants.anthony.plinth.ast.member;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.type.Type;

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
  private boolean isImmutable;
  private String nativeName;
  private Parameter[] parameters;
  private Block block;

  private TypeDefinition containingTypeDefinition;

  /**
   * Creates a new Method with the specified parameters
   * @param returnType - the return type of the method
   * @param name - the name of the method
   * @param isStatic - true if the method should be static, false otherwise
   * @param isImmutable - true if the method should be immutable, false otherwise
   * @param nativeName - the native name of the method, or null if no native name is specified
   * @param parameters - the parameters for the method
   * @param block - the block that the method should run, or null if no block is specified
   * @param lexicalPhrase - the LexicalPhrase of this method
   */
  public Method(Type returnType, String name, boolean isStatic, boolean isImmutable, String nativeName, Parameter[] parameters, Block block, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.returnType = returnType;
    this.name = name;
    this.isStatic = isStatic;
    this.isImmutable = isImmutable;
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
   * This methods sets the immutability of this Method. It should only be used when adding the Method to an immutable type.
   * @param isImmutable - true if this Method should be immutable, false otherwise
   */
  public void setImmutable(boolean isImmutable)
  {
    this.isImmutable = isImmutable;
  }

  /**
   * @return the isImmutable
   */
  public boolean isImmutable()
  {
    return isImmutable;
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
   * @return the mangled name for this Method
   */
  public String getMangledName()
  {
    StringBuffer buffer = new StringBuffer();
    if (isStatic)
    {
      buffer.append("_SM");
    }
    else
    {
      buffer.append("_M");
    }
    buffer.append(containingTypeDefinition.getQualifiedName().getMangledName());
    buffer.append('_');
    buffer.append(name);
    buffer.append('_');
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
    if (isImmutable)
    {
      buffer.append("immutable ");
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
    buffer.append(')');
    if (block == null)
    {
      buffer.append(';');
    }
    else
    {
      buffer.append('\n');
      buffer.append(block);
    }
    return buffer.toString();
  }
}
