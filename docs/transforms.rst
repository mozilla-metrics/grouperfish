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

TBD: Exact contents of a transform configuration


Result Types
------------

Clusters
^^^^^^^^

TBD: What is common to all clustering results?


Available Transforms
--------------------

Co-Clustering
^^^^^^^^^^^^^

Result Type
    Clusters

TBD: Describe how it works, link to papers, what parameters are available


LDA
^^^

Result Type
    Clusters

TBD: Describe how it works, link to papers, what parameters are available

