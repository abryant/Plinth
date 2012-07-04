package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.ast.type.Type;

/*
 * Created on 2 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class FieldAccessExpression extends Expression
{
  private Expression baseExpression;
  private Type baseType;
  private String fieldName;

  private Member resolvedMember;

  public FieldAccessExpression(Expression expression, String fieldName, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.baseExpression = expression;
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
   * @param baseExpression - the baseExpression to set
   */
  public void setBaseExpression(Expression baseExpression)
  {
    this.baseExpression = baseExpression;
  }

  /**
   * @return the baseType
   */
  public Type getBaseType()
  {
    return baseType;
  }

  /**
   * @param baseType - the baseType to set
   */
  public void setBaseType(Type baseType)
  {
    this.baseType = baseType;
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
   * @return the fieldName
   */
  public String getFieldName()
  {
    return fieldName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    if (baseExpression != null)
    {
      return baseExpression + "." + fieldName;
    }
    return baseType + "." + fieldName;
  }
}
