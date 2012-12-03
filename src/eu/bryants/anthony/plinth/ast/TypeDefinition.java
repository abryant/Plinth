package eu.bryants.anthony.plinth.ast;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Initialiser;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.metadata.FieldInitialiser;
import eu.bryants.anthony.plinth.ast.misc.QName;

/*
 * Created on 11 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class TypeDefinition
{
  private boolean isImmutable;

  private String name;
  private QName qname;

  private LexicalPhrase lexicalPhrase;

  /**
   * Creates a new TypeDefinition with the specified name.
   * @param isImmutable - true if this TypeDefinition should be immutable, false otherwise
   * @param name - the name of the type defintion
   * @param lexicalPhrase - the LexicalPhrase of this TypeDefinition
   */
  public TypeDefinition(boolean isImmutable, String name, LexicalPhrase lexicalPhrase)
  {
    this.isImmutable = isImmutable;
    this.name = name;
    this.lexicalPhrase = lexicalPhrase;
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
   */
  protected static Method[] buildNonStaticMethodList(Collection<Method> allMethods)
  {
    // filter out static fields, and sort the non-static fields by name
    List<Method> list = new LinkedList<Method>();
    for (Method method : allMethods)
    {
      if (!method.isStatic())
      {
        list.add(method);
      }
    }
    Collections.sort(list, new Comparator<Method>()
    {
      @Override
      public int compare(Method o1, Method o2)
      {
        return ((Comparable<Object>) o1.getDisambiguator()).compareTo(o2.getDisambiguator());
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
   * @param name - the name of the field to get
   * @return the Field with the specified name, or null if none exists
   */
  public abstract Field getField(String name);

  /**
   * @return the constructors of this TypeDefinition
   */
  public abstract Collection<Constructor> getConstructors();

  /**
   * @return an array containing all of the methods in this TypeDefinition
   */
  public abstract Method[] getAllMethods();

  /**
   * @return the non-static methods, in order of their indices
   */
  public abstract Method[] getNonStaticMethods();

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
    for (Constructor constructor : getConstructors())
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
