package eu.bryants.anthony.toylanguage.compiler;

import java.io.File;
import java.io.FileNotFoundException;

import parser.BadTokenException;
import parser.ParseException;
import parser.Token;
import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.compiler.passes.CodeGenerator;
import eu.bryants.anthony.toylanguage.compiler.passes.ControlFlowChecker;
import eu.bryants.anthony.toylanguage.compiler.passes.CycleChecker;
import eu.bryants.anthony.toylanguage.compiler.passes.Resolver;
import eu.bryants.anthony.toylanguage.compiler.passes.TypeChecker;
import eu.bryants.anthony.toylanguage.parser.LanguageParseException;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.ToyLanguageParser;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Compiler
{
  private static final String USAGE = "Usage: java eu.bryants.anthony.toylanguage.compiler.Compiler <input-file> <output-file>";

  public static void main(String... args) throws FileNotFoundException
  {
    if (args.length != 2)
    {
      System.err.println(USAGE);
      System.exit(1);
    }
    File input = new File(args[0]);
    File output = new File(args[1]);
    if (!input.isFile())
    {
      System.err.println(USAGE);
      System.exit(2);
    }
    if (output.exists() && !output.isFile())
    {
      System.err.println(USAGE);
      System.exit(3);
    }

    CompilationUnit compilationUnit;
    try
    {
      compilationUnit = ToyLanguageParser.parse(input);
    }
    catch (LanguageParseException e)
    {
      printParseError(e.getMessage(), e.getLexicalPhrase());
      System.exit(4);
      return;
    }
    catch (ParseException e)
    {
      e.printStackTrace();
      System.exit(5);
      return;
    }
    catch (BadTokenException e)
    {
      Token<ParseType> token = e.getBadToken();
      String message;
      LexicalPhrase lexicalPhrase;
      if (token.getType() == null)
      {
        message = "Unexpected end of input, expected one of: " + buildStringList(e.getExpectedTokenTypes());
        lexicalPhrase = (LexicalPhrase) token.getValue();
      }
      else
      {
        message = "Unexpected " + token.getType() + ", expected one of: " + buildStringList(e.getExpectedTokenTypes());
        // extract the LexicalPhrase from the token's value
        // this is simply a matter of casting in most cases, but for literals it must be extracted differently
        if (token.getType() == ParseType.NAME)
        {
          lexicalPhrase = ((Name) token.getValue()).getLexicalPhrase();
        }
        else if (token.getType() == ParseType.INTEGER_LITERAL)
        {
          lexicalPhrase = ((IntegerLiteral) token.getValue()).getLexicalPhrase();
        }
        else if (token.getValue() instanceof LexicalPhrase)
        {
          lexicalPhrase = (LexicalPhrase) token.getValue();
        }
        else
        {
          lexicalPhrase = null;
        }
      }
      printParseError(message, lexicalPhrase);
      System.exit(4);
      return;
    }

    try
    {
      Resolver.resolve(compilationUnit);
      CycleChecker.checkCycles(compilationUnit);
      ControlFlowChecker.checkControlFlow(compilationUnit);
      TypeChecker.checkTypes(compilationUnit);
    }
    catch (ConceptualException e)
    {
      printConceptualException(e.getMessage(), e.getLexicalPhrase());
      System.exit(6);
    }
    catch (NameNotResolvedException e)
    {
      printConceptualException(e.getMessage(), e.getLexicalPhrase());
      System.exit(6);
    }

    new CodeGenerator(compilationUnit).generate(output.getAbsolutePath());
    System.out.println(compilationUnit);
  }

  /**
   * Builds a string representing a list of the specified objects, separated by commas.
   * @param objects - the objects to convert to Strings and add to the list
   * @return the String representation of the list
   */
  private static String buildStringList(Object[] objects)
  {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < objects.length; i++)
    {
      buffer.append(objects[i]);
      if (i != objects.length - 1)
      {
        buffer.append(", ");
      }
    }
    return buffer.toString();
  }

  /**
   * Prints a parse error with the specified message and representing the location(s) that the LexicalPhrases store.
   * @param message - the message to print
   * @param lexicalPhrases - the LexicalPhrases representing the location in the input where the error occurred, or null if the location is the end of input
   */
  private static void printParseError(String message, LexicalPhrase... lexicalPhrases)
  {
    if (lexicalPhrases == null || lexicalPhrases.length < 1)
    {
      System.err.println(message);
      return;
    }
    // make a String representation of the LexicalPhrases' character ranges
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < lexicalPhrases.length; i++)
    {
      // line:start-end
      if (lexicalPhrases[i] == null)
      {
        buffer.append("<Unknown Location>");
      }
      else
      {
        buffer.append(lexicalPhrases[i].getLocationText());
      }
      if (i != lexicalPhrases.length - 1)
      {
        buffer.append(", ");
      }
    }
    if (lexicalPhrases.length == 1)
    {
      System.err.println(buffer + ": " + message);
      if (lexicalPhrases[0] != null)
      {
        System.err.println(lexicalPhrases[0].getHighlightedLine());
      }
    }
    else
    {
      System.err.println(buffer + ": " + message);
      for (LexicalPhrase phrase : lexicalPhrases)
      {
        System.err.println(phrase.getHighlightedLine());
      }
    }
  }

  /**
   * Prints all of the data from a ConceptualException to System.err.
   * @param message - the message from the exception
   * @param lexicalPhrases - the LexicalPhrases associated with the exception
   */
  public static void printConceptualException(String message, LexicalPhrase... lexicalPhrases)
  {
    if (lexicalPhrases == null || lexicalPhrases.length < 1)
    {
      System.err.println(message);
      return;
    }
    // make a String representation of the LexicalPhrases' character ranges
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < lexicalPhrases.length; i++)
    {
      // line:start-end
      buffer.append(lexicalPhrases[i].getLocationText());
      if (i != lexicalPhrases.length - 1)
      {
        buffer.append(", ");
      }
    }
    if (lexicalPhrases.length == 1)
    {
      System.err.println(buffer + ": " + message);
      System.err.println(lexicalPhrases[0].getHighlightedLine());
    }
    else
    {
      System.err.println(buffer + ": " + message);
      for (LexicalPhrase phrase : lexicalPhrases)
      {
        System.err.println(phrase.getHighlightedLine());
      }
    }
  }
}
