package eu.bryants.anthony.plinth.compiler;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompilationUnit;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.compiler.passes.ControlFlowChecker;
import eu.bryants.anthony.plinth.compiler.passes.CycleChecker;
import eu.bryants.anthony.plinth.compiler.passes.InheritanceChecker;
import eu.bryants.anthony.plinth.compiler.passes.NativeNameChecker;
import eu.bryants.anthony.plinth.compiler.passes.Resolver;
import eu.bryants.anthony.plinth.compiler.passes.SpecialTypeHandler;
import eu.bryants.anthony.plinth.compiler.passes.TypeChecker;
import eu.bryants.anthony.plinth.compiler.passes.TypePropagator;

/*
 * Created on 20 Jan 2013
 */

/**
 * @author Anthony Bryant
 */
public class PassManager
{

  private enum Pass
  {
    TOP_LEVEL_RESOLUTION,
    INHERITANCE_CYCLE_CHECKING,
    INHERITANCE_CHECKING,
    RESOLUTION,
    CYCLE_CHECKING,
    TYPE_CHECKING,
    TYPE_PROPAGATION,
    CONTROL_FLOW_CHECKING,
    BITCODE_NATIVE_NAME_CHECKING,
    SOURCE_NATIVE_NAME_CHECKING,
    FINISHED,
    ;

    Pass getNextPass()
    {
      Pass[] values = values();
      for (int i = 1; i < values.length; ++i)
      {
        if (values[i - 1] == this)
        {
          return values[i];
        }
      }
      return null;
    }
  }

  // the set of compilation units which have not yet had their initial processing to resolve their packages and imports
  private CompilationUnit[] compilationUnits;
  private Resolver resolver;
  private String mainTypeName;
  private NativeNameChecker nativeNameChecker;

  private Pass currentPass;
  private Map<Pass, Set<TypeDefinition>> pendingPasses = new EnumMap<Pass, Set<TypeDefinition>>(Pass.class);
  private Map<TypeDefinition, CompilationUnit> typeCompilationUnits = new HashMap<TypeDefinition, CompilationUnit>();
  private boolean doneSpecialTypeChecking;

  private TypeDefinition mainTypeDefinition;

  public PassManager(Resolver resolver, String mainTypeName)
  {
    this.resolver = resolver;
    this.mainTypeName = mainTypeName;
    nativeNameChecker = new NativeNameChecker(mainTypeName != null);
    currentPass = Pass.TOP_LEVEL_RESOLUTION;
    for (Pass pass : Pass.values())
    {
      pendingPasses.put(pass, new HashSet<TypeDefinition>());
    }
  }

  /**
   * Sets the compilation units to run the passes on
   * @param compilationUnits - the compilation units to run the passes on
   */
  public void setCompilationUnits(CompilationUnit[] compilationUnits)
  {
    this.compilationUnits = compilationUnits;
  }

  /**
   * Adds the specified TypeDefinition to the PassManager.
   * TypeDefinitions should always be added to the package hierarchy before they are added to the PassManager.
   * @param typeDefinition - the TypeDefinition to add
   * @throws ConceptualException - if a conceptual error occurs while bringing this TypeDefinition up to speed with the current pass
   */
  public void addTypeDefinition(TypeDefinition typeDefinition) throws ConceptualException
  {
    if (currentPass != Pass.TOP_LEVEL_RESOLUTION)
    {
      // bring this new TypeDefinition up to speed before we return control back to the Resolver
      for (Pass pass : Pass.values())
      {
        if (pass == currentPass)
        {
          break;
        }
        runPass(pass, typeDefinition);
      }
    }
    Set<TypeDefinition> currentPassDefinitions = pendingPasses.get(currentPass);
    currentPassDefinitions.add(typeDefinition);
  }

  /**
   * @return the main TypeDefinition resolved, or null if none was specified or the passes have not yet been run
   */
  public TypeDefinition getMainTypeDefinition()
  {
    return mainTypeDefinition;
  }

