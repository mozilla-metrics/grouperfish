# Grouperfish

#### A scalable text clustering service for the web

The nascent Grouperfish project aims to provide a simple, online, scalable text clustering solution as a REST/JSON service. Initially this service is needed to drive sites and themes for [Firefox Input](http://input.mozilla.com), as described in [bug 629019](https://bugzilla.mozilla.org/show_bug.cgi?id=629019). For distributed clustering we use an algorithm developed on top of [Apache Mahout](http://mahout.apache.org/).

The current status is that of a basic prototype that is used for continuous integration with the input team.

### Service Requirements

To clients, Grouperfish is an HTTP service exposing three REST API methods:

#### *add document*

    POST /collections/<namespace>/<collection-key>
    
    {"id": <doc-id>, "text": <textToCluster>}

Queues a document for clustering. If documents with the same collection-key exist already, the new document is clustered together with them. If the collection key does not exist yet, a new collection is created. The namespace serves as a general scope for collection key and document id. In later versions we’ll be able to manage permissions on this level, so that one Grouperfish installation can serve any number of clients.

A bulk load can be performed using a request body of the form

   {"bulk": [<doc-1>, <doc-2>, ..., <doc-n>]}

where the individual docs have the form of the single request (id with text). A good bulk load would be anywhere from 100 to 3,000 documents.

To do: Add new documents to existing clusters incrementally. 

#### *get all clusters*

    GET /clusters/<namespace>/<collection-key>

Fetches all *k* clusters associated with the given collection:

    {<cluster-label-1>: [<doc-id-1>, …, <doc-id-n>], …, <cluster-label-k>: […]}

The clusters consist only of their document ids. It is assumed that the client maintains the mapping to the original documents. 

To do: The cluster labels are supposed to be desciptive text labels, based on common features of the documents in the cluster.

#### *get individual cluster*

    GET /clusters/<namespace>/<collection-key>/<cluster-label>

Fetches only the cluster with the given label.

    [<doc-id-1>, <doc-id-2>, …, <doc-id-n>]


Note that cluster labels and document IDs are strings.

### Architecture Requirements

There is a [blog article on the architecture](http://www.thefoundation.de/michael/2011/mar/01/scalable-text-clustering/). We are considering changes, but the prototype version is consistent with the plans outlined in the document. 


## What's the status?

Development started Feb 20, 2011. The project is currently in the *early backyard stage* (aka 0.1), hoping to graduate to a basically usable *basement level* prototype (0.2) mid March.

Nevertheless, if you are interested in helping out or if you have your own ideas on how to do this: please contact us. Also, see the roadmap below.


## Components

* REST service: Store documents, deliver clusters

* Worker: Recompute clusters on a batch schedule (to do: incrementally)


Roadmap
-------

### 0.1 (Feb 20, 2011)
* A REST service that takes your documents and throws them away.
* It should return its favorite three clusters on every GET query.

### 0.2 (April, 2011)
* Working end-to-end process of storing docs and retrieving clusters: First implementation that can be used for input.
* Building of clusters, using a java port of Dave Dash’s textcluster
* Full initial build (or rebuild) of clusters from a TSV dump of the form: collection-key, document-id, text.
* Still serial processing (batch scheduling).
* Fixes for this prototype version are maintained on the `v0.2` branch.

### 0.3 (TBD)
* Incremental building of clusters ("like crystals in a water glass").
* …any number of workers

### 0.4 (TBD)
* Cache GETs using some LRU-based caching (redis?)
* Intelligent scheduling of full rebuilds. The more active collections (for input that means: latest version of Firefox, latest broken websites) need to be reprocessed more often.

### 1.0 (TBD)
* Web frontend for introspection of collections and clusters.
* Publish whenever clusters have changed (AMQP).



Setup
-----

The REST web service (Node.JS based) currently resides in the grouper-rest process. It is being replaces by a java-based service within the main project.

The worker uses a command line interface.

* Configure the project by copying the `grouperfish.json.example` to `grouperfish.json` and setting the properties you need. See `defaults.json` for the available parameters and default settings.
* Use `mvn install` to generate an assembly in `target/grouperfish-job.jar`
* Make sure `hbase` is running
* To create the hbase schema, (currently) you have to use the Node.JS-frontend (project *grouper-rest*) and to run `./bin/grouperfish reset` there (hbase-rest must be running).
* Import opinion sample data (takes a bit):
    > # creates various collections as determined by config option
    > # "input:import:collections:patterns" into namespace "myns"
    > wget http://input.mozilla.com/data/opinions.tsv.bz2
    > cat opinions.tsv.bz2 | bunzip2 \
    .    | hadoop jar target/grouperfish.jar import myns
* Cluster the collection "issue-firefox-4.0b12"
    > hadoop jar target/grouperfish-job.jar \
    >    "job:rebuild" "myns" "issue-firefox-4.0b12"

