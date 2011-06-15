package com.mozilla.grouperfish.jobs.carrot2;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similar.SimilarityQueries;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.base.Assert;

public class Index<T> {

	private static final Logger log = LoggerFactory.getLogger(CarrotClusterTool.class);

	final static String TEXT = "text";
	final static String LABEL = "label";

	final Map<String, T> items_ = new HashMap<String, T>();
	boolean closed_ = false;

	final Analyzer analyzer_;
	final Directory index_;
	final IndexWriter writer_;
	{
		index_ = new RAMDirectory();
		analyzer_ = new StopAnalyzer(Version.LUCENE_31);
		try {
			writer_ = new IndexWriter(index_, new IndexWriterConfig(Version.LUCENE_31, analyzer_));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	void add(String text, String label, T payload) {
		Assert.check(!closed_);

		items_.put(label, payload);

		Document doc = new Document();
		doc.add(new Field(TEXT, text, Field.Store.YES, Field.Index.ANALYZED));
		doc.add(new Field(LABEL, label, Field.Store.YES, Field.Index.ANALYZED));

		try {
			writer_.addDocument(doc);
			log.info("Added text '{}'", text);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	T find(final String text, final double[] scoreContainer) {
		try {
			if (!closed_) {
				closed_ = true;
				writer_.close();
			}
			IndexSearcher searcher = new IndexSearcher(index_, true);
			Query moreLikeThis = SimilarityQueries.formSimilarQuery(text, analyzer_, TEXT, null);

			TopDocs hits = searcher.search(moreLikeThis, 1);
			log.trace("Search for text '{}' yielded {} results.", text, hits.scoreDocs.length);
			if (hits.totalHits == 0) return null;

			ScoreDoc scoreDoc = hits.scoreDocs[0];
			String matchText = searcher.doc(scoreDoc.doc).get(TEXT);
			String matchLabel = searcher.doc(scoreDoc.doc).get(LABEL);
			double matchScore = scoreDoc.score;
			log.trace("    top result (score={}): {}", scoreDoc.score, matchText);
			log.trace("    top label: {}", matchLabel);
			scoreContainer[0] = matchScore;
			return items_.get(matchLabel);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
