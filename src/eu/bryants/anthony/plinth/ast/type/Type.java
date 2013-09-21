package eu.bryants.anthony.plinth.ast.type;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference.Disambiguator;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class Type
{
  private LexicalPhrase lexicalPhrase;

  private boolean nullable;

  /**
   * Creates a new Type with the specified nullability and LexicalPhrase
   * @param nullable - true if this Type should be nullable, false otherwise
   * @param lexicalPhrase - the LexicalPhrase of this Type
   */
  public Type(boolean nullable, LexicalPhrase lexicalPhrase)
  {
    this.lexicalPhrase = lexicalPhrase;
    this.nullable = nullable;
  }

  /**
   * @return true if this Type is nullable, false otherwise
   */
  public boolean isNullable()
  {
    return nullable;
  }

  /**
   * Checks whether this Type might be nullable. This is different from isNullable() in that it will return true for a possibly-nullable type parameter.
   * @return true if this Type might be nullable, false otherwise
   */
  public boolean canBeNullable()
  {
    return nullable;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

  /**
   * Checks whether the specified type can be assigned to a variable of this type.
   * @param type - the type to check
   * @return true iff the specified type can be assigned to a variable of this type
   */
  public abstract boolean canAssign(Type type);

  /**
   * Checks whether this type is absolutely equivalent to the specified type.
   * @param type - the type to check
   * @return true iff this type and the specified type are equivalent
   */
  public abstract boolean isEquivalent(Type type);

  /**
   * Checks whether this type is equivalent to the specified type at runtime.
   * Having this as a separate concept from absolute equivalence allows us to permit type erasure for certain things,
   * such as checked exceptions on function types.
   * @param type - the type to check equivalence with
   * @return true iff this type and the specified type are runtime-equivalent
   */
  public abstract boolean isRuntimeEquivalent(Type type);

  /**
   * Returns a set of MemberReferences to the Members of this type with the specified name
   * @param name - the name of the Members to get
   * @return the Members with the specified name, or the empty set if none exist
   */
  public abstract Set<MemberReference<?>> getMembers(String name);

  /**
   * Finds the MethodReference in this type with the specified Disambiguator.
   * @param disambiguator - the Disambiguator to search for
   * @return the MethodReference found, or null if no method exists with the specified signature
   */
  public MethodReference getMethod(Disambiguator disambiguator)
  {
    for (MemberReference<?> member : getMembers(disambiguator.getName()))
    {
      if (member instanceof MethodReference && ((MethodReference) member).getDisambiguator().matches(disambiguator))
      {
        return (MethodReference) member;
      }
    }
    return null;
  }

  /**
   * @return the mangled name of this type
   */
  public abstract String getMangledName();

  /**
   * @return true if this type has a default null value (i.e. 0 for uint, null for ?[]Foo, etc.)
   */
  public abstract boolean hasDefaultValue();

  /**
   * Finds whether the specified type is explicitly immutable in terms of data. i.e. the data inside it cannot be modified.
   * @param type - the type to check
   * @return true if the specified type is explicitly immutable in terms of data, false if the type is not or cannot be immutable
   */
  public static boolean isExplicitlyDataImmutable(Type type)
  {
    if (type instanceof ArrayType)
    {
      return ((ArrayType) type).isExplicitlyImmutable();
    }
    if (type instanceof NamedType)
    {
      return ((NamedType) type).isExplicitlyImmutable();
    }
    if (type instanceof ObjectType)
    {
      return ((ObjectType) type).isExplicitlyImmutable();
    }
    if (type instanceof WildcardType)
    {
      return ((WildcardType) type).isExplicitlyImmutable();
    }
    if (type instanceof FunctionType || type instanceof NullType || type instanceof PrimitiveType || type instanceof TupleType)
    {
      return false;
    }
    throw new IllegalArgumentException("Cannot find the data explicit-immutability of an unknown type: " + type);
  }

  /**
   * Finds whether the specified type can be explicitly immutable in terms of data. i.e. it is possible that the data inside it cannot be modified.
   * @param type - the type to check
   * @return true if the specified type can be explicitly immutable in terms of data, false if the type cannot be immutable
   */
  public static boolean canBeExplicitlyDataImmutable(Type type)
  {
    if (isExplicitlyDataImmutable(type))
    {
      return true;
    }
    if (type instanceof NamedType)
    {
      return ((NamedType) type).canBeExplicitlyImmutable();
    }
    if (type instanceof WildcardType)
    {
      return ((WildcardType) type).canBeExplicitlyImmutable();
    }
    return false;
  }

  /**
   * Finds whether the specified type is contextually immutable in terms of data. i.e. the data inside it cannot be modified.
   * @param type - the type to check
   * @return true if the specified type is contextually immutable in terms of data, false if the type is not or cannot be immutable
   */
  public static boolean isContextuallyDataImmutable(Type type)
  {
    if (type instanceof ArrayType)
    {
      return ((ArrayType) type).isContextuallyImmutable();
    }
    if (type instanceof NamedType)
    {
      return ((NamedType) type).isContextuallyImmutable();
    }
    if (type instanceof ObjectType)
    {
      return ((ObjectType) type).isContextuallyImmutable();
    }
    if (type instanceof WildcardType)
    {
      return ((WildcardType) type).isContextuallyImmutable();
    }
    if (type instanceof FunctionType || type instanceof NullType || type instanceof PrimitiveType || type instanceof TupleType)
    {
      return false;
    }
    throw new IllegalArgumentException("Cannot find the data contextual-immutability of an unknown type: " + type);
  }

  /**
   * Finds the equivalent of the specified type with the specified nullability.
   * @param type - the type to find the version of which has the specified nullability
   * @param nullable - true if the returned type should be nullable, false otherwise
   * @return the version of the specified type with the specified nullability, or the original type if it already has the requested nullability
   */
  public static Type findTypeWithNullability(Type type, boolean nullable)
  {
    if (type.isNullable() == nullable)
    {
      return type;
    }
    if (type instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) type;
      return new ArrayType(nullable, arrayType.isExplicitlyImmutable(), arrayType.isContextuallyImmutable(), arrayType.getBaseType(), null);
    }
    if (type instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) type;
      return new FunctionType(nullable, functionType.isImmutable(), functionType.getReturnType(), functionType.getParameterTypes(), functionType.getDefaultParameters(), functionType.getThrownTypes(), null);
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      if (namedType.getResolvedTypeParameter() != null)
      {
        return new NamedType(nullable, namedType.isExplicitlyImmutable(), namedType.isContextuallyImmutable(), namedType.getResolvedTypeParameter());
      }
      return new NamedType(nullable, namedType.isExplicitlyImmutable(), namedType.isContextuallyImmutable(), namedType.getResolvedTypeDefinition(), namedType.getTypeArguments());
    }
    if (type instanceof ObjectType)
    {
      ObjectType objectType = (ObjectType) type;
      return new ObjectType(nullable, objectType.isExplicitlyImmutable(), objectType.isContextuallyImmutable(), null);
    }
    if (type instanceof PrimitiveType)
    {
      return new PrimitiveType(nullable, ((PrimitiveType) type).getPrimitiveTypeType(), null);
    }
    if (type instanceof TupleType)
    {
      return new TupleType(nullable, ((TupleType) type).getSubTypes(), null);
    }
    if (type instanceof WildcardType)
    {
      WildcardType wildcardType = (WildcardType) type;
      return new WildcardType(nullable, wildcardType.isExplicitlyImmutable(), wildcardType.isContextuallyImmutable(), wildcardType.getSuperTypes(), wildcardType.getSubTypes(), null);
    }
    throw new IllegalArgumentException("Cannot find the " + (nullable ? "nullable" : "non-nullable") + " version of: " + type);
  }

  /**
   * Finds the equivalent of the specified type with the specified explicit and contextual data-immutability.
   * If the specified type does not have a concept of either explicit or contextual data-immutability, then that type of immutability is not checked in the calculation.
   * @param type - the type to find the version of which has the specified explicit immutability
   * @param explicitlyImmutable - true if the returned type should be explicitly data-immutable, false otherwise
   * @param contextuallyImmutable - true if the returned type should be contextually data-immutable, false otherwise
   * @return the version of the specified type with the specified explicit and contextual data-immutability, or the original type if it already has the requested data-immutability
   */
  public static Type findTypeWithDataImmutability(Type type, boolean explicitlyImmutable, boolean contextuallyImmutable)
  {
    if (type instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) type;
      if (arrayType.isExplicitlyImmutable() == explicitlyImmutable && arrayType.isContextuallyImmutable() == contextuallyImmutable)
      {
        return arrayType;
      }
      return new ArrayType(arrayType.isNullable(), explicitlyImmutable, contextuallyImmutable, arrayType.getBaseType(), null);
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      if (namedType.isExplicitlyImmutable() == explicitlyImmutable && namedType.isContextuallyImmutable() == contextuallyImmutable)
      {
        return namedType;
      }
      if (namedType.getResolvedTypeParameter() != null)
      {
        return new NamedType(namedType.isNullable(), explicitlyImmutable, contextuallyImmutable, namedType.getResolvedTypeParameter());
      }
      return new NamedType(namedType.isNullable(), explicitlyImmutable, contextuallyImmutable, namedType.getResolvedTypeDefinition(), namedType.getTypeArguments());
    }
    if (type instanceof ObjectType)
    {
      ObjectType objectType = (ObjectType) type;
      if (objectType.isExplicitlyImmutable() == explicitlyImmutable && objectType.isContextuallyImmutable() == contextuallyImmutable)
      {
        return objectType;
      }
      return new ObjectType(objectType.isNullable(), explicitlyImmutable, contextuallyImmutable, null);
    }
    if (type instanceof WildcardType)
    {
      WildcardType wildcardType = (WildcardType) type;
      if (wildcardType.isExplicitlyImmutable() == explicitlyImmutable && wildcardType.isContextuallyImmutable() == contextuallyImmutable)
      {
        return wildcardType;
      }
      return new WildcardType(wildcardType.isNullable(), explicitlyImmutable, contextuallyImmutable, wildcardType.getSuperTypes(), wildcardType.getSubTypes(), null);
    }
    if (type instanceof PrimitiveType || type instanceof TupleType || type instanceof FunctionType || type instanceof NullType)
    {
      return type;
    }
    throw new IllegalArgumentException("Cannot change the immutability of an unknown type: " + type);
  }

  /**
   * Finds a version of the specified type without any type modifiers (i.e. without nullability and immutability).
   * @param type - the Type to find without any type modifiers
   * @return a version of the specified type without any type modifiers
   */
  public static Type findTypeWithoutModifiers(Type type)
  {
    Type result = findTypeWithNullability(type, false);
    return findTypeWithDataImmutability(result, false, false);
  }

  /**
   * Finds all of the super-types of the specified type.
   * If the type is a wildcard or a type parameter, then this can return a set containing multiple types.
   * Otherwise, the set will just contain the original type.
   * Note: this method ignores all nullability and immutability constraints that the original type may have - they should be dealt with explicitly by the caller.
   * @param type - the type to find all of the super-types of
   * @return the set of all super-types of the specified type
   */
  public static Set<Type> findAllSuperTypes(Type type)
  {
    Set<TypeParameter> visitedTypeParameters = new HashSet<TypeParameter>();
    Set<Type> superTypes = new HashSet<Type>();
    Deque<Type> typeQueue = new LinkedList<Type>();
    typeQueue.add(type);
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
        // we can ignore the Wildcard type's nullability and immutability, as the caller has already checked that those properties do not conflict
        for (Type t : wildcardType.getSuperTypes())
        {
          typeQueue.add(t);
        }
      }
      else
      {
        superTypes.add(superType);
      }
    }
    return superTypes;
  }
}
