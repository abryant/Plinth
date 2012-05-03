package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.type.ArrayType;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 3 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ArrayCreationExpression extends Expression
{
  private ArrayType type;
  private Expression[] dimensionExpressions;
  private Expression[] valueExpressions;

  public ArrayCreationExpression(ArrayType type, Expression[] dimensionExpressions, Expression[] valueExpressions, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.type = type;
    this.dimensionExpressions = dimensionExpressions;
    this.valueExpressions = valueExpressions;
  }

  /**
   * @return the type
   */
  @Override
  public ArrayType getType()
  {
    return type;
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
    Type baseType = type;
    if (dimensionExpressions == null)
    {
      buffer.append("[]");
      baseType = type.getBaseType();
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
