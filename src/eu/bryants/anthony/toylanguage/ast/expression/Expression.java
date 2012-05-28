package eu.bryants.anthony.toylanguage.ast.expression;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.type.Type;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class Expression
{

  private Type type;

  private LexicalPhrase lexicalPhrase;

  public Expression(LexicalPhrase lexicalPhrase)
  {
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the type
   */
  public Type getType()
  {
    return type;
  }

  /**
   * @param type - the type to set
   */
  public void setType(Type type)
  {
    this.type = type;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }
}
