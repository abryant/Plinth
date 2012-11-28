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

  private static final Production<ParseType> MODIFIERS_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.BLOCK);
  private static final Production<ParseType> PRODUCTION           = new Production<ParseType>(                     ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.BLOCK);

  public ConstructorRule()
  {
    super(ParseType.CONSTRUCTOR, MODIFIERS_PRODUCTION, PRODUCTION);
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
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[2];
      Block block = (Block) args[3];
      return processModifiers(modifiers, name.getName(), parameters.toArray(new Parameter[parameters.size()]), block,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), name.getLexicalPhrase(), parameters.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    if (production == PRODUCTION)
    {
      Name name = (Name) args[0];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[1];
      Block block = (Block) args[2];
      return new Constructor(false, name.getName(), parameters.toArray(new Parameter[parameters.size()]), block,
                             LexicalPhrase.combine(name.getLexicalPhrase(), parameters.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    throw badTypeList();
  }

  private Constructor processModifiers(ParseList<Modifier> modifiers, String name, Parameter[] parameters, Block block, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isImmutable = false;
    for (Modifier modifier : modifiers)
    {
      if (modifier.getModifierType() == ModifierType.FINAL)
      {
        throw new LanguageParseException("Unexpected modifier: Constructors cannot be final", modifier.getLexicalPhrase());
      }
      else if (modifier.getModifierType() == ModifierType.IMMUTABLE)
      {
        if (isImmutable)
        {
          throw new LanguageParseException("Duplicate 'immutable' modifier", modifier.getLexicalPhrase());
        }
        isImmutable = true;
      }
      else if (modifier.getModifierType() == ModifierType.MUTABLE)
      {
        throw new LanguageParseException("Unexpected modifier: Constructors cannot be mutable", modifier.getLexicalPhrase());
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
    return new Constructor(isImmutable, name, parameters, block, lexicalPhrase);
  }
}
