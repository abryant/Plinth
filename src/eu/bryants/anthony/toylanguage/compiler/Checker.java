package eu.bryants.anthony.toylanguage.compiler;

import eu.bryants.anthony.toylanguage.ast.Block;
import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.Function;
import eu.bryants.anthony.toylanguage.ast.ReturnStatement;
import eu.bryants.anthony.toylanguage.ast.Statement;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Checker
{
  /**
   * Checks that the control flow of the specified compilation unit is well defined.
   * @param compilationUnit - the CompilationUnit to check
   * @throws ConceptualException - if any control flow related errors are detected
   */
  public static void checkControlFlow(CompilationUnit compilationUnit) throws ConceptualException
  {
    for (Function f : compilationUnit.getFunctions())
    {
      boolean returned = checkControlFlow(f.getBlock());
      if (!returned)
      {
        throw new ConceptualException("Function does not return a value", f.getLexicalPhrase());
      }
    }
  }

  /**
   * Checks that the control flow of the specified block is well defined.
   * @param block - the block to check
   * @return true if the block returns from its enclosing function, false if control flow continues after it
   * @throws ConceptualException - if any unreachable code is detected
   */
  private static boolean checkControlFlow(Block block) throws ConceptualException
  {
    boolean returned = false;
    for (Statement s : block.getStatements())
    {
      if (returned)
      {
        throw new ConceptualException("Unreachable code", s.getLexicalPhrase());
      }
      if (s instanceof ReturnStatement)
      {
        returned = true;
      }
      else if (s instanceof Block)
      {
        returned = checkControlFlow(block);
      }
    }
    return returned;
  }
}
