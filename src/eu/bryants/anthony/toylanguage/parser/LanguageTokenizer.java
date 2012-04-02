package eu.bryants.anthony.toylanguage.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;

import parser.ParseException;
import parser.Token;
import parser.Tokenizer;
import eu.bryants.anthony.toylanguage.ast.IntegerLiteral;
import eu.bryants.anthony.toylanguage.ast.Name;

/*
 * Created on 30 Jun 2010
 */

/**
 * The tokenizer for the language. This contains everything necessary to parse and read tokens in order from a given Reader.
 * @author Anthony Bryant
 */
public class LanguageTokenizer extends Tokenizer<ParseType>
{

  private RandomAccessReader reader;
  private int currentLine;
  private int currentColumn;

  /**
   * Creates a new LanguageTokenizer with the specified reader.
   * @param reader - the reader to read the input from
   */
  public LanguageTokenizer(Reader reader)
  {
    this.reader = new RandomAccessReader(reader);
    currentLine = 1;
    currentColumn = 1;
  }

  /**
   * Skips all whitespace and comment characters at the start of the stream, while updating the current position in the file.
   * @throws IOException - if an error occurs while reading
   */
  private void skipWhitespaceAndComments() throws IOException, LanguageParseException
  {
    int index = 0;
    while (true)
    {
      int nextChar = reader.read(index);
      if (nextChar < 0)
      {
        reader.discard(index);
        return;
      }
      else if (nextChar == '\r')
      {
        currentLine++;
        currentColumn = 1;
        // skip the line feed, since it is immediately following a carriage return
        int secondChar = reader.read(index + 1);
        if (secondChar == '\n')
        {
          index++;
        }
        index++;
        continue;
      }
      else if (nextChar == '\n')
      {
        currentLine++;
        currentColumn = 1;
        index++;
        continue;
      }
      else if (nextChar == '\t')
      {
        reader.discard(index); // discard so that getting the current line works
        throw new LanguageParseException("Tabs are not permitted in this language.", new LexicalPhrase(currentLine, reader.getCurrentLine(), currentColumn));
      }
      else if (Character.isWhitespace(nextChar))
      {
        currentColumn++;
        index++;
        continue;
      }
      else if (nextChar == '/')
      {
        int secondChar = reader.read(index + 1);
        if (secondChar == '*')
        {
          currentColumn += 2;
          index += 2;
          // skip to the end of the comment: "*/"
          int commentChar = reader.read(index);
          while (commentChar >= 0)
          {
            if (commentChar == '*')
            {
              int secondCommentChar = reader.read(index + 1);
              if (secondCommentChar == '/')
              {
                currentColumn += 2;
                index += 2;
                break;
              }
            }
            else if (commentChar == '\r')
            {
              currentLine++;
              currentColumn = 1;
              // skip the line feed, since it is immediately following a carriage return
              int secondCommentChar = reader.read(index + 1);
              if (secondCommentChar == '\n')
              {
                index++;
              }
              index++;
            }
            else if (commentChar == '\n')
            {
              currentLine++;
              currentColumn = 1;
              index++;
            }
            else if (commentChar == '\t')
            {
              reader.discard(index); // discard so that getting the current line works correctly
              throw new LanguageParseException("Tabs are not permitted in this language.", new LexicalPhrase(currentLine, reader.getCurrentLine(), currentColumn));
            }
            else
            {
              currentColumn++;
              index++;
            }
            commentChar = reader.read(index);
          }
          continue;
        }
        else if (secondChar == '/')
        {
          index += 2;
          // skip to the end of the comment: "\n" or "\r"
          int commentChar = reader.read(index);
          while (commentChar >= 0)
          {
            if (commentChar == '\r')
            {
              currentLine++;
              currentColumn = 1;
              // skip the line feed, since it is immediately following a carriage return
              int secondCommentChar = reader.read(index + 1);
              if (secondCommentChar == '\n')
              {
                index++;
              }
              index++;
              break;
            }
            else if (commentChar == '\n')
            {
              currentLine++;
              currentColumn = 1;
              index++;
              break;
            }
            else if (commentChar == '\t')
            {
              reader.discard(index); // discard so that getting the current line works correctly
              throw new LanguageParseException("Tabs are not permitted in this language.", new LexicalPhrase(currentLine, reader.getCurrentLine(), currentColumn));
            }
            else
            {
              currentColumn++;
              index++;
            }
            commentChar = reader.read(index);
          }
          continue;
        }
        else
        {
          reader.discard(index);
          return;
        }
      } // finished parsing comments
      else
      {
        reader.discard(index);
        return;
      }
    }
  }

