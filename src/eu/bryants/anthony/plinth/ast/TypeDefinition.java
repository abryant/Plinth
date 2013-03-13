package eu.bryants.anthony.plinth.ast;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Initialiser;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.metadata.FieldInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunction;
import eu.bryants.anthony.plinth.ast.metadata.MemberVariable;
import eu.bryants.anthony.plinth.ast.metadata.PropertyInitialiser;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;

/*
 * Created on 11 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class TypeDefinition
{
  private boolean isAbstract;
  private boolean isImmutable;

  private String name;
  private QName qname;

  private LexicalPhrase lexicalPhrase;

  // computed by the InheritanceChecker - specifies which order this type and its supertypes are inherited in
  private TypeDefinition[] inheritanceLinearisation;

  /**
   * Creates a new TypeDefinition with the specified name.
   * @param isAbstract - true if this TypeDefinition should be abstract, false otherwise
   * @param isImmutable - true if this TypeDefinition should be immutable, false otherwise
   * @param name - the name of the type defintion
   * @param lexicalPhrase - the LexicalPhrase of this TypeDefinition
   */
  public TypeDefinition(boolean isAbstract, boolean isImmutable, String name, LexicalPhrase lexicalPhrase)
  {
    this.isAbstract = isAbstract;
    this.isImmutable = isImmutable;
    this.name = name;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the isAbstract
   */
  public boolean isAbstract()
  {
    return isAbstract;
  }

  /**
   * @return the isImmutable
   */
  public boolean isImmutable()
  {
    return isImmutable;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @param qname - the new qualified name for this TypeDefinition
   */
  public void setQualifiedName(QName qname)
  {
    this.qname = qname;
  }

  /**
   * @return the qname
   */
  public QName getQualifiedName()
  {
    return qname;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

  /**
   * @return the order in which this type and its supertypes are inherited
   */
  public TypeDefinition[] getInheritanceLinearisation()
  {
    return inheritanceLinearisation;
  }

  /**
   * @param inheritanceLinearisation - the order in which this type and its supertypes are inherited
   */
  public void setInheritanceLinearisation(TypeDefinition[] inheritanceLinearisation)
  {
    this.inheritanceLinearisation = inheritanceLinearisation;
  }

  /**
   * Builds the array of member variables and sets the variables' indices.
   * The variable order is based on the lexicographical ordering of their names.
   */
  protected static MemberVariable[] buildMemberVariableList(Collection<Field> allFields, Collection<Property> allProperties)
  {
    // filter out static fields, and sort the non-static fields by name
    List<MemberVariable> list = new LinkedList<MemberVariable>();
    for (Field field : allFields)
    {
      if (!field.isStatic())
      {
        list.add(field.getMemberVariable());
      }
    }
    for (Property property : allProperties)
    {
      if (!property.isStatic() && !property.isUnbacked())
      {
        list.add(property.getBackingMemberVariable());
      }
    }
    Collections.sort(list, new Comparator<MemberVariable>()
    {
      @Override
      public int compare(MemberVariable o1, MemberVariable o2)
      {
        // first, compare the since specifiers, as they are always the first thing we sort on
        SinceSpecifier since1;
        if (o1.getField() != null)
        {
          since1 = o1.getField().getSinceSpecifier();
        }
        else // o1.getProperty() != null
        {
          since1 = o1.getProperty().getSinceSpecifier();
        }
        SinceSpecifier since2;
        if (o2.getField() != null)
        {
          since2 = o2.getField().getSinceSpecifier();
        }
        else // o2.getProperty() != null
        {
          since2 = o2.getProperty().getSinceSpecifier();
        }
        // two null since specifiers are equal, and a null since specifiers always comes before a not-null one
        if ((since1 == null) != (since2 == null))
        {
          return since1 == null ? -1 : 1;
        }
        if (since1 != null && since2 != null)
        {
          int sinceComparison = since1.compareTo(since2);
          if (sinceComparison != 0)
          {
            return sinceComparison;
          }
        }
        // if the since specifiers are equal, compare the names
        return o1.getName().compareTo(o2.getName());
      }
    });
    MemberVariable[] memberVariables = list.toArray(new MemberVariable[list.size()]);
    for (int i = 0; i < memberVariables.length; ++i)
    {
      memberVariables[i].setMemberIndex(i);
    }
    return memberVariables;
  }

  /**
   * Builds the array of non-static fields and sets the fields' indices.
   * The field order is based on the lexicographical ordering of their names.
   * @return the sorted array of non-static methods for this type
   */
  protected MemberFunction[] buildMemberFunctionList(Collection<Method> allMethods, Collection<Property> allProperties)
  {
    // filter out static methods
    List<MemberFunction> list = new LinkedList<MemberFunction>();
    for (Method method : allMethods)
    {
      if (method.isStatic())
      {
        continue;
      }
      list.add(method.getMemberFunction());
    }
    for (Property property : allProperties)
    {
      if (property.isStatic())
      {
        continue;
      }
      list.add(property.getGetterMemberFunction());
      if (!property.isFinal())
      {
        list.add(property.getSetterMemberFunction());
      }
      if (property.hasConstructor())
      {
        list.add(property.getConstructorMemberFunction());
      }
    }
    // sort the member functions into their natural order
    Collections.sort(list);
    MemberFunction[] memberFunctions = list.toArray(new MemberFunction[list.size()]);
    for (int i = 0; i < memberFunctions.length; ++i)
    {
      memberFunctions[i].setMemberIndex(i);
    }
    return memberFunctions;
  }

  /**
   * Builds the list of non-static methods, so that they can be used by the compilation passes.
   */
  public abstract void buildMemberFunctions();

  /**
   * @return the member functions
   */
  public abstract MemberFunction[] getMemberFunctions();

  /**
   * Note: the returned list should never be modified
   * @return the Initialisers, in declaration order
   */
  public abstract List<Initialiser> getInitialisers();

  /**
   * Note: the returned collection should never be modified
   * @return the fields
   */
  public abstract Collection<Field> getFields();

  /**
   * @param name - the name of the field to get
   * @return the Field with the specified name, or null if none exists
   */
  public abstract Field getField(String name);


  /**
   * Note: the returned collection should never be modified
   * @return the properties
   */
  public abstract Collection<Property> getProperties();

  /**
   * @param name - the name of the Property to get
   * @return the Property with the specified name, or null if none exists
   */
  public abstract Property getProperty(String name);

  /**
   * Note: this collection should never be modified
   * @return all of the constructors of this TypeDefinition, including ones which only differ in their since specifiers
   */
  public abstract Collection<Constructor> getAllConstructors();

  /**
   * @return all of the constructors for this TypeDefinition, excluding the ones which only differ in their since specifiers
   */
  public Set<Constructor> getUniqueConstructors()
  {
    Map<String, Constructor> constructors = new HashMap<String, Constructor>();
    for (Constructor constructor : getAllConstructors())
    {
      StringBuffer disambiguatorBuffer = new StringBuffer();
      for (Parameter p : constructor.getParameters())
      {
        disambiguatorBuffer.append(p.getType().getMangledName());
      }
      String disambiguator = disambiguatorBuffer.toString();
      Constructor existing = constructors.get(disambiguator);
      if (existing == null)
      {
        constructors.put(disambiguator, constructor);
      }
      else
      {
        SinceSpecifier existingSince = existing.getSinceSpecifier();
        SinceSpecifier currentSince = constructor.getSinceSpecifier();
        Constructor newer = existingSince == null ? constructor :
                            existingSince.compareTo(currentSince) < 0 ? constructor : existing;
        constructors.put(disambiguator, newer);
      }
    }
    return new HashSet<Constructor>(constructors.values());
  }

  /**
   * Note: this collection should never be modified
   * @return a Collection containing all of the methods in this TypeDefinition
   */
  public abstract Collection<Method> getAllMethods();

  /**
   * @param name - the name to get the methods with
   * @return the set of methods with the specified name
   */
  public abstract Set<Method> getMethodsByName(String name);

  /**
   * @return a String representing the body of this type definition, including all members and the braces around them
   */
  public String getBodyString()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append("{\n");
    // we don't try to print the initialisers interspersed with the fields here, it would take too much effort
    // regardless, the generated code will run them in the correct order
    // (with field initialisers run between two standard initialisers if that is the order they are defined in)
    for (Initialiser initialiser : getInitialisers())
    {
      if (initialiser instanceof FieldInitialiser || initialiser instanceof PropertyInitialiser)
      {
        continue;
      }
      buffer.append(initialiser.toString().replaceAll("(?m)^", "  "));
      buffer.append('\n');
    }
    for (Field field : getFields())
    {
      buffer.append(field.toString().replaceAll("(?m)^", "  "));
      buffer.append("\n");
    }
    for (Property property : getProperties())
    {
      buffer.append(property.toString().replaceAll("(?m)^", "  "));
      buffer.append("\n");
    }
    for (Constructor constructor : getAllConstructors())
    {
      buffer.append(constructor.toString().replaceAll("(?m)^", "  "));
      buffer.append("\n");
    }
    for (Method method : getAllMethods())
    {
      buffer.append(method.toString().replaceAll("(?m)^", "  "));
      buffer.append("\n");
    }
    buffer.append("}");
    return buffer.toString();
  }
}
