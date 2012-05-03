package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 2 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class FieldAccessExpression extends Expression
{
  private Expression expression;
  private String fieldName;

  private Member resolvedMember;

  public FieldAccessExpression(Expression expression, String fieldName, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.expression = expression;
    this.fieldName = fieldName;
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
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
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
    return expression + "." + fieldName;
  }
}
