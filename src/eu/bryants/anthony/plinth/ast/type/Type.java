package eu.bryants.anthony.plinth.ast.type;

import java.util.Set;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.Member;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.member.Method.Disambiguator;

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
   * Checks whether this type is absolutely equivalent to the specified type.
   * @param type - the type to check
   * @return true iff this type and the specified type are equivalent
   */
  public abstract boolean isEquivalent(Type type);

  /**
   * Checks whether this type is equivalent to the specified type at runtime.
   * Having this as a separate concept from absolute equivalence allows us to permit type erasure for certain things,
   * such as checked exceptions on function types.
   * @param type - the type to check equivalence with
   * @return true iff this type and the specified type are runtime-equivalent
   */
  public abstract boolean isRuntimeEquivalent(Type type);

  /**
   * Returns a set of the Members of this type with the specified name
   * @param name - the name of the Members to get
   * @return the Members with the specified name, or the empty set if none exist
   */
  public abstract Set<Member> getMembers(String name);

  /**
   * Finds the method in this type with the specified disambiguator.
   * @param disambiguator - the Disambiguator to search for
   * @return the Method found, or null if no method exists with the specified signature
   */
  public Method getMethod(Disambiguator disambiguator)
  {
    for (Member member : getMembers(disambiguator.getName()))
    {
      if (member instanceof Method && ((Method) member).getDisambiguator().matches(disambiguator))
      {
        return (Method) member;
      }
    }
    return null;
  }

  /**
   * @return the mangled name of this type
   */
  public abstract String getMangledName();

  /**
   * @return true if this type has a default null value (i.e. 0 for uint, null for ?[]Foo, etc.)
   */
  public abstract boolean hasDefaultValue();
}
