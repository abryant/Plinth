package eu.bryants.anthony.plinth.ast;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.metadata.FieldInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.GlobalVariable;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunction;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunctionType;
import eu.bryants.anthony.plinth.ast.metadata.MemberVariable;
import eu.bryants.anthony.plinth.ast.metadata.OverrideFunction;
import eu.bryants.anthony.plinth.ast.metadata.PropertyInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.VirtualFunction;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.compiler.passes.InheritanceChecker;
import eu.bryants.anthony.plinth.parser.LanguageParseException;

/*
 * Created on 9 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class ClassDefinition extends TypeDefinition
{

  private TypeParameter[] typeParameters;
  private NamedType superType;
  private NamedType[] superInterfaceTypes;

  private List<Initialiser> initialisers = new LinkedList<Initialiser>();
  // fields need a guaranteed order, so use a LinkedHashMap to store them
  private Map<String, Field> fields = new LinkedHashMap<String, Field>();
  private Map<String, Property> properties = new LinkedHashMap<String, Property>();
  private Set<Constructor> constructors = new HashSet<Constructor>();
  private Map<String, Set<Method>> methods = new HashMap<String, Set<Method>>();

  private MemberVariable[] memberVariables;
  private VirtualFunction[] virtualFunctions;

  /**
   * Creates a new ClassDefinition with the specified members.
   * @param isAbstract - true if this class definition should be abstract, false otherwise
   * @param isImmutable - true if this class definition should be immutable, false otherwise
   * @param name - the name of the class definition
   * @param typeParameters - the TypeParameters for this type (or an empty array if there are none)
   * @param superType - the NamedType representing the superclass (and any type arguments it may have), or null if there is no superclass
   * @param superInterfaceTypes - the NamedTypes of all interfaces that this class implements (and any type arguments they may have), or null if it does not implement any
   * @param members - the list of Members of this ClassDefinition
   * @param lexicalPhrase - the LexicalPhrase of this ClassDefinition
   * @throws LanguageParseException - if there is a name collision between any of the Members
   */
  public ClassDefinition(boolean isAbstract, boolean isImmutable, String name, TypeParameter[] typeParameters, NamedType superType, NamedType[] superInterfaceTypes, Member[] members, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    super(isAbstract, isImmutable, name, lexicalPhrase);
    this.typeParameters = typeParameters;
    Set<String> typeParameterNames = new HashSet<String>();
    for (TypeParameter t : typeParameters)
    {
      t.setContainingTypeDefinition(this);
      if (!typeParameterNames.add(t.getName()))
      {
        throw new LanguageParseException("Duplicate type parameter: " + t.getName(), t.getLexicalPhrase());
      }
    }
    this.superType = superType;
    this.superInterfaceTypes = superInterfaceTypes;
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
        field.setContainingTypeDefinition(this);
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
        if (properties.containsKey(method.getName()))
        {
          throw new LanguageParseException("A property with the name '" + method.getName() + "' already exists in '" + name + "', so a method cannot be defined with the same name", method.getLexicalPhrase());
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
   * Creates a new ClassDefinition with the specified members.
   * @param isAbstract - true if this class definition should be abstract, false otherwise
   * @param isImmutable - true if this class definition should be immutable, false otherwise
   * @param qname - the qualified name of the class definition
   * @param typeParameters - the TypeParameters for this type (or an empty array if there are none)
   * @param superType - the NamedType representing the superclass (and any type arguments it may have), or null if there is no superclass
   * @param superInterfaceTypes - the NamedTypes of all interfaces that this class implements (and any type arguments they may have), or null if it does not implement any
   * @param newFields - the fields, with their variables already filled in
   * @param newProperties - the properties, with their backing variables and MemberFunctions already filled in
   * @param newConstructors - the constructors
   * @param newMethods - the methods, with their MemberFunctions already filled in
   * @param memberVariables - the MemberVariables for each of the non-static variables in this class
   * @param virtualFunctions - the VirtualFunctions for each of the non-static functions in this class
   * @throws LanguageParseException - if there is a name collision between any of the methods
   */
  public ClassDefinition(boolean isAbstract, boolean isImmutable, QName qname, TypeParameter[] typeParameters, NamedType superType, NamedType[] superInterfaceTypes,
                         Field[] newFields, Property[] newProperties, Constructor[] newConstructors, Method[] newMethods,
                         MemberVariable[] memberVariables, VirtualFunction[] virtualFunctions) throws LanguageParseException
  {
    super(isAbstract, isImmutable, qname.getLastName(), null);
    setQualifiedName(qname);
    this.typeParameters = typeParameters;
    this.superType = superType;
    this.superInterfaceTypes = superInterfaceTypes;
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
      field.setContainingTypeDefinition(this);
      // we assume that the fields' variables have already been filled in
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
      property.setContainingTypeDefinition(this);
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
      method.setContainingTypeDefinition(this);
      Set<Method> methodSet = methods.get(method.getName());
      if (methodSet == null)
      {
        methodSet = new HashSet<Method>();
        methods.put(method.getName(), methodSet);
      }
      methodSet.add(method);
    }
    this.memberVariables = memberVariables;
    this.virtualFunctions = virtualFunctions;
  }

  /**
   * @return the typeParameters
   */
  @Override
  public TypeParameter[] getTypeParameters()
  {
    return typeParameters;
  }

  /**
   * @return the superType
   */
  public NamedType getSuperType()
  {
    return superType;
  }

  /**
   * @return the superInterfaceTypes
   */
  public NamedType[] getSuperInterfaceTypes()
  {
    return superInterfaceTypes;
  }

  /**
   * Builds the virtual function table for this ClassDefinition.
   */
  public void buildVirtualFunctions()
  {
    List<OverrideFunction> overrideFunctions = null;
    if (!isAbstract())
    {
      overrideFunctions = InheritanceChecker.getOverrideVirtualFunctions(this);
    }
    virtualFunctions = buildVirtualFunctionList(getAllMethods(), properties.values(), overrideFunctions);
  }

  /**
   * @return the memberVariables
   */
  public MemberVariable[] getMemberVariables()
  {
    return memberVariables;
  }

  /**
   * @return the virtual functions of this ClassDefinition, in order of their intended position in the final VFT
   */
  public VirtualFunction[] getVirtualFunctions()
  {
    return virtualFunctions;
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
   * @return the constructors of this ClassDefinition
   */
  @Override
  public Collection<Constructor> getAllConstructors()
  {
    return constructors;
  }

  /**
   * @return an array containing all of the methods in this ClassDefinition
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
   * @return the mangled name of the allocator of this type definition
   */
  public String getAllocatorMangledName()
  {
    return "_A" + getQualifiedName().getMangledName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    if (isAbstract())
    {
      buffer.append("abstract ");
    }
    if (isImmutable())
    {
      buffer.append("immutable ");
    }
    buffer.append("class ");
    buffer.append(getName());
    if (typeParameters.length > 0)
    {
      buffer.append('<');
      for (int i = 0; i < typeParameters.length; ++i)
      {
        buffer.append(typeParameters[i]);
        if (i != typeParameters.length - 1)
        {
          buffer.append(", ");
        }
      }
      buffer.append('>');
    }
    if (superType != null)
    {
      buffer.append(" extends ");
      buffer.append(superType);
    }
    if (superInterfaceTypes != null)
    {
      buffer.append(" implements ");
      for (int i = 0; i < superInterfaceTypes.length; ++i)
      {
        buffer.append(superInterfaceTypes[i]);
        if (i != superInterfaceTypes.length - 1)
        {
          buffer.append(", ");
        }
      }
    }
    buffer.append('\n');
    buffer.append(getBodyString());
    buffer.append('\n');
    return buffer.toString();
  }
}
