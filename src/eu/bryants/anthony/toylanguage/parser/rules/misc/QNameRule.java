package eu.bryants.anthony.toylanguage.parser.rules.misc;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.misc.QName;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.parser.ParseType;

/*
 * Created on 8 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class QNameRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> START_PRODUCTION = new Production<ParseType>(ParseType.NAME);
  private static final Production<ParseType> CONTINUATION_PRODUCTION = new Production<ParseType>(ParseType.QNAME, ParseType.DOT, ParseType.NAME);

  @SuppressWarnings("unchecked")
  public QNameRule()
  {
    super(ParseType.QNAME, START_PRODUCTION, CONTINUATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == START_PRODUCTION)
    {
      Name name = (Name) args[0];
      return new QName(name.getName(), name.getLexicalPhrase());
    }
    if (production == CONTINUATION_PRODUCTION)
    {
      QName qname = (QName) args[0];
      Name name = (Name) args[2];
      return new QName(qname, name.getName(), LexicalPhrase.combine(qname.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
