.. _batch_system:

============
Batch System
============

Batch runs are launched by a post request to a ``/runs`` resource, as 
described in the section :ref:`rest_api`.


Batch Operation
---------------

The Batch System performs these steps for every batch run:

1. Get queries to process

   * If a query was specified when starting the run, fetch that one.

   * Otherwise

       I. Fetch all concrete queries for this namespace
     
      II. Fetch all template queries for this namespace
     
     III. Resolve the template queries (see below). 
          Add the reults to the concrete queries obtained in (I).


2. Get transform configurations to use

   * If a transform configuration was specified when starting, fetch that

   * Otherwise, fetch all transform configurations for this namespace


3. For each concrete query

     I. Get the matching documents

    II. Write them to some ``hdfs://`` directory

   III. Call the transform executable with that directory's path 
        (see :ref:`transform-api`)

    IV. Tag documents in ElasticSearch 
        (if the transform has generated tags, see :ref:`tagging`)

     V. POST the results summary document to the rest service


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
     Any line breaks within strings are ecaped. Apart from formatting, this 
     document is the same that the user submitted originally.

* ``parameters.json``

  The parameters section from the transform configuration. This corresponds to 
  the possible parameters from the transform home directory.


When the algorithm succeeds, it produces these outputs in addition:

* ``results.json``

  This JSON documents will be visible to the result consumers through the REST 
  interface. It should contain all major results that the algorithm generates.
  
  The batch system will add a ``meta`` map before storing the result,
  containing the name of the transform configuration (``transform``), the date
  (``date``), the query (``query``), and the number of input documents
  (``input_size``).
  
  The transform is also allowed to create the ``meta`` map, to add 
  transform-specific diagnostics.

* ``tags.json`` (optional)
  
  The batch system will take this map from document IDs to tag names, and 
  modify the documents in ElasticSearch, so they can be looked up using these 
  labels. See :ref:`tagging` for details.

The transform should exit with status ``0`` on success, and ``1`` on failure. 
In the error case, the transform should put an error description in the
``results.json``. If the algorithm cannot write the ``results.json`` (e.g. if
there is a problem with accessing HDFS) it must write the error message to
the standard error stream.


.. _tagging:

Tagging
-------

When an algorithm produces a ``tags.json`` as part of its result, the batch 
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
last algorithm run into account.

.. note::
   This format is not finalized yet.
   Also, the necessary REST API that wraps ElasticSearch is not defined yet.
