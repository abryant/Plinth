package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.misc.QName;
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
  private QName qname;
  private Expression[] arguments;

  private NamedType resolvedType;
  private Constructor resolvedConstructor;

  public CreationExpression(boolean isHeapAllocation, QName qname, Expression[] arguments, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.isHeapAllocation = isHeapAllocation;
    this.qname = qname;
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
   * @return the qname
   */
  public QName getQualifiedName()
  {
    return qname;
  }

  /**
   * @return the arguments
   */
  public Expression[] getArguments()
  {
    return arguments;
  }

  /**
   * @return the resolvedType
   */
  public NamedType getResolvedType()
  {
    return resolvedType;
  }

  /**
   * @param resolvedType - the resolvedType to set
   */
  public void setResolvedType(NamedType resolvedType)
  {
    this.resolvedType = resolvedType;
  }

  /**
   * @return the resolvedConstructor
   */
  public Constructor getResolvedConstructor()
  {
    return resolvedConstructor;
  }

  /**
   * @param resolvedConstructor - the resolvedConstructor to set
   */
  public void setResolvedConstructor(Constructor resolvedConstructor)
  {
    this.resolvedConstructor = resolvedConstructor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer(isHeapAllocation ? "new " : "create ");
    buffer.append(qname);
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
