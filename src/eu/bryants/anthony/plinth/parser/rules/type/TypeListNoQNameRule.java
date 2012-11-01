package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 4 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class TypeListNoQNameRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> QNAME_PRODUCTION                 = new Production<ParseType>(ParseType.QNAME,             ParseType.COMMA, ParseType.TYPE_LIST_NO_QNAME);
  private static final Production<ParseType> NESTED_QNAME_LIST_PRODUCTION     = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.COMMA, ParseType.TYPE_LIST_NO_QNAME);
  private static final Production<ParseType> TYPE_NO_QNAME_PRODUCTION         = new Production<ParseType>(ParseType.TYPE_NO_QNAME,     ParseType.COMMA, ParseType.TYPE_LIST_NO_QNAME);
  private static final Production<ParseType> END_QNAME_LIST_PRODUCTION        = new Production<ParseType>(ParseType.TYPE_NO_QNAME,     ParseType.COMMA, ParseType.QNAME_LIST);
  private static final Production<ParseType> END_PRODUCTION                   = new Production<ParseType>(ParseType.TYPE_NO_QNAME);

  public TypeListNoQNameRule()
  {
    super(ParseType.TYPE_LIST_NO_QNAME, QNAME_PRODUCTION, NESTED_QNAME_LIST_PRODUCTION, TYPE_NO_QNAME_PRODUCTION, END_QNAME_LIST_PRODUCTION, END_PRODUCTION);
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
      @SuppressWarnings("unchecked")
      ParseList<Type> list = (ParseList<Type>) args[2];
      list.addFirst(new NamedType(false, qname, qname.getLexicalPhrase()), LexicalPhrase.combine(qname.getLexicalPhrase(), (LexicalPhrase) args[1], list.getLexicalPhrase()));
      return list;
    }
    if (production == NESTED_QNAME_LIST_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      Type type = element.convertToType();
      @SuppressWarnings("unchecked")
      ParseList<Type> list = (ParseList<Type>) args[2];
      list.addFirst(type, LexicalPhrase.combine(type.getLexicalPhrase(), (LexicalPhrase) args[1], list.getLexicalPhrase()));
      return list;
    }
    if (production == TYPE_NO_QNAME_PRODUCTION)
    {
      Type type = (Type) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Type> list = (ParseList<Type>) args[2];
      list.addFirst(type, LexicalPhrase.combine(type.getLexicalPhrase(), (LexicalPhrase) args[1], list.getLexicalPhrase()));
      return list;
    }
    if (production == END_QNAME_LIST_PRODUCTION)
    {
      Type type = (Type) args[0];
      @SuppressWarnings("unchecked")
      ParseList<QNameElement> qnameList = (ParseList<QNameElement>) args[2];
      QNameElement[] elements = qnameList.toArray(new QNameElement[qnameList.size()]);
      Type[] types = new Type[1 + elements.length];
      types[0] = type;
      for (int i = 0; i < elements.length; ++i)
      {
        types[i + 1] = elements[i].convertToType();
      }
      return new ParseList<Type>(types, LexicalPhrase.combine(type.getLexicalPhrase(), (LexicalPhrase) args[1], qnameList.getLexicalPhrase()));
    }
    if (production == END_PRODUCTION)
    {
      Type type = (Type) args[0];
      return new ParseList<Type>(type, type.getLexicalPhrase());
    }
    throw badTypeList();
  }

}
