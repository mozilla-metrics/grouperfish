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
package com.mozilla.grouperfish.transforms.coclustering.pig.eval.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

public class ConvertDocumentIDToID extends EvalFunc<Integer> {

    private static TupleFactory tupleFactory = TupleFactory.getInstance();
    private Map<String, Integer> documentIndex;

    private void loadDocumentIndex(String documentIndexPath)
					throws IOException {
        if (documentIndex == null) {
            documentIndex = new HashMap<String, Integer>();

            Path p = new Path(documentIndexPath);
            FileSystem fs = FileSystem.get(p.toUri(), new Configuration());
            int index = 0;
            for (FileStatus status : fs.listStatus(p)) {
		Path currPath = status.getPath();
                if (!status.isDir() && !currPath.getName().startsWith("_")) {
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(
			    new InputStreamReader(fs.open(currPath)));
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            documentIndex.put(line.trim(), index++);
                        }
                    } finally {
                        if (reader != null) {
                            reader.close();
                        }
                    }
                }
            }

            log.info("Loaded document index with size: " + documentIndex.size());
        }
    }

    @Override
    public Integer exec(Tuple input) throws IOException {
        if (input == null || input.size() == 0) {
            return null;
        }
        if (input.size() != 2) {
            throw new IOException("ConvertDocumentIDToID requires 2 parameters");
        }

        String documentIndexPath = (String) input.get(0);
        if (documentIndex == null) {
            loadDocumentIndex(documentIndexPath);
        }
	String docID = (String) input.get(1);
	return documentIndex.get(docID);
    }
}
