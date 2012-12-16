package eu.bryants.anthony.plinth.compiler.passes.llvm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMBuilderRef;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMTypeRef;
import nativelib.llvm.LLVM.LLVMValueRef;
import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 4 Dec 2012
 */

/**
 * @author Anthony Bryant
 */
public class VirtualFunctionHandler
{
  private static final String SUPERCLASS_VFT_GENERATOR_NAME = "plinth_core_generate_superclass_vft";
  private static final String VFT_PREFIX = "_VFT_";
  private static final String VFT_DESCRIPTOR_PREFIX = "_VFT_DESC_";
  private static final String VFT_INIT_FUNCTION_PREFIX = "_SUPER_VFT_INIT_";
  private static final String SUPERCLASS_VFT_GLOBAL_PREFIX = "_SUPER_VFT_";
  private static final String BASE_CHANGE_OBJECT_VFT_PREFIX = "_base_change_o_VFT_";

  private CodeGenerator codeGenerator;
  private TypeDefinition typeDefinition;
  private TypeHelper typeHelper;

  private LLVMModuleRef module;

  private LLVMTypeRef vftDescriptorType;
  private LLVMTypeRef vftType;
  private LLVMTypeRef functionSearchListType;

  private LLVMTypeRef objectVirtualTableType;
  private Map<ClassDefinition, LLVMTypeRef> nativeVirtualTableTypes = new HashMap<ClassDefinition, LLVMTypeRef>();

  public VirtualFunctionHandler(CodeGenerator codeGenerator, TypeDefinition typeDefinition, LLVMModuleRef module)
  {
    this.codeGenerator = codeGenerator;
    this.typeDefinition = typeDefinition;
    this.module = module;
  }

  /**
   * Sets the TypeHelper on this VirtualFunctionHandler, so that it can be used.
   * @param typeHelper - the TypeHelper to set
   */
  public void setTypeHelper(TypeHelper typeHelper)
  {
    this.typeHelper = typeHelper;
  }

  /**
   * Gets the global variable that stores the virtual function table for the object type.
   * @return the VFT global variable for the object type
   */
  public LLVMValueRef getObjectVFTGlobal()
  {
    String mangledName = VFT_PREFIX + ObjectType.MANGLED_NAME;

    LLVMValueRef existingGlobal = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingGlobal != null)
    {
      return existingGlobal;
    }

