package eu.bryants.anthony.plinth.compiler.passes.llvm;

import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMBasicBlockRef;
import nativelib.llvm.LLVM.LLVMBuilderRef;
import nativelib.llvm.LLVM.LLVMValueRef;

/*
 * Created on 13 Feb 2013
 */

/**
   * A container for landing pad blocks, to be passed around while building a function.
   * The first time a landing pad block is needed, this container will create it, and then the same landing pad will be used for the rest of the function.
   * @author Anthony Bryant
 */
public class LandingPadContainer
{

  private LLVMBuilderRef builder;
  private LLVMBasicBlockRef landingPad;

  /**
   * Creates a new LandingPadContainer, with the specified LLVMBuilderRef to build code with
   * @param builder - the builder to build the new block with
   */
  public LandingPadContainer(LLVMBuilderRef builder)
  {
    this.builder = builder;
  }

  /**
   * Gets the landing pad contained by this container, lazily adding it if necessary.
   * @return the landing pad block contained by this container
   */
  public LLVMBasicBlockRef getLandingPadBlock()
  {
    if (landingPad == null)
    {
      LLVMValueRef function = LLVM.LLVMGetBasicBlockParent(LLVM.LLVMGetInsertBlock(builder));
      landingPad = LLVM.LLVMAppendBasicBlock(function, "landingPad");
    }
    return landingPad;
  }

  /**
   * @return the landing pad block, or null if none has been created
   */
  public LLVMBasicBlockRef getExistingLandingPadBlock()
  {
    return landingPad;
  }
}
