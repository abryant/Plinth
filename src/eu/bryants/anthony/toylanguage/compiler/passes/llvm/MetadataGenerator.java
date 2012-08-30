package eu.bryants.anthony.toylanguage.compiler.passes.llvm;

import java.util.Collection;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMValueRef;
import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Method;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.type.ArrayType;
import eu.bryants.anthony.toylanguage.ast.type.FunctionType;
import eu.bryants.anthony.toylanguage.ast.type.NamedType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.toylanguage.ast.type.TupleType;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.ast.type.VoidType;

/*
 * Created on 28 Aug 2012
 */

/**
 * @author Anthony Bryant
 */
public class MetadataGenerator
{
  /**
   * Generates the metadata for the specified CompoundDefinition, and writes it into the specified module.
   * @param compoundDefinition - the CompoundDefinition to generate metadata for
   * @param module - the module to write the metadata to
   */
  public static void generateMetadata(CompoundDefinition compoundDefinition, LLVMModuleRef module)
  {
    String qualifiedName = compoundDefinition.getQualifiedName().toString();
    LLVMValueRef nameNode = LLVM.LLVMMDString(qualifiedName, qualifiedName.getBytes().length);

    Field[] nonStaticFields = compoundDefinition.getNonStaticFields();
    LLVMValueRef[] nonStaticFieldNodes = new LLVMValueRef[nonStaticFields.length];
    for (int i = 0; i < nonStaticFields.length; ++i)
    {
      nonStaticFieldNodes[i] = generateField(nonStaticFields[i]);
    }
    LLVMValueRef nonStaticFieldsNode = LLVM.LLVMMDNode(C.toNativePointerArray(nonStaticFieldNodes, false, true), nonStaticFieldNodes.length);

    Field[] allFields = compoundDefinition.getFields();
    LLVMValueRef[] staticFieldNodes = new LLVMValueRef[allFields.length - nonStaticFields.length];
    int staticIndex = 0;
    for (int i = 0; i < allFields.length; ++i)
    {
      if (allFields[i].isStatic())
      {
        staticFieldNodes[staticIndex] = generateField(allFields[i]);
        ++staticIndex;
      }
    }
    if (staticIndex != staticFieldNodes.length)
    {
      throw new IllegalStateException("Failed to generate the correct number of static field metadata nodes!");
    }
    LLVMValueRef staticFieldsNode = LLVM.LLVMMDNode(C.toNativePointerArray(staticFieldNodes, false, true), staticFieldNodes.length);

    Collection<Constructor> constructors = compoundDefinition.getConstructors();
    LLVMValueRef[] constructorNodes = new LLVMValueRef[constructors.size()];
    int constructorIndex = 0;
    for (Constructor constructor : constructors)
    {
      constructorNodes[constructorIndex] = generateConstructor(constructor);
      ++constructorIndex;
    }
    LLVMValueRef constructorsNode = LLVM.LLVMMDNode(C.toNativePointerArray(constructorNodes, false, true), constructorNodes.length);


    Method[] methods = compoundDefinition.getAllMethods();
    LLVMValueRef[] methodNodes = new LLVMValueRef[methods.length];
    for (int i = 0; i < methods.length; ++i)
    {
      methodNodes[i] = generateMethod(methods[i]);
    }
    LLVMValueRef methodsNode = LLVM.LLVMMDNode(C.toNativePointerArray(methodNodes, false, true), methodNodes.length);

    LLVMValueRef[] values = new LLVMValueRef[] {nameNode, nonStaticFieldsNode, staticFieldsNode, constructorsNode, methodsNode};
    LLVMValueRef resultNode = LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    LLVM.LLVMAddNamedMetadataOperand(module, "CompoundDefinitions", resultNode);
  }

  private static LLVMValueRef generateField(Field field)
  {
    LLVMValueRef typeNode = generateType(field.getType());
    LLVMValueRef nameNode = LLVM.LLVMMDString(field.getName(), field.getName().getBytes().length);
    LLVMValueRef[] values = new LLVMValueRef[] {typeNode, nameNode};
    return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
  }

  private static LLVMValueRef generateConstructor(Constructor constructor)
  {
    LLVMValueRef parametersNode = generateParameters(constructor.getParameters());
    LLVMValueRef[] values = new LLVMValueRef[] {parametersNode};
    return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
  }

