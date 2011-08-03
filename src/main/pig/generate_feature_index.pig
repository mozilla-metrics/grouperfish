register './akela-0.2-SNAPSHOT.jar'
register './grouperfish-0.3-SNAPSHOT.jar'
register './lib/lucene-core-3.1.0.jar'
register './lib/lucene-analyzers-3.1.0.jar'

SET default_parallel 7;

%default INPUT 'hbase://grouperfish'
%default STOPWORDS 'stopwords-en.txt'
%default STEM 'true'
%default FREQ_OUTPUT 'feature-freq'
%default OUTPUT 'feature-index'

raw = LOAD 'hbase://grouperfish' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('data:json') AS json:chararray;
genmap = FOREACH raw GENERATE com.mozilla.pig.eval.json.JsonMap(json) AS json_map:map[];

/* raw = LOAD '$INPUT' USING PigStorage('\t') AS (doc_id:int,datetime:long,praise_issue:chararray,product:chararray,version:chararray,os:chararray,locale:chararray,text:chararray); */

grouped_raw = GROUP raw ALL;
ndocs = FOREACH grouped_raw GENERATE COUNT(raw);

tokenized = FOREACH genmap GENERATE FLATTEN(com.mozilla.grouperfish.pig.eval.text.Tokenize(json_map#'text', '$STOPWORDS', '$STEM')) AS token:chararray;
grouped_words = GROUP tokenized BY token;
word_freq = FOREACH grouped_words GENERATE FLATTEN($0) AS word:chararray, COUNT($1) as count;
/* filter on minDF = (count) > 10 AND maxDF % = (count/ndocs) < 0.9 */
filtered_freq = FILTER word_freq BY SIZE(word) > 1 AND count > 10 AND ((double)count / (double)ndocs.$0) < 0.9;
index = FOREACH filtered_freq GENERATE word;

STORE filtered_freq INTO '$FREQ_OUTPUT';
STORE index INTO '$OUTPUT';