package eu.bryants.anthony.plinth.compiler.passes.llvm;

import java.math.BigInteger;
import java.util.Collection;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMValueRef;
import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.VoidType;

/*
 * Created on 28 Aug 2012
 */

/**
 * @author Anthony Bryant
 */
public class MetadataGenerator
{
  /**
   * Generates the metadata for the specified TypeDefinition, and writes it into the specified module.
   * @param typeDefinition - the TypeDefinition to generate metadata for
   * @param module - the module to write the metadata to
   */
  public static void generateMetadata(TypeDefinition typeDefinition, LLVMModuleRef module)
  {
    String qualifiedName = typeDefinition.getQualifiedName().toString();
    LLVMValueRef nameNode = createMDString(qualifiedName);

    LLVMValueRef immutabilityNode = createMDString(typeDefinition.isImmutable() ? "immutable" : "not-immutable");

    LLVMValueRef nonStaticFieldsNode = generateFieldList(typeDefinition.getNonStaticFields());
    LLVMValueRef staticFieldsNode = generateFieldList(typeDefinition.getStaticFields());
    LLVMValueRef constructorsNode = generateConstructorList(typeDefinition.getAllConstructors());
    LLVMValueRef nonStaticMethodsNode = generateMethodList(typeDefinition.getNonStaticMethods());
    LLVMValueRef staticMethodsNode = generateMethodList(typeDefinition.getStaticMethods());

    if (typeDefinition instanceof ClassDefinition)
    {
      LLVMValueRef abstractnessNode = createMDString(typeDefinition.isAbstract() ? "abstract" : "not-abstract");

      ClassDefinition superClass = ((ClassDefinition) typeDefinition).getSuperClassDefinition();
      LLVMValueRef superClassNode = createMDString(superClass == null ? "" : superClass.getQualifiedName().toString());
      LLVMValueRef superInterfacesNode = generateSuperInterfaces(((ClassDefinition) typeDefinition).getSuperInterfaceDefinitions());

      LLVMValueRef[] values = new LLVMValueRef[] {nameNode, abstractnessNode, immutabilityNode, superClassNode, superInterfacesNode, nonStaticFieldsNode, staticFieldsNode, constructorsNode, nonStaticMethodsNode, staticMethodsNode};
      LLVMValueRef resultNode = LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
      LLVM.LLVMAddNamedMetadataOperand(module, "ClassDefinitions", resultNode);
    }
    else if (typeDefinition instanceof CompoundDefinition)
    {
      LLVMValueRef[] values = new LLVMValueRef[] {nameNode, immutabilityNode, nonStaticFieldsNode, staticFieldsNode, constructorsNode, nonStaticMethodsNode, staticMethodsNode};
      LLVMValueRef resultNode = LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
      LLVM.LLVMAddNamedMetadataOperand(module, "CompoundDefinitions", resultNode);
    }
    else if (typeDefinition instanceof InterfaceDefinition)
    {
      LLVMValueRef superInterfacesNode = generateSuperInterfaces(((InterfaceDefinition) typeDefinition).getSuperInterfaceDefinitions());

      LLVMValueRef[] values = new LLVMValueRef[] {nameNode, immutabilityNode, superInterfacesNode, staticFieldsNode, nonStaticMethodsNode, staticMethodsNode};
      LLVMValueRef resultNode = LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
      LLVM.LLVMAddNamedMetadataOperand(module, "InterfaceDefinitions", resultNode);
    }
  }

  private static LLVMValueRef generateSuperInterfaces(InterfaceDefinition[] superInterfaces)
  {
    LLVMValueRef[] interfaceValues = new LLVMValueRef[superInterfaces == null ? 0 : superInterfaces.length];
    for (int i = 0; i < interfaceValues.length; ++i)
    {
      interfaceValues[i] = createMDString(superInterfaces[i].getQualifiedName().toString());
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(interfaceValues, false, true), interfaceValues.length);
  }

  private static LLVMValueRef generateFieldList(Field[] fields)
  {
    LLVMValueRef[] fieldNodes = new LLVMValueRef[fields.length];
    for (int i = 0; i < fields.length; ++i)
    {
      fieldNodes[i] = generateField(fields[i]);
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(fieldNodes, false, true), fieldNodes.length);
  }

  private static LLVMValueRef generateConstructorList(Collection<Constructor> constructors)
  {
    LLVMValueRef[] constructorNodes = new LLVMValueRef[constructors.size()];
    int index = 0;
    for (Constructor constructor : constructors)
    {
      constructorNodes[index] = generateConstructor(constructor);
      ++index;
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(constructorNodes, false, true), constructorNodes.length);
  }

  private static LLVMValueRef generateMethodList(Method[] methods)
  {
    LLVMValueRef[] methodNodes = new LLVMValueRef[methods.length];
    for (int i = 0; i < methods.length; ++i)
    {
      methodNodes[i] = generateMethod(methods[i]);
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(methodNodes, false, true), methodNodes.length);
  }

  private static LLVMValueRef generateField(Field field)
  {
    LLVMValueRef finalityNode = createMDString(field.isFinal() ? "final" : "not-final");
    LLVMValueRef mutabilityNode = createMDString(field.isMutable() ? "mutable" : "not-mutable");
    LLVMValueRef sinceSpecifierNode = generateSinceSpecifier(field.getSinceSpecifier());
    LLVMValueRef typeNode = generateType(field.getType());
    LLVMValueRef nameNode = createMDString(field.getName());
    LLVMValueRef[] values = new LLVMValueRef[] {finalityNode, mutabilityNode, sinceSpecifierNode, typeNode, nameNode};
    return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
  }

