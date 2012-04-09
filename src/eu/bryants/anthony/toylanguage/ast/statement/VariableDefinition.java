package eu.bryants.anthony.toylanguage.ast.statement;

import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 8 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class VariableDefinition extends Statement
{

  private Type type;
  private Name name;
  private Expression expression;

  private Variable variable;

  /**
   * Creates a new VariableDefinition with the specified type, name, and optionally an expression.
   * @param type - the type of the variable being defined
   * @param name - the name of the variable
   * @param expression - the expression being assigned to the variable, or null if this is just a declaration
   * @param lexicalPhrase - the lexicalPhrase containing information about this variable definition
   */
  public VariableDefinition(Type type, Name name, Expression expression, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.type = type;
    this.name = name;
    this.expression = expression;

    variable = new Variable(type, name.getName());
  }

  /**
   * @return the type
   */
  public Type getType()
  {
    return type;
  }

  /**
   * @return the name
   */
  public Name getName()
  {
    return name;
  }

  /**
   * @return the expression
   */
  public Expression getExpression()
  {
    return expression;
  }

  /**
   * @return the variable
   */
  public Variable getVariable()
  {
    return variable;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopsExecution()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return type + " " + name + (expression == null ? "" : " = " + expression) + ";";
  }
}
