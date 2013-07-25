package eu.bryants.anthony.plinth.ast.type;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.ArrayLengthMember;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.metadata.FieldReference;
import eu.bryants.anthony.plinth.ast.metadata.GenericTypeSpecialiser;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;
import eu.bryants.anthony.plinth.ast.metadata.PropertyReference;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class NamedType extends Type
{

  private QName qname;
  private Type[] typeArguments;

  // a type is explicitly immutable if it has been declared as immutable explicitly,
  // whereas a type is contextually immutable if it is just accessed in an immutable context
  // if a type is explicitly immutable, then it is always also contextually immutable
  private boolean explicitlyImmutable;
  private boolean contextuallyImmutable;

  private TypeDefinition resolvedTypeDefinition;
  private TypeParameter resolvedTypeParameter;

  public NamedType(boolean nullable, boolean explicitlyImmutable, boolean contextuallyImmutable, QName qname, Type[] typeArguments, LexicalPhrase lexicalPhrase)
  {
    super(nullable, lexicalPhrase);
    this.explicitlyImmutable = explicitlyImmutable;
    this.contextuallyImmutable = explicitlyImmutable | contextuallyImmutable;
    this.qname = qname;
    this.typeArguments = typeArguments;
  }

  public NamedType(boolean nullable, boolean explicitlyImmutable, boolean contextuallyImmutable, TypeDefinition typeDefinition, Type[] typeArguments)
  {
    this(nullable, explicitlyImmutable, contextuallyImmutable, typeDefinition.getQualifiedName(), typeArguments, null);
    this.resolvedTypeDefinition = typeDefinition;
  }

  /**
   * Creates a new NamedType with the type arguments filled in with references to the type parameters from the TypeDefinition.
   * @param nullable - whether the type should be nullable
   * @param explicitlyImmutable - whether the type should be explicitly immutable
   * @param contextuallyImmutable - whether the type should be contextually immutable
   * @param typeDefinition - the TypeDefinition to store a reference to
   */
  public NamedType(boolean nullable, boolean explicitlyImmutable, boolean contextuallyImmutable, TypeDefinition typeDefinition)
  {
    this(nullable, explicitlyImmutable, contextuallyImmutable, typeDefinition, null);
    TypeParameter[] typeParameters = typeDefinition.getTypeParameters();
    Type[] typeArguments = null;
    if (typeParameters.length > 0)
    {
      typeArguments = new Type[typeParameters.length];
      for (int i = 0; i < typeParameters.length; ++i)
      {
        typeArguments[i] = new NamedType(false, false, false, typeParameters[i]);
      }
    }
    this.typeArguments = typeArguments;
  }

  public NamedType(boolean nullable, boolean explicitlyImmutable, boolean contextuallyImmutable, TypeParameter typeParameter)
  {
    this(nullable, explicitlyImmutable, contextuallyImmutable, new QName(typeParameter.getName(), null), null, null);
    this.resolvedTypeParameter = typeParameter;
  }

  public NamedType(boolean nullable, boolean explicitlyImmutable, QName qname, Type[] typeArguments, LexicalPhrase lexicalPhrase)
  {
    this(nullable, explicitlyImmutable, false, qname, typeArguments, lexicalPhrase);
  }

  public NamedType(boolean nullable, boolean explicitlyImmutable, TypeDefinition typeDefinition, Type[] typeArguments)
  {
    this(nullable, explicitlyImmutable, false, typeDefinition, typeArguments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canBeNullable()
  {
    if (resolvedTypeDefinition == null && resolvedTypeParameter == null)
    {
      throw new IllegalStateException("Cannot check the nullability of a type before it has been resolved");
    }
    if (isNullable())
    {
      return true;
    }
    if (resolvedTypeParameter != null)
    {
      // check if this type parameter can be nullable
      boolean hasNotNullSuperType = false;
      // since type parameters can extend each other, we need to use a queue and make sure not to infinite loop
      // with a circular super-type restriction (e.g. Foo<A extends B, B extends A> - A and B should both be possibly-nullable here)
      Set<TypeParameter> visitedParameters = new HashSet<TypeParameter>();
      Set<WildcardType> visitedWildcards = new HashSet<WildcardType>();
      Deque<Type> typeQueue = new LinkedList<Type>();
      for (Type superType : resolvedTypeParameter.getSuperTypes())
      {
        typeQueue.add(superType);
      }
      visitedParameters.add(resolvedTypeParameter);
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
          // wildcard types shouldn't really ever have circular references, but we process them as
          // if they do rather than recursing with another canBeNullable() call
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
    return false;
  }

  /**
   * Note: this method should not be called until this type has been resolved
   * @return true if this type is explicitly immutable, false otherwise
   */
  public boolean isExplicitlyImmutable()
  {
    if (resolvedTypeDefinition == null && resolvedTypeParameter == null)
    {
      throw new IllegalStateException("Cannot check the immutability of a type before it has been resolved");
    }
    return explicitlyImmutable ||
           (resolvedTypeDefinition != null && resolvedTypeDefinition.isImmutable());
  }

  /**
   * Note: this method should not be called until this type has been resolved
   * @return true if this type is contextually immutable, false otherwise
   */
  public boolean isContextuallyImmutable()
  {
    if (resolvedTypeDefinition == null && resolvedTypeParameter == null)
    {
      throw new IllegalStateException("Cannot check the immutability of a type before it has been resolved");
    }
    // explicitlyImmutable implies contextuallyImmutable, so we do not need to check explicitlyImmutable
    return contextuallyImmutable ||
           (resolvedTypeDefinition != null && resolvedTypeDefinition.isImmutable());
  }

  /**
   * Checks whether this type is either explicitly immutable, or refers to a type parameter which might be explicitly immutable.
   * Note: this method should not be called until this type has been resolved
   * @return true if this type can be explicitly immutable, false otherwise
   */
  public boolean canBeExplicitlyImmutable()
  {
    if (resolvedTypeDefinition == null && resolvedTypeParameter == null)
    {
      throw new IllegalStateException("Cannot check the immutability of a type before it has been resolved");
    }
    if (isExplicitlyImmutable())
    {
      return true;
    }
    if (resolvedTypeParameter != null)
    {
      // check if this type parameter can be nullable
      boolean hasNotImmutableSuperType = false;
      // since type parameters can extend each other, we need to use a queue and make sure not to infinite loop
      // with a circular super-type restriction (e.g. Foo<A extends B, B extends A> - A and B should both be possibly-immutable here)
      Set<TypeParameter> visitedParameters = new HashSet<TypeParameter>();
      Set<WildcardType> visitedWildcards = new HashSet<WildcardType>();
      Deque<Type> typeQueue = new LinkedList<Type>();
      for (Type superType : resolvedTypeParameter.getSuperTypes())
      {
        typeQueue.add(superType);
      }
      visitedParameters.add(resolvedTypeParameter);
      while (!typeQueue.isEmpty())
      {
        Type superType = typeQueue.poll();
        if ((superType instanceof ArrayType  && !((ArrayType)  superType).isExplicitlyImmutable()) ||
            (superType instanceof NamedType  && !((NamedType)  superType).isExplicitlyImmutable()) ||
            (superType instanceof ObjectType && !((ObjectType) superType).isExplicitlyImmutable()) ||
            (superType instanceof WildcardType && !(((WildcardType) superType).isExplicitlyImmutable())))
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
    return false;
  }

  /**
   * @return the qualified name
   */
  public QName getQualifiedName()
  {
    return qname;
  }

  /**
   * @return the typeArguments
   */
  public Type[] getTypeArguments()
  {
    return typeArguments;
  }

  /**
   * @return the resolved TypeDefinition
   */
  public TypeDefinition getResolvedTypeDefinition()
  {
    return resolvedTypeDefinition;
  }

  /**
   * @param resolvedTypeDefinition - the resolved TypeDefinition to set
   */
  public void setResolvedTypeDefinition(TypeDefinition resolvedTypeDefinition)
  {
    this.resolvedTypeDefinition = resolvedTypeDefinition;
  }

  /**
   * @return the resolvedTypeParameter
   */
  public TypeParameter getResolvedTypeParameter()
  {
    return resolvedTypeParameter;
  }

  /**
   * @param resolvedTypeParameter - the resolved TypeParameter to set
   */
  public void setResolvedTypeParameter(TypeParameter resolvedTypeParameter)
  {
    this.resolvedTypeParameter = resolvedTypeParameter;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canAssign(Type type)
  {
    if (type instanceof NullType && isNullable())
    {
      // all nullable types can have null assigned to them
      return true;
    }
    if (resolvedTypeDefinition == null && resolvedTypeParameter == null)
    {
      throw new IllegalStateException("Cannot check whether two types are assign-compatible before they are resolved");
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
    // an explicitly-immutable type cannot be assigned to a non-explicitly-immutable named type
    if (!isExplicitlyImmutable() && ((type instanceof NamedType    && ((NamedType)    type).isExplicitlyImmutable()) ||
                                     (type instanceof ArrayType    && ((ArrayType)    type).isExplicitlyImmutable()) ||
                                     (type instanceof ObjectType   && ((ObjectType)   type).isExplicitlyImmutable()) ||
                                     (type instanceof WildcardType && ((WildcardType) type).isExplicitlyImmutable())))
    {
      return false;
    }
    // a contextually-immutable type cannot be assigned to a non-immutable named type
    if (!isContextuallyImmutable() && ((type instanceof NamedType    && ((NamedType)    type).isContextuallyImmutable()) ||
                                       (type instanceof ArrayType    && ((ArrayType)    type).isContextuallyImmutable()) ||
                                       (type instanceof ObjectType   && ((ObjectType)   type).isContextuallyImmutable()) ||
                                       (type instanceof WildcardType && ((WildcardType) type).isContextuallyImmutable())))
    {
      return false;
    }
    // a possibly-immutable type cannot be assigned to a not-possibly-immutable named type
    if (!canBeExplicitlyImmutable() && ((type instanceof NamedType    && ((NamedType)    type).canBeExplicitlyImmutable()) ||
                                        (type instanceof WildcardType && ((WildcardType) type).canBeExplicitlyImmutable())))
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
      // there is no upper bound for this type (meaning it is probably a type parameter), so just use ?#object
      otherSuperTypes.add(new ObjectType(true, true, null));
    }

    if (resolvedTypeDefinition != null)
    {
      for (Type checkType : otherSuperTypes)
      {
        // checkType can never be a TypeParameter, so if it is a NamedType, check its inheritance hierarchy
        if (checkType instanceof NamedType)
        {
          TypeDefinition checkTypeDefinition = ((NamedType) checkType).getResolvedTypeDefinition();
          GenericTypeSpecialiser checkTypeSpecialiser = new GenericTypeSpecialiser((NamedType) checkType);
          for (NamedType checkSuperType : checkTypeDefinition.getInheritanceLinearisation())
          {
            // check that checkSuperType matches this type
            if (checkSuperType.getResolvedTypeDefinition() != resolvedTypeDefinition)
            {
              continue;
            }
            // check that the type arguments match
            // (note: wildcard types mean that we cannot just check for equivalence)
            NamedType specialisedType = (NamedType) checkTypeSpecialiser.getSpecialisedType(checkSuperType);
            Type[] specialisedArguments = specialisedType.getTypeArguments();
            if ((typeArguments == null) != (specialisedArguments == null))
            {
              continue;
            }
            if (typeArguments == null)
            {
              // checkSuperType matches this NamedType, as there are no type arguments to check
              return true;
            }
            if (typeArguments.length != specialisedArguments.length)
            {
              throw new IllegalStateException("Number of type arguments does not match for " + resolvedTypeDefinition.getQualifiedName());
            }
            boolean argumentsMatch = true;
            for (int i = 0; argumentsMatch && i < typeArguments.length; ++i)
            {
              if (typeArguments[i] instanceof WildcardType)
              {
                argumentsMatch = ((WildcardType) typeArguments[i]).encompasses(specialisedArguments[i]);
              }
              else
              {
                argumentsMatch = typeArguments[i].isEquivalent(specialisedArguments[i]);
              }
            }
            if (argumentsMatch)
            {
              return true;
            }
          }
        }
      }
      // we did not find any compatible NamedTypes in the super-types of the other type
      return false;
    }

    // we are a TypeParameter
    // first, check whether the other type extends us
    if (otherTypeParameters.contains(resolvedTypeParameter))
    {
      return true;
    }

    // the other type is not a TypeParameter which extends us (or is us), so check whether it can be assigned to any of our sub-types

    // find a set of all of our sub-types
    Set<TypeParameter> visitedParameters = new HashSet<TypeParameter>();
    Set<WildcardType> visitedWildcardSubTypes = new HashSet<WildcardType>();
    Set<Type> thisSubTypes = new HashSet<Type>();
    Deque<Type> thisSubTypeQueue = new LinkedList<Type>();
    for (Type subType : resolvedTypeParameter.getSubTypes())
    {
      thisSubTypeQueue.add(subType);
    }
    visitedParameters.add(resolvedTypeParameter);
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
          // (this is because every one of our sub-types necessarily extends any actual type that the type parameter might end up being)
          return true;
        }
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEquivalent(Type type)
  {
    if (resolvedTypeParameter != null)
    {
      return type instanceof NamedType &&
             isNullable() == type.isNullable() &&
             resolvedTypeParameter == ((NamedType) type).getResolvedTypeParameter() &&
             isExplicitlyImmutable() == ((NamedType) type).isExplicitlyImmutable() &&
             isContextuallyImmutable() == ((NamedType) type).isContextuallyImmutable();
    }
    if (resolvedTypeDefinition != null)
    {
      if (!(type instanceof NamedType) ||
          isNullable() != type.isNullable() ||
          !resolvedTypeDefinition.equals(((NamedType) type).getResolvedTypeDefinition()) ||
          (typeArguments == null) != (((NamedType) type).getTypeArguments() == null) ||
          (!resolvedTypeDefinition.isImmutable() &&
              (isExplicitlyImmutable() != ((NamedType) type).isExplicitlyImmutable() ||
               isContextuallyImmutable() != ((NamedType) type).isContextuallyImmutable())))
      {
        return false;
      }
      if (typeArguments != null)
      {
        Type[] otherTypeArguments = ((NamedType) type).getTypeArguments();
        if (typeArguments.length != otherTypeArguments.length)
        {
          return false;
        }
        for (int i = 0; i < typeArguments.length; ++i)
        {
          if (!typeArguments[i].isEquivalent(otherTypeArguments[i]))
          {
            return false;
          }
        }
      }
      return true;
    }
    throw new IllegalStateException("Cannot check for type equivalence before the named type is resolved");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRuntimeEquivalent(Type type)
  {
    if (resolvedTypeParameter != null)
    {
      return type instanceof NamedType &&
             isNullable() == type.isNullable() &&
             resolvedTypeParameter == ((NamedType) type).getResolvedTypeParameter() &&
             isExplicitlyImmutable() == ((NamedType) type).isExplicitlyImmutable() &&
             isContextuallyImmutable() == ((NamedType) type).isContextuallyImmutable();
    }
    if (resolvedTypeDefinition != null)
    {
      if (!(type instanceof NamedType) ||
          isNullable() != type.isNullable() ||
          !resolvedTypeDefinition.equals(((NamedType) type).getResolvedTypeDefinition()) ||
          (typeArguments == null) != (((NamedType) type).getTypeArguments() == null) ||
          (!resolvedTypeDefinition.isImmutable() &&
              (isExplicitlyImmutable() != ((NamedType) type).isExplicitlyImmutable() ||
               isContextuallyImmutable() != ((NamedType) type).isContextuallyImmutable())))
      {
        return false;
      }
      if (typeArguments != null)
      {
        Type[] otherTypeArguments = ((NamedType) type).getTypeArguments();
        if (typeArguments.length != otherTypeArguments.length)
        {
          return false;
        }
        for (int i = 0; i < typeArguments.length; ++i)
        {
          if (!typeArguments[i].isRuntimeEquivalent(otherTypeArguments[i]))
          {
            return false;
          }
        }
      }
      return true;
    }
    throw new IllegalStateException("Cannot check for type equivalence before the named type is resolved");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<MemberReference<?>> getMembers(String name)
  {
    return getMembers(name, false);
  }

  /**
   * Finds all members of this type with the specified name, optionally including inherited static fields.
   * @param name - the name to search for
   * @param inheritStaticMembers - true if inherited static members should be included, false otherwise
   * @return the set of members that are part of this type
   */
  public Set<MemberReference<?>> getMembers(String name, boolean inheritStaticMembers)
  {
    return getMembers(name, inheritStaticMembers, false);
  }

  /**
   * Finds all members of this type with the specified name, optionally including inherited static fields.
   * @param name - the name to search for
   * @param inheritStaticMembers - true if inherited static members should be included, false otherwise
   * @param ignoreCurrentType - true if the current type should be ignored in favour of super-types, this is used to implement "super." expressions
   * @return the set of members that are part of this type
   */
  public Set<MemberReference<?>> getMembers(String name, boolean inheritStaticMembers, boolean ignoreCurrentType)
  {
    if (resolvedTypeDefinition == null && resolvedTypeParameter == null)
    {
      throw new IllegalStateException("Cannot get the members of a NamedType before it is resolved");
    }

    // store the disambiguators as well as the members, so that we can keep track of
    // whether we have added e.g. a function with type {uint -> void}
    Map<String, MemberReference<?>> matches = new HashMap<String, MemberReference<?>>();

    if (resolvedTypeDefinition == null)
    {
      // we need to find all of the types that this TypeParameter extends, so first do a search for them
      List<Type> superTypes = new LinkedList<Type>();

      // since type parameters can extend each other, we need to use a queue and make sure not to infinite loop
      // with a circular super-type restriction (e.g. Foo<A extends B, B extends A> - A and B should both be possibly-nullable here)
      Set<TypeParameter> visitedParameters = new HashSet<TypeParameter>();
      Set<WildcardType> visitedWildcardTypes = new HashSet<WildcardType>();
      Deque<Type> typeQueue = new LinkedList<Type>();
      for (Type superType : resolvedTypeParameter.getSuperTypes())
      {
        typeQueue.add(superType);
      }
      visitedParameters.add(resolvedTypeParameter);
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
          superTypes.add(superType);
        }
      }
      if (superTypes.isEmpty())
      {
        // this type parameter has no explicit super-types, so add the default object super-type to get the omnipresent members from
        superTypes.add(new ObjectType(true, true, null));
      }
      for (Type superType : superTypes)
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
            if ((!field.isStatic() || inheritStaticMembers) && !matches.containsKey("F" + field.getName()))
            {
              matches.put("F" + field.getName(), superReference);
            }
          }
          else if (member instanceof Property)
          {
            Property property = (Property) member;
            if ((!property.isStatic() || inheritStaticMembers) && !matches.containsKey("F" + property.getName()))
            {
              matches.put("F" + property.getName(), superReference);
            }
          }
          else if (member instanceof Method)
          {
            Method method = (Method) member;
            if (!method.isStatic() || inheritStaticMembers)
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

    List<NamedType> searchList = new LinkedList<NamedType>();
    GenericTypeSpecialiser genericTypeSpecialiser = typeArguments == null ? GenericTypeSpecialiser.IDENTITY_SPECIALISER : new GenericTypeSpecialiser(this);

    for (NamedType searchType : resolvedTypeDefinition.getInheritanceLinearisation())
    {
      // specialise the type to use any arguments that this NamedType provides
      // (since searchType is part of the resolved TypeDefinition's linearisation, it can contain references to TypeParameters from the resolved TypeDefinition)
      // we can cast to NamedType because every type in a linearisation is resolved to a TypeDefinition, not a TypeParameter
      searchList.add((NamedType) genericTypeSpecialiser.getSpecialisedType(searchType));
    }

    // try to add members from the inherited classes in order, but be careful not to add
    // members which cannot be disambiguated from those we have already added
    for (final NamedType currentSuperType : searchList)
    {
      final TypeDefinition currentTypeDefinition = currentSuperType.getResolvedTypeDefinition();
      if (ignoreCurrentType & currentTypeDefinition == resolvedTypeDefinition)
      {
        continue;
      }

      // we will use this GenericTypeSpecialiser to specialise any members of the current super-type to use the type arguments from the linearisation rather than their original type parameters
      GenericTypeSpecialiser superTypeGenericTypeSpecialiser = new GenericTypeSpecialiser(currentSuperType);

      Field currentField = currentTypeDefinition.getField(name);
      // exclude static fields from being inherited
      if (currentField != null && (!currentField.isStatic() || inheritStaticMembers || currentTypeDefinition == resolvedTypeDefinition) && !matches.containsKey("F" + currentField.getName()))
      {
        matches.put("F" + currentField.getName(), new FieldReference(currentField, superTypeGenericTypeSpecialiser));
      }
      // exclude static properties from being inherited
      // also, treat properties like fields, so that properties can hide fields and vice versa
      Property currentProperty = currentTypeDefinition.getProperty(name);
      if (currentProperty != null && (!currentProperty.isStatic() || inheritStaticMembers || currentTypeDefinition == resolvedTypeDefinition) && !matches.containsKey("F" + currentProperty.getName()))
      {
        matches.put("F" + currentProperty.getName(), new PropertyReference(currentProperty, superTypeGenericTypeSpecialiser));
      }
      Set<Method> currentMethodSet = currentTypeDefinition.getMethodsByName(name);
      if (currentMethodSet != null)
      {
        for (Method method : currentMethodSet)
        {
          if (!method.isStatic() || inheritStaticMembers || currentTypeDefinition == resolvedTypeDefinition)
          {
            MethodReference methodReference = new MethodReference(method, superTypeGenericTypeSpecialiser);
            String disambiguator = "M" + methodReference.getDisambiguator().toString();
            MemberReference<?> old = matches.get(disambiguator);
            if (old == null)
            {
              matches.put(disambiguator, methodReference);
            }
            else
            {
              MethodReference oldMethodReference = (MethodReference) old;
              Method oldMethod = oldMethodReference.getReferencedMember();
              // there is already another method with this disambiguator
              // if it is from a different class, then it was earlier in the resolution list, so don't even try to overwrite it
              if (oldMethod.getContainingTypeDefinition() != currentTypeDefinition)
              {
                continue;
              }
              // if either of these methods is non-static, then we must be processing the same Method for a second time,
              // (since the Resolver prohibits duplicate non-static methods in a single type)
              // (although, they might be different methods with the same signature if we are inheriting from the same generic class twice with different type arguments)
              if ((!method.isStatic() || !oldMethod.isStatic()) && method != oldMethod)
              {
                throw new IllegalStateException("Duplicate non-static methods can not exist in a class");
              }
              if (method.isStatic() && oldMethod.isStatic())
              {
                // find which of these static methods has the greater since specifier, and use it
                SinceSpecifier oldSince = oldMethod.getSinceSpecifier();
                SinceSpecifier newSince = method.getSinceSpecifier();
                MethodReference newerReference = oldSince == null ? methodReference :
                                                 oldSince.compareTo(newSince) < 0 ? methodReference : oldMethodReference;
                matches.put(disambiguator, newerReference);
              }
            }
          }
        }
      }
    }
    for (BuiltinMethod builtin : ObjectType.OBJECT_METHODS)
    {
      if (!builtin.getName().equals(name))
      {
        continue;
      }
      if (!builtin.isStatic() || inheritStaticMembers)
      {
        MethodReference builtinReference = new MethodReference(builtin, GenericTypeSpecialiser.IDENTITY_SPECIALISER);
        String disambiguator = "M" + builtinReference.getDisambiguator().toString();
        MemberReference<?> old = matches.get(disambiguator);
        if (old == null)
        {
          matches.put(disambiguator, builtinReference);
        }
      }
    }
    return new HashSet<MemberReference<?>>(matches.values());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    StringBuffer buffer = new StringBuffer();
    if (isNullable())
    {
      buffer.append('x');
    }
    if (isContextuallyImmutable())
    {
      buffer.append('c');
    }
    if (resolvedTypeParameter != null)
    {
      buffer.append('P');
      buffer.append(resolvedTypeParameter.getContainingTypeDefinition().getQualifiedName().getMangledName());
      buffer.append(resolvedTypeParameter.getName().getBytes().length);
      buffer.append(resolvedTypeParameter.getName());
    }
    else if (resolvedTypeDefinition != null)
    {
      if (resolvedTypeDefinition instanceof ClassDefinition)
      {
        buffer.append('C');
      }
      else if (resolvedTypeDefinition instanceof CompoundDefinition)
      {
        buffer.append('V');
      }
      else if (resolvedTypeDefinition instanceof InterfaceDefinition)
      {
        buffer.append('N');
      }
      else
      {
        throw new IllegalStateException("Unknown type of TypeDefinition");
      }
      buffer.append(resolvedTypeDefinition.getQualifiedName().getMangledName());
    }
    else
    {
      throw new IllegalStateException("Cannot get a mangled name before the NamedType is resolved");
    }
    if (typeArguments != null)
    {
      buffer.append('R');
      for (int i = 0; i < typeArguments.length; ++i)
      {
        buffer.append(typeArguments[i].getMangledName());
      }
    }
    buffer.append('E');
    return buffer.toString();
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
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    if (isNullable())
    {
      buffer.append('?');
    }
    if (contextuallyImmutable)
    {
      buffer.append('#');
    }
    buffer.append(qname);
    if (typeArguments != null)
    {
      buffer.append('<');
      for (int i = 0; i < typeArguments.length; ++i)
      {
        buffer.append(typeArguments[i]);
        if (i != typeArguments.length - 1)
        {
          buffer.append(", ");
        }
      }
      buffer.append('>');
    }
    return buffer.toString();
  }
}
