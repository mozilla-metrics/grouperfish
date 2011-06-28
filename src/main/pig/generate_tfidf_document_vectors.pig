register './akela-0.1.jar'
register './lib/lucene-core-3.1.0.jar'
register './lib/lucene-analyzers-3.1.0.jar'
register './lib/mahout-core-0.5.jar'
register './lib/mahout-math-0.5.jar'
register './lib/mahout-utils-0.5.jar'
register './lib/mahout-collections-1.0.jar'

/* New approach using Lucene Analyzers */
raw = LOAD 'opinions-en.tsv' USING PigStorage('\t') AS (doc_id:int,datetime:long,praise_issue:chararray,product:chararray,version:chararray,os:chararray,language:chararray,text:chararray);
tokenized = FOREACH raw GENERATE doc_id,FLATTEN(com.mozilla.pig.eval.text.Tokenize(text)) AS token:chararray;

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
              idf    = LOG((double)$NDOCS/(double)num_docs_with_token);
              tf_idf = (double)term_freq*idf;
                GENERATE
                  doc_id AS doc_id,
                  token  AS token,
                  tf_idf AS tf_idf
                ;
             };

STORE tfidf_all INTO '$OUT';

/* TODO: Filter based on minDF and maxDF% (e.g. num_docs_with_token > 10 AND (num_docs_with_token/$NDOCS) < 0.90)
/* Put things back into document vector form before storing in Mahout's vector format */
doc_vectors = GROUP tfidf_all BY doc_id;
feature_vectors = FOREACH doc_vectors GENERATE (chararray)group AS doc_id,com.mozilla.pig.eval.ml.TFIDFVectorizer('feature-index', $1) AS vec;

STORE feature_vectors INTO 'document-vectors' USING com.mozilla.pig.storage.DocumentVectorStorage('$NFEATURES');

/* Run Mahout's Clustering on this output */
/*
/usr/lib/hadoop/bin/hadoop jar /usr/lib/mahout/mahout-core-0.5-job.jar org.apache.mahout.driver.MahoutDriver kmeans -i document-vectors -o kmeans-tanimoto-out -dm org.apache.mahout.common.distance.TanimotoDistanceMeasure -c random-clusters -ow -k 25 -x 20 -cl
/usr/lib/mahout/bin/mahout clusterdump --seqFileDir kmeans-tanimoto-out/clusters-1 --pointsDir kmeans-tanimoto-out/clusteredPoints --output clusteranalyze.txt -d new-feature-index.txt -dt text
*/