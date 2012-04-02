package eu.bryants.anthony.toylanguage.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

import parser.BadTokenException;
import parser.ParseException;
import parser.Parser;
import parser.Token;
import parser.lalr.LALRParserGenerator;
import eu.bryants.anthony.toylanguage.ast.CompilationUnit;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ToyLanguageParser
{
  public static void main(String... args) throws FileNotFoundException, ParseException, BadTokenException
  {
    LALRParserGenerator<ParseType> parserGenerator = new LALRParserGenerator<ParseType>(ToyLanguageRules.getAllRules());
    parserGenerator.generate(ParseType.GENERATED_START_RULE);

    if (args.length < 1)
    {
      System.err.println("Please specify a file to parse");
      System.exit(1);
    }

    Reader reader = new FileReader(new File(args[0]));

    Parser<ParseType> parser = new Parser<ParseType>(parserGenerator.getStartState(), new LanguageTokenizer(reader));
    Token<ParseType> topLevelToken = parser.parse();
    CompilationUnit compilationUnit = (CompilationUnit) topLevelToken.getValue();
    System.out.println(compilationUnit);
  }
}
