package eu.bryants.anthony.plinth.compiler;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/*
 * Created on 12 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class ArgumentParser
{
  private String mainTypeName;
  private String output;
  private String outputDir;
  private String[] sources;
  private String[] importFiles;
  private Set<String> linkSet = new HashSet<String>();

  public ArgumentParser(String... arguments)
  {
    List<String> sourceList = new LinkedList<String>();
    List<String> importList = new LinkedList<String>();
    for (int i = 0; i < arguments.length; ++i)
    {
      if (arguments[i].equals("-m") || arguments[i].equals("--main-type"))
      {
        if (i >= arguments.length - 1 | mainTypeName != null)
        {
          usage();
          System.exit(1);
        }
        ++i;
        mainTypeName = arguments[i];
        continue;
      }
      if (arguments[i].equals("-o") || arguments[i].equals("--output"))
      {
        if (i >= arguments.length - 1 | output != null)
        {
          usage();
          System.exit(1);
        }
        ++i;
        output = arguments[i];
        continue;
      }
      if (arguments[i].equals("-d") || arguments[i].equals("--output-dir"))
      {
        if (i >= arguments.length - 1 | outputDir != null)
        {
          usage();
          System.exit(1);
        }
        ++i;
        outputDir = arguments[i];
        continue;
      }
      if (arguments[i].equals("-i") || arguments[i].equals("--import"))
      {
        if (i >= arguments.length - 1)
        {
          usage();
          System.exit(1);
        }
        ++i;
        importList.add(arguments[i]);
        continue;
      }
      if (arguments[i].equals("-l") || arguments[i].equals("--link"))
      {
        if (i >= arguments.length - 1)
        {
          usage();
          System.exit(1);
        }
        ++i;
        importList.add(arguments[i]);
        linkSet.add(arguments[i]);
        continue;
      }
      sourceList.add(arguments[i]);
    }
    sources = sourceList.toArray(new String[sourceList.size()]);
    importFiles = importList.toArray(new String[importList.size()]);
  }

  /**
   * @return the mainTypeName
   */
  public String getMainTypeName()
  {
    return mainTypeName;
  }

  /**
   * @return the output
   */
  public String getOutput()
  {
    return output;
  }

  /**
   * @return the outputDir
   */
  public String getOutputDir()
  {
    return outputDir;
  }

  /**
   * @return the sources
   */
  public String[] getSources()
  {
    return sources;
  }

  /**
   * @return the array of names of files which were specified with --import or --link, in the order they were specified
   */
  public String[] getImportedFiles()
  {
    return importFiles;
  }

  /**
   * @return the set of names of files which were specified with --link. A file name in this set will always be in the array returned by getImportedFiles().
   */
  public Set<String> getLinkedFileSet()
  {
    return linkSet;
  }

  /**
   * Prints the usage information for the program.
   */
  public static void usage()
  {
    System.out.println("Usage: java eu.bryants.anthony.plinth.compiler.Compiler [option]... [source-file]...\n" +
                       "Options:\n" +
                       "  -m <main-type>, --main-type <main-type>\n" +
                       "      Generates a low-level main method which calls the main() method in the type with the specified fully qualified name.\n" +
                       "      The correct signature for a main method is:\n" +
                       "        static uint main([]string)" +
                       "  -o <binary>, --output <binary>\n" +
                       "      Specifies the name of the binary produced.\n" +
                       "  -d <output-dir>, --output-dir <output-dir>\n" +
                       "      Specifies the output directory for the generated bitcode files, which will be generated in a directory structure equivalent to the package structure.\n" +
                       "      If this is not specified, these files will not be generated.\n" +
                       "  -i <bitcode-file>, --import <bitcode-file>\n" +
                       "      Imports the type definition(s) defined in the specified file into the search when searching for types.\n" +
                       "      Note: This option can be specified multiple times.\n" +
                       "  -l <bitcode-file>, --link <bitcode-file>\n" +
                       "      Links the specified bitcode file into the output module.\n" +
                       "      Using this option also implicitly imports the specified bitcode file (i.e. it also acts as --import).\n" +
                       "      Note: This option can be specified multiple times.");
  }
}
