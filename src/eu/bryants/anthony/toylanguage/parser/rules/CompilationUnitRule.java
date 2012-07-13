package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.misc.Import;
import eu.bryants.anthony.toylanguage.ast.misc.QName;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class CompilationUnitRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static Production<ParseType> IMPORTS_PRODUCTION = new Production<ParseType>(ParseType.IMPORTS);
  private static Production<ParseType> PACKAGE_IMPORTS_PRODUCTION = new Production<ParseType>(ParseType.PACKAGE_KEYWORD, ParseType.QNAME, ParseType.SEMICOLON, ParseType.IMPORTS);
  private static Production<ParseType> COMPOUND_PRODUCTION = new Production<ParseType>(ParseType.COMPILATION_UNIT, ParseType.COMPOUND_DEFINITION);

  @SuppressWarnings("unchecked")
  public CompilationUnitRule()
  {
    super(ParseType.COMPILATION_UNIT, IMPORTS_PRODUCTION, PACKAGE_IMPORTS_PRODUCTION, COMPOUND_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == IMPORTS_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Import> list = (ParseList<Import>) args[0];
      return new CompilationUnit(null, list.toArray(new Import[list.size()]), list.getLexicalPhrase());
    }
    if (production == PACKAGE_IMPORTS_PRODUCTION)
    {
      QName qname = (QName) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Import> list = (ParseList<Import>) args[3];
      return new CompilationUnit(qname, list.toArray(new Import[list.size()]), LexicalPhrase.combine((LexicalPhrase) args[0], qname.getLexicalPhrase(), (LexicalPhrase) args[2], list.getLexicalPhrase()));
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
