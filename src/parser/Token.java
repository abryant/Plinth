package parser;

/*
 * Created on 5 Apr 2010
 */

/**
 * A token read by the tokenizer or generated by the parser.
 * Each token has an associated type which corresponds to the types used in the rules that this token can be part of.
 *
 * @author Anthony Bryant
 * @param <T> - the enum type that holds all possible values for the token type
 *
 */
public final class Token<T extends Enum<T>>
{

  private T type;
  private Object value;

  /**
   * Creates a new Token with the specified type and value
   * @param type - the type of this token
   * @param value - the value of this token
   */
  public Token(T type, Object value)
  {
    this.type = type;
    this.value = value;
  }

  /**
   * @return the type of this token
   */
  public T getType()
  {
    return type;
  }

  /**
   * @return the value of this token
   */
  public Object getValue()
  {
    return value;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return type.toString();
  }

}
