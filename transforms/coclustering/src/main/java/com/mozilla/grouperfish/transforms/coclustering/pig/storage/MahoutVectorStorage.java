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
import org.apache.mahout.math.VectorWritable;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.LoadFunc;
import org.apache.pig.ResourceSchema;
import org.apache.pig.StoreFuncInterface;

public class MahoutVectorStorage extends LoadFunc
    implements StoreFuncInterface{
    @SuppressWarnings("rawtypes")
    protected RecordReader reader = null;
    @SuppressWarnings("rawtypes")
    protected RecordWriter writer = null;

    protected BagFactory bagFactory = BagFactory.getInstance();
    protected TupleFactory tupleFactory = TupleFactory.getInstance();
    protected boolean STORE_AS_DENSE;
    protected boolean STORE_AS_SEQUENTIAL;
    protected final String dimensionPath;
    protected int dimensions;
    private static final Logger LOG =
			    Logger.getLogger(MahoutVectorStorage.class);

    /**
     * Invoking the default constructor during Storage will lead to
     * IllegalArgumentException being thrown during PutNext() method.
     */
    public MahoutVectorStorage(){
	this.dimensionPath = null;
    }

    /**
     * This constructor should be called only during Storage of Vectors. It is
     * only in this case that it makes sense.
     */
    public MahoutVectorStorage(String dimensionPath, String isDense,
						String isSeq){
	this.dimensionPath = dimensionPath;
	this.STORE_AS_DENSE = Boolean.parseBoolean(isDense);
	this.STORE_AS_SEQUENTIAL = Boolean.parseBoolean(isSeq);
    }

    @Override
    public InputFormat getInputFormat() throws IOException {
	return new SequenceFileInputFormat<IntWritable, VectorWritable>();
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
	    Tuple currRow = tupleFactory.newTuple(2);
	    DataBag rowInfoBag = bagFactory.newDefaultBag();
	    IntWritable key = (IntWritable)reader.getCurrentKey();
	    int rowID = key.get();
	    VectorWritable value = (VectorWritable)reader.getCurrentValue();
	    Vector rowInfo = value.get();
	    if (rowInfo instanceof NamedVector){
		NamedVector nrowInfo = (NamedVector) rowInfo;
		rowID = Integer.parseInt(nrowInfo.getName());
	    }
	    for (Iterator<Vector.Element> itr = rowInfo.iterateNonZero();
		    itr.hasNext();){
		Vector.Element elemInfo = itr.next();
		Tuple currElement = tupleFactory.newTuple(2);
		currElement.set(0,elemInfo.index());
		currElement.set(1,elemInfo.get());
		rowInfoBag.add(currElement);
	    }
	    currRow.set(0,rowID);
	    currRow.set(1,rowInfoBag);
	    return currRow;
	} catch (InterruptedException ie){
	    LOG.error("Interrupted while reading", ie);
	    throw new IOException(ie);
	}
	catch (NumberFormatException ne){
	    LOG.error("Possible use of non int values for NamedVector keys",ne);
	    throw new IOException(ne);
	}
    }
    // Methods taken from StoreFunc
    @Override
    public void checkSchema(ResourceSchema s) throws IOException{
	// No-op because StoreFunc has no-op
    }
    @Override
    public String relToAbsPathForStoreLocation(String location, Path curDir)
    throws IOException {
        return LoadFunc.getAbsolutePath(location, curDir);
    }
    @Override
    public void cleanupOnFailure(String location, Job job)
    throws IOException {
        cleanupOnFailureImpl(location, job);
    }
    public static void cleanupOnFailureImpl(String location, Job job)
    throws IOException {
        Path path = new Path(location);
        FileSystem fs = path.getFileSystem(job.getConfiguration());
        if(fs.exists(path)){
            fs.delete(path, true);
        }
    }
    @Override
    public void setStoreFuncUDFContextSignature(String signature) {
        // default implementation is a no-op
    }
    @Override
    public OutputFormat getOutputFormat() throws IOException {
	return new SequenceFileOutputFormat<IntWritable, VectorWritable>();
    }
    @Override
    public void setStoreLocation(String location, Job job) throws IOException {
	job.setOutputKeyClass(IntWritable.class);
	job.setOutputValueClass(VectorWritable.class);
	FileOutputFormat.setOutputPath(job, new Path(location));
    }
    @Override
    public void prepareToWrite(RecordWriter writer) throws IOException{
	if(dimensionPath != null){
	    Path p = new Path(dimensionPath);
	    FileSystem fs = FileSystem.get(p.toUri(), new Configuration());
	    for (FileStatus status: fs.listStatus(p)){
		Path currPath = status.getPath();
		if (!status.isDir() && !currPath.getName().startsWith("_")){
		    BufferedReader reader = null;
		    try {
			reader = new BufferedReader(new InputStreamReader(
				    fs.open(currPath)));
			String line = reader.readLine();
			this.dimensions = Integer.parseInt(line);
		    } catch(NumberFormatException nfe){
			LOG.error("Unexpected input for dimensions", nfe);
			throw new IOException();
		    } finally {
			if (reader != null){
			    reader.close();
			}
			break;
		    }
		}
	    }
	}
	this.writer = writer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void putNext(Tuple t) throws IOException{
	IntWritable outputKey = new IntWritable();
	VectorWritable outputValue = new VectorWritable();
	outputKey.set((Integer)t.get(0));
	Tuple currRow = (Tuple)t.get(1);
	Vector currRowVector;
	if (dimensions == 0){
	    throw new IllegalArgumentException
					("Trying to create 0 dimension vector");
	}
	if (STORE_AS_DENSE){
	    currRowVector = new NamedVector(new DenseVector(dimensions),
					    outputKey.toString());
	}
	else if (STORE_AS_SEQUENTIAL){
	    currRowVector = new NamedVector(new SequentialAccessSparseVector(
					dimensions, currRow.size()),
					    outputKey.toString());
	}
	else{
	    currRowVector = new NamedVector(new RandomAccessSparseVector(
					dimensions, currRow.size()),
					    outputKey.toString());
	}
	for (int ii = 0; ii < currRow.size();ii ++){
	    Object o = currRow.get(ii);
	    switch (currRow.getType(ii)) {
		case DataType.INTEGER:
		case DataType.LONG:
		case DataType.FLOAT:
		case DataType.DOUBLE:
		    currRowVector.set(ii, (Double)o);
		    break;
		case DataType.TUPLE:
		    // If this is a tuple then we want to set column and element
		    Tuple subt = (Tuple)o;
		    currRowVector.set((Integer)subt.get(0), (Double)subt.get(1));
		    break;
		default:
		    throw new RuntimeException("Unexpected tuple form");
	    }
	}
	outputValue.set(currRowVector);
	try {
	    writer.write(outputKey, outputValue);
	} catch (InterruptedException e) {
	    LOG.error("Interrupted while writing", e);
	}
    }
}


