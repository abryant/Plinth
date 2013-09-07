package eu.bryants.anthony.plinth.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.misc.Argument;
import eu.bryants.anthony.plinth.ast.misc.NormalArgument;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 7 Sep 2013
 */

/**
 * @author Anthony Bryant
 */
public class ArgumentListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION       = new Production<ParseType>(ParseType.CONDITIONAL_EXPRESSION);
  private static final Production<ParseType> START_QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME_OR_LESS_THAN_EXPRESSION);
  private static final Production<ParseType> CONTINUATION_PRODUCTION       = new Production<ParseType>(ParseType.ARGUMENT_LIST, ParseType.COMMA, ParseType.CONDITIONAL_EXPRESSION);
  private static final Production<ParseType> CONTINUATION_QNAME_PRODUCTION = new Production<ParseType>(ParseType.ARGUMENT_LIST, ParseType.COMMA, ParseType.QNAME_OR_LESS_THAN_EXPRESSION);

  public ArgumentListRule()
  {
    super(ParseType.ARGUMENT_LIST, START_PRODUCTION, START_QNAME_PRODUCTION, CONTINUATION_PRODUCTION, CONTINUATION_QNAME_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION || production == START_QNAME_PRODUCTION)
    {
      Expression expression = (Expression) args[0];
      Argument argument = new NormalArgument(expression, expression.getLexicalPhrase());
      return new ParseList<Argument>(argument, argument.getLexicalPhrase());
    }
    if (production == CONTINUATION_PRODUCTION || production == CONTINUATION_QNAME_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Argument> list = (ParseList<Argument>) args[0];
      Expression expression = (Expression) args[2];
      Argument argument = new NormalArgument(expression, expression.getLexicalPhrase());
      list.addLast(argument, LexicalPhrase.combine(list.getLexicalPhrase(), (LexicalPhrase) args[1], argument.getLexicalPhrase()));
      return list;
    }
    throw badTypeList();
  }

}
