package eu.bryants.anthony.plinth.compiler.passes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.metadata.ConstructorReference;
import eu.bryants.anthony.plinth.ast.metadata.GenericTypeSpecialiser;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunctionType;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;
import eu.bryants.anthony.plinth.ast.metadata.OverrideFunction;
import eu.bryants.anthony.plinth.ast.metadata.PropertyReference;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.compiler.CoalescedConceptualException;
import eu.bryants.anthony.plinth.compiler.ConceptualException;

/*
 * Created on 9 Jan 2013
 */

/**
 * This compilation pass checks that all inherited and overriding members behave properly, by performing checks such as the following:
 * <ul>
 * <li>Overriding methods have the same guarantees on their behaviour that their superclass methods have, e.g. if a superclass method is immutable, then the overriding method must be too</li>
 * <li>Abstract methods must be overridden by all non-abstract classes</li>
 * <li>Sealed methods must never be overridden</li>
 * </ul>
 *
 * Requires that the following passes have already been run:
 * <ul>
 * <li>Resolver (top level types)</li>
 * <li>Member Function list building</li>
 * <li>Cycle Checker (inheritance)</li>
 * </ul>
 * @author Anthony Bryant
 */
public class InheritanceChecker
{
  /**
   * Finds the linearisation of the inherited classes of the specified type, and returns it after caching it in the TypeDefinition itself
   * @param typeDefinition - the TypeDefinition to get the linearisation of
   * @return the inheritance linearisation of the specified TypeDefinition
   * @throws ConceptualException - if there is an error finding the linearisation for the specified type
   */
  public static NamedType[] findInheritanceLinearisation(TypeDefinition typeDefinition) throws ConceptualException
  {
    if (typeDefinition.getInheritanceLinearisation() != null)
    {
      return typeDefinition.getInheritanceLinearisation();
    }

    List<NamedType> parents = new LinkedList<NamedType>();
    TypeParameter[] typeParameters;
    if (typeDefinition instanceof ClassDefinition)
    {
      ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
      typeParameters = classDefinition.getTypeParameters();
      NamedType superType = classDefinition.getSuperType();
      if (superType != null)
      {
        TypeDefinition superTypeDefinition = superType.getResolvedTypeDefinition();
        if (superTypeDefinition == null)
        {
          if (superType.getResolvedTypeParameter() != null)
          {
            throw new ConceptualException("A class cannot extend a type parameter", superType.getLexicalPhrase());
          }
          throw new IllegalArgumentException("Cannot check the inherited members of a type before its super-type has been resolved");
        }
        if (superTypeDefinition instanceof CompoundDefinition)
        {
          throw new ConceptualException("A class may not extend a compound type", classDefinition.getLexicalPhrase());
        }
        else if (superTypeDefinition instanceof InterfaceDefinition)
        {
          throw new ConceptualException("A class may not extend an interface", classDefinition.getLexicalPhrase());
        }
        else if (!(superTypeDefinition instanceof ClassDefinition))
        {
          throw new IllegalStateException("Unknown super-type definition: " + superTypeDefinition);
        }
        parents.add(superType);
      }
      NamedType[] superInterfaceTypes = classDefinition.getSuperInterfaceTypes();
      if (superInterfaceTypes != null)
      {
        for (NamedType superInterfaceType : superInterfaceTypes)
        {
          TypeDefinition superInterfaceTypeDefinition = superInterfaceType.getResolvedTypeDefinition();
          if (superInterfaceTypeDefinition == null)
          {
            if (superInterfaceType.getResolvedTypeParameter() != null)
            {
              throw new ConceptualException("A class cannot implement a type parameter", superInterfaceType.getLexicalPhrase());
            }
            throw new IllegalArgumentException("Cannot check the inherited members of a type before its super-interface-types have been resolved");
          }
          if (superInterfaceTypeDefinition instanceof ClassDefinition)
          {
            throw new ConceptualException("A class may not implement another class", classDefinition.getLexicalPhrase());
          }
          else if (superInterfaceTypeDefinition instanceof CompoundDefinition)
          {
            throw new ConceptualException("A class may not implement a compound type", classDefinition.getLexicalPhrase());
          }
          else if (!(superInterfaceTypeDefinition instanceof InterfaceDefinition))
          {
            throw new IllegalStateException("Unknown super-interface-type definition: " + superInterfaceTypeDefinition);
          }
          parents.add(superInterfaceType);
        }
      }
    }
    else if (typeDefinition instanceof InterfaceDefinition)
    {
      InterfaceDefinition interfaceDefinition = (InterfaceDefinition) typeDefinition;
      typeParameters = interfaceDefinition.getTypeParameters();
      NamedType[] superInterfaceTypes = interfaceDefinition.getSuperInterfaceTypes();
      if (superInterfaceTypes != null)
      {
        for (NamedType superInterfaceType : superInterfaceTypes)
        {
          TypeDefinition superInterfaceTypeDefinition = superInterfaceType.getResolvedTypeDefinition();
          if (superInterfaceTypeDefinition == null)
          {
            if (superInterfaceType.getResolvedTypeParameter() != null)
            {
              throw new ConceptualException("An interface cannot extend a type parameter", superInterfaceType.getLexicalPhrase());
            }
            throw new IllegalArgumentException("Cannot check the inherited members of a type before its super-interface-types have been resolved");
          }
          if (superInterfaceTypeDefinition instanceof ClassDefinition)
          {
            throw new ConceptualException("An interface may not extend a class", interfaceDefinition.getLexicalPhrase());
          }
          else if (superInterfaceTypeDefinition instanceof CompoundDefinition)
          {
            throw new ConceptualException("An interface may not extend a compound type", interfaceDefinition.getLexicalPhrase());
          }
          else if (!(superInterfaceTypeDefinition instanceof InterfaceDefinition))
          {
            throw new IllegalStateException("Unknown super-interface-type definition: " + superInterfaceTypeDefinition);
          }
          parents.add(superInterfaceType);
        }
      }
    }
    else if (typeDefinition instanceof CompoundDefinition)
    {
      CompoundDefinition compoundDefinition = (CompoundDefinition) typeDefinition;
      typeParameters = compoundDefinition.getTypeParameters();
    }
    else
    {
      throw new IllegalArgumentException("Unknown type of TypeDefinition: " + typeDefinition);
    }

    // filter duplicates from parents
    Iterator<NamedType> duplicateIterator = parents.iterator();
    while (duplicateIterator.hasNext())
    {
      NamedType current = duplicateIterator.next();
      boolean duplicated = false;
      for (NamedType t : parents)
      {
        if (t == current)
        {
          // we have got to the same position in the list as the outer loop
          break;
        }
        if (t.isEquivalent(current))
        {
          // there is another type before current in parents which is equivalent to current, so the current type is a duplicate
          duplicated = true;
          break;
        }
      }
      if (duplicated)
      {
        duplicateIterator.remove();
      }
    }

    List<List<NamedType>> precedenceLists = new LinkedList<List<NamedType>>();
    for (NamedType parent : parents)
    {
      GenericTypeSpecialiser genericTypeSpecialiser = new GenericTypeSpecialiser(parent);

      NamedType[] linearisation = findInheritanceLinearisation(parent.getResolvedTypeDefinition());
      List<NamedType> precedenceList = new LinkedList<NamedType>();
      for (NamedType t : linearisation)
      {
        precedenceList.add((NamedType) genericTypeSpecialiser.getSpecialisedType(t));
      }
      precedenceLists.add(precedenceList);
    }
    // add the local precedence order for this type
    precedenceLists.add(parents);

    // start the resulting linearisation, by adding this type to the list
    // the type arguments must be specified, so we set them to just point back to the type parameters
    List<NamedType> result = new LinkedList<NamedType>();
    Type[] typeArguments = null;
    if (typeParameters.length > 0)
    {
      typeArguments = new Type[typeParameters.length];
      for (int i = 0; i < typeParameters.length; ++i)
      {
        typeArguments[i] = new NamedType(false, false, false, typeParameters[i]);
      }
    }
    result.add(new NamedType(false, false, typeDefinition, typeArguments));
    while (true)
    {
      // remove empty lists
      Iterator<List<NamedType>> it = precedenceLists.iterator();
      while (it.hasNext())
      {
        if (it.next().isEmpty())
        {
          it.remove();
        }
      }
      if (precedenceLists.isEmpty())
      {
        break;
      }

      NamedType next = null;
      for (List<NamedType> candidateList : precedenceLists)
      {
        NamedType candidate = candidateList.get(0);
        for (List<NamedType> otherList : precedenceLists)
        {
          // check the tail of this otherList
          boolean skippedFirst = false;
          for (NamedType tailElement : otherList)
          {
            if (!skippedFirst)
            {
              skippedFirst = true;
              continue;
            }
            if (tailElement.isEquivalent(candidate))
            {
              candidate = null;
              break;
            }
          }
          if (candidate == null)
          {
            break;
          }
        }
        if (candidate != null)
        {
          next = candidate;
          break;
        }
      }

      if (next == null)
      {
        StringBuffer buffer = new StringBuffer();
        Set<NamedType> added = new HashSet<NamedType>();
        for (List<NamedType> currentList : precedenceLists)
        {
          NamedType candidate = currentList.get(0);
          boolean alreadyAdded = false;
          for (NamedType t : added)
          {
            if (t.isEquivalent(candidate))
            {
              alreadyAdded = true;
              break;
            }
          }
          if (alreadyAdded)
          {
            continue;
          }
          added.add(candidate);
          buffer.append("\n");
          buffer.append(candidate.toString());
        }
        throw new ConceptualException("Cannot find a good linearisation for the inheritance hierarchy of " + typeDefinition.getQualifiedName() + ": cannot decide which of the following super-types should be preferred:" + buffer, typeDefinition.getLexicalPhrase());
      }
      result.add(next);
      for (List<NamedType> currentList : precedenceLists)
      {
        if (currentList.get(0).isEquivalent(next))
        {
          currentList.remove(0);
        }
      }
    }

    NamedType[] linearisation = result.toArray(new NamedType[result.size()]);
    typeDefinition.setInheritanceLinearisation(linearisation);
    return linearisation;
  }

