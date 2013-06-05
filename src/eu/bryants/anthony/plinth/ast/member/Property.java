package eu.bryants.anthony.plinth.ast.member;

import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.metadata.GlobalVariable;
import eu.bryants.anthony.plinth.ast.metadata.MemberFunction;
import eu.bryants.anthony.plinth.ast.metadata.MemberVariable;
import eu.bryants.anthony.plinth.ast.metadata.PropertyPseudoVariable;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;

/*
 * Created on 22 Feb 2013
 */

/**
 * @author Anthony Bryant
 */
public class Property extends Member
{

  // an abstract property does not have implementations for its getter and setter, and is always unbacked
  private boolean isAbstract;
  // a final property can only have its constructor called once, and its setter never (but the backing variable is never final)
  private boolean isFinal;
  // a mutable property has a mutable backing variable, and can be assigned to on an immutable receiver, and accesses to it never result in contextually immutable values
  // basically, the backing variable and the property itself both behave as mutable fields
  private boolean isMutable;
  // a static property is part of a type rather than an object, and its backing variable (if any) is a global variable rather than a member variable
  private boolean isStatic;
  // an unbacked property has no backing variable
  private boolean isUnbacked;

  private SinceSpecifier sinceSpecifier;

  private Type type;
  private String name;
  private Expression initialiserExpression;

  private boolean declaresGetter;
  private boolean getterImmutable;
  private NamedType[] getterUncheckedThrownTypes;
  private Block getterBlock;

  private boolean declaresSetter;
  private boolean setterImmutable;
  private Parameter setterParameter;
  private NamedType[] setterUncheckedThrownTypes;
  private Block setterBlock;

  private boolean declaresConstructor;
  private boolean constructorImmutable;
  private Parameter constructorParameter;
  private NamedType[] constructorUncheckedThrownTypes;
  private Block constructorBlock;

  private PropertyPseudoVariable pseudoVariable;

  private MemberVariable backingMemberVariable;
  private GlobalVariable backingGlobalVariable;

  private MemberFunction getterMemberFunction;
  private MemberFunction setterMemberFunction;
  private MemberFunction constructorMemberFunction;

  private TypeDefinition containingTypeDefinition;


  /**
   * Creates a new Property with the specified properties.
   * @param isAbstract - true if the property should be abstract
   * @param isFinal - true if the property should be final
   * @param isMutable - true if the property should be mutable
   * @param isStatic - true if the property should be static
   * @param isUnbacked - true if the property should not have a backing variable
   * @param sinceSpecifier - the since specifier for the property, or null if there is none
   * @param type - the type of the property
   * @param name - the name of the property
   * @param initialiserExpression - the initialiser expression, or null if there is none
   * @param declaresGetter - whether this property explicitly declares a getter
   * @param getterImmutable - true if the getter should be an immutable function
   * @param getterUncheckedThrownTypes - the list of unchecked types thrown by this property's getter
   * @param getterBlock - the Block containing the getter's implementation, or null if the default implementation should be used
   * @param declaresSetter - whether this property explicitly declares a setter
   * @param setterImmutable - true if the setter should be an immutable function
   * @param setterParameter - the setter's parameter, or null if the setter does not have a block
   * @param setterUncheckedThrownTypes - the list of unchecked types thrown by this property's setter
   * @param setterBlock - the Block containing the setter's implementation, or null if the default implementation should be used
   * @param declaresConstructor - whether this property explicitly declares a constructor
   * @param constructorImmutable - true if the constructor should be an immutable function
   * @param constructorParameter - the constructor's parameter, or null if the constructor does not have a block
   * @param constructorUncheckedThrownTypes - the list of unchecked types thrown by this property's constructor
   * @param constructorBlock - the Block containing the constructor's implementation, or null if the default implementation should be used
   * @param lexicalPhrase - the LexicalPhrase representing the Property's location in the source code
   */
  public Property(boolean isAbstract, boolean isFinal, boolean isMutable, boolean isStatic, boolean isUnbacked, SinceSpecifier sinceSpecifier, Type type, String name, Expression initialiserExpression,
                  boolean declaresGetter, boolean getterImmutable, NamedType[] getterUncheckedThrownTypes, Block getterBlock,
                  boolean declaresSetter, boolean setterImmutable, Parameter setterParameter, NamedType[] setterUncheckedThrownTypes, Block setterBlock,
                  boolean declaresConstructor, boolean constructorImmutable, Parameter constructorParameter, NamedType[] constructorUncheckedThrownTypes, Block constructorBlock,
                  LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.isAbstract = isAbstract;
    this.isFinal = isFinal;
    this.isMutable = isMutable;
    this.isStatic = isStatic;
    this.isUnbacked = isUnbacked;
    this.sinceSpecifier = sinceSpecifier;
    this.type = type;
    this.name = name;
    this.initialiserExpression = initialiserExpression;
    this.declaresGetter = declaresGetter;
    this.getterImmutable = getterImmutable;
    this.getterUncheckedThrownTypes = getterUncheckedThrownTypes;
    this.getterBlock = getterBlock;
    this.declaresSetter = declaresSetter;
    this.setterImmutable = setterImmutable;
    this.setterParameter = setterParameter;
    this.setterUncheckedThrownTypes = setterUncheckedThrownTypes;
    this.setterBlock = setterBlock;
    this.declaresConstructor = declaresConstructor;
    this.constructorImmutable = declaresConstructor ? constructorImmutable : setterImmutable;
    this.constructorParameter = constructorParameter;
    this.constructorUncheckedThrownTypes = constructorUncheckedThrownTypes;
    this.constructorBlock = constructorBlock;
    pseudoVariable = new PropertyPseudoVariable(this);
  }

