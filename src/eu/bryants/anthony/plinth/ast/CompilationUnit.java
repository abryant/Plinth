package eu.bryants.anthony.plinth.ast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import eu.bryants.anthony.plinth.ast.metadata.PackageNode;
import eu.bryants.anthony.plinth.ast.misc.Import;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.LanguageParseException;

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

  private Map<String, TypeDefinition> typeDefinitions = new HashMap<String, TypeDefinition>();

  private LexicalPhrase lexicalPhrase;

  private PackageNode resolvedPackage;

  public CompilationUnit(QName declaredPackage, Import[] imports, LexicalPhrase lexicalPhrase)
  {
    this.declaredPackage = declaredPackage;
    this.imports = imports;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * Adds the specified type definition to this compilation unit.
   * @param typeDefinition - the TypeDefinition to add
   * @param newLexicalPhrase - the new LexicalPhrase for this compilation unit
   * @throws LanguageParseException - if a type with the same name already exists in this compilation unit
   */
  public void addType(TypeDefinition typeDefinition, LexicalPhrase newLexicalPhrase) throws LanguageParseException
  {
    if (typeDefinitions.containsKey(typeDefinition.getName()))
    {
      throw new LanguageParseException("Duplicate type name: a type named '" + typeDefinition.getName() + "' already exists in this compilation unit", typeDefinition.getLexicalPhrase());
    }
    typeDefinitions.put(typeDefinition.getName(), typeDefinition);
    if (declaredPackage == null)
    {
      typeDefinition.setQualifiedName(new QName(typeDefinition.getName(), null));
    }
    else
    {
      typeDefinition.setQualifiedName(new QName(declaredPackage, typeDefinition.getName(), null));
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
   * @return the typeDefinitions
   */
  public Collection<TypeDefinition> getTypeDefinitions()
  {
    return typeDefinitions.values();
  }

  /**
   * @param name - the name of the TypeDefinition to get
   * @return the TypeDefinition with the specified name, or null if none exists
   */
  public TypeDefinition getTypeDefinition(String name)
  {
    return typeDefinitions.get(name);
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
    for (TypeDefinition typeDefinition : typeDefinitions.values())
    {
      buffer.append(typeDefinition);
      buffer.append('\n');
    }
    return buffer.toString();
  }
}
