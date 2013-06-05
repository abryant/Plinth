package eu.bryants.anthony.plinth.ast.type;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;

/*
 * Created on 24 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeParameter
{

  private String name;
  private Type[] superTypes;
  private Type[] subTypes;

  private LexicalPhrase lexicalPhrase;

  private TypeDefinition containingTypeDefinition;

  public TypeParameter(String name, Type[] superTypes, Type[] subTypes, LexicalPhrase lexicalPhrase)
  {
    this.name = name;
    this.superTypes = superTypes;
    this.subTypes = subTypes;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the superTypes
   */
  public Type[] getSuperTypes()
  {
    return superTypes;
  }

  /**
   * @return the subTypes
   */
  public Type[] getSubTypes()
  {
    return subTypes;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

  /**
   * @return the containingTypeDefinition
   */
  public TypeDefinition getContainingTypeDefinition()
  {
    return containingTypeDefinition;
  }

  /**
   * @param containingTypeDefinition - the containingTypeDefinition to set
   */
  public void setContainingTypeDefinition(TypeDefinition containingTypeDefinition)
  {
    this.containingTypeDefinition = containingTypeDefinition;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append(name);
    if (superTypes != null && superTypes.length > 0)
    {
      buffer.append(" extends ");
      for (int i = 0; i < superTypes.length; ++i)
      {
        buffer.append(superTypes[i]);
        if (i != superTypes.length - 1)
        {
          buffer.append(" & ");
        }
      }
    }
    if (subTypes != null && subTypes.length > 0)
    {
      buffer.append(" super ");
      for (int i = 0; i < subTypes.length; ++i)
      {
        buffer.append(subTypes[i]);
        if (i != subTypes.length - 1)
        {
          buffer.append(" & ");
        }
      }
    }
    return buffer.toString();
  }

}
