package eu.bryants.anthony.plinth.parser;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;

/*
 * Created on 20 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class ParseUtil
{
  public ParseUtil()
  {
    throw new UnsupportedOperationException("Cannot instantiate ParseUtil");
  }

  /**
   * Splits the specified LexicalPhrase for a DOUBLE_RANGLE token in half and returns the LexicalPhrase for the first half of it.
   * @param doubleRAnglePhrase - the LexicalPhrase of the DOUBLE_RANGLE token
   * @return the LexicalPhrase of the first angle bracket in the DOUBLE_RANGLE
   * @throws LanguageParseException - if the DOUBLE_RANGLE token takes up multiple lines or is not 2 characters long
   */
  public static LexicalPhrase splitDoubleRAngle(LexicalPhrase doubleRAnglePhrase) throws LanguageParseException
  {
    int line = doubleRAnglePhrase.getLine();
    int startColumn = doubleRAnglePhrase.getStartColumn();
    if (doubleRAnglePhrase.getEndColumn() - startColumn != 2)
    {
      throw new LanguageParseException("Found a DOUBLE_RANGLE \">>\" token which is not 2 characters long", doubleRAnglePhrase);
    }
    return new LexicalPhrase(doubleRAnglePhrase.getPath(), line, doubleRAnglePhrase.getLineText(), startColumn, startColumn + 1);
  }

}
