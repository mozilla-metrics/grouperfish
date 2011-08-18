1. Copy all pig scripts to a directory of your choice. This directory will also
be used as local directory for the scripts.
2. Copy src/main/python/cocluster.py to this directory.
3. Copy the bash script ``cocluster`` to this directory.
4. Ensure you have Python 2.6 with ``python26 `` the command to invoke python
2.6.
5. Do mvn package and mvn assembly:assembly in the transforms/coclustering to
generate two JARS in the target folder.
6. Create a lib directory in your directory of choice that contains
    A. lucene-analyzers-3.1.0.jar
    B. lucene-core-3.1.0.jar
    C. akela-0.2-SNAPSHOT.jar
    D. grouperfish-transforms-coclustering-0.3-SNAPSHOT.jar
    E. mahout-collections-1.0.jar (Patched for CDH3 u0)
    F. mahout-core-0.5.jar (Patched for CDH3 u0)
    G. mahout-examples-0.5-job.jar (Patched for CDH3 u0)
    H. mahout-math-0.5.jar  (Patched for CDH3 u0)
7. Copy grouperfish-transforms-coclustering-0.3-SNAPSHOT-job.jar to top level of
the directory of your choice.
8. Create an HDFS directory that contains:
    A. input.TSV (Refer readthedocs for format )
    B. parameters.json (Refer this directory for example file)
9. Do ./cocluster <HDFS directory name>
10. You will find results.json and tags.json inside the HDFS directory.

Contact evijayakumar@mozilla.com for any questions.

