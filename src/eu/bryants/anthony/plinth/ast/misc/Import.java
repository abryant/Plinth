package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.metadata.PackageNode;

/*
 * Created on 13 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class Import
{
  private String name;
  private QName imported;

  private LexicalPhrase lexicalPhrase;

  private PackageNode resolvedPackage;
  private TypeDefinition resolvedTypeDefinition;

  /**
   * Creates a new Import which imports the specified QName as the specified name.
   * @param name - the name to import the QName as, or null if all of the names underneath the QName should be imported
   * @param imported - the QName to import
   * @param lexicalPhrase - the LexicalPhrase of this Import
   */
  public Import(String name, QName imported, LexicalPhrase lexicalPhrase)
  {
    this.name = name;
    this.imported = imported;
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
   * @return true if this import is a wildcard import, false otherwise
   */
  public boolean isWildcard()
  {
    return name == null;
  }

  /**
   * @return the imported QName
   */
  public QName getImported()
  {
    return imported;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
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
   * @return the resolvedTypeDefinition
   */
  public TypeDefinition getResolvedTypeDefinition()
  {
    return resolvedTypeDefinition;
  }

  /**
   * @param resolvedTypeDefinition - the resolvedTypeDefinition to set
   */
  public void setResolvedTypeDefinition(TypeDefinition resolvedTypeDefinition)
  {
    this.resolvedTypeDefinition = resolvedTypeDefinition;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    if (name == null)
    {
      return "import " + imported + ".*;";
    }
    return "import " + imported + ";";
  }
}
