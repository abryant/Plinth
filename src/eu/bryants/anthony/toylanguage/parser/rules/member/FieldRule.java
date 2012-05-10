package eu.bryants.anthony.toylanguage.parser.rules.member;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class FieldRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.TYPE, ParseType.NAME, ParseType.SEMICOLON);

  @SuppressWarnings("unchecked")
  public FieldRule()
  {
    super(ParseType.FIELD, PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      Type type = (Type) args[0];
      Name name = (Name) args[1];
      return new Field(type, name.getName(), LexicalPhrase.combine(type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    throw badTypeList();
  }

}