  /**
   * Reads a name token from the start of the reader.
   * This method assumes that all whitespace and comments have just been discarded,
   * and the currentLine and currentColumn are up to date.
   * @return a Token read from the input stream, or null if no Token could be read
   * @throws IOException - if an error occurs while reading from the stream
   * @throws LanguageParseException - if an invalid character sequence is detected
   */
  private Token<ParseType> readName() throws IOException, LanguageParseException
  {
    int nextChar = reader.read(0);
    if (nextChar < 0 || (!Character.isLetter(nextChar) && nextChar != '_'))
    {
      // there is no name here, so return null
      return null;
    }

    // we have the start of a name, so allocate a buffer for it
    StringBuffer buffer = new StringBuffer();
    buffer.append((char) nextChar);

    int index = 1;
    nextChar = reader.read(index);
    while (Character.isLetterOrDigit(nextChar) || nextChar == '_')
    {
      buffer.append((char) nextChar);
      index++;
      nextChar = reader.read(index);
    }
    // we will not be reading any more as part of the name, so discard the used characters
    reader.discard(index);

    // update the tokenizer's current location in the file
    currentColumn += index;

    // we have a name, so return it
    String name = buffer.toString();
    return new Token<ParseType>(ParseType.NAME, new Name(name, new LexicalPhrase(currentLine, reader.getCurrentLine(), currentColumn - index, currentColumn)));
  }

  /**
   * Reads an integer literal from the start of the reader.
   * This method assumes that all whitespace and comments have just been discarded,
   * and the currentLine and currentColumn are up to date.
   * @return a Token read from the input stream, or null if no Token could be read
   * @throws IOException - if an error occurs while reading from the stream
   * @throws LanguageParseException - if an unexpected character sequence is detected inside the integer literal
   */
  private Token<ParseType> readIntegerLiteral() throws IOException, LanguageParseException
  {
    int nextChar = reader.read(0);
    int index = 1;
    if (nextChar == '0')
    {
      StringBuffer buffer = new StringBuffer();
      buffer.append((char) nextChar);
      int secondChar = reader.read(index);
      index++;
      int base;
      switch (secondChar)
      {
      case 'b':
      case 'B':
        base = 2; break;
      case 'o':
      case 'O':
        base = 8; break;
      case 'x':
      case 'X':
        base = 16; break;
      default:
        base = 10; break;
      }
      if (base != 10)
      {
        buffer.append((char) secondChar);
        BigInteger value = readInteger(buffer, index, base);
        reader.discard(buffer.length());
        currentColumn += buffer.length();
        if (value == null)
        {
          // there was no value after the 0b, 0o, or 0x, so we have a parse error
          String baseString = "integer";
          if      (base == 2)  { baseString = "binary"; }
          else if (base == 8)  { baseString = "octal"; }
          else if (base == 16) { baseString = "hex"; }
          throw new LanguageParseException("Unexpected end of " + baseString + " literal.", new LexicalPhrase(currentLine, reader.getCurrentLine(), currentColumn));
        }
        IntegerLiteral literal = new IntegerLiteral(value, buffer.toString(), new LexicalPhrase(currentLine, reader.getCurrentLine(), currentColumn - buffer.length(), currentColumn));
        return new Token<ParseType>(ParseType.INTEGER_LITERAL, literal);
      }
      // backtrack an index, as we do not have b, o, or x as the second character in the literal
      // this makes it easier to parse the decimal literal without ignoring the character after the 0
      index--;
      BigInteger value = readInteger(buffer, index, 10);
      if (value == null)
      {
        // there was no value after the initial 0, so set the value to 0
        value = BigInteger.valueOf(0);
      }
      reader.discard(buffer.length());
      currentColumn += buffer.length();
      IntegerLiteral literal = new IntegerLiteral(value, buffer.toString(), new LexicalPhrase(currentLine, reader.getCurrentLine(), currentColumn - buffer.length(), currentColumn));
      return new Token<ParseType>(ParseType.INTEGER_LITERAL, literal);
    }

    // backtrack an index, as we do not have 0 as the first character in the literal
    // this makes it easier to parse the decimal literal without ignoring the first character
    index--;
    StringBuffer buffer = new StringBuffer();
    BigInteger value = readInteger(buffer, index, 10);
    if (value == null)
    {
      // this is not an integer literal
      return null;
    }
    reader.discard(buffer.length());
    currentColumn += buffer.length();
    IntegerLiteral literal = new IntegerLiteral(value, buffer.toString(), new LexicalPhrase(currentLine, reader.getCurrentLine(), currentColumn - buffer.length(), currentColumn));
    return new Token<ParseType>(ParseType.INTEGER_LITERAL, literal);
  }

