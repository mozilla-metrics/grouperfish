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
package com.mozilla.grouperfish.transforms.coclustering.pig.storage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.log4j.Logger;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.SequentialAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.clustering.WeightedVectorWritable;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.LoadFunc;
/**
 * KMeansOutputLoader loads Mahout WeightedWeightedVectorWritables and stores
 * cluster IDs and named vectors as bag.
 **/

public class KMeansOutputLoader extends LoadFunc{
    @SuppressWarnings("rawtypes")
    protected RecordReader reader = null;
    protected BagFactory bagFactory = BagFactory.getInstance();
    protected TupleFactory tupleFactory = TupleFactory.getInstance();

    private static final Logger LOG =
			    Logger.getLogger(KMeansOutputLoader.class);

    public KMeansOutputLoader(){
    }
    @Override
    public InputFormat getInputFormat() throws IOException {
	return new SequenceFileInputFormat<IntWritable, WeightedVectorWritable>();
    }
    @Override
    public void setLocation(String location, Job job) throws IOException {
	FileInputFormat.setInputPaths(job,location);
    }
    @SuppressWarnings("unchecked")
    @Override
    public void prepareToRead(RecordReader reader, PigSplit split){
	this.reader = reader;
    }
    @Override
    public Tuple getNext() throws IOException {
	try {
	    if (!this.reader.nextKeyValue()){
		return null;
	    }
	    Tuple currRow = tupleFactory.newTuple(3);
	    DataBag rowInfoBag = bagFactory.newDefaultBag();
	    IntWritable key = (IntWritable)reader.getCurrentKey();
	    int clusterID = key.get();
	    WeightedVectorWritable value = (WeightedVectorWritable)
							reader.getCurrentValue();
	    Vector rowInfo = value.getVector();
	    NamedVector nrowInfo = (NamedVector) rowInfo;
	    int vectorID = Integer.parseInt(nrowInfo.getName());
	    for (Iterator<Vector.Element> itr = rowInfo.iterateNonZero();
		    itr.hasNext();){
		Vector.Element elemInfo = itr.next();
		Tuple currElement = tupleFactory.newTuple(2);
		currElement.set(0,elemInfo.index());
		currElement.set(1,elemInfo.get());
		rowInfoBag.add(currElement);
	    }
	    currRow.set(0,clusterID);
	    currRow.set(1,vectorID);
	    currRow.set(2,rowInfoBag);
	    return currRow;
	} catch (InterruptedException ie){
	    LOG.error("Interrupted while reading", ie);
	    throw new IOException(ie);
	}
	catch (NumberFormatException ne){
	    LOG.error("Possible use of non int values for NamedVector keys",ne);
	    throw new IOException(ne);
	}
	catch (ClassCastException e){
	    LOG.error("Possible cast of normal Vector to NamedVector",e);
	    throw new IOException(e);
	}
    }
   }


