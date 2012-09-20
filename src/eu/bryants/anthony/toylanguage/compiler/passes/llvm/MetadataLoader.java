package eu.bryants.anthony.toylanguage.compiler.passes.llvm;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import nativelib.c.C;
import nativelib.c.C.PointerConverter;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMMemoryBufferRef;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMValueRef;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import eu.bryants.anthony.toylanguage.ast.ClassDefinition;
import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.TypeDefinition;
import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Method;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.misc.QName;
import eu.bryants.anthony.toylanguage.ast.type.ArrayType;
import eu.bryants.anthony.toylanguage.ast.type.FunctionType;
import eu.bryants.anthony.toylanguage.ast.type.NamedType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.toylanguage.ast.type.TupleType;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.ast.type.VoidType;
import eu.bryants.anthony.toylanguage.compiler.ConceptualException;
import eu.bryants.anthony.toylanguage.parser.LanguageParseException;

/*
 * Created on 29 Aug 2012
 */

/**
 * @author Anthony Bryant
 */
public class MetadataLoader
{

  /**
   * Parses the specified file as a bitcode file.
   * @param file - the file to load
   * @return the list of TypeDefinitions loaded
   * @throws IOException - if an IO problem occurs while trying to load the bitcode file
   * @throws MalformedMetadataException - if the bitcode file is malformed in some way (e.g. it doesn't have valid metadata)
   */
  public static List<TypeDefinition> loadBitcodeFile(File file) throws IOException, MalformedMetadataException
  {
    PointerByReference outMemoryBuffer = new PointerByReference();
    PointerByReference bufferOutMessage = new PointerByReference();
    boolean bufferFailure = LLVM.LLVMCreateMemoryBufferWithContentsOfFile(file.getAbsolutePath(), outMemoryBuffer, bufferOutMessage);
    if (bufferFailure || outMemoryBuffer.getValue() == null)
    {
      throw new IOException("Failed to load bitcode file: " + file.getAbsolutePath() + "\nReason: " + bufferOutMessage.getValue().getString(0));
    }
    LLVMMemoryBufferRef memoryBuffer = new LLVMMemoryBufferRef();
    memoryBuffer.setPointer(outMemoryBuffer.getValue());
    PointerByReference outModule = new PointerByReference();
    PointerByReference parseOutMessage = new PointerByReference();
    boolean parseFailure = LLVM.LLVMParseBitcode(memoryBuffer, outModule, parseOutMessage);
    LLVM.LLVMDisposeMemoryBuffer(memoryBuffer);
    if (parseFailure || outModule.getValue() == null)
    {
      throw new MalformedMetadataException("Failed to parse bitcode file: " + file.getAbsolutePath() + "\nReason: " + parseOutMessage.getValue().getString(0));
    }
    LLVMModuleRef module = new LLVMModuleRef();
    module.setPointer(outModule.getValue());
    LLVMValueRef[] classDefinitionNodes = readNamedMetadataOperands(module, "ClassDefinitions");
    LLVMValueRef[] compoundDefinitionNodes = readNamedMetadataOperands(module, "CompoundDefinitions");

    List<TypeDefinition> results = new LinkedList<TypeDefinition>();
    for (LLVMValueRef classDefinitionNode : classDefinitionNodes)
    {
      results.add(loadTypeDefinition(classDefinitionNode, true));
    }
    for (LLVMValueRef compoundDefinitionNode : compoundDefinitionNodes)
    {
      results.add(loadTypeDefinition(compoundDefinitionNode, false));
    }
    LLVM.LLVMDisposeModule(module);
    return results;
  }

