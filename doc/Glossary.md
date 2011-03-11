## Grouperfish Glossary

#### Document
A piece of text to be clustered. Has an ID, and possibly an associated URL. Eample URLs: input feedback item page, tweet page, bugzilla-comment-anchor on bug page.

#### Collection, Bucket
Documents to be clustered together. The client application determines which documents belong to the same collection.

#### Namespace
Each client application has its own namespace, in which collection-keys and
document-ids must be unique.

#### Configuration
A set of a parameters for which clusters are generated. Configurations are specified per collection. Each configuration has a name. Initially, there is only one configuration ("DEFAULT") for each collection. Whenever a message is added, it is added to the clusters indicated by each configuration.
User-defined configurations are not offered by the first versions of Grouperfish (but planned for 1.0+). This is useful to try out new clustering algorithms.

#### Cluster
One cluster of documents generated for a clustering. Each cluster has a center, an ID (or label) and a map of document-IDs (using the IDs that were provided by the client applications) to distance (the smaller, the more representative). Membership of messages in clusters can change over time.

#### REST node, Producer
A service (in Node.JS) accepting new messages. The REST node stores incoming messages in storage and queues them to be processed by workers. Also fetches clusters for clients apps.

#### Storage
A persistent data store (hbase) for documents and clusters.

#### Worker, Consumer
A java process somewhere that is registered to the update queue to process incoming messages. A single physical host can run any number of workers.

#### Update Queue, Task Queue, Message Queue
Buffers incoming documents and hands them off to workers for processing, i.e. cluster updating.

### Lock
A worker takes a lock on a collection before handling a message about it. When a collection is locked, messages about it are queued again with a delay.

#### ACK
A worker signals the queue using an ACK when it has processed a message. This way the queue knows the message can be discarded.
