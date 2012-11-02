package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 1 Nov 2012
 */

/**
 * @author Anthony Bryant
 */
public class ArgumentsRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION         = new Production<>(ParseType.LPAREN, ParseType.EXPRESSION_LIST, ParseType.RPAREN);
  private static final Production<ParseType> NO_ARGS_PRODUCTION = new Production<>(ParseType.LPAREN,                            ParseType.RPAREN);

  public ArgumentsRule()
  {
    super(ParseType.ARGUMENTS, PRODUCTION, NO_ARGS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Expression> list = (ParseList<Expression>) args[1];
      list.setLexicalPhrase(LexicalPhrase.combine((LexicalPhrase) args[0], list.getLexicalPhrase(), (LexicalPhrase) args[2]));
      return list;
    }
    if (production == NO_ARGS_PRODUCTION)
    {
      return new ParseList<Expression>(LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1]));
    }
    throw badTypeList();
  }

}
