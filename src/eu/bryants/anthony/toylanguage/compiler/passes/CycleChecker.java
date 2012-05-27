package eu.bryants.anthony.toylanguage.compiler.passes;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import eu.bryants.anthony.toylanguage.ast.CompilationUnit;
import eu.bryants.anthony.toylanguage.ast.CompoundDefinition;
import eu.bryants.anthony.toylanguage.ast.member.Field;
import eu.bryants.anthony.toylanguage.ast.type.NamedType;
import eu.bryants.anthony.toylanguage.ast.type.Type;
import eu.bryants.anthony.toylanguage.compiler.ConceptualException;

/*
 * Created on 9 May 2012
 */

/**
 * @author Anthony Bryant
 */
public class CycleChecker
{

  /**
   * Checks for any impossible cycles in a specified compilation unit.
   * These include any cycles starting at objects in the specified compilation unit, such as
   * compound types having fields that (perhaps indirectly) contain another variable of their own type (resulting in an infinitely deep structure type)
   * @param compilationUnit - the CompilationUnit to check
   * @throws ConceptualException - if a cycle is found
   */
  public static void checkCycles(CompilationUnit compilationUnit) throws ConceptualException
  {
    for (CompoundDefinition compoundDefinition : compilationUnit.getCompoundDefinitions())
    {
      Set<CompoundDefinition> visited = new HashSet<CompoundDefinition>();
      visited.add(compoundDefinition);
      checkCycles(compoundDefinition, new LinkedList<Field>(), visited);
    }
  }

  private static void checkCycles(CompoundDefinition startDefinition, List<Field> fieldStack, Set<CompoundDefinition> visited) throws ConceptualException
  {
    CompoundDefinition current = startDefinition;
    if (!fieldStack.isEmpty())
    {
      current = ((NamedType) fieldStack.get(fieldStack.size() - 1).getType()).getResolvedDefinition();
    }
    for (Field field : current.getFields())
    {
      Type type = field.getType();
      if (type instanceof NamedType)
      {
        CompoundDefinition resolvedDefinition = ((NamedType) type).getResolvedDefinition();
        if (visited.contains(resolvedDefinition))
        {
          StringBuffer buffer = new StringBuffer();
          for (Field f : fieldStack)
          {
            buffer.append(f.getName());
            buffer.append(".");
          }
          buffer.append(field.getName());
          throw new ConceptualException("Cycle detected in compound type '" + startDefinition.getName() + "' at: " + buffer, field.getLexicalPhrase());
        }
        fieldStack.add(field);
        visited.add(resolvedDefinition);
        checkCycles(startDefinition, fieldStack, visited);
        visited.remove(resolvedDefinition);
        fieldStack.remove(fieldStack.size() - 1);
      }
    }
  }
}