  /**
   * Loads a TypeDefinition from the specified metadata node.
   * @param metadataNode - the metadata node to load from
   * @param classDefinition - true if this node represents a ClassDefinition, false if it represents a CompoundDefinition
   * @return the TypeDefinition loaded
   * @throws MalformedMetadataException - if the metadata is malformed in some way
   */
  private static TypeDefinition loadTypeDefinition(LLVMValueRef metadataNode, boolean classDefinition) throws MalformedMetadataException
  {
    metadataNode = LLVM.LLVMIsAMDNode(metadataNode);
    if (metadataNode == null)
    {
      throw new MalformedMetadataException("A type definition must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 5)
    {
      throw new MalformedMetadataException("A type definition's metadata node must have the correct number of sub-nodes");
    }

    String qualifiedNameStr = readMDString(values[0]);
    if (qualifiedNameStr == null)
    {
      throw new MalformedMetadataException("A type definition must begin with a fully qualified name");
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

    if (LLVM.LLVMIsAMDNode(values[1]) == null || LLVM.LLVMIsAMDNode(values[2]) == null || LLVM.LLVMIsAMDNode(values[3]) == null || LLVM.LLVMIsAMDNode(values[4]) == null)
    {
      throw new MalformedMetadataException("The member nodes of a type definition must be metadata nodes");
    }

    LLVMValueRef[] nonStaticFieldNodes = readOperands(values[1]);
    Field[] nonStaticFields = new Field[nonStaticFieldNodes.length];
    for (int i = 0; i < nonStaticFieldNodes.length; ++i)
    {
      nonStaticFields[i] = loadField(nonStaticFieldNodes[i], false, i);
    }

    LLVMValueRef[] staticFieldNodes = readOperands(values[2]);
    Field[] staticFields = new Field[staticFieldNodes.length];
    for (int i = 0; i < staticFieldNodes.length; ++i)
    {
      staticFields[i] = loadField(staticFieldNodes[i], true, i);
    }

    LLVMValueRef[] constructorNodes = readOperands(values[3]);
    Constructor[] constructors = new Constructor[constructorNodes.length];
    for (int i = 0; i < constructorNodes.length; ++i)
    {
      constructors[i] = loadConstructor(constructorNodes[i], qname.getLastName());
    }

    LLVMValueRef[] methodNodes = readOperands(values[4]);
    Method[] methods = new Method[methodNodes.length];
    for (int i = 0; i < methodNodes.length; ++i)
    {
      methods[i] = loadMethod(methodNodes[i]);
    }

    TypeDefinition typeDefinition;
    try
    {
      if (classDefinition)
      {
        typeDefinition = new ClassDefinition(qname, nonStaticFields, staticFields, constructors, methods);
      }
      else
      {
        typeDefinition = new CompoundDefinition(qname, nonStaticFields, staticFields, constructors, methods);
      }
    }
    catch (LanguageParseException e)
    {
      throw new MalformedMetadataException(e.getMessage(), e);
    }
    return typeDefinition;
  }

  private static Field loadField(LLVMValueRef metadataNode, boolean isStatic, int index) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A field must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 3)
    {
      throw new MalformedMetadataException("A field's metadata node must have the correct number of sub-nodes");
    }

    String isFinalStr = readMDString(values[0]);
    if (isFinalStr == null)
    {
      throw new MalformedMetadataException("A field must have a valid finality property in its metadata node");
    }
    boolean isFinal = isFinalStr.equals("final");

    Type type = loadType(values[1]);
    String name = readMDString(values[2]);
    if (name == null)
    {
      throw new MalformedMetadataException("A field must have a valid name in its metadata node");
    }
    Field field = new Field(type, name, isStatic, isFinal, null);
    if (!isStatic)
    {
      field.setMemberIndex(index);
    }
    return field;
  }

  private static Constructor loadConstructor(LLVMValueRef metadataNode, String name) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A constructor must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 1)
    {
      throw new MalformedMetadataException("A constructor's metadata node must have the correct number of sub-nodes");
    }
    Parameter[] parameters = loadParameters(values[0]);
    return new Constructor(name, parameters, null, null);
  }

  private static Method loadMethod(LLVMValueRef metadataNode) throws MalformedMetadataException
  {
    if (LLVM.LLVMIsAMDNode(metadataNode) == null)
    {
      throw new MalformedMetadataException("A method must be represented by a metadata node");
    }
    LLVMValueRef[] values = readOperands(metadataNode);
    if (values.length != 5)
    {
      throw new MalformedMetadataException("A method's metadata node must have the correct number of sub-nodes");
    }

    String name = readMDString(values[0]);
    if (name == null)
    {
      throw new MalformedMetadataException("A method must have a valid name in its metadata node");
    }

    String isStaticStr = readMDString(values[1]);
    if (isStaticStr == null)
    {
      throw new MalformedMetadataException("A method must have a valid staticness property in its metadata node");
    }
    boolean isStatic = isStaticStr.equals("static");

    String nativeName = readMDString(values[2]);
    if (nativeName == null)
    {
      throw new MalformedMetadataException("A method must have a valid native name (or an empty string in its place) in its metadata node");
    }
    // an empty native name means that it shouldn't have a native version
    if (nativeName.equals(""))
    {
      nativeName = null;
    }

    Type returnType = loadType(values[3]);
    Parameter[] parameters = loadParameters(values[4]);
    return new Method(returnType, name, isStatic, nativeName, parameters, null, null);
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
      if (values.length != 3)
      {
        throw new MalformedMetadataException("An array type's metadata node must have the correct number of sub-nodes");
      }
      String nullabilityStr = readMDString(values[1]);
      if (nullabilityStr == null)
      {
        throw new MalformedMetadataException("An array type must have a valid nullability in its metadata node");
      }
      boolean nullable = nullabilityStr.equals("nullable");
      Type baseType = loadType(values[2]);
      return new ArrayType(nullable, baseType, null);
    }
    if (sortOfType.equals("function"))
    {
      if (values.length != 4)
      {
        throw new MalformedMetadataException("A function type's metadata node must have the correct number of sub-nodes");
      }
      String nullabilityStr = readMDString(values[1]);
      if (nullabilityStr == null)
      {
        throw new MalformedMetadataException("A function type must have a valid nullability in its metadata node");
      }
      boolean nullable = nullabilityStr.equals("nullable");
      Type returnType = loadType(values[2]);

      if (LLVM.LLVMIsAMDNode(values[3]) == null)
      {
        throw new MalformedMetadataException("A functions type's parameter type list must be represented by a metadata node");
      }
      LLVMValueRef[] parameterTypeNodes = readOperands(values[3]);
      Type[] parameterTypes = new Type[parameterTypeNodes.length];
      for (int i = 0; i < parameterTypeNodes.length; ++i)
      {
        parameterTypes[i] = loadType(parameterTypeNodes[i]);
      }
      return new FunctionType(nullable, returnType, parameterTypes, null);
    }
    if (sortOfType.equals("named"))
    {
      if (values.length != 3)
      {
        throw new MalformedMetadataException("A named type's metadata node must have the correct number of sub-nodes");
      }
      String nullabilityStr = readMDString(values[1]);
      if (nullabilityStr == null)
      {
        throw new MalformedMetadataException("A named type must have a valid nullability in its metadata node");
      }
      boolean nullable = nullabilityStr.equals("nullable");

      String qualifiedNameStr = readMDString(values[2]);
      if (qualifiedNameStr == null)
      {
        throw new MalformedMetadataException("A function type must have a valid qualified name in its metadata node");
      }
      try
      {
        QName qname = new QName(qualifiedNameStr);
        return new NamedType(nullable, qname, null);
      }
      catch (ConceptualException e)
      {
        throw new MalformedMetadataException(e.getMessage(), e);
      }
    }
    if (sortOfType.equals("primitive"))
    {
      if (values.length != 3)
      {
        throw new MalformedMetadataException("A primitive type's metadata node must have the correct number of sub-nodes");
      }
      String nullabilityStr = readMDString(values[1]);
      if (nullabilityStr == null)
      {
        throw new MalformedMetadataException("A primitive type must have a valid nullability in its metadata node");
      }
      boolean nullable = nullabilityStr.equals("nullable");

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
      String nullabilityStr = readMDString(values[1]);
      if (nullabilityStr == null)
      {
        throw new MalformedMetadataException("A tuple type must have a valid nullability in its metadata node");
      }
      boolean nullable = nullabilityStr.equals("nullable");

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
}
