package eu.bryants.anthony.plinth.ast.type;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.ArrayLengthMember;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;

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
    this.contextuallyImmutable = contextuallyImmutable | explicitlyImmutable;
    this.superTypes = superTypes;
    this.subTypes = subTypes;
  }

  public WildcardType(boolean nullable, boolean explicitlyImmutable, Type[] superTypes, Type[] subTypes, LexicalPhrase lexicalPhrase)
  {
    this(nullable, explicitlyImmutable, false, superTypes, subTypes, lexicalPhrase);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canBeNullable()
  {
    if (isNullable())
    {
      return true;
    }
    boolean hasNotNullSuperType = false;
    // wildcard types shouldn't really ever have circular references, but we process them in the
    // same way as we process type parameters in NamedType, so that we don't have to rely on this
    // assumption - and if it turns out to be wrong we won't get infinite recursion
    Set<TypeParameter> visitedParameters = new HashSet<TypeParameter>();
    Set<WildcardType> visitedWildcards = new HashSet<WildcardType>();
    Deque<Type> typeQueue = new LinkedList<Type>();
    for (Type superType : superTypes)
    {
      typeQueue.add(superType);
    }
    visitedWildcards.add(this);
    while (!typeQueue.isEmpty())
    {
      Type superType = typeQueue.poll();
      if (!superType.isNullable())
      {
        hasNotNullSuperType = true;
        break;
      }
      if (superType instanceof NamedType && ((NamedType) superType).getResolvedTypeParameter() != null)
      {
        TypeParameter typeParameter = ((NamedType) superType).getResolvedTypeParameter();
        if (visitedParameters.contains(typeParameter))
        {
          continue;
        }
        for (Type t : typeParameter.getSuperTypes())
        {
          typeQueue.add(t);
        }
        visitedParameters.add(typeParameter);
      }
      else if (superType instanceof WildcardType)
      {
        WildcardType wildcard = (WildcardType) superType;
        if (visitedWildcards.contains(wildcard))
        {
          continue;
        }
        for (Type t : wildcard.getSuperTypes())
        {
          typeQueue.add(t);
        }
        visitedWildcards.add(wildcard);
      }
    }
    return !hasNotNullSuperType;
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
   * Checks whether this type is either explicitly immutable, or refers to a super type or type parameter which might be explicitly immutable.
   * Note: this method should not be called until this type has been resolved
   * @return true if this type can be explicitly immutable, false otherwise
   */
  public boolean canBeExplicitlyImmutable()
  {
    if (isExplicitlyImmutable())
    {
      return true;
    }

    boolean hasNotImmutableSuperType = false;
    // wildcard types shouldn't really ever have circular references, but we process them in the
    // same way as we process type parameters in NamedType, so that we don't have to rely on this
    // assumption - and if it turns out to be wrong we won't get infinite recursion
    Set<TypeParameter> visitedParameters = new HashSet<TypeParameter>();
    Set<WildcardType> visitedWildcards = new HashSet<WildcardType>();
    Deque<Type> typeQueue = new LinkedList<Type>();
    for (Type superType : superTypes)
    {
      typeQueue.add(superType);
    }
    visitedWildcards.add(this);
    while (!typeQueue.isEmpty())
    {
      Type superType = typeQueue.poll();
      if (!Type.isExplicitlyDataImmutable(superType))
      {
        hasNotImmutableSuperType = true;
        break;
      }
      if (superType instanceof NamedType && ((NamedType) superType).getResolvedTypeParameter() != null)
      {
        TypeParameter typeParameter = ((NamedType) superType).getResolvedTypeParameter();
        if (visitedParameters.contains(typeParameter))
        {
          continue;
        }
        for (Type t : ((NamedType) superType).getResolvedTypeParameter().getSuperTypes())
        {
          typeQueue.add(t);
        }
        visitedParameters.add(typeParameter);
      }
      else if (superType instanceof WildcardType)
      {
        // wildcard types shouldn't really ever have circular references, but we process them as
        // if they do rather than recursing with another canBeExplicitlyImmutable() call
        WildcardType wildcard = (WildcardType) superType;
        if (visitedWildcards.contains(wildcard))
        {
          continue;
        }
        for (Type t : wildcard.getSuperTypes())
        {
          typeQueue.add(t);
        }
        visitedWildcards.add(wildcard);
      }
    }
    return !hasNotImmutableSuperType;
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
    if (isNullable() && type instanceof NullType)
    {
      // all nullable types can have null assigned to them
      return true;
    }

    // a nullable type cannot be assigned to a possibly-nullable or non-nullable type
    if (!isNullable() && type.isNullable())
    {
      return false;
    }
    // a possibly nullable type cannot be assigned to a not-nullable type
    if (!canBeNullable() && type.canBeNullable())
    {
      return false;
    }
    // an explicitly-immutable type cannot be assigned to a non-explicitly-immutable type
    if (!isExplicitlyImmutable() && Type.isExplicitlyDataImmutable(type))
    {
      return false;
    }
    // a contextually-immutable type cannot be assigned to a non-immutable type
    if (!isContextuallyImmutable() && Type.isContextuallyDataImmutable(type))
    {
      return false;
    }
    // a possibly-immutable type cannot be assigned to a not-possibly-immutable type
    if (!canBeExplicitlyImmutable() && Type.canBeExplicitlyDataImmutable(type))
    {
      return false;
    }

    // find a set of all the types we need to check against
    // if it is just a normal type, then we will have a single element set
    // but if it is a type parameter or a wildcard type, we will have a set of all of its super-types
    Set<TypeParameter> otherTypeParameters = new HashSet<TypeParameter>();
    Set<WildcardType> visitedWildcardSuperTypes = new HashSet<WildcardType>();
    Set<Type> otherSuperTypes = new HashSet<Type>();
    Deque<Type> typeQueue = new LinkedList<Type>();
    typeQueue.add(type);
    while (!typeQueue.isEmpty())
    {
      Type otherSuperType = typeQueue.poll();
      if (otherSuperType instanceof NamedType && ((NamedType) otherSuperType).getResolvedTypeParameter() != null)
      {
        TypeParameter otherTypeParameter = ((NamedType) otherSuperType).getResolvedTypeParameter();
        if (otherTypeParameters.contains(otherTypeParameter))
        {
          // since type parameters can extend each other, we need to continue here to make sure we don't infinite loop
          // with a circular super-type restriction (e.g. Foo<A extends B, B extends A>)
          continue;
        }
        otherTypeParameters.add(otherTypeParameter);
        // we can ignore the TypeParameter's nullability and immutability, as we have already checked that those properties do not conflict above
        for (Type parameterSuperType : otherTypeParameter.getSuperTypes())
        {
          typeQueue.add(parameterSuperType);
        }
      }
      else if (otherSuperType instanceof WildcardType)
      {
        WildcardType wildcardType = (WildcardType) otherSuperType;
        if (visitedWildcardSuperTypes.contains(wildcardType))
        {
          continue;
        }
        visitedWildcardSuperTypes.add(wildcardType);
        // we can ignore the Wildcard type's nullability and immutability, as we have already checked that those properties do not conflict above
        for (Type t : wildcardType.getSuperTypes())
        {
          typeQueue.add(t);
        }
      }
      else
      {
        otherSuperTypes.add(otherSuperType);
      }
    }
    if (otherSuperTypes.isEmpty())
    {
      // there is no upper bound for this type, so just use ?#object
      otherSuperTypes.add(new ObjectType(true, true, null));
    }

    // find a set of all of our sub-types
    Set<TypeParameter> visitedParameters = new HashSet<TypeParameter>();
    Set<WildcardType> visitedWildcardSubTypes = new HashSet<WildcardType>();
    Set<Type> thisSubTypes = new HashSet<Type>();
    Deque<Type> thisSubTypeQueue = new LinkedList<Type>();
    thisSubTypeQueue.add(this);
    while (!thisSubTypeQueue.isEmpty())
    {
      Type subType = thisSubTypeQueue.poll();
      if (subType instanceof NamedType && ((NamedType) subType).getResolvedTypeParameter() != null)
      {
        TypeParameter subTypeParameter = ((NamedType) subType).getResolvedTypeParameter();
        if (visitedParameters.contains(subTypeParameter))
        {
          // since type parameters can be super-types of each other, we need to continue here to make sure we don't infinite loop
          // with a circular super-type restriction (e.g. Foo<A super B, B super A>)
          continue;
        }
        visitedParameters.add(subTypeParameter);
        // we can ignore the TypeParameter's nullability and immutability, as we have already checked that those properties do not conflict above
        for (Type parameterSubType : subTypeParameter.getSubTypes())
        {
          thisSubTypeQueue.add(parameterSubType);
        }
      }
      else if (subType instanceof WildcardType)
      {
        WildcardType wildcardType = (WildcardType) subType;
        if (visitedWildcardSubTypes.contains(wildcardType))
        {
          continue;
        }
        visitedWildcardSubTypes.add(wildcardType);
        // we can ignore the Wildcard type's nullability and immutability, as we have already checked that those properties do not conflict above
        for (Type t : wildcardType.getSubTypes())
        {
          thisSubTypeQueue.add(t);
        }
      }
      else
      {
        thisSubTypes.add(subType);
      }
    }

    for (Type lowerType : thisSubTypes)
    {
      for (Type otherSuperType : otherSuperTypes)
      {
        if (lowerType.canAssign(otherSuperType))
        {
          // one of our sub-types can assign one of the other type's super-types, which is all we need for the assignment to work
          // (this is because every one of our sub-types necessarily extends any actual type that the wildcard type might end up being)
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks whether this WildcardType can be assigned to the specified Type, ignoring nullability and immutability constraints.
   * This should be called by individual canAssign() methods on other Types when they don't define their own behaviour for wildcard types.
   * @param type - the Type that this wildcard is being assigned to
   * @return true if this wildcard type can be assigned to the specified type, false otherwise
   */
  public boolean canBeAssignedTo(Type type)
  {
    if (type instanceof WildcardType || type instanceof NamedType || type instanceof NullType || type instanceof VoidType)
    {
      throw new IllegalArgumentException("Cannot check if " + type + " can assign a wildcard type via canBeAssignedTo()");
    }

    // find a set of all of our super types
    Set<TypeParameter> visitedTypeParameters = new HashSet<TypeParameter>();
    Set<WildcardType> visitedWildcardTypes = new HashSet<WildcardType>();
    Set<Type> ourSuperTypes = new HashSet<Type>();
    Deque<Type> typeQueue = new LinkedList<Type>();
    typeQueue.add(this);
    while (!typeQueue.isEmpty())
    {
      Type superType = typeQueue.poll();
      if (superType instanceof NamedType && ((NamedType) superType).getResolvedTypeParameter() != null)
      {
        TypeParameter typeParameter = ((NamedType) superType).getResolvedTypeParameter();
        if (visitedTypeParameters.contains(typeParameter))
        {
          // since type parameters can extend each other, we need to continue here to make sure we don't infinite loop
          // with a circular super-type restriction (e.g. Foo<A extends B, B extends A>)
          continue;
        }
        visitedTypeParameters.add(typeParameter);
        // we can ignore the TypeParameter's nullability and immutability, as the caller has already checked that those properties do not conflict
        for (Type parameterSuperType : typeParameter.getSuperTypes())
        {
          typeQueue.add(parameterSuperType);
        }
      }
      else if (superType instanceof WildcardType)
      {
        WildcardType wildcardType = (WildcardType) superType;
        if (visitedWildcardTypes.contains(wildcardType))
        {
          continue;
        }
        visitedWildcardTypes.add(wildcardType);
        // we can ignore the Wildcard type's nullability and immutability, as the caller has already checked that those properties do not conflict
        for (Type t : wildcardType.getSuperTypes())
        {
          typeQueue.add(t);
        }
      }
      else
      {
        ourSuperTypes.add(superType);
      }
    }
    if (ourSuperTypes.isEmpty())
    {
      // there is no upper bound for this type, so just use ?#object
      ourSuperTypes.add(new ObjectType(true, true, null));
    }

    for (Type superType : ourSuperTypes)
    {
      if (type.canAssign(superType))
      {
        // the other type can assign one of our super-types, which is all we need for the assignment to work
        return true;
      }
    }
    return false;
  }

  /**
   * Checks whether this wildcard is broader than the specified type.
   * If the type is a single type, then it checks whether it is contained by this wildcard.
   * If the type is another wildcard type, then it checks whether this wildcard encompasses all possible values of the other wildcard.
   * @param type - the type to check
   * @return true if this wildcard encompasses the specified type, false otherwise
   */
  public boolean encompasses(Type type)
  {
    for (Type superType : superTypes)
    {
      if (!superType.canAssign(type))
      {
        return false;
      }
    }
    for (Type subType : subTypes)
    {
      if (!type.canAssign(subType))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEquivalent(Type type)
  {
    if (!(type instanceof WildcardType))
    {
      return false;
    }
    WildcardType otherWildcardType = (WildcardType) type;
    if (isNullable() != otherWildcardType.isNullable() ||
        explicitlyImmutable != otherWildcardType.explicitlyImmutable ||
        contextuallyImmutable != otherWildcardType.contextuallyImmutable)
    {
      return false;
    }
    Type[] otherSuperTypes = otherWildcardType.superTypes;
    Type[] otherSubTypes = otherWildcardType.subTypes;
    if (superTypes.length != otherSuperTypes.length ||
        subTypes.length != otherSubTypes.length)
    {
      return false;
    }
    for (int i = 0; i < superTypes.length; ++i)
    {
      if (!superTypes[i].isEquivalent(otherSuperTypes[i]))
      {
        return false;
      }
    }
    for (int i = 0; i < subTypes.length; ++i)
    {
      if (!subTypes[i].isEquivalent(otherSubTypes[i]))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRuntimeEquivalent(Type type)
  {
    if (!(type instanceof WildcardType))
    {
      return false;
    }
    WildcardType otherWildcardType = (WildcardType) type;
    Type[] otherSuperTypes = otherWildcardType.superTypes;
    Type[] otherSubTypes = otherWildcardType.subTypes;
    if (superTypes.length != otherSuperTypes.length ||
        subTypes.length != otherSubTypes.length)
    {
      return false;
    }
    for (int i = 0; i < superTypes.length; ++i)
    {
      if (!superTypes[i].isRuntimeEquivalent(otherSuperTypes[i]))
      {
        return false;
      }
    }
    for (int i = 0; i < subTypes.length; ++i)
    {
      if (!subTypes[i].isRuntimeEquivalent(otherSubTypes[i]))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<MemberReference<?>> getMembers(String name)
  {
    // we need to find all of the types that this WildcardType extends, so first do a search for them
    List<Type> allSuperTypes = new LinkedList<Type>();

    // wildcard types shouldn't really ever have circular references, but we process them in the
    // same way as we process type parameters in NamedType, so that we don't have to rely on this
    // assumption - and if it turns out to be wrong we won't get infinite recursion
    Set<TypeParameter> visitedParameters = new HashSet<TypeParameter>();
    Set<WildcardType> visitedWildcardTypes = new HashSet<WildcardType>();
    Deque<Type> typeQueue = new LinkedList<Type>();
    typeQueue.add(this);
    while (!typeQueue.isEmpty())
    {
      Type superType = typeQueue.poll();
      if (superType instanceof NamedType && ((NamedType) superType).getResolvedTypeParameter() != null)
      {
        TypeParameter typeParameter = ((NamedType) superType).getResolvedTypeParameter();
        if (visitedParameters.contains(typeParameter))
        {
          continue;
        }
        for (Type t : ((NamedType) superType).getResolvedTypeParameter().getSuperTypes())
        {
          typeQueue.add(t);
        }
        visitedParameters.add(typeParameter);
      }
      else if (superType instanceof WildcardType)
      {
        WildcardType wildcardType = (WildcardType) superType;
        if (visitedWildcardTypes.contains(wildcardType))
        {
          continue;
        }
        visitedWildcardTypes.add(wildcardType);
        for (Type t : wildcardType.getSuperTypes())
        {
          typeQueue.add(t);
        }
      }
      else
      {
        // only add super-types which are not generic type parameters or wildcard types
        allSuperTypes.add(superType);
      }
    }
    if (allSuperTypes.isEmpty())
    {
      // this type parameter has no explicit super-types, so add the default object super-type to get the omnipresent members from
      allSuperTypes.add(new ObjectType(true, true, null));
    }


    // store the disambiguators as well as the members, so that we can keep track of
    // whether we have added e.g. a function with type {uint -> void}
    Map<String, MemberReference<?>> matches = new HashMap<String, MemberReference<?>>();

    for (Type superType : allSuperTypes)
    {
      Set<MemberReference<?>> superReferences = superType.getMembers(name);
      for (MemberReference<?> superReference : superReferences)
      {
        Member member = superReference.getReferencedMember();
        if (member instanceof ArrayLengthMember)
        {
          if (!matches.containsKey("F" + ArrayLengthMember.LENGTH_FIELD_NAME))
          {
            matches.put("F" + ArrayLengthMember.LENGTH_FIELD_NAME, superReference);
          }
        }
        else if (member instanceof Field)
        {
          Field field = (Field) member;
          if (!field.isStatic() && !matches.containsKey("F" + field.getName()))
          {
            matches.put("F" + field.getName(), superReference);
          }
        }
        else if (member instanceof Property)
        {
          Property property = (Property) member;
          if (!property.isStatic() && !matches.containsKey("F" + property.getName()))
          {
            matches.put("F" + property.getName(), superReference);
          }
        }
        else if (member instanceof Method)
        {
          Method method = (Method) member;
          if (!method.isStatic())
          {
            String disambiguator = "M" + ((MethodReference) superReference).getDisambiguator().toString();
            MemberReference<?> old = matches.get(disambiguator);
            if (old == null)
            {
              matches.put(disambiguator, superReference);
            }
          }
        }
      }
    }
    return new HashSet<MemberReference<?>>(matches.values());
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
