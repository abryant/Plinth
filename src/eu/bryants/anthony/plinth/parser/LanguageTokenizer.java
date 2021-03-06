package eu.bryants.anthony.plinth.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import parser.ParseException;
import parser.Token;
import parser.Tokenizer;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.terminal.FloatingLiteral;
import eu.bryants.anthony.plinth.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;
import eu.bryants.anthony.plinth.ast.terminal.StringLiteral;

/*
 * Created on 30 Jun 2010
 */

/**
 * The tokenizer for the language. This contains everything necessary to parse and read tokens in order from a given Reader.
 * @author Anthony Bryant
 */
public class LanguageTokenizer extends Tokenizer<ParseType>
{

  private static final Map<String, ParseType> KEYWORDS = new HashMap<String, ParseType>();
  static
  {
    KEYWORDS.put("abstract",   ParseType.ABSTRACT_KEYWORD);
    KEYWORDS.put("boolean",    ParseType.BOOLEAN_KEYWORD);
    KEYWORDS.put("break",      ParseType.BREAK_KEYWORD);
    KEYWORDS.put("byte",       ParseType.BYTE_KEYWORD);
    KEYWORDS.put("cast",       ParseType.CAST_KEYWORD);
    KEYWORDS.put("catch",      ParseType.CATCH_KEYWORD);
    KEYWORDS.put("class",      ParseType.CLASS_KEYWORD);
    KEYWORDS.put("compound",   ParseType.COMPOUND_KEYWORD);
    KEYWORDS.put("continue",   ParseType.CONTINUE_KEYWORD);
    KEYWORDS.put("create",     ParseType.CREATE_KEYWORD);
    KEYWORDS.put("double",     ParseType.DOUBLE_KEYWORD);
    KEYWORDS.put("else",       ParseType.ELSE_KEYWORD);
    KEYWORDS.put("extends",    ParseType.EXTENDS_KEYWORD);
    KEYWORDS.put("false",      ParseType.FALSE_KEYWORD);
    KEYWORDS.put("final",      ParseType.FINAL_KEYWORD);
    KEYWORDS.put("finally",    ParseType.FINALLY_KEYWORD);
    KEYWORDS.put("float",      ParseType.FLOAT_KEYWORD);
    KEYWORDS.put("for",        ParseType.FOR_KEYWORD);
    KEYWORDS.put("getter",     ParseType.GETTER_KEYWORD);
    KEYWORDS.put("if",         ParseType.IF_KEYWORD);
    KEYWORDS.put("immutable",  ParseType.IMMUTABLE_KEYWORD);
    KEYWORDS.put("implements", ParseType.IMPLEMENTS_KEYWORD);
    KEYWORDS.put("import",     ParseType.IMPORT_KEYWORD);
    KEYWORDS.put("instanceof", ParseType.INSTANCEOF_KEYWORD);
    KEYWORDS.put("in",         ParseType.IN_KEYWORD);
    KEYWORDS.put("int",        ParseType.INT_KEYWORD);
    KEYWORDS.put("interface",  ParseType.INTERFACE_KEYWORD);
    KEYWORDS.put("long",       ParseType.LONG_KEYWORD);
    KEYWORDS.put("mutable",    ParseType.MUTABLE_KEYWORD);
    KEYWORDS.put("native",     ParseType.NATIVE_KEYWORD);
    KEYWORDS.put("new",        ParseType.NEW_KEYWORD);
    KEYWORDS.put("null",       ParseType.NULL_KEYWORD);
    KEYWORDS.put("object",     ParseType.OBJECT_KEYWORD);
    KEYWORDS.put("package",    ParseType.PACKAGE_KEYWORD);
    KEYWORDS.put("property",   ParseType.PROPERTY_KEYWORD);
    KEYWORDS.put("return",     ParseType.RETURN_KEYWORD);
    KEYWORDS.put("selfish",    ParseType.SELFISH_KEYWORD);
    KEYWORDS.put("setter",     ParseType.SETTER_KEYWORD);
    KEYWORDS.put("short",      ParseType.SHORT_KEYWORD);
    // "since" is handled differently (the whole "since(1.2.3)" is parsed by the tokenizer)
    KEYWORDS.put("static",     ParseType.STATIC_KEYWORD);
    KEYWORDS.put("super",      ParseType.SUPER_KEYWORD);
    KEYWORDS.put("this",       ParseType.THIS_KEYWORD);
    KEYWORDS.put("throw",      ParseType.THROW_KEYWORD);
    KEYWORDS.put("throws",     ParseType.THROWS_KEYWORD);
    KEYWORDS.put("true",       ParseType.TRUE_KEYWORD);
    KEYWORDS.put("try",        ParseType.TRY_KEYWORD);
    KEYWORDS.put("ubyte",      ParseType.UBYTE_KEYWORD);
    KEYWORDS.put("uint",       ParseType.UINT_KEYWORD);
    KEYWORDS.put("ulong",      ParseType.ULONG_KEYWORD);
    KEYWORDS.put("unbacked",   ParseType.UNBACKED_KEYWORD);
    KEYWORDS.put("unchecked",  ParseType.UNCHECKED_KEYWORD);
    KEYWORDS.put("ushort",     ParseType.USHORT_KEYWORD);
    KEYWORDS.put("void",       ParseType.VOID_KEYWORD);
    KEYWORDS.put("while",      ParseType.WHILE_KEYWORD);
  }

