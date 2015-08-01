package se.tre.freki.storage.cassandra;

import com.google.common.base.Charsets;

import java.nio.charset.Charset;

/**
 * Constants used by Cassandra.
 */
public class CassandraConst {
  /**
   * Charset used to convert strings from and to byte arrays within the Cassandra store.
   */
  static final Charset CHARSET = Charsets.UTF_8;
}
