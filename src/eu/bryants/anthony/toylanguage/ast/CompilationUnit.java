package eu.bryants.anthony.toylanguage.ast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import eu.bryants.anthony.toylanguage.ast.metadata.PackageNode;
import eu.bryants.anthony.toylanguage.ast.misc.Import;
import eu.bryants.anthony.toylanguage.ast.misc.QName;
import eu.bryants.anthony.toylanguage.parser.LanguageParseException;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class CompilationUnit
{
  private QName declaredPackage;
  private Import[] imports;

  private Map<String, CompoundDefinition> compoundDefinitions = new HashMap<String, CompoundDefinition>();

  private LexicalPhrase lexicalPhrase;

  private PackageNode resolvedPackage;

  public CompilationUnit(QName declaredPackage, Import[] imports, LexicalPhrase lexicalPhrase)
  {
    this.declaredPackage = declaredPackage;
    this.imports = imports;
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
    if (declaredPackage == null)
    {
      compound.setQName(new QName(compound.getName(), null));
    }
    else
    {
      compound.setQName(new QName(declaredPackage, compound.getName(), null));
    }
    lexicalPhrase = newLexicalPhrase;
  }

  /**
   * @return the declaredPackage
   */
  public QName getDeclaredPackage()
  {
    return declaredPackage;
  }

  /**
   * @return the resolvedPackage
   */
  public PackageNode getResolvedPackage()
  {
    return resolvedPackage;
  }

  /**
   * @param resolvedPackage - the resolvedPackage to set
   */
  public void setResolvedPackage(PackageNode resolvedPackage)
  {
    this.resolvedPackage = resolvedPackage;
  }

  /**
   * @return the imports
   */
  public Import[] getImports()
  {
    return imports;
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
    if (declaredPackage != null)
    {
      buffer.append("package ");
      buffer.append(declaredPackage);
      buffer.append(";\n\n");
    }
    if (imports.length > 0)
    {
      for (Import i : imports)
      {
        buffer.append(i);
        buffer.append('\n');
      }
      buffer.append('\n');
    }
    for (CompoundDefinition compoundDefinition : compoundDefinitions.values())
    {
      buffer.append(compoundDefinition);
      buffer.append('\n');
    }
    return buffer.toString();
  }
}
