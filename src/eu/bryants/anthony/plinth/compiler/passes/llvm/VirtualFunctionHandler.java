package eu.bryants.anthony.plinth.compiler.passes.llvm;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMBasicBlockRef;
import nativelib.llvm.LLVM.LLVMBuilderRef;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMTypeRef;
import nativelib.llvm.LLVM.LLVMValueRef;
import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.metadata.GenericTypeSpecialiser;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunction;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunctionType;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;
import eu.bryants.anthony.plinth.ast.metadata.OverrideFunction;
import eu.bryants.anthony.plinth.ast.metadata.PropertyReference;
import eu.bryants.anthony.plinth.ast.metadata.VirtualFunction;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.ast.type.VoidType;

/*
 * Created on 4 Dec 2012
 */

/**
 * @author Anthony Bryant
 */
public class VirtualFunctionHandler
{
  private static final String SUPERTYPE_VFT_GENERATOR_FUNCTION_NAME = "plinth_core_generate_supertype_vft";
  private static final String VFT_PREFIX = "_VFT_";
  private static final String VFT_DESCRIPTOR_PREFIX = "_VFT_DESC_";
  private static final String VFT_INIT_FUNCTION_PREFIX = "_SUPER_VFT_INIT_";
  private static final String BASE_CHANGE_OBJECT_VFT_PREFIX = "_base_change_o_VFT_";
  private static final String TYPE_SEARCH_LIST_PREFIX = "_TYPE_SEARCH_LIST_";
  private static final String PROXY_OVERRIDE_FUNCTION_PREFIX = "_PROXY_OVERRIDE_";

  private CodeGenerator codeGenerator;
  private TypeHelper typeHelper;
  private RTTIHelper rttiHelper;

  private TypeDefinition typeDefinition;
  private LLVMModuleRef module;

  private LLVMTypeRef vftDescriptorType;
  private LLVMTypeRef vftType;
  private LLVMTypeRef functionSearchListType;

  private LLVMTypeRef objectVirtualTableType;
  private Map<TypeDefinition, LLVMTypeRef> nativeVirtualTableTypes = new HashMap<TypeDefinition, LLVMTypeRef>();

  public VirtualFunctionHandler(CodeGenerator codeGenerator, TypeDefinition typeDefinition, LLVMModuleRef module)
  {
    this.codeGenerator = codeGenerator;
    this.typeDefinition = typeDefinition;
    this.module = module;
  }

