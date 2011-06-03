register './akela-0.1.jar'                                                                                                               
/* Not sure why we have to register this JAR when it's already in Pig's classpath but we do */
register '/usr/lib/hbase/hbase-0.90.1-cdh3u0.jar'
register './mahout-core-0.5-job.jar'
register './mahout-core-0.5.jar'
register './mahout-math-0.5.jar'
register './mahout-utils-0.5.jar'

raw = LOAD 'hbase://grouperfish' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('data:json') AS json:chararray;
genmap = FOREACH raw GENERATE com.mozilla.pig.eval.json.JsonMap(json) AS json_map:map[];
document_word_vectors = FOREACH genmap GENERATE (chararray)json_map#'id' AS docid:chararray,com.mozilla.pig.eval.text.Normalize(json_map#'text') AS word_tuple;

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
vectors = FOREACH document_word_vectors GENERATE docid,com.mozilla.pig.eval.ml.Vectorizer('feature-index', word_tuple) AS vec;
STORE vectors INTO 'document-vectors' USING com.mozilla.pig.storage.DocumentVectorStorage();

/* Use this output if you're not using Mahout */
/*
flat_vectors = FOREACH vectors GENERATE docid,FLATTEN(vec);
STORE flat_vectors INTO 'document-vectors';
*/