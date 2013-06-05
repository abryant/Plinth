package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseContainer;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 23 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeTrailingArgsRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION                    = new Production<ParseType>(                                         ParseType.QNAME, ParseType.LANGLE, ParseType.TYPE_ARGUMENT_LIST_RANGLE);
  private static final Production<ParseType> NULLABLE_PRODUCTION           = new Production<ParseType>(ParseType.QUESTION_MARK,                 ParseType.QNAME, ParseType.LANGLE, ParseType.TYPE_ARGUMENT_LIST_RANGLE);
  private static final Production<ParseType> IMMUTABLE_PRODUCTION          = new Production<ParseType>(                         ParseType.HASH, ParseType.QNAME, ParseType.LANGLE, ParseType.TYPE_ARGUMENT_LIST_RANGLE);
  private static final Production<ParseType> NULLABLE_IMMUTABLE_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.HASH, ParseType.QNAME, ParseType.LANGLE, ParseType.TYPE_ARGUMENT_LIST_RANGLE);
  private static final Production<ParseType> ARRAY_PRODUCTION              = new Production<ParseType>(ParseType.ARRAY_TYPE_TRAILING_ARGS);

  public TypeTrailingArgsRule()
  {
    super(ParseType.TYPE_TRAILING_ARGS, PRODUCTION, NULLABLE_PRODUCTION, IMMUTABLE_PRODUCTION, NULLABLE_IMMUTABLE_PRODUCTION, ARRAY_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == ARRAY_PRODUCTION)
    {
      return args[0];
    }
    boolean isNullable = production == NULLABLE_PRODUCTION || production == NULLABLE_IMMUTABLE_PRODUCTION;
    boolean isImmutable = production == IMMUTABLE_PRODUCTION || production == NULLABLE_IMMUTABLE_PRODUCTION;
    LexicalPhrase prefixLexicalPhrase;
    QName qname;
    LexicalPhrase lAngleLexicalPhrase;
    ParseContainer<ParseList<Type>> listRAngle;
    if (production == PRODUCTION)
    {
      prefixLexicalPhrase = null;
      qname = (QName) args[0];
      lAngleLexicalPhrase = (LexicalPhrase) args[1];
      listRAngle = (ParseContainer<ParseList<Type>>) args[2];
    }
    else if (production == NULLABLE_PRODUCTION || production == IMMUTABLE_PRODUCTION)
    {
      prefixLexicalPhrase = (LexicalPhrase) args[0];
      qname = (QName) args[1];
      lAngleLexicalPhrase = (LexicalPhrase) args[2];
      listRAngle = (ParseContainer<ParseList<Type>>) args[3];
    }
    else if (production == NULLABLE_IMMUTABLE_PRODUCTION)
    {
      prefixLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1]);
      qname = (QName) args[2];
      lAngleLexicalPhrase = (LexicalPhrase) args[3];
      listRAngle = (ParseContainer<ParseList<Type>>) args[4];
    }
    else
    {
      throw badTypeList();
    }

    ParseList<Type> list = listRAngle.getItem();
    return new NamedType(isNullable, isImmutable, qname, list.toArray(new Type[list.size()]), LexicalPhrase.combine(prefixLexicalPhrase, qname.getLexicalPhrase(), lAngleLexicalPhrase, listRAngle.getLexicalPhrase()));
  }

}
