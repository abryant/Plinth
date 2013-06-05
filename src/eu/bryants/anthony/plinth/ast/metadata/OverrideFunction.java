package eu.bryants.anthony.plinth.ast.metadata;

/*
 * Created on 30 May 2013
 */

/**
 * @author Anthony Bryant
 */
public class OverrideFunction extends VirtualFunction
{

  private MemberFunctionType memberFunctionType;

  private MethodReference inheritedMethodReference;
  private MethodReference implementationMethodReference;

  private PropertyReference inheritedPropertyReference;
  private PropertyReference implementationPropertyReference;

  /**
   * Creates an OverrideFunction for overriding the specified inherited method with the specified implementation method.
   * @param inherited - the MethodReference referencing the inherited method
   * @param implementation - the MethodReference referencing the implementation method
   */
  public OverrideFunction(MethodReference inherited, MethodReference implementation)
  {
    memberFunctionType = MemberFunctionType.METHOD;
    inheritedMethodReference = inherited;
    implementationMethodReference = implementation;
  }

  /**
   * Creates an OverrideFunction for overriding the specified inherited property method with the specified implementation property.
   * @param inherited - the PropertyReference referencing the inherited property
   * @param implementation - the PropertyReference referencing the implementation property
   * @param memberFunctionType - the type of function that this OverrideFunction represents (this should be either PROPERTY_GETTER, PROPERTY_SETTER, or PROPERTY_CONSTRUCTOR)
   */
  public OverrideFunction(PropertyReference inherited, PropertyReference implementation, MemberFunctionType memberFunctionType)
  {
    this.memberFunctionType = memberFunctionType;
    inheritedPropertyReference = inherited;
    implementationPropertyReference = implementation;
  }

  /**
   * @return the memberFunctionType
   */
  public MemberFunctionType getMemberFunctionType()
  {
    return memberFunctionType;
  }

  /**
   * @return the inheritedMethodReference
   */
  public MethodReference getInheritedMethodReference()
  {
    return inheritedMethodReference;
  }

  /**
   * @return the implementationMethodReference
   */
  public MethodReference getImplementationMethodReference()
  {
    return implementationMethodReference;
  }

  /**
   * @return the inheritedPropertyReference
   */
  public PropertyReference getInheritedPropertyReference()
  {
    return inheritedPropertyReference;
  }

  /**
   * @return the implementationPropertyReference
   */
  public PropertyReference getImplementationPropertyReference()
  {
    return implementationPropertyReference;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(VirtualFunction otherFunction)
  {
    if (otherFunction instanceof MemberFunction)
    {
      // member functions always come before override functions
      return 1;
    }
    if (!(otherFunction instanceof OverrideFunction))
    {
      throw new UnsupportedOperationException("Unknown virtual function type: " + otherFunction);
    }
    // the order of OverrideFunctions doesn't matter, since they are never referenced directly,
    // it only matters that they are after the MemberFunctions, so make them all equal to each other
    return 0;
  }

}
