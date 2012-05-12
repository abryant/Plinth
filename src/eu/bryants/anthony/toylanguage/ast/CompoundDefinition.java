package eu.bryants.anthony.toylanguage.ast;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.member.Constructor;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Member;
import eu.bryants.anthony.toylanguage.ast.metadata.MemberVariable;
import eu.bryants.anthony.toylanguage.parser.LanguageParseException;
import eu.bryants.anthony.toylanguage.parser.LexicalPhrase;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class CompoundDefinition
{
  private String name;

  // fields needs a guaranteed order, so use a LinkedHashMap to store them
  private Map<String, Field> fields = new LinkedHashMap<String, Field>();
  private Set<Constructor> constructors = new HashSet<Constructor>();

  private LexicalPhrase lexicalPhrase;

  public CompoundDefinition(String name, Member[] members, LexicalPhrase lexicalPhrase) throws LanguageParseException
  {
    this.lexicalPhrase = lexicalPhrase;
    this.name = name;
    // add all of the members by name
    int fieldIndex = 0;
    for (Member member : members)
    {
      if (member instanceof Field)
      {
        Field field = (Field) member;
        field.setIndex(fieldIndex);
        field.setMemberVariable(new MemberVariable(field, this));
        fieldIndex++;
        fields.put(field.getName(), field);
      }
      if (member instanceof Constructor)
      {
        Constructor constructor = (Constructor) member;
        if (!constructor.getName().equals(name))
        {
          throw new LanguageParseException("The constructor '" + constructor.getName() + "' should be called '" + name + "' after the compound type it is defined in", constructor.getLexicalPhrase());
        }
        constructor.setContainingDefinition(this);
        constructors.add(constructor);
      }
      // TODO: when functions are added, make sure no names are duplicated between fields and functions
    }
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return the fields
   */
  public Field[] getFields()
  {
    Field[] fieldArray = new Field[fields.size()];
    Iterator<Field> it = fields.values().iterator();
    int i = 0;
    while (it.hasNext())
    {
      fieldArray[i] = it.next();
      i++;
    }
    return fieldArray;
  }

  /**
   * @param name - the name of the field to get
   * @return the Field with the specified name, or null if none exists
   */
  public Field getField(String name)
  {
    return fields.get(name);
  }

  /**
   * @return the constructors of this CompoundDefinition
   */
  public Collection<Constructor> getConstructors()
  {
    return constructors;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

  @Override
  public String toString()
  {
    StringBuffer buffer = new StringBuffer("compound ");
    buffer.append(name);
    buffer.append("\n{\n");
    for (Field field : fields.values())
    {
      buffer.append(field.toString().replaceAll("(?m)^", "  "));
      buffer.append("\n");
    }
    for (Constructor constructor : constructors)
    {
      buffer.append(constructor.toString().replaceAll("(?m)^", "  "));
      buffer.append("\n");
    }
    buffer.append("}\n");
    return buffer.toString();
  }
}
