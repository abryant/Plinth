package eu.bryants.anthony.toylanguage.parser;

import parser.Rule;
import parser.lalr.LALRRuleSet;
import eu.bryants.anthony.toylanguage.parser.rules.ArgumentsRule;
import eu.bryants.anthony.toylanguage.parser.rules.CompilationUnitRule;
import eu.bryants.anthony.toylanguage.parser.rules.FunctionRule;
import eu.bryants.anthony.toylanguage.parser.rules.FunctionsRule;
import eu.bryants.anthony.toylanguage.parser.rules.ParametersRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.AdditiveExpressionRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.ExpressionRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.MultiplicativeExpressionRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.PrimaryRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.UnaryExpressionRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.AssignStatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.BlockRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.IfStatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.ReturnStatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.StatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.StatementsRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.VariableDefinitionRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.WhileStatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.type.TypeRule;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class ToyLanguageRules
{
  private static final Rule<ParseType> startRule = new CompilationUnitRule();

  @SuppressWarnings("rawtypes")
  public static final Rule[] RULES = new Rule[]
  {
    // expression
    new AdditiveExpressionRule(),
    new ExpressionRule(),
    new MultiplicativeExpressionRule(),
    new PrimaryRule(),
    new UnaryExpressionRule(),

    // statement
    new AssignStatementRule(),
    new BlockRule(),
    new IfStatementRule(),
    new ReturnStatementRule(),
    new StatementRule(),
    new StatementsRule(),
    new VariableDefinitionRule(),
    new WhileStatementRule(),

    // type
    new TypeRule(),

    // top level
    new ArgumentsRule(),
    // startRule does not need to be included here: new CompilationUnitRule(),
    new FunctionRule(),
    new FunctionsRule(),
    new ParametersRule(),
  };

  @SuppressWarnings("unchecked")
  public static LALRRuleSet<ParseType> getAllRules()
  {
    LALRRuleSet<ParseType> ruleSet = new LALRRuleSet<ParseType>();
    ruleSet.addStartRule(startRule);
    for (Rule<ParseType> r : RULES)
    {
      ruleSet.addRule(r);
    }
    return ruleSet;
  }
}