  private static LLVMValueRef generateMethod(Method method)
  {
    LLVMValueRef nameNode = LLVM.LLVMMDString(method.getName(), method.getName().getBytes().length);
    LLVMValueRef isStaticNode = method.isStatic() ? LLVM.LLVMMDString("static", 6) : LLVM.LLVMMDString("not-static", 10);
    LLVMValueRef nativeNameNode = method.getNativeName() == null ? LLVM.LLVMMDString("", 0) : LLVM.LLVMMDString(method.getNativeName(), method.getNativeName().getBytes().length);
    LLVMValueRef returnTypeNode = generateType(method.getReturnType());
    LLVMValueRef parametersNode = generateParameters(method.getParameters());
    LLVMValueRef[] values = new LLVMValueRef[] {nameNode, isStaticNode, nativeNameNode, returnTypeNode, parametersNode};
    return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
  }

  private static LLVMValueRef generateParameters(Parameter[] parameters)
  {
    LLVMValueRef[] parameterNodes = new LLVMValueRef[parameters.length];
    for (int i = 0; i < parameters.length; ++i)
    {
      LLVMValueRef typeNode = generateType(parameters[i].getType());
      LLVMValueRef nameNode = LLVM.LLVMMDString(parameters[i].getName(), parameters[i].getName().getBytes().length);
      LLVMValueRef[] parameterValues = new LLVMValueRef[] {typeNode, nameNode};
      parameterNodes[i] = LLVM.LLVMMDNode(C.toNativePointerArray(parameterValues, false, true), parameterValues.length);
    }
    return LLVM.LLVMMDNode(C.toNativePointerArray(parameterNodes, false, true), parameterNodes.length);
  }

  private static LLVMValueRef generateType(Type type)
  {
    LLVMValueRef nullableNode = type.isNullable() ? LLVM.LLVMMDString("nullable", 8) : LLVM.LLVMMDString("not-nullable", 12);
    if (type instanceof ArrayType)
    {
      LLVMValueRef sortNode = LLVM.LLVMMDString("array", 5);
      LLVMValueRef baseTypeNode = generateType(((ArrayType) type).getBaseType());
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode, nullableNode, baseTypeNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    if (type instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) type;
      LLVMValueRef sortNode = LLVM.LLVMMDString("function", 8);
      LLVMValueRef returnTypeNode = generateType(functionType.getReturnType());
      Type[] parameterTypes = functionType.getParameterTypes();
      LLVMValueRef[] parameterTypeNodes = new LLVMValueRef[parameterTypes.length];
      for (int i = 0; i < parameterTypes.length; ++i)
      {
        parameterTypeNodes[i] = generateType(parameterTypes[i]);
      }
      LLVMValueRef parameterTypesNode = LLVM.LLVMMDNode(C.toNativePointerArray(parameterTypeNodes, false, true), parameterTypeNodes.length);
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode, nullableNode, returnTypeNode, parameterTypesNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      LLVMValueRef sortNode = LLVM.LLVMMDString("compound", 8);
      String qualifiedName = namedType.getResolvedDefinition().getQualifiedName().toString();
      LLVMValueRef qualifiedNameNode = LLVM.LLVMMDString(qualifiedName, qualifiedName.getBytes().length);
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode, nullableNode, qualifiedNameNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    if (type instanceof PrimitiveType)
    {
      PrimitiveTypeType primitiveTypeType = ((PrimitiveType) type).getPrimitiveTypeType();
      LLVMValueRef sortNode = LLVM.LLVMMDString("primitive", 9);
      LLVMValueRef nameNode = LLVM.LLVMMDString(primitiveTypeType.name, primitiveTypeType.name.getBytes().length);
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode, nullableNode, nameNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      LLVMValueRef sortNode = LLVM.LLVMMDString("tuple", 5);
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
      LLVMValueRef sortNode = LLVM.LLVMMDString("void", 4);
      LLVMValueRef[] values = new LLVMValueRef[] {sortNode};
      return LLVM.LLVMMDNode(C.toNativePointerArray(values, false, true), values.length);
    }
    throw new IllegalArgumentException("Internal metadata generation error: unknown sort of Type: " + type);
  }
}
