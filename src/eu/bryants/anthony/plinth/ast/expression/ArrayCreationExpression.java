package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 3 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ArrayCreationExpression extends Expression
{
  private ArrayType declaredType;
  private Expression[] dimensionExpressions;
  private Expression[] valueExpressions;

  public ArrayCreationExpression(ArrayType type, Expression[] dimensionExpressions, Expression[] valueExpressions, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.declaredType = type;
    this.dimensionExpressions = dimensionExpressions;
    this.valueExpressions = valueExpressions;
  }

  /**
   * @return the declaredType
   */
  public ArrayType getDeclaredType()
  {
    return declaredType;
  }

  /**
   * @return the dimensionExpressions
   */
  public Expression[] getDimensionExpressions()
  {
    return dimensionExpressions;
  }

  /**
   * @return the valueExpressions
   */
  public Expression[] getValueExpressions()
  {
    return valueExpressions;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer("new ");
    Type baseType = declaredType;
    if (dimensionExpressions == null)
    {
      buffer.append("[]");
      baseType = declaredType.getBaseType();
    }
    else
    {
      for (int i = 0; i < dimensionExpressions.length; i++)
      {
        buffer.append('[');
        buffer.append(dimensionExpressions[i]);
        buffer.append(']');
        baseType = ((ArrayType) baseType).getBaseType();
      }
    }
    buffer.append(baseType);
    if (valueExpressions != null)
    {
      buffer.append(" {");
      for (int i = 0; i < valueExpressions.length; i++)
      {
        buffer.append(valueExpressions[i]);
        if (i != valueExpressions.length - 1)
        {
          buffer.append(", ");
        }
      }
      buffer.append('}');
    }
    return buffer.toString();
  }
}
