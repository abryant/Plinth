package eu.bryants.anthony.toylanguage.compiler.passes.llvm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import nativelib.c.C;
import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMBasicBlockRef;
import nativelib.llvm.LLVM.LLVMBuilderRef;
import nativelib.llvm.LLVM.LLVMModuleRef;
import nativelib.llvm.LLVM.LLVMTypeRef;
import nativelib.llvm.LLVM.LLVMValueRef;
import eu.bryants.anthony.toylanguage.ast.ClassDefinition;
import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.TypeDefinition;
import eu.bryants.anthony.toylanguage.ast.expression.ArithmeticExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ArrayAccessExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ArrayCreationExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BitwiseNotExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BooleanLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BooleanNotExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BracketedExpression;
import eu.bryants.anthony.toylanguage.ast.expression.CastExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ClassCreationExpression;
import eu.bryants.anthony.toylanguage.ast.expression.EqualityExpression;
import eu.bryants.anthony.toylanguage.ast.expression.EqualityExpression.EqualityOperator;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.ast.expression.InlineIfExpression;
import eu.bryants.anthony.toylanguage.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.LogicalExpression;
import eu.bryants.anthony.toylanguage.ast.expression.LogicalExpression.LogicalOperator;
import eu.bryants.anthony.toylanguage.ast.expression.MinusExpression;
import eu.bryants.anthony.toylanguage.ast.expression.NullCoalescingExpression;
import eu.bryants.anthony.toylanguage.ast.expression.NullLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.RelationalExpression;
import eu.bryants.anthony.toylanguage.ast.expression.RelationalExpression.RelationalOperator;
import eu.bryants.anthony.toylanguage.ast.expression.ShiftExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ThisExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleIndexExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.member.ArrayLengthMember;
import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Initialiser;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.ast.member.Method;
import eu.bryants.anthony.toylanguage.ast.metadata.FieldInitialiser;
import eu.bryants.anthony.toylanguage.ast.metadata.GlobalVariable;
import eu.bryants.anthony.toylanguage.ast.metadata.MemberVariable;
import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
import eu.bryants.anthony.toylanguage.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.misc.BlankAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.FieldAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.misc.VariableAssignee;
import eu.bryants.anthony.toylanguage.ast.statement.AssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.statement.BreakStatement;
import eu.bryants.anthony.toylanguage.ast.statement.BreakableStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ContinueStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ExpressionStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ForStatement;
import eu.bryants.anthony.toylanguage.ast.statement.IfStatement;
import eu.bryants.anthony.toylanguage.ast.statement.PrefixIncDecStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ReturnStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ShorthandAssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.Statement;
import eu.bryants.anthony.toylanguage.ast.statement.WhileStatement;
import eu.bryants.anthony.toylanguage.ast.type.ArrayType;
import eu.bryants.anthony.toylanguage.ast.type.FunctionType;
import eu.bryants.anthony.toylanguage.ast.type.NamedType;
import eu.bryants.anthony.toylanguage.ast.type.NullType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.toylanguage.ast.type.TupleType;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.ast.type.VoidType;
import eu.bryants.anthony.toylanguage.compiler.passes.Resolver;
import eu.bryants.anthony.toylanguage.compiler.passes.TypeChecker;

