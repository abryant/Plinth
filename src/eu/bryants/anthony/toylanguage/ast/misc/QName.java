package eu.bryants.anthony.toylanguage.ast.misc;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;

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

  private LexicalPhrase lexicalPhrase;

  /**
   * Creates a new QName consisting of only a single name.
   * @param name - the single name of this QName
   * @param lexicalPhrase - the LexicalPhrase of this QName
   */
  public QName(String name, LexicalPhrase lexicalPhrase)
  {
    this.names = new String[] {name};
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * Creates a new QName consisting of a specified base QName and a new name to append to the end of it.
   * @param baseQName - the base QName
   * @param name - the name to add to the end of the base QName to get this QName
   * @param lexicalPhrase - the LexicalPhrase of this QName
   */
  public QName(QName baseQName, String name, LexicalPhrase lexicalPhrase)
  {
    String[] oldNames = baseQName.getNames();
    names = new String[oldNames.length + 1];
    System.arraycopy(oldNames, 0, names, 0, oldNames.length);
    names[oldNames.length] = name;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the names
   */
  public String[] getNames()
  {
    return names;
  }

  /**
   * @return the last name in this QName
   */
  public String getLastName()
  {
    return names[names.length - 1];
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
