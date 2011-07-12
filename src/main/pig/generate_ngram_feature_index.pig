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

/* Get all of the unigrams */
tokenized = FOREACH raw GENERATE doc_id,FLATTEN(com.mozilla.pig.eval.text.Tokenize(text, '$STOPWORDS', '$STEM')) AS token:chararray;
grouped_words = GROUP tokenized BY token;
word_freq = FOREACH grouped_words GENERATE FLATTEN($0) AS word:chararray, COUNT($1) as count;
/* filter on minDF = (count) > 10 AND maxDF % = (count/ndocs) < 0.9 */
filtered_freq = FILTER word_freq BY SIZE(word) > 1 AND count > 10 AND ((double)count / (double)ndocs.$0) < 0.9;
unigram_index = FOREACH filtered_freq GENERATE word;

/* Get all of the bi-grams */
bigram_tokenized = FOREACH raw GENERATE doc_id,FLATTEN(com.mozilla.pig.eval.text.NGramTokenize(text, '$STOPWORDS', '$STEM', 'false', '2', '2')) AS token:chararray;
bigram_grouped_words = GROUP bigram_tokenized BY token;
bigram_word_freq = FOREACH bigram_grouped_words GENERATE FLATTEN($0) AS ngram:chararray, COUNT($1) as count;
/* filter on minDF = (count) > 100 AND maxDF % = (count/ndocs) < 0.9 */
bigram_filtered_freq = FILTER bigram_word_freq BY SIZE(ngram) > 3 AND count > 100 AND ((double)count / (double)ndocs.$0) < 0.9;
bigram_index = FOREACH bigram_filtered_freq GENERATE ngram;

/* Get all of the tri-grams */
trigram_tokenized = FOREACH raw GENERATE doc_id,FLATTEN(com.mozilla.pig.eval.text.NGramTokenize(text, '$STOPWORDS', '$STEM', 'false', '3', '3')) AS token:chararray;
trigram_grouped_words = GROUP trigram_tokenized BY token;
trigram_word_freq = FOREACH trigram_grouped_words GENERATE FLATTEN($0) AS ngram:chararray, COUNT($1) as count;
/* filter on minDF = (count) > 100 AND maxDF % = (count/ndocs) < 0.9 */
trigram_filtered_freq = FILTER trigram_word_freq BY SIZE(ngram) > 5 AND count > 100 AND ((double)count / (double)ndocs.$0) < 0.9;
trigram_index = FOREACH trigram_filtered_freq GENERATE ngram;

/* Discard bigrams that are represented by trigrams */
bigrams_from_trigrams = FOREACH trigram_index GENERATE FLATTEN(com.mozilla.pig.eval.text.NGramTokenize(ngram, '$STOPWORDS', '$STEM', 'false', '2', '2')) AS token:chararray;
filtered_bigrams_from_trigrams = FILTER bigrams_from_trigrams BY SIZE(token) > 3;
uniq_bigrams_from_trigrams = DISTINCT filtered_bigrams_from_trigrams;

bigram_u = UNION uniq_bigrams_from_trigrams, bigram_index;
grouped_bigrams = GROUP bigram_u BY $0;
counted_bigrams = FOREACH grouped_bigrams GENERATE group, COUNT(bigram_u) AS count:long;
symm_diff_bigrams = FILTER counted_bigrams BY count == 1;
final_bigram_index = FOREACH symm_diff_bigrams GENERATE group AS ngram:chararray;

/* Discard unigrams that are represented by trigrams or bigrams */
bigrams_and_trigrams = UNION final_bigram_index, trigram_index;
unigrams_from_ngrams = FOREACH bigrams_and_trigrams GENERATE FLATTEN(com.mozilla.pig.eval.text.Tokenize(ngram, '$STOPWORDS', '$STEM')) AS word:chararray;
filtered_unigrams_from_ngrams = FILTER unigrams_from_ngrams BY SIZE(word) > 1;
uniq_unigrams_from_ngrams = DISTINCT filtered_unigrams_from_ngrams;

unigram_u = UNION unigram_index, uniq_unigrams_from_ngrams;
grouped_unigrams = GROUP unigram_u BY word;
counted_unigrams = FOREACH grouped_unigrams GENERATE group, COUNT(unigram_u) AS count:long;
symm_diff_unigrams = FILTER counted_unigrams BY count == 1;
final_unigram_index = FOREACH symm_diff_unigrams GENERATE group;

final_index = UNION final_unigram_index, final_bigram_index, trigram_index;

STORE final_index INTO '$OUTPUT';