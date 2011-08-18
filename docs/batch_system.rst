.. _batch_system:

============
Batch System
============

Batch runs are launched by a post request to a ``/runs`` resource, as
described in the section :ref:`rest_api`.


Batch Operation
---------------

The Batch System performs these steps for every batch run:

1. **Get queries to process**

   * If a query was specified when starting the run:

     Fetch that one query.

   * Otherwise:

     I.   Fetch all concrete queries for this namespace

     II.  Fetch all template queries for this namespace

     III. Resolve the template queries (see :ref:`template-queries`)

          Add the results to the concrete queries obtained in (I)


2. **Get transform configurations to use**

   * If a transform configuration was specified in the POST request:

     Fetch that one transform

   * Otherwise:

     Fetch *all* transform configurations for this namespace


3. **Run the transforms**

   For each concrete query

   I.   Get the matching documents

   II.  Write them to some ``hdfs://`` directory

   III. Call the transform executable with that directory's path
        (see :ref:`transform-api`)

   IV.  Tag documents in ElasticSearch
        (if the transform has generated tags, see :ref:`tagging`)

   V.   POST the results summary document to the rest service.

        From this point it will be served to consumers.


.. _transform-api:

The Transform API
-----------------

Each batch transform is implemented as an executable that is invoked by the
system. This allows for a maximum of flexibility (free choice of programming
language and library dependencies).

Directory Layout
^^^^^^^^^^^^^^^^

All transforms have to conform to a specific directory layout. This example
shows the fictitious ``bozo-cluster`` transform.

``grouperfish``

* ``transforms``

  * ``bozo-cluster``

    * ``bozo-cluster*``

    * ``install*``

    * ``parameters.json``

    * ...

  * ``lda``

    * ...

  * ...

To be recognized by grouperfish, there has to be both an ``install`` script,
and (possibly only after ``install`` script has been called), the executable
itself. The executable must have the same name as the directory.
Third, there should be a file ``parameters.json``, containing the possible
parameters for this transform.


Invocation
^^^^^^^^^^

Each transform is invoked like with  HDFS path (in URL syntax) as the first
argument, something like ``hdfs://namenode.com/grouperfish/..../workdir``.

Here are the contents of this working directory when the transform is started:

* ``input.tsv``

  Each line (delimited by ``LF``) of this ``UTF-8`` coded file contains two
  columns, separated by ``TAB``:

  1. The ID of a document to process (as a base10 string)

  2. The full document as JSON, on the same line:
     Any line breaks within strings are escaped. Apart from formatting, this
     document is the same that the user submitted originally.

  Example:

  ::

      4815162342``  ``{"id":"4815162342", "text": "some\ntext"}
      4815162343``  ``{"id":"4815162343", "text": "moar text"}
      ...


* ``parameters.json``

  The parameters section from the transform configuration. This corresponds to
  the possible parameters from the transform home directory.

  Example:

  ::
    {
     "text" : {
                "STOPWORDS" : [ "the", "cat" ]
                "STEM": "false",
        	"MIN_WORD_LEN": "2",
        	"MIN_DF": "1",
        	"MAX_DF_PERCENT": "0.99",
        	"DOC_COL_ID" : "id",
        	"TEXT_COL_ID" : "text"
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
                "KMEANS_DISTANCE" : "CosineDistanceMeasure"
               }
    }



When the transform succeeds, it produces these outputs in addition:

* ``output/results.json``

  This JSON documents will be visible to the result consumers through the REST
  interface. It should contain all major results that the transform generates.

  The batch system will add a ``meta`` map before storing the result,
  containing the name of the transform configuration (``transform``), the date
  (``date``), the query (``query``), and the number of input documents
  (``input_size``).

  The transform is also allowed to create the ``meta`` map, to add
  transform-specific diagnostics.

* ``output/tags.json`` (optional)

  The batch system will take this map from document IDs to tag names, and
  modify the documents in ElasticSearch, so they can be looked up using these
  labels. See :ref:`tagging` for details.

The transform should exit with status ``0`` on success, and ``1`` on failure.
Errors will be logged to standard error.


.. _tagging:

Tagging
-------

When an transform produces a ``tags.json`` as part of its result, the batch
system uses it to markup results in ElasticSearch. Transforms can output
cluster membership or classification results as tags, which will allow clients
to facet and scroll through the transform result using the full ElasticSearch
API.

A document with added tags looks like this:

::

    {
      "id": 12345,
      ...
      "grouperfish": {
        "my-query": {
          "my-transform": {
            "2012-12-21T00:00:00.000Z": ["tag-A", "tag-B"],
            ...
          }
        }
      }
    }

The timestamps are necessary because old tags become invalid when tagged
documents drop out of a result set (e.g. due to a date constraint). The
grouperfish API ensures that searches for results take the timestamp of the
last transform run into account.

.. note::
   This format is not finalized yet. We might use parent/child docs instead.
   Also, the necessary REST API that wraps ElasticSearch is not defined yet.
