package eu.bryants.anthony.plinth.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

import parser.BadTokenException;
import parser.ParseException;
import parser.Parser;
import parser.Token;
import parser.lalr.LALRParserGenerator;
import eu.bryants.anthony.plinth.ast.CompilationUnit;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class PlinthParser
{

  public static void main(String... args) throws FileNotFoundException, ParseException, BadTokenException
  {
    if (args.length < 1)
    {
      System.err.println("Please specify a file to parse");
      System.exit(1);
    }

    LALRParserGenerator<ParseType> parserGenerator = new LALRParserGenerator<ParseType>(PlinthParseRules.getAllRules());
    parserGenerator.generate(ParseType.GENERATED_START_RULE);

    File file = new File(args[0]);
    Reader reader = new FileReader(file);

    Parser<ParseType> parser = new Parser<ParseType>(parserGenerator.getStartState(), new LanguageTokenizer(reader, file.getAbsolutePath()));
    Token<ParseType> topLevelToken = parser.parse();
    CompilationUnit compilationUnit = (CompilationUnit) topLevelToken.getValue();
    System.out.println(compilationUnit);
  }

  /**
   * Parses the specified file into a CompilationUnit.
   * @param file - the file to open and parse
   * @param specifiedPath - the path that the user specified to the path, to be stored in LexicalPhrase objects in the AST (or shown in error messages)
   * @return the CompilationUnit parsed
   * @throws FileNotFoundException - if the file could not be opened
   * @throws ParseException - if there was a problem parsing the file
   * @throws BadTokenException - if the file did not conform to the language grammar
   */
  public static CompilationUnit parse(File file, String specifiedPath) throws FileNotFoundException, ParseException, BadTokenException
  {
    Reader reader = new FileReader(file);

    GeneratedParser parser = new GeneratedParser(new LanguageTokenizer(reader, specifiedPath));
    Token<ParseType> topLevelToken = parser.parse();
    return (CompilationUnit) topLevelToken.getValue();
  }
}
