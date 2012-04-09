package eu.bryants.anthony.toylanguage.parser.rules;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.Expression;
import eu.bryants.anthony.toylanguage.ast.Name;
import eu.bryants.anthony.toylanguage.ast.Type;
import eu.bryants.anthony.toylanguage.ast.VariableDefinition;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class VariableDefinitionRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> DECLARATION_PRODUCTION = new Production<ParseType>(ParseType.TYPE, ParseType.NAME, ParseType.SEMICOLON);
  private static final Production<ParseType> DEFINTION_PRODUCTION = new Production<ParseType>(ParseType.TYPE, ParseType.NAME, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.SEMICOLON);

  @SuppressWarnings("unchecked")
  public VariableDefinitionRule()
  {
    super(ParseType.VARIABLE_DEFINITION_STATEMENT, DECLARATION_PRODUCTION, DEFINTION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == DECLARATION_PRODUCTION)
    {
      Type type = (Type) args[0];
      Name name = (Name) args[1];
      return new VariableDefinition(type, name, null,
                                    LexicalPhrase.combine(type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    if (production == DEFINTION_PRODUCTION)
    {
      Type type = (Type) args[0];
      Name name = (Name) args[1];
      Expression expression = (Expression) args[3];
      return new VariableDefinition(type, name, expression,
                                    LexicalPhrase.combine(type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2], expression.getLexicalPhrase(), (LexicalPhrase) args[4]));
    }
    throw badTypeList();
  }

}
