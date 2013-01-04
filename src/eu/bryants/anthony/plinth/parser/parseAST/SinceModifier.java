package eu.bryants.anthony.plinth.parser.parseAST;

import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;

/*
 * Created on 2 Jan 2013
 */

/**
 * @author Anthony Bryant
 */
public class SinceModifier extends Modifier
{

  private SinceSpecifier sinceSpecifier;

  /**
   * Creates a new since modifier for the specified since specifier
   * @param sinceSpecifier - the SinceSpecifier to represent
   */
  public SinceModifier(SinceSpecifier sinceSpecifier)
  {
    super(ModifierType.SINCE, sinceSpecifier.getLexicalPhrase());
    this.sinceSpecifier = sinceSpecifier;
  }

  /**
   * @return the sinceSpecifier
   */
  public SinceSpecifier getSinceSpecifier()
  {
    return sinceSpecifier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return sinceSpecifier.toString();
  }
}
