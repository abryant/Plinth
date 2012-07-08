package eu.bryants.anthony.toylanguage.parser.parseAST;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;

/*
 * Created on 8 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class QName
{
  private String[] names;

  private LexicalPhrase lexicalPhrase;

  public QName(String[] names, LexicalPhrase lexicalPhrase)
  {
    this.names = names;
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
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }
}
