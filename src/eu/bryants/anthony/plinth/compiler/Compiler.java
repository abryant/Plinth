package eu.bryants.anthony.plinth.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMModuleRef;
import parser.BadTokenException;
import parser.ParseException;
import parser.Token;
import eu.bryants.anthony.plinth.ast.CompilationUnit;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.metadata.PackageNode;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.terminal.IntegerLiteral;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.compiler.passes.ControlFlowChecker;
import eu.bryants.anthony.plinth.compiler.passes.CycleChecker;
import eu.bryants.anthony.plinth.compiler.passes.NativeNameChecker;
import eu.bryants.anthony.plinth.compiler.passes.Resolver;
import eu.bryants.anthony.plinth.compiler.passes.SpecialTypeHandler;
import eu.bryants.anthony.plinth.compiler.passes.TypeChecker;
import eu.bryants.anthony.plinth.compiler.passes.TypePropagator;
import eu.bryants.anthony.plinth.compiler.passes.llvm.CodeGenerator;
import eu.bryants.anthony.plinth.compiler.passes.llvm.Linker;
import eu.bryants.anthony.plinth.compiler.passes.llvm.LinkerException;
import eu.bryants.anthony.plinth.compiler.passes.llvm.MalformedMetadataException;
import eu.bryants.anthony.plinth.compiler.passes.llvm.MetadataLoader;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.PlinthParser;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Compiler
{
  public static final String BITCODE_EXTENSION = ".pbc";

  public static void main(String... args) throws FileNotFoundException
  {
    ArgumentParser argumentParser = new ArgumentParser(args);
    String[] sources = argumentParser.getSources();
    String[] importedFiles = argumentParser.getImportedFiles();
    Set<String> linkedFiles = argumentParser.getLinkedFileSet();
    String output = argumentParser.getOutput();
    String outputDir = argumentParser.getOutputDir();
    if (sources.length < 1 || (outputDir == null && output == null))
    {
      ArgumentParser.usage();
      System.exit(1);
    }
    File outputFile = null;
    if (output != null)
    {
      outputFile = new File(output);
      if (outputFile.exists())
      {
        if (!outputFile.isFile())
        {
          System.err.println("Output already exists, and is not a file: " + outputFile);
          System.exit(2);
        }
        if (!outputFile.delete())
        {
          System.err.println("Output already exists, and could not be deleted: " + outputFile);
          System.exit(2);
        }
      }
    }
    File outputDirFile = null;
    if (outputDir != null)
    {
      outputDirFile = new File(outputDir);
      if (!outputDirFile.isDirectory())
      {
        ArgumentParser.usage();
        System.exit(3);
      }
    }

    File[] sourceFiles = new File[sources.length];
    CompilationUnit[] compilationUnits = new CompilationUnit[sources.length];
    for (int i = 0; i < sources.length; ++i)
    {
      sourceFiles[i] = new File(sources[i]);
      if (!sourceFiles[i].isFile())
      {
        System.err.println("Source is not a file: " + sourceFiles[i]);
        System.exit(4);
      }

      try
      {
        compilationUnits[i] = PlinthParser.parse(sourceFiles[i], sources[i]);
      }
      catch (LanguageParseException e)
      {
        printParseError(e.getMessage(), e.getLexicalPhrase());
        System.exit(5);
        return;
      }
      catch (ParseException e)
      {
        e.printStackTrace();
        System.exit(6);
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
        System.exit(5);
        return;
      }
    }

    List<File> searchDirectories = new LinkedList<File>();
    if (outputDirFile != null)
    {
      searchDirectories.add(outputDirFile);
    }
    BitcodePackageSearcher bitcodePackageSearcher = new BitcodePackageSearcher(searchDirectories);

    PackageNode rootPackage = new PackageNode(bitcodePackageSearcher);
    Resolver resolver = new Resolver(rootPackage);
    bitcodePackageSearcher.initialise(rootPackage, resolver);

    List<TypeDefinition> importedTypeDefinitions = new LinkedList<TypeDefinition>();
    List<LLVMModuleRef> linkedModules = new LinkedList<LLVM.LLVMModuleRef>();
    for (String filename : importedFiles)
    {
      LLVMModuleRef module;
      try
      {
        module = Linker.loadModule(new File(filename));
      }
      catch (IOException e)
      {
        e.printStackTrace();
        System.err.println("Error loading bitcode from '" + filename + "' - skipping.");
        continue;
      }
      List<TypeDefinition> newDefinitions = loadImportedTypeDefinitions(module, filename, rootPackage);
      importedTypeDefinitions.addAll(newDefinitions);
      if (linkedFiles.contains(filename))
      {
        linkedModules.add(module);
      }
    }

    try
    {
      for (CompilationUnit compilationUnit : compilationUnits)
      {
        resolver.resolvePackages(compilationUnit);
      }
      for (TypeDefinition typeDefinition : importedTypeDefinitions)
      {
        resolver.resolveTypes(typeDefinition, null);
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
      SpecialTypeHandler.verifySpecialTypes();
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
      System.exit(7);
    }
    catch (NameNotResolvedException e)
    {
      printConceptualException(e.getMessage(), e.getLexicalPhrase());
      e.printStackTrace();
      System.exit(7);
    }

    Map<TypeDefinition, File> resultFiles = new HashMap<TypeDefinition, File>();
    if (outputDirFile != null)
    {
      for (CompilationUnit compilationUnit : compilationUnits)
      {
        PackageNode declaredPackage = compilationUnit.getResolvedPackage();
        File packageDir = findPackageDir(outputDirFile, declaredPackage);
        for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
        {
          File typeOutputFile = new File(packageDir, typeDefinition.getName() + BITCODE_EXTENSION);
          if (typeOutputFile.exists() && !typeOutputFile.isFile())
          {
            System.err.println("Cannot create output file for " + typeDefinition.getQualifiedName() + ", a non-file with that name already exists");
            System.exit(8);
          }
          if (typeOutputFile.exists())
          {
            if (!typeOutputFile.delete())
            {
              System.err.println("Cannot create output file for " + typeDefinition.getQualifiedName() + ", failed to delete existing file");
              System.exit(9);
            }
          }
          resultFiles.put(typeDefinition, typeOutputFile);
        }
        // print each compilation unit before writing their bitcode files
        System.out.println(compilationUnit);
      }
    }

    Linker linker = null;
    if (outputFile != null)
    {
      linker = new Linker(output);
      for (LLVMModuleRef imported : linkedModules)
      {
        try
        {
          linker.linkModule(imported);
          LLVM.LLVMDisposeModule(imported);
        }
        catch (LinkerException e)
        {
          e.printStackTrace();
          System.exit(10);
        }
      }
    }
    for (CompilationUnit compilationUnit : compilationUnits)
    {
      for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
      {
        CodeGenerator generator = new CodeGenerator(typeDefinition);
        generator.generateModule();
        LLVMModuleRef module = generator.getModule();

        File resultFile = resultFiles.get(typeDefinition);
        if (resultFile != null)
        {
          LLVM.LLVMWriteBitcodeToFile(module, resultFile.getAbsolutePath());
        }
        if (linker != null)
        {
          try
          {
            linker.linkModule(module);
          }
          catch (LinkerException e)
          {
            e.printStackTrace();
            System.exit(10);
          }
        }
      }
    }

    if (linker != null)
    {
      LLVM.LLVMWriteBitcodeToFile(linker.getLinkedModule(), outputFile.getAbsolutePath());
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
    if (packageNode.getQualifiedName() == null)
    {
      return rootPackageDir;
    }
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
   * Loads all of the type definitions declared in the specified module into their respective packages under the root package.
   * Note: the loaded TypeDefinitions returned from this method must all have their types resolved later on, by calling Resolver.resolveTypes(typeDef, null).
   * @param module - the module to load the type definitions from
   * @param moduleFileName - the file name of the module that we are loading type definitions from
   * @param rootPackage - the root package to add the type definitions to
   * @return the list of TypeDefinitions loaded
   */
  private static List<TypeDefinition> loadImportedTypeDefinitions(LLVMModuleRef module, String moduleFileName, PackageNode rootPackage)
  {
    List<TypeDefinition> typeDefinitions;
    try
    {
      typeDefinitions = MetadataLoader.loadTypeDefinitions(module);
    }
    catch (MalformedMetadataException e)
    {
      // we couldn't load any type definitions from the file, so print the error and abort
      e.printStackTrace();
      System.err.println("Metadata parse error occurred while loading '" + moduleFileName + "' - skipping.\n" +
                         "  Note: If it was specified with '--link', this error will not exclude it from being linked.");
      return new LinkedList<TypeDefinition>();
    }
    Iterator<TypeDefinition> it = typeDefinitions.iterator();
    while (it.hasNext())
    {
      TypeDefinition typeDefinition = it.next();
      QName qualifiedName = typeDefinition.getQualifiedName();
      QName containingPackageName = new QName(qualifiedName.getAllNamesButLast());
      try
      {
        PackageNode containingPackage = rootPackage.addPackageTree(containingPackageName);
        containingPackage.addTypeDefinition(typeDefinition);
      }
      catch (ConceptualException e)
      {
        // there was a name conflict, so abort for this type definition and try the others
        e.printStackTrace();
        System.err.println("Name conflict occurred while loading '" + qualifiedName + "' - skipping.\n" +
                           "  Note: If it was specified with '--link', this error will not exclude it from being linked.");
        it.remove();
        continue;
      }
    }
    return typeDefinitions;
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