package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.metadata.ConstructorReference;
import eu.bryants.anthony.plinth.ast.misc.Argument;
import eu.bryants.anthony.plinth.ast.type.NamedType;

/*
 * Created on 13 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class CreationExpression extends Expression
{

  private boolean isHeapAllocation;
  private NamedType createdType;
  private Argument[] arguments;

  private ConstructorReference resolvedConstructorReference;

  public CreationExpression(boolean isHeapAllocation, NamedType createdType, Argument[] arguments, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.isHeapAllocation = isHeapAllocation;
    this.createdType = createdType;
    this.arguments = arguments;
  }

  /**
   * @return the isHeapAllocation
   */
  public boolean isHeapAllocation()
  {
    return isHeapAllocation;
  }

  /**
   * @return the createdType
   */
  public NamedType getCreatedType()
  {
    return createdType;
  }

  /**
   * @return the arguments
   */
  public Argument[] getArguments()
  {
    return arguments;
  }

  /**
   * @return the resolvedConstructorReference
   */
  public ConstructorReference getResolvedConstructorReference()
  {
    return resolvedConstructorReference;
  }

  /**
   * @param resolvedConstructorReference - the resolvedConstructorReference to set
   */
  public void setResolvedConstructorReference(ConstructorReference resolvedConstructorReference)
  {
    this.resolvedConstructorReference = resolvedConstructorReference;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer(isHeapAllocation ? "new " : "create ");
    buffer.append(createdType);
    buffer.append('(');
    for (int i = 0; i < arguments.length; ++i)
    {
      buffer.append(arguments[i]);
      if (i != arguments.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(')');
    return buffer.toString();
  }
}
