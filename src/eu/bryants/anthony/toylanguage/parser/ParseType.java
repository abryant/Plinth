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

  COMPILATION_UNIT, // CompilationUnit
  FUNCTIONS,        // ParseList<Function>
  FUNCTION,         // Function
  PARAMETERS,       // ParseList<Parameter>
  BLOCK,            // Block
  STATEMENTS,       // ParseList<Statement>
  STATEMENT,        // Statement
  ASSIGN_STATEMENT, // AssignStatement
  IF_STATEMENT,     // IfStatement
  RETURN_STATEMENT, // ReturnStatement
  VARIABLE_DEFINITION_STATEMENT, // VariableDefinition
  WHILE_STATEMENT,  // WhileStatement
  TYPE,             // Type
  EXPRESSION,       // Expression
  ADDITIVE_EXPRESSION, // Expression
  UNARY_EXPRESSION, // Expression
  PRIMARY,          // Expression
  FUNCTION_CALL,    // FunctionCallExpression
  ARGUMENTS,        // ParseList<Expression>

  // TERMINALS

  // literals
  NAME,             // Name
  INTEGER_LITERAL,  // IntegerLiteral
  FLOATING_LITERAL, // FloatingLiteral

  // symbols (values for these should all be LexicalPhrase)
  COMMA,
  DOUBLE_EQUALS,
  EQUALS,
  EXCLAIMATION_MARK,
  EXCLAIMATION_MARK_EQUALS,
  LANGLE,
  LANGLE_EQUALS,
  LBRACE,
  LPAREN,
  MINUS,
  PLUS,
  RANGLE,
  RANGLE_EQUALS,
  RBRACE,
  RPAREN,
  SEMICOLON,

  // keywords (values for these should all be LexicalPhrase)
  BOOLEAN_KEYWORD,
  CAST_KEYWORD,
  DOUBLE_KEYWORD,
  ELSE_KEYWORD,
  FALSE_KEYWORD,
  IF_KEYWORD,
  INT_KEYWORD,
  RETURN_KEYWORD,
  TRUE_KEYWORD,
  WHILE_KEYWORD,

  ;

}
