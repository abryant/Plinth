package eu.bryants.anthony.toylanguage.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.statement.PrefixIncDecStatement;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 8 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class PrefixIncDecStatementRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> INC_PRODUCTION = new Production<ParseType>(ParseType.DOUBLE_PLUS, ParseType.ASSIGNEE, ParseType.SEMICOLON);
  private static final Production<ParseType> DEC_PRODUCTION = new Production<ParseType>(ParseType.DOUBLE_MINUS, ParseType.ASSIGNEE, ParseType.SEMICOLON);

  @SuppressWarnings("unchecked")
  public PrefixIncDecStatementRule()
  {
    super(ParseType.PREFIX_INC_DEC_STATEMENT, INC_PRODUCTION, DEC_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    boolean increment;
    if (production == INC_PRODUCTION)
    {
      increment = true;
    }
    else if (production == DEC_PRODUCTION)
    {
      increment = false;
    }
    else
    {
      throw badTypeList();
    }
    Assignee assignee = (Assignee) args[1];
    return new PrefixIncDecStatement(assignee, increment, LexicalPhrase.combine((LexicalPhrase) args[0], assignee.getLexicalPhrase(), (LexicalPhrase) args[2]));
  }

}
