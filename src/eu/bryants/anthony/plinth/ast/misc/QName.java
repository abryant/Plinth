package eu.bryants.anthony.plinth.ast.misc;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.compiler.ConceptualException;

/*
 * Created on 8 Jul 2012
 */

/**
 * A qualified name, consisting of names which are separated by dots in the source code.
 * @author Anthony Bryant
 */
public class QName
{
  private String[] names;

  private LexicalPhrase[] lexicalPhrases;

  /**
   * Creates a new QName consisting of only a single name.
   * @param name - the single name of this QName
   * @param lexicalPhrase - the LexicalPhrase of this QName
   */
  public QName(String name, LexicalPhrase lexicalPhrase)
  {
    this.names = new String[] {name};
    this.lexicalPhrases = new LexicalPhrase[] {lexicalPhrase};
  }

  /**
   * Creates a new QName consisting of a specified base QName and a new name to append to the end of it.
   * @param baseQName - the base QName
   * @param name - the name to add to the end of the base QName to get this QName
   * @param separatorLexicalPhrase - the LexicalPhrase of the '.' between the old QName and the new name
   * @param nameLexicalPhrase - the LexicalPhrase of the new name
   */
  public QName(QName baseQName, String name, LexicalPhrase nameLexicalPhrase)
  {
    String[] oldNames = baseQName.getNames();
    names = new String[oldNames.length + 1];
    System.arraycopy(oldNames, 0, names, 0, oldNames.length);
    names[oldNames.length] = name;

    LexicalPhrase[] oldLexicalPhrases = baseQName.getLexicalPhrases();
    if (oldLexicalPhrases != null)
    {
      lexicalPhrases = new LexicalPhrase[oldLexicalPhrases.length + 1];
      System.arraycopy(oldLexicalPhrases, 0, lexicalPhrases, 0, oldLexicalPhrases.length);
      lexicalPhrases[oldLexicalPhrases.length] = nameLexicalPhrase;
    }
  }

  /**
   * Creates a new QName with the specified list of names.
   * @param names - the list of names for this new QName
   */
  public QName(String[] names)
  {
    if (names == null || names.length < 1)
    {
      throw new IllegalArgumentException("Cannot create a QName with no sub-names");
    }
    this.names = names;
  }

  /**
   * Creates a new QName from a dot-separated String. The string is split on '.' to create the list of names.
   * @param dotSeparatedNames - the dot separated names
   * @throws ConceptualException - if the list of names is malformed in some way
   */
  public QName(String dotSeparatedNames) throws ConceptualException
  {
    if (dotSeparatedNames == null)
    {
      throw new ConceptualException("A QName must be created with at least one name", null);
    }
    String[] names = dotSeparatedNames.split("\\.");
    if (names.length < 1)
    {
      throw new ConceptualException("A QName must be created with at least one name", null);
    }
    this.names = names;
  }

  /**
   * @return the names
   */
  public String[] getNames()
  {
    return names;
  }

  /**
   * @return an array of all of the names in this QName except the last one.
   *         Note: this array can have a zero length.
   */
  public String[] getAllNamesButLast()
  {
    String[] firstNames = new String[names.length - 1];
    System.arraycopy(names, 0, firstNames, 0, firstNames.length);
    return firstNames;
  }

  /**
   * @return the last name in this QName
   */
  public String getLastName()
  {
    return names[names.length - 1];
  }

  /**
   * @return the lexicalPhrases of the names in this QName
   */
  public LexicalPhrase[] getLexicalPhrases()
  {
    return lexicalPhrases;
  }

  /**
   * @return the LexicalPhrase of this QName (which is simply a combination of all of the LexicalPhrases for the names it contains)
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return LexicalPhrase.combine(lexicalPhrases);
  }

  /**
   * @return a mangled version of this QName
   */
  public String getMangledName()
  {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < names.length; ++i)
    {
      buffer.append(names[i].getBytes().length);
      buffer.append(names[i]);
    }
    return buffer.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < names.length; ++i)
    {
      buffer.append(names[i]);
      if (i != names.length - 1)
      {
        buffer.append('.');
      }
    }
    return buffer.toString();
  }
}
