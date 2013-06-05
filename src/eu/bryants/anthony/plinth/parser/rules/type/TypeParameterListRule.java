package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 24 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeParameterListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.TYPE_PARAMETER);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.TYPE_PARAMETER_LIST, ParseType.COMMA, ParseType.TYPE_PARAMETER);

  public TypeParameterListRule()
  {
    super(ParseType.TYPE_PARAMETER_LIST, START_PRODUCTION, CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      TypeParameter typeParameter = (TypeParameter) args[0];
      return new ParseList<TypeParameter>(typeParameter, typeParameter.getLexicalPhrase());
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<TypeParameter> list = (ParseList<TypeParameter>) args[0];
      TypeParameter typeParameter = (TypeParameter) args[2];
      list.addLast(typeParameter, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], typeParameter.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
