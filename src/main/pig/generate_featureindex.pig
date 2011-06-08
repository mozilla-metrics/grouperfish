register './akela-0.1.jar'                                                                                                               
/* Not sure why we have to register this JAR when it's already in Pig's classpath but we do */
register '/usr/lib/hbase/hbase-0.90.1-cdh3u0.jar'

/* Fixed in HBaseStorage Pig 0.8.1 which we aren't running yet */
SET pig.splitCombination 'false';

raw = LOAD 'hbase://grundle' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('data:json') AS json:chararray;
genmap = FOREACH raw GENERATE com.mozilla.pig.eval.json.JsonMap(json) AS json_map:map[];
words = FOREACH genmap GENERATE FLATTEN(com.mozilla.pig.eval.text.UnigramExtractor(json_map#'text')) AS word:chararray;
filtered_words = FILTER words BY NOT com.mozilla.pig.filter.IsStopword(word);                                                                               
index = DISTINCT filtered_words;

STORE index INTO 'feature-index';

/* Same as above except using tsv file for experimenting */
raw = LOAD 'opinions-en.tsv' USING PigStorage('\t') AS (docid:int,datetime:long,praise_issue:chararray,product:chararray,version:chararray,os:chararray,language:chararray,text:chararray);
words = FOREACH raw GENERATE FLATTEN(com.mozilla.pig.eval.text.UnigramExtractor(text)) AS word:chararray;
filtered_words = FILTER words BY NOT com.mozilla.pig.filter.IsStopword(word);                                                                               
grouped_words = GROUP filtered_words BY word;
feature_freq = FOREACH grouped_words GENERATE FLATTEN($0) AS word:chararray, COUNT($1) as count;
filtered_freq = FILTER feature_freq BY count > 2;
STORE filtered_freq INTO 'feature-freq';
index = FOREACH filtered_freq GENERATE word;
STORE index INTO 'feature-index';