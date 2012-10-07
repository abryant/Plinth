package eu.bryants.anthony.plinth.parser.rules.member;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.Constructor;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ModifierType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 11 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class ConstructorRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> MODIFIERS_PRODUCTION            = new Production<ParseType>(ParseType.MODIFIERS, ParseType.NAME, ParseType.LPAREN, ParseType.RPAREN, ParseType.BLOCK);
  private static final Production<ParseType> MODIFIERS_PARAMETERS_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.NAME, ParseType.LPAREN, ParseType.PARAMETERS, ParseType.RPAREN, ParseType.BLOCK);
  private static final Production<ParseType> PRODUCTION                      = new Production<ParseType>(                     ParseType.NAME, ParseType.LPAREN, ParseType.RPAREN, ParseType.BLOCK);
  private static final Production<ParseType> PARAMETERS_PRODUCTION           = new Production<ParseType>(                     ParseType.NAME, ParseType.LPAREN, ParseType.PARAMETERS, ParseType.RPAREN, ParseType.BLOCK);

  @SuppressWarnings("unchecked")
  public ConstructorRule()
  {
    super(ParseType.CONSTRUCTOR, MODIFIERS_PRODUCTION, MODIFIERS_PARAMETERS_PRODUCTION, PRODUCTION, PARAMETERS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == MODIFIERS_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Name name = (Name) args[1];
      Block block = (Block) args[4];
      return processModifiers(modifiers, name.getName(), new Parameter[0], block, LexicalPhrase.combine(modifiers.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2], (LexicalPhrase) args[3], block.getLexicalPhrase()));
    }
    if (production == MODIFIERS_PARAMETERS_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Name name = (Name) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[3];
      Block block = (Block) args[5];
      return processModifiers(modifiers, name.getName(), parameters.toArray(new Parameter[parameters.size()]), block,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[2], parameters.getLexicalPhrase(), (LexicalPhrase) args[4], block.getLexicalPhrase()));
    }
    if (production == PRODUCTION)
    {
      Name name = (Name) args[0];
      Block block = (Block) args[3];
      return new Constructor(name.getName(), new Parameter[0], block, LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], (LexicalPhrase) args[2], block.getLexicalPhrase()));
    }
    if (production == PARAMETERS_PRODUCTION)
    {
      Name name = (Name) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[2];
      Block block = (Block) args[4];
      return new Constructor(name.getName(), parameters.toArray(new Parameter[parameters.size()]), block,
                             LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], parameters.getLexicalPhrase(), (LexicalPhrase) args[3], block.getLexicalPhrase()));
    }
    throw badTypeList();
  }

  private Constructor processModifiers(ParseList<Modifier> modifiers, String name, Parameter[] parameters, Block block, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    for (Modifier modifier : modifiers)
    {
      if (modifier.getModifierType() == ModifierType.FINAL)
      {
        throw new LanguageParseException("Unexpected modifier: Constructors cannot be final", modifier.getLexicalPhrase());
      }
      else if (modifier.getModifierType() == ModifierType.NATIVE)
      {
        throw new LanguageParseException("Unexpected modifier: Constructors cannot be native functions", modifier.getLexicalPhrase());
      }
      else if (modifier.getModifierType() == ModifierType.STATIC)
      {
        throw new LanguageParseException("Unexpected modifier: Constructors cannot be static", modifier.getLexicalPhrase());
      }
      else
      {
        throw new IllegalStateException("Unknown modifier: " + modifier);
      }
    }
    return new Constructor(name, parameters, block, lexicalPhrase);
  }
}
