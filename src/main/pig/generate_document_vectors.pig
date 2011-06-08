register './akela-0.1.jar'                                                                                                               
/* Not sure why we have to register this JAR when it's already in Pig's classpath but we do */
register '/usr/lib/hbase/hbase-0.90.1-cdh3u0.jar'
register './mahout-core-0.5-job.jar'
register './mahout-core-0.5.jar'
register './mahout-math-0.5.jar'
register './mahout-utils-0.5.jar'

raw = LOAD 'hbase://grouperfish' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('data:json') AS json:chararray;
genmap = FOREACH raw GENERATE com.mozilla.pig.eval.json.JsonMap(json) AS json_map:map[];
document_word_bag = FOREACH genmap GENERATE (chararray)json_map#'id' AS docid:chararray,com.mozilla.pig.eval.text.UnigramExtractor(json_map#'text') AS word_bag;
document_word_vectors = FOREACH document_word_bag GENERATE docid, com.mozilla.pig.eval.ConvertBagToTuple(word_bag) AS word_vector;

/*
For input:
((the,quick,brown,fox,jumped,over,the,lazy,dog)) 

With feature index:
dog = 1 
fox = 2
the = 3 
lazy = 4
over = 5
brown = 6 
quick = 7
jumped = 8

Output should be:
((3,7,6,2,8,5,3,4,1)) 
*/
vectors = FOREACH document_word_vectors GENERATE (chararray)docid,com.mozilla.pig.eval.ml.Vectorizer('feature-index', word_vector) AS vec;
STORE vectors INTO 'document-vectors' USING com.mozilla.pig.storage.DocumentVectorStorage();

/* Use this output if you're not using Mahout */
/*
flat_vectors = FOREACH vectors GENERATE docid,FLATTEN(vec);
STORE flat_vectors INTO 'document-vectors';
*/


/* Same as above except using tsv file for experimenting */
raw = LOAD 'opinions-en.tsv' USING PigStorage('\t') AS (docid:int,datetime:long,praise_issue:chararray,product:chararray,version:chararray,os:chararray,language:chararray,text:chararray);
document_word_bag = FOREACH raw GENERATE docid, com.mozilla.pig.eval.text.UnigramExtractor(text) AS word_bag;
document_word_vectors = FOREACH document_word_bag GENERATE docid, com.mozilla.pig.eval.ConvertBagToTuple(word_bag) AS word_vector;
vectors = FOREACH document_word_vectors GENERATE (chararray)docid,com.mozilla.pig.eval.ml.Vectorizer('feature-index', word_vector) AS vec;
STORE vectors INTO 'document-vectors' USING com.mozilla.pig.storage.DocumentVectorStorage();