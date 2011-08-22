register './akela-0.2-SNAPSHOT.jar'
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
%default OUTPUT 'document-vectors-tf'

raw = LOAD 'hbase://grouperfish' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('data:json') AS json:chararray;
genmap = FOREACH raw GENERATE com.mozilla.pig.eval.json.JsonMap(json) AS json_map:map[];
/*raw = LOAD '$INPUT' USING PigStorage('\t') AS (doc_id:int,datetime:long,praise_issue:chararray,product:chararray,version:chararray,os:chararray,locale:chararray,text:chararray);*/

tokenized = FOREACH genmap GENERATE doc_id,com.grouperfish.mozilla.pig.eval.text.Tokenize(json_map#'text','$STOPWORDS', '$STEM') AS token_bag;
/* Comment out the line above and uncomment the line below if you are using an ngram feature-index */
/*tokenized = FOREACH filtered_raw GENERATE doc_id,com.mozilla.pig.eval.text.NGramTokenize(text,'$STOPWORDS', '$STEM', 'true') AS token_bag;*/
filtered_tokenized = FILTER tokenized BY SIZE(token_bag) > 1;
doc_vectors = FOREACH filtered_tokenized GENERATE doc_id,com.mozilla.grouperfish.pig.eval.text.TermFrequency(token_bag) AS tf_bag;

/* Put things back into document vector form before storing in Mahout's vector format */
feature_vectors = FOREACH doc_vectors GENERATE (chararray)doc_id,com.mozilla.pig.eval.ml.TFVectorizer('$FEATUREINDEX', tf_bag) AS vec;
STORE feature_vectors INTO '$OUTPUT' USING com.mozilla.grouperfish.pig.storage.DocumentVectorStorage('$NFEATURES');

/* Run Mahout's Clustering on this output */
/*
/usr/lib/hadoop/bin/hadoop jar /usr/lib/mahout/mahout-core-0.5-job.jar org.apache.mahout.driver.MahoutDriver lda 
-i document-vectors-tf 
-o lda-out 
-ow 
-k 20 
-v 12000
-x 20
*/