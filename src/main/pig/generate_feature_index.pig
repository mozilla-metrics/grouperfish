/* Not sure why we have to register this JAR when it's already in Pig's classpath but we do */
register '/usr/lib/hbase/hbase-0.90.1-cdh3u0.jar'
register './lib/akela-0.1.jar'
register './lib/lucene-core-3.1.0.jar'
register './lib/lucene-analyzers-3.1.0.jar'

/* Fixed in HBaseStorage Pig 0.8.1 which we aren't running yet */
SET pig.splitCombination 'false';

raw = LOAD 'hbase://grundle' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('data:json') AS json:chararray;
genmap = FOREACH raw GENERATE com.mozilla.pig.eval.json.JsonMap(json) AS json_map:map[];

/* New approach using Lucene Analyzers */
raw = LOAD 'opinions-en.tsv' USING PigStorage('\t') AS (docid:int,datetime:long,praise_issue:chararray,product:chararray,version:chararray,os:chararray,language:chararray,text:chararray);
tokenized = FOREACH raw GENERATE doc_id,FLATTEN(com.mozilla.pig.eval.text.Tokenize(text)) AS token:chararray;
grouped_words = GROUP tokenized BY word;
word_freq = FOREACH grouped_words GENERATE FLATTEN($0) AS word:chararray, COUNT($1) as count;
filtered_freq = FILTER word_freq BY SIZE(word) > 1 AND count > 10;
STORE filtered_freq INTO 'feature-freq';
index = FOREACH filtered_freq GENERATE word;
STORE index INTO 'feature-index';