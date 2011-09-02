register './akela-0.2-SNAPSHOT.jar'
register './grouperfish-0.3-SNAPSHOT.jar'
register './lib/lucene-core-3.1.0.jar'
register './lib/lucene-analyzers-3.1.0.jar'

SET default_parallel 7;

%default INPUT 'input.json.tsv'
%default STOPWORDS 'stopwords-en.txt'
%default STEM 'false'
%default FREQ_OUTPUT 'feature-freq'
%default OUTPUT 'feature-index'
%default MIN_WORD_LENGTH 3
%default MIN_DF 2
%default MAX_DF_PERCENTAGE 0.9

/*raw = LOAD 'hbase://grouperfish' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('data:json') AS json:chararray;*/
raw = LOAD '$INPUT' USING PigStorage('\t') AS (doc_id:int,json:chararray);
genmap = FOREACH raw GENERATE com.mozilla.pig.eval.json.JsonMap(json) AS json_map:map[];

grouped_raw = GROUP raw ALL;
ndocs = FOREACH grouped_raw GENERATE COUNT(raw);

tokenized = FOREACH genmap GENERATE FLATTEN(com.mozilla.grouperfish.pig.eval.text.Tokenize(json_map#'text', '$STOPWORDS', '$STEM')) AS token:chararray;
grouped_words = GROUP tokenized BY token;
word_freq = FOREACH grouped_words GENERATE FLATTEN($0) AS word:chararray, COUNT($1) as count;
/* filter on minDF = (count) > 10 AND maxDF % = (count/ndocs) < 0.9 */
filtered_freq = FILTER word_freq BY SIZE(word) > $MIN_WORD_LENGTH AND count > $MIN_DF AND ((double)count / (double)ndocs.$0) < $MAX_DF_PERCENTAGE;
index = FOREACH filtered_freq GENERATE word;

STORE filtered_freq INTO '$FREQ_OUTPUT';
STORE index INTO '$OUTPUT';