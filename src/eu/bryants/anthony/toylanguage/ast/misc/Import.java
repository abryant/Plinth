package eu.bryants.anthony.toylanguage.ast.misc;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;

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
