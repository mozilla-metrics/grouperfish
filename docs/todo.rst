.. _todo:

=======
To Do
=======

These components are not necessarily listed in the order they need to be
implemented:

* Bare bones REST Service (as defined in the section, see :ref:`rest_api`)

  * Filtering functionality (:ref:`filters`)

  * Language detection filter

  * Allow clients to extract sub-results from a result doc (using JSON paths)

* Remaining documentation

* Bare bones :ref:`batch_system`

  * Add template Queries

  * Add tagging of ElasticSearch documents

* :ref:`Transforms`

  * Co-Clustering

  * LDA

* Validate configuration pieces based on the ``parameters.json``
  (a schema provided by each transform)

* JS client library (possibly hook in with ``pyes``)
  E.g. to be used by the admin interface.

* Admin interface

* Python client library (possibly hook in with ``pyes``)

* Online service

  * Define online API (Client/server? JVM using Jython etc.?)

  * Implement an online clustering algorithm
