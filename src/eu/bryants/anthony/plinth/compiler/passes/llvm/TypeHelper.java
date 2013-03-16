package eu.bryants.anthony.plinth.compiler.passes.llvm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMBasicBlockRef;
import nativelib.llvm.LLVM.LLVMBuilderRef;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMTypeRef;
import nativelib.llvm.LLVM.LLVMValueRef;
import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod.BuiltinMethodType;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.metadata.MemberVariable;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.NullType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.compiler.passes.SpecialTypeHandler;
import eu.bryants.anthony.plinth.compiler.passes.TypeChecker;

/*
 * Created on 23 Sep 2012
 */

/**
 * A class which helps a CodeGenerator convert the AST into bitcode by providing methods to convert types into their native representations, and methods to convert between these native types.
 * @author Anthony Bryant
 */
public class TypeHelper
{
  private static final String BASE_CHANGE_FUNCTION_PREFIX = "_base_change_o";

  private CodeGenerator codeGenerator;
  private VirtualFunctionHandler virtualFunctionHandler;
  private RTTIHelper rttiHelper;

  private LLVMModuleRef module;

  private LLVMTypeRef opaqueType;
  private LLVMTypeRef objectType;
  private Map<String, LLVMTypeRef> nativeArrayTypes = new HashMap<String, LLVMTypeRef>();
  private Map<TypeDefinition, LLVMTypeRef> nativeNamedTypes = new HashMap<TypeDefinition, LLVMTypeRef>();

