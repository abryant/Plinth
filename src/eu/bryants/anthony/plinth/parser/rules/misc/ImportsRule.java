package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.Import;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 13 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class ImportsRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> EMPTY_PRODUCTION    = new Production<ParseType>();
  private static final Production<ParseType> IMPORT_PRODUCTION   = new Production<ParseType>(ParseType.IMPORTS, ParseType.IMPORT_KEYWORD, ParseType.QNAME, ParseType.SEMICOLON);
  private static final Production<ParseType> WILDCARD_PRODUCTION = new Production<ParseType>(ParseType.IMPORTS, ParseType.IMPORT_KEYWORD, ParseType.QNAME, ParseType.DOT, ParseType.STAR, ParseType.SEMICOLON);

  public ImportsRule()
  {
    super(ParseType.IMPORTS, EMPTY_PRODUCTION, IMPORT_PRODUCTION, WILDCARD_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == EMPTY_PRODUCTION)
    {
      return new ParseList<Import>(null);
    }
    if (production == IMPORT_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Import> list = (ParseList<Import>) args[0];
      QName qname = (QName) args[2];
      Import newImport = new Import(qname.getLastName(), qname, qname.getLexicalPhrase());
      list.addLast(newImport, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], qname.getLexicalPhrase(), (LexicalPhrase) args[3]));
      return list;
    }
    if (production == WILDCARD_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Import> list = (ParseList<Import>) args[0];
      QName qname = (QName) args[2];
      Import newImport = new Import(null, qname, qname.getLexicalPhrase());
      list.addLast(newImport, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], qname.getLexicalPhrase(), (LexicalPhrase) args[3], (LexicalPhrase) args[4], (LexicalPhrase) args[5]));
      return list;
    }
    throw badTypeList();
  }

}
