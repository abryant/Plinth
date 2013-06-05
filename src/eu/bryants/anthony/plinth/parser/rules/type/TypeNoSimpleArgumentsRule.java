package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 25 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeNoSimpleArgumentsRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION              = new Production<ParseType>(ParseType.BASIC_TYPE);
  private static final Production<ParseType> QNAME_PRODUCTION        = new Production<ParseType>(ParseType.QNAME);
  private static final Production<ParseType> NESTED_QNAME_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST);
  private static final Production<ParseType> ARRAY_ARGS_PRODUCTION = new Production<ParseType>(ParseType.ARRAY_TYPE_TRAILING_ARGS);

  public TypeNoSimpleArgumentsRule()
  {
    super(ParseType.TYPE_NO_SIMPLE_ARGUMENTS, PRODUCTION, QNAME_PRODUCTION, NESTED_QNAME_PRODUCTION, ARRAY_ARGS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION || production == ARRAY_ARGS_PRODUCTION)
    {
      return args[0];
    }
    if (production == QNAME_PRODUCTION)
    {
      QName qname = (QName) args[0];
      return new QNameElement(qname, qname.getLexicalPhrase()).convertToType();
    }
    if (production == NESTED_QNAME_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      return element.convertToType();
    }
    throw badTypeList();
  }

}
