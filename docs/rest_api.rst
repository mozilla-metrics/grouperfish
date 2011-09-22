.. _rest_api:

========
Rest API
========

This is a somewhat formal specification of the Grouperfish REST api. Look at
the :ref:`usage` chapter for specific examples.


Primer
------

The REST architecture talks about *resources*, *entities* and *methods*:

* In grouperfish, each *entity* (*document*, *result*, *query*,
  *configuration*) is represented as a piece of JSON.

* All entities are JSON documents, so the request/response Content-Type is
  always ``application/json``.

* The *resources* listed here contain ``<placeholders>`` for the actual
  parameter values. Values can use any printable unicode character, but
  URL-syntax (``?#=+/`` etc.) must be escaped properly.

* Most resources in Grouperfish allow for several HTTP *methods* to
  create/update (``PUT``), read (``GET``), or ``DELETE`` entities.
  Where allowed, resources respond like this to these methods:

  ``PUT``
      The request body contains the entity to be stored.
      Response status is always ``201 Created`` on success. The response
      status does not allow to determine if an existing resource was
      overridden.

  ``GET``
      Status is either ``200 OK`` accompanied with the requested entity in the
      response body, or ``404 Not Found`` if the entity name is not used.

  ``DELETE``
      Response code is always ``204 No Content``. No information is given on
      wether the resource existed before deletion.



For Document Producers
----------------------

Users that push documents can have a very limited view of the system.
They may see it just as a sink for their data.


Documents
^^^^^^^^^

============ =================================================================
Resource     ``/documents/<ns>/<document-id>``
============ =================================================================
Entity type  *document*
             e.g. ``{"id": 17, "fooness": "Over 9000", "tags": ["ba", "fu"]}``
Methods      ``PUT``, ``GET``
============ =================================================================

This allows to add documents, and also to look them up later.

It may take some time (depending on system configurations: seconds to
minutes) for documents to become indexed and thus visible to the batch processing system.



For Result Consumers
--------------------

Users that are (also) interested in getting results need to know about
queries, because each result is identified using the source query name. They
might even specify queries on which batch transformation should be performed.


Queries
^^^^^^^

A query is either an *concrete query* in ElasticSearch Query DSL, or a *template query*.


.. _`ElasticSearch Query DSL`:
   http://www.elasticsearch.org/guide/reference/query-dsl/

============ =================================================================
Resource     ``/queries/<ns>/<query-name>``
============ =================================================================
Entity type  *query*
             e.g. ``{"prefix": {"fooness": "Ove"}}``
Methods      ``PUT``, ``GET``, ``DELETE``
============ =================================================================

After a ``PUT``, when batch processing is performed on this namespace for the
next time, documents matching the query will be processed for each configured
transform.

The result can then be retrieved using ``GET /results/<ns>/<query-name>``.

To submit a template query, nest a normal query in a map like this:

::

    curl -XPUT /queries/mike/myQ -d '{
        "facet_by": ["product", "version"],
        "query": {"match_all": {}}
    }'

.. seealso:: :ref:`queries`


Results
^^^^^^^

For each combination of ES query and transform configuration, a result is put
into storage during the batch run.

============ =================================================================
Resource     ``/results/<ns>/<transform-name>/<query-name>``
============ =================================================================
Entity type  *result*
             e.g. ``{"output": ..., "meta": {...}}``
Methods      ``GET``
============ =================================================================

Return the last transform result for a combination of transform/query.
If no such result has been generated yet, return ``404 Not Found``.

To retrieve results for template queries, you need to specify actual values
for your facets. Just add the ``facets`` parameter to your get requests,
containing a ``key:value`` pair for each facet. Assuming the query
given in the previous example has been stored in the system, along with a
transform configuration named *themes*, you can get results like this:

::

    curl -XGET /results/mike/themes/myQ?facets=product%3AFirefox%20version%3A5

What exactly a result looks like is specific to the transform. See
:ref:`transforms` for details.


For Admins
----------

There is a number of administrative APIs that can either be triggered by
scripts (e.g. using ``curl``), or using the admin web UI.


Configuration
^^^^^^^^^^^^^

To use a filter for incoming documents, or a transform in the batch process,
a named piece of configuration needs to be added to the system.

============ =================================================================
Resource     ``/configuration/<ns>/<type>/<name>``
============ =================================================================
Entity type  ``configuration``
             e.g. ``{"transform": "LDA", "parameters": {"k": 3, ...}}``
Methods      ``PUT``, ``GET``, ``DELETE``
============ =================================================================

Type is currently one of ``"transform"`` and ``"filter"``.

.. note:: Filters are not yet available as of Grouperfish 0.1

.. seealso:: :ref:`transforms`, :ref:`filters`


Batch Runs
^^^^^^^^^^

Batch runs can be kicked off using the REST API as well.

============ =================================================================
Resource     ``/run/<ns>/<transform-name>/<query-name>``
============ =================================================================
Entity Type  N/A
Methods      ``POST``
============ =================================================================

Either transform name, or both query and transform name can be omitted to
run all transforms on the given query, or on all queries in the namespace.

If a batch run is already executing, this run is postponed.

The response status is ``202 Accepted`` if the run was scheduled, or ``404 Not
Found`` if either query or transform of the given names do not exist.

.. seealso:: :ref:`batch_system`


