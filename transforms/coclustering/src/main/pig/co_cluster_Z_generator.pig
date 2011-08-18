--
-- Script to compute Z
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

DEFINE get_docid com.mozilla.grouperfish.transforms.coclustering.pig.eval.text.ConvertDocumentIDToID();
DEFINE get_fid com.mozilla.grouperfish.transforms.coclustering.pig.eval.text.ConvertFeatureToID();

-- Load Singular Vectors of A
U = LOAD '$TEMP/SVD/U' USING com.mozilla.grouperfish.transforms.coclustering.pig.storage.MahoutVectorStorage()
	    AS (row_id:int, row_info:bag{t:tuple(col_id:int, element:double)});
V = LOAD '$TEMP/SVD/V' USING com.mozilla.grouperfish.transforms.coclustering.pig.storage.MahoutVectorStorage()
	    AS (row_id:int, row_info:bag{t:(c_id:int,element:double)});
describe U;
-- dump U;
-- Load Doc and feaure degrees.
doc_degrees = LOAD '$TEMP/doc_degrees' AS (doc_id:int, degree:double);
describe doc_degrees;
feature_degrees = LOAD '$TEMP/feature_degrees' AS (f_id:int, degree:double);
describe feature_degrees;
flatU = FOREACH U GENERATE row_id,
	     FLATTEN(row_info) AS (col_id:int, element:double);
describe flatU;
-- dump flatU;
-- Select singular vectors in range [u_2,...,u_l+1] where l = log_2(k)
filteredU = FILTER flatU BY col_id > 0;
-- dump filteredU;
-- Scale U by doc_degrees
doc_degreesU = JOIN doc_degrees BY doc_id, filteredU BY row_id;
rescaledU = FOREACH doc_degreesU
		GENERATE row_id AS row_id,
		    col_id AS col_id, element*degree AS element;
describe rescaledU;
groupedU = GROUP rescaledU BY row_id;
U_ = FOREACH groupedU
	    GENERATE group AS row_id,
		    rescaledU.(col_id,element) AS row_info;
describe U_;
-- dump U_;
allU = GROUP U_ ALL;
num_rows_U = FOREACH allU GENERATE COUNT(U_);
describe num_rows_U;
-- dump num_rows_U;
flatV = FOREACH V GENERATE row_id,
	    FLATTEN(row_info) AS (col_id:int, element:double);
describe flatV;
-- Select singular vectors in range [v_2,...,v_l+1] where l = log_2(k)
filteredV = FILTER flatV BY col_id > 0;
-- dump filteredV;
-- Scale V by feature_degrees
feature_degreesV = JOIN feature_degrees BY f_id, filteredV by row_id;
rescaledV = FOREACH feature_degreesV
		GENERATE row_id AS row_id,
		    col_id as col_id, element*degree AS element;
describe rescaledV;
groupedV = GROUP rescaledV BY row_id;
V_ = FOREACH groupedV
	    GENERATE group + (int) num_rows_U.$0 AS row_id,
		    rescaledV.(col_id,element) AS row_info;
describe V_;
-- dump V_;
-- Compute Z
Z_ = UNION U_,V_;
describe Z_;
Z = FOREACH Z_
	    GENERATE row_id,
		com.mozilla.grouperfish.transforms.coclustering.pig.eval.mahout.Vectorizer(row_info) AS vec;
describe Z;
STORE Z INTO '$TEMP/Z' USING
	com.mozilla.grouperfish.transforms.coclustering.pig.storage.MahoutVectorStorage('$TEMP/l','true','false');
-- Construct doc_map and feature_map to map Z rows to doc/feature.
doc_index = LOAD '$TEMP/doc_index' AS (doc:chararray);
doc_map = FOREACH doc_index
		    GENERATE get_docid('$TEMP/doc_index',doc) AS row_id:int,
			    doc;
STORE doc_map INTO '$TEMP/doc_map';
feature_index = LOAD '$TEMP/feature_index' AS (feature:chararray);
feature_map = FOREACH feature_index
		    GENERATE get_fid('$TEMP/feature_index',feature)
			+ (int) num_rows_U.$0 AS row_id,
			feature;
STORE feature_map INTO '$TEMP/feature_map';
