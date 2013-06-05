package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseContainer;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 24 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeParameterListRAngleRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> SINGLE_PRODUCTION = new Production<ParseType>(ParseType.TYPE_PARAMETER_RANGLE);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.TYPE_PARAMETER_LIST, ParseType.COMMA, ParseType.TYPE_PARAMETER_RANGLE);

  public TypeParameterListRAngleRule()
  {
    super(ParseType.TYPE_PARAMETER_LIST_RANGLE, SINGLE_PRODUCTION, CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == SINGLE_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseContainer<TypeParameter> containedParameter = (ParseContainer<TypeParameter>) args[0];
      TypeParameter typeParameter = containedParameter.getItem();
      ParseList<TypeParameter> list = new ParseList<TypeParameter>(typeParameter, typeParameter.getLexicalPhrase());
      return new ParseContainer<ParseList<TypeParameter>>(list, containedParameter.getLexicalPhrase());
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<TypeParameter> list = (ParseList<TypeParameter>) args[0];
      LexicalPhrase listLexicalPhrase = list.getLexicalPhrase();
      @SuppressWarnings("unchecked")
      ParseContainer<TypeParameter> containedParameter = (ParseContainer<TypeParameter>) args[2];
      TypeParameter typeParameter = containedParameter.getItem();
      list.addLast(typeParameter, LexicalPhrase.combine(listLexicalPhrase, (LexicalPhrase) args[1], typeParameter.getLexicalPhrase()));
      return new ParseContainer<ParseList<TypeParameter>>(list, LexicalPhrase.combine(listLexicalPhrase, (LexicalPhrase) args[1], containedParameter.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
