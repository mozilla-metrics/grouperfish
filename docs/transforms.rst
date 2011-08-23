.. _transforms:

==========
Transforms
==========

Transforms are the heart of Grouperfish. They generate the results that will
actually be interesting to consumers.

Note: The minimal transform interface is defined by the :ref:`batch_system`


Transform Configuration
-----------------------

The same transform (e.g. a clustering algorithm) might be used with different
parameters to generate different results.  For this reason, the system
contains a *transform configurations* for each result that should be
generated.

Primarily, a transform configuration parameterizes its transform (e.g. for
clustering, it might specify the desired number of clusters). It can also be
used to tell the  Grouperfish batch system how to interact with a transform.

TBD: Exact contents of a transform configuration


Result Types
------------

Clusters
^^^^^^^^

All unsupervised clustering results will guarantee the production of a
results.JSON object that will contain keys as cluster IDs and values as an
object that contains ``TOP_DOCS`` and ``TOP_FEATURES``.


Available Transforms
--------------------

Co-Clustering
^^^^^^^^^^^^^

* ``parameters.json``

::

    {
      "text" : {
        "STOPWORDS" : [ "the", "cat" ]
        "STEM": "false",
        "MIN_WORD_LEN": "2",
        "MIN_DF": "1",
        "MAX_DF_PERCENT": "0.99",
        "DOC_COL_ID": "id",
        "TEXT_COL_ID": "text"
      },
      "mapreduce":{
        "NUM_REDUCERS": "7"
      },
      "transform":{
        "KMEANS_NUM_CLUSTERS": "10",
        "KMEANS_NUM_ITERATIONS": "20",
        "SSVD_MULTIPLIER": "5",
        "SSVD_BLOCK_HEIGHT": "30000",
        "KMEANS_DELTA": "0.1",
        "KMEANS_DISTANCE": "CosineDistanceMeasure"
      }
    }


    * "text"

        - STOPWORDS : A list of stopwords to use.
        - STEM : "true" or "false" determines whether to use Porter Stemming or
                not
        - MIN_WORD_LEN : Any tokens of size less than this are discarded.
        - MIN_DF : Minimum allowed occurrences of a token in corpus
        - MAX_DF_PERCENT : Maximum number of occurrences of token in corpus.
        - DOC_COL_ID : Key in input.tsv which serves as identifier.
        - TEXT_COL_ID : Key in input.tsv that serves as text to be clustered.

    * "mapreduce"

        - NUM_REDUCERS : Number of reducers to be set in Pig/Hadoop/Mahout.

    * "transform"

        - KMEANS_NUM_CLUSTERS : Number of clusters to set. (Recommended < 30 )
        - KMEANS_NUM_ITERATIONS : Number of iterations to run KMeans part of
                    algorithms
        - SSVD_MULTIPLIER : Mahout parameter. (Recommend : don't change)
        - SSVD_BLOCK_HEIGHT : Mahout parameter. (Recommend : set at 30,000 )
        - KMEANS_DELTA : Relax to 0.5  - 0.8 for larger datasets.
        - KMEANS_DISTANCE : CosineDistanceMeasure OR EuclideanDistanceMeasure



* ``results.json``

  The results.json is an object that contains cluster ID as key and value as an
  object that contains keys ``TOP_DOCS`` and ``TOP_FEATURES`` and values as an
  array of top documents and top features respectively.

  Example:

::

    {
      "1": { "TOP_DOCS": ["48\t no flash", "32\t I love firefox"],
             "TOP_FEATURES": ["flash", "firefox" ] },
      ...
    }
    ...

* ``tags.json``

    Example:

::

    {
        "232": [ "13" ],
        "43": [ "32" ]
    }


References:
~~~~~~~~~~~
    1. Dhillon, I. (2001). Co-clustering documents and words using bipartite
       spectral graph partitioning. In Proceedings of the seventh ACM SIGKDD
       international conference on Knowledge discover aand data mining (KDD)
       (pp.269 – 274). New York: ACM Press.


LDA
^^^

Result Type
    Clusters

TBD: Describe how it works, link to papers, what parameters are available

