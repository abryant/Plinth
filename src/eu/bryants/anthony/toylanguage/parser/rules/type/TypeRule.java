package eu.bryants.anthony.toylanguage.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.misc.QName;
import eu.bryants.anthony.toylanguage.ast.type.ArrayType;
import eu.bryants.anthony.toylanguage.ast.type.NamedType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.toylanguage.ast.type.TupleType;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class TypeRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> BOOLEAN_PRODUCTION = new Production<ParseType>(ParseType.BOOLEAN_KEYWORD);
  private static final Production<ParseType>  DOUBLE_PRODUCTION = new Production<ParseType>(ParseType. DOUBLE_KEYWORD);
  private static final Production<ParseType>   FLOAT_PRODUCTION = new Production<ParseType>(ParseType.  FLOAT_KEYWORD);
  private static final Production<ParseType>    LONG_PRODUCTION = new Production<ParseType>(ParseType.   LONG_KEYWORD);
  private static final Production<ParseType>   ULONG_PRODUCTION = new Production<ParseType>(ParseType.  ULONG_KEYWORD);
  private static final Production<ParseType>     INT_PRODUCTION = new Production<ParseType>(ParseType.    INT_KEYWORD);
  private static final Production<ParseType>    UINT_PRODUCTION = new Production<ParseType>(ParseType.   UINT_KEYWORD);
  private static final Production<ParseType>   SHORT_PRODUCTION = new Production<ParseType>(ParseType.  SHORT_KEYWORD);
  private static final Production<ParseType>  USHORT_PRODUCTION = new Production<ParseType>(ParseType. USHORT_KEYWORD);
  private static final Production<ParseType>    BYTE_PRODUCTION = new Production<ParseType>(ParseType.   BYTE_KEYWORD);
  private static final Production<ParseType>   UBYTE_PRODUCTION = new Production<ParseType>(ParseType.  UBYTE_KEYWORD);

  private static final Production<ParseType> NULLABLE_BOOLEAN_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.BOOLEAN_KEYWORD);
  private static final Production<ParseType>  NULLABLE_DOUBLE_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType. DOUBLE_KEYWORD);
  private static final Production<ParseType>   NULLABLE_FLOAT_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.  FLOAT_KEYWORD);
  private static final Production<ParseType>    NULLABLE_LONG_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.   LONG_KEYWORD);
  private static final Production<ParseType>   NULLABLE_ULONG_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.  ULONG_KEYWORD);
  private static final Production<ParseType>     NULLABLE_INT_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.    INT_KEYWORD);
  private static final Production<ParseType>    NULLABLE_UINT_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.   UINT_KEYWORD);
  private static final Production<ParseType>   NULLABLE_SHORT_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.  SHORT_KEYWORD);
  private static final Production<ParseType>  NULLABLE_USHORT_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType. USHORT_KEYWORD);
  private static final Production<ParseType>    NULLABLE_BYTE_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.   BYTE_KEYWORD);
  private static final Production<ParseType>   NULLABLE_UBYTE_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.  UBYTE_KEYWORD);

  private static final Production<ParseType> NAMED_PRODUCTION = new Production<ParseType>(ParseType.COLON, ParseType.QNAME);
  private static final Production<ParseType> ARRAY_PRODUCTION = new Production<ParseType>(ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE);
  private static final Production<ParseType> TUPLE_PRODUCTION = new Production<ParseType>(ParseType.LPAREN, ParseType.TYPE_LIST, ParseType.RPAREN);

  private static final Production<ParseType> NULLABLE_NAMED_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.COLON, ParseType.QNAME);
  private static final Production<ParseType> NULLABLE_ARRAY_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE);
  private static final Production<ParseType> NULLABLE_TUPLE_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.LPAREN, ParseType.TYPE_LIST, ParseType.RPAREN);

  @SuppressWarnings("unchecked")
  public TypeRule()
  {
    super(ParseType.TYPE, BOOLEAN_PRODUCTION,                    NULLABLE_BOOLEAN_PRODUCTION,
                           DOUBLE_PRODUCTION,  FLOAT_PRODUCTION,  NULLABLE_DOUBLE_PRODUCTION,  NULLABLE_FLOAT_PRODUCTION,
                             LONG_PRODUCTION,  ULONG_PRODUCTION,    NULLABLE_LONG_PRODUCTION,  NULLABLE_ULONG_PRODUCTION,
                              INT_PRODUCTION,   UINT_PRODUCTION,     NULLABLE_INT_PRODUCTION,   NULLABLE_UINT_PRODUCTION,
                            SHORT_PRODUCTION, USHORT_PRODUCTION,   NULLABLE_SHORT_PRODUCTION, NULLABLE_USHORT_PRODUCTION,
                             BYTE_PRODUCTION,  UBYTE_PRODUCTION,    NULLABLE_BYTE_PRODUCTION,  NULLABLE_UBYTE_PRODUCTION,
                          NAMED_PRODUCTION, NULLABLE_NAMED_PRODUCTION,
                          ARRAY_PRODUCTION, NULLABLE_ARRAY_PRODUCTION,
                          TUPLE_PRODUCTION, NULLABLE_TUPLE_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == ARRAY_PRODUCTION)
    {
      Type baseType = (Type) args[2];
      return new ArrayType(false, baseType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], baseType.getLexicalPhrase()));
    }
    if (production == TUPLE_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Type> subTypes = (ParseList<Type>) args[1];
      return new TupleType(false, subTypes.toArray(new Type[subTypes.size()]),
                                            LexicalPhrase.combine((LexicalPhrase) args[0], subTypes.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    if (production == NAMED_PRODUCTION)
    {
      QName name = (QName) args[1];
      return new NamedType(false, name, name.getLexicalPhrase());
    }

    if (production == NULLABLE_ARRAY_PRODUCTION)
    {
      Type baseType = (Type) args[3];
      return new ArrayType(true, baseType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], baseType.getLexicalPhrase()));
    }
    if (production == NULLABLE_TUPLE_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Type> subTypes = (ParseList<Type>) args[2];
      return new TupleType(true, subTypes.toArray(new Type[subTypes.size()]),
                                            LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], subTypes.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == NULLABLE_NAMED_PRODUCTION)
    {
      QName name = (QName) args[2];
      return new NamedType(true, name, LexicalPhrase.combine((LexicalPhrase) args[0], name.getLexicalPhrase()));
    }

    PrimitiveTypeType type;
    boolean nullable = false;
         if (production == BOOLEAN_PRODUCTION) { type = PrimitiveTypeType.BOOLEAN; } else if (production == NULLABLE_BOOLEAN_PRODUCTION) { type = PrimitiveTypeType.BOOLEAN; nullable = true; }
    else if (production ==  DOUBLE_PRODUCTION) { type = PrimitiveTypeType.DOUBLE;  } else if (production ==  NULLABLE_DOUBLE_PRODUCTION) { type = PrimitiveTypeType.DOUBLE;  nullable = true; }
    else if (production ==   FLOAT_PRODUCTION) { type = PrimitiveTypeType.FLOAT;   } else if (production ==   NULLABLE_FLOAT_PRODUCTION) { type = PrimitiveTypeType.FLOAT;   nullable = true; }
    else if (production ==    LONG_PRODUCTION) { type = PrimitiveTypeType.LONG;    } else if (production ==    NULLABLE_LONG_PRODUCTION) { type = PrimitiveTypeType.LONG;    nullable = true; }
    else if (production ==   ULONG_PRODUCTION) { type = PrimitiveTypeType.ULONG;   } else if (production ==   NULLABLE_ULONG_PRODUCTION) { type = PrimitiveTypeType.ULONG;   nullable = true; }
    else if (production ==     INT_PRODUCTION) { type = PrimitiveTypeType.INT;     } else if (production ==     NULLABLE_INT_PRODUCTION) { type = PrimitiveTypeType.INT;     nullable = true; }
    else if (production ==    UINT_PRODUCTION) { type = PrimitiveTypeType.UINT;    } else if (production ==    NULLABLE_UINT_PRODUCTION) { type = PrimitiveTypeType.UINT;    nullable = true; }
    else if (production ==   SHORT_PRODUCTION) { type = PrimitiveTypeType.SHORT;   } else if (production ==   NULLABLE_SHORT_PRODUCTION) { type = PrimitiveTypeType.SHORT;   nullable = true; }
    else if (production ==  USHORT_PRODUCTION) { type = PrimitiveTypeType.USHORT;  } else if (production ==  NULLABLE_USHORT_PRODUCTION) { type = PrimitiveTypeType.USHORT;  nullable = true; }
    else if (production ==    BYTE_PRODUCTION) { type = PrimitiveTypeType.BYTE;    } else if (production ==    NULLABLE_BYTE_PRODUCTION) { type = PrimitiveTypeType.BYTE;    nullable = true; }
    else if (production ==   UBYTE_PRODUCTION) { type = PrimitiveTypeType.UBYTE;   } else if (production ==   NULLABLE_UBYTE_PRODUCTION) { type = PrimitiveTypeType.UBYTE;   nullable = true; }
    else { throw badTypeList(); }
    return new PrimitiveType(nullable, type, nullable ? LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1]) : (LexicalPhrase) args[0]);
  }

}
