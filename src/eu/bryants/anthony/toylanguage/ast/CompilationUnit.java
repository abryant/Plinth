package eu.bryants.anthony.toylanguage.ast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class CompilationUnit
{
  private Map<String, Function> functions = new HashMap<String, Function>();

  private LexicalPhrase lexicalPhrase;

  public CompilationUnit(Function[] functions, LexicalPhrase lexicalPhrase)
  {
    for (Function f : functions)
    {
      this.functions.put(f.getName(), f);
    }
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the functions
   */
  public Collection<Function> getFunctions()
  {
    return functions.values();
  }

  /**
   * @param name - the name of the function to get
   * @return the function with the specified name, or null if none exists
   */
  public Function getFunction(String name)
  {
    return functions.get(name);
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
    StringBuffer buffer = new StringBuffer();
    for (Function function : functions.values())
    {
      buffer.append(function);
      buffer.append('\n');
    }
    return buffer.toString();
  }
}
