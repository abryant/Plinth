package eu.bryants.anthony.plinth.ast;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.LanguageParseException;

/*
 * Created on 9 Jan 2013
 */

/**
 * @author Anthony Bryant
 */
public class InterfaceDefinition extends TypeDefinition
{

  private QName[] superInterfaceQNames;

  private List<Initialiser> initialisers = new LinkedList<Initialiser>();
  private Map<String, Field> fields = new HashMap<String, Field>();
  private Map<String, Set<Method>> methods = new HashMap<String, Set<Method>>();

  private Method[] nonStaticMethods;

  private InterfaceDefinition[] superInterfaceDefinitions;

  /**
   * Creates a new InterfaceDefinition with the specified members.
   * @param isImmutable - true if this interface definition should be immutable, false otherwise
   * @param name - the name of the interface definition
   * @param superInterfaceQNames - the qualified names of all of the super-interfaces of this interface, or null if there are no super-interfaces
   * @param members - the list of Members of this InterfaceDefinition
   * @param lexicalPhrase - the LexicalPhrase of this InterfaceDefinition
   * @throws LanguageParseException - if there is a name collision between any of the Members, or a conceptual problem with any of them belonging to an interface
   */
  public InterfaceDefinition(boolean isImmutable, String name, QName[] superInterfaceQNames, Member[] members, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    super(true, isImmutable, name, lexicalPhrase);
    this.superInterfaceQNames = superInterfaceQNames;
    // add all of the members by name
    Set<Method> allMethods = new HashSet<Method>();
    for (Member member : members)
    {
      if (member instanceof Initialiser)
      {
        if (!((Initialiser) member).isStatic())
        {
          throw new LanguageParseException("Interfaces cannot contain non-static initialisers", member.getLexicalPhrase());
        }
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
        if (!field.isStatic())
        {
          throw new LanguageParseException("Interfaces cannot store (non-static) data", field.getLexicalPhrase());
        }
        field.setGlobalVariable(new GlobalVariable(field, this));
        fields.put(field.getName(), field);
        if (field.getInitialiserExpression() != null)
        {
          initialisers.add(new FieldInitialiser(field));
        }
      }
      if (member instanceof Constructor)
      {
        throw new LanguageParseException("Interfaces cannot have constructors", member.getLexicalPhrase());
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
        if (isImmutable && !method.isStatic())
        {
          method.setImmutable(true);
        }
        if (!method.isStatic())
        {
          // all non-static interface methods without implementations are implicitly abstract
          boolean hasImplementation = method.getBlock() != null || method.getNativeName() != null;
          method.setAbstract(!hasImplementation);
        }
        methodSet.add(method);
        allMethods.add(method);
      }
    }
  }

  /**
   * Creates a new InterfaceDefinition with the specified members.
   * @param isImmutable - true if this interface definition should be immutable, false otherwise
   * @param qname - the qualified name of the interface definition
   * @param superInterfaceQNames - the qualified names of all of the super-interfaces of this interface, or null if there are no super-interfaces
   * @param staticFields - the static fields
   * @param nonStaticMethods - the non-static methods, with their indexes already filled in
   * @param staticMethods - the static methods
   * @throws LanguageParseException - if there is a name collision between any of the methods
   */
  public InterfaceDefinition(boolean isImmutable, QName qname, QName[] superInterfaceQNames, Field[] staticFields, Method[] nonStaticMethods, Method[] staticMethods) throws LanguageParseException
  {
    super(true, isImmutable, qname.getLastName(), null);
    setQualifiedName(qname);
    this.superInterfaceQNames = superInterfaceQNames;
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
    for (Method method : nonStaticMethods)
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
    this.nonStaticMethods = nonStaticMethods;
    for (Method method : staticMethods)
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
   * @return the superInterfaceQNames
   */
  public QName[] getSuperInterfaceQNames()
  {
    return superInterfaceQNames;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void buildNonStaticMethods()
  {
    nonStaticMethods = buildNonStaticMethodList();
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
   * {@inheritDoc}
   */
  @Override
  public Field[] getFields()
  {
    return fields.values().toArray(new Field[fields.size()]);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Field[] getNonStaticFields()
  {
    // interfaces have no non-static fields
    return new Field[0];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Field[] getStaticFields()
  {
    return getFields();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Field getField(String name)
  {
    return fields.get(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Constructor> getAllConstructors()
  {
    // interfaces have no constructors
    return new HashSet<Constructor>();
  }

  /**
   * {@inheritDoc}
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
   * {@inheritDoc}
   */
  @Override
  public Method[] getNonStaticMethods()
  {
    return nonStaticMethods;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Method[] getStaticMethods()
  {
    Method[] allMethods = getAllMethods();
    Method[] staticMethods = new Method[allMethods.length - nonStaticMethods.length];
    int index = 0;
    for (Method m : allMethods)
    {
      if (m.isStatic())
      {
        staticMethods[index] = m;
        ++index;
      }
    }
    return staticMethods;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Method> getMethodsByName(String name)
  {
    Set<Method> result = methods.get(name);
    if (result == null)
    {
      return new HashSet<Method>();
    }
    return result;
  }

  /**
   * @return the superInterfaceDefinitions
   */
  public InterfaceDefinition[] getSuperInterfaceDefinitions()
  {
    return superInterfaceDefinitions;
  }

  /**
   * @param superInterfaceDefinitions - the superInterfaceDefinitions to set
   */
  public void setSuperInterfaceDefinitions(InterfaceDefinition[] superInterfaceDefinitions)
  {
    this.superInterfaceDefinitions = superInterfaceDefinitions;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    if (isImmutable())
    {
      buffer.append("immutable ");
    }
    buffer.append("interface ");
    buffer.append(getName());
    buffer.append('\n');
    buffer.append(getBodyString());
    buffer.append('\n');
    return buffer.toString();
  }
}
