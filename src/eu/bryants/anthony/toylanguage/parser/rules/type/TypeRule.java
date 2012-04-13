package eu.bryants.anthony.toylanguage.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType;
import eu.bryants.anthony.toylanguage.ast.type.PrimitiveType.PrimitiveTypeType;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;
import eu.bryants.anthony.toylanguage.parser.ParseType;

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

  @SuppressWarnings("unchecked")
  public TypeRule()
  {
    super(ParseType.TYPE, BOOLEAN_PRODUCTION,
                           DOUBLE_PRODUCTION,  FLOAT_PRODUCTION,
                             LONG_PRODUCTION,  ULONG_PRODUCTION,
                              INT_PRODUCTION,   UINT_PRODUCTION,
                            SHORT_PRODUCTION, USHORT_PRODUCTION,
                             BYTE_PRODUCTION,  UBYTE_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    PrimitiveTypeType type;
         if (production == BOOLEAN_PRODUCTION) { type = PrimitiveTypeType.BOOLEAN; }
    else if (production ==  DOUBLE_PRODUCTION) { type = PrimitiveTypeType.DOUBLE;  }
    else if (production ==   FLOAT_PRODUCTION) { type = PrimitiveTypeType.FLOAT;   }
    else if (production ==    LONG_PRODUCTION) { type = PrimitiveTypeType.LONG;    }
    else if (production ==   ULONG_PRODUCTION) { type = PrimitiveTypeType.ULONG;   }
    else if (production ==     INT_PRODUCTION) { type = PrimitiveTypeType.INT;     }
    else if (production ==    UINT_PRODUCTION) { type = PrimitiveTypeType.UINT;    }
    else if (production ==   SHORT_PRODUCTION) { type = PrimitiveTypeType.SHORT;   }
    else if (production ==  USHORT_PRODUCTION) { type = PrimitiveTypeType.USHORT;  }
    else if (production ==    BYTE_PRODUCTION) { type = PrimitiveTypeType.BYTE;    }
    else if (production ==   UBYTE_PRODUCTION) { type = PrimitiveTypeType.UBYTE;   }
    else { throw badTypeList(); }
    return new PrimitiveType(type, (LexicalPhrase) args[0]);
  }

}
