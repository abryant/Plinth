package eu.bryants.anthony.plinth.parser;

import parser.lalr.LALRParserCodeGenerator;
import parser.lalr.LALRRuleSet;

/*
 * Created on 2 Apr 2012
 */

/**
 * @author Anthony Bryant
 */
public class PlinthParserGenerator
{
  public static void main(String[] args)
  {
    LALRRuleSet<ParseType> rules = PlinthParseRules.getAllRules();
    LALRParserCodeGenerator<ParseType> codeGenerator = new LALRParserCodeGenerator<ParseType>(rules, ParseType.GENERATED_START_RULE);
    codeGenerator.generateCode(System.out);
  }
}
