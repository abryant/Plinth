package eu.bryants.anthony.toylanguage.ast.metadata;

import java.util.HashMap;
import java.util.Map;

import eu.bryants.anthony.toylanguage.ast.TypeDefinition;
import eu.bryants.anthony.toylanguage.ast.misc.QName;
import eu.bryants.anthony.toylanguage.compiler.ConceptualException;
import eu.bryants.anthony.toylanguage.compiler.NameNotResolvedException;

/*
 * Created on 10 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class PackageNode
{
  private QName qname;

  private Map<String, PackageNode> subPackages = new HashMap<String, PackageNode>();

  private Map<String, TypeDefinition> typeDefinitions = new HashMap<String, TypeDefinition>();

  private PackageSearcher searcher;

  /**
   * An interface which allows a PackageNode to request a search for an item which may be inside that package.
   * @author Anthony Bryant
   */
  public interface PackageSearcher
  {
    /**
     * Searches for a type definition or sub-package in the specified PackageNode with the specified name, and loads it into the PackageNode if it is found.
     * @param name - the name to search for
     * @param packageNode - the PackageNode to search in
     * @throws NameNotResolvedException - if a name could not be resolved in a newly-loaded bitcode file
     * @throws ConceptualException - if a conceptual problem is found in a newly-loaded bitcode file
     */
    public void search(String name, PackageNode packageNode) throws NameNotResolvedException, ConceptualException;
  }

  /**
   * Creates a new root PackageNode, which can then create references to sub-packages with addPackageTree().
   * @param packageSearcher - the PackageSearcher to use to search for missing type definitions
   */
  public PackageNode(PackageSearcher packageSearcher)
  {
    this.searcher = packageSearcher;
  }

  /**
   * Creates a PackageNode with the specified qualified name. This should only be called by addPackageTree() from the root package.
   * @param name - the qualified name of this PackageNode
   * @param packageSearcher - the PackageSearcher to use to search for missing type definitions
   */
  private PackageNode(QName qname, PackageSearcher packageSearcher)
  {
    this.qname = qname;
    this.searcher = packageSearcher;
  }

  /**
   * @return the qualified name
   */
  public QName getQualifiedName()
  {
    return qname;
  }

  /**
   * Finds the sub-package with the specified name.
   * This method may try to search for new packages to load, using a PackageSearcher.
   * @param name - the name of the sub-package to get
   * @return the sub-package with the specified name, or null if none exists
   * @throws NameNotResolvedException - if a name could not be resolved in a newly-loaded bitcode file
   * @throws ConceptualException - if a conceptual problem is found in a newly-loaded bitcode file
   */
  public PackageNode getSubPackage(String name) throws NameNotResolvedException, ConceptualException
  {
    PackageNode result = subPackages.get(name);
    if (result == null)
    {
      searcher.search(name, this);
      result = subPackages.get(name);
      // if we still haven't found anything after the search, then we aren't going to find anything
    }
    return result;
  }

  /**
   * Finds the type definition with the specified name.
   * This method may try to search for new type definitions to load, using a PackageSearcher.
   * @param name - the name of the type definition to get
   * @return the type definition with the specified name, or null if none exists
   * @throws NameNotResolvedException - if a name could not be resolved in a newly-loaded bitcode file
   * @throws ConceptualException - if a conceptual problem is found in a newly-loaded bitcode file
   */
  public TypeDefinition getTypeDefinition(String name) throws NameNotResolvedException, ConceptualException
  {
    TypeDefinition result = typeDefinitions.get(name);
    if (result == null)
    {
      searcher.search(name, this);
      result = typeDefinitions.get(name);
      // if we still haven't found anything after the search, then we aren't going to find anything
    }
    return result;
  }

  /**
   * Adds the specified package as a sub-package.
   * @param packageNode - the sub-package to add
   * @throws ConceptualException - if there is a name conflict
   */
  public void addSubPackage(PackageNode packageNode) throws ConceptualException
  {
    String packageName = packageNode.getQualifiedName().getLastName();
    if (subPackages.containsKey(packageName))
    {
      throw new ConceptualException("Cannot add sub-package to " + (qname == null ? "the root package" : qname) + " - it already contains a sub-package called \"" + packageName + "\"", null);
    }
    if (typeDefinitions.containsKey(packageName))
    {
      throw new ConceptualException("Cannot add sub-package to " + (qname == null ? "the root package" : qname) + " - it already contains a type called \"" + packageName + "\"", null);
    }
    subPackages.put(packageName, packageNode);
  }

  /**
   * Adds the specified TypeDefinition to this PackageNode.
   * @param typeDefinition - the TypeDefinition to add
   * @throws ConceptualException - if there is a name conflict
   */
  public void addTypeDefinition(TypeDefinition typeDefinition) throws ConceptualException
  {
    String typeName = typeDefinition.getName();
    if (subPackages.containsKey(typeName))
    {
      throw new ConceptualException("Cannot add type to " + (qname == null ? "the root package" : qname) + " - it already contains a sub-package called \"" + typeName + "\"", null);
    }
    if (typeDefinitions.containsKey(typeName))
    {
      throw new ConceptualException("Cannot add type to " + (qname == null ? "the root package" : qname) + " - it already contains a type called \"" + typeName + "\"", null);
    }
    typeDefinitions.put(typeName, typeDefinition);
  }

  /**
   * Adds the specified package tree to the root package.
   * If this is not the root package (i.e. it has a name) then calling this will result in an exception.
   * @param treeQName - the qualified package name to create
   * @return the PackageNode corresponding to the last element in the QName
   * @throws ConceptualException - if there is a name conflict
   */
  public PackageNode addPackageTree(QName treeQName) throws ConceptualException
  {
    if (qname != null)
    {
      throw new UnsupportedOperationException("Non-root PackageNodes cannot create a package tree.");
    }
    PackageNode current = this;
    for (String subName : treeQName.getNames())
    {
      PackageNode child = current.subPackages.get(subName);
      if (child == null)
      {
        QName currentQName = current.getQualifiedName();
        child = new PackageNode(currentQName == null ? new QName(subName, null) : new QName(currentQName, subName, null), searcher);
        current.addSubPackage(child);
      }
      current = child;
    }
    return current;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return qname.toString();
  }
}
