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
  IMPORTS,                       // ParseList<Import>
  COMPOUND_DEFINITION,           // CompoundDefinition

  // Members
  MEMBER_LIST,                   // ParseList<Member>
  FIELD,                         // Field
  CONSTRUCTOR,                   // Constructor
  METHOD,                        // Method

  // Misc
  ASSIGNEE,                      // Assignee
  ASSIGNEE_LIST,                 // ParseList<Assignee>
  PARAMETERS,                    // ParseList<Parameter>
  FOR_INIT,                      // ParseContainer<Statement> (possibly containing null)
  FOR_UPDATE,                    // Statement or null
  MODIFIERS,                     // ParseList<Modifier>
  QNAME,                         // QName

  // Statements
  BLOCK,                         // Block
  STATEMENTS,                    // ParseList<Statement>
  STATEMENT,                     // Statement
  ASSIGN_STATEMENT,              // AssignStatement
  BREAK_STATEMENT,               // BreakStatement
  CONTINUE_STATEMENT,            // ContinueStatement
  FOR_STATEMENT,                 // ForStatement
  IF_STATEMENT,                  // IfStatement
  PREFIX_INC_DEC_STATEMENT,      // PrefixIncDecStatement
  RETURN_STATEMENT,              // ReturnStatement
  SHORTHAND_ASSIGNMENT,          // ShorthandAssignStatement
  WHILE_STATEMENT,               // WhileStatement

  // Types
  TYPE,                          // Type
  TYPE_LIST,                     // ParseList<Type>

  // Expressions
  TUPLE_EXPRESSION,              // Expression
  EXPRESSION,                    // Expression
  LOGICAL_EXPRESSION,            // Expression
  COMPARISON_EXPRESSION,         // Expression
  SHIFT_EXPRESSION,              // Expression
  ADDITIVE_EXPRESSION,           // Expression
  MULTIPLICATIVE_EXPRESSION,     // Expression
  TUPLE_INDEX_EXPRESSION,        // Expression
  UNARY_EXPRESSION,              // Expression
  PRIMARY,                       // Expression
  PRIMARY_NO_TRAILING_TYPE,      // Expression
  FUNCTION_CALL_EXPRESSION,      // FunctionCallExpression
  EXPRESSION_LIST,               // ParseList<Expression>
  DIMENSIONS,                    // ParseList<Expression>

  // TERMINALS

  // literals
  NAME,             // Name
  INTEGER_LITERAL,  // IntegerLiteral
  FLOATING_LITERAL, // FloatingLiteral
  STRING_LITERAL,   // StringLiteral

  // symbols (values for these should all be LexicalPhrase)
  AMPERSAND,
  AMPERSAND_EQUALS,
  CARET,
  CARET_EQUALS,
  COLON,
  COMMA,
  DOUBLE_AMPERSAND,
  DOUBLE_COLON,
  DOUBLE_EQUALS,
  DOUBLE_LANGLE,
  DOUBLE_LANGLE_EQUALS,
  DOUBLE_MINUS,
  DOUBLE_PERCENT,
  DOUBLE_PERCENT_EQUALS,
  DOUBLE_PIPE,
  DOUBLE_PLUS,
  DOUBLE_RANGLE,
  DOUBLE_RANGLE_EQUALS,
  DOT,
  EQUALS,
  EXCLAIMATION_MARK,
  EXCLAIMATION_MARK_EQUALS,
  FORWARD_SLASH,
  FORWARD_SLASH_EQUALS,
  LANGLE,
  LANGLE_EQUALS,
  LBRACE,
  LPAREN,
  LSQUARE,
  MINUS,
  MINUS_EQUALS,
  PERCENT,
  PERCENT_EQUALS,
  PIPE,
  PIPE_EQUALS,
  PLUS,
  PLUS_EQUALS,
  QUESTION_MARK,
  QUESTION_MARK_COLON,
  RANGLE,
  RANGLE_EQUALS,
  RBRACE,
  RPAREN,
  RSQUARE,
  SEMICOLON,
  STAR,
  STAR_EQUALS,
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
  FOR_KEYWORD,
  IF_KEYWORD,
  IMPORT_KEYWORD,
  INT_KEYWORD,
  LONG_KEYWORD,
  NATIVE_KEYWORD,
  NEW_KEYWORD,
  NULL_KEYWORD,
  PACKAGE_KEYWORD,
  RETURN_KEYWORD,
  SHORT_KEYWORD,
  STATIC_KEYWORD,
  THIS_KEYWORD,
  TRUE_KEYWORD,
  UBYTE_KEYWORD,
  UINT_KEYWORD,
  ULONG_KEYWORD,
  USHORT_KEYWORD,
  VOID_KEYWORD,
  WHILE_KEYWORD,

  ;

}
