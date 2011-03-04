#Grouperfish
#### A scalable text clustering service for the web

The nascent Grouperfish project aims to provide a simple, online, scalable text clustering solution as a REST/JSON service. Initially this service is needed to drive sites and themes for [Firefox Input](http://input.mozilla.com), as described in [bug 629019](https://bugzilla.mozilla.org/show_bug.cgi?id=629019). The backing library used for clustering is [Apache Mahout](http://mahout.apache.org/).

### Service Requirements

To clients, Grouperfish is an HTTP service exposing three REST API methods:

#### *add document*

    POST /collections/<namespace>/<collection-key>
    
    {"id": <doc-id>, "text": <textToCluster>}

Queues a document for clustering. If documents with the same collection-key exist already, the new document is clustered together with them, and added to an existing cluster if appropriate. If the collection key does not exist yet, a new collection is created. The namespace serves as a general scope for collection key and document id. In later versions we’ll be able to manage permissions on this level, so that one Grouperfish installation can serve any number of clients.

#### *get all clusters*

    GET /clusters/<namespace>/<collection-key>

Fetches all *k* clusters associated with the given collection:

    {<cluster-label-1>: [<doc-id-1>, …, <doc-id-n>], …, <cluster-label-k>: […]}

The clusters consist only of their document ids. It is assumed that the client maintains the mapping to the original documents. The cluster labels are supposed to be desciptive text labels, based on common features of the documents in the cluster.

#### *get individual cluster*

    GET /clusters/<namespace>/<collection-key>/<cluster-label>

Fetches only the cluster with the given label.

    [<doc-id-1>, <doc-id-2>, …, <doc-id-n>]


### Architecture Requirements

There is a [blog article on the architecture](http://www.thefoundation.de/michael/2011/mar/01/scalable-text-clustering/).


## What's the status?

Development just started (Feb 20, 2011). The project is currently in the *early backyard stage* (aka 0.1), hoping to graduate to the basically usable *basement level* (0.2) mid March.

Nevertheless, if you are interested in helping out or if you have your own ideas on how to do this: please contact me on github. Also, see the roadmap below.


## Components

* [grouperfish](https://github.com/michaelku/grouperfish) -- this project: Mainly a central place for docs like this and github tickets etc. Not much actual code.

* [grouper-rest](https://github.com/michaelku/grouper-rest) -- A node-service for REST clients. They add documents and retrieve clusters here, in JSON format. Documents and clusters are stored in [riak](https://github.com/basho/riak), one bucket per corpus.

* [grouper-worker](https://github.com/michaelku/grouper-worker) -- A java based clustering worker. Developed to run in a web container (tomcat, jetty...) to allow querying the status. Actual processing is triggered by subscribing to a [message queue](http://www.rabbitmq.com/). Java is used for painless integration with Mahout/Hadoop.


Roadmap
-------

### 0.1 *(The Backyard Stage)* (Feb 20, 2011)
* A REST service that takes your documents and throws them away.
* It should return its favorite three clusters on every GET query.

> It was a crazy piece of near junk. 
> It looked as if it had been knocked up in somebody's backyard, 
> and this was in fact precisely where it had been knocked up. 
>
> -- Douglas Adams / Life, the Universe, and Everything 

### 0.2 *(The Basement Level)* (March 15, 2011)
* Working end-to-end process of storing docs and retrieving clusters: First implementation that can be used for input.
* Building of clusters, using [Canopy Clustering](https://cwiki.apache.org/confluence/display/MAHOUT/Canopy+Clustering) and then [K-Means](https://cwiki.apache.org/confluence/display/MAHOUT/K-Means+Clustering)).
* Incremental building of clusters ("like crystals in a water glass").
* Full initial build (or rebuild) of clusters from a TSV dump of the form: collection-key, document-id, text.
* Still serial processing (but already using a queue).

### 0.3 *(Garage Phase)* (Early April, 2011)
* Locking using redis or zookeeper…
* …allows for any number of workers
* Failure recovery: reconstruct redis in-memory state from riak using M/R

## 0.4 
* The more active collections (for input that means: latest version of Firefox, latest broken websites) need to be updated more often.
* Allow for any value stored in redis to expire, by putting serialized copies into riak. This way we don't hit the mempory limit over time.

### 1.0 *(Front Lawn Status)* (May - June 2011)
* Web frontend for introspection of collections and clusters.
* Push messages whenever clusters have changed (AMQP).
* Have a beer and BBQ in the sun.
