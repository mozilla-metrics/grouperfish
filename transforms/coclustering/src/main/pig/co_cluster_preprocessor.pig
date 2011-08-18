--
-- Script to perform Preprocessing for Co-Clustering.
--
-- See "Co-clustering documents and words using Bipartite Spectral Graph
-- Partitioning" by Dhillon for more details.
--

%default INPUT 'opinions.tsv'
%default TEMP 'cct'
%default STOPWORDS 'stopwords-en.txt'
%default STEM 'true'
%default MIN_WORD_LEN 2
%default MIN_DF 1
%default MAX_DF_PERCENT 0.99
%default NUM_REDUCERS 7
register './lib/grouperfish-transforms-coclustering-0.3-SNAPSHOT.jar'
register './lib/akela-0.2-SNAPSHOT.jar'
register './lib/lucene-core-3.1.0.jar'
register './lib/lucene-analyzers-3.1.0.jar'
SET default_parallel $NUM_REDUCERS
SET pig.splitCombination 'false';

DEFINE generate_pig_map com.mozilla.pig.eval.json.JsonMap();
DEFINE tokenize com.mozilla.grouperfish.transforms.coclustering.pig.eval.text.Tokenize();
DEFINE generate_tf_bag com.mozilla.grouperfish.transforms.coclustering.pig.eval.text.TermFrequency();
--
-- Preprocessing Documents to generate TF Vectors
--
-- Load Docs
docs = LOAD '$INPUT' USING PigStorage('\t') AS (doc_id:chararray,
								json:chararray);
genmap = FOREACH docs GENERATE generate_pig_map(json) AS json_map:map[];
tokenized = FOREACH genmap
		GENERATE (chararray) json_map#'$DOC_COL_ID' AS doc_id:chararray,
			tokenize(json_map#'$TEXT_COL_ID', '$STOPWORDS', '$STEM')
						    AS token_bag;

/*
docs = LOAD '$INPUT' USING PigStorage('\t') AS (doc_id:chararray, datetime:long,
	praise_issue:chararray, product:chararray, version:chararray,
	os:chararray,locale:chararray,text:chararray);
tokenized = FOREACH docs
		GENERATE doc_id,
			 tokenize(text, '$STOPWORDS', '$STEM') AS token_bag;
*/
/* Comment out the line above and uncomment the line below if you are
    using an ngram feature-index */
/* tokenized_pre = FOREACH raw GENERATE doc_id,com.
    mozilla.pig.eval.text.NGramTokenize(text,'$STOPWORDS', '$STEM', 'true')
AS token_bag;
*/
-- Using minDF, maxDF conditions to filter out features.
grouped_docs = GROUP tokenized ALL;
ndocs = FOREACH grouped_docs
	    GENERATE COUNT(tokenized);
flat_tokenized = FOREACH tokenized
		    GENERATE doc_id,
		    	    FLATTEN(token_bag) AS word:chararray;
grouped_words = GROUP flat_tokenized BY word;
word_freq = FOREACH grouped_words
		GENERATE $0 AS word:chararray,
		    COUNT($1) AS count;
feature_freq = FILTER word_freq BY
			SIZE(word) >= $MIN_WORD_LEN  AND
			count > $MIN_DF AND
			((double)count / (double)ndocs.$0) < $MAX_DF_PERCENT;
features = FOREACH feature_freq
		GENERATE word;
grouped_features = GROUP feature_freq ALL;
nfeatures = FOREACH grouped_features
		    GENERATE COUNT(feature_freq);
STORE features INTO '$TEMP/feature_index';
STORE nfeatures INTO '$TEMP/nfeatures';
-- Remove all features that are irrelevant.
filtered_tokenized_features = JOIN features BY word, flat_tokenized BY word;
original_doc_ids = FOREACH filtered_tokenized_features
		    GENERATE flat_tokenized::doc_id;
doc_ids = DISTINCT original_doc_ids;
grouped_doc_ids = GROUP doc_ids ALL;
n_final_docs = FOREACH grouped_doc_ids
		    GENERATE COUNT(doc_ids);
STORE n_final_docs INTO '$TEMP/ndocs';
STORE doc_ids INTO '$TEMP/doc_index';
-- Store adjacency matrix.
doc_words = GROUP filtered_tokenized_features BY doc_id;
doc_words_cleaned = FOREACH doc_words
			GENERATE group AS doc_id,
				$1.features::word AS feature_bag;
A = FOREACH doc_words_cleaned
		GENERATE doc_id,
			 generate_tf_bag(feature_bag)
			    AS tf_bag:{t:(feature:chararray, tf:double)};
STORE A INTO '$TEMP/A';
