
// Pipe me into cqlsh to let me add this for you. Example: cat tables.cql | cqlsh

DROP KEYSPACE IF EXISTS "freki";

// We use SimpleStrategy and a low replication factor here during development
// but for a production deployment you probably want to change these to fit
// your requirements.
CREATE KEYSPACE freki WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

//
// Identifier tables
//
// Maps (identifier, type) into a name. We use a timestamp that indicates when
// the id was created to resolve conflicts. The timestamp should be the same
// as in the table name_to_id. The numeric ids are useful to avoid ID collisions
// for timeseries_ids (timeseries_ids are a hash of the the label_ids in this
// table). The type column is one of metric, tagk or tagv.
CREATE TABLE freki.id_to_name (
  label_id bigint,
  type varchar,
  creation_time timestamp,
  name varchar,
  PRIMARY KEY ((label_id, type), creation_time)
) WITH CLUSTERING ORDER BY (creation_time ASC);

// Maps (name, type) into a label_id. We use a timestamp that indicates when
// the id was created to resolve conflicts. The timestamp should be the same
// as in the table name_to_id.
CREATE TABLE freki.name_to_id (
  name varchar,
  type varchar,
  creation_time timestamp,
  label_id bigint,
  PRIMARY KEY ((name, type), creation_time)
) WITH CLUSTERING ORDER BY (creation_time ASC);
//
// End identifier tables
//

// An index for resolving which timeseries that uses a label_id.
CREATE TABLE freki.ts_inverted_index (
  label_id bigint,
  type varchar,
  timeseries_id uuid,
  metric bigint,
  tags map<bigint, bigint>,
  PRIMARY KEY ((label_id, type), timeseries_id)
);

// An index for resolving which label_ids a timeseries is composed of. The
// metric and tags are label_ids.
CREATE TABLE freki.ts_index (
  timeseries_id uuid,
  metric bigint,
  tags map<bigint, bigint>,
  PRIMARY KEY (timeseries_id)
);

// The table that contains all data points.
CREATE TABLE freki.datapoints (
  timeseries_id uuid,
  basetime bigint,
  timestamp bigint,
  float_value float,
  double_value double,
  long_value bigint,
  PRIMARY KEY ((timeseries_id, basetime), timestamp)
);

// The table that contains labels n-grams mapping
CREATE TABLE freki.label_search_index (
  ngram varchar,
  type varchar,
  label_id bigint,
  PRIMARY KEY (ngram, type, label_id)
);

// The meta table, here we store the meta to the labels.
CREATE TABLE freki.label_meta (
  label_id bigint,
  type varchar,
  creation_time timestamp,
  description varchar,
  PRIMARY KEY (label_id, type)
);
