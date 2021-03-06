package eu.bryants.anthony.plinth.parser.rules.type;

import java.util.LinkedList;
import java.util.List;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.misc.DefaultParameter;
import eu.bryants.anthony.plinth.ast.misc.QName;
import eu.bryants.anthony.plinth.ast.type.ArrayType;
import eu.bryants.anthony.plinth.ast.type.FunctionType;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.ObjectType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType;
import eu.bryants.anthony.plinth.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.plinth.ast.type.TupleType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.FunctionTypeParameterList;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.QNameElement;
import eu.bryants.anthony.plinth.parser.parseAST.ThrownExceptionType;

/*
 * Created on 29 Sep 2012
 */

/**
 * @author Anthony Bryant
 */
public class BasicTypeRule extends Rule<ParseType>
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

  private static final Production<ParseType> ARRAY_PRODUCTION                    = new Production<ParseType>(                                         ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE_NO_TRAILING_ARGUMENTS);
  private static final Production<ParseType> NULLABLE_ARRAY_PRODUCTION           = new Production<ParseType>(ParseType.QUESTION_MARK,                 ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE_NO_TRAILING_ARGUMENTS);
  private static final Production<ParseType> IMMUTABLE_ARRAY_PRODUCTION          = new Production<ParseType>(                         ParseType.HASH, ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE_NO_TRAILING_ARGUMENTS);
  private static final Production<ParseType> NULLABLE_IMMUTABLE_ARRAY_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.HASH, ParseType.LSQUARE, ParseType.RSQUARE, ParseType.TYPE_NO_TRAILING_ARGUMENTS);

  private static final Production<ParseType> FUNCTION_PRODUCTION                              = new Production<ParseType>(                         ParseType.LBRACE, ParseType.FUNCTION_TYPE_PARAMETERS,                 ParseType.ARROW, ParseType.RETURN_TYPE, ParseType.THROWS_CLAUSE, ParseType.RBRACE);
  private static final Production<ParseType> NULLABLE_FUNCTION_PRODUCTION                     = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.LBRACE, ParseType.FUNCTION_TYPE_PARAMETERS,                 ParseType.ARROW, ParseType.RETURN_TYPE, ParseType.THROWS_CLAUSE, ParseType.RBRACE);
  private static final Production<ParseType> IMMUTABLE_FUNCTION_PRODUCTION                    = new Production<ParseType>(                         ParseType.LBRACE, ParseType.FUNCTION_TYPE_PARAMETERS, ParseType.HASH, ParseType.ARROW, ParseType.RETURN_TYPE, ParseType.THROWS_CLAUSE, ParseType.RBRACE);
  private static final Production<ParseType> NULLABLE_IMMUTABLE_FUNCTION_PRODUCTION           = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.LBRACE, ParseType.FUNCTION_TYPE_PARAMETERS, ParseType.HASH, ParseType.ARROW, ParseType.RETURN_TYPE, ParseType.THROWS_CLAUSE, ParseType.RBRACE);
  private static final Production<ParseType> NO_PARAMS_FUNCTION_PRODUCTION                    = new Production<ParseType>(                         ParseType.LBRACE,                                                     ParseType.ARROW, ParseType.RETURN_TYPE, ParseType.THROWS_CLAUSE, ParseType.RBRACE);
  private static final Production<ParseType> NULLABLE_NO_PARAMS_FUNCTION_PRODUCTION           = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.LBRACE,                                                     ParseType.ARROW, ParseType.RETURN_TYPE, ParseType.THROWS_CLAUSE, ParseType.RBRACE);
  private static final Production<ParseType> IMMUTABLE_NO_PARAMS_FUNCTION_PRODUCTION          = new Production<ParseType>(                         ParseType.LBRACE,                                     ParseType.HASH, ParseType.ARROW, ParseType.RETURN_TYPE, ParseType.THROWS_CLAUSE, ParseType.RBRACE);
  private static final Production<ParseType> NULLABLE_IMMUTABLE_NO_PARAMS_FUNCTION_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.LBRACE,                                     ParseType.HASH, ParseType.ARROW, ParseType.RETURN_TYPE, ParseType.THROWS_CLAUSE, ParseType.RBRACE);

  private static final Production<ParseType> OBJECT_PRODUCTION = new Production<ParseType>(ParseType.OBJECT_KEYWORD);
  private static final Production<ParseType> NULLABLE_OBJECT_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.OBJECT_KEYWORD);
  private static final Production<ParseType> IMMUTABLE_OBJECT_PRODUCTION = new Production<ParseType>(ParseType.HASH, ParseType.OBJECT_KEYWORD);
  private static final Production<ParseType> NULLABLE_IMMUTABLE_OBJECT_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.HASH, ParseType.OBJECT_KEYWORD);

  private static final Production<ParseType> TUPLE_PRODUCTION                = new Production<ParseType>(                         ParseType.LPAREN, ParseType.TYPE_LIST_NOT_QNAME, ParseType.RPAREN);
  private static final Production<ParseType> NULLABLE_TUPLE_PRODUCTION       = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.LPAREN, ParseType.TYPE_LIST_NOT_QNAME, ParseType.RPAREN);
  private static final Production<ParseType> NULLABLE_QNAME_TUPLE_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.NESTED_QNAME_LIST);

  private static final Production<ParseType> NULLABLE_NAMED_PRODUCTION           = new Production<ParseType>(ParseType.QUESTION_MARK,                 ParseType.QNAME);
  private static final Production<ParseType> IMMUTABLE_NAMED_PRODUCTION          = new Production<ParseType>(                         ParseType.HASH, ParseType.QNAME);
  private static final Production<ParseType> NULLABLE_IMMUTABLE_NAMED_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.HASH, ParseType.QNAME);

  public BasicTypeRule()
  {
    super(ParseType.BASIC_TYPE, BOOLEAN_PRODUCTION,                    NULLABLE_BOOLEAN_PRODUCTION,
                                 DOUBLE_PRODUCTION,  FLOAT_PRODUCTION,  NULLABLE_DOUBLE_PRODUCTION,  NULLABLE_FLOAT_PRODUCTION,
                                   LONG_PRODUCTION,  ULONG_PRODUCTION,    NULLABLE_LONG_PRODUCTION,  NULLABLE_ULONG_PRODUCTION,
                                    INT_PRODUCTION,   UINT_PRODUCTION,     NULLABLE_INT_PRODUCTION,   NULLABLE_UINT_PRODUCTION,
                                  SHORT_PRODUCTION, USHORT_PRODUCTION,   NULLABLE_SHORT_PRODUCTION, NULLABLE_USHORT_PRODUCTION,
                                   BYTE_PRODUCTION,  UBYTE_PRODUCTION,    NULLABLE_BYTE_PRODUCTION,  NULLABLE_UBYTE_PRODUCTION,
                                 ARRAY_PRODUCTION, NULLABLE_ARRAY_PRODUCTION, IMMUTABLE_ARRAY_PRODUCTION, NULLABLE_IMMUTABLE_ARRAY_PRODUCTION,
                                 FUNCTION_PRODUCTION, NULLABLE_FUNCTION_PRODUCTION, IMMUTABLE_FUNCTION_PRODUCTION, NULLABLE_IMMUTABLE_FUNCTION_PRODUCTION,
                                 NO_PARAMS_FUNCTION_PRODUCTION, NULLABLE_NO_PARAMS_FUNCTION_PRODUCTION, IMMUTABLE_NO_PARAMS_FUNCTION_PRODUCTION, NULLABLE_IMMUTABLE_NO_PARAMS_FUNCTION_PRODUCTION,
                                 OBJECT_PRODUCTION, NULLABLE_OBJECT_PRODUCTION, IMMUTABLE_OBJECT_PRODUCTION, NULLABLE_IMMUTABLE_OBJECT_PRODUCTION,
                                 TUPLE_PRODUCTION, NULLABLE_TUPLE_PRODUCTION, NULLABLE_QNAME_TUPLE_PRODUCTION,
                                 NULLABLE_NAMED_PRODUCTION, IMMUTABLE_NAMED_PRODUCTION, NULLABLE_IMMUTABLE_NAMED_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == NULLABLE_NAMED_PRODUCTION)
    {
      QName name = (QName) args[1];
      return new NamedType(true, false, name, null, LexicalPhrase.combine((LexicalPhrase) args[0], name.getLexicalPhrase()));
    }
    if (production == IMMUTABLE_NAMED_PRODUCTION)
    {
      QName name = (QName) args[1];
      return new NamedType(false, true, name, null, LexicalPhrase.combine((LexicalPhrase) args[0], name.getLexicalPhrase()));
    }
    if (production == NULLABLE_IMMUTABLE_NAMED_PRODUCTION)
    {
      QName name = (QName) args[2];
      return new NamedType(true, true, name, null, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], name.getLexicalPhrase()));
    }

    if (production == TUPLE_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Type> subTypes = (ParseList<Type>) args[1];
      return new TupleType(false, subTypes.toArray(new Type[subTypes.size()]),
                                            LexicalPhrase.combine((LexicalPhrase) args[0], subTypes.getLexicalPhrase(), (LexicalPhrase) args[2]));
    }
    if (production == NULLABLE_TUPLE_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Type> subTypes = (ParseList<Type>) args[2];
      return new TupleType(true, subTypes.toArray(new Type[subTypes.size()]),
                                            LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], subTypes.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == NULLABLE_QNAME_TUPLE_PRODUCTION)
    {
      QNameElement element = (QNameElement) args[1];
      TupleType notNullableType = (TupleType) element.convertToType();
      return new TupleType(true, notNullableType.getSubTypes(), LexicalPhrase.combine((LexicalPhrase) args[0], notNullableType.getLexicalPhrase()));
    }

    if (production == ARRAY_PRODUCTION)
    {
      Type baseType = (Type) args[2];
      return new ArrayType(false, false, baseType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], baseType.getLexicalPhrase()));
    }
    if (production == NULLABLE_ARRAY_PRODUCTION)
    {
      Type baseType = (Type) args[3];
      return new ArrayType(true, false, baseType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], baseType.getLexicalPhrase()));
    }
    if (production == IMMUTABLE_ARRAY_PRODUCTION)
    {
      Type baseType = (Type) args[3];
      return new ArrayType(false, true, baseType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], baseType.getLexicalPhrase()));
    }
    if (production == NULLABLE_IMMUTABLE_ARRAY_PRODUCTION)
    {
      Type baseType = (Type) args[4];
      return new ArrayType(true, true, baseType, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], (LexicalPhrase) args[3], baseType.getLexicalPhrase()));
    }

    if (production == FUNCTION_PRODUCTION)
    {
      FunctionTypeParameterList params = (FunctionTypeParameterList) args[1];
      ParseList<Type> nonDefaultParamTypes = params.getNonDefaultTypes();
      Type returnType = (Type) args[3];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[4];
      NamedType[] thrownTypes = processThrowsClause(throwsList);
      return new FunctionType(false, false, returnType, nonDefaultParamTypes.toArray(new Type[nonDefaultParamTypes.size()]), params.getDefaultParameters(), thrownTypes,
                              LexicalPhrase.combine((LexicalPhrase) args[0], params.getLexicalPhrase(), (LexicalPhrase) args[2], returnType.getLexicalPhrase(), throwsList.getLexicalPhrase(), (LexicalPhrase) args[5]));
    }
    if (production == NULLABLE_FUNCTION_PRODUCTION)
    {
      FunctionTypeParameterList params = (FunctionTypeParameterList) args[2];
      ParseList<Type> nonDefaultParamTypes = params.getNonDefaultTypes();
      Type returnType = (Type) args[4];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[5];
      NamedType[] thrownTypes = processThrowsClause(throwsList);
      return new FunctionType(true, false, returnType, nonDefaultParamTypes.toArray(new Type[nonDefaultParamTypes.size()]), params.getDefaultParameters(), thrownTypes,
                              LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], params.getLexicalPhrase(), (LexicalPhrase) args[3], returnType.getLexicalPhrase(), throwsList.getLexicalPhrase(), (LexicalPhrase) args[6]));
    }
    if (production == IMMUTABLE_FUNCTION_PRODUCTION)
    {
      FunctionTypeParameterList params = (FunctionTypeParameterList) args[1];
      ParseList<Type> nonDefaultParamTypes = params.getNonDefaultTypes();
      Type returnType = (Type) args[4];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[5];
      NamedType[] thrownTypes = processThrowsClause(throwsList);
      return new FunctionType(false, true, returnType, nonDefaultParamTypes.toArray(new Type[nonDefaultParamTypes.size()]), params.getDefaultParameters(), thrownTypes,
                              LexicalPhrase.combine((LexicalPhrase) args[0], params.getLexicalPhrase(), (LexicalPhrase) args[2], (LexicalPhrase) args[3], returnType.getLexicalPhrase(), throwsList.getLexicalPhrase(), (LexicalPhrase) args[6]));
    }
    if (production == NULLABLE_IMMUTABLE_FUNCTION_PRODUCTION)
    {
      FunctionTypeParameterList params = (FunctionTypeParameterList) args[2];
      ParseList<Type> nonDefaultParamTypes = params.getNonDefaultTypes();
      Type returnType = (Type) args[5];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[6];
      NamedType[] thrownTypes = processThrowsClause(throwsList);
      return new FunctionType(true, true, returnType, nonDefaultParamTypes.toArray(new Type[nonDefaultParamTypes.size()]), params.getDefaultParameters(), thrownTypes,
                              LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], params.getLexicalPhrase(), (LexicalPhrase) args[3], (LexicalPhrase) args[4], returnType.getLexicalPhrase(), throwsList.getLexicalPhrase(), (LexicalPhrase) args[7]));
    }
    if (production == NO_PARAMS_FUNCTION_PRODUCTION)
    {
      Type returnType = (Type) args[2];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[3];
      NamedType[] thrownTypes = processThrowsClause(throwsList);
      return new FunctionType(false, false, returnType, new Type[0], new DefaultParameter[0], thrownTypes, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], returnType.getLexicalPhrase(), throwsList.getLexicalPhrase(), (LexicalPhrase) args[4]));
    }
    if (production == NULLABLE_NO_PARAMS_FUNCTION_PRODUCTION)
    {
      Type returnType = (Type) args[3];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[4];
      NamedType[] thrownTypes = processThrowsClause(throwsList);
      return new FunctionType(true, false, returnType, new Type[0], new DefaultParameter[0], thrownTypes, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], returnType.getLexicalPhrase(), throwsList.getLexicalPhrase(), (LexicalPhrase) args[5]));
    }
    if (production == IMMUTABLE_NO_PARAMS_FUNCTION_PRODUCTION)
    {
      Type returnType = (Type) args[3];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[4];
      NamedType[] thrownTypes = processThrowsClause(throwsList);
      return new FunctionType(false, true, returnType, new Type[0], new DefaultParameter[0], thrownTypes, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], returnType.getLexicalPhrase(), throwsList.getLexicalPhrase(), (LexicalPhrase) args[5]));
    }
    if (production == NULLABLE_IMMUTABLE_NO_PARAMS_FUNCTION_PRODUCTION)
    {
      Type returnType = (Type) args[4];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[5];
      NamedType[] thrownTypes = processThrowsClause(throwsList);
      return new FunctionType(true, true, returnType, new Type[0], new DefaultParameter[0], thrownTypes, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2], (LexicalPhrase) args[3], returnType.getLexicalPhrase(), throwsList.getLexicalPhrase(), (LexicalPhrase) args[6]));
    }

    if (production == OBJECT_PRODUCTION)
    {
      return new ObjectType(false, false, (LexicalPhrase) args[0]);
    }
    if (production == NULLABLE_OBJECT_PRODUCTION)
    {
      return new ObjectType(true, false, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1]));
    }
    if (production == IMMUTABLE_OBJECT_PRODUCTION)
    {
      return new ObjectType(false, true, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1]));
    }
    if (production == NULLABLE_IMMUTABLE_OBJECT_PRODUCTION)
    {
      return new ObjectType(true, true, LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], (LexicalPhrase) args[2]));
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

  private static NamedType[] processThrowsClause(ParseList<ThrownExceptionType> list) throws LanguageParseException
  {
    List<NamedType> thrownTypes = new LinkedList<NamedType>();
    for (ThrownExceptionType thrownExceptionType : list)
    {
      if (thrownExceptionType.isUnchecked())
      {
        throw new LanguageParseException("A function type cannot throw unchecked exceptions", thrownExceptionType.getLexicalPhrase());
      }
      thrownTypes.add(thrownExceptionType.getType());
    }
    return thrownTypes.toArray(new NamedType[thrownTypes.size()]);
  }

}
