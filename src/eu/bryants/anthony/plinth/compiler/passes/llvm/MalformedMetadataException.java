package eu.bryants.anthony.plinth.compiler.passes.llvm;

/*
 * Created on 30 Aug 2012
 */

/**
 * @author Anthony Bryant
 */
public class MalformedMetadataException extends Exception
{
  private static final long serialVersionUID = 1L;

  public MalformedMetadataException(String message)
  {
    super(message);
  }

  public MalformedMetadataException(String message, Exception cause)
  {
    super(message, cause);
  }
}
