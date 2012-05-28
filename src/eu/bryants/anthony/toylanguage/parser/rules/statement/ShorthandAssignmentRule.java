package eu.bryants.anthony.toylanguage.parser.rules.statement;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.statement.ShorthandAssignStatement;
import eu.bryants.anthony.toylanguage.ast.statement.ShorthandAssignStatement.ShorthandAssignmentOperator;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;

/*
 * Created on 26 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ShorthandAssignmentRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> ADD_PRODUCTION         = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.PLUS_EQUALS,           ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> SUBTRACT_PRODUCTION    = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.MINUS_EQUALS,          ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> MULTIPLY_PRODUCTION    = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.STAR_EQUALS,           ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> DIVIDE_PRODUCTION      = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.FORWARD_SLASH_EQUALS,  ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> REMAINDER_PRODUCTION   = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.PERCENT_EQUALS,        ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> MODULO_PRODUCTION      = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.DOUBLE_PERCENT_EQUALS, ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> LEFT_SHIFT_PRODUCTION  = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.DOUBLE_LANGLE_EQUALS,  ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> RIGHT_SHIFT_PRODUCTION = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.DOUBLE_RANGLE_EQUALS,  ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> AND_PRODUCTION         = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.AMPERSAND_EQUALS,      ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> OR_PRODUCTION          = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.PIPE_EQUALS,           ParseType.TUPLE_EXPRESSION);
  private static final Production<ParseType> XOR_PRODUCTION         = new Production<ParseType>(ParseType.ASSIGNEE_LIST, ParseType.CARET_EQUALS,          ParseType.TUPLE_EXPRESSION);

  @SuppressWarnings("unchecked")
  public ShorthandAssignmentRule()
  {
    super(ParseType.SHORTHAND_ASSIGNMENT, ADD_PRODUCTION,
                                          SUBTRACT_PRODUCTION,
                                          MULTIPLY_PRODUCTION,
                                          DIVIDE_PRODUCTION,
                                          REMAINDER_PRODUCTION,
                                          MODULO_PRODUCTION,
                                          LEFT_SHIFT_PRODUCTION,
                                          RIGHT_SHIFT_PRODUCTION,
                                          AND_PRODUCTION,
                                          OR_PRODUCTION,
                                          XOR_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    ShorthandAssignmentOperator operator;
         if (production == ADD_PRODUCTION)         { operator = ShorthandAssignmentOperator.ADD;         }
    else if (production == SUBTRACT_PRODUCTION)    { operator = ShorthandAssignmentOperator.SUBTRACT;    }
    else if (production == MULTIPLY_PRODUCTION)    { operator = ShorthandAssignmentOperator.MULTIPLY;    }
    else if (production == DIVIDE_PRODUCTION)      { operator = ShorthandAssignmentOperator.DIVIDE;      }
    else if (production == REMAINDER_PRODUCTION)   { operator = ShorthandAssignmentOperator.REMAINDER;   }
    else if (production == MODULO_PRODUCTION)      { operator = ShorthandAssignmentOperator.MODULO;      }
    else if (production == LEFT_SHIFT_PRODUCTION)  { operator = ShorthandAssignmentOperator.LEFT_SHIFT;  }
    else if (production == RIGHT_SHIFT_PRODUCTION) { operator = ShorthandAssignmentOperator.RIGHT_SHIFT; }
    else if (production == AND_PRODUCTION)         { operator = ShorthandAssignmentOperator.AND;         }
    else if (production == OR_PRODUCTION)          { operator = ShorthandAssignmentOperator.OR;          }
    else if (production == XOR_PRODUCTION)         { operator = ShorthandAssignmentOperator.XOR;         }
    else { throw badTypeList(); }

    @SuppressWarnings("unchecked")
    ParseList<Assignee> assignees = (ParseList<Assignee>) args[0];
    Expression expression = (Expression) args[2];
    return new ShorthandAssignStatement(assignees.toArray(new Assignee[assignees.size()]), operator, expression,
                                        LexicalPhrase.combine(assignees.getLexicalPhrase(), (LexicalPhrase) args[1], expression.getLexicalPhrase()));
  }

}
