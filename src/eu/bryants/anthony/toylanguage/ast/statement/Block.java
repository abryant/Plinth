package eu.bryants.anthony.toylanguage.ast.statement;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import eu.bryants.anthony.toylanguage.ast.metadata.Variable;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 6 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class Block extends Statement
{
  private Statement[] statements;

  private Map<String, Variable> variables = new HashMap<String, Variable>();

  public Block(Statement[] statements, LexicalPhrase lexicalPhrase)
  {
    super(lexicalPhrase);
    this.statements = statements;
  }

  /**
   * @return the statements
   */
  public Statement[] getStatements()
  {
    return statements;
  }

  /**
   * Adds the specified variable to this Block's variables set.
   * @param variable - the variable to add
   * @return the previous variable with the same name as the given one, or null if none previously existed
   */
  public Variable addVariable(Variable variable)
  {
    return variables.put(variable.getName(), variable);
  }

  /**
   * @param name - the name of the variable to get
   * @return true if this block contains the specified variable
   */
  public Variable getVariable(String name)
  {
    return variables.get(name);
  }

  /**
   * @return the collection of all of the variables currently in this Block
   */
  public Collection<Variable> getVariables()
  {
    return variables.values();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean stopsExecution()
  {
    // don't worry about any of the statements but the last - the control flow checker will make sure execution can always get to the last instruction
    return statements.length > 0 && statements[statements.length - 1].stopsExecution();
  }

  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer("{\n");
    for (Statement s : statements)
    {
      buffer.append(String.valueOf(s).replaceAll("(?m)^", "  "));
      buffer.append('\n');
    }
    buffer.append('}');
    return buffer.toString();
  }
}
