package com.mozilla.grouperfish.mahout.clustering.display.lda;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;
import org.apache.mahout.common.IntPairWritable;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.math.Vector.Element;

import com.mozilla.hadoop.fs.SequenceFileDirectoryReader;
import com.mozilla.util.Pair;

public class DisplayLDABase {

    private static final Logger LOG = Logger.getLogger(DisplayLDABase.class);
    
    // Adds the word if the queue is below capacity, or the score is high enough
    private static void enqueue(Queue<Pair<Double,String>> q, String word, double score, int numWordsToPrint) {
        if (q.size() >= numWordsToPrint && score > q.peek().getFirst()) {
            q.poll();
        }
        if (q.size() < numWordsToPrint) {
            q.add(new Pair<Double,String>(score, word));
        }
    }
    
    public static Map<Integer,PriorityQueue<Pair<Double,String>>> getTopWordsByTopic(String stateDirPath, Map<Integer,String> featureIndex, int numWordsToPrint) {
        Map<Integer,Double> expSums = new HashMap<Integer, Double>();
        Map<Integer,PriorityQueue<Pair<Double,String>>> queues = new HashMap<Integer,PriorityQueue<Pair<Double,String>>>();
        SequenceFileDirectoryReader reader = null;
        try {
            IntPairWritable k = new IntPairWritable();
            DoubleWritable v = new DoubleWritable();
            reader = new SequenceFileDirectoryReader(new Path(stateDirPath));
            while (reader.next(k, v)) {
                int topic = k.getFirst();
                int featureId = k.getSecond();
                if (featureId >= 0 && topic >= 0) {
                    double score = v.get();
                    Double curSum = expSums.get(topic);
                    if (curSum == null) {
                        curSum = 0.0;
                    }
                    expSums.put(topic, curSum + Math.exp(score));
                    String feature = featureIndex.get(featureId);
                    
                    PriorityQueue<Pair<Double,String>> q = queues.get(topic);
                    if (q == null) {
                        q = new PriorityQueue<Pair<Double,String>>(numWordsToPrint);
                    }
                    enqueue(q, feature, score, numWordsToPrint);
                    queues.put(topic, q);
                }
            }
        } catch (IOException e) {
            LOG.error("Error reading LDA state dir", e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        
        for (Map.Entry<Integer, PriorityQueue<Pair<Double,String>>> entry : queues.entrySet()) {
            int topic = entry.getKey();
            for (Pair<Double,String> p : entry.getValue()) {
                double score = p.getFirst();
                p.setFirst(Math.exp(score) / expSums.get(topic));
            }
        }
        
        return queues;
    }
    
    public static Map<Integer, PriorityQueue<Pair<Double,String>>> getTopDocIdsByTopic(Path docTopicsPath, int numDocs) {
        Map<Integer, PriorityQueue<Pair<Double,String>>> docIdMap = new HashMap<Integer, PriorityQueue<Pair<Double,String>>>();
        Map<Integer, Double> maxDocScores = new HashMap<Integer,Double>();
        SequenceFileDirectoryReader pointsReader = null;
        try {
            Text k = new Text();
            VectorWritable vw = new VectorWritable();
            pointsReader = new SequenceFileDirectoryReader(docTopicsPath);
            while (pointsReader.next(k, vw)) {
                String docId = k.toString();
                Vector normGamma = vw.get();
                Iterator<Element> iter = normGamma.iterateNonZero();
                double maxTopicScore = 0.0;
                int idx = 0;
                int topic = 0;
                while(iter.hasNext()) {
                    Element e = iter.next();
                    double score = e.get();
                    if (score > maxTopicScore) {
                        maxTopicScore = score;
                        topic = idx;
                    }
                    
                    idx++;  
                }
                
                PriorityQueue<Pair<Double,String>> docIdsForTopic = docIdMap.get(topic);
                if (docIdsForTopic == null) {
                    docIdsForTopic = new PriorityQueue<Pair<Double,String>>(numDocs);
                }
                
                Double maxDocScoreForTopic = maxDocScores.get(topic);
                if (maxDocScoreForTopic == null) {
                    maxDocScoreForTopic = 0.0;
                }
                if (maxTopicScore > maxDocScoreForTopic) {
                    maxDocScores.put(topic, maxTopicScore);
                }
                
                enqueue(docIdsForTopic, docId, maxTopicScore, numDocs);
                docIdMap.put(topic, docIdsForTopic);
            }
        } catch (IOException e) {
            LOG.error("IOException caught while reading clustered points", e);
        } finally {
            if (pointsReader != null) {
                pointsReader.close();
            }
        }

        for (Map.Entry<Integer, Double> entry : maxDocScores.entrySet()) {
            System.out.println("For topic: " + entry.getKey() + " max score: " + entry.getValue());
        }
        
        return docIdMap;
    }
    
}