/*
 * Created on 5 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class CodeGenerator
{
  private TypeDefinition typeDefinition;

  private LLVMModuleRef module;
  private LLVMBuilderRef builder;

  private LLVMValueRef callocFunction;

  private Map<GlobalVariable, LLVMValueRef> globalVariables = new HashMap<GlobalVariable, LLVMValueRef>();

  private TypeHelper typeHelper;

  public CodeGenerator(TypeDefinition typeDefinition)
  {
    this.typeDefinition = typeDefinition;
  }

  public void generateModule()
  {
    if (module != null || builder != null)
    {
      throw new IllegalStateException("Cannot generate the module again, it has already been generated by this CodeGenerator");
    }

    module = LLVM.LLVMModuleCreateWithName(typeDefinition.getQualifiedName().toString());
    builder = LLVM.LLVMCreateBuilder();

    typeHelper = new TypeHelper(builder);

    // add all of the global (static) variables
    addGlobalVariables();
    // add all of the LLVM functions, including initialisers, constructors, and methods
    addFunctions();

    addInitialiserBody(true);  // add the static initialisers
    setGlobalConstructor(getInitialiserFunction(true));
    addInitialiserBody(false); // add the non-static initialisers
    addConstructorBodies();
    addMethodBodies();

    MetadataGenerator.generateMetadata(typeDefinition, module);
  }

  public LLVMModuleRef getModule()
  {
    if (module == null)
    {
      throw new IllegalStateException("The module has not yet been created; please call generateModule() before getModule()");
    }
    return module;
  }

  public void writeModule(String outputPath)
  {
    if (module == null)
    {
      throw new IllegalStateException("The module has not yet been created; please call generateModule() before writeModule()");
    }
    LLVM.LLVMWriteBitcodeToFile(module, outputPath);
  }

  private void addGlobalVariables()
  {
    for (Field field : typeDefinition.getFields())
    {
      if (field.isStatic())
      {
        GlobalVariable globalVariable = field.getGlobalVariable();
        LLVMValueRef value = LLVM.LLVMAddGlobal(module, typeHelper.findStandardType(field.getType()), globalVariable.getMangledName());
        LLVM.LLVMSetInitializer(value, LLVM.LLVMConstNull(typeHelper.findStandardType(field.getType())));
        globalVariables.put(globalVariable, value);
      }
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
    LLVM.LLVMSetInitializer(newValue, LLVM.LLVMConstNull(typeHelper.findStandardType(type)));
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
    getInitialiserFunction(true);
    getInitialiserFunction(false);

    // create the constructor and method functions
    for (Constructor constructor : typeDefinition.getConstructors())
    {
      getConstructorFunction(constructor);
    }
    for (Method method : typeDefinition.getAllMethods())
    {
      getMethodFunction(method);
    }
  }

  /**
   * Gets the (static or non-static) initialiser function for the TypeDefinition we are building.
   * @param isStatic - true for the static initialiser, false for the non-static initialiser
   * @return the function declaration for the specified Initialiser
   */
  private LLVMValueRef getInitialiserFunction(boolean isStatic)
  {
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
      types[0] = typeHelper.findTemporaryType(new NamedType(false, typeDefinition));
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
  private LLVMValueRef getConstructorFunction(Constructor constructor)
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
    int startIndex = 0;
    if (typeDefinition instanceof ClassDefinition)
    {
      // class constructors need an extra 'uninitialised this' parameter at the start, which is the newly allocated data to initialise
      // the 'this' parameter always has a temporary type representation
      types = new LLVMTypeRef[1 + parameters.length];
      types[0] = typeHelper.findTemporaryType(new NamedType(false, typeDefinition));
      startIndex = 1;
    }
    else if (typeDefinition instanceof CompoundDefinition)
    {
      types = new LLVMTypeRef[parameters.length];
      startIndex = 0;
    }
    for (int i = 0; i < parameters.length; i++)
    {
      types[i + startIndex] = typeHelper.findStandardType(parameters[i].getType());
    }
    LLVMTypeRef resultType = typeHelper.findStandardType(new NamedType(false, typeDefinition));

    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);
    LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);

    if (typeDefinition instanceof ClassDefinition)
    {
      LLVM.LLVMSetValueName(LLVM.LLVMGetParam(llvmFunc, 0), "this");
    }
    for (int i = 0; i < parameters.length; i++)
    {
      LLVMValueRef parameter = LLVM.LLVMGetParam(llvmFunc, i + startIndex);
      LLVM.LLVMSetValueName(parameter, parameters[i].getName());
    }
    return llvmFunc;
  }

  /**
   * Gets the function definition for the specified Method. If necessary, it is added first.
   * @param method - the Method to find the declaration of (or to declare)
   * @return the function declaration for the specified Method
   */
  private LLVMValueRef getMethodFunction(Method method)
  {
    String mangledName = method.getMangledName();
    LLVMValueRef existingFunc = LLVM.LLVMGetNamedFunction(module, mangledName);
    if (existingFunc != null)
    {
      return existingFunc;
    }
    TypeDefinition typeDefinition = method.getContainingTypeDefinition();

    Parameter[] parameters = method.getParameters();
    LLVMTypeRef[] types;
    int startIndex = 0;
    if (method.isStatic())
    {
      types = new LLVMTypeRef[parameters.length];
      startIndex = 0;
    }
    else
    {
      types = new LLVMTypeRef[1 + parameters.length];
      // add the 'this' type to the function - 'this' always has a temporary type representation
      if (typeDefinition instanceof ClassDefinition)
      {
        types[0] = typeHelper.findTemporaryType(new NamedType(false, typeDefinition));
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        types[0] = typeHelper.findTemporaryType(new NamedType(false, typeDefinition));
      }
      startIndex = 1;
    }
    for (int i = 0; i < parameters.length; ++i)
    {
      types[i + startIndex] = typeHelper.findStandardType(parameters[i].getType());
    }
    LLVMTypeRef resultType = typeHelper.findStandardType(method.getReturnType());

    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, C.toNativePointerArray(types, false, true), types.length, false);
    LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);

    int paramCount = LLVM.LLVMCountParams(llvmFunc);
    if (paramCount != types.length)
    {
      throw new IllegalStateException("LLVM returned wrong number of parameters");
    }
    if (!method.isStatic())
    {
      LLVM.LLVMSetValueName(LLVM.LLVMGetParam(llvmFunc, 0), "this");
    }
    for (int i = 0; i < parameters.length; ++i)
    {
      LLVMValueRef parameter = LLVM.LLVMGetParam(llvmFunc, i + startIndex);
      LLVM.LLVMSetValueName(parameter, parameters[i].getName());
    }

    // add the native function if the programmer specified one
    if (method.getNativeName() != null)
    {
      addNativeFunction(method.getNativeName(), !(method.getReturnType() instanceof VoidType), functionType, llvmFunc);
    }

    return llvmFunc;
  }

  /**
   * Adds a native function which calls the specified non-native function.
   * This consists simply of a new function with the specified native name, which calls the non-native function and returns its result.
   * @param nativeName - the native name to export
   * @param hasReturnValue - true if this method returns a value, false otherwise
   * @param functionType - the type of the non-native function
   * @param nonNativeFunction - the non-native function to call
   */
  private void addNativeFunction(String nativeName, boolean hasReturnValue, LLVMTypeRef functionType, LLVMValueRef nonNativeFunction)
  {
    LLVMValueRef nativeFunction = LLVM.LLVMAddFunction(module, nativeName, functionType);
    LLVM.LLVMSetFunctionCallConv(nativeFunction, LLVM.LLVMCallConv.LLVMCCallConv);
    int paramCount = LLVM.LLVMCountParams(nativeFunction);
    LLVMValueRef[] arguments = new LLVMValueRef[paramCount];
    for (int i = 0; i < paramCount; ++i)
    {
      arguments[i] = LLVM.LLVMGetParam(nativeFunction, i);
    }
    LLVMBasicBlockRef block = LLVM.LLVMAppendBasicBlock(nativeFunction, "entry");
    LLVM.LLVMPositionBuilderAtEnd(builder, block);
    LLVMValueRef result = LLVM.LLVMBuildCall(builder, nonNativeFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");
    if (hasReturnValue)
    {
      LLVM.LLVMBuildRet(builder, result);
    }
    else
    {
      LLVM.LLVMBuildRetVoid(builder);
    }
  }

  private void addInitialiserBody(boolean isStatic)
  {
    LLVMValueRef initialiserFunc = getInitialiserFunction(isStatic);
    LLVMValueRef thisValue = isStatic ? null : LLVM.LLVMGetParam(initialiserFunc, 0);
    LLVMBasicBlockRef entryBlock = LLVM.LLVMAppendBasicBlock(initialiserFunc, "entry");
    LLVM.LLVMPositionBuilderAtEnd(builder, entryBlock);
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
        LLVMValueRef result = buildExpression(field.getInitialiserExpression(), initialiserFunc, thisValue, new HashMap<Variable, LLVM.LLVMValueRef>());
        LLVMValueRef assigneePointer = null;
        if (field.isStatic())
        {
          assigneePointer = getGlobal(field.getGlobalVariable());
        }
        else
        {
          LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), field.getMemberIndex(), false)};
          assigneePointer = LLVM.LLVMBuildGEP(builder, thisValue, C.toNativePointerArray(indices, false, true), indices.length, "");
        }
        LLVMValueRef convertedValue = typeHelper.convertTemporaryToStandard(result, field.getInitialiserExpression().getType(), field.getType(), initialiserFunc);
        LLVM.LLVMBuildStore(builder, convertedValue, assigneePointer);
      }
      else
      {
        // build allocas for all of the variables, at the start of the entry block
        Set<Variable> allVariables = Resolver.getAllNestedVariables(initialiser.getBlock());
        Map<Variable, LLVMValueRef> variables = new HashMap<Variable, LLVM.LLVMValueRef>();
        LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMPositionBuilderAtStart(builder, entryBlock);
        for (Variable v : allVariables)
        {
          LLVMValueRef allocaInst = LLVM.LLVMBuildAlloca(builder, typeHelper.findTemporaryType(v.getType()), v.getName());
          variables.put(v, allocaInst);
        }
        LLVM.LLVMPositionBuilderAtEnd(builder, currentBlock);

        buildStatement(initialiser.getBlock(), VoidType.VOID_TYPE, initialiserFunc, thisValue, variables, new HashMap<BreakableStatement, LLVM.LLVMBasicBlockRef>(), new HashMap<BreakableStatement, LLVM.LLVMBasicBlockRef>(), new Runnable()
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
  }

  private void setGlobalConstructor(LLVMValueRef initialiserFunc)
  {
    // build up the type of the global variable
    LLVMTypeRef[] paramTypes = new LLVMTypeRef[0];
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(LLVM.LLVMVoidType(), C.toNativePointerArray(paramTypes, false, true), paramTypes.length, false);
    LLVMTypeRef functionPointerType = LLVM.LLVMPointerType(functionType, 0);
    LLVMTypeRef[] structSubTypes = new LLVMTypeRef[] {LLVM.LLVMInt32Type(), functionPointerType};
    LLVMTypeRef structType = LLVM.LLVMStructType(C.toNativePointerArray(structSubTypes, false, true), structSubTypes.length, false);
    LLVMTypeRef arrayType = LLVM.LLVMArrayType(structType, 1);

    // build the constant expression for global variable's initialiser
    LLVMValueRef[] constantValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMInt32Type(), 0, false), initialiserFunc};
    LLVMValueRef element = LLVM.LLVMConstStruct(C.toNativePointerArray(constantValues, false, true), constantValues.length, false);
    LLVMValueRef[] arrayElements = new LLVMValueRef[] {element};
    LLVMValueRef array = LLVM.LLVMConstArray(structType, C.toNativePointerArray(arrayElements, false, true), arrayElements.length);

    // create the 'llvm.global_ctors' global variable, which lists which functions are run before main()
    LLVMValueRef global = LLVM.LLVMAddGlobal(module, arrayType, "llvm.global_ctors");
    LLVM.LLVMSetLinkage(global, LLVM.LLVMLinkage.LLVMAppendingLinkage);
    LLVM.LLVMSetInitializer(global, array);
  }

  private void addConstructorBodies()
  {
    for (Constructor constructor : typeDefinition.getConstructors())
    {
      final LLVMValueRef llvmFunction = getConstructorFunction(constructor);

      LLVMBasicBlockRef block = LLVM.LLVMAppendBasicBlock(llvmFunction, "entry");
      LLVM.LLVMPositionBuilderAtEnd(builder, block);

      // create LLVMValueRefs for all of the variables, including paramters
      Set<Variable> allVariables = Resolver.getAllNestedVariables(constructor.getBlock());
      Map<Variable, LLVMValueRef> variables = new HashMap<Variable, LLVM.LLVMValueRef>();
      for (Variable v : allVariables)
      {
        LLVMValueRef allocaInst = LLVM.LLVMBuildAlloca(builder, typeHelper.findTemporaryType(v.getType()), v.getName());
        variables.put(v, allocaInst);
      }

      final LLVMValueRef thisValue;
      int startIndex = 0;
      if (typeDefinition instanceof ClassDefinition)
      {
        // for a class, the first constructor parameter is always the newly allocated this pointer
        thisValue = LLVM.LLVMGetParam(llvmFunction, 0);
        startIndex = 1;
      }
      else if (typeDefinition instanceof CompoundDefinition)
      {
        // since the NamedType is not nullable, this makes thisValue equivalent to a temporary type representation of this NamedType
        thisValue = LLVM.LLVMBuildAlloca(builder, typeHelper.findStandardType(new NamedType(false, typeDefinition)), "this");
        startIndex = 0;
      }
      else
      {
        throw new IllegalArgumentException("Unknown type of TypeDefinition: " + typeDefinition);
      }

      // store the parameter values to the LLVMValueRefs
      for (Parameter p : constructor.getParameters())
      {
        LLVMValueRef llvmParameter = LLVM.LLVMGetParam(llvmFunction, p.getIndex() + startIndex);
        LLVMValueRef convertedParameter = typeHelper.convertStandardToTemporary(llvmParameter, p.getType(), llvmFunction);
        LLVM.LLVMBuildStore(builder, convertedParameter, variables.get(p.getVariable()));
      }

      // call the non-static initialiser function, which runs all non-static initialisers and sets the initial values for all of the fields
      LLVMValueRef initialiserFunction = getInitialiserFunction(false);
      LLVMValueRef[] initialiserArgs = new LLVMValueRef[] {thisValue};
      LLVM.LLVMBuildCall(builder, initialiserFunction, C.toNativePointerArray(initialiserArgs, false, true), initialiserArgs.length, "");

      buildStatement(constructor.getBlock(), VoidType.VOID_TYPE, llvmFunction, thisValue, variables, new HashMap<BreakableStatement, LLVM.LLVMBasicBlockRef>(), new HashMap<BreakableStatement, LLVM.LLVMBasicBlockRef>(), new Runnable()
      {
        @Override
        public void run()
        {
          // this will be called whenever a return void is found
          // so return the created object
          if (typeDefinition instanceof ClassDefinition)
          {
            LLVM.LLVMBuildRet(builder, thisValue);
          }
          else if (typeDefinition instanceof CompoundDefinition)
          {
            LLVMValueRef result = typeHelper.convertTemporaryToStandard(thisValue, new NamedType(false, typeDefinition), llvmFunction);
            LLVM.LLVMBuildRet(builder, result);
          }
        }
      });
      if (!constructor.getBlock().stopsExecution())
      {
        if (typeDefinition instanceof ClassDefinition)
        {
          LLVM.LLVMBuildRet(builder, thisValue);
        }
        else if (typeDefinition instanceof CompoundDefinition)
        {
          LLVMValueRef result = typeHelper.convertTemporaryToStandard(thisValue, new NamedType(false, typeDefinition), llvmFunction);
          LLVM.LLVMBuildRet(builder, result);
        }
      }
    }
  }

  private void addMethodBodies()
  {
    for (Method method : typeDefinition.getAllMethods())
    {
      LLVMValueRef llvmFunction = getMethodFunction(method);

      LLVMBasicBlockRef block = LLVM.LLVMAppendBasicBlock(llvmFunction, "entry");
      LLVM.LLVMPositionBuilderAtEnd(builder, block);

      // create LLVMValueRefs for all of the variables, including parameters
      Map<Variable, LLVMValueRef> variables = new HashMap<Variable, LLVM.LLVMValueRef>();
      for (Variable v : Resolver.getAllNestedVariables(method.getBlock()))
      {
        LLVMValueRef allocaInst = LLVM.LLVMBuildAlloca(builder, typeHelper.findTemporaryType(v.getType()), v.getName());
        variables.put(v, allocaInst);
      }

      // store the parameter values to the LLVMValueRefs
      for (Parameter p : method.getParameters())
      {
        LLVMValueRef llvmParameter = LLVM.LLVMGetParam(llvmFunction, p.getIndex() + (method.isStatic() ? 0 : 1));
        LLVMValueRef convertedParameter = typeHelper.convertStandardToTemporary(llvmParameter, p.getType(), llvmFunction);
        LLVM.LLVMBuildStore(builder, convertedParameter, variables.get(p.getVariable()));
      }

      LLVMValueRef thisValue = method.isStatic() ? null : LLVM.LLVMGetParam(llvmFunction, 0);
      buildStatement(method.getBlock(), method.getReturnType(), llvmFunction, thisValue, variables, new HashMap<BreakableStatement, LLVM.LLVMBasicBlockRef>(), new HashMap<BreakableStatement, LLVM.LLVMBasicBlockRef>(), new Runnable()
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
    }
  }

  private void buildStatement(Statement statement, Type returnType, LLVMValueRef llvmFunction, LLVMValueRef thisValue, Map<Variable, LLVMValueRef> variables,
                              Map<BreakableStatement, LLVMBasicBlockRef> breakBlocks, Map<BreakableStatement, LLVMBasicBlockRef> continueBlocks, Runnable returnVoidCallback)
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assignStatement = (AssignStatement) statement;
      Assignee[] assignees = assignStatement.getAssignees();
      LLVMValueRef[] llvmAssigneePointers = new LLVMValueRef[assignees.length];
      boolean[] standardTypeRepresentations = new boolean[assignees.length];
      for (int i = 0; i < assignees.length; i++)
      {
        if (assignees[i] instanceof VariableAssignee)
        {
          Variable resolvedVariable = ((VariableAssignee) assignees[i]).getResolvedVariable();
          if (resolvedVariable instanceof MemberVariable)
          {
            Field field = ((MemberVariable) resolvedVariable).getField();
            LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                         LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), field.getMemberIndex(), false)};
            llvmAssigneePointers[i] = LLVM.LLVMBuildGEP(builder, thisValue, C.toNativePointerArray(indices, false, true), indices.length, "");
            standardTypeRepresentations[i] = true;
          }
          else if (resolvedVariable instanceof GlobalVariable)
          {
            llvmAssigneePointers[i] = getGlobal((GlobalVariable) resolvedVariable);
            standardTypeRepresentations[i] = true;
          }
          else
          {
            llvmAssigneePointers[i] = variables.get(resolvedVariable);
            standardTypeRepresentations[i] = false;
          }
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          LLVMValueRef array = buildExpression(arrayElementAssignee.getArrayExpression(), llvmFunction, thisValue, variables);
          LLVMValueRef dimension = buildExpression(arrayElementAssignee.getDimensionExpression(), llvmFunction, thisValue, variables);
          LLVMValueRef convertedDimension = typeHelper.convertTemporaryToStandard(dimension, arrayElementAssignee.getDimensionExpression().getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, llvmFunction);
          LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false),
                                                       convertedDimension};
          llvmAssigneePointers[i] = LLVM.LLVMBuildGEP(builder, array, C.toNativePointerArray(indices, false, true), indices.length, "");
          standardTypeRepresentations[i] = true;
        }
        else if (assignees[i] instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];
          FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
          if (fieldAccessExpression.getResolvedMember() instanceof Field)
          {
            Field field = (Field) fieldAccessExpression.getResolvedMember();
            if (field.isStatic())
            {
              llvmAssigneePointers[i] = getGlobal(field.getGlobalVariable());
              standardTypeRepresentations[i] = true;
            }
            else
            {
              LLVMValueRef expressionValue = buildExpression(fieldAccessExpression.getBaseExpression(), llvmFunction, thisValue, variables);
              LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                           LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), field.getMemberIndex(), false)};
              llvmAssigneePointers[i] = LLVM.LLVMBuildGEP(builder, expressionValue, C.toNativePointerArray(indices, false, true), indices.length, "");
              standardTypeRepresentations[i] = true;
            }
          }
          else
          {
            throw new IllegalArgumentException("Unknown member assigned to in a FieldAssignee: " + fieldAccessExpression.getResolvedMember());
          }
        }
        else if (assignees[i] instanceof BlankAssignee)
        {
          // this assignee doesn't actually get assigned to
          llvmAssigneePointers[i] = null;
          standardTypeRepresentations[i] = false;
        }
        else
        {
          throw new IllegalStateException("Unknown Assignee type: " + assignees[i]);
        }
      }

      if (assignStatement.getExpression() != null)
      {
        LLVMValueRef value = buildExpression(assignStatement.getExpression(), llvmFunction, thisValue, variables);
        value = typeHelper.convertTemporary(value, assignStatement.getExpression().getType(), assignStatement.getResolvedType());
        if (llvmAssigneePointers.length == 1)
        {
          if (llvmAssigneePointers[0] != null)
          {
            LLVMValueRef convertedValue;
            if (standardTypeRepresentations[0])
            {
              convertedValue = typeHelper.convertTemporaryToStandard(value, assignStatement.getExpression().getType(), assignees[0].getResolvedType(), llvmFunction);
            }
            else
            {
              convertedValue = typeHelper.convertTemporary(value, assignStatement.getExpression().getType(), assignees[0].getResolvedType());
            }
            LLVM.LLVMBuildStore(builder, convertedValue, llvmAssigneePointers[0]);
          }
        }
        else
        {
          if (assignStatement.getResolvedType().isNullable())
          {
            throw new IllegalStateException("An assign statement's type cannot be nullable if it is about to be split into multiple assignees");
          }
          Type[] expressionSubTypes = ((TupleType) assignStatement.getExpression().getType()).getSubTypes();
          for (int i = 0; i < llvmAssigneePointers.length; i++)
          {
            if (llvmAssigneePointers[i] != null)
            {
              LLVMValueRef extracted = LLVM.LLVMBuildExtractValue(builder, value, i, "");
              LLVMValueRef convertedValue;
              if (standardTypeRepresentations[i])
              {
                convertedValue = typeHelper.convertTemporaryToStandard(extracted, expressionSubTypes[i], assignees[i].getResolvedType(), llvmFunction);
              }
              else
              {
                convertedValue = typeHelper.convertTemporary(extracted, expressionSubTypes[i], assignees[i].getResolvedType());
              }
              LLVM.LLVMBuildStore(builder, convertedValue, llvmAssigneePointers[i]);
            }
          }
        }
      }
    }
    else if (statement instanceof Block)
    {
      for (Statement s : ((Block) statement).getStatements())
      {
        buildStatement(s, returnType, llvmFunction, thisValue, variables, breakBlocks, continueBlocks, returnVoidCallback);
      }
    }
    else if (statement instanceof BreakStatement)
    {
      LLVMBasicBlockRef block = breakBlocks.get(((BreakStatement) statement).getResolvedBreakable());
      if (block == null)
      {
        throw new IllegalStateException("Break statement leads to a null block during code generation: " + statement);
      }
      LLVM.LLVMBuildBr(builder, block);
    }
    else if (statement instanceof ContinueStatement)
    {
      LLVMBasicBlockRef block = continueBlocks.get(((ContinueStatement) statement).getResolvedBreakable());
      if (block == null)
      {
        throw new IllegalStateException("Continue statement leads to a null block during code generation: " + statement);
      }
      LLVM.LLVMBuildBr(builder, block);
    }
    else if (statement instanceof ExpressionStatement)
    {
      buildExpression(((ExpressionStatement) statement).getExpression(), llvmFunction, thisValue, variables);
    }
    else if (statement instanceof ForStatement)
    {
      ForStatement forStatement = (ForStatement) statement;
      Statement init = forStatement.getInitStatement();
      if (init != null)
      {
        buildStatement(init, returnType, llvmFunction, thisValue, variables, breakBlocks, continueBlocks, returnVoidCallback);
      }
      Expression conditional = forStatement.getConditional();
      Statement update = forStatement.getUpdateStatement();

      LLVMBasicBlockRef loopCheck = conditional == null ? null : LLVM.LLVMAppendBasicBlock(llvmFunction, "forLoopCheck");
      LLVMBasicBlockRef loopBody = LLVM.LLVMAppendBasicBlock(llvmFunction, "forLoopBody");
      LLVMBasicBlockRef loopUpdate = update == null ? null : LLVM.LLVMAppendBasicBlock(llvmFunction, "forLoopUpdate");
      // only generate a continuation block if there is a way to get out of the loop
      LLVMBasicBlockRef continuationBlock = forStatement.stopsExecution() ? null : LLVM.LLVMAppendBasicBlock(llvmFunction, "afterForLoop");

      if (conditional == null)
      {
        LLVM.LLVMBuildBr(builder, loopBody);
      }
      else
      {
        LLVM.LLVMBuildBr(builder, loopCheck);
        LLVM.LLVMPositionBuilderAtEnd(builder, loopCheck);
        LLVMValueRef conditionResult = buildExpression(conditional, llvmFunction, thisValue, variables);
        conditionResult = typeHelper.convertTemporaryToStandard(conditionResult, conditional.getType(), new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), llvmFunction);
        LLVM.LLVMBuildCondBr(builder, conditionResult, loopBody, continuationBlock);
      }

      LLVM.LLVMPositionBuilderAtEnd(builder, loopBody);
      if (continuationBlock != null)
      {
        breakBlocks.put(forStatement, continuationBlock);
      }
      continueBlocks.put(forStatement, loopUpdate == null ? (loopCheck == null ? loopBody : loopCheck) : loopUpdate);
      buildStatement(forStatement.getBlock(), returnType, llvmFunction, thisValue, variables, breakBlocks, continueBlocks, returnVoidCallback);
      if (!forStatement.getBlock().stopsExecution())
      {
        LLVM.LLVMBuildBr(builder, loopUpdate == null ? (loopCheck == null ? loopBody : loopCheck) : loopUpdate);
      }
      if (update != null)
      {
        LLVM.LLVMPositionBuilderAtEnd(builder, loopUpdate);
        buildStatement(update, returnType, llvmFunction, thisValue, variables, breakBlocks, continueBlocks, returnVoidCallback);
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
      LLVMValueRef conditional = buildExpression(ifStatement.getExpression(), llvmFunction, thisValue, variables);
      conditional = typeHelper.convertTemporaryToStandard(conditional, ifStatement.getExpression().getType(), new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), llvmFunction);

      LLVMBasicBlockRef thenClause = LLVM.LLVMAppendBasicBlock(llvmFunction, "then");
      LLVMBasicBlockRef elseClause = null;
      if (ifStatement.getElseClause() != null)
      {
        elseClause = LLVM.LLVMAppendBasicBlock(llvmFunction, "else");
      }
      LLVMBasicBlockRef continuation = null;
      if (!ifStatement.stopsExecution())
      {
        continuation = LLVM.LLVMAppendBasicBlock(llvmFunction, "continuation");
      }

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
        buildStatement(ifStatement.getElseClause(), returnType, llvmFunction, thisValue, variables, breakBlocks, continueBlocks, returnVoidCallback);
        if (!ifStatement.getElseClause().stopsExecution())
        {
          LLVM.LLVMBuildBr(builder, continuation);
        }
      }

      // build the then clause
      LLVM.LLVMPositionBuilderAtEnd(builder, thenClause);
      buildStatement(ifStatement.getThenClause(), returnType, llvmFunction, thisValue, variables, breakBlocks, continueBlocks, returnVoidCallback);
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
      LLVMValueRef pointer;
      boolean standardTypeRepresentation = false;
      if (assignee instanceof VariableAssignee)
      {
        Variable resolvedVariable = ((VariableAssignee) assignee).getResolvedVariable();
        if (resolvedVariable instanceof MemberVariable)
        {
          Field field = ((MemberVariable) resolvedVariable).getField();
          LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), field.getMemberIndex(), false)};
          pointer = LLVM.LLVMBuildGEP(builder, thisValue, C.toNativePointerArray(indices, false, true), indices.length, "");
          standardTypeRepresentation = true;
        }
        else if (resolvedVariable instanceof GlobalVariable)
        {
          pointer = getGlobal((GlobalVariable) resolvedVariable);
          standardTypeRepresentation = true;
        }
        else
        {
          pointer = variables.get(resolvedVariable);
          standardTypeRepresentation = false;
        }
      }
      else if (assignee instanceof ArrayElementAssignee)
      {
        ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
        LLVMValueRef array = buildExpression(arrayElementAssignee.getArrayExpression(), llvmFunction, thisValue, variables);
        LLVMValueRef dimension = buildExpression(arrayElementAssignee.getDimensionExpression(), llvmFunction, thisValue, variables);
        LLVMValueRef convertedDimension = typeHelper.convertTemporaryToStandard(dimension, arrayElementAssignee.getDimensionExpression().getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, llvmFunction);
        LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                     LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false),
                                                     convertedDimension};
        pointer = LLVM.LLVMBuildGEP(builder, array, C.toNativePointerArray(indices, false, true), indices.length, "");
        standardTypeRepresentation = true;
      }
      else if (assignee instanceof FieldAssignee)
      {
        FieldAssignee fieldAssignee = (FieldAssignee) assignee;
        FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
        if (fieldAccessExpression.getResolvedMember() instanceof Field)
        {
          Field field = (Field) fieldAccessExpression.getResolvedMember();
          if (field.isStatic())
          {
            pointer = getGlobal(field.getGlobalVariable());
            standardTypeRepresentation = true;
          }
          else
          {
            LLVMValueRef expressionValue = buildExpression(fieldAccessExpression.getBaseExpression(), llvmFunction, thisValue, variables);
            LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                         LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), field.getMemberIndex(), false)};
            pointer = LLVM.LLVMBuildGEP(builder, expressionValue, C.toNativePointerArray(indices, false, true), indices.length, "");
            standardTypeRepresentation = true;
          }
        }
        else
        {
          throw new IllegalArgumentException("Unknown member assigned to in a FieldAssignee: " + fieldAccessExpression.getResolvedMember());
        }
      }
      else
      {
        // ignore blank assignees, they shouldn't be able to get through variable resolution
        throw new IllegalStateException("Unknown Assignee type: " + assignee);
      }
      LLVMValueRef loaded = LLVM.LLVMBuildLoad(builder, pointer, "");
      PrimitiveType type = (PrimitiveType) assignee.getResolvedType();
      LLVMValueRef result;
      if (type.getPrimitiveTypeType().isFloating())
      {
        LLVMValueRef one = LLVM.LLVMConstReal(typeHelper.findTemporaryType(type), 1);
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
        LLVMValueRef one = LLVM.LLVMConstInt(typeHelper.findTemporaryType(type), 1, false);
        if (prefixIncDecStatement.isIncrement())
        {
          result = LLVM.LLVMBuildAdd(builder, loaded, one, "");
        }
        else
        {
          result = LLVM.LLVMBuildSub(builder, loaded, one, "");
        }
      }
      if (standardTypeRepresentation)
      {
        result = typeHelper.convertTemporaryToStandard(result, type, llvmFunction);
      }
      LLVM.LLVMBuildStore(builder, result, pointer);
    }
    else if (statement instanceof ReturnStatement)
    {
      Expression returnedExpression = ((ReturnStatement) statement).getExpression();
      if (returnedExpression == null)
      {
        returnVoidCallback.run();
      }
      else
      {
        LLVMValueRef value = buildExpression(returnedExpression, llvmFunction, thisValue, variables);
        LLVMValueRef convertedValue = typeHelper.convertTemporaryToStandard(value, returnedExpression.getType(), returnType, llvmFunction);
        LLVM.LLVMBuildRet(builder, convertedValue);
      }
    }
    else if (statement instanceof ShorthandAssignStatement)
    {
      ShorthandAssignStatement shorthandAssignStatement = (ShorthandAssignStatement) statement;
      Assignee[] assignees = shorthandAssignStatement.getAssignees();
      LLVMValueRef[] llvmAssigneePointers = new LLVMValueRef[assignees.length];
      boolean[] standardTypeRepresentations = new boolean[assignees.length];
      for (int i = 0; i < assignees.length; ++i)
      {
        if (assignees[i] instanceof VariableAssignee)
        {
          Variable resolvedVariable = ((VariableAssignee) assignees[i]).getResolvedVariable();
          if (resolvedVariable instanceof MemberVariable)
          {
            Field field = ((MemberVariable) resolvedVariable).getField();
            LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                         LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), field.getMemberIndex(), false)};
            llvmAssigneePointers[i] = LLVM.LLVMBuildGEP(builder, thisValue, C.toNativePointerArray(indices, false, true), indices.length, "");
            standardTypeRepresentations[i] = true;
          }
          else if (resolvedVariable instanceof GlobalVariable)
          {
            llvmAssigneePointers[i] = getGlobal((GlobalVariable) resolvedVariable);
            standardTypeRepresentations[i] = true;
          }
          else
          {
            llvmAssigneePointers[i] = variables.get(resolvedVariable);
            standardTypeRepresentations[i] = false;
          }
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          LLVMValueRef array = buildExpression(arrayElementAssignee.getArrayExpression(), llvmFunction, thisValue, variables);
          LLVMValueRef dimension = buildExpression(arrayElementAssignee.getDimensionExpression(), llvmFunction, thisValue, variables);
          LLVMValueRef convertedDimension = typeHelper.convertTemporaryToStandard(dimension, arrayElementAssignee.getDimensionExpression().getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, llvmFunction);
          LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false),
                                                       convertedDimension};
          llvmAssigneePointers[i] = LLVM.LLVMBuildGEP(builder, array, C.toNativePointerArray(indices, false, true), indices.length, "");
          standardTypeRepresentations[i] = true;
        }
        else if (assignees[i] instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];
          FieldAccessExpression fieldAccessExpression = fieldAssignee.getFieldAccessExpression();
          if (fieldAccessExpression.getResolvedMember() instanceof Field)
          {
            Field field = (Field) fieldAccessExpression.getResolvedMember();
            if (field.isStatic())
            {
              llvmAssigneePointers[i] = getGlobal(field.getGlobalVariable());
              standardTypeRepresentations[i] = true;
            }
            else
            {
              LLVMValueRef expressionValue = buildExpression(fieldAccessExpression.getBaseExpression(), llvmFunction, thisValue, variables);
              LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                           LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), field.getMemberIndex(), false)};
              llvmAssigneePointers[i] = LLVM.LLVMBuildGEP(builder, expressionValue, C.toNativePointerArray(indices, false, true), indices.length, "");
              standardTypeRepresentations[i] = true;
            }
          }
          else
          {
            throw new IllegalArgumentException("Unknown member assigned to in a FieldAssignee: " + fieldAccessExpression.getResolvedMember());
          }
        }
        else if (assignees[i] instanceof BlankAssignee)
        {
          // this assignee doesn't actually get assigned to
          llvmAssigneePointers[i] = null;
          standardTypeRepresentations[i] = false;
        }
        else
        {
          throw new IllegalStateException("Unknown Assignee type: " + assignees[i]);
        }
      }

      LLVMValueRef result = buildExpression(shorthandAssignStatement.getExpression(), llvmFunction, thisValue, variables);
      Type resultType = shorthandAssignStatement.getExpression().getType();
      LLVMValueRef[] resultValues = new LLVMValueRef[assignees.length];
      Type[] resultValueTypes = new Type[assignees.length];
      if (resultType instanceof TupleType && !resultType.isNullable() && ((TupleType) resultType).getSubTypes().length == assignees.length)
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
        if (llvmAssigneePointers[i] == null)
        {
          // this is a blank assignee, so don't try to do anything for it
          continue;
        }
        LLVMValueRef leftValue = LLVM.LLVMBuildLoad(builder, llvmAssigneePointers[i], "");
        if (standardTypeRepresentations[i])
        {
          leftValue = typeHelper.convertStandardToTemporary(leftValue, assignees[i].getResolvedType(), llvmFunction);
        }
        LLVMValueRef rightValue = typeHelper.convertTemporary(resultValues[i], resultValueTypes[i], assignees[i].getResolvedType());
        PrimitiveTypeType primitiveType = ((PrimitiveType) assignees[i].getResolvedType()).getPrimitiveTypeType();
        boolean floating = primitiveType.isFloating();
        boolean signed = primitiveType.isSigned();
        LLVMValueRef assigneeResult;
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
        if (standardTypeRepresentations[i])
        {
          assigneeResult = typeHelper.convertTemporaryToStandard(assigneeResult, assignees[i].getResolvedType(), llvmFunction);
        }
        LLVM.LLVMBuildStore(builder, assigneeResult, llvmAssigneePointers[i]);
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;

      LLVMBasicBlockRef loopCheck = LLVM.LLVMAppendBasicBlock(llvmFunction, "whileLoopCheck");
      LLVM.LLVMBuildBr(builder, loopCheck);

      LLVM.LLVMPositionBuilderAtEnd(builder, loopCheck);
      LLVMValueRef conditional = buildExpression(whileStatement.getExpression(), llvmFunction, thisValue, variables);
      conditional = typeHelper.convertTemporaryToStandard(conditional, whileStatement.getExpression().getType(), new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), llvmFunction);

      LLVMBasicBlockRef loopBodyBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "whileLoopBody");
      LLVMBasicBlockRef afterLoopBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "afterWhileLoop");
      LLVM.LLVMBuildCondBr(builder, conditional, loopBodyBlock, afterLoopBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, loopBodyBlock);
      // add the while statement's afterLoop block to the breakBlocks map before it's statement is built
      breakBlocks.put(whileStatement, afterLoopBlock);
      continueBlocks.put(whileStatement, loopCheck);
      buildStatement(whileStatement.getStatement(), returnType, llvmFunction, thisValue, variables, breakBlocks, continueBlocks, returnVoidCallback);

      if (!whileStatement.getStatement().stopsExecution())
      {
        LLVM.LLVMBuildBr(builder, loopCheck);
      }

      LLVM.LLVMPositionBuilderAtEnd(builder, afterLoopBlock);
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

  private LLVMValueRef buildArrayCreation(LLVMValueRef llvmFunction, LLVMValueRef[] llvmLengths, ArrayType type)
  {
    LLVMTypeRef llvmArrayType = typeHelper.findTemporaryType(type);
    LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false), // go into the pointer to the {i32, [0 x <type>]}
                                                 LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false), // go into the structure to get the [0 x <type>]
                                                 llvmLengths[0]};                                                                     // go length elements along the array, to get the byte directly after the whole structure, which is also our size
    LLVMValueRef llvmArraySize = LLVM.LLVMBuildGEP(builder, LLVM.LLVMConstNull(llvmArrayType), C.toNativePointerArray(indices, false, true), indices.length, "");
    LLVMValueRef llvmSize = LLVM.LLVMBuildPtrToInt(builder, llvmArraySize, LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), "");

    // call calloc to allocate the memory and initialise it to a string of zeros
    LLVMValueRef[] arguments = new LLVMValueRef[] {llvmSize, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false)};
    LLVMValueRef memoryPointer = LLVM.LLVMBuildCall(builder, callocFunction, C.toNativePointerArray(arguments, false, true), arguments.length, "");
    LLVMValueRef allocatedPointer = LLVM.LLVMBuildBitCast(builder, memoryPointer, llvmArrayType, "");

    LLVMValueRef[] sizeIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                     LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false)};
    LLVMValueRef sizeElementPointer = LLVM.LLVMBuildGEP(builder, allocatedPointer, C.toNativePointerArray(sizeIndices, false, true), sizeIndices.length, "");
    LLVM.LLVMBuildStore(builder, llvmLengths[0], sizeElementPointer);

    if (llvmLengths.length > 1)
    {
      // build a loop to create all of the elements of this array by recursively calling buildArrayCreation()
      ArrayType subType = (ArrayType) type.getBaseType();

      LLVMBasicBlockRef startBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVMBasicBlockRef loopCheckBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "arrayCreationCheck");
      LLVMBasicBlockRef loopBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "arrayCreation");
      LLVMBasicBlockRef exitBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "arrayCreationEnd");

      LLVM.LLVMBuildBr(builder, loopCheckBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, loopCheckBlock);
      LLVMValueRef phiNode = LLVM.LLVMBuildPhi(builder, LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), "arrayCounter");
      LLVMValueRef breakBoolean = LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntULT, phiNode, llvmLengths[0], "");
      LLVM.LLVMBuildCondBr(builder, breakBoolean, loopBlock, exitBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, loopBlock);

      // recurse to create this element of the array
      LLVMValueRef[] subLengths = new LLVMValueRef[llvmLengths.length - 1];
      System.arraycopy(llvmLengths, 1, subLengths, 0, subLengths.length);
      LLVMValueRef subArray = buildArrayCreation(llvmFunction, subLengths, subType);

      // find the indices for the current location in the array
      LLVMValueRef[] assignmentIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                   LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false),
                                                   phiNode};
      LLVMValueRef elementPointer = LLVM.LLVMBuildGEP(builder, allocatedPointer, C.toNativePointerArray(assignmentIndices, false, true), assignmentIndices.length, "");
      LLVM.LLVMBuildStore(builder, subArray, elementPointer);

      // add the incoming values to the phi node
      LLVMValueRef nextCounterValue = LLVM.LLVMBuildAdd(builder, phiNode, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false), "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false), nextCounterValue};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {startBlock, LLVM.LLVMGetInsertBlock(builder)};
      LLVM.LLVMAddIncoming(phiNode, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), 2);

      LLVM.LLVMBuildBr(builder, loopCheckBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, exitBlock);
    }
    return allocatedPointer;
  }

  /**
   * Builds the LLVM statements for a null check on the specified value.
   * @param value - the LLVMValueRef to compare to null, in a temporary native representation
   * @param type - the type of the specified LLVMValueRef
   * @return an LLVMValueRef for an i1, which will be 1 if the value is non-null, and 0 if the value is null
   */
  private LLVMValueRef buildNullCheck(LLVMValueRef value, Type type)
  {
    if (!type.isNullable())
    {
      throw new IllegalArgumentException("A null check can only work on a nullable type");
    }
    if (type instanceof ArrayType)
    {
      return LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntPredicate.LLVMIntNE, value, LLVM.LLVMConstNull(typeHelper.findTemporaryType(type)), "");
    }
    if (type instanceof FunctionType)
    {
      LLVMValueRef functionPointer = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
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
    }
    if (type instanceof NullType)
    {
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 0, false);
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
   * @param llvmFunction - the LLVM Function that we are building this check in, this must be provided so that we can append basic blocks
   * @return an LLVMValueRef for an i1, which will be 1 if the check returns true, or 0 if the check returns false
   */
  private LLVMValueRef buildEqualityCheck(LLVMValueRef left, LLVMValueRef right, Type type, EqualityOperator operator, LLVMValueRef llvmFunction)
  {
    if (type instanceof ArrayType)
    {
      return LLVM.LLVMBuildICmp(builder, getPredicate(operator, false), left, right, "");
    }
    if (type instanceof FunctionType)
    {
      LLVMValueRef leftOpaque = LLVM.LLVMBuildExtractValue(builder, left, 0, "");
      LLVMValueRef rightOpaque = LLVM.LLVMBuildExtractValue(builder, right, 0, "");
      LLVMValueRef opaqueComparison = LLVM.LLVMBuildICmp(builder, getPredicate(operator, false), leftOpaque, rightOpaque, "");
      LLVMValueRef leftFunction = LLVM.LLVMBuildExtractValue(builder, left, 1, "");
      LLVMValueRef rightFunction = LLVM.LLVMBuildExtractValue(builder, right, 1, "");
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
        if (type.isNullable())
        {
          LLVMValueRef leftNullity = LLVM.LLVMBuildIsNotNull(builder, left, "");
          LLVMValueRef rightNullity = LLVM.LLVMBuildIsNotNull(builder, right, "");
          nullityComparison = LLVM.LLVMBuildICmp(builder, getPredicate(operator, false), leftNullity, rightNullity, "");
          LLVMValueRef bothNotNull = LLVM.LLVMBuildAnd(builder, leftNullity, rightNullity, "");

          startBlock = LLVM.LLVMGetInsertBlock(builder);
          LLVMBasicBlockRef comparisonBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "equality_comparevalues");
          finalBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "equality_final");

          LLVM.LLVMBuildCondBr(builder, bothNotNull, comparisonBlock, finalBlock);
          LLVM.LLVMPositionBuilderAtEnd(builder, comparisonBlock);
        }

        // compare each of the fields from the left and right values
        Field[] nonStaticFields = typeDefinition.getNonStaticFields();
        LLVMValueRef[] compareResults = new LLVMValueRef[nonStaticFields.length];
        for (int i = 0; i < nonStaticFields.length; ++i)
        {
          Type fieldType = nonStaticFields[i].getType();
          LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), i, false)};
          LLVMValueRef leftField = LLVM.LLVMBuildGEP(builder, left, C.toNativePointerArray(indices, false, true), indices.length, "");
          LLVMValueRef rightField = LLVM.LLVMBuildGEP(builder, right, C.toNativePointerArray(indices, false, true), indices.length, "");
          LLVMValueRef leftValue = LLVM.LLVMBuildLoad(builder, leftField, "");
          LLVMValueRef rightValue = LLVM.LLVMBuildLoad(builder, rightField, "");
          leftValue = typeHelper.convertStandardToTemporary(leftValue, fieldType, llvmFunction);
          rightValue = typeHelper.convertStandardToTemporary(rightValue, fieldType, llvmFunction);
          compareResults[i] = buildEqualityCheck(leftValue, rightValue, fieldType, operator, llvmFunction);
        }

        // AND or OR the list together, using a binary tree
        int multiple = 1;
        while (multiple < nonStaticFields.length)
        {
          for (int i = 0; i < nonStaticFields.length; i += 2 * multiple)
          {
            LLVMValueRef first = compareResults[i];
            if (i + multiple >= nonStaticFields.length)
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

        if (type.isNullable())
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
    }
    if (type instanceof PrimitiveType)
    {
      PrimitiveTypeType primitiveTypeType = ((PrimitiveType) type).getPrimitiveTypeType();
      LLVMValueRef leftValue = left;
      LLVMValueRef rightValue = right;
      if (type.isNullable())
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
      if (type.isNullable())
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
      if (type.isNullable())
      {
        LLVMValueRef leftNullity = LLVM.LLVMBuildExtractValue(builder, left, 0, "");
        LLVMValueRef rightNullity = LLVM.LLVMBuildExtractValue(builder, right, 0, "");
        nullityComparison = LLVM.LLVMBuildICmp(builder, getPredicate(operator, false), leftNullity, rightNullity, "");
        LLVMValueRef bothNotNull = LLVM.LLVMBuildAnd(builder, leftNullity, rightNullity, "");

        startBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVMBasicBlockRef comparisonBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "equality_comparevalues");
        finalBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "equality_final");

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
        compareResults[i] = buildEqualityCheck(leftValue, rightValue, subType, operator, llvmFunction);
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

      if (type.isNullable())
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

  private LLVMValueRef buildExpression(Expression expression, LLVMValueRef llvmFunction, LLVMValueRef thisValue, Map<Variable, LLVMValueRef> variables)
  {
    if (expression instanceof ArithmeticExpression)
    {
      ArithmeticExpression arithmeticExpression = (ArithmeticExpression) expression;
      LLVMValueRef left = buildExpression(arithmeticExpression.getLeftSubExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef right = buildExpression(arithmeticExpression.getRightSubExpression(), llvmFunction, thisValue, variables);
      PrimitiveType leftType = (PrimitiveType) arithmeticExpression.getLeftSubExpression().getType();
      PrimitiveType rightType = (PrimitiveType) arithmeticExpression.getRightSubExpression().getType();
      // cast if necessary
      PrimitiveType resultType = (PrimitiveType) arithmeticExpression.getType();
      left = typeHelper.convertTemporary(left, leftType, resultType);
      right = typeHelper.convertTemporary(right, rightType, resultType);
      boolean floating = resultType.getPrimitiveTypeType().isFloating();
      boolean signed = resultType.getPrimitiveTypeType().isSigned();
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
      LLVMValueRef arrayValue = buildExpression(arrayAccessExpression.getArrayExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef dimensionValue = buildExpression(arrayAccessExpression.getDimensionExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef convertedDimensionValue = typeHelper.convertTemporaryToStandard(dimensionValue, arrayAccessExpression.getDimensionExpression().getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, llvmFunction);
      LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                                     LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false),
                                                                     convertedDimensionValue};
      LLVMValueRef elementPointer = LLVM.LLVMBuildGEP(builder, arrayValue, C.toNativePointerArray(indices, false, true), indices.length, "");
      ArrayType arrayType = (ArrayType) arrayAccessExpression.getArrayExpression().getType();
      return typeHelper.convertStandardPointerToTemporary(elementPointer, arrayType.getBaseType(), arrayAccessExpression.getType(), llvmFunction);
    }
    if (expression instanceof ArrayCreationExpression)
    {
      ArrayCreationExpression arrayCreationExpression = (ArrayCreationExpression) expression;
      ArrayType type = arrayCreationExpression.getType();
      Expression[] dimensionExpressions = arrayCreationExpression.getDimensionExpressions();

      if (dimensionExpressions == null)
      {
        Expression[] valueExpressions = arrayCreationExpression.getValueExpressions();
        LLVMValueRef llvmLength = LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), valueExpressions.length, false);
        LLVMValueRef array = buildArrayCreation(llvmFunction, new LLVMValueRef[] {llvmLength}, type);
        for (int i = 0; i < valueExpressions.length; i++)
        {
          LLVMValueRef expressionValue = buildExpression(valueExpressions[i], llvmFunction, thisValue, variables);
          LLVMValueRef convertedValue = typeHelper.convertTemporaryToStandard(expressionValue, valueExpressions[i].getType(), type.getBaseType(), llvmFunction);
          LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), i, false)};
          LLVMValueRef elementPointer = LLVM.LLVMBuildGEP(builder, array, C.toNativePointerArray(indices, false, true), indices.length, "");
          LLVM.LLVMBuildStore(builder, convertedValue, elementPointer);
        }
        return array;
      }

      LLVMValueRef[] llvmLengths = new LLVMValueRef[dimensionExpressions.length];
      for (int i = 0; i < llvmLengths.length; i++)
      {
        LLVMValueRef expressionValue = buildExpression(dimensionExpressions[i], llvmFunction, thisValue, variables);
        llvmLengths[i] = typeHelper.convertTemporaryToStandard(expressionValue, dimensionExpressions[i].getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, llvmFunction);
      }
      return buildArrayCreation(llvmFunction, llvmLengths, type);
    }
    if (expression instanceof BitwiseNotExpression)
    {
      LLVMValueRef value = buildExpression(((BitwiseNotExpression) expression).getExpression(), llvmFunction, thisValue, variables);
      value = typeHelper.convertTemporary(value, ((BitwiseNotExpression) expression).getExpression().getType(), expression.getType());
      return LLVM.LLVMBuildNot(builder, value, "");
    }
    if (expression instanceof BooleanLiteralExpression)
    {
      LLVMValueRef value = LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), ((BooleanLiteralExpression) expression).getValue() ? 1 : 0, false);
      return typeHelper.convertStandardToTemporary(value, new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), expression.getType(), llvmFunction);
    }
    if (expression instanceof BooleanNotExpression)
    {
      LLVMValueRef value = buildExpression(((BooleanNotExpression) expression).getExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef result = LLVM.LLVMBuildNot(builder, value, "");
      return typeHelper.convertTemporary(result, ((BooleanNotExpression) expression).getExpression().getType(), expression.getType());
    }
    if (expression instanceof BracketedExpression)
    {
      BracketedExpression bracketedExpression = (BracketedExpression) expression;
      LLVMValueRef value = buildExpression(bracketedExpression.getExpression(), llvmFunction, thisValue, variables);
      return typeHelper.convertTemporary(value, bracketedExpression.getExpression().getType(), expression.getType());
    }
    if (expression instanceof CastExpression)
    {
      CastExpression castExpression = (CastExpression) expression;
      LLVMValueRef value = buildExpression(castExpression.getExpression(), llvmFunction, thisValue, variables);
      return typeHelper.convertTemporary(value, castExpression.getExpression().getType(), castExpression.getType());
    }
    if (expression instanceof ClassCreationExpression)
    {
      ClassCreationExpression classCreationExpression = (ClassCreationExpression) expression;
      Expression[] arguments = classCreationExpression.getArguments();
      Constructor constructor = classCreationExpression.getResolvedConstructor();
      Parameter[] parameters = constructor.getParameters();
      LLVMValueRef[] llvmArguments = new LLVMValueRef[1 + arguments.length];
      for (int i = 0; i < arguments.length; ++i)
      {
        LLVMValueRef argument = buildExpression(arguments[i], llvmFunction, thisValue, variables);
        llvmArguments[i + 1] = typeHelper.convertTemporaryToStandard(argument, arguments[i].getType(), parameters[i].getType(), llvmFunction);
      }
      Type type = classCreationExpression.getType();
      LLVMTypeRef nativeType = typeHelper.findTemporaryType(type);
      LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false)};
      LLVMValueRef llvmStructSize = LLVM.LLVMBuildGEP(builder, LLVM.LLVMConstNull(nativeType), C.toNativePointerArray(indices, false, true), indices.length, "");
      LLVMValueRef llvmSize = LLVM.LLVMBuildPtrToInt(builder, llvmStructSize, LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), "");
      LLVMValueRef[] callocArguments = new LLVMValueRef[] {llvmSize, LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false)};
      LLVMValueRef memory = LLVM.LLVMBuildCall(builder, callocFunction, C.toNativePointerArray(callocArguments, false, true), callocArguments.length, "");
      LLVMValueRef pointer = LLVM.LLVMBuildBitCast(builder, memory, nativeType, "");
      llvmArguments[0] = pointer;
      // get the constructor and call it
      LLVMValueRef llvmFunc = getConstructorFunction(constructor);
      LLVMValueRef result = LLVM.LLVMBuildCall(builder, llvmFunc, C.toNativePointerArray(llvmArguments, false, true), llvmArguments.length, "");
      return result;
    }
    if (expression instanceof EqualityExpression)
    {
      EqualityExpression equalityExpression = (EqualityExpression) expression;
      EqualityOperator operator = equalityExpression.getOperator();
      // if the type checker has annotated this as a null check, just perform it without building both sub-expressions
      Expression nullCheckExpression = equalityExpression.getNullCheckExpression();
      if (nullCheckExpression != null)
      {
        LLVMValueRef value = buildExpression(nullCheckExpression, llvmFunction, thisValue, variables);
        LLVMValueRef convertedValue = typeHelper.convertTemporary(value, nullCheckExpression.getType(), equalityExpression.getComparisonType());
        LLVMValueRef nullity = buildNullCheck(convertedValue, equalityExpression.getComparisonType());
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

      LLVMValueRef left = buildExpression(equalityExpression.getLeftSubExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef right = buildExpression(equalityExpression.getRightSubExpression(), llvmFunction, thisValue, variables);
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
        if (leftType.isNullable())
        {
          leftIsNotNull = LLVM.LLVMBuildExtractValue(builder, left, 0, "");
          leftValue = LLVM.LLVMBuildExtractValue(builder, left, 1, "");
        }
        if (rightType.isNullable())
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
          if (leftType.isNullable() || rightType.isNullable())
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
          return typeHelper.convertTemporary(comparisonResult, new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), equalityExpression.getType());
        }
        throw new IllegalArgumentException("Unknown result type, unable to generate comparison expression: " + equalityExpression);
      }
      // perform a standard equality check, using buildEqualityCheck()
      left = typeHelper.convertTemporary(left, leftType, comparisonType);
      right = typeHelper.convertTemporary(right, rightType, comparisonType);
      return buildEqualityCheck(left, right, comparisonType, operator, llvmFunction);
    }
    if (expression instanceof FieldAccessExpression)
    {
      FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) expression;
      Member member = fieldAccessExpression.getResolvedMember();
      if (member instanceof ArrayLengthMember)
      {
        LLVMValueRef array = buildExpression(fieldAccessExpression.getBaseExpression(), llvmFunction, thisValue, variables);
        LLVMValueRef[] sizeIndices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                         LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false)};
        LLVMValueRef elementPointer = LLVM.LLVMBuildGEP(builder, array, C.toNativePointerArray(sizeIndices, false, true), sizeIndices.length, "");
        return LLVM.LLVMBuildLoad(builder, elementPointer, "");
      }
      if (member instanceof Field)
      {
        Field field = (Field) member;
        if (field.isStatic())
        {
          LLVMValueRef global = getGlobal(field.getGlobalVariable());
          return typeHelper.convertStandardPointerToTemporary(global, field.getType(), fieldAccessExpression.getType(), llvmFunction);
        }

        LLVMValueRef baseValue = buildExpression(fieldAccessExpression.getBaseExpression(), llvmFunction, thisValue, variables);
        LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                     LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), field.getMemberIndex(), false)};
        LLVMValueRef elementPointer = LLVM.LLVMBuildGEP(builder, baseValue, C.toNativePointerArray(indices, false, true), indices.length, "");
        return typeHelper.convertStandardPointerToTemporary(elementPointer, field.getType(), fieldAccessExpression.getType(), llvmFunction);
      }
    }
    if (expression instanceof FloatingLiteralExpression)
    {
      double value = Double.parseDouble(((FloatingLiteralExpression) expression).getLiteral().toString());
      LLVMValueRef llvmValue = LLVM.LLVMConstReal(typeHelper.findStandardType(TypeChecker.findTypeWithNullability(expression.getType(), false)), value);
      return typeHelper.convertStandardToTemporary(llvmValue, TypeChecker.findTypeWithNullability(expression.getType(), false), expression.getType(), llvmFunction);
    }
    if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression functionExpression = (FunctionCallExpression) expression;
      Constructor resolvedConstructor = functionExpression.getResolvedConstructor();
      Method resolvedMethod = functionExpression.getResolvedMethod();
      Expression resolvedBaseExpression = functionExpression.getResolvedBaseExpression();

      Type[] parameterTypes;
      Type returnType;
      LLVMValueRef llvmResolvedFunction;
      if (resolvedConstructor != null)
      {
        Parameter[] params = resolvedConstructor.getParameters();
        parameterTypes = new Type[params.length];
        for (int i = 0; i < params.length; ++i)
        {
          parameterTypes[i] = params[i].getType();
        }
        returnType = new NamedType(false, resolvedConstructor.getContainingTypeDefinition());
        llvmResolvedFunction = getConstructorFunction(resolvedConstructor);
      }
      else if (resolvedMethod != null)
      {
        Parameter[] params = resolvedMethod.getParameters();
        parameterTypes = new Type[params.length];
        for (int i = 0; i < params.length; ++i)
        {
          parameterTypes[i] = params[i].getType();
        }
        returnType = resolvedMethod.getReturnType();
        LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
        llvmResolvedFunction = getMethodFunction(resolvedMethod);
        LLVM.LLVMPositionBuilderAtEnd(builder, currentBlock);
      }
      else if (resolvedBaseExpression != null)
      {
        FunctionType baseType = (FunctionType) resolvedBaseExpression.getType();
        parameterTypes = baseType.getParameterTypes();
        returnType = baseType.getReturnType();
        llvmResolvedFunction = null;
      }
      else
      {
        throw new IllegalArgumentException("Unresolved function call expression: " + functionExpression);
      }

      LLVMValueRef callee = null;
      if (resolvedBaseExpression != null)
      {
        callee = buildExpression(resolvedBaseExpression, llvmFunction, thisValue, variables);
      }

      Expression[] arguments = functionExpression.getArguments();
      LLVMValueRef[] values = new LLVMValueRef[arguments.length];
      for (int i = 0; i < arguments.length; i++)
      {
        LLVMValueRef arg = buildExpression(arguments[i], llvmFunction, thisValue, variables);
        values[i] = typeHelper.convertTemporaryToStandard(arg, arguments[i].getType(), parameterTypes[i], llvmFunction);
      }

      LLVMValueRef result;
      if (resolvedConstructor != null)
      {
        result = LLVM.LLVMBuildCall(builder, llvmResolvedFunction, C.toNativePointerArray(values, false, true), values.length, "");
      }
      else if (resolvedMethod != null)
      {
        LLVMValueRef[] realArguments;
        if (resolvedMethod.isStatic())
        {
          realArguments = values;
        }
        else
        {
          realArguments = new LLVMValueRef[values.length + 1];
          realArguments[0] = callee;
          if (callee == null)
          {
            realArguments[0] = thisValue;
          }
          System.arraycopy(values, 0, realArguments, 1, values.length);
        }
        result = LLVM.LLVMBuildCall(builder, llvmResolvedFunction, C.toNativePointerArray(realArguments, false, true), realArguments.length, "");
      }
      else if (resolvedBaseExpression != null)
      {
        // callee here is actually a tuple of an opaque pointer and a function type, where the first argument to the function is the opaque pointer
        LLVMValueRef firstArgument = LLVM.LLVMBuildExtractValue(builder, callee, 0, "");
        LLVMValueRef calleeFunction = LLVM.LLVMBuildExtractValue(builder, callee, 1, "");
        LLVMValueRef[] realArguments = new LLVMValueRef[values.length + 1];
        realArguments[0] = firstArgument;
        System.arraycopy(values, 0, realArguments, 1, values.length);
        result = LLVM.LLVMBuildCall(builder, calleeFunction, C.toNativePointerArray(realArguments, false, true), realArguments.length, "");
      }
      else
      {
        throw new IllegalArgumentException("Unresolved function call expression: " + functionExpression);
      }
      if (returnType instanceof VoidType)
      {
        return result;
      }
      return typeHelper.convertStandardToTemporary(result, returnType, functionExpression.getType(), llvmFunction);
    }
    if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIf = (InlineIfExpression) expression;
      LLVMValueRef conditionValue = buildExpression(inlineIf.getCondition(), llvmFunction, thisValue, variables);
      conditionValue = typeHelper.convertTemporaryToStandard(conditionValue, inlineIf.getCondition().getType(), new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), llvmFunction);
      LLVMBasicBlockRef thenBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "inlineIfThen");
      LLVMBasicBlockRef elseBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "inlineIfElse");
      LLVMBasicBlockRef continuationBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "afterInlineIf");

      LLVM.LLVMBuildCondBr(builder, conditionValue, thenBlock, elseBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, thenBlock);
      LLVMValueRef thenValue = buildExpression(inlineIf.getThenExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef convertedThenValue = typeHelper.convertTemporary(thenValue, inlineIf.getThenExpression().getType(), inlineIf.getType());
      LLVMBasicBlockRef thenBranchBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, elseBlock);
      LLVMValueRef elseValue = buildExpression(inlineIf.getElseExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef convertedElseValue = typeHelper.convertTemporary(elseValue, inlineIf.getElseExpression().getType(), inlineIf.getType());
      LLVMBasicBlockRef elseBranchBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
      LLVMValueRef result = LLVM.LLVMBuildPhi(builder, typeHelper.findTemporaryType(inlineIf.getType()), "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {convertedThenValue, convertedElseValue};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {thenBranchBlock, elseBranchBlock};
      LLVM.LLVMAddIncoming(result, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), 2);
      return result;
    }
    if (expression instanceof IntegerLiteralExpression)
    {
      int n = ((IntegerLiteralExpression) expression).getLiteral().getValue().intValue();
      LLVMValueRef value = LLVM.LLVMConstInt(typeHelper.findStandardType(TypeChecker.findTypeWithNullability(expression.getType(), false)), n, false);
      return typeHelper.convertStandardToTemporary(value, TypeChecker.findTypeWithNullability(expression.getType(), false), expression.getType(), llvmFunction);
    }
    if (expression instanceof LogicalExpression)
    {
      LogicalExpression logicalExpression = (LogicalExpression) expression;
      LLVMValueRef left = buildExpression(logicalExpression.getLeftSubExpression(), llvmFunction, thisValue, variables);
      PrimitiveType leftType = (PrimitiveType) logicalExpression.getLeftSubExpression().getType();
      PrimitiveType rightType = (PrimitiveType) logicalExpression.getRightSubExpression().getType();
      // cast if necessary
      PrimitiveType resultType = (PrimitiveType) logicalExpression.getType();
      left = typeHelper.convertTemporary(left, leftType, resultType);
      LogicalOperator operator = logicalExpression.getOperator();
      if (operator != LogicalOperator.SHORT_CIRCUIT_AND && operator != LogicalOperator.SHORT_CIRCUIT_OR)
      {
        LLVMValueRef right = buildExpression(logicalExpression.getRightSubExpression(), llvmFunction, thisValue, variables);
        right = typeHelper.convertTemporary(right, rightType, resultType);
        switch (operator)
        {
        case AND:
          return LLVM.LLVMBuildAnd(builder, left, right, "");
        case OR:
          return LLVM.LLVMBuildOr(builder, left, right, "");
        case XOR:
          return LLVM.LLVMBuildXor(builder, left, right, "");
        default:
          throw new IllegalStateException("Unexpected non-short-circuit operator: " + logicalExpression.getOperator());
        }
      }
      LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVMBasicBlockRef rightCheckBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "shortCircuitCheck");
      LLVMBasicBlockRef continuationBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "shortCircuitContinue");
      // the only difference between short circuit AND and OR is whether they jump to the check block when the left hand side is true or false
      LLVMBasicBlockRef trueDest = operator == LogicalOperator.SHORT_CIRCUIT_AND ? rightCheckBlock : continuationBlock;
      LLVMBasicBlockRef falseDest = operator == LogicalOperator.SHORT_CIRCUIT_AND ? continuationBlock : rightCheckBlock;
      LLVM.LLVMBuildCondBr(builder, left, trueDest, falseDest);

      LLVM.LLVMPositionBuilderAtEnd(builder, rightCheckBlock);
      LLVMValueRef right = buildExpression(logicalExpression.getRightSubExpression(), llvmFunction, thisValue, variables);
      right = typeHelper.convertTemporary(right, rightType, resultType);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
      // create a phi node for the result, and return it
      LLVMValueRef phi = LLVM.LLVMBuildPhi(builder, typeHelper.findTemporaryType(resultType), "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {left, right};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {currentBlock, rightCheckBlock};
      LLVM.LLVMAddIncoming(phi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), 2);
      return phi;
    }
    if (expression instanceof MinusExpression)
    {
      MinusExpression minusExpression = (MinusExpression) expression;
      LLVMValueRef value = buildExpression(minusExpression.getExpression(), llvmFunction, thisValue, variables);
      value = typeHelper.convertTemporary(value, minusExpression.getExpression().getType(), TypeChecker.findTypeWithNullability(minusExpression.getType(), false));
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
      return typeHelper.convertTemporary(result, TypeChecker.findTypeWithNullability(minusExpression.getType(), false), minusExpression.getType());
    }
    if (expression instanceof NullCoalescingExpression)
    {
      NullCoalescingExpression nullCoalescingExpression = (NullCoalescingExpression) expression;

      LLVMValueRef nullableValue = buildExpression(nullCoalescingExpression.getNullableExpression(), llvmFunction, thisValue, variables);
      nullableValue = typeHelper.convertTemporary(nullableValue, nullCoalescingExpression.getNullableExpression().getType(), TypeChecker.findTypeWithNullability(nullCoalescingExpression.getType(), true));
      LLVMValueRef checkResult = buildNullCheck(nullableValue, TypeChecker.findTypeWithNullability(nullCoalescingExpression.getType(), true));

      LLVMBasicBlockRef conversionBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "nullCoalescingConversion");
      LLVMBasicBlockRef alternativeBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "nullCoalescingAlternative");
      LLVMBasicBlockRef continuationBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "nullCoalescingContinuation");
      LLVM.LLVMBuildCondBr(builder, checkResult, conversionBlock, alternativeBlock);

      // create a block to convert the nullable value into a non-nullable value
      LLVM.LLVMPositionBuilderAtEnd(builder, conversionBlock);
      LLVMValueRef convertedNullableValue = typeHelper.convertTemporary(nullableValue, TypeChecker.findTypeWithNullability(nullCoalescingExpression.getType(), true), nullCoalescingExpression.getType());
      LLVMBasicBlockRef endConversionBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, alternativeBlock);
      LLVMValueRef alternativeValue = buildExpression(nullCoalescingExpression.getAlternativeExpression(), llvmFunction, thisValue, variables);
      alternativeValue = typeHelper.convertTemporary(alternativeValue, nullCoalescingExpression.getAlternativeExpression().getType(), nullCoalescingExpression.getType());
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
    if (expression instanceof RelationalExpression)
    {
      RelationalExpression relationalExpression = (RelationalExpression) expression;
      LLVMValueRef left = buildExpression(relationalExpression.getLeftSubExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef right = buildExpression(relationalExpression.getRightSubExpression(), llvmFunction, thisValue, variables);
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
            !leftType.isNullable() && !rightType.isNullable())
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
      left = typeHelper.convertTemporary(left, leftType, resultType);
      right = typeHelper.convertTemporary(right, rightType, resultType);
      LLVMValueRef result;
      if (resultType.getPrimitiveTypeType().isFloating())
      {
        result = LLVM.LLVMBuildFCmp(builder, getPredicate(relationalExpression.getOperator(), true, true), left, right, "");
      }
      else
      {
        result = LLVM.LLVMBuildICmp(builder, getPredicate(relationalExpression.getOperator(), false, resultType.getPrimitiveTypeType().isSigned()), left, right, "");
      }
      return typeHelper.convertTemporary(result, new PrimitiveType(false, PrimitiveTypeType.BOOLEAN, null), relationalExpression.getType());
    }
    if (expression instanceof ShiftExpression)
    {
      ShiftExpression shiftExpression = (ShiftExpression) expression;
      LLVMValueRef leftValue = buildExpression(shiftExpression.getLeftExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef rightValue = buildExpression(shiftExpression.getRightExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef convertedLeft = typeHelper.convertTemporary(leftValue, shiftExpression.getLeftExpression().getType(), shiftExpression.getType());
      LLVMValueRef convertedRight = typeHelper.convertTemporary(rightValue, shiftExpression.getRightExpression().getType(), shiftExpression.getType());
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
      Type nonNullableTupleType = TypeChecker.findTypeWithNullability(tupleExpression.getType(), false);
      LLVMValueRef currentValue = LLVM.LLVMGetUndef(typeHelper.findTemporaryType(nonNullableTupleType));
      for (int i = 0; i < subExpressions.length; i++)
      {
        LLVMValueRef value = buildExpression(subExpressions[i], llvmFunction, thisValue, variables);
        Type type = tupleTypes[i];
        value = typeHelper.convertTemporary(value, subExpressions[i].getType(), type);
        currentValue = LLVM.LLVMBuildInsertValue(builder, currentValue, value, i, "");
      }
      return typeHelper.convertTemporary(currentValue, nonNullableTupleType, tupleExpression.getType());
    }
    if (expression instanceof TupleIndexExpression)
    {
      TupleIndexExpression tupleIndexExpression = (TupleIndexExpression) expression;
      TupleType tupleType = (TupleType) tupleIndexExpression.getExpression().getType();
      LLVMValueRef result = buildExpression(tupleIndexExpression.getExpression(), llvmFunction, thisValue, variables);
      // convert the 1-based indexing to 0-based before extracting the value
      int index = tupleIndexExpression.getIndexLiteral().getValue().intValue() - 1;
      LLVMValueRef value = LLVM.LLVMBuildExtractValue(builder, result, index, "");
      return typeHelper.convertTemporary(value, tupleType.getSubTypes()[index], tupleIndexExpression.getType());
    }
    if (expression instanceof VariableExpression)
    {
      Variable variable = ((VariableExpression) expression).getResolvedVariable();
      if (variable instanceof MemberVariable)
      {
        Field field = ((MemberVariable) variable).getField();
        LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                     LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), field.getMemberIndex(), false)};
        LLVMValueRef elementPointer = LLVM.LLVMBuildGEP(builder, thisValue, C.toNativePointerArray(indices, false, true), indices.length, "");
        return typeHelper.convertStandardPointerToTemporary(elementPointer, variable.getType(), expression.getType(), llvmFunction);
      }
      if (variable instanceof GlobalVariable)
      {
        LLVMValueRef global = getGlobal((GlobalVariable) variable);
        return typeHelper.convertStandardPointerToTemporary(global, variable.getType(), expression.getType(), llvmFunction);
      }
      LLVMValueRef value = variables.get(variable);
      if (value == null)
      {
        throw new IllegalStateException("Missing LLVMValueRef in variable Map: " + ((VariableExpression) expression).getName());
      }
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    throw new IllegalArgumentException("Unknown Expression type: " + expression);
  }
}
