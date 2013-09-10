package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.DefaultParameter;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.FunctionTypeParameterList;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;

/*
 * Created on 8 Sep 2013
 */

/**
 * @author Anthony Bryant
 */
public class FunctionTypeParametersRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> TYPE_PRODUCTION = new Production<ParseType>(ParseType.TYPE_NOT_QNAME);
  private static final Production<ParseType> QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME);
  private static final Production<ParseType> NESTED_QNAME_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST);
  private static final Production<ParseType> DEFAULT_LIST_PRODUCTION = new Production<ParseType>(ParseType.FUNCTION_TYPE_DEFAULT_PARAMETER_LIST);
  private static final Production<ParseType> TYPE_CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.TYPE_NOT_QNAME, ParseType.COMMA, ParseType.FUNCTION_TYPE_PARAMETERS);
  private static final Production<ParseType> QNAME_CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.QNAME, ParseType.COMMA, ParseType.FUNCTION_TYPE_PARAMETERS);
  private static final Production<ParseType> NESTED_QNAME_CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.NESTED_QNAME_LIST, ParseType.COMMA, ParseType.FUNCTION_TYPE_PARAMETERS);

  public FunctionTypeParametersRule()
  {
    super(ParseType.FUNCTION_TYPE_PARAMETERS, TYPE_PRODUCTION, QNAME_PRODUCTION, NESTED_QNAME_PRODUCTION, DEFAULT_LIST_PRODUCTION,
                                              TYPE_CONTINUATION_PRODUCTION, QNAME_CONTINUATION_PRODUCTION, NESTED_QNAME_CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == TYPE_PRODUCTION)
    {
      Type type = (Type) args[0];
      ParseList<Type> list = new ParseList<Type>(type, type.getLexicalPhrase());
      return new FunctionTypeParameterList(list, new DefaultParameter[0], type.getLexicalPhrase());
    }
    if (production == QNAME_PRODUCTION)
    {
      QName qname = (QName) args[0];
      QNameElement element = new QNameElement(qname, qname.getLexicalPhrase());
      Type type = element.convertToType();
      ParseList<Type> list = new ParseList<Type>(type, type.getLexicalPhrase());
      return new FunctionTypeParameterList(list, new DefaultParameter[0], type.getLexicalPhrase());
    }
    if (production == NESTED_QNAME_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      Type type = element.convertToType();
      ParseList<Type> list = new ParseList<Type>(type, type.getLexicalPhrase());
      return new FunctionTypeParameterList(list, new DefaultParameter[0], type.getLexicalPhrase());
    }
    if (production == DEFAULT_LIST_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<DefaultParameter> list = (ParseList<DefaultParameter>) args[0];
      DefaultParameter[] defaultParameters = list.toArray(new DefaultParameter[list.size()]);
      return new FunctionTypeParameterList(new ParseList<Type>(null), defaultParameters, list.getLexicalPhrase());
    }

    Type type;
    if (production == TYPE_CONTINUATION_PRODUCTION)
    {
      type = (Type) args[0];
    }
    else if (production == QNAME_CONTINUATION_PRODUCTION)
    {
      QName qname = (QName) args[0];
      QNameElement element = new QNameElement(qname, qname.getLexicalPhrase());
      type = element.convertToType();
    }
    else if (production == NESTED_QNAME_CONTINUATION_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[0];
      type = element.convertToType();
    }
    else
    {
      throw badTypeList();
    }
    FunctionTypeParameterList list = (FunctionTypeParameterList) args[2];
    ParseList<Type> nonDefaultList = list.getNonDefaultTypes();
    nonDefaultList.addFirst(type, LexicalPhrase.combine(type.getLexicalPhrase(), (LexicalPhrase) args[1], nonDefaultList.getLexicalPhrase()));
    list.setLexicalPhrase(LexicalPhrase.combine(type.getLexicalPhrase(), (LexicalPhrase) args[1], list.getLexicalPhrase()));
    return list;
  }

}
