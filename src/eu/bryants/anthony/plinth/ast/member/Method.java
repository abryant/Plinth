package eu.bryants.anthony.plinth.ast.member;

import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunction;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;
import eu.bryants.anthony.plinth.ast.type.NamedType;
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
  private boolean isAbstract;
  private boolean isStatic;
  private boolean isImmutable;
  private String nativeName;
  private SinceSpecifier sinceSpecifier;
  private Parameter[] parameters;
  private NamedType[] checkedThrownTypes;
  private NamedType[] uncheckedThrownTypes;
  private Block block;

  private TypeDefinition containingTypeDefinition;

  private MemberFunction memberFunction;

  /**
   * Creates a new Method with the specified parameters
   * @param returnType - the return type of the method
   * @param name - the name of the method
   * @param isAbstract - true if the method should be abstract, false otherwise
   * @param isStatic - true if the method should be static, false otherwise
   * @param isImmutable - true if the method should be immutable, false otherwise
   * @param nativeName - the native name of the method, or null if no native name is specified
   * @param sinceSpecifier - the since specifier of the method, or null if none is given
   * @param parameters - the parameters for the method
   * @param checkedThrownTypes - the exception types that this method can throw as checked
   * @param uncheckedThrownTypes - the exception types that this method can throw as unchecked
   * @param block - the block that the method should run, or null if no block is specified
   * @param lexicalPhrase - the LexicalPhrase of this method
   */
  public Method(Type returnType, String name, boolean isAbstract, boolean isStatic, boolean isImmutable, String nativeName, SinceSpecifier sinceSpecifier, Parameter[] parameters, NamedType[] checkedThrownTypes, NamedType[] uncheckedThrownTypes, Block block, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.returnType = returnType;
    this.name = name;
    this.isAbstract = isAbstract;
    this.isStatic = isStatic;
    this.isImmutable = isImmutable;
    this.nativeName = nativeName;
    this.sinceSpecifier = sinceSpecifier;
    this.parameters = parameters;
    for (int i = 0; i < parameters.length; i++)
    {
      parameters[i].setIndex(i);
    }
    this.checkedThrownTypes = checkedThrownTypes;
    this.uncheckedThrownTypes = uncheckedThrownTypes;
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
   * @return the isAbstract
   */
  public boolean isAbstract()
  {
    return isAbstract;
  }

  /**
   * @param isAbstract - the isAbstract to set
   */
  public void setAbstract(boolean isAbstract)
  {
    this.isAbstract = isAbstract;
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
   * @return the checkedThrownTypes
   */
  public NamedType[] getCheckedThrownTypes()
  {
    return checkedThrownTypes;
  }

  /**
   * @return the uncheckedThrownTypes
   */
  public NamedType[] getUncheckedThrownTypes()
  {
    return uncheckedThrownTypes;
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
   * @return the memberFunction
   */
  public MemberFunction getMemberFunction()
  {
    return memberFunction;
  }

  /**
   * @param memberFunction - the memberFunction to set
   */
  public void setMemberFunction(MemberFunction memberFunction)
  {
    this.memberFunction = memberFunction;
  }

  /**
   * @return the descriptor string for this method, which should be used in the virtual function table descriptor for this method's class
   */
  public String getDescriptorString()
  {
    StringBuffer buffer = new StringBuffer();
    if (!isStatic && containingTypeDefinition instanceof InterfaceDefinition)
    {
      // non-static interface methods must have a unique descriptor, since their calling convention depends on which interface they are part of
      buffer.append('I');
      buffer.append(containingTypeDefinition.getQualifiedName().getMangledName());
    }
    buffer.append(isStatic ? "SM_" : "M_");
    buffer.append(name);
    buffer.append('_');
    buffer.append(returnType.getMangledName());
    buffer.append('_');
    for (int i = 0; i < parameters.length; ++i)
    {
      buffer.append(parameters[i].getType().getMangledName());
    }
    return buffer.toString();
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
    if (isStatic)
    {
      if (sinceSpecifier != null)
      {
        buffer.append(sinceSpecifier.getMangledName());
      }
      buffer.append('_');
    }
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
    if (isAbstract)
    {
      buffer.append("abstract ");
    }
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
    if (sinceSpecifier != null)
    {
      buffer.append(sinceSpecifier);
      buffer.append(' ');
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
    if (checkedThrownTypes.length > 0 || uncheckedThrownTypes.length > 0)
    {
      buffer.append(" throws ");
      for (int i = 0; i < checkedThrownTypes.length; ++i)
      {
        buffer.append(checkedThrownTypes[i]);
        if (i != checkedThrownTypes.length - 1 || uncheckedThrownTypes.length > 0)
        {
          buffer.append(", ");
        }
      }
      for (int i = 0; i < uncheckedThrownTypes.length; ++i)
      {
        buffer.append("unchecked ");
        buffer.append(uncheckedThrownTypes[i]);
        if (i != uncheckedThrownTypes.length - 1)
        {
          buffer.append(", ");
        }
      }
    }
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
