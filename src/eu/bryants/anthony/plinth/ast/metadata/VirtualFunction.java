package eu.bryants.anthony.plinth.ast.metadata;

/*
 * Created on 30 May 2013
 */

/**
 * Represents an entry in a virtual function table.
 * A virtual function can be a member function (method, or property getter/setter/constructor), or an override function.
 * Member functions are represented by the MemberFunction subclass, which stores the member function type as well as the Method/Property represented.
 * Override functions exist to proxy a call to a supertype's implementation of a method to the real implementation, if the calling convention differs somehow. They are represented by the OverrideFunction subclass.
 * @author Anthony Bryant
 */
public abstract class VirtualFunction implements Comparable<VirtualFunction>
{

  private int index;

  /**
   * Default constructor.
   */
  protected VirtualFunction()
  {
    // do nothing
  }

  /**
   * @return the index
   */
  public int getIndex()
  {
    return index;
  }

  /**
   * @param index - the index to set
   */
  public void setIndex(int index)
  {
    this.index = index;
  }

}
