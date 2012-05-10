package eu.bryants.anthony.toylanguage.ast;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.member.Member;
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

  private LexicalPhrase lexicalPhrase;

  public CompoundDefinition(String name, Member[] members, LexicalPhrase lexicalPhrase)
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
        fieldIndex++;
        fields.put(field.getName(), field);
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

  public Field getField(String name)
  {
    return fields.get(name);
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
    buffer.append("}\n");
    return buffer.toString();
  }
}
