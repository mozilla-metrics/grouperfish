package com.mozilla.grouperfish.jobs.textcluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;
import com.mozilla.grouperfish.jobs.Histogram;
import com.mozilla.grouperfish.model.BaseCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory clustering of tf-idf vectors based on an inverted index.
 * 
 * See https://github.com/davedash/textcluster for a similar Python version.
 * Differences: - Processing in chunks (these can later be merged by clustering
 * their medoids). - uses more sparse data structures (no zeroes from
 * defaultdict) - this version reconsiders assignemts - this version has a
 * "centrality" which have a cluster be reassigned to a "more central" similar
 * cluster after the initial run. The original textcluster assigns documents
 * greedy which can produce sub-optimum results.
 * 
 * 
 * The original paper on canopy clustering already suggests using an inverted
 * index for high dimensions. This roughly builds on that idea.
 * 
 * Sketch for full Map/Reduce parallelization without using chunks: 1. Build the
 * inverted index in the map stage (term) -> (doc, weight) 2. From reducer, emit
 * partial cosine similarity: (doc, doc, term) -> (weight) Q: what to do when
 * postings from the mappers become too large? 3. Remap to the key (doc, doc)
 * and value (score). 4. Reduce: Sum the now sorted partial scores, use the top
 * score and emit the entries with lower id to the left (normalization). These
 * are our cluster members. Q: Can we use the followers/splicing with that (not
 * triggered all that often anyways)?
 */
public class IndexClusterer {

	private static final Logger log = LoggerFactory.getLogger(IndexClusterer.class);

	// Defaults
	public static final int BLOCK_SIZE = 40000;
	public static final double SIMILARITY_THRESHOLD = .08;
	public static final double REASSIGN_THRESHOLD = .3;
	public static final int MIN_DOCUMENT_LENGTH = 2;

	/** The tf-idf vectors from the input documents. */
	private List<Vector> vectors_;

	/**
	 * Documents that could not be clustered are collected here, and not touched
	 * on reset.
	 */
	private final List<Vector> rest_ = new java.util.ArrayList<Vector>();

	/**
	 * Inverted index that maps: term-index --> (doc-index --> weight)
	 * 
	 * We do not use a sparse structure because we assume the load factor to be
	 * roughly 0.5 to 1 (depending on the collection size).
	 */
	private final List<Map<Integer, Double>> index_;

	private final double minTreshold_ = SIMILARITY_THRESHOLD;
	private final double reassignTreshold_ = REASSIGN_THRESHOLD;
	private final int blockSize_ = BLOCK_SIZE;
	private int n_;
	private int dictSize_;

	/** the size of the term dictionary = maximum vector length = index size */
	public IndexClusterer(int dictSize) {
		log.info(String.format("Creating index clusterer. Dictionary size: %d", dictSize));
		dictSize_ = dictSize;
		index_ = new java.util.ArrayList<Map<Integer, Double>>(dictSize);
		reset();
	}

	private void reset() {
		n_ = 0;
		for (int i = 0; i < index_.size(); ++i)
			index_.set(i, new java.util.HashMap<Integer, Double>());
		for (int i = index_.size(); i < dictSize_; ++i)
			index_.add(new java.util.HashMap<Integer, Double>());
		vectors_ = new java.util.LinkedList<Vector>();
	}

	/**
	 * Returns <tt>null</tt> until BLOCK_SIZE vectors have been added. Then,
	 * compute and return a clustering and reset the internal state.
	 */
	public List<BaseCluster> add(Vector next) {
		if (next.getNumNondefaultElements() < MIN_DOCUMENT_LENGTH)
			return null;
		++n_;
		vectors_.add(next);
		if (next.size() > dictSize_) {
			for (int i = dictSize_; i < next.size(); ++i)
				index_.add(new java.util.HashMap<Integer, Double>());
			dictSize_ = next.size();
		}

		if (n_ < blockSize_)
			return null;

		createIndex();
		List<BaseCluster> clusters = createClusters();
		reset();
		return clusters;
	}

