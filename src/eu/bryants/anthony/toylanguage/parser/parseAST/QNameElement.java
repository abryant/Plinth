package eu.bryants.anthony.toylanguage.parser.parseAST;

import eu.bryants.anthony.toylanguage.ast.LexicalPhrase;
import eu.bryants.anthony.toylanguage.ast.expression.BracketedExpression;
import eu.bryants.anthony.toylanguage.ast.expression.Expression;
import eu.bryants.anthony.toylanguage.ast.expression.FieldAccessExpression;
import eu.bryants.anthony.toylanguage.ast.expression.TupleExpression;
import eu.bryants.anthony.toylanguage.ast.expression.VariableExpression;
import eu.bryants.anthony.toylanguage.ast.misc.QName;
import eu.bryants.anthony.toylanguage.ast.type.NamedType;
import eu.bryants.anthony.toylanguage.ast.type.TupleType;
import eu.bryants.anthony.toylanguage.ast.type.Type;

/*
 * Created on 29 Sep 2012
 */

/**
 * Represents either a single QName, or a list of other QNameElements.
 * If it represents a single QName, then it is that QName on its own. e.g. "a.b.c"
 * If it represents a list of other QNameElements, then it is a comma separated list of QNameElements, enclosed in parentheses. e.g. "(a.b.c, d.e, f)"
 * This exists solely for parsing, and can be converted to either a Type or an Expression once the parser has determined which it represents.
 * @author Anthony Bryant
 */
public class QNameElement
{

  private QName qname;
  private ParseList<QNameElement> elements;

  private LexicalPhrase lexicalPhrase;

  /**
   * Creates a new QNameElement to represent the specified QName.
   * @param qname - the QName to represent
   * @param lexicalPhrase - the LexicalPhrase of the QName
   */
  public QNameElement(QName qname, LexicalPhrase lexicalPhrase)
  {
    this.qname = qname;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * Creates a new QNameElement to represent a bracketed list of other QNameElements
   * @param elements - the list of elements to represent
   * @param lexicalPhrase - the LexicalPhrase of the bracketed list of QNameElements
   */
  public QNameElement(ParseList<QNameElement> elements, LexicalPhrase lexicalPhrase)
  {
    this.elements = elements;
    this.lexicalPhrase = lexicalPhrase;
  }

  /**
   * @return the lexicalPhrase
   */
  public LexicalPhrase getLexicalPhrase()
  {
    return lexicalPhrase;
  }

  /**
   * Converts this QNameElement into a Type
   * @return a Type based on this QNameElement
   */
  public Type convertToType()
  {
    if (qname != null)
    {
      return new NamedType(false, qname, lexicalPhrase);
    }
    QNameElement[] elementArray = elements.toArray(new QNameElement[elements.size()]);
    Type[] types = new Type[elementArray.length];
    for (int i = 0; i < elementArray.length; ++i)
    {
      types[i] = elementArray[i].convertToType();
    }
    return new TupleType(false, types, lexicalPhrase);
  }

  /**
   * Converts this QNameElement into an Expression
   * @return an Expression based on this QNameElement
   */
  public Expression convertToExpression()
  {
    if (qname != null)
    {
      String[] names = qname.getNames();
      LexicalPhrase[] lexicalPhrases = qname.getLexicalPhrases();
      Expression current = new VariableExpression(names[0], lexicalPhrases[0]);
      for (int i = 1; i < names.length; ++i)
      {
        current = new FieldAccessExpression(current, false, names[i], LexicalPhrase.combine(current.getLexicalPhrase(), lexicalPhrases[i]));
      }
      return current;
    }

    QNameElement[] elementArray = elements.toArray(new QNameElement[elements.size()]);
    Expression[] expressions = new Expression[elementArray.length];
    for (int i = 0; i < elementArray.length; ++i)
    {
      expressions[i] = elementArray[i].convertToExpression();
    }
    TupleExpression tupleExpression = new TupleExpression(expressions, elements.getLexicalPhrase());
    return new BracketedExpression(tupleExpression, lexicalPhrase);
  }
}
