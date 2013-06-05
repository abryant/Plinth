package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseContainer;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 23 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeArgumentListDoubleRAngleRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> END_PRODUCTION               = new Production<ParseType>(ParseType.TYPE_ARGUMENT_DOUBLE_RANGLE);
  private static final Production<ParseType> LIST_PRODUCTION              = new Production<ParseType>(ParseType.TYPE_ARGUMENT_NOT_QNAME, ParseType.COMMA, ParseType.TYPE_ARGUMENT_LIST_DOUBLE_RANGLE);
  private static final Production<ParseType> QNAME_LIST_PRODUCTION        = new Production<ParseType>(ParseType.QNAME,                   ParseType.COMMA, ParseType.TYPE_ARGUMENT_LIST_DOUBLE_RANGLE);
  private static final Production<ParseType> NESTED_QNAME_LIST_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST,       ParseType.COMMA, ParseType.TYPE_ARGUMENT_LIST_DOUBLE_RANGLE);

  public TypeArgumentListDoubleRAngleRule()
  {
    super(ParseType.TYPE_ARGUMENT_LIST_DOUBLE_RANGLE, END_PRODUCTION, LIST_PRODUCTION, QNAME_LIST_PRODUCTION, NESTED_QNAME_LIST_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == END_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseContainer<ParseContainer<Type>> containerContainer = (ParseContainer<ParseContainer<Type>>) args[0];
      ParseContainer<Type> container = containerContainer.getItem();
      Type typeArgument = container.getItem();
      ParseList<Type> list = new ParseList<Type>(typeArgument, typeArgument.getLexicalPhrase());
      ParseContainer<ParseList<Type>> containedList = new ParseContainer<ParseList<Type>>(list, container.getLexicalPhrase());
      return new ParseContainer<ParseContainer<ParseList<Type>>>(containedList, containerContainer.getLexicalPhrase());
    }
    Type typeArgument;
    if (production == LIST_PRODUCTION)
    {
      typeArgument = (Type) args[0];
    }
    else if (production == QNAME_LIST_PRODUCTION)
    {
      QName qname = (QName) args[0];
      typeArgument = new QNameElement(qname, qname.getLexicalPhrase()).convertToType();
    }
    else if (production == NESTED_QNAME_LIST_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      typeArgument = element.convertToType();
    }
    else
    {
      throw badTypeList();
    }
    @SuppressWarnings("unchecked")
    ParseContainer<ParseContainer<ParseList<Type>>> containedContainedList = (ParseContainer<ParseContainer<ParseList<Type>>>) args[2];
    ParseContainer<ParseList<Type>> containedList = containedContainedList.getItem();
    ParseList<Type> list = containedList.getItem();
    list.addFirst(typeArgument, LexicalPhrase.combine(typeArgument.getLexicalPhrase(), (LexicalPhrase) args[1], list.getLexicalPhrase()));
    ParseContainer<ParseList<Type>> newContainedList = new ParseContainer<ParseList<Type>>(list, LexicalPhrase.combine(typeArgument.getLexicalPhrase(), (LexicalPhrase) args[1], containedList.getLexicalPhrase()));
    return new ParseContainer<ParseContainer<ParseList<Type>>>(newContainedList, LexicalPhrase.combine(typeArgument.getLexicalPhrase(), (LexicalPhrase) args[1], containedContainedList.getLexicalPhrase()));
  }

}
