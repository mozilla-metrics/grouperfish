Architecture
============

Grouperfish consists of several components which operate somewhat isolated from each other.


The REST Service
----------------

All users of the Grouperfish system interact exclusively with this service.  It exposes a REST api to producers for depositing documents, as well as an api for consumers, to extract results.  The REST service also defines APIs for admin users, to configure which sets of documents to process (using *queries*), and what to do with them (by configuring *transforms*).


**Namespaces**

So that multiple users can each work with their own set of documents and transforms, without affecting one nother, the concept of *namespaces* is used throughout all parts of grouperfish. To a given user, each namespace represents its own complete system, independent of any other namespaces.  Just to illustrate: an individual namespace relates to its Grouperfish installation like

* a database to a MySQL server

* a search index to an ElasticSearch cluster


**Filters**
Incoming documents might require post-processing to make them usable in transforms. An example of this would be language detection: Documents of the same language should be clustered together.

**Filter Configuration**
For a filter to be used for a namespace, it requires a corresponding piece of configuration. For the language detection code, this configuration might tell the algorithm on which field the detection should be based (e.g. *text* or *description*)


Storage
-------

To users of the REST service, as well as to the actual algorithms, this component is invisible.  It stores everything that is PUT into grouperfish, and makes it available for retrieval.  Under the hood, the Bagheera component is used to manage storage, indexing and caching.


The Batch System
----------------

The processing of document is kicked off by POST-ing to a special REST URL (e.g. triggered by cron or a client system).  This triggers a *batch run*.  But how does the batch system know which documents to process, and what transforms to apply?


**Queries**

*Queries* help the batch system to determine which documents to process.  In Grouperfish, a query is represented as a JSON-document that conforms to the ElasticSearch `Query DSL`_.  Internally, all stored documents are indexed by ElasticSearch, and so each query is actually processed by ElasticSearch itself, rather than some component in Grouperfish.  This ensures that the full power of ElasticSearch can be tapped by the clients.

.. _`Query DSL`: http://www.elasticsearch.org/guide/reference/query-dsl/


**Transforms**

*Transforms* are programs that operate on a set of documents to generate a result such as clusters, trends, statistics and so on.  They can be implemented in arbitrary technologies and programming languages, e.g. using the hadoop Map/Reduce, as long as they can be setup and executed on a Unix-like platform.

Other than the batch system and the REST service, transforms are not aware of things such as queries or namespaces. They act only based on data that is immediately presented to them by the system.


**Transform Configurations**

The same transform (e.g. a clustering algorithm) might be used with different parameters to generate different results.  For this reason, the system contains a *transform configurations* for each result that should be generated.

Primarily, a transform configuration serves to parameterize the associated transform (e.g. for clustering, it might specify the desired number of clusters). It can also be used to tell the  Grouperfish batch system how to interact with a transform.


**The Batch Process**

When the batch process is triggered for a namespace:

* The batch system retrieves all queries and all transform configurations that are defined within the namespace.

* The system uses the first query to get all matching documents from ElasticSearch. It exports this first set of documents into HDFS for the transform to use (*TODO: Only HDFS? --- can't we also configure things to run locally when hadoop is not available/needed?*).

* The system also puts the transform parameters (from the transform configuration) into a place where the transform can use them.

* Next, the transform is launched and it receives the location of the source documents and of the configuration as a parameter. If it succeeds, the generated result is put into storage.

* These steps are repeated for all other combinations of query and transform configuration.
