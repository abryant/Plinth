package eu.bryants.anthony.plinth.parser.parseAST;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.NamedType;

/*
 * Created on 20 Feb 2013
 */

/**
 * A holder for elements of a throws clause during parsing.
 * @author Anthony Bryant
 */
public class ThrownExceptionType
{

  private NamedType type;
  private boolean isUnchecked;

  private LexicalPhrase lexicalPhrase;

  public ThrownExceptionType(NamedType type, boolean isUnchecked, LexicalPhrase lexicalPhrase)
  {
    this.type = type;
    this.isUnchecked = isUnchecked;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the type
   */
  public NamedType getType()
  {
    return type;
  }

  /**
   * @return the isUnchecked
   */
  public boolean isUnchecked()
  {
    return isUnchecked;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

}
