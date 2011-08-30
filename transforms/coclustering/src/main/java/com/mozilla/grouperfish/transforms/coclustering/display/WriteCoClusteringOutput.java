/*
 * Copyright 2011 Mozilla Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mozilla.grouperfish.transforms.coclustering.display;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.mahout.clustering.WeightedVectorWritable;
import org.apache.mahout.clustering.kmeans.Cluster;
import org.apache.mahout.math.NamedVector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.mozilla.grouperfish.transforms.coclustering.text.Dictionary;

public class WriteCoClusteringOutput {
    private static final Logger LOG = LoggerFactory.getLogger(WriteCoClusteringOutput.class);
    private final String TOP_DOCS;
    private final String TOP_FEATURES;
    private Configuration conf = new Configuration();
    private FileSystem fs;
    private Path clusteredPointsPath;
    private Path clustersPath;
    private Path docIDMapPath;
    private Path featureIDMapPath;
    private Path docIDTextMapPath;
    private Path tagPath;
    private Path resultsPath;
    private int numResults;
    private String DOC_ID;
    private String[] DOC_TEXT;
    private Map<Integer, CoCluster> coclusters;
    private Map<Integer, String> docIDMap;
    private Map<Integer, String> featureIDMap;
    private Map<String, String> docIDTextMap;

    public WriteCoClusteringOutput(String clusteredPointsPath, String clustersPath, String docIDMapPath,
            String featureIDMapPath, String docIDTextMapPath, String tagPath, String resultsPath, String TOP_DOCS,
            String TOP_FEATURES, int numResults, String DOC_ID, String[] DOC_TEXT) {
        this.clusteredPointsPath = new Path(clusteredPointsPath);
        this.clustersPath = new Path(clustersPath);
        this.docIDMapPath = new Path(docIDMapPath);
        this.featureIDMapPath = new Path(featureIDMapPath);
        this.docIDTextMapPath = new Path(docIDTextMapPath);
        this.coclusters = new HashMap<Integer, CoCluster>();
        this.docIDTextMap = new TreeMap<String, String>();
        this.tagPath = new Path(tagPath);
        this.resultsPath = new Path(resultsPath);
        this.numResults = numResults;
        this.TOP_DOCS = TOP_DOCS;
        this.TOP_FEATURES = TOP_FEATURES;
        this.DOC_ID = DOC_ID;
        this.DOC_TEXT = DOC_TEXT;
    }

    public void initialize() throws IOException {
        loadCentroids();
        try {
            docIDMap = Dictionary.loadInvertedIndexWithKeys(docIDMapPath);
        } catch (IOException e) {
            LOG.error("Error in loading " + docIDMapPath, e);
        }
        try {
            featureIDMap = Dictionary.loadInvertedIndexWithKeys(featureIDMapPath);
        } catch (IOException e) {
            LOG.error("Error in loading " + docIDMapPath, e);
        }
        loadPoints();
    }

    private void loadCentroids() throws IOException {
        Text k = new Text();
        Cluster v = new Cluster();
        CoCluster c;
        SequenceFile.Reader currReader = null;
        try {
            fs = FileSystem.get(clustersPath.toUri(), conf);
            for (FileStatus status : fs.listStatus(clustersPath)) {
                Path p = status.getPath();
                if (!status.isDir() && !p.getName().startsWith("_")) {
                    try {
                        currReader = new SequenceFile.Reader(fs, p, conf);
                        while (currReader.next(k, v)) {
                            c = new CoCluster(v.getCenter(), v.getMeasure());
                            coclusters.put(v.getId(), c);
                        }
                    } finally {
                        IOUtils.closeStream(currReader);
                    }
                }
            }
        } catch (IOException ie) {
            LOG.error("Error while reading clusters", ie);

        } finally {
            if (currReader != null) {
                IOUtils.closeStream(currReader);
            }
            if (fs != null) {
                fs.close();
            }
        }
    }

    private void loadText(Set<String> allTopDocIDs) throws IOException {
        Map<String, String> currLine = null;
        String currID;
        String currText;
        ObjectMapper mapper = new ObjectMapper();
        BufferedReader reader = null;
        try {
            fs = FileSystem.get(docIDTextMapPath.toUri(), conf);
            for (FileStatus status : fs.listStatus(docIDTextMapPath)) {
                Path p = status.getPath();
                if (!status.isDir() && !p.getName().startsWith("_")) {
                    try {
                        reader = new BufferedReader(new InputStreamReader(fs.open(status.getPath())));
                        String line = null;
                        currID = null;
                        while ((line = reader.readLine()) != null) {
                            String[] pair = line.split("\t", 2);
                            currLine = mapper.readValue(pair[1], new TypeReference<Map<String, String>>() {
                            });
                            if (currLine.containsKey(this.DOC_ID)) {
                                currID = currLine.get(this.DOC_ID);
                                if (allTopDocIDs.contains(currID)) {
                                    currText = " ";
                                    for (String s : this.DOC_TEXT) {
                                        if (currLine.containsKey(s)) {
                                            currText += currLine.get(s) + '\t';
                                        } else {
                                            LOG.error("Possibly malformed" + "line,doesn't contain" + this.DOC_ID);
                                        }
                                    }
                                    if (currText != " ") {
                                        currText = currText.trim();
                                        docIDTextMap.put(currID, currText);
                                    }
                                }

                            } else {
                                LOG.error("Possibly malformed line," + " doesn't contain" + this.DOC_ID);
                            }
                        }
                    } finally {
                        if (reader != null) {
                            reader.close();
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Error reading original text file", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOG.error("Error closing original text file", e);
                }
            }
            if (fs != null) {
                fs.close();
            }
        }
    }

    private void loadPoints() throws IOException {
        SequenceFile.Reader currReader = null;
        IntWritable k = new IntWritable();
        CoCluster currCluster;
        int currVID;
        WeightedVectorWritable wvw = new WeightedVectorWritable();
        try {
            fs = FileSystem.get(clusteredPointsPath.toUri(), conf);
            for (FileStatus status : fs.listStatus(clusteredPointsPath)) {
                Path p = status.getPath();
                if (!status.isDir() && !p.getName().startsWith("_")) {
                    try {
                        currReader = new SequenceFile.Reader(fs, p, conf);
                        while (currReader.next(k, wvw)) {
                            currCluster = coclusters.get(k.get());
                            NamedVector v = (NamedVector) wvw.getVector();
                            currVID = Integer.parseInt(v.getName());
                            if (docIDMap.containsKey(currVID)) {
                                currCluster.put(v, docIDMap.get(currVID), true);
                            } else if (featureIDMap.containsKey(currVID)) {
                                currCluster.put(v, featureIDMap.get(currVID), false);
                            } else {
                                LOG.error("Key not feature or document!");
                            }
                        }
                    } finally {
                        if (currReader != null) {
                            IOUtils.closeStream(currReader);
                        }
                    }
                }
            }
        } catch (IOException ie) {
            LOG.info("Error while reading points", ie);
        } catch (ClassCastException ce) {
            LOG.info("NamedVectors possibly not used", ce);
        } finally {
            if (currReader != null) {
                IOUtils.closeStream(currReader);
            }
            if (fs != null) {
                fs.close();
            }
        }
    }

    public void writeResults() throws IOException {
        Map<String, Map<String, List<String>>> results = new LinkedHashMap<String, Map<String, List<String>>>();
        List<String> topDocList;
        List<String> topFeatureList;
        Integer currClusterID;
        Map<String, List<String>> currClusterMD;
        Set<String> allTopDocIDs = new HashSet<String>();
        for (Map.Entry<Integer, CoCluster> entry : coclusters.entrySet()) {
            // Extract top numResults features
            currClusterMD = new LinkedHashMap<String, List<String>>();
            currClusterID = entry.getKey();
            topDocList = entry.getValue().getDocuments(numResults);
            topFeatureList = entry.getValue().getFeatures(numResults);
            currClusterMD.put(TOP_DOCS, topDocList);
            currClusterMD.put(TOP_FEATURES, topFeatureList);
            results.put(currClusterID.toString(), currClusterMD);
            for (String s : topDocList) {
                allTopDocIDs.add(s);
            }
        }
        loadText(allTopDocIDs);
        String currText;
        for (Map.Entry<String, Map<String, List<String>>> c : results.entrySet()) {
            currClusterMD = c.getValue();
            topDocList = currClusterMD.get(TOP_DOCS);
            List<String> topDocTextList = new ArrayList<String>();
            for (String s : topDocList) {
                currText = s + "\t" + docIDTextMap.get(s);
                topDocTextList.add(currText);
            }
            currClusterMD.put(TOP_DOCS, topDocTextList);
        }
        ObjectMapper mapper = new ObjectMapper();
        FSDataOutputStream out = null;
        fs = FileSystem.get(conf);
        out = fs.create(resultsPath);
        out.write(mapper.writeValueAsBytes(results));
        if (fs != null) {
            fs.close();
        }
        if (out != null) {
            out.close();
        }
    }

    public void writeTags() throws IOException {
        Map<String, List<Integer>> tags = new LinkedHashMap<String, List<Integer>>();
        List<Integer> clusterIDList;
        List<String> currDocuments;
        int currNumDocs;
        Integer currClusterID;
        for (Map.Entry<Integer, CoCluster> entry : coclusters.entrySet()) {
            currClusterID = entry.getKey();
            currNumDocs = entry.getValue().getNumDocuments();
            currDocuments = entry.getValue().getDocuments(currNumDocs);
            for (String s : currDocuments) {
                clusterIDList = new ArrayList<Integer>();
                clusterIDList.add(currClusterID);
                tags.put(s, clusterIDList);
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        FSDataOutputStream out = null;
        fs = FileSystem.get(conf);
        out = fs.create(tagPath);
        out.write(mapper.writeValueAsBytes(tags));
        if (fs != null) {
            fs.close();
        }
        if (out != null) {
            out.close();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 12) {
            System.err.println("Usage: WriteCoClusteringOutput"
                    + "<clusteredPointsPath> <clustersPath> <docIDMap> <featureIDMap>"
                    + "<docIDTextMapPath> <tagPath> <resultsPath> <TOP_DOCS>"
                    + "<TOP_FEATURES> numResults <DOC_ID_KEY> <DOC_TEXT1,..,DOC_TEXTN>");
            System.exit(1);
        }
        String clusteredPointsPath = args[0];
        String clustersPath = args[1];
        String docIDMapPath = args[2];
        String featureIDMapPath = args[3];
        String docIDTextMapPath = args[4];
        String tagPath = args[5];
        String resultsPath = args[6];
        String TOP_DOCS = args[7];
        String TOP_FEATURES = args[8];
        int numResults = Integer.parseInt(args[9]);
        String DOC_ID = args[10];
        int ii = 0;
        String[] DOC_TEXT = new String[args.length - 11];
        while (ii + 11 < args.length) {
            DOC_TEXT[ii] = args[ii + 11];
            ii = ii + 1;
        }
        WriteCoClusteringOutput writer = new WriteCoClusteringOutput(clusteredPointsPath, clustersPath, docIDMapPath,
                featureIDMapPath, docIDTextMapPath, tagPath, resultsPath, TOP_DOCS, TOP_FEATURES, numResults, DOC_ID,
                DOC_TEXT);
        writer.initialize();
        writer.writeTags();
        writer.writeResults();
    }
}
