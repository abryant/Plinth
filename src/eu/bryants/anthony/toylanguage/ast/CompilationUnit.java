package eu.bryants.anthony.toylanguage.ast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import eu.bryants.anthony.toylanguage.parser.LanguageParseException;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class CompilationUnit
{
  private String[] declaredPackage;

  private Map<String, CompoundDefinition> compoundDefinitions = new HashMap<String, CompoundDefinition>();

  private LexicalPhrase lexicalPhrase;

  public CompilationUnit(String[] declaredPackage, LexicalPhrase lexicalPhrase)
  {
    this.declaredPackage = declaredPackage;
    this.lexicalPhrase = lexicalPhrase;
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
   * @return the declaredPackage
   */
  public String[] getDeclaredPackage()
  {
    return declaredPackage;
  }

  /**
   * @return the compoundDefinitions
   */
  public Collection<CompoundDefinition> getCompoundDefinitions()
  {
    return compoundDefinitions.values();
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
    if (declaredPackage.length > 0)
    {
      buffer.append("package ");
      for (int i = 0; i < declaredPackage.length; ++i)
      {
        buffer.append(declaredPackage[i]);
        if (i != declaredPackage.length - 1)
        {
          buffer.append('.');
        }
      }
      buffer.append(";\n\n");
    }
    for (CompoundDefinition compoundDefinition : compoundDefinitions.values())
    {
      buffer.append(compoundDefinition);
      buffer.append('\n');
    }
    return buffer.toString();
  }
}
