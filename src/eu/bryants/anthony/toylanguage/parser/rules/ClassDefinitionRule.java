package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.ClassDefinition;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;

/*
 * Created on 12 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class ClassDefinitionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.CLASS_KEYWORD, ParseType.NAME, ParseType.LBRACE, ParseType.MEMBER_LIST, ParseType.RBRACE);

  @SuppressWarnings("unchecked")
  public ClassDefinitionRule()
  {
    super(ParseType.CLASS_DEFINITION, PRODUCTION);
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
      return new ClassDefinition(name.getName(), members.toArray(new Member[members.size()]),
                                 LexicalPhrase.combine((LexicalPhrase) args[0], name.getLexicalPhrase(), (LexicalPhrase) args[2], members.getLexicalPhrase(), (LexicalPhrase) args[4]));
    }
    throw badTypeList();
  }

}
