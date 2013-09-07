package eu.bryants.anthony.plinth.compiler.passes;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import eu.bryants.anthony.plinth.ast.expression.CreationExpression;
import eu.bryants.anthony.plinth.ast.expression.EqualityExpression;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.plinth.ast.expression.FloatingLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.FunctionCallExpression;
import eu.bryants.anthony.plinth.ast.expression.InlineIfExpression;
import eu.bryants.anthony.plinth.ast.expression.InstanceOfExpression;
import eu.bryants.anthony.plinth.ast.expression.IntegerLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.LogicalExpression;
import eu.bryants.anthony.plinth.ast.expression.MinusExpression;
import eu.bryants.anthony.plinth.ast.expression.NullCoalescingExpression;
import eu.bryants.anthony.plinth.ast.expression.NullLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.ObjectCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.RelationalExpression;
import eu.bryants.anthony.plinth.ast.expression.ShiftExpression;
import eu.bryants.anthony.plinth.ast.expression.StringLiteralExpression;
import eu.bryants.anthony.plinth.ast.expression.SuperVariableExpression;
import eu.bryants.anthony.plinth.ast.expression.ThisExpression;
import eu.bryants.anthony.plinth.ast.expression.TupleExpression;
import eu.bryants.anthony.plinth.ast.expression.TupleIndexExpression;
import eu.bryants.anthony.plinth.ast.expression.VariableExpression;
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
import eu.bryants.anthony.plinth.ast.metadata.MemberReference;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference;
import eu.bryants.anthony.plinth.ast.metadata.MethodReference.Disambiguator;
import eu.bryants.anthony.plinth.ast.metadata.PackageNode;
import eu.bryants.anthony.plinth.ast.metadata.PropertyInitialiser;
import eu.bryants.anthony.plinth.ast.metadata.PropertyReference;
import eu.bryants.anthony.plinth.ast.metadata.Variable;
import eu.bryants.anthony.plinth.ast.misc.ArrayElementAssignee;
import eu.bryants.anthony.plinth.ast.misc.Assignee;
import eu.bryants.anthony.plinth.ast.misc.AutoAssignParameter;
import eu.bryants.anthony.plinth.ast.misc.BlankAssignee;
import eu.bryants.anthony.plinth.ast.misc.CatchClause;
import eu.bryants.anthony.plinth.ast.misc.FieldAssignee;
import eu.bryants.anthony.plinth.ast.misc.Import;
import eu.bryants.anthony.plinth.ast.misc.NormalParameter;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.misc.VariableAssignee;
import eu.bryants.anthony.plinth.ast.statement.AssignStatement;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.statement.BreakStatement;
import eu.bryants.anthony.plinth.ast.statement.ContinueStatement;
import eu.bryants.anthony.plinth.ast.statement.DelegateConstructorStatement;
import eu.bryants.anthony.plinth.ast.statement.ExpressionStatement;
import eu.bryants.anthony.plinth.ast.statement.ForEachStatement;
import eu.bryants.anthony.plinth.ast.statement.ForStatement;
import eu.bryants.anthony.plinth.ast.statement.IfStatement;
import eu.bryants.anthony.plinth.ast.statement.PrefixIncDecStatement;
import eu.bryants.anthony.plinth.ast.statement.ReturnStatement;
import eu.bryants.anthony.plinth.ast.statement.ShorthandAssignStatement;
import eu.bryants.anthony.plinth.ast.statement.Statement;
import eu.bryants.anthony.plinth.ast.statement.ThrowStatement;
import eu.bryants.anthony.plinth.ast.statement.TryStatement;
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
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.ast.type.WildcardType;
import eu.bryants.anthony.plinth.compiler.CoalescedConceptualException;
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
    resolve(SpecialTypeHandler.STRING_TYPE, null, null);
    resolve(SpecialTypeHandler.THROWABLE_TYPE, null, null);
    resolve(SpecialTypeHandler.CAST_ERROR_TYPE, null, null);
    resolve(SpecialTypeHandler.INDEX_ERROR_TYPE, null, null);
    resolve(SpecialTypeHandler.iteratorType, null, null);
    SpecialTypeHandler.iteratorType = new NamedType(false, false, false, SpecialTypeHandler.iteratorType.getResolvedTypeDefinition());
    resolve(SpecialTypeHandler.iterableType, null, null);
    SpecialTypeHandler.iterableType = new NamedType(false, false, false, SpecialTypeHandler.iterableType.getResolvedTypeDefinition());
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
   * Resolves the top level types in the specified type definition (e.g. function parameters and return types, field types),
   * so that they can be used anywhere in statements and expressions later on.
   * @param typeDefinition - the TypeDefinition to resolve the types of
   * @param compilationUnit - the optional CompilationUnit to resolve the types in the context of
   * @throws ConceptualException - if a conceptual problem is encountered during resolution, or a name could not be resolved
   */
  public void resolveTypes(TypeDefinition typeDefinition, CompilationUnit compilationUnit) throws ConceptualException
  {
    CoalescedConceptualException coalescedException = null;
    if (typeDefinition instanceof ClassDefinition)
    {
      ClassDefinition classDefinition = (ClassDefinition) typeDefinition;
      for (TypeParameter typeParameter : classDefinition.getTypeParameters())
      {
        for (Type superType : typeParameter.getSuperTypes())
        {
          try
          {
            resolve(superType, typeDefinition, compilationUnit);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        for (Type subType : typeParameter.getSubTypes())
        {
          try
          {
            resolve(subType, typeDefinition, compilationUnit);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
      }
      NamedType superType = classDefinition.getSuperType();
      if (superType != null)
      {
        try
        {
          resolve(superType, typeDefinition, compilationUnit);
          // make sure the super-type has the right number of type arguments, and that none of them are wildcards, etc.
          TypeChecker.checkSuperType(superType);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      NamedType[] superInterfaceTypes = classDefinition.getSuperInterfaceTypes();
      if (superInterfaceTypes != null)
      {
        for (int i = 0; i < superInterfaceTypes.length; ++i)
        {
          try
          {
            resolve(superInterfaceTypes[i], typeDefinition, compilationUnit);
            // make sure the super-type has the right number of type arguments, and that none of them are wildcards, etc.
            TypeChecker.checkSuperType(superInterfaceTypes[i]);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
      }
    }
    if (typeDefinition instanceof InterfaceDefinition)
    {
      InterfaceDefinition interfaceDefinition = (InterfaceDefinition) typeDefinition;
      for (TypeParameter typeParameter : interfaceDefinition.getTypeParameters())
      {
        for (Type superType : typeParameter.getSuperTypes())
        {
          try
          {
            resolve(superType, typeDefinition, compilationUnit);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        for (Type subType : typeParameter.getSubTypes())
        {
          try
          {
            resolve(subType, typeDefinition, compilationUnit);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
      }
      NamedType[] superInterfaceTypes = interfaceDefinition.getSuperInterfaceTypes();
      if (superInterfaceTypes != null)
      {
        for (int i = 0; i < superInterfaceTypes.length; ++i)
        {
          try
          {
            resolve(superInterfaceTypes[i], typeDefinition, compilationUnit);
            // make sure the super-type has the right number of type arguments, and that none of them are wildcards, etc.
            TypeChecker.checkSuperType(superInterfaceTypes[i]);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
      }
    }
    if (typeDefinition instanceof CompoundDefinition)
    {
      CompoundDefinition compoundDefinition = (CompoundDefinition) typeDefinition;
      for (TypeParameter typeParameter : compoundDefinition.getTypeParameters())
      {
        for (Type superType : typeParameter.getSuperTypes())
        {
          try
          {
            resolve(superType, typeDefinition, compilationUnit);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        for (Type subType : typeParameter.getSubTypes())
        {
          try
          {
            resolve(subType, typeDefinition, compilationUnit);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
      }
    }

    for (Field field : typeDefinition.getFields())
    {
      // resolve the field's type
      Type type = field.getType();
      try
      {
        resolve(type, typeDefinition, compilationUnit);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        continue;
      }

      // make sure the field is not both mutable and final/immutable
      if (field.isMutable())
      {
        // check whether the internals of the field can be altered
        boolean isAlterable = (type instanceof ArrayType    && !((ArrayType)    type).isContextuallyImmutable()) ||
                              (type instanceof NamedType    && !((NamedType)    type).isContextuallyImmutable()) ||
                              (type instanceof ObjectType   && !((ObjectType)   type).isContextuallyImmutable()) ||
                              (type instanceof WildcardType && !((WildcardType) type).isContextuallyImmutable());
        if (field.isFinal() && !isAlterable)
        {
          // the field is both final and not alterable (e.g. a final uint, or a final #Object), so it cannot be mutable
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("A final, immutably-typed field cannot be mutable", field.getLexicalPhrase()));
        }
      }
    }

    for (Property property : typeDefinition.getProperties())
    {
      // resolve the property's type
      Type type = property.getType();
      try
      {
        resolve(type, typeDefinition, compilationUnit);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }

      if (property.getGetterUncheckedThrownTypes() != null)
      {
        for (NamedType thrownType : property.getGetterUncheckedThrownTypes())
        {
          try
          {
            resolve(thrownType, typeDefinition, compilationUnit);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
      }

      if (property.getSetterBlock() != null)
      {
        Parameter setterParameter = property.getSetterParameter();
        if (setterParameter instanceof NormalParameter)
        {
          try
          {
            resolve(property.getSetterParameter().getType(), typeDefinition, compilationUnit);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        else if (setterParameter instanceof AutoAssignParameter)
        {
          AutoAssignParameter autoAssignParameter = (AutoAssignParameter) setterParameter;
          Field f = typeDefinition.getField(autoAssignParameter.getName());
          Property p = typeDefinition.getProperty(autoAssignParameter.getName());
          if (f != null)
          {
            autoAssignParameter.setResolvedVariable(f.isStatic() ? f.getGlobalVariable() : f.getMemberVariable());
          }
          else if (p != null)
          {
            if (p == property)
            {
              autoAssignParameter.setResolvedVariable(p.isStatic() ? p.getBackingGlobalVariable() : p.getBackingMemberVariable());
            }
            else
            {
              autoAssignParameter.setResolvedVariable(p.getPseudoVariable());
            }
          }
          else
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Unable to resolve: " + autoAssignParameter.getName(), autoAssignParameter.getLexicalPhrase()));
          }
        }
        else
        {
          throw new IllegalArgumentException("Unknown Parameter type: " + setterParameter);
        }
        if (property.getSetterUncheckedThrownTypes() != null)
        {
          for (NamedType thrownType : property.getSetterUncheckedThrownTypes())
          {
            try
            {
              resolve(thrownType, typeDefinition, compilationUnit);
            }
            catch (ConceptualException e)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
            }
          }
        }
      }

      if (property.getConstructorBlock() != null)
      {
        Parameter constructorParameter = property.getConstructorParameter();
        if (constructorParameter instanceof NormalParameter)
        {
          try
          {
            resolve(property.getConstructorParameter().getType(), typeDefinition, compilationUnit);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        else if (constructorParameter instanceof AutoAssignParameter)
        {
          AutoAssignParameter autoAssignParameter = (AutoAssignParameter) constructorParameter;
          Field f = typeDefinition.getField(autoAssignParameter.getName());
          Property p = typeDefinition.getProperty(autoAssignParameter.getName());
          if (f != null)
          {
            autoAssignParameter.setResolvedVariable(f.isStatic() ? f.getGlobalVariable() : f.getMemberVariable());
          }
          else if (p != null)
          {
            if (p == property)
            {
              autoAssignParameter.setResolvedVariable(p.isStatic() ? p.getBackingGlobalVariable() : p.getBackingMemberVariable());
            }
            else
            {
              autoAssignParameter.setResolvedVariable(p.getPseudoVariable());
            }
          }
          else
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Unable to resolve: " + autoAssignParameter.getName(), autoAssignParameter.getLexicalPhrase()));
          }
        }
        else
        {
          throw new IllegalArgumentException("Unknown Parameter type: " + constructorParameter);
        }
        if (property.getConstructorUncheckedThrownTypes() != null)
        {
          for (NamedType thrownType : property.getConstructorUncheckedThrownTypes())
          {
            try
            {
              resolve(thrownType, typeDefinition, compilationUnit);
            }
            catch (ConceptualException e)
            {
              coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
            }
          }
        }
      }

      if (coalescedException != null)
      {
        continue;
      }

      if (property.getSetterBlock() != null)
      {
        Parameter setterParameter = property.getSetterParameter();
        if (setterParameter instanceof NormalParameter)
        {
          property.getSetterBlock().addVariable(((NormalParameter) setterParameter).getVariable());
        }
      }
      if (property.getConstructorBlock() != null)
      {
        Parameter constructorParameter = property.getConstructorParameter();
        if (constructorParameter instanceof NormalParameter)
        {
          property.getConstructorBlock().addVariable(((NormalParameter) constructorParameter).getVariable());
        }
      }

      if (typeDefinition.isImmutable() && !property.isStatic() && !property.isFinal() && !property.isSetterImmutable())
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("An instance property in an immutable type must either have an immutable setter, or be marked as final (and thus have no setter)", property.getLexicalPhrase()));
      }
    }

    // note: Field and Property resolution must happen before Constructor and Method resolution
    // this is because Constructors and Methods are disambiguated by their parameters, and
    // AutoAssignParameters take their types from Fields and Properties.
    // if an error happens in Field or Property type resolution, we need to avoid doing Constructor
    // and Method duplicate checking based on potentially-bad types

    Map<String, Constructor> allConstructors = new HashMap<String, Constructor>();
    for (Constructor constructor : typeDefinition.getAllConstructors())
    {
      Block mainBlock = constructor.getBlock();
      StringBuffer disambiguatorBuffer = new StringBuffer();
      if (constructor.getSinceSpecifier() != null)
      {
        disambiguatorBuffer.append(constructor.getSinceSpecifier().getMangledName());
      }
      disambiguatorBuffer.append('_');
      boolean parameterTypeResolveFailed = false;
      Set<String> usedParameterNames = new HashSet<String>();
      for (Parameter parameter : constructor.getParameters())
      {
        if (usedParameterNames.contains(parameter.getName()))
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Duplicate parameter: " + parameter.getName(), parameter.getLexicalPhrase()));
        }
        usedParameterNames.add(parameter.getName());

        if (parameter instanceof NormalParameter)
        {
          NormalParameter normalParameter = (NormalParameter) parameter;
          // mainBlock can be null if we are resolving a bitcode file with no blocks inside it
          if (mainBlock != null)
          {
            mainBlock.addVariable(normalParameter.getVariable());
          }
          try
          {
            resolve(normalParameter.getType(), typeDefinition, compilationUnit);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
            parameterTypeResolveFailed = true;
          }
        }
        else if (parameter instanceof AutoAssignParameter)
        {
          AutoAssignParameter autoAssignParameter = (AutoAssignParameter) parameter;
          Field field = typeDefinition.getField(autoAssignParameter.getName());
          Property property = typeDefinition.getProperty(autoAssignParameter.getName());
          if (field != null)
          {
            autoAssignParameter.setResolvedVariable(field.isStatic() ? field.getGlobalVariable() : field.getMemberVariable());
            // the Field's type may have failed to resolve earlier
            // since this is a difficult thing to check, just pretend that this parameter also failed to resolve if any other errors have happened
            parameterTypeResolveFailed |= coalescedException != null;
          }
          else if (property != null)
          {
            autoAssignParameter.setResolvedVariable(property.getPseudoVariable());
            // the Property's type may have failed to resolve earlier
            // since this is a difficult thing to check, just pretend that this parameter also failed to resolve if any other errors have happened
            parameterTypeResolveFailed |= coalescedException != null;
          }
          else
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Unable to resolve: " + autoAssignParameter.getName(), autoAssignParameter.getLexicalPhrase()));
            parameterTypeResolveFailed = true;
          }
        }
        else
        {
          throw new IllegalArgumentException("Unknown Parameter type: " + parameter);
        }
        if (!parameterTypeResolveFailed)
        {
          disambiguatorBuffer.append(parameter.getType().getMangledName());
        }
      }
      if (!parameterTypeResolveFailed)
      {
        String disambiguator = disambiguatorBuffer.toString();
        Constructor existing = allConstructors.put(disambiguator, constructor);
        if (existing != null)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Duplicate constructor", constructor.getLexicalPhrase()));
        }
      }
      for (NamedType thrownType : constructor.getCheckedThrownTypes())
      {
        try
        {
          resolve(thrownType, typeDefinition, compilationUnit);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      for (NamedType uncheckedThrownType : constructor.getUncheckedThrownTypes())
      {
        try
        {
          resolve(uncheckedThrownType, typeDefinition, compilationUnit);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
    }

    // resolve all method return and parameter types, and check for duplicate methods
    // however, we must allow duplicated static methods if their since specifiers differ, so our map must allow for multiple methods per disambiguator
    Map<Disambiguator, Set<Method>> allMethods = new HashMap<Disambiguator, Set<Method>>();
    for (Method method : typeDefinition.getAllMethods())
    {
      boolean typeResolveFailed = false;
      try
      {
        resolve(method.getReturnType(), typeDefinition, compilationUnit);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        typeResolveFailed = true;
      }
      Block mainBlock = method.getBlock();

      Parameter[] parameters = method.getParameters();
      Set<String> usedParameterNames = new HashSet<String>();
      for (Parameter parameter : parameters)
      {
        if (usedParameterNames.contains(parameter.getName()))
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Duplicate parameter: " + parameter.getName(), parameter.getLexicalPhrase()));
        }
        usedParameterNames.add(parameter.getName());

        if (parameter instanceof NormalParameter)
        {
          NormalParameter normalParameter = (NormalParameter) parameter;
          // mainBlock can be null if we are resolving a bitcode file with no blocks inside it
          if (mainBlock != null)
          {
            mainBlock.addVariable(normalParameter.getVariable());
          }
          try
          {
            resolve(normalParameter.getType(), typeDefinition, compilationUnit);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
            typeResolveFailed = true;
          }
        }
        else if (parameter instanceof AutoAssignParameter)
        {
          AutoAssignParameter autoAssignParameter = (AutoAssignParameter) parameter;
          Field field = typeDefinition.getField(autoAssignParameter.getName());
          Property property = typeDefinition.getProperty(autoAssignParameter.getName());
          if (field != null)
          {
            autoAssignParameter.setResolvedVariable(field.isStatic() ? field.getGlobalVariable() : field.getMemberVariable());
            // the Field's type may have failed to resolve earlier
            // since this is a difficult thing to check, just pretend that this parameter also failed to resolve if any other errors have happened
            typeResolveFailed |= coalescedException != null;
          }
          else if (property != null)
          {
            autoAssignParameter.setResolvedVariable(property.getPseudoVariable());
            // the Property's type may have failed to resolve earlier
            // since this is a difficult thing to check, just pretend that this parameter also failed to resolve if any other errors have happened
            typeResolveFailed |= coalescedException != null;
          }
          else
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Unable to resolve: " + autoAssignParameter.getName(), autoAssignParameter.getLexicalPhrase()));
            typeResolveFailed = true;
          }
        }
        else
        {
          throw new IllegalArgumentException("Unknown Parameter type: " + parameter);
        }
      }
      for (NamedType thrownType : method.getCheckedThrownTypes())
      {
        try
        {
          resolve(thrownType, typeDefinition, compilationUnit);
        }
        catch (ConceptualException e)
        {
          // this doesn't count as a failure to resolve the method's type, since it doesn't affect the disambiguator
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      for (NamedType uncheckedThrownType : method.getUncheckedThrownTypes())
      {
        try
        {
          resolve(uncheckedThrownType, typeDefinition, compilationUnit);
        }
        catch (ConceptualException e)
        {
          // this doesn't count as a failure to resolve the method's type, since it doesn't affect the disambiguator
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }

      if (!typeResolveFailed)
      {
        MethodReference methodReference = new MethodReference(method, GenericTypeSpecialiser.IDENTITY_SPECIALISER);
        Set<Method> methodSet = allMethods.get(methodReference.getDisambiguator());
        if (methodSet == null)
        {
          methodSet = new HashSet<Method>();
          allMethods.put(methodReference.getDisambiguator(), methodSet);
        }
        if (methodSet.isEmpty())
        {
          methodSet.add(method);
        }
        else
        {
          // there is already a method with this disambiguator
          if (!method.isStatic())
          {
            // disallow all duplicates for non-static methods (this works because Disambiguators take staticness into account)
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Duplicate non-static method: " + method.getName(), method.getLexicalPhrase()));
          }
          else
          {
            // for static methods, we only allow another method if it has a different since specifier from all of the existing ones
            SinceSpecifier newSpecifier = method.getSinceSpecifier();
            for (Method existing : methodSet)
            {
              SinceSpecifier currentSpecifier = existing.getSinceSpecifier();
              if (newSpecifier == null ? currentSpecifier == null : newSpecifier.compareTo(currentSpecifier) == 0)
              {
                coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Duplicate static method: " + method.getName(), method.getLexicalPhrase()));
                break;
              }
            }
            // no methods exist with the same since specifier, so add the new one
            methodSet.add(method);
          }
        }
      }
    }

    if (coalescedException != null)
    {
      throw coalescedException;
    }
  }

  /**
   * Tries to resolve the specified QName to a TypeDefinition, from the context of the root package.
   * @param qname - the QName to resolve
   * @return the TypeDefinition resolved
   * @throws NameNotResolvedException - if the QName cannot be resolved
   * @throws ConceptualException - if a conceptual error occurs while resolving the TypeDefinition
   */
  public TypeDefinition resolveTypeDefinition(QName qname) throws NameNotResolvedException, ConceptualException
  {
    NamedType namedType = new NamedType(false, false, qname, null, qname.getLexicalPhrase());
    resolve(namedType, null, null);
    return namedType.getResolvedTypeDefinition();
  }

  /**
   * Resolves all of the method bodies, field assignments, etc. in the specified TypeDefinition.
   * @param typeDefinition - the TypeDefinition to resolve
   * @param compilationUnit - the CompilationUnit that the TypeDefinition was defined in
   * @throws ConceptualException - if a conceptual error occurs while resolving the TypeDefinition, or a QName cannot be resolved
   */
  public void resolve(TypeDefinition typeDefinition, CompilationUnit compilationUnit) throws ConceptualException
  {
    CoalescedConceptualException coalescedException = null;
    // a non-static initialiser is an immutable context if there is at least one immutable constructor
    // so we need to check whether there are any immutable constructors here
    boolean hasImmutableConstructors = false;
    for (Constructor constructor : typeDefinition.getAllConstructors())
    {
      if (constructor.isImmutable())
      {
        hasImmutableConstructors = true;
      }
      try
      {
        resolveTopLevelBlock(constructor.getBlock(), typeDefinition, compilationUnit, false, constructor.isImmutable(), false, null);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
    }
    for (Initialiser initialiser : typeDefinition.getInitialisers())
    {
      if (initialiser instanceof FieldInitialiser)
      {
        Field field = ((FieldInitialiser) initialiser).getField();
        try
        {
          resolve(field.getInitialiserExpression(), initialiser.getBlock(), typeDefinition, compilationUnit, initialiser.isStatic(), !initialiser.isStatic() & hasImmutableConstructors, null);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      else if (initialiser instanceof PropertyInitialiser)
      {
        Property property = ((PropertyInitialiser) initialiser).getProperty();
        try
        {
          resolve(property.getInitialiserExpression(), initialiser.getBlock(), typeDefinition, compilationUnit, initialiser.isStatic(), !initialiser.isStatic() & hasImmutableConstructors, null);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      else
      {
        try
        {
          resolveTopLevelBlock(initialiser.getBlock(), typeDefinition, compilationUnit, initialiser.isStatic(), !initialiser.isStatic() & hasImmutableConstructors, false, null);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
    }
    for (Property property : typeDefinition.getProperties())
    {
      if (property.getGetterBlock() != null)
      {
        try
        {
          // a getter can only return against contextual immutability if:
          // 1. the property is not mutable (a mutable property never makes a result contextually immutable)
          // 2. it is immutable itself (otherwise being able to do this would be pointless, as nothing would be contextually immutable anyway)
          boolean canReturnAgainstContextualImmutability = !property.isMutable() && property.isGetterImmutable();
          resolveTopLevelBlock(property.getGetterBlock(), typeDefinition, compilationUnit, property.isStatic(), property.isGetterImmutable(), canReturnAgainstContextualImmutability, property);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (property.getSetterBlock() != null)
      {
        try
        {
          resolveTopLevelBlock(property.getSetterBlock(), typeDefinition, compilationUnit, property.isStatic(), property.isSetterImmutable(), false, property);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (property.getConstructorBlock() != null)
      {
        try
        {
          resolveTopLevelBlock(property.getConstructorBlock(), typeDefinition, compilationUnit, property.isStatic(), property.isConstructorImmutable(), false, property);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
    }
    for (Method method : typeDefinition.getAllMethods())
    {
      if (method.getBlock() != null)
      {
        try
        {
          resolveTopLevelBlock(method.getBlock(), typeDefinition, compilationUnit, method.isStatic(), method.isImmutable(), false, null);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
    }

    if (coalescedException != null)
    {
      throw coalescedException;
    }
  }

  private void resolve(Type type, TypeDefinition enclosingDefinition, CompilationUnit compilationUnit) throws NameNotResolvedException, ConceptualException
  {
    if (type instanceof ArrayType)
    {
      resolve(((ArrayType) type).getBaseType(), enclosingDefinition, compilationUnit);
    }
    else if (type instanceof FunctionType)
    {
      FunctionType functionType = (FunctionType) type;
      CoalescedConceptualException coalescedException = null;
      for (Type parameterType : functionType.getParameterTypes())
      {
        try
        {
          resolve(parameterType, enclosingDefinition, compilationUnit);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      try
      {
        resolve(functionType.getReturnType(), enclosingDefinition, compilationUnit);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      for (Type thrownType : functionType.getThrownTypes())
      {
        try
        {
          resolve(thrownType, enclosingDefinition, compilationUnit);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (type instanceof NamedType)
    {
      NamedType namedType = (NamedType) type;
      if (namedType.getResolvedTypeDefinition() != null || namedType.getResolvedTypeParameter() != null)
      {
        return;
      }

      String[] names = namedType.getQualifiedName().getNames();
      // start by looking up the first name in the current type's TypeParameters
      if (enclosingDefinition != null)
      {
        TypeParameter[] typeParameters = null;
        if (enclosingDefinition instanceof ClassDefinition)
        {
          typeParameters = ((ClassDefinition) enclosingDefinition).getTypeParameters();
        }
        else if (enclosingDefinition instanceof InterfaceDefinition)
        {
          typeParameters = ((InterfaceDefinition) enclosingDefinition).getTypeParameters();
        }
        else if (enclosingDefinition instanceof CompoundDefinition)
        {
          typeParameters = ((CompoundDefinition) enclosingDefinition).getTypeParameters();
        }
        if (typeParameters != null)
        {
          for (TypeParameter typeParameter : typeParameters)
          {
            if (names[0].equals(typeParameter.getName()))
            {
              if (names.length > 1)
              {
                throw new NameNotResolvedException("Unable to resolve '" + names[1] + "': type parameters do not have sub-types", namedType.getLexicalPhrase());
              }
              namedType.setResolvedTypeParameter(typeParameter);
              return;
            }
          }
        }
      }

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
      if (namedType.getTypeArguments() != null)
      {
        CoalescedConceptualException coalescedException = null;
        for (Type typeArgument : namedType.getTypeArguments())
        {
          try
          {
            resolve(typeArgument, enclosingDefinition, compilationUnit);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        if (coalescedException != null)
        {
          throw coalescedException;
        }
      }
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
      CoalescedConceptualException coalescedException = null;
      for (Type subType : tupleType.getSubTypes())
      {
        try
        {
          resolve(subType, enclosingDefinition, compilationUnit);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (type instanceof VoidType)
    {
      // do nothing
    }
    else if (type instanceof WildcardType)
    {
      WildcardType wildcardType = (WildcardType) type;
      for (Type superType : wildcardType.getSuperTypes())
      {
        resolve(superType, enclosingDefinition, compilationUnit);
      }
      for (Type subType : wildcardType.getSubTypes())
      {
        resolve(subType, enclosingDefinition, compilationUnit);
      }
    }
    else
    {
      throw new IllegalArgumentException("Unknown Type type: " + type);
    }
  }

  private void resolveTopLevelBlock(Block block, TypeDefinition enclosingDefinition, CompilationUnit compilationUnit, boolean inStaticContext, boolean inImmutableContext, boolean canReturnAgainstContextualImmutability, Property enclosingProperty) throws ConceptualException
  {
    CoalescedConceptualException coalescedException = null;
    for (Statement statement : block.getStatements())
    {
      try
      {
        resolve(statement, block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, canReturnAgainstContextualImmutability, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
    }
    if (coalescedException != null)
    {
      throw coalescedException;
    }
  }

  private void resolve(Statement statement, Block enclosingBlock, TypeDefinition enclosingDefinition, CompilationUnit compilationUnit, boolean inStaticContext, boolean inImmutableContext, boolean canReturnAgainstContextualImmutability, Property enclosingProperty) throws ConceptualException
  {
    if (statement instanceof AssignStatement)
    {
      AssignStatement assignStatement = (AssignStatement) statement;
      Type type = assignStatement.getType();
      if (type != null)
      {
        resolve(type, enclosingDefinition, compilationUnit);
      }
      CoalescedConceptualException coalescedException = null;
      Assignee[] assignees = assignStatement.getAssignees();
      boolean distributedTupleType = type != null && type instanceof TupleType && !type.canBeNullable() && ((TupleType) type).getSubTypes().length == assignees.length;
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
          MemberReference<?> memberReference = null;
          if (variable == null && enclosingDefinition != null)
          {
            // we haven't got a declared variable, so try to resolve it outside the block
            Field field = null;
            Property property = null;
            for (final NamedType superType : enclosingDefinition.getInheritanceLinearisation())
            {
              TypeDefinition superTypeDefinition = superType.getResolvedTypeDefinition();
              // note: this allows static fields from the superclass to be resolved, which is possible inside the class itself, but not by specifying an explicit type
              field = superTypeDefinition.getField(variableAssignee.getVariableName());
              property = superTypeDefinition.getProperty(variableAssignee.getVariableName());
              if (field != null || property != null)
              {
                GenericTypeSpecialiser genericTypeSpecialiser = new GenericTypeSpecialiser(superType);
                if (field != null)
                {
                  memberReference = new FieldReference(field, genericTypeSpecialiser);
                }
                else // property != null
                {
                  memberReference = new PropertyReference(property, genericTypeSpecialiser);
                }
                break;
              }
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
            else if (property != null)
            {
              if (property == enclosingProperty)
              {
                if (property.isStatic())
                {
                  variable = property.getBackingGlobalVariable();
                }
                else
                {
                  variable = property.getBackingMemberVariable();
                }
              }
              else
              {
                variable = property.getPseudoVariable();
              }
            }
          }
          if (variable == null)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new NameNotResolvedException("Unable to resolve: " + variableAssignee.getVariableName(), variableAssignee.getLexicalPhrase()));
          }
          else
          {
            variableAssignee.setResolvedVariable(variable);
            variableAssignee.setResolvedMemberReference(memberReference);
          }
        }
        else if (assignees[i] instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignees[i];
          try
          {
            resolve(arrayElementAssignee.getArrayExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
          try
          {
            resolve(arrayElementAssignee.getDimensionExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        else if (assignees[i] instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignees[i];
          fieldAssignee.getFieldAccessExpression().setIsAssignableHint(true);
          // use the expression resolver to resolve the contained field access expression
          try
          {
            resolve(fieldAssignee.getFieldAccessExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
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
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("'" + variableAssignee.getVariableName() + "' has already been declared, and cannot be redeclared", variableAssignee.getLexicalPhrase()));
        }
        else
        {
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
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("The variables " + buffer + " have all already been declared, and cannot be redeclared", assignStatement.getLexicalPhrase()));
        }
      }
      if (assignStatement.getExpression() != null)
      {
        try
        {
          resolve(assignStatement.getExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (statement instanceof Block)
    {
      Block subBlock = (Block) statement;
      for (Variable v : enclosingBlock.getVariables())
      {
        subBlock.addVariable(v);
      }
      CoalescedConceptualException coalescedException = null;
      for (Statement s : subBlock.getStatements())
      {
        try
        {
          resolve(s, subBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, canReturnAgainstContextualImmutability, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
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
      CoalescedConceptualException coalescedException = null;
      Expression[] arguments = delegateConstructorStatement.getArguments();
      for (Expression argument : arguments)
      {
        try
        {
          resolve(argument, enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      NamedType constructorType;
      if (delegateConstructorStatement.isSuperConstructor())
      {
        if (enclosingDefinition instanceof CompoundDefinition)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot call a super(...) constructor from a compound type", delegateConstructorStatement.getLexicalPhrase()));
          throw coalescedException;
        }
        else if (!(enclosingDefinition instanceof ClassDefinition))
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("A super(...) constructor can only be called from inside a class definition", delegateConstructorStatement.getLexicalPhrase()));
          throw coalescedException;
        }
        constructorType = ((ClassDefinition) enclosingDefinition).getSuperType();
      }
      else
      {
        // create a NamedType from enclosingDefinition
        TypeParameter[] typeParameters = enclosingDefinition.getTypeParameters();
        Type[] typeArguments = null;
        if (typeParameters != null)
        {
          typeArguments = new Type[typeParameters.length];
          for (int i = 0; i < typeParameters.length; ++i)
          {
            typeArguments[i] = new NamedType(false, false, false, typeParameters[i]);
          }
        }
        constructorType = new NamedType(false, false, enclosingDefinition, typeArguments);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
      if (constructorType == null)
      {
        // if the resolved type definition is null, it means that the object constructor should be called (which is a no-op, but runs the initialiser)
        delegateConstructorStatement.setResolvedConstructorReference(null);
      }
      else
      {
        ConstructorReference resolvedConstructor = resolveConstructor(constructorType, arguments, delegateConstructorStatement.getLexicalPhrase(), enclosingDefinition, inStaticContext);
        delegateConstructorStatement.setResolvedConstructorReference(resolvedConstructor);
      }
      // if there was no matching constructor, the resolved constructor call may not type check
      // in this case, we should point out this error before we run the cycle checker, because the cycle checker could find that the constructor is recursive
      // so run the type checker on this statement now
      TypeChecker.checkTypes(delegateConstructorStatement, null, enclosingDefinition, inStaticContext); // give a null return type here, since the type checker will not need to use it
    }
    else if (statement instanceof ExpressionStatement)
    {
      resolve(((ExpressionStatement) statement).getExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
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
      CoalescedConceptualException coalescedException = null;
      if (init != null)
      {
        try
        {
          resolve(init, block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, canReturnAgainstContextualImmutability, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (condition != null)
      {
        try
        {
          resolve(condition, block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (update != null)
      {
        try
        {
          resolve(update, block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, canReturnAgainstContextualImmutability, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      for (Statement s : block.getStatements())
      {
        try
        {
          resolve(s, block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, canReturnAgainstContextualImmutability, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (statement instanceof ForEachStatement)
    {
      ForEachStatement forEachStatement = (ForEachStatement) statement;
      CoalescedConceptualException coalescedException = null;

      // resolve the iterable expression, outside the for loop's block
      try
      {
        resolve(forEachStatement.getIterableExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }

      // populate the block with the variables from its enclosing block
      Block block = forEachStatement.getBlock();
      for (Variable v : enclosingBlock.getVariables())
      {
        block.addVariable(v);
      }

      // resolve the variable's type
      // if this throws an exception, do not try to add the variable to the for loop's block, as its type will be invalid
      Type variableType = forEachStatement.getVariableType();
      try
      {
        resolve(variableType, enclosingDefinition, compilationUnit);

        Variable variable = new Variable(forEachStatement.isVariableFinal(), variableType, forEachStatement.getVariableName());
        forEachStatement.setResolvedVariable(variable);

        Variable oldVariable = block.addVariable(variable);
        if (oldVariable != null)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("'" + variable.getName() + "' has already been declared, and cannot be redeclared", forEachStatement.getLexicalPhrase()));
        }
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }

      // resolve the statements in the for loop's block
      for (Statement s : block.getStatements())
      {
        try
        {
          resolve(s, block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, canReturnAgainstContextualImmutability, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }

      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (statement instanceof IfStatement)
    {
      IfStatement ifStatement = (IfStatement) statement;
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(ifStatement.getExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        resolve(ifStatement.getThenClause(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, canReturnAgainstContextualImmutability, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (ifStatement.getElseClause() != null)
      {
        try
        {
          resolve(ifStatement.getElseClause(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, canReturnAgainstContextualImmutability, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
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
        MemberReference<?> memberReference = null;
        if (variable == null && enclosingDefinition != null)
        {
          Field field = null;
          Property property = null;
          for (final NamedType superType : enclosingDefinition.getInheritanceLinearisation())
          {
            TypeDefinition superTypeDefinition = superType.getResolvedTypeDefinition();
            // note: this allows static fields from the superclass to be resolved, which is possible inside the class itself, but not by specifying an explicit type
            field = superTypeDefinition.getField(variableAssignee.getVariableName());
            property = superTypeDefinition.getProperty(variableAssignee.getVariableName());
            if (field != null || property != null)
            {
              GenericTypeSpecialiser genericTypeSpecialiser = new GenericTypeSpecialiser(superType);
              if (field != null)
              {
                memberReference = new FieldReference(field, genericTypeSpecialiser);
              }
              else // property != null
              {
                memberReference = new PropertyReference(property, genericTypeSpecialiser);
              }
              break;
            }
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
          else if (property != null)
          {
            if (property == enclosingProperty)
            {
              if (property.isStatic())
              {
                variable = property.getBackingGlobalVariable();
              }
              else
              {
                variable = property.getBackingMemberVariable();
              }
            }
            else
            {
              variable = property.getPseudoVariable();
            }
          }
        }
        if (variable == null)
        {
          throw new NameNotResolvedException("Unable to resolve: " + variableAssignee.getVariableName(), variableAssignee.getLexicalPhrase());
        }
        variableAssignee.setResolvedVariable(variable);
        variableAssignee.setResolvedMemberReference(memberReference);
      }
      else if (assignee instanceof ArrayElementAssignee)
      {
        ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
        CoalescedConceptualException coalescedException = null;
        try
        {
          resolve(arrayElementAssignee.getArrayExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
        try
        {
          resolve(arrayElementAssignee.getDimensionExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
        if (coalescedException != null)
        {
          throw coalescedException;
        }
      }
      else if (assignee instanceof BlankAssignee)
      {
        throw new ConceptualException("Cannot " + (prefixIncDecStatement.isIncrement() ? "inc" : "dec") + "rement a blank assignee", assignee.getLexicalPhrase());
      }
      else if (assignee instanceof FieldAssignee)
      {
        FieldAssignee fieldAssignee = (FieldAssignee) assignee;
        // use the expression resolver to resolve the contained field access expression
        resolve(fieldAssignee.getFieldAccessExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      else
      {
        throw new IllegalStateException("Unknown Assignee type: " + assignee);
      }
    }
    else if (statement instanceof ReturnStatement)
    {
      ReturnStatement returnStatement = (ReturnStatement) statement;
      returnStatement.setCanReturnAgainstContextualImmutability(canReturnAgainstContextualImmutability);
      Expression returnedExpression = returnStatement.getExpression();
      if (returnedExpression != null)
      {
        resolve(returnedExpression, enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
    }
    else if (statement instanceof ShorthandAssignStatement)
    {
      ShorthandAssignStatement shorthandAssignStatement = (ShorthandAssignStatement) statement;
      CoalescedConceptualException coalescedException = null;
      for (Assignee assignee : shorthandAssignStatement.getAssignees())
      {
        if (assignee instanceof VariableAssignee)
        {
          VariableAssignee variableAssignee = (VariableAssignee) assignee;
          Variable variable = enclosingBlock.getVariable(variableAssignee.getVariableName());
          MemberReference<?> memberReference = null;
          if (variable == null && enclosingDefinition != null)
          {
            Field field = null;
            Property property = null;
            for (final NamedType superType : enclosingDefinition.getInheritanceLinearisation())
            {
              TypeDefinition superTypeDefinition = superType.getResolvedTypeDefinition();
              // note: this allows static fields from the superclass to be resolved, which is possible inside the class itself, but not by specifying an explicit type
              field = superTypeDefinition.getField(variableAssignee.getVariableName());
              property = superTypeDefinition.getProperty(variableAssignee.getVariableName());
              if (field != null || property != null)
              {
                GenericTypeSpecialiser genericTypeSpecialiser = new GenericTypeSpecialiser(superType);
                if (field != null)
                {
                  memberReference = new FieldReference(field, genericTypeSpecialiser);
                }
                else // property != null
                {
                  memberReference = new PropertyReference(property, genericTypeSpecialiser);
                }
                break;
              }
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
            else if (property != null)
            {
              if (property == enclosingProperty)
              {
                if (property.isStatic())
                {
                  variable = property.getBackingGlobalVariable();
                }
                else
                {
                  variable = property.getBackingMemberVariable();
                }
              }
              else
              {
                variable = property.getPseudoVariable();
              }
            }
          }
          if (variable == null)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, new NameNotResolvedException("Unable to resolve: " + variableAssignee.getVariableName(), variableAssignee.getLexicalPhrase()));
          }
          else
          {
            variableAssignee.setResolvedVariable(variable);
            variableAssignee.setResolvedMemberReference(memberReference);
          }
        }
        else if (assignee instanceof ArrayElementAssignee)
        {
          ArrayElementAssignee arrayElementAssignee = (ArrayElementAssignee) assignee;
          try
          {
            resolve(arrayElementAssignee.getArrayExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
          try
          {
            resolve(arrayElementAssignee.getDimensionExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
        else if (assignee instanceof FieldAssignee)
        {
          FieldAssignee fieldAssignee = (FieldAssignee) assignee;
          // use the expression resolver to resolve the contained field access expression
          try
          {
            resolve(fieldAssignee.getFieldAccessExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
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
      try
      {
        resolve(shorthandAssignStatement.getExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (statement instanceof ThrowStatement)
    {
      resolve(((ThrowStatement) statement).getThrownExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
    }
    else if (statement instanceof TryStatement)
    {
      TryStatement tryStatement = (TryStatement) statement;
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(tryStatement.getTryBlock(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, canReturnAgainstContextualImmutability, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }

      for (CatchClause catchClause : tryStatement.getCatchClauses())
      {
        CoalescedConceptualException subCoalescedException = null;
        // process the block ourselves instead of recursing, as we need to add the exception variable ourselves

        // resolve the type of the caught variable
        for (Type t : catchClause.getCaughtTypes())
        {
          try
          {
            resolve(t, enclosingDefinition, compilationUnit);
          }
          catch (ConceptualException e)
          {
            subCoalescedException = CoalescedConceptualException.coalesce(subCoalescedException, e);
          }
        }
        if (subCoalescedException != null)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, subCoalescedException);
          continue;
        }
        Type variableType;
        try
        {
          variableType = TypeChecker.checkCatchClauseTypes(catchClause.getCaughtTypes(), enclosingDefinition, inStaticContext);
        }
        catch (ConceptualException e)
        {
          subCoalescedException = CoalescedConceptualException.coalesce(subCoalescedException, e);
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, subCoalescedException);
          continue;
        }

        // create the new variable, and set up the variables of the catch block
        Variable variable = new Variable(catchClause.isVariableFinal(), variableType, catchClause.getVariableName());
        catchClause.setResolvedExceptionVariable(variable);
        Block catchBlock = catchClause.getBlock();
        for (Variable v : enclosingBlock.getVariables())
        {
          catchBlock.addVariable(v);
        }
        Variable oldVar = catchBlock.addVariable(variable);
        if (oldVar != null)
        {
          subCoalescedException = CoalescedConceptualException.coalesce(subCoalescedException, new ConceptualException("'" + variable.getName() + "' has already been declared, and cannot be redeclared", catchClause.getLexicalPhrase()));
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, subCoalescedException);
          continue;
        }

        // resolve the contents of the catch block
        for (Statement s : catchBlock.getStatements())
        {
          try
          {
            resolve(s, catchBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, canReturnAgainstContextualImmutability, enclosingProperty);
          }
          catch (ConceptualException e)
          {
            subCoalescedException = CoalescedConceptualException.coalesce(subCoalescedException, e);
          }
        }
        if (subCoalescedException != null)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, subCoalescedException);
          continue;
        }
      }

      if (tryStatement.getFinallyBlock() != null)
      {
        try
        {
          resolve(tryStatement.getFinallyBlock(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, canReturnAgainstContextualImmutability, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (statement instanceof WhileStatement)
    {
      WhileStatement whileStatement = (WhileStatement) statement;
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(whileStatement.getExpression(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        resolve(whileStatement.getStatement(), enclosingBlock, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, canReturnAgainstContextualImmutability, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else
    {
      throw new IllegalArgumentException("Internal name resolution error: Unknown statement type: " + statement);
    }
  }

  private void resolve(Expression expression, Block block, TypeDefinition enclosingDefinition, CompilationUnit compilationUnit, boolean inStaticContext, boolean inImmutableContext, Property enclosingProperty) throws ConceptualException
  {
    if (expression instanceof ArithmeticExpression)
    {
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(((ArithmeticExpression) expression).getLeftSubExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        resolve(((ArithmeticExpression) expression).getRightSubExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof ArrayAccessExpression)
    {
      ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(arrayAccessExpression.getArrayExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        resolve(arrayAccessExpression.getDimensionExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof ArrayCreationExpression)
    {
      ArrayCreationExpression creationExpression = (ArrayCreationExpression) expression;
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(creationExpression.getDeclaredType(), enclosingDefinition, compilationUnit);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (creationExpression.getDimensionExpressions() != null)
      {
        for (Expression expr : creationExpression.getDimensionExpressions())
        {
          try
          {
            resolve(expr, block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
      }
      if (creationExpression.getValueExpressions() != null)
      {
        for (Expression expr : creationExpression.getValueExpressions())
        {
          try
          {
            resolve(expr, block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
          }
          catch (ConceptualException e)
          {
            coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
          }
        }
      }
      if (creationExpression.getInitialisationExpression() != null)
      {
        try
        {
          resolve(creationExpression.getInitialisationExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof BitwiseNotExpression)
    {
      resolve(((BitwiseNotExpression) expression).getExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
    }
    else if (expression instanceof BooleanLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof BooleanNotExpression)
    {
      resolve(((BooleanNotExpression) expression).getExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
    }
    else if (expression instanceof BracketedExpression)
    {
      resolve(((BracketedExpression) expression).getExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
    }
    else if (expression instanceof CastExpression)
    {
      CastExpression castExpression = (CastExpression) expression;
      Type castType = expression.getType();
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(castType, enclosingDefinition, compilationUnit);

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
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }

      try
      {
        resolve(castExpression.getExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof CreationExpression)
    {
      CreationExpression creationExpression = (CreationExpression) expression;
      CoalescedConceptualException coalescedException = null;
      NamedType type = creationExpression.getCreatedType();
      try
      {
        resolve(type, enclosingDefinition, compilationUnit);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (type.getResolvedTypeParameter() != null)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, new ConceptualException("Cannot create an instance of a type parameter", type.getLexicalPhrase()));
      }
      Expression[] arguments = creationExpression.getArguments();
      for (Expression argument : arguments)
      {
        try
        {
          resolve(argument, block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
      ConstructorReference resolvedConstructor = resolveConstructor(type, arguments, creationExpression.getLexicalPhrase(), enclosingDefinition, inStaticContext);
      creationExpression.setResolvedConstructorReference(resolvedConstructor);
    }
    else if (expression instanceof EqualityExpression)
    {
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(((EqualityExpression) expression).getLeftSubExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        resolve(((EqualityExpression) expression).getRightSubExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
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
        resolve(fieldAccessExpression.getBaseExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);

        // find the type of the sub-expression, by calling the type checker
        // this is fine as long as we resolve all of the sub-expression first
        baseType = TypeChecker.checkTypes(fieldAccessExpression.getBaseExpression(), enclosingDefinition, inStaticContext);
        baseIsStatic = false;
      }
      else if (fieldAccessExpression.getBaseType() != null)
      {
        baseType = fieldAccessExpression.getBaseType();
        resolve(baseType, enclosingDefinition, compilationUnit);
        TypeChecker.checkType(baseType, true, enclosingDefinition, inStaticContext);
        baseIsStatic = true;
      }
      else
      {
        throw new IllegalStateException("Unknown base type for a field access: " + fieldAccessExpression);
      }

      Set<MemberReference<?>> memberSet = baseType.getMembers(fieldName);
      Set<MemberReference<?>> staticFiltered = new HashSet<MemberReference<?>>();
      for (MemberReference<?> member : memberSet)
      {
        if (member instanceof ArrayLengthMemberReference)
        {
          if (baseIsStatic)
          {
            throw new ConceptualException("Cannot access the array length member statically", fieldAccessExpression.getLexicalPhrase());
          }
          staticFiltered.add(member);
        }
        else if (member instanceof FieldReference)
        {
          if (((FieldReference) member).getReferencedMember().isStatic() == baseIsStatic)
          {
            staticFiltered.add(member);
          }
        }
        else if (member instanceof PropertyReference)
        {
          if (((PropertyReference) member).getReferencedMember().isStatic() == baseIsStatic)
          {
            staticFiltered.add(member);
          }
        }
        else if (member instanceof MethodReference)
        {
          if (((MethodReference) member).getReferencedMember().isStatic() == baseIsStatic)
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
      MemberReference<?> resolved = null;
      if (staticFiltered.size() == 1)
      {
        resolved = staticFiltered.iterator().next();
      }
      else
      {
        Set<MemberReference<?>> hintFiltered = applyTypeHints(staticFiltered, fieldAccessExpression.getTypeHint(), fieldAccessExpression.getReturnTypeHint(), fieldAccessExpression.getIsFunctionHint(), fieldAccessExpression.getIsAssignableHint());
        if (hintFiltered.size() == 1)
        {
          resolved = hintFiltered.iterator().next();
        }
      }
      if (resolved == null)
      {
        throw new ConceptualException("Multiple " + (baseIsStatic ? "static" : "non-static") + " members have the name '" + fieldName + "'", fieldAccessExpression.getLexicalPhrase());
      }
      fieldAccessExpression.setResolvedMemberReference(resolved);
    }
    else if (expression instanceof FloatingLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof FunctionCallExpression)
    {
      FunctionCallExpression expr = (FunctionCallExpression) expression;
      // resolve all of the sub-expressions
      CoalescedConceptualException coalescedException = null;
      for (Expression e : expr.getArguments())
      {
        try
        {
          resolve(e, block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
          TypeChecker.checkTypes(e, enclosingDefinition, inStaticContext);
        }
        catch (ConceptualException exception)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, exception);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
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
        resolve(functionExpression, block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
        expressionType = TypeChecker.checkTypes(functionExpression, enclosingDefinition, inStaticContext);
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
            if (variableExpression.getResolvedMemberReference() instanceof MethodReference) // "null instanceof Something" is always false
            {
              // the base resolved to a Method, so just resolve this FunctionCallExpression to the same Method
              expr.setResolvedMethodReference((MethodReference) variableExpression.getResolvedMemberReference());
              if (variableExpression instanceof SuperVariableExpression)
              {
                // this function call is of the form 'super.method()', so make it non-virtual
                expr.setResolvedIsVirtual(false);
              }
              return;
            }
          }
          else if (testExpression instanceof FieldAccessExpression)
          {
            FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) testExpression;
            MemberReference<?> resolvedMemberReference = fieldAccessExpression.getResolvedMemberReference();
            if (resolvedMemberReference instanceof MethodReference)
            {
              // the base resolved to a Method, so just resolve this FunctionCallExpression to the same Method
              expr.setResolvedMethodReference((MethodReference) resolvedMemberReference);
              expr.setResolvedBaseExpression(fieldAccessExpression.getBaseExpression()); // this will be null for static field accesses
              expr.setResolvedNullTraversal(fieldAccessExpression.isNullTraversing());
              return;
            }
          }
          expr.setResolvedBaseExpression(functionExpression);
          return;
        }
        throw new ConceptualException("Cannot call a non-function-typed value", functionExpression.getLexicalPhrase());
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

      Map<Type[], MethodReference> paramTypeLists = new LinkedHashMap<Type[], MethodReference>();
      Map<Type[], MethodReference> hintedParamLists = new LinkedHashMap<Type[], MethodReference>();
      Map<MethodReference, Expression> methodBaseExpressions = new HashMap<MethodReference, Expression>();
      boolean isSuperAccess = false;
      if (functionExpression instanceof VariableExpression)
      {
        VariableExpression variableExpression = (VariableExpression) functionExpression;
        isSuperAccess = variableExpression instanceof SuperVariableExpression;
        String name = variableExpression.getName();
        // the sub-expression didn't resolve to a variable or a field, or we would have got a valid type back in expressionType
        if (enclosingDefinition != null)
        {
          Set<MemberReference<?>> memberSet = new NamedType(false, false, false, enclosingDefinition).getMembers(name, !isSuperAccess, isSuperAccess);
          memberSet = memberSet != null ? memberSet : new HashSet<MemberReference<?>>();
          Set<MemberReference<?>> hintedMemberSet = applyTypeHints(memberSet, variableExpression.getTypeHint(), variableExpression.getReturnTypeHint(), variableExpression.getIsFunctionHint(), variableExpression.getIsAssignableHint());
          for (MemberReference<?> m : memberSet)
          {
            if (m instanceof MethodReference)
            {
              MethodReference methodReference = (MethodReference) m;
              Type[] parameterTypes = methodReference.getParameterTypes();
              paramTypeLists.put(parameterTypes, methodReference);
              if (hintedMemberSet.contains(methodReference))
              {
                hintedParamLists.put(parameterTypes, methodReference);
              }
              // leave methodBaseExpressions with a null value for this method, as we have no base expression
            }
          }
        }
      }
      else if (functionExpression instanceof FieldAccessExpression)
      {
        FieldAccessExpression fieldAccessExpression = (FieldAccessExpression) functionExpression;
        expr.setResolvedNullTraversal(fieldAccessExpression.isNullTraversing());

        try
        {
          String name = fieldAccessExpression.getFieldName();

          Expression baseExpression = fieldAccessExpression.getBaseExpression();
          Type baseType;
          boolean baseIsStatic;
          if (baseExpression != null)
          {
            resolve(baseExpression, block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);

            // find the type of the sub-expression, by calling the type checker
            // this is fine as long as we resolve all of the sub-expression first
            baseType = TypeChecker.checkTypes(baseExpression, enclosingDefinition, inStaticContext);
            baseIsStatic = false;
          }
          else if (fieldAccessExpression.getBaseType() != null)
          {
            baseType = fieldAccessExpression.getBaseType();
            resolve(baseType, enclosingDefinition, compilationUnit);
            TypeChecker.checkType(baseType, true, enclosingDefinition, inStaticContext);
            baseIsStatic = true;
          }
          else
          {
            throw new IllegalStateException("Unknown base type for a field access: " + fieldAccessExpression);
          }

          Set<MemberReference<?>> memberSet = baseType.getMembers(name);
          memberSet = memberSet != null ? memberSet : new HashSet<MemberReference<?>>();
          Set<MemberReference<?>> hintedMemberSet = applyTypeHints(memberSet, fieldAccessExpression.getTypeHint(), fieldAccessExpression.getReturnTypeHint(), fieldAccessExpression.getIsFunctionHint(), fieldAccessExpression.getIsAssignableHint());
          for (MemberReference<?> member : memberSet)
          {
            // only allow access to this method if it is called in the right way, depending on whether or not it is static
            if (member instanceof MethodReference && ((MethodReference) member).getReferencedMember().isStatic() == baseIsStatic)
            {
              MethodReference methodReference = (MethodReference) member;
              paramTypeLists.put(methodReference.getParameterTypes(), methodReference);
              if (hintedMemberSet.contains(methodReference))
              {
                hintedParamLists.put(methodReference.getParameterTypes(), methodReference);
              }
              methodBaseExpressions.put(methodReference, baseExpression);
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
        // first, try filtering for argument type equivalence, but ignoring nullability
        Map<Type[], MethodReference> equivalenceFilteredHintedParamLists = new LinkedHashMap<Type[], MethodReference>(hintedParamLists);
        filterParameterLists(equivalenceFilteredHintedParamLists.entrySet(), expr.getArguments(), true, true);

        if (!equivalenceFilteredHintedParamLists.isEmpty())
        {
          hintedParamLists = equivalenceFilteredHintedParamLists;
          if (hintedParamLists.size() > 1)
          {
            // the equivalence filter was not enough, so try a nullability filter as well
            Map<Type[], MethodReference> nullabilityFilteredHintedParamLists = new LinkedHashMap<Type[], MethodReference>(hintedParamLists);
            filterParameterLists(nullabilityFilteredHintedParamLists.entrySet(), expr.getArguments(), true, false);

            if (!nullabilityFilteredHintedParamLists.isEmpty())
            {
              hintedParamLists = nullabilityFilteredHintedParamLists;
            }
          }
        }
      }
      if (hintedParamLists.size() == 1)
      {
        paramTypeLists = hintedParamLists;
      }
      else
      {
        // try the same thing without using hintedParamLists
        filterParameterLists(paramTypeLists.entrySet(), expr.getArguments(), false, false);
        if (paramTypeLists.size() > 1)
        {
          Map<Type[], MethodReference> equivalenceFilteredParamTypeLists = new LinkedHashMap<Type[], MethodReference>(paramTypeLists);
          filterParameterLists(equivalenceFilteredParamTypeLists.entrySet(), expr.getArguments(), true, true);

          if (!equivalenceFilteredParamTypeLists.isEmpty())
          {
            paramTypeLists = equivalenceFilteredParamTypeLists;
            if (paramTypeLists.size() > 1)
            {
              Map<Type[], MethodReference> nullabilityFilteredParamTypeLists = new LinkedHashMap<Type[], MethodReference>(paramTypeLists);
              filterParameterLists(nullabilityFilteredParamTypeLists.entrySet(), expr.getArguments(), true, false);

              if (!nullabilityFilteredParamTypeLists.isEmpty())
              {
                paramTypeLists = nullabilityFilteredParamTypeLists;
              }
            }
          }
        }
      }

      if (paramTypeLists.size() > 1)
      {
        throw new ConceptualException("Ambiguous method call, there are at least two applicable methods which take these arguments", expr.getLexicalPhrase());
      }
      if (paramTypeLists.isEmpty())
      {
        // we didn't find anything, so rethrow the exception from earlier
        if (cachedException instanceof NameNotResolvedException)
        {
          throw (NameNotResolvedException) cachedException;
        }
        throw (ConceptualException) cachedException;
      }

      Entry<Type[], MethodReference> entry = paramTypeLists.entrySet().iterator().next();
      expr.setResolvedMethodReference(entry.getValue());
      // if the method call had no base expression, e.g. it was a VariableExpression being called, this will just set it to null
      expr.setResolvedBaseExpression(methodBaseExpressions.get(entry.getValue()));
      if (isSuperAccess)
      {
        // this function call is of the form 'super.method()', so make it non-virtual
        expr.setResolvedIsVirtual(false);
      }
    }
    else if (expression instanceof InlineIfExpression)
    {
      InlineIfExpression inlineIfExpression = (InlineIfExpression) expression;
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(inlineIfExpression.getCondition(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        resolve(inlineIfExpression.getThenExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        resolve(inlineIfExpression.getElseExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof InstanceOfExpression)
    {
      InstanceOfExpression instanceOfExpression = (InstanceOfExpression) expression;
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(instanceOfExpression.getExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        resolve(instanceOfExpression.getInstanceOfType(), enclosingDefinition, compilationUnit);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof IntegerLiteralExpression)
    {
      // do nothing
    }
    else if (expression instanceof LogicalExpression)
    {
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(((LogicalExpression) expression).getLeftSubExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        resolve(((LogicalExpression) expression).getRightSubExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof MinusExpression)
    {
      resolve(((MinusExpression) expression).getExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
    }
    else if (expression instanceof NullCoalescingExpression)
    {
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(((NullCoalescingExpression) expression).getNullableExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        resolve(((NullCoalescingExpression) expression).getAlternativeExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
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
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(((RelationalExpression) expression).getLeftSubExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        resolve(((RelationalExpression) expression).getRightSubExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof ShiftExpression)
    {
      CoalescedConceptualException coalescedException = null;
      try
      {
        resolve(((ShiftExpression) expression).getLeftExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      try
      {
        resolve(((ShiftExpression) expression).getRightExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
      }
      catch (ConceptualException e)
      {
        coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
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
      CoalescedConceptualException coalescedException = null;
      Expression[] subExpressions = tupleExpression.getSubExpressions();
      for (int i = 0; i < subExpressions.length; i++)
      {
        try
        {
          resolve(subExpressions[i], block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
        }
        catch (ConceptualException e)
        {
          coalescedException = CoalescedConceptualException.coalesce(coalescedException, e);
        }
      }
      if (coalescedException != null)
      {
        throw coalescedException;
      }
    }
    else if (expression instanceof TupleIndexExpression)
    {
      TupleIndexExpression indexExpression = (TupleIndexExpression) expression;
      resolve(indexExpression.getExpression(), block, enclosingDefinition, compilationUnit, inStaticContext, inImmutableContext, enclosingProperty);
    }
    else if (expression instanceof VariableExpression)
    {
      VariableExpression expr = (VariableExpression) expression;
      boolean isSuperAccess = expr instanceof SuperVariableExpression;
      expr.setResolvedContextImmutability(inImmutableContext);
      Variable var = block.getVariable(expr.getName());
      if (var != null & !isSuperAccess)
      {
        expr.setResolvedVariable(var);
        return;
      }
      if (enclosingDefinition != null)
      {
        Set<MemberReference<?>> members = new NamedType(false, false, enclosingDefinition, null).getMembers(expr.getName(), !isSuperAccess, isSuperAccess);
        members = members != null ? members : new HashSet<MemberReference<?>>();

        MemberReference<?> resolved = null;
        if (members.size() == 1)
        {
          resolved = members.iterator().next();
        }
        else
        {
          Set<MemberReference<?>> filteredMembers = applyTypeHints(members, expr.getTypeHint(), expr.getReturnTypeHint(), expr.getIsFunctionHint(), expr.getIsAssignableHint());
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
          if (resolved instanceof FieldReference)
          {
            FieldReference fieldReference = (FieldReference) resolved;
            if (fieldReference.getReferencedMember().isStatic())
            {
              var = fieldReference.getReferencedMember().getGlobalVariable();
            }
            else
            {
              var = fieldReference.getReferencedMember().getMemberVariable();
            }
            expr.setResolvedVariable(var);
            expr.setResolvedMemberReference(fieldReference);
            return;
          }
          else if (resolved instanceof PropertyReference)
          {
            PropertyReference propertyReference = (PropertyReference) resolved;
            Property property = propertyReference.getReferencedMember();
            if (property == enclosingProperty)
            {
              if (property.isStatic())
              {
                var = property.getBackingGlobalVariable();
              }
              else
              {
                var = property.getBackingMemberVariable();
              }
            }
            else
            {
              var = property.getPseudoVariable();
            }
            expr.setResolvedVariable(var);
            expr.setResolvedMemberReference(propertyReference);
            return;
          }
          else if (resolved instanceof MethodReference)
          {
            expr.setResolvedMemberReference(resolved);
            return;
          }
          throw new IllegalStateException("Unknown member type: " + resolved);
        }
      }
      throw new NameNotResolvedException("Unable to resolve \"" + (isSuperAccess ? "super." : "") + expr.getName() + "\"", expr.getLexicalPhrase());
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
  private Set<MemberReference<?>> applyTypeHints(Set<MemberReference<?>> members, Type typeHint, Type returnTypeHint, boolean isFunctionHint, boolean isAssignableHint)
  {
    // TODO: allow members which only match the typeHints and returnTypeHints if the result is being casted (i.e. check canAssign() in reverse, but perform the same checks as the TypeChecker)
    //       (this works, since typeHints and returnTypeHints are only added by casting)
    Set<MemberReference<?>> filtered = new HashSet<MemberReference<?>>(members);
    Iterator<MemberReference<?>> it = filtered.iterator();
    while (it.hasNext())
    {
      MemberReference<?> member = it.next();
      if (isAssignableHint && member instanceof MethodReference)
      {
        it.remove();
        continue;
      }
      if (isFunctionHint)
      {
        if (member instanceof FieldReference && !(((FieldReference) member).getType() instanceof FunctionType))
        {
          it.remove();
          continue;
        }
        if (member instanceof PropertyReference && !(((PropertyReference) member).getType() instanceof FunctionType))
        {
          it.remove();
          continue;
        }
        if (returnTypeHint != null && member instanceof MethodReference && !returnTypeHint.canAssign(((MethodReference) member).getReturnType()))
        {
          it.remove();
          continue;
        }
      }
      if (typeHint != null)
      {
        if (member instanceof FieldReference && !typeHint.canAssign(((FieldReference) member).getType()))
        {
          it.remove();
          continue;
        }
        if (member instanceof PropertyReference && !typeHint.canAssign(((PropertyReference) member).getType()))
        {
          it.remove();
          continue;
        }
        if (member instanceof MethodReference)
        {
          MethodReference methodReference = (MethodReference) member;
          FunctionType functionType = new FunctionType(false, methodReference.getReferencedMember().isImmutable(), methodReference.getReturnType(), methodReference.getParameterTypes(), methodReference.getCheckedThrownTypes(), null);
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
   * Filters a set of parameter type lists based on which lists can be assigned from the specified arguments.
   * If ensureEquivalent is true, then this method will also remove all parameter type lists which are not equivalent to the argument types.
   * If allowNullable is true, then the equivalency check ignores the nullability of the parameter types.
   * @param paramTypeLists - the set of parameter type lists to filter
   * @param arguments - the arguments to filter the parameter type lists based on
   * @param ensureEquivalent - true to filter out parameter type lists which do not have equivalent types to the arguments, false to just check whether they are assign-compatible
   * @param allowNullable - true to ignore the nullability of the parameter types in the equivalence check, false to check for strict equivalence
   * @param M - the Member type for the set of entries (this is never actually used)
   */
  private <M extends MemberReference<?>> void filterParameterLists(Set<Entry<Type[], M>> paramTypeLists, Expression[] arguments, boolean ensureEquivalent, boolean allowNullable)
  {
    Iterator<Entry<Type[], M>> it = paramTypeLists.iterator();
    while (it.hasNext())
    {
      Entry<Type[], M> entry = it.next();
      Type[] parameterTypes = entry.getKey();
      boolean typesMatch = parameterTypes.length == arguments.length;
      if (typesMatch)
      {
        for (int i = 0; i < parameterTypes.length; i++)
        {
          Type parameterType = parameterTypes[i];
          Type argumentType = arguments[i].getType();
          if (!parameterType.canAssign(argumentType))
          {
            typesMatch = false;
            break;
          }
          if (ensureEquivalent && !(parameterType.isEquivalent(argumentType) ||
                                    (argumentType instanceof NullType && parameterType.isNullable()) ||
                                    (allowNullable && parameterType.isEquivalent(Type.findTypeWithNullability(argumentType, true)))))
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
   * This method runs the type checker on each of the arguments in order to determine their types, so it needs to know the enclosing TypeDefinition and whether or not we are in a static context.
   * @param creationType - the type which contains the constructor being called
   * @param arguments - the arguments being passed to the constructor
   * @param callerLexicalPhrase - the LexicalPhrase of the caller, to be used in any errors generated
   * @param enclosingDefinition - the TypeDefinition which encloses the call to the constructor
   * @param inStaticContext - true if the constructor call is in a static context, false otherwise
   * @return the ConstructorReference resolved
   * @throws ConceptualException - if there was a conceptual problem resolving the Constructor
   */
  private ConstructorReference resolveConstructor(final NamedType creationType, Expression[] arguments, LexicalPhrase callerLexicalPhrase, TypeDefinition enclosingDefinition, boolean inStaticContext) throws ConceptualException
  {
    Type[] argumentTypes = new Type[arguments.length];
    for (int i = 0; i < arguments.length; ++i)
    {
      argumentTypes[i] = TypeChecker.checkTypes(arguments[i], enclosingDefinition, inStaticContext);
    }
    GenericTypeSpecialiser genericTypeSpecialiser = new GenericTypeSpecialiser(creationType);
    // resolve the constructor being called
    TypeDefinition typeDefinition = creationType.getResolvedTypeDefinition();
    Collection<Constructor> constructors = typeDefinition.getUniqueConstructors();
    ConstructorReference[] constructorReferences = new ConstructorReference[constructors.size()];
    Map<Type[], ConstructorReference> parameterTypeLists = new LinkedHashMap<Type[], ConstructorReference>();
    int index = 0;
    for (Constructor constructor : constructors)
    {
      constructorReferences[index] = new ConstructorReference(constructor, genericTypeSpecialiser);
      parameterTypeLists.put(constructorReferences[index].getParameterTypes(), constructorReferences[index]);
      index++;
    }

    filterParameterLists(parameterTypeLists.entrySet(), arguments, false, false);

    // if there are multiple parameter lists, try to narrow it down to one that is equivalent to the argument list
    if (parameterTypeLists.size() > 1)
    {
      // first, try filtering for argument type equivalence, but ignoring nullability
      Map<Type[], ConstructorReference> equivalenceFiltered = new LinkedHashMap<Type[], ConstructorReference>(parameterTypeLists);
      filterParameterLists(equivalenceFiltered.entrySet(), arguments, true, true);

      if (!equivalenceFiltered.isEmpty())
      {
        parameterTypeLists = equivalenceFiltered;
        if (parameterTypeLists.size() > 1)
        {
          // the equivalence filter was not enough, so try a nullability filter as well
          Map<Type[], ConstructorReference> nullabilityEquivalenceFiltered = new LinkedHashMap<Type[], ConstructorReference>(parameterTypeLists);
          filterParameterLists(nullabilityEquivalenceFiltered.entrySet(), arguments, true, false);

          if (!nullabilityEquivalenceFiltered.isEmpty())
          {
            parameterTypeLists = nullabilityEquivalenceFiltered;
          }
        }
      }
    }

    if (parameterTypeLists.size() > 1)
    {
      throw new ConceptualException("Ambiguous constructor call, there are at least two applicable constructors which take these arguments", callerLexicalPhrase);
    }
    if (!parameterTypeLists.isEmpty())
    {
      return parameterTypeLists.entrySet().iterator().next().getValue();
    }
    // since we failed to resolve the constructor, pick the most relevant one so that the type checker can point out exactly why it failed to match
    ConstructorReference mostRelevantConstructorReference = null;
    int mostRelevantArgCount = -1;
    for (ConstructorReference constructorReference : constructorReferences)
    {
      // try to maximise the index of the first parameter that doesn't match
      Type[] parameterTypes = constructorReference.getParameterTypes();
      if (parameterTypes.length == arguments.length)
      {
        for (int i = 0; i < parameterTypes.length; ++i)
        {
          if (!parameterTypes[i].canAssign(argumentTypes[i]))
          {
            if (i + 1 > mostRelevantArgCount)
            {
              mostRelevantConstructorReference = constructorReference;
              mostRelevantArgCount = i + 1;
            }
            break;
          }
        }
      }
    }
    if (mostRelevantConstructorReference != null)
    {
      return mostRelevantConstructorReference;
    }
    if (constructorReferences.length >= 1)
    {
      return constructorReferences[0];
    }
    throw new ConceptualException("Cannot create '" + typeDefinition.getQualifiedName() + "' - it has no constructors", callerLexicalPhrase);
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
      else if (statement instanceof ForEachStatement)
      {
        stack.push(((ForEachStatement) statement).getBlock());
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
      else if (statement instanceof TryStatement)
      {
        TryStatement tryStatement = (TryStatement) statement;
        stack.push(tryStatement.getTryBlock());
        for (CatchClause catchClause : tryStatement.getCatchClauses())
        {
          stack.push(catchClause.getBlock());
        }
        if (tryStatement.getFinallyBlock() != null)
        {
          stack.push(tryStatement.getFinallyBlock());
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
