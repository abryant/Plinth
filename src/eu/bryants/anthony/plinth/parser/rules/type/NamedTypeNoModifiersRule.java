package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseContainer;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 21 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class NamedTypeNoModifiersRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.QNAME);
  private static final Production<ParseType> ARGUMENTS_PRODUCTION = new Production<ParseType>(ParseType.QNAME, ParseType.LANGLE, ParseType.TYPE_ARGUMENT_LIST_RANGLE);

  public NamedTypeNoModifiersRule()
  {
    super(ParseType.NAMED_TYPE_NO_MODIFIERS, PRODUCTION, ARGUMENTS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      QName qname = (QName) args[0];
      return new NamedType(false, false, qname, null, qname.getLexicalPhrase());
    }
    if (production == ARGUMENTS_PRODUCTION)
    {
      QName qname = (QName) args[0];
      @SuppressWarnings("unchecked")
      ParseContainer<ParseList<Type>> listRAngle = (ParseContainer<ParseList<Type>>) args[2];
      ParseList<Type> list = listRAngle.getItem();
      return new NamedType(false, false, qname, list.toArray(new Type[list.size()]),
                           LexicalPhrase.combine(qname.getLexicalPhrase(), (LexicalPhrase) args[1], listRAngle.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
