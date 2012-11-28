package eu.bryants.anthony.plinth.ast.member;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.metadata.GlobalVariable;
import eu.bryants.anthony.plinth.ast.metadata.MemberVariable;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class Field extends Member
{

  private Type type;
  private String name;
  private boolean isStatic;
  private boolean isFinal;
  private boolean isMutable;
  private Expression initialiserExpression;

  private MemberVariable memberVariable;
  private GlobalVariable globalVariable;
  private int memberIndex;

  public Field(Type type, String name, boolean isStatic, boolean isFinal, boolean isMutable, Expression initialiserExpression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.type = type;
    this.name = name;
    this.isStatic = isStatic;
    this.isFinal = isFinal;
    this.isMutable = isMutable;
    this.initialiserExpression = initialiserExpression;
  }

  /**
   * @return the type
   */
  public Type getType()
  {
    return type;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the isFinal
   */
  public boolean isFinal()
  {
    return isFinal;
  }

  /**
   * @return the isStatic
   */
  public boolean isStatic()
  {
    return isStatic;
  }

  /**
   * @return the isMutable
   */
  public boolean isMutable()
  {
    return isMutable;
  }

  /**
   * @return the initialiserExpression
   */
  public Expression getInitialiserExpression()
  {
    return initialiserExpression;
  }

  /**
   * @return the memberIndex
   */
  public int getMemberIndex()
  {
    return memberIndex;
  }

  /**
   * @param memberIndex - the memberIndex to set
   */
  public void setMemberIndex(int memberIndex)
  {
    this.memberIndex = memberIndex;
  }

  /**
   * @return the memberVariable
   */
  public MemberVariable getMemberVariable()
  {
    return memberVariable;
  }

  /**
   * @param memberVariable - the memberVariable to set
   */
  public void setMemberVariable(MemberVariable memberVariable)
  {
    this.memberVariable = memberVariable;
  }

  /**
   * @return the globalVariable
   */
  public GlobalVariable getGlobalVariable()
  {
    return globalVariable;
  }

  /**
   * @param globalVariable - the globalVariable to set
   */
  public void setGlobalVariable(GlobalVariable globalVariable)
  {
    this.globalVariable = globalVariable;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return (isStatic ? "static " : "") + (isFinal ? "final " : "") + (isMutable ? "mutable " : "") + type + " " + name + (initialiserExpression == null ? "" : " = " + initialiserExpression) + ";";
  }
}
