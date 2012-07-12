package eu.bryants.anthony.toylanguage.compiler;

import java.util.LinkedList;
import java.util.List;

/*
 * Created on 12 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class ArgumentParser
{
  private String output;
  private String outputDir;
  private String[] sources;

  public ArgumentParser(String... arguments)
  {
    List<String> sourceList = new LinkedList<String>();
    for (int i = 0; i < arguments.length; ++i)
    {
      if (arguments[i].equals("-o"))
      {
        if (i >= arguments.length - 1 | output != null | !sourceList.isEmpty())
        {
          usage();
        }
        ++i;
        output = arguments[i];
        continue;
      }
      if (arguments[i].equals("-d"))
      {
        if (i >= arguments.length - 1 | outputDir != null | !sourceList.isEmpty())
        {
          usage();
        }
        ++i;
        outputDir = arguments[i];
        continue;
      }
      sourceList.add(arguments[i]);
    }
    sources = sourceList.toArray(new String[sourceList.size()]);
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
   * Prints the usage information for the program.
   */
  public static void usage()
  {
    System.out.println("Usage: java eu.bryants.anthony.toylanguage.compiler.Compiler [option]... [source-file]...\n" +
                       "Options:\n" +
                       "  -o <binary>, --output <binary>\n" +
                       "      Specifies the name of the binary produced.\n" +
                       "  -d <output-dir>, --output-dir <output-dir>\n" +
                       "      Specifies the output directory for the generated bitcode files. They will be generated in a directory structure equivalent to the package structure. If this is not specified, these files will not be generated.");
  }
}
