package eu.bryants.anthony.plinth.ast.terminal;

import java.math.BigInteger;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;

/*
 * Created on 2 Jan 2013
 */

/**
 * @author Anthony Bryant
 */
public class SinceSpecifier implements Comparable<SinceSpecifier>
{

  private BigInteger[] versionParts;

  private LexicalPhrase lexicalPhrase;

  /**
   * Creates a new SinceSpecifier with the specified parts of the version number and LexicalPhrase
   * @param versionParts - the parts of the version number, most significant first
   * @param lexicalPhrase - the LexicalPhrase to denote where this since specifier was located in the source code
   */
  public SinceSpecifier(BigInteger[] versionParts, LexicalPhrase lexicalPhrase)
  {
    this.versionParts = versionParts;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the versionParts
   */
  public BigInteger[] getVersionParts()
  {
    return versionParts;
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
  public int compareTo(SinceSpecifier other)
  {
    if (other == null)
    {
      // a non-null since specifier is always greater
      return 1;
    }
    int commonLength = Math.min(versionParts.length, other.versionParts.length);
    for (int i = 0; i < commonLength; ++i)
    {
      int comparison = versionParts[i].compareTo(other.versionParts[i]);
      if (comparison != 0)
      {
        return comparison;
      }
    }
    // if one is a prefix of the other, then the longer one is greater
    return versionParts.length - other.versionParts.length;
  }

  /**
   * @return the mangled name of this since specifier (just the decimal version number parts separated by underscores)
   */
  public String getMangledName()
  {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < versionParts.length; ++i)
    {
      buffer.append(versionParts[i].toString());
      if (i != versionParts.length - 1)
      {
        buffer.append('_');
      }
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
    buffer.append("since(");
    for (int i = 0; i < versionParts.length; ++i)
    {
      buffer.append(versionParts[i].toString());
      if (i != versionParts.length - 1)
      {
        buffer.append('.');
      }
    }
    buffer.append(')');
    return buffer.toString();
  }
}
