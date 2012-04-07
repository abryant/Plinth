package eu.bryants.anthony.toylanguage.ast;

import java.util.HashMap;
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
  private Parameter[] parameters;
  private Map<String, Parameter> parametersByName = new HashMap<String, Parameter>();
  private Block block;

  private LexicalPhrase lexicalPhrase;

  public Function(String name, Parameter[] parameters, Block block, LexicalPhrase lexicalPhrase)
  {
    this.name = name;
    this.parameters = parameters;
    for (int i = 0; i < parameters.length; i++)
    {
      parameters[i].setIndex(i);
      parametersByName.put(parameters[i].getName(), parameters[i]);
    }
    this.block = block;
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
  public Parameter[] getParameters()
  {
    return parameters;
  }

  /**
   * @param name - the name of the parameter to get
   * @return the parameter with the specified name, or null if none exists
   */
  public Parameter getParameter(String name)
  {
    return parametersByName.get(name);
  }

  /**
   * @return the block
   */
  public Block getBlock()
  {
    return block;
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
    for (int i = 0; i < parameters.length; i++)
    {
      buffer.append(parameters[i]);
      if (i != parameters.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(")\n");
    buffer.append(block);
    buffer.append('\n');
    return buffer.toString();
  }
}
