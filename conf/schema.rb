#! hbase shell

# TODO: Change the "<namespace>/<collection-key>" part of each row key to a 
#       more compact function of (namespace, collection-key).
#       We cannot use type-4 UUIDs because these are not injective (we need
#       to be able to lookup by the original key).
#
#       We might want to use type-3 or type-5 UUIDs. Text IDs (natural keys)
#       seem fine to get started and to see that everything works as 
#       expected though.

MAX_VERSION = 2147483647


create 'documents', {NAME => 'content'}, \
                    {NAME => 'mahout'}, \
                    {NAME => 'membership', VERSIONS => MAX_VERSION}
# Row Key: 
#   <namespace>/<collection-key>/<MD5(object-id)>
#   example: org.mozilla.input/desktop,windows,4.0/\xF5\x7B...
#
#   We put into the same family what is accessed and/or modified together.
#
#   'content' family:
#      'namespace'   the namespace (same that is used for the row key)
#      'collection'  the collection key (same)
#      'text'        the text content of the document
#      'id'          the original ID of the doc, provided the client
#      'url'         (optional) permalink to the original resource
#   'mahout' family:
#      'vector'      a sparse vector representation of the document text
#      'id'          the original ID, redundant, as input to the clusterer
#   'membership' family:
#      '<configuration-name>': '<cluster-id>' 
#                    Allows to track a document through clusterings.
#                    The timestamp should be the time a doc was added 
#                    to the cluster. Multiple configurations can be 
#                    "valid" for the same document. Each can have 
#                    multiple versions.
#                    To start with, there is one configuration: 
#                    "DEFAULT" with the special value "QUEUED"
#                    This also allows us to check if messages were 
#                    dropped from the queue.



create 'collections', {NAME => 'meta'}, \
                      {NAME => 'mahout'}, \
                      {NAME => 'configurations'}
# Row Key: <namespace>/<collection-key>
#
#   'meta':
#      'namespace'   the namespace (same as used for the row key)
#      'key'         the collection-key (same as used for the row key)
#      'size'        number of documents
#   'mahout':
#      'dictionary'  term->vector mappings, used to vectorize new documents
#   'configurations':
#      '<name>'      (at the start "DEFAULT" is the only name)
#                    hbase-timestamp: the time of the last update
#                    JSON-contents: clustering algorithm, parameters,
#                    distance measure, initial clustering time, 
#                    last-modified time
#                    In principle, this could also be something other than
#                    a clustering, e.g. a classification.



create 'clusters', {NAME => 'meta', VERSIONS => MAX_VERSION}, \
                   {NAME => 'mahout', VERSIONS => MAX_VERSION}, \
                   {NAME => 'documents', VERSIONS => MAX_VERSION}
# Row Key: 
# /<configuration-name>/<namespace>/<colection-key>/<ts>/<cluster-id>
# - in the beginning, configuration-name will always be "DEFAULT"
# - later on it will help with sharding (multiple configurations of the same
#   collection will be rebuilt at about the same time)
# - the timestamp <ts> refers to the full rebuild. Clusters of a previous
#   builds could actually be updated (for performance) while a complete 
#   reclustering is being computed (for quality). When a rebuild is 
#   complete, the 'current' timestamp of the configuration is updated.
# 
#     'meta': (like for collections)
#         'namespace'
#         'key'
#         'configuration'
#         'size'
#
#     'mahout':         
#         'center': a sparse vector for the center, e.g. used by k-means
#
#     'documents': 
#         '<original-id>': '<distance-to-centroid>'
#         , ... (lots of these, build can be 'replayed' looking at the ts)
#
