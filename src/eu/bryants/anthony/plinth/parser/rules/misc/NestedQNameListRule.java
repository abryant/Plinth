package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 30 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class NestedQNameListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.LPAREN, ParseType.QNAME_LIST, ParseType.RPAREN);

  public NestedQNameListRule()
  {
    super(ParseType.NESTED_QNAME_LIST, PRODUCTION);
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
      ParseList<QNameElement> list = (ParseList<QNameElement>) args[1];
      return new QNameElement(list, LexicalPhrase.combine((LexicalPhrase) args[0], list.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    throw badTypeList();
  }

}
