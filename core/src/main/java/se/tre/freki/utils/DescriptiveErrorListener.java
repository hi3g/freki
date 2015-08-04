package se.tre.freki.utils;

import se.tre.freki.query.QueryException;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DescriptiveErrorListener extends BaseErrorListener {

  public static final DescriptiveErrorListener INSTANCE = new DescriptiveErrorListener();

  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                          int line, int charPositionInLine,
                          String msg, RecognitionException exception ) {

    String[] tokens = recognizer.getTokenNames();
    final List<String> tokensAsList = Arrays.asList(tokens);

    final Iterator<Integer> tokenids = exception.getExpectedTokens().toList().iterator();

    final StringBuilder expectedTokens = new StringBuilder();

    while (tokenids.hasNext()) {
      expectedTokens.append(tokensAsList.get(tokenids.next()));
      if (!tokenids.hasNext()) {
        expectedTokens.append(".");
        break;
      }
      expectedTokens.append(", ");
    }

    final String errorTokens =  expectedTokens.toString().replaceAll("'", "");

    throw new QueryException("Wrong input at " + "'" + exception.getOffendingToken().getText()
                             + "' at " + "Line " + line + " Char " + charPositionInLine
                             + ", expected one of the following tokens "  + errorTokens);
  }
}
