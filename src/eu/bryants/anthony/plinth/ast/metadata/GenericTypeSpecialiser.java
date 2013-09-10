package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.misc.DefaultParameter;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.NullType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.ast.type.WildcardType;

/*
 * Created on 8 May 2013
 */

/**
 * A type-specialisation interface which allows MemberReferences to translate from the types of their referenced Members to more specialised types.
 * @author Anthony Bryant
 */
public final class GenericTypeSpecialiser
{
  /**
   * A GenericTypeSpecialiser which doesn't actually specialise anything.
   * This should be used if generic type parameters are never available in a given situation but a MemberReference still needs to be created.
   */
  public static GenericTypeSpecialiser IDENTITY_SPECIALISER = new GenericTypeSpecialiser(null);

  private NamedType specialisationType;

  public GenericTypeSpecialiser(NamedType specialisationType)
  {
    this.specialisationType = specialisationType;
  }

  /**
   * Tries to specialise the specified type into a more specific one, by replacing references to type parameters by references to the type arguments of this GenericTypeSpecialiser's specialisationType.
   * @param type - the Type to specialise
   * @return the specialised type, or a type equivalent to the original if it could not be specialised
   */
  public Type getSpecialisedType(Type type)
  {
    if (specialisationType == null || specialisationType.getTypeArguments() == null)
    {
      return type;
    }
    TypeDefinition specialisationTypeDefinition = specialisationType.getResolvedTypeDefinition();
    if (specialisationTypeDefinition == null)
    {
      throw new IllegalStateException("Cannot specialise a member with an unresolved specialisationType");
    }
    TypeParameter[] typeParameters = specialisationTypeDefinition.getTypeParameters();
    if (typeParameters.length == 0)
    {
      return type;
    }
    if (type instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) type;
      Type specialisedBaseType = getSpecialisedType(arrayType.getBaseType());
      return new ArrayType(arrayType.isNullable(), arrayType.isExplicitlyImmutable(), arrayType.isContextuallyImmutable(), specialisedBaseType, null);
    }
    if (type instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) type;
      Type[] oldParameterTypes = functionType.getParameterTypes();
      Type[] newParameterTypes = new Type[oldParameterTypes.length];
      for (int i = 0; i < oldParameterTypes.length; ++i)
      {
        newParameterTypes[i] = getSpecialisedType(oldParameterTypes[i]);
      }
      DefaultParameter[] oldDefaultParameters = functionType.getDefaultParameters();
      DefaultParameter[] newDefaultParameters = new DefaultParameter[oldDefaultParameters.length];
      for (int i = 0; i < oldDefaultParameters.length; ++i)
      {
        Type oldType = oldDefaultParameters[i].getType();
        Type newType = getSpecialisedType(oldType);
        if (oldType == newType)
        {
          newDefaultParameters[i] = oldDefaultParameters[i];
        }
        else
        {
          newDefaultParameters[i] = new DefaultParameter(newType, oldDefaultParameters[i].getName(), oldDefaultParameters[i].getExpression(), null);
        }
      }
      Type returnType = getSpecialisedType(functionType.getReturnType());
      NamedType[] oldThrownTypes = functionType.getThrownTypes();
      NamedType[] newThrownTypes = new NamedType[oldThrownTypes.length];
      for (int i = 0; i < oldThrownTypes.length; ++i)
      {
        // we can cast the specialised type to a NamedType here because only NamedTypes can implement Throwable
        newThrownTypes[i] = (NamedType) getSpecialisedType(oldThrownTypes[i]);
      }
      return new FunctionType(functionType.isNullable(), functionType.isImmutable(), returnType, newParameterTypes, newDefaultParameters, newThrownTypes, null);
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      if (namedType.getResolvedTypeParameter() != null)
      {
        for (int i = 0; i < typeParameters.length; ++i)
        {
          if (typeParameters[i] == namedType.getResolvedTypeParameter())
          {
            // make a replacement
            Type replacementType = specialisationType.getTypeArguments()[i];
            if (namedType.isNullable())
            {
              replacementType = Type.findTypeWithNullability(replacementType, true);
            }
            if (namedType.isExplicitlyImmutable())
            {
              replacementType = Type.findTypeWithDataImmutability(replacementType, true, true);
            }
            else if (namedType.isContextuallyImmutable())
            {
              replacementType = Type.findTypeWithDataImmutability(replacementType, Type.isExplicitlyDataImmutable(replacementType), true);
            }
            return replacementType;
          }
        }
      }
      Type[] typeArguments = namedType.getTypeArguments();
      if (typeArguments != null && typeArguments.length > 0)
      {
        Type[] newTypeArguments = new Type[typeArguments.length];
        for (int i = 0; i < typeArguments.length; ++i)
        {
          newTypeArguments[i] = getSpecialisedType(typeArguments[i]);
        }
        return new NamedType(namedType.isNullable(), namedType.isExplicitlyImmutable(), namedType.isContextuallyImmutable(), namedType.getResolvedTypeDefinition(), newTypeArguments);
      }
      // nothing to replace
      return namedType;
    }
    if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      Type[] subTypes = tupleType.getSubTypes();
      Type[] newSubTypes = new Type[subTypes.length];
      for (int i = 0; i < subTypes.length; ++i)
      {
        newSubTypes[i] = getSpecialisedType(subTypes[i]);
      }
      return new TupleType(tupleType.isNullable(), newSubTypes, null);
    }
    if (type instanceof WildcardType)
    {
      WildcardType wildcardType = (WildcardType) type;
      Type[] superTypes = wildcardType.getSuperTypes();
      Type[] subTypes = wildcardType.getSubTypes();
      if (superTypes.length == 0 && subTypes.length == 0)
      {
        // nothing to replace
        return wildcardType;
      }
      Type[] newSuperTypes = new Type[superTypes.length];
      Type[] newSubTypes = new Type[subTypes.length];
      for (int i = 0; i < superTypes.length; ++i)
      {
        newSuperTypes[i] = getSpecialisedType(superTypes[i]);
      }
      for (int i = 0; i < subTypes.length; ++i)
      {
        newSubTypes[i] = getSpecialisedType(subTypes[i]);
      }
      return new WildcardType(wildcardType.isNullable(), wildcardType.isExplicitlyImmutable(), wildcardType.isContextuallyImmutable(), newSuperTypes, newSubTypes, null);
    }
    if (type instanceof NullType || type instanceof ObjectType || type instanceof PrimitiveType || type instanceof VoidType)
    {
      return type;
    }
    throw new IllegalArgumentException("Unknown type of Type: " + type);
  }
}