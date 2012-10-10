package eu.bryants.anthony.plinth.compiler.passes.llvm;

import java.io.File;
import java.io.IOException;

import nativelib.llvm.LLVM;
import nativelib.llvm.LLVM.LLVMMemoryBufferRef;
import nativelib.llvm.LLVM.LLVMModuleRef;

import com.sun.jna.ptr.PointerByReference;

/*
 * Created on 10 Oct 2012
 */

/**
 * @author Anthony Bryant
 */
public class Linker
{
  private LLVMModuleRef linkedModule;

  public Linker(String moduleName)
  {
    linkedModule = LLVM.LLVMModuleCreateWithName(moduleName);
  }

  /**
   * Returns the linked module, as an LLVMModuleRef.
   * Note: if another module is linked using this Linker after this call, the linking process will alter the same module returned from this method.
   * @return the linked module
   */
  public LLVMModuleRef getLinkedModule()
  {
    return linkedModule;
  }

  /**
   * Links the specified module into this Linker's linked module.
   * @param module - the module to link into this Linker's linked module
   * @throws LinkerException - if a linker error occurs
   */
  public void linkModule(LLVMModuleRef module) throws LinkerException
  {
    PointerByReference bufferOutMessage = new PointerByReference();
    boolean failed = LLVM.LLVMLinkModules(linkedModule, module, LLVM.LLVMLinkerMode.LLVMLinkerPreserveSource, bufferOutMessage);
    if (failed)
    {
      throw new LinkerException("Failed to link module. Reason: " + bufferOutMessage.getValue().getString(0));
    }
  }

  /**
   * Loads a module from the specified bitcode file.
   * @param file - the file to load
   * @return the LLVMModuleRef loaded
   * @throws IOException - if an I/O problem occurs while trying to load the bitcode file, or it is malformed in some way
   */
  public static LLVMModuleRef loadModule(File file) throws IOException
  {
    PointerByReference outMemoryBuffer = new PointerByReference();
    PointerByReference bufferOutMessage = new PointerByReference();
    boolean bufferFailure = LLVM.LLVMCreateMemoryBufferWithContentsOfFile(file.getAbsolutePath(), outMemoryBuffer, bufferOutMessage);
    if (bufferFailure || outMemoryBuffer.getValue() == null)
    {
      throw new IOException("Failed to load bitcode file: " + file.getAbsolutePath() + "\nReason: " + bufferOutMessage.getValue().getString(0));
    }
    LLVMMemoryBufferRef memoryBuffer = new LLVMMemoryBufferRef();
    memoryBuffer.setPointer(outMemoryBuffer.getValue());
    PointerByReference outModule = new PointerByReference();
    PointerByReference parseOutMessage = new PointerByReference();
    boolean parseFailure = LLVM.LLVMParseBitcode(memoryBuffer, outModule, parseOutMessage);
    LLVM.LLVMDisposeMemoryBuffer(memoryBuffer);
    if (parseFailure || outModule.getValue() == null)
    {
      throw new IOException("Failed to parse bitcode file: " + file.getAbsolutePath() + "\nReason: " + parseOutMessage.getValue().getString(0));
    }
    LLVMModuleRef module = new LLVMModuleRef();
    module.setPointer(outModule.getValue());
    return module;
  }
}
