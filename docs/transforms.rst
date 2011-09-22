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

Currently, a transform configuration is a JSON document with two fields: The
*transform* determines which piece of software to use, and *parameters* tells
that software what to do.
Example configuration for the *textcluster* transform:

::

    {
        "transform": "textcluster",
        "parameters": {
            "fields": {"id": "id", "text": "text"},
            "limits": {"clusters": 10,"top_documents": 10}
        }
    }


Result Types
------------

Topics (or Clusters)
^^^^^^^^^^^^^^^^^^^^

Clustering transforms try to extract the main topics from a set of documents.
As of Grouperfish version 0.1, the only available transform is a clustering
transform named textcluster. The results of clustering transform are topics,
the structure of the result is as follows:

::

    {
        "clusters": [
            {
                "top_documents": [{...}, {...}, ..., {...}],
                "top_terms": ["Something", "Else", ..., "Another"]
            },
            ...
        ]
    }

Depending on the actually configured transform, only top documents *or* top
terms might be generated for a topic. Also, any given transform might add
other top-level fields than just *clusters*.


Available Transforms
--------------------

textcluster
^^^^^^^^^^^

Textcluster is a relatively simple clustering algorithm written in Python by
Dave Dash for Firefox Input. It is very fast for small input sets, but
requires a lot of memory, especially when processing more than 10,000
documents at a time. Textcluster is `available on github`__.

.. __: https://github.com/davedash/textcluster

In Grouperfish, you can select how many topics you want textcluster to
extract, and how many documents to include in the results for each topic.

* Parameters

  ::

      {
          "fields": {
              "id": "id",
              "text": "text"
          },
          "limits": {
              "clusters": 10,
              "top_documents": 10
          }
      }

  These are the default parameters (top 10 topics/clusters,
  with 10 documents each).


* Results

  Textcluster uses the standard clustering result format (see above), but does
  not inclue top terms, only documents.