  private RandomAccessReader reader;

  private String filePath;
  private int currentLine;
  private int currentColumn;

  /**
   * Creates a new LanguageTokenizer with the specified reader.
   * @param reader - the reader to read the input from
   * @param filePath - the file path to store in LexicalPhrase objects created
   */
  public LanguageTokenizer(Reader reader, String filePath)
  {
    this.reader = new RandomAccessReader(reader);
    this.filePath = filePath;
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
        throw new LanguageParseException("Tabs are not permitted in this language.", new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn));
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
              currentColumn++;
              index++;
              int secondCommentChar = reader.read(index);
              if (secondCommentChar == '/')
              {
                currentColumn++;
                index++;
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
              throw new LanguageParseException("Tabs are not permitted in this language.", new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn));
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
              throw new LanguageParseException("Tabs are not permitted in this language.", new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn));
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
  private Token<ParseType> readNameOrKeyword() throws IOException, LanguageParseException
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

    // we have a full name or keyword, so compare it against a list of keywords to find out which
    String name = buffer.toString();
    ParseType keyword = KEYWORDS.get(name);
    if (keyword != null)
    {
      return new Token<ParseType>(keyword, new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn - index, currentColumn));
    }

    // check if the name is the start of a since specifier, and if it is then read the rest of it
    if (name.equals("since"))
    {
      return readSinceSpecifier(new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn - index, currentColumn));
    }

    // check if the name is an underscore, and if it is then return it
    if (name.equals("_"))
    {
      return new Token<ParseType>(ParseType.UNDERSCORE, new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn - index, currentColumn));
    }

    // we have a name, so return it
    return new Token<ParseType>(ParseType.NAME, new Name(name, new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn - index, currentColumn)));
  }

  /**
   * Reads a since specifier from the stream.
   * This method assumes that a "since" keyword has just been parsed, and will throw exceptions if invalid tokens are detected after it.
   * @param sinceKeywordPhrase - the LexicalPhrase of the "since" keyword which has already been parsed
   * @return a since specifier Token
   * @throws IOException - if an error occurs while reading from the stream
   * @throws LanguageParseException - if an invalid token is detected in the since specifier
   */
  private Token<ParseType> readSinceSpecifier(LexicalPhrase sinceKeywordPhrase) throws IOException, LanguageParseException
  {
    // skip whitespace between "since" and "("
    skipWhitespaceAndComments();

    // read the "("
    int nextChar = reader.read(0);
    if (nextChar != '(')
    {
      throw new LanguageParseException("Expected '(' after 'since'.", new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn));
    }
    LexicalPhrase lparenPhrase = new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn, currentColumn + 1);
    currentColumn++;
    reader.discard(1);

    skipWhitespaceAndComments();

    // read the first number
    Token<ParseType> firstLiteralToken = readIntegerLiteral();
    if (firstLiteralToken == null)
    {
      throw new LanguageParseException("Expected integer literal in since specifier.", new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn));
    }
    IntegerLiteral firstLiteral = (IntegerLiteral) firstLiteralToken.getValue();
    List<BigInteger> versionPartList = new LinkedList<BigInteger>();
    versionPartList.add(firstLiteral.getValue());

    skipWhitespaceAndComments();

    LexicalPhrase versionPhrase = firstLiteral.getLexicalPhrase();
    LexicalPhrase rparenPhrase;
    while (true)
    {
      nextChar = reader.read(0);
      if (nextChar == '.')
      {
        // read the dot
        currentColumn++;
        reader.discard(1);
        LexicalPhrase dotPhrase = new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn - 1, currentColumn);

        skipWhitespaceAndComments();

        // read the next version number part
        Token<ParseType> literalToken = readIntegerLiteral();
        if (literalToken == null)
        {
          throw new LanguageParseException("Expected integer literal in since specifier.", new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn));
        }
        IntegerLiteral literal = (IntegerLiteral) literalToken.getValue();
        versionPartList.add(literal.getValue());

        versionPhrase = LexicalPhrase.combine(versionPhrase, dotPhrase, literal.getLexicalPhrase());

        skipWhitespaceAndComments();
      }
      else if (nextChar == ')')
      {
        // read the RParen
        currentColumn++;
        reader.discard(1);
        rparenPhrase = new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn - 1, currentColumn);
        break;
      }
      else
      {
        throw new LanguageParseException("Expected '.' or ')' after integer literal in since specifier.", new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn));
      }
    }

    BigInteger[] versionParts = versionPartList.toArray(new BigInteger[versionPartList.size()]);
    SinceSpecifier sinceSpecifier = new SinceSpecifier(versionParts, LexicalPhrase.combine(sinceKeywordPhrase, lparenPhrase, versionPhrase, rparenPhrase));
    return new Token<ParseType>(ParseType.SINCE_SPECIFIER, sinceSpecifier);
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
          throw new LanguageParseException("Unexpected end of " + baseString + " literal.", new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn));
        }
        IntegerLiteral literal = new IntegerLiteral(value, buffer.toString(), new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn - buffer.length(), currentColumn));
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
      IntegerLiteral literal = new IntegerLiteral(value, buffer.toString(), new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn - buffer.length(), currentColumn));
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
    IntegerLiteral literal = new IntegerLiteral(value, buffer.toString(), new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn - buffer.length(), currentColumn));
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
   * Reads a floating literal from the start of the reader.
   * This method assumes that all whitespace and comments have just been discarded,
   * and the currentLine and currentColumn are up to date.
   * @return a Token read from the input stream, or null if no Token could be read
   * @throws IOException - if an error occurs while reading from the stream
   */
  private Token<ParseType> readFloatingLiteral() throws IOException
  {
    int nextChar;
    int index = 0;
    StringBuffer buffer = new StringBuffer();
    boolean hasInitialNumber = false;
    while (true)
    {
      nextChar = reader.read(index);
      int digitValue = Character.digit(nextChar, 10);
      if (digitValue < 0)
      {
        // we do not have a digit in the range 0-9
        break;
      }
      hasInitialNumber = true;
      buffer.append((char) nextChar);
      index++;
    }
    boolean hasFractionalPart = false;
    if (nextChar == '.')
    {
      buffer.append('.');
      index++;

      while (true)
      {
        nextChar = reader.read(index);
        int digitValue = Character.digit(nextChar, 10);
        if (digitValue < 0)
        {
          // we do not have a digit in the range 0-9
          if (!hasFractionalPart)
          {
            // there is no fractional part to this number, so it is not a valid floating point literal
            return null;
          }
          break;
        }
        hasFractionalPart = true;
        buffer.append((char) nextChar);
        index++;
      }
    }

    boolean hasExponent = false;
    if (nextChar == 'e' || nextChar == 'E')
    {
      int indexBeforeExponent = index;

      StringBuffer exponentialBuffer = new StringBuffer();
      exponentialBuffer.append((char) nextChar);
      index++;

      nextChar = reader.read(index);
      if (nextChar == '+' || nextChar == '-')
      {
        exponentialBuffer.append((char) nextChar);
        index++;
      }

      while (true)
      {
        nextChar = reader.read(index);
        int digitValue = Character.digit(nextChar, 10);
        if (digitValue < 0)
        {
          // we do not have a digit in the range 0-9
          break;
        }
        hasExponent = true;
        exponentialBuffer.append((char) nextChar);
        index++;
      }
      // only add the exponent if it all exists
      if (hasExponent)
      {
        buffer.append(exponentialBuffer);
      }
      else
      {
        index = indexBeforeExponent;
      }
    }

    if (hasFractionalPart || (hasInitialNumber && hasExponent))
    {
      String floatingPointText = buffer.toString();
      reader.discard(index);
      currentColumn += index;
      FloatingLiteral literal = new FloatingLiteral(floatingPointText, new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn - index, currentColumn));
      return new Token<ParseType>(ParseType.FLOATING_LITERAL, literal);
    }

    // there was no valid floating literal, so do not return a Token
    return null;
  }

  /**
   * Reads a string literal from the start of the reader.
   * This method assumes that all whitespace and comments have just been discarded,
   * and the currentLine and currentColumn are up to date.
   * @return a Token read from the input stream, or null if no Token could be read
   * @throws IOException - if an error occurs while reading from the stream
   * @throws LanguageParseException - if an unexpected character is detected inside the string literal
   */
  private Token<ParseType> readStringLiteral() throws IOException, LanguageParseException
  {
    int nextChar = reader.read(0);
    if (nextChar != '"')
    {
      // this is not a string literal, so return null
      return null;
    }
    StringBuffer stringRepresentation = new StringBuffer();
    stringRepresentation.append('"');

    StringBuffer buffer = new StringBuffer();
    int index = 1;
    while (true)
    {
      nextChar = reader.read(index);
      if (nextChar < 0)
      {
        reader.discard(index - 1); // discard so that getting the current line works correctly
        throw new LanguageParseException("Unexpected end of input inside string literal.", new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn + index));
      }
      if (nextChar == '\n')
      {
        reader.discard(index); // discard so that getting the current line works correctly
        throw new LanguageParseException("Unexpected end of line inside string literal.", new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn + index));
      }

      if (nextChar == '"')
      {
        index++;
        stringRepresentation.append((char) nextChar);
        break;
      }

      if (nextChar == '\\')
      {
        stringRepresentation.append((char) nextChar);
        // process the escape sequence, adding the read characters to the stringRepresentation buffer
        // and the escaped character to the literal value buffer
        char escapedChar = processEscapeSequence(index, stringRepresentation);
        index = stringRepresentation.length();
        buffer.append(escapedChar);
        continue;
      } // finished escape sequences

      buffer.append((char) nextChar);
      stringRepresentation.append((char) nextChar);
      index++;
    }

    // a whole string literal has been read, so create a token from it
    // (index is now the length of the entire literal, including quotes)
    reader.discard(index);
    currentColumn += index;
    StringLiteral literal = new StringLiteral(buffer.toString(), stringRepresentation.toString(), new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn - index, currentColumn));
    return new Token<ParseType>(ParseType.STRING_LITERAL, literal);
  }

  /**
   * Processes an escape sequence in a character or string literal.
   * @param startIndex - the index of the start of the escape sequence (i.e. the '\' character)
   * @param buffer - the buffer to append the characters from the stream to
   * @return the escaped character
   * @throws IOException - if there is an error reading from the stream
   * @throws LanguageParseException - if there is an invalid character in the escape sequence
   */
  private char processEscapeSequence(int startIndex, StringBuffer buffer) throws IOException, LanguageParseException
  {
    int index = startIndex;
    int secondChar = reader.read(index + 1);
    if (secondChar < 0)
    {
      reader.discard(index); // discard so that getting the current line works correctly
      throw new LanguageParseException("Unexpected end of input inside escape sequence.", new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn + index + 1));
    }
    Character escaped = null;
    // check all of the single character escape sequences (i.e. the ones that only have one character after the \)
    switch (secondChar)
    {
    case '\\': escaped = '\\'; break;
    case 'b':  escaped = '\b'; break;
    case 't':  escaped = '\t'; break;
    case 'n':  escaped = '\n'; break;
    case 'f':  escaped = '\f'; break;
    case 'r':  escaped = '\r'; break;
    case '"':  escaped = '"';  break;
    case '\'': escaped = '\''; break;
    default:
      break;
    }
    if (escaped != null)
    {
      buffer.append((char) secondChar);
    }

    // check the multi-character escape sequences
    int firstOctalDigit = Character.digit(secondChar, 8);
    if (escaped == null && firstOctalDigit >= 0)
    {
      // the character is a valid digit in base 4, so it begins an octal character escape
      int octal = firstOctalDigit;
      buffer.append((char) secondChar);
      int thirdChar = reader.read(index + 2);
      int secondOctalDigit = Character.digit(thirdChar, 8);
      if (secondOctalDigit >= 0)
      {
        octal = octal * 8 + secondOctalDigit;
        buffer.append((char) thirdChar);
        int fourthChar = reader.read(index + 3);
        int thirdOctalDigit = Character.digit(fourthChar, 8);
        if (thirdOctalDigit >= 0)
        {
          octal = octal * 8 + thirdOctalDigit;
          buffer.append((char) fourthChar);
        }
      }
      escaped = (char) octal;
    }
    if (escaped == null && secondChar == 'u')
    {
      buffer.append((char) secondChar);
      // read the next 4 characters as a hexadecimal unicode constant
      int hex = 0;
      for (int i = 0; i < 4; i++)
      {
        int ithChar = reader.read(index + 2 + i);
        int hexDigit = Character.digit(ithChar, 16);
        if (hexDigit < 0)
        {
          reader.discard(index + 2 + i - 1); // discard so that getting the current line works correctly
          throw new LanguageParseException("Invalid character in unicode escape sequence" + (ithChar >= 0 ? ": " + (char) ithChar : ""),
                                           new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn + index + 2 + i));
        }
        hex = hex * 8 + hexDigit;
        buffer.append((char) ithChar);
      }
      escaped = (char) hex;
    }

    if (escaped == null)
    {
      reader.discard(index + 1); // discard so that getting the current line works correctly
      throw new LanguageParseException("Invalid escape sequence" + (secondChar >= 0 ? ": \\" + (char) secondChar : ""),
                                       new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn + index + 1));
    }
    return escaped.charValue();
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

    if (nextChar == '&')
    {
      int secondChar = reader.read(1);
      if (secondChar == '&')
      {
        return makeSymbolToken(ParseType.DOUBLE_AMPERSAND, 2);
      }
      if (secondChar == '=')
      {
        return makeSymbolToken(ParseType.AMPERSAND_EQUALS, 2);
      }
      return makeSymbolToken(ParseType.AMPERSAND, 1);
    }
    if (nextChar == '@')
    {
      return makeSymbolToken(ParseType.AT, 1);
    }
    if (nextChar == '^')
    {
      int secondChar = reader.read(1);
      if (secondChar == '=')
      {
        return makeSymbolToken(ParseType.CARET_EQUALS, 2);
      }
      return makeSymbolToken(ParseType.CARET, 1);
    }
    if (nextChar == ':')
    {
      int secondChar = reader.read(1);
      if (secondChar == ':')
      {
        return makeSymbolToken(ParseType.DOUBLE_COLON, 2);
      }
      return makeSymbolToken(ParseType.COLON, 1);
    }
    if (nextChar == ',')
    {
      return makeSymbolToken(ParseType.COMMA, 1);
    }
    if (nextChar == '.')
    {
      int secondChar = reader.read(1);
      if (secondChar == '.')
      {
        int thirdChar = reader.read(2);
        if (thirdChar == '.')
        {
          return makeSymbolToken(ParseType.ELLIPSIS, 3);
        }
      }
      return makeSymbolToken(ParseType.DOT, 1);
    }
    if (nextChar == '=')
    {
      int secondChar = reader.read(1);
      if (secondChar == '=')
      {
        int thirdChar = reader.read(2);
        if (thirdChar == '=')
        {
          return makeSymbolToken(ParseType.TRIPLE_EQUALS, 3);
        }
        return makeSymbolToken(ParseType.DOUBLE_EQUALS, 2);
      }
      return makeSymbolToken(ParseType.EQUALS, 1);
    }
    if (nextChar == '!')
    {
      int secondChar = reader.read(1);
      if (secondChar == '=')
      {
        int thirdChar = reader.read(2);
        if (thirdChar == '=')
        {
          return makeSymbolToken(ParseType.EXCLAIMATION_MARK_DOUBLE_EQUALS, 3);
        }
        return makeSymbolToken(ParseType.EXCLAIMATION_MARK_EQUALS, 2);
      }
      return makeSymbolToken(ParseType.EXCLAIMATION_MARK, 1);
    }
    if (nextChar == '/')
    {
      int secondChar = reader.read(1);
      if (secondChar == '=')
      {
        return makeSymbolToken(ParseType.FORWARD_SLASH_EQUALS, 2);
      }
      return makeSymbolToken(ParseType.FORWARD_SLASH, 1);
    }
    if (nextChar == '#')
    {
      return makeSymbolToken(ParseType.HASH, 1);
    }
    if (nextChar == '<')
    {
      int secondChar = reader.read(1);
      if (secondChar == '=')
      {
        return makeSymbolToken(ParseType.LANGLE_EQUALS, 2);
      }
      if (secondChar == '<')
      {
        int thirdChar = reader.read(2);
        if (thirdChar == '=')
        {
          return makeSymbolToken(ParseType.DOUBLE_LANGLE_EQUALS, 3);
        }
        return makeSymbolToken(ParseType.DOUBLE_LANGLE, 2);
      }
      return makeSymbolToken(ParseType.LANGLE, 1);
    }
    if (nextChar == '{')
    {
      return makeSymbolToken(ParseType.LBRACE, 1);
    }
    if (nextChar == '(')
    {
      return makeSymbolToken(ParseType.LPAREN, 1);
    }
    if (nextChar == '[')
    {
      return makeSymbolToken(ParseType.LSQUARE, 1);
    }
    if (nextChar == '-')
    {
      int secondChar = reader.read(1);
      if (secondChar == '>')
      {
        return makeSymbolToken(ParseType.ARROW, 2);
      }
      if (secondChar == '-')
      {
        return makeSymbolToken(ParseType.DOUBLE_MINUS, 2);
      }
      if (secondChar == '=')
      {
        return makeSymbolToken(ParseType.MINUS_EQUALS, 2);
      }
      return makeSymbolToken(ParseType.MINUS, 1);
    }
    if (nextChar == '%')
    {
      int secondChar = reader.read(1);
      if (secondChar == '%')
      {
        int thirdChar = reader.read(2);
        if (thirdChar == '=')
        {
          return makeSymbolToken(ParseType.DOUBLE_PERCENT_EQUALS, 3);
        }
        return makeSymbolToken(ParseType.DOUBLE_PERCENT, 2);
      }
      if (secondChar == '=')
      {
        return makeSymbolToken(ParseType.PERCENT_EQUALS, 2);
      }
      return makeSymbolToken(ParseType.PERCENT, 1);
    }
    if (nextChar == '|')
    {
      int secondChar = reader.read(1);
      if (secondChar == '|')
      {
        return makeSymbolToken(ParseType.DOUBLE_PIPE, 2);
      }
      if (secondChar == '=')
      {
        return makeSymbolToken(ParseType.PIPE_EQUALS, 2);
      }
      return makeSymbolToken(ParseType.PIPE, 1);
    }
    if (nextChar == '+')
    {
      int secondChar = reader.read(1);
      if (secondChar == '+')
      {
        return makeSymbolToken(ParseType.DOUBLE_PLUS, 2);
      }
      if (secondChar == '=')
      {
        return makeSymbolToken(ParseType.PLUS_EQUALS, 2);
      }
      return makeSymbolToken(ParseType.PLUS, 1);
    }
    if (nextChar == '?')
    {
      int secondChar = reader.read(1);
      if (secondChar == ':')
      {
        return makeSymbolToken(ParseType.QUESTION_MARK_COLON, 2);
      }
      if (secondChar == '.')
      {
        return makeSymbolToken(ParseType.QUESTION_MARK_DOT, 2);
      }
      return makeSymbolToken(ParseType.QUESTION_MARK, 1);
    }
    if (nextChar == '>')
    {
      int secondChar = reader.read(1);
      if (secondChar == '=')
      {
        return makeSymbolToken(ParseType.RANGLE_EQUALS, 2);
      }
      if (secondChar == '>')
      {
        int thirdChar = reader.read(2);
        if (thirdChar == '=')
        {
          return makeSymbolToken(ParseType.DOUBLE_RANGLE_EQUALS, 3);
        }
        return makeSymbolToken(ParseType.DOUBLE_RANGLE, 2);
      }
      return makeSymbolToken(ParseType.RANGLE, 1);
    }
    if (nextChar == '}')
    {
      return makeSymbolToken(ParseType.RBRACE, 1);
    }
    if (nextChar == ')')
    {
      return makeSymbolToken(ParseType.RPAREN, 1);
    }
    if (nextChar == ']')
    {
      return makeSymbolToken(ParseType.RSQUARE, 1);
    }
    if (nextChar == ';')
    {
      return makeSymbolToken(ParseType.SEMICOLON, 1);
    }
    if (nextChar == '*')
    {
      int secondChar = reader.read(1);
      if (secondChar == '=')
      {
        return makeSymbolToken(ParseType.STAR_EQUALS, 2);
      }
      return makeSymbolToken(ParseType.STAR, 1);
    }
    if (nextChar == '~')
    {
      return makeSymbolToken(ParseType.TILDE, 1);
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
    return new Token<ParseType>(parseType, new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn - length, currentColumn));
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

      Token<ParseType> token = readNameOrKeyword();
      if (token != null)
      {
        return token;
      }
      token = readFloatingLiteral();
      if (token != null)
      {
        return token;
      }
      token = readIntegerLiteral();
      if (token != null)
      {
        return token;
      }
      token = readStringLiteral();
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
        return new Token<ParseType>(null, new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn));
      }
      throw new LanguageParseException("Unexpected character while parsing: '" + (char) nextChar + "'", new LexicalPhrase(filePath, currentLine, reader.getCurrentLine(), currentColumn));
    }
    catch (IOException e)
    {
      throw new LanguageParseException("An IO Exception occurred while reading the source code.", e, new LexicalPhrase(filePath, currentLine, "", currentColumn));
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
