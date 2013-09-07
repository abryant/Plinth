package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.CreationExpression;
import eu.bryants.anthony.plinth.ast.misc.Argument;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 13 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class CreationExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> NEW_PRODUCTION    = new Production<ParseType>(ParseType.NEW_KEYWORD,    ParseType.NAMED_TYPE_NO_MODIFIERS, ParseType.ARGUMENTS);
  private static final Production<ParseType> CREATE_PRODUCTION = new Production<ParseType>(ParseType.CREATE_KEYWORD, ParseType.NAMED_TYPE_NO_MODIFIERS, ParseType.ARGUMENTS);

  public CreationExpressionRule()
  {
    super(ParseType.CREATION_EXPRESSION, NEW_PRODUCTION, CREATE_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == NEW_PRODUCTION || production == CREATE_PRODUCTION)
    {
      NamedType type = (NamedType) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Argument> arguments = (ParseList<Argument>) args[2];
      return new CreationExpression(production == NEW_PRODUCTION, type, arguments.toArray(new Argument[arguments.size()]), LexicalPhrase.combine((LexicalPhrase) args[0], type.getLexicalPhrase(), arguments.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
