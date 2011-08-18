--
-- Script to compute Tags
--
-- See "Co-clustering documents and words using Bipartite Spectral Graph
-- Partitioning" by Dhillon for more details.
--

%default TEMP 'cct'
%default NUM_REDUCERS 7
register './lib/grouperfish-transforms-coclustering-0.3-SNAPSHOT.jar'
register './lib/mahout-core-0.5.jar'
register './lib/mahout-math-0.5.jar'
register './lib/mahout-utils-0.5.jar'
register './lib/mahout-collections-1.0.jar'
SET default_parallel $NUM_REDUCERS
SET pig.splitCombination 'false';

-- Load clustered Points which are in the format <key>, <Vector>
clustered_points = LOAD '$TEMP/kmeans/out/clusteredPoints' USING com.mozilla.grouperfish.transforms.coclustering.pig.storage.KMeansOutputLoader()
	    AS (cluster_id:int, v_id:int, v_info:bag{t:tuple(col_id:int,
						    eblement:double)});
describe clustered_points;
points_clusters = FOREACH clustered_points
		     GENERATE v_id, cluster_id;
describe points_clusters
doc_map = LOAD '$TEMP/doc_map' AS (doc_id:int, doc: chararray);
doc_clusters = JOIN doc_map BY doc_id, points_clusters BY v_id;
tags = FOREACH doc_clusters
	    GENERATE  doc AS doc,
		      cluster_id AS cluster_id;
describe tags;
STORE tags INTO '$TEMP/tags' USING PigStorage('\t');








