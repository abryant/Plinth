package eu.bryants.anthony.toylanguage.parser.parseAST;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;

/*
 * Created on 28 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class Modifier
{
  private ModifierType modifierType;
  private LexicalPhrase lexicalPhrase;

  public Modifier(ModifierType modifierType, LexicalPhrase lexicalPhrase)
  {
    this.modifierType = modifierType;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the modifierType
   */
  public ModifierType getModifierType()
  {
    return modifierType;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return modifierType.toString();
  }
}
