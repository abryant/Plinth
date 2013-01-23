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
   */
  public CoalescedConceptualException()
  {
    super("Coalesced ConceptualException set, not for displaying as a group", null);
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
   * @return true iff this CoalescedConceptualException has some stored ConceptualExceptions
   */
  public boolean hasStoredExceptions()
  {
    return !storedExceptions.isEmpty();
  }

  /**
   * @return the ConceptualExceptions stored in this CoalescedConceptualException
   */
  public ConceptualException[] getStoredExceptions()
  {
    return storedExceptions.toArray(new ConceptualException[storedExceptions.size()]);
  }
}