  /**
   * Checks the inherited members of the specified TypeDefinition for problems.
   * @param typeDefinition - the TypeDefinition to check all of the inherited members of
   * @throws ConceptualException - if there is a conceptual problem with the specified TypeDefinition
   */
  public static void checkInheritedMembers(TypeDefinition typeDefinition) throws ConceptualException
  {
    CoalescedConceptualException coalescedException = null;
    if (typeDefinition instanceof ClassDefinition)
    {
      ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
      NamedType superType = classDefinition.getSuperType();
      if (superType != null)
      {
        ClassDefinition superClass = (ClassDefinition) superType.getResolvedTypeDefinition();
        if (superClass.isImmutable() && !classDefinition.isImmutable())
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot define a non-immutable class to be a subclass of an immutable class", classDefinition.getLexicalPhrase()));
        }
      }

      NamedType[] superInterfaceTypes = classDefinition.getSuperInterfaceTypes();
      if (superInterfaceTypes != null)
      {
        for (NamedType superInterfaceType : superInterfaceTypes)
        {
          InterfaceDefinition superInterface = (InterfaceDefinition) superInterfaceType.getResolvedTypeDefinition();
          if (superInterface.isImmutable() && !classDefinition.isImmutable())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot define a non-immutable class to implement an immutable interface", classDefinition.getLexicalPhrase()));
          }
        }
      }
    }
    else if (typeDefinition instanceof InterfaceDefinition)
    {
      InterfaceDefinition interfaceDefinition = (InterfaceDefinition) typeDefinition;
      NamedType[] superInterfaceTypes = interfaceDefinition.getSuperInterfaceTypes();
      if (superInterfaceTypes != null)
      {
        for (NamedType superInterfaceType : superInterfaceTypes)
        {
          InterfaceDefinition superInterface = (InterfaceDefinition) superInterfaceType.getResolvedTypeDefinition();
          if (superInterface.isImmutable() && !interfaceDefinition.isImmutable())
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot define a non-immutable interface to implement an immutable interface", interfaceDefinition.getLexicalPhrase()));
          }
        }
      }
    }

    for (Property property : typeDefinition.getProperties())
    {
      try
      {
        checkProperty(property);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
    }

    for (Method method : typeDefinition.getAllMethods())
    {
      try
      {
        checkMethod(method);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
    }

    if (coalescedException != null)
    {
      throw coalescedException;
    }

    checkTypeMembers(typeDefinition.getInheritanceLinearisation()[0], false, typeDefinition.getLexicalPhrase(), typeDefinition.getInheritanceLinearisation());
  }

  /**
   * Checks that the specified NamedType is possible from the perspective of inheritance.
   * For example, some generic specialisations of a type are impossible because they would result in duplicated members.
   * @param namedType - the NamedType to check
   * @throws ConceptualException - if there is a conceptual problem with any of the type's members
   */
  public static void checkNamedType(NamedType namedType) throws ConceptualException
  {
    if (namedType.getResolvedTypeDefinition() == null)
    {
      throw new IllegalArgumentException("Cannot check a NamedType which hasn't been resolved to a TypeDefinition");
    }
    GenericTypeSpecialiser genericTypeSpecialiser = new GenericTypeSpecialiser(namedType);
    NamedType[] originalLinearisation = namedType.getResolvedTypeDefinition().getInheritanceLinearisation();
    NamedType[] specialisedLinearisation = new NamedType[originalLinearisation.length];
    for (int i = 0; i < originalLinearisation.length; ++i)
    {
      // this cast is fine, as nothing in the linearisation can represent a TypeParameter
      specialisedLinearisation[i] = (NamedType) genericTypeSpecialiser.getSpecialisedType(originalLinearisation[i]);
    }
    checkTypeMembers(namedType, true, namedType.getLexicalPhrase(), specialisedLinearisation);
  }

  /**
   * Checks the members of a type which is a combination of the specified NamedTypes.
   * None of the NamedTypes can represent TypeParameters.
   * @param concreteType - the concrete type that is a sub-type of all of the combined types (this cannot represent a TypeParameter)
   * @param concreteIsSpecialised - true iff the concrete type has been specialised and does not represent just a TypeDefinition (this suppresses certain errors that should only occur for TypeDefinitions)
   * @param concreteTypeLexicalPhrase - the LexicalPhrase to display as the cause of any errors that occur that are the type's problem and not a member's
   * @param linearisation - the linearisation that makes up the type to check. This can be a specialised linearisation based on a NamedType with some filled-in type arguments, or just the pure linearisation of a TypeDefinition.
   * @throws ConceptualException - if there is a conceptual problem with any of the type's members
   */
  private static void checkTypeMembers(NamedType concreteType, boolean concreteIsSpecialised, LexicalPhrase concreteTypeLexicalPhrase, NamedType[] linearisation) throws ConceptualException
  {
    Set<MethodReference> inheritedMethodReferences = new HashSet<MethodReference>();
    Set<PropertyReference> inheritedPropertyReferences = new HashSet<PropertyReference>();
    for (Method method : ObjectType.OBJECT_METHODS)
    {
      if (!method.isStatic())
      {
        inheritedMethodReferences.add(new MethodReference(method, GenericTypeSpecialiser.IDENTITY_SPECIALISER));
      }
    }

    for (NamedType currentType : linearisation)
    {
      TypeDefinition currentTypeDefinition = currentType.getResolvedTypeDefinition();
      if (!concreteIsSpecialised && currentTypeDefinition == concreteType.getResolvedTypeDefinition())
      {
        continue;
      }

      checkDuplicateConstructors(currentType, concreteType, concreteTypeLexicalPhrase);

      // check the current type for any duplicated methods
      // this can happen if filling in some generic type parameters creates a second method with the same signature as one that already exists
      GenericTypeSpecialiser currentTypeSpecialiser = new GenericTypeSpecialiser(currentType);
      for (Method method : currentTypeDefinition.getAllMethods())
      {
        if (method.isStatic())
        {
          continue;
        }
        Set<Method> sameNameMethods = currentTypeDefinition.getMethodsByName(method.getName());
        if (sameNameMethods.size() <= 1)
        {
          continue;
        }
        MethodReference methodReference = new MethodReference(method, currentTypeSpecialiser);
        for (Method m : sameNameMethods)
        {
          if (m == method || m.isStatic())
          {
            continue;
          }
          MethodReference checkReference = new MethodReference(m, currentTypeSpecialiser);
          if (methodReference.getDisambiguator().matches(checkReference.getDisambiguator()))
          {
            throw new ConceptualException("Two methods inside " + currentTypeDefinition.getQualifiedName() + " have the signature '" + buildMethodDisambiguatorString(methodReference) + "' after generic types are filled in for " + concreteType,
                                          concreteTypeLexicalPhrase);
          }
        }
      }

      // find the inherited members to check overrides for
      for (Method m : currentTypeDefinition.getAllMethods())
      {
        if (!m.isStatic())
        {
          inheritedMethodReferences.add(new MethodReference(m, currentTypeSpecialiser));
        }
      }
      for (Property p : currentTypeDefinition.getProperties())
      {
        if (!p.isStatic())
        {
          inheritedPropertyReferences.add(new PropertyReference(p, currentTypeSpecialiser));
        }
      }
    }

    // check all of the method overrides
    for (MethodReference inheritedMethodReference : inheritedMethodReferences)
    {
      boolean inheritedIsAbstract = inheritedMethodReference.getReferencedMember().isAbstract();
      boolean hasImplementation = false;

      for (MethodReference override : findAllOverrides(inheritedMethodReference, linearisation, true))
      {
        try
        {
          checkMethodOverride(inheritedMethodReference.getReferencedMember(), override.getReferencedMember());
        }
        catch (ConceptualException e)
        {
          NamedType inheritedType = inheritedMethodReference.getContainingType();
          NamedType overrideType = override.getContainingType();
          if (inheritedType == null || inheritedType.canAssign(overrideType))
          {
            // the existing error message is good enough, because the overriding type is a sub-type of the inherited type
            if (concreteIsSpecialised || !overrideType.isEquivalent(concreteType))
            {
              // this is an error, but it does not involve the class we are checking, so wait until we check currentType to report it, or we will get duplicate errors
              continue;
            }
            throw e;
          }
          // add a new message to point out that the overriding is because the concrete type we are checking inherits from two different types
          throw new ConceptualException("Incompatible method: " + buildMethodDisambiguatorString(inheritedMethodReference) + " is inherited incompatibly from both " + inheritedType + " and " + overrideType + " - the problem is:",
                                        concreteTypeLexicalPhrase, e);
        }
        if (!override.getReferencedMember().isAbstract())
        {
          hasImplementation = true;
        }
      }

      if (!concreteIsSpecialised && !concreteType.getResolvedTypeDefinition().isAbstract() && inheritedIsAbstract && !hasImplementation)
      {
        String declarationType = inheritedMethodReference.getContainingType() == null ? "object" : inheritedMethodReference.getContainingType().toString();
        String disambiguator = buildMethodDisambiguatorString(inheritedMethodReference);
        throw new ConceptualException(concreteType.getResolvedTypeDefinition().getName() + " does not implement the abstract method: " +
                                        disambiguator + " (from type: " + declarationType + ")",
                                      concreteTypeLexicalPhrase);
      }
    }

    // check all of the property overrides
    for (PropertyReference inheritedPropertyReference : inheritedPropertyReferences)
    {
      boolean inheritedIsAbstract = inheritedPropertyReference.getReferencedMember().isAbstract();
      boolean hasImplementation = false;

      for (PropertyReference override : findAllOverrides(inheritedPropertyReference, linearisation, true))
      {
        try
        {
          checkPropertyOverride(inheritedPropertyReference, override);
        }
        catch (ConceptualException e)
        {
          NamedType inheritedType = inheritedPropertyReference.getContainingType();
          NamedType overrideType = override.getContainingType();
          if (inheritedType == null || inheritedType.canAssign(overrideType))
          {
            // the existing error message is good enough, because the current type is a sub-type of the inherited type
            if (concreteIsSpecialised || !overrideType.isEquivalent(concreteType))
            {
              // this is an error, but it does not involve the class we are checking, so wait until we check currentType to report it, or we will get duplicate errors
              continue;
            }
            throw e;
          }
          // add a new message to point out that the overriding is because this TypeDefinition inherits from two different types
          throw new ConceptualException("Incompatible property: '" + inheritedPropertyReference.getReferencedMember().getName() + "' is inherited incompatibly from both " + inheritedType + " and " + overrideType + " - the problem is:",
                                        concreteTypeLexicalPhrase, e);
        }
        if (!override.getReferencedMember().isAbstract())
        {
          hasImplementation = true;
        }
      }

      if (!concreteIsSpecialised && !concreteType.getResolvedTypeDefinition().isAbstract() && inheritedIsAbstract && !hasImplementation)
      {
        String declarationType = inheritedPropertyReference.getContainingType() == null ? "object" : inheritedPropertyReference.getContainingType().toString();
        throw new ConceptualException(concreteType.getResolvedTypeDefinition().getName() + " does not implement the abstract property: " +
                                        inheritedPropertyReference.getReferencedMember().getName() + " (from type: " + declarationType + ")",
                                      concreteTypeLexicalPhrase);
      }
    }
  }



  /**
   * Finds all overrides of the specified MethodReference in the specified linearisation.
   * @param inheritedMethodReference - the MethodReference to find the overrides of
   * @param linearisation - the linearisation to search through
   * @param includeAbstract - true if abstract methods should be included in the set of overrides, false otherwise
   * @return a list of all overrides of the specified MethodReference, with the most-specific override first (i.e. if there is an implementation, it will be first)
   */
  private static List<MethodReference> findAllOverrides(MethodReference inheritedMethodReference, NamedType[] linearisation, boolean includeAbstract)
  {
    List<MethodReference> overrides = new LinkedList<MethodReference>();

    Set<NamedType> ignoredSuperTypes = new HashSet<NamedType>();
    if (inheritedMethodReference.getContainingType() != null)
    {
      ignoredSuperTypes.add(inheritedMethodReference.getContainingType());
    }

    String methodName = inheritedMethodReference.getReferencedMember().getName();

    linearisationLoop:
    for (NamedType currentType : linearisation)
    {
      for (NamedType ignored : ignoredSuperTypes)
      {
        // if the current type is a super-type of one we have already processed and ignored, then ignore it too
        // (here, we depend on all super-types being processed after their sub-types, which is true due to the nature of the C3 linearisation)
        // we ignore any types which declare this member as abstract
        if (!currentType.isEquivalent(ignored) && currentType.canAssign(ignored))
        {
          continue linearisationLoop;
        }
      }

      GenericTypeSpecialiser currentTypeSpecialiser = new GenericTypeSpecialiser(currentType);

      for (Method method : currentType.getResolvedTypeDefinition().getMethodsByName(methodName))
      {
        MethodReference methodReference = new MethodReference(method, currentTypeSpecialiser);
        if (methodReference.getDisambiguator().matches(inheritedMethodReference.getDisambiguator()))
        {
          if (includeAbstract || !method.isAbstract())
          {
            overrides.add(methodReference);
          }
          if (method.isAbstract())
          {
            // ignore all of this type's super-types, but still process other methods from this type
            ignoredSuperTypes.add(currentType);
          }
        }
      }
    }
    if (overrides.isEmpty() && !inheritedMethodReference.getReferencedMember().isAbstract())
    {
      // the inherited member is not abstract, so it should have gone into the overrides list
      // the only way this could happen is if the inherited member is not part of the linearisation
      // which means it must be a built-in method
      // if there are no ignored super-types, then we can just add the inherited type itself to the overrides list
      if (ignoredSuperTypes.isEmpty())
      {
        overrides.add(inheritedMethodReference);
      }
    }
    return overrides;
  }

  /**
   * Finds all overrides of the specified PropertyReference in the specified linearisation.
   * @param inheritedPropertyReference - the PropertyReference to find the overrides of
   * @param linearisation - the linearisation to search through
   * @param includeAbstract - true if abstract properties should be included in the set of overrides, false otherwise
   * @return a list of all overrides of the specified PropertyReference, with the most-specific override first (i.e. if there is an implementation, it will be first)
   */
  private static List<PropertyReference> findAllOverrides(PropertyReference inheritedPropertyReference, NamedType[] linearisation, boolean includeAbstract)
  {
    List<PropertyReference> overrides = new LinkedList<PropertyReference>();

    Set<NamedType> ignoredSuperTypes = new HashSet<NamedType>();
    if (inheritedPropertyReference.getContainingType() != null)
    {
      ignoredSuperTypes.add(inheritedPropertyReference.getContainingType());
    }

    String propertyName = inheritedPropertyReference.getReferencedMember().getName();

    linearisationLoop:
    for (NamedType currentType : linearisation)
    {
      for (NamedType ignored : ignoredSuperTypes)
      {
        // if the current type is a super-type of one we have already processed and ignored, then ignore it too
        // (here, we depend on all super-types being processed after their sub-types, which is true due to the nature of the C3 linearisation)
        // we ignore any types which declare this member as abstract
        if (!currentType.isEquivalent(ignored) && currentType.canAssign(ignored))
        {
          continue linearisationLoop;
        }
      }

      Property property = currentType.getResolvedTypeDefinition().getProperty(propertyName);
      if (property != null)
      {
        GenericTypeSpecialiser currentTypeSpecialiser = new GenericTypeSpecialiser(currentType);
        if (includeAbstract || !property.isAbstract())
        {
          overrides.add(new PropertyReference(property, currentTypeSpecialiser));
        }
        if (property.isAbstract())
        {
          // ignore all of this type's super-types, but still process other properties from this type
          ignoredSuperTypes.add(currentType);
        }
      }
    }
    return overrides;
  }

  /**
   * Finds a list of OverrideFunctions which need to be included in the specified ClassDefinition in order for certain functions to be overridden correctly.
   * @param classDefinition - the ClassDefinition to find the override functions of
   * @return the list of OverrideFunctions for the specified ClassDefinition
   */
  public static List<OverrideFunction> getOverrideVirtualFunctions(ClassDefinition classDefinition)
  {
    if (classDefinition.isAbstract())
    {
      throw new IllegalArgumentException("Cannot get the override virtual functions for an abstract class");
    }
    Set<MethodReference> inheritedMethods = new HashSet<MethodReference>();
    Set<PropertyReference> inheritedProperties = new HashSet<PropertyReference>();
    for (Method method : ObjectType.OBJECT_METHODS)
    {
      if (!method.isStatic())
      {
        inheritedMethods.add(new MethodReference(method, GenericTypeSpecialiser.IDENTITY_SPECIALISER));
      }
    }
    for (NamedType superType : classDefinition.getInheritanceLinearisation())
    {
      if (superType.getResolvedTypeDefinition() == classDefinition)
      {
        continue;
      }
      GenericTypeSpecialiser genericTypeSpecialiser = new GenericTypeSpecialiser(superType);
      TypeDefinition superTypeDefinition = superType.getResolvedTypeDefinition();
      for (Method method : superTypeDefinition.getAllMethods())
      {
        if (!method.isStatic())
        {
          inheritedMethods.add(new MethodReference(method, genericTypeSpecialiser));
        }
      }
      for (Property property : superTypeDefinition.getProperties())
      {
        if (!property.isStatic())
        {
          inheritedProperties.add(new PropertyReference(property, genericTypeSpecialiser));
        }
      }
    }

    List<OverrideFunction> overrideFunctions = new LinkedList<OverrideFunction>();

    NamedType[] linearisation = classDefinition.getInheritanceLinearisation();
    for (MethodReference inheritedMethod : inheritedMethods)
    {
      List<MethodReference> overrides = findAllOverrides(inheritedMethod, linearisation, false);
      if (overrides.isEmpty())
      {
        // there is no implementation for this method, so it must be an abstract non-implemented method
        // this shouldn't be allowed, since this class is not abstract
        throw new IllegalStateException("Could not find the implementation of: " + buildMethodDisambiguatorString(inheritedMethod) + " in " + classDefinition.getQualifiedName());
      }
      // the first non-abstract override must be the implementation, and this overrides list doesn't contain any abstract overrides
      MethodReference implementation = overrides.get(0);

      // check whether we need to create an override virtual function for this
      if (inheritedMethod.getReferencedMember() == implementation.getReferencedMember())
      {
        continue;
      }
      // check whether the VFT descriptor strings that will be used in the underlying VFT match
      // if not, an override function is needed to make sure the override actually takes place
      String inheritedDescriptorName = inheritedMethod.getReferencedMember().getDescriptorString();
      String implementationDescriptorName = implementation.getReferencedMember().getDescriptorString();
      if (!inheritedDescriptorName.equals(implementationDescriptorName))
      {
        // the descriptors are not equal, so generate a proxy function
        overrideFunctions.add(new OverrideFunction(inheritedMethod, implementation));
      }
    }

    for (PropertyReference inheritedReference : inheritedProperties)
    {
      List<PropertyReference> overrides = findAllOverrides(inheritedReference, linearisation, false);
      if (overrides.isEmpty())
      {
        // there is no implementation for this property, so it must be an abstract non-implemented property
        // this shouldn't be allowed, since this class is not abstract
        throw new IllegalStateException("Could not find the implementation of: " + inheritedReference.getReferencedMember().getName() + " in " + classDefinition.getQualifiedName());
      }
      // the first non-abstract override must be the implementation, and this overrides list doesn't contain any abstract overrides
      PropertyReference implementationReference = overrides.get(0);

      // check whether we need to create an override virtual function for this
      if (inheritedReference.getReferencedMember() == implementationReference.getReferencedMember())
      {
        continue;
      }
      Property inherited = inheritedReference.getReferencedMember();
      Property implementation = implementationReference.getReferencedMember();
      if (!inherited.getGetterDescriptor().equals(implementation.getGetterDescriptor()))
      {
        // the descriptors are not equal, so generate a proxy function
        overrideFunctions.add(new OverrideFunction(inheritedReference, implementationReference, MemberFunctionType.PROPERTY_GETTER));
      }
      if (!inherited.isFinal() && !inherited.getSetterDescriptor().equals(implementation.getSetterDescriptor()))
      {
        // the descriptors are not equal, so generate a proxy function
        overrideFunctions.add(new OverrideFunction(inheritedReference, implementationReference, MemberFunctionType.PROPERTY_SETTER));
      }
      if (inherited.hasConstructor() && !inherited.getConstructorDescriptor().equals(implementation.getConstructorDescriptor()))
      {
        // the descriptors are not equal, so generate a proxy function
        overrideFunctions.add(new OverrideFunction(inheritedReference, implementationReference, MemberFunctionType.PROPERTY_CONSTRUCTOR));
      }
    }
    return overrideFunctions;
  }

  /**
   * Checks whether the specified NamedType has any duplicated ConstructorReferences after the type arguments have been applied.
   * @param namedType - the NamedType to check
   * @param concreteType - the concrete type that is being checked
   * @param concreteTypeLexicalPhrase - the LexicalPhrase of the concrete type
   * @throws ConceptualException - if a duplicate constructor is detected
   */
  private static void checkDuplicateConstructors(NamedType namedType, NamedType concreteType, LexicalPhrase concreteTypeLexicalPhrase) throws ConceptualException
  {
    TypeDefinition typeDefinition = namedType.getResolvedTypeDefinition();
    GenericTypeSpecialiser genericTypeSpecialiser = new GenericTypeSpecialiser(namedType);
    // use the unique constructor set, which doesn't include duplicate constructors which have the same pre-generic type list
    // since we are just checking the post-generic type list, we don't want to have constructors which only differ in their since specifier or thrown types before generics
    Collection<Constructor> constructors = typeDefinition.getUniqueConstructors();
    ConstructorReference[] constructorReferences = new ConstructorReference[constructors.size()];
    int index = 0;
    for (Constructor constructor : constructors)
    {
      constructorReferences[index] = new ConstructorReference(constructor, genericTypeSpecialiser);
      index++;
    }
    Set<String> disambiguators = new HashSet<String>();
    for (int i = 0; i < constructorReferences.length; ++i)
    {
      Type[] parameterTypes = constructorReferences[i].getParameterTypes();
      StringBuffer buffer = new StringBuffer();
      for (Type t : parameterTypes)
      {
        buffer.append(t.getMangledName());
      }
      String disambiguator = buffer.toString();
      boolean notKnownAlready = disambiguators.add(disambiguator);
      if (!notKnownAlready)
      {
        throw new ConceptualException("Two constructors inside " + typeDefinition.getQualifiedName() + " have the same signature after generic types are filled in for " + concreteType, concreteTypeLexicalPhrase);
      }
    }
  }

  private static void checkProperty(Property property) throws ConceptualException
  {
    if (property.isAbstract())
    {
      if (property.isStatic())
      {
        throw new ConceptualException("A static property cannot be abstract", property.getLexicalPhrase());
      }
      if (property.getGetterBlock() != null || property.getSetterBlock() != null || property.getConstructorBlock() != null)
      {
        throw new ConceptualException("An abstract property cannot define a getter, a setter, or a constructor", property.getLexicalPhrase());
      }
      if (property.getContainingTypeDefinition() instanceof CompoundDefinition)
      {
        throw new ConceptualException("A compound type cannot contain abstract properties", property.getLexicalPhrase());
      }
      if (!property.getContainingTypeDefinition().isAbstract())
      {
        throw new ConceptualException("Abstract properties can only be declared in abstract classes or interfaces", property.getLexicalPhrase());
      }
    }
  }

  private static void checkMethod(Method method) throws ConceptualException
  {
    if (method.isAbstract())
    {
      if (method.isStatic())
      {
        throw new ConceptualException("A static method cannot be abstract", method.getLexicalPhrase());
      }
      if (method.getNativeName() != null)
      {
        throw new ConceptualException("An abstract method cannot be native", method.getLexicalPhrase());
      }
      if (method.getBlock() != null)
      {
        throw new ConceptualException("An abstract method cannot have a body", method.getLexicalPhrase());
      }
      if (method.getContainingTypeDefinition() instanceof CompoundDefinition)
      {
        throw new ConceptualException("A compound type cannot contain abstract methods", method.getLexicalPhrase());
      }
      if (!method.getContainingTypeDefinition().isAbstract())
      {
        throw new ConceptualException("Abstract methods can only be declared in abstract classes or interfaces", method.getLexicalPhrase());
      }
    }
  }

  private static void checkPropertyOverride(PropertyReference overriddenPropertyReference, PropertyReference propertyReference) throws ConceptualException
  {
    Property overriddenProperty = overriddenPropertyReference.getReferencedMember();
    Property property = propertyReference.getReferencedMember();
    if (!overriddenPropertyReference.getType().isEquivalent(propertyReference.getType()))
    {
      throw new ConceptualException("A property must always have the same type as a property it overrides, in this case: " + overriddenPropertyReference.getType(), property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
    if (overriddenProperty.isFinal() != property.isFinal())
    {
      throw new ConceptualException("A property must always have the same finality as a property it overrides", property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
    if (overriddenProperty.hasConstructor() && !property.hasConstructor())
    {
      throw new ConceptualException("An overriding property must have a constructor if the property it overrides has a constructor", property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
    if (!overriddenProperty.hasConstructor() && property.hasConstructor())
    {
      throw new ConceptualException("An overriding property must not have a constructor if the property it overrides does not have a constructor", property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
    if (overriddenProperty.isGetterImmutable() && !property.isGetterImmutable())
    {
      throw new ConceptualException("A non-immutable property getter cannot override an immutable property getter", property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
    if (overriddenProperty.isSetterImmutable() && !property.isSetterImmutable())
    {
      throw new ConceptualException("A non-immutable property setter cannot override an immutable property setter", property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
    if (overriddenProperty.isConstructorImmutable() && !property.isConstructorImmutable())
    {
      throw new ConceptualException("A non-immutable property constructor cannot override an immutable property constructor", property.getLexicalPhrase(),
                                    overriddenProperty.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenProperty.getLexicalPhrase()) : null);
    }
  }

  private static void checkMethodOverride(Method overriddenMethod, Method method) throws ConceptualException
  {
    if (overriddenMethod.isImmutable() && !method.isImmutable())
    {
      throw new ConceptualException("A non-immutable method cannot override an immutable method", method.getLexicalPhrase(),
                                    overriddenMethod.getLexicalPhrase() != null ? new ConceptualException("Note: overridden from here", overriddenMethod.getLexicalPhrase()) : null);
    }
  }

  private static String buildMethodDisambiguatorString(MethodReference method)
  {
    Type[] parameterTypes = method.getParameterTypes();
    StringBuffer buffer = new StringBuffer();
    buffer.append(method.getReturnType());
    buffer.append(' ');
    buffer.append(method.getReferencedMember().getName());
    buffer.append('(');
    for (int i = 0; i < parameterTypes.length; ++i)
    {
      buffer.append(parameterTypes[i]);
      if (i != parameterTypes.length - 1)
      {
        buffer.append(", ");
      }
    }
    buffer.append(')');
    return buffer.toString();
  }
}
