package eu.bryants.anthony.plinth.compiler.passes.llvm;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import nativelib.c.C;
import nativelib.c.C.PointerConverter;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMValueRef;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.metadata.GlobalVariable;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunction;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunctionType;
import eu.bryants.anthony.plinth.ast.metadata.MemberVariable;
import eu.bryants.anthony.plinth.ast.metadata.VirtualFunction;
import eu.bryants.anthony.plinth.ast.misc.NormalParameter;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.ast.type.WildcardType;
import eu.bryants.anthony.plinth.compiler.ConceptualException;
import eu.bryants.anthony.plinth.parser.LanguageParseException;

/*
 * Created on 29 Aug 2012
 */

/**
 * @author Anthony Bryant
 */
public class MetadataLoader
{

  /**
   * Loads all TypeDefinitions from the specified module.
   * @param module - the module to load the TypeDefinitions from the metadata of.
   * @return the list of TypeDefinitions loaded
   * @throws MalformedMetadataException - if the bitcode file is malformed in some way(e.g. it doesn't have valid metadata)
   */
  public static List<TypeDefinition> loadTypeDefinitions(LLVMModuleRef module) throws MalformedMetadataException
  {
    LLVMValueRef[] classDefinitionNodes = readNamedMetadataOperands(module, "ClassDefinitions");
    LLVMValueRef[] compoundDefinitionNodes = readNamedMetadataOperands(module, "CompoundDefinitions");
    LLVMValueRef[] interfaceDefinitionNodes = readNamedMetadataOperands(module, "InterfaceDefinitions");

    List<TypeDefinition> results = new LinkedList<TypeDefinition>();
    for (LLVMValueRef classDefinitionNode : classDefinitionNodes)
    {
      results.add(loadClassDefinition(classDefinitionNode));
    }
    for (LLVMValueRef compoundDefinitionNode : compoundDefinitionNodes)
    {
      results.add(loadCompoundDefinition(compoundDefinitionNode));
    }
    for (LLVMValueRef interfaceDefinitionNode : interfaceDefinitionNodes)
    {
      results.add(loadInterfaceDefinition(interfaceDefinitionNode));
    }
    return results;
  }

