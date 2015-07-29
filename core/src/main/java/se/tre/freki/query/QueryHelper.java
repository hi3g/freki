package se.tre.freki.query;

import se.tre.freki.core.LabelClient;
import se.tre.freki.labels.LabelType;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class QueryHelper {
  final ANTLRInputStream input;
  final se.tre.freki.query.QueryStatementsLexer lexer;
  final CommonTokenStream tokens;
  final se.tre.freki.query.QueryStatementsParser parser;
  final ParseTree tree;
  final ParseTreeWalker treeWalker;

  /**
   * Takes a query as a string and tries to convert it to the java equivialent.
   * @param query
   */
  public QueryHelper(final String query, final LabelClient labelClient) {
    this.input = new ANTLRInputStream(query);
    this.lexer = new se.tre.freki.query.QueryStatementsLexer(input);
    this.tokens = new CommonTokenStream(lexer);
    this.parser = new se.tre.freki.query.QueryStatementsParser(tokens);

    this.tree = parser.query();
    this.treeWalker = new ParseTreeWalker();
    treeWalker.walk(new QueryStringTranslator(labelClient), tree);

  }
}