	/**
	 * Force calculation of the cluster, e.g. where the collection is smaller
	 * than the chunk size.
	 */
	public List<BaseCluster> clusters() {
		createIndex();
		return createClusters();
	}

	public List<Vector> rest() {
		return rest_;
	}

	/** Builds the inverted index after all vectors have been added. */
	private void createIndex() {

		final long ts = System.currentTimeMillis();
		log.info("Creating inverted index...");
		int doc = 0;
		for (final Vector v : vectors_) {
			final Iterator<Element> it = v.iterateNonZero();
			while (it.hasNext()) {
				final Element e = it.next();
				final int term = e.index();
				final double weight = e.get();
				index_.get(term).put(doc, weight);
			}
			++doc;
		}

		log.info(String.format("Index created. Took %d ms.", dictSize_, System.currentTimeMillis() - ts));
	}

	/**
	 * Calculates clusters. Remaining clusters are added to the remainder.
	 */
	private List<BaseCluster> createClusters() {
		final long ts1 = System.currentTimeMillis();
		log.info(String.format("1/3 Creating scores for %d vectors...", n_));

		// Save about each document
		//
		// 1) The most similar other document,
		// 2) the similarity to that other document,
		// 3) the overall "centricity" of this document.
		//
		// The centricity has no impact on the distribution of documents among
		// clusters, but documents with the highest centricity will become
		// cluster
		// medoids.

		final int[] leader = new int[n_];
		final double[] leaderScore = new double[n_];
		final double[] centricity = new double[n_];

		final int NO_MATCH = -1;
		for (int i = 0; i < n_; ++i)
			leader[i] = NO_MATCH;

		// List of vectors that are actually used.
		final List<Integer> used = new ArrayList<Integer>(n_);

		// Metric: How often could a document be reassigned to a better medoid?
		int reassignments = 0;

		// Stage 1:
		//
		// For each document A find matches in the index. Each match M that is
		// similar enough is added as a "follower" to the A (and A becomes the
		// "best match" of M). The score is saved along with the association.
		//
		// If M is matched by another document B later on for a higher
		// similarity,
		// M is reassigned to B.
		{
			final Iterator<Vector> vs = vectors_.listIterator();

			final Map<Integer, Double> similars = new HashMap<Integer, Double>();
			int docIdx = 0;
			while (vs.hasNext()) {

				// Iterate search terms, find matches and calculate scores.
				final Iterator<Element> it = vs.next().iterateNonZero();
				while (it.hasNext()) {
					final Element e = it.next();
					final int term = e.index();
					final Map<Integer, Double> posting = index_.get(term);
					final double weight = e.get();

					// Walk all documents matching this term.
					for (Map.Entry<Integer, Double> match : posting.entrySet()) {
						final Integer matchIdx = match.getKey();
						if (matchIdx <= docIdx)
							continue; // these are already complete

						// add score for this term
						final double termScore = weight * match.getValue().doubleValue();
						final Double pairScore = similars.get(matchIdx);
						if (pairScore == null)
							similars.put(matchIdx, termScore);
						else
							similars.put(matchIdx, pairScore.doubleValue() + termScore);
					}
				}

				// Check each match for minimum-score and possibly
				// reassign-score.
				for (Map.Entry<Integer, Double> entry : similars.entrySet()) {
					double score = entry.getValue();
					if (score < minTreshold_)
						continue; // too far off
					int matchIdx = entry.getKey();
					if (matchIdx < docIdx)
						continue; // already computed
					centricity[docIdx] += score;
					centricity[matchIdx] += score;

					if (leader[matchIdx] != NO_MATCH) {
						if (score < reassignTreshold_ || leaderScore[matchIdx] > score)
							continue;
						reassignments++;
					}

					leader[matchIdx] = docIdx;
					leaderScore[matchIdx] = score;
				}

				if (centricity[docIdx] > 0)
					used.add(docIdx);
				else
					rest_.add(vectors_.get(docIdx));

				++docIdx;
				if (docIdx % 5000 == 0)
					log.info("...{}", docIdx);
				similars.clear();
			}
		}

		// TODO: maybe make another pass and reassign based on centricity...

		// Introduce a total ordering by centricity ASC, index DESC.
		//
		// The latter is desc for best stability when clusters are rebuilt,
		// because for the output, the order is completely reversed:
		//
		// Clusters will be assigned in order of ascending centricity...
		// Documents will be associated to their best matches bottom-up.
		// Clusters will then be assigned top down.

		final Integer[] toUse = used.toArray(new Integer[used.size()]);
		Arrays.sort(toUse, new Comparator<Integer>() {
			@Override
			public int compare(Integer docA, Integer docB) {
				double primary = centricity[docA] - centricity[docB];
				if (primary == 0)
					return docB - docA;
				return (primary < 0) ? -1 : 1;
			}
		});

		log.info(String.format("    OK. %d reassignments. Using %d/%d elements. Took %sms.", reassignments,
				toUse.length, n_, System.currentTimeMillis() - ts1));

		// Stage 2:
		// Make all "best match" pointers point into the direction of the sort
		// order we just established. Then merge clusters bottom-up in one pass.
		//
		// TODO:
		// The merging can degenerate to n**2 worst case (everything is in one
		// cluster), because the backlinks are copied repeatedly. We need a
		// datastructure that has efficient splicing.

		@SuppressWarnings("unchecked")
		final ArrayList<Integer>[] followers = new ArrayList[n_];

		{
			log.info("2/3 Creating followers...");
			final long ts2 = System.currentTimeMillis();
			for (Integer docIdx : toUse) {
				final int idx = docIdx.intValue();
				final int target = leader[idx];
				if (target == NO_MATCH)
					continue;
				// Make sure everything is pointing "up" to the cluster medoid.
				if (centricity[target] > centricity[idx])
					continue;
				if (centricity[target] == centricity[idx] && target < docIdx)
					continue;
				leader[target] = idx;
				leader[idx] = NO_MATCH;
			}

			int splices = 0;
			for (Integer docIdx : toUse) {
				final int idx = docIdx.intValue();
				final int target = leader[idx];
				if (target == NO_MATCH)
					continue;

				if (followers[target] == null) {
					followers[target] = new ArrayList<Integer>();
				}
				followers[target].add(docIdx);

				// This document is a follower. Move its followers to its
				// leader.
				if (followers[idx] != null) {
					splices++;
					followers[target].addAll(followers[idx]);
				}
				followers[idx] = null;
			}
			log.info(String.format("    Backlinks created. Took %sms. Spliced %dx.", splices,
					System.currentTimeMillis() - ts2));
		}

		// Stage 3:
		// Now we just need to create a cluster for each medoid and its
		// followers.

		final List<BaseCluster> clusters = new ArrayList<BaseCluster>();

		{
			log.info("3/3. Creating clusters, sorted by size desc:");
			long ts3 = System.currentTimeMillis();

			final Histogram histogram = new Histogram();

			for (Integer docIdx : toUse) {
				final int idx = docIdx.intValue();
				if (followers[idx] == null)
					continue;

				final List<Vector> followersList = new ArrayList<Vector>(followers[idx].size());
				final List<Double> similarityList = new ArrayList<Double>(followers[idx].size());

				for (int followerIdx : followers[idx]) {
					followersList.add(vectors_.get(followerIdx));
					similarityList.add(Double.valueOf(leaderScore[followerIdx]));
				}

				for (int i = 0; i < followersList.size(); ++i) {
					histogram.add(followersList.size());
				}

				clusters.add(new BaseCluster(vectors_.get(idx), followersList, similarityList));
			}

			log.info(String.format("Size distribution: %s", histogram));
			log.info(String.format("    Done. Created and sorted %d clusters in %dms.", clusters.size(),
					System.currentTimeMillis() - ts3));
		}

		return clusters;
	}

}
