package eu.bryants.anthony.plinth.ast.member;

import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.compiler.passes.SpecialTypeHandler;

/*
 * Created on 12 Oct 2012
 */

/**
 * @author Anthony Bryant
 */
public class BuiltinMethod extends Method
{

  public static enum BuiltinMethodType
  {
    BOOLEAN_TO_STRING       (SpecialTypeHandler.STRING_TYPE, "toString", false, null, new Parameter[0]),
    SIGNED_TO_STRING        (SpecialTypeHandler.STRING_TYPE, "toString", false, null, new Parameter[0]),
    SIGNED_TO_STRING_RADIX  (SpecialTypeHandler.STRING_TYPE, "toString", false, null, new Parameter[] {new Parameter(false, new PrimitiveType(false, PrimitiveTypeType.UINT, null), "radix", null)}),
    UNSIGNED_TO_STRING      (SpecialTypeHandler.STRING_TYPE, "toString", false, null, new Parameter[0]),
    UNSIGNED_TO_STRING_RADIX(SpecialTypeHandler.STRING_TYPE, "toString", false, null, new Parameter[] {new Parameter(false, new PrimitiveType(false, PrimitiveTypeType.UINT, null), "radix", null)}),
    FLOATING_TO_STRING      (SpecialTypeHandler.STRING_TYPE, "toString", false, null, new Parameter[0]),
    ;
    public final Type returnType;
    public final String methodName;
    public final boolean isStatic;
    public final String nativeName;
    public final Parameter[] parameters;

    private BuiltinMethodType(Type returnType, String methodName, boolean isStatic, String nativeName, Parameter[] parameters)
    {
      this.returnType = returnType;
      this.methodName = methodName;
      this.isStatic = isStatic;
      this.nativeName = nativeName;
      this.parameters = parameters;
    }
  }

  private Type baseType;
  private BuiltinMethodType builtinType;

  /**
   * Creates a new BuiltinMethod with the specified base type and BuiltinMethodType
   * @param baseType - the base type that this BuiltinMethod is contained in
   * @param builtinType - the type of builtin method that this BuiltinMethod represents
   */
  public BuiltinMethod(Type baseType, BuiltinMethodType builtinType)
  {
    super(builtinType.returnType, builtinType.methodName, builtinType.isStatic, builtinType.nativeName, builtinType.parameters, null, null);
    this.baseType = baseType;
    this.builtinType = builtinType;
    if (baseType.isNullable())
    {
      throw new IllegalArgumentException("A builtin method cannot have a nullable base type");
    }
  }

  /**
   * @return the baseType
   */
  public Type getBaseType()
  {
    return baseType;
  }

  /**
   * @return the builtinType
   */
  public BuiltinMethodType getBuiltinType()
  {
    return builtinType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMangledName()
  {
    StringBuffer buffer = new StringBuffer();
    if (isStatic())
    {
      buffer.append("_SB");
    }
    else
    {
      buffer.append("_B");
    }
    buffer.append(baseType.getMangledName());
    buffer.append('_');
    buffer.append(getName());
    buffer.append('_');
    buffer.append(getReturnType().getMangledName());
    buffer.append('_');
    for (Parameter p : getParameters())
    {
      buffer.append(p.getType().getMangledName());
    }
    return buffer.toString();
  }
}
