package eu.bryants.anthony.plinth.parser.rules.member;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.expression.Expression;
import eu.bryants.anthony.plinth.ast.member.Property;
import eu.bryants.anthony.plinth.ast.misc.Parameter;
import eu.bryants.anthony.plinth.ast.statement.Block;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.terminal.SinceSpecifier;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.parser.LanguageParseException;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.Modifier;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;
import eu.bryants.anthony.plinth.parser.parseAST.PropertyMethod;
import eu.bryants.anthony.plinth.parser.parseAST.PropertyMethod.PropertyMethodType;
import eu.bryants.anthony.plinth.parser.parseAST.SinceModifier;

/*
 * Created on 22 Feb 2013
 */

/**
 * @author Anthony Bryant
 */
public class PropertyRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.PROPERTY_KEYWORD, ParseType.TYPE, ParseType.NAME, ParseType.PROPERTY_METHOD_LIST, ParseType.SEMICOLON);
  private static final Production<ParseType> INITIALISER_PRODUCTION = new Production<ParseType>(ParseType.OPTIONAL_MODIFIERS, ParseType.PROPERTY_KEYWORD, ParseType.TYPE, ParseType.NAME, ParseType.EQUALS, ParseType.EXPRESSION, ParseType.PROPERTY_METHOD_LIST, ParseType.SEMICOLON);

  public PropertyRule()
  {
    super(ParseType.PROPERTY, PRODUCTION, INITIALISER_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    if (production == PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type type = (Type) args[2];
      Name name = (Name) args[3];
      @SuppressWarnings("unchecked")
      ParseList<PropertyMethod> methodList = (ParseList<PropertyMethod>) args[4];
      return process(modifiers, type, name.getName(), null, methodList.toArray(new PropertyMethod[methodList.size()]),
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], type.getLexicalPhrase(), name.getLexicalPhrase(), methodList.getLexicalPhrase(), (LexicalPhrase) args[5]));
    }
    if (production == INITIALISER_PRODUCTION)
    {
      @SuppressWarnings("unchecked")
      ParseList<Modifier> modifiers = (ParseList<Modifier>) args[0];
      Type type = (Type) args[2];
      Name name = (Name) args[3];
      Expression initialiser = (Expression) args[5];
      @SuppressWarnings("unchecked")
      ParseList<PropertyMethod> methodList = (ParseList<PropertyMethod>) args[6];
      return process(modifiers, type, name.getName(), initialiser, methodList.toArray(new PropertyMethod[methodList.size()]),
                              LexicalPhrase.combine(modifiers.getLexicalPhrase(), (LexicalPhrase) args[1], type.getLexicalPhrase(), name.getLexicalPhrase(), (LexicalPhrase) args[4], initialiser.getLexicalPhrase(), methodList.getLexicalPhrase(), (LexicalPhrase) args[7]));
    }
    throw badTypeList();
  }

  private static Property process(ParseList<Modifier> modifiers, Type type, String name, Expression initialiser, PropertyMethod[] propertyMethods, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    boolean isAbstract = false;
    boolean isFinal = false;
    boolean isMutable = false;
    boolean isStatic = false;
    boolean isUnbacked = false;
    SinceSpecifier sinceSpecifier = null;
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
        if (isFinal)
        {
          throw new LanguageParseException("Duplicate 'final' modifier", modifier.getLexicalPhrase());
        }
        isFinal = true;
        break;
      case IMMUTABLE:
        throw new LanguageParseException("Unexpected modifier: Properties cannot be immutable", modifier.getLexicalPhrase());
      case MUTABLE:
        if (isMutable)
        {
          throw new LanguageParseException("Duplicate 'mutable' modifier", modifier.getLexicalPhrase());
        }
        isMutable = true;
        break;
      case NATIVE:
        throw new LanguageParseException("Unexpected modifier: Properties cannot be native", modifier.getLexicalPhrase());
      case SELFISH:
        throw new LanguageParseException("Unexpected modifier: Properties cannot be selfish", modifier.getLexicalPhrase());
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
        if (isUnbacked)
        {
          throw new LanguageParseException("Duplicate 'unbacked' modifier", modifier.getLexicalPhrase());
        }
        isUnbacked = true;
        break;
      default:
        throw new IllegalStateException("Unknown modifier: " + modifier);
      }
    }
    boolean haveGetter = false;
    boolean haveSetter = false;
    boolean haveConstructor = false;
    // getters default to immutable, setters default to mutable
    boolean isGetterImmutable = true;
    NamedType[] getterThrownTypes = null;
    Block getterBlock = null;

    boolean isSetterImmutable = false;
    Parameter setterParameter = null;
    NamedType[] setterThrownTypes = null;
    Block setterBlock = null;

    boolean isConstructorImmutable = false;
    Parameter constructorParameter = null;
    NamedType[] constructorThrownTypes = null;
    Block constructorBlock = null;

    if (propertyMethods != null)
    {
      for (PropertyMethod propertyMethod : propertyMethods)
      {
        if (propertyMethod.getType() == PropertyMethodType.GETTER)
        {
          if (haveGetter)
          {
            throw new LanguageParseException("A property cannot have two getters", propertyMethod.getLexicalPhrase());
          }
          isGetterImmutable = propertyMethod.isImmutable();
          getterThrownTypes = propertyMethod.getUncheckedThrownTypes();
          getterBlock = propertyMethod.getBlock();
          haveGetter = true;
        }
        else if (propertyMethod.getType() == PropertyMethodType.SETTER)
        {
          if (haveSetter)
          {
            throw new LanguageParseException("A property cannot have two setters", propertyMethod.getLexicalPhrase());
          }
          if (isFinal)
          {
            throw new LanguageParseException("A final property cannot have a setter", propertyMethod.getLexicalPhrase());
          }
          isSetterImmutable = propertyMethod.isImmutable();
          setterParameter = propertyMethod.getParameter();
          if (setterParameter == null)
          {
            setterParameter = new Parameter(propertyMethod.isParameterFinal(), type, propertyMethod.getParameterName(), propertyMethod.getParameterLexicalPhrase());
          }
          setterThrownTypes = propertyMethod.getUncheckedThrownTypes();
          setterBlock = propertyMethod.getBlock();
          haveSetter = true;
        }
        else if (propertyMethod.getType() == PropertyMethodType.CONSTRUCTOR)
        {
          if (haveConstructor)
          {
            throw new LanguageParseException("A property cannot have two constructors", propertyMethod.getLexicalPhrase());
          }
          isConstructorImmutable = propertyMethod.isImmutable();
          constructorParameter = propertyMethod.getParameter();
          if (constructorParameter == null)
          {
            constructorParameter = new Parameter(propertyMethod.isParameterFinal(), type, propertyMethod.getParameterName(), propertyMethod.getParameterLexicalPhrase());
          }
          constructorThrownTypes = propertyMethod.getUncheckedThrownTypes();
          constructorBlock = propertyMethod.getBlock();
          haveConstructor = true;
        }
      }
    }

    return new Property(isAbstract, isFinal, isMutable, isStatic, isUnbacked, sinceSpecifier, type, name, initialiser,
                        haveGetter, isGetterImmutable, getterThrownTypes, getterBlock,
                        haveSetter, isSetterImmutable, setterParameter, setterThrownTypes, setterBlock,
                        haveConstructor, isConstructorImmutable, constructorParameter, constructorThrownTypes, constructorBlock,
                        lexicalPhrase);
  }
}
