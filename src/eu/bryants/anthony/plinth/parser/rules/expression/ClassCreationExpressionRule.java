package eu.bryants.anthony.plinth.parser.rules.expression;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.ClassCreationExpression;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 13 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class ClassCreationExpressionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.NEW_KEYWORD, ParseType.QNAME, ParseType.ARGUMENTS);

  public ClassCreationExpressionRule()
  {
    super(ParseType.CLASS_CREATION_EXPRESSION, PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      QName qname = (QName) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Expression> arguments = (ParseList<Expression>) args[2];
      return new ClassCreationExpression(qname, arguments.toArray(new Expression[arguments.size()]), LexicalPhrase.combine((LexicalPhrase) args[0], qname.getLexicalPhrase(), arguments.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
