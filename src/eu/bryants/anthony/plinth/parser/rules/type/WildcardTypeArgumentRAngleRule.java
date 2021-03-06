package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.WildcardType;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseContainer;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 23 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class WildcardTypeArgumentRAngleRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION               = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.RANGLE);
  private static final Production<ParseType> EXTENDS_PRODUCTION       = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.EXTENDS_KEYWORD, ParseType.TYPE_BOUND_LIST_RANGLE);
  private static final Production<ParseType> SUPER_PRODUCTION         = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.SUPER_KEYWORD,   ParseType.TYPE_BOUND_LIST_RANGLE);
  private static final Production<ParseType> EXTENDS_SUPER_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.EXTENDS_KEYWORD, ParseType.TYPE_BOUND_LIST, ParseType.SUPER_KEYWORD,   ParseType.TYPE_BOUND_LIST_RANGLE);
  private static final Production<ParseType> SUPER_EXTENDS_PRODUCTION = new Production<ParseType>(ParseType.QUESTION_MARK, ParseType.SUPER_KEYWORD,   ParseType.TYPE_BOUND_LIST, ParseType.EXTENDS_KEYWORD, ParseType.TYPE_BOUND_LIST_RANGLE);

  public WildcardTypeArgumentRAngleRule()
  {
    super(ParseType.WILDCARD_TYPE_ARGUMENT_RANGLE, PRODUCTION, EXTENDS_PRODUCTION, SUPER_PRODUCTION, EXTENDS_SUPER_PRODUCTION, SUPER_EXTENDS_PRODUCTION);
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
    if (production == PRODUCTION)
    {
      lexicalPhrase = (LexicalPhrase) args[0];
      rAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1]);
    }
    else if (production == EXTENDS_PRODUCTION)
    {
      ParseContainer<ParseList<Type>> containedSuperTypeList = (ParseContainer<ParseList<Type>>) args[2];
      superTypeList = containedSuperTypeList.getItem();
      lexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], superTypeList.getLexicalPhrase());
      rAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], containedSuperTypeList.getLexicalPhrase());
    }
    else if (production == SUPER_PRODUCTION)
    {
      ParseContainer<ParseList<Type>> containedSubTypeList = (ParseContainer<ParseList<Type>>) args[2];
      subTypeList = containedSubTypeList.getItem();
      lexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], subTypeList.getLexicalPhrase());
      rAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], containedSubTypeList.getLexicalPhrase());
    }
    else if (production == EXTENDS_SUPER_PRODUCTION)
    {
      superTypeList = (ParseList<Type>) args[2];
      ParseContainer<ParseList<Type>> containedSubTypeList = (ParseContainer<ParseList<Type>>) args[4];
      subTypeList = containedSubTypeList.getItem();
      lexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], superTypeList.getLexicalPhrase(), (LexicalPhrase) args[3], subTypeList.getLexicalPhrase());
      rAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], superTypeList.getLexicalPhrase(), (LexicalPhrase) args[3], containedSubTypeList.getLexicalPhrase());
    }
    else if (production == SUPER_EXTENDS_PRODUCTION)
    {
      subTypeList = (ParseList<Type>) args[2];
      ParseContainer<ParseList<Type>> containedSuperTypeList = (ParseContainer<ParseList<Type>>) args[4];
      superTypeList = containedSuperTypeList.getItem();
      lexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], subTypeList.getLexicalPhrase(), (LexicalPhrase) args[3], superTypeList.getLexicalPhrase());
      rAngleLexicalPhrase = LexicalPhrase.combine((LexicalPhrase) args[0], (LexicalPhrase) args[1], subTypeList.getLexicalPhrase(), (LexicalPhrase) args[3], containedSuperTypeList.getLexicalPhrase());
    }
    else
    {
      throw badTypeList();
    }
    Type[] superTypes = superTypeList == null ? new Type[0] : superTypeList.toArray(new Type[superTypeList.size()]);
    Type[] subTypes = subTypeList == null ? new Type[0] : subTypeList.toArray(new Type[subTypeList.size()]);
    WildcardType wildcardTypeArgument = new WildcardType(false, false, false, superTypes, subTypes, lexicalPhrase);
    return new ParseContainer<Type>(wildcardTypeArgument, rAngleLexicalPhrase);
  }

}
