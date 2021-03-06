package se.tre.freki.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.ConfigValue;

/**
 * Exception thrown when something about a specific {@link com.typesafe.config.ConfigValue} is
 * wrong.
 */
public class InvalidConfigException extends RuntimeException {
  public InvalidConfigException(final ConfigValue value,
                                final String message) {
    super(value.origin().description() + ": " + checkNotNull(message));
  }

  public InvalidConfigException(final ConfigValue value,
                                final String message,
                                final Throwable cause) {
    super(value.origin().description() + ": " + checkNotNull(message), cause);
  }
}
