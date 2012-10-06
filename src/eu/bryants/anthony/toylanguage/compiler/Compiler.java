package eu.bryants.anthony.toylanguage.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import parser.BadTokenException;
import parser.ParseException;
import parser.Token;
import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.TypeDefinition;
import eu.bryants.anthony.toylanguage.ast.metadata.PackageNode;
import eu.bryants.anthony.toylanguage.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.compiler.passes.ControlFlowChecker;
import eu.bryants.anthony.toylanguage.compiler.passes.CycleChecker;
import eu.bryants.anthony.toylanguage.compiler.passes.NativeNameChecker;
import eu.bryants.anthony.toylanguage.compiler.passes.Resolver;
import eu.bryants.anthony.toylanguage.compiler.passes.TypeChecker;
import eu.bryants.anthony.toylanguage.compiler.passes.TypePropagator;
import eu.bryants.anthony.toylanguage.compiler.passes.llvm.CodeGenerator;
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
  public static final String BITCODE_EXTENSION = ".bc";

  public static void main(String... args) throws FileNotFoundException
  {
    ArgumentParser argumentParser = new ArgumentParser(args);
    String[] sources = argumentParser.getSources();
    String outputDir = argumentParser.getOutputDir();
    if (sources.length < 1 || outputDir == null)
    {
      ArgumentParser.usage();
      System.exit(1);
    }
    File outputDirFile = new File(outputDir);
    if (!outputDirFile.isDirectory())
    {
      ArgumentParser.usage();
      System.exit(2);
    }

    File[] sourceFiles = new File[sources.length];
    CompilationUnit[] compilationUnits = new CompilationUnit[sources.length];
    for (int i = 0; i < sources.length; ++i)
    {
      sourceFiles[i] = new File(sources[i]);
      if (!sourceFiles[i].isFile())
      {
        System.err.println("Source is not a file: " + sourceFiles[i]);
        System.exit(3);
      }

      try
      {
        compilationUnits[i] = ToyLanguageParser.parse(sourceFiles[i], sources[i]);
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
    }

    List<File> searchDirectories = new LinkedList<File>();
    searchDirectories.add(outputDirFile);
    BitcodePackageSearcher bitcodePackageSearcher = new BitcodePackageSearcher(searchDirectories);

    PackageNode rootPackage = new PackageNode(bitcodePackageSearcher);
    Resolver resolver = new Resolver(rootPackage);
    bitcodePackageSearcher.initialise(rootPackage, resolver);

    try
    {
      for (CompilationUnit compilationUnit : compilationUnits)
      {
        resolver.resolvePackages(compilationUnit);
      }
      for (CompilationUnit compilationUnit : compilationUnits)
      {
        resolver.resolveTopLevelTypes(compilationUnit);
      }
      for (CompilationUnit compilationUnit : compilationUnits)
      {
        resolver.resolve(compilationUnit);
      }
      for (CompilationUnit compilationUnit : compilationUnits)
      {
        CycleChecker.checkCycles(compilationUnit);
      }
      for (CompilationUnit compilationUnit : compilationUnits)
      {
        ControlFlowChecker.checkControlFlow(compilationUnit);
      }
      for (CompilationUnit compilationUnit : compilationUnits)
      {
        TypeChecker.checkTypes(compilationUnit);
      }
      for (CompilationUnit compilationUnit : compilationUnits)
      {
        TypePropagator.propagateTypes(compilationUnit);
      }
      for (CompilationUnit compilationUnit : compilationUnits)
      {
        NativeNameChecker.checkNativeNames(compilationUnit);
      }
    }
    catch (ConceptualException e)
    {
      printConceptualException(e.getMessage(), e.getLexicalPhrase());
      e.printStackTrace();
      System.exit(6);
    }
    catch (NameNotResolvedException e)
    {
      printConceptualException(e.getMessage(), e.getLexicalPhrase());
      e.printStackTrace();
      System.exit(6);
    }

    Map<TypeDefinition, File> resultFiles = new HashMap<TypeDefinition, File>();
    for (CompilationUnit compilationUnit : compilationUnits)
    {
      PackageNode declaredPackage = compilationUnit.getResolvedPackage();
      File packageDir = findPackageDir(outputDirFile, declaredPackage);
      for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
      {
        File outputFile = new File(packageDir, typeDefinition.getName() + BITCODE_EXTENSION);
        if (outputFile.exists() && !outputFile.isFile())
        {
          System.err.println("Cannot create output file for " + typeDefinition.getQualifiedName() + ", a non-file with that name already exists");
          System.exit(9);
        }
        if (outputFile.exists())
        {
          if (!outputFile.delete())
          {
            System.err.println("Cannot create output file for " + typeDefinition.getQualifiedName() + ", failed to delete existing file");
            System.exit(10);
          }
        }
        resultFiles.put(typeDefinition, outputFile);
      }
      // print each compilation unit before writing their bitcode files
      System.out.println(compilationUnit);
    }

    for (Entry<TypeDefinition, File> entry : resultFiles.entrySet())
    {
      TypeDefinition typeDefinition = entry.getKey();
      File file = entry.getValue();
      CodeGenerator generator = new CodeGenerator(typeDefinition);
      generator.generateModule();

      generator.writeModule(file.getAbsolutePath());
    }
  }

  /**
   * Finds the directory for the specified package node in the given root package directory, creating subdirectories as necessary.
   * This method assumes that rootPackageDir exists and is a directory.
   * @param rootPackageDir - the root package directory
   * @param packageNode - the PackageNode to find the package of
   * @return the File representing the PackageNode's directory
   */
  private static File findPackageDir(File rootPackageDir, PackageNode packageNode)
  {
    String[] names = packageNode.getQualifiedName().getNames();
    File current = rootPackageDir;
    for (String name : names)
    {
      current = new File(current, name);
      if (current.exists() && !current.isDirectory())
      {
        System.err.println("Cannot create a sub-directory for package: " + packageNode.getQualifiedName() + " - a file with that name already exists");
        System.exit(7);
      }
      if (!current.isDirectory())
      {
        if (!current.mkdir())
        {
          System.err.println("Failed to create a sub-directory for package: " + packageNode.getQualifiedName());
          System.exit(8);
        }
      }
    }
    return current;
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
      if (lexicalPhrases[i] == null)
      {
        System.err.println(message);
        return;
      }
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
