package eu.bryants.anthony.plinth.ast.metadata;

import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;

/*
 * Created on 27 Feb 2013
 */

/**
 * @author Anthony Bryant
 */
public class MemberFunction extends VirtualFunction
{
  private MemberFunctionType memberFunctionType;
  private Method method;
  private Property property;

  /**
   * Creates a new MemberFunction for the specified non-static Method.
   * @param method - the Method to create the MemberFunction for
   */
  public MemberFunction(Method method)
  {
    this.method = method;
    memberFunctionType = MemberFunctionType.METHOD;
  }

  /**
   * Creates a new MemberFunction for either the getter or the setter of the specified non-static Property.
   * @param property - the Property to create the MemberFunction for
   * @param memberFunctionType - the type of member function to create (e.g. a getter)
   */
  public MemberFunction(Property property, MemberFunctionType memberFunctionType)
  {
    this.property = property;
    this.memberFunctionType = memberFunctionType;
    if (memberFunctionType != MemberFunctionType.PROPERTY_GETTER && memberFunctionType != MemberFunctionType.PROPERTY_SETTER && memberFunctionType != MemberFunctionType.PROPERTY_CONSTRUCTOR)
    {
      throw new IllegalArgumentException("A member function for a property must be either a getter, setter, or constructor");
    }
  }

  /**
   * @return the memberFunctionType
   */
  public MemberFunctionType getMemberFunctionType()
  {
    return memberFunctionType;
  }

  /**
   * @return the method
   */
  public Method getMethod()
  {
    return method;
  }

  /**
   * @return the property
   */
  public Property getProperty()
  {
    return property;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(VirtualFunction otherFunction)
  {
    if (otherFunction instanceof OverrideFunction)
    {
      // member functions always come before override functions
      return -1;
    }
    if (!(otherFunction instanceof MemberFunction))
    {
      throw new UnsupportedOperationException("Unknown virtual function type: " + otherFunction);
    }
    MemberFunction other = (MemberFunction) otherFunction;

    SinceSpecifier since1;
    String name1;
    switch (memberFunctionType)
    {
    case METHOD:
      since1 = method.getSinceSpecifier();
      name1 = method.getName();
      break;
    case PROPERTY_GETTER:
    case PROPERTY_SETTER:
    case PROPERTY_CONSTRUCTOR:
      since1 = property.getSinceSpecifier();
      name1 = property.getName();
      break;
    default:
      throw new IllegalStateException("Unknown member function type: " + memberFunctionType);
    }
    SinceSpecifier since2;
    String name2;
    switch (other.getMemberFunctionType())
    {
    case METHOD:
      since2 = other.getMethod().getSinceSpecifier();
      name2 = other.getMethod().getName();
      break;
    case PROPERTY_GETTER:
    case PROPERTY_SETTER:
    case PROPERTY_CONSTRUCTOR:
      since2 = other.getProperty().getSinceSpecifier();
      name2 = other.getProperty().getName();
      break;
    default:
      throw new IllegalStateException("Unknown member function type: " + memberFunctionType);
    }

    // compare since specifier, then name, then type (method vs getter vs setter), then mangled return type, then each mangled parameter in turn, using lexicographic ordering
    // if they are equal except that one parameter list is a prefix of the other, then the comparison makes the longer parameter list larger

    // two null since specifiers are equal, and a null since specifiers always comes before a not-null one
    if ((since1 == null) != (since2 == null))
    {
      return since1 == null ? -1 : 1;
    }
    if (since1 != null && since2 != null)
    {
      int sinceComparison = since1.compareTo(since2);
      if (sinceComparison != 0)
      {
        return sinceComparison;
      }
    }

    int nameComparison = name1.compareTo(name2);
    if (nameComparison != 0)
    {
      return nameComparison;
    }

    // getters are always before non-getters
    if ((memberFunctionType == MemberFunctionType.PROPERTY_GETTER) != (other.getMemberFunctionType() == MemberFunctionType.PROPERTY_GETTER))
    {
      // if this is the getter, then it is before the other one
      return memberFunctionType == MemberFunctionType.PROPERTY_GETTER ? -1 : 1;
    }
    if (memberFunctionType == MemberFunctionType.PROPERTY_GETTER && other.getMemberFunctionType() == MemberFunctionType.PROPERTY_GETTER)
    {
      // if they are both getters with the same since specifier and name, they are equal
      return 0;
    }
    // now we know neither of them are getters.
    // setters are always before property constructors and methods
    if ((memberFunctionType == MemberFunctionType.PROPERTY_SETTER) != (other.getMemberFunctionType() == MemberFunctionType.PROPERTY_SETTER))
    {
      // if this is the setter, then it is before the other one
      return memberFunctionType == MemberFunctionType.PROPERTY_SETTER ? -1 : 1;
    }
    if (memberFunctionType == MemberFunctionType.PROPERTY_SETTER && other.getMemberFunctionType() == MemberFunctionType.PROPERTY_SETTER)
    {
      // if they are both setters with the same since specifier and name, they are equal
      return 0;
    }
    // now we know neither of them are getters or setters.
    // property constructors are always before methods
    if ((memberFunctionType == MemberFunctionType.PROPERTY_CONSTRUCTOR) != (other.getMemberFunctionType() == MemberFunctionType.PROPERTY_CONSTRUCTOR))
    {
      // if this is the constructor, then it is before the other one
      return memberFunctionType == MemberFunctionType.PROPERTY_CONSTRUCTOR ? -1 : 1;
    }
    if (memberFunctionType == MemberFunctionType.PROPERTY_CONSTRUCTOR && other.getMemberFunctionType() == MemberFunctionType.PROPERTY_CONSTRUCTOR)
    {
      // if they are both constructors with the same since specifier and name, they are equal
      return 0;
    }

    // neither of them are getters, setters, or constructors, so they must be methods, so compare the return and parameter types
    Method method1 = method;
    Method method2 = other.getMethod();

    int returnTypeComparison = method.getReturnType().getMangledName().compareTo(method2.getReturnType().getMangledName());
    if (returnTypeComparison != 0)
    {
      return returnTypeComparison;
    }

    Parameter[] parameters1 = method1.getParameters();
    Parameter[] parameters2 = method2.getParameters();

    for (int i = 0; i < parameters1.length & i < parameters2.length; ++i)
    {
      int paramComparison = parameters1[i].getMangledName().compareTo(parameters2[i].getMangledName());
      if (paramComparison != 0)
      {
        return paramComparison;
      }
    }
    return parameters1.length - parameters2.length;
  }



}
