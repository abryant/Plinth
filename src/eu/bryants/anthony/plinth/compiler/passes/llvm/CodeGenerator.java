package eu.bryants.anthony.plinth.compiler.passes.llvm;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMBasicBlockRef;
import nativelib.llvm.LLVM.LLVMBuilderRef;
import nativelib.llvm.LLVM.LLVMContextRef;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMTypeRef;
import nativelib.llvm.LLVM.LLVMValueRef;
import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.expression.ArithmeticExpression;
import eu.bryants.anthony.plinth.ast.expression.ArithmeticExpression.ArithmeticOperator;
import eu.bryants.anthony.plinth.ast.expression.ArrayAccessExpression;
import eu.bryants.anthony.plinth.ast.expression.ArrayCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.BitwiseNotExpression;
import eu.bryants.anthony.plinth.ast.expression.BooleanLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.BooleanNotExpression;
import eu.bryants.anthony.plinth.ast.expression.BracketedExpression;
import eu.bryants.anthony.plinth.ast.expression.CastExpression;
import eu.bryants.anthony.plinth.ast.expression.CreationExpression;
import eu.bryants.anthony.plinth.ast.expression.EqualityExpression;
import eu.bryants.anthony.plinth.ast.expression.EqualityExpression.EqualityOperator;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.plinth.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.plinth.ast.expression.InlineIfExpression;
import eu.bryants.anthony.plinth.ast.expression.InstanceOfExpression;
import eu.bryants.anthony.plinth.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.LogicalExpression;
import eu.bryants.anthony.plinth.ast.expression.LogicalExpression.LogicalOperator;
import eu.bryants.anthony.plinth.ast.expression.MinusExpression;
import eu.bryants.anthony.plinth.ast.expression.NullCoalescingExpression;
import eu.bryants.anthony.plinth.ast.expression.NullLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.ObjectCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression.RelationalOperator;
import eu.bryants.anthony.plinth.ast.expression.ShiftExpression;
import eu.bryants.anthony.plinth.ast.expression.StringLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.SuperVariableExpression;
import eu.bryants.anthony.plinth.ast.expression.ThisExpression;
import eu.bryants.anthony.plinth.ast.expression.TupleExpression;
import eu.bryants.anthony.plinth.ast.expression.TupleIndexExpression;
import eu.bryants.anthony.plinth.ast.expression.VariableExpression;
import eu.bryants.anthony.plinth.ast.member.ArrayLengthMember;
import eu.bryants.anthony.plinth.ast.member.BuiltinMethod;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Initialiser;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.metadata.ArrayLengthMemberReference;
import eu.bryants.anthony.plinth.ast.metadata.ConstructorReference;
import eu.bryants.anthony.plinth.ast.metadata.FieldInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.FieldReference;
import eu.bryants.anthony.plinth.ast.metadata.GenericTypeSpecialiser;
import eu.bryants.anthony.plinth.ast.metadata.GlobalVariable;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunction;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunctionType;
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;
import eu.bryants.anthony.plinth.ast.metadata.MemberVariable;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;
import eu.bryants.anthony.plinth.ast.metadata.PropertyInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.PropertyPseudoVariable;
import eu.bryants.anthony.plinth.ast.metadata.PropertyReference;
import eu.bryants.anthony.plinth.ast.metadata.Variable;
import eu.bryants.anthony.plinth.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.plinth.ast.misc.Assignee;
import eu.bryants.anthony.plinth.ast.misc.BlankAssignee;
import eu.bryants.anthony.plinth.ast.misc.CatchClause;
import eu.bryants.anthony.plinth.ast.misc.FieldAssignee;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.misc.VariableAssignee;
import eu.bryants.anthony.plinth.ast.statement.AssignStatement;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.statement.BreakStatement;
import eu.bryants.anthony.plinth.ast.statement.BreakableStatement;
import eu.bryants.anthony.plinth.ast.statement.ContinueStatement;
import eu.bryants.anthony.plinth.ast.statement.DelegateConstructorStatement;
import eu.bryants.anthony.plinth.ast.statement.ExpressionStatement;
import eu.bryants.anthony.plinth.ast.statement.ForStatement;
import eu.bryants.anthony.plinth.ast.statement.IfStatement;
import eu.bryants.anthony.plinth.ast.statement.PrefixIncDecStatement;
import eu.bryants.anthony.plinth.ast.statement.ReturnStatement;
import eu.bryants.anthony.plinth.ast.statement.ShorthandAssignStatement;
import eu.bryants.anthony.plinth.ast.statement.ShorthandAssignStatement.ShorthandAssignmentOperator;
import eu.bryants.anthony.plinth.ast.statement.Statement;
import eu.bryants.anthony.plinth.ast.statement.ThrowStatement;
import eu.bryants.anthony.plinth.ast.statement.TryStatement;
import eu.bryants.anthony.plinth.ast.statement.WhileStatement;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.NullType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.compiler.passes.Resolver;
import eu.bryants.anthony.plinth.compiler.passes.SpecialTypeHandler;

