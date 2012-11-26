package eu.bryants.anthony.plinth.parser.rules.member;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ModifierType;
import eu.bryants.anthony.plinth.parser.parseAST.NativeSpecifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 20 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class MethodRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  // do not use RETURN_TYPE here, as it causes shift-reduce conflicts with field declarations
  // basically, given "A b" we couldn't tell if A was a TYPE or a RETURN_TYPE without further information
  private static final Production<ParseType> PRODUCTION                            = new Production<ParseType>(                     ParseType.TYPE,         ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.BLOCK);
  private static final Production<ParseType> VOID_PRODUCTION                       = new Production<ParseType>(                     ParseType.VOID_KEYWORD, ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.BLOCK);
  private static final Production<ParseType> MODIFIERS_PRODUCTION                  = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE,         ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.BLOCK);
  private static final Production<ParseType> MODIFIERS_VOID_PRODUCTION             = new Production<ParseType>(ParseType.MODIFIERS, ParseType.VOID_KEYWORD, ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.BLOCK);
  private static final Production<ParseType> DECLARATION_PRODUCTION                = new Production<ParseType>(                     ParseType.TYPE,         ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> VOID_DECLARATION_PRODUCTION           = new Production<ParseType>(                     ParseType.VOID_KEYWORD, ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> MODIFIERS_DECLARATION_PRODUCTION      = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE,         ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> MODIFIERS_VOID_DECLARATION_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.VOID_KEYWORD, ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.SEMICOLON);

  public MethodRule()
  {
    super(ParseType.METHOD, PRODUCTION, VOID_PRODUCTION, MODIFIERS_PRODUCTION, MODIFIERS_VOID_PRODUCTION,
                            DECLARATION_PRODUCTION, VOID_DECLARATION_PRODUCTION, MODIFIERS_DECLARATION_PRODUCTION, MODIFIERS_VOID_DECLARATION_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      Type returnType = (Type) args[0];
      Name name = (Name) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[2];
      Block block = (Block) args[3];
      return processModifiers(null, returnType, name.getName(), parameters.toArray(new Parameter[parameters.size()]), block,
                              LexicalPhrase.combine(returnType.getLexicalPhrase(), name.getLexicalPhrase(), parameters.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    if (production == VOID_PRODUCTION)
    {
      Name name = (Name) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[2];
      Block block = (Block) args[3];
      return processModifiers(null, new VoidType((LexicalPhrase) args[0]), name.getName(), parameters.toArray(new Parameter[parameters.size()]), block,
                              LexicalPhrase.combine((LexicalPhrase) args[0], name.getLexicalPhrase(), parameters.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    if (production == MODIFIERS_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type returnType = (Type) args[1];
      Name name = (Name) args[2];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[3];
      Block block = (Block) args[4];
      return processModifiers(modifiers, returnType, name.getName(), parameters.toArray(new Parameter[parameters.size()]), block,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), returnType.getLexicalPhrase(), name.getLexicalPhrase(), parameters.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    if (production == MODIFIERS_VOID_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Name name = (Name) args[2];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[3];
      Block block = (Block) args[4];
      return processModifiers(modifiers, new VoidType((LexicalPhrase) args[1]), name.getName(), parameters.toArray(new Parameter[parameters.size()]), block,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase(), parameters.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    if (production == DECLARATION_PRODUCTION)
    {
      Type returnType = (Type) args[0];
      Name name = (Name) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[2];
      return processModifiers(null, returnType, name.getName(), parameters.toArray(new Parameter[parameters.size()]), null,
                              LexicalPhrase.combine(returnType.getLexicalPhrase(), name.getLexicalPhrase(), parameters.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == VOID_DECLARATION_PRODUCTION)
    {
      Name name = (Name) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[2];
      return processModifiers(null, new VoidType((LexicalPhrase) args[0]), name.getName(), parameters.toArray(new Parameter[parameters.size()]), null,
                              LexicalPhrase.combine((LexicalPhrase) args[0], name.getLexicalPhrase(), parameters.getLexicalPhrase(), (LexicalPhrase) args[3]));
    }
    if (production == MODIFIERS_DECLARATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type returnType = (Type) args[1];
      Name name = (Name) args[2];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[3];
      return processModifiers(modifiers, returnType, name.getName(), parameters.toArray(new Parameter[parameters.size()]), null,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), returnType.getLexicalPhrase(), name.getLexicalPhrase(), parameters.getLexicalPhrase(), (LexicalPhrase) args[4]));
    }
    if (production == MODIFIERS_VOID_DECLARATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Name name = (Name) args[2];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[3];
      return processModifiers(modifiers, new VoidType((LexicalPhrase) args[1]), name.getName(), parameters.toArray(new Parameter[parameters.size()]), null,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase(), parameters.getLexicalPhrase(), (LexicalPhrase) args[4]));
    }
    throw badTypeList();
  }

  private Method processModifiers(ParseList<Modifier> modifiers, Type returnType, String name, Parameter[] parameters, Block block, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isStatic = false;
    boolean isImmutable = false;
    String nativeName = null;
    if (modifiers != null)
    {
      for (Modifier modifier : modifiers)
      {
        if (modifier.getModifierType() == ModifierType.FINAL)
        {
          throw new LanguageParseException("Unexpected modifier: Methods cannot be final", modifier.getLexicalPhrase());
        }
        else if (modifier.getModifierType() == ModifierType.IMMUTABLE)
        {
          if (isImmutable)
          {
            throw new LanguageParseException("Duplicate 'immutable' modifier", modifier.getLexicalPhrase());
          }
          isImmutable = true;
        }
        else if (modifier.getModifierType() == ModifierType.NATIVE)
        {
          if (nativeName != null)
          {
            throw new LanguageParseException("Duplicate 'native' specifier", modifier.getLexicalPhrase());
          }
          NativeSpecifier nativeSpecifier = (NativeSpecifier) modifier;
          nativeName = nativeSpecifier.getNativeName();
          if (nativeName == null)
          {
            nativeName = name;
          }
        }
        else if (modifier.getModifierType() == ModifierType.STATIC)
        {
          if (isStatic)
          {
            throw new LanguageParseException("Duplicate 'static' modifier", modifier.getLexicalPhrase());
          }
          isStatic = true;
        }
        else
        {
          throw new IllegalStateException("Unknown modifier: " + modifier);
        }
      }
    }
    return new Method(returnType, name, isStatic, isImmutable, nativeName, parameters, block, lexicalPhrase);
  }

}