  /**
   * Computes whether this property has a constructor, based on the following rules:
   * <ul>
   * <li>all final properties have constructors</li>
   * <li>static properties do not have constructors unless they are final</li>
   * <li>if a non-static, non-final property declares a constructor, it always gets one</li>
   * <li>if the property has a backing variable which doesn't have a default value, it needs a constructor (note: static properties must always have default values)</li>
   * <li>properties do not need a constructor unless one of the previous rules applies</li>
   * </ul>
   * @return true if this Property has a constructor
   */
  public boolean hasConstructor()
  {
    if (isFinal)
    {
      // all final properties have constructors
      return true;
    }
    if (isStatic)
    {
      // static properties do not have constructors unless they are final
      return false;
    }
    if (declaresConstructor)
    {
      // if a non-static, non-final property declares a constructor, it always gets one
      return true;
    }
    if ((isAbstract || !isUnbacked) && !type.hasDefaultValue())
    {
      // if the property has a backing variable which doesn't have a default value, it needs a constructor
      return true;
    }
    // properties do not need a constructor unless one of the previous rules applies
    return false;
  }

  /**
   * This should only be used when adding a Property to an interface.
   * @param isAbstract - true if this Property should be abstract
   */
  public void setAbstract(boolean isAbstract)
  {
    this.isAbstract = isAbstract;
  }

  /**
   * @return the isAbstract
   */
  public boolean isAbstract()
  {
    return isAbstract;
  }

  /**
   * @return the isFinal
   */
  public boolean isFinal()
  {
    return isFinal;
  }

  /**
   * @return the isMutable
   */
  public boolean isMutable()
  {
    return isMutable;
  }

  /**
   * @return the isStatic
   */
  public boolean isStatic()
  {
    return isStatic;
  }

  /**
   * This should only be called when adding a Property to a type.
   * For example, if it is abstract, it should probably be set to unbacked.
   * @param isUnbacked - the isUnbacked to set
   */
  public void setUnbacked(boolean isUnbacked)
  {
    this.isUnbacked = isUnbacked;
  }

  /**
   * @return the isUnbacked
   */
  public boolean isUnbacked()
  {
    return isUnbacked;
  }

  /**
   * @return the sinceSpecifier
   */
  public SinceSpecifier getSinceSpecifier()
  {
    return sinceSpecifier;
  }

