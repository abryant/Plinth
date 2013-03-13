package eu.bryants.anthony.plinth.parser.parseAST;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.type.NamedType;

/*
 * Created on 1 Mar 2013
 */

/**
 * Stores a property's getter, setter, or constructor during parsing of a Property.
 * @author Anthony Bryant
 */
public class PropertyMethod
{
  public static enum PropertyMethodType
  {
    GETTER("getter"),
    SETTER("setter"),
    CONSTRUCTOR("constructor"),
    ;
    private String stringRepresentation;
    private PropertyMethodType(String stringRepresentation)
    {
      this.stringRepresentation = stringRepresentation;
    }
    @Override
    public String toString()
    {
      return stringRepresentation;
    }
  }

  private PropertyMethodType type;
  private boolean isImmutable;

  // either the name and finality are stored, or the actual parameter is
  private boolean isParameterFinal;
  private String parameterName;
  private LexicalPhrase parameterLexicalPhrase;
  private Parameter parameter;

  private NamedType[] uncheckedThrownTypes;
  private Block block;

  private LexicalPhrase lexicalPhrase;

  public PropertyMethod(PropertyMethodType type, boolean isImmutable, boolean isParameterFinal, String parameterName, LexicalPhrase parameterLexicalPhrase, NamedType[] uncheckedThrownTypes, Block block, LexicalPhrase lexicalPhrase)
  {
    this.type = type;
    this.isImmutable = isImmutable;
    this.isParameterFinal = isParameterFinal;
    this.parameterName = parameterName;
    this.parameterLexicalPhrase = parameterLexicalPhrase;
    this.uncheckedThrownTypes = uncheckedThrownTypes;
    this.block = block;
    this.lexicalPhrase = lexicalPhrase;
  }

  public PropertyMethod(PropertyMethodType type, boolean isImmutable, Parameter parameter, NamedType[] uncheckedThrownTypes, Block block, LexicalPhrase lexicalPhrase)
  {
    this.type = type;
    this.isImmutable = isImmutable;
    this.parameter = parameter;
    this.uncheckedThrownTypes = uncheckedThrownTypes;
    this.block = block;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the type
   */
  public PropertyMethodType getType()
  {
    return type;
  }

  /**
   * @return the isImmutable
   */
  public boolean isImmutable()
  {
    return isImmutable;
  }

  /**
   * @return the isParameterFinal
   */
  public boolean isParameterFinal()
  {
    return isParameterFinal;
  }

  /**
   * @return the parameterName
   */
  public String getParameterName()
  {
    return parameterName;
  }

  /**
   * @return the parameterLexicalPhrase
   */
  public LexicalPhrase getParameterLexicalPhrase()
  {
    return parameterLexicalPhrase;
  }

  /**
   * @return the parameter
   */
  public Parameter getParameter()
  {
    return parameter;
  }

  /**
   * @return the uncheckedThrownTypes, or null if no thrown types were specified
   */
  public NamedType[] getUncheckedThrownTypes()
  {
    return uncheckedThrownTypes;
  }

  /**
   * @return the block
   */
  public Block getBlock()
  {
    return block;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

}
