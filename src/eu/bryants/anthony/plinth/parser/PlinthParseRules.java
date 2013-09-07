package eu.bryants.anthony.plinth.parser;

import parser.Rule;
import parser.lalr.LALRRuleSet;
import eu.bryants.anthony.plinth.parser.rules.ClassDefinitionRule;
import eu.bryants.anthony.plinth.parser.rules.CompilationUnitRule;
import eu.bryants.anthony.plinth.parser.rules.CompoundDefinitionRule;
import eu.bryants.anthony.plinth.parser.rules.InterfaceDefinitionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.AdditiveExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.ComparisonExpressionLessThanQNameRule;
import eu.bryants.anthony.plinth.parser.rules.expression.ConditionalExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.CreationExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.DimensionsRule;
import eu.bryants.anthony.plinth.parser.rules.expression.EqualityExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.ExpressionListRule;
import eu.bryants.anthony.plinth.parser.rules.expression.ExpressionNotLessThanQNameRule;
import eu.bryants.anthony.plinth.parser.rules.expression.ExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.FunctionCallExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.LogicalExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.MultiplicativeExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.PrimaryNotThisRule;
import eu.bryants.anthony.plinth.parser.rules.expression.PrimaryRule;
import eu.bryants.anthony.plinth.parser.rules.expression.QNameExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.QNameOrLessThanExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.ShiftExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.TupleExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.TupleIndexExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.expression.UnaryExpressionRule;
import eu.bryants.anthony.plinth.parser.rules.member.ConstructorRule;
import eu.bryants.anthony.plinth.parser.rules.member.FieldRule;
import eu.bryants.anthony.plinth.parser.rules.member.InitialiserRule;
import eu.bryants.anthony.plinth.parser.rules.member.MemberListRule;
import eu.bryants.anthony.plinth.parser.rules.member.MethodRule;
import eu.bryants.anthony.plinth.parser.rules.member.PropertyRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ArgumentListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ArgumentsRule;
import eu.bryants.anthony.plinth.parser.rules.misc.AssigneeListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.AssigneeNoQNameRule;
import eu.bryants.anthony.plinth.parser.rules.misc.AssigneeRule;
import eu.bryants.anthony.plinth.parser.rules.misc.CatchTypeListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.DeclarationAssigneeListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ForInitRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ForUpdateRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ImplementsClauseRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ImportsRule;
import eu.bryants.anthony.plinth.parser.rules.misc.InterfaceListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ModifiersRule;
import eu.bryants.anthony.plinth.parser.rules.misc.NestedQNameListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.OptionalModifiersRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ParameterListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ParametersRule;
import eu.bryants.anthony.plinth.parser.rules.misc.PropertyMethodListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.PropertyMethodRule;
import eu.bryants.anthony.plinth.parser.rules.misc.QNameListRule;
import eu.bryants.anthony.plinth.parser.rules.misc.QNameRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ThrowsClauseRule;
import eu.bryants.anthony.plinth.parser.rules.misc.ThrowsListRule;
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
import eu.bryants.anthony.plinth.parser.rules.statement.TryCatchStatementRule;
import eu.bryants.anthony.plinth.parser.rules.statement.TryFinallyStatementRule;
import eu.bryants.anthony.plinth.parser.rules.statement.WhileStatementRule;
import eu.bryants.anthony.plinth.parser.rules.type.ArrayTypeTrailingArgsRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.ArrayTypeTrailingArgsRule;
import eu.bryants.anthony.plinth.parser.rules.type.BasicTypeRule;
import eu.bryants.anthony.plinth.parser.rules.type.NamedTypeNoModifiersRule;
import eu.bryants.anthony.plinth.parser.rules.type.OptionalTypeParametersRule;
import eu.bryants.anthony.plinth.parser.rules.type.ReturnTypeRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeArgumentDoubleRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeArgumentListDoubleRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeArgumentListRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeArgumentNotQNameRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeArgumentRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeBoundListDoubleRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeBoundListRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeBoundListRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeDoubleRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeListNotQNameRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeListRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeNoSimpleArgumentsRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeNoTrailingArgumentsRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeNotQNameRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeParameterListRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeParameterListRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeParameterRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeParameterRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeTrailingArgsRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.TypeTrailingArgsRule;
import eu.bryants.anthony.plinth.parser.rules.type.WildcardTypeArgumentDoubleRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.WildcardTypeArgumentRAngleRule;
import eu.bryants.anthony.plinth.parser.rules.type.WildcardTypeArgumentRule;

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
    new ComparisonExpressionLessThanQNameRule(),
    new ConditionalExpressionRule(),
    new CreationExpressionRule(),
    new DimensionsRule(),
    new EqualityExpressionRule(),
    new ExpressionListRule(),
    new ExpressionNotLessThanQNameRule(),
    new ExpressionRule(),
    new FunctionCallExpressionRule(),
    new LogicalExpressionRule(),
    new MultiplicativeExpressionRule(),
    new PrimaryNotThisRule(),
    new PrimaryRule(),
    new QNameExpressionRule(),
    new QNameOrLessThanExpressionRule(),
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
    new PropertyRule(),

    // misc
    new ArgumentListRule(),
    new ArgumentsRule(),
    new AssigneeListRule(),
    new AssigneeNoQNameRule(),
    new AssigneeRule(),
    new CatchTypeListRule(),
    new DeclarationAssigneeListRule(),
    new ForInitRule(),
    new ForUpdateRule(),
    new ImplementsClauseRule(),
    new ImportsRule(),
    new InterfaceListRule(),
    new ModifiersRule(),
    new NestedQNameListRule(),
    new OptionalModifiersRule(),
    new ParameterListRule(),
    new ParametersRule(),
    new PropertyMethodListRule(),
    new PropertyMethodRule(),
    new QNameListRule(),
    new QNameRule(),
    new ThrowsClauseRule(),
    new ThrowsListRule(),

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
    new TryCatchStatementRule(),
    new TryFinallyStatementRule(),
    new WhileStatementRule(),

    // type
    new ArrayTypeTrailingArgsRAngleRule(),
    new ArrayTypeTrailingArgsRule(),
    new BasicTypeRule(),
    new NamedTypeNoModifiersRule(),
    new OptionalTypeParametersRule(),
    new ReturnTypeRule(),
    new TypeArgumentDoubleRAngleRule(),
    new TypeArgumentListDoubleRAngleRule(),
    new TypeArgumentListRAngleRule(),
    new TypeArgumentNotQNameRule(),
    new TypeArgumentRAngleRule(),
    new TypeBoundListDoubleRAngleRule(),
    new TypeBoundListRAngleRule(),
    new TypeBoundListRule(),
    new TypeDoubleRAngleRule(),
    new TypeListNotQNameRule(),
    new TypeListRule(),
    new TypeNoSimpleArgumentsRule(),
    new TypeNotQNameRule(),
    new TypeNoTrailingArgumentsRule(),
    new TypeParameterListRAngleRule(),
    new TypeParameterListRule(),
    new TypeParameterRAngleRule(),
    new TypeParameterRule(),
    new TypeRAngleRule(),
    new TypeRule(),
    new TypeTrailingArgsRAngleRule(),
    new TypeTrailingArgsRule(),
    new WildcardTypeArgumentDoubleRAngleRule(),
    new WildcardTypeArgumentRAngleRule(),
    new WildcardTypeArgumentRule(),

    // top level
    // startRule does not need to be included here: new CompilationUnitRule(),
    new ClassDefinitionRule(),
    new CompoundDefinitionRule(),
    new InterfaceDefinitionRule(),
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