    LLVMTypeRef vftType = getObjectVFTType();
    LLVMValueRef global = LLVM.LLVMAddGlobal(module, vftType, mangledName);
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMLinkOnceODRLinkage);
    LLVM.LLVMSetVisibility(global, LLVM.LLVMVisibility.LLVMHiddenVisibility);

    Method[] methods = ObjectType.OBJECT_METHODS;
    LLVMValueRef[] llvmMethods = new LLVMValueRef[methods.length];
    for (int i = 0; i < methods.length; ++i)
    {
      llvmMethods[i] = codeGenerator.getMethodFunction(null, null, methods[i]);
    }
    LLVM.LLVMSetInitializer(global, LLVM.LLVMConstNamedStruct(vftType, C.toNativePointerArray(llvmMethods, false, true), llvmMethods.length));

    return global;
  }

  /**
   * Gets the base change VFT for the specified type.
   * @param baseType - the type to get the base change VFT for
   * @return a VFT compatible with the 'object' VFT, but with methods which in turn call the methods for the specified type
   */
  public LLVMValueRef getBaseChangeObjectVFT(Type baseType)
  {
    String mangledName = BASE_CHANGE_OBJECT_VFT_PREFIX + baseType.getMangledName();

    LLVMValueRef global = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (global != null)
    {
      return global;
    }

    LLVMTypeRef vftType = getObjectVFTType();
    global = LLVM.LLVMAddGlobal(module, vftType, mangledName);
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMLinkOnceODRLinkage);
    LLVM.LLVMSetVisibility(global, LLVM.LLVMVisibility.LLVMHiddenVisibility);

    BuiltinMethod[] methods = ObjectType.OBJECT_METHODS;
    LLVMValueRef[] llvmMethods = new LLVMValueRef[methods.length];
    for (int i = 0; i < methods.length; ++i)
    {
      Method actualMethod = baseType.getMethod(methods[i].getDisambiguator());
      llvmMethods[i] = typeHelper.getBaseChangeFunction(actualMethod);
    }
    LLVM.LLVMSetInitializer(global, LLVM.LLVMConstNamedStruct(vftType, C.toNativePointerArray(llvmMethods, false, true), llvmMethods.length));

    return global;
  }

  /**
   * Finds the VFT pointer for the specified ClassDefinition
   * @param classDefinition - the ClassDefinition to get the virtual function table pointer for
   * @return the virtual function table pointer for the specified ClassDefinition
   */
  public LLVMValueRef getVirtualFunctionTablePointer(ClassDefinition classDefinition)
  {
    String mangledName = VFT_PREFIX + classDefinition.getQualifiedName().getMangledName();
    LLVMValueRef existingVFT = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingVFT != null)
    {
      return existingVFT;
    }
    LLVMValueRef result = LLVM.LLVMAddGlobal(module, getVFTType(classDefinition), mangledName);
    return result;
  }

  /**
   * Finds a virtual function table descriptor pointer for the specified ClassDefinition
   * @param classDefinition - the ClassDefinition to get the virtual function table descriptor pointer for
   * @return the VFT descriptor pointer for the specified ClassDefinition
   */
  public LLVMValueRef getVirtualFunctionTableDescriptorPointer(ClassDefinition classDefinition)
  {
    String mangledName = VFT_DESCRIPTOR_PREFIX + classDefinition.getQualifiedName().getMangledName();
    LLVMValueRef existingDesc = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingDesc != null)
    {
      return existingDesc;
    }
    LLVMValueRef result = LLVM.LLVMAddGlobal(module, getDescriptorType(classDefinition), mangledName);
    return result;
  }


  /**
   * Adds the class's virtual function table, and stores it in the global variable that has been allocated for this VFT.
   */
  public void addVirtualFunctionTable()
  {
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalStateException("Cannot add a virtual function table for any type but a ClassDefinition");
    }
    ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
    LLVMValueRef vftPointer = getVirtualFunctionTablePointer(classDefinition);
    Method[] methods = classDefinition.getNonStaticMethods();
    LLVMValueRef[] llvmMethods = new LLVMValueRef[methods.length];
    for (int i = 0; i < methods.length; ++i)
    {
      llvmMethods[i] = codeGenerator.getMethodFunction(null, null, methods[i]);
    }
    LLVMTypeRef vftType = getVFTType(classDefinition);
    LLVM.LLVMSetInitializer(vftPointer, LLVM.LLVMConstNamedStruct(vftType, C.toNativePointerArray(llvmMethods, false, true), llvmMethods.length));
  }

  /**
   * Adds the class's virtual function table descriptor, and stores it in the global variable that has been allocated for this VFT descriptor.
   */
  public void addVirtualFunctionTableDescriptor()
  {
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalStateException("Cannot add a virtual function table descriptor for any type but a ClassDefinition");
    }
    ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
    LLVMValueRef vftDescriptorGlobalVar = getVirtualFunctionTableDescriptorPointer(classDefinition);
    Method[] methods = classDefinition.getNonStaticMethods();
    LLVMValueRef[] llvmStrings = new LLVMValueRef[methods.length];

    LLVMTypeRef byteArrayType = LLVM.LLVMArrayType(LLVM.LLVMInt8Type(), 0);
    LLVMTypeRef[] stringSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), byteArrayType};
    LLVMTypeRef stringType = LLVM.LLVMStructType(C.toNativePointerArray(stringSubTypes, false, true), stringSubTypes.length, false);
    LLVMTypeRef stringPointerType = LLVM.LLVMPointerType(stringType, 0);

    for (int i = 0; i < methods.length; ++i)
    {
      String disambiguator = methods[i].getDisambiguator().toString();
      LLVMValueRef stringConstant = codeGenerator.addStringConstant(disambiguator);
      llvmStrings[i] = LLVM.LLVMConstBitCast(stringConstant, stringPointerType);
    }
    LLVMValueRef disambiguatorArray = LLVM.LLVMConstArray(stringPointerType, C.toNativePointerArray(llvmStrings, false, true), llvmStrings.length);
    LLVMValueRef[] descriptorSubValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), llvmStrings.length, false),
                                                             disambiguatorArray};
    LLVMValueRef descriptorValue = LLVM.LLVMConstStruct(C.toNativePointerArray(descriptorSubValues, false, true), descriptorSubValues.length, false);
    LLVM.LLVMSetInitializer(vftDescriptorGlobalVar, descriptorValue);
  }

  /**
   * Finds a pointer to the first virtual function table pointer inside the specified base value.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param baseValue - the base value to find the virtual function table pointer inside
   * @return a pointer to the virtual function table pointer inside the specified base value
   */
  public LLVMValueRef getFirstVirtualFunctionTablePointer(LLVMBuilderRef builder, LLVMValueRef baseValue)
  {
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false)};
    return LLVM.LLVMBuildGEP(builder, baseValue, C.toNativePointerArray(indices, false, true), indices.length, "");
  }

  /**
   * Finds a pointer to the virtual function table pointer inside the specified base value.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param baseValue - the base value to find the virtual function table pointer inside
   * @param classDefinition - the ClassDefinition to find the virtual function table of
   * @return a pointer to the virtual function table pointer inside the specified base value
   */
  public LLVMValueRef getVirtualFunctionTablePointer(LLVMBuilderRef builder, LLVMValueRef baseValue, ClassDefinition classDefinition)
  {
    int index = 0;
    ClassDefinition superClassDefinition = classDefinition.getSuperClassDefinition();
    while (superClassDefinition != null)
    {
      index += 1 + superClassDefinition.getNonStaticFields().length;
      superClassDefinition = superClassDefinition.getSuperClassDefinition();
    }
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), index, false)};
    return LLVM.LLVMBuildGEP(builder, baseValue, C.toNativePointerArray(indices, false, true), indices.length, "");
  }

  /**
   * Finds the pointer to the specified Method inside the specified base value
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param baseValue - the base value to look up the method in one of the virtual function tables of
   * @param method - the Method to look up in a virtual function table
   * @return a pointer to the native function representing the specified method
   */
  public LLVMValueRef getMethodPointer(LLVMBuilderRef builder, LLVMValueRef baseValue, Method method)
  {
    if (method.isStatic())
    {
      throw new IllegalArgumentException("Cannot get a method pointer for a static method");
    }
    LLVMValueRef vftPointer = null;
    if (method.getContainingTypeDefinition() != null)
    {
      TypeDefinition typeDefinition = method.getContainingTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        vftPointer = getVirtualFunctionTablePointer(builder, baseValue, (ClassDefinition) typeDefinition);
      }
    }
    else if (method instanceof BuiltinMethod)
    {
      BuiltinMethod builtinMethod = (BuiltinMethod) method;
      if (builtinMethod.getBaseType() instanceof ObjectType)
      {
        vftPointer = getFirstVirtualFunctionTablePointer(builder, baseValue);
      }
    }
    if (vftPointer == null)
    {
      throw new IllegalArgumentException("Cannot get a method pointer for a method from something other than an object or a ClassDefinition");
    }
    LLVMValueRef vft = LLVM.LLVMBuildLoad(builder, vftPointer, "");
    int index = method.getMethodIndex();
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), index, false)};
    LLVMValueRef vftElement = LLVM.LLVMBuildGEP(builder, vft, C.toNativePointerArray(indices, false, true), indices.length, "");
    return LLVM.LLVMBuildLoad(builder, vftElement, "");
  }

  /**
   * Builds the VFT descriptor type for the specified ClassDefinition, and returns it
   * @param classDefinition - the ClassDefinition to find the descriptor type for
   * @return the type of a VFT descriptor for the specified ClassDefinition
   */
  public LLVMTypeRef getDescriptorType(ClassDefinition classDefinition)
  {
    int numMethods = classDefinition.getNonStaticMethods().length;
    LLVMTypeRef byteArrayType = LLVM.LLVMArrayType(LLVM.LLVMInt8Type(), 0);
    LLVMTypeRef[] stringSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), byteArrayType};
    LLVMTypeRef stringType = LLVM.LLVMStructType(C.toNativePointerArray(stringSubTypes, false, true), stringSubTypes.length, false);
    LLVMTypeRef stringPointerType = LLVM.LLVMPointerType(stringType, 0);
    LLVMTypeRef arrayType = LLVM.LLVMArrayType(stringPointerType, numMethods);
    LLVMTypeRef[] descriptorSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), arrayType};
    return LLVM.LLVMStructType(C.toNativePointerArray(descriptorSubTypes, false, true), descriptorSubTypes.length, false);
  }

  /**
   * Builds a VFT descriptor type and returns it
   * @return the type of a virtual function table descriptor
   */
  public LLVMTypeRef getGenericDescriptorType()
  {
    if (vftDescriptorType != null)
    {
      return vftDescriptorType;
    }
    LLVMTypeRef byteArrayType = LLVM.LLVMArrayType(LLVM.LLVMInt8Type(), 0);
    LLVMTypeRef[] stringSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), byteArrayType};
    LLVMTypeRef stringValueType = LLVM.LLVMStructType(C.toNativePointerArray(stringSubTypes, false, true), stringSubTypes.length, false);
    LLVMTypeRef stringType = LLVM.LLVMPointerType(stringValueType, 0);
    LLVMTypeRef stringArrayType = LLVM.LLVMArrayType(stringType, 0);
    LLVMTypeRef[] vftDescriptorSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), stringArrayType};
    vftDescriptorType = LLVM.LLVMStructCreateNamed(LLVM.LLVMGetGlobalContext(), "VFT_Descriptor");
    LLVM.LLVMStructSetBody(vftDescriptorType, C.toNativePointerArray(vftDescriptorSubTypes, false, true), vftDescriptorSubTypes.length, false);
    return vftDescriptorType;
  }

  /**
   * Finds the native type for the virtual function table for the specified ClassDefinition.
   * @param classDefinition - the ClassDefinition to find the VFT type for
   * @return the native type of the virtual function table for the specified ClassDefinition
   */
  public LLVMTypeRef getVFTType(ClassDefinition classDefinition)
  {
    LLVMTypeRef cachedResult = nativeVirtualTableTypes.get(classDefinition);
    if (cachedResult != null)
    {
      return cachedResult;
    }
    LLVMTypeRef result = LLVM.LLVMStructCreateNamed(LLVM.LLVMGetGlobalContext(), classDefinition.getQualifiedName().toString() + "_VFT");
    // cache the LLVM type before we call findMethodType(), so that once we call it, everything will be able to use this type instead of recreating it and possibly recursing infinitely
    // later on, we add the fields using LLVMStructSetBody
    nativeVirtualTableTypes.put(classDefinition, result);

    Method[] methods = classDefinition.getNonStaticMethods();
    LLVMTypeRef[] methodTypes = new LLVMTypeRef[methods.length];
    for (int i = 0; i < methods.length; ++i)
    {
      methodTypes[i] = LLVM.LLVMPointerType(typeHelper.findMethodType(methods[i]), 0);
    }
    LLVM.LLVMStructSetBody(result, C.toNativePointerArray(methodTypes, false, true), methodTypes.length, false);
    return result;
  }

  /**
   * Finds the native type for the virtual function table for the 'object' type.
   * @return the native type for the virtual function table for the 'object' type
   */
  public LLVMTypeRef getObjectVFTType()
  {
    if (objectVirtualTableType != null)
    {
      return objectVirtualTableType;
    }
    LLVMTypeRef result = LLVM.LLVMStructCreateNamed(LLVM.LLVMGetGlobalContext(), ObjectType.MANGLED_NAME + "_VFT");
    // cache the LLVM type before we call findMethodType(), so that once we call it, everything will be able to use this type instead of recreating it and possibly recursing infinitely
    // later on, we add the fields using LLVMStructSetBody
    objectVirtualTableType = result;

    Method[] methods = ObjectType.OBJECT_METHODS;
    LLVMTypeRef[] methodTypes = new LLVMTypeRef[methods.length];
    for (int i = 0; i < methods.length; ++i)
    {
      methodTypes[i] = LLVM.LLVMPointerType(typeHelper.findMethodType(methods[i]), 0);
    }
    LLVM.LLVMStructSetBody(result, C.toNativePointerArray(methodTypes, false, true), methodTypes.length, false);
    return result;
  }

  /**
   * @return the type of a generic virtual function table
   */
  private LLVMTypeRef getGenericVFTType()
  {
    if (vftType != null)
    {
      return vftType;
    }
    LLVMTypeRef element = typeHelper.getOpaquePointer();
    vftType = LLVM.LLVMArrayType(element, 0);
    return vftType;
  }

  /**
   * @return the type that is used to store a list of (Descriptor, VFT) pairs to search through for functions
   */
  private LLVMTypeRef getFunctionSearchListType(int arrayElements)
  {
    if (functionSearchListType != null)
    {
      return functionSearchListType;
    }
    LLVMTypeRef descriptorPointer = LLVM.LLVMPointerType(getGenericDescriptorType(), 0);
    LLVMTypeRef vftPointer = LLVM.LLVMPointerType(getGenericVFTType(), 0);
    LLVMTypeRef[] elementSubTypes = new LLVMTypeRef[] {descriptorPointer, vftPointer};
    LLVMTypeRef elementType = LLVM.LLVMStructType(C.toNativePointerArray(elementSubTypes, false, true), elementSubTypes.length, false);
    LLVMTypeRef arrayType = LLVM.LLVMArrayType(elementType, arrayElements);
    LLVMTypeRef[] searchListSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), arrayType};
    functionSearchListType = LLVM.LLVMStructCreateNamed(LLVM.LLVMGetGlobalContext(), "FunctionSearchList");
    LLVM.LLVMStructSetBody(functionSearchListType, C.toNativePointerArray(searchListSubTypes, false, true), searchListSubTypes.length, false);
    return functionSearchListType;
  }

  /**
   * @return the superclass VFT generator function
   */
  private LLVMValueRef getSuperclassVirtualFunctionTableGenerator()
  {
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, SUPERCLASS_VFT_GENERATOR_NAME);
    if (existingFunction != null)
    {
      return existingFunction;
    }
    LLVMTypeRef[] parameterTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(getGenericDescriptorType(), 0),
                                                      LLVM.LLVMPointerType(getGenericVFTType(), 0),
                                                      LLVM.LLVMPointerType(getFunctionSearchListType(0), 0)};
    LLVMTypeRef resultType = LLVM.LLVMPointerType(getGenericVFTType(), 0);
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(parameterTypes, false, true), parameterTypes.length, false);
    LLVMValueRef function = LLVM.LLVMAddFunction(module, SUPERCLASS_VFT_GENERATOR_NAME, functionType);
    return function;
  }

  /**
   * Generates code to generate a superclass's virtual function table, by searching through the specified descriptors in order for overridden functions.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param valueClass - the class that the VFT will be stored in, i.e. the most specialised class that the object is based on
   * @param superClass - the superclass that the VFT will be based on
   * @return the VFT generated
   */
  public LLVMValueRef buildSuperClassVFTGeneration(LLVMBuilderRef builder, ClassDefinition valueClass, ClassDefinition superClass)
  {
    List<ClassDefinition> searchClassesList = new LinkedList<ClassDefinition>();
    ClassDefinition current = valueClass;
    while (current != superClass)
    {
      searchClassesList.add(current);
      current = current.getSuperClassDefinition();
      if (current == null)
      {
        throw new IllegalArgumentException("The superClass parameter must actually be a superclass of valueClass");
      }
    }
    ClassDefinition[] searchClasses = searchClassesList.toArray(new ClassDefinition[searchClassesList.size()]);
    LLVMValueRef[] searchDescriptors = new LLVMValueRef[searchClasses.length];
    LLVMValueRef[] searchVFTs = new LLVMValueRef[searchClasses.length];
    for (int i = 0; i < searchClasses.length; ++i)
    {
      LLVMValueRef descriptor = getVirtualFunctionTableDescriptorPointer(searchClasses[i]);
      searchDescriptors[i] = LLVM.LLVMConstBitCast(descriptor, LLVM.LLVMPointerType(getGenericDescriptorType(), 0));
      LLVMValueRef vft = getVirtualFunctionTablePointer(searchClasses[i]);
      searchVFTs[i] = LLVM.LLVMConstBitCast(vft, LLVM.LLVMPointerType(getGenericVFTType(), 0));
    }

    LLVMValueRef descriptor = getVirtualFunctionTableDescriptorPointer(superClass);
    descriptor = LLVM.LLVMConstBitCast(descriptor, LLVM.LLVMPointerType(getGenericDescriptorType(), 0));
    LLVMValueRef vft = getVirtualFunctionTablePointer(superClass);
    vft = LLVM.LLVMConstBitCast(vft, LLVM.LLVMPointerType(getGenericVFTType(), 0));

    LLVMValueRef[] elements = new LLVMValueRef[searchClasses.length];
    LLVMTypeRef[] elementSubTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(getGenericDescriptorType(), 0),
                                                       LLVM.LLVMPointerType(getGenericVFTType(), 0)};
    LLVMTypeRef elementType = LLVM.LLVMStructType(C.toNativePointerArray(elementSubTypes, false, true), elementSubTypes.length, false);
    for (int i = 0; i < searchDescriptors.length; ++i)
    {
      LLVMValueRef[] structElements = new LLVMValueRef[] {searchDescriptors[i], searchVFTs[i]};
      elements[i] = LLVM.LLVMConstStruct(C.toNativePointerArray(structElements, false, true), structElements.length, false);
    }
    LLVMValueRef array = LLVM.LLVMConstArray(elementType, C.toNativePointerArray(elements, false, true), elements.length);
    LLVMValueRef[] searchListValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), searchClasses.length, false), array};
    LLVMValueRef searchList = LLVM.LLVMConstNamedStruct(getFunctionSearchListType(searchClasses.length), C.toNativePointerArray(searchListValues, false, true), searchListValues.length);

    LLVMValueRef searchListGlobal = LLVM.LLVMAddGlobal(module,
                                                       getFunctionSearchListType(searchClasses.length),
                                                       "FunctionSearchList_" + valueClass.getQualifiedName().getMangledName() + "_" + superClass.getQualifiedName().getMangledName());
    LLVM.LLVMSetLinkage(searchListGlobal, LLVM.LLVMLinkage.LLVMPrivateLinkage);
    LLVM.LLVMSetGlobalConstant(searchListGlobal, true);
    LLVM.LLVMSetInitializer(searchListGlobal, searchList);

    LLVMValueRef function = getSuperclassVirtualFunctionTableGenerator();
    LLVMValueRef[] arguments = new LLVMValueRef[] {descriptor, vft, searchListGlobal};
    return LLVM.LLVMBuildCall(builder, function, C.toNativePointerArray(arguments, false, true), arguments.length, "");
  }

  /**
   * Gets the function which will initialise all of the superclass VFTs for the specified ClassDefinition.
   * The returned function pointer will have the LLVM type signature: void()*
   * @param classDefinition - the ClassDefinition to get the VFT initialisation function for
   * @return the VFT initialisation function for the specified class
   */
  public LLVMValueRef getClassVFTInitialisationFunction()
  {
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalStateException("Cannot get a VFT initialisation function for a non-class type");
    }
    ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
    String mangledName = VFT_INIT_FUNCTION_PREFIX + classDefinition.getQualifiedName().getMangledName();
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunction != null)
    {
      return existingFunction;
    }
    LLVMTypeRef returnType = LLVM.LLVMVoidType();
    LLVMTypeRef[] paramTypes = new LLVMTypeRef[0];
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(returnType, C.toNativePointerArray(paramTypes, false, true), paramTypes.length, false);
    LLVMValueRef function = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetLinkage(function, LLVM.LLVMLinkage.LLVMPrivateLinkage);
    LLVM.LLVMSetVisibility(function, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    return function;
  }

  /**
   * Gets the global variable that will be used to store a pointer to the virtual function table for the specified superclass of the current type definition.
   * @param superClass - the superclass to generate the VFT pointer for
   * @return the superclass VFT global variable for the specified superclass of the current type definition
   */
  public LLVMValueRef getSuperClassVFTGlobal(ClassDefinition superClass)
  {
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalStateException("Cannot get a superclass's VFT global variable for a non-class type");
    }
    ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
    String mangledName = SUPERCLASS_VFT_GLOBAL_PREFIX + classDefinition.getQualifiedName().getMangledName() + "_" + superClass.getQualifiedName().getMangledName();

    LLVMValueRef existingGlobal = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingGlobal != null)
    {
      return existingGlobal;
    }

    LLVMTypeRef type = LLVM.LLVMPointerType(getVFTType(superClass), 0);
    LLVMValueRef global = LLVM.LLVMAddGlobal(module, type, mangledName);
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMPrivateLinkage);
    LLVM.LLVMSetVisibility(global, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    LLVM.LLVMSetInitializer(global, LLVM.LLVMConstNull(type));
    return global;
  }

  /**
   * Builds a function which will generate all of the superclass VFTs for the specified ClassDefinition.
   * @param classDefinition - the ClassDefinition to build the VFT initialiser for
   */
  public void addClassVFTInitialisationFunction()
  {
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalStateException("Cannot generate a VFT initialisation function for a non-class type");
    }
    ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
    LLVMValueRef function = getClassVFTInitialisationFunction();
    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(function);

    ClassDefinition superClassDefinition = classDefinition.getSuperClassDefinition();
    while (superClassDefinition != null)
    {
      LLVMValueRef vft = buildSuperClassVFTGeneration(builder, classDefinition, superClassDefinition);
      LLVMValueRef castedVFT = LLVM.LLVMBuildBitCast(builder, vft, LLVM.LLVMPointerType(getVFTType(superClassDefinition), 0), "");
      LLVMValueRef vftGlobal = getSuperClassVFTGlobal(superClassDefinition);
      LLVM.LLVMBuildStore(builder, castedVFT, vftGlobal);
      superClassDefinition = superClassDefinition.getSuperClassDefinition();
    }
    LLVM.LLVMBuildRetVoid(builder);
    LLVM.LLVMDisposeBuilder(builder);
  }
}
