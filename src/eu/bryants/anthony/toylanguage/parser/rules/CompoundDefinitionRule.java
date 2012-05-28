package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class CompoundDefinitionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.COMPOUND_KEYWORD, ParseType.NAME, ParseType.LBRACE, ParseType.MEMBER_LIST, ParseType.RBRACE);

  @SuppressWarnings("unchecked")
  public CompoundDefinitionRule()
  {
    super(ParseType.COMPOUND_DEFINITION, PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      Name name = (Name) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Member> members = (ParseList<Member>) args[3];
      return new CompoundDefinition(name.getName(), members.toArray(new Member[members.size()]),
                                    LexicalPhrase.combine((LexicalPhrase) args[0], name.getLexicalPhrase(), (LexicalPhrase) args[2], members.getLexicalPhrase(), (LexicalPhrase) args[4]));
    }
    throw badTypeList();
  }

}
