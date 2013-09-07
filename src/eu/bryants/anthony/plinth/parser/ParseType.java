package eu.bryants.anthony.plinth.parser;

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
  CLASS_DEFINITION,              // ClassDefinition
  COMPOUND_DEFINITION,           // CompoundDefinition
  INTERFACE_DEFINITION,          // InterfaceDefinition

  // Members
  MEMBER_LIST,                   // ParseList<Member>
  INITIALISER,                   // Initialiser
  FIELD,                         // Field
  CONSTRUCTOR,                   // Constructor
  METHOD,                        // Method
  PROPERTY,                      // Property

  // Misc
  ARGUMENTS,                     // ParseList<Expression>
  ASSIGNEE,                      // Assignee
  ASSIGNEE_NO_QNAME,             // Assignee
  ASSIGNEE_LIST,                 // ParseList<Assignee>
  DECLARATION_ASSIGNEE_LIST,     // ParseList<Assignee>
  CATCH_TYPE_LIST,               // ParseList<Type>
  PROPERTY_METHOD_LIST,          // ParseList<PropertyMethod>
  PROPERTY_METHOD,               // PropertyMethod
  PARAMETERS,                    // ParseList<Parameter>
  PARAMETER_LIST,                // ParseList<Parameter>
  FOR_INIT,                      // ParseContainer<Statement> (possibly containing null)
  FOR_UPDATE,                    // Statement or null
  IMPLEMENTS_CLAUSE,             // ParseList<NamedType> or null
  INTERFACE_LIST,                // ParseList<NamedType>
  MODIFIERS,                     // ParseList<Modifier>
  OPTIONAL_MODIFIERS,            // ParseList<Modifier>
  QNAME,                         // QName
  QNAME_LIST,                    // ParseList<QNameElement>
  NESTED_QNAME_LIST,             // QNameElement
  THROWS_CLAUSE,                 // ParseList<ThrownExceptionType>
  THROWS_LIST,                   // ParseList<ThrownExceptionType>

  // Statements
  BLOCK,                         // Block
  STATEMENTS,                    // ParseList<Statement>
  STATEMENT,                     // Statement
  ASSIGN_STATEMENT,              // AssignStatement
  BREAK_STATEMENT,               // BreakStatement
  CONTINUE_STATEMENT,            // ContinueStatement
  FOR_STATEMENT,                 // Statement (ForStatement or ForEachStatement)
  IF_STATEMENT,                  // IfStatement
  PREFIX_INC_DEC_STATEMENT,      // PrefixIncDecStatement
  RETURN_STATEMENT,              // ReturnStatement
  SHORTHAND_ASSIGNMENT,          // ShorthandAssignStatement
  TRY_CATCH_STATEMENT,           // TryStatement
  TRY_FINALLY_STATEMENT,         // TryStatement
  WHILE_STATEMENT,               // WhileStatement

  // Types
  RETURN_TYPE,                          // Type
  TYPE,                                 // Type
  TYPE_RANGLE,                          // ParseContainer<Type>
  TYPE_DOUBLE_RANGLE,                   // ParseContainer<ParseContainer<Type>>
  TYPE_NOT_QNAME,                       // Type
  TYPE_NO_SIMPLE_ARGUMENTS,             // Type
  TYPE_NO_TRAILING_ARGUMENTS,           // Type
  BASIC_TYPE,                           // Type
  TYPE_LIST,                            // ParseList<Type>
  TYPE_LIST_NOT_QNAME,                  // ParseList<Type>
  NAMED_TYPE_NO_MODIFIERS,              // NamedType
  TYPE_TRAILING_ARGS,                   // Type
  TYPE_TRAILING_ARGS_RANGLE,            // ParseContainer<Type>
  ARRAY_TYPE_TRAILING_ARGS,             // ArrayType
  ARRAY_TYPE_TRAILING_ARGS_RANGLE,      // ParseContainer<Type>
  TYPE_ARGUMENT_LIST_RANGLE,            // ParseContainer<ParseList<Type>>
  TYPE_ARGUMENT_LIST_DOUBLE_RANGLE,     // ParseContainer<ParseContainer<ParseList<Type>>>
  TYPE_ARGUMENT_NOT_QNAME,              // Type
  TYPE_ARGUMENT_RANGLE,                 // ParseContainer<Type>
  TYPE_ARGUMENT_DOUBLE_RANGLE,          // ParseContainer<ParseContainer<Type>>
  WILDCARD_TYPE_ARGUMENT,               // WildcardType
  WILDCARD_TYPE_ARGUMENT_RANGLE,        // ParseContainer<Type>
  WILDCARD_TYPE_ARGUMENT_DOUBLE_RANGLE, // ParseContainer<ParseContainer<Type>>
  TYPE_BOUND_LIST,                      // ParseList<Type>
  TYPE_BOUND_LIST_RANGLE,               // ParseContainer<ParseList<Type>>
  TYPE_BOUND_LIST_DOUBLE_RANGLE,        // ParseContainer<ParseContainer<ParseList<Type>>>
  TYPE_PARAMETER,                       // TypeParameter
  TYPE_PARAMETER_RANGLE,                // ParseContainer<TypeParameter>
  TYPE_PARAMETER_LIST,                  // ParseList<TypeParameter>
  TYPE_PARAMETER_LIST_RANGLE,           // ParseContainer<ParseList<TypeParameter>>
  OPTIONAL_TYPE_PARAMETERS,             // ParseList<TypeParameter>

  // Expressions
  EXPRESSION,                    // Expression
  TUPLE_EXPRESSION,              // Expression
  CONDITIONAL_EXPRESSION,        // Expression
  LOGICAL_EXPRESSION,            // Expression
  EQUALITY_EXPRESSION,           // Expression
  QNAME_OR_LESS_THAN_EXPRESSION, // Expression
  COMPARISON_EXPRESSION_LESS_THAN_QNAME, // Expression
  EXPRESSION_NOT_LESS_THAN_QNAME, // Expression
  ADDITIVE_EXPRESSION,           // Expression
  MULTIPLICATIVE_EXPRESSION,     // Expression
  SHIFT_EXPRESSION,              // Expression
  TUPLE_INDEX_EXPRESSION,        // Expression
  UNARY_EXPRESSION,              // Expression
  PRIMARY,                       // Expression
  PRIMARY_NOT_THIS,              // Expression
  CREATION_EXPRESSION,           // CreationExpression
  FUNCTION_CALL_EXPRESSION,      // FunctionCallExpression
  EXPRESSION_LIST,               // ParseList<Expression>
  DIMENSIONS,                    // ParseList<Expression>
  QNAME_EXPRESSION,              // Expression

  // TERMINALS

  // literals
  NAME,             // Name
  INTEGER_LITERAL,  // IntegerLiteral
  FLOATING_LITERAL, // FloatingLiteral
  SINCE_SPECIFIER,  // SinceSpecifier
  STRING_LITERAL,   // StringLiteral

  // symbols (values for these should all be LexicalPhrase)
  AMPERSAND,
  AMPERSAND_EQUALS,
  ARROW,
  AT,
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
  HASH,
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
  QUESTION_MARK_DOT,
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
  ABSTRACT_KEYWORD,
  BOOLEAN_KEYWORD,
  BREAK_KEYWORD,
  BYTE_KEYWORD,
  CAST_KEYWORD,
  CATCH_KEYWORD,
  CLASS_KEYWORD,
  COMPOUND_KEYWORD,
  CONTINUE_KEYWORD,
  CREATE_KEYWORD,
  DOUBLE_KEYWORD,
  ELSE_KEYWORD,
  EXTENDS_KEYWORD,
  FALSE_KEYWORD,
  FINAL_KEYWORD,
  FINALLY_KEYWORD,
  FLOAT_KEYWORD,
  FOR_KEYWORD,
  GETTER_KEYWORD,
  IF_KEYWORD,
  IMMUTABLE_KEYWORD,
  IMPLEMENTS_KEYWORD,
  IMPORT_KEYWORD,
  INSTANCEOF_KEYWORD,
  IN_KEYWORD,
  INT_KEYWORD,
  INTERFACE_KEYWORD,
  LONG_KEYWORD,
  MUTABLE_KEYWORD,
  NATIVE_KEYWORD,
  NEW_KEYWORD,
  NULL_KEYWORD,
  OBJECT_KEYWORD,
  PACKAGE_KEYWORD,
  PROPERTY_KEYWORD,
  RETURN_KEYWORD,
  SELFISH_KEYWORD,
  SETTER_KEYWORD,
  SHORT_KEYWORD,
  STATIC_KEYWORD,
  SUPER_KEYWORD,
  THIS_KEYWORD,
  THROW_KEYWORD,
  THROWS_KEYWORD,
  TRUE_KEYWORD,
  TRY_KEYWORD,
  UBYTE_KEYWORD,
  UINT_KEYWORD,
  ULONG_KEYWORD,
  UNBACKED_KEYWORD,
  UNCHECKED_KEYWORD,
  USHORT_KEYWORD,
  VOID_KEYWORD,
  WHILE_KEYWORD,

  ;

}
