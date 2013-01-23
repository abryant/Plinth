package eu.bryants.anthony.plinth.compiler;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMModuleRef;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.metadata.PackageNode;
import eu.bryants.anthony.plinth.ast.metadata.PackageNode.PackageSearcher;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.compiler.passes.llvm.Linker;
import eu.bryants.anthony.plinth.compiler.passes.llvm.MalformedMetadataException;
import eu.bryants.anthony.plinth.compiler.passes.llvm.MetadataLoader;

/*
 * Created on 3 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class BitcodePackageSearcher implements PackageSearcher
{
  private List<File> searchDirectories;

  private PackageNode rootPackage;
  private PassManager passManager;

  /**
   * Creates a new BitcodePackageSearcher which will search the specified list of directories for bitcode files.
   * @param searchDirectories - the list of directories to search for missing bitcode files
   * @param rootPackage - the root PackageNode of the package structure to load things into
   */
  public BitcodePackageSearcher(List<File> searchDirectories)
  {
    this.searchDirectories = searchDirectories;
  }

  /**
   * Initialises this BitcodePackageSearcher with the specified root PackageNode and Resolver.
   * @param rootPackage - the root PackageNode of the package hierarchy to add things to
   * @param passManager - the PassManager to add newly-loaded TypeDefinitions to
   */
  public void initialise(PackageNode rootPackage, PassManager passManager)
  {
    this.rootPackage = rootPackage;
    this.passManager = passManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void search(String name, PackageNode packageNode) throws NameNotResolvedException, ConceptualException
  {
    QName packageQName = packageNode.getQualifiedName();
    String[] packageNames = packageQName == null ? new String[0] : packageQName.getNames();
    for (File startDir : searchDirectories)
    {
      File currentDir = startDir;
      for (int i = 0; i < packageNames.length; ++i)
      {
        if (!currentDir.isDirectory())
        {
          currentDir = null;
          break;
        }
        currentDir = new File(currentDir, packageNames[i]);
      }
      if (currentDir == null)
      {
        continue;
      }
      File subPackageDir = new File(currentDir, name);
      if (subPackageDir.isDirectory())
      {
        try
        {
          rootPackage.addPackageTree(packageQName == null ? new QName(name, null) : new QName(packageQName, name, null));
        }
        catch (ConceptualException e)
        {
          e.printStackTrace();
        }
        // even if we failed, stop searching, because a failure to add a package means that something already exists with that name
        break;
      }

      File bitcodeFile = new File(currentDir, name + Compiler.BITCODE_EXTENSION);
      if (bitcodeFile.isFile())
      {
        List<TypeDefinition> loadedDefinitions;
        try
        {
          LLVMModuleRef module = Linker.loadModule(bitcodeFile);
          loadedDefinitions = MetadataLoader.loadTypeDefinitions(module);
          LLVM.LLVMDisposeModule(module);
        }
        catch (IOException e)
        {
          e.printStackTrace();
          continue;
        }
        catch (MalformedMetadataException e)
        {
          e.printStackTrace();
          continue;
        }

        Iterator<TypeDefinition> it = loadedDefinitions.iterator();
        while (it.hasNext())
        {
          TypeDefinition typeDefinition = it.next();
          String[] resultNames = typeDefinition.getQualifiedName().getAllNamesButLast();
          if (resultNames.length != packageNames.length)
          {
            it.remove();
            continue;
          }
          boolean equal = true;
          for (int i = 0; equal && i < resultNames.length; ++i)
          {
            equal &= resultNames[i].equals(packageNames[i]);
          }
          if (!equal)
          {
            it.remove();
            continue;
          }
          // the TypeDefinition is in the requested package, so try to add it
          try
          {
            packageNode.addTypeDefinition(typeDefinition);
          }
          catch (ConceptualException e)
          {
            // we couldn't add it to the package, so remove it from the list so that it won't be resolved later
            e.printStackTrace();
            it.remove();
          }
        }

        for (TypeDefinition typeDefinition : loadedDefinitions)
        {
          try
          {
            passManager.addTypeDefinition(typeDefinition);
          }
          catch (ConceptualException e)
          {
            packageNode.removeTypeDefinition(typeDefinition);
            Compiler.printConceptualException(e);
          }
        }

        // we should have found the correct file now, so don't try to load any more
        break;
      }
    }
  }

}
