Grundle overview
================

Text clustering service for the web.


What is this?
-------------

Purpose: provide an online, scalable text clustering solution as a REST/JSON service, initially for [Firefox Input](http://input.mozilla.com), as triggered by this [bug](https://bugzilla.mozilla.org/show_bug.cgi?id=629019).

It must be able to handle small numbers of large document collections (millions of messages), as well as large numbers of small corpuses (millions of collections).


What's the status?
------------------

Development just started (Feb 20, 2011). The project is currently in the *early backyard stage*, hoping to graduate to *basement level* mid March.

*To quote Douglas Adams:*

> It was a crazy piece of near junk. 
> It looked as if it had been knocked up in somebody's backyard, 
> and this was in fact precisely where it had been knocked up. 
>
> -- Life, the Universe, and Everything 

Nevertheless, if you are interested in helping out or if you have your own ideas on how to do this: please contact me on github. Also, see the roadmap below.


Components
----------

* [grundle](http://github.com/michaelku/grundle) -- this project: Mainly a central place for docs like this and github tickets etc. Not much actual code.

* [grundle-rest](http://github.com/michaelku/grundle-rest) -- A node-service for REST clients. They add documents and retrieve clusters here, in JSON format. Documents and clusters are stored in [riak](https://github.com/basho/riak), one bucket per corpus.

* [grundle-worker](http://github.com/michaelku/grundle-worker) A java based clustering worker. Developed to run in a web container (tomcat, jetty...) to allow querying the status. Actual processing is triggered by subscribing to a [message queue](http://www.zeromq.org/). Java is used for painless integration with Mahout/Hadoop.


Roadmap
-------

### Backyard Stage (Feb 20, 2011)
* A REST service that takes your documents and throws them away.
* It should return its favorite three clusters on every GET query.

### Basement Level (March 15, 2011)
* Working end-to-end process of storing docs and retrieving clusters.
* Incremental building of clusters (using k-means), at least for big clusters
(where mapreduce is used). At least full rebuilds for the others.
* Serial processing, maybe worker threads (ugh).
* Look at the [big picture](http://github.com/michaelku/grundle/doc/medium_sized_picture.pdf) 
for insight through lines, boxes and colored text.
* Full (re-)build from a TSV dump of the form: collection-id, document-id, text

### Garage Phase (Early April, 2011)
* Multihost worker scaling (using a lock service, maybe zookeeper).
* Queue compaction. Or a multi-level queue. Or heapification. Or some other 
vague term hinting on how to handle live updates to clusters of varying size.
* More active collections (for input that means: latest version of Firefox, latest broken websites) need to be updated more quickly.

### Front Lawn Status (May - June 2011), aka v1.0
* Web frontend for introspection of collections and clusters.
* Public message push when clusters are updated (maybe AMQP).
* Have a beer and BBQ in the sun.