  /**
   * Reads an integer in the specified radix from the stream, and calculates its value as well as appending it to the buffer.
   * @param buffer - the buffer to append the raw string to
   * @param startIndex - the index in the reader to start at
   * @param radix - the radix to read the number in
   * @return the value of the integer, or null if there was no numeric value
   * @throws IOException - if there is an error reading from the stream
   */
  private BigInteger readInteger(StringBuffer buffer, int startIndex, int radix) throws IOException
  {
    int index = startIndex;
    BigInteger value = BigInteger.valueOf(0);

    boolean hasNumeral = false;
    while (true)
    {
      int nextChar = reader.read(index);
      int digit = Character.digit(nextChar, radix);
      if (digit < 0)
      {
        break;
      }
      hasNumeral = true;
      buffer.append((char) nextChar);
      value = value.multiply(BigInteger.valueOf(radix)).add(BigInteger.valueOf(digit));
      index++;
    }
    if (hasNumeral)
    {
      return value;
    }
    return null;
  }

  /**
   * Reads a symbol token from the start of the reader.
   * This method assumes that all whitespace and comments have just been discarded,
   * and the currentLine and currentColumn are up to date.
   * @return a Token read from the input stream, or null if no Token could be read
   * @throws IOException - if an error occurs while reading from the stream
   */
  private Token<ParseType> readSymbol() throws IOException
  {
    int nextChar = reader.read(0);
    if (nextChar < 0)
    {
      // there is no symbol here, just the end of the file
      return null;
    }

    if (nextChar == '+')
    {
      return makeSymbolToken(ParseType.PLUS, 1);
    }
    if (nextChar == ':')
    {
      return makeSymbolToken(ParseType.COLON, 1);
    }
    if (nextChar == ',')
    {
      return makeSymbolToken(ParseType.COMMA, 1);
    }
    if (nextChar == '(')
    {
      return makeSymbolToken(ParseType.LPAREN, 1);
    }
    if (nextChar == ')')
    {
      return makeSymbolToken(ParseType.RPAREN, 1);
    }
    if (nextChar == ';')
    {
      return makeSymbolToken(ParseType.SEMICOLON, 1);
    }
    // none of the symbols matched, so return null
    return null;
  }

  /**
   * Convenience method which readSymbol() uses to create its Tokens.
   * This assumes that the symbol does not span multiple lines, and is exactly <code>length</code> columns long.
   * @param parseType - the ParseType of the token to create.
   * @param length - the length of the symbol
   * @return the Token created
   * @throws IOException - if there is an error discarding the characters that were read in
   */
  private Token<ParseType> makeSymbolToken(ParseType parseType, int length) throws IOException
  {
    reader.discard(length);
    currentColumn += length;
    return new Token<ParseType>(parseType, new LexicalPhrase(currentLine, reader.getCurrentLine(), currentColumn - length, currentColumn));
  }

  /**
   * @see parser.Tokenizer#generateToken()
   */
  @Override
  protected Token<ParseType> generateToken() throws ParseException
  {
    try
    {
      skipWhitespaceAndComments();

      Token<ParseType> token = readName();
      if (token != null)
      {
        return token;
      }
      token = readIntegerLiteral();
      if (token != null)
      {
        return token;
      }
      token = readSymbol();
      if (token != null)
      {
        return token;
      }

      int nextChar = reader.read(0);
      if (nextChar < 0)
      {
        // a value of less than 0 means the end of input, so return a token with type null
        return new Token<ParseType>(null, new LexicalPhrase(currentLine, reader.getCurrentLine(), currentColumn));
      }
      throw new LanguageParseException("Unexpected character while parsing: '" + (char) nextChar + "'", new LexicalPhrase(currentLine, reader.getCurrentLine(), currentColumn));
    }
    catch (IOException e)
    {
      throw new LanguageParseException("An IO Exception occured while reading the source code.", e, new LexicalPhrase(currentLine, "", currentColumn));
    }
  }

  /**
   * Closes this LanguageTokenizer.
   * @throws IOException - if there is an error closing the underlying stream
   */
  public void close() throws IOException
  {
    reader.close();
  }

