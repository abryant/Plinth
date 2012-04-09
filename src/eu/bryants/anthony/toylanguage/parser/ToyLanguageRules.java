package eu.bryants.anthony.toylanguage.parser;

import parser.Rule;
import parser.lalr.LALRRuleSet;
import eu.bryants.anthony.toylanguage.parser.rules.ArgumentsRule;
import eu.bryants.anthony.toylanguage.parser.rules.AssignStatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.BlockRule;
import eu.bryants.anthony.toylanguage.parser.rules.CompilationUnitRule;
import eu.bryants.anthony.toylanguage.parser.rules.ExpressionRule;
import eu.bryants.anthony.toylanguage.parser.rules.FunctionRule;
import eu.bryants.anthony.toylanguage.parser.rules.FunctionsRule;
import eu.bryants.anthony.toylanguage.parser.rules.IfStatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.ParametersRule;
import eu.bryants.anthony.toylanguage.parser.rules.PrimaryRule;
import eu.bryants.anthony.toylanguage.parser.rules.ReturnStatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.StatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.StatementsRule;
import eu.bryants.anthony.toylanguage.parser.rules.TypeRule;
import eu.bryants.anthony.toylanguage.parser.rules.VariableDefinitionRule;
import eu.bryants.anthony.toylanguage.parser.rules.WhileStatementRule;

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
    new ArgumentsRule(),
    new AssignStatementRule(),
    new BlockRule(),
    // startRule does not need to be included here: new CompilationUnitRule(),
    new ExpressionRule(),
    new FunctionRule(),
    new FunctionsRule(),
    new IfStatementRule(),
    new ParametersRule(),
    new PrimaryRule(),
    new ReturnStatementRule(),
    new StatementRule(),
    new StatementsRule(),
    new TypeRule(),
    new VariableDefinitionRule(),
    new WhileStatementRule(),
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
