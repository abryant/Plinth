package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 29 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class QNameListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> QNAME_PRODUCTION                          = new Production<ParseType>(ParseType.QNAME);
  private static final Production<ParseType> NESTED_QNAME_LIST_PRODUCTION              = new Production<ParseType>(ParseType.NESTED_QNAME_LIST);
  private static final Production<ParseType> QNAME_CONTINUATION_PRODUCTION             = new Production<ParseType>(ParseType.QNAME, ParseType.COMMA, ParseType.QNAME_LIST);
  private static final Production<ParseType> NESTED_QNAME_LIST_CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.COMMA, ParseType.QNAME_LIST);

  @SuppressWarnings("unchecked")
  public QNameListRule()
  {
    super(ParseType.QNAME_LIST, QNAME_PRODUCTION, NESTED_QNAME_LIST_PRODUCTION,
                                QNAME_CONTINUATION_PRODUCTION, NESTED_QNAME_LIST_CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == QNAME_PRODUCTION)
    {
      QName qname = (QName) args[0];
      QNameElement element = new QNameElement(qname, qname.getLexicalPhrase());
      return new ParseList<QNameElement>(element, element.getLexicalPhrase());
    }
    if (production == NESTED_QNAME_LIST_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      return new ParseList<QNameElement>(element, element.getLexicalPhrase());
    }
    if (production == QNAME_CONTINUATION_PRODUCTION)
    {
      QName qname = (QName) args[0];
      QNameElement element = new QNameElement(qname, qname.getLexicalPhrase());
      @SuppressWarnings("unchecked")
      ParseList<QNameElement> list = (ParseList<QNameElement>) args[2];
      list.addFirst(element, LexicalPhrase.combine(element.getLexicalPhrase(), (LexicalPhrase) args[1], list.getLexicalPhrase()));
      return list;
    }
    if (production == NESTED_QNAME_LIST_CONTINUATION_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      @SuppressWarnings("unchecked")
      ParseList<QNameElement> list = (ParseList<QNameElement>) args[2];
      list.addFirst(element, LexicalPhrase.combine(element.getLexicalPhrase(), (LexicalPhrase) args[1], list.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
