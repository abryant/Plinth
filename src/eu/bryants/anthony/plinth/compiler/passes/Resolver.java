package eu.bryants.anthony.plinth.compiler.passes;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompilationUnit;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.expression.ArithmeticExpression;
import eu.bryants.anthony.plinth.ast.expression.ArrayAccessExpression;
import eu.bryants.anthony.plinth.ast.expression.ArrayCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.BitwiseNotExpression;
import eu.bryants.anthony.plinth.ast.expression.BooleanLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.BooleanNotExpression;
import eu.bryants.anthony.plinth.ast.expression.BracketedExpression;
import eu.bryants.anthony.plinth.ast.expression.CastExpression;
import eu.bryants.anthony.plinth.ast.expression.ClassCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.EqualityExpression;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.plinth.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.plinth.ast.expression.InlineIfExpression;
import eu.bryants.anthony.plinth.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.LogicalExpression;
import eu.bryants.anthony.plinth.ast.expression.MinusExpression;
import eu.bryants.anthony.plinth.ast.expression.NullCoalescingExpression;
import eu.bryants.anthony.plinth.ast.expression.NullLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.ObjectCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression;
import eu.bryants.anthony.plinth.ast.expression.ShiftExpression;
import eu.bryants.anthony.plinth.ast.expression.StringLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.ThisExpression;
import eu.bryants.anthony.plinth.ast.expression.TupleExpression;
import eu.bryants.anthony.plinth.ast.expression.TupleIndexExpression;
import eu.bryants.anthony.plinth.ast.expression.VariableExpression;
import eu.bryants.anthony.plinth.ast.member.ArrayLengthMember;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.member.Initialiser;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.metadata.FieldInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.PackageNode;
import eu.bryants.anthony.plinth.ast.metadata.Variable;
import eu.bryants.anthony.plinth.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.plinth.ast.misc.Assignee;
import eu.bryants.anthony.plinth.ast.misc.BlankAssignee;
import eu.bryants.anthony.plinth.ast.misc.FieldAssignee;
import eu.bryants.anthony.plinth.ast.misc.Import;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.misc.VariableAssignee;
import eu.bryants.anthony.plinth.ast.statement.AssignStatement;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.statement.BreakStatement;
import eu.bryants.anthony.plinth.ast.statement.ContinueStatement;
import eu.bryants.anthony.plinth.ast.statement.DelegateConstructorStatement;
import eu.bryants.anthony.plinth.ast.statement.ExpressionStatement;
import eu.bryants.anthony.plinth.ast.statement.ForStatement;
import eu.bryants.anthony.plinth.ast.statement.IfStatement;
import eu.bryants.anthony.plinth.ast.statement.PrefixIncDecStatement;
import eu.bryants.anthony.plinth.ast.statement.ReturnStatement;
import eu.bryants.anthony.plinth.ast.statement.ShorthandAssignStatement;
import eu.bryants.anthony.plinth.ast.statement.Statement;
import eu.bryants.anthony.plinth.ast.statement.WhileStatement;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.NullType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.compiler.ConceptualException;
import eu.bryants.anthony.plinth.compiler.NameNotResolvedException;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Resolver
{
  private PackageNode rootPackage;

  public Resolver(PackageNode rootPackage)
  {
    this.rootPackage = rootPackage;
  }

  /**
   * Resolves the specified compilation unit's declared package, and the type definitions it makes to that package.
   * @param compilationUnit - the compilation unit to resolve
   * @throws ConceptualException - if there is a problem adding something to a package (e.g. a name conflict)
   */
  public void resolvePackages(CompilationUnit compilationUnit) throws ConceptualException
  {
    // find the package for this compilation unit
    PackageNode compilationUnitPackage = rootPackage;
    if (compilationUnit.getDeclaredPackage() != null)
    {
      compilationUnitPackage = rootPackage.addPackageTree(compilationUnit.getDeclaredPackage());
    }
    compilationUnit.setResolvedPackage(compilationUnitPackage);

    // add all of the type definitions in this compilation unit to the file's package
    for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
    {
      compilationUnitPackage.addTypeDefinition(typeDefinition);
    }
  }

  /**
   * Resolves the special types that are required for every program, such as string.
   * @throws ConceptualException - if there is a conceptual problem while resolving the names
   * @throws NameNotResolvedException - if a name could not be resolved
   */
  public void resolveSpecialTypes() throws NameNotResolvedException, ConceptualException
  {
    resolve(SpecialTypeHandler.STRING_TYPE, null);
  }

  /**
   * Resolves all of the imports in the specified CompilationUnit
   * @param compilationUnit - the CompilationUnit to resolve the imports of
   * @throws NameNotResolvedException - if a name could not be resolved
   * @throws ConceptualException - if there is a conceptual problem while resolving the names
   */
  public void resolveImports(CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    for (Import currentImport : compilationUnit.getImports())
    {
      QName qname = currentImport.getImported();
      String[] names = qname.getNames();
      PackageNode currentPackage = rootPackage;
      TypeDefinition currentTypeDefinition = null;

      // now resolve the rest of the names (or as many as possible until the current items are all null)
      for (int i = 0; i < names.length; ++i)
      {
        if (currentPackage != null)
        {
          // at most one of these lookups can succeed
          currentTypeDefinition = currentPackage.getTypeDefinition(names[i]);
          // update currentPackage last (and only if we don't have a type definition)
          currentPackage = currentTypeDefinition == null ? currentPackage.getSubPackage(names[i]) : null;
        }
        else if (currentTypeDefinition != null)
        {
          // TODO: if/when we add inner types, resolve the sub-type here
          // for now, we cannot resolve the name on this definition, so fail by setting everything to null
          currentTypeDefinition = null;
        }
        else
        {
          break;
        }
      }

      if (currentTypeDefinition == null && currentPackage == null)
      {
        throw new NameNotResolvedException("Unable to resolve the import: " + qname, qname.getLexicalPhrase());
      }
      if (currentPackage != null && !currentImport.isWildcard())
      {
        throw new NameNotResolvedException("A non-wildcard import cannot resolve to a package", qname.getLexicalPhrase());
      }
      // only one of these calls will set the resolved object to a non-null value
      currentImport.setResolvedPackage(currentPackage);
      currentImport.setResolvedTypeDefinition(currentTypeDefinition);
    }
  }

  /**
   * Resolves the top level types in the specified compilation unit (e.g. function parameters and return types, field types),
   * so that they can be used anywhere in statements and expressions later on.
   * @param compilationUnit - the compilation unit to resolve the top level types of
   * @throws NameNotResolvedException - if a name could not be resolved
   * @throws ConceptualException - if there is a conceptual problem while resolving the names
   */
  public void resolveTypes(CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
    {
      resolveTypes(typeDefinition, compilationUnit);
    }
  }

  /**
   * Resolves the top level types in the specified type definition (e.g. function parameters and return types, field types),
   * so that they can be used anywhere in statements and expressions later on.
   * @param typeDefinition - the TypeDefinition to resolve the types of
   * @param compilationUnit - the optional CompilationUnit to resolve the types in the context of
   * @throws NameNotResolvedException - if a name could not be resolved
   * @throws ConceptualException - if a conceptual problem is encountered during resolution
   */
  public void resolveTypes(TypeDefinition typeDefinition, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    if (typeDefinition instanceof ClassDefinition)
    {
      ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
      QName superQName = classDefinition.getSuperClassQName();
      if (superQName != null)
      {
        TypeDefinition superTypeDefinition = resolveTypeDefinition(superQName, compilationUnit);
        if (superTypeDefinition instanceof CompoundDefinition)
        {
          throw new ConceptualException("A class may not extend a compound type", classDefinition.getLexicalPhrase());
        }
        else if (superTypeDefinition instanceof InterfaceDefinition)
        {
          throw new ConceptualException("A class may not extend an interface", classDefinition.getLexicalPhrase());
        }
        else if (!(superTypeDefinition instanceof ClassDefinition))
        {
          throw new ConceptualException("A class may only extend another class", classDefinition.getLexicalPhrase());
        }
        classDefinition.setSuperClassDefinition((ClassDefinition) superTypeDefinition);
      }
      QName[] superInterfaceQNames = classDefinition.getSuperInterfaceQNames();
      if (superInterfaceQNames != null)
      {
        InterfaceDefinition[] resolvedDefinitions = new InterfaceDefinition[superInterfaceQNames.length];
        for (int i = 0; i < superInterfaceQNames.length; ++i)
        {
          TypeDefinition resolvedInterface = resolveTypeDefinition(superInterfaceQNames[i], compilationUnit);
          if (resolvedInterface instanceof ClassDefinition)
          {
            throw new ConceptualException("A class may not implement another class", classDefinition.getLexicalPhrase());
          }
          else if (resolvedInterface instanceof CompoundDefinition)
          {
            throw new ConceptualException("A class may not implement a compound type", classDefinition.getLexicalPhrase());
          }
          else if (!(resolvedInterface instanceof InterfaceDefinition))
          {
            throw new ConceptualException("A class may only implement interfaces", classDefinition.getLexicalPhrase());
          }
          resolvedDefinitions[i] = (InterfaceDefinition) resolvedInterface;
        }
        classDefinition.setSuperInterfaceDefinitions(resolvedDefinitions);
      }
    }
    if (typeDefinition instanceof InterfaceDefinition)
    {
      InterfaceDefinition interfaceDefinition = (InterfaceDefinition) typeDefinition;
      QName[] superInterfaceQNames = interfaceDefinition.getSuperInterfaceQNames();
      if (superInterfaceQNames != null)
      {
        InterfaceDefinition[] resolvedDefinitions = new InterfaceDefinition[superInterfaceQNames.length];
        for (int i = 0; i < superInterfaceQNames.length; ++i)
        {
          TypeDefinition resolvedInterface = resolveTypeDefinition(superInterfaceQNames[i], compilationUnit);
          if (resolvedInterface instanceof ClassDefinition)
          {
            throw new ConceptualException("An interface may not extend a class", interfaceDefinition.getLexicalPhrase());
          }
          else if (resolvedInterface instanceof CompoundDefinition)
          {
            throw new ConceptualException("An interface may not extend a compound type", interfaceDefinition.getLexicalPhrase());
          }
          else if (!(resolvedInterface instanceof InterfaceDefinition))
          {
            throw new ConceptualException("An interface may only extend other interfaces", interfaceDefinition.getLexicalPhrase());
          }
          resolvedDefinitions[i] = (InterfaceDefinition) resolvedInterface;
        }
        interfaceDefinition.setSuperInterfaceDefinitions(resolvedDefinitions);
      }
    }

    for (Field field : typeDefinition.getFields())
    {
      // resolve the field's type
      Type type = field.getType();
      resolve(type, compilationUnit);

      // make sure the field is not both mutable and final/immutable
      if (field.isMutable())
      {
        // check whether the internals of the field can be altered
        boolean isAlterable = (type instanceof ArrayType && !((ArrayType) type).isContextuallyImmutable()) ||
                              (type instanceof NamedType && !((NamedType) type).isContextuallyImmutable());
        if (field.isFinal() && !isAlterable)
        {
          // the field is both final and not alterable (e.g. a final uint, or a final #Object), so it cannot be mutable
          throw new ConceptualException("A final, immutably-typed field cannot be mutable", field.getLexicalPhrase());
        }
      }
    }

    Map<String, Constructor> allConstructors = new HashMap<String, Constructor>();
    for (Constructor constructor : typeDefinition.getAllConstructors())
    {
      Block mainBlock = constructor.getBlock();
      if (mainBlock == null)
      {
        // we are resolving a bitcode file with no blocks inside it, so create a temporary one so that we can check for duplicate parameters easily
        mainBlock = new Block(null, null);
      }
      StringBuffer disambiguatorBuffer = new StringBuffer();
      if (constructor.getSinceSpecifier() != null)
      {
        disambiguatorBuffer.append(constructor.getSinceSpecifier().getMangledName());
      }
      disambiguatorBuffer.append('_');
      for (Parameter p : constructor.getParameters())
      {
        Variable oldVar = mainBlock.addVariable(p.getVariable());
        if (oldVar != null)
        {
          throw new ConceptualException("Duplicate parameter: " + p.getName(), p.getLexicalPhrase());
        }
        resolve(p.getType(), compilationUnit);
        disambiguatorBuffer.append(p.getType().getMangledName());
      }
      String disambiguator = disambiguatorBuffer.toString();
      Constructor existing = allConstructors.get(disambiguator);
      if (existing != null)
      {
        throw new ConceptualException("Duplicate constructor", constructor.getLexicalPhrase());
      }
      allConstructors.put(disambiguator, constructor);
    }

    // resolve all method return and parameter types, and check for duplicate methods
    // however, we must allow duplicated static methods if their since specifiers differ, so our map must allow for multiple methods per disambiguator
    Map<Object, Set<Method>> allMethods = new HashMap<Object, Set<Method>>();
    for (Method method : typeDefinition.getAllMethods())
    {
      resolve(method.getReturnType(), compilationUnit);
      Block mainBlock = method.getBlock();
      if (mainBlock == null)
      {
        // we are resolving a method with no block, so create a temporary one so that we can check for duplicate parameters easily
        mainBlock = new Block(null, null);
      }
      Parameter[] parameters = method.getParameters();
      for (int i = 0; i < parameters.length; ++i)
      {
        Variable oldVar = mainBlock.addVariable(parameters[i].getVariable());
        if (oldVar != null)
        {
          throw new ConceptualException("Duplicate parameter: " + parameters[i].getName(), parameters[i].getLexicalPhrase());
        }
        resolve(parameters[i].getType(), compilationUnit);
      }

      Set<Method> methodSet = allMethods.get(method.getDisambiguator());
      if (methodSet == null)
      {
        methodSet = new HashSet<Method>();
        allMethods.put(method.getDisambiguator(), methodSet);
      }
      if (methodSet.isEmpty())
      {
        methodSet.add(method);
      }
      else
      {
        // there is already a method with this disambiguator
        // first, disallow all duplicates for non-static methods (this works because Disambiguators take staticness into account)
        if (!method.isStatic())
        {
          throw new ConceptualException("Duplicate non-static method: " + method.getName(), method.getLexicalPhrase());
        }
        // for static methods, we only allow another method if it has a different since specifier from all of the existing ones
        SinceSpecifier newSpecifier = method.getSinceSpecifier();
        for (Method existing : methodSet)
        {
          SinceSpecifier currentSpecifier = existing.getSinceSpecifier();
          if (newSpecifier == null ? currentSpecifier == null : newSpecifier.compareTo(currentSpecifier) == 0)
          {
            throw new ConceptualException("Duplicate static method: " + method.getName(), method.getLexicalPhrase());
          }
        }
        // no methods exist with the same since specifier, so add the new one
        methodSet.add(method);
      }
    }
  }

  /**
   * Tries to resolve the specified QName to a TypeDefinition, from the context of the specified compilation unit,
   * or if no compilation unit is given or nothing can be found there, the root package.
   * @param qname - the QName to resolve
   * @param compilationUnit - the CompilationUnit to resolve the QName in the context of
   * @return the TypeDefinition resolved
   * @throws NameNotResolvedException - if the QName cannot be resolved
   * @throws ConceptualException - if a conceptual error occurs while resolving the TypeDefinition
   */
  public TypeDefinition resolveTypeDefinition(QName qname, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    NamedType namedType = new NamedType(false, false, qname, qname.getLexicalPhrase());
    resolve(namedType, compilationUnit);
    return namedType.getResolvedTypeDefinition();
  }

  /**
   * Resolves all of the method bodies, field assignments, etc. in the specified TypeDefinition.
   * @param typeDefinition - the TypeDefinition to resolve
   * @param compilationUnit - the CompilationUnit that the TypeDefinition was defined in
   * @throws NameNotResolvedException - if the QName cannot be resolved
   * @throws ConceptualException - if a conceptual error occurs while resolving the TypeDefinition
   */
  public void resolve(TypeDefinition typeDefinition, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    // a non-static initialiser is an immutable context if there is at least one immutable constructor
    // so we need to check whether there are any immutable constructors here
    boolean hasImmutableConstructors = false;
    for (Constructor constructor : typeDefinition.getAllConstructors())
    {
      if (constructor.isImmutable())
      {
        hasImmutableConstructors = true;
      }
      Block mainBlock = constructor.getBlock();
      for (Statement s : mainBlock.getStatements())
      {
        resolve(s, mainBlock, typeDefinition, compilationUnit, constructor.isImmutable());
      }
    }
    for (Initialiser initialiser : typeDefinition.getInitialisers())
    {
      if (initialiser instanceof FieldInitialiser)
      {
        Field field = ((FieldInitialiser) initialiser).getField();
        resolve(field.getInitialiserExpression(), initialiser.getBlock(), typeDefinition, compilationUnit, !initialiser.isStatic() & hasImmutableConstructors);
      }
      else
      {
        Block block = initialiser.getBlock();
        for (Statement statement : block.getStatements())
        {
          resolve(statement, initialiser.getBlock(), typeDefinition, compilationUnit, !initialiser.isStatic() & hasImmutableConstructors);
        }
      }
    }
    for (Method method : typeDefinition.getAllMethods())
    {
      Block mainBlock = method.getBlock();
      if (mainBlock != null)
      {
        for (Statement s : mainBlock.getStatements())
        {
          resolve(s, mainBlock, typeDefinition, compilationUnit, method.isImmutable());
        }
      }
    }
  }

  private void resolve(Type type, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    if (type instanceof ArrayType)
    {
      resolve(((ArrayType) type).getBaseType(), compilationUnit);
    }
    else if (type instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) type;
      resolve(functionType.getReturnType(), compilationUnit);
      for (Type parameterType : functionType.getParameterTypes())
      {
        resolve(parameterType, compilationUnit);
      }
    }
    else if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      if (namedType.getResolvedTypeDefinition() != null)
      {
        return;
      }

      String[] names = namedType.getQualifiedName().getNames();
      // start by looking up the first name in the compilation unit
      TypeDefinition currentDefinition = compilationUnit == null ? null : compilationUnit.getTypeDefinition(names[0]);
      PackageNode currentPackage = null;
      if (currentDefinition == null && compilationUnit != null)
      {
        // the lookup in the compilation unit failed, so try each of the imports in turn
        for (Import currentImport : compilationUnit.getImports())
        {
          PackageNode importPackage = currentImport.getResolvedPackage();
          TypeDefinition importDefinition = currentImport.getResolvedTypeDefinition();
          if (currentImport.isWildcard())
          {
            if (importPackage != null)
            {
              // at most one of these lookups can succeed
              currentDefinition = importPackage.getTypeDefinition(names[0]);
              // update currentPackage last (and only if we don't have a type definition)
              currentPackage = currentDefinition == null ? importPackage.getSubPackage(names[0]) : null;
            }
            else // if (importDefinition != null)
            {
              // TODO: if/when inner types are added, resolve the sub-type of importDefinition here
            }
          }
          else if (currentImport.getName().equals(names[0]))
          {
            currentPackage = importPackage;
            currentDefinition = importDefinition;
          }
          if (currentPackage != null || currentDefinition != null)
          {
            break;
          }
        }
      }
      if (currentPackage == null && currentDefinition == null && compilationUnit != null)
      {
        // the lookup from the imports failed, so try to look up the first name on the compilation unit's package instead
        // (at most one of the following lookups can succeed)
        currentDefinition = compilationUnit.getResolvedPackage().getTypeDefinition(names[0]);
        // update currentPackage last (and only if we don't have a type definition)
        if (currentDefinition == null)
        {
          currentPackage = compilationUnit.getResolvedPackage().getSubPackage(names[0]);
        }
      }
      if (currentPackage == null && currentDefinition == null)
      {
        // all other lookups failed, so try to look up the first name on the root package
        // (at most one of the following lookups can succeed)
        currentDefinition = rootPackage.getTypeDefinition(names[0]);
        // update currentPackage last (and only if we don't have a type definition)
        if (currentDefinition == null)
        {
          currentPackage = rootPackage.getSubPackage(names[0]);
        }
      }
      // now resolve the rest of the names (or as many as possible until the current items are all null)
      for (int i = 1; i < names.length; ++i)
      {
        if (currentPackage != null)
        {
          // at most one of these lookups can succeed
          currentDefinition = currentPackage.getTypeDefinition(names[i]);
          // update currentPackage last (and only if we don't have a type definition)
          currentPackage = currentDefinition == null ? currentPackage.getSubPackage(names[i]) : null;
        }
        else if (currentDefinition != null)
        {
          // TODO: if/when we add inner types, resolve the sub-type here
          // for now, we cannot resolve the name on this definition, so fail by setting everything to null
          currentDefinition = null;
        }
        else
        {
          break;
        }
      }

      if (currentDefinition == null)
      {
        if (currentPackage != null)
        {
          throw new ConceptualException("A package cannot be used as a type", namedType.getLexicalPhrase());
        }
        throw new NameNotResolvedException("Unable to resolve: " + namedType.getQualifiedName(), namedType.getLexicalPhrase());
      }
      namedType.setResolvedTypeDefinition(currentDefinition);
    }
    else if (type instanceof ObjectType)
    {
      // do nothing
    }
    else if (type instanceof PrimitiveType)
    {
      // do nothing
    }
    else if (type instanceof TupleType)
    {
      TupleType tupleType = (TupleType) type;
      for (Type subType : tupleType.getSubTypes())
      {
        resolve(subType, compilationUnit);
      }
    }
    else if (type instanceof VoidType)
    {
      // do nothing
    }
    else
    {
      throw new IllegalArgumentException("Unknown Type type: " + type);
    }
  }

  private void resolve(Statement statement, Block enclosingBlock, TypeDefinition enclosingDefinition, CompilationUnit compilationUnit, boolean inImmutableContext) throws NameNotResolvedException, ConceptualException
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assignStatement = (AssignStatement) statement;
      Type type = assignStatement.getType();
      if (type != null)
      {
        resolve(type, compilationUnit);
      }
      Assignee[] assignees = assignStatement.getAssignees();
      boolean distributedTupleType = type != null && type instanceof TupleType && !type.isNullable() && ((TupleType) type).getSubTypes().length == assignees.length;
      boolean madeVariableDeclaration = false;
      List<VariableAssignee> alreadyDeclaredVariables = new LinkedList<VariableAssignee>();
      for (int i = 0; i < assignees.length; i++)
      {
        if (assignees[i] instanceof VariableAssignee)
        {
          VariableAssignee variableAssignee = (VariableAssignee) assignees[i];
          Variable variable = enclosingBlock.getVariable(variableAssignee.getVariableName());
          if (variable != null)
          {
            alreadyDeclaredVariables.add(variableAssignee);
          }
          if (variable == null && type != null)
          {
            // we have a type, and the variable is not yet declared in this block, so declare the variable now
            if (distributedTupleType)
            {
              Type subType = ((TupleType) type).getSubTypes()[i];
              variable = new Variable(assignStatement.isFinal(), subType, variableAssignee.getVariableName());
            }
            else
            {
              variable = new Variable(assignStatement.isFinal(), type, variableAssignee.getVariableName());
            }
            enclosingBlock.addVariable(variable);
            madeVariableDeclaration = true;
          }
          if (variable == null && enclosingDefinition != null)
          {
            // we haven't got a declared variable, so try to resolve it outside the block
            Field field;
            if (enclosingDefinition instanceof ClassDefinition)
            {
              ClassDefinition current = (ClassDefinition) enclosingDefinition;
              field = null;
              // check the base class and then each of the super-classes in turn
              // note: this allows static fields from the superclass to be resolved, which is possible inside the class itself, but not by specifying an explicit type
              while (field == null & current != null)
              {
                field = current.getField(variableAssignee.getVariableName());
                current = current.getSuperClassDefinition();
              }
            }
            else if (enclosingDefinition instanceof CompoundDefinition)
            {
              field = enclosingDefinition.getField(variableAssignee.getVariableName());
            }
            else
            {
              throw new IllegalArgumentException("Unknown enclosing definition type: " + enclosingDefinition);
            }
            if (field != null)
            {
              if (field.isStatic())
              {
                variable = field.getGlobalVariable();
              }
              else
              {
                variable = field.getMemberVariable();
              }
            }
          }
          if (variable == null)
          {
            throw new NameNotResolvedException("Unable to resolve: " + variableAssignee.getVariableName(), variableAssignee.getLexicalPhrase());
          }
          variableAssignee.setResolvedVariable(variable);
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          resolve(arrayElementAssignee.getArrayExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
          resolve(arrayElementAssignee.getDimensionExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
        }
        else if (assignees[i] instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];
          fieldAssignee.getFieldAccessExpression().setIsAssignableHint(true);
          // use the expression resolver to resolve the contained field access expression
          resolve(fieldAssignee.getFieldAccessExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
        }
        else if (assignees[i] instanceof BlankAssignee)
        {
          // do nothing, this assignee doesn't actually get assigned to
        }
        else
        {
          throw new IllegalStateException("Unknown Assignee type: " + assignees[i]);
        }
      }
      if (type != null && !madeVariableDeclaration)
      {
        // giving a type indicates a variable declaration, which is not allowed if all of the variables have already been declared
        // if at least one of them is being declared, however, we allow the type to be present
        if (alreadyDeclaredVariables.size() == 1)
        {
          VariableAssignee variableAssignee = alreadyDeclaredVariables.get(0);
          throw new ConceptualException("'" + variableAssignee.getVariableName() + "' has already been declared, and cannot be redeclared", variableAssignee.getLexicalPhrase());
        }
        StringBuffer buffer = new StringBuffer();
        Iterator<VariableAssignee> it = alreadyDeclaredVariables.iterator();
        while (it.hasNext())
        {
          buffer.append('\'');
          buffer.append(it.next().getVariableName());
          buffer.append('\'');
          if (it.hasNext())
          {
            buffer.append(", ");
          }
        }
        throw new ConceptualException("The variables " + buffer + " have all already been declared, and cannot be redeclared", assignStatement.getLexicalPhrase());
      }
      if (assignStatement.getExpression() != null)
      {
        resolve(assignStatement.getExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
      }
    }
    else if (statement instanceof Block)
    {
      Block subBlock = (Block) statement;
      for (Variable v : enclosingBlock.getVariables())
      {
        subBlock.addVariable(v);
      }
      for (Statement s : subBlock.getStatements())
      {
        resolve(s, subBlock, enclosingDefinition, compilationUnit, inImmutableContext);
      }
    }
    else if (statement instanceof BreakStatement)
    {
      // do nothing
    }
    else if (statement instanceof ContinueStatement)
    {
      // do nothing
    }
    else if (statement instanceof DelegateConstructorStatement)
    {
      DelegateConstructorStatement delegateConstructorStatement = (DelegateConstructorStatement) statement;
      Expression[] arguments = delegateConstructorStatement.getArguments();
      for (Expression argument : arguments)
      {
        resolve(argument, enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
      }
      TypeDefinition constructorTypeDefinition;
      if (delegateConstructorStatement.isSuperConstructor())
      {
        if (enclosingDefinition instanceof CompoundDefinition)
        {
          throw new ConceptualException("Cannot call a super(...) constructor from a compound type", delegateConstructorStatement.getLexicalPhrase());
        }
        else if (!(enclosingDefinition instanceof ClassDefinition))
        {
          throw new ConceptualException("A super(...) constructor can only be called from inside a class definition", delegateConstructorStatement.getLexicalPhrase());
        }
        ClassDefinition superClassDefinition = ((ClassDefinition) enclosingDefinition).getSuperClassDefinition();
        if (superClassDefinition == null)
        {
          // TODO: once the type system has been unified under a single common super-type, remove this restriction, and allow super() to mean just calling the object() constructor (a no-op) and running the initialisers
          throw new ConceptualException("Cannot call a super(...) constructor from a class with no superclass", delegateConstructorStatement.getLexicalPhrase());
        }
        constructorTypeDefinition = superClassDefinition;
      }
      else
      {
        constructorTypeDefinition = enclosingDefinition;
      }
      Constructor resolvedConstructor = resolveConstructor(constructorTypeDefinition, arguments, delegateConstructorStatement.getLexicalPhrase());
      delegateConstructorStatement.setResolvedConstructor(resolvedConstructor);
      // if there was no matching constructor, the resolved constructor call may not type check
      // in this case, we should point out this error before we run the cycle checker, because the cycle checker could find that the constructor is recursive
      // so run the type checker on this statement now
      TypeChecker.checkTypes(statement, null); // give a null return type here, since the type checker will not need to use it
    }
    else if (statement instanceof ExpressionStatement)
    {
      resolve(((ExpressionStatement) statement).getExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (statement instanceof ForStatement)
    {
      ForStatement forStatement = (ForStatement) statement;
      Statement init = forStatement.getInitStatement();
      Expression condition = forStatement.getConditional();
      Statement update = forStatement.getUpdateStatement();
      Block block = forStatement.getBlock();
      // process this block right here instead of recursing, since we need to process the init, condition, and update parts of the statement inside it after adding the variables, but before the rest of the resolution
      for (Variable v : enclosingBlock.getVariables())
      {
        block.addVariable(v);
      }
      if (init != null)
      {
        resolve(init, block, enclosingDefinition, compilationUnit, inImmutableContext);
      }
      if (condition != null)
      {
        resolve(condition, block, enclosingDefinition, compilationUnit, inImmutableContext);
      }
      if (update != null)
      {
        resolve(update, block, enclosingDefinition, compilationUnit, inImmutableContext);
      }
      for (Statement s : block.getStatements())
      {
        resolve(s, block, enclosingDefinition, compilationUnit, inImmutableContext);
      }
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      resolve(ifStatement.getExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
      resolve(ifStatement.getThenClause(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
      if (ifStatement.getElseClause() != null)
      {
        resolve(ifStatement.getElseClause(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
      }
    }
    else if (statement instanceof PrefixIncDecStatement)
    {
      PrefixIncDecStatement prefixIncDecStatement = (PrefixIncDecStatement) statement;
      Assignee assignee = prefixIncDecStatement.getAssignee();
      if (assignee instanceof VariableAssignee)
      {
        VariableAssignee variableAssignee = (VariableAssignee) assignee;
        Variable variable = enclosingBlock.getVariable(variableAssignee.getVariableName());
        if (variable == null && enclosingDefinition != null)
        {
          Field field;
          if (enclosingDefinition instanceof ClassDefinition)
          {
            ClassDefinition current = (ClassDefinition) enclosingDefinition;
            field = null;
            // check the base class and then each of the super-classes in turn
            // note: this allows static fields from the superclass to be resolved, which is possible inside the class itself, but not by specifying an explicit type
            while (field == null & current != null)
            {
              field = current.getField(variableAssignee.getVariableName());
              current = current.getSuperClassDefinition();
            }
          }
          else if (enclosingDefinition instanceof CompoundDefinition)
          {
            field = enclosingDefinition.getField(variableAssignee.getVariableName());
          }
          else
          {
            throw new IllegalArgumentException("Unknown enclosing definition type: " + enclosingDefinition);
          }
          if (field != null)
          {
            if (field.isStatic())
            {
              variable = field.getGlobalVariable();
            }
            else
            {
              variable = field.getMemberVariable();
            }
          }
        }
        if (variable == null)
        {
          throw new NameNotResolvedException("Unable to resolve: " + variableAssignee.getVariableName(), variableAssignee.getLexicalPhrase());
        }
        variableAssignee.setResolvedVariable(variable);
      }
      else if (assignee instanceof ArrayElementAssignee)
      {
        ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
        resolve(arrayElementAssignee.getArrayExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
        resolve(arrayElementAssignee.getDimensionExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
      }
      else if (assignee instanceof BlankAssignee)
      {
        throw new ConceptualException("Cannot " + (prefixIncDecStatement.isIncrement() ? "inc" : "dec") + "rement a blank assignee", assignee.getLexicalPhrase());
      }
      else if (assignee instanceof FieldAssignee)
      {
        FieldAssignee fieldAssignee = (FieldAssignee) assignee;
        // use the expression resolver to resolve the contained field access expression
        resolve(fieldAssignee.getFieldAccessExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
      }
      else
      {
        throw new IllegalStateException("Unknown Assignee type: " + assignee);
      }
    }
    else if (statement instanceof ReturnStatement)
    {
      ReturnStatement returnStatement = (ReturnStatement) statement;
      // currently, nothing can return against contextual immutability.
      // however, once properties exist, their getters will be able to
      returnStatement.setCanReturnAgainstContextualImmutability(false);
      Expression returnedExpression = returnStatement.getExpression();
      if (returnedExpression != null)
      {
        resolve(returnedExpression, enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
      }
    }
    else if (statement instanceof ShorthandAssignStatement)
    {
      ShorthandAssignStatement shorthandAssignStatement = (ShorthandAssignStatement) statement;
      for (Assignee assignee : shorthandAssignStatement.getAssignees())
      {
        if (assignee instanceof VariableAssignee)
        {
          VariableAssignee variableAssignee = (VariableAssignee) assignee;
          Variable variable = enclosingBlock.getVariable(variableAssignee.getVariableName());
          if (variable == null && enclosingDefinition != null)
          {
            Field field;
            if (enclosingDefinition instanceof ClassDefinition)
            {
              ClassDefinition current = (ClassDefinition) enclosingDefinition;
              field = null;
              // check the base class and then each of the super-classes in turn
              // note: this allows static fields from the superclass to be resolved, which is possible inside the class itself, but not by specifying an explicit type
              while (field == null & current != null)
              {
                field = current.getField(variableAssignee.getVariableName());
                current = current.getSuperClassDefinition();
              }
            }
            else if (enclosingDefinition instanceof CompoundDefinition)
            {
              field = enclosingDefinition.getField(variableAssignee.getVariableName());
            }
            else
            {
              throw new IllegalArgumentException("Unknown enclosing definition type: " + enclosingDefinition);
            }
            if (field != null)
            {
              if (field.isStatic())
              {
                variable = field.getGlobalVariable();
              }
              else
              {
                variable = field.getMemberVariable();
              }
            }
          }
          if (variable == null)
          {
            throw new NameNotResolvedException("Unable to resolve: " + variableAssignee.getVariableName(), variableAssignee.getLexicalPhrase());
          }
          variableAssignee.setResolvedVariable(variable);
        }
        else if (assignee instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
          resolve(arrayElementAssignee.getArrayExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
          resolve(arrayElementAssignee.getDimensionExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
        }
        else if (assignee instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignee;
          // use the expression resolver to resolve the contained field access expression
          resolve(fieldAssignee.getFieldAccessExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
        }
        else if (assignee instanceof BlankAssignee)
        {
          // do nothing, this assignee doesn't actually get assigned to
        }
        else
        {
          throw new IllegalStateException("Unknown Assignee type: " + assignee);
        }
      }
      resolve(shorthandAssignStatement.getExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      resolve(whileStatement.getExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
      resolve(whileStatement.getStatement(), enclosingBlock, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else
    {
      throw new ConceptualException("Internal name resolution error: Unknown statement type: " + statement, statement.getLexicalPhrase());
    }
  }

  private void resolve(Expression expression, Block block, TypeDefinition enclosingDefinition, CompilationUnit compilationUnit, boolean inImmutableContext) throws NameNotResolvedException, ConceptualException
  {
    if (expression instanceof ArithmeticExpression)
    {
      resolve(((ArithmeticExpression) expression).getLeftSubExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
      resolve(((ArithmeticExpression) expression).getRightSubExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof ArrayAccessExpression)
    {
      ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
      resolve(arrayAccessExpression.getArrayExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
      resolve(arrayAccessExpression.getDimensionExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof ArrayCreationExpression)
    {
      ArrayCreationExpression creationExpression = (ArrayCreationExpression) expression;
      resolve(creationExpression.getDeclaredType(), compilationUnit);
      if (creationExpression.getDimensionExpressions() != null)
      {
        for (Expression e : creationExpression.getDimensionExpressions())
        {
          resolve(e, block, enclosingDefinition, compilationUnit, inImmutableContext);
        }
      }
      if (creationExpression.getValueExpressions() != null)
      {
        for (Expression e : creationExpression.getValueExpressions())
        {
          resolve(e, block, enclosingDefinition, compilationUnit, inImmutableContext);
        }
      }
    }
    else if (expression instanceof BitwiseNotExpression)
    {
      resolve(((BitwiseNotExpression) expression).getExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof BooleanLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof BooleanNotExpression)
    {
      resolve(((BooleanNotExpression) expression).getExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof BracketedExpression)
    {
      resolve(((BracketedExpression) expression).getExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof CastExpression)
    {
      CastExpression castExpression = (CastExpression) expression;
      Type castType = expression.getType();
      resolve(castType, compilationUnit);

      // before resolving the casted expression, add hints for any FieldAccessExpressions or VariableExpressions that are directly inside it
      Expression subExpression = castExpression.getExpression();
      while (subExpression instanceof BracketedExpression)
      {
        subExpression = ((BracketedExpression) subExpression).getExpression();
      }
      if (subExpression instanceof FieldAccessExpression)
      {
        ((FieldAccessExpression) subExpression).setTypeHint(castType);
      }
      if (subExpression instanceof VariableExpression)
      {
        ((VariableExpression) subExpression).setTypeHint(castType);
      }
      if (subExpression instanceof FunctionCallExpression)
      {
        Expression baseExpression = ((FunctionCallExpression) subExpression).getFunctionExpression();
        while (baseExpression instanceof BracketedExpression)
        {
          baseExpression = ((BracketedExpression) baseExpression).getExpression();
        }
        if (baseExpression instanceof FieldAccessExpression)
        {
          ((FieldAccessExpression) baseExpression).setReturnTypeHint(castType);
        }
        if (baseExpression instanceof VariableExpression)
        {
          ((VariableExpression) baseExpression).setReturnTypeHint(castType);
        }
      }

      resolve(castExpression.getExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof ClassCreationExpression)
    {
      ClassCreationExpression classCreationExpression = (ClassCreationExpression) expression;
      NamedType type = new NamedType(false, false, classCreationExpression.getQualifiedName(), null);
      resolve(type, compilationUnit);
      classCreationExpression.setResolvedType(type);
      Expression[] arguments = classCreationExpression.getArguments();
      for (Expression argument : arguments)
      {
        resolve(argument, block, enclosingDefinition, compilationUnit, inImmutableContext);
      }
      Constructor resolvedConstructor = resolveConstructor(type.getResolvedTypeDefinition(), arguments, classCreationExpression.getLexicalPhrase());
      classCreationExpression.setResolvedConstructor(resolvedConstructor);
    }
    else if (expression instanceof EqualityExpression)
    {
      resolve(((EqualityExpression) expression).getLeftSubExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
      resolve(((EqualityExpression) expression).getRightSubExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof FieldAccessExpression)
    {
      FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) expression;
      fieldAccessExpression.setResolvedContextImmutability(inImmutableContext);
      String fieldName = fieldAccessExpression.getFieldName();

      Type baseType;
      boolean baseIsStatic;
      if (fieldAccessExpression.getBaseExpression() != null)
      {
        resolve(fieldAccessExpression.getBaseExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);

        // find the type of the sub-expression, by calling the type checker
        // this is fine as long as we resolve all of the sub-expression first
        baseType = TypeChecker.checkTypes(fieldAccessExpression.getBaseExpression());
        baseIsStatic = false;
      }
      else if (fieldAccessExpression.getBaseType() != null)
      {
        baseType = fieldAccessExpression.getBaseType();
        resolve(baseType, compilationUnit);
        baseIsStatic = true;
      }
      else
      {
        throw new IllegalStateException("Unknown base type for a field access: " + fieldAccessExpression);
      }

      Set<Member> memberSet = baseType.getMembers(fieldName);
      Set<Member> staticFiltered = new HashSet<Member>();
      for (Member member : memberSet)
      {
        if (member instanceof ArrayLengthMember)
        {
          if (baseIsStatic)
          {
            throw new ConceptualException("Cannot access the array length member statically", fieldAccessExpression.getLexicalPhrase());
          }
          staticFiltered.add(member);
        }
        else if (member instanceof Field)
        {
          if (((Field) member).isStatic() == baseIsStatic)
          {
            staticFiltered.add(member);
          }
        }
        else if (member instanceof Method)
        {
          if (((Method) member).isStatic() == baseIsStatic)
          {
            staticFiltered.add(member);
          }
        }
        else
        {
          throw new IllegalStateException("Unknown member type: " + member);
        }
      }

      if (staticFiltered.isEmpty())
      {
        throw new NameNotResolvedException("No such " + (baseIsStatic ? "static" : "non-static") + " member \"" + fieldName + "\" for type " + baseType, fieldAccessExpression.getLexicalPhrase());
      }
      Member resolved = null;
      if (staticFiltered.size() == 1)
      {
        resolved = staticFiltered.iterator().next();
      }
      else
      {
        Set<Member> hintFiltered = applyTypeHints(staticFiltered, fieldAccessExpression.getTypeHint(), fieldAccessExpression.getReturnTypeHint(), fieldAccessExpression.getIsFunctionHint(), fieldAccessExpression.getIsAssignableHint());
        if (hintFiltered.size() == 1)
        {
          resolved = hintFiltered.iterator().next();
        }
      }
      if (resolved == null)
      {
        throw new ConceptualException("Multiple " + (baseIsStatic ? "static" : "non-static") + " members have the name '" + fieldName + "'", fieldAccessExpression.getLexicalPhrase());
      }
      fieldAccessExpression.setResolvedMember(resolved);
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression expr = (FunctionCallExpression) expression;
      // resolve all of the sub-expressions
      for (Expression e : expr.getArguments())
      {
        resolve(e, block, enclosingDefinition, compilationUnit, inImmutableContext);
        TypeChecker.checkTypes(e);
      }

      Expression functionExpression = expr.getFunctionExpression();

      // before resolving the functionExpression, add hints for any FieldAccessExpressions or VariableExpressions that are directly inside it
      Expression subExpression = functionExpression;
      while (subExpression instanceof BracketedExpression)
      {
        subExpression = ((BracketedExpression) subExpression).getExpression();
      }
      if (subExpression instanceof FieldAccessExpression)
      {
        ((FieldAccessExpression) subExpression).setIsFunctionHint(true);
      }
      if (subExpression instanceof VariableExpression)
      {
        ((VariableExpression) subExpression).setIsFunctionHint(true);
      }

      Type expressionType = null;
      Exception cachedException = null;
      // first, try to resolve the function call as a normal expression
      // this MUST be done first, so that local variables with function types are considered before outside methods
      try
      {
        resolve(functionExpression, block, enclosingDefinition, compilationUnit, inImmutableContext);
        expressionType = TypeChecker.checkTypes(functionExpression);
      }
      catch (NameNotResolvedException e)
      {
        cachedException = e;
      }
      catch (ConceptualException e)
      {
        cachedException = e;
      }
      if (cachedException == null)
      {
        if (expressionType instanceof FunctionType)
        {
          // the sub-expressions all resolved properly, so we could just return here
          // however, if this is just a normal method call, we can pull the resolved method into
          // this FunctionCallExpression, so that we don't have to convert through FunctionType
          Expression testExpression = functionExpression;
          while (testExpression instanceof BracketedExpression)
          {
            testExpression = ((BracketedExpression) testExpression).getExpression();
          }
          if (testExpression instanceof VariableExpression)
          {
            VariableExpression variableExpression = (VariableExpression) testExpression;
            if (variableExpression.getResolvedMethod() != null)
            {
              // the base resolved to a Method, so just resolve this FunctionCallExpression to the same Method
              expr.setResolvedMethod(variableExpression.getResolvedMethod());
              return;
            }
          }
          else if (testExpression instanceof FieldAccessExpression)
          {
            FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) testExpression;
            Member resolvedMember = fieldAccessExpression.getResolvedMember();
            if (resolvedMember instanceof Method)
            {
              // the base resolved to a Method, so just resolve this FunctionCallExpression to the same Method
              expr.setResolvedMethod((Method) resolvedMember);
              expr.setResolvedBaseExpression(fieldAccessExpression.getBaseExpression()); // this will be null for static field accesses
              expr.setResolvedNullTraversal(fieldAccessExpression.isNullTraversing());
              return;
            }
          }
          expr.setResolvedBaseExpression(functionExpression);
          return;
        }
        throw new ConceptualException("Cannot call a function on a non-function type", functionExpression.getLexicalPhrase());
      }

      // we failed to resolve the sub-expression into something with a function type
      // but the recursive resolver doesn't know which parameter types we're looking for here, so we may be able to consider some different options
      // we can do this by checking if the function expression is actually a variable access or a field access expression, and checking them for other sources of method calls,
      // such as constructor calls and method calls, each of which can be narrowed down by their parameter types

      // first, go through any bracketed expressions, as we can ignore them
      while (functionExpression instanceof BracketedExpression)
      {
        functionExpression = ((BracketedExpression) functionExpression).getExpression();
      }

      Map<Parameter[], Member> paramLists = new HashMap<Parameter[], Member>();
      Map<Parameter[], Member> hintedParamLists = new HashMap<Parameter[], Member>();
      Map<Method, Expression> methodBaseExpressions = new HashMap<Method, Expression>();
      if (functionExpression instanceof VariableExpression)
      {
        VariableExpression variableExpression = (VariableExpression) functionExpression;
        String name = variableExpression.getName();
        // the sub-expression didn't resolve to a variable or a field, or we would have got a valid type back in expressionType
        if (enclosingDefinition != null)
        {
          Set<Member> memberSet = new NamedType(false, false, enclosingDefinition).getMembers(name, true);
          memberSet = memberSet != null ? memberSet : new HashSet<Member>();
          Set<Member> hintedMemberSet = applyTypeHints(memberSet, variableExpression.getTypeHint(), variableExpression.getReturnTypeHint(), variableExpression.getIsFunctionHint(), variableExpression.getIsAssignableHint());
          for (Member m : memberSet)
          {
            if (m instanceof Method)
            {
              Parameter[] params = ((Method) m).getParameters();
              paramLists.put(params, m);
              if (hintedMemberSet.contains(m))
              {
                hintedParamLists.put(params, m);
              }
              // leave methodBaseExpressions with a null value for this method, as we have no base expression
            }
          }
        }
        // try resolving it as a constructor call for a CompoundDefinition, by calling resolve() on it as a NamedType
        try
        {
          NamedType type = new NamedType(false, false, new QName(name, null), null);
          resolve(type, compilationUnit);
          TypeDefinition typeDefinition = type.getResolvedTypeDefinition();
          if (typeDefinition != null && typeDefinition instanceof CompoundDefinition)
          {
            for (Constructor c : typeDefinition.getUniqueConstructors())
            {
              paramLists.put(c.getParameters(), c);
              if (!variableExpression.getIsAssignableHint() && variableExpression.getTypeHint() == null)
              {
                Type returnTypeHint = variableExpression.getReturnTypeHint();
                if (returnTypeHint != null && returnTypeHint.canAssign(new NamedType(false, false, typeDefinition)))
                {
                  hintedParamLists.put(c.getParameters(), c);
                }
              }
            }
          }
        }
        catch (NameNotResolvedException e)
        {
          // ignore this error, just assume it wasn't meant to resolve to a constructor call
        }
        catch (ConceptualException e)
        {
          // ignore this error, just assume it wasn't meant to resolve to a constructor call
        }
      }
      else if (functionExpression instanceof FieldAccessExpression)
      {
        FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) functionExpression;
        expr.setResolvedNullTraversal(fieldAccessExpression.isNullTraversing());

        // first, check whether this is a call to a constructor of a CompoundDefinition
        QName qname = extractFieldAccessQName(fieldAccessExpression);
        if (qname != null)
        {
          try
          {
            NamedType type = new NamedType(false, false, qname, null);
            resolve(type, compilationUnit);
            TypeDefinition typeDefinition = type.getResolvedTypeDefinition();
            if (typeDefinition != null && typeDefinition instanceof CompoundDefinition)
            {
              for (Constructor c : typeDefinition.getUniqueConstructors())
              {
                paramLists.put(c.getParameters(), c);
                if (!fieldAccessExpression.getIsAssignableHint() && fieldAccessExpression.getTypeHint() == null)
                {
                  Type returnTypeHint = fieldAccessExpression.getReturnTypeHint();
                  if (returnTypeHint != null && returnTypeHint.canAssign(new NamedType(false, false, typeDefinition)))
                  {
                    hintedParamLists.put(c.getParameters(), c);
                  }
                }
              }
            }
          }
          catch (NameNotResolvedException e)
          {
            // ignore this error, just assume it wasn't meant to resolve to a constructor call
          }
          catch (ConceptualException e)
          {
            // ignore this error, just assume it wasn't meant to resolve to a constructor call
          }
        }

        // now look for normal method accesses
        try
        {
          String name = fieldAccessExpression.getFieldName();

          Expression baseExpression = fieldAccessExpression.getBaseExpression();
          Type baseType;
          boolean baseIsStatic;
          if (baseExpression != null)
          {
            resolve(baseExpression, block, enclosingDefinition, compilationUnit, inImmutableContext);

            // find the type of the sub-expression, by calling the type checker
            // this is fine as long as we resolve all of the sub-expression first
            baseType = TypeChecker.checkTypes(baseExpression);
            baseIsStatic = false;
          }
          else if (fieldAccessExpression.getBaseType() != null)
          {
            baseType = fieldAccessExpression.getBaseType();
            resolve(baseType, compilationUnit);
            baseIsStatic = true;
          }
          else
          {
            throw new IllegalStateException("Unknown base type for a field access: " + fieldAccessExpression);
          }

          Set<Member> memberSet = baseType.getMembers(name);
          memberSet = memberSet != null ? memberSet : new HashSet<Member>();
          Set<Member> hintedMemberSet = applyTypeHints(memberSet, fieldAccessExpression.getTypeHint(), fieldAccessExpression.getReturnTypeHint(), fieldAccessExpression.getIsFunctionHint(), fieldAccessExpression.getIsAssignableHint());
          for (Member member : memberSet)
          {
            // only allow access to this method if it is called in the right way, depending on whether or not it is static
            if (member instanceof Method && ((Method) member).isStatic() == baseIsStatic)
            {
              Method method = (Method) member;
              paramLists.put(method.getParameters(), method);
              if (hintedMemberSet.contains(member))
              {
                hintedParamLists.put(method.getParameters(), method);
              }
              methodBaseExpressions.put(method, baseExpression);
            }
          }
        }
        catch (NameNotResolvedException e)
        {
          // ignore this error, just assume it wasn't meant to resolve to a method call
        }
        catch (ConceptualException e)
        {
          // ignore this error, just assume it wasn't meant to resolve to a method call
        }
      }

      // filter out parameter lists which are not assign-compatible with the arguments
      filterParameterLists(hintedParamLists.entrySet(), expr.getArguments(), false, false);
      // if there are multiple parameter lists, try to narrow it down to one that is equivalent to the argument list
      if (hintedParamLists.size() > 1)
      {
        Map<Parameter[], Member> backupHintedParamLists = new HashMap<Parameter[], Member>(hintedParamLists);
        filterParameterLists(hintedParamLists.entrySet(), expr.getArguments(), true, false);
        // if we have filtered out all of the parameter lists, try to narrow it down again, but this time allow nullable versions of argument types for parameters
        if (hintedParamLists.isEmpty())
        {
          // revert back to the unfiltered one, and refilter with a broader condition
          hintedParamLists = backupHintedParamLists;
          filterParameterLists(hintedParamLists.entrySet(), expr.getArguments(), true, true);
        }
      }
      if (hintedParamLists.size() == 1)
      {
        paramLists = hintedParamLists;
      }
      else
      {
        // try the same thing without using hintedParamLists
        filterParameterLists(paramLists.entrySet(), expr.getArguments(), false, false);
        if (paramLists.size() > 1)
        {
          Map<Parameter[], Member> backupParamLists = new HashMap<Parameter[], Member>(paramLists);
          filterParameterLists(paramLists.entrySet(), expr.getArguments(), true, false);
          if (paramLists.isEmpty())
          {
            paramLists = backupParamLists;
            filterParameterLists(paramLists.entrySet(), expr.getArguments(), true, true);
          }
        }
      }

      if (paramLists.size() > 1)
      {
        throw new ConceptualException("Ambiguous function call, there are at least two applicable functions which take these arguments", expr.getLexicalPhrase());
      }
      if (paramLists.isEmpty())
      {
        // we didn't find anything, so rethrow the exception from earlier
        if (cachedException instanceof NameNotResolvedException)
        {
          throw (NameNotResolvedException) cachedException;
        }
        throw (ConceptualException) cachedException;
      }

      Entry<Parameter[], Member> entry = paramLists.entrySet().iterator().next();
      if (entry.getValue() instanceof Constructor)
      {
        expr.setResolvedConstructor((Constructor) entry.getValue());
      }
      else if (entry.getValue() instanceof Method)
      {
        expr.setResolvedMethod((Method) entry.getValue());
        // if the method call had no base expression, e.g. it was a VariableExpression being called, this will just set it to null
        expr.setResolvedBaseExpression(methodBaseExpressions.get(entry.getValue()));
      }
      else
      {
        throw new IllegalStateException("Unknown function call expression target type: " + entry.getValue());
      }
    }
    else if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIfExpression = (InlineIfExpression) expression;
      resolve(inlineIfExpression.getCondition(), block, enclosingDefinition, compilationUnit, inImmutableContext);
      resolve(inlineIfExpression.getThenExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
      resolve(inlineIfExpression.getElseExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof LogicalExpression)
    {
      resolve(((LogicalExpression) expression).getLeftSubExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
      resolve(((LogicalExpression) expression).getRightSubExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof MinusExpression)
    {
      resolve(((MinusExpression) expression).getExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof NullCoalescingExpression)
    {
      resolve(((NullCoalescingExpression) expression).getNullableExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
      resolve(((NullCoalescingExpression) expression).getAlternativeExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof NullLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof ObjectCreationExpression)
    {
      // do nothing
    }
    else if (expression instanceof RelationalExpression)
    {
      resolve(((RelationalExpression) expression).getLeftSubExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
      resolve(((RelationalExpression) expression).getRightSubExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof ShiftExpression)
    {
      resolve(((ShiftExpression) expression).getLeftExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
      resolve(((ShiftExpression) expression).getRightExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof StringLiteralExpression)
    {
      // resolve the type of the string literal here, so that we have access to it in the type checker
      expression.setType(SpecialTypeHandler.STRING_TYPE);
    }
    else if (expression instanceof ThisExpression)
    {
      ThisExpression thisExpression = (ThisExpression) expression;
      if (enclosingDefinition == null)
      {
        throw new ConceptualException("'this' does not refer to anything in this context", thisExpression.getLexicalPhrase());
      }
      thisExpression.setType(new NamedType(false, false, inImmutableContext, enclosingDefinition));
    }
    else if (expression instanceof TupleExpression)
    {
      TupleExpression tupleExpression = (TupleExpression) expression;
      Expression[] subExpressions = tupleExpression.getSubExpressions();
      for (int i = 0; i < subExpressions.length; i++)
      {
        resolve(subExpressions[i], block, enclosingDefinition, compilationUnit, inImmutableContext);
      }
    }
    else if (expression instanceof TupleIndexExpression)
    {
      TupleIndexExpression indexExpression = (TupleIndexExpression) expression;
      resolve(indexExpression.getExpression(), block, enclosingDefinition, compilationUnit, inImmutableContext);
    }
    else if (expression instanceof VariableExpression)
    {
      VariableExpression expr = (VariableExpression) expression;
      expr.setResolvedContextImmutability(inImmutableContext);
      Variable var = block.getVariable(expr.getName());
      if (var != null)
      {
        expr.setResolvedVariable(var);
        return;
      }
      if (enclosingDefinition != null)
      {
        Set<Member> members = new NamedType(false, false, enclosingDefinition).getMembers(expr.getName(), true);
        members = members != null ? members : new HashSet<Member>();

        Member resolved = null;
        if (members.size() == 1)
        {
          resolved = members.iterator().next();
        }
        else
        {
          Set<Member> filteredMembers = applyTypeHints(members, expr.getTypeHint(), expr.getReturnTypeHint(), expr.getIsFunctionHint(), expr.getIsAssignableHint());
          if (filteredMembers.size() == 1)
          {
            resolved = filteredMembers.iterator().next();
          }
          else if (members.size() > 1)
          {
            throw new ConceptualException("Multiple members have the name '" + expr.getName() + "'", expr.getLexicalPhrase());
          }
        }

        if (resolved != null)
        {
          if (resolved instanceof Field)
          {
            Field field = (Field) resolved;
            if (field.isStatic())
            {
              var = field.getGlobalVariable();
            }
            else
            {
              var = field.getMemberVariable();
            }
            expr.setResolvedVariable(var);
            return;
          }
          else if (resolved instanceof Method)
          {
            expr.setResolvedMethod((Method) resolved);
            return;
          }
          throw new IllegalStateException("Unknown member type: " + resolved);
        }
      }
      throw new NameNotResolvedException("Unable to resolve \"" + expr.getName() + "\"", expr.getLexicalPhrase());
    }
    else
    {
      throw new ConceptualException("Internal name resolution error: Unknown expression type", expression.getLexicalPhrase());
    }
  }

  /**
   * Applies the specified type hints to the given member set, and returns the resulting set. The original set is not modified.
   * @param members - the set of members to filter
   * @param typeHint - a hint about the type of the member, or null for no hint
   * @param returnTypeHint - a hint about the return type of the member (which only applies if isFunctionHint == true), or null for no hint
   * @param isFunctionHint - true to hint that the result should have a function type, false to not hint anything
   * @param isAssignableHint - true to hint that the result should be an assignable member, false to not hint anything
   * @return a set of only the Members which match the given hints
   */
  private Set<Member> applyTypeHints(Set<Member> members, Type typeHint, Type returnTypeHint, boolean isFunctionHint, boolean isAssignableHint)
  {
    // TODO: allow members which only match the typeHints and returnTypeHints if the result is being casted (i.e. check canAssign() in reverse, but perform the same checks as the TypeChecker)
    //       (this works, since typeHints and returnTypeHints are only added by casting)
    Set<Member> filtered = new HashSet<Member>(members);
    Iterator<Member> it = filtered.iterator();
    while (it.hasNext())
    {
      Member member = it.next();
      if (isAssignableHint && member instanceof Method)
      {
        it.remove();
        continue;
      }
      if (isFunctionHint)
      {
        if (member instanceof Field && !(((Field) member).getType() instanceof FunctionType))
        {
          it.remove();
          continue;
        }
        if (returnTypeHint != null && member instanceof Method && !returnTypeHint.canAssign(((Method) member).getReturnType()))
        {
          it.remove();
          continue;
        }
      }
      if (typeHint != null)
      {
        if (member instanceof Field && !typeHint.canAssign(((Field) member).getType()))
        {
          it.remove();
          continue;
        }
        if (member instanceof Method)
        {
          Method method = (Method) member;
          Parameter[] parameters = method.getParameters();
          Type[] parameterTypes = new Type[parameters.length];
          for (int i = 0; i < parameters.length; ++i)
          {
            parameterTypes[i] = parameters[i].getType();
          }
          FunctionType functionType = new FunctionType(false, method.isImmutable(), method.getReturnType(), parameterTypes, null);
          if (!typeHint.canAssign(functionType))
          {
            it.remove();
            continue;
          }
        }
      }
    }
    return filtered;
  }

  /**
   * Filters a set of parameter lists based on which lists can be assigned from the specified arguments.
   * If ensureEquivalent is true, then this method will also remove all parameter lists which do not have types equivalent to the argument types.
   * If allowNullable is true, then the equivalency check ignores the nullability of the parameter types.
   * @param paramLists - the set of parameter lists to filter
   * @param arguments - the arguments to filter the parameter lists based on
   * @param ensureEquivalent - true to filter out parameter lists which do not have equivalent types to the arguments, false to just check whether they are assign-compatible
   * @param allowNullable - true to ignore the nullability of the parameter types in the equivalence check, false to check for strict equivalence.
   * @param M - the Member type for the set of entries (this is never actually used)
   */
  private <M extends Member> void filterParameterLists(Set<Entry<Parameter[], M>> paramLists, Expression[] arguments, boolean ensureEquivalent, boolean allowNullable)
  {
    Iterator<Entry<Parameter[], M>> it = paramLists.iterator();
    while (it.hasNext())
    {
      Entry<Parameter[], M> entry = it.next();
      Parameter[] parameters = entry.getKey();
      boolean typesMatch = parameters.length == arguments.length;
      if (typesMatch)
      {
        for (int i = 0; i < parameters.length; i++)
        {
          Type parameterType = parameters[i].getType();
          Type argumentType = arguments[i].getType();
          if (!parameterType.canAssign(argumentType))
          {
            typesMatch = false;
            break;
          }
          if (ensureEquivalent && !(parameterType.isEquivalent(argumentType) ||
                                    (argumentType instanceof NullType && parameterType.isNullable()) ||
                                    (allowNullable && parameterType.isEquivalent(TypeChecker.findTypeWithNullability(argumentType, true)))))
          {
            typesMatch = false;
            break;
          }
        }
      }
      if (!typesMatch)
      {
        it.remove();
      }
    }
  }

  /**
   * Resolves a constructor call from the specified target type and argument list.
   * This method runs the type checker on each of the arguments in order to determine their types.
   * @param typeDefinition - the type which contains the constructor being called
   * @param arguments - the arguments being passed to the constructor
   * @param callerLexicalPhrase - the LexicalPhrase of the caller, to be used in any errors generated
   * @return the Constructor resolved
   * @throws ConceptualException - if there was a conceptual problem resolving the Constructor
   */
  private Constructor resolveConstructor(TypeDefinition typeDefinition, Expression[] arguments, LexicalPhrase callerLexicalPhrase) throws ConceptualException
  {
    Type[] argumentTypes = new Type[arguments.length];
    for (int i = 0; i < arguments.length; ++i)
    {
      argumentTypes[i] = TypeChecker.checkTypes(arguments[i]);
    }
    // resolve the constructor being called
    Collection<Constructor> constructors = typeDefinition.getUniqueConstructors();
    Map<Parameter[], Constructor> parameterLists = new HashMap<Parameter[], Constructor>();
    for (Constructor constructor : constructors)
    {
      parameterLists.put(constructor.getParameters(), constructor);
    }
    filterParameterLists(parameterLists.entrySet(), arguments, false, false);

    // if there are multiple parameter lists, try to narrow it down to one that is equivalent to the argument list
    if (parameterLists.size() > 1)
    {
      Map<Parameter[], Constructor> backupParamLists = new HashMap<Parameter[], Constructor>(parameterLists);

      filterParameterLists(parameterLists.entrySet(), arguments, true, false);

      // if we have filtered out all of the parameter lists, try to narrow it down again, but this time allow nullable versions of argument types for parameters
      if (parameterLists.isEmpty())
      {
        // revert back to the unfiltered one, and refilter with a broader condition
        parameterLists = backupParamLists;
        filterParameterLists(parameterLists.entrySet(), arguments, true, true);
      }
    }

    if (parameterLists.size() > 1)
    {
      throw new ConceptualException("Ambiguous constructor call, there are at least two applicable constructors which take these arguments", callerLexicalPhrase);
    }
    if (!parameterLists.isEmpty())
    {
      return parameterLists.entrySet().iterator().next().getValue();
    }
    // since we failed to resolve the constructor, pick the most relevant one so that the type checker can point out exactly why it failed to match
    Constructor mostRelevantConstructor = null;
    int mostRelevantArgCount = -1;
    for (Constructor constructor : constructors)
    {
      Parameter[] parameters = constructor.getParameters();
      if (parameters.length == arguments.length)
      {
        for (int i = 0; i < parameters.length; ++i)
        {
          if (!parameters[i].getType().canAssign(argumentTypes[i]))
          {
            if (i + 1 > mostRelevantArgCount)
            {
              mostRelevantConstructor = constructor;
              mostRelevantArgCount = i + 1;
            }
            break;
          }
        }
      }
    }
    if (mostRelevantConstructor != null)
    {
      return mostRelevantConstructor;
    }
    if (constructors.size() >= 1)
    {
      return constructors.iterator().next();
    }
    throw new ConceptualException("Cannot create a '" + typeDefinition.getQualifiedName() + "' - it has no constructors", callerLexicalPhrase);
  }

  /**
   * Tries to extract a qualified name from the specified FieldAccessExpression, but fails if it doesn't look EXACTLY like one.
   * @param fieldAccessExpression - the FieldAccessExpression to extract the QName from
   * @return the QName from the specified FieldAccessExpression, or null if it isn't just a QName
   */
  private static QName extractFieldAccessQName(FieldAccessExpression fieldAccessExpression)
  {
    Stack<String> nameStack = new Stack<String>();
    FieldAccessExpression current = fieldAccessExpression;
    while (current != null)
    {
      nameStack.push(current.getFieldName());
      if (current.getBaseExpression() == null || current.getBaseType() != null || current.isNullTraversing())
      {
        return null;
      }
      Expression baseExpression = current.getBaseExpression();
      if (baseExpression instanceof FieldAccessExpression)
      {
        current = (FieldAccessExpression) baseExpression;
      }
      else if (baseExpression instanceof VariableExpression)
      {
        nameStack.push(((VariableExpression) baseExpression).getName());
        String[] names = new String[nameStack.size()];
        for (int i = 0; i < names.length; ++i)
        {
          names[i] = nameStack.pop();
        }
        return new QName(names);
      }
      else
      {
        return null;
      }
    }
    throw new IllegalStateException("Unknown error extracting a QName from a FieldAccessExpression");
  }

  /**
   * Finds all of the nested variables of a block.
   * Before calling this, resolve() must have been called on the compilation unit containing the block.
   * @param block - the block to get all the nested variables of
   * @return a set containing all of the variables defined in this block, including in nested blocks
   */
  public static Set<Variable> getAllNestedVariables(Block block)
  {
    Set<Variable> result = new HashSet<Variable>();
    Deque<Statement> stack = new LinkedList<Statement>();
    stack.push(block);
    while (!stack.isEmpty())
    {
      Statement statement = stack.pop();
      if (statement instanceof Block)
      {
        // add all variables from this block to the result set
        result.addAll(((Block) statement).getVariables());
        for (Statement s : ((Block) statement).getStatements())
        {
          stack.push(s);
        }
      }
      else if (statement instanceof ForStatement)
      {
        ForStatement forStatement = (ForStatement) statement;
        if (forStatement.getInitStatement() != null)
        {
          stack.push(forStatement.getInitStatement());
        }
        if (forStatement.getUpdateStatement() != null)
        {
          stack.push(forStatement.getUpdateStatement());
        }
        stack.push(forStatement.getBlock());
      }
      else if (statement instanceof IfStatement)
      {
        IfStatement ifStatement = (IfStatement) statement;
        stack.push(ifStatement.getThenClause());
        if (ifStatement.getElseClause() != null)
        {
          stack.push(ifStatement.getElseClause());
        }
      }
      else if (statement instanceof WhileStatement)
      {
        stack.push(((WhileStatement) statement).getStatement());
      }
    }
    return result;
  }

}
