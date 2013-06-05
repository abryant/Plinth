package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class TypeRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME);
  private static final Production<ParseType> NESTED_QNAME_LIST_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST);
  private static final Production<ParseType> TYPE_NO_QNAME_PRODUCTION = new Production<ParseType>(ParseType.TYPE_NOT_QNAME);

  public TypeRule()
  {
    super(ParseType.TYPE, QNAME_PRODUCTION, NESTED_QNAME_LIST_PRODUCTION, TYPE_NO_QNAME_PRODUCTION);
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
      return new NamedType(false, false, qname, null, qname.getLexicalPhrase());
    }
    if (production == NESTED_QNAME_LIST_PRODUCTION)
    {
      QNameElement qnameElement = (QNameElement) args[0];
      return qnameElement.convertToType();
    }
    if (production == TYPE_NO_QNAME_PRODUCTION)
    {
      return args[0];
    }
    throw badTypeList();
  }

}