  /**
   * Runs all of the passes in order on this PassManager's list of CompilationUnits.
   * @throws ConceptualException - if there is a problem with some of the code being checked
   */
  public void runPasses() throws ConceptualException
  {
    for (CompilationUnit compilationUnit : compilationUnits)
    {
      resolver.resolvePackages(compilationUnit);
    }
    for (CompilationUnit compilationUnit : compilationUnits)
    {
      resolver.resolveImports(compilationUnit);
      for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
      {
        addTypeDefinition(typeDefinition);
        typeCompilationUnits.put(typeDefinition, compilationUnit);
      }
    }
    resolver.resolveSpecialTypes();
    currentPass = Pass.TOP_LEVEL_RESOLUTION;

    while (currentPass != Pass.FINISHED)
    {
      if (!doneSpecialTypeChecking & currentPass == Pass.RESOLUTION)
      {
        // the special type checking pass comes just before the main resolution pass, so perform it now (if we haven't already done it)
        if (mainTypeName != null)
        {
          mainTypeDefinition = resolver.resolveTypeDefinition(new QName(mainTypeName), null);
          SpecialTypeHandler.checkMainMethod(mainTypeDefinition);
        }
        SpecialTypeHandler.verifySpecialTypes();
        doneSpecialTypeChecking = true;
      }

      Set<TypeDefinition> currentPassDefinitions = pendingPasses.get(currentPass);
      while (!currentPassDefinitions.isEmpty())
      {
        TypeDefinition typeDefinition = currentPassDefinitions.iterator().next();
        runPass(currentPass, typeDefinition);

        currentPassDefinitions.remove(typeDefinition);
        Pass nextPass = currentPass.getNextPass();
        if (nextPass != null)
        {
          pendingPasses.get(nextPass).add(typeDefinition);
        }
      }
      currentPass = currentPass.getNextPass();
    }
  }

  private void runPass(Pass pass, TypeDefinition typeDefinition) throws ConceptualException
  {
    switch (pass)
    {
    case TOP_LEVEL_RESOLUTION:
      resolver.resolveTypes(typeDefinition, typeCompilationUnits.get(typeDefinition));
      if (typeCompilationUnits.containsKey(typeDefinition))
      {
        typeDefinition.buildNonStaticMethods();
      }
      break;
    case INHERITANCE_CYCLE_CHECKING:
      if (typeDefinition instanceof ClassDefinition)
      {
        CycleChecker.checkInheritanceCycles((ClassDefinition) typeDefinition);
      }
      if (typeDefinition instanceof InterfaceDefinition)
      {
        CycleChecker.checkInheritanceCycles((InterfaceDefinition) typeDefinition);
      }
      break;
    case INHERITANCE_CHECKING:
      InheritanceChecker.checkInheritedMembers(typeDefinition);
      break;
    case RESOLUTION:
      CompilationUnit compilationUnit = typeCompilationUnits.get(typeDefinition);
      if (compilationUnit != null)
      {
        resolver.resolve(typeDefinition, compilationUnit);
      }
      break;
    case CYCLE_CHECKING:
      if (typeDefinition instanceof CompoundDefinition)
      {
        CycleChecker.checkCompoundTypeFieldCycles((CompoundDefinition) typeDefinition);
      }
      if (typeCompilationUnits.containsKey(typeDefinition))
      {
        CycleChecker.checkConstructorDelegateCycles(typeDefinition);
      }
      break;
    case TYPE_CHECKING:
      if (typeCompilationUnits.containsKey(typeDefinition))
      {
        TypeChecker.checkTypes(typeDefinition);
      }
      break;
    case TYPE_PROPAGATION:
      if (typeCompilationUnits.containsKey(typeDefinition))
      {
        TypePropagator.propagateTypes(typeDefinition);
      }
      break;
    case CONTROL_FLOW_CHECKING:
      if (typeCompilationUnits.containsKey(typeDefinition))
      {
        ControlFlowChecker.checkControlFlow(typeDefinition);
      }
      break;
    case BITCODE_NATIVE_NAME_CHECKING:
      if (!typeCompilationUnits.containsKey(typeDefinition))
      {
        nativeNameChecker.checkNormalNativeNames(typeDefinition);
        nativeNameChecker.checkSpecifiedNativeNames(typeDefinition);
      }
      break;
    case SOURCE_NATIVE_NAME_CHECKING:
      if (typeCompilationUnits.containsKey(typeDefinition))
      {
        nativeNameChecker.checkNormalNativeNames(typeDefinition);
        nativeNameChecker.checkSpecifiedNativeNames(typeDefinition);
      }
      break;
    case FINISHED:
    default:
      break;
    }
  }
}
