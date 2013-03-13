package eu.bryants.anthony.plinth.ast;

/*
 * Created on 5 Aug 2010
 */

/**
 * An immutable class which stores information from the parser, such as the location in a file that something came from.
 * @author Anthony Bryant
 */
public class LexicalPhrase
{
  private String path;
  private int line;
  private String lineText;
  private int startColumn;
  private int endColumn;

  /**
   * Creates a new LexicalPhrase object to represent the specified location.
   * @param path - the path to the file that this LexicalPhrase is inside
   * @param line - the line number
   * @param lineText - the text of the line
   * @param column - the column on the line to represent
   */
  public LexicalPhrase(String path, int line, String lineText, int column)
  {
    this(path, line, lineText, column, column + 1);
  }

  /**
   * Creates a new LexicalPhrase object to represent the specified range of characters on a single line
   * @param path - the path to the file that this LexicalPhrase is inside
   * @param line - the line number
   * @param lineText - the text of the line
   * @param startColumn - the start column on the line
   * @param endColumn - the end column on the line + 1 (so that end - start == length)
   */
  public LexicalPhrase(String path, int line, String lineText, int startColumn, int endColumn)
  {
    this.path = path;
    this.line = line;
    this.lineText = lineText;
    this.startColumn = startColumn;
    this.endColumn = endColumn;
  }

  /**
   * Creates a new LexicalPhrase object containing the line number from the first object, and the start and end column info the range of columns used on the first line
   * @param lexicalPhrases - the LexicalPhrase objects to combine
   * @return a LexicalPhrase object representing all of the specified LexicalPhrase objects, or null if all specified inputs are null
   */
  public static LexicalPhrase combine(LexicalPhrase... lexicalPhrases)
  {
    LexicalPhrase combined = null;
    for (LexicalPhrase phrase : lexicalPhrases)
    {
      if (phrase == null)
      {
        continue;
      }
      if (combined == null)
      {
        combined = new LexicalPhrase(phrase.getPath(), phrase.getLine(), phrase.getLineText(),
                                     phrase.getStartColumn(), phrase.getEndColumn());
      }
      else if (combined.getLine() == phrase.getLine())
      {
        combined.startColumn = Math.min(combined.getStartColumn(), phrase.getStartColumn());
        combined.endColumn = Math.max(combined.getEndColumn(), phrase.getEndColumn());
      }
    }
    return combined;
  }

  /**
   * @return the path
   */
  public String getPath()
  {
    return path;
  }

  /**
   * @return the line
   */
  public int getLine()
  {
    return line;
  }

  /**
   * @return the lineText
   */
  public String getLineText()
  {
    return lineText;
  }

  /**
   * @return the startColumn
   */
  public int getStartColumn()
  {
    return startColumn;
  }

  /**
   * @return the endColumn
   */
  public int getEndColumn()
  {
    return endColumn;
  }

  /**
   * @return the location information that this object represents, as a human readable string
   */
  public String getLocationText()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append(path);
    buffer.append(": ");
    buffer.append(line);
    buffer.append(':');
    buffer.append(startColumn);
    if (startColumn < endColumn - 1)
    {
      buffer.append('-');
      buffer.append(endColumn);
    }
    return buffer.toString();
  }

  /**
   * @return the line text of this object, concatenated with a newline and some space and caret ('^') characters pointing to the part of the line that this object represents
   */
  public String getHighlightedLine()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append(lineText);
    buffer.append('\n');
    for (int i = 1; i < startColumn && i <= lineText.length(); i++)
    {
      buffer.append(' ');
    }
    for (int i = startColumn; i < endColumn && i <= lineText.length() + 1; i++)
    {
      buffer.append('^');
    }
    return buffer.toString();
  }

}
