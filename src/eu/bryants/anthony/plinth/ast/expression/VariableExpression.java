package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;
import eu.bryants.anthony.plinth.ast.metadata.Variable;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class VariableExpression extends Expression
{
  private String name;

  // hints for the Resolver
  private Type typeHint; // hints that the variable should have this type
  private Type returnTypeHint; // hints that the variable should be a function which returns this type
  private boolean isFunctionHint; // if true, hints that the variable should be a function
  private boolean isAssignableHint; // if true, hints that the variable can be assigned to


  private Variable resolvedVariable;
  private MemberReference<?> resolvedMemberReference;
  // whether or not this expression is in an immutable context
  // this is used by the TypeChecker to make variables implicitly immutable when in an immutable context
  private boolean resolvedContextImmutability;

  public VariableExpression(String name, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.name = name;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the typeHint
   */
  public Type getTypeHint()
  {
    return typeHint;
  }

  /**
   * @param typeHint - the typeHint to set
   */
  public void setTypeHint(Type typeHint)
  {
    this.typeHint = typeHint;
  }

  /**
   * @return the returnTypeHint
   */
  public Type getReturnTypeHint()
  {
    return returnTypeHint;
  }

  /**
   * @param returnTypeHint - the returnTypeHint to set
   */
  public void setReturnTypeHint(Type returnTypeHint)
  {
    this.returnTypeHint = returnTypeHint;
  }

  /**
   * @return the isFunctionHint
   */
  public boolean getIsFunctionHint()
  {
    return isFunctionHint;
  }

  /**
   * @param isFunctionHint - the isFunctionHint to set
   */
  public void setIsFunctionHint(boolean isFunctionHint)
  {
    this.isFunctionHint = isFunctionHint;
  }

  /**
   * @return the isAssignableHint
   */
  public boolean getIsAssignableHint()
  {
    return isAssignableHint;
  }

  /**
   * @param isAssignableHint - the isAssignableHint to set
   */
  public void setAssignableHint(boolean isAssignableHint)
  {
    this.isAssignableHint = isAssignableHint;
  }

  /**
   * @return the resolved variable
   */
  public Variable getResolvedVariable()
  {
    return resolvedVariable;
  }

  /**
   * @param resolvedVariable - the resolvedVariable to set
   */
  public void setResolvedVariable(Variable resolvedVariable)
  {
    this.resolvedVariable = resolvedVariable;
  }

  /**
   * @return the resolvedMemberReference
   */
  public MemberReference<?> getResolvedMemberReference()
  {
    return resolvedMemberReference;
  }

  /**
   * @param resolvedMemberReference - the resolvedMemberReference to set
   */
  public void setResolvedMemberReference(MemberReference<?> resolvedMemberReference)
  {
    this.resolvedMemberReference = resolvedMemberReference;
  }

  /**
   * @return the resolvedContextImmutability
   */
  public boolean getResolvedContextImmutability()
  {
    return resolvedContextImmutability;
  }

  /**
   * @param resolvedContextImmutability - the resolvedContextImmutability to set
   */
  public void setResolvedContextImmutability(boolean resolvedContextImmutability)
  {
    this.resolvedContextImmutability = resolvedContextImmutability;
  }

  @Override
  public String toString()
  {
    return name;
  }
}