  private static LLVMValueRef generateConstructor(Constructor constructor)
  {
    LLVMValueRef immutabilityNode = createMDString(constructor.isImmutable() ? "immutable" : "not-immutable");
    LLVMValueRef selfishnessNode = createMDString(constructor.isSelfish() ? "selfish" : "not-selfish");
    LLVMValueRef sinceSpecifierNode = generateSinceSpecifier(constructor.getSinceSpecifier());
    LLVMValueRef parametersNode = generateParameters(constructor.getParameters());
    LLVMValueRef[] values = new LLVMValueRef[] {immutabilityNode, selfishnessNode, sinceSpecifierNode, parametersNode};
    return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
  }

  private static LLVMValueRef generateMethod(Method method)
  {
    LLVMValueRef nameNode = createMDString(method.getName());
    LLVMValueRef isAbstractNode = createMDString(method.isAbstract() ? "abstract" : "not-abstract");
    LLVMValueRef isImmutableNode = createMDString(method.isImmutable() ? "immutable" : "not-immutable");
    LLVMValueRef nativeNameNode = createMDString(method.getNativeName() == null ? "" : method.getNativeName());
    LLVMValueRef sinceSpecifierNode = generateSinceSpecifier(method.getSinceSpecifier());
    LLVMValueRef returnTypeNode = generateType(method.getReturnType());
    LLVMValueRef parametersNode = generateParameters(method.getParameters());
    LLVMValueRef[] values = new LLVMValueRef[] {nameNode, isAbstractNode, isImmutableNode, nativeNameNode, sinceSpecifierNode, returnTypeNode, parametersNode};
    return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
  }

  private static LLVMValueRef generateParameters(Parameter[] parameters)
  {
    LLVMValueRef[] parameterNodes = new LLVMValueRef[parameters.length];
    for (int i = 0; i < parameters.length; ++i)
    {
      LLVMValueRef typeNode = generateType(parameters[i].getType());
      LLVMValueRef nameNode = createMDString(parameters[i].getName());
      LLVMValueRef[] parameterValues = new LLVMValueRef[] {typeNode, nameNode};
      parameterNodes[i] = LLVM.LLVMMDNode(C.toNativePointerArray(parameterValues, false, true), parameterValues.length);
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(parameterNodes, false, true), parameterNodes.length);
  }

  private static LLVMValueRef generateSinceSpecifier(SinceSpecifier sinceSpecifier)
  {
    if (sinceSpecifier == null)
    {
      LLVMValueRef[] values = new LLVMValueRef[0];
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    BigInteger[] versionParts = sinceSpecifier.getVersionParts();
    LLVMValueRef[] llvmVersionParts = new LLVMValueRef[versionParts.length];
    for (int i = 0; i < versionParts.length; ++i)
    {
      llvmVersionParts[i] = createMDString(versionParts[i].toString());
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(llvmVersionParts, false, true), llvmVersionParts.length);
  }

  private static LLVMValueRef generateType(Type type)
  {
    LLVMValueRef nullableNode = createMDString(type.isNullable() ? "nullable" : "not-nullable");
    if (type instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) type;
      LLVMValueRef sortNode = createMDString("array");
      LLVMValueRef immutableNode = createMDString(arrayType.isExplicitlyImmutable() ? "immutable" : "not-immutable");
      LLVMValueRef baseTypeNode = generateType(arrayType.getBaseType());
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode, nullableNode, immutableNode, baseTypeNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    if (type instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) type;
      LLVMValueRef sortNode = createMDString("function");
      LLVMValueRef immutableNode = createMDString(functionType.isImmutable() ? "immutable" : "not-immutable");
      LLVMValueRef returnTypeNode = generateType(functionType.getReturnType());
      Type[] parameterTypes = functionType.getParameterTypes();
      LLVMValueRef[] parameterTypeNodes = new LLVMValueRef[parameterTypes.length];
      for (int i = 0; i < parameterTypes.length; ++i)
      {
        parameterTypeNodes[i] = generateType(parameterTypes[i]);
      }
      LLVMValueRef parameterTypesNode = LLVM.LLVMMDNode(C.toNativePointerArray(parameterTypeNodes, false, true), parameterTypeNodes.length);
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode, nullableNode, immutableNode, returnTypeNode, parameterTypesNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      LLVMValueRef sortNode = createMDString("named");
      LLVMValueRef immutableNode = createMDString(namedType.isExplicitlyImmutable() ? "immutable" : "not-immutable");
      String qualifiedName = namedType.getResolvedTypeDefinition().getQualifiedName().toString();
      LLVMValueRef qualifiedNameNode = createMDString(qualifiedName);
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode, nullableNode, immutableNode, qualifiedNameNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    if (type instanceof PrimitiveType)
    {
      PrimitiveTypeType primitiveTypeType = ((PrimitiveType) type).getPrimitiveTypeType();
      LLVMValueRef sortNode = createMDString("primitive");
      LLVMValueRef nameNode = createMDString(primitiveTypeType.name);
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode, nullableNode, nameNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      LLVMValueRef sortNode = createMDString("tuple");
      Type[] subTypes = tupleType.getSubTypes();
      LLVMValueRef[] subTypeNodes = new LLVMValueRef[subTypes.length];
      for (int i = 0; i < subTypes.length; ++i)
      {
        subTypeNodes[i] = generateType(subTypes[i]);
      }
      LLVMValueRef subTypesNode = LLVM.LLVMMDNode(C.toNativePointerArray(subTypeNodes, false, true), subTypeNodes.length);
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode, nullableNode, subTypesNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    if (type instanceof VoidType)
    {
      LLVMValueRef sortNode = createMDString("void");
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    throw new IllegalArgumentException("Internal metadata generation error: unknown sort of Type: " + type);
  }

  private static LLVMValueRef createMDString(String str)
  {
    return LLVM.LLVMMDString(str, str.getBytes().length);
  }
}
