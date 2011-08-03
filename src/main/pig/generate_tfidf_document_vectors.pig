register './akela-0.1.jar'
register './lib/lucene-core-3.1.0.jar'
register './lib/lucene-analyzers-3.1.0.jar'
register './lib/mahout-core-0.5.jar'
register './lib/mahout-math-0.5.jar'
register './lib/mahout-utils-0.5.jar'
register './lib/mahout-collections-1.0.jar'

SET default_parallel 7;

%default INPUT 'hbase://grouperfish'
%default STOPWORDS 'stopwords-en.txt'
%default STEM 'true'
%default FEATUREINDEX 'feature-index'
%default OUTPUT 'document-vectors-tfidf'

raw = LOAD 'hbase://grouperfish' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('data:json') AS json:chararray;
genmap = FOREACH raw GENERATE com.mozilla.pig.eval.json.JsonMap(json) AS json_map:map[];
/*raw = LOAD '$INPUT' USING PigStorage('\t') AS (doc_id:int,datetime:long,praise_issue:chararray,product:chararray,version:chararray,os:chararray,locale:chararray,text:chararray);*/

grouped_raw = GROUP raw all;
ndocs = FOREACH grouped_raw GENERATE COUNT(raw);
tokenized = FOREACH genmap GENERATE doc_id,FLATTEN(com.mozilla.grouperfish.pig.eval.text.Tokenize(json_map#'text','$STOPWORDS', '$STEM')) AS token:chararray;
/* Comment out the line above and uncomment the line below if you are using an ngram feature-index */
/*tokenized = FOREACH filtered_raw GENERATE doc_id,FLATTEN(com.mozilla.pig.eval.text.NGramTokenize(text,'$STOPWORDS', '$STEM', 'true')) AS token:chararray;*/

/* Pulled from @datachef's blog at http://thedatachef.blogspot.com to give proper credit */
doc_tokens       = GROUP tokenized BY (doc_id, token);
doc_token_counts = FOREACH doc_tokens GENERATE FLATTEN(group) AS (doc_id, token), COUNT(tokenized) AS num_doc_tok_usages;

doc_usage_bag    = GROUP doc_token_counts BY doc_id;
doc_usage_bag_fg = FOREACH doc_usage_bag GENERATE
                     group                                                 AS doc_id,
                     FLATTEN(doc_token_counts.(token, num_doc_tok_usages)) AS (token, num_doc_tok_usages), 
                     SUM(doc_token_counts.num_doc_tok_usages)              AS doc_size
                   ;

term_freqs = FOREACH doc_usage_bag_fg GENERATE
               doc_id                                          AS doc_id,
               token                                           AS token,
               ((double)num_doc_tok_usages / (double)doc_size) AS term_freq;
             ;
             
term_usage_bag  = GROUP term_freqs BY token;
token_usages    = FOREACH term_usage_bag GENERATE
                    FLATTEN(term_freqs) AS (doc_id, token, term_freq),
                    COUNT(term_freqs)   AS num_docs_with_token
                   ;

tfidf_all = FOREACH token_usages {
              idf    = LOG((double)ndocs.$0/(double)num_docs_with_token);
              tf_idf = (double)term_freq*idf;
                GENERATE
                  doc_id AS doc_id,
                  token  AS token,
                  tf_idf AS tf_idf
                ;
             };

/* Put things back into document vector form before storing in Mahout's vector format */
doc_vectors = GROUP tfidf_all BY doc_id;
feature_vectors = FOREACH doc_vectors GENERATE (chararray)group AS doc_id,com.mozilla.grouperfish.pig.eval.ml.TFIDFVectorizer('$FEATUREINDEX', $1) AS vec;

STORE feature_vectors INTO '$OUTPUT' USING com.mozilla.grouperfish.pig.storage.DocumentVectorStorage('$NFEATURES');

/* Run Mahout's Clustering on this output */
/*
/usr/lib/hadoop/bin/hadoop jar /usr/lib/mahout/mahout-core-0.5-job.jar org.apache.mahout.driver.MahoutDriver kmeans 
-i document-vectors 
-o kmeans-cosine-out 
-dm org.apache.mahout.common.distance.CosineDistanceMeasure 
-c random-clusters 
-ow 
-k 15 
-x 20 
-cl
*/