package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.WildcardType;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.ParseUtil;
import eu.bryants.anthony.plinth.parser.parseAST.ParseContainer;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 23 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class WildcardTypeArgumentDoubleRAngleRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION               = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.DOUBLE_RANGLE);
  private static final Production<ParseType> EXTENDS_PRODUCTION       = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.EXTENDS_KEYWORD, ParseType.TYPE_BOUND_LIST_DOUBLE_RANGLE);
  private static final Production<ParseType> SUPER_PRODUCTION         = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.SUPER_KEYWORD,   ParseType.TYPE_BOUND_LIST_DOUBLE_RANGLE);
  private static final Production<ParseType> EXTENDS_SUPER_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.EXTENDS_KEYWORD, ParseType.TYPE_BOUND_LIST, ParseType.SUPER_KEYWORD,   ParseType.TYPE_BOUND_LIST_DOUBLE_RANGLE);
  private static final Production<ParseType> SUPER_EXTENDS_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.SUPER_KEYWORD,   ParseType.TYPE_BOUND_LIST, ParseType.EXTENDS_KEYWORD, ParseType.TYPE_BOUND_LIST_DOUBLE_RANGLE);

  public WildcardTypeArgumentDoubleRAngleRule()
  {
    super(ParseType.WILDCARD_TYPE_ARGUMENT_DOUBLE_RANGLE, PRODUCTION, EXTENDS_PRODUCTION, SUPER_PRODUCTION, EXTENDS_SUPER_PRODUCTION, SUPER_EXTENDS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    ParseList<Type> superTypeList = null;
    ParseList<Type> subTypeList = null;
    LexicalPhrase lexicalPhrase;
    LexicalPhrase rAngleLexicalPhrase;
    LexicalPhrase doubleRAngleLexicalPhrase;
    if (production == PRODUCTION)
    {
      lexicalPhrase = (LexicalPhrase) args[0];
      LexicalPhrase firstRAngleLexicalPhrase = ParseUtil.splitDoubleRAngle((LexicalPhrase) args[1]);
      rAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], firstRAngleLexicalPhrase);
      doubleRAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1]);
    }
    else if (production == EXTENDS_PRODUCTION)
    {
      ParseContainer<ParseContainer<ParseList<Type>>> containedContainedSuperTypeList = (ParseContainer<ParseContainer<ParseList<Type>>>) args[2];
      ParseContainer<ParseList<Type>> containedSuperTypeList = containedContainedSuperTypeList.getItem();
      superTypeList = containedSuperTypeList.getItem();
      lexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], superTypeList.getLexicalPhrase());
      rAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], containedSuperTypeList.getLexicalPhrase());
      doubleRAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], containedContainedSuperTypeList.getLexicalPhrase());
    }
    else if (production == SUPER_PRODUCTION)
    {
      ParseContainer<ParseContainer<ParseList<Type>>> containedContainedSubTypeList = (ParseContainer<ParseContainer<ParseList<Type>>>) args[2];
      ParseContainer<ParseList<Type>> containedSubTypeList = containedContainedSubTypeList.getItem();
      subTypeList = containedSubTypeList.getItem();
      lexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], subTypeList.getLexicalPhrase());
      rAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], containedSubTypeList.getLexicalPhrase());
      doubleRAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], containedContainedSubTypeList.getLexicalPhrase());
    }
    else if (production == EXTENDS_SUPER_PRODUCTION)
    {
      superTypeList = (ParseList<Type>) args[2];
      ParseContainer<ParseContainer<ParseList<Type>>> containedContainedSubTypeList = (ParseContainer<ParseContainer<ParseList<Type>>>) args[4];
      ParseContainer<ParseList<Type>> containedSubTypeList = containedContainedSubTypeList.getItem();
      subTypeList = containedSubTypeList.getItem();
      lexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], superTypeList.getLexicalPhrase(), (LexicalPhrase) args[3], subTypeList.getLexicalPhrase());
      rAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], superTypeList.getLexicalPhrase(), (LexicalPhrase) args[3], containedSubTypeList.getLexicalPhrase());
      doubleRAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], superTypeList.getLexicalPhrase(), (LexicalPhrase) args[3], containedContainedSubTypeList.getLexicalPhrase());
    }
    else if (production == SUPER_EXTENDS_PRODUCTION)
    {
      subTypeList = (ParseList<Type>) args[2];
      ParseContainer<ParseContainer<ParseList<Type>>> containedContainedSuperTypeList = (ParseContainer<ParseContainer<ParseList<Type>>>) args[4];
      ParseContainer<ParseList<Type>> containedSuperTypeList = containedContainedSuperTypeList.getItem();
      superTypeList = containedSuperTypeList.getItem();
      lexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], subTypeList.getLexicalPhrase(), (LexicalPhrase) args[3], superTypeList.getLexicalPhrase());
      rAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], subTypeList.getLexicalPhrase(), (LexicalPhrase) args[3], containedSuperTypeList.getLexicalPhrase());
      doubleRAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], subTypeList.getLexicalPhrase(), (LexicalPhrase) args[3], containedContainedSuperTypeList.getLexicalPhrase());
    }
    else
    {
      throw badTypeList();
    }
    Type[] superTypes = superTypeList == null ? new Type[0] : superTypeList.toArray(new Type[superTypeList.size()]);
    Type[] subTypes = subTypeList == null ? new Type[0] : subTypeList.toArray(new Type[subTypeList.size()]);
    WildcardType wildcardTypeArgument = new WildcardType(false, false, false, superTypes, subTypes, lexicalPhrase);
    ParseContainer<Type> containedWildcard = new ParseContainer<Type>(wildcardTypeArgument, rAngleLexicalPhrase);
    return new ParseContainer<ParseContainer<Type>>(containedWildcard, doubleRAngleLexicalPhrase);
  }

}
