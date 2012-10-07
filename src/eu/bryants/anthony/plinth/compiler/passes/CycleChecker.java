package eu.bryants.anthony.plinth.compiler.passes;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import eu.bryants.anthony.plinth.ast.CompilationUnit;
import eu.bryants.anthony.plinth.ast.CompoundDefinition;
import eu.bryants.anthony.plinth.ast.TypeDefinition;
import eu.bryants.anthony.plinth.ast.member.Field;
import eu.bryants.anthony.plinth.ast.type.NamedType;
import eu.bryants.anthony.plinth.ast.type.Type;
import eu.bryants.anthony.plinth.compiler.ConceptualException;

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
    for (TypeDefinition typeDefinition : compilationUnit.getTypeDefinitions())
    {
      if (!(typeDefinition instanceof CompoundDefinition))
      {
        continue;
      }
      CompoundDefinition compoundDefinition = (CompoundDefinition) typeDefinition;
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
      NamedType type = (NamedType) fieldStack.get(fieldStack.size() - 1).getType();
      current = (CompoundDefinition) type.getResolvedTypeDefinition();
    }
    for (Field field : current.getNonStaticFields())
    {
      Type type = field.getType();
      if (type instanceof NamedType)
      {
        TypeDefinition resolvedDefinition = ((NamedType) type).getResolvedTypeDefinition();
        if (!(resolvedDefinition instanceof CompoundDefinition))
        {
          // skip any NamedType which isn't for a CompoundDefinition
          continue;
        }
        CompoundDefinition compoundDefinition = (CompoundDefinition) resolvedDefinition;
        if (visited.contains(compoundDefinition))
        {
          StringBuffer buffer = new StringBuffer();
          for (Field f : fieldStack)
          {
            buffer.append(f.getName());
            buffer.append(".");
          }
          buffer.append(field.getName());
          throw new ConceptualException("Cycle detected in compound type '" + startDefinition.getQualifiedName() + "' at: " + buffer, field.getLexicalPhrase());
        }
        fieldStack.add(field);
        visited.add(compoundDefinition);
        checkCycles(startDefinition, fieldStack, visited);
        visited.remove(compoundDefinition);
        fieldStack.remove(fieldStack.size() - 1);
      }
    }
  }
}
