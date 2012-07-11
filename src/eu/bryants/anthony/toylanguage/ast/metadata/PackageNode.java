package eu.bryants.anthony.toylanguage.ast.metadata;

import java.util.HashMap;
import java.util.Map;

import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.misc.QName;
import eu.bryants.anthony.toylanguage.compiler.ConceptualException;

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

  private Map<String, CompoundDefinition> compoundDefinitions = new HashMap<String, CompoundDefinition>();

  /**
   * Creates a new root PackageNode, which can then create references to sub-packages with addPackageTree().
   */
  public PackageNode()
  {
    // do nothing
  }

  /**
   * Creates a PackageNode with the specified qualified name. This should only be called by addPackageTree() from the root package.
   * @param name - the qualified name of this PackageNode
   */
  private PackageNode(QName qname)
  {
    this.qname = qname;
  }

  /**
   * @return the qualified name
   */
  public QName getQualifiedName()
  {
    return qname;
  }

  /**
   * @param name - the name of the sub-package to get
   * @return the sub-package with the specified name
   */
  public PackageNode getSubPackage(String name)
  {
    return subPackages.get(name);
  }

  /**
   * @param name - the name of the compound definition to get
   * @return the compound definition with the specified name
   */
  public CompoundDefinition getCompoundDefinition(String name)
  {
    return compoundDefinitions.get(name);
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
      throw new ConceptualException("Cannot add sub-package to " + qname + " - a sub-package called \"" + packageName + "\" already exists.", null);
    }
    if (compoundDefinitions.containsKey(packageName))
    {
      throw new ConceptualException("Cannot add sub-package to " + qname + " - a compound type called \"" + packageName + "\" already exists.", null);
    }
    subPackages.put(packageName, packageNode);
  }

  /**
   * Adds the specified CompoundDefinition to this PackageNode.
   * @param compoundDefinition - the CompoundDefinition to add
   * @throws ConceptualException - if there is a name conflict
   */
  public void addCompoundDefinition(CompoundDefinition compoundDefinition) throws ConceptualException
  {
    String compoundName = compoundDefinition.getName();
    if (subPackages.containsKey(compoundName))
    {
      throw new ConceptualException("Cannot add compound type to " + qname + " - a sub-package called \"" + compoundName + "\" already exists.", null);
    }
    if (compoundDefinitions.containsKey(compoundName))
    {
      throw new ConceptualException("Cannot add compound type to " + qname + " - a compound type called \"" + compoundName + "\" already exists.", null);
    }
    compoundDefinitions.put(compoundName, compoundDefinition);
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
      throw new IllegalStateException("Non-root PackageNodes cannot create a package tree.");
    }
    PackageNode current = this;
    for (String subName : treeQName.getNames())
    {
      PackageNode child = current.getSubPackage(subName);
      if (child == null)
      {
        QName currentQName = current.getQualifiedName();
        child = new PackageNode(currentQName == null ? new QName(subName, null) : new QName(currentQName, subName, null));
        current.addSubPackage(child);
      }
      current = child;
    }
    return current;
  }
}
