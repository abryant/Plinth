package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseContainer;

/*
 * Created on 5 Jun 2013
 */

/**
 * @author Anthony Bryant
 */
public class ArrayTypeTrailingArgsRAngleRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> ARRAY_PRODUCTION                    = new Production<ParseType>(                                         ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE_TRAILING_ARGS_RANGLE);
  private static final Production<ParseType> NULLABLE_ARRAY_PRODUCTION           = new Production<ParseType>(ParseType.QUESTION_MARK,                 ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE_TRAILING_ARGS_RANGLE);
  private static final Production<ParseType> IMMUTABLE_ARRAY_PRODUCTION          = new Production<ParseType>(                         ParseType.HASH, ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE_TRAILING_ARGS_RANGLE);
  private static final Production<ParseType> NULLABLE_IMMUTABLE_ARRAY_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.HASH, ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE_TRAILING_ARGS_RANGLE);

  public ArrayTypeTrailingArgsRAngleRule()
  {
    super(ParseType.ARRAY_TYPE_TRAILING_ARGS_RANGLE, ARRAY_PRODUCTION, NULLABLE_ARRAY_PRODUCTION, IMMUTABLE_ARRAY_PRODUCTION, NULLABLE_IMMUTABLE_ARRAY_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == ARRAY_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseContainer<Type> typeContainer = (ParseContainer<Type>) args[2];
      Type baseType = typeContainer.getItem();
      ArrayType arrayType = new ArrayType(false, false, baseType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], baseType.getLexicalPhrase()));
      return new ParseContainer<Type>(arrayType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], typeContainer.getLexicalPhrase()));
    }
    if (production == NULLABLE_ARRAY_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseContainer<Type> typeContainer = (ParseContainer<Type>) args[3];
      Type baseType = typeContainer.getItem();
      ArrayType arrayType = new ArrayType(true, false, baseType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], baseType.getLexicalPhrase()));
      return new ParseContainer<Type>(arrayType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], typeContainer.getLexicalPhrase()));
    }
    if (production == IMMUTABLE_ARRAY_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseContainer<Type> typeContainer = (ParseContainer<Type>) args[3];
      Type baseType = typeContainer.getItem();
      ArrayType arrayType = new ArrayType(false, true, baseType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], baseType.getLexicalPhrase()));
      return new ParseContainer<Type>(arrayType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], typeContainer.getLexicalPhrase()));
    }
    if (production == NULLABLE_IMMUTABLE_ARRAY_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseContainer<Type> typeContainer = (ParseContainer<Type>) args[4];
      Type baseType = typeContainer.getItem();
      ArrayType arrayType = new ArrayType(true, true, baseType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], (LexicalPhrase) args[3], baseType.getLexicalPhrase()));
      return new ParseContainer<Type>(arrayType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], (LexicalPhrase) args[3], typeContainer.getLexicalPhrase()));
    }
    throw badTypeList();
  }

}
