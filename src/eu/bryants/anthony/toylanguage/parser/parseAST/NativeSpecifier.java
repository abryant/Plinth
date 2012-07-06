package eu.bryants.anthony.toylanguage.parser.parseAST;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;

/*
 * Created on 6 Jul 2012
 */

/**
 * @author Anthony Bryant
 */
public class NativeSpecifier extends Modifier
{

  private String nativeName;

  /**
   * Creates a NativeSpecifier with the specified native name and LexicalPhrase
   * @param nativeName - the native name, after any string literal escape sequences have been processed
   * @param lexicalPhrase - the lexical phrase of the native specifier
   */
  public NativeSpecifier(String nativeName, LexicalPhrase lexicalPhrase)
  {
    super(ModifierType.NATIVE, lexicalPhrase);
    this.nativeName = nativeName;
  }

  /**
   * @return the nativeName
   */
  public String getNativeName()
  {
    return nativeName;
  }

}
