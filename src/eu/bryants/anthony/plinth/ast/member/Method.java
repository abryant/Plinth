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
  private int methodIndex;

  private Disambiguator disambiguator = new Disambiguator();

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
   * @return the disambiguator for this Method
   */
  public Disambiguator getDisambiguator()
  {
    return disambiguator;
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
   * @return the methodIndex
   */
  public int getMethodIndex()
  {
    return methodIndex;
  }

  /**
   * @param methodIndex - the methodIndex to set
   */
  public void setMethodIndex(int methodIndex)
  {
    this.methodIndex = methodIndex;
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

  /**
   * A disambiguator for method calls, which allows methods which are semantically equivalent (i.e. have the same name and types) can be easily distinguished.
   * It also allows methods to be sorted into a predictable order, by implementing comparable.
   * @author Anthony Bryant
   */
  public class Disambiguator implements Comparable<Disambiguator>
  {

    /**
     * @return the name associated with this Disambiguator
     */
    public String getName()
    {
      return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Disambiguator other)
    {
      Method otherMethod = other.getMethod();
      // compare name, then mangled return type, then each mangled parameter in turn, using lexicographic ordering
      // if they are equal except that one parameter list is a prefix of the other, then the comparison makes the longer parameter list larger
      int nameComparison = name.compareTo(otherMethod.name);
      if (nameComparison != 0)
      {
        return nameComparison;
      }
      int returnTypeComparison = returnType.getMangledName().compareTo(otherMethod.returnType.getMangledName());
      if (returnTypeComparison != 0)
      {
        return returnTypeComparison;
      }

      for (int i = 0; i < parameters.length & i < otherMethod.parameters.length; ++i)
      {
        int paramComparison = parameters[i].getType().getMangledName().compareTo(otherMethod.parameters[i].getType().getMangledName());
        if (paramComparison != 0)
        {
          return paramComparison;
        }
      }
      return parameters.length - otherMethod.parameters.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
      StringBuffer buffer = new StringBuffer();
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
     * @return the enclosing Method, for use only in equals() and compareTo()
     */
    private Method getMethod()
    {
      return Method.this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o)
    {
      if (!(o instanceof Disambiguator))
      {
        return false;
      }
      Disambiguator other = (Disambiguator) o;
      Method otherMethod = other.getMethod();
      if (!returnType.isEquivalent(otherMethod.returnType) || !name.equals(otherMethod.name) || parameters.length != otherMethod.parameters.length)
      {
        return false;
      }
      for (int i = 0; i < parameters.length; ++i)
      {
        if (!parameters[i].getType().isEquivalent(otherMethod.parameters[i].getType()))
        {
          return false;
        }
      }
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
      return name.hashCode();
    }
  }
}
