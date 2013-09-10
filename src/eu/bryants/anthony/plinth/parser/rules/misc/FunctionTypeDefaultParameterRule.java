package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.DefaultParameter;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 8 Sep 2013
 */

/**
 * @author Anthony Bryant
 */
public class FunctionTypeDefaultParameterRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.TYPE_NOT_QNAME, ParseType.NAME, ParseType.EQUALS, ParseType.ELLIPSIS);
  private static final Production<ParseType> QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME, ParseType.NAME, ParseType.EQUALS, ParseType.ELLIPSIS);
  private static final Production<ParseType> NESTED_QNAME_LIST_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.NAME, ParseType.EQUALS, ParseType.ELLIPSIS);

  public FunctionTypeDefaultParameterRule()
  {
    super(ParseType.FUNCTION_TYPE_DEFAULT_PARAMETER, PRODUCTION, QNAME_PRODUCTION, NESTED_QNAME_LIST_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      Type type = (Type) args[0];
      Name name = (Name) args[1];
      return new DefaultParameter(type, name.getName(), null, LexicalPhrase.combine(type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2], (LexicalPhrase) args[3]));
    }
    if (production == QNAME_PRODUCTION)
    {
      QName qname = (QName) args[0];
      QNameElement qnameElement = new QNameElement(qname, qname.getLexicalPhrase());
      Type type = qnameElement.convertToType();
      Name name = (Name) args[1];
      return new DefaultParameter(type, name.getName(), null, LexicalPhrase.combine(type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2], (LexicalPhrase) args[3]));
    }
    if (production == NESTED_QNAME_LIST_PRODUCTION)
    {
      QNameElement qnameElement = (QNameElement) args[0];
      Type type = qnameElement.convertToType();
      Name name = (Name) args[1];
      return new DefaultParameter(type, name.getName(), null, LexicalPhrase.combine(type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2], (LexicalPhrase) args[3]));
    }
    throw badTypeList();
  }

}
