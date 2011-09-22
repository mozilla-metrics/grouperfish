package com.mozilla.grouperfish.pig.storage;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.log4j.Logger;
import org.apache.pig.StoreFunc;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;

public class VWStorage extends StoreFunc {

    private static final Logger LOG = Logger.getLogger(VWStorage.class);
    
    private static final String PIPE = "|";
    private static final String SPACE = " ";
    private static final String COLON = ":";
    
    @SuppressWarnings("rawtypes")
    protected RecordWriter writer = null;
    
    private Text outputKey = new Text();
    private NullWritable outputValue = NullWritable.get();
    
    public VWStorage() {
        super();
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public OutputFormat getOutputFormat() throws IOException {
        return new TextOutputFormat<LongWritable, Text>();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void prepareToWrite(RecordWriter writer) throws IOException {
        this.writer = writer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void putNext(Tuple tuple) throws IOException {
        String docId = (String)tuple.get(0);
        StringBuilder sb = new StringBuilder(docId + PIPE + SPACE);
        Tuple vectorTuple = (Tuple)tuple.get(1);
        int vectorSize = vectorTuple.size();
        for (int i=0; i < vectorSize; i++) {
            Object o = vectorTuple.get(i);
            switch (vectorTuple.getType(i)) {
                case DataType.INTEGER:
                    // If this is just an integer then we just want to set the index to 1.0
                    sb.append((Integer)o);
                    if ((i+1) < vectorSize) {
                        sb.append(SPACE);
                    }
                    break;
                case DataType.TUPLE:
                    // If this is a tuple then we want to set the index and the weight/frequency
                    Tuple subt = (Tuple)o;
                    sb.append((Integer)subt.get(0));
                    sb.append(COLON);
                    sb.append((Double)subt.get(1));
                    if ((i+1) < vectorSize) {
                        sb.append(SPACE);
                    }
                    break;
                default:
                    throw new RuntimeException("Unexpected tuple form");
            }
        }
        try {
            outputKey.set(sb.toString());
            writer.write(outputKey, outputValue);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while writing", e);
        }
    }

    @Override
    public void setStoreLocation(String location, Job job) throws IOException {
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);
        FileOutputFormat.setOutputPath(job, new Path(location));
    }

}
