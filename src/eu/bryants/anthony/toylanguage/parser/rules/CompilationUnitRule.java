package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.misc.QName;
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

  private static Production<ParseType> BLANK_PRODUCTION = new Production<ParseType>();
  private static Production<ParseType> PACKAGE_PRODUCTION = new Production<ParseType>(ParseType.PACKAGE_KEYWORD, ParseType.QNAME, ParseType.SEMICOLON);
  private static Production<ParseType> COMPOUND_PRODUCTION = new Production<ParseType>(ParseType.COMPILATION_UNIT, ParseType.COMPOUND_DEFINITION);

  @SuppressWarnings("unchecked")
  public CompilationUnitRule()
  {
    super(ParseType.COMPILATION_UNIT, BLANK_PRODUCTION, PACKAGE_PRODUCTION, COMPOUND_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == BLANK_PRODUCTION)
    {
      return new CompilationUnit(null, null);
    }
    if (production == PACKAGE_PRODUCTION)
    {
      QName qname = (QName) args[1];
      return new CompilationUnit(qname, LexicalPhrase.combine((LexicalPhrase) args[0], qname.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    if (production == COMPOUND_PRODUCTION)
    {
      CompilationUnit compilationUnit = (CompilationUnit) args[0];
      CompoundDefinition compound = (CompoundDefinition) args[1];
      compilationUnit.addCompound(compound, LexicalPhrase.combine(compilationUnit.getLexicalPhrase(), compound.getLexicalPhrase()));
      return compilationUnit;
    }
    throw badTypeList();
  }

}
