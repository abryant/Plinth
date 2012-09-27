package eu.bryants.anthony.toylanguage.ast;

import java.util.Collection;
import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Initialiser;
import eu.bryants.anthony.toylanguage.ast.member.Method;
import eu.bryants.anthony.toylanguage.ast.metadata.FieldInitialiser;
import eu.bryants.anthony.toylanguage.ast.misc.QName;

/*
 * Created on 11 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class TypeDefinition
{

  private String name;
  private QName qname;

  private LexicalPhrase lexicalPhrase;

  /**
   * Creates a new TypeDefinition with the specified name.
   * @param name - the name of the type defintion
   * @param lexicalPhrase - the LexicalPhrase of this TypeDefinition
   */
  public TypeDefinition(String name, LexicalPhrase lexicalPhrase)
  {
    this.name = name;
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
