package se.tre.freki.query;

import se.tre.freki.core.LabelClient;

public class QueryStringTranslator extends se.tre.freki.query.SelectParserBaseListener {
  private final LabelClient labelClient;

  public QueryStringTranslator(final LabelClient labelClient) {
    this.labelClient = labelClient;
  }
}
