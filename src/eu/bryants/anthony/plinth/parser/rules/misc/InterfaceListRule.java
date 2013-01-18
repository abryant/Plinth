package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 8 Jan 2013
 */

/**
 * @author Anthony Bryant
 */
public class InterfaceListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.QNAME);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.INTERFACE_LIST, ParseType.COMMA, ParseType.QNAME);

  public InterfaceListRule()
  {
    super(ParseType.INTERFACE_LIST, START_PRODUCTION, CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      QName qname = (QName) args[0];
      return new ParseList<QName>(qname, qname.getLexicalPhrase());
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<QName> list = (ParseList<QName>) args[0];
      QName qname = (QName) args[2];
      list.addLast(qname, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], qname.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
