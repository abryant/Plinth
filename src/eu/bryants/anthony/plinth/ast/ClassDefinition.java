package eu.bryants.anthony.plinth.ast;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Initialiser;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.metadata.FieldInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.GlobalVariable;
import eu.bryants.anthony.plinth.ast.metadata.MemberVariable;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.LanguageParseException;

/*
 * Created on 9 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class ClassDefinition extends TypeDefinition
{

  private List<Initialiser> initialisers = new LinkedList<Initialiser>();
  // fields need a guaranteed order, so use a LinkedHashMap to store them
  private Map<String, Field> fields = new LinkedHashMap<String, Field>();
  private Set<Constructor> constructors = new HashSet<Constructor>();
  private Map<String, Set<Method>> methods = new HashMap<String, Set<Method>>();

  private Field[] nonStaticFields;

  public ClassDefinition(String name, Member[] members, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    super(name, lexicalPhrase);
    // add all of the members by name
    int fieldIndex = 0;
    List<Field> nonStaticFieldList = new LinkedList<Field>();
    for (Member member : members)
    {
      if (member instanceof Initialiser)
      {
        initialisers.add((Initialiser) member);
      }
      if (member instanceof Field)
      {
        Field field = (Field) member;
        if (fields.containsKey(field.getName()))
        {
          throw new LanguageParseException("A field with the name '" + field.getName() + "' already exists in '" + name + "', so another field cannot be defined with the same name", field.getLexicalPhrase());
        }
        if (methods.containsKey(field.getName()))
        {
          throw new LanguageParseException("A method with the name '" + field.getName() + "' already exists in '" + name + "', so a field cannot be defined with the same name", field.getLexicalPhrase());
        }
        if (field.isStatic())
        {
          field.setGlobalVariable(new GlobalVariable(field, this));
        }
        else
        {
          field.setMemberIndex(fieldIndex);
          field.setMemberVariable(new MemberVariable(field, this));
          fieldIndex++;
          nonStaticFieldList.add(field);
        }
        fields.put(field.getName(), field);
        if (field.getInitialiserExpression() != null)
        {
          initialisers.add(new FieldInitialiser(field));
        }
      }
      if (member instanceof Constructor)
      {
        Constructor constructor = (Constructor) member;
        if (!constructor.getName().equals(name))
        {
          throw new LanguageParseException("The constructor '" + constructor.getName() + "' should be named '" + name + "' after the class type it is defined in", constructor.getLexicalPhrase());
        }
        constructor.setContainingTypeDefinition(this);
        constructors.add(constructor);
      }
      if (member instanceof Method)
      {
        Method method = (Method) member;
        if (fields.containsKey(method.getName()))
        {
          throw new LanguageParseException("A field with the name '" + method.getName() + "' already exists in '" + name + "', so a method cannot be defined with the same name", method.getLexicalPhrase());
        }
        Set<Method> methodSet = methods.get(method.getName());
        if (methodSet == null)
        {
          methodSet = new HashSet<Method>();
          methods.put(method.getName(), methodSet);
        }
        method.setContainingTypeDefinition(this);
        methodSet.add(method);
      }
    }
    nonStaticFields = nonStaticFieldList.toArray(new Field[nonStaticFieldList.size()]);
  }

  /**
   * Creates a new ClassDefinition with the specified members.
   * @param qname - the qualified name of the class definition
   * @param nonStaticFields - the non static fields, with their indexes already filled in
   * @param staticFields - the static fields
   * @param newConstructors - the constructors
   * @param newMethods - the methods
   * @throws LanguageParseException - if there is a name collision between any of the methods, or a Constructor's name is wrong
   */
  public ClassDefinition(QName qname, Field[] nonStaticFields, Field[] staticFields, Constructor[] newConstructors, Method[] newMethods) throws LanguageParseException
  {
    super(qname.getLastName(), null);
    setQualifiedName(qname);
    for (Field f : nonStaticFields)
    {
      if (fields.containsKey(f.getName()))
      {
        throw new LanguageParseException("A field with the name '" + f.getName() + "' already exists in '" + getName() + "', so another field cannot be defined with the same name", f.getLexicalPhrase());
      }
      if (methods.containsKey(f.getName()))
      {
        throw new LanguageParseException("A method with the name '" + f.getName() + "' already exists in '" + getName() + "', so a field cannot be defined with the same name", f.getLexicalPhrase());
      }
      f.setMemberVariable(new MemberVariable(f, this));
      // we assume that the fields' indices have already been filled in
      fields.put(f.getName(), f);
    }
    this.nonStaticFields = nonStaticFields;
    for (Field f : staticFields)
    {
      if (fields.containsKey(f.getName()))
      {
        throw new LanguageParseException("A field with the name '" + f.getName() + "' already exists in '" + getName() + "', so another field cannot be defined with the same name", f.getLexicalPhrase());
      }
      if (methods.containsKey(f.getName()))
      {
        throw new LanguageParseException("A method with the name '" + f.getName() + "' already exists in '" + getName() + "', so a field cannot be defined with the same name", f.getLexicalPhrase());
      }
      f.setGlobalVariable(new GlobalVariable(f, this));
      fields.put(f.getName(), f);
    }
    for (Constructor constructor : newConstructors)
    {
      if (!constructor.getName().equals(getName()))
      {
        throw new LanguageParseException("The constructor '" + constructor.getName() + "' should be called '" + getName() + "' after the class it is defined in", constructor.getLexicalPhrase());
      }
      constructor.setContainingTypeDefinition(this);
      constructors.add(constructor);
    }
    for (Method method : newMethods)
    {
      if (fields.containsKey(method.getName()))
      {
        throw new LanguageParseException("A field with the name '" + method.getName() + "' already exists in '" + getName() + "', so a method cannot be defined with the same name", method.getLexicalPhrase());
      }
      Set<Method> methodSet = methods.get(method.getName());
      if (methodSet == null)
      {
        methodSet = new HashSet<Method>();
        methods.put(method.getName(), methodSet);
      }
      method.setContainingTypeDefinition(this);
      methodSet.add(method);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Initialiser[] getInitialisers()
  {
    return initialisers.toArray(new Initialiser[initialisers.size()]);
  }

  /**
   * @return the fields
   */
  @Override
  public Field[] getFields()
  {
    Field[] fieldArray = new Field[fields.size()];
    Iterator<Field> it = fields.values().iterator();
    int i = 0;
    while (it.hasNext())
    {
      fieldArray[i] = it.next();
      i++;
    }
    return fieldArray;
  }

  /**
   * @return the non-static fields, in order of their indices
   */
  @Override
  public Field[] getNonStaticFields()
  {
    return nonStaticFields;
  }

  /**
   * @param name - the name of the field to get
   * @return the Field with the specified name, or null if none exists
   */
  @Override
  public Field getField(String name)
  {
    return fields.get(name);
  }

  /**
   * @return the constructors of this ClassDefinition
   */
  @Override
  public Collection<Constructor> getConstructors()
  {
    return constructors;
  }

  /**
   * @return an array containing all of the methods in this ClassDefinition
   */
  @Override
  public Method[] getAllMethods()
  {
    int size = 0;
    for (Set<Method> methodSet : methods.values())
    {
      size += methodSet.size();
    }
    Method[] methodArray = new Method[size];
    int index = 0;
    for (Set<Method> methodSet : methods.values())
    {
      for (Method method : methodSet)
      {
        methodArray[index] = method;
        index++;
      }
    }
    return methodArray;
  }

  /**
   * @param name - the name to get the methods with
   * @return the set of methods with the specified name
   */
  @Override
  public Set<Method> getMethodsByName(String name)
  {
    return methods.get(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "class " + getName() + "\n" + getBodyString() + "\n";
  }
}