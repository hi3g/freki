package se.tre.freki.query;

public class QueryException extends RuntimeException {
  public QueryException(final String message) {
    super(message);
  }

  public QueryException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
