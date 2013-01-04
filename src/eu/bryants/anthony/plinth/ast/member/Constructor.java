package eu.bryants.anthony.plinth.ast.member;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;

/*
 * Created on 10 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class Constructor extends Member
{
  private boolean isImmutable;
  private boolean isSelfish;
  private SinceSpecifier sinceSpecifier;
  private Parameter[] parameters;
  private Block block;

  private TypeDefinition containingTypeDefinition;
  private boolean callsDelegateConstructor;

  public Constructor(boolean isImmutable, boolean isSelfish, SinceSpecifier sinceSpecifier, Parameter[] parameters, Block block, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.isImmutable = isImmutable;
    this.isSelfish = isSelfish;
    this.sinceSpecifier = sinceSpecifier;
    this.parameters = parameters;
    for (int i = 0; i < parameters.length; i++)
    {
      parameters[i].setIndex(i);
    }
    this.block = block;
  }

  /**
   * This methods sets the immutability of this Constructor. It should only be used when adding the Constructor to an immutable type.
   * @param isImmutable - true if this Constructor should be immutable, false otherwise
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
   * @return the isSelfish
   */
  public boolean isSelfish()
  {
    return isSelfish;
  }

  /**
   * @return the sinceSpecifier
   */
  public SinceSpecifier getSinceSpecifier()
  {
    return sinceSpecifier;
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
   * @return true if this Constructor calls a delegate constructor at any point in its block, false otherwise
   */
  public boolean getCallsDelegateConstructor()
  {
    return callsDelegateConstructor;
  }

  /**
   * @param callsDelegateConstructor - true if this Constructor calls a delegate constructor at any point in its block, false otherwise
   */
  public void setCallsDelegateConstructor(boolean callsDelegateConstructor)
  {
    this.callsDelegateConstructor = callsDelegateConstructor;
  }

  /**
   * @return the mangled name for this constructor
   */
  public String getMangledName()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append("_C");
    if (isSelfish)
    {
      buffer.append("s");
    }
    buffer.append(containingTypeDefinition.getQualifiedName().getMangledName());
    buffer.append('_');
    if (sinceSpecifier != null)
    {
      buffer.append(sinceSpecifier.getMangledName());
    }
    buffer.append('_');
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
    StringBuffer buffer = new StringBuffer();
    if (isImmutable)
    {
      buffer.append("immutable ");
    }
    if (isSelfish)
    {
      buffer.append("selfish ");
    }
    if (sinceSpecifier != null)
    {
      buffer.append(sinceSpecifier);
      buffer.append(' ');
    }
    buffer.append("this(");
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
