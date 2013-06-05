package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseContainer;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 20 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeRAngleRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.TYPE_NOT_QNAME, ParseType.RANGLE);
  private static final Production<ParseType> QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME, ParseType.RANGLE);
  private static final Production<ParseType> NESTED_QNAME_LIST_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.RANGLE);
  private static final Production<ParseType> NAMED_TYPE_PRODUCTION = new Production<ParseType>(ParseType.TYPE_TRAILING_ARGS_RANGLE);

  public TypeRAngleRule()
  {
    super(ParseType.TYPE_RANGLE, PRODUCTION, QNAME_PRODUCTION, NESTED_QNAME_LIST_PRODUCTION, NAMED_TYPE_PRODUCTION);
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
      return new ParseContainer<Type>(type, LexicalPhrase.combine(type.getLexicalPhrase(), (LexicalPhrase) args[1]));
    }
    if (production == QNAME_PRODUCTION)
    {
      QName qname = (QName) args[0];
      Type type = new QNameElement(qname, qname.getLexicalPhrase()).convertToType();
      return new ParseContainer<Type>(type, LexicalPhrase.combine(type.getLexicalPhrase(), (LexicalPhrase) args[1]));
    }
    if (production == NESTED_QNAME_LIST_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      Type type = element.convertToType();
      return new ParseContainer<Type>(type, LexicalPhrase.combine(type.getLexicalPhrase(), (LexicalPhrase) args[1]));
    }
    if (production == NAMED_TYPE_PRODUCTION)
    {
      return args[0];
    }
    throw badTypeList();
  }
}
