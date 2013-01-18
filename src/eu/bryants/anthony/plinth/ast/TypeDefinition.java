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
import eu.bryants.anthony.plinth.ast.metadata.FieldInitialiser;
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
   * Builds the array of non-static fields and sets the fields' indices.
   * The field order is based on the lexicographical ordering of their names.
   */
  protected static Field[] buildNonStaticFieldList(Collection<Field> allFields)
  {
    // filter out static fields, and sort the non-static fields by name
    List<Field> list = new LinkedList<Field>();
    for (Field field : allFields)
    {
      if (!field.isStatic())
      {
        list.add(field);
      }
    }
    Collections.sort(list, new Comparator<Field>()
    {
      @Override
      public int compare(Field o1, Field o2)
      {
        // first, compare the since specifiers, as they are always the first thing we sort on
        SinceSpecifier since1 = o1.getSinceSpecifier();
        SinceSpecifier since2 = o2.getSinceSpecifier();
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
    Field[] nonStaticFields = list.toArray(new Field[list.size()]);
    for (int i = 0; i < nonStaticFields.length; ++i)
    {
      nonStaticFields[i].setMemberIndex(i);
    }
    return nonStaticFields;
  }

  /**
   * Builds the array of non-static fields and sets the fields' indices.
   * The field order is based on the lexicographical ordering of their names.
   * @return the sorted array of non-static methods for this type
   */
  protected Method[] buildNonStaticMethodList()
  {
    Method[] allMethods = getAllMethods();
    // filter out static methods
    List<Method> list = new LinkedList<Method>();
    for (Method method : allMethods)
    {
      if (method.isStatic())
      {
        continue;
      }
      list.add(method);
    }
    // sort them by disambiguator
    Collections.sort(list, new Comparator<Method>()
    {
      @Override
      public int compare(Method o1, Method o2)
      {
        return o1.getDisambiguator().compareTo(o2.getDisambiguator());
      }
    });
    Method[] nonStaticMethods = list.toArray(new Method[list.size()]);
    for (int i = 0; i < nonStaticMethods.length; ++i)
    {
      nonStaticMethods[i].setMethodIndex(i);
    }
    return nonStaticMethods;
  }

  /**
   * Builds the list of non-static methods, so that they can be used by the compilation passes.
   */
  public abstract void buildNonStaticMethods();

  /**
   * @return the Initialisers, in declaration order
   */
  public abstract Initialiser[] getInitialisers();

  /**
   * @return the fields
   */
  public abstract Field[] getFields();

  /**
   * @return the non-static fields, in order of their indices
   */
  public abstract Field[] getNonStaticFields();

  /**
   * @return the static fields
   */
  public abstract Field[] getStaticFields();

  /**
   * @param name - the name of the field to get
   * @return the Field with the specified name, or null if none exists
   */
  public abstract Field getField(String name);

  /**
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
   * @return an array containing all of the methods in this TypeDefinition
   */
  public abstract Method[] getAllMethods();

  /**
   * NOTE: for a newly parsed TypeDefinition, buildNonStaticMethods() should always be called before this, as before it is called, this will return null
   * For imported type definitions, the metadata contains the non-static method list in the correct order, so this should not be called.
   * @return the non-static methods, in order of their indices
   */
  public abstract Method[] getNonStaticMethods();

  /**
   * NOTE: for a newly parsed TypeDefinition, buildNonStaticMethods() should always be called before this, because it can rely on the non-static methods list.
   * @return all of the static methods in this TypeDefinition
   */
  public abstract Method[] getStaticMethods();

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
      if (initialiser instanceof FieldInitialiser)
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
