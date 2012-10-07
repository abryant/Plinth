package eu.bryants.anthony.plinth.ast.member;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.statement.Block;

/*
 * Created on 24 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class Initialiser extends Member
{

  private boolean isStatic;
  private Block block;

  /**
   * Creates a new Initialiser with the specified staticness and Block.
   * @param isStatic - true if this Initialiser is static, false otherwise
   * @param block - the block that this Initialiser executes
   * @param lexicalPhrase - the LexicalPhrase of this Initialiser
   */
  public Initialiser(boolean isStatic, Block block, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.isStatic = isStatic;
    this.block = block;
  }

  /**
   * @return true if this initialiser is static, false otherwise
   */
  public boolean isStatic()
  {
    return isStatic;
  }

  /**
   * @return the block
   */
  public Block getBlock()
  {
    return block;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return (isStatic ? "static\n" : "") + block;
  }

  /**
   * Finds the mangled name for the static/non-static initialiser for the specified type definition.
   * @param typeDefinition - the TypeDefinition to find the initialiser for
   * @param isStatic - true for the static initialiser, false for the instance initialiser
   * @return the mangled name of the specified initialiser
   */
  public static String getMangledName(TypeDefinition typeDefinition, boolean isStatic)
  {
    if (isStatic)
    {
      return "_SI" + typeDefinition.getQualifiedName().getMangledName();
    }
    return "_I" + typeDefinition.getQualifiedName().getMangledName();
  }
}
