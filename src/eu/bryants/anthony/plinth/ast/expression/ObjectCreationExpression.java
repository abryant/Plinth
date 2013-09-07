package eu.bryants.anthony.plinth.ast.expression;

import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.Argument;
import eu.bryants.anthony.plinth.parser.LanguageParseException;

/*
 * Created on 13 Dec 2012
 */

/**
 * @author Anthony Bryant
 */
public class ObjectCreationExpression extends Expression
{

  public ObjectCreationExpression(Argument[] arguments, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    super(lexicalPhrase);
    if (arguments != null && arguments.length > 0)
    {
      throw new LanguageParseException("An object() constructor does not take any arguments", lexicalPhrase);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "new object()";
  }
}
