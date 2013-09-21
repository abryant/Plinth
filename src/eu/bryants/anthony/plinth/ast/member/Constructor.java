package eu.bryants.anthony.plinth.ast.member;

import java.util.Arrays;
import java.util.Comparator;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.misc.DefaultParameter;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;
import eu.bryants.anthony.plinth.ast.type.NamedType;

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
  private NamedType[] checkedThrownTypes;
  private NamedType[] uncheckedThrownTypes;
  private Block block;

  private TypeDefinition containingTypeDefinition;
  private boolean callsDelegateConstructor;

  public Constructor(boolean isImmutable, boolean isSelfish, SinceSpecifier sinceSpecifier, Parameter[] parameters, NamedType[] checkedThrownTypes, NamedType[] uncheckedThrownTypes, Block block, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.isImmutable = isImmutable;
    this.isSelfish = isSelfish;
    this.sinceSpecifier = sinceSpecifier;

    this.parameters = parameters;
    // we need to determine the indices of the parameters for the constructor ABI,
    // for default parameters, this requires sorting them by name
    // however, we don't want to sort our Parameter[] by name, as it would lose all of the
    // information about the order of evaluation for the DefaultParameters' expressions
    // instead, sort a copy of the array, and use that ordering to set index properties on the Parameter objects
    Parameter[] sortedParameters = new Parameter[parameters.length];
    System.arraycopy(parameters, 0, sortedParameters, 0, parameters.length);
    // perform a stable sort, that will only reorder the DefaultParameters into the correct name order,
    // and will keep all other parameters in the order that they originally occurred in the parameters array
    // note: this sort will put all default parameters at the end of the sorted array
    //       this is fine, as the parameter indices we are setting here will not be used until after the resolver
    //       has checked that the user didn't specify any non-default parameters after the first default parameter
    Arrays.sort(sortedParameters, new Comparator<Parameter>()
    {
      @Override
      public int compare(Parameter o1, Parameter o2)
      {
        if (o1 instanceof DefaultParameter && o2 instanceof DefaultParameter)
        {
          return ((DefaultParameter) o1).getName().compareTo(((DefaultParameter) o2).getName());
        }
        if ((o1 instanceof DefaultParameter) != (o2 instanceof DefaultParameter))
        {
          // DefaultParameters always go at the end
          return (o1 instanceof DefaultParameter) ? 1 : -1;
        }
        return 0;
      }
    });
    // now that we've sorted the default parameters by name, we can assign indices to all of the parameters
    for (int i = 0; i < sortedParameters.length; ++i)
    {
      sortedParameters[i].setIndex(i);
    }

    this.checkedThrownTypes = checkedThrownTypes;
    this.uncheckedThrownTypes = uncheckedThrownTypes;
    this.block = block;
  }

  /**
   * This method sets the immutability of this Constructor. It should only be used when adding the Constructor to an immutable type.
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
      buffer.append(parameter.getMangledName());
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
    buffer.append("constructor(");
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
    buffer.append('\n');
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
