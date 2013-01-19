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
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.type.NamedType;
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
  private static final String SUPERTYPE_VFT_GENERATOR_FUNCTION_NAME = "plinth_core_generate_supertype_vft";
  private static final String INTERFACE_VFT_LOOKUP_FUNCTION_NAME = "plinth_core_find_interface_vft";
  private static final String VFT_PREFIX = "_VFT_";
  private static final String VFT_DESCRIPTOR_PREFIX = "_VFT_DESC_";
  private static final String VFT_INIT_FUNCTION_PREFIX = "_SUPER_VFT_INIT_";
  private static final String SUPERCLASS_VFT_GLOBAL_PREFIX = "_SUPER_VFT_";
  private static final String BASE_CHANGE_OBJECT_VFT_PREFIX = "_base_change_o_VFT_";
  private static final String INTERFACE_SEARCH_LIST_PREFIX = "_INTERFACE_SEARCH_LIST_";

  private CodeGenerator codeGenerator;
  private TypeDefinition typeDefinition;
  private TypeHelper typeHelper;

  private LLVMModuleRef module;

  private LLVMTypeRef vftDescriptorType;
  private LLVMTypeRef vftType;
  private LLVMTypeRef functionSearchListType;
  private LLVMTypeRef interfaceSearchListType;

  private LLVMTypeRef objectVirtualTableType;
  private Map<TypeDefinition, LLVMTypeRef> nativeVirtualTableTypes = new HashMap<TypeDefinition, LLVMTypeRef>();

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
      llvmMethods[i] = codeGenerator.getMethodFunction(methods[i]);
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

    LLVMValueRef existingGlobal = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingGlobal != null)
    {
      return existingGlobal;
    }

    // create the global first, so we don't recurse before we've finished building the method list
    LLVMTypeRef vftType = getObjectVFTType();
    LLVMValueRef global = LLVM.LLVMAddGlobal(module, vftType, mangledName);
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMLinkOnceAnyLinkage);
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
   * Finds the VFT pointer for the specified TypeDefinition
   * @param typeDefinition - the TypeDefinition to get the virtual function table pointer for
   * @return the virtual function table pointer for the specified TypeDefinition
   */
  public LLVMValueRef getVFTGlobal(TypeDefinition typeDefinition)
  {
    String mangledName = VFT_PREFIX + typeDefinition.getQualifiedName().getMangledName();
    LLVMValueRef existingVFT = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingVFT != null)
    {
      return existingVFT;
    }
    LLVMValueRef result = LLVM.LLVMAddGlobal(module, getVFTType(typeDefinition), mangledName);
    return result;
  }

  /**
   * Gets the VFT descriptor pointer for the object type.
   * @return the VFT descriptor pointer for the object type
   */
  private LLVMValueRef getObjectVFTDescriptorPointer()
  {
    String mangledName = VFT_DESCRIPTOR_PREFIX + ObjectType.MANGLED_NAME;

    LLVMValueRef existingGlobal = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingGlobal != null)
    {
      return existingGlobal;
    }

    BuiltinMethod[] methods = ObjectType.OBJECT_METHODS;
    LLVMValueRef[] llvmStrings = new LLVMValueRef[methods.length];

    LLVMTypeRef stringType = LLVM.LLVMPointerType(typeHelper.findRawStringType(), 0);

    for (int i = 0; i < methods.length; ++i)
    {
      String disambiguator = methods[i].getDisambiguator().toString();
      LLVMValueRef stringConstant = codeGenerator.addStringConstant(disambiguator);
      llvmStrings[i] = LLVM.LLVMConstBitCast(stringConstant, stringType);
    }
    LLVMValueRef disambiguatorArray = LLVM.LLVMConstArray(stringType, C.toNativePointerArray(llvmStrings, false, true), llvmStrings.length);
    LLVMValueRef[] descriptorSubValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), llvmStrings.length, false),
                                                             disambiguatorArray};
    LLVMValueRef descriptorValue = LLVM.LLVMConstStruct(C.toNativePointerArray(descriptorSubValues, false, true), descriptorSubValues.length, false);

    LLVMValueRef objectVFTDescriptor = LLVM.LLVMAddGlobal(module, getDescriptorType(methods.length), "");
    LLVM.LLVMSetLinkage(objectVFTDescriptor, LLVM.LLVMLinkage.LLVMLinkOnceAnyLinkage);
    LLVM.LLVMSetVisibility(objectVFTDescriptor, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    LLVM.LLVMSetInitializer(objectVFTDescriptor, descriptorValue);

    return objectVFTDescriptor;
  }

  /**
   * Finds a virtual function table descriptor pointer for the specified TypeDefinition
   * @param typeDefinition - the TypeDefinition to get the virtual function table descriptor pointer for
   * @return the VFT descriptor pointer for the specified TypeDefinition
   */
  public LLVMValueRef getVFTDescriptorPointer(TypeDefinition typeDefinition)
  {
    String mangledName = VFT_DESCRIPTOR_PREFIX + typeDefinition.getQualifiedName().getMangledName();
    LLVMValueRef existingDesc = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingDesc != null)
    {
      return existingDesc;
    }
    LLVMValueRef result = LLVM.LLVMAddGlobal(module, getDescriptorType(typeDefinition.getNonStaticMethods().length), mangledName);
    return result;
  }


  /**
   * Adds the class's virtual function table, and stores it in the global variable that has been allocated for this VFT.
   */
  public void addVirtualFunctionTable()
  {
    if (!(typeDefinition instanceof ClassDefinition) && !(typeDefinition instanceof InterfaceDefinition))
    {
      throw new IllegalStateException("Cannot add a virtual function table for types which are neither a ClassDefinition nor an InterfaceDefinition");
    }
    LLVMValueRef vftGlobal = getVFTGlobal(typeDefinition);
    Method[] methods = typeDefinition.getNonStaticMethods();
    LLVMValueRef[] llvmMethods = new LLVMValueRef[methods.length];
    for (int i = 0; i < methods.length; ++i)
    {
      if (methods[i].isAbstract())
      {
        llvmMethods[i] = LLVM.LLVMConstNull(LLVM.LLVMPointerType(typeHelper.findMethodType(methods[i]), 0));
      }
      else
      {
        llvmMethods[i] = codeGenerator.getMethodFunction(methods[i]);
      }
    }
    LLVMTypeRef vftType = getVFTType(typeDefinition);
    LLVM.LLVMSetInitializer(vftGlobal, LLVM.LLVMConstNamedStruct(vftType, C.toNativePointerArray(llvmMethods, false, true), llvmMethods.length));
  }

  /**
   * Adds the class's virtual function table descriptor, and stores it in the global variable that has been allocated for this VFT descriptor.
   */
  public void addVirtualFunctionTableDescriptor()
  {
    if (!(typeDefinition instanceof ClassDefinition) && !(typeDefinition instanceof InterfaceDefinition))
    {
      throw new IllegalStateException("Cannot add a virtual function table descriptor for a type which is neither a ClassDefinition nor an InterfaceDefinition");
    }
    LLVMValueRef vftDescriptorGlobalVar = getVFTDescriptorPointer(typeDefinition);
    Method[] methods = typeDefinition.getNonStaticMethods();
    LLVMValueRef[] llvmStrings = new LLVMValueRef[methods.length];

    LLVMTypeRef stringType = LLVM.LLVMPointerType(typeHelper.findRawStringType(), 0);

    for (int i = 0; i < methods.length; ++i)
    {
      String disambiguator = methods[i].getDisambiguator().toString();
      LLVMValueRef stringConstant = codeGenerator.addStringConstant(disambiguator);
      llvmStrings[i] = LLVM.LLVMConstBitCast(stringConstant, stringType);
    }
    LLVMValueRef disambiguatorArray = LLVM.LLVMConstArray(stringType, C.toNativePointerArray(llvmStrings, false, true), llvmStrings.length);
    LLVMValueRef[] descriptorSubValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), llvmStrings.length, false),
                                                             disambiguatorArray};
    LLVMValueRef descriptorValue = LLVM.LLVMConstStruct(C.toNativePointerArray(descriptorSubValues, false, true), descriptorSubValues.length, false);
    LLVM.LLVMSetInitializer(vftDescriptorGlobalVar, descriptorValue);
  }

  /**
   * Gets the interface search list for the current class definition. This method assumes that the current type definition is a class definition.
   * @return the interface search list for the current class definition
   */
  public LLVMValueRef getInterfaceSearchList()
  {
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalStateException("Cannot get an interface search list for a non-class type");
    }
    String mangledName = INTERFACE_SEARCH_LIST_PREFIX + typeDefinition.getQualifiedName().getMangledName();
    LLVMValueRef existingValue = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingValue != null)
    {
      return existingValue;
    }

    List<InterfaceDefinition> interfaceList = new LinkedList<InterfaceDefinition>();
    for (TypeDefinition t : typeDefinition.getInheritanceLinearisation())
    {
      if (t instanceof InterfaceDefinition)
      {
        interfaceList.add((InterfaceDefinition) t);
      }
    }
    InterfaceDefinition[] interfaces = interfaceList.toArray(new InterfaceDefinition[interfaceList.size()]);
    LLVMValueRef[] elements = new LLVMValueRef[interfaces.length];

    LLVMTypeRef stringType = LLVM.LLVMPointerType(typeHelper.findRawStringType(), 0);
    LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(getGenericVFTType(), 0);
    LLVMTypeRef[] elementSubTypes = new LLVMTypeRef[] {stringType, vftPointerType};
    LLVMTypeRef elementType = LLVM.LLVMStructType(C.toNativePointerArray(elementSubTypes, false, true), elementSubTypes.length, false);
    LLVMTypeRef arrayType = LLVM.LLVMArrayType(elementType, elements.length);
    LLVMTypeRef[] structSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), arrayType};
    LLVMTypeRef structType = LLVM.LLVMStructType(C.toNativePointerArray(structSubTypes, false, true), structSubTypes.length, false);

    for (int i = 0; i < interfaces.length; ++i)
    {
      LLVMValueRef stringValue = codeGenerator.addStringConstant(interfaces[i].getQualifiedName().getMangledName());
      LLVMValueRef convertedString = LLVM.LLVMConstBitCast(stringValue, stringType);
      LLVMValueRef nullVFTValue = LLVM.LLVMConstNull(vftPointerType);
      LLVMValueRef[] elementSubValues = new LLVMValueRef[] {convertedString, nullVFTValue};
      elements[i] = LLVM.LLVMConstStruct(C.toNativePointerArray(elementSubValues, false, true), elementSubValues.length, false);
    }

    LLVMValueRef array = LLVM.LLVMConstArray(elementType, C.toNativePointerArray(elements, false, true), elements.length);

    LLVMValueRef lengthValue = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), elements.length, false);
    LLVMValueRef[] structSubValues = new LLVMValueRef[] {lengthValue, array};
    LLVMValueRef struct = LLVM.LLVMConstStruct(C.toNativePointerArray(structSubValues, false, true), structSubValues.length, false);

    LLVMValueRef global = LLVM.LLVMAddGlobal(module, structType, mangledName);
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMPrivateLinkage);
    LLVM.LLVMSetVisibility(global, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    LLVM.LLVMSetInitializer(global, struct);
    return global;
  }

  /**
   * @return the interface search list for any types which don't implement any interfaces
   */
  public LLVMValueRef getEmptyInterfaceSearchList()
  {
    String mangledName = INTERFACE_SEARCH_LIST_PREFIX + ObjectType.MANGLED_NAME;
    LLVMValueRef existingValue = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingValue != null)
    {
      return existingValue;
    }

    LLVMTypeRef stringType = LLVM.LLVMPointerType(typeHelper.findRawStringType(), 0);
    LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(getGenericVFTType(), 0);
    LLVMTypeRef[] elementSubTypes = new LLVMTypeRef[] {stringType, vftPointerType};
    LLVMTypeRef elementType = LLVM.LLVMStructType(C.toNativePointerArray(elementSubTypes, false, true), elementSubTypes.length, false);

    LLVMValueRef[] elements = new LLVMValueRef[0];
    LLVMValueRef array = LLVM.LLVMConstArray(elementType, C.toNativePointerArray(elements, false, true), elements.length);
    LLVMValueRef lengthValue = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), elements.length, false);

    LLVMTypeRef structType = getInterfaceSearchListType();
    LLVMValueRef[] structSubValues = new LLVMValueRef[] {lengthValue, array};
    LLVMValueRef struct = LLVM.LLVMConstNamedStruct(structType, C.toNativePointerArray(structSubValues, false, true), structSubValues.length);

    LLVMValueRef global = LLVM.LLVMAddGlobal(module, structType, mangledName);
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMLinkOnceAnyLinkage);
    LLVM.LLVMSetVisibility(global, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    LLVM.LLVMSetInitializer(global, struct);
    return global;
  }

  /**
   * Finds a pointer to the interface lookup table pointer inside the specified base value.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param baseValue - the base value to find the interface lookup table pointer inside
   * @return a pointer to the interface lookup table pointer inside the specified base value
   */
  public LLVMValueRef getInterfaceSearchListPointer(LLVMBuilderRef builder, LLVMValueRef baseValue)
  {
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false)};
    return LLVM.LLVMBuildGEP(builder, baseValue, C.toNativePointerArray(indices, false, true), indices.length, "");
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
                                                 LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false)};
    return LLVM.LLVMBuildGEP(builder, baseValue, C.toNativePointerArray(indices, false, true), indices.length, "");
  }

  /**
   * Finds a pointer to a virtual function table pointer inside the specified base value.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param baseValue - the base value to find the virtual function table pointer inside
   * @param subclassDefinition - the ClassDefinition to find the virtual function table inside, can be equal to searchTypeDefinition if it is not an interface
   * @param searchTypeDefinition - the TypeDefinition to find the virtual function table of
   * @return a pointer to the virtual function table pointer inside the specified base value
   */
  public LLVMValueRef getVirtualFunctionTablePointer(LLVMBuilderRef builder, LLVMValueRef baseValue, ClassDefinition subclassDefinition, TypeDefinition searchTypeDefinition)
  {
    ClassDefinition encapsulatingClassDefinition = null;
    if (searchTypeDefinition instanceof ClassDefinition)
    {
      encapsulatingClassDefinition = (ClassDefinition) searchTypeDefinition;
    }
    else
    {
      ClassDefinition current = subclassDefinition;
      while (current != null)
      {
        boolean inLinearisation = false;
        TypeDefinition[] linearisation = current.getInheritanceLinearisation();
        for (TypeDefinition t : linearisation)
        {
          if (t == searchTypeDefinition)
          {
            inLinearisation = true;
            break;
          }
        }
        if (inLinearisation)
        {
          encapsulatingClassDefinition = current;
          // keep going, so that we find the highest-up class with this interface in its linearisation
        }
        current = current.getSuperClassDefinition();
      }
      if (encapsulatingClassDefinition == null)
      {
        throw new IllegalArgumentException("Cannot find a VFT pointer for " + searchTypeDefinition.getQualifiedName() + " inside " + subclassDefinition.getQualifiedName());
      }
    }
    // start at 2 to skip the interface search list and the object VFT
    int index = 2;
    ClassDefinition superClassDefinition = encapsulatingClassDefinition.getSuperClassDefinition();
    while (superClassDefinition != null)
    {
      index += 1 + typeHelper.findSubClassInterfaces(superClassDefinition).length + superClassDefinition.getNonStaticFields().length;
      superClassDefinition = superClassDefinition.getSuperClassDefinition();
    }
    if (searchTypeDefinition instanceof InterfaceDefinition)
    {
      InterfaceDefinition[] subClassInterfaces = typeHelper.findSubClassInterfaces(encapsulatingClassDefinition);
      for (int i = 0; i < subClassInterfaces.length; ++i)
      {
        if (subClassInterfaces[i] == searchTypeDefinition)
        {
          // skip the class's VFT, and all of the interfaces before this one
          index += 1 + i;
          break;
        }
      }
    }
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), index, false)};
    return LLVM.LLVMBuildGEP(builder, baseValue, C.toNativePointerArray(indices, false, true), indices.length, "");
  }

  /**
   * Finds the pointer to the specified Method inside the specified base value
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param baseValue - the base value to look up the method in one of the virtual function tables of
   * @param baseType - the Type of the base value
   * @param method - the Method to look up in a virtual function table
   * @return a pointer to the native function representing the specified method
   */
  public LLVMValueRef getMethodPointer(LLVMBuilderRef builder, LLVMValueRef baseValue, Type baseType, Method method)
  {
    if (method.isStatic())
    {
      throw new IllegalArgumentException("Cannot get a method pointer for a static method");
    }
    LLVMValueRef vft = null;
    TypeDefinition methodTypeDefinition = method.getContainingTypeDefinition();
    if (methodTypeDefinition != null)
    {
      if (!(baseType instanceof NamedType))
      {
        throw new IllegalArgumentException("Cannot get a method pointer for non-built-in method on anything other than a NamedType");
      }
      TypeDefinition baseTypeDefinition = ((NamedType) baseType).getResolvedTypeDefinition();
      if (baseTypeDefinition instanceof ClassDefinition)
      {
        LLVMValueRef vftPointer = getVirtualFunctionTablePointer(builder, baseValue, (ClassDefinition) baseTypeDefinition, methodTypeDefinition);
        vft = LLVM.LLVMBuildLoad(builder, vftPointer, "");
      }
      else if (baseTypeDefinition instanceof InterfaceDefinition)
      {
        boolean inLinearisation = false;
        for (TypeDefinition t : baseTypeDefinition.getInheritanceLinearisation())
        {
          if (t == methodTypeDefinition)
          {
            inLinearisation = true;
            break;
          }
        }
        if (!inLinearisation)
        {
          throw new IllegalArgumentException("Cannot get a method pointer for '" + method.getName() + "', it is not part of the base value's type: " + baseType);
        }
        LLVMValueRef convertedBaseValue = typeHelper.convertTemporary(builder, baseValue, baseType, new NamedType(false, false, methodTypeDefinition));
        // extract the VFT from the interface's type representation
        vft = LLVM.LLVMBuildExtractValue(builder, convertedBaseValue, 0, "");
      }
    }
    else if (method instanceof BuiltinMethod)
    {
      if (baseType instanceof NamedType && ((NamedType) baseType).getResolvedTypeDefinition() instanceof InterfaceDefinition)
      {
        ObjectType objectType = new ObjectType(false, false, null);
        baseValue = typeHelper.convertTemporary(builder, baseValue, baseType, objectType);
        baseType = objectType;
      }
      if (baseType instanceof ObjectType ||
          (baseType instanceof NamedType && ((NamedType) baseType).getResolvedTypeDefinition() instanceof ClassDefinition))
      {
        LLVMValueRef vftPointer = getFirstVirtualFunctionTablePointer(builder, baseValue);
        vft = LLVM.LLVMBuildLoad(builder, vftPointer, "");
      }
    }
    if (vft == null)
    {
      throw new IllegalArgumentException("Cannot get a method pointer for a method from anything but an object, a ClassDefinition, or an InterfaceDefinition");
    }
    int index = method.getMethodIndex();
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), index, false)};
    LLVMValueRef vftElement = LLVM.LLVMBuildGEP(builder, vft, C.toNativePointerArray(indices, false, true), indices.length, "");
    return LLVM.LLVMBuildLoad(builder, vftElement, "");
  }

  /**
   * Builds the VFT descriptor type for the specified TypeDefinition, and returns it
   * @param numMethods - the number of methods that will be included in the VFT descriptor
   * @return the type of a VFT descriptor for the specified TypeDefinition
   */
  private LLVMTypeRef getDescriptorType(int numMethods)
  {
    LLVMTypeRef stringType = LLVM.LLVMPointerType(typeHelper.findRawStringType(), 0);
    LLVMTypeRef arrayType = LLVM.LLVMArrayType(stringType, numMethods);
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
    LLVMTypeRef stringType = LLVM.LLVMPointerType(typeHelper.findRawStringType(), 0);
    LLVMTypeRef stringArrayType = LLVM.LLVMArrayType(stringType, 0);
    LLVMTypeRef[] vftDescriptorSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), stringArrayType};
    vftDescriptorType = LLVM.LLVMStructCreateNamed(LLVM.LLVMGetGlobalContext(), "VFT_Descriptor");
    LLVM.LLVMStructSetBody(vftDescriptorType, C.toNativePointerArray(vftDescriptorSubTypes, false, true), vftDescriptorSubTypes.length, false);
    return vftDescriptorType;
  }

  /**
   * Finds the native type for the virtual function table for the specified TypeDefinition.
   * @param typeDefinition - the TypeDefinition to find the VFT type for
   * @return the native type of the virtual function table for the specified TypeDefinition
   */
  public LLVMTypeRef getVFTType(TypeDefinition typeDefinition)
  {
    LLVMTypeRef cachedResult = nativeVirtualTableTypes.get(typeDefinition);
    if (cachedResult != null)
    {
      return cachedResult;
    }
    LLVMTypeRef result = LLVM.LLVMStructCreateNamed(LLVM.LLVMGetGlobalContext(), typeDefinition.getQualifiedName().toString() + "_VFT");
    // cache the LLVM type before we call findMethodType(), so that once we call it, everything will be able to use this type instead of recreating it and possibly recursing infinitely
    // later on, we add the fields using LLVMStructSetBody
    nativeVirtualTableTypes.put(typeDefinition, result);

    Method[] methods = typeDefinition.getNonStaticMethods();
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
   * Finds the type of an interface search list, a named struct type representing: {i32, [0 x {%RawString*, %VFT*}]}
   * @return the LLVM type of the an interface search list
   */
  public LLVMTypeRef getInterfaceSearchListType()
  {
    if (interfaceSearchListType != null)
    {
      return interfaceSearchListType;
    }
    // store the named struct in interfaceSearchListType first, so that when we get the raw string type we don't infinitely recurse
    interfaceSearchListType = LLVM.LLVMStructCreateNamed(LLVM.LLVMGetGlobalContext(), "InterfaceSearchList");
    LLVMTypeRef stringType = LLVM.LLVMPointerType(typeHelper.findRawStringType(), 0);
    LLVMTypeRef vftType = LLVM.LLVMPointerType(getGenericVFTType(), 0);
    LLVMTypeRef[] elementSubTypes = new LLVMTypeRef[] {stringType, vftType};
    LLVMTypeRef elementType = LLVM.LLVMStructType(C.toNativePointerArray(elementSubTypes, false, true), elementSubTypes.length, false);
    LLVMTypeRef arrayType = LLVM.LLVMArrayType(elementType, 0);
    LLVMTypeRef[] searchListSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), arrayType};
    LLVM.LLVMStructSetBody(interfaceSearchListType, C.toNativePointerArray(searchListSubTypes, false, true), searchListSubTypes.length, false);
    return interfaceSearchListType;
  }

  /**
   * @return the type that is used to store a list of (Descriptor, VFT) pairs to search through for functions
   */
  private LLVMTypeRef getFunctionSearchListType()
  {
    if (functionSearchListType != null)
    {
      return functionSearchListType;
    }
    LLVMTypeRef descriptorPointer = LLVM.LLVMPointerType(getGenericDescriptorType(), 0);
    LLVMTypeRef vftPointer = LLVM.LLVMPointerType(getGenericVFTType(), 0);
    LLVMTypeRef[] elementSubTypes = new LLVMTypeRef[] {descriptorPointer, vftPointer};
    LLVMTypeRef elementType = LLVM.LLVMStructType(C.toNativePointerArray(elementSubTypes, false, true), elementSubTypes.length, false);
    LLVMTypeRef arrayType = LLVM.LLVMArrayType(elementType, 0);
    LLVMTypeRef[] searchListSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), arrayType};
    functionSearchListType = LLVM.LLVMStructCreateNamed(LLVM.LLVMGetGlobalContext(), "FunctionSearchList");
    LLVM.LLVMStructSetBody(functionSearchListType, C.toNativePointerArray(searchListSubTypes, false, true), searchListSubTypes.length, false);
    return functionSearchListType;
  }

  /**
   * @return the superclass VFT generator function
   */
  private LLVMValueRef getSuperTypeVFTGeneratorFunction()
  {
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, SUPERTYPE_VFT_GENERATOR_FUNCTION_NAME);
    if (existingFunction != null)
    {
      return existingFunction;
    }
    LLVMTypeRef[] parameterTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(getGenericDescriptorType(), 0),
                                                      LLVM.LLVMPointerType(getGenericVFTType(), 0),
                                                      LLVM.LLVMPointerType(getFunctionSearchListType(), 0)};
    LLVMTypeRef resultType = LLVM.LLVMPointerType(getGenericVFTType(), 0);
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(parameterTypes, false, true), parameterTypes.length, false);
    LLVMValueRef function = LLVM.LLVMAddFunction(module, SUPERTYPE_VFT_GENERATOR_FUNCTION_NAME, functionType);
    return function;
  }

  /**
   * @return the interface VFT lookup function
   */
  private LLVMValueRef getInterfaceVFTLookupFunction()
  {
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, INTERFACE_VFT_LOOKUP_FUNCTION_NAME);
    if (existingFunction != null)
    {
      return existingFunction;
    }
    LLVMTypeRef stringType = LLVM.LLVMPointerType(typeHelper.findRawStringType(), 0);
    LLVMTypeRef[] parameterTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(getInterfaceSearchListType(), 0), stringType};
    LLVMTypeRef resultType = LLVM.LLVMPointerType(getGenericVFTType(), 0);
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(parameterTypes, false, true), parameterTypes.length, false);
    LLVMValueRef function = LLVM.LLVMAddFunction(module, INTERFACE_VFT_LOOKUP_FUNCTION_NAME, functionType);
    return function;
  }

  /**
   * Builds code to lookup the specified interface's VFT inside the specified object value.
   * @param builder - the builder to build code with
   * @param objectValue - the object to look up the interface's VFT inside
   * @param interfaceDefinition - the InterfaceDefinition to search for
   * @return an LLVMValueRef representing a pointer to the resulting VFT, or a null pointer if this object does not implement the specified interface
   */
  public LLVMValueRef lookupInterfaceVFT(LLVMBuilderRef builder, LLVMValueRef objectValue, InterfaceDefinition interfaceDefinition)
  {
    LLVMValueRef interfaceSearchListPointer = getInterfaceSearchListPointer(builder, objectValue);
    LLVMValueRef interfaceSearchList = LLVM.LLVMBuildLoad(builder, interfaceSearchListPointer, "");
    interfaceSearchList = LLVM.LLVMBuildBitCast(builder, interfaceSearchList, LLVM.LLVMPointerType(getInterfaceSearchListType(), 0), "");

    String interfaceMangledName = interfaceDefinition.getQualifiedName().getMangledName();
    LLVMValueRef interfaceRawString = codeGenerator.addStringConstant(interfaceMangledName);
    LLVMTypeRef stringType = LLVM.LLVMPointerType(typeHelper.findRawStringType(), 0);
    interfaceRawString = LLVM.LLVMBuildBitCast(builder, interfaceRawString, stringType, "");

    LLVMValueRef[] arguments = new LLVMValueRef[] {interfaceSearchList, interfaceRawString};
    LLVMValueRef interfaceVFTLookupFunction = getInterfaceVFTLookupFunction();
    LLVMValueRef result = LLVM.LLVMBuildCall(builder, interfaceVFTLookupFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");
    return result;
  }

  /**
   * Gets the function search list that will be used for looking up methods in the current type definition.
   * @return the function search list
   */
  private LLVMValueRef getFunctionSearchList()
  {
    String mangledName = "FunctionSearchList_" + typeDefinition.getQualifiedName().getMangledName();
    LLVMValueRef existingGlobal = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingGlobal != null)
    {
      return existingGlobal;
    }

    TypeDefinition[] searchTypes = typeDefinition.getInheritanceLinearisation();

    LLVMValueRef[] searchDescriptors = new LLVMValueRef[searchTypes.length + 1];
    LLVMValueRef[] searchVFTs = new LLVMValueRef[searchTypes.length + 1];
    for (int i = 0; i < searchTypes.length; ++i)
    {
      LLVMValueRef descriptor = getVFTDescriptorPointer(searchTypes[i]);
      searchDescriptors[i] = LLVM.LLVMConstBitCast(descriptor, LLVM.LLVMPointerType(getGenericDescriptorType(), 0));
      LLVMValueRef vft = getVFTGlobal(searchTypes[i]);
      searchVFTs[i] = LLVM.LLVMConstBitCast(vft, LLVM.LLVMPointerType(getGenericVFTType(), 0));
    }
    LLVMValueRef objectDescriptor = getObjectVFTDescriptorPointer();
    searchDescriptors[searchTypes.length] = LLVM.LLVMConstBitCast(objectDescriptor, LLVM.LLVMPointerType(getGenericDescriptorType(), 0));
    LLVMValueRef objectVFT = getObjectVFTGlobal();
    searchVFTs[searchTypes.length] = LLVM.LLVMConstBitCast(objectVFT, LLVM.LLVMPointerType(getGenericVFTType(), 0));

    LLVMValueRef[] elements = new LLVMValueRef[searchTypes.length + 1];
    LLVMTypeRef[] elementSubTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(getGenericDescriptorType(), 0),
                                                       LLVM.LLVMPointerType(getGenericVFTType(), 0)};
    LLVMTypeRef elementType = LLVM.LLVMStructType(C.toNativePointerArray(elementSubTypes, false, true), elementSubTypes.length, false);
    for (int i = 0; i < elements.length; ++i)
    {
      LLVMValueRef[] structElements = new LLVMValueRef[] {searchDescriptors[i], searchVFTs[i]};
      elements[i] = LLVM.LLVMConstStruct(C.toNativePointerArray(structElements, false, true), structElements.length, false);
    }
    LLVMValueRef array = LLVM.LLVMConstArray(elementType, C.toNativePointerArray(elements, false, true), elements.length);
    LLVMValueRef[] searchListValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), elements.length, false), array};
    LLVMValueRef searchList = LLVM.LLVMConstStruct(C.toNativePointerArray(searchListValues, false, true), searchListValues.length, false);

    LLVMTypeRef arrayType = LLVM.LLVMArrayType(elementType, elements.length);
    LLVMTypeRef[] searchListSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), arrayType};
    LLVMTypeRef searchListType = LLVM.LLVMStructType(C.toNativePointerArray(searchListSubTypes, false, true), searchListSubTypes.length, false);

    LLVMValueRef searchListGlobal = LLVM.LLVMAddGlobal(module, searchListType, mangledName);
    LLVM.LLVMSetLinkage(searchListGlobal, LLVM.LLVMLinkage.LLVMPrivateLinkage);
    LLVM.LLVMSetVisibility(searchListGlobal, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    LLVM.LLVMSetGlobalConstant(searchListGlobal, true);
    LLVM.LLVMSetInitializer(searchListGlobal, searchList);

    return searchListGlobal;
  }

  /**
   * Generates code to generate a super-type's virtual function table, by searching through the function search list.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param superType - the super-type that the VFT will be based on
   * @return the VFT generated
   */
  private LLVMValueRef buildSuperTypeVFTGeneration(LLVMBuilderRef builder, TypeDefinition superType)
  {
    LLVMValueRef descriptor = getVFTDescriptorPointer(superType);
    descriptor = LLVM.LLVMConstBitCast(descriptor, LLVM.LLVMPointerType(getGenericDescriptorType(), 0));
    LLVMValueRef vft = getVFTGlobal(superType);
    vft = LLVM.LLVMConstBitCast(vft, LLVM.LLVMPointerType(getGenericVFTType(), 0));

    LLVMValueRef functionSearchList = getFunctionSearchList();
    functionSearchList = LLVM.LLVMBuildBitCast(builder, functionSearchList, LLVM.LLVMPointerType(getFunctionSearchListType(), 0), "");

    LLVMValueRef function = getSuperTypeVFTGeneratorFunction();
    LLVMValueRef[] arguments = new LLVMValueRef[] {descriptor, vft, functionSearchList};
    return LLVM.LLVMBuildCall(builder, function, C.toNativePointerArray(arguments, false, true), arguments.length, "");
  }

  private LLVMValueRef buildObjectSuperTypeVFTGeneration(LLVMBuilderRef builder)
  {
    LLVMValueRef descriptor = getObjectVFTDescriptorPointer();
    descriptor = LLVM.LLVMConstBitCast(descriptor, LLVM.LLVMPointerType(getGenericDescriptorType(), 0));
    LLVMValueRef vft = getObjectVFTGlobal();
    vft = LLVM.LLVMConstBitCast(vft, LLVM.LLVMPointerType(getGenericVFTType(), 0));

    LLVMValueRef functionSearchList = getFunctionSearchList();
    functionSearchList = LLVM.LLVMBuildBitCast(builder, functionSearchList, LLVM.LLVMPointerType(getFunctionSearchListType(), 0), "");

    LLVMValueRef function = getSuperTypeVFTGeneratorFunction();
    LLVMValueRef[] arguments = new LLVMValueRef[] {descriptor, vft, functionSearchList};
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
    if (typeDefinition.isAbstract())
    {
      throw new IllegalStateException("Cannot get a VFT initialisation function for an abstract type");
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
   * Gets the global variable that will be used to store a pointer to the virtual function table for the specified super-type of the current type definition.
   * @param superType - the super-type to generate the VFT pointer for
   * @return the super-type VFT global variable for the specified super-type of the current type definition
   */
  public LLVMValueRef getSuperTypeVFTGlobal(TypeDefinition superType)
  {
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalStateException("Cannot get a superclass's VFT global variable for a non-class type");
    }

    ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
    if (superType instanceof InterfaceDefinition)
    {
      // interface VFTs are stored inside the interface search list, so find the index and return the pointer
      int interfaceIndex = 0;
      for (TypeDefinition t : classDefinition.getInheritanceLinearisation())
      {
        if (t instanceof InterfaceDefinition)
        {
          if (t == superType)
          {
            break;
          }
          ++interfaceIndex;
        }
      }
      LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                   LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false),
                                                   LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), interfaceIndex, false),
                                                   LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false)};
      LLVMValueRef value = LLVM.LLVMConstGEP(getInterfaceSearchList(), C.toNativePointerArray(indices, false, true), indices.length);
      LLVMTypeRef type = LLVM.LLVMPointerType(LLVM.LLVMPointerType(getVFTType(superType), 0), 0);
      return LLVM.LLVMConstBitCast(value, type);
    }

    String mangledName = SUPERCLASS_VFT_GLOBAL_PREFIX + classDefinition.getQualifiedName().getMangledName() + "_" + superType.getQualifiedName().getMangledName();

    LLVMValueRef existingGlobal = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingGlobal != null)
    {
      return existingGlobal;
    }

    LLVMTypeRef type = LLVM.LLVMPointerType(getVFTType(superType), 0);
    LLVMValueRef global = LLVM.LLVMAddGlobal(module, type, mangledName);
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMPrivateLinkage);
    LLVM.LLVMSetVisibility(global, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    LLVM.LLVMSetInitializer(global, LLVM.LLVMConstNull(type));
    return global;
  }

  /**
   * Gets the global variable that will be used to store a pointer to the virtual funtion table for the 'object' super-type of the current type definition.
   * @return the object super-type VFT global variable for the current type definition
   */
  public LLVMValueRef getObjectSuperTypeVFTGlobal()
  {
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalStateException("Cannot get a superclass's VFT global variable for a non-class type");
    }
    ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
    String mangledName = SUPERCLASS_VFT_GLOBAL_PREFIX + classDefinition.getQualifiedName().getMangledName() + "_" + ObjectType.MANGLED_NAME;

    LLVMValueRef existingGlobal = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingGlobal != null)
    {
      return existingGlobal;
    }

    LLVMTypeRef type = LLVM.LLVMPointerType(getObjectVFTType(), 0);
    LLVMValueRef global = LLVM.LLVMAddGlobal(module, type, mangledName);
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMPrivateLinkage);
    LLVM.LLVMSetVisibility(global, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    LLVM.LLVMSetInitializer(global, LLVM.LLVMConstNull(type));
    return global;
  }

  /**
   * Builds a function which will generate all of the super-type VFTs for the specified ClassDefinition.
   */
  public void addClassVFTInitialisationFunction()
  {
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalStateException("Cannot generate a VFT initialisation function for a non-class type");
    }
    LLVMValueRef function = getClassVFTInitialisationFunction();
    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(function);

    for (TypeDefinition superType : typeDefinition.getInheritanceLinearisation())
    {
      if (superType == typeDefinition)
      {
        continue;
      }
      LLVMValueRef vft = buildSuperTypeVFTGeneration(builder, superType);
      vft = LLVM.LLVMBuildBitCast(builder, vft, LLVM.LLVMPointerType(getVFTType(superType), 0), "");
      LLVMValueRef vftGlobal = getSuperTypeVFTGlobal(superType);
      LLVM.LLVMBuildStore(builder, vft, vftGlobal);
    }
    LLVMValueRef objectVFT = buildObjectSuperTypeVFTGeneration(builder);
    objectVFT = LLVM.LLVMBuildBitCast(builder, objectVFT, LLVM.LLVMPointerType(getObjectVFTType(), 0), "");
    LLVMValueRef objectVFTGlobal = getObjectSuperTypeVFTGlobal();
    LLVM.LLVMBuildStore(builder, objectVFT, objectVFTGlobal);

    LLVM.LLVMBuildRetVoid(builder);
    LLVM.LLVMDisposeBuilder(builder);
  }
}
