package eu.bryants.anthony.toylanguage.ast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import eu.bryants.anthony.toylanguage.parser.LanguageParseException;
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
  private Map<String, CompoundDefinition> compoundDefinitions = new HashMap<String, CompoundDefinition>();

  private LexicalPhrase lexicalPhrase;

  public CompilationUnit(LexicalPhrase lexicalPhrase)
  {
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * Adds the specified function to this compilation unit.
   * @param function - the function to add
   * @param newLexicalPhrase - the new LexicalPhrase for this compilation unit
   * @throws LanguageParseException - if a function with the same name already exists in this compilation unit
   */
  public void addFunction(Function function, LexicalPhrase newLexicalPhrase) throws LanguageParseException
  {
    Function oldValue = functions.put(function.getName(), function);
    if (oldValue != null)
    {
      throw new LanguageParseException("Duplicated function: " + function.getName(), function.getLexicalPhrase());
    }
    lexicalPhrase = newLexicalPhrase;
  }

  /**
   * Adds the specified compound type definition to this compilation unit.
   * @param compound - the compound to add
   * @param newLexicalPhrase - the new LexicalPhrase for this compilation unit
   * @throws LanguageParseException - if a compound with the same name already exists in this compilation unit
   */
  public void addCompound(CompoundDefinition compound, LexicalPhrase newLexicalPhrase) throws LanguageParseException
  {
    CompoundDefinition oldValue = compoundDefinitions.put(compound.getName(), compound);
    if (oldValue != null)
    {
      throw new LanguageParseException("Duplicated compound type: " + compound.getName(), compound.getLexicalPhrase());
    }
    lexicalPhrase = newLexicalPhrase;
  }

  /**
   * @return the functions
   */
  public Collection<Function> getFunctions()
  {
    return functions.values();
  }

  /**
   * @return the compoundDefinitions
   */
  public Collection<CompoundDefinition> getCompoundDefinitions()
  {
    return compoundDefinitions.values();
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
   * @param name - the name of the compound definition to get
   * @return the compound definition with the specified name, or null if none exists
   */
  public CompoundDefinition getCompoundDefinition(String name)
  {
    return compoundDefinitions.get(name);
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
    for (CompoundDefinition compoundDefinition : compoundDefinitions.values())
    {
      buffer.append(compoundDefinition);
      buffer.append('\n');
    }
    for (Function function : functions.values())
    {
      buffer.append(function);
      buffer.append('\n');
    }
    return buffer.toString();
  }
}
