register './akela-0.1.jar'
register './lib/lucene-core-3.1.0.jar'
register './lib/lucene-analyzers-3.1.0.jar'
register './lib/mahout-core-0.5.jar'
register './lib/mahout-math-0.5.jar'
register './lib/mahout-utils-0.5.jar'
register './lib/mahout-collections-1.0.jar'

SET default_parallel 7;
SET pig.splitCombination 'false';

raw = LOAD 'opinions-en.tsv' USING PigStorage('\t') AS (doc_id:int,datetime:long,praise_issue:chararray,product:chararray,version:chararray,os:chararray,language:chararray,text:chararray);
filtered_raw = FILTER raw BY praise_issue == 'praise' AND version == '4.0b12';
group_filtered = GROUP filtered_raw all;
ndocs = FOREACH group_filtered GENERATE COUNT(filtered_raw);
tokenized = FOREACH filtered_raw GENERATE doc_id,com.mozilla.pig.eval.text.Tokenize(text,'stopwords-en.txt') AS token_bag;
doc_vectors = FOREACH tokenized GENERATE doc_id,com.mozilla.pig.eval.text.TermFrequency(token_bag) AS tf_bag;

/* Put things back into document vector form before storing in Mahout's vector format */
feature_vectors = FOREACH doc_vectors GENERATE (chararray)doc_id,com.mozilla.pig.eval.ml.TFVectorizer('feature-index', tf_bag) AS vec;
STORE feature_vectors INTO 'document-vectors-tf' USING com.mozilla.pig.storage.DocumentVectorStorage('$NFEATURES');

/* Run Mahout's Clustering on this output */
/*
/usr/lib/hadoop/bin/hadoop jar /usr/lib/mahout/mahout-core-0.5-job.jar org.apache.mahout.driver.MahoutDriver kmeans -i document-vectors -o kmeans-tanimoto-out -dm org.apache.mahout.common.distance.TanimotoDistanceMeasure -c random-clusters -ow -k 25 -x 20 -cl
/usr/lib/mahout/bin/mahout clusterdump --seqFileDir kmeans-tanimoto-out/clusters-1 --pointsDir kmeans-tanimoto-out/clusteredPoints --output clusteranalyze.txt -d new-feature-index.txt -dt text
*/