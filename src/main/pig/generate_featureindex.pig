register './akela-0.1.jar'                                                                                                               
/* Not sure why we have to register this JAR when it's already in Pig's classpath but we do */
register '/usr/lib/hbase/hbase-0.90.1-cdh3u0.jar'

raw = LOAD 'hbase://grouperfish' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('data:json') AS json:chararray;
genmap = FOREACH raw GENERATE com.mozilla.pig.eval.json.JsonMap(json) AS json_map:map[];
words = FOREACH genmap GENERATE FLATTEN(com.mozilla.pig.eval.text.Normalize(json_map#'text')) AS word_tuple;
flat_words = FOREACH words GENERATE FLATTEN(TOBAG(*));
index = DISTINCT flat_words;

STORE index INTO 'feature-index';