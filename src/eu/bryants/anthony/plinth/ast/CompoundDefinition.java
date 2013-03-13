package eu.bryants.anthony.plinth.ast;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Initialiser;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.metadata.FieldInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.GlobalVariable;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunction;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunction.MemberFunctionType;
import eu.bryants.anthony.plinth.ast.metadata.MemberVariable;
import eu.bryants.anthony.plinth.ast.metadata.PropertyInitialiser;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.parser.LanguageParseException;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class CompoundDefinition extends TypeDefinition
{

  private List<Initialiser> initialisers = new LinkedList<Initialiser>();
  // fields need a guaranteed order, so use a LinkedHashMap to store them
  private Map<String, Field> fields = new LinkedHashMap<String, Field>();
  private Map<String, Property> properties = new LinkedHashMap<String, Property>();
  private Set<Constructor> constructors = new HashSet<Constructor>();
  private Map<String, Set<Method>> methods = new HashMap<String, Set<Method>>();

  private MemberVariable[] memberVariables;
  private MemberFunction[] memberFunctions;

  /**
   * Creates a new CompoundDefinition with the specified members.
   * @param isImmutable - true if this CompoundDefinition should be immutable, false otherwise
   * @param name - the name of the CompoundDefinition
   * @param members - the array of all of the members
   * @param lexicalPhrase - the LexicalPhrase of this CompoundDefinition
   * @throws LanguageParseException - if there is a name collision between any of the methods, or a Constructor's name is wrong
   */
  public CompoundDefinition(boolean isImmutable, String name, Member[] members, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    super(false, isImmutable, name, lexicalPhrase);
    // add all of the members by name
    Set<Method> allMethods = new HashSet<Method>();
    for (Member member : members)
    {
      if (member instanceof Initialiser)
      {
        initialisers.add((Initialiser) member);
      }
      else if (member instanceof Field)
      {
        Field field = (Field) member;
        if (fields.containsKey(field.getName()))
        {
          throw new LanguageParseException("A field with the name '" + field.getName() + "' already exists in '" + name + "', so another field cannot be defined with the same name", field.getLexicalPhrase());
        }
        if (properties.containsKey(field.getName()))
        {
          throw new LanguageParseException("A property with the name '" + field.getName() + "' already exists in '" + name + "', so a field cannot be defined with the same name", field.getLexicalPhrase());
        }
        if (methods.containsKey(field.getName()))
        {
          throw new LanguageParseException("A method with the name '" + field.getName() + "' already exists in '" + name + "', so a field cannot be defined with the same name", field.getLexicalPhrase());
        }
        if (field.isStatic())
        {
          GlobalVariable globalVariable = new GlobalVariable(field);
          globalVariable.setEnclosingTypeDefinition(this);
          field.setGlobalVariable(globalVariable);
        }
        else
        {
          MemberVariable memberVariable = new MemberVariable(field);
          memberVariable.setEnclosingTypeDefinition(this);
          field.setMemberVariable(memberVariable);
        }
        fields.put(field.getName(), field);
        if (field.getInitialiserExpression() != null)
        {
          initialisers.add(new FieldInitialiser(field));
        }
      }
      else if (member instanceof Property)
      {
        Property property = (Property) member;
        if (fields.containsKey(property.getName()))
        {
          throw new LanguageParseException("A field with the name '" + property.getName() + "' already exists in '" + name + "', so a property cannot be defined with the same name", property.getLexicalPhrase());
        }
        if (properties.containsKey(property.getName()))
        {
          throw new LanguageParseException("A property with the name '" + property.getName() + "' already exists in '" + name + "', so another property cannot be defined with the same name", property.getLexicalPhrase());
        }
        if (methods.containsKey(property.getName()))
        {
          throw new LanguageParseException("A method with the name '" + property.getName() + "' already exists in '" + name + "', so a property cannot be defined with the same name", property.getLexicalPhrase());
        }
        if (property.isAbstract())
        {
          // abstract properties are automatically unbacked
          // (even though abstract properties will be disallowed for a compound type,
          // set it to unbacked here for consistency with other types)
          property.setUnbacked(true);
        }
        if (!property.isUnbacked())
        {
          if (property.isStatic())
          {
            GlobalVariable globalVariable = new GlobalVariable(property);
            globalVariable.setEnclosingTypeDefinition(this);
            property.setBackingGlobalVariable(globalVariable);
          }
          else
          {
            MemberVariable memberVariable = new MemberVariable(property);
            memberVariable.setEnclosingTypeDefinition(this);
            property.setBackingMemberVariable(memberVariable);
          }
        }
        if (!property.isStatic())
        {
          property.setGetterMemberFunction(new MemberFunction(property, MemberFunctionType.PROPERTY_GETTER));
          if (!property.isFinal())
          {
            property.setSetterMemberFunction(new MemberFunction(property, MemberFunctionType.PROPERTY_SETTER));
          }
          if (property.hasConstructor())
          {
            property.setConstructorMemberFunction(new MemberFunction(property, MemberFunctionType.PROPERTY_CONSTRUCTOR));
          }
        }
        property.setContainingTypeDefinition(this);
        properties.put(property.getName(), property);
        if (property.getInitialiserExpression() != null)
        {
          initialisers.add(new PropertyInitialiser(property));
        }
      }
      else if (member instanceof Constructor)
      {
        Constructor constructor = (Constructor) member;
        constructor.setContainingTypeDefinition(this);
        if (isImmutable)
        {
          constructor.setImmutable(true);
        }
        constructors.add(constructor);
      }
      else if (member instanceof Method)
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
          method.setMemberFunction(new MemberFunction(method));
        }
        methodSet.add(method);
        allMethods.add(method);
      }
      else
      {
        throw new LanguageParseException("Unknown Member: " + member, member.getLexicalPhrase());
      }
    }
    memberVariables = buildMemberVariableList(fields.values(), properties.values());
  }

  /**
   * Creates a new CompoundDefinition with the specified members.
   * @param isImmutable - true if this CompoundDefinition should be immutable, false otherwise
   * @param qname - the qualified name of the compound definition
   * @param newFields - the fields, with their variables already filled in
   * @param newProperties - the properties, with their backing variables and MemberFunctions already filled in
   * @param newConstructors - the constructors
   * @param newMethods - the methods, with their MemberFunctions already filled in
   * @param memberVariables - the MemberVariables for each of the non-static variables in this compound type
   * @param memberFunctions - the MemberFunctions for each of the non-static functions in this compound type
   * @throws LanguageParseException - if there is a name collision between any of the methods, or a Constructor's name is wrong
   */
  public CompoundDefinition(boolean isImmutable, QName qname, Field[] newFields, Property[] newProperties, Constructor[] newConstructors, Method[] newMethods,
                            MemberVariable[] memberVariables, MemberFunction[] memberFunctions) throws LanguageParseException
  {
    super(false, isImmutable, qname.getLastName(), null);
    setQualifiedName(qname);
    for (Field field : newFields)
    {
      if (fields.containsKey(field.getName()))
      {
        throw new LanguageParseException("A field with the name '" + field.getName() + "' already exists in '" + getName() + "', so another field cannot be defined with the same name", field.getLexicalPhrase());
      }
      if (properties.containsKey(field.getName()))
      {
        throw new LanguageParseException("A property with the name '" + field.getName() + "' already exists in '" + getName() + "', so a field cannot be defined with the same name", field.getLexicalPhrase());
      }
      if (methods.containsKey(field.getName()))
      {
        throw new LanguageParseException("A method with the name '" + field.getName() + "' already exists in '" + getName() + "', so a field cannot be defined with the same name", field.getLexicalPhrase());
      }
      // we assume that the fields' indices have already been filled in
      fields.put(field.getName(), field);
    }
    for (Property property : newProperties)
    {
      if (fields.containsKey(property.getName()))
      {
        throw new LanguageParseException("A field with the name '" + property.getName() + "' already exists in '" + getName() + "', so a property cannot be defined with the same name", property.getLexicalPhrase());
      }
      if (properties.containsKey(property.getName()))
      {
        throw new LanguageParseException("A property with the name '" + property.getName() + "' already exists in '" + getName() + "', so another property cannot be defined with the same name", property.getLexicalPhrase());
      }
      if (methods.containsKey(property.getName()))
      {
        throw new LanguageParseException("A method with the name '" + property.getName() + "' already exists in '" + getName() + "', so a property cannot be defined with the same name", property.getLexicalPhrase());
      }
      properties.put(property.getName(), property);
    }
    for (Constructor constructor : newConstructors)
    {
      constructor.setContainingTypeDefinition(this);
      constructors.add(constructor);
    }
    for (Method method : newMethods)
    {
      if (fields.containsKey(method.getName()))
      {
        throw new LanguageParseException("A field with the name '" + method.getName() + "' already exists in '" + getName() + "', so a method cannot be defined with the same name", method.getLexicalPhrase());
      }
      if (properties.containsKey(method.getName()))
      {
        throw new LanguageParseException("A property with the name '" + method.getName() + "' already exists in '" + getName() + "', so a method cannot be defined with the same name", method.getLexicalPhrase());
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
    this.memberVariables = memberVariables;
    this.memberFunctions = memberFunctions;
  }

  /**
   * Adds all of the builtin methods which are not explicitly implemented.
   */
  private void addBuiltinMethods()
  {
    for (BuiltinMethod builtinMethod : ObjectType.OBJECT_METHODS)
    {
      Set<Method> methodSet = methods.get(builtinMethod.getName());
      if (methodSet == null)
      {
        methodSet = new HashSet<Method>();
        methods.put(builtinMethod.getName(), methodSet);
      }
      boolean found = false;
      for (Method m : methodSet)
      {
        if (m.getDisambiguator().matches(builtinMethod.getDisambiguator()))
        {
          found = true;
          break;
        }
      }
      if (!found)
      {
        BuiltinMethod customBuiltin = new BuiltinMethod(new NamedType(false, isImmutable(), this), builtinMethod.getBuiltinType());
        customBuiltin.setContainingTypeDefinition(this);
        if (!customBuiltin.isStatic())
        {
          customBuiltin.setMemberFunction(new MemberFunction(customBuiltin));
        }
        methodSet.add(customBuiltin);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void buildMemberFunctions()
  {
    // add any builtin methods which were not overridden by the user
    addBuiltinMethods();
    memberFunctions = buildMemberFunctionList(getAllMethods(), properties.values());
  }

  /**
   * @return the memberVariables
   */
  public MemberVariable[] getMemberVariables()
  {
    return memberVariables;
  }

  /**
   * @return the memberFunctions
   */
  @Override
  public MemberFunction[] getMemberFunctions()
  {
    return memberFunctions;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Initialiser> getInitialisers()
  {
    return initialisers;
  }

  /**
   * @return the fields
   */
  @Override
  public Collection<Field> getFields()
  {
    return fields.values();
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
   * {@inheritDoc}
   */
  @Override
  public Collection<Property> getProperties()
  {
    return properties.values();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Property getProperty(String name)
  {
    return properties.get(name);
  }

  /**
   * @return the constructors of this CompoundDefinition
   */
  @Override
  public Collection<Constructor> getAllConstructors()
  {
    return constructors;
  }

  /**
   * @return an array containing all of the methods in this CompoundDefinition
   */
  @Override
  public Collection<Method> getAllMethods()
  {
    Set<Method> allMethods = new HashSet<Method>();
    for (Set<Method> methodSet : methods.values())
    {
      allMethods.addAll(methodSet);
    }
    return allMethods;
  }

  /**
   * @param name - the name to get the methods with
   * @return the set of methods with the specified name
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
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return (isImmutable() ? "immutable " : "") + "compound " + getName() + "\n" + getBodyString() + "\n";
  }
}
