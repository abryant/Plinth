package eu.bryants.anthony.plinth.parser.rules.type;

import parser.ParseException;
import parser.Production;
import parser.Rule;
import eu.bryants.anthony.plinth.ast.LexicalPhrase;
import eu.bryants.anthony.plinth.ast.terminal.Name;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.ast.type.TypeParameter;
import eu.bryants.anthony.plinth.parser.ParseType;
import eu.bryants.anthony.plinth.parser.parseAST.ParseList;

/*
 * Created on 24 Mar 2013
 */

/**
 * @author Anthony Bryant
 */
public class TypeParameterRule extends Rule<ParseType>
{
  private static final long serialVersionUID = 1L;

  private static final Production<ParseType> PRODUCTION               = new Production<ParseType>(ParseType.NAME);
  private static final Production<ParseType> EXTENDS_PRODUCTION       = new Production<ParseType>(ParseType.NAME, ParseType.EXTENDS_KEYWORD, ParseType.TYPE_BOUND_LIST);
  private static final Production<ParseType> SUPER_PRODUCTION         = new Production<ParseType>(ParseType.NAME, ParseType.SUPER_KEYWORD,   ParseType.TYPE_BOUND_LIST);
  private static final Production<ParseType> EXTENDS_SUPER_PRODUCTION = new Production<ParseType>(ParseType.NAME, ParseType.EXTENDS_KEYWORD, ParseType.TYPE_BOUND_LIST, ParseType.SUPER_KEYWORD,   ParseType.TYPE_BOUND_LIST);
  private static final Production<ParseType> SUPER_EXTENDS_PRODUCTION = new Production<ParseType>(ParseType.NAME, ParseType.SUPER_KEYWORD,   ParseType.TYPE_BOUND_LIST, ParseType.EXTENDS_KEYWORD, ParseType.TYPE_BOUND_LIST);

  public TypeParameterRule()
  {
    super(ParseType.TYPE_PARAMETER, PRODUCTION, EXTENDS_PRODUCTION, SUPER_PRODUCTION, EXTENDS_SUPER_PRODUCTION, SUPER_EXTENDS_PRODUCTION);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Object match(Production<ParseType> production, Object[] args) throws ParseException
  {
    Name name;
    ParseList<Type> superTypeList = null;
    ParseList<Type> subTypeList = null;
    LexicalPhrase lexicalPhrase;

    if (production == PRODUCTION)
    {
      name = (Name) args[0];
      lexicalPhrase = name.getLexicalPhrase();
    }
    else if (production == EXTENDS_PRODUCTION)
    {
      name = (Name) args[0];
      superTypeList = (ParseList<Type>) args[2];
      lexicalPhrase = LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], superTypeList.getLexicalPhrase());
    }
    else if (production == SUPER_PRODUCTION)
    {
      name = (Name) args[0];
      subTypeList = (ParseList<Type>) args[2];
      lexicalPhrase = LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], subTypeList.getLexicalPhrase());
    }
    else if (production == EXTENDS_SUPER_PRODUCTION)
    {
      name = (Name) args[0];
      superTypeList = (ParseList<Type>) args[2];
      subTypeList = (ParseList<Type>) args[4];
      lexicalPhrase = LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], superTypeList.getLexicalPhrase(), (LexicalPhrase) args[3], subTypeList.getLexicalPhrase());
    }
    else if (production == SUPER_EXTENDS_PRODUCTION)
    {
      name = (Name) args[0];
      subTypeList = (ParseList<Type>) args[2];
      superTypeList = (ParseList<Type>) args[4];
      lexicalPhrase = LexicalPhrase.combine(name.getLexicalPhrase(), (LexicalPhrase) args[1], subTypeList.getLexicalPhrase(), (LexicalPhrase) args[3], superTypeList.getLexicalPhrase());
    }
    else
    {
      throw badTypeList();
    }
    Type[] superTypes = superTypeList == null ? new Type[0] : superTypeList.toArray(new Type[superTypeList.size()]);
    Type[] subTypes = subTypeList == null ? new Type[0] : subTypeList.toArray(new Type[subTypeList.size()]);
    return new TypeParameter(name.getName(), superTypes, subTypes, lexicalPhrase);
  }

}
