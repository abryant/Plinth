package eu.bryants.anthony.plinth.compiler;

import java.util.LinkedList;
import java.util.List;

/*
 * Created on 20 Jan 2013
 */

/**
 * @author Anthony Bryant
 */
public class CoalescedConceptualException extends ConceptualException
{
  private static final long serialVersionUID = 1L;

  private List<ConceptualException> storedExceptions = new LinkedList<ConceptualException>();

  /**
   * Creates a new CoalescedConceptualException, which can store any number of ConceptualExceptions
   * @param e - the first ConceptualException to store
   */
  public CoalescedConceptualException(ConceptualException e)
  {
    super("Coalesced ConceptualException set, not for displaying as a group", null);
    addException(e);
  }

  /**
   * Adds the specified ConceptualException to this coalesced exception
   * @param conceptualException - the ConceptualException to add
   */
  public void addException(ConceptualException conceptualException)
  {
    if (conceptualException instanceof CoalescedConceptualException)
    {
      for (ConceptualException storedException : ((CoalescedConceptualException) conceptualException).getStoredExceptions())
      {
        addException(storedException);
      }
    }
    else
    {
      storedExceptions.add(conceptualException);
    }
  }

  /**
   * Coalesces the specified ConceptualException into the specified CoalescedConceptualException. If CoalescedConceptualException is null, a new one is created.
   * @param coalescedException - the CoalescedConceptualException to coalesce the new ConceptualException into, or null to create a new one
   * @param e - the ConceptualException
   * @return the coalesced exception, including the new ConceptualException
   */
  public static CoalescedConceptualException coalesce(CoalescedConceptualException coalescedException, ConceptualException e)
  {
    if (coalescedException == null)
    {
      return new CoalescedConceptualException(e);
    }
    coalescedException.addException(e);
    return coalescedException;
  }

  /**
   * @return the ConceptualExceptions stored in this CoalescedConceptualException
   */
  public ConceptualException[] getStoredExceptions()
  {
    return storedExceptions.toArray(new ConceptualException[storedExceptions.size()]);
  }
}
