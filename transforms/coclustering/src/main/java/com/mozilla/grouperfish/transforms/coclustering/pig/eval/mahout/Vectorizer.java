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
package com.mozilla.grouperfish.transforms.coclustering.pig.eval.mahout;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

public class Vectorizer extends EvalFunc<Tuple> {

    private static final TupleFactory tupleFactory =
	TupleFactory.getInstance();
    public Tuple exec(Tuple input) throws IOException {
	if (input == null) {
	    return null;
	}

	if (input.size() != 1) {
	    throw new IOException
		("Vectorizer requires exactly 1 parameter");
	}
	Tuple output = tupleFactory.newTuple();
	DataBag db = (DataBag)input.get(0);
	for (Tuple t : db) {
	    if (t.size() == 2) {
		Integer rowId = (Integer) t.get(0);
		if (rowId != null) {
		    Tuple subt = tupleFactory.newTuple(2);
		    subt.set(0, rowId);
		    subt.set(1, t.get(1));
		    output.append(subt);
		}
	    }
	}
	return output;
    }
}
