package eu.bryants.anthony.plinth.ast;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import eu.bryants.anthony.plinth.ast.metadata.PropertyInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.VirtualFunction;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.parser.LanguageParseException;

/*
 * Created on 9 Jan 2013
 */

/**
 * @author Anthony Bryant
 */
public class InterfaceDefinition extends TypeDefinition
{
  private TypeParameter[] typeParameters;
  private NamedType[] superInterfaceTypes;

  private List<Initialiser> initialisers = new LinkedList<Initialiser>();
  private Map<String, Field> fields = new LinkedHashMap<String, Field>();
  private Map<String, Property> properties = new LinkedHashMap<String, Property>();
  private Map<String, Set<Method>> methods = new LinkedHashMap<String, Set<Method>>();

  private VirtualFunction[] virtualFunctions;

  /**
   * Creates a new InterfaceDefinition with the specified members.
   * @param isImmutable - true if this interface definition should be immutable, false otherwise
   * @param name - the name of the interface definition
   * @param typeParameters - the TypeParameters for this type (or an empty array if there are none)
   * @param superInterfaceTypes - the NamedTypes of all super-interfaces of this interface (and any type arguments they may have), or null if there are no super-interfaces
   * @param members - the list of Members of this InterfaceDefinition
   * @param lexicalPhrase - the LexicalPhrase of this InterfaceDefinition
   * @throws LanguageParseException - if there is a name collision between any of the Members, or a conceptual problem with any of them belonging to an interface
   */
  public InterfaceDefinition(boolean isImmutable, String name, TypeParameter[] typeParameters, NamedType[] superInterfaceTypes, Member[] members, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    super(true, isImmutable, name, lexicalPhrase);
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
    this.superInterfaceTypes = superInterfaceTypes;
    // add all of the members by name
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
        if (!field.isStatic())
        {
          throw new LanguageParseException("Interfaces cannot store (non-static) data", field.getLexicalPhrase());
        }
        field.setContainingTypeDefinition(this);
        GlobalVariable globalVariable = new GlobalVariable(field);
        globalVariable.setEnclosingTypeDefinition(this);
        field.setGlobalVariable(globalVariable);
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
        if (!property.isStatic() && property.getGetterBlock() == null && property.getSetterBlock() == null && property.getConstructorBlock() == null)
        {
          // non-static interface properties with no implementation are automatically abstract
          property.setAbstract(true);
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
            throw new LanguageParseException("Interfaces cannot store (non-static) data - should this property be unbacked?", property.getLexicalPhrase());
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
          if (!property.isStatic())
          {
            throw new LanguageParseException("Interfaces cannot contain non-static initialisers", property.getInitialiserExpression().getLexicalPhrase());
          }
          initialisers.add(new PropertyInitialiser(property));
        }
      }
      else if (member instanceof Constructor)
      {
        throw new LanguageParseException("Interfaces cannot have constructors", member.getLexicalPhrase());
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
          methodSet = new LinkedHashSet<Method>();
          methods.put(method.getName(), methodSet);
        }
        method.setContainingTypeDefinition(this);
        if (isImmutable && !method.isStatic())
        {
          method.setImmutable(true);
        }
        if (!method.isStatic())
        {
          if (method.getBlock() == null && method.getNativeName() == null)
          {
            // all non-static interface methods without implementations are implicitly abstract
            method.setAbstract(true);
          }
          if (typeParameters.length > 0 && method.getNativeName() != null && method.getBlock() != null)
          {
            throw new LanguageParseException("Non-static methods on generic interfaces cannot be native up-calls", method.getLexicalPhrase());
          }
          method.setMemberFunction(new MemberFunction(method));
        }
        methodSet.add(method);
      }
      else
      {
        throw new LanguageParseException("Unknown Member: " + member, member.getLexicalPhrase());
      }
    }
  }

  /**
   * Creates a new InterfaceDefinition with the specified members.
   * @param isImmutable - true if this interface definition should be immutable, false otherwise
   * @param qname - the qualified name of the interface definition
   * @param typeParameters - the TypeParameters for this type (or an empty array if there are none)
   * @param superInterfaceTypes - the NamedTypes of all super-interfaces of this interface (and any type arguments they may have), or null if there are no super-interfaces
   * @param newFields - the fields, with their variables already filled in
   * @param newProperties - the properties, with their backing variables and MemberFunctions already filled in
   * @param newMethods - the methods, with their MemberFunctions already filled in
   * @param virtualFunctions - the VirtualFunctions for each of the non-static functions in this interface
   * @throws LanguageParseException - if there is a name collision between any of the methods
   */
  public InterfaceDefinition(boolean isImmutable, QName qname, TypeParameter[] typeParameters, NamedType[] superInterfaceTypes, Field[] newFields, Property[] newProperties, Method[] newMethods,
                             VirtualFunction[] virtualFunctions) throws LanguageParseException
  {
    super(true, isImmutable, qname.getLastName(), null);
    setQualifiedName(qname);
    this.typeParameters = typeParameters;
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
      if (!field.isStatic())
      {
        throw new LanguageParseException("An interface cannot store (non-static) data", field.getLexicalPhrase());
      }
      field.setContainingTypeDefinition(this);
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
      if (!property.isStatic() && !property.isUnbacked())
      {
        throw new LanguageParseException("An interface cannot store (non-static) data - should this property be unbacked?", property.getLexicalPhrase());
      }
      property.setContainingTypeDefinition(this);
      properties.put(property.getName(), property);
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
      if (!method.isStatic() && typeParameters.length > 0 && method.getNativeName() != null && method.getBlock() != null)
      {
        throw new LanguageParseException("Non-static methods on generic interfaces cannot be native up-calls", method.getLexicalPhrase());
      }
      Set<Method> methodSet = methods.get(method.getName());
      if (methodSet == null)
      {
        methodSet = new LinkedHashSet<Method>();
        methods.put(method.getName(), methodSet);
      }
      methodSet.add(method);
    }
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
   * @return the superInterfaceTypes
   */
  public NamedType[] getSuperInterfaceTypes()
  {
    return superInterfaceTypes;
  }

  /**
   * Builds the virtual function table for this InterfaceDefinition.
   */
  public void buildVirtualFunctions()
  {
    virtualFunctions = buildVirtualFunctionList(getAllMethods(), properties.values(), null);
  }

  /**
   * @return the virtual functions of this InterfaceDefinition, in order of their intended position in the VFT
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
   * {@inheritDoc}
   */
  @Override
  public Collection<Field> getFields()
  {
    return fields.values();
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
  public Collection<Method> getAllMethods()
  {
    Set<Method> allMethods = new LinkedHashSet<Method>();
    for (Set<Method> methodSet : methods.values())
    {
      allMethods.addAll(methodSet);
    }
    return allMethods;
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
    if (superInterfaceTypes != null)
    {
      buffer.append(" extends ");
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
