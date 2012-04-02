package eu.bryants.anthony.toylanguage.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Function
{
  private String name;
  private Map<String, Parameter> parameters = new HashMap<String, Parameter>();
  private Expression expression;

  private LexicalPhrase lexicalPhrase;

  public Function(String name, Parameter[] parameters, Expression expression, LexicalPhrase lexicalPhrase)
  {
    this.name = name;
    int index = 0;
    for (Parameter p : parameters)
    {
      this.parameters.put(p.getName(), p);
      p.setIndex(index);
      index++;
    }
    this.expression = expression;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the parameters
   */
  public Collection<Parameter> getParameters()
  {
    return parameters.values();
  }

  /**
   * @param name - the name of the parameter to get
   * @return the parameter with the specified name, or null if none exists
   */
  public Parameter getParameter(String name)
  {
    return parameters.get(name);
  }

  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer(name);
    buffer.append('(');
    List<Parameter> list = new ArrayList<Parameter>(parameters.values());
    Collections.sort(list, new Comparator<Parameter>()
    {
      @Override
      public int compare(Parameter o1, Parameter o2)
      {
        return o1.getIndex() - o2.getIndex();
      }
    });
    for (int i = 0; i < list.size(); i++)
    {
      buffer.append(list.get(i));
      if (i != parameters.size() - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append("): ");
    buffer.append(expression);
    buffer.append(';');
    return buffer.toString();
  }
}
