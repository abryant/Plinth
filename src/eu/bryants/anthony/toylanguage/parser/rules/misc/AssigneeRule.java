package eu.bryants.anthony.toylanguage.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.misc.Assignee;
import eu.bryants.anthony.toylanguage.ast.misc.FieldAssignee;
import eu.bryants.anthony.toylanguage.ast.misc.QName;
import eu.bryants.anthony.toylanguage.ast.misc.VariableAssignee;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 8 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class AssigneeRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> QNAME_PRODUCTION = new Production<ParseType>(ParseType.QNAME);
  private static final Production<ParseType> NO_QNAME_PRODUCTION = new Production<ParseType>(ParseType.ASSIGNEE_NO_QNAME);

  @SuppressWarnings("unchecked")
  public AssigneeRule()
  {
    super(ParseType.ASSIGNEE, QNAME_PRODUCTION, NO_QNAME_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == QNAME_PRODUCTION)
    {
      QName qname = (QName) args[0];
      return createQNameAssignee(qname);
    }
    if (production == NO_QNAME_PRODUCTION)
    {
      return args[0];
    }
    throw badTypeList();
  }

  /**
   * Creates an Assignee out of the specified QName
   * @param qname - the QName to create an assignee from
   * @return the Assignee created
   */
  public static Assignee createQNameAssignee(QName qname)
  {
    String[] names = qname.getNames();
    LexicalPhrase[] lexicalPhrases = qname.getLexicalPhrases();
    if (names.length == 1)
    {
      return new VariableAssignee(names[0], lexicalPhrases[0]);
    }
    VariableExpression variableExpression = new VariableExpression(names[0], lexicalPhrases[0]);
    FieldAccessExpression current = new FieldAccessExpression(variableExpression, false, names[1], lexicalPhrases[1]);
    for (int i = 2; i < names.length; ++i)
    {
      current = new FieldAccessExpression(current, false, names[i], LexicalPhrase.combine(current.getLexicalPhrase(), lexicalPhrases[i]));
    }
    return new FieldAssignee(current);
  }

}
