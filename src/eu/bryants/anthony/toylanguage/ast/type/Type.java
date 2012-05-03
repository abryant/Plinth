package eu.bryants.anthony.toylanguage.ast.type;

import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class Type
{
  private LexicalPhrase lexicalPhrase;

  public Type(LexicalPhrase lexicalPhrase)
  {
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

  /**
   * Checks whether the specified type can be assigned to a variable of this type.
   * @param type - the type to check
   * @return true iff the specified type can be assigned to a variable of this type
   */
  public abstract boolean canAssign(Type type);

  /**
   * Returns the Member of this type with the specified name
   * @param name - the name of the Member to get
   * @return the Member with the specified name, or null if none exists
   */
  public abstract Member getMember(String name);
}
