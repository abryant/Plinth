package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 2 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class FieldAccessExpression extends Expression
{
  private Expression baseExpression;
  private boolean nullTraversing;
  private Type baseType;
  private String fieldName;

  private Member resolvedMember;
  // whether or not this expression is in an immutable context
  // this is used by the TypeChecker to make static variables implicitly immutable when in an immutable context
  private boolean resolvedContextImmutability;

  /**
   * Creates a new FieldAccessExpression to access the specified field of the specified base expression.
   * @param baseExpression - the base expression to access the field on
   * @param nullTraversing - true if this should be a null traversing FieldAccessExpression
   * @param fieldName - the name of the field to access
   * @param lexicalPhrase - the LexicalPhrase of this FieldAccessExpression
   */
  public FieldAccessExpression(Expression baseExpression, boolean nullTraversing, String fieldName, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.baseExpression = baseExpression;
    this.nullTraversing = nullTraversing;
    this.fieldName = fieldName;
  }

  /**
   * Creates a new FieldAccessExpression to access the specified field of the specified base type.
   * @param baseType - the base type to access the field on
   * @param fieldName - the name of the field to access
   * @param lexicalPhrase - the LexicalPhrase of this FieldAccessExpression
   */
  public FieldAccessExpression(Type baseType, String fieldName, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.baseType = baseType;
    this.fieldName = fieldName;
  }

  /**
   * @return the baseExpression
   */
  public Expression getBaseExpression()
  {
    return baseExpression;
  }

  /**
   * @return true if this FieldAccessExpression is null-traversing, false otherwise
   */
  public boolean isNullTraversing()
  {
    return nullTraversing;
  }

  /**
   * @return the baseType
   */
  public Type getBaseType()
  {
    return baseType;
  }

  /**
   * @return the fieldName
   */
  public String getFieldName()
  {
    return fieldName;
  }

  /**
   * @return the resolvedMember
   */
  public Member getResolvedMember()
  {
    return resolvedMember;
  }

  /**
   * @param resolvedMember - the resolvedMember to set
   */
  public void setResolvedMember(Member resolvedMember)
  {
    this.resolvedMember = resolvedMember;
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

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    if (baseExpression != null)
    {
      return baseExpression + (nullTraversing ? "?." : ".") + fieldName;
    }
    return baseType + "::" + fieldName;
  }
}
