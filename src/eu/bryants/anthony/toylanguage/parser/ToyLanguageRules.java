package eu.bryants.anthony.toylanguage.parser;

import parser.Rule;
import parser.lalr.LALRRuleSet;
import eu.bryants.anthony.toylanguage.parser.rules.CompilationUnitRule;
import eu.bryants.anthony.toylanguage.parser.rules.FunctionRule;
import eu.bryants.anthony.toylanguage.parser.rules.FunctionsRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.AdditiveExpressionRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.ComparisonExpressionRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.DimensionsRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.ExpressionListRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.FunctionCallExpressionRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.LogicalExpressionRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.MultiplicativeExpressionRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.PrimaryRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.TupleExpressionRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.TupleIndexExpressionRule;
import eu.bryants.anthony.toylanguage.parser.rules.expression.UnaryExpressionRule;
import eu.bryants.anthony.toylanguage.parser.rules.misc.AssigneeListRule;
import eu.bryants.anthony.toylanguage.parser.rules.misc.ParametersRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.AssignStatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.BlockRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.BreakStatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.ContinueStatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.IfStatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.ReturnStatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.StatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.StatementsRule;
import eu.bryants.anthony.toylanguage.parser.rules.statement.WhileStatementRule;
import eu.bryants.anthony.toylanguage.parser.rules.type.TypeListRule;
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
    new ComparisonExpressionRule(),
    new DimensionsRule(),
    new ExpressionListRule(),
    new FunctionCallExpressionRule(),
    new LogicalExpressionRule(),
    new MultiplicativeExpressionRule(),
    new PrimaryRule(),
    new TupleExpressionRule(),
    new TupleIndexExpressionRule(),
    new UnaryExpressionRule(),

    // misc
    new AssigneeListRule(),
    new ParametersRule(),

    // statement
    new AssignStatementRule(),
    new BlockRule(),
    new BreakStatementRule(),
    new ContinueStatementRule(),
    new IfStatementRule(),
    new ReturnStatementRule(),
    new StatementRule(),
    new StatementsRule(),
    new WhileStatementRule(),

    // type
    new TypeListRule(),
    new TypeRule(),

    // top level
    // startRule does not need to be included here: new CompilationUnitRule(),
    new FunctionRule(),
    new FunctionsRule(),
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
