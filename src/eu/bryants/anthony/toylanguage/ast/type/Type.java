package eu.bryants.anthony.toylanguage.ast.type;

import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Member;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public abstract class Type
{
  private LexicalPhrase lexicalPhrase;

  private boolean nullable;

  /**
   * Creates a new Type with the specified nullability and LexicalPhrase
   * @param nullable - true if this Type should be nullable, false otherwise
   * @param lexicalPhrase - the LexicalPhrase of this Type
   */
  public Type(boolean nullable, LexicalPhrase lexicalPhrase)
  {
    this.lexicalPhrase = lexicalPhrase;
    this.nullable = nullable;
  }

  /**
   * @return true if this Type is nullable, false otherwise
   */
  public boolean isNullable()
  {
    return nullable;
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
   * Checks whether the specified type is absolutely equivalent to the specified type.
   * @param type - the type to check
   * @return true iff this type and the specified type are equivalent
   */
  public abstract boolean isEquivalent(Type type);

  /**
   * Returns a set of the Members of this type with the specified name
   * @param name - the name of the Members to get
   * @return the Members with the specified name, or the empty set if none exist
   */
  public abstract Set<Member> getMembers(String name);

  /**
   * @return the mangled name of this type
   */
  public abstract String getMangledName();
}
