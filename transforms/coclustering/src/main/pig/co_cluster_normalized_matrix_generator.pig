--
-- Script to compute normalized matrix An
--
-- See "Co-clustering documents and words using Bipartite Spectral Graph
-- Partitioning" by Dhillon for more details.
--

%default TEMP 'cct'
%default NUM_REDUCERS 7
register './lib/grouperfish-transforms-coclustering-0.3-SNAPSHOT.jar'
register './lib/lucene-core-3.1.0.jar'
register './lib/lucene-analyzers-3.1.0.jar'
register './lib/mahout-core-0.5.jar'
register './lib/mahout-math-0.5.jar'
register './lib/mahout-utils-0.5.jar'
register './lib/mahout-collections-1.0.jar'
SET default_parallel $NUM_REDUCERS
SET pig.splitCombination 'false';

DEFINE get_docid com.mozilla.grouperfish.transforms.coclustering.pig.eval.text.ConvertDocumentIDToID();
DEFINE get_fid com.mozilla.grouperfish.transforms.coclustering.pig.eval.text.ConvertFeatureToID();

-- Reindex A using doc_index and feature_index.
A_preindex = LOAD '$TEMP/A' AS (doc_id:chararray,
				    tf_bag:{t:(feature:chararray,
						     tf:double)});
A_doc_reindexed = FOREACH A_preindex
		    GENERATE get_docid('$TEMP/doc_index',doc_id)
					AS doc_id:int,
			     tf_bag;
flat_A = FOREACH A_doc_reindexed
		GENERATE doc_id,
			FLATTEN(tf_bag) AS (feature,tf);
A_reindexed = FOREACH flat_A
		GENERATE doc_id,
			get_fid('$TEMP/feature_index',feature) AS f_id,tf;
A_precleanup = GROUP A_reindexed BY doc_id;
A = FOREACH A_precleanup
	GENERATE group AS doc_id,
	         A_reindexed.(f_id,tf)
		    AS feature_bag;
-- Compute Doc Degrees
doc_degrees = FOREACH A
		GENERATE doc_id,
		     ((double)1/SQRT(SUM(feature_bag.tf))) AS degree;
STORE doc_degrees INTO '$TEMP/doc_degrees';
-- Compute feature degrees.
flat_features = FOREACH A
		    GENERATE doc_id,
			    FLATTEN(feature_bag.(f_id,tf))
				AS (f_id:int, tf:double);

grouped_features = GROUP flat_features BY f_id;
--dump grouped_features;
feature_degrees = FOREACH grouped_features
		    GENERATE group AS f_id,
			      ((double)1/SQRT(SUM(flat_features.tf)))
							    AS degree;
describe feature_degrees;
STORE feature_degrees INTO '$TEMP/feature_degrees';

-- First we normalize using doc_degrees
doc_deg_adj = JOIN doc_degrees BY doc_id, flat_features BY doc_id;
describe doc_deg_adj;
--dump doc_deg_adj;
part_norm_adj = FOREACH doc_deg_adj
		    GENERATE  doc_degrees::doc_id AS doc_id,
				 f_id AS f_id, tf*degree AS value;
describe part_norm_adj;
--dump part_norm_adj;
-- Next we normalize using feature_degrees
feature_deg_adj = JOIN feature_degrees BY f_id,
			part_norm_adj BY f_id;
describe feature_deg_adj;
flat_norm_adj = FOREACH feature_deg_adj
		    GENERATE
			part_norm_adj::doc_id AS doc_id,
			part_norm_adj::f_id AS f_id,
			feature_degrees::degree*part_norm_adj::value AS value;
describe flat_norm_adj;
--dump flat_norm_adj;
norm_adj_grouped =  GROUP flat_norm_adj BY doc_id;
describe norm_adj_grouped;
norm_adj = FOREACH norm_adj_grouped
		GENERATE group AS row_id:int,
			flat_norm_adj.(f_id,value) AS row_info;
describe norm_adj;
--dump norm_adj;
An = FOREACH norm_adj
		GENERATE row_id, com.mozilla.grouperfish.transforms.coclustering.pig.eval.mahout.Vectorizer(row_info) AS vec;
STORE An INTO '$TEMP/An' USING
    com.mozilla.grouperfish.transforms.coclustering.pig.storage.MahoutVectorStorage
				    ('$TEMP/nfeatures','false','true');


