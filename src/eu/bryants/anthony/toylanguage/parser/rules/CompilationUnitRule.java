package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.Function;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class CompilationUnitRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.FUNCTIONS);

  @SuppressWarnings("unchecked")
  public CompilationUnitRule()
  {
    super(ParseType.COMPILATION_UNIT, PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Function> functions = (ParseList<Function>) args[0];
      return new CompilationUnit(functions.toArray(new Function[functions.size()]), functions.getLexicalPhrase());
    }
    throw badTypeList();
  }

}
