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
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.misc.DefaultParameter;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
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

    LLVMValueRef fieldsNode = generateFieldList(typeDefinition.getFields());
    LLVMValueRef propertiesNode = generatePropertyList(typeDefinition.getProperties());
    LLVMValueRef constructorsNode = generateConstructorList(typeDefinition.getAllConstructors());
    LLVMValueRef methodsNode = generateMethodList(typeDefinition.getAllMethods());

    if (typeDefinition instanceof ClassDefinition)
    {
      LLVMValueRef abstractnessNode = createMDString(typeDefinition.isAbstract() ? "abstract" : "not-abstract");

      LLVMValueRef typeParametersNode = generateTypeParameters(typeDefinition.getTypeParameters());

      NamedType superClass = ((ClassDefinition) typeDefinition).getSuperType();
      LLVMValueRef superClassNode = generateType(superClass == null ? new ObjectType(false, false, null) : superClass);
      LLVMValueRef superInterfacesNode = generateSuperInterfaces(((ClassDefinition) typeDefinition).getSuperInterfaceTypes());

      LLVMValueRef[] values = new LLVMValueRef[] {nameNode, abstractnessNode, immutabilityNode, typeParametersNode, superClassNode, superInterfacesNode, fieldsNode, propertiesNode, constructorsNode, methodsNode};
      LLVMValueRef resultNode = LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
      LLVM.LLVMAddNamedMetadataOperand(module, "ClassDefinitions", resultNode);
    }
    else if (typeDefinition instanceof CompoundDefinition)
    {
      LLVMValueRef typeParametersNode = generateTypeParameters(typeDefinition.getTypeParameters());

      LLVMValueRef[] values = new LLVMValueRef[] {nameNode, immutabilityNode, typeParametersNode, fieldsNode, propertiesNode, constructorsNode, methodsNode};
      LLVMValueRef resultNode = LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
      LLVM.LLVMAddNamedMetadataOperand(module, "CompoundDefinitions", resultNode);
    }
    else if (typeDefinition instanceof InterfaceDefinition)
    {
      LLVMValueRef typeParametersNode = generateTypeParameters(typeDefinition.getTypeParameters());

      LLVMValueRef superInterfacesNode = generateSuperInterfaces(((InterfaceDefinition) typeDefinition).getSuperInterfaceTypes());

      LLVMValueRef[] values = new LLVMValueRef[] {nameNode, immutabilityNode, typeParametersNode, superInterfacesNode, fieldsNode, propertiesNode, methodsNode};
      LLVMValueRef resultNode = LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
      LLVM.LLVMAddNamedMetadataOperand(module, "InterfaceDefinitions", resultNode);
    }
  }

  private static LLVMValueRef generateTypeParameters(TypeParameter[] typeParameters)
  {
    LLVMValueRef[] nodes = new LLVMValueRef[typeParameters.length];
    for (int i = 0; i < typeParameters.length; ++i)
    {
      nodes[i] = generateTypeParameter(typeParameters[i]);
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(nodes, false, true), nodes.length);
  }

  private static LLVMValueRef generateTypeParameter(TypeParameter typeParameter)
  {
    LLVMValueRef nameNode = createMDString(typeParameter.getName());

    Type[] superTypes = typeParameter.getSuperTypes();
    LLVMValueRef[] superTypeNodes = new LLVMValueRef[superTypes.length];
    for (int i = 0; i < superTypes.length; ++i)
    {
      superTypeNodes[i] = generateType(superTypes[i]);
    }
    LLVMValueRef superTypesNode = LLVM.LLVMMDNode(C.toNativePointerArray(superTypeNodes, false, true), superTypeNodes.length);

    Type[] subTypes = typeParameter.getSubTypes();
    LLVMValueRef[] subTypeNodes = new LLVMValueRef[subTypes.length];
    for (int i = 0; i < subTypes.length; ++i)
    {
      subTypeNodes[i] = generateType(subTypes[i]);
    }
    LLVMValueRef subTypesNode = LLVM.LLVMMDNode(C.toNativePointerArray(subTypeNodes, false, true), subTypeNodes.length);

    LLVMValueRef[] values = new LLVMValueRef[] {nameNode, superTypesNode, subTypesNode};
    return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
  }

  private static LLVMValueRef generateSuperInterfaces(NamedType[] superInterfaceTypes)
  {
    LLVMValueRef[] interfaceValues = new LLVMValueRef[superInterfaceTypes == null ? 0 : superInterfaceTypes.length];
    for (int i = 0; i < interfaceValues.length; ++i)
    {
      interfaceValues[i] = generateType(superInterfaceTypes[i]);
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(interfaceValues, false, true), interfaceValues.length);
  }

  private static LLVMValueRef generateFieldList(Collection<Field> fields)
  {
    LLVMValueRef[] fieldNodes = new LLVMValueRef[fields.size()];
    int index = 0;
    for (Field field : fields)
    {
      fieldNodes[index] = generateField(field);
      index++;
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(fieldNodes, false, true), fieldNodes.length);
  }

  private static LLVMValueRef generatePropertyList(Collection<Property> properties)
  {
    LLVMValueRef[] propertyNodes = new LLVMValueRef[properties.size()];
    int index = 0;
    for (Property property : properties)
    {
      propertyNodes[index] = generateProperty(property);
      index++;
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(propertyNodes, false, true), propertyNodes.length);
  }

  private static LLVMValueRef generateConstructorList(Collection<Constructor> constructors)
  {
    LLVMValueRef[] constructorNodes = new LLVMValueRef[constructors.size()];
    int index = 0;
    for (Constructor constructor : constructors)
    {
      constructorNodes[index] = generateConstructor(constructor);
      index++;
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(constructorNodes, false, true), constructorNodes.length);
  }

  private static LLVMValueRef generateMethodList(Collection<Method> methods)
  {
    LLVMValueRef[] methodNodes = new LLVMValueRef[methods.size()];
    int index = 0;
    for (Method method : methods)
    {
      methodNodes[index] = generateMethod(method);
      index++;
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(methodNodes, false, true), methodNodes.length);
  }

  private static LLVMValueRef generateField(Field field)
  {
    LLVMValueRef finalityNode = createMDString(field.isFinal() ? "final" : "not-final");
    LLVMValueRef mutabilityNode = createMDString(field.isMutable() ? "mutable" : "not-mutable");
    LLVMValueRef sinceSpecifierNode = generateSinceSpecifier(field.getSinceSpecifier());
    LLVMValueRef memberIndexNode = createMDString(field.isStatic() ? "static" : ("" + field.getMemberVariable().getMemberIndex()));
    LLVMValueRef typeNode = generateType(field.getType());
    LLVMValueRef nameNode = createMDString(field.getName());
    LLVMValueRef[] values = new LLVMValueRef[] {finalityNode, mutabilityNode, sinceSpecifierNode, memberIndexNode, typeNode, nameNode};
    return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
  }

  private static LLVMValueRef generateProperty(Property property)
  {
    LLVMValueRef isAbstractNode = createMDString(property.isAbstract() ? "abstract" : "not-abstract");
    LLVMValueRef finalityNode = createMDString(property.isFinal() ? "final" : "not-final");
    LLVMValueRef mutabilityNode = createMDString(property.isMutable() ? "mutable" : "not-mutable");
    LLVMValueRef isUnbackedNode = createMDString(property.isUnbacked() ? "unbacked" : "not-unbacked");
    LLVMValueRef isGetterImmutableNode = createMDString(property.isGetterImmutable() ? "getter-immutable" : "getter-mutable");
    LLVMValueRef isSetterImmutableNode = createMDString(property.isSetterImmutable() ? "setter-immutable" : "setter-mutable");
    LLVMValueRef isConstructorImmutableNode = createMDString(property.isConstructorImmutable() ? "constructor-immutable" : "constructor-mutable");
    LLVMValueRef sinceSpecifierNode = generateSinceSpecifier(property.getSinceSpecifier());
    LLVMValueRef backingMemberIndexNode = createMDString((property.isStatic() || property.isUnbacked()) ? "no_index" : ("" + property.getBackingMemberVariable().getMemberIndex()));
    LLVMValueRef getterMemberFunctionIndexNode = createMDString(property.isStatic() ? "static" : ("" + property.getGetterMemberFunction().getIndex()));
    LLVMValueRef setterMemberFunctionIndexNode = createMDString(property.isStatic() ? "static" : property.isFinal() ? "final" : ("" + property.getSetterMemberFunction().getIndex()));
    LLVMValueRef constructorMemberFunctionIndexNode = createMDString(property.isStatic() ? "static" : !property.hasConstructor() ? "no-constructor" : ("" + property.getConstructorMemberFunction().getIndex()));
    LLVMValueRef typeNode = generateType(property.getType());
    LLVMValueRef nameNode = createMDString(property.getName());
    LLVMValueRef[] values = new LLVMValueRef[] {isAbstractNode, finalityNode, mutabilityNode, isUnbackedNode, isGetterImmutableNode, isSetterImmutableNode, isConstructorImmutableNode, sinceSpecifierNode,
                                                backingMemberIndexNode, getterMemberFunctionIndexNode, setterMemberFunctionIndexNode, constructorMemberFunctionIndexNode, typeNode, nameNode};
    return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
  }

  private static LLVMValueRef generateConstructor(Constructor constructor)
  {
    LLVMValueRef immutabilityNode = createMDString(constructor.isImmutable() ? "immutable" : "not-immutable");
    LLVMValueRef selfishnessNode = createMDString(constructor.isSelfish() ? "selfish" : "not-selfish");
    LLVMValueRef sinceSpecifierNode = generateSinceSpecifier(constructor.getSinceSpecifier());
    LLVMValueRef parametersNode = generateParameters(constructor.getParameters());
    LLVMValueRef thrownTypesNode = generateThrownTypes(constructor.getCheckedThrownTypes());
    LLVMValueRef[] values = new LLVMValueRef[] {immutabilityNode, selfishnessNode, sinceSpecifierNode, parametersNode, thrownTypesNode};
    return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
  }

  private static LLVMValueRef generateMethod(Method method)
  {
    LLVMValueRef nameNode = createMDString(method.getName());
    LLVMValueRef isAbstractNode = createMDString(method.isAbstract() ? "abstract" : "not-abstract");
    LLVMValueRef isImmutableNode = createMDString(method.isImmutable() ? "immutable" : "not-immutable");
    LLVMValueRef nativeNameNode = createMDString(method.getNativeName() == null ? "" : method.getNativeName());
    LLVMValueRef sinceSpecifierNode = generateSinceSpecifier(method.getSinceSpecifier());
    LLVMValueRef memberIndexNode = createMDString(method.isStatic() ? "static" : ("" + method.getMemberFunction().getIndex()));
    LLVMValueRef returnTypeNode = generateType(method.getReturnType());
    LLVMValueRef parametersNode = generateParameters(method.getParameters());
    LLVMValueRef thrownTypesNode = generateThrownTypes(method.getCheckedThrownTypes());
    LLVMValueRef[] values = new LLVMValueRef[] {nameNode, isAbstractNode, isImmutableNode, nativeNameNode, sinceSpecifierNode, memberIndexNode, returnTypeNode, parametersNode, thrownTypesNode};
    return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
  }

  private static <P extends Parameter> LLVMValueRef generateParameters(P[] parameters)
  {
    LLVMValueRef[] parameterNodes = new LLVMValueRef[parameters.length];
    for (int i = 0; i < parameters.length; ++i)
    {
      LLVMValueRef typeNode = generateType(parameters[i].getType());
      LLVMValueRef nameNode = createMDString(parameters[i].getName());
      LLVMValueRef isDefaultNode = createMDString(parameters[i] instanceof DefaultParameter ? "default" : "not-default");
      LLVMValueRef[] parameterValues = new LLVMValueRef[] {typeNode, nameNode, isDefaultNode};
      parameterNodes[i] = LLVM.LLVMMDNode(C.toNativePointerArray(parameterValues, false, true), parameterValues.length);
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(parameterNodes, false, true), parameterNodes.length);
  }

  private static LLVMValueRef generateThrownTypes(NamedType[] thrownTypes)
  {
    LLVMValueRef[] thrownTypeNodes = new LLVMValueRef[thrownTypes.length];
    for (int i = 0; i < thrownTypes.length; ++i)
    {
      thrownTypeNodes[i] = generateType(thrownTypes[i]);
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(thrownTypeNodes, false, true), thrownTypeNodes.length);
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
      DefaultParameter[] defaultParameters = functionType.getDefaultParameters();
      LLVMValueRef defaultParametersNode = generateParameters(defaultParameters);
      LLVMValueRef thrownTypesNode = generateThrownTypes(functionType.getThrownTypes());
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode, nullableNode, immutableNode, returnTypeNode, parameterTypesNode, defaultParametersNode, thrownTypesNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      LLVMValueRef sortNode = createMDString("named");
      LLVMValueRef immutableNode = createMDString(namedType.isExplicitlyImmutable() ? "immutable" : "not-immutable");
      String qualifiedName;
      if (namedType.getResolvedTypeDefinition() != null)
      {
        qualifiedName = namedType.getResolvedTypeDefinition().getQualifiedName().toString();
      }
      else // (namedType.getResolvedTypeParameter() != null)
      {
        qualifiedName = namedType.getResolvedTypeParameter().getName();
      }
      LLVMValueRef qualifiedNameNode = createMDString(qualifiedName);

      Type[] typeArguments = namedType.getTypeArguments();
      LLVMValueRef[] typeArgumentNodes = new LLVMValueRef[typeArguments == null ? 0 : typeArguments.length];
      for (int i = 0; typeArguments != null && i < typeArguments.length; ++i)
      {
        typeArgumentNodes[i] = generateType(typeArguments[i]);
      }
      LLVMValueRef typeArgumentsNode = LLVM.LLVMMDNode(C.toNativePointerArray(typeArgumentNodes, false, true), typeArgumentNodes.length);

      LLVMValueRef[] values = new LLVMValueRef[] {sortNode, nullableNode, immutableNode, qualifiedNameNode, typeArgumentsNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    if (type instanceof ObjectType)
    {
      ObjectType objectType = (ObjectType) type;
      LLVMValueRef sortNode = createMDString("object");
      LLVMValueRef immutableNode = createMDString(objectType.isExplicitlyImmutable() ? "immutable" : "not-immutable");
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode, nullableNode, immutableNode};
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
    if (type instanceof WildcardType)
    {
      WildcardType wildcardType = (WildcardType) type;
      LLVMValueRef sortNode = createMDString("wildcard");
      LLVMValueRef immutableNode = createMDString(wildcardType.isExplicitlyImmutable() ? "immutable" : "not-immutable");
      Type[] superTypes = wildcardType.getSuperTypes();
      Type[] subTypes = wildcardType.getSubTypes();
      LLVMValueRef[] superTypeNodes = new LLVMValueRef[superTypes.length];
      LLVMValueRef[] subTypeNodes = new LLVMValueRef[subTypes.length];
      for (int i = 0; i < superTypes.length; ++i)
      {
        superTypeNodes[i] = generateType(superTypes[i]);
      }
      for (int i = 0; i < subTypes.length; ++i)
      {
        subTypeNodes[i] = generateType(subTypes[i]);
      }
      LLVMValueRef superTypesNode = LLVM.LLVMMDNode(C.toNativePointerArray(superTypeNodes, false, true), superTypeNodes.length);
      LLVMValueRef subTypesNode = LLVM.LLVMMDNode(C.toNativePointerArray(subTypeNodes, false, true), subTypeNodes.length);
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode, nullableNode, immutableNode, superTypesNode, subTypesNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    throw new IllegalArgumentException("Internal metadata generation error: unknown sort of Type: " + type);
  }

  private static LLVMValueRef createMDString(String str)
  {
    return LLVM.LLVMMDString(str, str.getBytes().length);
  }
}
