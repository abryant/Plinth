package eu.bryants.anthony.toylanguage.parser;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public enum ParseType
{
  // the rule for this is generated by the parser generator, and it returns
  // the same as the start rule, which is COMPILATION_UNIT
  GENERATED_START_RULE,

  // NON-TERMINALS:

  COMPILATION_UNIT,              // CompilationUnit
  COMPOUND_DEFINITION,           // CompoundDefinition
  FUNCTION,                      // Function

  // Members
  MEMBER_LIST,                   // ParseList<Member>
  FIELD,                         // Field
  CONSTRUCTOR,                   // Constructor

  // Misc
  ASSIGNEE,                      // Assignee
  ASSIGNEE_LIST,                 // ParseList<Assignee>
  PARAMETERS,                    // ParseList<Parameter>

  // Statements
  BLOCK,                         // Block
  STATEMENTS,                    // ParseList<Statement>
  STATEMENT,                     // Statement
  ASSIGN_STATEMENT,              // Statement
  BREAK_STATEMENT,               // BreakStatement
  CONTINUE_STATEMENT,            // ContinueStatement
  IF_STATEMENT,                  // IfStatement
  PREFIX_INC_DEC_STATEMENT,      // PrefixIncDecStatement
  RETURN_STATEMENT,              // ReturnStatement
  WHILE_STATEMENT,               // WhileStatement

  // Types
  RETURN_TYPE,                   // Type
  TYPE,                          // Type
  TYPE_LIST,                     // ParseList<Type>

  // Expressions
  TUPLE_EXPRESSION,              // Expression
  EXPRESSION,                    // Expression
  LOGICAL_EXPRESSION,            // Expression
  COMPARISON_EXPRESSION,         // Expression
  ADDITIVE_EXPRESSION,           // Expression
  MULTIPLICATIVE_EXPRESSION,     // Expression
  TUPLE_INDEX_EXPRESSION,        // Expression
  UNARY_EXPRESSION,              // Expression
  PRIMARY,                       // Expression
  FUNCTION_CALL_EXPRESSION,      // FunctionCallExpression
  EXPRESSION_LIST,               // ParseList<Expression>
  DIMENSIONS,                    // ParseList<Expression>

  // TERMINALS

  // literals
  NAME,             // Name
  INTEGER_LITERAL,  // IntegerLiteral
  FLOATING_LITERAL, // FloatingLiteral

  // symbols (values for these should all be LexicalPhrase)
  AMPERSAND,
  CARET,
  COLON,
  COMMA,
  DOUBLE_AMPERSAND,
  DOUBLE_EQUALS,
  DOUBLE_MINUS,
  DOUBLE_PERCENT,
  DOUBLE_PIPE,
  DOUBLE_PLUS,
  DOT,
  EQUALS,
  EXCLAIMATION_MARK,
  EXCLAIMATION_MARK_EQUALS,
  FORWARD_SLASH,
  LANGLE,
  LANGLE_EQUALS,
  LBRACE,
  LPAREN,
  LSQUARE,
  MINUS,
  PERCENT,
  PIPE,
  PLUS,
  QUESTION_MARK,
  RANGLE,
  RANGLE_EQUALS,
  RBRACE,
  RPAREN,
  RSQUARE,
  SEMICOLON,
  STAR,
  TILDE,
  UNDERSCORE,

  // keywords (values for these should all be LexicalPhrase)
  BOOLEAN_KEYWORD,
  BREAK_KEYWORD,
  BYTE_KEYWORD,
  CAST_KEYWORD,
  COMPOUND_KEYWORD,
  CONTINUE_KEYWORD,
  DOUBLE_KEYWORD,
  ELSE_KEYWORD,
  FALSE_KEYWORD,
  FLOAT_KEYWORD,
  IF_KEYWORD,
  INT_KEYWORD,
  LONG_KEYWORD,
  NEW_KEYWORD,
  RETURN_KEYWORD,
  SHORT_KEYWORD,
  TRUE_KEYWORD,
  UBYTE_KEYWORD,
  UINT_KEYWORD,
  ULONG_KEYWORD,
  USHORT_KEYWORD,
  VOID_KEYWORD,
  WHILE_KEYWORD,

  ;

}