  /**
   * Creates a new TypeHelper to build type conversions with the specified builder.
   * @param codeGenerator - the CodeGenerator to use to generate any miscellaneous sections of code, such as null checks
   * @param virtualFunctionHandler - the VirtualFunctionHandler to handle building the types of virtual function tables
   * @param module - the LLVMModuleRef that this TypeHelper will build inside
   */
  public TypeHelper(CodeGenerator codeGenerator, VirtualFunctionHandler virtualFunctionHandler, LLVMModuleRef module)
  {
    this.codeGenerator = codeGenerator;
    this.virtualFunctionHandler = virtualFunctionHandler;
    this.module = module;
    opaqueType = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), "opaque");
  }

  /**
   * Initialises this TypeHelper, so that it has all of the references required to operate..
   * @param rttiHelper - the RTTIHelper to set
   */
  public void initialise(RTTIHelper rttiHelper)
  {
    this.rttiHelper = rttiHelper;
  }

  /**
   * @return an opaque pointer type
   */
  public LLVMTypeRef getOpaquePointer()
  {
    return LLVM.LLVMPointerType(opaqueType, 0);
  }

  /**
   * @return the result type of a landingpad instruction
   */
  public LLVMTypeRef getLandingPadType()
  {
    LLVMTypeRef[] subTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0), LLVM.LLVMInt32Type()};
    return LLVM.LLVMStructType(C.toNativePointerArray(subTypes, false, true), subTypes.length, false);
  }

  /**
   * Finds the standard representation for the specified type, to be used when passing parameters, or storing fields, etc.
   * @param type - the type to find the native type of
   * @return the standard native representation of the specified Type
   */
  public LLVMTypeRef findStandardType(Type type)
  {
    return findNativeType(type, false);
  }

  /**
   * Finds the temporary representation for the specified type, to be used when manipulating values inside a function.
   * @param type - the type to find the native type of
   * @return the temporary native representation of the specified Type
   */
  public LLVMTypeRef findTemporaryType(Type type)
  {
    return findNativeType(type, true);
  }

  /**
   * Finds the native representation of the specified type. The native representation can be of two forms: standard, and temporary.
   * These forms are used in different places, and can be converted between using other utility functions.
   * This method is not public, so to find a standard representation of a type, use findStandardType(Type); or to find a temporary representation, use findTemporaryType(Type).
   * @param type - the type to find the native representation of
   * @param temporary - true if the representation should be of the temporary form, or false if it should be in the standard form
   * @return the native type of the specified type
   */
  private LLVMTypeRef findNativeType(Type type, boolean temporary)
  {
    if (type instanceof PrimitiveType)
    {
      LLVMTypeRef nonNullableType;
      PrimitiveTypeType primitiveTypeType = ((PrimitiveType) type).getPrimitiveTypeType();
      if (primitiveTypeType == PrimitiveTypeType.DOUBLE)
      {
        nonNullableType = LLVM.LLVMDoubleType();
      }
      else if (primitiveTypeType == PrimitiveTypeType.FLOAT)
      {
        nonNullableType = LLVM.LLVMFloatType();
      }
      else
      {
        nonNullableType = LLVM.LLVMIntType(primitiveTypeType.getBitCount());
      }
      if (type.isNullable())
      {
        // tuple the non-nullable type with a boolean, so that we can tell whether or not the value is null
        LLVMTypeRef[] types = new LLVMTypeRef[] {LLVM.LLVMInt1Type(), nonNullableType};
        return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
      }
      return nonNullableType;
    }
    if (type instanceof ArrayType)
    {
      ArrayType arrayType = new ArrayType(false, false, ((ArrayType) type).getBaseType(), null);
      String mangledTypeName = arrayType.getMangledName();
      LLVMTypeRef existingType = nativeArrayTypes.get(mangledTypeName);
      if (existingType != null)
      {
        return LLVM.LLVMPointerType(existingType, 0);
      }
      LLVMTypeRef llvmArrayType = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), mangledTypeName);
      nativeArrayTypes.put(mangledTypeName, llvmArrayType);

      LLVMTypeRef baseType = findNativeType(arrayType.getBaseType(), false);
      LLVMTypeRef llvmArray = LLVM.LLVMArrayType(baseType, 0);
      LLVMTypeRef rttiType = rttiHelper.getGenericInstanceRTTIType();
      LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(virtualFunctionHandler.getObjectVFTType(), 0);
      LLVMTypeRef[] structureTypes = new LLVMTypeRef[] {rttiType, vftPointerType, LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), llvmArray};
      LLVM.LLVMStructSetBody(llvmArrayType, C.toNativePointerArray(structureTypes, false, true), structureTypes.length, false);
      return LLVM.LLVMPointerType(llvmArrayType, 0);
    }
    if (type instanceof FunctionType)
    {
      // create a tuple of an opaque pointer and a function pointer which has an opaque pointer as its first argument
      FunctionType functionType = (FunctionType) type;
      LLVMTypeRef rttiPointerType = rttiHelper.getGenericInstanceRTTIType();
      LLVMTypeRef llvmOpaquePointerType = LLVM.LLVMPointerType(opaqueType, 0);
      LLVMTypeRef llvmFunctionPointer = findRawFunctionPointerType(functionType);
      LLVMTypeRef[] subTypes = new LLVMTypeRef[] {rttiPointerType, llvmOpaquePointerType, llvmFunctionPointer};
      return LLVM.LLVMStructType(C.toNativePointerArray(subTypes, false, true), subTypes.length, false);
    }
    if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      Type[] subTypes = tupleType.getSubTypes();
      LLVMTypeRef[] llvmSubTypes = new LLVMTypeRef[subTypes.length];
      for (int i = 0; i < subTypes.length; i++)
      {
        llvmSubTypes[i] = findNativeType(subTypes[i], temporary);
      }
      LLVMTypeRef nonNullableType = LLVM.LLVMStructType(C.toNativePointerArray(llvmSubTypes, false, true), llvmSubTypes.length, false);
      if (tupleType.isNullable())
      {
        // tuple the non-nullable type with a boolean, so that we can tell whether or not the value is null
        LLVMTypeRef[] types = new LLVMTypeRef[] {LLVM.LLVMInt1Type(), nonNullableType};
        return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
      }
      return nonNullableType;
    }
    if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      TypeDefinition typeDefinition = namedType.getResolvedTypeDefinition();
      // check whether the type has been cached
      LLVMTypeRef existingType = nativeNamedTypes.get(typeDefinition);
      if (existingType != null)
      {
        if (typeDefinition instanceof CompoundDefinition)
        {
          if (temporary)
          {
            // for temporary CompoundDefinition values, we use a pointer to the non-nullable type, whether or not the type is nullable
            return LLVM.LLVMPointerType(existingType, 0);
          }
          if (namedType.isNullable())
          {
            // tuple the non-nullable type with a boolean, so that we can tell whether or not the value is null
            // this is not necessary for ClassDefinitions, since they are pointers which can actually be null
            LLVMTypeRef[] types = new LLVMTypeRef[] {LLVM.LLVMInt1Type(), existingType};
            return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
          }
        }
        return existingType;
      }
      // the type isn't cached, so create it
      if (typeDefinition instanceof ClassDefinition)
      {
        // cache the LLVM type before we recurse, so that once we recurse, everything will be able to use this type instead of recreating it and possibly recursing infinitely
        // later on, we add the fields using LLVMStructSetBody
        LLVMTypeRef structType = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), typeDefinition.getQualifiedName().toString());
        LLVMTypeRef pointerToStruct = LLVM.LLVMPointerType(structType, 0);
        nativeNamedTypes.put(typeDefinition, pointerToStruct);

        // add the fields to the struct type (findClassSubTypes() will call findNativeType() recursively)
        LLVMTypeRef[] llvmSubTypes = findClassSubTypes((ClassDefinition) typeDefinition);
        LLVM.LLVMStructSetBody(structType, C.toNativePointerArray(llvmSubTypes, false, true), llvmSubTypes.length, false);
        return pointerToStruct;
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        // cache the LLVM type before we recurse, so that once we recurse, everything will be able to use this type instead of recreating it
        // later on, we add the fields using LLVMStructSetBody
        LLVMTypeRef nonNullableStructType = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), typeDefinition.getQualifiedName().toString());
        nativeNamedTypes.put(typeDefinition, nonNullableStructType);

        // add the fields to the struct recursively
        MemberVariable[] instanceVariables = ((CompoundDefinition) typeDefinition).getMemberVariables();
        LLVMTypeRef[] llvmSubTypes = new LLVMTypeRef[instanceVariables.length];
        for (int i = 0; i < instanceVariables.length; i++)
        {
          llvmSubTypes[i] = findNativeType(instanceVariables[i].getType(), false);
        }
        LLVM.LLVMStructSetBody(nonNullableStructType, C.toNativePointerArray(llvmSubTypes, false, true), llvmSubTypes.length, false);
        if (temporary)
        {
          // for temporary values, we use a pointer to the non-nullable type, whether or not the type is nullable
          return LLVM.LLVMPointerType(nonNullableStructType, 0);
        }
        if (namedType.isNullable())
        {
          // tuple the non-nullable type with a boolean, so that we can tell whether or not the value is null
          LLVMTypeRef[] types = new LLVMTypeRef[] {LLVM.LLVMInt1Type(), nonNullableStructType};
          return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
        }
        return nonNullableStructType;
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        LLVMTypeRef vftType = LLVM.LLVMPointerType(virtualFunctionHandler.getVFTType(typeDefinition), 0);
        LLVMTypeRef objectType = findNativeType(new ObjectType(false, false, null), false);
        LLVMTypeRef[] types = new LLVMTypeRef[] {vftType, objectType};
        return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
      }
    }
    if (type instanceof ObjectType)
    {
      if (objectType != null)
      {
        return LLVM.LLVMPointerType(objectType, 0);
      }
      objectType = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), "object");

      LLVMTypeRef rttiType = rttiHelper.getGenericInstanceRTTIType();
      LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(virtualFunctionHandler.getObjectVFTType(), 0);
      LLVMTypeRef[] structSubTypes = new LLVMTypeRef[] {rttiType, vftPointerType};
      LLVM.LLVMStructSetBody(objectType, C.toNativePointerArray(structSubTypes, false, true), structSubTypes.length, false);
      return LLVM.LLVMPointerType(objectType, 0);
    }
    if (type instanceof NullType)
    {
      return LLVM.LLVMStructType(C.toNativePointerArray(new LLVMTypeRef[0], false, true), 0, false);
    }
    if (type instanceof VoidType)
    {
      return LLVM.LLVMVoidType();
    }
    throw new IllegalStateException("Unexpected Type: " + type);
  }

  /**
   * Finds a function pointer type in its raw form, before being tupled with its RTTI and first argument (always an opaque pointer).
   * This <b>IS NOT</b> a full function type, and should not be used as such.
   * @param functionType - the function type to find the raw LLVM form of
   * @return the LLVMTypeRef corresponding to the raw form of the specified function type
   */
  // package protected and not private, because it needs to be accessible to CodeGenerator for buildNullCheck()
  LLVMTypeRef findRawFunctionPointerType(FunctionType functionType)
  {
    LLVMTypeRef llvmFunctionReturnType = findNativeType(functionType.getReturnType(), false);
    Type[] parameterTypes = functionType.getParameterTypes();
    LLVMTypeRef[] llvmParameterTypes = new LLVMTypeRef[parameterTypes.length + 1];
    llvmParameterTypes[0] = LLVM.LLVMPointerType(opaqueType, 0);
    for (int i = 0; i < parameterTypes.length; ++i)
    {
      llvmParameterTypes[i + 1] = findNativeType(parameterTypes[i], false);
    }
    LLVMTypeRef llvmFunctionType = LLVM.LLVMFunctionType(llvmFunctionReturnType, C.toNativePointerArray(llvmParameterTypes, false, true), llvmParameterTypes.length, false);
    return LLVM.LLVMPointerType(llvmFunctionType, 0);
  }

  /**
   * Finds the raw string type, which is just the type of []ubyte, as an LLVM struct.
   * @return the raw LLVM type of a string constant (i.e. []ubyte)
   */
  public LLVMTypeRef findRawStringType()
  {
    return findNativeType(new ArrayType(false, false, new PrimitiveType(false, PrimitiveTypeType.UBYTE, null), null), false);
  }

  /**
   * Finds an object type specialised to include some specialised data of the specified type.
   * @param specialisationType - the type that this object should hold (to be appended to the normal object type representation)
   * @return an object type, with an extra field to contain the standard type representation of the specified specialisation type
   */
  public LLVMTypeRef findSpecialisedObjectType(Type specialisationType)
  {
    LLVMTypeRef rttiType = rttiHelper.getGenericInstanceRTTIType();
    LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(virtualFunctionHandler.getObjectVFTType(), 0);
    LLVMTypeRef llvmSpecialisedType = findStandardType(specialisationType);
    LLVMTypeRef[] structSubTypes = new LLVMTypeRef[] {rttiType, vftPointerType, llvmSpecialisedType};
    LLVMTypeRef structType = LLVM.LLVMStructType(C.toNativePointerArray(structSubTypes, false, true), structSubTypes.length, false);
    return structType;
  }

  /**
   * Finds the native (LLVM) type of the specified Method
   * @param method - the Method to find the LLVM type of
   * @return the LLVMTypeRef representing the type of the specified Method
   */
  public LLVMTypeRef findMethodType(Method method)
  {
    Parameter[] parameters = method.getParameters();
    LLVMTypeRef[] types = new LLVMTypeRef[1 + parameters.length];
    // add the 'this' type to the function - 'this' always has a temporary type representation
    if (method.isStatic())
    {
      // for static methods, we add an unused opaque*, so that the static method can be easily converted to a function type
      types[0] = getOpaquePointer();
    }
    else if (method instanceof BuiltinMethod)
    {
      types[0] = findTemporaryType(((BuiltinMethod) method).getBaseType());
    }
    else if (method.getContainingTypeDefinition() instanceof ClassDefinition)
    {
      types[0] = findTemporaryType(new NamedType(false, method.isImmutable(), method.getContainingTypeDefinition()));
    }
    else if (method.getContainingTypeDefinition() instanceof CompoundDefinition)
    {
      types[0] = findTemporaryType(new NamedType(false, method.isImmutable(), method.getContainingTypeDefinition()));
    }
    else if (method.getContainingTypeDefinition() instanceof InterfaceDefinition)
    {
      types[0] = findTemporaryType(new ObjectType(false, method.isImmutable(), null));
    }
    for (int i = 0; i < parameters.length; ++i)
    {
      types[i + 1] = findStandardType(parameters[i].getType());
    }
    LLVMTypeRef resultType = findStandardType(method.getReturnType());

    return LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);
  }

  /**
   * Finds the native (LLVM) type of the getter of the specified Property
   * @param property - the Property to find the LLVM type of the getter of
   * @return the LLVMTypeRef representing the type of the specified Property's getter method
   */
  public LLVMTypeRef findPropertyGetterType(Property property)
  {
    TypeDefinition typeDefinition = property.getContainingTypeDefinition();
    LLVMTypeRef[] types = new LLVMTypeRef[1];
    if (property.isStatic())
    {
      types[0] = getOpaquePointer();
    }
    else
    {
      if (typeDefinition instanceof InterfaceDefinition)
      {
        types[0] = findTemporaryType(new ObjectType(false, false, null));
      }
      else
      {
        types[0] = findTemporaryType(new NamedType(false, false, typeDefinition));
      }
    }
    LLVMTypeRef resultType = findStandardType(property.getType());
    return LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);
  }

  /**
   * Finds the native (LLVM) type of the setter or constructor of the specified Property
   * @param property - the Property to find the LLVM type of the setter/constructor of
   * @return the LLVMTypeRef representing the type of the specified Property's setter/constructor method
   */
  public LLVMTypeRef findPropertySetterConstructorType(Property property)
  {
    TypeDefinition typeDefinition = property.getContainingTypeDefinition();
    LLVMTypeRef[] types = new LLVMTypeRef[2];
    if (property.isStatic())
    {
      types[0] = getOpaquePointer();
    }
    else
    {
      if (typeDefinition instanceof InterfaceDefinition)
      {
        types[0] = findTemporaryType(new ObjectType(false, false, null));
      }
      else
      {
        types[0] = findTemporaryType(new NamedType(false, false, typeDefinition));
      }
    }
    types[1] = findStandardType(property.getType());
    LLVMTypeRef resultType = LLVM.LLVMVoidType();
    return LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);
  }

  /**
   * Finds the sub-types of the native representation of the specified ClassDefinition, including fields and virtual function table pointers.
   * @param classDefinition - the class definition to find the sub-types of
   * @return the sub-types of the specified ClassDefinition
   */
  private LLVMTypeRef[] findClassSubTypes(ClassDefinition classDefinition)
  {
    InterfaceDefinition[] implementedInterfaces = findSubClassInterfaces(classDefinition);
    ClassDefinition superClassDefinition = classDefinition.getSuperClassDefinition();
    LLVMTypeRef[] subTypes;
    MemberVariable[] nonStaticVariables = classDefinition.getMemberVariables();
    int offset; // offset to the class VFT
    if (superClassDefinition == null)
    {
      // 1 RTTI pointer, 1 object-VFT (for builtin methods), 1 class VFT, some interface VFTs, and some fields
      subTypes = new LLVMTypeRef[3 + implementedInterfaces.length + nonStaticVariables.length];
      subTypes[0] = rttiHelper.getGenericInstanceRTTIType();
      subTypes[1] = LLVM.LLVMPointerType(virtualFunctionHandler.getObjectVFTType(), 0);
      offset = 2;
    }
    else
    {
      LLVMTypeRef[] superClassSubTypes = findClassSubTypes(superClassDefinition);
      // everything from the super-class, 1 class VFT, some interface VFTs, and some fields
      // we only include interfaces which were not included in any super-classes
      subTypes = new LLVMTypeRef[superClassSubTypes.length + 1 + implementedInterfaces.length + nonStaticVariables.length];
      System.arraycopy(superClassSubTypes, 0, subTypes, 0, superClassSubTypes.length);
      offset = superClassSubTypes.length;
    }
    subTypes[offset] = LLVM.LLVMPointerType(virtualFunctionHandler.getVFTType(classDefinition), 0);
    for (int i = 0; i < implementedInterfaces.length; ++i)
    {
      subTypes[offset + 1 + i] = LLVM.LLVMPointerType(virtualFunctionHandler.getVFTType(implementedInterfaces[i]), 0);
    }
    offset += 1 + implementedInterfaces.length;
    for (int i = 0; i < nonStaticVariables.length; ++i)
    {
      subTypes[offset + i] = findNativeType(nonStaticVariables[i].getType(), false);
    }
    return subTypes;
  }

  /**
   * Finds the list of interfaces which are implemented in the specified ClassDefinition, but none of its super-classes, in linearisation order.
   * @param classDefinition - the ClassDefinition to find the list of interfaces for
   * @return the list of interfaces for the specified sub-class in linearisation order
   */
  InterfaceDefinition[] findSubClassInterfaces(ClassDefinition classDefinition)
  {
    List<InterfaceDefinition> result = new LinkedList<InterfaceDefinition>();
    for (TypeDefinition typeDefinition : classDefinition.getInheritanceLinearisation())
    {
      if (typeDefinition instanceof InterfaceDefinition)
      {
        result.add((InterfaceDefinition) typeDefinition);
      }
    }
    // since the direct super-class's linearisation contains everything inherited above this class, we don't need to go through the whole hierarchy ourselves
    ClassDefinition superClass = classDefinition.getSuperClassDefinition();
    if (superClass != null)
    {
      for (TypeDefinition typeDefinition : superClass.getInheritanceLinearisation())
      {
        result.remove(typeDefinition);
      }
    }
    return result.toArray(new InterfaceDefinition[result.size()]);
  }

  /**
   * Finds the pointer to the specified field inside the specified value.
   * The value should be a NamedType in a temporary type representation, and should be for the type which contains the specified field, or a subtype thereof.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param baseValue - the base value to get the field of
   * @param memberVariable - the MemberVariable to extract
   * @return a pointer to the specified field inside baseValue
   */
  public LLVMValueRef getMemberPointer(LLVMBuilderRef builder, LLVMValueRef baseValue, MemberVariable memberVariable)
  {
    TypeDefinition typeDefinition = memberVariable.getEnclosingTypeDefinition();
    int index = memberVariable.getMemberIndex();
    if (typeDefinition instanceof ClassDefinition)
    {
      // skip the RTTI pointer and the object VFT from the top-level class
      index += 2;
      // skip the super-class representations
      ClassDefinition superClassDefinition = ((ClassDefinition) typeDefinition).getSuperClassDefinition();
      while (superClassDefinition != null)
      {
        // 1 class VFT, some interface VFTs, and some fields
        index += 1 + findSubClassInterfaces(superClassDefinition).length + superClassDefinition.getMemberVariables().length;
        superClassDefinition = superClassDefinition.getSuperClassDefinition();
      }
      // skip the virtual function tables from this class
      index += 1 + findSubClassInterfaces((ClassDefinition) typeDefinition).length;
    }
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), index, false)};
    return LLVM.LLVMBuildGEP(builder, baseValue, C.toNativePointerArray(indices, false, true), indices.length, "");
  }

  /**
   * Gets the poiner to the length field of the specified array.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param array - the array to get the length field of, in a temporary type representation
   * @return a pointer to the length field of the specified array
   */
  public LLVMValueRef getArrayLengthPointer(LLVMBuilderRef builder, LLVMValueRef array)
  {
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 2, false)};
    return LLVM.LLVMBuildGEP(builder, array, C.toNativePointerArray(indices, false, true), indices.length, "");
  }

  /**
   * Gets the pointer to the specified element of the specified array.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param array - the array to get the element of, in a temporary type representation
   * @param index - the index into the array to go, as a uint
   * @return a pointer to the specified array element (in a pointer-to-standard type representation)
   */
  public LLVMValueRef getArrayElementPointer(LLVMBuilderRef builder, LLVMValueRef array, LLVMValueRef index)
  {
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 3, false),
                                                 index};
    return LLVM.LLVMBuildGEP(builder, array, C.toNativePointerArray(indices, false, true), indices.length, "");
  }

  /**
   * Converts the specified Method's callee to the correct type to be passed into the Method.
   * This method assumes that the callee is already a subtype of the correct type to pass into the Method.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param callee - the callee to convert, in a temporary type representation
   * @param calleeType - the current type of the callee
   * @param method - the Method that the callee will be passed into
   * @return the converted callee
   */
  public LLVMValueRef convertMethodCallee(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef callee, Type calleeType, Method method)
  {
    if (method.isStatic())
    {
      // the callee should already have its required type (i.e. a null opaque pointer)
      return callee;
    }
    if (method instanceof BuiltinMethod)
    {
      BuiltinMethod builtinMethod = (BuiltinMethod) method;
      return convertTemporary(builder, landingPadContainer, callee, calleeType, builtinMethod.getBaseType());
    }
    TypeDefinition containingDefinition = method.getContainingTypeDefinition();
    if (containingDefinition != null)
    {
      // convert the callee to the correct type for this Method
      // this is determined by the type definition which it is declared in, so that it matches the VFT we look up the Method in
      if (containingDefinition instanceof ClassDefinition)
      {
        return convertTemporary(builder, landingPadContainer, callee, calleeType, new NamedType(false, false, containingDefinition));
      }
      // for interfaces, the callee will be of type object
      if (containingDefinition instanceof InterfaceDefinition)
      {
        return convertTemporary(builder, landingPadContainer, callee, calleeType, new ObjectType(false, false, null));
      }
    }
    // the callee should already have its required value
    return callee;
  }

  /**
   * Converts the specified Property's callee to the correct type to be passed into one of its property methods
   * This method assumes that the callee is already a subtype of the correct type to pass into the Property method.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param callee - the callee to convert, in a temporary type representation
   * @param calleeType - the current type of the callee
   * @param property - the Property that the callee will be passed into a method of
   * @return the converted callee
   */
  public LLVMValueRef convertPropertyCallee(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef callee, Type calleeType, Property property)
  {
    if (property.isStatic())
    {
      // the callee should already have its required type (i.e. a null opaque pointer)
      return callee;
    }
    TypeDefinition containingDefinition = property.getContainingTypeDefinition();
    // convert the callee to the correct type for this Property
    // this is determined by the type definition which it is declared in, so that it matches the VFT we look up the Method in
    if (containingDefinition instanceof ClassDefinition)
    {
      return convertTemporary(builder, landingPadContainer, callee, calleeType, new NamedType(false, false, containingDefinition));
    }
    // for interfaces, the callee will be of type object
    if (containingDefinition instanceof InterfaceDefinition)
    {
      return convertTemporary(builder, landingPadContainer, callee, calleeType, new ObjectType(false, false, null));
    }
    throw new IllegalArgumentException("Cannot convert a property callee for the property: " + containingDefinition + "." + property.getName());
  }

  /**
   * Gets a function that takes a callee of type 'object' and converts it to the base type of the specified method before calling that method.
   * @param method - the method to find the base change method for
   * @return the base change method for the specified method
   */
  public LLVMValueRef getBaseChangeFunction(Method method)
  {
    if (method.isStatic())
    {
      throw new IllegalArgumentException("Cannot change the base of a static method");
    }
    String mangledName = BASE_CHANGE_FUNCTION_PREFIX + method.getMangledName();

    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    Parameter[] parameters = method.getParameters();
    LLVMTypeRef[] types = new LLVMTypeRef[1 + parameters.length];
    ObjectType objectType = new ObjectType(false, false, null);
    types[0] = findTemporaryType(objectType);
    for (int i = 0; i < parameters.length; ++i)
    {
      types[i + 1] = findStandardType(parameters[i].getType());
    }
    LLVMTypeRef resultType = findStandardType(method.getReturnType());
    LLVMTypeRef objectFunctionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);

    LLVMValueRef objectFunction = LLVM.LLVMAddFunction(module, mangledName, objectFunctionType);
    LLVM.LLVMSetLinkage(objectFunction, LLVM.LLVMLinkage.LLVMLinkOnceODRLinkage);
    LLVM.LLVMSetVisibility(objectFunction, LLVM.LLVMVisibility.LLVMHiddenVisibility);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(objectFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    Type baseType;
    if (method.getContainingTypeDefinition() != null)
    {
      baseType = new NamedType(false, false, method.getContainingTypeDefinition());
    }
    else if (method instanceof BuiltinMethod)
    {
      baseType = ((BuiltinMethod) method).getBaseType();
    }
    else
    {
      throw new IllegalArgumentException("Method has no base type: " + method);
    }
    LLVMValueRef callee = LLVM.LLVMGetParam(objectFunction, 0);
    LLVMValueRef convertedBaseValue = convertTemporary(builder, landingPadContainer, callee, objectType, baseType);

    LLVMValueRef methodFunction = codeGenerator.lookupMethodFunction(builder, landingPadContainer, convertedBaseValue, baseType, method, false);
    LLVMValueRef[] arguments = new LLVMValueRef[1 + parameters.length];
    arguments[0] = convertedBaseValue;
    for (int i = 0; i < parameters.length; ++i)
    {
      arguments[i + 1] = LLVM.LLVMGetParam(objectFunction, i + 1);
    }
    LLVMBasicBlockRef methodInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "methodInvokeContinue");
    LLVMValueRef result = LLVM.LLVMBuildInvoke(builder, methodFunction, C.toNativePointerArray(arguments, false, true), arguments.length, methodInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, methodInvokeContinueBlock);
    if (method.getReturnType() instanceof VoidType)
    {
      LLVM.LLVMBuildRetVoid(builder);
    }
    else
    {
      LLVM.LLVMBuildRet(builder, result);
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return objectFunction;
  }

  /**
   * Initialises the specified value as a compound definition of the specified type.
   * This method performs any initialisation which must happen before the constructor is called, such as zeroing fields which have default values.
   * @param compoundDefinition - the CompoundDefinition to initialise the value as
   * @param compoundValue - the value to initialise, which is a temporary type representation of the specified CompoundDefinition
   */
  void initialiseCompoundType(LLVMBuilderRef builder, CompoundDefinition compoundDefinition, LLVMValueRef compoundValue)
  {
    // initialise all of the fields which have default values to zero/null
    for (MemberVariable variable : compoundDefinition.getMemberVariables())
    {
      if (variable.getType().hasDefaultValue())
      {
        LLVMValueRef pointer = getMemberPointer(builder, compoundValue, variable);
        LLVM.LLVMBuildStore(builder, LLVM.LLVMConstNull(findStandardType(variable.getType())), pointer);
      }
    }
  }

  /**
   * Builds code to throw a cast error, with the specified from type, to type, and reason.
   * Note: this method finishes the current LLVM basic block.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param fromType - the fromType string to call the CastError constructor with
   * @param toType - the toType string to call the CastError constructor with
   * @param reason - the reason string to call the CastError constructor with (can be null)
   */
  public void buildThrowCastError(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, String fromType, String toType, String reason)
  {
    buildThrowCastError(builder, landingPadContainer, codeGenerator.buildStringCreation(builder, landingPadContainer, fromType), toType, reason);
  }

  /**
   * Builds code to throw a cast error, with the specified from type, to type, and reason.
   * Note: this method finishes the current LLVM basic block.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param llvmFromType - the fromType string to call the CastError constructor with, in a temporary type representation
   * @param toType - the toType string to call the CastError constructor with
   * @param reason - the reason string to call the CastError constructor with (can be null)
   */
  public void buildThrowCastError(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef llvmFromType, String toType, String reason)
  {
    ClassDefinition castErrorTypeDefinition = (ClassDefinition) SpecialTypeHandler.CAST_ERROR_TYPE.getResolvedTypeDefinition();
    LLVMValueRef[] allocatorArgs = new LLVMValueRef[0];
    LLVMValueRef allocator = codeGenerator.getAllocatorFunction(castErrorTypeDefinition);
    LLVMBasicBlockRef allocatorInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "castErrorAllocatorContinue");
    LLVMValueRef allocatedCastError = LLVM.LLVMBuildInvoke(builder, allocator, C.toNativePointerArray(allocatorArgs, false, true), allocatorArgs.length, allocatorInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, allocatorInvokeContinueBlock);

    llvmFromType = convertTemporaryToStandard(builder, llvmFromType, SpecialTypeHandler.STRING_TYPE);
    LLVMValueRef llvmToType = codeGenerator.buildStringCreation(builder, landingPadContainer, toType);
    llvmToType = convertTemporaryToStandard(builder, llvmToType, SpecialTypeHandler.STRING_TYPE);
    LLVMValueRef llvmReason;
    Type nullableStringType = TypeChecker.findTypeWithNullability(SpecialTypeHandler.STRING_TYPE, true);
    if (reason == null)
    {
      llvmReason = LLVM.LLVMConstNull(findStandardType(nullableStringType));
    }
    else
    {
      llvmReason = codeGenerator.buildStringCreation(builder, landingPadContainer, reason);
      llvmReason = convertTemporaryToStandard(builder, landingPadContainer, llvmReason, SpecialTypeHandler.STRING_TYPE, nullableStringType);
    }

    LLVMValueRef constructorFunction = codeGenerator.getConstructorFunction(SpecialTypeHandler.castErrorTypesReasonConstructor);
    LLVMValueRef[] constructorArguments = new LLVMValueRef[] {allocatedCastError, llvmFromType, llvmToType, llvmReason};
    LLVMBasicBlockRef constructorInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "castErrorConstructorContinue");
    LLVM.LLVMBuildInvoke(builder, constructorFunction, C.toNativePointerArray(constructorArguments, false, true), constructorArguments.length, constructorInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, constructorInvokeContinueBlock);
    LLVMValueRef castError = convertTemporaryToStandard(builder, allocatedCastError, SpecialTypeHandler.CAST_ERROR_TYPE);

    LLVMValueRef unwindException = codeGenerator.buildCreateException(builder, castError);
    codeGenerator.buildThrow(builder, landingPadContainer, unwindException);
  }

  /**
   * Builds a null check which will throw a CastError on failure.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the value to perform the null check on, in a temporary type representation
   * @param from - the Type of value
   * @param to - the Type to pass to the CastError if the value is null
   */
  private void buildCastNullCheck(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef value, Type from, Type to)
  {
    LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
    LLVMBasicBlockRef continueBlock = LLVM.LLVMAddBasicBlock(builder, "castNullCheckContinue");
    LLVMBasicBlockRef nullBlock = LLVM.LLVMAddBasicBlock(builder, "castNullFailure");
    LLVM.LLVMBuildCondBr(builder, isNotNull, continueBlock, nullBlock);

    LLVM.LLVMPositionBuilderAtEnd(builder, nullBlock);
    buildThrowCastError(builder, landingPadContainer, "null", to.toString(), null);

    LLVM.LLVMPositionBuilderAtEnd(builder, continueBlock);
  }

  /**
   * Converts the specified value from the specified 'from' type to the specified 'to' type, as a temporary.
   * This method assumes that the incoming value has a temporary native type, and produces a result with a temporary native type.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the value to convert
   * @param from - the Type to convert from
   * @param to - the Type to convert to
   * @return the converted value
   */
  public LLVMValueRef convertTemporary(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef value, Type from, Type to)
  {
    if (from.isRuntimeEquivalent(to))
    {
      return value;
    }

    // perform a null check if necessary, and throw a CastError if it fails
    if (from.isNullable() && !to.isNullable())
    {
      LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
      LLVMBasicBlockRef continueBlock = LLVM.LLVMAddBasicBlock(builder, "castNullCheckContinue");
      LLVMBasicBlockRef nullBlock = LLVM.LLVMAddBasicBlock(builder, "castNullFailure");
      LLVM.LLVMBuildCondBr(builder, isNotNull, continueBlock, nullBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, nullBlock);
      buildThrowCastError(builder, landingPadContainer, "null", to.toString(), null);

      LLVM.LLVMPositionBuilderAtEnd(builder, continueBlock);
    }

    if (from instanceof PrimitiveType && to instanceof PrimitiveType)
    {
      return convertPrimitiveType(builder, value, (PrimitiveType) from, (PrimitiveType) to);
    }
    if (from instanceof ArrayType && to instanceof ArrayType)
    {
      // array casts are illegal unless the base types are the same, so they must have the same basic type
      if (from.isNullable() && !to.isNullable())
      {
        buildCastNullCheck(builder, landingPadContainer, value, from, to);
      }
      // immutability will be checked by the type checker, but it doesn't have any effect on the native type, so we do not need to do anything special here
      return value;
    }
    if (from instanceof FunctionType && to instanceof FunctionType)
    {
      // function casts are illegal unless the parameter and return types are the same, so they must have the same basic type
      if (from.isNullable() && !to.isNullable())
      {
        buildCastNullCheck(builder, landingPadContainer, value, from, to);
      }

      // a cast from a non-immutable function to an immutable function type is impossible
      // so perform a run-time check that this constraint is not violated
      if (!((FunctionType) from).isImmutable() && ((FunctionType) to).isImmutable())
      {
        // this is only allowed if the run-time type of value shows that it is immutable
        LLVMBasicBlockRef functionImmutabilityCheckContinueBlock = LLVM.LLVMAddBasicBlock(builder, "functionImmutabilityCheckContinue");
        // if the value is nullable, allow null values to pass through (we've already checked the nullability)
        if (from.isNullable())
        {
          LLVMBasicBlockRef functionImmutabilityCheckBlock = LLVM.LLVMAddBasicBlock(builder, "functionImmutabilityCheck");
          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
          LLVM.LLVMBuildCondBr(builder, isNotNull, functionImmutabilityCheckBlock, functionImmutabilityCheckContinueBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, functionImmutabilityCheckBlock);
        }
        LLVMValueRef runtimeTypeMatches = rttiHelper.buildInstanceOfCheck(builder, landingPadContainer, value, TypeChecker.findTypeWithNullability(from, false), to);
        LLVMBasicBlockRef immutabilityCastFailure = LLVM.LLVMAddBasicBlock(builder, "functionImmutabilityCheckFailure");
        LLVM.LLVMBuildCondBr(builder, runtimeTypeMatches, functionImmutabilityCheckContinueBlock, immutabilityCastFailure);

        LLVM.LLVMPositionBuilderAtEnd(builder, immutabilityCastFailure);
        buildThrowCastError(builder, landingPadContainer, from.toString(), to.toString(), "non-immutable functions cannot be cast to immutable");

        LLVM.LLVMPositionBuilderAtEnd(builder, functionImmutabilityCheckContinueBlock);
      }

      return value;
    }
    if (from instanceof NamedType && to instanceof NamedType &&
        ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition &&
        ((NamedType) to).getResolvedTypeDefinition() instanceof ClassDefinition)
    {
      if (from.isNullable() && !to.isNullable())
      {
        buildCastNullCheck(builder, landingPadContainer, value, from, to);
      }

      if (!((NamedType) from).getResolvedTypeDefinition().equals(((NamedType) to).getResolvedTypeDefinition()))
      {
        LLVMBasicBlockRef instanceOfContinueBlock = LLVM.LLVMAddBasicBlock(builder, "castInstanceOfCheckContinue");
        if (from.isNullable())
        {
          // if the value is null, then skip the check
          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
          LLVMBasicBlockRef instanceOfCheckBlock = LLVM.LLVMAddBasicBlock(builder, "castInstanceOfCheck");
          LLVM.LLVMBuildCondBr(builder, isNotNull, instanceOfCheckBlock, instanceOfContinueBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfCheckBlock);
        }
        LLVMValueRef runtimeTypeMatches = rttiHelper.buildInstanceOfCheck(builder, landingPadContainer, value, TypeChecker.findTypeWithNullability(from, false), to);
        LLVMBasicBlockRef castFailureBlock = LLVM.LLVMAddBasicBlock(builder, "castFailure");
        LLVM.LLVMBuildCondBr(builder, runtimeTypeMatches, instanceOfContinueBlock, castFailureBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, castFailureBlock);
        LLVMValueRef rttiPointer = rttiHelper.lookupPureRTTI(builder, value);
        LLVMValueRef classNameUbyteArray = rttiHelper.lookupClassName(builder, rttiPointer);
        LLVMValueRef classNameString = codeGenerator.buildStringCreation(builder, landingPadContainer, classNameUbyteArray);
        buildThrowCastError(builder, landingPadContainer, classNameString, to.toString(), null);

        LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfContinueBlock);
        // both from and to are class types, and we have made sure that we can convert between them
        // so bitcast value to the new type
        value = LLVM.LLVMBuildBitCast(builder, value, findTemporaryType(to), "");
      }
      return value;
    }
    if (from instanceof NamedType && to instanceof NamedType &&
        ((NamedType) from).getResolvedTypeDefinition() instanceof CompoundDefinition &&
        ((NamedType) to).getResolvedTypeDefinition() instanceof CompoundDefinition)
    {
      // compound type casts are illegal unless the type definitions are the same, so they must have the same type
      if (from.isNullable() && !to.isNullable())
      {
        buildCastNullCheck(builder, landingPadContainer, value, from, to);
      }
      return value;
    }
    if (from instanceof NamedType && to instanceof NamedType &&
        ((NamedType) from).getResolvedTypeDefinition() instanceof InterfaceDefinition &&
        ((NamedType) to).getResolvedTypeDefinition() instanceof ClassDefinition)
    {
      ObjectType objectType = new ObjectType(from.isNullable(), false, null);
      LLVMValueRef objectValue = convertTemporary(builder, landingPadContainer, value, from, objectType);
      return convertTemporary(builder, landingPadContainer, objectValue, objectType, to);
    }
    if (from instanceof NamedType && to instanceof NamedType &&
        ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition &&
        ((NamedType) to).getResolvedTypeDefinition() instanceof InterfaceDefinition)
    {
      ClassDefinition classDefinition = (ClassDefinition) ((NamedType) from).getResolvedTypeDefinition();
      InterfaceDefinition toInterface = (InterfaceDefinition) ((NamedType) to).getResolvedTypeDefinition();
      boolean found = false;
      for (TypeDefinition type : classDefinition.getInheritanceLinearisation())
      {
        if (type == toInterface)
        {
          found = true;
          break;
        }
      }
      if (found)
      {
        if (from.isNullable() && !to.isNullable())
        {
          buildCastNullCheck(builder, landingPadContainer, value, from, to);
        }

        LLVMBasicBlockRef startBlock = null;
        LLVMBasicBlockRef continueBlock = null;
        // make sure we don't do a second null check if this is a cast from nullable to not-nullable
        if (from.isNullable() && to.isNullable())
        {
          startBlock = LLVM.LLVMGetInsertBlock(builder);
          continueBlock = LLVM.LLVMAddBasicBlock(builder, "toInterfaceContinuation");
          LLVMBasicBlockRef convertBlock = LLVM.LLVMAddBasicBlock(builder, "toInterfaceConversion");

          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
          LLVM.LLVMBuildCondBr(builder, isNotNull, convertBlock, continueBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, convertBlock);
        }
        LLVMTypeRef resultNativeType = findStandardType(to);

        // we know exactly where the VFT is at compile time, so we don't need to search for it at run time, just look it up
        LLVMValueRef vftPointer = virtualFunctionHandler.getVirtualFunctionTablePointer(builder, value, classDefinition, toInterface);
        LLVMValueRef vft = LLVM.LLVMBuildLoad(builder, vftPointer, "");
        LLVMValueRef objectPointer = convertTemporary(builder, landingPadContainer, value, from, new ObjectType(from.isNullable(), false, null));
        LLVMValueRef interfaceValue = LLVM.LLVMGetUndef(resultNativeType);
        interfaceValue = LLVM.LLVMBuildInsertValue(builder, interfaceValue, vft, 0, "");
        interfaceValue = LLVM.LLVMBuildInsertValue(builder, interfaceValue, objectPointer, 1, "");

        if (from.isNullable() && to.isNullable())
        {
          LLVMBasicBlockRef endConvertBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildBr(builder, continueBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, continueBlock);

          LLVMValueRef phiNode = LLVM.LLVMBuildPhi(builder, resultNativeType, "");
          LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstNull(resultNativeType), interfaceValue};
          LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endConvertBlock};
          LLVM.LLVMAddIncoming(phiNode, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);

          return phiNode;
        }
        return interfaceValue;
      }
    }
    if (to instanceof NamedType && ((NamedType) to).getResolvedTypeDefinition() instanceof InterfaceDefinition)
    {
      LLVMValueRef objectValue = null;
      Type objectType = null;
      if (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition)
      {
        objectType = new ObjectType(from.isNullable(), ((NamedType) from).isContextuallyImmutable(), null);
        objectValue = convertTemporary(builder, landingPadContainer, value, from, objectType);
      }
      else if (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof InterfaceDefinition)
      {
        objectType = new ObjectType(from.isNullable(), ((NamedType) from).isContextuallyImmutable(), null);
        objectValue = convertTemporary(builder, landingPadContainer, value, from, objectType);
      }
      else if (from instanceof ObjectType)
      {
        objectType = from;
        objectValue = value;
      }
      if (objectValue != null)
      {
        if (from.isNullable() && !to.isNullable())
        {
          buildCastNullCheck(builder, landingPadContainer, value, from, to);
        }

        LLVMBasicBlockRef startBlock = null;
        LLVMBasicBlockRef continueBlock = null;
        // make sure we don't do a second null check if this is a cast from nullable to not-nullable
        if (objectType.isNullable() && to.isNullable())
        {
          startBlock = LLVM.LLVMGetInsertBlock(builder);
          continueBlock = LLVM.LLVMAddBasicBlock(builder, "toInterfaceContinuation");
          LLVMBasicBlockRef convertBlock = LLVM.LLVMAddBasicBlock(builder, "toInterfaceConversion");

          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, objectValue, objectType);
          LLVM.LLVMBuildCondBr(builder, isNotNull, convertBlock, continueBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, convertBlock);
        }
        LLVMTypeRef resultNativeType = findStandardType(to);

        InterfaceDefinition toInterfaceDefinition = (InterfaceDefinition) ((NamedType) to).getResolvedTypeDefinition();
        LLVMValueRef vftPointer = virtualFunctionHandler.lookupInstanceVFT(builder, objectValue, toInterfaceDefinition);
        LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(virtualFunctionHandler.getVFTType(toInterfaceDefinition), 0);
        vftPointer = LLVM.LLVMBuildBitCast(builder, vftPointer, vftPointerType, "");
        LLVMValueRef vftPointerIsNotNull = LLVM.LLVMBuildIsNotNull(builder, vftPointer, "");
        LLVMBasicBlockRef castSuccessBlock = LLVM.LLVMAddBasicBlock(builder, "toInterfaceCastSuccess");
        LLVMBasicBlockRef castFailureBlock = LLVM.LLVMAddBasicBlock(builder, "toInterfaceCastFailure");
        LLVM.LLVMBuildCondBr(builder, vftPointerIsNotNull, castSuccessBlock, castFailureBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, castFailureBlock);
        if ((from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition) ||
            (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof InterfaceDefinition))
        {
          // if we're coming from a class or an interface, then we know that we can look up the real class name
          LLVMValueRef rttiPointer = rttiHelper.lookupPureRTTI(builder, objectValue);
          LLVMValueRef classNameUbyteArray = rttiHelper.lookupClassName(builder, rttiPointer);
          LLVMValueRef classNameString = codeGenerator.buildStringCreation(builder, landingPadContainer, classNameUbyteArray);
          buildThrowCastError(builder, landingPadContainer, classNameString, to.toString(), null);
        }
        else
        {
          buildThrowCastError(builder, landingPadContainer, from.toString(), to.toString(), "this object does not implement " + toInterfaceDefinition.getQualifiedName().toString());
        }

        LLVM.LLVMPositionBuilderAtEnd(builder, castSuccessBlock);

        // TODO: if the VFT pointer is null (i.e. the object doesn't implement this interface), throw an exception here instead of just storing null in the interface's VFT field (and causing undefined behaviour)
        LLVMValueRef interfaceValue = LLVM.LLVMGetUndef(resultNativeType);
        interfaceValue = LLVM.LLVMBuildInsertValue(builder, interfaceValue, vftPointer, 0, "");
        interfaceValue = LLVM.LLVMBuildInsertValue(builder, interfaceValue, objectValue, 1, "");

        if (objectType.isNullable() && to.isNullable())
        {
          LLVMBasicBlockRef endConvertBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildBr(builder, continueBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, continueBlock);

          LLVMValueRef phiNode = LLVM.LLVMBuildPhi(builder, resultNativeType, "");
          LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstNull(resultNativeType), interfaceValue};
          LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endConvertBlock};
          LLVM.LLVMAddIncoming(phiNode, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);

          return phiNode;
        }
        return interfaceValue;
      }
    }
    if (from instanceof TupleType && !(to instanceof TupleType))
    {
      TupleType fromTuple = (TupleType) from;
      if (fromTuple.getSubTypes().length == 1 && fromTuple.getSubTypes()[0].isRuntimeEquivalent(to))
      {
        if (from.isNullable())
        {
          // if from is nullable and value is null, then we need to throw a CastError here
          buildCastNullCheck(builder, landingPadContainer, value, from, to);

          // extract the value of the tuple from the nullable structure
          value = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
        }
        return LLVM.LLVMBuildExtractValue(builder, value, 0, "");
      }
    }
    if (!(from instanceof TupleType) && to instanceof TupleType)
    {
      TupleType toTuple = (TupleType) to;
      if (toTuple.getSubTypes().length == 1 && toTuple.getSubTypes()[0].isRuntimeEquivalent(from))
      {
        LLVMValueRef tupledValue = LLVM.LLVMGetUndef(findTemporaryType(new TupleType(false, toTuple.getSubTypes(), null)));
        tupledValue = LLVM.LLVMBuildInsertValue(builder, tupledValue, value, 0, "");
        if (to.isNullable())
        {
          LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(to));
          result = LLVM.LLVMBuildInsertValue(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), 0, "");
          return LLVM.LLVMBuildInsertValue(builder, result, tupledValue, 1, "");
        }
        return tupledValue;
      }
    }
    if (from instanceof TupleType && to instanceof TupleType)
    {
      if (from.isNullable() && !to.isNullable())
      {
        buildCastNullCheck(builder, landingPadContainer, value, from, to);
      }

      TupleType fromTuple = (TupleType) from;
      TupleType toTuple = (TupleType) to;
      Type[] fromSubTypes = fromTuple.getSubTypes();
      Type[] toSubTypes = toTuple.getSubTypes();
      if (fromSubTypes.length != toSubTypes.length)
      {
        throw new IllegalArgumentException("Cannot convert from a " + from + " to a " + to);
      }
      boolean subTypesEquivalent = true;
      for (int i = 0; i < fromSubTypes.length; ++i)
      {
        if (!fromSubTypes[i].isRuntimeEquivalent(toSubTypes[i]))
        {
          subTypesEquivalent = false;
          break;
        }
      }
      if (subTypesEquivalent)
      {
        // just convert the nullability
        if (from.isNullable() && !to.isNullable())
        {
          // extract the value of the tuple from the nullable structure
          // (we have already done the associated null check above)
          return LLVM.LLVMBuildExtractValue(builder, value, 1, "");
        }
        if (!from.isNullable() && to.isNullable())
        {
          LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(to));
          // set the flag to one to indicate that this value is not null
          result = LLVM.LLVMBuildInsertValue(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), 0, "");
          return LLVM.LLVMBuildInsertValue(builder, result, value, 1, "");
        }
        throw new IllegalArgumentException("Unable to convert from a " + from + " to a " + to + " - their sub types and nullability are equivalent, but the types themselves are not");
      }

      LLVMValueRef isNotNullValue = null;
      LLVMValueRef tupleValue = value;
      if (from.isNullable())
      {
        isNotNullValue = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
        tupleValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
      }

      LLVMValueRef currentValue = LLVM.LLVMGetUndef(findTemporaryType(toTuple));
      for (int i = 0; i < fromSubTypes.length; i++)
      {
        LLVMValueRef current = LLVM.LLVMBuildExtractValue(builder, tupleValue, i, "");
        LLVMValueRef converted = convertTemporary(builder, landingPadContainer, current, fromSubTypes[i], toSubTypes[i]);
        currentValue = LLVM.LLVMBuildInsertValue(builder, currentValue, converted, i, "");
      }

      if (to.isNullable())
      {
        LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(to));
        if (from.isNullable())
        {
          result = LLVM.LLVMBuildInsertValue(builder, result, isNotNullValue, 0, "");
        }
        else
        {
          // set the flag to one to indicate that this value is not null
          result = LLVM.LLVMBuildInsertValue(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), 0, "");
        }
        return LLVM.LLVMBuildInsertValue(builder, result, currentValue, 1, "");
      }
      // return the value directly, since the to type is not nullable
      return currentValue;
    }
    if (from instanceof ObjectType && to instanceof ObjectType)
    {
      // object casts are always legal
      if (from.isNullable() && !to.isNullable())
      {
        buildCastNullCheck(builder, landingPadContainer, value, from, to);
      }
      // immutability will be checked by the type checker, and doesn't have any effect on the native type, so we do not need to do anything special here
      return value;
    }
    if (to instanceof ObjectType)
    {
      // anything can convert to object
      if (from instanceof NullType)
      {
        if (!to.isNullable())
        {
          throw new IllegalArgumentException("Cannot convert from NullType to a not-null ObjectType");
        }
        return LLVM.LLVMConstNull(findTemporaryType(to));
      }
      if ((from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof ClassDefinition) ||
          from instanceof ArrayType)
      {
        if (from.isNullable() && !to.isNullable())
        {
          buildCastNullCheck(builder, landingPadContainer, value, from, to);
        }
        // class and array types can be safely bitcast to object types
        return LLVM.LLVMBuildBitCast(builder, value, findTemporaryType(to), "");
      }
      if (from instanceof NamedType && ((NamedType) from).getResolvedTypeDefinition() instanceof InterfaceDefinition)
      {
        if (from.isNullable() && !to.isNullable())
        {
          buildCastNullCheck(builder, landingPadContainer, value, from, to);
        }
        // extract the object part of the interface's type
        return LLVM.LLVMBuildExtractValue(builder, value, 1, "");
      }
      Type notNullFromType = TypeChecker.findTypeWithNullability(from, false);
      LLVMValueRef notNullValue = value;
      LLVMBasicBlockRef startBlock = null;
      LLVMBasicBlockRef notNullBlock;
      LLVMBasicBlockRef continuationBlock = null;
      if (from.isNullable())
      {
        if (to.isNullable())
        {
          continuationBlock = LLVM.LLVMAddBasicBlock(builder, "toObjectConversionContinuation");
          notNullBlock = LLVM.LLVMAddBasicBlock(builder, "toObjectConversionNotNull");

          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
          startBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildCondBr(builder, isNotNull, notNullBlock, continuationBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, notNullBlock);
        }
        else
        {
          buildCastNullCheck(builder, landingPadContainer, value, from, to);
        }
        // TODO: converting from nullable to not-null here will cause an unnecessary null check, try to eliminate it
        notNullValue = convertTemporary(builder, landingPadContainer, value, from, notNullFromType);
      }
      LLVMTypeRef nativeType = LLVM.LLVMPointerType(findSpecialisedObjectType(notNullFromType), 0);
      // allocate memory for the object
      LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false)};
      LLVMValueRef llvmStructSize = LLVM.LLVMBuildGEP(builder, LLVM.LLVMConstNull(nativeType), C.toNativePointerArray(indices, false, true), indices.length, "");
      LLVMValueRef llvmSize = LLVM.LLVMBuildPtrToInt(builder, llvmStructSize, LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), "");
      LLVMValueRef[] callocArguments = new LLVMValueRef[] {llvmSize, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false)};
      LLVMValueRef memory = LLVM.LLVMBuildCall(builder, codeGenerator.getCallocFunction(), C.toNativePointerArray(callocArguments, false, true), callocArguments.length, "");

      LLVMValueRef isNotNull = LLVM.LLVMBuildIsNotNull(builder, memory, "");
      LLVMBasicBlockRef callocContinueBlock = LLVM.LLVMAddBasicBlock(builder, "objectCastCallocContinue");
      LLVMBasicBlockRef callocFailedBlock = LLVM.LLVMAddBasicBlock(builder, "objectCastCallocFailed");
      LLVM.LLVMBuildCondBr(builder, isNotNull, callocContinueBlock, callocFailedBlock);
      LLVM.LLVMPositionBuilderAtEnd(builder, callocFailedBlock);
      codeGenerator.buildOutOfMemoryHandler(builder);

      LLVM.LLVMPositionBuilderAtEnd(builder, callocContinueBlock);
      LLVMValueRef pointer = LLVM.LLVMBuildBitCast(builder, memory, nativeType, "");

      // store the object's run-time type information
      LLVMValueRef rtti;
      if (notNullFromType instanceof FunctionType)
      {
        // for function types, take the RTTI out of the value, don't generate it from the static type
        rtti = LLVM.LLVMBuildExtractValue(builder, notNullValue, 0, "");
      }
      else
      {
        rtti = rttiHelper.getInstanceRTTI(TypeChecker.findTypeWithoutModifiers(notNullFromType));
      }
      LLVMValueRef rttiPointer = rttiHelper.getRTTIPointer(builder, pointer);
      LLVM.LLVMBuildStore(builder, rtti, rttiPointer);

      // build the base change VFT, and store it as the object's VFT
      LLVMValueRef baseChangeVFT = virtualFunctionHandler.getBaseChangeObjectVFT(notNullFromType);
      LLVMValueRef vftElementPointer = virtualFunctionHandler.getFirstVirtualFunctionTablePointer(builder, pointer);
      LLVM.LLVMBuildStore(builder, baseChangeVFT, vftElementPointer);

      // store the value inside the object
      LLVMValueRef[] elementIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                          LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 2, false)};
      LLVMValueRef elementPointer = LLVM.LLVMBuildGEP(builder, pointer, C.toNativePointerArray(elementIndices, false, true), elementIndices.length, "");
      notNullValue = convertTemporaryToStandard(builder, notNullValue, notNullFromType);
      LLVM.LLVMBuildStore(builder, notNullValue, elementPointer);

      // cast away the part of the type that contains the value
      LLVMValueRef notNullResult = LLVM.LLVMBuildBitCast(builder, pointer, findTemporaryType(to), "");

      if (from.isNullable() && to.isNullable())
      {
        LLVMBasicBlockRef endNotNullBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildBr(builder, continuationBlock);
        LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);

        LLVMValueRef resultPhi = LLVM.LLVMBuildPhi(builder, findTemporaryType(to), "");
        LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstNull(findTemporaryType(to)), notNullResult};
        LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endNotNullBlock};
        LLVM.LLVMAddIncoming(resultPhi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
        return resultPhi;
      }
      return notNullResult;
    }
    if (from instanceof ObjectType)
    {
      if ((to instanceof NamedType && ((NamedType) to).getResolvedTypeDefinition() instanceof ClassDefinition) ||
          to instanceof ArrayType)
      {
        if (from.isNullable() && !to.isNullable())
        {
          buildCastNullCheck(builder, landingPadContainer, value, from, to);
        }

        LLVMValueRef isInstanceOfToType = rttiHelper.buildInstanceOfCheck(builder, landingPadContainer, value, from, to);
        LLVMBasicBlockRef instanceOfSuccessBlock = LLVM.LLVMAddBasicBlock(builder, "castObjectInstanceOfSuccess");
        LLVMBasicBlockRef instanceOfFailureBlock = LLVM.LLVMAddBasicBlock(builder, "castObjectInstanceOfFailure");
        LLVM.LLVMBuildCondBr(builder, isInstanceOfToType, instanceOfSuccessBlock, instanceOfFailureBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfFailureBlock);
        buildThrowCastError(builder, landingPadContainer, from.toString(), to.toString(), null);

        LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfSuccessBlock);
        return LLVM.LLVMBuildBitCast(builder, value, findTemporaryType(to), "");
      }

      LLVMValueRef notNullValue = value;
      LLVMBasicBlockRef startBlock = null;
      LLVMBasicBlockRef notNullBlock = null;
      LLVMBasicBlockRef continuationBlock = null;
      if (from.isNullable())
      {
        if (to.isNullable())
        {
          continuationBlock = LLVM.LLVMAddBasicBlock(builder, "fromObjectConversionContinuation");
          notNullBlock = LLVM.LLVMAddBasicBlock(builder, "fromObjectConversionNotNull");

          LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, from);
          startBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildCondBr(builder, isNotNull, notNullBlock, continuationBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, notNullBlock);
        }
        else
        {
          buildCastNullCheck(builder, landingPadContainer, value, from, to);
        }
        // TODO: converting from nullable to not-null here will cause an unnecessary null check, try to eliminate it
        notNullValue = convertTemporary(builder, landingPadContainer, value, from, TypeChecker.findTypeWithNullability(from, false));
      }

      LLVMValueRef isInstanceOfToType = rttiHelper.buildInstanceOfCheck(builder, landingPadContainer, notNullValue, TypeChecker.findTypeWithNullability(from, false), to);
      LLVMBasicBlockRef instanceOfSuccessBlock = LLVM.LLVMAddBasicBlock(builder, "castObjectInstanceOfSuccess");
      LLVMBasicBlockRef instanceOfFailureBlock = LLVM.LLVMAddBasicBlock(builder, "castObjectInstanceOfFailure");
      LLVM.LLVMBuildCondBr(builder, isInstanceOfToType, instanceOfSuccessBlock, instanceOfFailureBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfFailureBlock);
      buildThrowCastError(builder, landingPadContainer, from.toString(), to.toString(), null);

      LLVM.LLVMPositionBuilderAtEnd(builder, instanceOfSuccessBlock);

      Type notNullToType = TypeChecker.findTypeWithNullability(to, false);
      LLVMTypeRef nativeType = LLVM.LLVMPointerType(findSpecialisedObjectType(notNullToType), 0);
      LLVMValueRef castedValue = LLVM.LLVMBuildBitCast(builder, notNullValue, nativeType, "");

      LLVMValueRef[] elementIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                          LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 2, false)};
      LLVMValueRef elementPointer = LLVM.LLVMBuildGEP(builder, castedValue, C.toNativePointerArray(elementIndices, false, true), elementIndices.length, "");
      LLVMValueRef notNullResult = convertStandardPointerToTemporary(builder, landingPadContainer, elementPointer, TypeChecker.findTypeWithNullability(to, false), to);

      if (from.isNullable() && to.isNullable())
      {
        LLVMBasicBlockRef endNotNullBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);

        LLVMValueRef resultPhi = LLVM.LLVMBuildPhi(builder, findTemporaryType(to), "");
        LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstNull(findTemporaryType(to)), notNullResult};
        LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endNotNullBlock};
        LLVM.LLVMAddIncoming(resultPhi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
        return resultPhi;
      }
      return notNullResult;
    }
    throw new IllegalArgumentException("Unknown type conversion, from '" + from + "' to '" + to + "'");
  }

  /**
   * Converts the specified value from the specified 'from' PrimitiveType to the specified 'to' PrimitiveType.
   * @param value - the value to convert
   * @param from - the PrimitiveType to convert from
   * @param to - the PrimitiveType to convert to
   * @return the converted value
   */
  private LLVMValueRef convertPrimitiveType(LLVMBuilderRef builder, LLVMValueRef value, PrimitiveType from, PrimitiveType to)
  {
    PrimitiveTypeType fromType = from.getPrimitiveTypeType();
    PrimitiveTypeType toType = to.getPrimitiveTypeType();
    if (fromType == toType && from.isNullable() == to.isNullable())
    {
      return value;
    }
    LLVMValueRef primitiveValue = value;
    if (from.isNullable())
    {
      primitiveValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
    }
    // perform the conversion
    LLVMTypeRef toNativeType = findTemporaryType(new PrimitiveType(false, toType, null));
    if (fromType == toType)
    {
      // do not alter primitiveValue, we only need to change the nullability
    }
    else if (fromType.isFloating() && toType.isFloating())
    {
      primitiveValue = LLVM.LLVMBuildFPCast(builder, primitiveValue, toNativeType, "");
    }
    else if (fromType.isFloating() && !toType.isFloating())
    {
      if (toType.isSigned())
      {
        primitiveValue = LLVM.LLVMBuildFPToSI(builder, primitiveValue, toNativeType, "");
      }
      else
      {
        primitiveValue = LLVM.LLVMBuildFPToUI(builder, primitiveValue, toNativeType, "");
      }
    }
    else if (!fromType.isFloating() && toType.isFloating())
    {
      if (fromType.isSigned())
      {
        primitiveValue = LLVM.LLVMBuildSIToFP(builder, primitiveValue, toNativeType, "");
      }
      else
      {
        primitiveValue = LLVM.LLVMBuildUIToFP(builder, primitiveValue, toNativeType, "");
      }
    }
    // both integer types, so perform a sign-extend, zero-extend, or truncation
    else if (fromType.getBitCount() > toType.getBitCount())
    {
      primitiveValue = LLVM.LLVMBuildTrunc(builder, primitiveValue, toNativeType, "");
    }
    else if (fromType.getBitCount() == toType.getBitCount() && fromType.isSigned() != toType.isSigned())
    {
      primitiveValue = LLVM.LLVMBuildBitCast(builder, primitiveValue, toNativeType, "");
    }
    // the value needs extending, so decide whether to do a sign-extend or a zero-extend based on whether the from type is signed
    else if (fromType.isSigned())
    {
      primitiveValue = LLVM.LLVMBuildSExt(builder, primitiveValue, toNativeType, "");
    }
    else
    {
      primitiveValue = LLVM.LLVMBuildZExt(builder, primitiveValue, toNativeType, "");
    }
    // pack up the result before returning it
    if (to.isNullable())
    {
      LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(to));
      if (from.isNullable())
      {
        LLVMValueRef isNotNullValue = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
        result = LLVM.LLVMBuildInsertValue(builder, result, isNotNullValue, 0, "");
      }
      else
      {
        // set the flag to one to indicate that this value is not null
        result = LLVM.LLVMBuildInsertValue(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), 0, "");
      }
      return LLVM.LLVMBuildInsertValue(builder, result, primitiveValue, 1, "");
    }
    // return the primitive value directly, since the to type is not nullable
    return primitiveValue;
  }

  /**
   * Builds a conversion of the specified value from the specified type into the string type, by calling toString() or using the constant "null" string as necessary.
   * @param builder - the builder to build the conversion with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the value to convert to a string, in a temporary type representation
   * @param type - the type of the value to convert, which can be any type except VoidType (including NullType and any nullable types)
   * @return an LLVMValueRef containing the string representation of value, in a standard type representation
   */
  public LLVMValueRef convertToString(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef value, Type type)
  {
    if (type.isRuntimeEquivalent(SpecialTypeHandler.STRING_TYPE))
    {
      return convertTemporaryToStandard(builder, value, SpecialTypeHandler.STRING_TYPE);
    }
    if (type instanceof NullType)
    {
      LLVMValueRef stringValue = codeGenerator.buildStringCreation(builder, landingPadContainer, "null");
      return convertTemporaryToStandard(builder, stringValue, SpecialTypeHandler.STRING_TYPE);
    }
    LLVMValueRef notNullValue = value;
    Type notNullType = type;
    LLVMBasicBlockRef alternativeBlock = null;
    LLVMBasicBlockRef continuationBlock = null;
    if (type.isNullable())
    {
      continuationBlock = LLVM.LLVMAddBasicBlock(builder, "stringConversionContinuation");
      alternativeBlock = LLVM.LLVMAddBasicBlock(builder, "stringConversionNull");
      LLVMBasicBlockRef conversionBlock = LLVM.LLVMAddBasicBlock(builder, "stringConversion");

      LLVMValueRef isNotNull = codeGenerator.buildNullCheck(builder, value, type);
      LLVM.LLVMBuildCondBr(builder, isNotNull, conversionBlock, alternativeBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, conversionBlock);
      notNullType = TypeChecker.findTypeWithNullability(type, false);
      notNullValue = convertTemporary(builder, landingPadContainer, value, type, notNullType);
    }
    Method method = notNullType.getMethod(new BuiltinMethod(notNullType, BuiltinMethodType.TO_STRING).getDisambiguator());
    if (method == null)
    {
      throw new IllegalStateException("Type " + type + " does not have a 'toString()' method!");
    }
    LLVMValueRef function = codeGenerator.lookupMethodFunction(builder, landingPadContainer, notNullValue, notNullType, method, false);
    LLVMValueRef callee = convertMethodCallee(builder, landingPadContainer, notNullValue, notNullType, method);
    LLVMValueRef[] arguments = new LLVMValueRef[] {callee};
    LLVMBasicBlockRef toStringInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "toStringInvokeContinue");
    LLVMValueRef stringValue = LLVM.LLVMBuildInvoke(builder, function, C.toNativePointerArray(arguments, false, true), arguments.length, toStringInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, toStringInvokeContinueBlock);

    if (type.isNullable())
    {
      LLVMBasicBlockRef endConversionBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, alternativeBlock);
      LLVMValueRef alternativeStringValue = codeGenerator.buildStringCreation(builder, landingPadContainer, "null");
      alternativeStringValue = convertTemporaryToStandard(builder, alternativeStringValue, SpecialTypeHandler.STRING_TYPE);
      LLVMBasicBlockRef endAlternativeBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
      LLVMValueRef phi = LLVM.LLVMBuildPhi(builder, findStandardType(SpecialTypeHandler.STRING_TYPE), "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {stringValue, alternativeStringValue};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {endConversionBlock, endAlternativeBlock};
      LLVM.LLVMAddIncoming(phi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
      return phi;
    }
    return stringValue;
  }

  /**
   * Converts the specified value of the specified type from a temporary type representation to a standard type representation, after converting it from 'fromType' to 'toType'.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the value to convert
   * @param fromType - the type to convert from
   * @param toType - the type to convert to
   * @return the converted value
   */
  public LLVMValueRef convertTemporaryToStandard(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef value, Type fromType, Type toType)
  {
    LLVMValueRef temporary = convertTemporary(builder, landingPadContainer, value, fromType, toType);
    return convertTemporaryToStandard(builder, temporary, toType);
  }

  /**
   * Converts the specified value of the specified type from a temporary type representation to a standard type representation.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param value - the value to convert
   * @param type - the type to convert
   * @return the converted value
   */
  public LLVMValueRef convertTemporaryToStandard(LLVMBuilderRef builder, LLVMValueRef value, Type type)
  {
    if (type instanceof ArrayType)
    {
      // the temporary and standard types are the same for ArrayTypes
      return value;
    }
    if (type instanceof FunctionType)
    {
      // the temporary and standard types are the same for FunctionTypes
      return value;
    }
    if (type instanceof NamedType)
    {
      TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        // the temporary and standard types are the same for class types
        return value;
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        // the temporary and standard types are the same for interface types
        return value;
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        if (type.isNullable())
        {
          LLVMTypeRef standardType = findStandardType(type);
          // we are converting from a pointer to a non-nullable compound into a possibly-null compound
          LLVMValueRef isNotNullValue = LLVM.LLVMBuildIsNotNull(builder, value, "");
          // we need to branch on isNotNullValue, to decide whether to load from the pointer
          LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVMBasicBlockRef convertedBlock = LLVM.LLVMAddBasicBlock(builder, "compoundConverted");
          LLVMBasicBlockRef loadBlock = LLVM.LLVMAddBasicBlock(builder, "compoundConversion");

          LLVM.LLVMBuildCondBr(builder, isNotNullValue, loadBlock, convertedBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, loadBlock);
          LLVMValueRef loaded = LLVM.LLVMBuildLoad(builder, value, "");
          LLVMValueRef notNullResult = LLVM.LLVMBuildInsertValue(builder, LLVM.LLVMGetUndef(standardType), LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), 0, "");
          notNullResult = LLVM.LLVMBuildInsertValue(builder, notNullResult, loaded, 1, "");
          LLVM.LLVMBuildBr(builder, convertedBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, convertedBlock);
          LLVMValueRef phi = LLVM.LLVMBuildPhi(builder, standardType, "");
          LLVMValueRef nullResult = LLVM.LLVMConstNull(standardType);
          LLVMValueRef[] values = new LLVMValueRef[] {nullResult, notNullResult};
          LLVMBasicBlockRef[] blocks = new LLVMBasicBlockRef[] {currentBlock, loadBlock};
          LLVM.LLVMAddIncoming(phi, C.toNativePointerArray(values, false, true), C.toNativePointerArray(blocks, false, true), values.length);
          return phi;
        }
        // type is not nullable, so we can just load it directly
        return LLVM.LLVMBuildLoad(builder, value, "");
      }
    }
    if (type instanceof NullType)
    {
      // the temporary and standard types are the same for NullTypes
      return value;
    }
    if (type instanceof ObjectType)
    {
      // the temporary and standard types are the same for ObjectTypes
      return value;
    }
    if (type instanceof PrimitiveType)
    {
      // the temporary and standard types are the same for PrimitiveTypes
      return value;
    }
    if (type instanceof TupleType)
    {
      boolean containsCompound = false;
      Queue<TupleType> typeQueue = new LinkedList<TupleType>();
      typeQueue.add((TupleType) type);
      while (!typeQueue.isEmpty())
      {
        TupleType currentType = typeQueue.poll();
        for (Type subType : currentType.getSubTypes())
        {
          if (subType instanceof TupleType)
          {
            typeQueue.add((TupleType) subType);
          }
          if (subType instanceof NamedType && ((NamedType) subType).getResolvedTypeDefinition() instanceof CompoundDefinition)
          {
            containsCompound = true;
            break;
          }
        }
      }
      if (!containsCompound)
      {
        // if this tuple does not contain any compound types (after an arbitrary degree of nesting),
        // then it does not need converting, as the standard and temporary representations are the same
        return value;
      }

      LLVMValueRef notNullValue = value;
      if (type.isNullable())
      {
        notNullValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
      }
      LLVMValueRef resultNotNull = LLVM.LLVMGetUndef(findStandardType(TypeChecker.findTypeWithNullability(type, false)));
      Type[] subTypes = ((TupleType) type).getSubTypes();
      for (int i = 0; i < subTypes.length; ++i)
      {
        LLVMValueRef extractedValue = LLVM.LLVMBuildExtractValue(builder, notNullValue, i, "");
        LLVMValueRef convertedValue = convertTemporaryToStandard(builder, extractedValue, subTypes[i]);
        resultNotNull = LLVM.LLVMBuildInsertValue(builder, resultNotNull, convertedValue, i, "");
      }
      if (type.isNullable())
      {
        LLVMValueRef isNotNullValue = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
        LLVMValueRef result = LLVM.LLVMGetUndef(findStandardType(type));
        result = LLVM.LLVMBuildInsertValue(builder, result, isNotNullValue, 0, "");
        result = LLVM.LLVMBuildInsertValue(builder, result, resultNotNull, 1, "");
        return result;
      }
      return resultNotNull;
    }
    if (type instanceof VoidType)
    {
      throw new IllegalArgumentException("VoidType has no standard representation");
    }
    throw new IllegalArgumentException("Unknown type: " + type);
  }

  /**
   * Converts the specified value of the specified type from a standard type representation to a temporary type representation, before converting it from 'fromType' to 'toType'.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the value to convert
   * @param fromType - the type to convert from
   * @param toType - the type to convert to
   * @return the converted value
   */
  public LLVMValueRef convertStandardToTemporary(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef value, Type fromType, Type toType)
  {
    LLVMValueRef temporary = convertStandardToTemporary(builder, value, fromType);
    return convertTemporary(builder, landingPadContainer, temporary, fromType, toType);
  }

  /**
   * Converts the specified value of the specified type from a standard type representation to a temporary type representation.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param value - the value to convert
   * @param type - the type to convert
   * @return the converted value
   */
  public LLVMValueRef convertStandardToTemporary(LLVMBuilderRef builder, LLVMValueRef value, Type type)
  {
    if (type instanceof ArrayType)
    {
      // the temporary and standard types are the same for ArrayTypes
      return value;
    }
    if (type instanceof FunctionType)
    {
      // the temporary and standard types are the same for FunctionTypes
      return value;
    }
    if (type instanceof NamedType)
    {
      TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        // the temporary and standard types are the same for class types
        return value;
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        // the temporary and standard types are the same for interface types
        return value;
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        LLVMValueRef notNullValue = value;
        if (type.isNullable())
        {
          notNullValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
        }

        // find the type to alloca, which is the standard representation of a non-nullable version of this type
        // when we alloca this type, it becomes equivalent to the temporary type representation of this compound type (with any nullability)
        LLVMTypeRef allocaBaseType = findStandardType(TypeChecker.findTypeWithNullability(type, false));
        LLVMValueRef alloca = LLVM.LLVMBuildAllocaInEntryBlock(builder, allocaBaseType, "");
        LLVM.LLVMBuildStore(builder, notNullValue, alloca);
        if (type.isNullable())
        {
          LLVMValueRef isNotNullValue = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
          return LLVM.LLVMBuildSelect(builder, isNotNullValue, alloca, LLVM.LLVMConstNull(findTemporaryType(type)), "");
        }
        return alloca;
      }
    }
    if (type instanceof NullType)
    {
      // the temporary and standard types are the same for NullTypes
      return value;
    }
    if (type instanceof ObjectType)
    {
      // the temporary and standard types are the same for ObjectTypes
      return value;
    }
    if (type instanceof PrimitiveType)
    {
      // the temporary and standard types are the same for PrimitiveTypes
      return value;
    }
    if (type instanceof TupleType)
    {
      boolean containsCompound = false;
      Queue<TupleType> typeQueue = new LinkedList<TupleType>();
      typeQueue.add((TupleType) type);
      while (!typeQueue.isEmpty())
      {
        TupleType currentType = typeQueue.poll();
        for (Type subType : currentType.getSubTypes())
        {
          if (subType instanceof TupleType)
          {
            typeQueue.add((TupleType) subType);
          }
          if (subType instanceof NamedType && ((NamedType) subType).getResolvedTypeDefinition() instanceof CompoundDefinition)
          {
            containsCompound = true;
            break;
          }
        }
      }
      if (!containsCompound)
      {
        // if this tuple does not contain any compound types (after an arbitrary degree of nesting),
        // then it does not need converting, as the standard and temporary representations are the same
        return value;
      }

      LLVMValueRef notNullValue = value;
      if (type.isNullable())
      {
        notNullValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
      }
      LLVMValueRef resultNotNull = LLVM.LLVMGetUndef(findTemporaryType(TypeChecker.findTypeWithNullability(type, false)));
      Type[] subTypes = ((TupleType) type).getSubTypes();
      for (int i = 0; i < subTypes.length; ++i)
      {
        LLVMValueRef extractedValue = LLVM.LLVMBuildExtractValue(builder, notNullValue, i, "");
        LLVMValueRef convertedValue = convertStandardToTemporary(builder, extractedValue, subTypes[i]);
        resultNotNull = LLVM.LLVMBuildInsertValue(builder, resultNotNull, convertedValue, i, "");
      }
      if (type.isNullable())
      {
        LLVMValueRef isNotNullValue = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
        LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(type));
        result = LLVM.LLVMBuildInsertValue(builder, result, isNotNullValue, 0, "");
        result = LLVM.LLVMBuildInsertValue(builder, result, resultNotNull, 1, "");
        return result;
      }
      return resultNotNull;
    }
    if (type instanceof VoidType)
    {
      throw new IllegalArgumentException("VoidType has no temporary representation");
    }
    throw new IllegalArgumentException("Unknown type: " + type);
  }

  /**
   * Converts the specified pointer to a value of the specified type from a pointer to a standard type representation to a temporary type representation, before converting it from 'fromType' to 'toType'.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param pointer - the pointer to the value to convert
   * @param fromType - the type to convert from
   * @param toType - the type to convert to
   * @return the converted value
   */
  public LLVMValueRef convertStandardPointerToTemporary(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef pointer, Type fromType, Type toType)
  {
    LLVMValueRef temporary = convertStandardPointerToTemporary(builder, pointer, fromType);
    return convertTemporary(builder, landingPadContainer, temporary, fromType, toType);
  }

  /**
   * Converts the specified pointer to a value of the specified type from a pointer to a standard type representation to a temporary type representation.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param value - the pointer to the value to convert
   * @param type - the type to convert
   * @return the converted value
   */
  public LLVMValueRef convertStandardPointerToTemporary(LLVMBuilderRef builder, LLVMValueRef value, Type type)
  {
    if (type instanceof ArrayType)
    {
      // the temporary and standard types are the same for ArrayTypes
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    if (type instanceof FunctionType)
    {
      // the temporary and standard types are the same for FunctionTypes
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    if (type instanceof NamedType)
    {
      TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        // the temporary and standard types are the same for class types
        return LLVM.LLVMBuildLoad(builder, value, "");
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        // the temporary and standard types are the same for interface types
        return LLVM.LLVMBuildLoad(builder, value, "");
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        if (type.isNullable())
        {
          LLVMValueRef[] nullabilityIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                                  LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false)};
          LLVMValueRef isNotNullPointer = LLVM.LLVMBuildGEP(builder, value, C.toNativePointerArray(nullabilityIndices, false, true), nullabilityIndices.length, "");
          LLVMValueRef isNotNullValue = LLVM.LLVMBuildLoad(builder, isNotNullPointer, "");

          LLVMValueRef[] valueIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                            LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false)};
          LLVMValueRef notNullValue = LLVM.LLVMBuildGEP(builder, value, C.toNativePointerArray(valueIndices, false, true), valueIndices.length, "");
          return LLVM.LLVMBuildSelect(builder, isNotNullValue, notNullValue, LLVM.LLVMConstNull(findTemporaryType(type)), "");
        }
        // the pointer to the standard non-nullable representation is the same as the temporary representation
        return value;
      }
    }
    if (type instanceof NullType)
    {
      // the temporary and standard types are the same for NullTypes
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    if (type instanceof ObjectType)
    {
      // the temporary and standard types are the same for ObjectTypes
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    if (type instanceof PrimitiveType)
    {
      // the temporary and standard types are the same for PrimitiveTypes
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    if (type instanceof TupleType)
    {
      boolean containsCompound = false;
      Queue<TupleType> typeQueue = new LinkedList<TupleType>();
      typeQueue.add((TupleType) type);
      while (!typeQueue.isEmpty())
      {
        TupleType currentType = typeQueue.poll();
        for (Type subType : currentType.getSubTypes())
        {
          if (subType instanceof TupleType)
          {
            typeQueue.add((TupleType) subType);
          }
          if (subType instanceof NamedType && ((NamedType) subType).getResolvedTypeDefinition() instanceof CompoundDefinition)
          {
            containsCompound = true;
            break;
          }
        }
      }
      if (!containsCompound)
      {
        // if this tuple does not contain any compound types (after an arbitrary degree of nesting),
        // then it does not need converting, as the standard and temporary representations are the same
        return LLVM.LLVMBuildLoad(builder, value, "");
      }

      LLVMValueRef isNotNullValue = null;
      LLVMValueRef notNullPointer = value;
      if (type.isNullable())
      {
        LLVMValueRef[] nullabilityIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                                LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false)};
        LLVMValueRef isNotNullPointer = LLVM.LLVMBuildGEP(builder, value, C.toNativePointerArray(nullabilityIndices, false, true), nullabilityIndices.length, "");
        isNotNullValue = LLVM.LLVMBuildLoad(builder, isNotNullPointer, "");

        LLVMValueRef[] valueIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                          LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false)};
        notNullPointer = LLVM.LLVMBuildGEP(builder, value, C.toNativePointerArray(valueIndices, false, true), valueIndices.length, "");
      }
      LLVMValueRef resultNotNull = LLVM.LLVMGetUndef(findTemporaryType(TypeChecker.findTypeWithNullability(type, false)));
      Type[] subTypes = ((TupleType) type).getSubTypes();
      for (int i = 0; i < subTypes.length; ++i)
      {
        LLVMValueRef[] valueIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                          LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), i, false)};
        LLVMValueRef valuePointer = LLVM.LLVMBuildGEP(builder, notNullPointer, C.toNativePointerArray(valueIndices, false, true), valueIndices.length, "");
        LLVMValueRef convertedValue = convertStandardPointerToTemporary(builder, valuePointer, subTypes[i]);
        resultNotNull = LLVM.LLVMBuildInsertValue(builder, resultNotNull, convertedValue, i, "");
      }
      if (type.isNullable())
      {
        LLVMValueRef result = LLVM.LLVMGetUndef(findTemporaryType(type));
        result = LLVM.LLVMBuildInsertValue(builder, result, isNotNullValue, 0, "");
        result = LLVM.LLVMBuildInsertValue(builder, result, resultNotNull, 1, "");
        return result;
      }
      return resultNotNull;
    }
    if (type instanceof VoidType)
    {
      throw new IllegalArgumentException("VoidType has no standard representation");
    }
    throw new IllegalArgumentException("Unknown type: " + type);
  }

}
