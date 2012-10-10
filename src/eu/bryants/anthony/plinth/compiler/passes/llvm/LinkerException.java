package eu.bryants.anthony.plinth.compiler.passes.llvm;

/*
 * Created on 10 Oct 2012
 */

/**
 * @author Anthony Bryant
 */
public class LinkerException extends Exception
{
  private static final long serialVersionUID = 1L;

  public LinkerException(String message)
  {
    super(message);
  }

  public LinkerException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
