Architecture
============

Grouperfish consists of three independently functioning components.


.. _rest-service:

The REST Service
----------------

Consumers of Grouperfish interact exclusively with this service.
It exposes a RESTful API to insert documents and query results.
There are APIs for configuring which sets of documents to process (using
queries) and what to do with them (using transforms).

For example, you may want to create a query for all documents from January
and transform this set by clustering them together.


Namespaces
^^^^^^^^^^

So that multiple users (of a single Grouperfish cluster)
can each work with their own set of documents and
transforms, without affecting one another, the concept of *namespaces* is used
throughout all parts of Grouperfish.  Namespaces are similar to databases in
MySQL or indexes in ElasticSearch.


Filters
^^^^^^^

Incoming documents might require post-processing to make them usable in
transforms.
For example, you may want to do language detection so that you can
cluster documents in the same language together.


Storage
-------

The stoarge system stores everything
It stores everything that is inserted into Grouperfish and
makes it available for retrieval.
Under the hood, the Bagheera_ component is
used to manage storage, indexing and caching.

.. _Bagheera: https://github.com/mozilla-metrics/bagheera

The Batch System
----------------

The processing of document is kicked off by POST-ing to a special REST URL
(e.g. triggered by cron or a client system).
This triggers a *batch run*.
But how does the batch system know which documents to process,
and what transforms to apply?


Queries
^^^^^^^

Queries help the batch system determine which documents to process.
In Grouperfish, a query is represented as a JSON-document that conforms to the
ElasticSearch `Query DSL`_.
Internally, all stored documents are indexed by ElasticSearch,
and each query is actually processed by ElasticSearch itself.

.. _`Query DSL`: http://www.elasticsearch.org/guide/reference/query-dsl/


Transforms
^^^^^^^^^^

Transforms are programs that operate on a set of documents to
generate a result such as clusters, trends, statistics and so on.
They can be implemented in arbitrary technologies and programming languages,
e.g. using the hadoop Map/Reduce, as long as they can be setup and executed on
a Unix-like platform.

Other than the batch system and the REST service,
transforms are not aware of things such as queries or namespaces.
They act only based on data that is immediately presented to them by the
system.

.. seealso:: :ref:`transforms`


The Batch Process
^^^^^^^^^^^^^^^^^

When the batch process is triggered for a namespace:

* The batch system retrieves all queries and all transform configurations that
  are defined within the namespace.

* The system uses the first query to get all matching documents from
  ElasticSearch.
  It exports this first set of documents into HDFS for the
  transform to use (*TODO: Only HDFS? --- can't we also configure things to run
  locally when hadoop is not available/needed?*).

* The system also puts the transform parameters
  (from the transform configuration)
  into a place where the transform can use them.

* Finally,
  the transform is launched and receives the location of the source
  documents and of the configuration as a parameter.
  If it succeeds, the generated result is put into storage.

* These steps are repeated for all other combinations of query and transform
  configuration.