  /**
   * A class that provides a sort of random-access interface to a stream.
   * It keeps a buffer of characters, and lazily reads the stream into it.
   * It allows single character reads from anywhere in the stream, and also supports discarding from the start of the buffer.
   * @author Anthony Bryant
   */
  private static final class RandomAccessReader
  {

    private Reader reader;
    private StringBuffer lookahead;
    private String currentLine;

    /**
     * Creates a new RandomAccessReader to read from the specified Reader
     * @param reader - the Reader to read from
     */
    public RandomAccessReader(Reader reader)
    {
      this.reader = new BufferedReader(reader);
      lookahead = new StringBuffer();
    }

    /**
     * Reads the character at the specified offset.
     * @param offset - the offset to read the character at
     * @return the character read from the stream, or -1 if the end of the stream was reached
     * @throws IOException - if an error occurs while reading from the underlying reader
     */
    public int read(int offset) throws IOException
    {
      int result = ensureContains(offset + 1);
      if (result < 0)
      {
        return result;
      }
      return lookahead.charAt(offset);
    }

    /**
     * Discards all of the characters in this reader before the specified offset.
     * After calling this, the character previously returned from read(offset) will be returned from read(0).
     * @param offset - the offset to delete all of the characters before, must be >= 0
     * @throws IOException - if an error occurs while reading from the underlying reader
     */
    public void discard(int offset) throws IOException
    {
      int result = ensureContains(offset);
      if (result < offset)
      {
        throw new IndexOutOfBoundsException("Tried to discard past the end of a RandomAccessReader");
      }
      updateCurrentLine(offset);
      lookahead.delete(0, offset);
    }

    /**
     * @return the current line that is at offset 0 in this reader
     * @throws IOException - if an error occurs while reading from the underlying reader
     */
    public String getCurrentLine() throws IOException
    {
      if (currentLine == null)
      {
        updateCurrentLine(0);
      }
      return currentLine;
    }

    /**
     * Ensures that the lookahead contains at least the specified number of characters.
     * @param length - the number of characters to ensure are in the lookahead
     * @return the number of characters now in the lookahead buffer, or -1 if the end of the stream has been reached
     * @throws IOException - if an error occurs while reading from the underlying reader
     */
    private int ensureContains(int length) throws IOException
    {
      if (length <= 0 || length < lookahead.length())
      {
        return lookahead.length();
      }

      char[] buffer = new char[length - lookahead.length()];
      int readChars = reader.read(buffer);
      if (readChars < 0)
      {
        return readChars;
      }
      lookahead.append(buffer, 0, readChars);
      return lookahead.length();
    }

    /**
     * Updates the current line to be the line at the specified offset.
     * @param offset - the offset to update currentLine from
     * @throws IOException - if an error occurs while reading from the underlying reader
     */
    private void updateCurrentLine(int offset) throws IOException
    {
      ensureContains(offset);
      ensureNewLineAfter(offset);
      int start = -1;
      // start at offset - 1 so that if we start on a \n we count backwards from there
      for (int i = offset - 1; i >= 0; i--)
      {
        if (lookahead.charAt(i) == '\n')
        {
          start = i + 1;
          break;
        }
      }
      if (start < 0)
      {
        if (currentLine != null)
        {
          return;
        }
        start = 0;
      }
      int end = offset;
      while (end < lookahead.length() && lookahead.charAt(end) != '\n')
      {
        end++;
      }
      currentLine = lookahead.substring(start, end);
    }

    /**
     * Ensures that the lookahead buffer contains a newline after (or at) the specified index.
     * @param offset - the offset to ensure there is a newline after.
     * @throws IOException - if an error occurs while reading from the underlying reader
     */
    private void ensureNewLineAfter(int offset) throws IOException
    {
      // after this many characters in lookahead, we will stop trying to read another newline
      final int MAX_LOOKAHEAD_LENGTH = 10000;

      for (int i = offset; i < lookahead.length(); i++)
      {
        if (lookahead.charAt(i) == '\n')
        {
          return;
        }
      }
      while (lookahead.length() < MAX_LOOKAHEAD_LENGTH)
      {
        int nextChar = reader.read();
        if (nextChar < 0)
        {
          // end of stream, just leave lookahead as it was
          return;
        }
        lookahead.append((char) nextChar);
        if (nextChar == '\n' && lookahead.length() >= offset)
        {
          return;
        }
      }
    }

    /**
     * Closes the underlying stream of this RandomAccessReader
     * @throws IOException - if an error occurs while reading from the underlying reader
     */
    public void close() throws IOException
    {
      reader.close();
    }
  }

}
