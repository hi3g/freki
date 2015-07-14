package se.tre.freki.storage.cassandra;

import com.google.common.base.Charsets;

import java.nio.charset.Charset;

/**
 * Constants used by Cassandra.
 */
public class CassandraConst {
  /**
   * The default Cassandra Port used by the cassandra by default if the port was not specified in
   * the configuration file.
   */
  public static final int DEFAULT_CASSANDRA_PORT = 9042;

  /**
   * Charset used to convert strings from and to byte arrays within the Cassandra store.
   */
  static final Charset CHARSET = Charsets.UTF_8;

  /** Max time delta (in milliseconds) we can store in a column qualifier. */
  public static final int BASE_TIME_PERIOD = 3600000;

}
