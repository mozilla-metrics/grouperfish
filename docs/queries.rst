.. _queries:

=======
Queries
=======

Concrete Queries
----------------

A concrete query is just a regular ElasticSearch query, e.g.:

::

    {
      "query": {
        "bool": {
          "must": [
            {"field": {"os": "Android"}},
            {"field": {"platform": "ARM"}},
          ]
        }
      }
    }

All documents matching this query will be processed together in a batch run.

.. note::
   Find the full `Query DSL documentation`_ on the ElasticSearch Website.

.. _`Query DSL documentation`:
   http://www.elasticsearch.org/guide/reference/query-dsl/


.. _template-queries:


Template Queries
----------------

A template query will generate a bunch of concrete queries everytime it is
evaluated. It is different in that it has an additional top-level field
"facet_by", which is a list of field names.

Let us assume we have these documents in our namespace:

::

    {"id": 1, "desc": "Why do you crash?", "os": "win7", "platform": "x64"},
    {"id": 2, "desc": "Don't crash plz", "os": "xp", "platform": "x86"},
    {"id": 3, "desc": "It doesn't crash!", "os": "win7", "platform": "x86"},
    {"id": 3, "desc": "Over 9000!", "os": "linux", "platform": "x86"},


And this template query:

::

    {
      "query": {"text": {"desc": "crash"}},
      "facet_by": ["platform", "os"]
    }


This will generate the following set of queries:

::

    {"query": {"filtered":
        {"query": {"text": {"desc": "crash"}}, "filter": {"and": [
            {"field": {"os": "win7"}},
            {"field": {"platform": "x64"}},
    ]}}}}
    {"query": {"filtered":
        {"query": {"text": {"desc": "crash"}}, "filter": {"and": [
            {"field": {"os": "win7"}},
            {"field": {"platform": "x86"}},
    ]}}}}
    {"query": {"filtered":
        {"query": {"text": {"desc": "crash"}}, "filter": {"and": [
            {"field": {"os": "xp"}},
            {"field": {"platform": "x86"}},
    ]}}}}

Note that no query for ``os=linux`` is generated in this case, because the
query for ``crash`` does not match any document with that ``os`` in the first
place.
