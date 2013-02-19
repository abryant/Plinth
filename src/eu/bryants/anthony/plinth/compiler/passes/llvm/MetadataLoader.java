package eu.bryants.anthony.plinth.compiler.passes.llvm;

import java.math.BigInteger;
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
import eu.bryants.anthony.plinth.ast.type.VoidType;
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

    String superClassQNameStr = readMDString(values[3]);
    if (superClassQNameStr == null)
    {
      throw new MalformedMetadataException("A class definition must contain the fully qualified name of its superclass (or an empty string in its place)");
    }
    QName superClassQName;
    try
    {
      superClassQName = superClassQNameStr.equals("") ? null : new QName(superClassQNameStr);
    }
    catch (ConceptualException e)
    {
      throw new MalformedMetadataException(e.getMessage(), e);
    }

    QName[] superInterfaceQNames = loadSuperInterfaces(values[4]);

    Field[] nonStaticFields = loadFields(values[5], false);
    Field[] staticFields = loadFields(values[6], true);
    Constructor[] constructors = loadConstructors(values[7]);
    Method[] nonStaticMethods = loadMethods(values[8], false);
    Method[] staticMethods = loadMethods(values[9], true);

    try
    {
      return new ClassDefinition(isAbstract, isImmutable, qname, superClassQName, superInterfaceQNames, nonStaticFields, staticFields, constructors, nonStaticMethods, staticMethods);
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

    Field[] nonStaticFields = loadFields(values[2], false);
    Field[] staticFields = loadFields(values[3], true);
    Constructor[] constructors = loadConstructors(values[4]);
    Method[] nonStaticMethods = loadMethods(values[5], false);
    Method[] staticMethods = loadMethods(values[6], true);

    try
    {
      return new CompoundDefinition(isImmutable, qname, nonStaticFields, staticFields, constructors, nonStaticMethods, staticMethods);
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
    if (values.length != 6)
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

    QName[] superInterfaceQNames = loadSuperInterfaces(values[2]);

    Field[] staticFields = loadFields(values[3], true);
    Method[] nonStaticMethods = loadMethods(values[4], false);
    Method[] staticMethods = loadMethods(values[5], true);

    try
    {
      return new InterfaceDefinition(isImmutable, qname, superInterfaceQNames, staticFields, nonStaticMethods, staticMethods);
    }
    catch (LanguageParseException e)
    {
      throw new MalformedMetadataException(e.getMessage(), e);
    }
  }

  private static QName[] loadSuperInterfaces(LLVMValueRef metadataNode) throws MalformedMetadataException
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
    QName[] qnames = new QName[values.length];
    for (int i = 0; i < qnames.length; ++i)
    {
      String valueStr = readMDString(values[i]);
      if (valueStr == null)
      {
        throw new MalformedMetadataException("A super-interface must be represented by a metadata string");
      }
      try
      {
        qnames[i] = valueStr.equals("") ? null : new QName(valueStr);
      }
      catch (ConceptualException e)
      {
        throw new MalformedMetadataException(e.getMessage(), e);
      }
    }
    return qnames;
  }


  private static Field[] loadFields(LLVMValueRef metadataNode, boolean isStatic) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A field list must be represented by a metadata node");
    }
    LLVMValueRef[] fieldNodes = readOperands(metadataNode);
    Field[] fields = new Field[fieldNodes.length];
    for (int i = 0; i < fieldNodes.length; ++i)
    {
      fields[i] = loadField(fieldNodes[i], isStatic, i);
    }
    return fields;
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

  private static Method[] loadMethods(LLVMValueRef metadataNode, boolean isStatic) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A method list must be represented by a metadata node");
    }
    LLVMValueRef[] methodNodes = readOperands(metadataNode);
    Method[] methods = new Method[methodNodes.length];
    for (int i = 0; i < methodNodes.length; ++i)
    {
      methods[i] = loadMethod(methodNodes[i], isStatic, i);
    }
    return methods;
  }

  private static Field loadField(LLVMValueRef metadataNode, boolean isStatic, int index) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A field must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 5)
    {
      throw new MalformedMetadataException("A field's metadata node must have the correct number of sub-nodes");
    }

    boolean isFinal = readBooleanValue(values[0], "field", "final");
    boolean isMutable = readBooleanValue(values[1], "field", "mutable");

    SinceSpecifier sinceSpecifier = loadSinceSpecifier(values[2]);

    Type type = loadType(values[3]);
    String name = readMDString(values[4]);
    if (name == null)
    {
      throw new MalformedMetadataException("A field must have a valid name in its metadata node");
    }
    Field field = new Field(type, name, isStatic, isFinal, isMutable, sinceSpecifier, null, null);
    if (!isStatic)
    {
      field.setMemberIndex(index);
    }
    return field;
  }

  private static Constructor loadConstructor(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A constructor must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 4)
    {
      throw new MalformedMetadataException("A constructor's metadata node must have the correct number of sub-nodes");
    }

    boolean isImmutable = readBooleanValue(values[0], "constructor", "immutable");
    boolean isSelfish = readBooleanValue(values[1], "constructor", "selfish");
    SinceSpecifier sinceSpecifier = loadSinceSpecifier(values[2]);
    Parameter[] parameters = loadParameters(values[3]);

    return new Constructor(isImmutable, isSelfish, sinceSpecifier, parameters, null, null);
  }

  private static Method loadMethod(LLVMValueRef metadataNode, boolean isStatic, int index) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A method must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 7)
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
    Type returnType = loadType(values[5]);
    Parameter[] parameters = loadParameters(values[6]);

    if (isAbstract && isStatic)
    {
      throw new MalformedMetadataException("A static method cannot be abstract");
    }
    Method method = new Method(returnType, name, isAbstract, isStatic, isImmutable, nativeName, sinceSpecifier, parameters, null, null);
    if (!isStatic)
    {
      method.setMethodIndex(index);
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
      parameters[i] = new Parameter(false, type, name, null);
    }
    return parameters;
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
      if (values.length != 5)
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
      return new FunctionType(nullable, immutable, returnType, parameterTypes, null);
    }
    if (sortOfType.equals("named"))
    {
      if (values.length != 4)
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
      try
      {
        QName qname = new QName(qualifiedNameStr);
        return new NamedType(nullable, immutable, qname, null);
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
