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

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.math.Vector;

import com.mozilla.util.Pair;

public class CoCluster {

    private Vector centroid;
    private DistanceMeasure currDistanceMeasure;
    private PriorityQueue<Pair<Double, String>> documents;
    private PriorityQueue<Pair<Double, String>> features;

    public CoCluster(Vector centroid, DistanceMeasure distanceMeasure) {
        this.centroid = centroid;
        this.currDistanceMeasure = distanceMeasure;
        documents = new PriorityQueue<Pair<Double, String>>();
        features = new PriorityQueue<Pair<Double, String>>();
    }

    public void put(Vector v, String ID, boolean isDocument) {
        double distance = currDistanceMeasure.distance(v, centroid);
        if (isDocument) {
            documents.add(new Pair<Double, String>(distance, ID));
        } else {
            features.add(new Pair<Double, String>(distance, ID));
        }
    }

    public int getNumDocuments() {
        return documents.size();
    }

    public int getNumFeatures() {
        return features.size();
    }

    public List<String> getDocuments(int numDocuments) {
        List<String> documentSubset = new ArrayList<String>(numDocuments);
        List<Pair<Double, String>> buffer = new ArrayList<Pair<Double, String>>(numDocuments);
        int ii = 0;
        Pair<Double, String> currPair = null;
        while ((ii < numDocuments) && (documents.peek() != null)) {
            currPair = documents.poll();
            documentSubset.add(currPair.getSecond());
            ii = ii + 1;
            buffer.add(currPair);
        }
        for (Pair<Double, String> p : buffer) {
            documents.add(p);
        }
        return documentSubset;
    }

    public List<String> getFeatures(int numFeatures) {
        List<String> featureSubset = new ArrayList<String>(numFeatures);
        List<Pair<Double, String>> buffer = new ArrayList<Pair<Double, String>>(numFeatures);
        int ii = 0;
        Pair<Double, String> currPair = null;
        while ((ii < numFeatures) && (features.peek() != null)) {
            currPair = features.poll();
            featureSubset.add(currPair.getSecond());
            ii = ii + 1;
            buffer.add(currPair);
        }
        for (@SuppressWarnings("unused")
        Pair<Double, String> p : buffer) {
            // TODO: SMELLY: Should this not be "features.add(p)"? (see
            // suppressed warning)
            features.add(currPair);
        }
        return featureSubset;
    }
}
