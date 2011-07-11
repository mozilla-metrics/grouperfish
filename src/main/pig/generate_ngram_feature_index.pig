register './akela-0.1.jar'
register './lib/lucene-core-3.1.0.jar'
register './lib/lucene-analyzers-3.1.0.jar'

SET default_parallel 7;
SET pig.splitCombination 'false';

%default INPUT 'opinions.tsv'
%default STOPWORDS 'stopwords-en.txt'
%default STEM 'false'
%default FREQ_OUTPUT 'ngram-feature-freq'
%default OUTPUT 'ngram-feature-index'

raw = LOAD '$INPUT' USING PigStorage('\t') AS (doc_id:int,datetime:long,praise_issue:chararray,product:chararray,version:chararray,os:chararray,language:chararray,text:chararray);
grouped_raw = GROUP raw ALL;
ndocs = FOREACH grouped_raw GENERATE COUNT(raw);

tokenized = FOREACH raw GENERATE doc_id,FLATTEN(com.mozilla.pig.eval.text.Tokenize(text, '$STOPWORDS', '$STEM')) AS token:chararray;
grouped_words = GROUP tokenized BY token;
word_freq = FOREACH grouped_words GENERATE FLATTEN($0) AS word:chararray, COUNT($1) as count;
/* filter on minDF = (count) > 10 AND maxDF % = (count/ndocs) < 0.9 */
filtered_freq = FILTER word_freq BY SIZE(word) > 1 AND count > 10 AND ((double)count / (double)ndocs.$0) < 0.9;
index = FOREACH filtered_freq GENERATE word;

ngram_tokenized = FOREACH raw GENERATE doc_id,FLATTEN(com.mozilla.pig.eval.text.NGramTokenize(text, '$STOPWORDS', '$STEM', 'false')) AS token:chararray;
ngram_grouped_words = GROUP ngram_tokenized BY token;
ngram_word_freq = FOREACH ngram_grouped_words GENERATE FLATTEN($0) AS ngram:chararray, COUNT($1) as count;
/* filter on minDF = (count) > 100 AND maxDF % = (count/ndocs) < 0.9 */
ngram_filtered_freq = FILTER ngram_word_freq BY SIZE(ngram) > 3 AND count > 100 AND ((double)count / (double)ndocs.$0) < 0.9;
ngram_index = FOREACH ngram_filtered_freq GENERATE ngram;

unigrams_from_ngrams = FOREACH ngram_index GENERATE FLATTEN(com.mozilla.pig.eval.text.Tokenize(ngram, '$STOPWORDS', '$STEM')) AS word:chararray;
filterd_unigrams_from_ngrams = FILTER unigrams_from_ngrams BY SIZE(word) > 1;
uniq_unigrams_from_ngrams = DISTINCT filterd_unigrams_from_ngrams;

u = UNION index, uniq_unigrams_from_ngrams;
grouped_u = GROUP u BY word;
counted_u = FOREACH grouped_u GENERATE group, COUNT(u) AS count:long;
symm_diff = FILTER counted_u BY count == 1;
unigram_index = FOREACH symm_diff GENERATE group;

final_index = UNION unigram_index, ngram_index;

STORE ngram_filtered_freq INTO '$FREQ_OUTPUT';
STORE final_index INTO '$OUTPUT';