  private static ClassDefinition loadClassDefinition(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    metadataNode = LLVM.LLVMIsAMDNode(metadataNode);
    if (metadataNode == null)
    {
      throw new MalformedMetadataException("A class definition must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 10)
    {
      throw new MalformedMetadataException("A class definition's metadata node must have the correct number of sub-nodes");
    }

    String qualifiedNameStr = readMDString(values[0]);
    if (qualifiedNameStr == null)
    {
      throw new MalformedMetadataException("A class definition must begin with a fully qualified name");
    }
    QName qname;
    try
    {
      qname = new QName(qualifiedNameStr);
    }
    catch (ConceptualException e)
    {
      throw new MalformedMetadataException(e.getMessage(), e);
    }

    boolean isAbstract = readBooleanValue(values[1], "class definition", "abstract");
    boolean isImmutable = readBooleanValue(values[2], "class definition", "immutable");

    TypeParameter[] typeParameters = loadTypeParameters(values[3]);

    Type superType = loadType(values[4]);
    if (!(superType instanceof NamedType || superType instanceof ObjectType))
    {
      throw new MalformedMetadataException("A super-type must either be a named type or the object type");
    }
    NamedType superNamedType = superType instanceof NamedType ? (NamedType) superType : null;
    NamedType[] superInterfaceTypes = loadSuperInterfaces(values[5]);

    Field[] fields = loadFields(values[6]);
    Property[] properties = loadProperties(values[7]);
    Constructor[] constructors = loadConstructors(values[8]);
    Method[] methods = loadMethods(values[9]);

    MemberVariable[] memberVariables = processMemberVariables(fields, properties);
    VirtualFunction[] virtualFunctions = processVirtualFunctions(properties, methods);
    Collection<GlobalVariable> globalVariables = extractGlobalVariables(fields, properties);

    try
    {
      ClassDefinition result = new ClassDefinition(isAbstract, isImmutable, qname, typeParameters, superNamedType, superInterfaceTypes, fields, properties, constructors, methods, memberVariables, virtualFunctions);
      for (TypeParameter typeParameter : typeParameters)
      {
        typeParameter.setContainingTypeDefinition(result);
      }
      for (MemberVariable memberVariable : memberVariables)
      {
        memberVariable.setEnclosingTypeDefinition(result);
      }
      for (GlobalVariable globalVariable : globalVariables)
      {
        globalVariable.setEnclosingTypeDefinition(result);
      }
      return result;
    }
    catch (LanguageParseException e)
    {
      throw new MalformedMetadataException(e.getMessage(), e);
    }
  }

  private static CompoundDefinition loadCompoundDefinition(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    metadataNode = LLVM.LLVMIsAMDNode(metadataNode);
    if (metadataNode == null)
    {
      throw new MalformedMetadataException("A compound type definition must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 7)
    {
      throw new MalformedMetadataException("A compound type definition's metadata node must have the correct number of sub-nodes");
    }

    String qualifiedNameStr = readMDString(values[0]);
    if (qualifiedNameStr == null)
    {
      throw new MalformedMetadataException("A compound type definition must begin with a fully qualified name");
    }
    QName qname;
    try
    {
      qname = new QName(qualifiedNameStr);
    }
    catch (ConceptualException e)
    {
      throw new MalformedMetadataException(e.getMessage(), e);
    }

    boolean isImmutable = readBooleanValue(values[1], "compound type definition", "immutable");

    TypeParameter[] typeParameters = loadTypeParameters(values[2]);

    Field[] fields = loadFields(values[3]);
    Property[] properties = loadProperties(values[4]);
    Constructor[] constructors = loadConstructors(values[5]);
    Method[] methods = loadMethods(values[6]);

    MemberVariable[] memberVariables = processMemberVariables(fields, properties);
    Collection<GlobalVariable> globalVariables = extractGlobalVariables(fields, properties);

    try
    {
      CompoundDefinition result = new CompoundDefinition(isImmutable, qname, typeParameters, fields, properties, constructors, methods, memberVariables);
      for (TypeParameter typeParameter : typeParameters)
      {
        typeParameter.setContainingTypeDefinition(result);
      }
      for (MemberVariable memberVariable : memberVariables)
      {
        memberVariable.setEnclosingTypeDefinition(result);
      }
      for (GlobalVariable globalVariable : globalVariables)
      {
        globalVariable.setEnclosingTypeDefinition(result);
      }
      return result;
    }
    catch (LanguageParseException e)
    {
      throw new MalformedMetadataException(e.getMessage(), e);
    }
  }

  private static InterfaceDefinition loadInterfaceDefinition(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    metadataNode = LLVM.LLVMIsAMDNode(metadataNode);
    if (metadataNode == null)
    {
      throw new MalformedMetadataException("An interface definition must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 7)
    {
      throw new MalformedMetadataException("An interface definition's metadata node must have the correct number of sub-nodes");
    }

    String qualifiedNameStr = readMDString(values[0]);
    if (qualifiedNameStr == null)
    {
      throw new MalformedMetadataException("An interface definition must begin with a fully qualified name");
    }
    QName qname;
    try
    {
      qname = new QName(qualifiedNameStr);
    }
    catch (ConceptualException e)
    {
      throw new MalformedMetadataException(e.getMessage(), e);
    }

    boolean isImmutable = readBooleanValue(values[1], "interface definition", "immutable");

    TypeParameter[] typeParameters = loadTypeParameters(values[2]);

    NamedType[] superInterfaceTypes = loadSuperInterfaces(values[3]);

    Field[] fields = loadFields(values[4]);
    Property[] properties = loadProperties(values[5]);
    Method[] methods = loadMethods(values[6]);

    VirtualFunction[] virtualFunctions = processVirtualFunctions(properties, methods);
    Collection<GlobalVariable> globalVariables = extractGlobalVariables(fields, properties);

    try
    {
      InterfaceDefinition result = new InterfaceDefinition(isImmutable, qname, typeParameters, superInterfaceTypes, fields, properties, methods, virtualFunctions);
      for (TypeParameter typeParameter : typeParameters)
      {
        typeParameter.setContainingTypeDefinition(result);
      }
      for (GlobalVariable globalVariable : globalVariables)
      {
        globalVariable.setEnclosingTypeDefinition(result);
      }
      return result;
    }
    catch (LanguageParseException e)
    {
      throw new MalformedMetadataException(e.getMessage(), e);
    }
  }

  private static MemberVariable[] processMemberVariables(Field[] fields, Property[] properties)
  {
    List<MemberVariable> memberVariables = new LinkedList<MemberVariable>();
    for (Field field : fields)
    {
      if (!field.isStatic())
      {
        memberVariables.add(field.getMemberVariable());
      }
    }
    for (Property property : properties)
    {
      if (!property.isUnbacked() && !property.isStatic())
      {
        memberVariables.add(property.getBackingMemberVariable());
      }
    }
    Collections.sort(memberVariables, new Comparator<MemberVariable>()
    {
      @Override
      public int compare(MemberVariable o1, MemberVariable o2)
      {
        return o1.getMemberIndex() - o2.getMemberIndex();
      }
    });
    return memberVariables.toArray(new MemberVariable[memberVariables.size()]);
  }

  private static Collection<GlobalVariable> extractGlobalVariables(Field[] fields, Property[] properties)
  {
    List<GlobalVariable> globalVariables = new LinkedList<GlobalVariable>();
    for (Field field : fields)
    {
      if (field.isStatic())
      {
        globalVariables.add(field.getGlobalVariable());
      }
    }
    for (Property property : properties)
    {
      if (!property.isUnbacked() && property.isStatic())
      {
        globalVariables.add(property.getBackingGlobalVariable());
      }
    }
    return globalVariables;
  }

  private static VirtualFunction[] processVirtualFunctions(Property[] properties, Method[] methods)
  {
    List<VirtualFunction> virtualFunctions = new LinkedList<VirtualFunction>();
    for (Property property : properties)
    {
      if (!property.isStatic())
      {
        virtualFunctions.add(property.getGetterMemberFunction());
        virtualFunctions.add(property.getSetterMemberFunction());
      }
    }
    for (Method method : methods)
    {
      if (!method.isStatic())
      {
        virtualFunctions.add(method.getMemberFunction());
      }
    }
    Collections.sort(virtualFunctions, new Comparator<VirtualFunction>()
    {
      @Override
      public int compare(VirtualFunction o1, VirtualFunction o2)
      {
        return o1.getIndex() - o2.getIndex();
      }
    });
    return virtualFunctions.toArray(new VirtualFunction[virtualFunctions.size()]);
  }

  private static TypeParameter[] loadTypeParameters(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A type parameter list must be represented by a metadata node");
    }
    LLVMValueRef[] nodes = readOperands(metadataNode);
    TypeParameter[] typeParameters = new TypeParameter[nodes.length];
    for (int i = 0; i < nodes.length; ++i)
    {
      typeParameters[i] = loadTypeParameter(nodes[i]);
    }
    return typeParameters;
  }

  private static TypeParameter loadTypeParameter(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A type parameter must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 3)
    {
      throw new MalformedMetadataException("A type parameter's metadata node must have the correct number of sub-nodes");
    }

    String name = readMDString(values[0]);
    if (name == null)
    {
      throw new MalformedMetadataException("A type parameter must have a valid name in its metadata node");
    }

    if (LLVM.LLVMIsAMDNode(values[1]) == null)
    {
      throw new MalformedMetadataException("A type parameter must have a valid super-type list in its metadata node");
    }
    LLVMValueRef[] superTypeNodes = readOperands(values[1]);
    Type[] superTypes = new Type[superTypeNodes.length];
    for (int i = 0; i < superTypes.length; ++i)
    {
      superTypes[i] = loadType(superTypeNodes[i]);
    }

    if (LLVM.LLVMIsAMDNode(values[2]) == null)
    {
      throw new MalformedMetadataException("A type parameter must have a valid sub-type list in its metadata node");
    }
    LLVMValueRef[] subTypeNodes = readOperands(values[2]);
    Type[] subTypes = new Type[subTypeNodes.length];
    for (int i = 0; i < subTypes.length; ++i)
    {
      subTypes[i] = loadType(subTypeNodes[i]);
    }

    return new TypeParameter(name, superTypes, subTypes, null);
  }

  private static NamedType[] loadSuperInterfaces(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A super-interface list must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length == 0)
    {
      return null;
    }
    NamedType[] types = new NamedType[values.length];
    for (int i = 0; i < types.length; ++i)
    {
      Type interfaceType = loadType(values[i]);
      if (!(interfaceType instanceof NamedType))
      {
        throw new MalformedMetadataException("A super-interface must be represented by a named type");
      }
      types[i] = (NamedType) interfaceType;
    }
    return types;
  }

  private static Field[] loadFields(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A field list must be represented by a metadata node");
    }
    LLVMValueRef[] fieldNodes = readOperands(metadataNode);
    Field[] fields = new Field[fieldNodes.length];
    for (int i = 0; i < fieldNodes.length; ++i)
    {
      fields[i] = loadField(fieldNodes[i]);
    }
    return fields;
  }

  private static Property[] loadProperties(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A property list must be represented by a metadata node");
    }
    LLVMValueRef[] propertyNodes = readOperands(metadataNode);
    Property[] properties = new Property[propertyNodes.length];
    for (int i = 0; i < propertyNodes.length; ++i)
    {
      properties[i] = loadProperty(propertyNodes[i]);
    }
    return properties;
  }

  private static Constructor[] loadConstructors(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A constructor list must be represented by a metadata node");
    }
    LLVMValueRef[] constructorNodes = readOperands(metadataNode);
    Constructor[] constructors = new Constructor[constructorNodes.length];
    for (int i = 0; i < constructorNodes.length; ++i)
    {
      constructors[i] = loadConstructor(constructorNodes[i]);
    }
    return constructors;
  }

