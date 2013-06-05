package eu.bryants.anthony.plinth.ast.type;

import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;

/*
 * Created on 5 Apr 2013
 */

/**
 * @author Anthony Bryant
 */
public class WildcardType extends Type
{

  private boolean explicitlyImmutable;
  private boolean contextuallyImmutable;

  private Type[] superTypes;
  private Type[] subTypes;

  public WildcardType(boolean nullable, boolean explicitlyImmutable, boolean contextuallyImmutable, Type[] superTypes, Type[] subTypes, LexicalPhrase lexicalPhrase)
  {
    super(nullable, lexicalPhrase);
    this.explicitlyImmutable = explicitlyImmutable;
    this.contextuallyImmutable = contextuallyImmutable;
    this.superTypes = superTypes;
    this.subTypes = subTypes;
    {} // TODO: add wildcard type support throughout the compiler
    throw new UnsupportedOperationException("Wildcard types are not supported yet!");
  }

  /**
   * @return the explicitlyImmutable
   */
  public boolean isExplicitlyImmutable()
  {
    return explicitlyImmutable;
  }

  /**
   * @return the contextuallyImmutable
   */
  public boolean isContextuallyImmutable()
  {
    return contextuallyImmutable;
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
   * {@inheritDoc}
   */
  @Override
  public boolean canAssign(Type type)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEquivalent(Type type)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRuntimeEquivalent(Type type)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<MemberReference<?>> getMembers(String name)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasDefaultValue()
  {
    return isNullable();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append('W');
    if (superTypes != null)
    {
      for (int i = 0; i < superTypes.length; ++i)
      {
        buffer.append(superTypes[i].getMangledName());
      }
    }
    buffer.append('_');
    if (subTypes != null)
    {
      for (int i = 0; i < subTypes.length; ++i)
      {
        buffer.append(subTypes[i].getMangledName());
      }
    }
    buffer.append('E');
    return buffer.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append('?');
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
