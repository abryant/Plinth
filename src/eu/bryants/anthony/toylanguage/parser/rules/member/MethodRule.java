package eu.bryants.anthony.toylanguage.parser.rules.member;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.member.Method;
import eu.bryants.anthony.toylanguage.ast.misc.Parameter;
import eu.bryants.anthony.toylanguage.ast.statement.Block;
import eu.bryants.anthony.toylanguage.ast.terminal.Name;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.ast.type.VoidType;
import eu.bryants.anthony.toylanguage.parser.LanguageParseException;
import eu.bryants.anthony.toylanguage.parser.ParseType;
import eu.bryants.anthony.toylanguage.parser.parseAST.Modifier;
import eu.bryants.anthony.toylanguage.parser.parseAST.ModifierType;
import eu.bryants.anthony.toylanguage.parser.parseAST.ParseList;

/*
 * Created on 20 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class MethodRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PARAMS_PRODUCTION      = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE,         ParseType.NAME, ParseType.LPAREN, ParseType.PARAMETERS, ParseType.RPAREN, ParseType.BLOCK);
  private static final Production<ParseType> PRODUCTION             = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE,         ParseType.NAME, ParseType.LPAREN,                       ParseType.RPAREN, ParseType.BLOCK);
  private static final Production<ParseType> VOID_PARAMS_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.VOID_KEYWORD, ParseType.NAME, ParseType.LPAREN, ParseType.PARAMETERS, ParseType.RPAREN, ParseType.BLOCK);
  private static final Production<ParseType> VOID_PRODUCTION        = new Production<ParseType>(ParseType.MODIFIERS, ParseType.VOID_KEYWORD, ParseType.NAME, ParseType.LPAREN,                       ParseType.RPAREN, ParseType.BLOCK);

  @SuppressWarnings("unchecked")
  public MethodRule()
  {
    super(ParseType.METHOD, PARAMS_PRODUCTION, PRODUCTION, VOID_PARAMS_PRODUCTION, VOID_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PARAMS_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type returnType = (Type) args[1];
      Name name = (Name) args[2];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[4];
      Block block = (Block) args[6];
      return processModifiers(modifiers, returnType, name.getName(), parameters.toArray(new Parameter[parameters.size()]), block,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), returnType.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[3], parameters.getLexicalPhrase(), (LexicalPhrase) args[5], block.getLexicalPhrase()));
    }
    if (production == PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type returnType = (Type) args[1];
      Name name = (Name) args[2];
      Block block = (Block) args[5];
      return processModifiers(modifiers, returnType, name.getName(), new Parameter[0], block,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), returnType.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[3], (LexicalPhrase) args[4], block.getLexicalPhrase()));
    }
    if (production == VOID_PARAMS_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Name name = (Name) args[2];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[4];
      Block block = (Block) args[6];
      return processModifiers(modifiers, new VoidType((LexicalPhrase) args[1]), name.getName(), parameters.toArray(new Parameter[parameters.size()]), block,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase(), (LexicalPhrase) args[3], parameters.getLexicalPhrase(), (LexicalPhrase) args[5], block.getLexicalPhrase()));
    }
    if (production == VOID_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Name name = (Name) args[2];
      Block block = (Block) args[5];
      return processModifiers(modifiers, new VoidType((LexicalPhrase) args[1]), name.getName(), new Parameter[0], block,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase(), (LexicalPhrase) args[3], (LexicalPhrase) args[4], block.getLexicalPhrase()));
    }
    throw badTypeList();
  }

  private Method processModifiers(ParseList<Modifier> modifiers, Type returnType, String name, Parameter[] parameters, Block block, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    for (Modifier modifier : modifiers)
    {
      if (modifier.getModifierType() == ModifierType.STATIC)
      {
        // TODO: allow static methods
        throw new LanguageParseException("Unexpected modifier: Methods cannot yet be static", modifier.getLexicalPhrase());
      }
      throw new IllegalStateException("Unknown modifier: " + modifier);
    }
    return new Method(returnType, name, parameters, block, lexicalPhrase);
  }

}
