.. _usage:

=====
Usage
=====

Having started up at least one Grouperfish node, these examples should get you
started with using the REST api to add documents, queries and transforms, and
to retrieve results.

Throughout the examples, we are using the ``input`` namespace because we are
dealing with `Firefox Input`_ style data. Keep in mind that this is just an
arbitrary identifier that has no further meaning by itself.

.. _`Firefox Input`: http://input.mozilla.org

Add individual documents
------------------------

To add a document, use the ``documents`` resource:

::

    > curl -XPUT "http://localhost:61732/documents/input/123456789" \
         -H "Content-Type: application/json" \
         -d '
    {
        "id": "123456789",
        "product": "firefox",
        "timestamp": "1314781147",
        "platform": "win7",
        "text": "This is an interesting test document.",
        "manufacturer": "",
        "locale": "id",
        "device": "",
        "type": "issue",
        "url": "",
        "version": "6.0"
    }'

Verify the success of your operation by getting the document right back:

::

    > curl -XGET "http://localhost:61732/documents/input/123456789"


Batch-load a large number of documents
--------------------------------------

Grouperfish only becomes really interesting if you use it with a larger
number of documents. There is a tool that allows you to load the entire input
data set.

::

    grouperfish-0.1> cd tools/firefox_input
    firefox_input> curl -XGET http://input.mozilla.com/data/opinions.tsv.bz2 \
                        -o opinions.tsv.bz2
    firefox_input> cat opinions.tsv.bz2 | bunzip2 | ./load_opinions input

This will run a parallel import of Firefox user feedback data into your
Grouperfish cluster (specifically, into the ``input`` namespace).
Depending on your hardware, this should take between 5 and 25 minutes.


Add a query and a transform configuration
-----------------------------------------

Now that we have added a couple of million documents, we need to determine
which subset to select, and what to do with the selected documents:

::

    curl -XPUT "http://localhost:61732/queries/input/myQ" \
        -H "Content-Type: application/json" \
        -d '
    {
        "query": {
            "query_string": {
                "query": "version:6.0 AND platform:win7 AND type:issue"
            }
        }
    }'

    curl -XPUT "http://localhost:61732/configurations/input/transforms/myT" \
        -H "Content-Type: application/json" \
        -d '
    {
        "transform": "textcluster",
        "parameters": {
            "fields": {
                "id": "id",
                "text": "text"
            },
            "limits": {
                "clusters": 10,
                "top_documents": 10
            }
        }

    }'

Now we have a named query (*myQ*), which selects Firefox 6 issues from Windows
7 users, and a named transform configuration (*myT*). The query is in
ElasticSearch QueryDSL syntax which (using a lucene query string). The
configured transform is *textcluster*, and it is configured for the top 10
topics, with the top 10 messages each.


Run the transform
-----------------

::

    curl -XPOST "http://localhost:61732/run/input/myT/myQ"

This fetches everything matching *myQ* (about 20 thousand documents) and
invokes textcluster on them (this takes a couple of seconds).


Get the results
---------------

Getting the transform result works fairly similarly:

::

    curl -XGET "http://localhost:61732/results/input/myT/myQ"

Results can be fairly large since they contain the full top-documents for each
cluster. A tool such as the `JSONView`__ Firefox addon can be used to browse
results more comfortably.

.. __: https://addons.mozilla.org/en-US/firefox/addon/jsonview/