  /**
   * @return the type
   */
  public Type getType()
  {
    return type;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the initialiserExpression
   */
  public Expression getInitialiserExpression()
  {
    return initialiserExpression;
  }

  /**
   * @return the declaresGetter
   */
  public boolean getDeclaresGetter()
  {
    return declaresGetter;
  }

  /**
   * @return the getterImmutable
   */
  public boolean isGetterImmutable()
  {
    return getterImmutable;
  }

  /**
   * @return the getterUncheckedThrownTypes
   */
  public NamedType[] getGetterUncheckedThrownTypes()
  {
    return getterUncheckedThrownTypes;
  }

  /**
   * @return the getterBlock
   */
  public Block getGetterBlock()
  {
    return getterBlock;
  }

  /**
   * @return the declaresSetter
   */
  public boolean getDeclaresSetter()
  {
    return declaresSetter;
  }

  /**
   * @return the setterImmutable
   */
  public boolean isSetterImmutable()
  {
    return setterImmutable;
  }

  /**
   * @return the setterParameter
   */
  public Parameter getSetterParameter()
  {
    return setterParameter;
  }

  /**
   * @return the setterUncheckedThrownTypes
   */
  public NamedType[] getSetterUncheckedThrownTypes()
  {
    return setterUncheckedThrownTypes;
  }

  /**
   * @return the setterBlock
   */
  public Block getSetterBlock()
  {
    return setterBlock;
  }

  /**
   * @return the declaresConstructor
   */
  public boolean getDeclaresConstructor()
  {
    return declaresConstructor;
  }

  /**
   * @return the constructorImmutable
   */
  public boolean isConstructorImmutable()
  {
    return constructorImmutable;
  }

  /**
   * @return the constructorParameter
   */
  public Parameter getConstructorParameter()
  {
    return constructorParameter;
  }

  /**
   * @return the constructorUncheckedThrownTypes
   */
  public NamedType[] getConstructorUncheckedThrownTypes()
  {
    return constructorUncheckedThrownTypes;
  }

  /**
   * @return the constructorBlock
   */
  public Block getConstructorBlock()
  {
    return constructorBlock;
  }

  /**
   * @return the pseudoVariable
   */
  public PropertyPseudoVariable getPseudoVariable()
  {
    return pseudoVariable;
  }

  /**
   * @param pseudoVariable - the pseudoVariable to set
   */
  public void setPseudoVariable(PropertyPseudoVariable pseudoVariable)
  {
    this.pseudoVariable = pseudoVariable;
  }

  /**
   * @return the backingMemberVariable
   */
  public MemberVariable getBackingMemberVariable()
  {
    return backingMemberVariable;
  }

  /**
   * @param backingMemberVariable - the backingMemberVariable to set
   */
  public void setBackingMemberVariable(MemberVariable backingMemberVariable)
  {
    this.backingMemberVariable = backingMemberVariable;
  }

  /**
   * @return the backingGlobalVariable
   */
  public GlobalVariable getBackingGlobalVariable()
  {
    return backingGlobalVariable;
  }

  /**
   * @param backingGlobalVariable - the backingGlobalVariable to set
   */
  public void setBackingGlobalVariable(GlobalVariable backingGlobalVariable)
  {
    this.backingGlobalVariable = backingGlobalVariable;
  }

  /**
   * @return the getterMemberFunction
   */
  public MemberFunction getGetterMemberFunction()
  {
    return getterMemberFunction;
  }

  /**
   * @param getterMemberFunction - the getterMemberFunction to set
   */
  public void setGetterMemberFunction(MemberFunction getterMemberFunction)
  {
    this.getterMemberFunction = getterMemberFunction;
  }

  /**
   * @return the setterMemberFunction
   */
  public MemberFunction getSetterMemberFunction()
  {
    return setterMemberFunction;
  }

  /**
   * @param setterMemberFunction - the setterMemberFunction to set
   */
  public void setSetterMemberFunction(MemberFunction setterMemberFunction)
  {
    this.setterMemberFunction = setterMemberFunction;
  }

  /**
   * @return the constructorMemberFunction
   */
  public MemberFunction getConstructorMemberFunction()
  {
    return constructorMemberFunction;
  }

  /**
   * @param constructorMemberFunction - the constructorMemberFunction to set
   */
  public void setConstructorMemberFunction(MemberFunction constructorMemberFunction)
  {
    this.constructorMemberFunction = constructorMemberFunction;
  }

  /**
   * @return the containingTypeDefinition
   */
  public TypeDefinition getContainingTypeDefinition()
  {
    return containingTypeDefinition;
  }

  /**
   * @param containingTypeDefinition - the containingTypeDefinition to set
   */
  public void setContainingTypeDefinition(TypeDefinition containingTypeDefinition)
  {
    this.containingTypeDefinition = containingTypeDefinition;
  }

  /**
   * @param typeString - the mangled type of the part of this property to be represented (e.g. "G" for getter)
   * @return the descriptor string for the specified type of property function, which should be used in the virtual function table descriptor for this property's class
   */
  private String getDescriptorString(String typeString)
  {
    StringBuffer buffer = new StringBuffer();
    if (!isStatic && containingTypeDefinition instanceof InterfaceDefinition)
    {
      // non-static interface functions must have a unique disambiguator, since their calling convention depends on which interface they are part of
      buffer.append('I');
      buffer.append(containingTypeDefinition.getQualifiedName().getMangledName());
    }
    buffer.append(isStatic ? "SP" : "P");
    buffer.append(typeString);
    buffer.append('_');
    buffer.append(name);
    return buffer.toString();
  }

  /**
   * @return the descriptor for the getter of this property, which should be used in the virtual function table descriptor for this property's class
   */
  public String getGetterDescriptor()
  {
    return getDescriptorString("G");
  }

  /**
   * @return the descriptor for the setter of this property, which should be used in the virtual function table descriptor for this property's class
   */
  public String getSetterDescriptor()
  {
    return getDescriptorString("S");
  }

  /**
   * @return the descriptor for the constructor of this property, which should be used in the virtual function table descriptor for this property's class
   */
  public String getConstructorDescriptor()
  {
    return getDescriptorString("C");
  }

  /**
   * @param typeString - the mangled type of the part of this property to be represented (e.g. "G" for getter)
   * @return the mangled name of part of this Property
   */
  private String getMangledName(String typeString)
  {
    StringBuffer buffer = new StringBuffer();
    if (isStatic)
    {
      buffer.append("_SP");
    }
    else
    {
      buffer.append("_P");
    }
    buffer.append(typeString);
    buffer.append(containingTypeDefinition.getQualifiedName().getMangledName());
    buffer.append('_');
    if (isStatic)
    {
      if (sinceSpecifier != null)
      {
        buffer.append(sinceSpecifier.getMangledName());
      }
      buffer.append('_');
    }
    buffer.append(name);
    return buffer.toString();
  }

  /**
   * @return the mangled name of this Property's getter
   */
  public String getGetterMangledName()
  {
    return getMangledName("G");
  }

  /**
   * @return the mangled name of this Property's setter
   */
  public String getSetterMangledName()
  {
    return getMangledName("S");
  }

  /**
   * @return the mangled name of this Property's constructor
   */
  public String getConstructorMangledName()
  {
    return getMangledName("C");
  }

  /**
   * @return the mangled name of this Property's backing variable
   */
  public String getBackingVariableMangledName()
  {
    return getMangledName("B");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    if (isAbstract)
    {
      buffer.append("abstract ");
    }
    if (isFinal)
    {
      buffer.append("final ");
    }
    if (isMutable)
    {
      buffer.append("mutable ");
    }
    if (isStatic)
    {
      buffer.append("static ");
    }
    if (isUnbacked)
    {
      buffer.append("unbacked ");
    }
    if (sinceSpecifier != null)
    {
      buffer.append(sinceSpecifier);
      buffer.append(' ');
    }
    buffer.append("property ");
    buffer.append(type);
    buffer.append(' ');
    buffer.append(name);
    if (initialiserExpression != null)
    {
      buffer.append(" = ");
      buffer.append(initialiserExpression);
    }
    if (declaresConstructor)
    {
      buffer.append('\n');
      if (constructorImmutable)
      {
        buffer.append("immutable ");
      }
      buffer.append("constructor");
      if (constructorBlock != null)
      {
        buffer.append('(');
        buffer.append(constructorParameter);
        buffer.append(')');
        if (constructorUncheckedThrownTypes != null && constructorUncheckedThrownTypes.length > 0)
        {
          buffer.append(" throws ");
          for (int i = 0; i < constructorUncheckedThrownTypes.length; ++i)
          {
            buffer.append("unchecked ");
            buffer.append(constructorUncheckedThrownTypes[i]);
            if (i != constructorUncheckedThrownTypes.length - 1)
            {
              buffer.append(", ");
            }
          }
        }
        buffer.append('\n');
        buffer.append(constructorBlock);
      }
    }
    if (declaresSetter)
    {
      buffer.append('\n');
      if (setterImmutable)
      {
        buffer.append("immutable ");
      }
      buffer.append("setter");
      if (setterBlock != null)
      {
        buffer.append('(');
        buffer.append(setterParameter);
        buffer.append(')');
        if (setterUncheckedThrownTypes != null && setterUncheckedThrownTypes.length > 0)
        {
          buffer.append(" throws ");
          for (int i = 0; i < setterUncheckedThrownTypes.length; ++i)
          {
            buffer.append("unchecked ");
            buffer.append(setterUncheckedThrownTypes[i]);
            if (i != setterUncheckedThrownTypes.length - 1)
            {
              buffer.append(", ");
            }
          }
        }
        buffer.append('\n');
        buffer.append(setterBlock);
      }
    }
    if (declaresGetter)
    {
      buffer.append('\n');
      if (!getterImmutable)
      {
        buffer.append("mutable ");
      }
      buffer.append("getter");
      if (getterUncheckedThrownTypes != null && getterUncheckedThrownTypes.length > 0)
      {
        buffer.append(" throws ");
        for (int i = 0; i < getterUncheckedThrownTypes.length; ++i)
        {
          buffer.append("unchecked ");
          buffer.append(getterUncheckedThrownTypes[i]);
          if (i != getterUncheckedThrownTypes.length - 1)
          {
            buffer.append(", ");
          }
        }
      }
      if (getterBlock != null)
      {
        buffer.append('\n');
        buffer.append(getterBlock);
      }
    }
    buffer.append(';');
    return buffer.toString();
  }
}
