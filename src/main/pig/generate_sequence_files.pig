register './akela-0.1.jar'                                                                                                               
/* Not sure why we have to register this JAR when it's already in Pig's classpath but we do */
register '/usr/lib/hbase/hbase-0.90.1-cdh3u0.jar'

raw = LOAD 'hbase://grouperfish' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('data:json') AS json:chararray;
genmap = FOREACH raw GENERATE com.mozilla.pig.eval.json.JsonMap(json) AS json_map:map[];
documents = FOREACH raw GENERATE (chararray)docid,com.mozilla.pig.eval.text.RemoveStopwords(text);
filtered_documents = FILTER documents BY normtext IS NOT NULL AND SIZE(normtext) > 0;
STORE filtered_documents INTO 'documents' USING com.mozilla.pig.storage.SequenceFileStorage();

/* Same as above except using tsv file for experimenting */
raw = LOAD 'opinions-en.tsv' USING PigStorage('\t') AS (docid:int,datetime:long,praise_issue:chararray,product:chararray,version:chararray,os:chararray,language:chararray,text:chararray);
documents = FOREACH raw GENERATE (chararray)docid,text;
/* filtered_documents = FILTER documents BY normtext IS NOT NULL AND SIZE(normtext) > 0; */
STORE filtered_documents INTO 'documents' USING com.mozilla.pig.storage.SequenceFileStorage();

/*
Follow up steps:

hadoop jar mahout-examples-0.5-job.jar org.apache.mahout.driver.MahoutDriver seq2sparse -i documents -wt tfidf --minDF 2 --maxDFPercent 90 -o seq2sparse-out
hadoop jar mahout-core-0.5-job.jar org.apache.mahout.driver.MahoutDriver kmeans -i seq2sparse-out/tfidf-vectors -o kmeans-cosine-out -dm org.apache.mahout.common.distance.CosineDistanceMeasure -c random-clusters -ow -k 20 -x 10 -cl

*/