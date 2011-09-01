/* Not sure why we have to register this JAR when it's already in Pig's classpath but we do */
register '/usr/lib/hbase/hbase-0.90.1-cdh3u0.jar'
register './lib/akela-0.1.jar'
register './lib/lucene-core-3.1.0.jar'
register './lib/lucene-analyzers-3.1.0.jar'
register './lib/mahout-core-0.5.jar'
register './lib/mahout-math-0.5.jar'
register './lib/mahout-utils-0.5.jar'
register './lib/mahout-collections-1.0.jar'

SET default_parallel 7;
SET pig.splitCombination 'false';

%default INPUT 'opinions.tsv'
%default STOPWORDS 'stopwords-en.txt'
%default STEM 'true'
%default FEATUREINDEX 'feature-index'
%default OUTPUT 'document-vectors'

/*
raw = LOAD 'hbase://grouperfish' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('data:json') AS json:chararray;
genmap = FOREACH raw GENERATE com.mozilla.pig.eval.json.JsonMap(json) AS json_map:map[];
document_word_bag = FOREACH genmap GENERATE (chararray)json_map#'id' AS docid:chararray,com.mozilla.pig.eval.text.UnigramExtractor(json_map#'text') AS word_bag;
document_word_vectors = FOREACH document_word_bag GENERATE docid, com.mozilla.pig.eval.ConvertBagToTuple(word_bag) AS word_vector;

vectors = FOREACH document_word_vectors GENERATE (chararray)docid,com.mozilla.pig.eval.ml.Vectorizer('feature-index', word_vector) AS vec;
STORE vectors INTO 'document-vectors' USING com.mozilla.pig.storage.DocumentVectorStorage();
*/

/* Use this output if you're not using Mahout */
/*
flat_vectors = FOREACH vectors GENERATE docid,FLATTEN(vec);
STORE flat_vectors INTO 'document-vectors';
*/

/* Same as above except using tsv file for experimenting */
raw = LOAD '$INPUT' USING PigStorage('\t') AS (doc_id:int,datetime:long,praise_issue:chararray,product:chararray,version:chararray,os:chararray,locale:chararray,text:chararray);
filtered_raw = FILTER raw BY locale == 'en-US' AND praise_issue == 'issue' AND version == '5.0';
tokenized = FOREACH filtered_raw GENERATE doc_id,com.mozilla.pig.eval.text.Tokenize(text,'$STOPWORDS', '$STEM') AS token_bag;
vectors = FOREACH tokenized GENERATE (chararray)docid,com.mozilla.pig.eval.ml.Vectorizer('$FEATUREINDEX', token_bag) AS vec;
STORE vectors INTO '$OUTPUT' USING com.mozilla.pig.storage.DocumentVectorStorage('$NFEATURES');