  /**
   * Initialises this VirtualFunctionHandler, so that it has all of the references required to operate.
   * @param typeHelper - the TypeHelper to set
   * @param rttiHelper - the RTTIHelper to set
   */
  public void initialise(TypeHelper typeHelper, RTTIHelper rttiHelper)
  {
    this.typeHelper = typeHelper;
    this.rttiHelper = rttiHelper;
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
      MethodReference actualMethodReference = baseType.getMethod(new MethodReference(methods[i], GenericTypeSpecialiser.IDENTITY_SPECIALISER).getDisambiguator());
      Method actualMethod = actualMethodReference.getReferencedMember();
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
    LLVMValueRef[] llvmEntries = new LLVMValueRef[methods.length];

    LLVMTypeRef stringType = typeHelper.findRawStringType();
    LLVMTypeRef rttiType = rttiHelper.getGenericPureRTTIType();
    LLVMTypeRef[] entrySubTypes = new LLVMTypeRef[] {stringType, rttiType};
    LLVMTypeRef entryType = LLVM.LLVMStructType(C.toNativePointerArray(entrySubTypes, false, true), entrySubTypes.length, false);

    for (int i = 0; i < methods.length; ++i)
    {
      String descriptorString = methods[i].getDescriptorString();
      LLVMValueRef stringConstant = codeGenerator.addStringConstant(descriptorString);
      LLVMValueRef convertedString = LLVM.LLVMConstBitCast(stringConstant, stringType);
      LLVMValueRef nullRTTIPointer = LLVM.LLVMConstNull(rttiType);
      LLVMValueRef[] entryValues = new LLVMValueRef[] {convertedString, nullRTTIPointer};
      llvmEntries[i] = LLVM.LLVMConstStruct(C.toNativePointerArray(entryValues, false, true), entryValues.length, false);
    }

    LLVMValueRef disambiguatorArray = LLVM.LLVMConstArray(entryType, C.toNativePointerArray(llvmEntries, false, true), llvmEntries.length);
    LLVMValueRef[] descriptorSubValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), llvmEntries.length, false),
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
    int numFunctions;
    if (typeDefinition instanceof ClassDefinition)
    {
      numFunctions = ((ClassDefinition) typeDefinition).getVirtualFunctions().length;
    }
    else if (typeDefinition instanceof InterfaceDefinition)
    {
      numFunctions = ((InterfaceDefinition) typeDefinition).getVirtualFunctions().length;
    }
    else
    {
      throw new IllegalArgumentException("Cannot get a VFT descriptor pointer - unknown TypeDefinition: " + typeDefinition);
    }
    LLVMValueRef result = LLVM.LLVMAddGlobal(module, getDescriptorType(numFunctions), mangledName);
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

    VirtualFunction[] virtualFunctions;
    if (typeDefinition instanceof ClassDefinition)
    {
      virtualFunctions = ((ClassDefinition) typeDefinition).getVirtualFunctions();
    }
    else if (typeDefinition instanceof InterfaceDefinition)
    {
      virtualFunctions = ((InterfaceDefinition) typeDefinition).getVirtualFunctions();
    }
    else
    {
      throw new IllegalStateException("Cannot generate a VFT for a type which doesn't have any virtual functions: " + typeDefinition);
    }

    LLVMValueRef[] llvmMethods = new LLVMValueRef[virtualFunctions.length];
    for (int i = 0; i < virtualFunctions.length; ++i)
    {
      if (virtualFunctions[i] instanceof MemberFunction)
      {
        MemberFunction memberFunction = (MemberFunction) virtualFunctions[i];
        switch (memberFunction.getMemberFunctionType())
        {
        case METHOD:
          if (memberFunction.getMethod().isAbstract())
          {
            llvmMethods[i] = LLVM.LLVMConstNull(LLVM.LLVMPointerType(typeHelper.findMethodType(memberFunction.getMethod()), 0));
          }
          else
          {
            llvmMethods[i] = codeGenerator.getMethodFunction(memberFunction.getMethod());
          }
          break;
        case PROPERTY_GETTER:
          if (memberFunction.getProperty().isAbstract())
          {
            llvmMethods[i] = LLVM.LLVMConstNull(LLVM.LLVMPointerType(typeHelper.findPropertyGetterType(memberFunction.getProperty()), 0));
          }
          else
          {
            llvmMethods[i] = codeGenerator.getPropertyGetterFunction(memberFunction.getProperty());
          }
          break;
        case PROPERTY_SETTER:
          if (memberFunction.getProperty().isAbstract())
          {
            llvmMethods[i] = LLVM.LLVMConstNull(LLVM.LLVMPointerType(typeHelper.findPropertySetterConstructorType(memberFunction.getProperty()), 0));
          }
          else
          {
            llvmMethods[i] = codeGenerator.getPropertySetterFunction(memberFunction.getProperty());
          }
          break;
        case PROPERTY_CONSTRUCTOR:
          if (memberFunction.getProperty().isAbstract())
          {
            llvmMethods[i] = LLVM.LLVMConstNull(LLVM.LLVMPointerType(typeHelper.findPropertySetterConstructorType(memberFunction.getProperty()), 0));
          }
          else
          {
            llvmMethods[i] = codeGenerator.getPropertyConstructorFunction(memberFunction.getProperty());
          }
          break;
        default:
          throw new IllegalStateException("Unknown member function type: " + memberFunction.getMemberFunctionType());
        }
      }
      else if (virtualFunctions[i] instanceof OverrideFunction)
      {
        OverrideFunction overrideFunction = (OverrideFunction) virtualFunctions[i];
        switch (overrideFunction.getMemberFunctionType())
        {
        case METHOD:
          llvmMethods[i] = buildOverrideProxyMethod(overrideFunction.getInheritedMethodReference(), overrideFunction.getImplementationMethodReference());
          break;
        case PROPERTY_GETTER:
          llvmMethods[i] = buildOverrideProxyPropertyGetter(overrideFunction.getInheritedPropertyReference(), overrideFunction.getImplementationPropertyReference());
          break;
        case PROPERTY_SETTER:
        case PROPERTY_CONSTRUCTOR:
          llvmMethods[i] = buildOverrideProxyPropertySetterConstructor(overrideFunction.getInheritedPropertyReference(), overrideFunction.getImplementationPropertyReference(), overrideFunction.getMemberFunctionType());
          break;
        default:
          throw new IllegalStateException("Unknown member function type in an override function: " + overrideFunction.getMemberFunctionType());
        }
      }
      else
      {
        throw new IllegalStateException("Unknown virtual function type: " + virtualFunctions[i]);
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

    LLVMTypeRef stringType = typeHelper.findRawStringType();
    LLVMTypeRef rttiType = rttiHelper.getGenericPureRTTIType();
    LLVMTypeRef[] entrySubTypes = new LLVMTypeRef[] {stringType, rttiType};
    LLVMTypeRef entryType = LLVM.LLVMStructType(C.toNativePointerArray(entrySubTypes, false, true), entrySubTypes.length, false);

    VirtualFunction[] virtualFunctions;
    if (typeDefinition instanceof ClassDefinition)
    {
      virtualFunctions = ((ClassDefinition) typeDefinition).getVirtualFunctions();
    }
    else if (typeDefinition instanceof InterfaceDefinition)
    {
      virtualFunctions = ((InterfaceDefinition) typeDefinition).getVirtualFunctions();
    }
    else
    {
      throw new IllegalStateException("Cannot generate a VFT descriptor for a type which doesn't have any virtual functions: " + typeDefinition);
    }

    LLVMValueRef[] llvmEntries = new LLVMValueRef[virtualFunctions.length];
    for (int i = 0; i < virtualFunctions.length; ++i)
    {
      String functionDescriptorString;
      NamedType overrideType;
      if (virtualFunctions[i] instanceof MemberFunction)
      {
        MemberFunction memberFunction = (MemberFunction) virtualFunctions[i];
        switch (memberFunction.getMemberFunctionType())
        {
        case METHOD:
          functionDescriptorString = memberFunction.getMethod().getDescriptorString();
          break;
        case PROPERTY_GETTER:
          functionDescriptorString = memberFunction.getProperty().getGetterDescriptor();
          break;
        case PROPERTY_SETTER:
          functionDescriptorString = memberFunction.getProperty().getSetterDescriptor();
          break;
        case PROPERTY_CONSTRUCTOR:
          functionDescriptorString = memberFunction.getProperty().getConstructorDescriptor();
          break;
        default:
          throw new IllegalStateException("Unknown member function type: " + memberFunction.getMemberFunctionType());
        }
        overrideType = null;
      }
      else if (virtualFunctions[i] instanceof OverrideFunction)
      {
        OverrideFunction overrideFunction = (OverrideFunction) virtualFunctions[i];
        switch (overrideFunction.getMemberFunctionType())
        {
        case METHOD:
          MethodReference inheritedMethod = overrideFunction.getInheritedMethodReference();
          functionDescriptorString = inheritedMethod.getReferencedMember().getDescriptorString();
          overrideType = inheritedMethod.getContainingType();
          break;
        case PROPERTY_GETTER:
          PropertyReference getterInheritedProperty = overrideFunction.getInheritedPropertyReference();
          functionDescriptorString = getterInheritedProperty.getReferencedMember().getGetterDescriptor();
          overrideType = getterInheritedProperty.getContainingType();
          break;
        case PROPERTY_SETTER:
          PropertyReference setterInheritedProperty = overrideFunction.getInheritedPropertyReference();
          functionDescriptorString = setterInheritedProperty.getReferencedMember().getSetterDescriptor();
          overrideType = setterInheritedProperty.getContainingType();
          break;
        case PROPERTY_CONSTRUCTOR:
          PropertyReference constructorInheritedProperty = overrideFunction.getInheritedPropertyReference();
          functionDescriptorString = constructorInheritedProperty.getReferencedMember().getConstructorDescriptor();
          overrideType = constructorInheritedProperty.getContainingType();
          break;
        default:
          throw new IllegalStateException("Unknown member function type in override function: " + overrideFunction.getMemberFunctionType());
        }
      }
      else
      {
        throw new IllegalStateException("Unknown type of virtual function: " + virtualFunctions[i]);
      }
      LLVMValueRef llvmDescriptorString = codeGenerator.addStringConstant(functionDescriptorString);
      llvmDescriptorString = LLVM.LLVMConstBitCast(llvmDescriptorString, stringType);
      LLVMValueRef overrideTypeRTTI;
      if (overrideType == null)
      {
        overrideTypeRTTI = LLVM.LLVMConstNull(rttiType);
      }
      else
      {
        overrideTypeRTTI = rttiHelper.getPureRTTI(overrideType);
      }
      LLVMValueRef[] entryValues = new LLVMValueRef[] {llvmDescriptorString, overrideTypeRTTI};
      llvmEntries[i] = LLVM.LLVMConstStruct(C.toNativePointerArray(entryValues, false, true), entryValues.length, false);
    }
    LLVMValueRef disambiguatorArray = LLVM.LLVMConstArray(entryType, C.toNativePointerArray(llvmEntries, false, true), llvmEntries.length);
    LLVMValueRef[] descriptorSubValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), llvmEntries.length, false),
                                                             disambiguatorArray};
    LLVMValueRef descriptorValue = LLVM.LLVMConstStruct(C.toNativePointerArray(descriptorSubValues, false, true), descriptorSubValues.length, false);
    LLVM.LLVMSetInitializer(vftDescriptorGlobalVar, descriptorValue);
  }

  /**
   * Builds a proxy method which can override the specified inherited method, and calls the specified implementation method.
   * Both the inherited and the implementation references must be specialised to the current TypeDefinition, which must be a ClassDefinition.
   * @param inheritedReference - the MethodReference being overridden, which this proxy function will conform to the signature of
   * @param implementationReference - the MethodReference of the implementation to call
   * @return the proxy function generated
   */
  private LLVMValueRef buildOverrideProxyMethod(MethodReference inheritedReference, MethodReference implementationReference)
  {
    if (inheritedReference.getReferencedMember().isStatic())
    {
      throw new IllegalArgumentException("Cannot generate an override proxy for a static method");
    }
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalArgumentException("Cannot generate an override proxy as part of any type but a class");
    }

    final String mangledName = PROXY_OVERRIDE_FUNCTION_PREFIX + "METHOD_" + typeDefinition.getQualifiedName().getMangledName() + "_" + inheritedReference.getReferencedMember().getDescriptorString() + "_" + implementationReference.getReferencedMember().getDescriptorString();
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    Method inheritedMethod = inheritedReference.getReferencedMember();
    LLVMTypeRef functionType = typeHelper.findMethodType(inheritedMethod);
    LLVMValueRef proxyFunction = LLVM.LLVMAddFunction(module, mangledName, functionType);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(proxyFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    // this method is definitely not static, so there is a 'this' value
    // so we can extract it and convert it to our class's type
    Type currentType = inheritedReference.getContainingType();
    if (currentType == null)
    {
      // the inherited member must be a built-in
      currentType = new ObjectType(false, false, null);
    }

    LLVMValueRef thisValue = LLVM.LLVMGetParam(proxyFunction, 0);

    // find the inherited TypeParameterAccessor
    int parameterOffset = 1;
    TypeDefinition inheritedDefinition = inheritedMethod.getContainingTypeDefinition();
    TypeParameterAccessor inheritedTypeAccessor;
    if (inheritedDefinition == null)
    {
      inheritedTypeAccessor = new TypeParameterAccessor(builder, rttiHelper);
    }
    else if (inheritedDefinition instanceof InterfaceDefinition)
    {
      Map<TypeParameter, LLVMValueRef> knownTypeParameters = new HashMap<TypeParameter, LLVMValueRef>();
      TypeParameter[] inheritedParameters = inheritedDefinition.getTypeParameters();
      for (int i = 0; i < inheritedParameters.length; ++i)
      {
        knownTypeParameters.put(inheritedParameters[i], LLVM.LLVMGetParam(proxyFunction, 1 + i));
      }
      inheritedTypeAccessor = new TypeParameterAccessor(builder, rttiHelper, inheritedDefinition, knownTypeParameters);
      // this proxy function is overridden from an interface, so we must add the number of type parameters to the parameter offset
      parameterOffset += inheritedParameters.length;
    }
    else
    {
      inheritedTypeAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, inheritedDefinition, thisValue);
    }

    NamedType thisType = new NamedType(false, false, false, typeDefinition);
    TypeParameterAccessor nullAccessor = new TypeParameterAccessor(builder, rttiHelper); // a TypeParameterAccessor which stores no type parameters, as we will never need them
    thisValue = typeHelper.convertTemporary(builder, landingPadContainer, thisValue, currentType, thisType, true, inheritedTypeAccessor, nullAccessor);

    TypeParameterAccessor concreteTypeAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, typeDefinition, thisValue);

    Parameter[] inheritedParameters = inheritedMethod.getParameters();
    Type[] implementationParameterTypes = implementationReference.getParameterTypes();
    LLVMValueRef[] arguments = new LLVMValueRef[implementationParameterTypes.length];
    for (int i = 0; i < implementationParameterTypes.length; ++i)
    {
      // convert from this inherited parameter's type to the implementation reference's type
      LLVMValueRef parameter = LLVM.LLVMGetParam(proxyFunction, parameterOffset + i);
      arguments[i] = typeHelper.convertStandardToTemporary(builder, landingPadContainer, parameter, inheritedParameters[i].getType(), implementationParameterTypes[i], inheritedTypeAccessor, concreteTypeAccessor);
    }

    LLVMValueRef result = typeHelper.buildMethodCall(builder, landingPadContainer, thisValue, thisType, implementationReference, arguments, concreteTypeAccessor);

    Type returnType = inheritedMethod.getReturnType();
    if (returnType instanceof VoidType)
    {
      LLVM.LLVMBuildRetVoid(builder);
    }
    else
    {
      // convert the return value to the inherited method's return type
      result = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, result, implementationReference.getReturnType(), returnType, concreteTypeAccessor, inheritedTypeAccessor);
      LLVM.LLVMBuildRet(builder, result);
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return proxyFunction;
  }

  /**
   * Builds a proxy property getter which can override the specified inherited property's getter, and calls the specified implementation property's getter.
   * Both the inherited and the implementation references must be specialised to the current TypeDefinition, which must be a ClassDefinition.
   * @param inheritedReference - the PropertyReference being overridden, which this proxy function will conform to the signature of
   * @param implementationReference - the PropertyReference of the implementation to call the getter of
   * @return the proxy function generated
   */
  private LLVMValueRef buildOverrideProxyPropertyGetter(PropertyReference inheritedReference, PropertyReference implementationReference)
  {
    if (inheritedReference.getReferencedMember().isStatic())
    {
      throw new IllegalArgumentException("Cannot generate an override getter proxy for a static property");
    }
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalArgumentException("Cannot generate an override proxy as part of any type but a class");
    }

    final String mangledName = PROXY_OVERRIDE_FUNCTION_PREFIX + "PROPERTY_GETTER_" + typeDefinition.getQualifiedName().getMangledName() + "_" + inheritedReference.getReferencedMember().getGetterDescriptor() + "_" + implementationReference.getReferencedMember().getGetterDescriptor();
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    Property inheritedProperty = inheritedReference.getReferencedMember();
    LLVMTypeRef functionType = typeHelper.findPropertyGetterType(inheritedProperty);
    LLVMValueRef proxyFunction = LLVM.LLVMAddFunction(module, mangledName, functionType);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(proxyFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    // this method is definitely not static, so there is a 'this' value
    // so we can extract it and convert it to our class's type
    Type currentType = inheritedReference.getContainingType();
    if (currentType == null)
    {
      // the inherited member must be a built-in
      currentType = new ObjectType(false, false, null);
    }

    LLVMValueRef thisValue = LLVM.LLVMGetParam(proxyFunction, 0);

    // find the inherited TypeParameterAccessor
    TypeDefinition inheritedDefinition = inheritedProperty.getContainingTypeDefinition();
    TypeParameterAccessor inheritedTypeAccessor;
    if (inheritedDefinition == null)
    {
      inheritedTypeAccessor = new TypeParameterAccessor(builder, rttiHelper);
    }
    else if (inheritedDefinition instanceof InterfaceDefinition)
    {
      Map<TypeParameter, LLVMValueRef> knownTypeParameters = new HashMap<TypeParameter, LLVMValueRef>();
      TypeParameter[] inheritedParameters = inheritedDefinition.getTypeParameters();
      for (int i = 0; i < inheritedParameters.length; ++i)
      {
        knownTypeParameters.put(inheritedParameters[i], LLVM.LLVMGetParam(proxyFunction, 1 + i));
      }
      inheritedTypeAccessor = new TypeParameterAccessor(builder, rttiHelper, inheritedDefinition, knownTypeParameters);
    }
    else
    {
      inheritedTypeAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, inheritedDefinition, thisValue);
    }

    NamedType thisType = new NamedType(false, false, false, typeDefinition);
    TypeParameterAccessor nullAccessor = new TypeParameterAccessor(builder, rttiHelper); // a TypeParameterAccessor which stores no type parameters, as we will never need them
    thisValue = typeHelper.convertTemporary(builder, landingPadContainer, thisValue, currentType, thisType, true, inheritedTypeAccessor, nullAccessor);

    TypeParameterAccessor concreteTypeAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, typeDefinition, thisValue);

    LLVMValueRef result = typeHelper.buildPropertyGetterFunctionCall(builder, landingPadContainer, thisValue, thisType, implementationReference, concreteTypeAccessor);

    // convert the return value to the specialised type
    result = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, result, implementationReference.getType(), inheritedProperty.getType(), concreteTypeAccessor, inheritedTypeAccessor);
    LLVM.LLVMBuildRet(builder, result);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return proxyFunction;
  }

  /**
   * Builds a proxy property setter/constructor which can override the specified inherited property's setter/constructor, and calls the specified implementation property's setter/constructor.
   * Both the inherited and the implementation references must be specialised to the current TypeDefinition, which must be a ClassDefinition.
   * @param inheritedReference - the PropertyReference being overridden, which this proxy function will conform to the signature of
   * @param implementationReference - the PropertyReference of the implementation to call the getter of
   * @param memberFunctionType - the type of member function to override, this should be either PROPERTY_SETTER or PROPERTY_CONSTRUCTOR
   * @return the proxy function generated
   */
  private LLVMValueRef buildOverrideProxyPropertySetterConstructor(PropertyReference inheritedReference, PropertyReference implementationReference, MemberFunctionType memberFunctionType)
  {
    if (inheritedReference.getReferencedMember().isStatic())
    {
      throw new IllegalArgumentException("Cannot generate an override setter/constructor proxy for a static property");
    }
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalArgumentException("Cannot generate an override proxy as part of any type but a class");
    }

    Property inheritedProperty = inheritedReference.getReferencedMember();

    String functionTypeString = memberFunctionType == MemberFunctionType.PROPERTY_CONSTRUCTOR ? "PROPERTY_CONSTRUCTOR_" : "PROPERTY_SETTER_";
    String inheritedDescriptor = memberFunctionType == MemberFunctionType.PROPERTY_CONSTRUCTOR ? inheritedProperty.getConstructorDescriptor() : inheritedProperty.getSetterDescriptor();
    String implementationDescriptor = memberFunctionType == MemberFunctionType.PROPERTY_CONSTRUCTOR ? implementationReference.getReferencedMember().getConstructorDescriptor() : implementationReference.getReferencedMember().getSetterDescriptor();
    final String mangledName = PROXY_OVERRIDE_FUNCTION_PREFIX + functionTypeString + typeDefinition.getQualifiedName().getMangledName() + "_" + inheritedDescriptor + "_" + implementationDescriptor;
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunction != null)
    {
      return existingFunction;
    }

    LLVMTypeRef functionType = typeHelper.findPropertySetterConstructorType(inheritedProperty);
    LLVMValueRef proxyFunction = LLVM.LLVMAddFunction(module, mangledName, functionType);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(proxyFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    // this method is definitely not static, so there is a 'this' value
    // so we can extract it and convert it to our class's type
    Type currentType = inheritedReference.getContainingType();
    if (currentType == null)
    {
      // the inherited member must be a built-in
      currentType = new ObjectType(false, false, null);
    }
    LLVMValueRef thisValue = LLVM.LLVMGetParam(proxyFunction, 0);

    // find the inherited TypeParameterAccessor
    int parameterOffset = 1;
    TypeDefinition inheritedDefinition = inheritedProperty.getContainingTypeDefinition();
    TypeParameterAccessor inheritedTypeAccessor;
    if (inheritedDefinition == null)
    {
      inheritedTypeAccessor = new TypeParameterAccessor(builder, rttiHelper);
    }
    else if (inheritedDefinition instanceof InterfaceDefinition)
    {
      Map<TypeParameter, LLVMValueRef> knownTypeParameters = new HashMap<TypeParameter, LLVMValueRef>();
      TypeParameter[] inheritedParameters = inheritedDefinition.getTypeParameters();
      for (int i = 0; i < inheritedParameters.length; ++i)
      {
        knownTypeParameters.put(inheritedParameters[i], LLVM.LLVMGetParam(proxyFunction, 1 + i));
      }
      inheritedTypeAccessor = new TypeParameterAccessor(builder, rttiHelper, inheritedDefinition, knownTypeParameters);
      // this proxy function is overridden from an interface, so we must add the number of type parameters to the parameter offset
      parameterOffset += inheritedParameters.length;
    }
    else
    {
      inheritedTypeAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, inheritedDefinition, thisValue);
    }

    NamedType thisType = new NamedType(false, false, false, typeDefinition);
    TypeParameterAccessor nullAccessor = new TypeParameterAccessor(builder, rttiHelper); // a TypeParameterAccessor which stores no type parameters, as we will never need them
    thisValue = typeHelper.convertTemporary(builder, landingPadContainer, thisValue, currentType, thisType, true, inheritedTypeAccessor, nullAccessor);

    TypeParameterAccessor concreteTypeAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, typeDefinition, thisValue);

    // convert from the parameter's type to the reference's type
    LLVMValueRef parameter = LLVM.LLVMGetParam(proxyFunction, parameterOffset);
    LLVMValueRef argument = typeHelper.convertStandardToTemporary(builder, landingPadContainer, parameter, inheritedProperty.getType(), implementationReference.getType(), inheritedTypeAccessor, concreteTypeAccessor);

    typeHelper.buildPropertySetterConstructorFunctionCall(builder, landingPadContainer, thisValue, thisType, argument, implementationReference, memberFunctionType, concreteTypeAccessor);

    LLVM.LLVMBuildRetVoid(builder);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), codeGenerator.getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);

    return proxyFunction;
  }

  /**
   * Gets the type search list for the specified class definition. This method assumes that the specified type definition is a class definition.
   * @param typeDefinition - the TypeDefinition to get the type search list global for
   * @return the type search list for the current class definition
   */
  public LLVMValueRef getTypeSearchList(TypeDefinition typeDefinition)
  {
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalArgumentException("Cannot get a type search list for a non-class type");
    }
    String mangledName = TYPE_SEARCH_LIST_PREFIX + typeDefinition.getQualifiedName().getMangledName();
    LLVMValueRef existingValue = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingValue != null)
    {
      return existingValue;
    }

    NamedType[] inheritanceLinearisation = typeDefinition.getInheritanceLinearisation();
    LLVMValueRef[] elements = new LLVMValueRef[inheritanceLinearisation.length + 1];

    LLVMTypeRef rttiType = rttiHelper.getGenericPureRTTIType();
    LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(getGenericVFTType(), 0);
    LLVMTypeRef[] elementSubTypes = new LLVMTypeRef[] {rttiType, vftPointerType};
    LLVMTypeRef elementType = LLVM.LLVMStructType(C.toNativePointerArray(elementSubTypes, false, true), elementSubTypes.length, false);
    LLVMTypeRef arrayType = LLVM.LLVMArrayType(elementType, elements.length);
    LLVMTypeRef[] structSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), arrayType};
    LLVMTypeRef structType = LLVM.LLVMStructType(C.toNativePointerArray(structSubTypes, false, true), structSubTypes.length, false);

    LLVMValueRef global = LLVM.LLVMAddGlobal(module, structType, mangledName);
    LLVM.LLVMSetVisibility(global, LLVM.LLVMVisibility.LLVMProtectedVisibility);

    // only give it a definition if it is for the current type definition
    if (typeDefinition == this.typeDefinition)
    {
      for (int i = 0; i < inheritanceLinearisation.length; ++i)
      {
        LLVMValueRef rttiValue = rttiHelper.getPureRTTI(inheritanceLinearisation[i]);
        LLVMValueRef vftValue;
        if (inheritanceLinearisation[i].getResolvedTypeDefinition() == typeDefinition)
        {
          vftValue = getVFTGlobal(typeDefinition);
          vftValue = LLVM.LLVMConstBitCast(vftValue, vftPointerType);
        }
        else
        {
          // leave it as null, to be filled in at runtime before anything else is executed
          vftValue = LLVM.LLVMConstNull(vftPointerType);
        }
        LLVMValueRef[] elementSubValues = new LLVMValueRef[] {rttiValue, vftValue};
        elements[i] = LLVM.LLVMConstStruct(C.toNativePointerArray(elementSubValues, false, true), elementSubValues.length, false);
      }
      LLVMValueRef objectRTTIValue = rttiHelper.getPureRTTI(new ObjectType(false, false, null));
      LLVMValueRef nullVFTValue = LLVM.LLVMConstNull(vftPointerType);
      LLVMValueRef[] elementSubValues = new LLVMValueRef[] {objectRTTIValue, nullVFTValue};
      elements[inheritanceLinearisation.length] = LLVM.LLVMConstStruct(C.toNativePointerArray(elementSubValues, false, true), elementSubValues.length, false);

      LLVMValueRef array = LLVM.LLVMConstArray(elementType, C.toNativePointerArray(elements, false, true), elements.length);

      LLVMValueRef lengthValue = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), elements.length, false);
      LLVMValueRef[] structSubValues = new LLVMValueRef[] {lengthValue, array};
      LLVMValueRef struct = LLVM.LLVMConstStruct(C.toNativePointerArray(structSubValues, false, true), structSubValues.length, false);

      LLVM.LLVMSetInitializer(global, struct);
    }
    return global;
  }

  /**
   * @param type - the Type to get the type search list for
   * @param objectVFT - the object VFT for the specified Type, to store in the created search list
   * @return the type search list for the object type with the specified object VFT
   */
  public LLVMValueRef getObjectTypeSearchList(Type type, LLVMValueRef objectVFT)
  {
    String mangledName = TYPE_SEARCH_LIST_PREFIX + type.getMangledName();
    LLVMValueRef existingValue = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingValue != null)
    {
      return existingValue;
    }

    LLVMTypeRef rttiType = rttiHelper.getGenericPureRTTIType();
    LLVMTypeRef vftPointerType = LLVM.LLVMPointerType(getGenericVFTType(), 0);
    LLVMTypeRef[] elementSubTypes = new LLVMTypeRef[] {rttiType, vftPointerType};
    LLVMTypeRef elementType = LLVM.LLVMStructType(C.toNativePointerArray(elementSubTypes, false, true), elementSubTypes.length, false);

    LLVMValueRef[] elements = new LLVMValueRef[1];

    LLVMValueRef objectRTTIValue = rttiHelper.getPureRTTI(new ObjectType(false, false, null));
    LLVMValueRef nullVFTValue = LLVM.LLVMConstBitCast(objectVFT, vftPointerType);
    LLVMValueRef[] elementSubValues = new LLVMValueRef[] {objectRTTIValue, nullVFTValue};
    elements[0] = LLVM.LLVMConstStruct(C.toNativePointerArray(elementSubValues, false, true), elementSubValues.length, false);

    LLVMValueRef array = LLVM.LLVMConstArray(elementType, C.toNativePointerArray(elements, false, true), elements.length);
    LLVMValueRef lengthValue = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), elements.length, false);

    LLVMTypeRef arrayType = LLVM.LLVMArrayType(elementType, elements.length);
    LLVMTypeRef[] structSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), arrayType};
    LLVMTypeRef structType = LLVM.LLVMStructType(C.toNativePointerArray(structSubTypes, false, true), structSubTypes.length, false);

    LLVMValueRef[] structSubValues = new LLVMValueRef[] {lengthValue, array};
    LLVMValueRef struct = LLVM.LLVMConstNamedStruct(structType, C.toNativePointerArray(structSubValues, false, true), structSubValues.length);

    LLVMValueRef global = LLVM.LLVMAddGlobal(module, structType, mangledName);
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMLinkOnceAnyLinkage);
    LLVM.LLVMSetVisibility(global, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    LLVM.LLVMSetInitializer(global, struct);
    return global;
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
   * The base type should be a class, and should never be nullable.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param baseValue - the base value to find the virtual function table pointer inside
   * @param baseType - the type of baseValue, which we should find the virtual function table inside
   * @param searchType - the NamedType to find the virtual function table of.
   *                     This must resolve to a ClassDefinition or an InterfaceDefinition,
   *                     and must be a super-type of (or equivalent to) baseType,
   *                     with any type arguments specialised to the level of baseType.
   * @return a pointer to the virtual function table pointer inside the specified base value
   */
  public LLVMValueRef getVirtualFunctionTablePointer(LLVMBuilderRef builder, LLVMValueRef baseValue, NamedType baseType, NamedType searchType)
  {
    if (!(baseType.getResolvedTypeDefinition() instanceof ClassDefinition))
    {
      throw new IllegalArgumentException("Cannot find a VFT pointer on something of type: " + baseType);
    }

    ClassDefinition encapsulatingClassDefinition = null;
    // maintain a stack of specialisers that allow us to work our way down the type hierarchy to the subclassDefinition
    Deque<GenericTypeSpecialiser> specialisers = new LinkedList<GenericTypeSpecialiser>();
    if (searchType.getResolvedTypeDefinition() instanceof ClassDefinition)
    {
      encapsulatingClassDefinition = (ClassDefinition) searchType.getResolvedTypeDefinition();
    }
    else
    {
      // we are searching for an interface, so find the highest up class in baseType's linearisation which implements this interface
      ClassDefinition current = (ClassDefinition) baseType.getResolvedTypeDefinition();
      specialisers.addFirst(new GenericTypeSpecialiser(baseType));
      while (current != null)
      {
        boolean inLinearisation = false;
        for (NamedType t : current.getInheritanceLinearisation())
        {
          NamedType specialised = t;
          for (GenericTypeSpecialiser specialiser : specialisers)
          {
            // casting to NamedType is safe here, as the type was part of an inheritance linearisation, so it cannot be a type parameter
            specialised = (NamedType) specialiser.getSpecialisedType(specialised);
          }
          if (specialised.isRuntimeEquivalent(searchType))
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
        NamedType superType = current.getSuperType();
        if (superType != null && superType.getResolvedTypeDefinition() instanceof ClassDefinition)
        {
          current = (ClassDefinition) superType.getResolvedTypeDefinition();
          specialisers.addFirst(new GenericTypeSpecialiser(superType));
        }
        else
        {
          current = null;
        }
      }
      if (encapsulatingClassDefinition == null)
      {
        throw new IllegalArgumentException("Cannot find a VFT pointer for " + searchType + " inside " + baseType);
      }
    }
    // calculate the VFT's index
    // start at 2 to skip the RTTI and the object VFT
    int index = 2;
    NamedType superType = encapsulatingClassDefinition.getSuperType();
    while (superType != null && superType.getResolvedTypeDefinition() instanceof ClassDefinition)
    {
      ClassDefinition superClassDefinition = (ClassDefinition) superType.getResolvedTypeDefinition();
      index += 1 + typeHelper.findSubClassInterfaces(superClassDefinition).length + superClassDefinition.getTypeParameters().length + superClassDefinition.getMemberVariables().length;
      superType = superClassDefinition.getSuperType();
    }
    if (searchType.getResolvedTypeDefinition() instanceof InterfaceDefinition)
    {
      // add on the offset to the interface's VFT
      NamedType[] subClassInterfaces = typeHelper.findSubClassInterfaces(encapsulatingClassDefinition);
      for (int i = 0; i < subClassInterfaces.length; ++i)
      {
        NamedType specialisedSubClassInterface = subClassInterfaces[i];
        for (GenericTypeSpecialiser specialiser : specialisers)
        {
          // casting to NamedType is safe here, as specialisedSubClassInterface is always an interface and never a TypeParameter
          specialisedSubClassInterface = (NamedType) specialiser.getSpecialisedType(specialisedSubClassInterface);
        }
        if (specialisedSubClassInterface.isRuntimeEquivalent(searchType))
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
   * Finds a virtual function table pointer inside the specified baseValue, which corresponds to the specified searchType.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param baseValue - the base value to find the virtual function table inside
   * @param baseType - the type of baseValue, which we should find the virtual function table inside
   * @param searchType - the NamedType to find the virtual function table of.
   *                     This must resolve to a ClassDefinition or an InterfaceDefinition,
   *                     and must be a super-type of (or equivalent to) baseType,
   *                     with any type arguments specialised to the level of baseType.
   * @param baseAccessor - the TypeParameterAccessor that contains the RTTI for any type parameters that might be in baseType
   * @param searchAccessor - the TypeParameterAccessor that contains the RTTI for any type parameters that might be in searchType
   * @return the requested virtual function table pointer inside the specified baseValue
   */
  public LLVMValueRef getVirtualFunctionTable(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef baseValue, NamedType baseType, NamedType searchType, TypeParameterAccessor baseAccessor, TypeParameterAccessor searchAccessor)
  {
    if (baseType.getResolvedTypeParameter() != null)
    {
      LLVMValueRef convertedBaseValue = typeHelper.convertTemporary(builder, landingPadContainer, baseValue, baseType, searchType, false, baseAccessor, searchAccessor);
      // recurse to get the VFT for whichever sort of type searchType is
      return getVirtualFunctionTable(builder, landingPadContainer, convertedBaseValue, searchType, searchType, searchAccessor, searchAccessor);
    }

    if (baseType.getResolvedTypeDefinition() instanceof InterfaceDefinition)
    {
      if (!(searchType.getResolvedTypeDefinition() instanceof InterfaceDefinition))
      {
        throw new IllegalArgumentException("Cannot find the VFT pointer for " + searchType + " on " + baseType + ", searchType should be an interface in order to be a super-type of baseType");
      }
      LLVMValueRef convertedBaseValue = typeHelper.convertTemporary(builder, landingPadContainer, baseValue, baseType, searchType, false, baseAccessor, searchAccessor);
      // extract the VFT from the interface's type representation
      return LLVM.LLVMBuildExtractValue(builder, convertedBaseValue, 0, "");
    }

    // we are looking up a VFT on a class/object base type, so find the pointer to it inside this object and load the result
    LLVMValueRef vftPointer = getVirtualFunctionTablePointer(builder, baseValue, baseType, searchType);
    return LLVM.LLVMBuildLoad(builder, vftPointer, "");
  }

  /**
   * Finds the pointer to the specified Method inside the specified base value
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param baseValue - the base value to look up the method in one of the virtual function tables of
   * @param baseType - the Type of the base value
   * @param methodReference - the MethodReference to look up in a virtual function table, which has been specialised to baseType
   * @param baseAccessor - the TypeParameterAccessor that contains the RTTI for any type parameters accessible inside baseType
   * @return a pointer to the native function representing the specified method
   */
  public LLVMValueRef getMethodPointer(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef baseValue, Type baseType, MethodReference methodReference, TypeParameterAccessor baseAccessor)
  {
    if (methodReference.getReferencedMember().isStatic())
    {
      throw new IllegalArgumentException("Cannot get a method pointer for a static method");
    }
    LLVMValueRef vft = null;
    NamedType methodContainingType = methodReference.getContainingType();
    if (methodContainingType != null)
    {
      if (!(baseType instanceof NamedType))
      {
        throw new IllegalArgumentException("Cannot get a method pointer for non-built-in method on anything other than a NamedType");
      }
      vft = getVirtualFunctionTable(builder, landingPadContainer, baseValue, (NamedType) baseType, methodContainingType, baseAccessor, baseAccessor);
    }
    else if (methodReference.getReferencedMember() instanceof BuiltinMethod)
    {
      if (baseType instanceof NamedType && ((NamedType) baseType).getResolvedTypeDefinition() instanceof InterfaceDefinition)
      {
        ObjectType objectType = new ObjectType(false, false, null);
        // objectType doesn't have any type parameters, so use null for its TypeParameterAccessor
        baseValue = typeHelper.convertTemporary(builder, landingPadContainer, baseValue, baseType, objectType, false, baseAccessor, null);
        baseType = objectType;
      }
      if (baseType instanceof ObjectType ||
          (baseType instanceof NamedType && ((NamedType) baseType).getResolvedTypeParameter() != null) ||
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
    int index = methodReference.getReferencedMember().getMemberFunction().getIndex();
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), index, false)};
    LLVMValueRef vftElement = LLVM.LLVMBuildGEP(builder, vft, C.toNativePointerArray(indices, false, true), indices.length, "");
    return LLVM.LLVMBuildLoad(builder, vftElement, "");
  }

  /**
   * Finds the pointer to the specified property MemberFunction inside the specified base value.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param baseValue - the base value to look up the property function in one of the virtual function tables of
   * @param baseType - the Type of the base value
   * @param propertyReference - the PropertyReference which has been specialised to baseType
   * @param propertyFunction - the function to look up in a virtual function table
   * @param baseAccessor - the TypeParameterAccessor that contains the RTTI for any type parameters inside baseType
   * @return a pointer to the native function representing the specified property function
   */
  public LLVMValueRef getPropertyFuntionPointer(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef baseValue, Type baseType, PropertyReference propertyReference, MemberFunction propertyFunction, TypeParameterAccessor baseAccessor)
  {
    if (!(baseType instanceof NamedType))
    {
      throw new IllegalArgumentException("Cannot get a property function pointer for a property on anything other than a NamedType");
    }
    LLVMValueRef vft;
    NamedType propertyContainingType = propertyReference.getContainingType();
    if (propertyContainingType != null)
    {
      vft = getVirtualFunctionTable(builder, landingPadContainer, baseValue, (NamedType) baseType, propertyContainingType, baseAccessor, baseAccessor);
    }
    else
    {
      throw new IllegalArgumentException("Cannot get a property function pointer for a property from anything but a ClassDefinition or an InterfaceDefinition (accessing property: '" + propertyReference.getReferencedMember().getName() + "')");
    }
    int index = propertyFunction.getIndex();
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
    LLVMTypeRef stringType = typeHelper.findRawStringType();
    LLVMTypeRef rttiType = rttiHelper.getGenericPureRTTIType();
    LLVMTypeRef[] entrySubTypes = new LLVMTypeRef[] {stringType, rttiType};
    LLVMTypeRef entryType = LLVM.LLVMStructType(C.toNativePointerArray(entrySubTypes, false, true), entrySubTypes.length, false);

    LLVMTypeRef arrayType = LLVM.LLVMArrayType(entryType, numMethods);
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
    LLVMTypeRef stringType = typeHelper.findRawStringType();
    LLVMTypeRef rttiType = rttiHelper.getGenericPureRTTIType();
    LLVMTypeRef[] entrySubTypes = new LLVMTypeRef[] {stringType, rttiType};
    LLVMTypeRef entryType = LLVM.LLVMStructType(C.toNativePointerArray(entrySubTypes, false, true), entrySubTypes.length, false);

    LLVMTypeRef arrayType = LLVM.LLVMArrayType(entryType, 0);
    LLVMTypeRef[] vftDescriptorSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), arrayType};
    vftDescriptorType = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), "VFT_Descriptor");
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
    LLVMTypeRef result = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), typeDefinition.getQualifiedName().toString() + "_VFT");
    // cache the LLVM type before we call findMethodType(), so that once we call it, everything will be able to use this type instead of recreating it and possibly recursing infinitely
    // later on, we add the fields using LLVMStructSetBody
    nativeVirtualTableTypes.put(typeDefinition, result);

    VirtualFunction[] virtualFunctions;
    if (typeDefinition instanceof ClassDefinition)
    {
      virtualFunctions = ((ClassDefinition) typeDefinition).getVirtualFunctions();
    }
    else if (typeDefinition instanceof InterfaceDefinition)
    {
      virtualFunctions = ((InterfaceDefinition) typeDefinition).getVirtualFunctions();
    }
    else
    {
      throw new IllegalArgumentException("Cannot find the VFT type for a type without a VFT: " + typeDefinition);
    }

    LLVMTypeRef[] functionTypes = new LLVMTypeRef[virtualFunctions.length];
    for (int i = 0; i < virtualFunctions.length; ++i)
    {
      if (virtualFunctions[i] instanceof MemberFunction)
      {
        MemberFunction memberFunction = (MemberFunction) virtualFunctions[i];
        switch (memberFunction.getMemberFunctionType())
        {
        case METHOD:
          functionTypes[i] = LLVM.LLVMPointerType(typeHelper.findMethodType(memberFunction.getMethod()), 0);
          break;
        case PROPERTY_GETTER:
          functionTypes[i] = LLVM.LLVMPointerType(typeHelper.findPropertyGetterType(memberFunction.getProperty()), 0);
          break;
        case PROPERTY_SETTER:
        case PROPERTY_CONSTRUCTOR:
          functionTypes[i] = LLVM.LLVMPointerType(typeHelper.findPropertySetterConstructorType(memberFunction.getProperty()), 0);
          break;
        default:
          throw new IllegalStateException("Unknown member function type: " + memberFunction.getMemberFunctionType());
        }
      }
      else if (virtualFunctions[i] instanceof OverrideFunction)
      {
        OverrideFunction overrideFunction = (OverrideFunction) virtualFunctions[i];
        switch (overrideFunction.getMemberFunctionType())
        {
        case METHOD:
          Method inheritedMethod = overrideFunction.getInheritedMethodReference().getReferencedMember();
          functionTypes[i] = LLVM.LLVMPointerType(typeHelper.findMethodType(inheritedMethod), 0);
          break;
        case PROPERTY_GETTER:
          Property inheritedGetterProperty = overrideFunction.getInheritedPropertyReference().getReferencedMember();
          functionTypes[i] = LLVM.LLVMPointerType(typeHelper.findPropertyGetterType(inheritedGetterProperty), 0);
          break;
        case PROPERTY_SETTER:
        case PROPERTY_CONSTRUCTOR:
          Property inheritedSetterConstructorProperty = overrideFunction.getInheritedPropertyReference().getReferencedMember();
          functionTypes[i] = LLVM.LLVMPointerType(typeHelper.findPropertySetterConstructorType(inheritedSetterConstructorProperty), 0);
          break;
        default:
          throw new IllegalStateException("Unknown member function type in override function: " + overrideFunction.getMemberFunctionType());
        }
      }
      else
      {
        throw new IllegalArgumentException("Unknown virtual function type: " + virtualFunctions[i]);
      }
    }
    LLVM.LLVMStructSetBody(result, C.toNativePointerArray(functionTypes, false, true), functionTypes.length, false);
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
    LLVMTypeRef result = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), ObjectType.MANGLED_NAME + "_VFT");
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
  public LLVMTypeRef getGenericVFTType()
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
   * @param numElements - the number of elements in the exclude list, or 0 for a generic exclude list type
   * @return the type of an exclude list in a function search list
   */
  private LLVMTypeRef getExcludeListType(int numElements)
  {
    return LLVM.LLVMArrayType(LLVM.LLVMInt1Type(), numElements);
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
    LLVMTypeRef rttiPointer = rttiHelper.getGenericPureRTTIType();
    LLVMTypeRef descriptorPointer = LLVM.LLVMPointerType(getGenericDescriptorType(), 0);
    LLVMTypeRef vftPointer = LLVM.LLVMPointerType(getGenericVFTType(), 0);
    LLVMTypeRef excludeListPointer = LLVM.LLVMPointerType(getExcludeListType(0), 0);
    LLVMTypeRef[] elementSubTypes = new LLVMTypeRef[] {rttiPointer, descriptorPointer, vftPointer, excludeListPointer};
    LLVMTypeRef elementType = LLVM.LLVMStructType(C.toNativePointerArray(elementSubTypes, false, true), elementSubTypes.length, false);
    LLVMTypeRef arrayType = LLVM.LLVMArrayType(elementType, 0);
    LLVMTypeRef[] searchListSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), arrayType};
    functionSearchListType = LLVM.LLVMStructCreateNamed(codeGenerator.getContext(), "FunctionSearchList");
    LLVM.LLVMStructSetBody(functionSearchListType, C.toNativePointerArray(searchListSubTypes, false, true), searchListSubTypes.length, false);
    return functionSearchListType;
  }

  /**
   * @return the superclass VFT generator function (see the core runtime's 'vft.ll' file)
   */
  private LLVMValueRef getSuperTypeVFTGeneratorFunction()
  {
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, SUPERTYPE_VFT_GENERATOR_FUNCTION_NAME);
    if (existingFunction != null)
    {
      return existingFunction;
    }
    LLVMTypeRef[] parameterTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(getFunctionSearchListType(), 0),
                                                      LLVM.LLVMInt32Type()};
    LLVMTypeRef resultType = LLVM.LLVMPointerType(getGenericVFTType(), 0);
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(parameterTypes, false, true), parameterTypes.length, false);
    LLVMValueRef function = LLVM.LLVMAddFunction(module, SUPERTYPE_VFT_GENERATOR_FUNCTION_NAME, functionType);
    return function;
  }

  /**
   * Finds the exclude list for the specified super-type of the current type definition.
   * @param superType - the super-type to find the exclude list for, or null to find the exclude list for the object type
   * @return the exclude list for the specified super-type of the current type definition
   */
  private LLVMValueRef getExcludeList(NamedType superType)
  {
    String mangledName = "ExcludeList_" + typeDefinition.getQualifiedName().getMangledName() + "_" + (superType == null ? ObjectType.MANGLED_NAME : superType.getMangledName());
    LLVMValueRef existingGlobal = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingGlobal != null)
    {
      return existingGlobal;
    }

    GenericTypeSpecialiser superTypeSpecialiser = superType == null ? GenericTypeSpecialiser.IDENTITY_SPECIALISER : new GenericTypeSpecialiser(superType);

    NamedType[] linearisation = typeDefinition.getInheritanceLinearisation();
    // build the exclude list's values, which are only true for super-types of the given superType
    LLVMValueRef[] excludeListValues = new LLVMValueRef[linearisation.length + 1];
    for (int i = 0; i < linearisation.length; ++i)
    {
      boolean inSuperType = false;
      if (superType != null)
      {
        // if this type is a super-type of superType, then exclude it
        for (NamedType test : superType.getResolvedTypeDefinition().getInheritanceLinearisation())
        {
          if (superTypeSpecialiser.getSpecialisedType(test).isRuntimeEquivalent(linearisation[i]))
          {
            inSuperType = true;
            break;
          }
        }
      }
      excludeListValues[i] = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), inSuperType ? 1 : 0, false);
    }
    // the last value is for the object type, which everything extends, and is therefore always in the exclude list
    excludeListValues[linearisation.length] = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
    LLVMValueRef array = LLVM.LLVMConstArray(LLVM.LLVMInt1Type(), C.toNativePointerArray(excludeListValues, false, true), excludeListValues.length);

    LLVMValueRef global = LLVM.LLVMAddGlobal(module, getExcludeListType(linearisation.length + 1), mangledName);
    LLVM.LLVMSetGlobalConstant(global, true);
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMPrivateLinkage);
    LLVM.LLVMSetVisibility(global, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    LLVM.LLVMSetInitializer(global, array);
    return global;
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

    NamedType[] searchTypes = typeDefinition.getInheritanceLinearisation();

    LLVMValueRef[] searchRTTIs = new LLVMValueRef[searchTypes.length + 1];
    LLVMValueRef[] searchDescriptors = new LLVMValueRef[searchTypes.length + 1];
    LLVMValueRef[] searchVFTs = new LLVMValueRef[searchTypes.length + 1];
    LLVMValueRef[] searchExcludeLists = new LLVMValueRef[searchTypes.length + 1];
    for (int i = 0; i < searchTypes.length; ++i)
    {
      searchRTTIs[i] = rttiHelper.getPureRTTI(searchTypes[i]);
      LLVMValueRef descriptor = getVFTDescriptorPointer(searchTypes[i].getResolvedTypeDefinition());
      searchDescriptors[i] = LLVM.LLVMConstBitCast(descriptor, LLVM.LLVMPointerType(getGenericDescriptorType(), 0));
      LLVMValueRef vft = getVFTGlobal(searchTypes[i].getResolvedTypeDefinition());
      searchVFTs[i] = LLVM.LLVMConstBitCast(vft, LLVM.LLVMPointerType(getGenericVFTType(), 0));
      LLVMValueRef excludeList = getExcludeList(searchTypes[i]);
      searchExcludeLists[i] = LLVM.LLVMConstBitCast(excludeList, LLVM.LLVMPointerType(getExcludeListType(0), 0));
    }
    searchRTTIs[searchTypes.length] = rttiHelper.getPureRTTI(new ObjectType(false, false, null));
    LLVMValueRef objectDescriptor = getObjectVFTDescriptorPointer();
    searchDescriptors[searchTypes.length] = LLVM.LLVMConstBitCast(objectDescriptor, LLVM.LLVMPointerType(getGenericDescriptorType(), 0));
    LLVMValueRef objectVFT = getObjectVFTGlobal();
    searchVFTs[searchTypes.length] = LLVM.LLVMConstBitCast(objectVFT, LLVM.LLVMPointerType(getGenericVFTType(), 0));
    LLVMValueRef objectExcludeList = getExcludeList(null);
    searchExcludeLists[searchTypes.length] = LLVM.LLVMConstBitCast(objectExcludeList, LLVM.LLVMPointerType(getExcludeListType(0), 0));

    LLVMValueRef[] elements = new LLVMValueRef[searchTypes.length + 1];
    LLVMTypeRef[] elementSubTypes = new LLVMTypeRef[] {rttiHelper.getGenericPureRTTIType(),
                                                       LLVM.LLVMPointerType(getGenericDescriptorType(), 0),
                                                       LLVM.LLVMPointerType(getGenericVFTType(), 0),
                                                       LLVM.LLVMPointerType(getExcludeListType(0), 0)};
    LLVMTypeRef elementType = LLVM.LLVMStructType(C.toNativePointerArray(elementSubTypes, false, true), elementSubTypes.length, false);
    for (int i = 0; i < elements.length; ++i)
    {
      LLVMValueRef[] structElements = new LLVMValueRef[] {searchRTTIs[i], searchDescriptors[i], searchVFTs[i], searchExcludeLists[i]};
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
   * @param searchListIndex - the index of the super-type to build the VFT for in the function search list (which is in the same order as the inheritance linearisation, but with 'object' added to the end)
   * @return the VFT generated
   */
  private LLVMValueRef buildSuperTypeVFTGeneration(LLVMBuilderRef builder, int searchListIndex)
  {
    LLVMValueRef functionSearchList = getFunctionSearchList();
    functionSearchList = LLVM.LLVMBuildBitCast(builder, functionSearchList, LLVM.LLVMPointerType(getFunctionSearchListType(), 0), "");
    LLVMValueRef indexValue = LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), searchListIndex, false);

    LLVMValueRef function = getSuperTypeVFTGeneratorFunction();
    LLVMValueRef[] arguments = new LLVMValueRef[] {functionSearchList, indexValue};
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
   * Gets the storage location for a super-type VFT.
   * The super-type to get the storage location for is determined using the specified search list index, which is both:
   * (a) the index into the type search list for the current type definition, and
   * (b) the index into the type linearisation for the current type definition (or equal to the length of the linearisation for the object super-type)
   * @param searchListIndex - the index into the type search list to get
   * @return the super-type VFT memory location for the specified super-type of the current type definition, as a pointer to the correct type of VFT
   */
  public LLVMValueRef getSuperTypeVFTStorage(int searchListIndex)
  {
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new IllegalStateException("Cannot get a superclass's VFT global variable for a non-class type");
    }
    if (typeDefinition.isAbstract())
    {
      throw new IllegalStateException("Cannot get a superclass's VFT global variable for an abstract type");
    }

    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), searchListIndex, false),
                                                 LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false)};
    LLVMValueRef value = LLVM.LLVMConstGEP(getTypeSearchList(typeDefinition), C.toNativePointerArray(indices, false, true), indices.length);

    NamedType[] linearisation = typeDefinition.getInheritanceLinearisation();
    LLVMTypeRef vftType;
    if (searchListIndex == linearisation.length)
    {
      vftType = getObjectVFTType();
    }
    else
    {
      vftType = getVFTType(linearisation[searchListIndex].getResolvedTypeDefinition());
    }
    LLVMTypeRef type = LLVM.LLVMPointerType(LLVM.LLVMPointerType(vftType, 0), 0);
    return LLVM.LLVMConstBitCast(value, type);
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

    NamedType[] superTypes = typeDefinition.getInheritanceLinearisation();
    for (int i = 0; i < superTypes.length; ++i)
    {
      if (superTypes[i].getResolvedTypeDefinition() == typeDefinition)
      {
        continue;
      }
      LLVMValueRef vft = buildSuperTypeVFTGeneration(builder, i);
      vft = LLVM.LLVMBuildBitCast(builder, vft, LLVM.LLVMPointerType(getVFTType(superTypes[i].getResolvedTypeDefinition()), 0), "");
      LLVMValueRef vftGlobal = getSuperTypeVFTStorage(i);
      LLVM.LLVMBuildStore(builder, vft, vftGlobal);
    }
    LLVMValueRef objectVFT = buildSuperTypeVFTGeneration(builder, superTypes.length);
    objectVFT = LLVM.LLVMBuildBitCast(builder, objectVFT, LLVM.LLVMPointerType(getObjectVFTType(), 0), "");
    LLVMValueRef objectVFTGlobal = getSuperTypeVFTStorage(superTypes.length);
    LLVM.LLVMBuildStore(builder, objectVFT, objectVFTGlobal);

    LLVM.LLVMBuildRetVoid(builder);
    LLVM.LLVMDisposeBuilder(builder);
  }
}
