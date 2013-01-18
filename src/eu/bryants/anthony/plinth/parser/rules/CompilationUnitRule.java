package eu.bryants.anthony.plinth.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.ClassDefinition;
import eu.bryants.anthony.plinth.ast.CompilationUnit;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.InterfaceDefinition;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.Import;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

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
  private static Production<ParseType> CLASS_PRODUCTION = new Production<ParseType>(ParseType.COMPILATION_UNIT, ParseType.CLASS_DEFINITION);
  private static Production<ParseType> COMPOUND_PRODUCTION = new Production<ParseType>(ParseType.COMPILATION_UNIT, ParseType.COMPOUND_DEFINITION);
  private static Production<ParseType> INTERFACE_PRODUCTION = new Production<ParseType>(ParseType.COMPILATION_UNIT, ParseType.INTERFACE_DEFINITION);

  public CompilationUnitRule()
  {
    super(ParseType.COMPILATION_UNIT, IMPORTS_PRODUCTION, PACKAGE_IMPORTS_PRODUCTION, CLASS_PRODUCTION, COMPOUND_PRODUCTION, INTERFACE_PRODUCTION);
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
    if (production == CLASS_PRODUCTION)
    {
      CompilationUnit compilationUnit = (CompilationUnit) args[0];
      ClassDefinition classDefinition = (ClassDefinition) args[1];
      compilationUnit.addType(classDefinition, LexicalPhrase.combine(compilationUnit.getLexicalPhrase(), classDefinition.getLexicalPhrase()));
      return compilationUnit;
    }
    if (production == COMPOUND_PRODUCTION)
    {
      CompilationUnit compilationUnit = (CompilationUnit) args[0];
      CompoundDefinition compoundDefinition = (CompoundDefinition) args[1];
      compilationUnit.addType(compoundDefinition, LexicalPhrase.combine(compilationUnit.getLexicalPhrase(), compoundDefinition.getLexicalPhrase()));
      return compilationUnit;
    }
    if (production == INTERFACE_PRODUCTION)
    {
      CompilationUnit compilationUnit = (CompilationUnit) args[0];
      InterfaceDefinition interfaceDefinition = (InterfaceDefinition) args[1];
      compilationUnit.addType(interfaceDefinition, LexicalPhrase.combine(compilationUnit.getLexicalPhrase(), interfaceDefinition.getLexicalPhrase()));
      return compilationUnit;
    }
    throw badTypeList();
  }

}
