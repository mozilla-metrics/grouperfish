package com.mozilla.grouperfish.jobs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.analysis.Analyzer;
import org.apache.mahout.vectorizer.DefaultAnalyzer;
import org.apache.mahout.vectorizer.DictionaryVectorizer;
import org.apache.mahout.vectorizer.DocumentProcessor;
import org.apache.mahout.vectorizer.collocations.llr.LLRReducer;
import org.apache.mahout.vectorizer.tfidf.TFIDFConverter;
import com.mozilla.grouperfish.conf.Conf;
import com.mozilla.grouperfish.model.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The functionality here is practically cloned from the Mahout utility
 * <tt>SparseVectorsFromSequenceFiles</tt>, which serves a very similar purpose
 * (but as a command line utility).
 */
public class VectorizeDocuments extends AbstractCollectionTool {

	public VectorizeDocuments(Conf conf, Configuration hadoopConf) {
		super(conf, hadoopConf);
	}

	@Override
	public int run(Collection collection, long timestamp) throws Exception {
		final Configuration hadoopConf = getConf();
		CollectionTool source = new ExportDocuments(conf_, hadoopConf);
		final Path inputDir = util_.outputDir(collection.ref(), timestamp, source);
		final Path outputDir = util_.outputDir(collection.ref(), timestamp, this);

		new Util(conf_).setJobTracker(getConf(), collection);

		// 1. Tokenize
		Class<? extends Analyzer> analyzerClass = DefaultAnalyzer.class;
		Path tokenizedPath = new Path(outputDir, DocumentProcessor.TOKENIZED_DOCUMENT_OUTPUT_FOLDER);

		DocumentProcessor.tokenizeDocuments(inputDir, analyzerClass, tokenizedPath, hadoopConf);

		// 2. TF Vectors
		int chunkSize = 200;
		int minSupport = 10;
		int maxNGramSize = 1;
		float minLLRValue = LLRReducer.DEFAULT_MIN_LLR;
		log.info("Minimum LLR value: {}", minLLRValue);

		int reduceTasks = 1;
		log.info("Number of reduce tasks: {}", reduceTasks);

		boolean namedVectors = true;
		boolean sequentialAccessOutput = true;

		DictionaryVectorizer.createTermFrequencyVectors(tokenizedPath, outputDir, hadoopConf, minSupport, maxNGramSize,
				minLLRValue, -1.0f, false, reduceTasks, chunkSize, sequentialAccessOutput, namedVectors);

		// 3. IDF Vectors

		// Matches the similarity model we use with the inverted index later on.
		float norm = 2.0f;
		boolean logNormalize = true;

		// minimum number of documents a term appears in for it to be considered
		int minDf = 10;

		// max percentage of docs before term is considered a stopword
		int maxDFPercent = 95;

		Path tfPath = new Path(outputDir, DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER);
		TFIDFConverter.processTfIdf(tfPath, outputDir, hadoopConf, chunkSize, minDf, maxDFPercent, norm, logNormalize,
				sequentialAccessOutput, namedVectors, reduceTasks);

		return 0;
	}

	@Override
	public String name() {
		return NAME;
	}

	private static final Logger log = LoggerFactory.getLogger(VectorizeDocuments.class);

	public static final String NAME = "vectorize";

}
