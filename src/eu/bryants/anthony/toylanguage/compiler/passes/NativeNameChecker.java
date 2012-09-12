package eu.bryants.anthony.toylanguage.compiler.passes;

import java.util.HashSet;
import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.TypeDefinition;
import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Method;
import eu.bryants.anthony.toylanguage.ast.metadata.GlobalVariable;
import eu.bryants.anthony.toylanguage.compiler.ConceptualException;

/*
 * Created on 6 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class NativeNameChecker
{

  private static final Set<String> FORBIDDEN_NAMES = new HashSet<String>();
  static
  {
    FORBIDDEN_NAMES.add("malloc");
    FORBIDDEN_NAMES.add("free");
  }

  /**
   * Checks that the compilation unit does not have any bad (e.g. duplicated) native names.
   * This ensures that the names do not collide during code generation.
   * @param compilationUnit - the compilation unit to check the native names of
   * @throws ConceptualException - if a bad native name is found
   */
  public static void checkNativeNames(CompilationUnit compilationUnit) throws ConceptualException
  {
    Set<String> usedNativeNames = new HashSet<String>();

    for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
    {
      for (Constructor constructor : typeDefinition.getConstructors())
      {
        checkNativeName(constructor, usedNativeNames);
      }
      for (Field field : typeDefinition.getFields())
      {
        checkNativeName(field, usedNativeNames);
      }
      for (Method method : typeDefinition.getAllMethods())
      {
        checkMangledNativeName(method, usedNativeNames);
      }
    }

    // iterate over the methods a second time, so that we know all of the mangled names before we check the user specified names
    // this should ensure that when we have a duplicate, the name that the user specifies is flagged as an error, not the mangled name of the function
    for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
    {
      for (Method method : typeDefinition.getAllMethods())
      {
        checkSpecifiedNativeName(method, usedNativeNames);
      }
    }
  }

  /**
   * Checks whether the specified Constructor has a forbidden or duplicated native name.
   * @param constructor - the Constructor to check
   * @param usedNativeNames - the set of used native names to check against
   * @throws ConceptualException - if a bad native name is found
   */
  private static void checkNativeName(Constructor constructor, Set<String> usedNativeNames) throws ConceptualException
  {
    checkForbidden(constructor.getMangledName(), constructor.getLexicalPhrase());
    boolean newName = usedNativeNames.add(constructor.getMangledName());
    if (!newName)
    {
      throw new ConceptualException("Duplicate native name: " + constructor.getMangledName(), constructor.getLexicalPhrase());
    }
  }

  /**
   * Checks whether the specified Field has a forbidden or duplicated native name.
   * @param field - the Field to check
   * @param usedNativeNames - the set of used native names to check against
   * @throws ConceptualException - if a bad native name is found
   */
  private static void checkNativeName(Field field, Set<String> usedNativeNames) throws ConceptualException
  {
    if (!field.isStatic())
    {
      return;
    }
    GlobalVariable global = field.getGlobalVariable();
    checkForbidden(global.getMangledName(), field.getLexicalPhrase());
    boolean newName = usedNativeNames.add(global.getMangledName());
    if (!newName)
    {
      throw new ConceptualException("Duplicate native name: " + global.getMangledName(), field.getLexicalPhrase());
    }
  }

  /**
   * Checks whether the specified Method has a forbidden or duplicated mangled native name.
   * @param method - the Method to check
   * @param usedNativeNames - the set of used native names to check against
   * @throws ConceptualException - if a bad native name is found
   */
  private static void checkMangledNativeName(Method method, Set<String> usedNativeNames) throws ConceptualException
  {
    checkForbidden(method.getMangledName(), method.getLexicalPhrase());
    boolean newName = usedNativeNames.add(method.getMangledName());
    if (!newName)
    {
      throw new ConceptualException("Duplicate native name: " + method.getMangledName(), method.getLexicalPhrase());
    }
  }

  /**
   * Checks whether the specified Method has a forbidden or duplicated user-specified native name.
   * @param method - the Method to check
   * @param usedNativeNames - the set of used native names to check against
   * @throws ConceptualException - if a bad native name is found
   */
  private static void checkSpecifiedNativeName(Method method, Set<String> usedNativeNames) throws ConceptualException
  {
    String nativeName = method.getNativeName();
    if (nativeName != null)
    {
      checkForbidden(nativeName, method.getLexicalPhrase());
      boolean newName = usedNativeNames.add(nativeName);
      if (!newName)
      {
        throw new ConceptualException("Duplicate native name: " + nativeName, method.getLexicalPhrase());
      }
    }
  }

  /**
   * Checks whether the specified native name is explicitly forbidden, and if so throws a ConceptualException
   * @param name - the name to check
   * @param lexicalPhrase - the lexical phrase to throw as part of the ConceptualException if the name is forbidden
   * @throws ConceptualException - if the name is forbidden
   */
  private static void checkForbidden(String name, LexicalPhrase lexicalPhrase) throws ConceptualException
  {
    if (FORBIDDEN_NAMES.contains(name) || name.startsWith("llvm."))
    {
      throw new ConceptualException("Forbidden native name: " + name, lexicalPhrase);
    }
  }

}
