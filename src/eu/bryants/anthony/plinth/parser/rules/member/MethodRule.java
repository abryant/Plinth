package eu.bryants.anthony.plinth.parser.rules.member;

import java.util.LinkedList;
import java.util.List;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.member.Method;
import eu.bryants.anthony.plinth.ast.misc.AutoAssignParameter;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.statement.Statement;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.VoidType;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.NativeSpecifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.SinceModifier;
import eu.bryants.anthony.plinth.parser.parseAST.ThrownExceptionType;

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
  private static final Production<ParseType> PRODUCTION                            = new Production<ParseType>(                     ParseType.TYPE,         ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.THROWS_CLAUSE, ParseType.BLOCK);
  private static final Production<ParseType> VOID_PRODUCTION                       = new Production<ParseType>(                     ParseType.VOID_KEYWORD, ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.THROWS_CLAUSE, ParseType.BLOCK);
  private static final Production<ParseType> MODIFIERS_PRODUCTION                  = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE,         ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.THROWS_CLAUSE, ParseType.BLOCK);
  private static final Production<ParseType> MODIFIERS_VOID_PRODUCTION             = new Production<ParseType>(ParseType.MODIFIERS, ParseType.VOID_KEYWORD, ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.THROWS_CLAUSE, ParseType.BLOCK);
  private static final Production<ParseType> DECLARATION_PRODUCTION                = new Production<ParseType>(                     ParseType.TYPE,         ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.THROWS_CLAUSE, ParseType.SEMICOLON);
  private static final Production<ParseType> VOID_DECLARATION_PRODUCTION           = new Production<ParseType>(                     ParseType.VOID_KEYWORD, ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.THROWS_CLAUSE, ParseType.SEMICOLON);
  private static final Production<ParseType> MODIFIERS_DECLARATION_PRODUCTION      = new Production<ParseType>(ParseType.MODIFIERS, ParseType.TYPE,         ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.THROWS_CLAUSE, ParseType.SEMICOLON);
  private static final Production<ParseType> MODIFIERS_VOID_DECLARATION_PRODUCTION = new Production<ParseType>(ParseType.MODIFIERS, ParseType.VOID_KEYWORD, ParseType.NAME, ParseType.PARAMETER_LIST, ParseType.THROWS_CLAUSE, ParseType.SEMICOLON);

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
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[3];
      NamedType[] checkedThrownTypes = extractCheckedThrownTypes(throwsList);
      NamedType[] uncheckedThrownTypes = extractUncheckedThrownTypes(throwsList);
      Block block = (Block) args[4];
      return processModifiers(null, returnType, name.getName(), parameters.toArray(new Parameter[parameters.size()]), checkedThrownTypes, uncheckedThrownTypes, block,
                              LexicalPhrase.combine(returnType.getLexicalPhrase(), name.getLexicalPhrase(), parameters.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    if (production == VOID_PRODUCTION)
    {
      Name name = (Name) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[2];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[3];
      NamedType[] checkedThrownTypes = extractCheckedThrownTypes(throwsList);
      NamedType[] uncheckedThrownTypes = extractUncheckedThrownTypes(throwsList);
      Block block = (Block) args[4];
      return processModifiers(null, new VoidType((LexicalPhrase) args[0]), name.getName(), parameters.toArray(new Parameter[parameters.size()]), checkedThrownTypes, uncheckedThrownTypes, block,
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
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[4];
      NamedType[] checkedThrownTypes = extractCheckedThrownTypes(throwsList);
      NamedType[] uncheckedThrownTypes = extractUncheckedThrownTypes(throwsList);
      Block block = (Block) args[5];
      return processModifiers(modifiers, returnType, name.getName(), parameters.toArray(new Parameter[parameters.size()]), checkedThrownTypes, uncheckedThrownTypes, block,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), returnType.getLexicalPhrase(), name.getLexicalPhrase(), parameters.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    if (production == MODIFIERS_VOID_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Name name = (Name) args[2];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[3];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[4];
      NamedType[] checkedThrownTypes = extractCheckedThrownTypes(throwsList);
      NamedType[] uncheckedThrownTypes = extractUncheckedThrownTypes(throwsList);
      Block block = (Block) args[5];
      return processModifiers(modifiers, new VoidType((LexicalPhrase) args[1]), name.getName(), parameters.toArray(new Parameter[parameters.size()]), checkedThrownTypes, uncheckedThrownTypes, block,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase(), parameters.getLexicalPhrase(), block.getLexicalPhrase()));
    }
    if (production == DECLARATION_PRODUCTION)
    {
      Type returnType = (Type) args[0];
      Name name = (Name) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[2];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[3];
      NamedType[] checkedThrownTypes = extractCheckedThrownTypes(throwsList);
      NamedType[] uncheckedThrownTypes = extractUncheckedThrownTypes(throwsList);
      return processModifiers(null, returnType, name.getName(), parameters.toArray(new Parameter[parameters.size()]), checkedThrownTypes, uncheckedThrownTypes, null,
                              LexicalPhrase.combine(returnType.getLexicalPhrase(), name.getLexicalPhrase(), parameters.getLexicalPhrase(), (LexicalPhrase) args[4]));
    }
    if (production == VOID_DECLARATION_PRODUCTION)
    {
      Name name = (Name) args[1];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[2];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[3];
      NamedType[] checkedThrownTypes = extractCheckedThrownTypes(throwsList);
      NamedType[] uncheckedThrownTypes = extractUncheckedThrownTypes(throwsList);
      return processModifiers(null, new VoidType((LexicalPhrase) args[0]), name.getName(), parameters.toArray(new Parameter[parameters.size()]), checkedThrownTypes, uncheckedThrownTypes, null,
                              LexicalPhrase.combine((LexicalPhrase) args[0], name.getLexicalPhrase(), parameters.getLexicalPhrase(), (LexicalPhrase) args[4]));
    }
    if (production == MODIFIERS_DECLARATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type returnType = (Type) args[1];
      Name name = (Name) args[2];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[3];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[4];
      NamedType[] checkedThrownTypes = extractCheckedThrownTypes(throwsList);
      NamedType[] uncheckedThrownTypes = extractUncheckedThrownTypes(throwsList);
      return processModifiers(modifiers, returnType, name.getName(), parameters.toArray(new Parameter[parameters.size()]), checkedThrownTypes, uncheckedThrownTypes, null,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), returnType.getLexicalPhrase(), name.getLexicalPhrase(), parameters.getLexicalPhrase(), (LexicalPhrase) args[5]));
    }
    if (production == MODIFIERS_VOID_DECLARATION_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Name name = (Name) args[2];
      @SuppressWarnings("unchecked")
      ParseList<Parameter> parameters = (ParseList<Parameter>) args[3];
      @SuppressWarnings("unchecked")
      ParseList<ThrownExceptionType> throwsList = (ParseList<ThrownExceptionType>) args[4];
      NamedType[] checkedThrownTypes = extractCheckedThrownTypes(throwsList);
      NamedType[] uncheckedThrownTypes = extractUncheckedThrownTypes(throwsList);
      return processModifiers(modifiers, new VoidType((LexicalPhrase) args[1]), name.getName(), parameters.toArray(new Parameter[parameters.size()]), checkedThrownTypes, uncheckedThrownTypes, null,
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], name.getLexicalPhrase(), parameters.getLexicalPhrase(), (LexicalPhrase) args[5]));
    }
    throw badTypeList();
  }

  private Method processModifiers(ParseList<Modifier> modifiers, Type returnType, String name, Parameter[] parameters, NamedType[] checkedThrownTypes, NamedType[] uncheckedThrownTypes, Block block, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isAbstract = false;
    boolean isStatic = false;
    boolean isImmutable = false;
    String nativeName = null;
    SinceSpecifier sinceSpecifier = null;
    if (modifiers != null)
    {
      for (Modifier modifier : modifiers)
      {
        switch (modifier.getModifierType())
        {
        case ABSTRACT:
          if (isAbstract)
          {
            throw new LanguageParseException("Duplicate 'abstract' modifier", modifier.getLexicalPhrase());
          }
          isAbstract = true;
          break;
        case FINAL:
          throw new LanguageParseException("Unexpected modifier: Methods cannot be final", modifier.getLexicalPhrase());
        case IMMUTABLE:
          if (isImmutable)
          {
            throw new LanguageParseException("Duplicate 'immutable' modifier", modifier.getLexicalPhrase());
          }
          isImmutable = true;
          break;
        case MUTABLE:
          throw new LanguageParseException("Unexpected modifier: Methods cannot be mutable", modifier.getLexicalPhrase());
        case NATIVE:
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
          break;
        case SELFISH:
          throw new LanguageParseException("Unexpected modifier: Methods cannot be selfish", modifier.getLexicalPhrase());
        case SINCE:
          if (sinceSpecifier != null)
          {
            throw new LanguageParseException("Duplicate since(...) specifier", modifier.getLexicalPhrase());
          }
          sinceSpecifier = ((SinceModifier) modifier).getSinceSpecifier();
          break;
        case STATIC:
          if (isStatic)
          {
            throw new LanguageParseException("Duplicate 'static' modifier", modifier.getLexicalPhrase());
          }
          isStatic = true;
          break;
        case UNBACKED:
          throw new LanguageParseException("Unexpected modifier: Methods cannot be unbacked", modifier.getLexicalPhrase());
        default:
          throw new IllegalStateException("Unknown modifier: " + modifier);
        }
      }
    }
    // if we have any AutoAssignParameters, they implicitly add a block to the method if it didn't exist already
    if (block == null)
    {
      for (Parameter p : parameters)
      {
        if (p instanceof AutoAssignParameter)
        {
          block = new Block(new Statement[0], null);
          break;
        }
      }
    }
    return new Method(returnType, name, isAbstract, isStatic, isImmutable, nativeName, sinceSpecifier, parameters, checkedThrownTypes, uncheckedThrownTypes, block, lexicalPhrase);
  }

  private static NamedType[] extractCheckedThrownTypes(ParseList<ThrownExceptionType> list)
  {
    List<NamedType> thrownTypes = new LinkedList<NamedType>();
    for (ThrownExceptionType thrownExceptionType : list)
    {
      if (!thrownExceptionType.isUnchecked())
      {
        thrownTypes.add(thrownExceptionType.getType());
      }
    }
    return thrownTypes.toArray(new NamedType[thrownTypes.size()]);
  }

  private static NamedType[] extractUncheckedThrownTypes(ParseList<ThrownExceptionType> list)
  {
    List<NamedType> thrownTypes = new LinkedList<NamedType>();
    for (ThrownExceptionType thrownExceptionType : list)
    {
      if (thrownExceptionType.isUnchecked())
      {
        thrownTypes.add(thrownExceptionType.getType());
      }
    }
    return thrownTypes.toArray(new NamedType[thrownTypes.size()]);
  }
}
