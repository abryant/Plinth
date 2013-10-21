package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 15 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class EqualityExpression extends Expression
{

  public enum EqualityOperator
  {
    EQUAL("=="),
    NOT_EQUAL("!="),
    IDENTICALLY_EQUAL("==="),
    NOT_IDENTICALLY_EQUAL("!=="),
    ;

    private String stringRepresentation;
    private EqualityOperator(String stringRepresentation)
    {
      this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String toString()
    {
      return stringRepresentation;
    }
  }

  private Expression leftSubExpression;
  private Expression rightSubExpression;
  private EqualityOperator operator;

  // generated by the type checker so that the code generator knows which type to convert the operands to before comparing them
  // this can be null if both sub-expressions are for integer PrimitiveTypes and neither of them are the common super-type (e.g. ulong and int)
  // in this case, the CodeGenerator is expected to extend both integers into a large-enough signed integer to compare them properly (e.g. 65 bits)
  private Type comparisonType;

  // generated by the type checker if this expression is a null check
  // if this is not null, it will point to either leftSubExpression or rightSubExpression, which will then be compared to null instead of the other sub-expression
  private Expression nullCheckExpression;

  public EqualityExpression(Expression leftSubExpression, Expression rightSubExpression, EqualityOperator operator, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.operator = operator;
    this.leftSubExpression = leftSubExpression;
    this.rightSubExpression = rightSubExpression;
  }

  /**
   * @return the leftSubExpression
   */
  public Expression getLeftSubExpression()
  {
    return leftSubExpression;
  }

  /**
   * @return the rightSubExpression
   */
  public Expression getRightSubExpression()
  {
    return rightSubExpression;
  }

  /**
   * @return the operator
   */
  public EqualityOperator getOperator()
  {
    return operator;
  }

  /**
   * @return the comparisonType
   */
  public Type getComparisonType()
  {
    return comparisonType;
  }

  /**
   * @param comparisonType - the comparisonType to set
   */
  public void setComparisonType(Type comparisonType)
  {
    this.comparisonType = comparisonType;
  }

  /**
   * @return the nullCheckExpression
   */
  public Expression getNullCheckExpression()
  {
    return nullCheckExpression;
  }

  /**
   * @param nullCheckExpression - the nullCheckExpression to set
   */
  public void setNullCheckExpression(Expression nullCheckExpression)
  {
    this.nullCheckExpression = nullCheckExpression;
  }

  @Override
  public String toString()
  {
    return leftSubExpression + " " + operator + " " + rightSubExpression;
  }

}
