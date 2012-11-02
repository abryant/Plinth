package eu.bryants.anthony.plinth.parser;

import parser.Rule;
import parser.lalr.LALRRuleSet;
import eu.bryants.anthony.plinth.parser.rules.ClassDefinitionRule;
import eu.bryants.anthony.plinth.parser.rules.CompilationUnitRule;
import eu.bryants.anthony.plinth.parser.rules.CompoundDefinitionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.AdditiveExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.ClassCreationExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.ComparisonExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.DimensionsRule;
import eu.bryants.anthony.plinth.parser.rules.expression.ExpressionListRule;
import eu.bryants.anthony.plinth.parser.rules.expression.ExpressionNoTupleRule;
import eu.bryants.anthony.plinth.parser.rules.expression.ExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.FunctionCallExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.LogicalExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.MultiplicativeExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.PrimaryNoTrailingTypeRule;
import eu.bryants.anthony.plinth.parser.rules.expression.PrimaryRule;
import eu.bryants.anthony.plinth.parser.rules.expression.QNameExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.ShiftExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.TupleExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.TupleIndexExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.UnaryExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.member.ConstructorRule;
import eu.bryants.anthony.plinth.parser.rules.member.FieldRule;
import eu.bryants.anthony.plinth.parser.rules.member.InitialiserRule;
import eu.bryants.anthony.plinth.parser.rules.member.MemberListRule;
import eu.bryants.anthony.plinth.parser.rules.member.MethodRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ArgumentsRule;
import eu.bryants.anthony.plinth.parser.rules.misc.AssigneeListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.AssigneeNoQNameRule;
import eu.bryants.anthony.plinth.parser.rules.misc.AssigneeRule;
import eu.bryants.anthony.plinth.parser.rules.misc.DeclarationAssigneeListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ForInitRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ForUpdateRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ImportsRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ModifiersRule;
import eu.bryants.anthony.plinth.parser.rules.misc.NestedQNameListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.OptionalModifiersRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ParameterListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ParametersRule;
import eu.bryants.anthony.plinth.parser.rules.misc.QNameListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.QNameRule;
import eu.bryants.anthony.plinth.parser.rules.statement.AssignStatementRule;
import eu.bryants.anthony.plinth.parser.rules.statement.BlockRule;
import eu.bryants.anthony.plinth.parser.rules.statement.BreakStatementRule;
import eu.bryants.anthony.plinth.parser.rules.statement.ContinueStatementRule;
import eu.bryants.anthony.plinth.parser.rules.statement.ForStatementRule;
import eu.bryants.anthony.plinth.parser.rules.statement.IfStatementRule;
import eu.bryants.anthony.plinth.parser.rules.statement.PrefixIncDecStatementRule;
import eu.bryants.anthony.plinth.parser.rules.statement.ReturnStatementRule;
import eu.bryants.anthony.plinth.parser.rules.statement.ShorthandAssignmentRule;
import eu.bryants.anthony.plinth.parser.rules.statement.StatementRule;
import eu.bryants.anthony.plinth.parser.rules.statement.StatementsRule;
import eu.bryants.anthony.plinth.parser.rules.statement.WhileStatementRule;
import eu.bryants.anthony.plinth.parser.rules.type.ReturnTypeRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeListNoQNameRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeNoQNameRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeRule;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class PlinthParseRules
{
  private static final Rule<ParseType> startRule = new CompilationUnitRule();

  @SuppressWarnings("rawtypes")
  public static final Rule[] RULES = new Rule[]
  {
    // expression
    new AdditiveExpressionRule(),
    new ClassCreationExpressionRule(),
    new ComparisonExpressionRule(),
    new DimensionsRule(),
    new ExpressionListRule(),
    new ExpressionNoTupleRule(),
    new ExpressionRule(),
    new FunctionCallExpressionRule(),
    new LogicalExpressionRule(),
    new MultiplicativeExpressionRule(),
    new PrimaryNoTrailingTypeRule(),
    new PrimaryRule(),
    new QNameExpressionRule(),
    new ShiftExpressionRule(),
    new TupleExpressionRule(),
    new TupleIndexExpressionRule(),
    new UnaryExpressionRule(),

    // member
    new ConstructorRule(),
    new FieldRule(),
    new InitialiserRule(),
    new MemberListRule(),
    new MethodRule(),

    // misc
    new ArgumentsRule(),
    new AssigneeListRule(),
    new AssigneeNoQNameRule(),
    new AssigneeRule(),
    new DeclarationAssigneeListRule(),
    new ForInitRule(),
    new ForUpdateRule(),
    new ImportsRule(),
    new ModifiersRule(),
    new NestedQNameListRule(),
    new OptionalModifiersRule(),
    new ParameterListRule(),
    new ParametersRule(),
    new QNameListRule(),
    new QNameRule(),

    // statement
    new AssignStatementRule(),
    new BlockRule(),
    new BreakStatementRule(),
    new ContinueStatementRule(),
    new ForStatementRule(),
    new IfStatementRule(),
    new PrefixIncDecStatementRule(),
    new ReturnStatementRule(),
    new ShorthandAssignmentRule(),
    new StatementRule(),
    new StatementsRule(),
    new WhileStatementRule(),

    // type
    new ReturnTypeRule(),
    new TypeListNoQNameRule(),
    new TypeNoQNameRule(),
    new TypeRule(),

    // top level
    // startRule does not need to be included here: new CompilationUnitRule(),
    new ClassDefinitionRule(),
    new CompoundDefinitionRule(),
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