/*
 * Created on 5 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class CodeGenerator
{
  private TypeDefinition typeDefinition;

  private LLVMContextRef context;
  private LLVMModuleRef module;

  private LLVMValueRef callocFunction;

  private Map<GlobalVariable, LLVMValueRef> globalVariables = new HashMap<GlobalVariable, LLVMValueRef>();

  private TypeHelper typeHelper;
  private VirtualFunctionHandler virtualFunctionHandler;
  private RTTIHelper rttiHelper;
  private BuiltinCodeGenerator builtinGenerator;

  public CodeGenerator(TypeDefinition typeDefinition)
  {
    this.typeDefinition = typeDefinition;
  }

  public void generateModule()
  {
    if (module != null)
    {
      throw new IllegalStateException("Cannot generate the module again, it has already been generated by this CodeGenerator");
    }

    context = LLVM.LLVMContextCreate();
    module = LLVM.LLVMModuleCreateWithName(typeDefinition.getQualifiedName().toString());

    virtualFunctionHandler = new VirtualFunctionHandler(this, typeDefinition, module);
    typeHelper = new TypeHelper(typeDefinition, this, virtualFunctionHandler, module);
    rttiHelper = new RTTIHelper(module, this, typeHelper, virtualFunctionHandler);
    virtualFunctionHandler.initialise(typeHelper, rttiHelper);
    typeHelper.initialise(rttiHelper);
    builtinGenerator = new BuiltinCodeGenerator(module, this, typeHelper, rttiHelper);

    // add all of the global (static) variables
    addGlobalVariables();
    // add all of the LLVM functions, including initialisers, constructors, and methods
    addFunctions();

    if (typeDefinition instanceof ClassDefinition || typeDefinition instanceof InterfaceDefinition)
    {
      virtualFunctionHandler.addVirtualFunctionTable();
      virtualFunctionHandler.addVirtualFunctionTableDescriptor();
    }
    if (typeDefinition instanceof ClassDefinition && !typeDefinition.isAbstract())
    {
      virtualFunctionHandler.addClassVFTInitialisationFunction();
      addAllocatorFunction();
    }
    addInitialiserBody(true);  // add the static initialisers
    if (!(typeDefinition instanceof InterfaceDefinition))
    {
      addInitialiserBody(false); // add the non-static initialisers (but not for interfaces)
    }
    addConstructorBodies();
    addPropertyFunctionBodies();
    addMethodBodies();

    // set the llvm.global_ctors variable, to contain things which need to run before main()
    LLVMValueRef[] globalConstructorFunctions;
    int[] priorities;
    if (typeDefinition instanceof ClassDefinition && !typeDefinition.isAbstract())
    {
      globalConstructorFunctions = new LLVMValueRef[] {virtualFunctionHandler.getClassVFTInitialisationFunction(),
                                                       getInitialiserFunction(true)};
      priorities = new int[] {0, 10};
    }
    else
    {
      globalConstructorFunctions = new LLVMValueRef[] {getInitialiserFunction(true)};
      priorities = new int[] {10};
    }
    setGlobalConstructors(globalConstructorFunctions, priorities);

    MetadataGenerator.generateMetadata(typeDefinition, module);
  }

  public LLVMContextRef getContext()
  {
    if (context == null)
    {
      throw new IllegalStateException("The context has not yet been created; please call generateModule() before getContext()");
    }
    return context;
  }

  public LLVMModuleRef getModule()
  {
    if (module == null)
    {
      throw new IllegalStateException("The module has not yet been created; please call generateModule() before getModule()");
    }
    return module;
  }

  private void addGlobalVariables()
  {
    for (Field field : typeDefinition.getFields())
    {
      if (field.isStatic())
      {
        getGlobal(field.getGlobalVariable());
      }
    }
    for (Property property : typeDefinition.getProperties())
    {
      if (property.isStatic() && !property.isUnbacked())
      {
        getGlobal(property.getBackingGlobalVariable());
      }
    }

    if (typeDefinition instanceof ClassDefinition)
    {
      virtualFunctionHandler.getVFTGlobal(typeDefinition);
      virtualFunctionHandler.getVFTDescriptorPointer(typeDefinition);
    }
  }

  private LLVMValueRef getGlobal(GlobalVariable globalVariable)
  {
    LLVMValueRef value = globalVariables.get(globalVariable);
    if (value != null)
    {
      return value;
    }
    // lazily initialise globals which do not yet exist
    Type type = globalVariable.getType();
    LLVMValueRef newValue = LLVM.LLVMAddGlobal(module, typeHelper.findStandardType(type), globalVariable.getMangledName());
    if (globalVariable.getEnclosingTypeDefinition() == typeDefinition)
    {
      LLVM.LLVMSetInitializer(newValue, LLVM.LLVMConstNull(typeHelper.findStandardType(type)));
    }
    globalVariables.put(globalVariable, newValue);
    return newValue;
  }

  private void addFunctions()
  {
    // add calloc() as an external function
    LLVMTypeRef callocReturnType = LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0);
    LLVMTypeRef[] callocParamTypes = new LLVMTypeRef[] {LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount())};
    callocFunction = LLVM.LLVMAddFunction(module, "calloc", LLVM.LLVMFunctionType(callocReturnType, C.toNativePointerArray(callocParamTypes, false, true), callocParamTypes.length, false));

    // create the static and non-static initialiser functions
    if (typeDefinition instanceof ClassDefinition && !typeDefinition.isAbstract())
    {
      virtualFunctionHandler.getClassVFTInitialisationFunction();
      getAllocatorFunction((ClassDefinition) typeDefinition);
    }
    getInitialiserFunction(true);
    if (!(typeDefinition instanceof InterfaceDefinition))
    {
      getInitialiserFunction(false);
    }

    // create the constructor and method functions
    for (Constructor constructor : typeDefinition.getAllConstructors())
    {
      getConstructorFunction(constructor);
    }
    for (Property property : typeDefinition.getProperties())
    {
      if (property.isAbstract())
      {
        continue;
      }
      getPropertyGetterFunction(property);
      if (!property.isFinal())
      {
        getPropertySetterFunction(property);
      }
      if (property.hasConstructor())
      {
        getPropertyConstructorFunction(property);
      }
    }
    for (Method method : typeDefinition.getAllMethods())
    {
      if (method.isAbstract())
      {
        continue;
      }
      getMethodFunction(method);
    }
  }

  /**
   * @return the declaration of the i8* calloc(i32, i32) function
   */
  public LLVMValueRef getCallocFunction()
  {
    return callocFunction;
  }

  /**
   * @return the personality function to be used in landingpad instructions (for exception handling)
   */
  public LLVMValueRef getPersonalityFunction()
  {
    final String personalityFunctionName = "plinth_personality";
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, personalityFunctionName);
    if (existingFunction != null)
    {
      return existingFunction;
    }
    LLVMTypeRef resultType = LLVM.LLVMInt32Type();
    // don't assume the arguments have any particular types, the real types will be target dependent
    LLVMTypeRef[] argumentTypes = new LLVMTypeRef[] {};
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(argumentTypes, false, true), argumentTypes.length, true);
    return LLVM.LLVMAddFunction(module, personalityFunctionName, functionType);
  }

  /**
   * Gets the allocator function for the specified ClassDefinition.
   * @param classDefinition - the class definition to get the allocator for
   * @return the function declaration for the allocator of the specified ClassDefinition
   */
  public LLVMValueRef getAllocatorFunction(ClassDefinition classDefinition)
  {
    if (classDefinition.isAbstract())
    {
      throw new IllegalArgumentException("Abstract classes do not have allocator functions");
    }
    String mangledName = classDefinition.getAllocatorMangledName();
    LLVMValueRef existingFunc = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunc != null)
    {
      return existingFunc;
    }

    // an allocator's arguments are the pure RTTI blocks of each of the type parameters
    TypeParameter[] typeParameters = classDefinition.getTypeParameters();
    LLVMTypeRef[] types = new LLVMTypeRef[typeParameters.length];
    for (int i = 0; i < typeParameters.length; ++i)
    {
      types[i] = rttiHelper.getGenericPureRTTIType();
    }

    LLVMTypeRef returnType = typeHelper.findTemporaryType(new NamedType(false, false, false, classDefinition));
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(returnType, C.toNativePointerArray(types, false, true), types.length, false);
    LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);
    return llvmFunc;
  }

  /**
   * Gets the (static or non-static) initialiser function for the TypeDefinition we are building.
   * @param isStatic - true for the static initialiser, false for the non-static initialiser
   * @return the function declaration for the specified Initialiser
   */
  private LLVMValueRef getInitialiserFunction(boolean isStatic)
  {
    if (typeDefinition instanceof InterfaceDefinition && !isStatic)
    {
      throw new IllegalArgumentException("Interfaces do not have non-static initialisers");
    }
    String mangledName = Initialiser.getMangledName(typeDefinition, isStatic);
    LLVMValueRef existingFunc = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunc != null)
    {
      return existingFunc;
    }

    LLVMTypeRef[] types = null;
    if (isStatic)
    {
      types = new LLVMTypeRef[0];
    }
    else
    {
      types = new LLVMTypeRef[1];
      types[0] = typeHelper.findTemporaryType(new NamedType(false, false, false, typeDefinition));
    }
    LLVMTypeRef returnType = LLVM.LLVMVoidType();

    LLVMTypeRef functionType = LLVM.LLVMFunctionType(returnType, C.toNativePointerArray(types, false, true), types.length, false);
    LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);

    if (!isStatic)
    {
      LLVM.LLVMSetValueName(LLVM.LLVMGetParam(llvmFunc, 0), "this");
    }
    return llvmFunc;
  }

  /**
   * Gets the function definition for the specified Constructor. If necessary, it is added first.
   * @param constructor - the Constructor to find the declaration of (or to declare)
   * @return the function declaration for the specified Constructor
   */
  LLVMValueRef getConstructorFunction(Constructor constructor)
  {
    String mangledName = constructor.getMangledName();
    LLVMValueRef existingFunc = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunc != null)
    {
      return existingFunc;
    }
    TypeDefinition typeDefinition = constructor.getContainingTypeDefinition();

    Parameter[] parameters = constructor.getParameters();
    LLVMTypeRef[] types = null;
    // constructors need an extra 'uninitialised this' parameter at the start, which is the newly allocated data to initialise
    // the 'this' parameter always has a temporary type representation
    types = new LLVMTypeRef[1 + parameters.length];
    types[0] = typeHelper.findTemporaryType(new NamedType(false, false, false, typeDefinition));
    for (int i = 0; i < parameters.length; i++)
    {
      types[1 + i] = typeHelper.findStandardType(parameters[i].getType());
    }
    LLVMTypeRef resultType = LLVM.LLVMVoidType();

    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);
    LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);

    LLVM.LLVMSetValueName(LLVM.LLVMGetParam(llvmFunc, 0), "this");
    for (int i = 0; i < parameters.length; i++)
    {
      LLVMValueRef parameter = LLVM.LLVMGetParam(llvmFunc, 1 + i);
      LLVM.LLVMSetValueName(parameter, parameters[i].getName());
    }
    return llvmFunc;
  }

  /**
   * Looks up the function definition for the specified Method. If necessary, its declaration is added first.
   * For non-static methods, this function may generate a lookup into a virtual function table on the callee rather than looking up the method directly.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param callee - the callee of the method, to look up the virtual method on if the Method is part of a virtual function table
   * @param calleeType - the Type of the callee of the method, to determine how to extract the function from the callee value
   * @param methodReference - the reference to the Method to find
   * @param skipVirtualLookup - true if the method should not be looked up in the virtual function table
   * @param typeParameterAccessor - the TypeParameterAccessor that contains the RTTI for any type parameters accessible in this context
   * @return the function to call for the specified Method
   */
  LLVMValueRef lookupMethodFunction(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef callee, Type calleeType, MethodReference methodReference, boolean skipVirtualLookup, TypeParameterAccessor typeParameterAccessor)
  {
    if (!methodReference.getReferencedMember().isStatic() && !skipVirtualLookup &&
         (calleeType instanceof ObjectType ||
           (calleeType instanceof NamedType && ((NamedType) calleeType).getResolvedTypeParameter() != null) ||
           (calleeType instanceof NamedType && ((NamedType) calleeType).getResolvedTypeDefinition() instanceof ClassDefinition) ||
           (calleeType instanceof NamedType && ((NamedType) calleeType).getResolvedTypeDefinition() instanceof InterfaceDefinition)))
    {
      // generate a virtual function table lookup
      return virtualFunctionHandler.getMethodPointer(builder, landingPadContainer, callee, calleeType, methodReference, typeParameterAccessor);
    }
    return getMethodFunction(methodReference.getReferencedMember());
  }

  /**
   * Looks up the function definition for the specified Property function. If necessary, its declaration is added first.
   * For non-static properties, this function may generate a lookup into a virtual function table on the callee rather than looking up the function directly.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param callee - the callee of the property function, to look up the virtual function on if it is part of a virtual function table
   * @param calleeType - the Type of the callee of the property function, to determine how to extract the function from the callee value
   * @param propertyReference - the reference to the property to find
   * @param memberFunctionType - the type of property function to look up
   * @param typeParameterAccessor - the TypeParameterAccessor that contains the RTTI for any type parameters accessible in this context
   * @return the function to call for the specified property function
   */
  LLVMValueRef lookupPropertyFunction(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef callee, Type calleeType, PropertyReference propertyReference, MemberFunctionType memberFunctionType, TypeParameterAccessor typeParameterAccessor)
  {
    Property property = propertyReference.getReferencedMember();
    if (!property.isStatic() &&
        (calleeType instanceof ObjectType ||
          (calleeType instanceof NamedType && ((NamedType) calleeType).getResolvedTypeDefinition() instanceof ClassDefinition) ||
          (calleeType instanceof NamedType && ((NamedType) calleeType).getResolvedTypeDefinition() instanceof InterfaceDefinition)))
    {
      MemberFunction propertyFunction;
      switch (memberFunctionType)
      {
      case PROPERTY_CONSTRUCTOR:
        propertyFunction = property.getConstructorMemberFunction();
        break;
      case PROPERTY_GETTER:
        propertyFunction = property.getGetterMemberFunction();
        break;
      case PROPERTY_SETTER:
        propertyFunction = property.getSetterMemberFunction();
        break;
      case METHOD:
      default:
        throw new IllegalArgumentException("Cannot get a non-property MemberFunction on a property reference");
      }
      // generate a virtual function table lookup
      return virtualFunctionHandler.getPropertyFuntionPointer(builder, landingPadContainer, callee, calleeType, propertyReference, propertyFunction, typeParameterAccessor);
    }
    switch (memberFunctionType)
    {
    case PROPERTY_CONSTRUCTOR:
      return getPropertyConstructorFunction(property);
    case PROPERTY_GETTER:
      return getPropertyGetterFunction(property);
    case PROPERTY_SETTER:
      return getPropertySetterFunction(property);
    case METHOD:
    default:
      throw new IllegalArgumentException("Cannot get a non-property MemberFunction on a property reference");
    }
  }

  /**
   * Gets the LLVM function for the specified Method. If necessary, its declaration is added first.
   * @param method - the Method to find the declaration of (or to declare)
   * @return the function declaration for the specified Method
   */
  LLVMValueRef getMethodFunction(Method method)
  {
    if (method.isAbstract())
    {
      throw new IllegalArgumentException("Abstract methods do not have LLVM functions: " + method);
    }
    String mangledName = method.getMangledName();
    LLVMValueRef existingFunc = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunc != null)
    {
      return existingFunc;
    }
    // if it is a built-in method, generate it (unless it is part of a type definition, in which case just define it to be linked in)
    if (method instanceof BuiltinMethod && method.getContainingTypeDefinition() == null)
    {
      return builtinGenerator.generateMethod((BuiltinMethod) method);
    }

    LLVMTypeRef functionType = typeHelper.findMethodType(method);
    LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);

    LLVM.LLVMSetValueName(LLVM.LLVMGetParam(llvmFunc, 0), method.isStatic() ? "unused" : "this");
    Parameter[] parameters = method.getParameters();
    for (int i = 0; i < parameters.length; ++i)
    {
      LLVMValueRef parameter = LLVM.LLVMGetParam(llvmFunc, i + 1);
      LLVM.LLVMSetValueName(parameter, parameters[i].getName());
    }
    return llvmFunc;
  }

  /**
   * Gets the function declaration for the specified Property getter.
   * @param property - the Property to find the declaration of the getter for
   * @return the function declaration for the getter of the specified Property
   */
  LLVMValueRef getPropertyGetterFunction(Property property)
  {
    if (property.isAbstract())
    {
      throw new IllegalArgumentException("Abstract properties do not have LLVM functions: " + property);
    }
    String mangledName = property.getGetterMangledName();
    LLVMValueRef existingFunc = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunc != null)
    {
      return existingFunc;
    }
    LLVMTypeRef functionType = typeHelper.findPropertyGetterType(property);

    LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);
    LLVM.LLVMSetValueName(LLVM.LLVMGetParam(llvmFunc, 0), property.isStatic() ? "unused" : "this");
    return llvmFunc;
  }

  /**
   * Gets the function declaration for the specified Property setter.
   * @param property - the Property to find the declaration of the setter for
   * @return the function declaration for the setter of the specified Property
   */
  LLVMValueRef getPropertySetterFunction(Property property)
  {
    if (property.isAbstract())
    {
      throw new IllegalArgumentException("Abstract properties do not have LLVM functions: " + property);
    }
    String mangledName = property.getSetterMangledName();
    LLVMValueRef existingFunc = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunc != null)
    {
      return existingFunc;
    }
    LLVMTypeRef functionType = typeHelper.findPropertySetterConstructorType(property);

    LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);
    LLVM.LLVMSetValueName(LLVM.LLVMGetParam(llvmFunc, 0), property.isStatic() ? "unused" : "this");
    return llvmFunc;
  }

  /**
   * Gets the function declaration for the specified Property constructor.
   * @param property - the Property to find the declaration of the constructor for
   * @return the function declaration for the constructor of the specified Property
   */
  LLVMValueRef getPropertyConstructorFunction(Property property)
  {
    if (property.isAbstract())
    {
      throw new IllegalArgumentException("Abstract properties do not have LLVM functions: " + property);
    }
    String mangledName = property.getConstructorMangledName();
    LLVMValueRef existingFunc = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunc != null)
    {
      return existingFunc;
    }
    LLVMTypeRef functionType = typeHelper.findPropertySetterConstructorType(property);

    LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);
    LLVM.LLVMSetValueName(LLVM.LLVMGetParam(llvmFunc, 0), property.isStatic() ? "unused" : "this");
    return llvmFunc;
  }

  /**
   * Adds a native function which calls the specified non-native function.
   * This consists simply of a new function with the method's native name, which calls the non-native function and returns its result.
   * @param method - the method that this native upcall function is for
   * @param nonNativeFunction - the non-native function to call
   */
  private void addNativeUpcallFunction(Method method, LLVMValueRef nonNativeFunction)
  {
    Parameter[] parameters = method.getParameters();
    Type returnType = method.getReturnType();
    // if the method is non-static, add the pointer argument
    int offset = method.isStatic() ? 0 : 1;
    LLVMTypeRef[] parameterTypes = new LLVMTypeRef[offset + parameters.length];
    if (!method.isStatic())
    {
      if (typeDefinition instanceof ClassDefinition || typeDefinition instanceof CompoundDefinition)
      {
        parameterTypes[0] = typeHelper.findTemporaryType(new NamedType(false, method.isImmutable(), method.isImmutable(), method.getContainingTypeDefinition()));
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        // interfaces are just represented by objects in native code, without a VFT tuple
        parameterTypes[0] = typeHelper.findTemporaryType(new ObjectType(false, method.isImmutable(), null));
        if (typeDefinition.getTypeParameters().length > 0)
        {
          // if we have type parameters, we cannot generate this native upcall, since converting from object to the interface type would require knowing the values of the type parameters
          throw new IllegalStateException("A generic interface cannot have a non-static native upcall method");
        }
      }
    }
    for (int i = 0; i < parameters.length; ++i)
    {
      Type type = parameters[i].getType();
      if (type instanceof NamedType && ((NamedType) type).getResolvedTypeDefinition() instanceof InterfaceDefinition)
      {
        // interfaces are represented by objects in native code
        parameterTypes[offset + i] = typeHelper.findStandardType(new ObjectType(type.isNullable(), ((NamedType) type).isContextuallyImmutable(), null));
      }
      else
      {
        parameterTypes[offset + i] = typeHelper.findStandardType(parameters[i].getType());
      }
    }
    LLVMTypeRef resultType;
    if (returnType instanceof NamedType && ((NamedType) returnType).getResolvedTypeDefinition() instanceof InterfaceDefinition)
    {
      resultType = typeHelper.findStandardType(new ObjectType(false, false, null));
    }
    else
    {
      resultType = typeHelper.findStandardType(returnType);
    }

    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(parameterTypes, false, true), parameterTypes.length, false);

    LLVMValueRef nativeFunction = LLVM.LLVMAddFunction(module, method.getNativeName(), functionType);
    LLVM.LLVMSetFunctionCallConv(nativeFunction, LLVM.LLVMCallConv.LLVMCCallConv);
    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(nativeFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);
    TypeParameterAccessor typeParameterAccessor;

    // if the method is static, add a null first argument to the list of arguments to pass to the non-native function
    LLVMValueRef[] arguments = new LLVMValueRef[1 + parameters.length];
    if (method.isStatic())
    {
      arguments[0] = LLVM.LLVMConstNull(typeHelper.getOpaquePointer());
      typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper);
    }
    else
    {
      LLVMValueRef callee = LLVM.LLVMGetParam(nativeFunction, 0);
      if (typeDefinition instanceof InterfaceDefinition)
      {
        // we cannot get here if there are any type parameters on this interface, so build an empty TypeParameterAccessor here
        typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper);
        arguments[0] = typeHelper.convertTemporary(builder, landingPadContainer, callee, new ObjectType(false, method.isImmutable(), null), new NamedType(false, method.isImmutable(), method.isImmutable(), typeDefinition), false, typeParameterAccessor);
      }
      else
      {
        arguments[0] = callee;
        typeParameterAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, typeDefinition, callee);
      }
    }
    for (int i = 0; i < parameters.length; ++i)
    {
      Type type = parameters[i].getType();
      if (type instanceof NamedType && ((NamedType) type).getResolvedTypeDefinition() instanceof InterfaceDefinition)
      {
        // interfaces are represented by objects in native code, so convert them back to interfaces
        ObjectType objectType = new ObjectType(type.isNullable(), ((NamedType) type).isContextuallyImmutable(), null);
        LLVMValueRef tempValue = typeHelper.convertStandardToTemporary(builder, LLVM.LLVMGetParam(nativeFunction, offset + i), objectType);
        arguments[1 + i] = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, tempValue, objectType, type, typeParameterAccessor);
      }
      else
      {
        arguments[1 + i] = LLVM.LLVMGetParam(nativeFunction, offset + i);
      }
    }
    LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "invokeContinue");
    LLVMValueRef result = LLVM.LLVMBuildInvoke(builder, nonNativeFunction, C.toNativePointerArray(arguments, false, true), arguments.length, invokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);
    if (returnType instanceof VoidType)
    {
      LLVM.LLVMBuildRetVoid(builder);
    }
    else
    {
      if (returnType instanceof NamedType && ((NamedType) returnType).getResolvedTypeDefinition() instanceof InterfaceDefinition)
      {
        // interfaces are represented by objects in native code, so convert this result back to an object
        result = typeHelper.convertStandardToTemporary(builder, result, returnType);
        result = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, result, returnType, new ObjectType(false, false, null), typeParameterAccessor);
      }
      LLVM.LLVMBuildRet(builder, result);
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);
  }

  /**
   * Adds a native function, and calls it from the specified non-native function.
   * This consists simply of a new function declaration with the method's native name,
   * and a call to it from the specified non-native function which returns its result.
   * @param method - the method that this native downcall function is for
   * @param nonNativeFunction - the non-native function to make the downcall
   */
  private void addNativeDowncallFunction(Method method, LLVMValueRef nonNativeFunction)
  {
    Parameter[] parameters = method.getParameters();
    Type returnType = method.getReturnType();
    // if the method is non-static, add the pointer argument
    int nativeArgumentOffset = method.isStatic() ? 0 : 1;
    LLVMTypeRef[] argumentTypes = new LLVMTypeRef[nativeArgumentOffset + parameters.length];
    if (!method.isStatic())
    {
      if (typeDefinition instanceof ClassDefinition || typeDefinition instanceof CompoundDefinition)
      {
        argumentTypes[0] = typeHelper.findTemporaryType(new NamedType(false, method.isImmutable(), method.isImmutable(), method.getContainingTypeDefinition()));
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        argumentTypes[0] = typeHelper.findTemporaryType(new ObjectType(false, method.isImmutable(), null));
      }
    }
    for (int i = 0; i < parameters.length; ++i)
    {
      Type type = parameters[i].getType();
      if (type instanceof NamedType && ((NamedType) type).getResolvedTypeDefinition() instanceof InterfaceDefinition)
      {
        // interfaces are represented by objects in native code
        argumentTypes[nativeArgumentOffset + i] = typeHelper.findStandardType(new ObjectType(type.isNullable(), ((NamedType) type).isContextuallyImmutable(), null));
      }
      else
      {
        argumentTypes[nativeArgumentOffset + i] = typeHelper.findStandardType(parameters[i].getType());
      }
    }
    LLVMTypeRef resultType;
    if (returnType instanceof NamedType && ((NamedType) returnType).getResolvedTypeDefinition() instanceof InterfaceDefinition)
    {
      // interfaces are represented by objects in native code
      resultType = typeHelper.findStandardType(new ObjectType(false, false, null));
    }
    else
    {
      resultType = typeHelper.findStandardType(method.getReturnType());
    }
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(argumentTypes, false, true), argumentTypes.length, false);

    LLVMValueRef nativeFunction = LLVM.LLVMAddFunction(module, method.getNativeName(), functionType);
    LLVM.LLVMSetFunctionCallConv(nativeFunction, LLVM.LLVMCallConv.LLVMCCallConv);

    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(nonNativeFunction);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    LLVMValueRef thisValue = method.isStatic() ? null : LLVM.LLVMGetParam(nonNativeFunction, 0);

    TypeParameterAccessor typeParameterAccessor;
    int nonNativeParameterOffset;
    if (method.isStatic())
    {
      typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper);
      nonNativeParameterOffset = 1;
    }
    else
    {
      if (method.getContainingTypeDefinition() instanceof InterfaceDefinition)
      {
        // extract the interface's type parameters
        TypeParameter[] typeParameters = method.getContainingTypeDefinition().getTypeParameters();
        Map<TypeParameter, LLVMValueRef> knownTypeParameters = new HashMap<TypeParameter, LLVMValueRef>();
        for (int i = 0; i < typeParameters.length; ++i)
        {
          LLVMValueRef parameterValue = LLVM.LLVMGetParam(nonNativeFunction, 1 + i);
          knownTypeParameters.put(typeParameters[i], parameterValue);
        }
        typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper, method.getContainingTypeDefinition(), knownTypeParameters);
        nonNativeParameterOffset = 1 + typeParameters.length;
      }
      else
      {
        typeParameterAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, method.getContainingTypeDefinition(), thisValue);
        nonNativeParameterOffset = 1;
      }
    }

    LLVMValueRef[] arguments = new LLVMValueRef[argumentTypes.length];
    if (!method.isStatic())
    {
      if (typeDefinition instanceof InterfaceDefinition)
      {
        arguments[0] = typeHelper.convertTemporary(builder, landingPadContainer, thisValue, new NamedType(false, method.isImmutable(), method.isImmutable(), method.getContainingTypeDefinition()), new ObjectType(false, method.isImmutable(), null), false, typeParameterAccessor);
      }
      else
      {
        arguments[0] = thisValue;
      }
    }

    for (int i = 0; i < parameters.length; ++i)
    {
      Type type = parameters[i].getType();
      LLVMValueRef nonNativeParameter = LLVM.LLVMGetParam(nonNativeFunction, nonNativeParameterOffset + i);
      if (type instanceof NamedType && ((NamedType) type).getResolvedTypeDefinition() instanceof InterfaceDefinition)
      {
        // convert the interface to an object
        LLVMValueRef tempValue = typeHelper.convertStandardToTemporary(builder, nonNativeParameter, type);
        arguments[nativeArgumentOffset + i] = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, tempValue, type, new ObjectType(false, ((NamedType) type).isContextuallyImmutable(), null), typeParameterAccessor);
      }
      else
      {
        arguments[nativeArgumentOffset + i] = nonNativeParameter;
      }
    }
    LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "invokeContinue");
    LLVMValueRef result = LLVM.LLVMBuildInvoke(builder, nativeFunction, C.toNativePointerArray(arguments, false, true), arguments.length, invokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);
    if (returnType instanceof VoidType)
    {
      LLVM.LLVMBuildRetVoid(builder);
    }
    else
    {
      if (returnType instanceof NamedType && ((NamedType) returnType).getResolvedTypeDefinition() instanceof InterfaceDefinition)
      {
        // interfaces are represented as objects in native code, so convert the result back to the interface representation
        ObjectType objectType = new ObjectType(false, false, null);
        result = typeHelper.convertStandardToTemporary(builder, result, objectType);
        result = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, result, objectType, returnType, typeParameterAccessor);
      }
      LLVM.LLVMBuildRet(builder, result);
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);
  }

  private void addAllocatorFunction()
  {
    if (!(typeDefinition instanceof ClassDefinition))
    {
      throw new UnsupportedOperationException("Allocators cannot be created for anything but class definitions");
    }
    LLVMValueRef llvmFunction = getAllocatorFunction((ClassDefinition) typeDefinition);
    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(llvmFunction);

    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    NamedType namedType = new NamedType(false, false, false, typeDefinition);

    // allocate memory for the object
    LLVMTypeRef nativeType = typeHelper.findTemporaryType(namedType);
    LLVMValueRef pointer = buildHeapAllocation(builder, nativeType);

    // gather the type parameter RTTI blocks from the allocator function parameters
    TypeParameter[] typeParameters = typeDefinition.getTypeParameters();
    Map<TypeParameter, LLVMValueRef> typeParameterMap = new HashMap<TypeParameter, LLVMValueRef>();
    for (int i = 0; i < typeParameters.length; ++i)
    {
      typeParameterMap.put(typeParameters[i], LLVM.LLVMGetParam(llvmFunction, i));
    }
    TypeParameterAccessor typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper, typeDefinition, typeParameterMap);

    // generate and store the run-time type information
    LLVMValueRef rtti = rttiHelper.buildRTTICreation(builder, true, namedType, typeParameterAccessor);
    LLVMValueRef rttiPointer = rttiHelper.getRTTIPointer(builder, pointer);
    LLVM.LLVMBuildStore(builder, rtti, rttiPointer);

    for (NamedType linearisationType : typeDefinition.getInheritanceLinearisation())
    {
      TypeDefinition linearisationTypeDefinition = linearisationType.getResolvedTypeDefinition();
      if (linearisationTypeDefinition instanceof ClassDefinition)
      {
        TypeParameter[] linearisationTypeParameters = linearisationTypeDefinition.getTypeParameters();
        Type[] linearisationTypeArguments = linearisationType.getTypeArguments();
        for (int i = 0; i < linearisationTypeParameters.length; ++i)
        {
          LLVMValueRef typeArgumentRTTI = rttiHelper.buildRTTICreation(builder, false, linearisationTypeArguments[i], typeParameterAccessor);
          LLVMValueRef typeParameterRTTIPointer = typeHelper.getTypeParameterPointer(builder, pointer, linearisationTypeParameters[i]);
          LLVM.LLVMBuildStore(builder, typeArgumentRTTI, typeParameterRTTIPointer);
        }
      }
    }

    // set up the virtual function tables
    NamedType[] linearisation = typeDefinition.getInheritanceLinearisation();
    for (int i = 0; i < linearisation.length; ++i)
    {
      LLVMValueRef currentVFT;
      if (linearisation[i].getResolvedTypeDefinition() == typeDefinition)
      {
        currentVFT = virtualFunctionHandler.getVFTGlobal(linearisation[i].getResolvedTypeDefinition());
      }
      else
      {
        LLVMValueRef globalValue = virtualFunctionHandler.getSuperTypeVFTStorage(i);
        currentVFT = LLVM.LLVMBuildLoad(builder, globalValue, "");
      }
      LLVMValueRef vftPointer = virtualFunctionHandler.getVirtualFunctionTablePointer(builder, pointer, namedType, linearisation[i]);
      LLVM.LLVMBuildStore(builder, currentVFT, vftPointer);
    }
    LLVMValueRef objectVFTGlobal = virtualFunctionHandler.getSuperTypeVFTStorage(linearisation.length);
    LLVMValueRef objectVFT = LLVM.LLVMBuildLoad(builder, objectVFTGlobal, "");
    LLVMValueRef objectVFTPointer = virtualFunctionHandler.getFirstVirtualFunctionTablePointer(builder, pointer);
    LLVM.LLVMBuildStore(builder, objectVFT, objectVFTPointer);

    LLVM.LLVMBuildRet(builder, pointer);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);
  }

  private void addInitialiserBody(boolean isStatic)
  {
    LLVMValueRef initialiserFunc = getInitialiserFunction(isStatic);
    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(initialiserFunc);
    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    LLVMValueRef thisValue = isStatic ? null : LLVM.LLVMGetParam(initialiserFunc, 0);
    NamedType thisType = new NamedType(false, false, false, typeDefinition);

    TypeParameterAccessor typeParameterAccessor = isStatic ? null : new TypeParameterAccessor(builder, typeHelper, rttiHelper, typeDefinition, thisValue);

    // build all of the static/non-static initialisers in one LLVM function
    for (Initialiser initialiser : typeDefinition.getInitialisers())
    {
      if (initialiser.isStatic() != isStatic)
      {
        continue;
      }
      if (initialiser instanceof FieldInitialiser)
      {
        Field field = ((FieldInitialiser) initialiser).getField();
        LLVMValueRef result = buildExpression(field.getInitialiserExpression(), builder, thisValue, new HashMap<Variable, LLVM.LLVMValueRef>(), landingPadContainer, typeParameterAccessor);
        LLVMValueRef assigneePointer = null;
        if (field.isStatic())
        {
          assigneePointer = getGlobal(field.getGlobalVariable());
        }
        else
        {
          assigneePointer = typeHelper.getMemberPointer(builder, thisValue, field.getMemberVariable());
        }

        Type contextualisedType = typeHelper.getContextualisedActualMemberType(field.getType(), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
        LLVMValueRef convertedValue = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, result, field.getInitialiserExpression().getType(), contextualisedType, typeParameterAccessor);
        LLVM.LLVMBuildStore(builder, convertedValue, assigneePointer);
      }
      else if (initialiser instanceof PropertyInitialiser)
      {
        Property property = ((PropertyInitialiser) initialiser).getProperty();
        LLVMValueRef result = buildExpression(property.getInitialiserExpression(), builder, thisValue, new HashMap<Variable, LLVMValueRef>(), landingPadContainer, typeParameterAccessor);

        PropertyReference propertyReference = new PropertyReference(property, GenericTypeSpecialiser.IDENTITY_SPECIALISER);
        result = typeHelper.convertTemporary(builder, landingPadContainer, result, property.getInitialiserExpression().getType(), propertyReference.getType(), false, typeParameterAccessor);

        boolean isConstructorCall = ((PropertyInitialiser) initialiser).isPropertyConstructorCall();
        MemberFunctionType propertyFunctionType = isConstructorCall ? MemberFunctionType.PROPERTY_CONSTRUCTOR : MemberFunctionType.PROPERTY_SETTER;
        LLVMValueRef callee;
        Type calleeType;
        if (property.isStatic())
        {
          callee = LLVM.LLVMConstNull(typeHelper.getOpaquePointer());
          calleeType = null;
        }
        else
        {
          callee = thisValue;
          calleeType = thisType;
        }
        typeHelper.buildPropertySetterConstructorFunctionCall(builder, landingPadContainer, callee, calleeType, result, propertyReference, propertyFunctionType, typeParameterAccessor);
      }
      else
      {
        // build allocas for all of the variables, at the start of the entry block
        Set<Variable> allVariables = Resolver.getAllNestedVariables(initialiser.getBlock());
        Map<Variable, LLVMValueRef> variables = new HashMap<Variable, LLVM.LLVMValueRef>();
        for (Variable v : allVariables)
        {
          LLVMValueRef allocaInst = LLVM.LLVMBuildAllocaInEntryBlock(builder, typeHelper.findTemporaryType(v.getType()), v.getName());
          variables.put(v, allocaInst);
        }

        buildStatement(initialiser.getBlock(), VoidType.VOID_TYPE, builder, thisValue, variables, typeParameterAccessor,
                       landingPadContainer, new HashMap<TryStatement, LLVMBasicBlockRef>(), new HashMap<TryStatement, LLVMValueRef>(),
                       new HashMap<TryStatement, List<LLVMBasicBlockRef>>(), new HashMap<BreakableStatement, LLVMBasicBlockRef>(),
                       new HashMap<BreakableStatement, LLVMBasicBlockRef>(), new Runnable()
        {
          @Override
          public void run()
          {
            throw new IllegalStateException("Cannot return from an initialiser");
          }
        });
      }
    }
    LLVM.LLVMBuildRetVoid(builder);

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);
  }

  /**
   * Sets the global constructors for this module. This should only be done once per module.
   * The functions provided are put into the llvm.global_ctors variable, along with their associated priorities.
   * At run time, these functions will be run before main(), in ascending order of priority (so priority 0 is run first, then priority 1, etc.)
   * @param functions - the functions to run before main()
   * @param priorities - the priorities of the functions
   */
  private void setGlobalConstructors(LLVMValueRef[] functions, int[] priorities)
  {
    if (functions.length != priorities.length)
    {
      throw new IllegalArgumentException("To set the global constructors, you must provide an equal number of functions and priorities");
    }

    // build up the type of the global variable
    LLVMTypeRef[] paramTypes = new LLVMTypeRef[0];
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(LLVM.LLVMVoidType(), C.toNativePointerArray(paramTypes, false, true), paramTypes.length, false);
    LLVMTypeRef functionPointerType = LLVM.LLVMPointerType(functionType, 0);
    LLVMTypeRef[] structSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), functionPointerType};
    LLVMTypeRef structType = LLVM.LLVMStructType(C.toNativePointerArray(structSubTypes, false, true), structSubTypes.length, false);
    LLVMTypeRef arrayType = LLVM.LLVMArrayType(structType, functions.length);

    // build the constant expression for global variable's initialiser
    LLVMValueRef[] arrayElements = new LLVMValueRef[functions.length];
    for (int i = 0; i < functions.length; ++i)
    {
      LLVMValueRef[] constantValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), priorities[i], false), functions[i]};
      arrayElements[i] = LLVM.LLVMConstStruct(C.toNativePointerArray(constantValues, false, true), constantValues.length, false);
    }
    LLVMValueRef array = LLVM.LLVMConstArray(structType, C.toNativePointerArray(arrayElements, false, true), arrayElements.length);

    // create the 'llvm.global_ctors' global variable, which lists which functions are run before main()
    LLVMValueRef global = LLVM.LLVMAddGlobal(module, arrayType, "llvm.global_ctors");
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMAppendingLinkage);
    LLVM.LLVMSetInitializer(global, array);
  }

  private void addConstructorBodies()
  {
    for (Constructor constructor : typeDefinition.getAllConstructors())
    {
      final LLVMValueRef llvmFunction = getConstructorFunction(constructor);

      final LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(llvmFunction);
      LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

      // the first constructor parameter is always the newly allocated 'this' pointer
      final LLVMValueRef thisValue = LLVM.LLVMGetParam(llvmFunction, 0);

      TypeParameterAccessor typeParameterAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, typeDefinition, thisValue);

      // create LLVMValueRefs for all of the variables, including paramters
      Set<Variable> allVariables = Resolver.getAllNestedVariables(constructor.getBlock());
      Map<Variable, LLVMValueRef> variables = new HashMap<Variable, LLVM.LLVMValueRef>();
      for (Variable v : allVariables)
      {
        LLVMValueRef allocaInst = LLVM.LLVMBuildAlloca(builder, typeHelper.findTemporaryType(v.getType()), v.getName());
        variables.put(v, allocaInst);
      }

      // store the parameter values to the LLVMValueRefs
      for (Parameter p : constructor.getParameters())
      {
        LLVMValueRef llvmParameter = LLVM.LLVMGetParam(llvmFunction, 1 + p.getIndex());
        LLVMValueRef convertedParameter = typeHelper.convertStandardToTemporary(builder, llvmParameter, p.getType());
        LLVM.LLVMBuildStore(builder, convertedParameter, variables.get(p.getVariable()));
      }

      if (!constructor.getCallsDelegateConstructor())
      {
        // for classes which have superclasses, we must call the implicit no-args super() constructor here
        if (typeDefinition instanceof ClassDefinition)
        {
          NamedType superType = ((ClassDefinition) typeDefinition).getSuperType();
          if (superType != null)
          {
            ClassDefinition superClassDefinition = (ClassDefinition) superType.getResolvedTypeDefinition();
            Constructor noArgsSuper = null;
            for (Constructor test : superClassDefinition.getUniqueConstructors())
            {
              if (test.getParameters().length == 0)
              {
                noArgsSuper = test;
                break;
              }
            }
            if (noArgsSuper == null)
            {
              throw new IllegalArgumentException("Missing no-args super() constructor");
            }
            NamedType thisType = new NamedType(false, false, false, typeDefinition);
            LLVMValueRef convertedThis = typeHelper.convertTemporary(builder, landingPadContainer, thisValue, thisType, superType, false, typeParameterAccessor);
            LLVMValueRef[] superConstructorArgs = new LLVMValueRef[] {convertedThis};
            LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "delegateConstructorInvokeContinue");
            LLVM.LLVMBuildInvoke(builder, getConstructorFunction(noArgsSuper), C.toNativePointerArray(superConstructorArgs, false, true), superConstructorArgs.length, invokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
            LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);
          }
        }

        // call the non-static initialiser function, which runs all non-static initialisers and sets the initial values for all of the fields
        // if this constructor calls a delegate constructor then it will be called later on in the block
        LLVMValueRef initialiserFunction = getInitialiserFunction(false);
        LLVMValueRef[] initialiserArgs = new LLVMValueRef[] {thisValue};
        LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "initialiserInvokeContinue");
        LLVM.LLVMBuildInvoke(builder, initialiserFunction, C.toNativePointerArray(initialiserArgs, false, true), initialiserArgs.length, invokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
        LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);
      }

      buildStatement(constructor.getBlock(), VoidType.VOID_TYPE, builder, thisValue, variables, typeParameterAccessor,
                     landingPadContainer, new HashMap<TryStatement, LLVMBasicBlockRef>(), new HashMap<TryStatement, LLVMValueRef>(),
                     new HashMap<TryStatement, List<LLVMBasicBlockRef>>(), new HashMap<BreakableStatement, LLVMBasicBlockRef>(),
                     new HashMap<BreakableStatement, LLVMBasicBlockRef>(), new Runnable()
      {
        @Override
        public void run()
        {
          // this will be run whenever a return void is found
          // so return the result of the constructor, which is always void
          LLVM.LLVMBuildRetVoid(builder);
        }
      });
      // return if control reaches the end of the function
      if (!constructor.getBlock().stopsExecution())
      {
        LLVM.LLVMBuildRetVoid(builder);
      }

      LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
      if (landingPadBlock != null)
      {
        LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
        LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), getPersonalityFunction(), 0, "");
        LLVM.LLVMSetCleanup(landingPad, true);
        LLVM.LLVMBuildResume(builder, landingPad);
      }

      LLVM.LLVMDisposeBuilder(builder);
    }
  }

  private void addPropertyFunctionBodies()
  {
    for (Property property : typeDefinition.getProperties())
    {
      if (property.isAbstract())
      {
        continue;
      }
      addPropertyGetterFunction(property);
      if (!property.isFinal())
      {
        addPropertySetterFunction(property);
      }
      if (property.hasConstructor())
      {
        addPropertyConstructorFunction(property);
      }
    }
  }

  private void addPropertyGetterFunction(Property property)
  {
    LLVMValueRef function = getPropertyGetterFunction(property);
    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(function);

    if (property.getGetterBlock() == null)
    {
      if (property.isUnbacked())
      {
        throw new IllegalArgumentException("Cannot generate a default getter for an unbacked property");
      }
      LLVMValueRef variable;
      if (property.isStatic())
      {
        variable = getGlobal(property.getBackingGlobalVariable());
      }
      else
      {
        LLVMValueRef thisValue = LLVM.LLVMGetParam(function, 0);
        // no need to convert thisValue to anything here, as its type is already the containing type of the property,
        // and it cannot be an interface type, since interfaces cannot have backed properties
        variable = typeHelper.getMemberPointer(builder, thisValue, property.getBackingMemberVariable());
      }
      LLVMValueRef loaded = LLVM.LLVMBuildLoad(builder, variable, "");
      LLVM.LLVMBuildRet(builder, loaded);
      LLVM.LLVMDisposeBuilder(builder);
      return;
    }

    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    // create LLVMValueRefs for all of the variables, including parameters
    Map<Variable, LLVMValueRef> variables = new HashMap<Variable, LLVM.LLVMValueRef>();
    for (Variable v : Resolver.getAllNestedVariables(property.getGetterBlock()))
    {
      LLVMValueRef allocaInst = LLVM.LLVMBuildAlloca(builder, typeHelper.findTemporaryType(v.getType()), v.getName());
      variables.put(v, allocaInst);
    }

    LLVMValueRef thisValue = property.isStatic() ? null : LLVM.LLVMGetParam(function, 0);

    TypeParameterAccessor typeParameterAccessor;
    if (property.isStatic())
    {
      typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper);
    }
    else
    {
      if (property.getContainingTypeDefinition() instanceof InterfaceDefinition)
      {
        // extract the interface's type parameters
        TypeParameter[] typeParameters = property.getContainingTypeDefinition().getTypeParameters();
        Map<TypeParameter, LLVMValueRef> knownTypeParameters = new HashMap<TypeParameter, LLVMValueRef>();
        for (int i = 0; i < typeParameters.length; ++i)
        {
          LLVMValueRef parameterValue = LLVM.LLVMGetParam(function, 1 + i);
          knownTypeParameters.put(typeParameters[i], parameterValue);
        }
        typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper, property.getContainingTypeDefinition(), knownTypeParameters);
      }
      else
      {
        typeParameterAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, property.getContainingTypeDefinition(), thisValue);
      }
    }

    buildStatement(property.getGetterBlock(), property.getType(), builder, thisValue, variables, typeParameterAccessor,
                   landingPadContainer, new HashMap<TryStatement, LLVMBasicBlockRef>(), new HashMap<TryStatement, LLVMValueRef>(),
                   new HashMap<TryStatement, List<LLVMBasicBlockRef>>(), new HashMap<BreakableStatement, LLVMBasicBlockRef>(),
                   new HashMap<BreakableStatement, LLVMBasicBlockRef>(), new Runnable()
    {
      @Override
      public void run()
      {
        throw new IllegalStateException("A property getter cannot return void");
      }
    });
    if (!property.getGetterBlock().stopsExecution())
    {
      throw new IllegalStateException("A property getter must always return a value");
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);
  }

  private void addPropertySetterFunction(Property property)
  {
    LLVMValueRef function = getPropertySetterFunction(property);
    final LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(function);

    if (property.getSetterBlock() == null)
    {
      if (property.isUnbacked())
      {
        throw new IllegalArgumentException("Cannot generate a default setter for an unbacked property");
      }
      LLVMValueRef newValue = LLVM.LLVMGetParam(function, 1);
      LLVMValueRef variable;
      if (property.isStatic())
      {
        variable = getGlobal(property.getBackingGlobalVariable());
      }
      else
      {
        LLVMValueRef thisValue = LLVM.LLVMGetParam(function, 0);
        // no need to convert thisValue to anything here, as its type is already the containing type of the property,
        // and it cannot be an interface type, since interfaces cannot have backed properties
        variable = typeHelper.getMemberPointer(builder, thisValue, property.getBackingMemberVariable());
      }
      LLVM.LLVMBuildStore(builder, newValue, variable);
      LLVM.LLVMBuildRetVoid(builder);
      LLVM.LLVMDisposeBuilder(builder);
      return;
    }

    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    // create LLVMValueRefs for all of the variables, including parameters
    Map<Variable, LLVMValueRef> variables = new HashMap<Variable, LLVM.LLVMValueRef>();
    for (Variable v : Resolver.getAllNestedVariables(property.getSetterBlock()))
    {
      LLVMValueRef allocaInst = LLVM.LLVMBuildAlloca(builder, typeHelper.findTemporaryType(v.getType()), v.getName());
      variables.put(v, allocaInst);
    }

    LLVMValueRef thisValue = property.isStatic() ? null : LLVM.LLVMGetParam(function, 0);

    int argumentOffset;
    TypeParameterAccessor typeParameterAccessor;
    if (property.isStatic())
    {
      typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper);
      argumentOffset = 1;
    }
    else
    {
      if (property.getContainingTypeDefinition() instanceof InterfaceDefinition)
      {
        // extract the interface's type parameters
        TypeParameter[] typeParameters = property.getContainingTypeDefinition().getTypeParameters();
        Map<TypeParameter, LLVMValueRef> knownTypeParameters = new HashMap<TypeParameter, LLVMValueRef>();
        for (int i = 0; i < typeParameters.length; ++i)
        {
          LLVMValueRef parameterValue = LLVM.LLVMGetParam(function, 1 + i);
          knownTypeParameters.put(typeParameters[i], parameterValue);
        }
        typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper, property.getContainingTypeDefinition(), knownTypeParameters);
        argumentOffset = 1 + typeParameters.length;
      }
      else
      {
        typeParameterAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, property.getContainingTypeDefinition(), thisValue);
        argumentOffset = 1;
      }
    }

    // store the parameter's value in its variable
    LLVMValueRef llvmParameter = LLVM.LLVMGetParam(function, argumentOffset);
    LLVMValueRef convertedParameter = typeHelper.convertStandardToTemporary(builder, llvmParameter, property.getType());
    LLVM.LLVMBuildStore(builder, convertedParameter, variables.get(property.getSetterParameter().getVariable()));

    buildStatement(property.getSetterBlock(), property.getType(), builder, thisValue, variables, typeParameterAccessor,
                   landingPadContainer, new HashMap<TryStatement, LLVMBasicBlockRef>(), new HashMap<TryStatement, LLVMValueRef>(),
                   new HashMap<TryStatement, List<LLVMBasicBlockRef>>(), new HashMap<BreakableStatement, LLVMBasicBlockRef>(),
                   new HashMap<BreakableStatement, LLVMBasicBlockRef>(), new Runnable()
    {
      @Override
      public void run()
      {
        // this will be run whenever a return void is found
        // so return void
        LLVM.LLVMBuildRetVoid(builder);
      }
    });
    // add a "ret void" if control reaches the end of the function
    if (!property.getSetterBlock().stopsExecution())
    {
      LLVM.LLVMBuildRetVoid(builder);
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);
  }

  private void addPropertyConstructorFunction(Property property)
  {
    LLVMValueRef function = getPropertyConstructorFunction(property);
    final LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(function);

    Parameter implementationParameter = property.getConstructorParameter();
    Block implementationBlock = property.getConstructorBlock();
    if (implementationBlock == null)
    {
      implementationParameter = property.getSetterParameter();
      implementationBlock = property.getSetterBlock();
    }
    if (implementationBlock == null)
    {
      if (property.isUnbacked())
      {
        throw new IllegalArgumentException("Cannot generate a default constructor for an unbacked property");
      }
      LLVMValueRef newValue = LLVM.LLVMGetParam(function, 1);
      LLVMValueRef variable;
      if (property.isStatic())
      {
        variable = getGlobal(property.getBackingGlobalVariable());
      }
      else
      {
        LLVMValueRef thisValue = LLVM.LLVMGetParam(function, 0);
        // no need to convert thisValue to anything here, as its type is already the containing type of the property,
        // and it cannot be an interface type, since interfaces cannot have backed properties
        variable = typeHelper.getMemberPointer(builder, thisValue, property.getBackingMemberVariable());
      }
      LLVM.LLVMBuildStore(builder, newValue, variable);
      LLVM.LLVMBuildRetVoid(builder);
      LLVM.LLVMDisposeBuilder(builder);
      return;
    }

    LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

    // create LLVMValueRefs for all of the variables, including parameters
    Map<Variable, LLVMValueRef> variables = new HashMap<Variable, LLVM.LLVMValueRef>();
    for (Variable v : Resolver.getAllNestedVariables(implementationBlock))
    {
      LLVMValueRef allocaInst = LLVM.LLVMBuildAlloca(builder, typeHelper.findTemporaryType(v.getType()), v.getName());
      variables.put(v, allocaInst);
    }

    LLVMValueRef thisValue = property.isStatic() ? null : LLVM.LLVMGetParam(function, 0);

    int argumentOffset;
    TypeParameterAccessor typeParameterAccessor;
    if (property.isStatic())
    {
      typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper);
      argumentOffset = 1;
    }
    else
    {
      if (property.getContainingTypeDefinition() instanceof InterfaceDefinition)
      {
        // extract the interface's type parameters
        TypeParameter[] typeParameters = property.getContainingTypeDefinition().getTypeParameters();
        Map<TypeParameter, LLVMValueRef> knownTypeParameters = new HashMap<TypeParameter, LLVMValueRef>();
        for (int i = 0; i < typeParameters.length; ++i)
        {
          LLVMValueRef parameterValue = LLVM.LLVMGetParam(function, 1 + i);
          knownTypeParameters.put(typeParameters[i], parameterValue);
        }
        typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper, property.getContainingTypeDefinition(), knownTypeParameters);
        argumentOffset = 1 + typeParameters.length;
      }
      else
      {
        typeParameterAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, property.getContainingTypeDefinition(), thisValue);
        argumentOffset = 1;
      }
    }

    // store the parameter's value in its variable
    LLVMValueRef llvmParameter = LLVM.LLVMGetParam(function, argumentOffset);
    LLVMValueRef convertedParameter = typeHelper.convertStandardToTemporary(builder, llvmParameter, property.getType());
    LLVM.LLVMBuildStore(builder, convertedParameter, variables.get(implementationParameter.getVariable()));

    buildStatement(implementationBlock, property.getType(), builder, thisValue, variables, typeParameterAccessor,
                   landingPadContainer, new HashMap<TryStatement, LLVMBasicBlockRef>(), new HashMap<TryStatement, LLVMValueRef>(),
                   new HashMap<TryStatement, List<LLVMBasicBlockRef>>(), new HashMap<BreakableStatement, LLVMBasicBlockRef>(),
                   new HashMap<BreakableStatement, LLVMBasicBlockRef>(), new Runnable()
    {
      @Override
      public void run()
      {
        // this will be run whenever a return void is found
        // so return void
        LLVM.LLVMBuildRetVoid(builder);
      }
    });
    // add a "ret void" if control reaches the end of the function
    if (!implementationBlock.stopsExecution())
    {
      LLVM.LLVMBuildRetVoid(builder);
    }

    LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
    if (landingPadBlock != null)
    {
      LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
      LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), getPersonalityFunction(), 0, "");
      LLVM.LLVMSetCleanup(landingPad, true);
      LLVM.LLVMBuildResume(builder, landingPad);
    }

    LLVM.LLVMDisposeBuilder(builder);
  }

  private void addMethodBodies()
  {
    for (Method method : typeDefinition.getAllMethods())
    {
      if (method.isAbstract())
      {
        continue;
      }
      if (method instanceof BuiltinMethod)
      {
        builtinGenerator.generateMethod((BuiltinMethod) method);
        continue;
      }
      LLVMValueRef llvmFunction = getMethodFunction(method);

      // add the native function if the programmer specified one
      if (method.getNativeName() != null)
      {
        if (method.getBlock() == null)
        {
          addNativeDowncallFunction(method, llvmFunction);
        }
        else
        {
          addNativeUpcallFunction(method, llvmFunction);
        }
      }

      if (method.getBlock() == null)
      {
        continue;
      }

      final LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(llvmFunction);

      LandingPadContainer landingPadContainer = new LandingPadContainer(builder);

      // create LLVMValueRefs for all of the variables, including parameters
      Map<Variable, LLVMValueRef> variables = new HashMap<Variable, LLVM.LLVMValueRef>();
      for (Variable v : Resolver.getAllNestedVariables(method.getBlock()))
      {
        LLVMValueRef allocaInst = LLVM.LLVMBuildAlloca(builder, typeHelper.findTemporaryType(v.getType()), v.getName());
        variables.put(v, allocaInst);
      }

      LLVMValueRef thisValue = method.isStatic() ? null : LLVM.LLVMGetParam(llvmFunction, 0);

      TypeParameterAccessor typeParameterAccessor;
      int argumentOffset;
      if (method.isStatic())
      {
        typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper);
        argumentOffset = 1;
      }
      else
      {
        if (method.getContainingTypeDefinition() instanceof InterfaceDefinition)
        {
          // extract the interface's type parameters
          TypeParameter[] typeParameters = method.getContainingTypeDefinition().getTypeParameters();
          Map<TypeParameter, LLVMValueRef> knownTypeParameters = new HashMap<TypeParameter, LLVMValueRef>();
          for (int i = 0; i < typeParameters.length; ++i)
          {
            LLVMValueRef parameterValue = LLVM.LLVMGetParam(llvmFunction, 1 + i);
            knownTypeParameters.put(typeParameters[i], parameterValue);
          }
          typeParameterAccessor = new TypeParameterAccessor(builder, rttiHelper, method.getContainingTypeDefinition(), knownTypeParameters);
          argumentOffset = 1 + typeParameters.length;
        }
        else
        {
          typeParameterAccessor = new TypeParameterAccessor(builder, typeHelper, rttiHelper, method.getContainingTypeDefinition(), thisValue);
          argumentOffset = 1;
        }
      }

      // store the parameter values to the LLVMValueRefs
      for (Parameter p : method.getParameters())
      {
        // find the LLVM parameter, the +1 on the index is to account for the 'this' pointer (or the unused opaque* for static methods)
        LLVMValueRef llvmParameter = LLVM.LLVMGetParam(llvmFunction, argumentOffset + p.getIndex());
        LLVMValueRef convertedParameter = typeHelper.convertStandardToTemporary(builder, llvmParameter, p.getType());
        LLVM.LLVMBuildStore(builder, convertedParameter, variables.get(p.getVariable()));
      }

      buildStatement(method.getBlock(), method.getReturnType(), builder, thisValue, variables, typeParameterAccessor,
                     landingPadContainer, new HashMap<TryStatement, LLVMBasicBlockRef>(), new HashMap<TryStatement, LLVMValueRef>(),
                     new HashMap<TryStatement, List<LLVMBasicBlockRef>>(), new HashMap<BreakableStatement, LLVMBasicBlockRef>(),
                     new HashMap<BreakableStatement, LLVMBasicBlockRef>(), new Runnable()
      {
        @Override
        public void run()
        {
          // this will be run whenever a return void is found
          // so return void
          LLVM.LLVMBuildRetVoid(builder);
        }
      });
      // add a "ret void" if control reaches the end of the function
      if (!method.getBlock().stopsExecution())
      {
        LLVM.LLVMBuildRetVoid(builder);
      }

      LLVMBasicBlockRef landingPadBlock = landingPadContainer.getExistingLandingPadBlock();
      if (landingPadBlock != null)
      {
        LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
        LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), getPersonalityFunction(), 0, "");
        LLVM.LLVMSetCleanup(landingPad, true);
        LLVM.LLVMBuildResume(builder, landingPad);
      }

      LLVM.LLVMDisposeBuilder(builder);
    }
  }

  /**
   * Builds a heap allocation for the specified native type, which calls the out of memory handler if the allocation fails.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param nativePointerType - the type of a pointer to the native type to allocate
   * @return a newly allocated value of type nativePointerType
   */
  public LLVMValueRef buildHeapAllocation(LLVMBuilderRef builder, LLVMTypeRef nativePointerType)
  {
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false)};
    LLVMValueRef sizePtr = LLVM.LLVMBuildGEP(builder, LLVM.LLVMConstNull(nativePointerType), C.toNativePointerArray(indices, false, true), indices.length, "");
    LLVMValueRef sizeInt = LLVM.LLVMBuildPtrToInt(builder, sizePtr, LLVM.LLVMInt32Type(), "");
    LLVMValueRef[] callocArguments = new LLVMValueRef[] {sizeInt, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false)};
    LLVMValueRef memory = LLVM.LLVMBuildCall(builder, callocFunction, C.toNativePointerArray(callocArguments, false, true), callocArguments.length, "");

    LLVMValueRef isNotNull = LLVM.LLVMBuildIsNotNull(builder, memory, "");
    LLVMBasicBlockRef callocContinueBlock = LLVM.LLVMAddBasicBlock(builder, "callocContinue");
    LLVMBasicBlockRef callocFailedBlock = LLVM.LLVMAddBasicBlock(builder, "callocFailed");
    LLVM.LLVMBuildCondBr(builder, isNotNull, callocContinueBlock, callocFailedBlock);
    LLVM.LLVMPositionBuilderAtEnd(builder, callocFailedBlock);
    buildOutOfMemoryHandler(builder);

    LLVM.LLVMPositionBuilderAtEnd(builder, callocContinueBlock);
    return LLVM.LLVMBuildBitCast(builder, memory, nativePointerType, "");
  }

  /**
   * Builds the code to handle an out-of-memory error.
   * Note: this ends the current LLVM basic block.
   * @param builder - the LLVMBuilderRef to build the code with
   */
  public void buildOutOfMemoryHandler(LLVMBuilderRef builder)
  {
    final String outOfMemoryFunctionName = "plinth_outofmemory_abort";
    LLVMValueRef function = LLVM.LLVMGetNamedFunction(module, outOfMemoryFunctionName);
    if (function == null)
    {
      LLVMTypeRef returnType = LLVM.LLVMVoidType();
      LLVMTypeRef[] parameterTypes = new LLVMTypeRef[0];
      LLVMTypeRef functionType = LLVM.LLVMFunctionType(returnType, C.toNativePointerArray(parameterTypes, false, true), parameterTypes.length, false);
      function = LLVM.LLVMAddFunction(module, outOfMemoryFunctionName, functionType);
    }

    LLVMValueRef[] arguments = new LLVMValueRef[0];
    LLVM.LLVMBuildCall(builder, function, C.toNativePointerArray(arguments, false, true), arguments.length, "");
    LLVM.LLVMBuildUnreachable(builder);
  }

  /**
   * Creates a native _Unwind_Exception object holding the specified plinth object.
   * @param builder - the LLVMBuilderRef to build the code with
   * @param throwableObject - the throwable plinth object to create a native exception object for, in a standard type representation
   * @return the pointer to the native _Unwind_Exception created
   */
  public LLVMValueRef buildCreateException(LLVMBuilderRef builder, LLVMValueRef throwableObject)
  {
    final String nativeExceptionCreatorName = "plinth_create_exception";
    LLVMValueRef exceptionCreator = LLVM.LLVMGetNamedFunction(module, nativeExceptionCreatorName);
    if (exceptionCreator == null)
    {
      LLVMTypeRef returnType = LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0);
      LLVMTypeRef[] parameterTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0)};
      LLVMTypeRef functionType = LLVM.LLVMFunctionType(returnType, C.toNativePointerArray(parameterTypes, false, true), parameterTypes.length, false);
      exceptionCreator = LLVM.LLVMAddFunction(module, nativeExceptionCreatorName, functionType);
    }
    throwableObject = LLVM.LLVMBuildBitCast(builder, throwableObject, LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0), "");
    LLVMValueRef[] exceptionCreatorArgs = new LLVMValueRef[] {throwableObject};
    return LLVM.LLVMBuildCall(builder, exceptionCreator, C.toNativePointerArray(exceptionCreatorArgs, false, true), exceptionCreatorArgs.length, "");
  }

  /**
   * Raises the specified exception. The exception should be a pointer to an _Unwind_Exception.
   * Note: this ends the current LLVM basic block.
   * @param builder - the LLVMBuilderRef to build the code with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param unwindException - the pointer to the native _Unwind_Exception to raise
   */
  public void buildThrow(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef unwindException)
  {
    final String nativeThrowFunctionName = "plinth_throw";
    LLVMValueRef throwFunction = LLVM.LLVMGetNamedFunction(module, nativeThrowFunctionName);
    if (throwFunction == null)
    {
      LLVMTypeRef returnType = LLVM.LLVMVoidType();
      LLVMTypeRef[] parameterTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0)};
      LLVMTypeRef functionType = LLVM.LLVMFunctionType(returnType, C.toNativePointerArray(parameterTypes, false, true), parameterTypes.length, false);
      throwFunction = LLVM.LLVMAddFunction(module, nativeThrowFunctionName, functionType);
    }
    LLVMValueRef[] throwArguments = new LLVMValueRef[] {unwindException};
    LLVMBasicBlockRef throwContinueBlock = LLVM.LLVMAddBasicBlock(builder, "throwContinue");
    LLVM.LLVMBuildInvoke(builder, throwFunction, C.toNativePointerArray(throwArguments, false, true), throwArguments.length, throwContinueBlock, landingPadContainer.getLandingPadBlock(), "");

    LLVM.LLVMPositionBuilderAtEnd(builder, throwContinueBlock);
    LLVM.LLVMBuildUnreachable(builder);
  }

  /**
   * Builds code to extract the plinth exception object from the specified unwind-exception.
   * @param builder - the LLVMBuilderRef to build the code with
   * @param unwindExceptionPointer - the _Unwind_Exception pointer to extract the caught plinth object from
   * @return the caught plinth object's pointer, as an i8*
   */
  public LLVMValueRef buildExtractExceptionObject(LLVMBuilderRef builder, LLVMValueRef unwindExceptionPointer)
  {
    final String name = "plinth_extract_exception_object";
    LLVMValueRef extractFunction = LLVM.LLVMGetNamedFunction(module, name);
    if (extractFunction == null)
    {
      LLVMTypeRef[] argumentTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0)};
      LLVMTypeRef resultType = LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0);
      LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(argumentTypes, false, true), argumentTypes.length, false);
      extractFunction = LLVM.LLVMAddFunction(module, name, functionType);
    }
    LLVMValueRef[] arguments = new LLVMValueRef[] {unwindExceptionPointer};
    return LLVM.LLVMBuildCall(builder, extractFunction, C.toNativePointerArray(arguments, false, true), arguments.length, name);
  }

  /**
   * Builds code to destroy the specified native exception object (of type _Unwind_Exception* in C)
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param unwindExceptionPointer - the pointer to the unwind exception to destroy
   */
  public void buildDestroyNativeException(LLVMBuilderRef builder, LLVMValueRef unwindExceptionPointer)
  {
    final String name = "plinth_destroy_exception";
    LLVMValueRef freeFunction = LLVM.LLVMGetNamedFunction(module, name);
    if (freeFunction == null)
    {
      LLVMTypeRef[] argumentTypes = new LLVMTypeRef[] {LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0)};
      LLVMTypeRef resultType = LLVM.LLVMVoidType();
      LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(argumentTypes, false, true), argumentTypes.length, false);
      freeFunction = LLVM.LLVMAddFunction(module, name, functionType);
    }
    LLVMValueRef[] arguments = new LLVMValueRef[] {unwindExceptionPointer};
    LLVM.LLVMBuildCall(builder, freeFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");
  }

  /**
   * Adds a string constant with the specified value, with an LLVM type which can be bitcast to the native type of []ubyte
   * @param value - the value to store in the constant
   * @return the global variable created (a pointer to the constant value)
   */
  public LLVMValueRef addStringConstant(String value)
  {
    byte[] bytes;
    try
    {
      bytes = value.getBytes("UTF-8");
    }
    catch (UnsupportedEncodingException e)
    {
      throw new IllegalStateException("UTF-8 encoding not supported!", e);
    }

    StringBuffer nameBuffer = new StringBuffer("_STR_");
    byte[] hexChars = "0123456789ABCDEF".getBytes();
    for (byte b : bytes)
    {
      if (('a' <= b & b <= 'z') | ('A' <= b & b <= 'Z') | ('0' <= b & b <= '9'))
      {
        nameBuffer.append((char) b);
      }
      else
      {
        nameBuffer.append('_');
        nameBuffer.append((char) hexChars[(b >> 4) & 0xf]);
        nameBuffer.append((char) hexChars[b & 0xf]);
      }
    }
    String mangledName = nameBuffer.toString();

    LLVMValueRef existingGlobal = LLVM.LLVMGetNamedGlobal(module, mangledName);
    if (existingGlobal != null)
    {
      return existingGlobal;
    }

    // build the []ubyte up from the string value, and store it as a global variable
    ArrayType arrayType = new ArrayType(false, true, new PrimitiveType(false, PrimitiveTypeType.UBYTE, null), null);
    LLVMValueRef rttiValue = rttiHelper.getInstanceRTTI(arrayType);
    LLVMValueRef vftValue = virtualFunctionHandler.getBaseChangeObjectVFT(arrayType);
    LLVMValueRef lengthValue = LLVM.LLVMConstInt(typeHelper.findStandardType(ArrayLengthMember.ARRAY_LENGTH_TYPE), bytes.length, false);
    LLVMValueRef getterFunctionValue = typeHelper.getArrayGetterFunction(arrayType);
    LLVMValueRef setterFunctionValue = typeHelper.getArraySetterFunction(arrayType);
    LLVMValueRef constString = LLVM.LLVMConstString(bytes, bytes.length, true);
    LLVMValueRef[] arrayValues = new LLVMValueRef[] {rttiValue, vftValue, lengthValue, getterFunctionValue, setterFunctionValue, constString};
    LLVMValueRef byteArrayStruct = LLVM.LLVMConstStruct(C.toNativePointerArray(arrayValues, false, true), arrayValues.length, false);

    LLVMTypeRef structType = typeHelper.findNonProxiedArrayStructureType(arrayType, bytes.length);

    LLVMValueRef globalVariable = LLVM.LLVMAddGlobal(module, structType, mangledName);
    LLVM.LLVMSetInitializer(globalVariable, byteArrayStruct);
    LLVM.LLVMSetLinkage(globalVariable, LLVM.LLVMLinkage.LLVMLinkOnceODRLinkage);
    LLVM.LLVMSetVisibility(globalVariable, LLVM.LLVMVisibility.LLVMHiddenVisibility);
    LLVM.LLVMSetGlobalConstant(globalVariable, true);
    return globalVariable;
  }

  /**
   * Builds a string creation for the specified string value.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param value - the value of the string to create
   * @return the result of creating the string, in a temporary type representation
   */
  public LLVMValueRef buildStringCreation(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, String value)
  {
    LLVMValueRef globalVariable = addStringConstant(value);
    return buildStringCreation(builder, landingPadContainer, globalVariable);
  }

  /**
   * Builds a string creation for the specified []ubyte value.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param ubyteArrayValue - the value of the string to create
   * @return the result of creating the string, in a temporary type representation
   */
  public LLVMValueRef buildStringCreation(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef ubyteArrayValue)
  {
    // extract the string([]ubyte) constructor from the type of this expression
    LLVMValueRef constructorFunction = getConstructorFunction(SpecialTypeHandler.stringArrayConstructor);
    LLVMTypeRef ubyteArrayType = typeHelper.findStandardType(new ArrayType(false, false, new PrimitiveType(false, PrimitiveTypeType.UBYTE, null), null));
    LLVMValueRef bitcastedArray = LLVM.LLVMBuildBitCast(builder, ubyteArrayValue, ubyteArrayType, "");

    // find the type to alloca, which is the standard representation of a non-nullable version of this type
    // when we alloca this type, it becomes equivalent to the temporary type representation of this compound type (with any nullability)
    LLVMTypeRef allocaBaseType = typeHelper.findStandardType(SpecialTypeHandler.STRING_TYPE);
    LLVMValueRef alloca = LLVM.LLVMBuildAllocaInEntryBlock(builder, allocaBaseType, "");
    typeHelper.initialiseCompoundType(builder, SpecialTypeHandler.STRING_TYPE, alloca, null);

    LLVMValueRef[] arguments = new LLVMValueRef[] {alloca, bitcastedArray};
    LLVMBasicBlockRef stringInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "stringCreationInvokeContinue");
    LLVM.LLVMBuildInvoke(builder, constructorFunction, C.toNativePointerArray(arguments, false, true), arguments.length, stringInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, stringInvokeContinueBlock);
    return alloca;
  }

  /**
   * Builds a string concatenation for the specified strings.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param strings - the array of strings to concatenate, each in a standard type representation
   * @return the result of concatenating the two strings, in a temporary type representation
   */
  public LLVMValueRef buildStringConcatenation(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef... strings)
  {
    if (strings.length < 2)
    {
      throw new IllegalArgumentException("Cannot concatenate less than two strings");
    }
    // concatenate the strings
    // find the type to alloca, which is the standard representation of a non-nullable version of this type
    // when we alloca this type, it becomes equivalent to the temporary type representation of this compound type (with any nullability)
    LLVMTypeRef allocaBaseType = typeHelper.findStandardType(SpecialTypeHandler.STRING_TYPE);
    LLVMValueRef alloca = LLVM.LLVMBuildAllocaInEntryBlock(builder, allocaBaseType, "");
    typeHelper.initialiseCompoundType(builder, SpecialTypeHandler.STRING_TYPE, alloca, null);

    if (strings.length == 2)
    {
      // call the string(string, string) constructor
      LLVMValueRef[] arguments = new LLVMValueRef[] {alloca, strings[0], strings[1]};
      LLVMValueRef concatenationConstructor = getConstructorFunction(SpecialTypeHandler.stringConcatenationConstructor);
      LLVMBasicBlockRef concatenationInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "concatenateInvokeContinue");
      LLVM.LLVMBuildInvoke(builder, concatenationConstructor, C.toNativePointerArray(arguments, false, true), arguments.length, concatenationInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
      LLVM.LLVMPositionBuilderAtEnd(builder, concatenationInvokeContinueBlock);
      return alloca;
    }

    LLVMValueRef arrayLength = LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), strings.length, false);
    ArrayType arrayType = new ArrayType(false, false, SpecialTypeHandler.STRING_TYPE, null);
    LLVMValueRef array = buildArrayCreation(builder, landingPadContainer, new LLVMValueRef[] {arrayLength}, arrayType, null, null, false, null);

    for (int i = 0; i < strings.length; ++i)
    {
      LLVMValueRef index = LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), i, false);
      typeHelper.buildStoreArrayElement(builder, landingPadContainer, array, index, strings[i]);
    }

    LLVMValueRef[] arguments = new LLVMValueRef[] {alloca, typeHelper.convertTemporaryToStandard(builder, array, arrayType)};
    LLVMValueRef concatenationConstructor = getConstructorFunction(SpecialTypeHandler.stringArrayConcatenationConstructor);
    LLVMBasicBlockRef concatenationInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "concatenateInvokeContinue");
    LLVM.LLVMBuildInvoke(builder, concatenationConstructor, C.toNativePointerArray(arguments, false, true), arguments.length, concatenationInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
    LLVM.LLVMPositionBuilderAtEnd(builder, concatenationInvokeContinueBlock);
    return alloca;
  }

  /**
   * Finds the C "strlen" function, which can be used to find the length of a C string, such as the arguments to main()
   * @return the C "strlen" function
   */
  public LLVMValueRef getStrLenFunction()
  {
    final String name = "strlen";
    LLVMValueRef existingFunction = LLVM.LLVMGetNamedFunction(module, name);
    if (existingFunction != null)
    {
      return existingFunction;
    }
    LLVMTypeRef[] strlenParameters = new LLVMTypeRef[] {LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0)};
    LLVMTypeRef strlenFunctionType = LLVM.LLVMFunctionType(LLVM.LLVMInt32Type(), C.toNativePointerArray(strlenParameters, false, true), strlenParameters.length, false);
    return LLVM.LLVMAddFunction(module, name, strlenFunctionType);
  }

  /**
   * Generates a main method for the "static uint main([]string)" method in the TypeDefinition we are generating.
   */
  public void generateMainMethod()
  {
    Type argsType = new ArrayType(false, false, SpecialTypeHandler.STRING_TYPE, null);
    Method mainMethod = null;
    for (Method method : typeDefinition.getAllMethods())
    {
      if (method.isStatic() && method.getName().equals(SpecialTypeHandler.MAIN_METHOD_NAME) && method.getReturnType().isRuntimeEquivalent(new PrimitiveType(false, PrimitiveTypeType.UINT, null)))
      {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 1 && parameters[0].getType().isRuntimeEquivalent(argsType))
        {
          mainMethod = method;
          break;
        }
      }
    }
    if (mainMethod == null)
    {
      throw new IllegalArgumentException("Could not find main method in " + typeDefinition.getQualifiedName());
    }
    LLVMValueRef languageMainFunction = getMethodFunction(mainMethod);

    // define main
    LLVMTypeRef argvType = LLVM.LLVMPointerType(LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0), 0);
    LLVMTypeRef[] paramTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), argvType};
    LLVMTypeRef returnType = LLVM.LLVMInt32Type();
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(returnType, C.toNativePointerArray(paramTypes, false, true), paramTypes.length, false);

    LLVMValueRef mainFunction = LLVM.LLVMAddFunction(module, "main", functionType);
    LLVMBuilderRef builder = LLVM.LLVMCreateFunctionBuilder(mainFunction);
    LLVMBasicBlockRef finalBlock = LLVM.LLVMAddBasicBlock(builder, "startProgram");
    LLVMBasicBlockRef argvLoopEndBlock = LLVM.LLVMAddBasicBlock(builder, "argvCopyLoopEnd");
    LLVMBasicBlockRef stringLoopBlock = LLVM.LLVMAddBasicBlock(builder, "stringCopyLoop");
    LLVMBasicBlockRef argvLoopBlock = LLVM.LLVMAddBasicBlock(builder, "argvCopyLoop");

    LLVMValueRef argc = LLVM.LLVMGetParam(mainFunction, 0);
    LLVM.LLVMSetValueName(argc, "argc");
    LLVMValueRef argv = LLVM.LLVMGetParam(mainFunction, 1);
    LLVM.LLVMSetValueName(argv, "argv");

    // create the final args array
    ArrayType stringArrayType = new ArrayType(false, false, SpecialTypeHandler.STRING_TYPE, null);
    LLVMTypeRef llvmStringArrayType = LLVM.LLVMPointerType(typeHelper.findNonProxiedArrayStructureType(stringArrayType, 0), 0);
    // find the element of our array at index argc (i.e. one past the end of the array), which gives us our size
    LLVMValueRef llvmArraySize = typeHelper.getNonProxiedArrayElementPointer(builder, LLVM.LLVMConstNull(llvmStringArrayType), argc);
    LLVMValueRef llvmSize = LLVM.LLVMBuildPtrToInt(builder, llvmArraySize, LLVM.LLVMInt32Type(), "");
    LLVMValueRef[] callocArgs = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false), llvmSize};
    LLVMValueRef stringArray = LLVM.LLVMBuildCall(builder, callocFunction, C.toNativePointerArray(callocArgs, false, true), callocArgs.length, "");

    LLVMValueRef stringArrayIsNotNull = LLVM.LLVMBuildIsNotNull(builder, stringArray, "");
    LLVMBasicBlockRef stringArrayCallocContinueBlock = LLVM.LLVMAddBasicBlock(builder, "stringArrayCallocContinue");
    LLVMBasicBlockRef stringArrayCallocFailedBlock = LLVM.LLVMAddBasicBlock(builder, "stringArrayCallocFailed");
    LLVM.LLVMBuildCondBr(builder, stringArrayIsNotNull, stringArrayCallocContinueBlock, stringArrayCallocFailedBlock);
    LLVM.LLVMPositionBuilderAtEnd(builder, stringArrayCallocFailedBlock);
    buildOutOfMemoryHandler(builder);

    LLVM.LLVMPositionBuilderAtEnd(builder, stringArrayCallocContinueBlock);
    stringArray = LLVM.LLVMBuildBitCast(builder, stringArray, llvmStringArrayType, "");

    LLVMValueRef stringsRTTI = rttiHelper.getInstanceRTTI(stringArrayType);
    LLVMValueRef stringsRTTIPointer = rttiHelper.getRTTIPointer(builder, stringArray);
    LLVM.LLVMBuildStore(builder, stringsRTTI, stringsRTTIPointer);
    LLVMValueRef stringsVFT = virtualFunctionHandler.getBaseChangeObjectVFT(stringArrayType);
    LLVMValueRef stringsVFTPointer = virtualFunctionHandler.getFirstVirtualFunctionTablePointer(builder, stringArray);
    LLVM.LLVMBuildStore(builder, stringsVFT, stringsVFTPointer);
    LLVMValueRef stringsLengthPointer = typeHelper.getArrayLengthPointer(builder, stringArray);
    LLVM.LLVMBuildStore(builder, argc, stringsLengthPointer);
    LLVMValueRef stringsArrayGetterFunction = typeHelper.getArrayGetterFunction(stringArrayType);
    LLVMValueRef stringsArrayGetterFunctionPointer = typeHelper.getArrayGetterFunctionPointer(builder, stringArray);
    LLVM.LLVMBuildStore(builder, stringsArrayGetterFunction, stringsArrayGetterFunctionPointer);
    LLVMValueRef stringsArraySetterFunction = typeHelper.getArraySetterFunction(stringArrayType);
    LLVMValueRef stringsArraySetterFunctionPointer = typeHelper.getArraySetterFunctionPointer(builder, stringArray);
    LLVM.LLVMBuildStore(builder, stringsArraySetterFunction, stringsArraySetterFunctionPointer);

    // branch to the argv-copying loop
    LLVMValueRef initialArgvLoopCheck = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntNE, argc, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false), "");
    LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVM.LLVMBuildCondBr(builder, initialArgvLoopCheck, argvLoopBlock, finalBlock);
    LLVM.LLVMPositionBuilderAtEnd(builder, argvLoopBlock);

    LLVMValueRef argvIndex = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt32Type(), "");
    LLVMValueRef[] charArrayIndices = new LLVMValueRef[] {argvIndex};
    LLVMValueRef charArrayPointer = LLVM.LLVMBuildGEP(builder, argv, C.toNativePointerArray(charArrayIndices, false, true), charArrayIndices.length, "");
    LLVMValueRef charArray = LLVM.LLVMBuildLoad(builder, charArrayPointer, "");

    // call strlen(argv[argvIndex])
    LLVMValueRef[] strlenArgs = new LLVMValueRef[] {charArray};
    LLVMValueRef argLength = LLVM.LLVMBuildCall(builder, getStrLenFunction(), C.toNativePointerArray(strlenArgs, false, true), strlenArgs.length, "");

    // allocate the []ubyte to contain this argument
    ArrayType ubyteArrayType = new ArrayType(false, true, new PrimitiveType(false, PrimitiveTypeType.UBYTE, null), null);
    LLVMTypeRef llvmUbyteArrayType = LLVM.LLVMPointerType(typeHelper.findNonProxiedArrayStructureType(ubyteArrayType, 0), 0);
    // find the element of our array at index argLength (i.e. one past the end of the array), which gives us our size
    LLVMValueRef llvmUbyteArraySize = typeHelper.getNonProxiedArrayElementPointer(builder, LLVM.LLVMConstNull(llvmUbyteArrayType), argLength);
    LLVMValueRef llvmUbyteSize = LLVM.LLVMBuildPtrToInt(builder, llvmUbyteArraySize, LLVM.LLVMInt32Type(), "");
    LLVMValueRef[] ubyteCallocArgs = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false), llvmUbyteSize};
    LLVMValueRef bytes = LLVM.LLVMBuildCall(builder, callocFunction, C.toNativePointerArray(ubyteCallocArgs, false, true), ubyteCallocArgs.length, "");

    LLVMValueRef bytesIsNotNull = LLVM.LLVMBuildIsNotNull(builder, bytes, "");
    LLVMBasicBlockRef bytesCallocContinueBlock = LLVM.LLVMAddBasicBlock(builder, "bytesCallocContinue");
    LLVMBasicBlockRef bytesCallocFailedBlock = LLVM.LLVMAddBasicBlock(builder, "bytesCallocFailed");
    LLVM.LLVMBuildCondBr(builder, bytesIsNotNull, bytesCallocContinueBlock, bytesCallocFailedBlock);
    LLVM.LLVMPositionBuilderAtEnd(builder, bytesCallocFailedBlock);
    buildOutOfMemoryHandler(builder);

    LLVM.LLVMPositionBuilderAtEnd(builder, bytesCallocContinueBlock);
    bytes = LLVM.LLVMBuildBitCast(builder, bytes, llvmUbyteArrayType, "");

    LLVMValueRef bytesRTTI = rttiHelper.getInstanceRTTI(ubyteArrayType);
    LLVMValueRef bytesRTTIPointer = rttiHelper.getRTTIPointer(builder, bytes);
    LLVM.LLVMBuildStore(builder, bytesRTTI, bytesRTTIPointer);
    LLVMValueRef bytesVFT = virtualFunctionHandler.getBaseChangeObjectVFT(ubyteArrayType);
    LLVMValueRef bytesVFTPointer = virtualFunctionHandler.getFirstVirtualFunctionTablePointer(builder, bytes);
    LLVM.LLVMBuildStore(builder, bytesVFT, bytesVFTPointer);
    LLVMValueRef bytesLengthPointer = typeHelper.getArrayLengthPointer(builder, bytes);
    LLVM.LLVMBuildStore(builder, argLength, bytesLengthPointer);
    LLVMValueRef ubyteArrayGetterFunction = typeHelper.getArrayGetterFunction(ubyteArrayType);
    LLVMValueRef ubyteArrayGetterFunctionPointer = typeHelper.getArrayGetterFunctionPointer(builder, bytes);
    LLVM.LLVMBuildStore(builder, ubyteArrayGetterFunction, ubyteArrayGetterFunctionPointer);
    LLVMValueRef ubyteArraySetterFunction = typeHelper.getArraySetterFunction(ubyteArrayType);
    LLVMValueRef ubyteArraySetterFunctionPointer = typeHelper.getArraySetterFunctionPointer(builder, bytes);
    LLVM.LLVMBuildStore(builder, ubyteArraySetterFunction, ubyteArraySetterFunctionPointer);

    // branch to the character copying loop
    LLVMValueRef initialBytesLoopCheck = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntNE, argLength, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false), "");
    LLVMBasicBlockRef beforeStringLoopBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVM.LLVMBuildCondBr(builder, initialBytesLoopCheck, stringLoopBlock, argvLoopEndBlock);
    LLVM.LLVMPositionBuilderAtEnd(builder, stringLoopBlock);

    // copy the character
    LLVMValueRef characterIndex = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt32Type(), "");
    LLVMValueRef[] inPointerIndices = new LLVMValueRef[] {characterIndex};
    LLVMValueRef inPointer = LLVM.LLVMBuildGEP(builder, charArray, C.toNativePointerArray(inPointerIndices, false, true), inPointerIndices.length, "");
    LLVMValueRef character = LLVM.LLVMBuildLoad(builder, inPointer, "");
    LLVMValueRef outPointer = typeHelper.getNonProxiedArrayElementPointer(builder, bytes, characterIndex);
    LLVM.LLVMBuildStore(builder, character, outPointer);

    // update the character index, and branch
    LLVMValueRef incCharacterIndex = LLVM.LLVMBuildAdd(builder, characterIndex, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false), "");
    LLVMValueRef bytesLoopCheck = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntNE, incCharacterIndex, argLength, "");
    LLVMBasicBlockRef endStringLoopBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVM.LLVMBuildCondBr(builder, bytesLoopCheck, stringLoopBlock, argvLoopEndBlock);

    // add the incomings for the character index
    LLVMValueRef[] bytesLoopPhiValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false), incCharacterIndex};
    LLVMBasicBlockRef[] bytesLoopPhiBlocks = new LLVMBasicBlockRef[] {beforeStringLoopBlock, endStringLoopBlock};
    LLVM.LLVMAddIncoming(characterIndex, C.toNativePointerArray(bytesLoopPhiValues, false, true), C.toNativePointerArray(bytesLoopPhiBlocks, false, true), bytesLoopPhiValues.length);

    // build the end of the string creation loop
    LLVM.LLVMPositionBuilderAtEnd(builder, argvLoopEndBlock);
    LLVMValueRef stringArrayElementPointer = typeHelper.getNonProxiedArrayElementPointer(builder, stringArray, argvIndex);
    typeHelper.initialiseCompoundType(builder, SpecialTypeHandler.STRING_TYPE, stringArrayElementPointer, null);
    LLVMValueRef convertedBytes = LLVM.LLVMBuildBitCast(builder, bytes, typeHelper.findStandardType(ubyteArrayType), "");
    LLVMValueRef[] stringCreationArgs = new LLVMValueRef[] {stringArrayElementPointer, convertedBytes};
    LLVMBasicBlockRef landingPadBlock = LLVM.LLVMAppendBasicBlock(LLVM.LLVMGetBasicBlockParent(LLVM.LLVMGetInsertBlock(builder)), "landingPad");
    LLVMBasicBlockRef stringInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "stringCreationInvokeContinue");
    LLVM.LLVMBuildInvoke(builder, getConstructorFunction(SpecialTypeHandler.stringArrayConstructor), C.toNativePointerArray(stringCreationArgs, false, true), stringCreationArgs.length, stringInvokeContinueBlock, landingPadBlock, "");
    LLVM.LLVMPositionBuilderAtEnd(builder, stringInvokeContinueBlock);

    // update the argv index, and branch
    LLVMValueRef incArgvIndex = LLVM.LLVMBuildAdd(builder, argvIndex, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false), "");
    LLVMValueRef argvLoopCheck = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntNE, incArgvIndex, argc, "");
    LLVMBasicBlockRef endArgvLoopEndBlock = LLVM.LLVMGetInsertBlock(builder);
    LLVM.LLVMBuildCondBr(builder, argvLoopCheck, argvLoopBlock, finalBlock);

    // add the incomings for the argv index
    LLVMValueRef[] argvLoopPhiValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false), incArgvIndex};
    LLVMBasicBlockRef[] argvLoopPhiBlocks = new LLVMBasicBlockRef[] {startBlock, endArgvLoopEndBlock};
    LLVM.LLVMAddIncoming(argvIndex, C.toNativePointerArray(argvLoopPhiValues, false, true), C.toNativePointerArray(argvLoopPhiBlocks, false, true), argvLoopPhiValues.length);

    // build the actual main function call
    LLVM.LLVMPositionBuilderAtEnd(builder, finalBlock);
    LLVMValueRef convertedStringArray = LLVM.LLVMBuildBitCast(builder, stringArray, typeHelper.findStandardType(stringArrayType), "");
    LLVMValueRef[] arguments = new LLVMValueRef[] {LLVM.LLVMConstNull(typeHelper.getOpaquePointer()), convertedStringArray};
    LLVMBasicBlockRef mainInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "mainInvokeContinue");
    LLVMValueRef returnCode = LLVM.LLVMBuildInvoke(builder, languageMainFunction, C.toNativePointerArray(arguments, false, true), arguments.length, mainInvokeContinueBlock, landingPadBlock, "");
    LLVM.LLVMPositionBuilderAtEnd(builder, mainInvokeContinueBlock);
    LLVM.LLVMBuildRet(builder, returnCode);

    // build the landing pad
    LLVM.LLVMPositionBuilderAtEnd(builder, landingPadBlock);
    LLVMValueRef landingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), getPersonalityFunction(), 0, "");
    LLVM.LLVMSetCleanup(landingPad, true);
    LLVM.LLVMBuildResume(builder, landingPad);

    LLVM.LLVMDisposeBuilder(builder);
  }

  private void buildStatement(Statement statement, Type returnType, LLVMBuilderRef builder, LLVMValueRef thisValue, Map<Variable, LLVMValueRef> variables, TypeParameterAccessor typeParameterAccessor,
                              LandingPadContainer landingPadContainer, Map<TryStatement, LLVMBasicBlockRef> finallyBlocks, Map<TryStatement, LLVMValueRef> finallyJumpVariables,
                              Map<TryStatement, List<LLVMBasicBlockRef>> finallyJumpBlocks, Map<BreakableStatement, LLVMBasicBlockRef> breakBlocks, Map<BreakableStatement, LLVMBasicBlockRef> continueBlocks, Runnable returnVoidCallback)
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assignStatement = (AssignStatement) statement;
      Assignee[] assignees = assignStatement.getAssignees();

      // for each assignee, there are five possible ways we can populate these array elements:
      // * only assigneePointers[i]: a local variable
      // * assigneePointers[i] and fieldReferences[i]: a field
      // * assigneePointers[i] and propertyReferences[i]: a property backing variable
      // * propertyReferences[i], propertyCallees[i], propertyCalleeTypes[i], and propertyIsConstructors[i]: a normal property reference
      // * arrayAssignees[i] and arrayAssigneeIndices[i]: an array element
      LLVMValueRef[] assigneePointers = new LLVMValueRef[assignees.length];
      FieldReference[] fieldReferences = new FieldReference[assignees.length];
      PropertyReference[] propertyReferences = new PropertyReference[assignees.length];

      LLVMValueRef[] propertyCallees = new LLVMValueRef[assignees.length];
      Type[] propertyCalleeTypes = new Type[assignees.length];
      boolean[] propertyIsConstructors = new boolean[assignees.length];

      LLVMValueRef[] arrayAssignees = new LLVMValueRef[assignees.length];
      LLVMValueRef[] arrayAssigneeIndices = new LLVMValueRef[assignees.length];

      for (int i = 0; i < assignees.length; i++)
      {
        if (assignees[i] instanceof VariableAssignee)
        {
          Variable resolvedVariable = ((VariableAssignee) assignees[i]).getResolvedVariable();
          if (resolvedVariable instanceof MemberVariable)
          {
            MemberVariable memberVariable = (MemberVariable) resolvedVariable;
            assigneePointers[i] = typeHelper.getMemberPointer(builder, thisValue, memberVariable);
            if (memberVariable.getField() != null)
            {
              fieldReferences[i] = new FieldReference(memberVariable.getField(), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
            }
            else if (memberVariable.getProperty() != null)
            {
              propertyReferences[i] = new PropertyReference(memberVariable.getProperty(), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
            }
            else
            {
              throw new IllegalArgumentException("Unknown sort of MemberVariable: not a field or a property backing variable");
            }
          }
          else if (resolvedVariable instanceof GlobalVariable)
          {
            GlobalVariable globalVariable = (GlobalVariable) resolvedVariable;
            assigneePointers[i] = getGlobal(globalVariable);
            if (globalVariable.getField() != null)
            {
              fieldReferences[i] = new FieldReference(globalVariable.getField(), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
            }
            else if (globalVariable.getProperty() != null)
            {
              propertyReferences[i] = new PropertyReference(globalVariable.getProperty(), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
            }
            else
            {
              throw new IllegalArgumentException("Unknown sort of GlobalVariable: not a field or a property backing variable");
            }
          }
          else if (resolvedVariable instanceof PropertyPseudoVariable)
          {
            Property property = ((PropertyPseudoVariable) resolvedVariable).getProperty();
            propertyReferences[i] = new PropertyReference(property, GenericTypeSpecialiser.IDENTITY_SPECIALISER);
            propertyIsConstructors[i] = ((VariableAssignee) assignees[i]).isPropertyConstructorCall();
            if (property.isStatic())
            {
              propertyCallees[i] = LLVM.LLVMConstNull(typeHelper.getOpaquePointer());
              propertyCalleeTypes[i] = null;
            }
            else
            {
              propertyCallees[i] = thisValue;
              propertyCalleeTypes[i] = new NamedType(false, false, false, typeDefinition);
            }
          }
          else
          {
            assigneePointers[i] = variables.get(resolvedVariable);
          }
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          arrayAssignees[i] = buildExpression(arrayElementAssignee.getArrayExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
          LLVMValueRef dimension = buildExpression(arrayElementAssignee.getDimensionExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
          arrayAssigneeIndices[i] = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, dimension, arrayElementAssignee.getDimensionExpression().getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, typeParameterAccessor);
        }
        else if (assignees[i] instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];
          FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
          if (fieldAccessExpression.getResolvedMemberReference() instanceof FieldReference)
          {
            FieldReference fieldReference = (FieldReference) fieldAccessExpression.getResolvedMemberReference();
            Field field = fieldReference.getReferencedMember();
            fieldReferences[i] = fieldReference;
            if (field.isStatic())
            {
              assigneePointers[i] = getGlobal(field.getGlobalVariable());
            }
            else
            {
              LLVMValueRef expressionValue = buildExpression(fieldAccessExpression.getBaseExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
              assigneePointers[i] = typeHelper.getMemberPointer(builder, expressionValue, field.getMemberVariable());
            }
          }
          else if (fieldAccessExpression.getResolvedMemberReference() instanceof PropertyReference)
          {
            PropertyReference propertyReference = (PropertyReference) fieldAccessExpression.getResolvedMemberReference();
            Property property = propertyReference.getReferencedMember();
            propertyReferences[i] = propertyReference;
            propertyIsConstructors[i] = fieldAssignee.isPropertyConstructorCall();
            if (property.isStatic())
            {
              propertyCallees[i] = LLVM.LLVMConstNull(typeHelper.getOpaquePointer());
              propertyCalleeTypes[i] = null;
            }
            else
            {
              propertyCallees[i] = buildExpression(fieldAccessExpression.getBaseExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
              propertyCalleeTypes[i] = fieldAccessExpression.getBaseExpression().getType();
            }
          }
          else
          {
            throw new IllegalArgumentException("Unknown member assigned to in a FieldAssignee: " + fieldAccessExpression.getResolvedMemberReference());
          }
        }
        else if (assignees[i] instanceof BlankAssignee)
        {
          // this assignee doesn't actually get assigned to, so don't set this element in any of the arrays (leave it as null)
        }
        else
        {
          throw new IllegalStateException("Unknown Assignee type: " + assignees[i]);
        }
      }

      if (assignStatement.getExpression() != null)
      {
        LLVMValueRef value = buildExpression(assignStatement.getExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        Type[] expressionSubTypes = null;
        if (assignees.length != 1)
        {
          if (assignStatement.getResolvedType().canBeNullable())
          {
            throw new IllegalStateException("An assign statement's type cannot be nullable if it is about to be split into multiple assignees");
          }
          expressionSubTypes = ((TupleType) assignStatement.getExpression().getType()).getSubTypes();
        }
        for (int i = 0; i < assignees.length; ++i)
        {
          if (assigneePointers[i] == null && propertyReferences[i] == null && arrayAssignees[i] == null)
          {
            continue;
          }
          LLVMValueRef extractedValue;
          Type extractedType;
          if (assignees.length == 1)
          {
            extractedValue = value;
            extractedType = assignStatement.getExpression().getType();
          }
          else
          {
            // we have already made sure that the tuple is not nullable, so extract this sub-value from it
            extractedValue = LLVM.LLVMBuildExtractValue(builder, value, i, "");
            extractedType = expressionSubTypes[i];
          }

          LLVMValueRef convertedValue = typeHelper.convertTemporary(builder, landingPadContainer, extractedValue, extractedType, assignees[i].getResolvedType(), false, typeParameterAccessor);
          if (assigneePointers[i] != null)
          {
            if (fieldReferences[i] != null)
            {
              // we are storing something in a field, so convert from the reference type that we currently have to the underlying field's type
              FieldReference fieldReference = fieldReferences[i];
              Field field = fieldReference.getReferencedMember();
              GenericTypeSpecialiser genericTypeSpecialiser = GenericTypeSpecialiser.IDENTITY_SPECIALISER;
              if (fieldReference.getContainingType() != null)
              {
                genericTypeSpecialiser = new GenericTypeSpecialiser(fieldReference.getContainingType());
              }
              Type contextualisedType = typeHelper.getContextualisedActualMemberType(field.getType(), genericTypeSpecialiser);
              convertedValue = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, convertedValue, assignees[i].getResolvedType(), contextualisedType, typeParameterAccessor);
            }
            else if (propertyReferences[i] != null)
            {
              // we are storing something in a property backing variable, so convert from the reference type that we currently have to the underlying property's type
              PropertyReference propertyReference = propertyReferences[i];
              Property property = propertyReference.getReferencedMember();
              GenericTypeSpecialiser genericTypeSpecialiser = GenericTypeSpecialiser.IDENTITY_SPECIALISER;
              if (propertyReference.getContainingType() != null)
              {
                genericTypeSpecialiser = new GenericTypeSpecialiser(propertyReference.getContainingType());
              }
              Type contextualisedType = typeHelper.getContextualisedActualMemberType(property.getType(), genericTypeSpecialiser);
              convertedValue = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, convertedValue, assignees[i].getResolvedType(), contextualisedType, typeParameterAccessor);
            }
            else
            {
              // on variable assignment, we need to make sure compound types do not result in an alias to an existing variable, so convert to standard and back again
              convertedValue = typeHelper.convertTemporaryToStandard(builder, convertedValue, assignees[i].getResolvedType());
              convertedValue = typeHelper.convertStandardToTemporary(builder, convertedValue, assignees[i].getResolvedType());
            }
            LLVM.LLVMBuildStore(builder, convertedValue, assigneePointers[i]);
          }
          else if (propertyReferences[i] != null)
          {
            // note: no conversion is required, because the assignee's resolvedType is equivalent to the PropertyReference's type
            MemberFunctionType memberFunctionType = propertyIsConstructors[i] ? MemberFunctionType.PROPERTY_CONSTRUCTOR : MemberFunctionType.PROPERTY_SETTER;
            typeHelper.buildPropertySetterConstructorFunctionCall(builder, landingPadContainer, propertyCallees[i], propertyCalleeTypes[i], convertedValue, propertyReferences[i], memberFunctionType, typeParameterAccessor);
          }
          else // if (assigneeArrays[i] != null)
          {
            // note: no conversion is required, because the assignee's resolvedType is equivalent to the array's base type
            convertedValue = typeHelper.convertTemporaryToStandard(builder, convertedValue, assignees[i].getResolvedType());
            typeHelper.buildStoreArrayElement(builder, landingPadContainer, arrayAssignees[i], arrayAssigneeIndices[i], convertedValue);
          }
        }
      }
    }
    else if (statement instanceof Block)
    {
      for (Statement s : ((Block) statement).getStatements())
      {
        buildStatement(s, returnType, builder, thisValue, variables, typeParameterAccessor, landingPadContainer, finallyBlocks, finallyJumpVariables, finallyJumpBlocks, breakBlocks, continueBlocks, returnVoidCallback);
      }
    }
    else if (statement instanceof BreakStatement)
    {
      BreakStatement breakStatement = (BreakStatement) statement;
      LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);

      LLVMBasicBlockRef lastBlock;
      List<TryStatement> breakFinallyBlocks = breakStatement.getResolvedFinallyBlocks();
      // if this is null, it means that we break through a finally that stops execution
      if (breakStatement.getResolvedBreakable() != null)
      {
        lastBlock = breakBlocks.get(breakStatement.getResolvedBreakable());
      }
      else
      {
        TryStatement lastFinally = breakFinallyBlocks.remove(breakFinallyBlocks.size() - 1);
        lastBlock = finallyBlocks.get(lastFinally);
      }
      if (lastBlock == null)
      {
        throw new IllegalStateException("Break statement leads to a null block during code generation: " + statement);
      }

      // work our way up from the last finally block to the break statement
      LLVMBasicBlockRef nextBlock = lastBlock;
      while (!breakFinallyBlocks.isEmpty())
      {
        TryStatement currentFinally = breakFinallyBlocks.remove(breakFinallyBlocks.size() - 1);
        LLVMBasicBlockRef currentBlock = finallyBlocks.get(currentFinally);
        if (currentBlock == null)
        {
          throw new IllegalStateException("Break statement leads to a null block during code generation: " + statement);
        }

        // this finally block doesn't stop execution, so tell it to jump to nextBlock after it finishes
        LLVMBasicBlockRef indirectionBlock = LLVM.LLVMAddBasicBlock(builder, "breakFinallyIndirection");
        LLVM.LLVMPositionBuilderAtEnd(builder, indirectionBlock);

        List<LLVMBasicBlockRef> finallyJumpBlockList = finallyJumpBlocks.get(currentFinally);
        int jumpIndex = finallyJumpBlockList.size();
        finallyJumpBlockList.add(nextBlock);

        LLVMValueRef finallyJumpVariable = finallyJumpVariables.get(currentFinally);
        LLVM.LLVMBuildStore(builder, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), jumpIndex, false), finallyJumpVariable);
        LLVM.LLVMBuildBr(builder, currentBlock);

        nextBlock = indirectionBlock;
      }

      // branch to the next block in the chain
      LLVM.LLVMPositionBuilderAtEnd(builder, startBlock);
      LLVM.LLVMBuildBr(builder, nextBlock);
    }
    else if (statement instanceof ContinueStatement)
    {
      ContinueStatement continueStatement = (ContinueStatement) statement;
      LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);

      LLVMBasicBlockRef lastBlock;
      List<TryStatement> continueFinallyBlocks = continueStatement.getResolvedFinallyBlocks();
      // if this is null, it means that we continue through a finally that stops execution
      if (continueStatement.getResolvedBreakable() != null)
      {
        lastBlock = continueBlocks.get(continueStatement.getResolvedBreakable());
      }
      else
      {
        TryStatement lastFinally = continueFinallyBlocks.remove(continueFinallyBlocks.size() - 1);
        lastBlock = finallyBlocks.get(lastFinally);
      }
      if (lastBlock == null)
      {
        throw new IllegalStateException("Continue statement leads to a null block during code generation: " + statement);
      }

      // work our way up from the last finally block to the break statement
      LLVMBasicBlockRef nextBlock = lastBlock;
      while (!continueFinallyBlocks.isEmpty())
      {
        TryStatement currentFinally = continueFinallyBlocks.remove(continueFinallyBlocks.size() - 1);
        LLVMBasicBlockRef currentBlock = finallyBlocks.get(currentFinally);
        if (currentBlock == null)
        {
          throw new IllegalStateException("Continue statement leads to a null block during code generation: " + statement);
        }

        // this finally block doesn't stop execution, so tell it to jump to nextBlock after it finishes
        LLVMBasicBlockRef indirectionBlock = LLVM.LLVMAddBasicBlock(builder, "continueFinallyIndirection");
        LLVM.LLVMPositionBuilderAtEnd(builder, indirectionBlock);

        List<LLVMBasicBlockRef> finallyJumpBlockList = finallyJumpBlocks.get(currentFinally);
        int jumpIndex = finallyJumpBlockList.size();
        finallyJumpBlockList.add(nextBlock);

        LLVMValueRef finallyJumpVariable = finallyJumpVariables.get(currentFinally);
        LLVM.LLVMBuildStore(builder, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), jumpIndex, false), finallyJumpVariable);
        LLVM.LLVMBuildBr(builder, currentBlock);

        nextBlock = indirectionBlock;
      }

      // branch to the next block in the chain
      LLVM.LLVMPositionBuilderAtEnd(builder, startBlock);
      LLVM.LLVMBuildBr(builder, nextBlock);
    }
    else if (statement instanceof DelegateConstructorStatement)
    {
      DelegateConstructorStatement delegateConstructorStatement = (DelegateConstructorStatement) statement;
      ConstructorReference delegatedConstructorReference = delegateConstructorStatement.getResolvedConstructorReference();
      // a null delegate constructor reference represents a call to the object super-constructor, which is a no-op, but the initialiser is still run afterwards
      if (delegatedConstructorReference != null)
      {
        Type[] parameterTypes = delegatedConstructorReference.getParameterTypes();
        Parameter[] realParameters = delegatedConstructorReference.getReferencedMember().getParameters();
        Expression[] arguments = delegateConstructorStatement.getArguments();
        GenericTypeSpecialiser constructorReferenceSpecialiser = new GenericTypeSpecialiser(delegatedConstructorReference.getContainingType());

        LLVMValueRef llvmConstructor = getConstructorFunction(delegatedConstructorReference.getReferencedMember());
        LLVMValueRef[] llvmArguments = new LLVMValueRef[1 + realParameters.length];
        // convert the thisValue to the delegated constructor's type, since if this is a super(...) constructor, the native type representation will be different
        llvmArguments[0] = typeHelper.convertTemporary(builder, landingPadContainer, thisValue, new NamedType(false, false, false, typeDefinition), delegatedConstructorReference.getContainingType(), false, typeParameterAccessor);
        for (int i = 0; i < realParameters.length; ++i)
        {
          LLVMValueRef argument = buildExpression(arguments[i], builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
          argument = typeHelper.convertTemporary(builder, landingPadContainer, argument, arguments[i].getType(), parameterTypes[i], false, typeParameterAccessor);

          Type contextualParameterType = typeHelper.getContextualisedActualMemberType(realParameters[i].getType(), constructorReferenceSpecialiser);
          llvmArguments[1 + i] = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, argument, parameterTypes[i], contextualParameterType, typeParameterAccessor);
        }
        LLVMBasicBlockRef constructorInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "delegateConstructorInvokeContinue");
        LLVM.LLVMBuildInvoke(builder, llvmConstructor, C.toNativePointerArray(llvmArguments, false, true), llvmArguments.length, constructorInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
        LLVM.LLVMPositionBuilderAtEnd(builder, constructorInvokeContinueBlock);
      }
      if (delegateConstructorStatement.isSuperConstructor())
      {
        // call the non-static initialiser function, which runs all non-static initialisers and sets the initial values for all of the fields
        // since, unlike a this(...) constructor, the super(...) constructor will not call this implicitly for us
        LLVMValueRef initialiserFunction = getInitialiserFunction(false);
        LLVMValueRef[] initialiserArgs = new LLVMValueRef[] {thisValue};
        LLVMBasicBlockRef initialiserInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "initialiserInvokeContinue");
        LLVM.LLVMBuildInvoke(builder, initialiserFunction, C.toNativePointerArray(initialiserArgs, false, true), initialiserArgs.length, initialiserInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
        LLVM.LLVMPositionBuilderAtEnd(builder, initialiserInvokeContinueBlock);
      }
    }
    else if (statement instanceof ExpressionStatement)
    {
      buildExpression(((ExpressionStatement) statement).getExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
    }
    else if (statement instanceof ForStatement)
    {
      ForStatement forStatement = (ForStatement) statement;
      Statement init = forStatement.getInitStatement();
      if (init != null)
      {
        buildStatement(init, returnType, builder, thisValue, variables, typeParameterAccessor, landingPadContainer, finallyBlocks, finallyJumpVariables, finallyJumpBlocks, breakBlocks, continueBlocks, returnVoidCallback);
      }
      Expression conditional = forStatement.getConditional();
      Statement update = forStatement.getUpdateStatement();

      // only generate a continuation block if there is a way to get out of the loop
      LLVMBasicBlockRef continuationBlock = forStatement.stopsExecution() ? null : LLVM.LLVMAddBasicBlock(builder, "afterForLoop");
      LLVMBasicBlockRef loopUpdate = update == null ? null : LLVM.LLVMAddBasicBlock(builder, "forLoopUpdate");
      LLVMBasicBlockRef loopBody = LLVM.LLVMAddBasicBlock(builder, "forLoopBody");
      LLVMBasicBlockRef loopCheck = conditional == null ? null : LLVM.LLVMAddBasicBlock(builder, "forLoopCheck");

      if (conditional == null)
      {
        LLVM.LLVMBuildBr(builder, loopBody);
      }
      else
      {
        LLVM.LLVMBuildBr(builder, loopCheck);
        LLVM.LLVMPositionBuilderAtEnd(builder, loopCheck);
        LLVMValueRef conditionResult = buildExpression(conditional, builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        conditionResult = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, conditionResult, conditional.getType(), new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), typeParameterAccessor);
        LLVM.LLVMBuildCondBr(builder, conditionResult, loopBody, continuationBlock);
      }

      LLVM.LLVMPositionBuilderAtEnd(builder, loopBody);
      if (continuationBlock != null)
      {
        breakBlocks.put(forStatement, continuationBlock);
      }
      continueBlocks.put(forStatement, loopUpdate == null ? (loopCheck == null ? loopBody : loopCheck) : loopUpdate);
      buildStatement(forStatement.getBlock(), returnType, builder, thisValue, variables, typeParameterAccessor, landingPadContainer, finallyBlocks, finallyJumpVariables, finallyJumpBlocks, breakBlocks, continueBlocks, returnVoidCallback);
      if (!forStatement.getBlock().stopsExecution())
      {
        LLVM.LLVMBuildBr(builder, loopUpdate == null ? (loopCheck == null ? loopBody : loopCheck) : loopUpdate);
      }
      if (update != null)
      {
        LLVM.LLVMPositionBuilderAtEnd(builder, loopUpdate);
        buildStatement(update, returnType, builder, thisValue, variables, typeParameterAccessor, landingPadContainer, finallyBlocks, finallyJumpVariables, finallyJumpBlocks, breakBlocks, continueBlocks, returnVoidCallback);
        if (update.stopsExecution())
        {
          throw new IllegalStateException("For loop update stops execution before the branch to the loop check: " + update);
        }
        LLVM.LLVMBuildBr(builder, loopCheck == null ? loopBody : loopCheck);
      }
      if (continuationBlock != null)
      {
        LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
      }
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      LLVMValueRef conditional = buildExpression(ifStatement.getExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      conditional = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, conditional, ifStatement.getExpression().getType(), new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), typeParameterAccessor);

      LLVMBasicBlockRef continuation = null;
      if (!ifStatement.stopsExecution())
      {
        continuation = LLVM.LLVMAddBasicBlock(builder, "continuation");
      }
      LLVMBasicBlockRef elseClause = null;
      if (ifStatement.getElseClause() != null)
      {
        elseClause = LLVM.LLVMAddBasicBlock(builder, "else");
      }
      LLVMBasicBlockRef thenClause = LLVM.LLVMAddBasicBlock(builder, "then");

      // build the branch instruction
      if (elseClause == null)
      {
        // if we have no else clause, then a continuation must have been created, since the if statement cannot stop execution
        LLVM.LLVMBuildCondBr(builder, conditional, thenClause, continuation);
      }
      else
      {
        LLVM.LLVMBuildCondBr(builder, conditional, thenClause, elseClause);

        // build the else clause
        LLVM.LLVMPositionBuilderAtEnd(builder, elseClause);
        buildStatement(ifStatement.getElseClause(), returnType, builder, thisValue, variables, typeParameterAccessor, landingPadContainer, finallyBlocks, finallyJumpVariables, finallyJumpBlocks, breakBlocks, continueBlocks, returnVoidCallback);
        if (!ifStatement.getElseClause().stopsExecution())
        {
          LLVM.LLVMBuildBr(builder, continuation);
        }
      }

      // build the then clause
      LLVM.LLVMPositionBuilderAtEnd(builder, thenClause);
      buildStatement(ifStatement.getThenClause(), returnType, builder, thisValue, variables, typeParameterAccessor, landingPadContainer, finallyBlocks, finallyJumpVariables, finallyJumpBlocks, breakBlocks, continueBlocks, returnVoidCallback);
      if (!ifStatement.getThenClause().stopsExecution())
      {
        LLVM.LLVMBuildBr(builder, continuation);
      }

      if (continuation != null)
      {
        LLVM.LLVMPositionBuilderAtEnd(builder, continuation);
      }
    }
    else if (statement instanceof PrefixIncDecStatement)
    {
      PrefixIncDecStatement prefixIncDecStatement = (PrefixIncDecStatement) statement;
      Assignee assignee = prefixIncDecStatement.getAssignee();

      // there are five possible ways we can populate these variables:
      // * only assigneePointer: a local variable
      // * assigneePointer and fieldReference: a field
      // * assigneePointer and propertyReference: a property backing variable
      // * propertyReference, propertyCallee, propertyCalleeType, and propertyIsConstructor: a normal property reference
      // * arrayAssignee and arrayAssigneeIndex: an array element
      LLVMValueRef assigneePointer = null;
      FieldReference fieldReference = null;

      PropertyReference propertyReference = null;
      LLVMValueRef propertyCallee = null;
      Type propertyCalleeType = null;
      boolean propertyIsConstructor = false;

      LLVMValueRef arrayAssignee = null;
      LLVMValueRef arrayAssigneeIndex = null;

      if (assignee instanceof VariableAssignee)
      {
        Variable resolvedVariable = ((VariableAssignee) assignee).getResolvedVariable();
        if (resolvedVariable instanceof MemberVariable)
        {
          MemberVariable memberVariable = (MemberVariable) resolvedVariable;
          assigneePointer = typeHelper.getMemberPointer(builder, thisValue, memberVariable);
          if (memberVariable.getField() != null)
          {
            fieldReference = new FieldReference(memberVariable.getField(), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
          }
          else if (memberVariable.getProperty() != null)
          {
            propertyReference = new PropertyReference(memberVariable.getProperty(), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
          }
          else
          {
            throw new IllegalArgumentException("Unknown sort of MemberVariable: not a field or a property backing variable");
          }
        }
        else if (resolvedVariable instanceof GlobalVariable)
        {
          GlobalVariable globalVariable = (GlobalVariable) resolvedVariable;
          assigneePointer = getGlobal(globalVariable);
          if (globalVariable.getField() != null)
          {
            fieldReference = new FieldReference(globalVariable.getField(), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
          }
          else if (globalVariable.getProperty() != null)
          {
            propertyReference = new PropertyReference(globalVariable.getProperty(), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
          }
          else
          {
            throw new IllegalArgumentException("Unknown sort of GlobalVariable: not a field or a property backing variable");
          }
        }
        else if (resolvedVariable instanceof PropertyPseudoVariable)
        {
          Property property = ((PropertyPseudoVariable) resolvedVariable).getProperty();
          propertyReference = new PropertyReference(property, GenericTypeSpecialiser.IDENTITY_SPECIALISER);
          propertyIsConstructor = ((VariableAssignee) assignee).isPropertyConstructorCall();
          if (property.isStatic())
          {
            propertyCallee = LLVM.LLVMConstNull(typeHelper.getOpaquePointer());
          }
          else
          {
            propertyCallee = thisValue;
            propertyCalleeType = new NamedType(false, false, false, typeDefinition);
          }
        }
        else
        {
          assigneePointer = variables.get(resolvedVariable);
        }
      }
      else if (assignee instanceof ArrayElementAssignee)
      {
        ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
        arrayAssignee = buildExpression(arrayElementAssignee.getArrayExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        LLVMValueRef dimension = buildExpression(arrayElementAssignee.getDimensionExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        arrayAssigneeIndex = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, dimension, arrayElementAssignee.getDimensionExpression().getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, typeParameterAccessor);
      }
      else if (assignee instanceof FieldAssignee)
      {
        FieldAssignee fieldAssignee = (FieldAssignee) assignee;
        FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
        if (fieldAccessExpression.getResolvedMemberReference() instanceof FieldReference)
        {
          fieldReference = (FieldReference) fieldAccessExpression.getResolvedMemberReference();
          Field field = fieldReference.getReferencedMember();
          if (field.isStatic())
          {
            assigneePointer = getGlobal(field.getGlobalVariable());
          }
          else
          {
            LLVMValueRef expressionValue = buildExpression(fieldAccessExpression.getBaseExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
            assigneePointer = typeHelper.getMemberPointer(builder, expressionValue, field.getMemberVariable());
          }
        }
        else if (fieldAccessExpression.getResolvedMemberReference() instanceof PropertyReference)
        {
          propertyReference = (PropertyReference) fieldAccessExpression.getResolvedMemberReference();
          propertyIsConstructor = ((FieldAssignee) assignee).isPropertyConstructorCall();
          Property property = propertyReference.getReferencedMember();
          if (property.isStatic())
          {
            propertyCallee = LLVM.LLVMConstNull(typeHelper.getOpaquePointer());
          }
          else
          {
            propertyCallee = buildExpression(fieldAccessExpression.getBaseExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
            propertyCalleeType = fieldAccessExpression.getBaseExpression().getType();
          }
        }
        else
        {
          throw new IllegalArgumentException("Unknown member assigned to in a FieldAssignee: " + fieldAccessExpression.getResolvedMemberReference());
        }
      }
      else
      {
        // ignore blank assignees, they shouldn't be able to get through variable resolution
        throw new IllegalStateException("Unknown Assignee type: " + assignee);
      }
      PrimitiveType assigneeType = (PrimitiveType) assignee.getResolvedType();

      // load the current value of whatever assignee we are working on
      LLVMValueRef loaded;
      if (assigneePointer != null)
      {
        loaded = LLVM.LLVMBuildLoad(builder, assigneePointer, "");
        if (fieldReference != null)
        {
          // this assignee is a field, so convert from the underlying field's type to reference type that we need in this context
          Field field = fieldReference.getReferencedMember();
          GenericTypeSpecialiser genericTypeSpecialiser = GenericTypeSpecialiser.IDENTITY_SPECIALISER;
          if (fieldReference.getContainingType() != null)
          {
            genericTypeSpecialiser = new GenericTypeSpecialiser(fieldReference.getContainingType());
          }
          Type contextualisedType = typeHelper.getContextualisedActualMemberType(field.getType(), genericTypeSpecialiser);
          loaded = typeHelper.convertStandardToTemporary(builder, landingPadContainer, loaded, contextualisedType, assigneeType, typeParameterAccessor);
        }
        else if (propertyReference != null)
        {
          // this assignee is a property backing variable, so convert from the underlying property's type to reference type that we need in this context
          Property property = propertyReference.getReferencedMember();
          GenericTypeSpecialiser genericTypeSpecialiser = GenericTypeSpecialiser.IDENTITY_SPECIALISER;
          if (propertyReference.getContainingType() != null)
          {
            genericTypeSpecialiser = new GenericTypeSpecialiser(propertyReference.getContainingType());
          }
          Type contextualisedType = typeHelper.getContextualisedActualMemberType(property.getType(), genericTypeSpecialiser);
          loaded = typeHelper.convertStandardToTemporary(builder, landingPadContainer, loaded, contextualisedType, assigneeType, typeParameterAccessor);
        }
      }
      else if (propertyReference != null)
      {
        loaded = typeHelper.buildPropertyGetterFunctionCall(builder, landingPadContainer, propertyCallee, propertyCalleeType, propertyReference, typeParameterAccessor);
      }
      else // if (assigneeArray != null)
      {
        loaded = typeHelper.buildRetrieveArrayElement(builder, landingPadContainer, arrayAssignee, arrayAssigneeIndex);
      }

      LLVMValueRef result;
      if (assigneeType.getPrimitiveTypeType().isFloating())
      {
        LLVMValueRef one = LLVM.LLVMConstReal(typeHelper.findTemporaryType(assigneeType), 1);
        if (prefixIncDecStatement.isIncrement())
        {
          result = LLVM.LLVMBuildFAdd(builder, loaded, one, "");
        }
        else
        {
          result = LLVM.LLVMBuildFSub(builder, loaded, one, "");
        }
      }
      else
      {
        LLVMValueRef one = LLVM.LLVMConstInt(typeHelper.findTemporaryType(assigneeType), 1, false);
        if (prefixIncDecStatement.isIncrement())
        {
          result = LLVM.LLVMBuildAdd(builder, loaded, one, "");
        }
        else
        {
          result = LLVM.LLVMBuildSub(builder, loaded, one, "");
        }
      }
      if (assigneePointer != null)
      {
        if (fieldReference != null)
        {
          Field field = fieldReference.getReferencedMember();

          GenericTypeSpecialiser genericTypeSpecialiser = GenericTypeSpecialiser.IDENTITY_SPECIALISER;
          if (fieldReference.getContainingType() != null)
          {
            genericTypeSpecialiser = new GenericTypeSpecialiser(fieldReference.getContainingType());
          }
          Type contextualisedType = typeHelper.getContextualisedActualMemberType(field.getType(), genericTypeSpecialiser);
          result = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, result, assigneeType, contextualisedType, typeParameterAccessor);
        }
        else if (propertyReference != null)
        {
          Property property = propertyReference.getReferencedMember();
          GenericTypeSpecialiser genericTypeSpecialiser = GenericTypeSpecialiser.IDENTITY_SPECIALISER;
          if (propertyReference.getContainingType() != null)
          {
            genericTypeSpecialiser = new GenericTypeSpecialiser(propertyReference.getContainingType());
          }
          Type contextualisedType = typeHelper.getContextualisedActualMemberType(property.getType(), genericTypeSpecialiser);
          result = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, result, assigneeType, contextualisedType, typeParameterAccessor);
        }
        else
        {
          // on assignment, we need to make sure compound types do not result in an alias to an existing variable, so convert to standard and back again
          // (this is mainly for compound types, so it will probably not be very useful here, but we leave it for consistency - it won't add any extra code)
          result = typeHelper.convertTemporaryToStandard(builder, result, assigneeType);
          result = typeHelper.convertStandardToTemporary(builder, result, assigneeType);
        }
        LLVM.LLVMBuildStore(builder, result, assigneePointer);
      }
      else if (propertyReference != null)
      {
        MemberFunctionType memberFunctionType = propertyIsConstructor ? MemberFunctionType.PROPERTY_CONSTRUCTOR : MemberFunctionType.PROPERTY_SETTER;
        typeHelper.buildPropertySetterConstructorFunctionCall(builder, landingPadContainer, propertyCallee, propertyCalleeType, result, propertyReference, memberFunctionType, typeParameterAccessor);
      }
      else // if (assigneeArray != null)
      {
        result = typeHelper.convertTemporaryToStandard(builder, result, assigneeType);
        typeHelper.buildStoreArrayElement(builder, landingPadContainer, arrayAssignee, arrayAssigneeIndex, result);
      }
    }
    else if (statement instanceof ReturnStatement)
    {
      ReturnStatement returnStatement = (ReturnStatement) statement;

      // always build the expression, even if we're blocked by a finally statement which stops execution
      LLVMValueRef returnedValue = null;
      if (returnStatement.getExpression() != null)
      {
        returnedValue = buildExpression(returnStatement.getExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        returnedValue = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, returnedValue, returnStatement.getExpression().getType(), returnType, typeParameterAccessor);
      }
      LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);

      List<TryStatement> returnFinallyBlocks = returnStatement.getResolvedFinallyBlocks();

      LLVMBasicBlockRef lastBlock;
      if (returnStatement.isStoppedByFinally())
      {
        // we are stopped by a finally statement, so it becomes our last block
        TryStatement lastFinally = returnFinallyBlocks.remove(returnFinallyBlocks.size() - 1);
        lastBlock = finallyBlocks.get(lastFinally);
        if (lastBlock == null)
        {
          throw new IllegalStateException("Return statement leads to a null block during code generation: " + statement);
        }
      }
      else
      {
        // build the actual return statement
        if (returnStatement.getExpression() == null)
        {
          lastBlock = LLVM.LLVMAddBasicBlock(builder, "return");
          LLVM.LLVMPositionBuilderAtEnd(builder, lastBlock);
          returnVoidCallback.run();
        }
        else
        {
          LLVMValueRef storedValueAlloca = LLVM.LLVMBuildAllocaInEntryBlock(builder, typeHelper.findStandardType(returnType), "");
          LLVM.LLVMBuildStore(builder, returnedValue, storedValueAlloca);
          startBlock = LLVM.LLVMGetInsertBlock(builder);

          lastBlock = LLVM.LLVMAddBasicBlock(builder, "return");
          LLVM.LLVMPositionBuilderAtEnd(builder, lastBlock);
          LLVMValueRef loadedValue = LLVM.LLVMBuildLoad(builder, storedValueAlloca, "");
          LLVM.LLVMBuildRet(builder, loadedValue);
        }
      }

      // process each of the finally blocks in turn
      LLVMBasicBlockRef nextBlock = lastBlock;
      while (!returnFinallyBlocks.isEmpty())
      {
        TryStatement currentFinally = returnFinallyBlocks.remove(returnFinallyBlocks.size() - 1);
        LLVMBasicBlockRef currentBlock = finallyBlocks.get(currentFinally);
        if (currentBlock == null)
        {
          throw new IllegalStateException("Return statement leads to a null block during code generation: " + statement);
        }

        // this finally block doesn't stop execution, so tell it to jump to nextBlock after it finishes
        LLVMBasicBlockRef indirectionBlock = LLVM.LLVMAddBasicBlock(builder, "breakFinallyIndirection");
        LLVM.LLVMPositionBuilderAtEnd(builder, indirectionBlock);

        List<LLVMBasicBlockRef> finallyJumpBlockList = finallyJumpBlocks.get(currentFinally);
        int jumpIndex = finallyJumpBlockList.size();
        finallyJumpBlockList.add(nextBlock);

        LLVMValueRef finallyJumpVariable = finallyJumpVariables.get(currentFinally);
        LLVM.LLVMBuildStore(builder, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), jumpIndex, false), finallyJumpVariable);
        LLVM.LLVMBuildBr(builder, currentBlock);

        nextBlock = indirectionBlock;
      }

      LLVM.LLVMPositionBuilderAtEnd(builder, startBlock);
      LLVM.LLVMBuildBr(builder, nextBlock);
    }
    else if (statement instanceof ShorthandAssignStatement)
    {
      ShorthandAssignStatement shorthandAssignStatement = (ShorthandAssignStatement) statement;
      Assignee[] assignees = shorthandAssignStatement.getAssignees();

      // for each assignee, there are five possible ways we can populate these array elements:
      // * only assigneePointers[i]: a local variable
      // * assigneePointers[i] and fieldReferences[i]: a field
      // * assigneePointers[i] and propertyReferences[i]: a property backing variable
      // * propertyReferences[i], propertyCallees[i], propertyCalleeTypes[i], and propertyIsConstructors[i]: a normal property reference
      // * arrayAssignees[i] and arrayAssigneeIndices[i]: an array element
      LLVMValueRef[] assigneePointers = new LLVMValueRef[assignees.length];
      FieldReference[] fieldReferences = new FieldReference[assignees.length];

      PropertyReference[] propertyReferences = new PropertyReference[assignees.length];
      LLVMValueRef[] propertyCallees = new LLVMValueRef[assignees.length];
      Type[] propertyCalleeTypes = new Type[assignees.length];
      boolean[] propertyIsConstructor = new boolean[assignees.length];

      LLVMValueRef[] arrayAssignees = new LLVMValueRef[assignees.length];
      LLVMValueRef[] arrayAssigneeIndices = new LLVMValueRef[assignees.length];

      for (int i = 0; i < assignees.length; ++i)
      {
        if (assignees[i] instanceof VariableAssignee)
        {
          Variable resolvedVariable = ((VariableAssignee) assignees[i]).getResolvedVariable();
          if (resolvedVariable instanceof MemberVariable)
          {
            MemberVariable memberVariable = (MemberVariable) resolvedVariable;
            assigneePointers[i] = typeHelper.getMemberPointer(builder, thisValue, memberVariable);
            if (memberVariable.getField() != null)
            {
              fieldReferences[i] = new FieldReference(memberVariable.getField(), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
            }
            else if (memberVariable.getProperty() != null)
            {
              propertyReferences[i] = new PropertyReference(memberVariable.getProperty(), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
            }
            else
            {
              throw new IllegalArgumentException("Unknown sort of MemberVariable: not a field or a property backing variable");
            }
          }
          else if (resolvedVariable instanceof GlobalVariable)
          {
            GlobalVariable globalVariable = (GlobalVariable) resolvedVariable;
            assigneePointers[i] = getGlobal(globalVariable);
            if (globalVariable.getField() != null)
            {
              fieldReferences[i] = new FieldReference(globalVariable.getField(), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
            }
            else if (globalVariable.getProperty() != null)
            {
              propertyReferences[i] = new PropertyReference(globalVariable.getProperty(), GenericTypeSpecialiser.IDENTITY_SPECIALISER);
            }
            else
            {
              throw new IllegalArgumentException("Unknown sort of GlobalVariable: not a field or a property backing variable");
            }
          }
          else if (resolvedVariable instanceof PropertyPseudoVariable)
          {
            Property property = ((PropertyPseudoVariable) resolvedVariable).getProperty();
            propertyReferences[i] = new PropertyReference(property, GenericTypeSpecialiser.IDENTITY_SPECIALISER);
            propertyIsConstructor[i] = ((VariableAssignee) assignees[i]).isPropertyConstructorCall();
            if (property.isStatic())
            {
              propertyCallees[i] = LLVM.LLVMConstNull(typeHelper.getOpaquePointer());
              propertyCalleeTypes[i] = null;
            }
            else
            {
              propertyCallees[i] = thisValue;
              propertyCalleeTypes[i] = new NamedType(false, false, false, typeDefinition);
            }
          }
          else
          {
            assigneePointers[i] = variables.get(resolvedVariable);
          }
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          arrayAssignees[i] = buildExpression(arrayElementAssignee.getArrayExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
          LLVMValueRef dimension = buildExpression(arrayElementAssignee.getDimensionExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
          arrayAssigneeIndices[i] = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, dimension, arrayElementAssignee.getDimensionExpression().getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, typeParameterAccessor);
        }
        else if (assignees[i] instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];
          FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
          if (fieldAccessExpression.getResolvedMemberReference() instanceof FieldReference)
          {
            fieldReferences[i] = (FieldReference) fieldAccessExpression.getResolvedMemberReference();
            Field field = fieldReferences[i].getReferencedMember();
            if (field.isStatic())
            {
              assigneePointers[i] = getGlobal(field.getGlobalVariable());
            }
            else
            {
              LLVMValueRef expressionValue = buildExpression(fieldAccessExpression.getBaseExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
              assigneePointers[i] = typeHelper.getMemberPointer(builder, expressionValue, field.getMemberVariable());
            }
          }
          else if (fieldAccessExpression.getResolvedMemberReference() instanceof PropertyReference)
          {
            propertyReferences[i] = (PropertyReference) fieldAccessExpression.getResolvedMemberReference();
            Property property = propertyReferences[i].getReferencedMember();
            propertyIsConstructor[i] = fieldAssignee.isPropertyConstructorCall();
            if (property.isStatic())
            {
              propertyCallees[i] = LLVM.LLVMConstNull(typeHelper.getOpaquePointer());
              propertyCalleeTypes[i] = null;
            }
            else
            {
              propertyCallees[i] = buildExpression(fieldAccessExpression.getBaseExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
              propertyCalleeTypes[i] = fieldAccessExpression.getBaseExpression().getType();
            }
          }
          else
          {
            throw new IllegalArgumentException("Unknown member assigned to in a FieldAssignee: " + fieldAccessExpression.getResolvedMemberReference());
          }
        }
        else if (assignees[i] instanceof BlankAssignee)
        {
          // this assignee doesn't actually get assigned to, so don't set this element in any of the arrays (leave it as null)
        }
        else
        {
          throw new IllegalStateException("Unknown Assignee type: " + assignees[i]);
        }
      }

      LLVMValueRef result = buildExpression(shorthandAssignStatement.getExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      Type resultType = shorthandAssignStatement.getExpression().getType();
      LLVMValueRef[] resultValues = new LLVMValueRef[assignees.length];
      Type[] resultValueTypes = new Type[assignees.length];
      if (resultType instanceof TupleType && !resultType.canBeNullable() && ((TupleType) resultType).getSubTypes().length == assignees.length)
      {
        Type[] subTypes = ((TupleType) resultType).getSubTypes();
        for (int i = 0; i < assignees.length; ++i)
        {
          if (assignees[i] instanceof BlankAssignee)
          {
            continue;
          }
          resultValues[i] = LLVM.LLVMBuildExtractValue(builder, result, i, "");
          resultValueTypes[i] = subTypes[i];
        }
      }
      else
      {
        for (int i = 0; i < assignees.length; ++i)
        {
          resultValues[i] = result;
          resultValueTypes[i] = resultType;
        }
      }
      for (int i = 0; i < assignees.length; ++i)
      {
        if (assigneePointers[i] == null && propertyReferences[i] == null && arrayAssignees[i] == null)
        {
          // this is a blank assignee, so don't try to do anything for it
          continue;
        }
        Type type = assignees[i].getResolvedType();
        LLVMValueRef leftValue;
        if (assigneePointers[i] != null)
        {
          leftValue = LLVM.LLVMBuildLoad(builder, assigneePointers[i], "");
          if (fieldReferences[i] != null)
          {
            // this assignee is a field, so convert from the underlying field's type to reference type that we need in this context
            Field field = fieldReferences[i].getReferencedMember();
            GenericTypeSpecialiser genericTypeSpecialiser = GenericTypeSpecialiser.IDENTITY_SPECIALISER;
            if (fieldReferences[i].getContainingType() != null)
            {
              genericTypeSpecialiser = new GenericTypeSpecialiser(fieldReferences[i].getContainingType());
            }
            Type contextualisedType = typeHelper.getContextualisedActualMemberType(field.getType(), genericTypeSpecialiser);
            leftValue = typeHelper.convertStandardToTemporary(builder, landingPadContainer, leftValue, contextualisedType, type, typeParameterAccessor);
          }
          else if (propertyReferences[i] != null)
          {
            // this assignee is a property backing variable, so convert from the underlying property's type to reference type that we need in this context
            Property property = propertyReferences[i].getReferencedMember();
            GenericTypeSpecialiser genericTypeSpecialiser = GenericTypeSpecialiser.IDENTITY_SPECIALISER;
            if (propertyReferences[i].getContainingType() != null)
            {
              genericTypeSpecialiser = new GenericTypeSpecialiser(propertyReferences[i].getContainingType());
            }
            Type contextualisedType = typeHelper.getContextualisedActualMemberType(property.getType(), genericTypeSpecialiser);
            leftValue = typeHelper.convertStandardToTemporary(builder, landingPadContainer, leftValue, contextualisedType, type, typeParameterAccessor);
          }
        }
        else if (propertyReferences[i] != null)
        {
          leftValue = typeHelper.buildPropertyGetterFunctionCall(builder, landingPadContainer, propertyCallees[i], propertyCalleeTypes[i], propertyReferences[i], typeParameterAccessor);
        }
        else // if (assigneeArrays[i] != null)
        {
          leftValue = typeHelper.buildRetrieveArrayElement(builder, landingPadContainer, arrayAssignees[i], arrayAssigneeIndices[i]);
        }


        LLVMValueRef assigneeResult;
        if (shorthandAssignStatement.getOperator() == ShorthandAssignmentOperator.ADD && type.isRuntimeEquivalent(SpecialTypeHandler.STRING_TYPE))
        {
          leftValue = typeHelper.convertTemporaryToStandard(builder, leftValue, type);
          LLVMValueRef rightStringValue = typeHelper.convertToString(builder, landingPadContainer, resultValues[i], resultValueTypes[i], typeParameterAccessor);
          assigneeResult = buildStringConcatenation(builder, landingPadContainer, leftValue, rightStringValue);
        }
        else if (type instanceof PrimitiveType)
        {
          LLVMValueRef rightValue = typeHelper.convertTemporary(builder, landingPadContainer, resultValues[i], resultValueTypes[i], type, false, typeParameterAccessor);
          PrimitiveTypeType primitiveType = ((PrimitiveType) type).getPrimitiveTypeType();
          boolean floating = primitiveType.isFloating();
          boolean signed = primitiveType.isSigned();
          switch (shorthandAssignStatement.getOperator())
          {
          case AND:
            assigneeResult = LLVM.LLVMBuildAnd(builder, leftValue, rightValue, "");
            break;
          case OR:
            assigneeResult = LLVM.LLVMBuildOr(builder, leftValue, rightValue, "");
            break;
          case XOR:
            assigneeResult = LLVM.LLVMBuildXor(builder, leftValue, rightValue, "");
            break;
          case ADD:
            assigneeResult = floating ? LLVM.LLVMBuildFAdd(builder, leftValue, rightValue, "") : LLVM.LLVMBuildAdd(builder, leftValue, rightValue, "");
            break;
          case SUBTRACT:
            assigneeResult = floating ? LLVM.LLVMBuildFSub(builder, leftValue, rightValue, "") : LLVM.LLVMBuildSub(builder, leftValue, rightValue, "");
            break;
          case MULTIPLY:
            assigneeResult = floating ? LLVM.LLVMBuildFMul(builder, leftValue, rightValue, "") : LLVM.LLVMBuildMul(builder, leftValue, rightValue, "");
            break;
          case DIVIDE:
            assigneeResult = floating ? LLVM.LLVMBuildFDiv(builder, leftValue, rightValue, "") : signed ? LLVM.LLVMBuildSDiv(builder, leftValue, rightValue, "") : LLVM.LLVMBuildUDiv(builder, leftValue, rightValue, "");
            break;
          case REMAINDER:
            assigneeResult = floating ? LLVM.LLVMBuildFRem(builder, leftValue, rightValue, "") : signed ? LLVM.LLVMBuildSRem(builder, leftValue, rightValue, "") : LLVM.LLVMBuildURem(builder, leftValue, rightValue, "");
            break;
          case MODULO:
            if (floating)
            {
              LLVMValueRef rem = LLVM.LLVMBuildFRem(builder, leftValue, rightValue, "");
              LLVMValueRef add = LLVM.LLVMBuildFAdd(builder, rem, rightValue, "");
              assigneeResult = LLVM.LLVMBuildFRem(builder, add, rightValue, "");
            }
            else if (signed)
            {
              LLVMValueRef rem = LLVM.LLVMBuildSRem(builder, leftValue, rightValue, "");
              LLVMValueRef add = LLVM.LLVMBuildAdd(builder, rem, rightValue, "");
              assigneeResult = LLVM.LLVMBuildSRem(builder, add, rightValue, "");
            }
            else
            {
              // unsigned modulo is the same as unsigned remainder
              assigneeResult = LLVM.LLVMBuildURem(builder, leftValue, rightValue, "");
            }
            break;
          case LEFT_SHIFT:
            assigneeResult = LLVM.LLVMBuildShl(builder, leftValue, rightValue, "");
            break;
          case RIGHT_SHIFT:
            assigneeResult = signed ? LLVM.LLVMBuildAShr(builder, leftValue, rightValue, "") : LLVM.LLVMBuildLShr(builder, leftValue, rightValue, "");
            break;
          default:
            throw new IllegalStateException("Unknown shorthand assignment operator: " + shorthandAssignStatement.getOperator());
          }
        }
        else
        {
          throw new IllegalStateException("Unknown shorthand assignment operation: " + shorthandAssignStatement);
        }


        if (assigneePointers[i] != null)
        {
          if (fieldReferences[i] != null)
          {
            Field field = fieldReferences[i].getReferencedMember();
            GenericTypeSpecialiser genericTypeSpecialiser = GenericTypeSpecialiser.IDENTITY_SPECIALISER;
            if (fieldReferences[i].getContainingType() != null)
            {
              genericTypeSpecialiser = new GenericTypeSpecialiser(fieldReferences[i].getContainingType());
            }
            Type contextualisedType = typeHelper.getContextualisedActualMemberType(field.getType(), genericTypeSpecialiser);
            assigneeResult = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, assigneeResult, type, contextualisedType, typeParameterAccessor);
          }
          else if (propertyReferences[i] != null)
          {
            Property property = propertyReferences[i].getReferencedMember();
            GenericTypeSpecialiser genericTypeSpecialiser = GenericTypeSpecialiser.IDENTITY_SPECIALISER;
            if (propertyReferences[i].getContainingType() != null)
            {
              genericTypeSpecialiser = new GenericTypeSpecialiser(propertyReferences[i].getContainingType());
            }
            Type contextualisedType = typeHelper.getContextualisedActualMemberType(property.getType(), genericTypeSpecialiser);
            assigneeResult = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, assigneeResult, type, contextualisedType, typeParameterAccessor);
          }
          else
          {
            // on assignment, we need to make sure compound types do not result in an alias to an existing variable, so convert to standard and back again
            // (this is mainly for compound types, so it will probably not be very useful here, but we leave it for consistency - it won't add any extra code)
            assigneeResult = typeHelper.convertTemporaryToStandard(builder, assigneeResult, assignees[i].getResolvedType());
            assigneeResult = typeHelper.convertStandardToTemporary(builder, assigneeResult, assignees[i].getResolvedType());
          }
          LLVM.LLVMBuildStore(builder, assigneeResult, assigneePointers[i]);
        }
        else if (propertyReferences[i] != null)
        {
          MemberFunctionType memberFunctionType = propertyIsConstructor[i] ? MemberFunctionType.PROPERTY_CONSTRUCTOR : MemberFunctionType.PROPERTY_SETTER;
          typeHelper.buildPropertySetterConstructorFunctionCall(builder, landingPadContainer, propertyCallees[i], propertyCalleeTypes[i], assigneeResult, propertyReferences[i], memberFunctionType, typeParameterAccessor);
        }
        else // if (assigneeArrays[i] != null)
        {
          assigneeResult = typeHelper.convertTemporaryToStandard(builder, assigneeResult, type);
          typeHelper.buildStoreArrayElement(builder, landingPadContainer, arrayAssignees[i], arrayAssigneeIndices[i], assigneeResult);
        }
      }
    }
    else if (statement instanceof ThrowStatement)
    {
      ThrowStatement throwStatement = (ThrowStatement) statement;
      LLVMValueRef thrownValue = buildExpression(throwStatement.getThrownExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      // in case we're throwing an interface, convert to a not-null object in a standard type representation
      thrownValue = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, thrownValue, throwStatement.getThrownExpression().getType(), new ObjectType(false, false, null), typeParameterAccessor);
      LLVMValueRef unwindException = buildCreateException(builder, thrownValue);
      buildThrow(builder, landingPadContainer, unwindException);
    }
    else if (statement instanceof TryStatement)
    {
      TryStatement tryStatement = (TryStatement) statement;

      LLVMBasicBlockRef afterTryCatchFinallyBlock = null;
      if (!tryStatement.stopsExecution())
      {
        afterTryCatchFinallyBlock = LLVM.LLVMAddBasicBlock(builder, "afterTryCatchFinally");
      }

      LLVMBasicBlockRef llvmFinallyBlock = null;
      if (tryStatement.getFinallyBlock() != null)
      {
        llvmFinallyBlock = LLVM.LLVMAddBasicBlock(builder, "finally");
        finallyBlocks.put(tryStatement, llvmFinallyBlock);
        if (!tryStatement.getFinallyBlock().stopsExecution())
        {
          LLVMValueRef jumpVariable = LLVM.LLVMBuildAllocaInEntryBlock(builder, LLVM.LLVMInt32Type(), "finallyDestination");
          finallyJumpVariables.put(tryStatement, jumpVariable);
          finallyJumpBlocks.put(tryStatement, new LinkedList<LLVMBasicBlockRef>());
        }
      }

      LandingPadContainer tryLandingPadContainer = new LandingPadContainer(builder);
      buildStatement(tryStatement.getTryBlock(), returnType, builder, thisValue, variables, typeParameterAccessor, tryLandingPadContainer, finallyBlocks, finallyJumpVariables, finallyJumpBlocks, breakBlocks, continueBlocks, returnVoidCallback);

      if (!tryStatement.getTryBlock().stopsExecution())
      {
        if (tryStatement.getFinallyBlock() == null)
        {
          LLVM.LLVMBuildBr(builder, afterTryCatchFinallyBlock);
        }
        else
        {
          if (tryStatement.getFinallyBlock().stopsExecution())
          {
            LLVM.LLVMBuildBr(builder, llvmFinallyBlock);
          }
          else
          {
            // the finally block doesn't stop execution, so decide where to jump to after it finishes
            List<LLVMBasicBlockRef> finallyJumpBlockList = finallyJumpBlocks.get(tryStatement);
            int jumpIndex = finallyJumpBlockList.size();
            finallyJumpBlockList.add(afterTryCatchFinallyBlock);

            LLVMValueRef finallyJumpVariable = finallyJumpVariables.get(tryStatement);
            LLVM.LLVMBuildStore(builder, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), jumpIndex, false), finallyJumpVariable);
            LLVM.LLVMBuildBr(builder, llvmFinallyBlock);
          }
        }
      }

      LLVMBasicBlockRef tryLandingPadBlock = tryLandingPadContainer.getExistingLandingPadBlock();
      if (tryLandingPadBlock != null)
      {
        CatchClause[] catchClauses = tryStatement.getCatchClauses();
        LLVMBasicBlockRef[] catchClauseBlocks = new LLVMBasicBlockRef[catchClauses.length];
        for (int i = 0; i < catchClauses.length; ++i)
        {
          catchClauseBlocks[i] = LLVM.LLVMAddBasicBlock(builder, "catch");
        }
        LLVM.LLVMPositionBuilderAtEnd(builder, tryLandingPadBlock);
        LLVMValueRef tryLandingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), getPersonalityFunction(), 1, "");
        // create an empty filter, so that we catch everything
        LLVMValueRef[] tryFilterValues = new LLVMValueRef[0];
        LLVM.LLVMAddClause(tryLandingPad, LLVM.LLVMConstArray(LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0), C.toNativePointerArray(tryFilterValues, false, true), tryFilterValues.length));

        // extract the plinth exception and do instanceof checks to decide which catch block to jump to
        LLVMValueRef tryUnwindExceptionPointer = LLVM.LLVMBuildExtractValue(builder, tryLandingPad, 0, "");
        LLVMValueRef plinthException = buildExtractExceptionObject(builder, tryUnwindExceptionPointer);
        plinthException = LLVM.LLVMBuildBitCast(builder, plinthException, typeHelper.findStandardType(new ObjectType(false, false, null)), "");
        for (int i = 0; i < catchClauses.length; ++i)
        {
          for (Type caughtType : catchClauses[i].getCaughtTypes())
          {
            LLVMValueRef typeMatches = rttiHelper.buildInstanceOfCheck(builder, null, plinthException, new ObjectType(false, false, null), caughtType, typeParameterAccessor);
            LLVMBasicBlockRef nextLandingPadCheckBlock = LLVM.LLVMAddBasicBlock(builder, "landingPadCheck");
            LLVM.LLVMBuildCondBr(builder, typeMatches, catchClauseBlocks[i], nextLandingPadCheckBlock);
            LLVM.LLVMPositionBuilderAtEnd(builder, nextLandingPadCheckBlock);
          }
        }

        if (tryStatement.getFinallyBlock() == null)
        {
          // we can't just resume here, as it would make us skip any catch/finally blocks above this one but in the same function
          buildThrow(builder, landingPadContainer, tryUnwindExceptionPointer);
        }
        else
        {
          if (tryStatement.getFinallyBlock().stopsExecution())
          {
            // the finally block never terminates, so we don't need to store the exception, it always gets discarded
            buildDestroyNativeException(builder, tryUnwindExceptionPointer);
            // TODO: garbage collection: handle the stopped plinthException resulting from the catch
            LLVM.LLVMBuildBr(builder, llvmFinallyBlock);
          }
          else
          {
            // the finally block doesn't stop execution, so decide where to jump to after it finishes
            LLVMBasicBlockRef resumeBlock = LLVM.LLVMAddBasicBlock(builder, "finallyResume");

            List<LLVMBasicBlockRef> finallyJumpBlockList = finallyJumpBlocks.get(tryStatement);
            int jumpIndex = finallyJumpBlockList.size();
            finallyJumpBlockList.add(resumeBlock);

            LLVMValueRef finallyJumpVariable = finallyJumpVariables.get(tryStatement);

            LLVMValueRef exceptionStorageAlloca = LLVM.LLVMBuildAllocaInEntryBlock(builder, LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0), "exceptionStorage");
            LLVM.LLVMBuildStore(builder, tryUnwindExceptionPointer, exceptionStorageAlloca);
            LLVM.LLVMBuildStore(builder, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), jumpIndex, false), finallyJumpVariable);
            LLVM.LLVMBuildBr(builder, llvmFinallyBlock);

            LLVM.LLVMPositionBuilderAtEnd(builder, resumeBlock);
            LLVMValueRef storedException = LLVM.LLVMBuildLoad(builder, exceptionStorageAlloca, "");
            buildThrow(builder, landingPadContainer, storedException);
          }
        }

        // build the catch blocks
        LandingPadContainer catchLandingPadContainer;
        if (tryStatement.getFinallyBlock() == null)
        {
          catchLandingPadContainer = landingPadContainer;
        }
        else
        {
          catchLandingPadContainer = new LandingPadContainer(builder);
        }

        ObjectType objectType = new ObjectType(false, false, null);
        for (int i = 0; i < catchClauses.length; ++i)
        {
          LLVM.LLVMPositionBuilderAtEnd(builder, catchClauseBlocks[i]);
          buildDestroyNativeException(builder, tryUnwindExceptionPointer);

          Variable variable = catchClauses[i].getResolvedExceptionVariable();
          LLVMValueRef caughtException = typeHelper.convertStandardToTemporary(builder, catchLandingPadContainer, plinthException, objectType, variable.getType(), typeParameterAccessor);
          LLVM.LLVMBuildStore(builder, caughtException, variables.get(variable));

          buildStatement(catchClauses[i].getBlock(), returnType, builder, thisValue, variables, typeParameterAccessor, catchLandingPadContainer, finallyBlocks, finallyJumpVariables, finallyJumpBlocks, breakBlocks, continueBlocks, returnVoidCallback);
          if (!catchClauses[i].getBlock().stopsExecution())
          {
            if (tryStatement.getFinallyBlock() == null)
            {
              LLVM.LLVMBuildBr(builder, afterTryCatchFinallyBlock);
            }
            else
            {
              if (tryStatement.getFinallyBlock().stopsExecution())
              {
                LLVM.LLVMBuildBr(builder, llvmFinallyBlock);
              }
              else
              {
                // the finally block doesn't stop execution, so decide where to jump to after it finishes
                List<LLVMBasicBlockRef> finallyJumpBlockList = finallyJumpBlocks.get(tryStatement);
                int jumpIndex = finallyJumpBlockList.size();
                finallyJumpBlockList.add(afterTryCatchFinallyBlock);

                LLVMValueRef finallyJumpVariable = finallyJumpVariables.get(tryStatement);
                LLVM.LLVMBuildStore(builder, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), jumpIndex, false), finallyJumpVariable);
                LLVM.LLVMBuildBr(builder, llvmFinallyBlock);
              }
            }
          }
        }

        // build the landing pad for the catch statements
        // (we only build one for ourselves if there is a finally block, otherwise we just use the outer one)
        LLVMBasicBlockRef catchLandingPadBlock = catchLandingPadContainer.getExistingLandingPadBlock();
        if (tryStatement.getFinallyBlock() != null && catchLandingPadBlock != null)
        {
          LLVM.LLVMPositionBuilderAtEnd(builder, catchLandingPadBlock);
          LLVMValueRef catchLandingPad = LLVM.LLVMBuildLandingPad(builder, typeHelper.getLandingPadType(), getPersonalityFunction(), 1, "");
          LLVMValueRef catchUnwindExceptionPointer = LLVM.LLVMBuildExtractValue(builder, catchLandingPad, 0, "");

          // create an empty filter, so that we catch everything for a finally block
          LLVMValueRef[] catchFilterValues = new LLVMValueRef[0];
          LLVM.LLVMAddClause(catchLandingPad, LLVM.LLVMConstArray(LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0), C.toNativePointerArray(catchFilterValues, false, true), catchFilterValues.length));

          if (tryStatement.getFinallyBlock().stopsExecution())
          {
            // the finally block never terminates, so we don't need to store the exception, it always gets discarded
            LLVMValueRef plinthExceptionObject = buildExtractExceptionObject(builder, catchUnwindExceptionPointer);
            plinthExceptionObject = LLVM.LLVMBuildBitCast(builder, plinthExceptionObject, typeHelper.findStandardType(new ObjectType(false, false, null)), "");
            buildDestroyNativeException(builder, catchUnwindExceptionPointer);
            // TODO: garbage collection: handle the stopped plinthException resulting from the catch
            LLVM.LLVMBuildBr(builder, llvmFinallyBlock);
          }
          else
          {
            // the finally block doesn't stop execution, so decide where to jump to after it finishes
            LLVMBasicBlockRef resumeBlock = LLVM.LLVMAddBasicBlock(builder, "finallyResume");

            List<LLVMBasicBlockRef> finallyJumpBlockList = finallyJumpBlocks.get(tryStatement);
            int jumpIndex = finallyJumpBlockList.size();
            finallyJumpBlockList.add(resumeBlock);

            LLVMValueRef finallyJumpVariable = finallyJumpVariables.get(tryStatement);

            LLVMValueRef exceptionStorageAlloca = LLVM.LLVMBuildAllocaInEntryBlock(builder, LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0), "exceptionStorage");
            LLVM.LLVMBuildStore(builder, catchUnwindExceptionPointer, exceptionStorageAlloca);
            LLVM.LLVMBuildStore(builder, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), jumpIndex, false), finallyJumpVariable);
            LLVM.LLVMBuildBr(builder, llvmFinallyBlock);

            LLVM.LLVMPositionBuilderAtEnd(builder, resumeBlock);
            LLVMValueRef storedException = LLVM.LLVMBuildLoad(builder, exceptionStorageAlloca, "");
            buildThrow(builder, landingPadContainer, storedException);
          }
        }
      }

      // we have finished building all of the try and catch blocks
      // so now build the finally block
      if (tryStatement.getFinallyBlock() != null)
      {
        LLVM.LLVMPositionBuilderAtEnd(builder, llvmFinallyBlock);
        buildStatement(tryStatement.getFinallyBlock(), returnType, builder, thisValue, variables, typeParameterAccessor, landingPadContainer, finallyBlocks, finallyJumpVariables, finallyJumpBlocks, breakBlocks, continueBlocks, returnVoidCallback);

        if (!tryStatement.getFinallyBlock().stopsExecution())
        {
          List<LLVMBasicBlockRef> jumpList = finallyJumpBlocks.get(tryStatement);
          LLVMValueRef jumpVariable = finallyJumpVariables.get(tryStatement);
          LLVMValueRef jumpValue = LLVM.LLVMBuildLoad(builder, jumpVariable, "");

          LLVMBasicBlockRef defaultCase = LLVM.LLVMAddBasicBlock(builder, "finallyDefaultJump");
          LLVMValueRef switchNode = LLVM.LLVMBuildSwitch(builder, jumpValue, defaultCase, jumpList.size());
          int index = 0;
          Iterator<LLVMBasicBlockRef> it = jumpList.iterator();
          while (it.hasNext())
          {
            LLVMBasicBlockRef block = it.next();
            LLVM.LLVMAddCase(switchNode, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), index, false), block);
            index++;
          }

          LLVM.LLVMPositionBuilderAtEnd(builder, defaultCase);
          LLVM.LLVMBuildUnreachable(builder);
        }
      }

      if (afterTryCatchFinallyBlock != null)
      {
        LLVM.LLVMPositionBuilderAtEnd(builder, afterTryCatchFinallyBlock);
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;

      LLVMBasicBlockRef afterLoopBlock = LLVM.LLVMAddBasicBlock(builder, "afterWhileLoop");
      LLVMBasicBlockRef loopBodyBlock = LLVM.LLVMAddBasicBlock(builder, "whileLoopBody");
      LLVMBasicBlockRef loopCheck = LLVM.LLVMAddBasicBlock(builder, "whileLoopCheck");
      LLVM.LLVMBuildBr(builder, loopCheck);

      LLVM.LLVMPositionBuilderAtEnd(builder, loopCheck);
      LLVMValueRef conditional = buildExpression(whileStatement.getExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      conditional = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, conditional, whileStatement.getExpression().getType(), new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), typeParameterAccessor);

      LLVM.LLVMBuildCondBr(builder, conditional, loopBodyBlock, afterLoopBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, loopBodyBlock);
      // add the while statement's afterLoop block to the breakBlocks map before it's statement is built
      breakBlocks.put(whileStatement, afterLoopBlock);
      continueBlocks.put(whileStatement, loopCheck);
      buildStatement(whileStatement.getStatement(), returnType, builder, thisValue, variables, typeParameterAccessor, landingPadContainer, finallyBlocks, finallyJumpVariables, finallyJumpBlocks, breakBlocks, continueBlocks, returnVoidCallback);

      if (!whileStatement.getStatement().stopsExecution())
      {
        LLVM.LLVMBuildBr(builder, loopCheck);
      }

      LLVM.LLVMPositionBuilderAtEnd(builder, afterLoopBlock);
    }
    else
    {
      throw new IllegalArgumentException("Unknown Statement type: " + statement);
    }
  }

  private int getPredicate(EqualityOperator operator, boolean floating)
  {
    if (floating)
    {
      switch (operator)
      {
      case EQUAL:
        return LLVM.LLVMRealPredicate.LLVMRealOEQ;
      case NOT_EQUAL:
        return LLVM.LLVMRealPredicate.LLVMRealONE;
      }
    }
    else
    {
      switch (operator)
      {
      case EQUAL:
        return LLVM.LLVMIntPredicate.LLVMIntEQ;
      case NOT_EQUAL:
        return LLVM.LLVMIntPredicate.LLVMIntNE;
      }
    }
    throw new IllegalArgumentException("Unknown predicate '" + operator + "'");
  }

  private int getPredicate(RelationalOperator operator, boolean floating, boolean signed)
  {
    if (floating)
    {
      switch (operator)
      {
      case LESS_THAN:
        return LLVM.LLVMRealPredicate.LLVMRealOLT;
      case LESS_THAN_EQUAL:
        return LLVM.LLVMRealPredicate.LLVMRealOLE;
      case MORE_THAN:
        return LLVM.LLVMRealPredicate.LLVMRealOGT;
      case MORE_THAN_EQUAL:
        return LLVM.LLVMRealPredicate.LLVMRealOGE;
      }
    }
    else
    {
      switch (operator)
      {
      case LESS_THAN:
        return signed ? LLVM.LLVMIntPredicate.LLVMIntSLT : LLVM.LLVMIntPredicate.LLVMIntULT;
      case LESS_THAN_EQUAL:
        return signed ? LLVM.LLVMIntPredicate.LLVMIntSLE : LLVM.LLVMIntPredicate.LLVMIntULE;
      case MORE_THAN:
        return signed ? LLVM.LLVMIntPredicate.LLVMIntSGT : LLVM.LLVMIntPredicate.LLVMIntUGT;
      case MORE_THAN_EQUAL:
        return signed ? LLVM.LLVMIntPredicate.LLVMIntSGE : LLVM.LLVMIntPredicate.LLVMIntUGE;
      }
    }
    throw new IllegalArgumentException("Unknown predicate '" + operator + "'");
  }

  /**
   * Builds code to create an array in the specified function, with the specified length(s) and type
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param landingPadContainer - the LandingPadContainer containing the landing pad block for exceptions to be unwound to
   * @param llvmLengths - the list of lengths of the array(s), each being a native uint.
   *                      if the array is multidimensional, this should contain an element per dimension to be created
   * @param type - the type of the array to create
   * @param initialiserValue - the value of the initialiser to initialise the values inside the array with, in a temporary type representation, or null if there is no initialiser
   * @param initialiserType - the type of the initialiser, or null if there is no initialiser
   * @param initialiserIsFunction - true if the initialiser is a function which should be called to produce the values in the array, false otherwise
   * @param typeParameterAccessor - the TypeParameterAccessor to get the values of any TypeParameters from
   * @return the pointer to the array created, in a temporary type representation
   */
  public LLVMValueRef buildArrayCreation(LLVMBuilderRef builder, LandingPadContainer landingPadContainer, LLVMValueRef[] llvmLengths, ArrayType type, LLVMValueRef initialiserValue, Type initialiserType, boolean initialiserIsFunction, TypeParameterAccessor typeParameterAccessor)
  {
    // we need to create the array as if any type parameters inside it are actually objects, so strip them
    ArrayType currentType = (ArrayType) typeHelper.stripTypeParameters(type);

    // if there is an initialiser, we add an extra element to the end of created, which is the value generated by the initialiser
    LLVMValueRef[] created = new LLVMValueRef[llvmLengths.length + (initialiserType == null ? 0 : 1)];
    ArrayType[] createdTypes = new ArrayType[llvmLengths.length];

    LLVMValueRef[] initialisationPhiNodes = new LLVMValueRef[llvmLengths.length];
    LLVMBasicBlockRef[] initialisationLoopCheckBlocks = new LLVMBasicBlockRef[llvmLengths.length];
    LLVMBasicBlockRef[] initialisationLoopBlocks = new LLVMBasicBlockRef[llvmLengths.length];
    LLVMBasicBlockRef initialisationExitBlock = null;

    for (int i = 0; i < llvmLengths.length; ++i)
    {
      // first, create the current array, we'll worry about initialising it once it has its members filled in (RTTI, length, etc.)

      LLVMTypeRef llvmArrayType = LLVM.LLVMPointerType(typeHelper.findNonProxiedArrayStructureType(currentType, 0), 0);
      // find the element of our array at index length (i.e. one past the end of the array), which gives us our size
      LLVMValueRef llvmArraySize = typeHelper.getNonProxiedArrayElementPointer(builder, LLVM.LLVMConstNull(llvmArrayType), llvmLengths[i]);
      LLVMValueRef llvmSize = LLVM.LLVMBuildPtrToInt(builder, llvmArraySize, LLVM.LLVMInt32Type(), "");

      // call calloc to allocate the memory and initialise it to a string of zeros
      LLVMValueRef[] arguments = new LLVMValueRef[] {llvmSize, LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 1, false)};
      LLVMValueRef memoryPointer = LLVM.LLVMBuildCall(builder, callocFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");

      LLVMValueRef isNotNull = LLVM.LLVMBuildIsNotNull(builder, memoryPointer, "");
      LLVMBasicBlockRef callocContinueBlock = LLVM.LLVMAddBasicBlock(builder, "arrayCallocContinue");
      LLVMBasicBlockRef callocFailedBlock = LLVM.LLVMAddBasicBlock(builder, "arrayCallocFailed");
      LLVM.LLVMBuildCondBr(builder, isNotNull, callocContinueBlock, callocFailedBlock);
      LLVM.LLVMPositionBuilderAtEnd(builder, callocFailedBlock);
      buildOutOfMemoryHandler(builder);

      LLVM.LLVMPositionBuilderAtEnd(builder, callocContinueBlock);
      LLVMValueRef allocatedPointer = LLVM.LLVMBuildBitCast(builder, memoryPointer, llvmArrayType, "");

      Type rttiType = Type.findTypeWithNullability(currentType, false);
      LLVMValueRef rtti = rttiHelper.buildRTTICreation(builder, true, rttiType, typeParameterAccessor);
      LLVMValueRef rttiPointer = rttiHelper.getRTTIPointer(builder, allocatedPointer);
      LLVM.LLVMBuildStore(builder, rtti, rttiPointer);

      LLVMValueRef vftPointer = virtualFunctionHandler.getFirstVirtualFunctionTablePointer(builder, allocatedPointer);
      LLVMValueRef vft = virtualFunctionHandler.getBaseChangeObjectVFT(currentType);
      LLVM.LLVMBuildStore(builder, vft, vftPointer);

      LLVMValueRef sizeElementPointer = typeHelper.getArrayLengthPointer(builder, allocatedPointer);
      LLVM.LLVMBuildStore(builder, llvmLengths[i], sizeElementPointer);

      LLVMValueRef getFunction = typeHelper.getArrayGetterFunction(currentType);
      LLVMValueRef getFunctionPointer = typeHelper.getArrayGetterFunctionPointer(builder, allocatedPointer);
      LLVM.LLVMBuildStore(builder, getFunction, getFunctionPointer);

      LLVMValueRef setFunction = typeHelper.getArraySetterFunction(currentType);
      LLVMValueRef setFunctionPointer = typeHelper.getArraySetterFunctionPointer(builder, allocatedPointer);
      LLVM.LLVMBuildStore(builder, setFunction, setFunctionPointer);


      // we have an array object in 'allocatedPointer', so we can now initialise it, if necessary
      created[i] = allocatedPointer;
      createdTypes[i] = currentType;

      if (i < llvmLengths.length - 1 || initialiserType != null)
      {
        // there is another layer to do in initialising this array, so build a loop to handle it
        LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVMBasicBlockRef exitBlock = LLVM.LLVMAddBasicBlock(builder, "arrayInitialisationEnd");
        LLVMBasicBlockRef loopBlock = LLVM.LLVMAddBasicBlock(builder, "arrayInitialisation");
        LLVMBasicBlockRef loopCheckBlock = LLVM.LLVMAddBasicBlock(builder, "arrayInitialisationCheck");

        // build the loop check
        LLVM.LLVMBuildBr(builder, loopCheckBlock);
        LLVM.LLVMPositionBuilderAtEnd(builder, loopCheckBlock);
        LLVMValueRef phiNode = LLVM.LLVMBuildPhi(builder, LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), "");
        LLVMValueRef breakBoolean = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntULT, phiNode, llvmLengths[i], "");
        LLVM.LLVMBuildCondBr(builder, breakBoolean, loopBlock, exitBlock);

        // add the initial value for the loop counter phi node
        LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false)};
        LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock};
        LLVM.LLVMAddIncoming(phiNode, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);

        // get ready for the next run of this for-loop to build the next array creation
        LLVM.LLVMPositionBuilderAtEnd(builder, loopBlock);

        // store all of the values we have just generated, so that we can fix up the loop after the sub-loops have been generated
        initialisationPhiNodes[i] = phiNode;
        initialisationLoopCheckBlocks[i] = loopCheckBlock;
        initialisationLoopBlocks[i] = loopBlock;
        if (i > 0)
        {
          // if this isn't the first loop block, then startBlock was the previous iteration's loopBlock, and we have closed it off
          // so make this loop's exitBlock into the last loop's loopBlock
          initialisationLoopBlocks[i - 1] = exitBlock;
        }
        else
        {
          // this is the first loop block, so our exitBlock must be where we resume from at the end of this array creation
          initialisationExitBlock = exitBlock;
        }
      }
      else
      {
        if (i > 0)
        {
          // we have built the creation code for this sub-array, but the code we have built may have changed where the loop cleanup for the previous array needs to go
          // in effect, we have probably ended initialisationLoopBlocks[i - 1], so we need to set it to our new position so that that loop can be terminated properly
          initialisationLoopBlocks[i - 1] = LLVM.LLVMGetInsertBlock(builder);
        }
        else
        {
          // this is the first block, and there are no loops in this array creation
          // so set the exit block to the current block
          initialisationExitBlock = LLVM.LLVMGetInsertBlock(builder);
        }
      }

      if (i < llvmLengths.length - 1)
      {
        // there is a next iteration in this for-loop, so go to the base type
        currentType = (ArrayType) currentType.getBaseType();
      }
    }

    if (initialiserType != null)
    {
      // we have to recalculate the array's base type here, as the creation above was working with stripped types, but we need the actual type for conversions from initialiser values here
      Type baseType = type;
      for (int i = 0; i < llvmLengths.length; ++i)
      {
        baseType = ((ArrayType) baseType).getBaseType();
      }

      // we need to build code to initialise the current value
      // first, find the value we need to insert into the array
      LLVMValueRef initialisedValue;
      if (initialiserIsFunction)
      {
        FunctionType functionType = (FunctionType) initialiserType;
        Type[] parameterTypes = functionType.getParameterTypes();
        LLVMValueRef initialiserCallee = LLVM.LLVMBuildExtractValue(builder, initialiserValue, 1, "");
        LLVMValueRef initialiserFunction = LLVM.LLVMBuildExtractValue(builder, initialiserValue, 2, "");
        LLVMValueRef[] initialiserParams;
        if (parameterTypes.length == 0)
        {
          // we don't need to specify any lengths
          initialiserParams = new LLVMValueRef[] {initialiserCallee};
        }
        else if (parameterTypes.length == llvmLengths.length)
        {
          // we need to specify all of the lengths
          initialiserParams = new LLVMValueRef[1 + parameterTypes.length];
          initialiserParams[0] = initialiserCallee;
          for (int i = 0; i < parameterTypes.length; ++i)
          {
            initialiserParams[1 + i] = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, initialisationPhiNodes[i], ArrayLengthMember.ARRAY_LENGTH_TYPE, parameterTypes[i], typeParameterAccessor);
          }
        }
        else
        {
          throw new IllegalArgumentException("An initialiser function must take either all of the length arguments, or no arguments at all");
        }

        // call the initialiser function
        initialisedValue = LLVM.LLVMBuildCall(builder, initialiserFunction, C.toNativePointerArray(initialiserParams, false, true), initialiserParams.length, "");
        initialisedValue = typeHelper.convertStandardToTemporary(builder, landingPadContainer, initialisedValue, functionType.getReturnType(), baseType, typeParameterAccessor);
      }
      else
      {
        initialisedValue = typeHelper.convertTemporary(builder, landingPadContainer, initialiserValue, initialiserType, baseType, false, typeParameterAccessor);
      }

      // now, store the value in created[length - 1], so that it gets inserted as the value of created[length - 2] when we fix up the loops
      created[created.length - 1] = typeHelper.convertTemporaryToStandard(builder, initialisedValue, baseType);
      // we don't actually need to set createdTypes[length - 1] here, as that is only stored for arrays
    }

    // fix up each of the loops, starting with the innermost one
    int i = llvmLengths.length - (initialiserType == null ? 1 : 0);
    while (i > 0)
    {
      --i;

      // created[i] is the array to finish the initialisation loop of, and created[i+1] is the value to store in the current index of that array
      LLVMValueRef currentLoopCounter = initialisationPhiNodes[i];

      // finish the loop block
      LLVM.LLVMPositionBuilderAtEnd(builder, initialisationLoopBlocks[i]);

      // store the element
      LLVMValueRef elementPointer = typeHelper.getNonProxiedArrayElementPointer(builder, created[i], currentLoopCounter);
      LLVMValueRef element = created[i + 1];
      if (i + 1 < createdTypes.length)
      {
        // the value we are storing is definitely an array, so before storing it in this bigger array, we need to bitcast it
        // to the standard array type representation (currently, it is in a non-proxied array representation)
        element = LLVM.LLVMBuildBitCast(builder, element, typeHelper.findStandardType(createdTypes[i + 1]), "");
      }
      LLVM.LLVMBuildStore(builder, element, elementPointer);

      // add the next iteration's loop counter to the phi node
      LLVMValueRef nextCounterValue = LLVM.LLVMBuildAdd(builder, currentLoopCounter, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false), "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {nextCounterValue};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {LLVM.LLVMGetInsertBlock(builder)};
      LLVM.LLVMAddIncoming(currentLoopCounter, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);

      LLVM.LLVMBuildBr(builder, initialisationLoopCheckBlocks[i]);
    }

    // all of the loops have been fixed-up, so return to the initialisation exit block and return the created array
    LLVM.LLVMPositionBuilderAtEnd(builder, initialisationExitBlock);
    // currently, the array we need to return is in a non-proxied array representation, so bitcast it to the normal array represenation before returning
    return LLVM.LLVMBuildBitCast(builder, created[0], typeHelper.findTemporaryType(createdTypes[0]), "");
  }

  /**
   * Builds the LLVM statements for a null check on the specified value.
   * @param builder - the LLVMBuilderRef to build instructions with
   * @param value - the LLVMValueRef to compare to null, in a temporary native representation
   * @param type - the type of the specified LLVMValueRef
   * @return an LLVMValueRef for an i1, which will be 1 if the value is non-null, and 0 if the value is null
   */
  public LLVMValueRef buildNullCheck(LLVMBuilderRef builder, LLVMValueRef value, Type type)
  {
    if (!type.canBeNullable())
    {
      throw new IllegalArgumentException("A null check can only work on a nullable type");
    }
    if (type instanceof ArrayType)
    {
      return LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntNE, value, LLVM.LLVMConstNull(typeHelper.findTemporaryType(type)), "");
    }
    if (type instanceof FunctionType)
    {
      LLVMValueRef functionPointer = LLVM.LLVMBuildExtractValue(builder, value, 2, "");
      LLVMTypeRef llvmFunctionPointerType = typeHelper.findRawFunctionPointerType((FunctionType) type);
      return LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntNE, functionPointer, LLVM.LLVMConstNull(llvmFunctionPointerType), "");
    }
    if (type instanceof NamedType)
    {
      TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        return LLVM.LLVMBuildIsNotNull(builder, value, "");
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        // a compound type with a temporary native representation is a pointer which may or may not be null
        return LLVM.LLVMBuildIsNotNull(builder, value, "");
      }
      else if (typeDefinition instanceof InterfaceDefinition)
      {
        // extract the object pointer from the interface representation, and check whether it is null
        LLVMValueRef objectValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
        return LLVM.LLVMBuildIsNotNull(builder, objectValue, "");
      }
    }
    if (type instanceof NullType)
    {
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
    }
    if (type instanceof ObjectType ||
        (type instanceof NamedType && ((NamedType) type).getResolvedTypeParameter() != null))
    {
      return LLVM.LLVMBuildIsNotNull(builder, value, "");
    }
    if (type instanceof PrimitiveType)
    {
      return LLVM.LLVMBuildExtractValue(builder, value, 0, "");
    }
    if (type instanceof TupleType)
    {
      return LLVM.LLVMBuildExtractValue(builder, value, 0, "");
    }
    throw new IllegalArgumentException("Cannot build a null check for the unrecognised type: " + type);
  }

  /**
   * Builds the LLVM statements for an equality check between the specified two values, which are both of the specified type.
   * The equality check either checks whether the values are equal, or not equal, depending on the EqualityOperator provided.
   * @param left - the left LLVMValueRef in the comparison, in a temporary native representation
   * @param right - the right LLVMValueRef in the comparison, in a temporary native representation
   * @param type - the Type of both of the values - both of the values should be converted to this type before this function is called
   * @param operator - the EqualityOperator which determines which way to compare the values (e.g. EQUAL results in a 1 iff the values are equal)
   * @return an LLVMValueRef for an i1, which will be 1 if the check returns true, or 0 if the check returns false
   */
  private LLVMValueRef buildEqualityCheck(LLVMBuilderRef builder, LLVMValueRef left, LLVMValueRef right, Type type, EqualityOperator operator)
  {
    if (type instanceof ArrayType)
    {
      return LLVM.LLVMBuildICmp(builder, getPredicate(operator, false), left, right, "");
    }
    if (type instanceof FunctionType)
    {
      LLVMValueRef leftOpaque = LLVM.LLVMBuildExtractValue(builder, left, 1, "");
      LLVMValueRef rightOpaque = LLVM.LLVMBuildExtractValue(builder, right, 1, "");
      LLVMValueRef opaqueComparison = LLVM.LLVMBuildICmp(builder, getPredicate(operator, false), leftOpaque, rightOpaque, "");
      LLVMValueRef leftFunction = LLVM.LLVMBuildExtractValue(builder, left, 2, "");
      LLVMValueRef rightFunction = LLVM.LLVMBuildExtractValue(builder, right, 2, "");
      LLVMValueRef functionComparison = LLVM.LLVMBuildICmp(builder, getPredicate(operator, false), leftFunction, rightFunction, "");
      if (operator == EqualityOperator.EQUAL)
      {
        return LLVM.LLVMBuildAnd(builder, opaqueComparison, functionComparison, "");
      }
      if (operator == EqualityOperator.NOT_EQUAL)
      {
        return LLVM.LLVMBuildOr(builder, opaqueComparison, functionComparison, "");
      }
      throw new IllegalArgumentException("Cannot build an equality check without a valid EqualityOperator");
    }
    if (type instanceof NamedType)
    {
      TypeDefinition typeDefinition = ((NamedType) type).getResolvedTypeDefinition();
      if (typeDefinition instanceof ClassDefinition)
      {
        return LLVM.LLVMBuildICmp(builder, getPredicate(operator, false), left, right, "");
      }
      if (typeDefinition instanceof CompoundDefinition)
      {
        // we don't want to compare anything if one of the compound definitions is null, so we need to branch and only compare them if they are both not-null
        LLVMValueRef nullityComparison = null;
        LLVMBasicBlockRef startBlock = null;
        LLVMBasicBlockRef finalBlock = null;
        if (type.canBeNullable())
        {
          LLVMValueRef leftNullity = LLVM.LLVMBuildIsNotNull(builder, left, "");
          LLVMValueRef rightNullity = LLVM.LLVMBuildIsNotNull(builder, right, "");
          nullityComparison = LLVM.LLVMBuildICmp(builder, getPredicate(operator, false), leftNullity, rightNullity, "");
          LLVMValueRef bothNotNull = LLVM.LLVMBuildAnd(builder, leftNullity, rightNullity, "");

          startBlock = LLVM.LLVMGetInsertBlock(builder);
          finalBlock = LLVM.LLVMAddBasicBlock(builder, "equality_final");
          LLVMBasicBlockRef comparisonBlock = LLVM.LLVMAddBasicBlock(builder, "equality_comparevalues");

          LLVM.LLVMBuildCondBr(builder, bothNotNull, comparisonBlock, finalBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, comparisonBlock);
        }

        // compare each of the member variables from the left and right values
        MemberVariable[] memberVariables = ((CompoundDefinition) typeDefinition).getMemberVariables();
        LLVMValueRef[] compareResults = new LLVMValueRef[memberVariables.length];
        for (int i = 0; i < memberVariables.length; ++i)
        {
          Type variableType = memberVariables[i].getType();
          LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), i, false)};
          LLVMValueRef leftField = LLVM.LLVMBuildGEP(builder, left, C.toNativePointerArray(indices, false, true), indices.length, "");
          LLVMValueRef rightField = LLVM.LLVMBuildGEP(builder, right, C.toNativePointerArray(indices, false, true), indices.length, "");
          LLVMValueRef leftValue = LLVM.LLVMBuildLoad(builder, leftField, "");
          LLVMValueRef rightValue = LLVM.LLVMBuildLoad(builder, rightField, "");
          leftValue = typeHelper.convertStandardToTemporary(builder, leftValue, variableType);
          rightValue = typeHelper.convertStandardToTemporary(builder, rightValue, variableType);
          compareResults[i] = buildEqualityCheck(builder, leftValue, rightValue, variableType, operator);
        }

        // AND or OR the list together, using a binary tree
        int multiple = 1;
        while (multiple < memberVariables.length)
        {
          for (int i = 0; i < memberVariables.length; i += 2 * multiple)
          {
            LLVMValueRef first = compareResults[i];
            if (i + multiple >= memberVariables.length)
            {
              continue;
            }
            LLVMValueRef second = compareResults[i + multiple];
            LLVMValueRef result = null;
            if (operator == EqualityOperator.EQUAL)
            {
              result = LLVM.LLVMBuildAnd(builder, first, second, "");
            }
            else if (operator == EqualityOperator.NOT_EQUAL)
            {
              result = LLVM.LLVMBuildOr(builder, first, second, "");
            }
            compareResults[i] = result;
          }
          multiple *= 2;
        }
        LLVMValueRef normalComparison = compareResults[0];

        if (type.canBeNullable())
        {
          LLVMBasicBlockRef endComparisonBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildBr(builder, finalBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, finalBlock);
          LLVMValueRef phiNode = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt1Type(), "");
          LLVMValueRef[] incomingValues = new LLVMValueRef[] {nullityComparison, normalComparison};
          LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endComparisonBlock};
          LLVM.LLVMAddIncoming(phiNode, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
          return phiNode;
        }
        return normalComparison;
      }
      if (typeDefinition instanceof InterfaceDefinition)
      {
        // extract the object pointer from each of the interface representations, and check whether they are equal
        LLVMValueRef leftObject = LLVM.LLVMBuildExtractValue(builder, left, 1, "");
        LLVMValueRef rightObject = LLVM.LLVMBuildExtractValue(builder, right, 1, "");
        return LLVM.LLVMBuildICmp(builder, getPredicate(operator, false), leftObject, rightObject, "");
      }
    }
    if (type instanceof NullType)
    {
      // two null-typed values are always equal
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
    }
    if (type instanceof ObjectType ||
        (type instanceof NamedType && ((NamedType) type).getResolvedTypeParameter() != null))
    {
      // compare the two pointers
      return LLVM.LLVMBuildICmp(builder, getPredicate(operator, false), left, right, "");
    }
    if (type instanceof PrimitiveType)
    {
      PrimitiveTypeType primitiveTypeType = ((PrimitiveType) type).getPrimitiveTypeType();
      LLVMValueRef leftValue = left;
      LLVMValueRef rightValue = right;
      if (type.canBeNullable())
      {
        leftValue = LLVM.LLVMBuildExtractValue(builder, left, 1, "");
        rightValue = LLVM.LLVMBuildExtractValue(builder, right, 1, "");
      }
      LLVMValueRef valueEqualityResult;
      if (primitiveTypeType.isFloating())
      {
        valueEqualityResult = LLVM.LLVMBuildFCmp(builder, getPredicate(operator, true), leftValue, rightValue, "");
      }
      else
      {
        valueEqualityResult = LLVM.LLVMBuildICmp(builder, getPredicate(operator, false), leftValue, rightValue, "");
      }
      if (type.canBeNullable())
      {
        LLVMValueRef leftNullity = LLVM.LLVMBuildExtractValue(builder, left, 0, "");
        LLVMValueRef rightNullity = LLVM.LLVMBuildExtractValue(builder, right, 0, "");
        LLVMValueRef bothNotNull = LLVM.LLVMBuildAnd(builder, leftNullity, rightNullity, "");
        LLVMValueRef notNullAndValueResult = LLVM.LLVMBuildAnd(builder, bothNotNull, valueEqualityResult, "");
        LLVMValueRef nullityComparison;
        if (operator == EqualityOperator.EQUAL)
        {
          nullityComparison = LLVM.LLVMBuildNot(builder, LLVM.LLVMBuildOr(builder, leftNullity, rightNullity, ""), "");
        }
        else
        {
          nullityComparison = LLVM.LLVMBuildXor(builder, leftNullity, rightNullity, "");
        }
        return LLVM.LLVMBuildOr(builder, notNullAndValueResult, nullityComparison, "");
      }
      return valueEqualityResult;
    }
    if (type instanceof TupleType)
    {
      // we don't want to compare anything if one of the tuples is null, so we need to branch and only compare them if they are both not-null
      LLVMValueRef nullityComparison = null;
      LLVMBasicBlockRef startBlock = null;
      LLVMBasicBlockRef finalBlock = null;
      LLVMValueRef leftNotNull = left;
      LLVMValueRef rightNotNull = right;
      if (type.canBeNullable())
      {
        LLVMValueRef leftNullity = LLVM.LLVMBuildExtractValue(builder, left, 0, "");
        LLVMValueRef rightNullity = LLVM.LLVMBuildExtractValue(builder, right, 0, "");
        nullityComparison = LLVM.LLVMBuildICmp(builder, getPredicate(operator, false), leftNullity, rightNullity, "");
        LLVMValueRef bothNotNull = LLVM.LLVMBuildAnd(builder, leftNullity, rightNullity, "");

        startBlock = LLVM.LLVMGetInsertBlock(builder);
        finalBlock = LLVM.LLVMAddBasicBlock(builder, "equality_final");
        LLVMBasicBlockRef comparisonBlock = LLVM.LLVMAddBasicBlock(builder, "equality_comparevalues");

        LLVM.LLVMBuildCondBr(builder, bothNotNull, comparisonBlock, finalBlock);
        LLVM.LLVMPositionBuilderAtEnd(builder, comparisonBlock);

        leftNotNull = LLVM.LLVMBuildExtractValue(builder, left, 1, "");
        rightNotNull = LLVM.LLVMBuildExtractValue(builder, right, 1, "");
      }

      // compare each of the fields from the left and right values
      Type[] subTypes = ((TupleType) type).getSubTypes();
      LLVMValueRef[] compareResults = new LLVMValueRef[subTypes.length];
      for (int i = 0; i < subTypes.length; ++i)
      {
        Type subType = subTypes[i];
        LLVMValueRef leftValue = LLVM.LLVMBuildExtractValue(builder, leftNotNull, i, "");
        LLVMValueRef rightValue = LLVM.LLVMBuildExtractValue(builder, rightNotNull, i, "");
        compareResults[i] = buildEqualityCheck(builder, leftValue, rightValue, subType, operator);
      }

      // AND or OR the list together, using a binary tree
      int multiple = 1;
      while (multiple < subTypes.length)
      {
        for (int i = 0; i < subTypes.length; i += 2 * multiple)
        {
          LLVMValueRef first = compareResults[i];
          if (i + multiple >= subTypes.length)
          {
            continue;
          }
          LLVMValueRef second = compareResults[i + multiple];
          LLVMValueRef result = null;
          if (operator == EqualityOperator.EQUAL)
          {
            result = LLVM.LLVMBuildAnd(builder, first, second, "");
          }
          else if (operator == EqualityOperator.NOT_EQUAL)
          {
            result = LLVM.LLVMBuildOr(builder, first, second, "");
          }
          compareResults[i] = result;
        }
        multiple *= 2;
      }
      LLVMValueRef normalComparison = compareResults[0];

      if (type.canBeNullable())
      {
        LLVMBasicBlockRef endComparisonBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildBr(builder, finalBlock);
        LLVM.LLVMPositionBuilderAtEnd(builder, finalBlock);
        LLVMValueRef phiNode = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt1Type(), "");
        LLVMValueRef[] incomingValues = new LLVMValueRef[] {nullityComparison, normalComparison};
        LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endComparisonBlock};
        LLVM.LLVMAddIncoming(phiNode, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
        return phiNode;
      }
      return normalComparison;
    }
    throw new IllegalArgumentException("Cannot compare two values of type '" + type + "' for equality");
  }

  private LLVMValueRef buildExpression(Expression expression, LLVMBuilderRef builder, LLVMValueRef thisValue, Map<Variable, LLVMValueRef> variables, LandingPadContainer landingPadContainer, TypeParameterAccessor typeParameterAccessor)
  {
    if (expression instanceof ArithmeticExpression)
    {
      ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;
      LLVMValueRef left = buildExpression(arithmeticExpression.getLeftSubExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      LLVMValueRef right = buildExpression(arithmeticExpression.getRightSubExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      Type leftType = arithmeticExpression.getLeftSubExpression().getType();
      Type rightType = arithmeticExpression.getRightSubExpression().getType();
      Type resultType = arithmeticExpression.getType();
      if (arithmeticExpression.getOperator() == ArithmeticOperator.ADD && resultType.isRuntimeEquivalent(SpecialTypeHandler.STRING_TYPE))
      {
        LLVMValueRef leftString = typeHelper.convertToString(builder, landingPadContainer, left, leftType, typeParameterAccessor);
        LLVMValueRef rightString = typeHelper.convertToString(builder, landingPadContainer, right, rightType, typeParameterAccessor);
        LLVMValueRef result = buildStringConcatenation(builder, landingPadContainer, leftString, rightString);
        return typeHelper.convertTemporary(builder, landingPadContainer, result, SpecialTypeHandler.STRING_TYPE, resultType, false, typeParameterAccessor);
      }
      // cast if necessary
      left = typeHelper.convertTemporary(builder, landingPadContainer, left, leftType, resultType, false, typeParameterAccessor);
      right = typeHelper.convertTemporary(builder, landingPadContainer, right, rightType, resultType, false, typeParameterAccessor);
      boolean floating = ((PrimitiveType) resultType).getPrimitiveTypeType().isFloating();
      boolean signed = ((PrimitiveType) resultType).getPrimitiveTypeType().isSigned();
      switch (arithmeticExpression.getOperator())
      {
      case ADD:
        return floating ? LLVM.LLVMBuildFAdd(builder, left, right, "") : LLVM.LLVMBuildAdd(builder, left, right, "");
      case SUBTRACT:
        return floating ? LLVM.LLVMBuildFSub(builder, left, right, "") : LLVM.LLVMBuildSub(builder, left, right, "");
      case MULTIPLY:
        return floating ? LLVM.LLVMBuildFMul(builder, left, right, "") : LLVM.LLVMBuildMul(builder, left, right, "");
      case DIVIDE:
        return floating ? LLVM.LLVMBuildFDiv(builder, left, right, "") : signed ? LLVM.LLVMBuildSDiv(builder, left, right, "") : LLVM.LLVMBuildUDiv(builder, left, right, "");
      case REMAINDER:
        return floating ? LLVM.LLVMBuildFRem(builder, left, right, "") : signed ? LLVM.LLVMBuildSRem(builder, left, right, "") : LLVM.LLVMBuildURem(builder, left, right, "");
      case MODULO:
        if (floating)
        {
          LLVMValueRef rem = LLVM.LLVMBuildFRem(builder, left, right, "");
          LLVMValueRef add = LLVM.LLVMBuildFAdd(builder, rem, right, "");
          return LLVM.LLVMBuildFRem(builder, add, right, "");
        }
        if (signed)
        {
          LLVMValueRef rem = LLVM.LLVMBuildSRem(builder, left, right, "");
          LLVMValueRef add = LLVM.LLVMBuildAdd(builder, rem, right, "");
          return LLVM.LLVMBuildSRem(builder, add, right, "");
        }
        // unsigned modulo is the same as unsigned remainder
        return LLVM.LLVMBuildURem(builder, left, right, "");
      }
      throw new IllegalArgumentException("Unknown arithmetic operator: " + arithmeticExpression.getOperator());
    }
    if (expression instanceof ArrayAccessExpression)
    {
      ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
      LLVMValueRef arrayValue = buildExpression(arrayAccessExpression.getArrayExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      LLVMValueRef dimensionValue = buildExpression(arrayAccessExpression.getDimensionExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      LLVMValueRef convertedDimensionValue = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, dimensionValue, arrayAccessExpression.getDimensionExpression().getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, typeParameterAccessor);

      LLVMValueRef result = typeHelper.buildRetrieveArrayElement(builder, landingPadContainer, arrayValue, convertedDimensionValue);
      ArrayType arrayType = (ArrayType) arrayAccessExpression.getArrayExpression().getType();
      return typeHelper.convertTemporary(builder, landingPadContainer, result, arrayType.getBaseType(), arrayAccessExpression.getType(), false, typeParameterAccessor);
    }
    if (expression instanceof ArrayCreationExpression)
    {
      ArrayCreationExpression arrayCreationExpression = (ArrayCreationExpression) expression;
      ArrayType type = arrayCreationExpression.getDeclaredType();
      Expression[] dimensionExpressions = arrayCreationExpression.getDimensionExpressions();

      if (dimensionExpressions == null)
      {
        Expression[] valueExpressions = arrayCreationExpression.getValueExpressions();
        LLVMValueRef llvmLength = LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), valueExpressions.length, false);
        LLVMValueRef array = buildArrayCreation(builder, landingPadContainer, new LLVMValueRef[] {llvmLength}, type, null, null, false, typeParameterAccessor);
        for (int i = 0; i < valueExpressions.length; i++)
        {
          LLVMValueRef expressionValue = buildExpression(valueExpressions[i], builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
          LLVMValueRef convertedValue = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, expressionValue, valueExpressions[i].getType(), type.getBaseType(), typeParameterAccessor);
          LLVMValueRef index = LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), i, false);
          typeHelper.buildStoreArrayElement(builder, landingPadContainer, array, index, convertedValue);
        }
        return typeHelper.convertTemporary(builder, landingPadContainer, array, type, arrayCreationExpression.getType(), false, typeParameterAccessor);
      }

      LLVMValueRef[] llvmLengths = new LLVMValueRef[dimensionExpressions.length];
      for (int i = 0; i < llvmLengths.length; i++)
      {
        LLVMValueRef expressionValue = buildExpression(dimensionExpressions[i], builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        llvmLengths[i] = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, expressionValue, dimensionExpressions[i].getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, typeParameterAccessor);
      }
      Type initialiserType = null;
      LLVMValueRef initialiserValue = null;
      boolean initialiserIsFunction = false;
      if (arrayCreationExpression.getInitialisationExpression() != null)
      {
        Expression initialiser = arrayCreationExpression.getInitialisationExpression();
        initialiserType = initialiser.getType();
        initialiserValue = buildExpression(initialiser, builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        initialiserIsFunction = arrayCreationExpression.getResolvedIsInitialiserFunction();
      }
      LLVMValueRef array = buildArrayCreation(builder, landingPadContainer, llvmLengths, type, initialiserValue, initialiserType, initialiserIsFunction, typeParameterAccessor);
      return typeHelper.convertTemporary(builder, landingPadContainer, array, type, arrayCreationExpression.getType(), false, typeParameterAccessor);
    }
    if (expression instanceof BitwiseNotExpression)
    {
      LLVMValueRef value = buildExpression(((BitwiseNotExpression) expression).getExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      value = typeHelper.convertTemporary(builder, landingPadContainer, value, ((BitwiseNotExpression) expression).getExpression().getType(), expression.getType(), false, typeParameterAccessor);
      return LLVM.LLVMBuildNot(builder, value, "");
    }
    if (expression instanceof BooleanLiteralExpression)
    {
      LLVMValueRef value = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), ((BooleanLiteralExpression) expression).getValue() ? 1 : 0, false);
      return typeHelper.convertStandardToTemporary(builder, landingPadContainer, value, new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), expression.getType(), typeParameterAccessor);
    }
    if (expression instanceof BooleanNotExpression)
    {
      LLVMValueRef value = buildExpression(((BooleanNotExpression) expression).getExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      LLVMValueRef result = LLVM.LLVMBuildNot(builder, value, "");
      return typeHelper.convertTemporary(builder, landingPadContainer, result, ((BooleanNotExpression) expression).getExpression().getType(), expression.getType(), false, typeParameterAccessor);
    }
    if (expression instanceof BracketedExpression)
    {
      BracketedExpression bracketedExpression = (BracketedExpression) expression;
      LLVMValueRef value = buildExpression(bracketedExpression.getExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      return typeHelper.convertTemporary(builder, landingPadContainer, value, bracketedExpression.getExpression().getType(), expression.getType(), false, typeParameterAccessor);
    }
    if (expression instanceof CastExpression)
    {
      CastExpression castExpression = (CastExpression) expression;
      LLVMValueRef value = buildExpression(castExpression.getExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      return typeHelper.convertTemporary(builder, landingPadContainer, value, castExpression.getExpression().getType(), castExpression.getType(), false, typeParameterAccessor);
    }
    if (expression instanceof CreationExpression)
    {
      CreationExpression creationExpression = (CreationExpression) expression;
      Expression[] arguments = creationExpression.getArguments();
      ConstructorReference constructorReference = creationExpression.getResolvedConstructorReference();
      Type[] parameterTypes = constructorReference.getParameterTypes();

      Constructor constructor = constructorReference.getReferencedMember();
      Parameter[] realParameters = constructor.getParameters();
      GenericTypeSpecialiser contextSpecialiser = new GenericTypeSpecialiser(constructorReference.getContainingType());

      LLVMValueRef[] llvmArguments = new LLVMValueRef[1 + arguments.length];
      for (int i = 0; i < arguments.length; ++i)
      {
        LLVMValueRef argument = buildExpression(arguments[i], builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        argument = typeHelper.convertTemporary(builder, landingPadContainer, argument, arguments[i].getType(), parameterTypes[i], false, typeParameterAccessor);

        Type contextualArgumentType = typeHelper.getContextualisedActualMemberType(realParameters[i].getType(), contextSpecialiser);
        llvmArguments[i + 1] = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, argument, parameterTypes[i], contextualArgumentType, typeParameterAccessor);
      }
      NamedType type = creationExpression.getCreatedType();
      LLVMValueRef pointer;
      if (creationExpression.isHeapAllocation())
      {
        if (!(type.getResolvedTypeDefinition() instanceof ClassDefinition))
        {
          throw new IllegalStateException("A 'new' creation expression must be for a class type");
        }
        ClassDefinition classDefinition = (ClassDefinition) type.getResolvedTypeDefinition();
        TypeParameter[] typeParameters = classDefinition.getTypeParameters();
        Type[] typeArguments = type.getTypeArguments();
        LLVMValueRef[] allocatorArgs = new LLVMValueRef[typeParameters.length];
        for (int i = 0; i < typeParameters.length; ++i)
        {
          allocatorArgs[i] = rttiHelper.buildRTTICreation(builder, false, typeArguments[i], typeParameterAccessor);
        }
        LLVMBasicBlockRef allocatorInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "allocatorInvokeContinue");
        pointer = LLVM.LLVMBuildInvoke(builder, getAllocatorFunction(classDefinition), C.toNativePointerArray(allocatorArgs, false, true), allocatorArgs.length, allocatorInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
        LLVM.LLVMPositionBuilderAtEnd(builder, allocatorInvokeContinueBlock);
      }
      else
      {
        if (!(type.getResolvedTypeDefinition() instanceof CompoundDefinition))
        {
          throw new IllegalStateException("A 'create' creation expression must be for a compound type");
        }
        // find the type to alloca, which is the standard representation of a non-nullable type
        // when we alloca this standard type, it becomes equivalent to the temporary type representation of this compound type (with either nullability)
        LLVMTypeRef allocaBaseType = typeHelper.findStandardType(type);
        pointer = LLVM.LLVMBuildAllocaInEntryBlock(builder, allocaBaseType, "");
        typeHelper.initialiseCompoundType(builder, type, pointer, typeParameterAccessor);
      }
      llvmArguments[0] = pointer;
      // get the constructor and call it
      LLVMValueRef llvmFunc = getConstructorFunction(constructor);
      LLVMBasicBlockRef constructorInvokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "constructorInvokeContinue");
      LLVM.LLVMBuildInvoke(builder, llvmFunc, C.toNativePointerArray(llvmArguments, false, true), llvmArguments.length, constructorInvokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
      LLVM.LLVMPositionBuilderAtEnd(builder, constructorInvokeContinueBlock);
      return typeHelper.convertTemporary(builder, landingPadContainer, pointer, type, creationExpression.getType(), false, typeParameterAccessor);
    }
    if (expression instanceof EqualityExpression)
    {
      EqualityExpression equalityExpression = (EqualityExpression) expression;
      EqualityOperator operator = equalityExpression.getOperator();
      // if the type checker has annotated this as a null check, just perform it without building both sub-expressions
      Expression nullCheckExpression = equalityExpression.getNullCheckExpression();
      if (nullCheckExpression != null)
      {
        LLVMValueRef value = buildExpression(nullCheckExpression, builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        LLVMValueRef convertedValue = typeHelper.convertTemporary(builder, landingPadContainer, value, nullCheckExpression.getType(), equalityExpression.getComparisonType(), false, typeParameterAccessor);
        LLVMValueRef nullity = buildNullCheck(builder, convertedValue, equalityExpression.getComparisonType());
        switch (operator)
        {
        case EQUAL:
          return LLVM.LLVMBuildNot(builder, nullity, "");
        case NOT_EQUAL:
          return nullity;
        default:
          throw new IllegalArgumentException("Cannot build an EqualityExpression with no EqualityOperator");
        }
      }

      LLVMValueRef left = buildExpression(equalityExpression.getLeftSubExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      LLVMValueRef right = buildExpression(equalityExpression.getRightSubExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      Type leftType = equalityExpression.getLeftSubExpression().getType();
      Type rightType = equalityExpression.getRightSubExpression().getType();
      Type comparisonType = equalityExpression.getComparisonType();
      // if comparisonType is null, then the types are integers which cannot be assigned to each other either way around, because one is signed and the other is unsigned
      // so we must extend each of them to a larger bitCount which they can both fit into, and compare them there
      if (comparisonType == null)
      {
        if (!(leftType instanceof PrimitiveType) || !(rightType instanceof PrimitiveType))
        {
          throw new IllegalStateException("A comparison type must be provided if either the left or right type is not a PrimitiveType: " + equalityExpression);
        }
        LLVMValueRef leftIsNotNull = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
        LLVMValueRef rightIsNotNull = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false);
        LLVMValueRef leftValue = left;
        LLVMValueRef rightValue = right;
        if (leftType.canBeNullable())
        {
          leftIsNotNull = LLVM.LLVMBuildExtractValue(builder, left, 0, "");
          leftValue = LLVM.LLVMBuildExtractValue(builder, left, 1, "");
        }
        if (rightType.canBeNullable())
        {
          rightIsNotNull = LLVM.LLVMBuildExtractValue(builder, right, 0, "");
          rightValue = LLVM.LLVMBuildExtractValue(builder, right, 1, "");
        }
        PrimitiveTypeType leftTypeType = ((PrimitiveType) leftType).getPrimitiveTypeType();
        PrimitiveTypeType rightTypeType = ((PrimitiveType) rightType).getPrimitiveTypeType();
        if (!leftTypeType.isFloating() && !rightTypeType.isFloating() &&
            leftTypeType.isSigned() != rightTypeType.isSigned())
        {
          // compare the signed and non-signed integers as (bitCount + 1) bit numbers, since they will not fit in bitCount bits
          int bitCount = Math.max(leftTypeType.getBitCount(), rightTypeType.getBitCount()) + 1;
          LLVMTypeRef llvmComparisonType = LLVM.LLVMIntType(bitCount);
          if (leftTypeType.isSigned())
          {
            leftValue = LLVM.LLVMBuildSExt(builder, leftValue, llvmComparisonType, "");
            rightValue = LLVM.LLVMBuildZExt(builder, rightValue, llvmComparisonType, "");
          }
          else
          {
            leftValue = LLVM.LLVMBuildZExt(builder, leftValue, llvmComparisonType, "");
            rightValue = LLVM.LLVMBuildSExt(builder, rightValue, llvmComparisonType, "");
          }
          LLVMValueRef comparisonResult = LLVM.LLVMBuildICmp(builder, getPredicate(equalityExpression.getOperator(), false), leftValue, rightValue, "");
          if (leftType.canBeNullable() || rightType.canBeNullable())
          {
            LLVMValueRef nullityComparison = LLVM.LLVMBuildICmp(builder, getPredicate(equalityExpression.getOperator(), false), leftIsNotNull, rightIsNotNull, "");
            if (equalityExpression.getOperator() == EqualityOperator.EQUAL)
            {
              comparisonResult = LLVM.LLVMBuildAnd(builder, nullityComparison, comparisonResult, "");
            }
            else
            {
              comparisonResult = LLVM.LLVMBuildOr(builder, nullityComparison, comparisonResult, "");
            }
          }
          return typeHelper.convertTemporary(builder, landingPadContainer, comparisonResult, new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), equalityExpression.getType(), false, typeParameterAccessor);
        }
        throw new IllegalArgumentException("Unknown result type, unable to generate comparison expression: " + equalityExpression);
      }
      // perform a standard equality check, using buildEqualityCheck()
      left = typeHelper.convertTemporary(builder, landingPadContainer, left, leftType, comparisonType, false, typeParameterAccessor);
      right = typeHelper.convertTemporary(builder, landingPadContainer, right, rightType, comparisonType, false, typeParameterAccessor);
      return buildEqualityCheck(builder, left, right, comparisonType, operator);
    }
    if (expression instanceof FieldAccessExpression)
    {
      FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) expression;
      MemberReference<?> memberReference = fieldAccessExpression.getResolvedMemberReference();

      Expression baseExpression = fieldAccessExpression.getBaseExpression();
      if (baseExpression != null)
      {
        LLVMValueRef baseValue = buildExpression(baseExpression, builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        Type notNullType = baseExpression.getType();
        LLVMValueRef notNullValue = baseValue;
        LLVMBasicBlockRef startBlock = null;
        LLVMBasicBlockRef continuationBlock = null;
        if (fieldAccessExpression.isNullTraversing())
        {
          LLVMValueRef nullCheckResult = buildNullCheck(builder, baseValue, baseExpression.getType());
          continuationBlock = LLVM.LLVMAddBasicBlock(builder, "nullTraversalContinuation");
          LLVMBasicBlockRef accessBlock = LLVM.LLVMAddBasicBlock(builder, "nullTraversalAccess");
          startBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildCondBr(builder, nullCheckResult, accessBlock, continuationBlock);

          LLVM.LLVMPositionBuilderAtEnd(builder, accessBlock);
          notNullType = Type.findTypeWithNullability(baseExpression.getType(), false);
          notNullValue = typeHelper.convertTemporary(builder, landingPadContainer, baseValue, baseExpression.getType(), notNullType, false, typeParameterAccessor);
        }

        LLVMValueRef result;
        if (memberReference instanceof ArrayLengthMemberReference)
        {
          LLVMValueRef elementPointer = typeHelper.getArrayLengthPointer(builder, notNullValue);
          result = LLVM.LLVMBuildLoad(builder, elementPointer, "");
          result = typeHelper.convertStandardToTemporary(builder, landingPadContainer, result, ArrayLengthMember.ARRAY_LENGTH_TYPE, fieldAccessExpression.getType(), typeParameterAccessor);
        }
        else if (memberReference instanceof FieldReference)
        {
          FieldReference fieldReference = (FieldReference) memberReference;
          Field field = fieldReference.getReferencedMember();
          if (field.isStatic())
          {
            throw new IllegalStateException("A FieldAccessExpression for a static field should not have a base expression");
          }
          LLVMValueRef fieldPointer = typeHelper.getMemberPointer(builder, notNullValue, field.getMemberVariable());

          // convert from the underlying field's type to the referenced type that we are expecting in this context
          GenericTypeSpecialiser genericTypeSpecialiser = GenericTypeSpecialiser.IDENTITY_SPECIALISER;
          if (fieldReference.getContainingType() != null)
          {
            genericTypeSpecialiser = new GenericTypeSpecialiser(fieldReference.getContainingType());
          }
          Type contextualisedType = typeHelper.getContextualisedActualMemberType(field.getType(), genericTypeSpecialiser);
          result = typeHelper.convertStandardPointerToTemporary(builder, landingPadContainer, fieldPointer, contextualisedType, fieldReference.getType(), typeParameterAccessor);
          result = typeHelper.convertTemporary(builder, landingPadContainer, result, fieldReference.getType(), fieldAccessExpression.getType(), false, typeParameterAccessor);
        }
        else if (memberReference instanceof PropertyReference)
        {
          PropertyReference propertyReference = (PropertyReference) memberReference;
          if (propertyReference.getReferencedMember().isStatic())
          {
            throw new IllegalStateException("A FieldAccessExpression for a static property should not have a base expression");
          }
          result = typeHelper.buildPropertyGetterFunctionCall(builder, landingPadContainer, notNullValue, notNullType, propertyReference, typeParameterAccessor);
          result = typeHelper.convertTemporary(builder, landingPadContainer, result, propertyReference.getType(), fieldAccessExpression.getType(), false, typeParameterAccessor);
        }
        else if (memberReference instanceof MethodReference)
        {
          MethodReference methodReference = (MethodReference) memberReference;
          if (methodReference.getReferencedMember().isStatic())
          {
            throw new IllegalStateException("A FieldAccessExpression for a static method should not have a base expression");
          }

          result = typeHelper.extractMethodFunction(builder, landingPadContainer, notNullValue, notNullType, methodReference, false, typeParameterAccessor);
          FunctionType resultFunctionType = new FunctionType(false, methodReference.getReferencedMember().isImmutable(), methodReference.getReturnType(), methodReference.getParameterTypes(), methodReference.getCheckedThrownTypes(), null);
          result = typeHelper.convertTemporary(builder, landingPadContainer, result, resultFunctionType, fieldAccessExpression.getType(), false, typeParameterAccessor);
        }
        else
        {
          throw new IllegalArgumentException("Unknown member type for a FieldAccessExpression: " + memberReference);
        }

        if (fieldAccessExpression.isNullTraversing())
        {
          LLVMBasicBlockRef accessBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVM.LLVMBuildBr(builder, continuationBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
          LLVMValueRef phiNode = LLVM.LLVMBuildPhi(builder, typeHelper.findTemporaryType(fieldAccessExpression.getType()), "");
          LLVMValueRef nullAlternative = LLVM.LLVMConstNull(typeHelper.findTemporaryType(fieldAccessExpression.getType()));
          LLVMValueRef[] phiValues = new LLVMValueRef[] {result, nullAlternative};
          LLVMBasicBlockRef[] phiBlocks = new LLVMBasicBlockRef[] {accessBlock, startBlock};
          LLVM.LLVMAddIncoming(phiNode, C.toNativePointerArray(phiValues, false, true), C.toNativePointerArray(phiBlocks, false, true), phiValues.length);
          return phiNode;
        }
        return result;
      }

      // we don't have a base expression, so handle the static field accesses
      if (memberReference instanceof FieldReference)
      {
        FieldReference fieldReference = (FieldReference) memberReference;
        Field field = fieldReference.getReferencedMember();
        if (!field.isStatic())
        {
          throw new IllegalStateException("A FieldAccessExpression for a non-static field should have a base expression");
        }
        LLVMValueRef global = getGlobal(field.getGlobalVariable());

        // convert from the underlying field's type to the referenced type that we are expecting in this context
        GenericTypeSpecialiser genericTypeSpecialiser = GenericTypeSpecialiser.IDENTITY_SPECIALISER;
        if (fieldReference.getContainingType() != null)
        {
          genericTypeSpecialiser = new GenericTypeSpecialiser(fieldReference.getContainingType());
        }
        Type contextualisedType = typeHelper.getContextualisedActualMemberType(field.getType(), genericTypeSpecialiser);
        LLVMValueRef result = typeHelper.convertStandardPointerToTemporary(builder, landingPadContainer, global, contextualisedType, fieldReference.getType(), typeParameterAccessor);
        return typeHelper.convertTemporary(builder, landingPadContainer, result, fieldReference.getType(), fieldAccessExpression.getType(), false, typeParameterAccessor);
      }
      if (memberReference instanceof PropertyReference)
      {
        PropertyReference propertyReference = (PropertyReference) memberReference;
        Property property = propertyReference.getReferencedMember();
        if (!property.isStatic())
        {
          throw new IllegalStateException("A FieldAccessExpression for a non-static property should have a base expression");
        }

        LLVMValueRef callee = LLVM.LLVMConstNull(typeHelper.getOpaquePointer());
        LLVMValueRef result = typeHelper.buildPropertyGetterFunctionCall(builder, landingPadContainer, callee, null, propertyReference, typeParameterAccessor);
        return typeHelper.convertTemporary(builder, landingPadContainer, result, propertyReference.getType(), fieldAccessExpression.getType(), false, typeParameterAccessor);
      }
      if (memberReference instanceof MethodReference)
      {
        MethodReference methodReference = (MethodReference) memberReference;
        Method method = methodReference.getReferencedMember();
        if (!method.isStatic())
        {
          throw new IllegalStateException("A FieldAccessExpression for a non-static method should have a base expression");
        }

        LLVMValueRef callee = LLVM.LLVMConstNull(typeHelper.getOpaquePointer());
        LLVMValueRef result = typeHelper.extractMethodFunction(builder, landingPadContainer, callee, null, methodReference, false, typeParameterAccessor);
        FunctionType resultFunctionType = new FunctionType(false, methodReference.getReferencedMember().isImmutable(), methodReference.getReturnType(), methodReference.getParameterTypes(), methodReference.getCheckedThrownTypes(), null);
        return typeHelper.convertTemporary(builder, landingPadContainer, result, resultFunctionType, fieldAccessExpression.getType(), false, typeParameterAccessor);
      }
      throw new IllegalArgumentException("Unknown member type for a FieldAccessExpression: " + memberReference);
    }
    if (expression instanceof FloatingLiteralExpression)
    {
      double value = Double.parseDouble(((FloatingLiteralExpression) expression).getLiteral().toString());
      LLVMValueRef llvmValue = LLVM.LLVMConstReal(typeHelper.findStandardType(Type.findTypeWithNullability(expression.getType(), false)), value);
      return typeHelper.convertStandardToTemporary(builder, landingPadContainer, llvmValue, Type.findTypeWithNullability(expression.getType(), false), expression.getType(), typeParameterAccessor);
    }
    if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression functionExpression = (FunctionCallExpression) expression;
      MethodReference resolvedMethodReference = functionExpression.getResolvedMethodReference();
      Expression resolvedBaseExpression = functionExpression.getResolvedBaseExpression();

      Type[] parameterTypes;
      Type returnType;
      if (resolvedMethodReference != null)
      {
        parameterTypes = resolvedMethodReference.getParameterTypes();
        returnType = resolvedMethodReference.getReturnType();
      }
      else if (resolvedBaseExpression != null)
      {
        FunctionType baseType = (FunctionType) resolvedBaseExpression.getType();
        parameterTypes = baseType.getParameterTypes();
        returnType = baseType.getReturnType();
      }
      else
      {
        throw new IllegalArgumentException("Unresolved function call expression: " + functionExpression);
      }

      LLVMValueRef callee = null;
      Type calleeType = null;
      if (resolvedBaseExpression != null)
      {
        callee = buildExpression(resolvedBaseExpression, builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        calleeType = resolvedBaseExpression.getType();
      }

      // if this is a null traversing function call, apply it properly
      boolean nullTraversal = resolvedBaseExpression != null && resolvedMethodReference != null && functionExpression.getResolvedNullTraversal();
      LLVMValueRef notNullCallee = callee;
      LLVMBasicBlockRef startBlock = null;
      LLVMBasicBlockRef continuationBlock = null;
      if (nullTraversal)
      {
        LLVMValueRef nullCheckResult = buildNullCheck(builder, callee, resolvedBaseExpression.getType());
        continuationBlock = LLVM.LLVMAddBasicBlock(builder, "nullTraversalCallContinuation");
        LLVMBasicBlockRef callBlock = LLVM.LLVMAddBasicBlock(builder, "nullTraversalCall");
        startBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildCondBr(builder, nullCheckResult, callBlock, continuationBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, callBlock);
        calleeType = Type.findTypeWithNullability(resolvedBaseExpression.getType(), false);
        notNullCallee = typeHelper.convertTemporary(builder, landingPadContainer, callee, resolvedBaseExpression.getType(), calleeType, false, typeParameterAccessor);
      }

      Expression[] arguments = functionExpression.getArguments();
      LLVMValueRef[] values = new LLVMValueRef[arguments.length];
      for (int i = 0; i < arguments.length; i++)
      {
        LLVMValueRef arg = buildExpression(arguments[i], builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        values[i] = typeHelper.convertTemporary(builder, landingPadContainer, arg, arguments[i].getType(), parameterTypes[i], false, typeParameterAccessor);
      }

      LLVMValueRef result;
      if (resolvedMethodReference != null)
      {
        Method method = resolvedMethodReference.getReferencedMember();
        LLVMValueRef realCallee;
        Type realCalleeType;
        if (method.isStatic())
        {
          realCallee = LLVM.LLVMConstNull(typeHelper.getOpaquePointer());
          realCalleeType = null;
        }
        else
        {
          realCallee = notNullCallee != null ? notNullCallee : thisValue;
          realCalleeType = calleeType != null ? calleeType : new NamedType(false, false, false, typeDefinition);
        }

        result = typeHelper.buildMethodCall(builder, landingPadContainer, realCallee, realCalleeType, resolvedMethodReference, values, typeParameterAccessor);
      }
      else if (resolvedBaseExpression != null)
      {
        // callee here is actually a tuple of an RTTI pointer, an opaque pointer, and a function type, where the first argument to the function is the opaque pointer
        LLVMValueRef firstArgument = LLVM.LLVMBuildExtractValue(builder, callee, 1, "");
        LLVMValueRef calleeFunction = LLVM.LLVMBuildExtractValue(builder, callee, 2, "");
        LLVMValueRef[] realArguments = new LLVMValueRef[values.length + 1];
        realArguments[0] = firstArgument;
        for (int i = 0; i < values.length; ++i)
        {
          realArguments[1 + i] = typeHelper.convertTemporaryToStandard(builder, values[i], parameterTypes[i]);
        }
        LLVMBasicBlockRef invokeContinueBlock = LLVM.LLVMAddBasicBlock(builder, "functionInvokeContinue");
        result = LLVM.LLVMBuildInvoke(builder, calleeFunction, C.toNativePointerArray(realArguments, false, true), realArguments.length, invokeContinueBlock, landingPadContainer.getLandingPadBlock(), "");
        LLVM.LLVMPositionBuilderAtEnd(builder, invokeContinueBlock);
        if (!(returnType instanceof VoidType))
        {
          result = typeHelper.convertStandardToTemporary(builder, result, returnType);
        }
      }
      else
      {
        throw new IllegalArgumentException("Unresolved function call expression: " + functionExpression);
      }

      if (nullTraversal)
      {
        if (returnType instanceof VoidType)
        {
          LLVM.LLVMBuildBr(builder, continuationBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
          return null;
        }

        result = typeHelper.convertTemporary(builder, landingPadContainer, result, returnType, functionExpression.getType(), false, typeParameterAccessor);
        LLVMBasicBlockRef callBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildBr(builder, continuationBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
        LLVMValueRef phiNode = LLVM.LLVMBuildPhi(builder, typeHelper.findTemporaryType(functionExpression.getType()), "");
        LLVMValueRef nullAlternative = LLVM.LLVMConstNull(typeHelper.findTemporaryType(functionExpression.getType()));
        LLVMValueRef[] phiValues = new LLVMValueRef[] {result, nullAlternative};
        LLVMBasicBlockRef[] phiBlocks = new LLVMBasicBlockRef[] {callBlock, startBlock};
        LLVM.LLVMAddIncoming(phiNode, C.toNativePointerArray(phiValues, false, true), C.toNativePointerArray(phiBlocks, false, true), phiValues.length);
        return phiNode;
      }

      if (returnType instanceof VoidType)
      {
        return result;
      }
      return typeHelper.convertTemporary(builder, landingPadContainer, result, returnType, functionExpression.getType(), false, typeParameterAccessor);
    }
    if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIf = (InlineIfExpression) expression;
      LLVMValueRef conditionValue = buildExpression(inlineIf.getCondition(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      conditionValue = typeHelper.convertTemporaryToStandard(builder, landingPadContainer, conditionValue, inlineIf.getCondition().getType(), new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), typeParameterAccessor);
      LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "afterInlineIf");
      LLVMBasicBlockRef elseBlock = LLVM.LLVMAddBasicBlock(builder, "inlineIfElse");
      LLVMBasicBlockRef thenBlock = LLVM.LLVMAddBasicBlock(builder, "inlineIfThen");

      LLVM.LLVMBuildCondBr(builder, conditionValue, thenBlock, elseBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, thenBlock);
      LLVMValueRef thenValue = buildExpression(inlineIf.getThenExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      LLVMValueRef convertedThenValue = typeHelper.convertTemporary(builder, landingPadContainer, thenValue, inlineIf.getThenExpression().getType(), inlineIf.getType(), false, typeParameterAccessor);
      LLVMBasicBlockRef thenBranchBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, elseBlock);
      LLVMValueRef elseValue = buildExpression(inlineIf.getElseExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      LLVMValueRef convertedElseValue = typeHelper.convertTemporary(builder, landingPadContainer, elseValue, inlineIf.getElseExpression().getType(), inlineIf.getType(), false, typeParameterAccessor);
      LLVMBasicBlockRef elseBranchBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
      LLVMValueRef result = LLVM.LLVMBuildPhi(builder, typeHelper.findTemporaryType(inlineIf.getType()), "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {convertedThenValue, convertedElseValue};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {thenBranchBlock, elseBranchBlock};
      LLVM.LLVMAddIncoming(result, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), 2);
      return result;
    }
    if (expression instanceof InstanceOfExpression)
    {
      InstanceOfExpression instanceOfExpression = (InstanceOfExpression) expression;
      LLVMValueRef expressionResult = buildExpression(instanceOfExpression.getExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      Type expressionType = instanceOfExpression.getExpression().getType();
      Type checkType = instanceOfExpression.getInstanceOfType();
      if (expressionType instanceof NullType)
      {
        // null is never an instance of anything
        return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
      }

      Type notNullType = expressionType;
      LLVMValueRef notNullValue = expressionResult;
      LLVMBasicBlockRef startBlock = null;
      LLVMBasicBlockRef continuationBlock = null;
      if (expressionType.canBeNullable())
      {
        continuationBlock = LLVM.LLVMAddBasicBlock(builder, "instanceofCheckContinuation");
        LLVMBasicBlockRef nullCheckBlock = LLVM.LLVMAddBasicBlock(builder, "instanceofCheck");
        LLVMValueRef nullCheckResult = buildNullCheck(builder, expressionResult, expressionType);
        startBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildCondBr(builder, nullCheckResult, nullCheckBlock, continuationBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, nullCheckBlock);
        notNullType = Type.findTypeWithNullability(expressionType, false);
        notNullValue = typeHelper.convertTemporary(builder, landingPadContainer, expressionResult, expressionType, notNullType, false, typeParameterAccessor);
      }

      LLVMValueRef checkResult = rttiHelper.buildInstanceOfCheck(builder, landingPadContainer, notNullValue, notNullType, checkType, typeParameterAccessor);

      if (expressionType.canBeNullable())
      {
        LLVMBasicBlockRef endCheckBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMBuildBr(builder, continuationBlock);

        LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
        LLVMValueRef phi = LLVM.LLVMBuildPhi(builder, LLVM.LLVMInt1Type(), "");
        LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false), checkResult};
        LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, endCheckBlock};
        LLVM.LLVMAddIncoming(phi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
        return phi;
      }
      return checkResult;
    }
    if (expression instanceof IntegerLiteralExpression)
    {
      BigInteger bigintValue = ((IntegerLiteralExpression) expression).getLiteral().getValue();
      byte[] bytes = bigintValue.toByteArray();
      // convert the big-endian byte[] from the BigInteger into a little-endian long[] for LLVM
      long[] longs = new long[(bytes.length + 7) / 8];
      for (int i = 0; i < bytes.length; ++i)
      {
        int longIndex = (bytes.length - 1 - i) / 8;
        int longBitPos = ((bytes.length - 1 - i) % 8) * 8;
        longs[longIndex] |= (((long) bytes[i]) & 0xff) << longBitPos;
      }
      LLVMValueRef value = LLVM.LLVMConstIntOfArbitraryPrecision(typeHelper.findStandardType(Type.findTypeWithNullability(expression.getType(), false)), longs.length, longs);
      return typeHelper.convertStandardToTemporary(builder, landingPadContainer, value, Type.findTypeWithNullability(expression.getType(), false), expression.getType(), typeParameterAccessor);
    }
    if (expression instanceof LogicalExpression)
    {
      LogicalExpression logicalExpression = (LogicalExpression) expression;
      LLVMValueRef left = buildExpression(logicalExpression.getLeftSubExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      PrimitiveType leftType = (PrimitiveType) logicalExpression.getLeftSubExpression().getType();
      PrimitiveType rightType = (PrimitiveType) logicalExpression.getRightSubExpression().getType();
      // cast if necessary
      PrimitiveType resultType = (PrimitiveType) logicalExpression.getType();
      left = typeHelper.convertTemporary(builder, landingPadContainer, left, leftType, resultType, false, typeParameterAccessor);
      LogicalOperator operator = logicalExpression.getOperator();
      if (operator != LogicalOperator.SHORT_CIRCUIT_AND && operator != LogicalOperator.SHORT_CIRCUIT_OR)
      {
        LLVMValueRef right = buildExpression(logicalExpression.getRightSubExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        right = typeHelper.convertTemporary(builder, landingPadContainer, right, rightType, resultType, false, typeParameterAccessor);
        switch (operator)
        {
        case AND:
          return LLVM.LLVMBuildAnd(builder, left, right, "");
        case OR:
          return LLVM.LLVMBuildOr(builder, left, right, "");
        case XOR:
          return LLVM.LLVMBuildXor(builder, left, right, "");
        case SHORT_CIRCUIT_AND:
        case SHORT_CIRCUIT_OR:
        default:
          throw new IllegalStateException("Unexpected non-short-circuit operator: " + logicalExpression.getOperator());
        }
      }
      LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "shortCircuitContinue");
      LLVMBasicBlockRef rightCheckBlock = LLVM.LLVMAddBasicBlock(builder, "shortCircuitCheck");
      // the only difference between short circuit AND and OR is whether they jump to the check block when the left hand side is true or false
      LLVMBasicBlockRef trueDest = operator == LogicalOperator.SHORT_CIRCUIT_AND ? rightCheckBlock : continuationBlock;
      LLVMBasicBlockRef falseDest = operator == LogicalOperator.SHORT_CIRCUIT_AND ? continuationBlock : rightCheckBlock;
      LLVM.LLVMBuildCondBr(builder, left, trueDest, falseDest);

      LLVM.LLVMPositionBuilderAtEnd(builder, rightCheckBlock);
      LLVMValueRef right = buildExpression(logicalExpression.getRightSubExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      right = typeHelper.convertTemporary(builder, landingPadContainer, right, rightType, resultType, false, typeParameterAccessor);
      LLVMBasicBlockRef endRightCheckBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
      // create a phi node for the result, and return it
      LLVMValueRef phi = LLVM.LLVMBuildPhi(builder, typeHelper.findTemporaryType(resultType), "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {left, right};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {currentBlock, endRightCheckBlock};
      LLVM.LLVMAddIncoming(phi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), 2);
      return phi;
    }
    if (expression instanceof MinusExpression)
    {
      MinusExpression minusExpression = (MinusExpression) expression;
      LLVMValueRef value = buildExpression(minusExpression.getExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      value = typeHelper.convertTemporary(builder, landingPadContainer, value, minusExpression.getExpression().getType(), Type.findTypeWithNullability(minusExpression.getType(), false), false, typeParameterAccessor);
      PrimitiveTypeType primitiveTypeType = ((PrimitiveType) minusExpression.getType()).getPrimitiveTypeType();
      LLVMValueRef result;
      if (primitiveTypeType.isFloating())
      {
        result = LLVM.LLVMBuildFNeg(builder, value, "");
      }
      else
      {
        result = LLVM.LLVMBuildNeg(builder, value, "");
      }
      return typeHelper.convertTemporary(builder, landingPadContainer, result, Type.findTypeWithNullability(minusExpression.getType(), false), minusExpression.getType(), false, typeParameterAccessor);
    }
    if (expression instanceof NullCoalescingExpression)
    {
      NullCoalescingExpression nullCoalescingExpression = (NullCoalescingExpression) expression;

      LLVMValueRef nullableValue = buildExpression(nullCoalescingExpression.getNullableExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      nullableValue = typeHelper.convertTemporary(builder, landingPadContainer, nullableValue, nullCoalescingExpression.getNullableExpression().getType(), Type.findTypeWithNullability(nullCoalescingExpression.getType(), true), false, typeParameterAccessor);
      LLVMValueRef checkResult = buildNullCheck(builder, nullableValue, Type.findTypeWithNullability(nullCoalescingExpression.getType(), true));

      LLVMBasicBlockRef continuationBlock = LLVM.LLVMAddBasicBlock(builder, "nullCoalescingContinuation");
      LLVMBasicBlockRef alternativeBlock = LLVM.LLVMAddBasicBlock(builder, "nullCoalescingAlternative");
      LLVMBasicBlockRef conversionBlock = LLVM.LLVMAddBasicBlock(builder, "nullCoalescingConversion");
      LLVM.LLVMBuildCondBr(builder, checkResult, conversionBlock, alternativeBlock);

      // create a block to convert the nullable value into a non-nullable value
      LLVM.LLVMPositionBuilderAtEnd(builder, conversionBlock);
      LLVMValueRef convertedNullableValue = typeHelper.convertTemporary(builder, landingPadContainer, nullableValue, Type.findTypeWithNullability(nullCoalescingExpression.getType(), true), nullCoalescingExpression.getType(), false, typeParameterAccessor);
      LLVMBasicBlockRef endConversionBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, alternativeBlock);
      LLVMValueRef alternativeValue = buildExpression(nullCoalescingExpression.getAlternativeExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      alternativeValue = typeHelper.convertTemporary(builder, landingPadContainer, alternativeValue, nullCoalescingExpression.getAlternativeExpression().getType(), nullCoalescingExpression.getType(), false, typeParameterAccessor);
      LLVMBasicBlockRef endAlternativeBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
      // create a phi node for the result, and return it
      LLVMTypeRef resultType = typeHelper.findTemporaryType(nullCoalescingExpression.getType());
      LLVMValueRef result = LLVM.LLVMBuildPhi(builder, resultType, "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {convertedNullableValue, alternativeValue};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {endConversionBlock, endAlternativeBlock};
      LLVM.LLVMAddIncoming(result, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), incomingValues.length);
      return result;
    }
    if (expression instanceof NullLiteralExpression)
    {
      Type type = expression.getType();
      return LLVM.LLVMConstNull(typeHelper.findTemporaryType(type));
    }
    if (expression instanceof ObjectCreationExpression)
    {
      ObjectType objectType = new ObjectType(false, false, null);
      LLVMTypeRef nativeType = typeHelper.findStandardType(objectType);
      // allocate memory for the object
      LLVMValueRef pointer = buildHeapAllocation(builder, nativeType);

      // store the VFT
      LLVMValueRef rtti = rttiHelper.getInstanceRTTI(objectType);
      LLVMValueRef rttiPointer = rttiHelper.getRTTIPointer(builder, pointer);
      LLVM.LLVMBuildStore(builder, rtti, rttiPointer);
      LLVMValueRef objectVFT = virtualFunctionHandler.getObjectVFTGlobal();
      LLVMValueRef vftElementPointer = virtualFunctionHandler.getFirstVirtualFunctionTablePointer(builder, pointer);
      LLVM.LLVMBuildStore(builder, objectVFT, vftElementPointer);

      return typeHelper.convertStandardToTemporary(builder, pointer, objectType);
    }
    if (expression instanceof RelationalExpression)
    {
      RelationalExpression relationalExpression = (RelationalExpression) expression;
      LLVMValueRef left = buildExpression(relationalExpression.getLeftSubExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      LLVMValueRef right = buildExpression(relationalExpression.getRightSubExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      PrimitiveType leftType = (PrimitiveType) relationalExpression.getLeftSubExpression().getType();
      PrimitiveType rightType = (PrimitiveType) relationalExpression.getRightSubExpression().getType();
      // cast if necessary
      PrimitiveType resultType = relationalExpression.getComparisonType();
      if (resultType == null)
      {
        PrimitiveTypeType leftTypeType = leftType.getPrimitiveTypeType();
        PrimitiveTypeType rightTypeType = rightType.getPrimitiveTypeType();
        if (!leftTypeType.isFloating() && !rightTypeType.isFloating() &&
            leftTypeType.isSigned() != rightTypeType.isSigned() &&
            !leftType.canBeNullable() && !rightType.canBeNullable())
        {
          // compare the signed and non-signed integers as (bitCount + 1) bit numbers, since they will not fit in bitCount bits
          int bitCount = Math.max(leftTypeType.getBitCount(), rightTypeType.getBitCount()) + 1;
          LLVMTypeRef comparisonType = LLVM.LLVMIntType(bitCount);
          if (leftTypeType.isSigned())
          {
            left = LLVM.LLVMBuildSExt(builder, left, comparisonType, "");
            right = LLVM.LLVMBuildZExt(builder, right, comparisonType, "");
          }
          else
          {
            left = LLVM.LLVMBuildZExt(builder, left, comparisonType, "");
            right = LLVM.LLVMBuildSExt(builder, right, comparisonType, "");
          }
          return LLVM.LLVMBuildICmp(builder, getPredicate(relationalExpression.getOperator(), false, true), left, right, "");
        }
        throw new IllegalArgumentException("Unknown result type, unable to generate comparison expression: " + expression);
      }
      left = typeHelper.convertTemporary(builder, landingPadContainer, left, leftType, resultType, false, typeParameterAccessor);
      right = typeHelper.convertTemporary(builder, landingPadContainer, right, rightType, resultType, false, typeParameterAccessor);
      LLVMValueRef result;
      if (resultType.getPrimitiveTypeType().isFloating())
      {
        result = LLVM.LLVMBuildFCmp(builder, getPredicate(relationalExpression.getOperator(), true, true), left, right, "");
      }
      else
      {
        result = LLVM.LLVMBuildICmp(builder, getPredicate(relationalExpression.getOperator(), false, resultType.getPrimitiveTypeType().isSigned()), left, right, "");
      }
      return typeHelper.convertTemporary(builder, landingPadContainer, result, new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), relationalExpression.getType(), false, typeParameterAccessor);
    }
    if (expression instanceof ShiftExpression)
    {
      ShiftExpression shiftExpression = (ShiftExpression) expression;
      LLVMValueRef leftValue = buildExpression(shiftExpression.getLeftExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      LLVMValueRef rightValue = buildExpression(shiftExpression.getRightExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      LLVMValueRef convertedLeft = typeHelper.convertTemporary(builder, landingPadContainer, leftValue, shiftExpression.getLeftExpression().getType(), shiftExpression.getType(), false, typeParameterAccessor);
      LLVMValueRef convertedRight = typeHelper.convertTemporary(builder, landingPadContainer, rightValue, shiftExpression.getRightExpression().getType(), shiftExpression.getType(), false, typeParameterAccessor);
      switch (shiftExpression.getOperator())
      {
      case RIGHT_SHIFT:
        if (((PrimitiveType) shiftExpression.getType()).getPrimitiveTypeType().isSigned())
        {
          return LLVM.LLVMBuildAShr(builder, convertedLeft, convertedRight, "");
        }
        return LLVM.LLVMBuildLShr(builder, convertedLeft, convertedRight, "");
      case LEFT_SHIFT:
        return LLVM.LLVMBuildShl(builder, convertedLeft, convertedRight, "");
      }
      throw new IllegalArgumentException("Unknown shift operator: " + shiftExpression.getOperator());
    }
    if (expression instanceof StringLiteralExpression)
    {
      StringLiteralExpression stringLiteralExpression = (StringLiteralExpression) expression;
      String value = stringLiteralExpression.getLiteral().getLiteralValue();
      LLVMValueRef llvmString = buildStringCreation(builder, landingPadContainer, value);
      return typeHelper.convertTemporary(builder, landingPadContainer, llvmString, SpecialTypeHandler.STRING_TYPE, expression.getType(), false, typeParameterAccessor);
    }
    if (expression instanceof ThisExpression)
    {
      // the 'this' value always has a temporary representation
      return thisValue;
    }
    if (expression instanceof TupleExpression)
    {
      TupleExpression tupleExpression = (TupleExpression) expression;
      Type[] tupleTypes = ((TupleType) tupleExpression.getType()).getSubTypes();
      Expression[] subExpressions = tupleExpression.getSubExpressions();
      Type nonNullableTupleType = Type.findTypeWithNullability(tupleExpression.getType(), false);
      LLVMValueRef currentValue = LLVM.LLVMGetUndef(typeHelper.findTemporaryType(nonNullableTupleType));
      for (int i = 0; i < subExpressions.length; i++)
      {
        LLVMValueRef value = buildExpression(subExpressions[i], builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
        Type type = tupleTypes[i];
        value = typeHelper.convertTemporary(builder, landingPadContainer, value, subExpressions[i].getType(), type, false, typeParameterAccessor);
        currentValue = LLVM.LLVMBuildInsertValue(builder, currentValue, value, i, "");
      }
      return typeHelper.convertTemporary(builder, landingPadContainer, currentValue, nonNullableTupleType, tupleExpression.getType(), false, typeParameterAccessor);
    }
    if (expression instanceof TupleIndexExpression)
    {
      TupleIndexExpression tupleIndexExpression = (TupleIndexExpression) expression;
      TupleType tupleType = (TupleType) tupleIndexExpression.getExpression().getType();
      LLVMValueRef result = buildExpression(tupleIndexExpression.getExpression(), builder, thisValue, variables, landingPadContainer, typeParameterAccessor);
      // convert the 1-based indexing to 0-based before extracting the value
      int index = tupleIndexExpression.getIndexLiteral().getValue().intValue() - 1;
      LLVMValueRef value = LLVM.LLVMBuildExtractValue(builder, result, index, "");
      return typeHelper.convertTemporary(builder, landingPadContainer, value, tupleType.getSubTypes()[index], tupleIndexExpression.getType(), false, typeParameterAccessor);
    }
    if (expression instanceof VariableExpression)
    {
      VariableExpression variableExpression = (VariableExpression) expression;
      MemberReference<?> memberReference = variableExpression.getResolvedMemberReference();
      Variable variable = variableExpression.getResolvedVariable();
      if (variable != null)
      {
        if (variable instanceof MemberVariable || variable instanceof GlobalVariable)
        {
          NamedType memberContainingType;
          Type memberType;
          Type memberReferenceType;
          if (memberReference instanceof FieldReference)
          {
            FieldReference fieldReference = (FieldReference) memberReference;
            memberContainingType = fieldReference.getContainingType();
            memberType = fieldReference.getReferencedMember().getType();
            memberReferenceType = fieldReference.getType();
          }
          else // if (memberReference instanceof PropertyReference)
          {
            PropertyReference propertyReference = (PropertyReference) memberReference;
            memberContainingType = propertyReference.getContainingType();
            memberType = propertyReference.getReferencedMember().getType();
            memberReferenceType = propertyReference.getType();
          }
          LLVMValueRef fieldPointer;
          if (variable instanceof MemberVariable)
          {
            fieldPointer = typeHelper.getMemberPointer(builder, thisValue, (MemberVariable) variable);
          }
          else // if (variable instanceof GlobalVariable)
          {
            fieldPointer = getGlobal((GlobalVariable) variable);
          }

          // convert from the underlying field's type to the referenced type that we are expecting in this context
          GenericTypeSpecialiser genericTypeSpecialiser = GenericTypeSpecialiser.IDENTITY_SPECIALISER;
          if (memberContainingType != null)
          {
            genericTypeSpecialiser = new GenericTypeSpecialiser(memberContainingType);
          }
          Type contextualisedType = typeHelper.getContextualisedActualMemberType(memberType, genericTypeSpecialiser);
          LLVMValueRef result = typeHelper.convertStandardPointerToTemporary(builder, landingPadContainer, fieldPointer, contextualisedType, memberReferenceType, typeParameterAccessor);
          return typeHelper.convertTemporary(builder, landingPadContainer, result, memberReferenceType, variableExpression.getType(), false, typeParameterAccessor);
        }
        if (variable instanceof PropertyPseudoVariable)
        {
          PropertyReference propertyReference = (PropertyReference) memberReference;
          Property property = ((PropertyPseudoVariable) variable).getProperty();
          LLVMValueRef callee;
          Type calleeType;
          if (property.isStatic())
          {
            callee = LLVM.LLVMConstNull(typeHelper.getOpaquePointer());
            calleeType = null;
          }
          else
          {
            callee = thisValue;
            calleeType = new NamedType(false, false, false, typeDefinition);
          }
          LLVMValueRef result = typeHelper.buildPropertyGetterFunctionCall(builder, landingPadContainer, callee, calleeType, propertyReference, typeParameterAccessor);
          return typeHelper.convertTemporary(builder, landingPadContainer, result, propertyReference.getType(), variableExpression.getType(), false, typeParameterAccessor);
        }
        LLVMValueRef value = variables.get(variable);
        if (value == null)
        {
          throw new IllegalStateException("Missing LLVMValueRef in variable Map: " + variableExpression.getName());
        }
        return LLVM.LLVMBuildLoad(builder, value, "");
      }
      if (memberReference instanceof MethodReference)
      {
        MethodReference methodReference = (MethodReference) memberReference;
        Method method = methodReference.getReferencedMember();

        LLVMValueRef callee;
        Type calleeType;
        if (method.isStatic())
        {
          callee = LLVM.LLVMConstNull(typeHelper.getOpaquePointer());
          calleeType = null;
        }
        else
        {
          callee = thisValue;
          calleeType = new NamedType(false, false, false, typeDefinition);
        }

        boolean isNonVirtual = variableExpression instanceof SuperVariableExpression;

        LLVMValueRef result = typeHelper.extractMethodFunction(builder, landingPadContainer, callee, calleeType, methodReference, isNonVirtual, typeParameterAccessor);
        FunctionType resultFunctionType = new FunctionType(false, methodReference.getReferencedMember().isImmutable(), methodReference.getReturnType(), methodReference.getParameterTypes(), methodReference.getCheckedThrownTypes(), null);
        return typeHelper.convertTemporary(builder, landingPadContainer, result, resultFunctionType, variableExpression.getType(), false, typeParameterAccessor);
      }
    }
    throw new IllegalArgumentException("Unknown Expression type: " + expression);
  }
}