  private static Method[] loadMethods(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A method list must be represented by a metadata node");
    }
    LLVMValueRef[] methodNodes = readOperands(metadataNode);
    Method[] methods = new Method[methodNodes.length];
    for (int i = 0; i < methodNodes.length; ++i)
    {
      methods[i] = loadMethod(methodNodes[i]);
    }
    return methods;
  }

  private static Field loadField(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A field must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 6)
    {
      throw new MalformedMetadataException("A field's metadata node must have the correct number of sub-nodes");
    }

    boolean isFinal = readBooleanValue(values[0], "field", "final");
    boolean isMutable = readBooleanValue(values[1], "field", "mutable");

    SinceSpecifier sinceSpecifier = loadSinceSpecifier(values[2]);

    String memberIndexString = readMDString(values[3]);
    if (memberIndexString == null)
    {
      throw new MalformedMetadataException("A field must either have a valid member index in its metadata node, or declare itself as static");
    }
    boolean isStatic = false;
    int memberIndex = -1;
    if (memberIndexString.equals("static"))
    {
      isStatic = true;
    }
    else
    {
      try
      {
        memberIndex = Integer.parseInt(memberIndexString);
      }
      catch (NumberFormatException e)
      {
        throw new MalformedMetadataException("A field must either have a valid member index in its metadata node, or declare itself as static", e);
      }
    }

    Type type = loadType(values[4]);
    String name = readMDString(values[5]);
    if (name == null)
    {
      throw new MalformedMetadataException("A field must have a valid name in its metadata node");
    }
    Field field = new Field(type, name, isStatic, isFinal, isMutable, sinceSpecifier, null, null);
    if (isStatic)
    {
      field.setGlobalVariable(new GlobalVariable(field));
    }
    else
    {
      MemberVariable memberVariable = new MemberVariable(field);
      memberVariable.setMemberIndex(memberIndex);
      field.setMemberVariable(memberVariable);
    }
    return field;
  }

  private static Property loadProperty(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A property must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 14)
    {
      throw new MalformedMetadataException("A property's metadata node must have the correct number of sub-nodes");
    }

    boolean isAbstract = readBooleanValue(values[0], "property", "abstract");
    boolean isFinal = readBooleanValue(values[1], "property", "final");
    boolean isMutable = readBooleanValue(values[2], "property", "mutable");
    boolean isUnbacked = readBooleanValue(values[3], "property", "unbacked");
    boolean isGetterImmutable = readBooleanValue(values[4], "property", "getter-immutable");
    boolean isSetterImmutable = readBooleanValue(values[5], "property", "setter-immutable");
    boolean isConstructorImmutable = readBooleanValue(values[6], "property", "constructor-immutable");
    SinceSpecifier sinceSpecifier = loadSinceSpecifier(values[7]);

    String backingMemberIndexString = readMDString(values[8]);
    if (backingMemberIndexString == null)
    {
      throw new MalformedMetadataException("A property must either have a valid backing member index in its metadata node, or declare itself as having none");
    }
    int memberIndex = -1;
    if (!backingMemberIndexString.equals("no_index"))
    {
      try
      {
        memberIndex = Integer.parseInt(backingMemberIndexString);
      }
      catch (NumberFormatException e)
      {
        throw new MalformedMetadataException("A property must either have a valid backing member index in its metadata node, or declare itself as having none", e);
      }
    }

    String getterMemberFunctionIndexString = readMDString(values[9]);
    String setterMemberFunctionIndexString = readMDString(values[10]);
    String constructorMemberFunctionIndexString = readMDString(values[11]);
    if (getterMemberFunctionIndexString == null || setterMemberFunctionIndexString == null || constructorMemberFunctionIndexString == null)
    {
      throw new MalformedMetadataException("A property must either have valid member function indexes in its metadata node for a getter, a setter, and a constructor, or declare itself as static");
    }
    int getterMemberFunctionIndex = -1;
    int setterMemberFunctionIndex = -1;
    int constructorMemberFunctionIndex = -1;
    boolean isStatic = false;
    boolean hasConstructorIndex = false;
    if (getterMemberFunctionIndexString.equals("static") && setterMemberFunctionIndexString.equals("static") && constructorMemberFunctionIndexString.equals("static"))
    {
      isStatic = true;
    }
    else
    {
      try
      {
        getterMemberFunctionIndex = Integer.parseInt(getterMemberFunctionIndexString);
      }
      catch (NumberFormatException e)
      {
        throw new MalformedMetadataException("A property must either have a valid member function index in its metadata node for its getter, or declare itself as static", e);
      }
      if (!isFinal)
      {
        try
        {
          setterMemberFunctionIndex = Integer.parseInt(setterMemberFunctionIndexString);
        }
        catch (NumberFormatException e)
        {
          throw new MalformedMetadataException("A property must either have a valid setter member function index in its metadata node, or declare itself as static or final", e);
        }
      }
      if (!constructorMemberFunctionIndexString.equals("no-constructor"))
      {
        try
        {
          constructorMemberFunctionIndex = Integer.parseInt(constructorMemberFunctionIndexString);
          hasConstructorIndex = true;
        }
        catch (NumberFormatException e)
        {
          throw new MalformedMetadataException("A property must either have a valid constructor member function index in its metadata node, or declare itself as static or unbacked", e);
        }
      }
    }

    if (!isStatic && !isUnbacked && backingMemberIndexString.equals("no_index"))
    {
      throw new MalformedMetadataException("A non-static property must either have a backing member index in its metadata node, or be declared as unbacked");
    }
    if (isAbstract && !isUnbacked)
    {
      throw new MalformedMetadataException("An abstract property must be saved as unbacked");
    }

    Type type = loadType(values[12]);
    String name = readMDString(values[13]);
    if (name == null)
    {
      throw new MalformedMetadataException("A property must have a valid name in its metadata node");
    }

    boolean hasGetter = true;
    boolean hasSetter = !isFinal;
    boolean hasConstructor = isFinal || (!isStatic && hasConstructorIndex);

    Property property = new Property(isAbstract, isFinal, isMutable, isStatic, isUnbacked, sinceSpecifier, type, name, null,
                                     hasGetter, isGetterImmutable, null, null,
                                     hasSetter, isSetterImmutable, null, null, null,
                                     hasConstructor, isConstructorImmutable, null, null, null,
                                     null);
    if (!isUnbacked)
    {
      if (isStatic)
      {
        property.setBackingGlobalVariable(new GlobalVariable(property));
      }
      else
      {
        MemberVariable backingMemberVariable = new MemberVariable(property);
        backingMemberVariable.setMemberIndex(memberIndex);
        property.setBackingMemberVariable(backingMemberVariable);
      }
    }
    if (!isStatic)
    {
      MemberFunction getterMemberFunction = new MemberFunction(property, MemberFunctionType.PROPERTY_GETTER);
      getterMemberFunction.setIndex(getterMemberFunctionIndex);
      property.setGetterMemberFunction(getterMemberFunction);
      MemberFunction setterMemberFunction = new MemberFunction(property, MemberFunctionType.PROPERTY_SETTER);
      setterMemberFunction.setIndex(setterMemberFunctionIndex);
      property.setSetterMemberFunction(setterMemberFunction);
      MemberFunction constructorMemberFunction = new MemberFunction(property, MemberFunctionType.PROPERTY_CONSTRUCTOR);
      constructorMemberFunction.setIndex(constructorMemberFunctionIndex);
      property.setConstructorMemberFunction(constructorMemberFunction);
    }
    return property;
  }

  private static Constructor loadConstructor(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A constructor must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 5)
    {
      throw new MalformedMetadataException("A constructor's metadata node must have the correct number of sub-nodes");
    }

    boolean isImmutable = readBooleanValue(values[0], "constructor", "immutable");
    boolean isSelfish = readBooleanValue(values[1], "constructor", "selfish");
    SinceSpecifier sinceSpecifier = loadSinceSpecifier(values[2]);
    Parameter[] parameters = loadParameters(values[3]);
    NamedType[] thrownTypes = loadThrownTypes(values[4]);

    return new Constructor(isImmutable, isSelfish, sinceSpecifier, parameters, thrownTypes, new NamedType[0], null, null);
  }

  private static Method loadMethod(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A method must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 9)
    {
      throw new MalformedMetadataException("A method's metadata node must have the correct number of sub-nodes");
    }

    String name = readMDString(values[0]);
    if (name == null)
    {
      throw new MalformedMetadataException("A method must have a valid name in its metadata node");
    }
    boolean isAbstract = readBooleanValue(values[1], "method", "abstract");
    boolean isImmutable = readBooleanValue(values[2], "method", "immutable");
    String nativeName = readMDString(values[3]);
    if (nativeName == null)
    {
      throw new MalformedMetadataException("A method must have a valid native name (or an empty string in its place) in its metadata node");
    }
    // an empty native name means that it shouldn't have a native version
    if (nativeName.equals(""))
    {
      nativeName = null;
    }
    SinceSpecifier sinceSpecifier = loadSinceSpecifier(values[4]);

    String memberIndexString = readMDString(values[5]);
    int memberIndex = -1;
    boolean isStatic = false;
    if (memberIndexString.equals("static"))
    {
      isStatic = true;
    }
    else
    {
      try
      {
        memberIndex = Integer.parseInt(memberIndexString);
      }
      catch (NumberFormatException e)
      {
        throw new MalformedMetadataException("A method must either have a valid member index in its metadata node, or declare itself as static", e);
      }
    }

    Type returnType = loadType(values[6]);
    Parameter[] parameters = loadParameters(values[7]);
    NamedType[] thrownTypes = loadThrownTypes(values[8]);

    Method method = new Method(returnType, name, isAbstract, isStatic, isImmutable, nativeName, sinceSpecifier, parameters, thrownTypes, new NamedType[0], null, null);
    if (!isStatic)
    {
      MemberFunction memberFunction = new MemberFunction(method);
      memberFunction.setIndex(memberIndex);
      method.setMemberFunction(memberFunction);
    }
    return method;
  }

  private static Parameter[] loadParameters(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A parameter list must be represented by a metadata node");
    }
    LLVMValueRef[] parameterNodes = readOperands(metadataNode);
    Parameter[] parameters = new Parameter[parameterNodes.length];
    for (int i = 0; i < parameterNodes.length; ++i)
    {
      if (LLVM.LLVMIsAMDNode(parameterNodes[i]) == null)
      {
        throw new MalformedMetadataException("A parameter must be represented by a metadata node");
      }
      LLVMValueRef[] parameterSubNodes = readOperands(parameterNodes[i]);
      if (parameterSubNodes.length != 2)
      {
        throw new MalformedMetadataException("A parameter's metadata node must have the correct number of sub-nodes");
      }
      Type type = loadType(parameterSubNodes[0]);
      String name = readMDString(parameterSubNodes[1]);
      if (name == null)
      {
        throw new MalformedMetadataException("A parameter must have a valid name in its metadata node");
      }
      parameters[i] = new NormalParameter(false, type, name, null);
    }
    return parameters;
  }

  private static NamedType[] loadThrownTypes(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A throws list must be represented by a metadata node");
    }
    LLVMValueRef[] thrownTypeNodes = readOperands(metadataNode);
    NamedType[] thrownTypes = new NamedType[thrownTypeNodes.length];
    for (int i = 0; i < thrownTypeNodes.length; ++i)
    {
      Type type = loadType(thrownTypeNodes[i]);
      if (!(type instanceof NamedType))
      {
        throw new MalformedMetadataException("A throws list must only consist of Named types");
      }
      thrownTypes[i] = (NamedType) type;
    }
    return thrownTypes;
  }

  private static SinceSpecifier loadSinceSpecifier(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A since specifier must be represented by a metadata node");
    }
    LLVMValueRef[] versionPartNodes = readOperands(metadataNode);
    if (versionPartNodes.length == 0)
    {
      // the since specifier is blank
      return null;
    }
    BigInteger[] versionParts = new BigInteger[versionPartNodes.length];
    for (int i = 0; i < versionPartNodes.length; ++i)
    {
      String versionPartStr = readMDString(versionPartNodes[i]);
      if (versionPartStr == null)
      {
        throw new MalformedMetadataException("A since specifier's version number elements must be represented by metadata strings");
      }
      try
      {
        versionParts[i] = new BigInteger(versionPartStr);
      }
      catch (NumberFormatException e)
      {
        throw new MalformedMetadataException("A since specifier must consist of a list of version number elements, all of which must be integers", e);
      }
    }
    return new SinceSpecifier(versionParts, null);
  }

  private static Type loadType(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A type must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length < 1)
    {
      throw new MalformedMetadataException("A type's metadata node must have the correct number of sub-nodes");
    }

    String sortOfType = readMDString(values[0]);
    if (sortOfType == null)
    {
      throw new MalformedMetadataException("A type must specify in its metadata node which sort of type it is");
    }
    if (sortOfType.equals("array"))
    {
      if (values.length != 4)
      {
        throw new MalformedMetadataException("An array type's metadata node must have the correct number of sub-nodes");
      }
      boolean nullable = readBooleanValue(values[1], "array type", "nullable");
      boolean immutable = readBooleanValue(values[2], "array type", "immutable");
      Type baseType = loadType(values[3]);
      return new ArrayType(nullable, immutable, baseType, null);
    }
    if (sortOfType.equals("function"))
    {
      if (values.length != 6)
      {
        throw new MalformedMetadataException("A function type's metadata node must have the correct number of sub-nodes");
      }
      boolean nullable = readBooleanValue(values[1], "function type", "nullable");
      boolean immutable = readBooleanValue(values[2], "function type", "immutable");
      Type returnType = loadType(values[3]);
      if (LLVM.LLVMIsAMDNode(values[4]) == null)
      {
        throw new MalformedMetadataException("A functions type's parameter type list must be represented by a metadata node");
      }
      LLVMValueRef[] parameterTypeNodes = readOperands(values[4]);
      Type[] parameterTypes = new Type[parameterTypeNodes.length];
      for (int i = 0; i < parameterTypeNodes.length; ++i)
      {
        parameterTypes[i] = loadType(parameterTypeNodes[i]);
      }
      NamedType[] thrownTypes = loadThrownTypes(values[5]);
      return new FunctionType(nullable, immutable, returnType, parameterTypes, thrownTypes, null);
    }
    if (sortOfType.equals("named"))
    {
      if (values.length != 5)
      {
        throw new MalformedMetadataException("A named type's metadata node must have the correct number of sub-nodes");
      }
      boolean nullable = readBooleanValue(values[1], "named type", "nullable");
      boolean immutable = readBooleanValue(values[2], "named type", "immutable");
      String qualifiedNameStr = readMDString(values[3]);
      if (qualifiedNameStr == null)
      {
        throw new MalformedMetadataException("A function type must have a valid qualified name in its metadata node");
      }
      if (LLVM.LLVMIsAMDNode(values[4]) == null)
      {
        throw new MalformedMetadataException("A named type's type argument list must be represented by a metadata node");
      }
      LLVMValueRef[] typeArgumentNodes = readOperands(values[4]);
      Type[] typeArguments = null;
      if (typeArgumentNodes.length > 0)
      {
        typeArguments = new Type[typeArgumentNodes.length];
        for (int i = 0; i < typeArguments.length; ++i)
        {
          typeArguments[i] = loadType(typeArgumentNodes[i]);
        }
      }
      try
      {
        QName qname = new QName(qualifiedNameStr);
        return new NamedType(nullable, immutable, qname, typeArguments, null);
      }
      catch (ConceptualException e)
      {
        throw new MalformedMetadataException(e.getMessage(), e);
      }
    }
    if (sortOfType.equals("object"))
    {
      if (values.length != 3)
      {
        throw new MalformedMetadataException("An object type's metadata node must have the correct number of sub-nodes");
      }
      boolean nullable = readBooleanValue(values[1], "object type", "nullable");
      boolean immutable = readBooleanValue(values[2], "object type", "immutable");
      return new ObjectType(nullable, immutable, null);
    }
    if (sortOfType.equals("primitive"))
    {
      if (values.length != 3)
      {
        throw new MalformedMetadataException("A primitive type's metadata node must have the correct number of sub-nodes");
      }
      boolean nullable = readBooleanValue(values[1], "primitive type", "nullable");
      String name = readMDString(values[2]);
      PrimitiveTypeType typeType = PrimitiveTypeType.getByName(name);
      if (typeType == null)
      {
        throw new MalformedMetadataException("A primitive type must have a valid type name in its metadata node");
      }
      return new PrimitiveType(nullable, typeType, null);
    }
    if (sortOfType.equals("tuple"))
    {
      if (values.length != 3)
      {
        throw new MalformedMetadataException("A tuple type's metadata node must have the correct number of sub-nodes");
      }
      boolean nullable = readBooleanValue(values[1], "tuple type", "nullable");
      if (LLVM.LLVMIsAMDNode(values[2]) == null)
      {
        throw new MalformedMetadataException("A tuple type's sub-type list must be represented by a metadata node");
      }
      LLVMValueRef[] subTypeNodes = readOperands(values[2]);
      Type[] subTypes = new Type[subTypeNodes.length];
      for (int i = 0; i < subTypeNodes.length; ++i)
      {
        subTypes[i] = loadType(subTypeNodes[i]);
      }
      return new TupleType(nullable, subTypes, null);
    }
    if (sortOfType.equals("void"))
    {
      if (values.length != 1)
      {
        throw new MalformedMetadataException("A void type's metadata node must have the correct number of sub-nodes");
      }
      return new VoidType(null);
    }
    if (sortOfType.equals("wildcard"))
    {
      if (values.length != 5)
      {
        throw new MalformedMetadataException("A void type's metadata node must have the correct number of sub-nodes");
      }
      boolean nullable = readBooleanValue(values[1], "wildcard type", "nullable");
      boolean immutable = readBooleanValue(values[2], "wildcard type", "immutable");
      if (LLVM.LLVMIsAMDNode(values[3]) == null)
      {
        throw new MalformedMetadataException("A wildcard type's super-type list must be represented by a metadata node");
      }
      if (LLVM.LLVMIsAMDNode(values[4]) == null)
      {
        throw new MalformedMetadataException("A wildcard type's sub-type list must be represented by a metadata node");
      }
      LLVMValueRef[] superTypeNodes = readOperands(values[3]);
      LLVMValueRef[] subTypeNodes = readOperands(values[4]);
      Type[] superTypes = new Type[superTypeNodes.length];
      Type[] subTypes = new Type[subTypeNodes.length];
      for (int i = 0; i < superTypeNodes.length; ++i)
      {
        superTypes[i] = loadType(superTypeNodes[i]);
      }
      for (int i = 0; i < subTypeNodes.length; ++i)
      {
        subTypes[i] = loadType(subTypeNodes[i]);
      }
      return new WildcardType(nullable, immutable, superTypes, subTypes, null);
    }
    throw new MalformedMetadataException("A type must specify a valid sort of type in its metadata node (e.g. a primitive type, or an array type)");
  }

  /**
   * Reads the operands of the specified LLVM metadata node. It is assumed that the metadata node has already been checked using LLVM.LLVMISAMDNode()
   * @param metadataNode - the metadata node to extract the values from
   * @return the array of values contained by the specified metadata node
   */
  private static LLVMValueRef[] readNamedMetadataOperands(LLVMModuleRef module, String metadataName)
  {
    int numOperands = LLVM.LLVMGetNamedMetadataNumOperands(module, metadataName);
    if (numOperands == 0)
    {
      return new LLVMValueRef[0];
    }
    LLVMValueRef[] operands = new LLVMValueRef[numOperands];
    Pointer pointer = C.toNativePointerArray(operands, false, true);
    LLVM.LLVMGetNamedMetadataOperands(module, metadataName, pointer);
    C.readNativePointerArray(pointer, operands, new PointerConverter<LLVMValueRef>()
    {
      @Override
      public LLVMValueRef convert(Pointer pointer)
      {
        if (pointer == null)
        {
          return null;
        }
        LLVMValueRef value = new LLVMValueRef();
        value.setPointer(pointer);
        return value;
      }
    });
    return operands;
  }

  /**
   * Reads the operands of the specified LLVM metadata node. It is assumed that the metadata node has already been checked using LLVM.LLVMISAMDNode()
   * @param metadataNode - the metadata node to extract the values from
   * @return the array of values contained by the specified metadata node
   */
  private static LLVMValueRef[] readOperands(LLVMValueRef metadataNode)
  {
    int subNodes = LLVM.LLVMGetMDNodeNumOperands(metadataNode);
    if (subNodes == 0)
    {
      return new LLVMValueRef[0];
    }
    LLVMValueRef[] values = new LLVMValueRef[subNodes];
    Pointer pointer = C.toNativePointerArray(values, false, true);
    LLVM.LLVMGetMDNodeOperands(metadataNode, pointer);
    C.readNativePointerArray(pointer, values, new PointerConverter<LLVMValueRef>()
    {
      @Override
      public LLVMValueRef convert(Pointer pointer)
      {
        LLVMValueRef result = new LLVMValueRef();
        result.setPointer(pointer);
        return result;
      }
    });
    return values;
  }

  /**
   * Tries to read the java String value of the specified MDString, or returns null if the specified value is not an MDString.
   * @param metadataString - the MDString to read
   * @return the java String representation of the specified MDString, or null if the specified value is not an MDString
   */
  private static String readMDString(LLVMValueRef metadataString)
  {
    metadataString = LLVM.LLVMIsAMDString(metadataString);
    if (metadataString == null)
    {
      return null;
    }
    IntByReference lengthInt = new IntByReference();
    Pointer resultPointer = LLVM.LLVMGetMDString(metadataString, lengthInt);
    byte[] bytes = new byte[lengthInt.getValue()];
    resultPointer.read(0, bytes, 0, bytes.length);
    return new String(bytes);
  }

  /**
   * Tries to read the specified metadata value as a boolean.
   * @param metadataString - the MDString to read
   * @param encapsulatingTypeName - the name of the type which this metadata string is contained in (e.g. "method", "class definition"), only used in MalformedMetadataExceptions
   * @param trueValue - the value to compare the string to
   * @return true if the string is equal to trueValue, false otherwise
   * @throws MalformedMetadataException - if the specified LLVM value is not a metadata string
   */
  private static boolean readBooleanValue(LLVMValueRef metadataString, String encapsulatingTypeName, String trueValue) throws MalformedMetadataException
  {
    String value = readMDString(metadataString);
    if (value == null)
    {
      throw new MalformedMetadataException("Every " + encapsulatingTypeName + " must have a valid value for: " + trueValue);
    }
    return value.equals(trueValue);
  }
}
