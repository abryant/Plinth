package eu.bryants.anthony.toylanguage.compiler.passes;

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

import com.sun.jna.Pointer;

import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.expression.ArithmeticExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ArrayAccessExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ArrayCreationExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BitwiseNotExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BooleanLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BooleanNotExpression;
import eu.bryants.anthony.toylanguage.ast.expression.BracketedExpression;
import eu.bryants.anthony.toylanguage.ast.expression.CastExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ComparisonExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ComparisonExpression.ComparisonOperator;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.toylanguage.ast.expression.InlineIfExpression;
import eu.bryants.anthony.toylanguage.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.LogicalExpression;
import eu.bryants.anthony.toylanguage.ast.expression.LogicalExpression.LogicalOperator;
import eu.bryants.anthony.toylanguage.ast.expression.MinusExpression;
import eu.bryants.anthony.toylanguage.ast.expression.NullLiteralExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ShiftExpression;
import eu.bryants.anthony.toylanguage.ast.expression.ThisExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleIndexExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.member.ArrayLengthMember;
import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.ast.member.Method;
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

/*
 * Created on 5 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class CodeGenerator
{
  private CompoundDefinition compoundDefinition;

  private LLVMModuleRef module;
  private LLVMBuilderRef builder;

  private LLVMValueRef callocFunction;

  private Map<GlobalVariable, LLVMValueRef> globalVariables = new HashMap<GlobalVariable, LLVMValueRef>();

  public CodeGenerator(CompoundDefinition compoundDefinition)
  {
    this.compoundDefinition = compoundDefinition;
    module = LLVM.LLVMModuleCreateWithName(compoundDefinition.getQualifiedName().toString());
    builder = LLVM.LLVMCreateBuilder();
  }

  public void generate(String outputPath)
  {
    // add all of the global (static) variables
    addGlobalVariables();
    // add all of the LLVM functions, including constructors, methods, and normal functions
    addFunctions();

    addConstructorBodies(compoundDefinition);
    addMethodBodies(compoundDefinition);
    LLVM.LLVMWriteBitcodeToFile(module, outputPath);
  }

  private void addGlobalVariables()
  {
    for (Field field : compoundDefinition.getFields())
    {
      if (field.isStatic())
      {
        GlobalVariable globalVariable = field.getGlobalVariable();
        LLVMValueRef value = LLVM.LLVMAddGlobal(module, findNativeType(field.getType()), globalVariable.getMangledName());
        LLVM.LLVMSetInitializer(value, LLVM.LLVMConstNull(findNativeType(field.getType())));
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
    LLVMValueRef newValue = LLVM.LLVMAddGlobal(module, findNativeType(type), globalVariable.getMangledName());
    LLVM.LLVMSetInitializer(newValue, LLVM.LLVMConstNull(findNativeType(type)));
    globalVariables.put(globalVariable, newValue);
    return newValue;
  }

  private void addFunctions()
  {
    // add calloc() as an external function
    LLVMTypeRef callocReturnType = LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0);
    LLVMTypeRef[] callocParamTypes = new LLVMTypeRef[] {LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount())};
    callocFunction = LLVM.LLVMAddFunction(module, "calloc", LLVM.LLVMFunctionType(callocReturnType, C.toNativePointerArray(callocParamTypes, false, true), callocParamTypes.length, false));

    for (Constructor constructor : compoundDefinition.getConstructors())
    {
      getConstructorFunction(constructor);
    }

    for (Method method : compoundDefinition.getAllMethods())
    {
      getMethodFunction(method);
    }
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

    Parameter[] parameters = constructor.getParameters();
    LLVMTypeRef[] types = new LLVMTypeRef[parameters.length];
    for (int i = 0; i < types.length; i++)
    {
      types[i] = findNativeType(parameters[i].getType());
    }
    LLVMTypeRef resultType = findNativeType(new NamedType(false, compoundDefinition));

    Pointer paramTypes = C.toNativePointerArray(types, false, true);
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, paramTypes, types.length, false);
    LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);

    int paramCount = LLVM.LLVMCountParams(llvmFunc);
    if (paramCount != parameters.length)
    {
      throw new IllegalStateException("LLVM returned wrong number of parameters");
    }
    for (int i = 0; i < paramCount; i++)
    {
      LLVMValueRef parameter = LLVM.LLVMGetParam(llvmFunc, i);
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

    Parameter[] parameters = method.getParameters();
    LLVMTypeRef[] types;
    if (method.isStatic())
    {
      types = new LLVMTypeRef[parameters.length];
      for (int i = 0; i < parameters.length; ++i)
      {
        types[i] = findNativeType(parameters[i].getType());
      }
    }
    else
    {
      types = new LLVMTypeRef[1 + parameters.length];
      // add the 'this' type to the function
      types[0] = LLVM.LLVMPointerType(findNativeType(new NamedType(false, compoundDefinition)), 0);
      for (int i = 0; i < parameters.length; i++)
      {
        types[i + 1] = findNativeType(parameters[i].getType());
      }
    }
    LLVMTypeRef resultType = findNativeType(method.getReturnType());

    Pointer paramTypes = C.toNativePointerArray(types, false, true);
    LLVMTypeRef functionType = LLVM.LLVMFunctionType(resultType, paramTypes, types.length, false);
    LLVMValueRef llvmFunc = LLVM.LLVMAddFunction(module, mangledName, functionType);
    LLVM.LLVMSetFunctionCallConv(llvmFunc, LLVM.LLVMCallConv.LLVMCCallConv);

    int paramCount = LLVM.LLVMCountParams(llvmFunc);
    if (paramCount != types.length)
    {
      throw new IllegalStateException("LLVM returned wrong number of parameters");
    }
    if (method.isStatic())
    {
      for (int i = 0; i < paramCount; i++)
      {
        LLVMValueRef parameter = LLVM.LLVMGetParam(llvmFunc, i);
        LLVM.LLVMSetValueName(parameter, parameters[i].getName());
      }
    }
    else
    {
      LLVM.LLVMSetValueName(LLVM.LLVMGetParam(llvmFunc, 0), "this");
      for (int i = 1; i < paramCount; i++)
      {
        LLVMValueRef parameter = LLVM.LLVMGetParam(llvmFunc, i);
        LLVM.LLVMSetValueName(parameter, parameters[i - 1].getName());
      }
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

  private LLVMTypeRef findNativeType(Type type)
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
      ArrayType arrayType = (ArrayType) type;
      LLVMTypeRef baseType = findNativeType(arrayType.getBaseType());
      LLVMTypeRef llvmArray = LLVM.LLVMArrayType(baseType, 0);
      LLVMTypeRef[] structureTypes = new LLVMTypeRef[] {LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), llvmArray};
      LLVMTypeRef llvmStructure = LLVM.LLVMStructType(C.toNativePointerArray(structureTypes, false, true), 2, false);
      return LLVM.LLVMPointerType(llvmStructure, 0);
    }
    if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      Type[] subTypes = tupleType.getSubTypes();
      LLVMTypeRef[] llvmSubTypes = new LLVMTypeRef[subTypes.length];
      for (int i = 0; i < subTypes.length; i++)
      {
        llvmSubTypes[i] = findNativeType(subTypes[i]);
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
      CompoundDefinition compound = namedType.getResolvedDefinition();
      Field[] fields = compound.getNonStaticFields();
      LLVMTypeRef[] llvmSubTypes = new LLVMTypeRef[fields.length];
      for (int i = 0; i < fields.length; i++)
      {
        llvmSubTypes[i] = findNativeType(fields[i].getType());
      }
      LLVMTypeRef nonNullableType = LLVM.LLVMStructType(C.toNativePointerArray(llvmSubTypes, false, true), llvmSubTypes.length, false);
      if (namedType.isNullable())
      {
        // tuple the non-nullable type with a boolean, so that we can tell whether or not the value is null
        LLVMTypeRef[] types = new LLVMTypeRef[] {LLVM.LLVMInt1Type(), nonNullableType};
        return LLVM.LLVMStructType(C.toNativePointerArray(types, false, true), types.length, false);
      }
      return nonNullableType;
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

  private void addConstructorBodies(CompoundDefinition compoundDefinition)
  {
    for (Constructor constructor : compoundDefinition.getConstructors())
    {
      LLVMValueRef llvmFunction = getConstructorFunction(constructor);

      LLVMBasicBlockRef block = LLVM.LLVMAppendBasicBlock(llvmFunction, "entry");
      LLVM.LLVMPositionBuilderAtEnd(builder, block);

      // create LLVMValueRefs for all of the variables, including paramters
      Set<Variable> allVariables = Resolver.getAllNestedVariables(constructor.getBlock());
      Map<Variable, LLVMValueRef> variables = new HashMap<Variable, LLVM.LLVMValueRef>();
      for (Variable v : allVariables)
      {
        LLVMValueRef allocaInst = LLVM.LLVMBuildAlloca(builder, findNativeType(v.getType()), v.getName());
        variables.put(v, allocaInst);
      }

      // store the parameter values to the LLVMValueRefs
      for (Parameter p : constructor.getParameters())
      {
        LLVM.LLVMBuildStore(builder, LLVM.LLVMGetParam(llvmFunction, p.getIndex()), variables.get(p.getVariable()));
      }

      final LLVMValueRef thisValue = LLVM.LLVMBuildAlloca(builder, findNativeType(new NamedType(false, compoundDefinition)), "this");

      buildStatement(constructor.getBlock(), VoidType.VOID_TYPE, llvmFunction, thisValue, variables, new HashMap<BreakableStatement, LLVM.LLVMBasicBlockRef>(), new HashMap<BreakableStatement, LLVM.LLVMBasicBlockRef>(), new Runnable()
      {
        @Override
        public void run()
        {
          // this will be called whenever a return void is found
          // so return the created object
          LLVMValueRef result = LLVM.LLVMBuildLoad(builder, thisValue, "");
          LLVM.LLVMBuildRet(builder, result);
        }
      });
      if (!constructor.getBlock().stopsExecution())
      {
        LLVMValueRef result = LLVM.LLVMBuildLoad(builder, thisValue, "");
        LLVM.LLVMBuildRet(builder, result);
      }
    }
  }

  private void addMethodBodies(CompoundDefinition compoundDefinition)
  {
    for (Method method : compoundDefinition.getAllMethods())
    {
      LLVMValueRef llvmFunction = getMethodFunction(method);

      LLVMBasicBlockRef block = LLVM.LLVMAppendBasicBlock(llvmFunction, "entry");
      LLVM.LLVMPositionBuilderAtEnd(builder, block);

      // create LLVMValueRefs for all of the variables, including parameters
      Set<Variable> allVariables = Resolver.getAllNestedVariables(method.getBlock());
      Map<Variable, LLVMValueRef> variables = new HashMap<Variable, LLVM.LLVMValueRef>();
      for (Variable v : allVariables)
      {
        LLVMValueRef allocaInst = LLVM.LLVMBuildAlloca(builder, findNativeType(v.getType()), v.getName());
        variables.put(v, allocaInst);
      }

      // store the parameter values to the LLVMValueRefs
      for (Parameter p : method.getParameters())
      {
        LLVM.LLVMBuildStore(builder, LLVM.LLVMGetParam(llvmFunction, p.getIndex() + (method.isStatic() ? 0 : 1)), variables.get(p.getVariable()));
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
          }
          else if (resolvedVariable instanceof GlobalVariable)
          {
            llvmAssigneePointers[i] = getGlobal((GlobalVariable) resolvedVariable);
          }
          else
          {
            llvmAssigneePointers[i] = variables.get(((VariableAssignee) assignees[i]).getResolvedVariable());
          }
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          LLVMValueRef array = buildExpression(arrayElementAssignee.getArrayExpression(), llvmFunction, thisValue, variables);
          LLVMValueRef dimension = buildExpression(arrayElementAssignee.getDimensionExpression(), llvmFunction, thisValue, variables);
          LLVMValueRef convertedDimension = convertType(dimension, arrayElementAssignee.getDimensionExpression().getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, llvmFunction);
          LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false),
                                                       convertedDimension};
          llvmAssigneePointers[i] = LLVM.LLVMBuildGEP(builder, array, C.toNativePointerArray(indices, false, true), indices.length, "");
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
            }
            else
            {
              LLVMValueRef expressionValue = buildExpression(fieldAccessExpression.getBaseExpression(), llvmFunction, thisValue, variables);
              LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                           LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), field.getMemberIndex(), false)};
              llvmAssigneePointers[i] = LLVM.LLVMBuildGEP(builder, expressionValue, C.toNativePointerArray(indices, false, true), indices.length, "");
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
        }
        else
        {
          throw new IllegalStateException("Unknown Assignee type: " + assignees[i]);
        }
      }

      if (assignStatement.getExpression() != null)
      {
        LLVMValueRef value = buildExpression(assignStatement.getExpression(), llvmFunction, thisValue, variables);
        if (llvmAssigneePointers.length == 1)
        {
          if (llvmAssigneePointers[0] != null)
          {
            LLVMValueRef convertedValue = convertType(value, assignStatement.getExpression().getType(), assignees[0].getResolvedType(), llvmFunction);
            Type type = assignees[0].getResolvedType();
            if (type instanceof NamedType) // TODO: when this does not cause a warning, add it: && ((NamedType) type).getResolvedDefinition() instanceof CompoundDefinition)
            {
              // for compound types, we need to load from the result of the expression and store a copy in the new pointer
              convertedValue = LLVM.LLVMBuildLoad(builder, convertedValue, "");
            }
            LLVM.LLVMBuildStore(builder, convertedValue, llvmAssigneePointers[0]);
          }
        }
        else
        {
          Type[] expressionSubTypes = ((TupleType) assignStatement.getExpression().getType()).getSubTypes();
          for (int i = 0; i < llvmAssigneePointers.length; i++)
          {
            if (llvmAssigneePointers[i] != null)
            {
              LLVMValueRef extracted = LLVM.LLVMBuildExtractValue(builder, value, i, "");
              LLVMValueRef convertedValue = convertType(extracted, expressionSubTypes[i], assignees[i].getResolvedType(), llvmFunction);
              // since we are extracting from a tuple here, we do not need to treat compound types differently
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
      if (assignee instanceof VariableAssignee)
      {
        pointer = variables.get(((VariableAssignee) assignee).getResolvedVariable());
      }
      else if (assignee instanceof ArrayElementAssignee)
      {
        ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
        LLVMValueRef array = buildExpression(arrayElementAssignee.getArrayExpression(), llvmFunction, thisValue, variables);
        LLVMValueRef dimension = buildExpression(arrayElementAssignee.getDimensionExpression(), llvmFunction, thisValue, variables);
        LLVMValueRef convertedDimension = convertType(dimension, arrayElementAssignee.getDimensionExpression().getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, llvmFunction);
        LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                     LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false),
                                                     convertedDimension};
        pointer = LLVM.LLVMBuildGEP(builder, array, C.toNativePointerArray(indices, false, true), indices.length, "");
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
        LLVMValueRef one = LLVM.LLVMConstReal(findNativeType(type), 1);
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
        LLVMValueRef one = LLVM.LLVMConstInt(findNativeType(type), 1, false);
        if (prefixIncDecStatement.isIncrement())
        {
          result = LLVM.LLVMBuildAdd(builder, loaded, one, "");
        }
        else
        {
          result = LLVM.LLVMBuildSub(builder, loaded, one, "");
        }
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
        LLVMValueRef convertedValue = convertType(value, returnedExpression.getType(), returnType, llvmFunction);
        if (returnType instanceof NamedType) // TODO: when this does not cause a warning, add it: && ((NamedType) returnType).getResolvedDefinition() instanceof CompoundDefinition)
        {
          // for compound types, we need to load from the result of the expression and return that value
          convertedValue = LLVM.LLVMBuildLoad(builder, convertedValue, "");
        }
        LLVM.LLVMBuildRet(builder, convertedValue);
      }
    }
    else if (statement instanceof ShorthandAssignStatement)
    {
      ShorthandAssignStatement shorthandAssignStatement = (ShorthandAssignStatement) statement;
      Assignee[] assignees = shorthandAssignStatement.getAssignees();
      LLVMValueRef[] llvmAssigneePointers = new LLVMValueRef[assignees.length];
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
          }
          else if (resolvedVariable instanceof GlobalVariable)
          {
            llvmAssigneePointers[i] = getGlobal((GlobalVariable) resolvedVariable);
          }
          else
          {
            llvmAssigneePointers[i] = variables.get(((VariableAssignee) assignees[i]).getResolvedVariable());
          }
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          LLVMValueRef array = buildExpression(arrayElementAssignee.getArrayExpression(), llvmFunction, thisValue, variables);
          LLVMValueRef dimension = buildExpression(arrayElementAssignee.getDimensionExpression(), llvmFunction, thisValue, variables);
          LLVMValueRef convertedDimension = convertType(dimension, arrayElementAssignee.getDimensionExpression().getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, llvmFunction);
          LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                       LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false),
                                                       convertedDimension};
          llvmAssigneePointers[i] = LLVM.LLVMBuildGEP(builder, array, C.toNativePointerArray(indices, false, true), indices.length, "");
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
            }
            else
            {
              LLVMValueRef expressionValue = buildExpression(fieldAccessExpression.getBaseExpression(), llvmFunction, thisValue, variables);
              LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                           LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), field.getMemberIndex(), false)};
              llvmAssigneePointers[i] = LLVM.LLVMBuildGEP(builder, expressionValue, C.toNativePointerArray(indices, false, true), indices.length, "");
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
      if (resultType instanceof TupleType && ((TupleType) resultType).getSubTypes().length == assignees.length)
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
        LLVMValueRef rightValue = convertType(resultValues[i], resultValueTypes[i], assignees[i].getResolvedType(), llvmFunction);
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

  private LLVMValueRef convertType(LLVMValueRef value, Type from, Type to, LLVMValueRef llvmFunction)
  {
    if (from.isEquivalent(to))
    {
      return value;
    }
    if (from instanceof PrimitiveType && to instanceof PrimitiveType)
    {
      return convertPrimitiveType(value, (PrimitiveType) from, (PrimitiveType) to);
    }
    if (from instanceof ArrayType && to instanceof ArrayType)
    {
      // array casts are illegal unless from and to types are the same, so they must have the same type
      // nullability will be checked by the type checker, but has no effect on the native type, so we do not need to do anything special here

      // if from and to are nullable and value is null, then the value we are returning here is undefined
      // TODO: if from and to are nullable and value is null, throw an exception here instead of having undefined behaviour
      return value;
    }
    if (from instanceof NamedType && to instanceof NamedType) // TODO: when it doesn't cause a warning, add: &&
        //((NamedType) from).getResolvedDefinition() instanceof CompoundDefinition &&
        //((NamedType) to).getResolvedDefinition() instanceof CompoundDefinition)
    {
      // compound type casts are illegal unless from and to types are the same, so they must have the same type
      LLVMValueRef loadedValue = LLVM.LLVMBuildLoad(builder, value, "");
      LLVMValueRef isNotNullValue = null;
      LLVMValueRef namedValue = loadedValue;
      if (from.isNullable())
      {
        isNotNullValue = LLVM.LLVMBuildExtractValue(builder, loadedValue, 0, "");
        namedValue = LLVM.LLVMBuildExtractValue(builder, loadedValue, 1, "");
      }
      LLVMValueRef result;
      if (to.isNullable())
      {
        result = LLVM.LLVMGetUndef(findNativeType(to));
        if (from.isNullable())
        {
          result = LLVM.LLVMBuildInsertValue(builder, result, isNotNullValue, 0, "");
        }
        else
        {
          // set the flag to one to indicate that this value is not null
          result = LLVM.LLVMBuildInsertValue(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), 0, "");
        }
        result = LLVM.LLVMBuildInsertValue(builder, result, namedValue, 1, "");
      }
      else
      {
        // return the primitive value directly, since the to type is not nullable
        // if from is nullable and value is null, then the value we are returning here is undefined
        // TODO: if from is nullable and value is null, throw an exception here instead of having undefined behaviour
        result = namedValue;
      }
      // for compound types, we need to return a pointer to the value
      // so build an alloca in the entry block
      LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMPositionBuilderBefore(builder, LLVM.LLVMGetFirstInstruction(LLVM.LLVMGetEntryBasicBlock(llvmFunction)));
      LLVMValueRef alloca = LLVM.LLVMBuildAlloca(builder, findNativeType(to), "");
      LLVM.LLVMPositionBuilderAtEnd(builder, currentBlock);
      LLVM.LLVMBuildStore(builder, result, alloca);
      return alloca;
    }
    if (from instanceof TupleType && !(to instanceof TupleType))
    {
      TupleType fromTuple = (TupleType) from;
      if (fromTuple.getSubTypes().length != 1)
      {
        throw new IllegalArgumentException("Cannot convert from a " + from + " to a " + to);
      }
      if (from.isNullable())
      {
        // extract the value of the tuple from the nullable structure
        // if from is nullable and value is null, then the value we are using here is undefined
        // TODO: if from is nullable and value is null, throw an exception here instead of having undefined behaviour
        value = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
      }
      return LLVM.LLVMBuildExtractValue(builder, value, 0, "");
    }
    if (!(from instanceof TupleType) && to instanceof TupleType)
    {
      TupleType toTuple = (TupleType) to;
      if (toTuple.getSubTypes().length != 1)
      {
        throw new IllegalArgumentException("Cannot convert from a " + from + " to a " + to);
      }
      LLVMValueRef tupledValue = LLVM.LLVMGetUndef(findNativeType(new TupleType(false, toTuple.getSubTypes(), null)));
      tupledValue = LLVM.LLVMBuildInsertValue(builder, tupledValue, value, 0, "");
      if (to.isNullable())
      {
        LLVMValueRef result = LLVM.LLVMGetUndef(findNativeType(to));
        result = LLVM.LLVMBuildInsertValue(builder, result, LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), 1, false), 0, "");
        return LLVM.LLVMBuildInsertValue(builder, result, tupledValue, 1, "");
      }
      return tupledValue;
    }
    if (from instanceof TupleType && to instanceof TupleType)
    {
      TupleType fromTuple = (TupleType) from;
      TupleType toTuple = (TupleType) to;
      if (fromTuple.isEquivalent(toTuple))
      {
        return value;
      }
      Type[] fromSubTypes = fromTuple.getSubTypes();
      Type[] toSubTypes = toTuple.getSubTypes();
      if (fromSubTypes.length != toSubTypes.length)
      {
        throw new IllegalArgumentException("Cannot convert from a " + from + " to a " + to);
      }
      boolean subTypesEquivalent = true;
      for (int i = 0; i < fromSubTypes.length; ++i)
      {
        if (!fromSubTypes[i].isEquivalent(toSubTypes[i]))
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
          // if from is nullable and value is null, then the value we are using here is undefined
          // TODO: if from is nullable and value is null, throw an exception here instead of having undefined behaviour
          return LLVM.LLVMBuildExtractValue(builder, value, 1, "");
        }
        if (!from.isNullable() && to.isNullable())
        {
          LLVMValueRef result = LLVM.LLVMGetUndef(findNativeType(to));
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

      LLVMValueRef currentValue = LLVM.LLVMGetUndef(findNativeType(toTuple));
      for (int i = 0; i < fromTuple.getSubTypes().length; i++)
      {
        LLVMValueRef current = LLVM.LLVMBuildExtractValue(builder, tupleValue, i, "");
        LLVMValueRef converted = convertType(current, fromSubTypes[i], toSubTypes[i], llvmFunction);
        currentValue = LLVM.LLVMBuildInsertValue(builder, currentValue, converted, i, "");
      }

      if (to.isNullable())
      {
        LLVMValueRef result = LLVM.LLVMGetUndef(findNativeType(to));
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
      // return the primitive value directly, since the to type is not nullable
      // if from is nullable and value is null, then the value we are using here is undefined
      // TODO: if from is nullable and value is null, throw an exception here instead of having undefined behaviour
      return currentValue;
    }
    throw new IllegalArgumentException("Unknown type conversion, from '" + from + "' to '" + to + "'");
  }

  private LLVMValueRef convertPrimitiveType(LLVMValueRef value, PrimitiveType from, PrimitiveType to)
  {
    PrimitiveTypeType fromType = from.getPrimitiveTypeType();
    PrimitiveTypeType toType = to.getPrimitiveTypeType();
    if (fromType == toType && from.isNullable() == to.isNullable())
    {
      return value;
    }
    LLVMValueRef isNotNullValue = null;
    LLVMValueRef primitiveValue = value;
    if (from.isNullable())
    {
      isNotNullValue = LLVM.LLVMBuildExtractValue(builder, value, 0, "");
      primitiveValue = LLVM.LLVMBuildExtractValue(builder, value, 1, "");
    }
    // perform the conversion
    LLVMTypeRef toNativeType = findNativeType(new PrimitiveType(false, toType, null));
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
      LLVMValueRef result = LLVM.LLVMGetUndef(findNativeType(to));
      if (from.isNullable())
      {
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
    // if from was null, then the value we are returning here is undefined
    // TODO: if from was null, throw an exception here instead of having undefined behaviour
    return primitiveValue;
  }

  private int getPredicate(ComparisonOperator operator, boolean floating, boolean signed)
  {
    if (floating)
    {
      switch (operator)
      {
      case EQUAL:
        return LLVM.LLVMRealPredicate.LLVMRealOEQ;
      case LESS_THAN:
        return LLVM.LLVMRealPredicate.LLVMRealOLT;
      case LESS_THAN_EQUAL:
        return LLVM.LLVMRealPredicate.LLVMRealOLE;
      case MORE_THAN:
        return LLVM.LLVMRealPredicate.LLVMRealOGT;
      case MORE_THAN_EQUAL:
        return LLVM.LLVMRealPredicate.LLVMRealOGE;
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
      case LESS_THAN:
        return signed ? LLVM.LLVMIntPredicate.LLVMIntSLT : LLVM.LLVMIntPredicate.LLVMIntULT;
      case LESS_THAN_EQUAL:
        return signed ? LLVM.LLVMIntPredicate.LLVMIntSLE : LLVM.LLVMIntPredicate.LLVMIntULE;
      case MORE_THAN:
        return signed ? LLVM.LLVMIntPredicate.LLVMIntSGT : LLVM.LLVMIntPredicate.LLVMIntUGT;
      case MORE_THAN_EQUAL:
        return signed ? LLVM.LLVMIntPredicate.LLVMIntSGE : LLVM.LLVMIntPredicate.LLVMIntUGE;
      case NOT_EQUAL:
        return LLVM.LLVMIntPredicate.LLVMIntNE;
      }
    }
    throw new IllegalArgumentException("Unknown predicate '" + operator + "'");
  }

  private LLVMValueRef buildArrayCreation(LLVMValueRef llvmFunction, LLVMValueRef[] llvmLengths, ArrayType type)
  {
    LLVMTypeRef llvmArrayType = findNativeType(type);
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
      left = convertPrimitiveType(left, leftType, resultType);
      right = convertPrimitiveType(right, rightType, resultType);
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
      LLVMValueRef convertedDimensionValue = convertType(dimensionValue, arrayAccessExpression.getDimensionExpression().getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, llvmFunction);
      LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                                     LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 1, false),
                                                                     convertedDimensionValue};
      LLVMValueRef elementPointer = LLVM.LLVMBuildGEP(builder, arrayValue, C.toNativePointerArray(indices, false, true), indices.length, "");
      ArrayType arrayType = (ArrayType) arrayAccessExpression.getArrayExpression().getType();
      if (arrayType.getBaseType() instanceof NamedType) // TODO (when it doesn't cause a warning): && ((NamedType) arrayAccessExpression.getType()).getResolvedDefinition() instanceof CompoundDefinition)
      {
        // for compound types, we do not need to load anything here
        return convertType(elementPointer, arrayType.getBaseType(), arrayAccessExpression.getType(), llvmFunction);
      }
      LLVMValueRef value = LLVM.LLVMBuildLoad(builder, elementPointer, "");
      return convertType(value, arrayType.getBaseType(), arrayAccessExpression.getType(), llvmFunction);
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
          LLVMValueRef convertedValue = convertType(expressionValue, valueExpressions[i].getType(), type.getBaseType(), llvmFunction);
          Type valueType = valueExpressions[i].getType();
          if (valueType instanceof NamedType) // TODO: when it doesn't cause a warning, add: && ((NamedType) valueType).getResolvedDefinition() instanceof CompoundDefinition)
          {
            convertedValue = LLVM.LLVMBuildLoad(builder, convertedValue, "");
          }
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
        llvmLengths[i] = convertType(expressionValue, dimensionExpressions[i].getType(), ArrayLengthMember.ARRAY_LENGTH_TYPE, llvmFunction);
      }
      return buildArrayCreation(llvmFunction, llvmLengths, type);
    }
    if (expression instanceof BitwiseNotExpression)
    {
      LLVMValueRef value = buildExpression(((BitwiseNotExpression) expression).getExpression(), llvmFunction, thisValue, variables);
      return LLVM.LLVMBuildNot(builder, value, "");
    }
    if (expression instanceof BooleanLiteralExpression)
    {
      return LLVM.LLVMConstInt(LLVM.LLVMInt1Type(), ((BooleanLiteralExpression) expression).getValue() ? 1 : 0, false);
    }
    if (expression instanceof BooleanNotExpression)
    {
      LLVMValueRef value = buildExpression(((BooleanNotExpression) expression).getExpression(), llvmFunction, thisValue, variables);
      return LLVM.LLVMBuildNot(builder, value, "");
    }
    if (expression instanceof BracketedExpression)
    {
      BracketedExpression bracketedExpression = (BracketedExpression) expression;
      LLVMValueRef value = buildExpression(bracketedExpression.getExpression(), llvmFunction, thisValue, variables);
      return convertType(value, bracketedExpression.getExpression().getType(), expression.getType(), llvmFunction);
    }
    if (expression instanceof CastExpression)
    {
      CastExpression castExpression = (CastExpression) expression;
      LLVMValueRef value = buildExpression(castExpression.getExpression(), llvmFunction, thisValue, variables);
      return convertType(value, castExpression.getExpression().getType(), castExpression.getType(), llvmFunction);
    }
    if (expression instanceof ComparisonExpression)
    {
      ComparisonExpression comparisonExpression = (ComparisonExpression) expression;
      LLVMValueRef left = buildExpression(comparisonExpression.getLeftSubExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef right = buildExpression(comparisonExpression.getRightSubExpression(), llvmFunction, thisValue, variables);
      PrimitiveType leftType = (PrimitiveType) comparisonExpression.getLeftSubExpression().getType();
      PrimitiveType rightType = (PrimitiveType) comparisonExpression.getRightSubExpression().getType();
      // cast if necessary
      PrimitiveType resultType = comparisonExpression.getComparisonType();
      if (resultType == null)
      {
        PrimitiveTypeType leftTypeType = leftType.getPrimitiveTypeType();
        PrimitiveTypeType rightTypeType = rightType.getPrimitiveTypeType();
        if (!leftTypeType.isFloating() && !rightTypeType.isFloating() &&
            leftTypeType.getBitCount() == rightTypeType.getBitCount() &&
            leftTypeType.isSigned() != rightTypeType.isSigned())
        {
          // compare the signed and non-signed integers as (bitCount + 1) bit numbers, since they will not fit in bitCount bits
          LLVMTypeRef comparisonType = LLVM.LLVMIntType(leftType.getPrimitiveTypeType().getBitCount() + 1);
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
          return LLVM.LLVMBuildICmp(builder, getPredicate(comparisonExpression.getOperator(), false, true), left, right, "");
        }
        throw new IllegalArgumentException("Unknown result type, unable to generate comparison expression: " + expression);
      }
      left = convertPrimitiveType(left, leftType, resultType);
      right = convertPrimitiveType(right, rightType, resultType);
      if (resultType.getPrimitiveTypeType().isFloating())
      {
        return LLVM.LLVMBuildFCmp(builder, getPredicate(comparisonExpression.getOperator(), true, true), left, right, "");
      }
      return LLVM.LLVMBuildICmp(builder, getPredicate(comparisonExpression.getOperator(), false, resultType.getPrimitiveTypeType().isSigned()), left, right, "");
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
          if (field.getType() instanceof NamedType) // TODO (when it doesn't cause a warning): && ((NamedType) field.getType()).getResolvedDefinition() instanceof CompoundDefinition)
          {
            // for compound types, we do not need to load anything here
            return global;
          }
          return LLVM.LLVMBuildLoad(builder, global, "");
        }

        LLVMValueRef baseValue = buildExpression(fieldAccessExpression.getBaseExpression(), llvmFunction, thisValue, variables);
        LLVMValueRef[] indices = new LLVMValueRef[] {LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), 0, false),
                                                     LLVM.LLVMConstInt(LLVM.LLVMIntType(PrimitiveTypeType.UINT.getBitCount()), field.getMemberIndex(), false)};
        LLVMValueRef elementPointer = LLVM.LLVMBuildGEP(builder, baseValue, C.toNativePointerArray(indices, false, true), indices.length, "");
        if (field.getType() instanceof NamedType) // TODO (when it doesn't cause a warning): && ((NamedType) field.getType()).getResolvedDefinition() instanceof CompoundDefinition)
        {
          // for compound types, we do not need to load anything here
          return elementPointer;
        }
        return LLVM.LLVMBuildLoad(builder, elementPointer, "");
      }
    }
    if (expression instanceof FloatingLiteralExpression)
    {
      double value = Double.parseDouble(((FloatingLiteralExpression) expression).getLiteral().toString());
      return LLVM.LLVMConstReal(findNativeType(expression.getType()), value);
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
        returnType = new NamedType(false, resolvedConstructor.getContainingDefinition());
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
        llvmResolvedFunction = getMethodFunction(resolvedMethod);
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
        values[i] = convertType(arg, arguments[i].getType(), parameterTypes[i], llvmFunction);
        if (parameterTypes[i] instanceof NamedType) // TODO: when it doesn't cause a warning, add: && ((NamedType) parameterTypes[i]).getResolvedDefinition() instanceof CompoundDefinition)
        {
          // for compound types, we need to pass the value itself, not the pointer to the value
          values[i] = LLVM.LLVMBuildLoad(builder, values[i], "");
        }
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

      if (returnType instanceof NamedType) // TODO (when it doesn't cause a warning): && ((NamedType) returnType).getResolvedDefinition() instanceof CompoundDefinition)
      {
        // for compound types, we need to get a pointer from this returned value
        // so build an alloca in the entry block
        LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMPositionBuilderBefore(builder, LLVM.LLVMGetFirstInstruction(LLVM.LLVMGetEntryBasicBlock(llvmFunction)));
        LLVMValueRef alloca = LLVM.LLVMBuildAlloca(builder, findNativeType(returnType), "");
        LLVM.LLVMPositionBuilderAtEnd(builder, currentBlock);
        LLVM.LLVMBuildStore(builder, result, alloca);
        return alloca;
      }
      return result;
    }
    if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIf = (InlineIfExpression) expression;
      LLVMValueRef conditionValue = buildExpression(inlineIf.getCondition(), llvmFunction, thisValue, variables);
      LLVMBasicBlockRef thenBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "inlineIfThen");
      LLVMBasicBlockRef elseBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "inlineIfElse");
      LLVMBasicBlockRef continuationBlock = LLVM.LLVMAppendBasicBlock(llvmFunction, "afterInlineIf");

      LLVM.LLVMBuildCondBr(builder, conditionValue, thenBlock, elseBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, thenBlock);
      LLVMValueRef thenValue = buildExpression(inlineIf.getThenExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef convertedThenValue = convertType(thenValue, inlineIf.getThenExpression().getType(), inlineIf.getType(), llvmFunction);
      LLVMBasicBlockRef thenBranchBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, elseBlock);
      LLVMValueRef elseValue = buildExpression(inlineIf.getElseExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef convertedElseValue = convertType(elseValue, inlineIf.getElseExpression().getType(), inlineIf.getType(), llvmFunction);
      LLVMBasicBlockRef elseBranchBlock = LLVM.LLVMGetInsertBlock(builder);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
      LLVMValueRef result = LLVM.LLVMBuildPhi(builder, findNativeType(inlineIf.getType()), "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {convertedThenValue, convertedElseValue};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {thenBranchBlock, elseBranchBlock};
      LLVM.LLVMAddIncoming(result, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), 2);
      return result;
    }
    if (expression instanceof IntegerLiteralExpression)
    {
      int n = ((IntegerLiteralExpression) expression).getLiteral().getValue().intValue();
      return LLVM.LLVMConstInt(findNativeType(expression.getType()), n, false);
    }
    if (expression instanceof LogicalExpression)
    {
      LogicalExpression logicalExpression = (LogicalExpression) expression;
      LLVMValueRef left = buildExpression(logicalExpression.getLeftSubExpression(), llvmFunction, thisValue, variables);
      PrimitiveType leftType = (PrimitiveType) logicalExpression.getLeftSubExpression().getType();
      PrimitiveType rightType = (PrimitiveType) logicalExpression.getRightSubExpression().getType();
      // cast if necessary
      PrimitiveType resultType = (PrimitiveType) logicalExpression.getType();
      left = convertPrimitiveType(left, leftType, resultType);
      LogicalOperator operator = logicalExpression.getOperator();
      if (operator != LogicalOperator.SHORT_CIRCUIT_AND && operator != LogicalOperator.SHORT_CIRCUIT_OR)
      {
        LLVMValueRef right = buildExpression(logicalExpression.getRightSubExpression(), llvmFunction, thisValue, variables);
        right = convertPrimitiveType(right, rightType, resultType);
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
      right = convertPrimitiveType(right, rightType, resultType);
      LLVM.LLVMBuildBr(builder, continuationBlock);

      LLVM.LLVMPositionBuilderAtEnd(builder, continuationBlock);
      // create a phi node for the result, and return it
      LLVMValueRef phi = LLVM.LLVMBuildPhi(builder, findNativeType(resultType), "");
      LLVMValueRef[] incomingValues = new LLVMValueRef[] {left, right};
      LLVMBasicBlockRef[] incomingBlocks = new LLVMBasicBlockRef[] {currentBlock, rightCheckBlock};
      LLVM.LLVMAddIncoming(phi, C.toNativePointerArray(incomingValues, false, true), C.toNativePointerArray(incomingBlocks, false, true), 2);
      return phi;
    }
    if (expression instanceof MinusExpression)
    {
      MinusExpression minusExpression = (MinusExpression) expression;
      LLVMValueRef value = buildExpression(minusExpression.getExpression(), llvmFunction, thisValue, variables);
      value = convertPrimitiveType(value, (PrimitiveType) minusExpression.getExpression().getType(), (PrimitiveType) minusExpression.getType());
      PrimitiveTypeType primitiveTypeType = ((PrimitiveType) minusExpression.getType()).getPrimitiveTypeType();
      if (primitiveTypeType.isFloating())
      {
        return LLVM.LLVMBuildFNeg(builder, value, "");
      }
      return LLVM.LLVMBuildNeg(builder, value, "");
    }
    if (expression instanceof NullLiteralExpression)
    {
      Type type = expression.getType();
      LLVMValueRef value = LLVM.LLVMConstNull(findNativeType(type));
      if (type instanceof NamedType) // TODO (when it doesn't cause a warning): && ((NamedType) type).getResolvedDefinition() instanceof CompoundDefinition)
      {
        // for compound types, we need to get a pointer from this null value
        // so build an alloca in the entry block
        LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMPositionBuilderBefore(builder, LLVM.LLVMGetFirstInstruction(LLVM.LLVMGetEntryBasicBlock(llvmFunction)));
        LLVMValueRef alloca = LLVM.LLVMBuildAlloca(builder, findNativeType(type), "");
        LLVM.LLVMPositionBuilderAtEnd(builder, currentBlock);
        LLVM.LLVMBuildStore(builder, value, alloca);
        return alloca;
      }
      return value;
    }
    if (expression instanceof ShiftExpression)
    {
      ShiftExpression shiftExpression = (ShiftExpression) expression;
      LLVMValueRef leftValue = buildExpression(shiftExpression.getLeftExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef rightValue = buildExpression(shiftExpression.getRightExpression(), llvmFunction, thisValue, variables);
      LLVMValueRef convertedLeft = convertType(leftValue, shiftExpression.getLeftExpression().getType(), shiftExpression.getType(), llvmFunction);
      LLVMValueRef convertedRight = convertType(rightValue, shiftExpression.getRightExpression().getType(), shiftExpression.getType(), llvmFunction);
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
      return thisValue;
    }
    if (expression instanceof TupleExpression)
    {
      TupleExpression tupleExpression = (TupleExpression) expression;
      Type[] tupleTypes = ((TupleType) tupleExpression.getType()).getSubTypes();
      Expression[] subExpressions = tupleExpression.getSubExpressions();
      LLVMValueRef currentValue = LLVM.LLVMGetUndef(findNativeType(tupleExpression.getType()));
      for (int i = 0; i < subExpressions.length; i++)
      {
        LLVMValueRef value = buildExpression(subExpressions[i], llvmFunction, thisValue, variables);
        Type type = tupleTypes[i];
        value = convertType(value, subExpressions[i].getType(), type, llvmFunction);
        if (type instanceof NamedType) // TODO: when this does not cause a warning, add it: && ((NamedType) type).getResolvedDefinition() instanceof CompoundDefinition)
        {
          // for compound types, we need to load from the result of the expression before storing the result in the tuple
          value = LLVM.LLVMBuildLoad(builder, value, "");
        }
        currentValue = LLVM.LLVMBuildInsertValue(builder, currentValue, value, i, "");
      }
      return currentValue;
    }
    if (expression instanceof TupleIndexExpression)
    {
      TupleIndexExpression tupleIndexExpression = (TupleIndexExpression) expression;
      TupleType tupleType = (TupleType) tupleIndexExpression.getExpression().getType();
      LLVMValueRef result = buildExpression(tupleIndexExpression.getExpression(), llvmFunction, thisValue, variables);
      // convert the 1-based indexing to 0-based before extracting the value
      int index = tupleIndexExpression.getIndexLiteral().getValue().intValue() - 1;
      LLVMValueRef value = LLVM.LLVMBuildExtractValue(builder, result, index, "");
      LLVMValueRef convertedValue = convertType(value, tupleType.getSubTypes()[index], tupleIndexExpression.getType(), llvmFunction);

      Type type = tupleIndexExpression.getType();
      if (type instanceof NamedType) // TODO: when the doesn't cause a warning, add it: && ((NamedType) type).getResolvedDefinition() instanceof CompoundDefinition)
      {
        // for compound types, we need to get a pointer to the extracted value
        // so build an alloca in the entry block
        LLVMBasicBlockRef currentBlock = LLVM.LLVMGetInsertBlock(builder);
        LLVM.LLVMPositionBuilderBefore(builder, LLVM.LLVMGetFirstInstruction(LLVM.LLVMGetEntryBasicBlock(llvmFunction)));
        LLVMValueRef alloca = LLVM.LLVMBuildAlloca(builder, findNativeType(type), "");
        LLVM.LLVMPositionBuilderAtEnd(builder, currentBlock);
        LLVM.LLVMBuildStore(builder, convertedValue, alloca);
        return alloca;
      }
      return convertedValue;
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
        if (field.getType() instanceof NamedType) // TODO (when it doesn't cause a warning): && ((NamedType) field.getType()).getResolvedDefinition() instanceof CompoundDefinition)
        {
          // for compound types, we do not need to load anything here
          return elementPointer;
        }
        return LLVM.LLVMBuildLoad(builder, elementPointer, "");
      }
      if (variable instanceof GlobalVariable)
      {
        LLVMValueRef global = getGlobal((GlobalVariable) variable);
        if (expression.getType() instanceof NamedType) // TODO (when it doesn't cause a warning): && ((NamedType) field.getType()).getResolvedDefinition() instanceof CompoundDefinition)
        {
          // for compound types, we do not need to load anything here
          return global;
        }
        return LLVM.LLVMBuildLoad(builder, global, "");
      }
      LLVMValueRef value = variables.get(variable);
      if (value == null)
      {
        throw new IllegalStateException("Missing LLVMValueRef in variable Map: " + ((VariableExpression) expression).getName());
      }
      if (variable.getType() instanceof NamedType) // TODO (when it doesn't cause a warning): && ((NamedType) variable.getType()).getResolvedDefinition() instanceof CompoundDefinition)
      {
        // for compound types, we do not need to load anything here
        return value;
      }
      return LLVM.LLVMBuildLoad(builder, value, "");
    }
    throw new IllegalArgumentException("Unknown Expression type: " + expression);
  }
}
