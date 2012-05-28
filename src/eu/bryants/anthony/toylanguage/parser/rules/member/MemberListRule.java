package eu.bryants.anthony.toylanguage.parser.rules.member;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.parser.ParseList;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class MemberListRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> EMPTY_PRODUCTION = new Production<ParseType>();
  private static final Production<ParseType> FIELD_PRODUCTION = new Production<ParseType>(ParseType.MEMBER_LIST, ParseType.FIELD);
  private static final Production<ParseType> CONSTRUCTOR_PRODUCTION = new Production<ParseType>(ParseType.MEMBER_LIST, ParseType.CONSTRUCTOR);
  private static final Production<ParseType> METHOD_PRODUCTION = new Production<ParseType>(ParseType.MEMBER_LIST, ParseType.METHOD);

  @SuppressWarnings("unchecked")
  public MemberListRule()
  {
    super(ParseType.MEMBER_LIST, EMPTY_PRODUCTION, FIELD_PRODUCTION, CONSTRUCTOR_PRODUCTION, METHOD_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == EMPTY_PRODUCTION)
    {
      return new ParseList<Member>(null);
    }
    if (production == FIELD_PRODUCTION || production == CONSTRUCTOR_PRODUCTION || production == METHOD_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Member> members = (ParseList<Member>) args[0];
      Member member = (Member) args[1];
      members.addLast(member, LexicalPhrase.combine(members.getLexicalPhrase(), member.getLexicalPhrase()));
      return members;
    }
    throw badTypeList();
  }

}
