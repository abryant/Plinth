package eu.bryants.anthony.toylanguage.ast.misc;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.member.Member;

/*
 * Created on 19 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class FieldAssignee extends Assignee
{

  private Expression expression;
  private String name;

  private Member resolvedMember;

  public FieldAssignee(Expression expression, String name, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.expression = expression;
    this.name = name;
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
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return expression + "." + name;
  }
}
