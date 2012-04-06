package eu.bryants.anthony.toylanguage.compiler;

import java.io.File;
import java.io.FileNotFoundException;

import parser.BadTokenException;
import parser.ParseException;
import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
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

  public static void main(String... args) throws FileNotFoundException, ParseException, BadTokenException, NameNotResolvedException, ConceptualException
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

    CompilationUnit compilationUnit = ToyLanguageParser.parse(input);

    Resolver.resolve(compilationUnit);

    new CodeGenerator(compilationUnit).generate(output.getAbsolutePath());
    System.out.println(compilationUnit);
  }
}
